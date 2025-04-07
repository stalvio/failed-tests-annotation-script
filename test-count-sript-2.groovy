def folderPath = "path/to/your/target/folder" // Replace with your folder path
def testMethodPattern = ~/def\s+"[^"]+"\s*\(/
def wherePattern = ~/where\s*:/
def tableHeaderPattern = ~/^\s*[^\/].*\|/ // Matches table header or data row with |
def dataPipePattern = ~/^\s*(\w+)\s*<<\s*(.+)/ // Matches a << anything

def testMethods = [:] // Map of test method name to iteration count

new File(folderPath).eachFileRecurse { file ->
    if (file.name.endsWith('.groovy')) {
        def currentMethod = null
        def inWhereBlock = false
        def tableRows = []
        def pipeValues = [:]

        file.eachLine { line ->
            if (line =~ testMethodPattern) {
                // Start of a new test method
                if (currentMethod && (tableRows || pipeValues)) {
                    testMethods[currentMethod] = computeIterations(tableRows, pipeValues)
                }
                currentMethod = line.trim().find(/def\s+"([^"]+)"/) { it[1] }
                testMethods[currentMethod] = 1 // Default to 1
                inWhereBlock = false
                tableRows = []
                pipeValues = [:]
            } else if (currentMethod && line =~ wherePattern) {
                inWhereBlock = true
            } else if (inWhereBlock && line =~ tableHeaderPattern) {
                tableRows << line
            } else if (inWhereBlock && line =~ dataPipePattern) {
                def matcher = (line =~ dataPipePattern)
                def varName = matcher[0][1]
                def expression = matcher[0][2].trim()
                def count = evaluatePipeExpression(expression)
                pipeValues[varName] = count
            } else if (inWhereBlock && line =~ /^\s*$/) {
                if (tableRows || pipeValues) {
                    testMethods[currentMethod] = computeIterations(tableRows, pipeValues)
                }
                inWhereBlock = false
            }
        }
        // Handle file end
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
        return tableRows.size() - 1 // Subtract header row
    } else if (pipeValues) {
        return pipeValues.values().max() ?: 0
    }
    return 1
}

// Helper function to evaluate data pipe expressions
def evaluatePipeExpression(String expression) {
    // Match simple list literals: [1, 2, 3]
    if (expression =~ /^\[.*\]$/) {
        def values = expression[1..-2].split(',').collect { it.trim() }.findAll { it }
        return values.size()
    }

    // Match Collections.nCopies(n, value)
    def nCopiesPattern = /Collections\.nCopies\((\d+),\s*[^)]+\)/
    def total = 0
    def parts = expression.split(/\s*\+\s*/)

    parts.each { part ->
        if (part =~ nCopiesPattern) {
            def matcher = (part =~ nCopiesPattern)
            total += matcher[0][1].toInteger()
        } else if (part =~ /^\[.*\]$/) {
            def values = part[1..-2].split(',').collect { it.trim() }.findAll { it }
            total += values.size()
        }
    }
    return total > 0 ? total : 1 // Fallback if no matches
}
