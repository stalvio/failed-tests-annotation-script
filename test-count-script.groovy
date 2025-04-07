def folderPath = "path/to/your/target/folder" // Replace with your folder path
def testMethodPattern = ~/def\s+"[^"]+"\s*\(/
def wherePattern = ~/where\s*:/
def tableHeaderPattern = ~/^\s*[^\/].*\|/ // Matches table header or data row with |
def dataPipePattern = ~/^\s*(\w+)\s*<<\s*\[(.*?)\]/ // Matches a << [values]

def testMethods = [:] // Map of test method name to iteration count

new File(folderPath).eachFileRecurse { file ->
    if (file.name.endsWith('.groovy')) {
        def currentMethod = null
        def inWhereBlock = false
        def iterationCount = 0
        def tableRows = []
        def pipeValues = [:]

        file.eachLine { line ->
            if (line =~ testMethodPattern) {
                // Start of a new test method
                if (currentMethod && (tableRows || pipeValues)) {
                    // Finalize previous method's count
                    testMethods[currentMethod] = computeIterations(tableRows, pipeValues)
                }
                currentMethod = line.trim().find(/def\s+"([^"]+)"/) { it[1] }
                testMethods[currentMethod] = 1 // Default to 1
                inWhereBlock = false
                iterationCount = 0
                tableRows = []
                pipeValues = [:]
            } else if (currentMethod && line =~ wherePattern) {
                inWhereBlock = true
            } else if (inWhereBlock && line =~ tableHeaderPattern) {
                // Collect table rows
                tableRows << line
            } else if (inWhereBlock && line =~ dataPipePattern) {
                // Collect data pipe values
                def matcher = (line =~ dataPipePattern)
                def varName = matcher[0][1]
                def values = matcher[0][2].split(',').collect { it.trim() }.findAll { it }
                pipeValues[varName] = values.size()
            } else if (inWhereBlock && line =~ /^\s*$/) {
                // Empty line might end the where block
                if (tableRows || pipeValues) {
                    testMethods[currentMethod] = computeIterations(tableRows, pipeValues)
                }
                inWhereBlock = false
            }
        }
        // Handle case where file ends in a where block
        if (inWhereBlock && (tableRows || pipeValues)) {
            testMethods[currentMethod] = computeIterations(tableRows, pipeValues)
        }
    }
}

// Print results
testMethods.each { method, count ->
    println "Test: $method - Iterations: $count"
}
println "Total test methods: ${testMethods.size()}"

// Helper function to compute iterations
def computeIterations(tableRows, pipeValues) {
    if (tableRows) {
        // Table style: count data rows (exclude header)
        return tableRows.size() - 1 // Subtract 1 for the header row
    } else if (pipeValues) {
        // Data pipe style: use the max number of values across variables
        return pipeValues.values().max() ?: 0
    }
    return 1 // Fallback (shouldn't hit this due to default)
}
