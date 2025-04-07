def folderPath = "path/to/your/target/folder" // Replace with your folder path
def testMethodPattern = ~/def\s+"[^"]+"\s*\(/
def ignorePattern = /@Ignore/
def wherePattern = ~/where\s*:/
def tableHeaderPattern = ~/^\s*[^\/].*\|/
def dataPipePattern = ~/^\s*(\w+)\s*<<\s*(.+)/

def ignoredTestMethods = [:] // Only store methods with @Ignore

new File(folderPath).eachFileRecurse { file ->
    if (file.name.endsWith('.groovy')) {
        def currentMethod = null
        def inWhereBlock = false
        def tableRows = []
        def pipeValues = [:]
        def hasIgnore = false

        file.eachLine { line ->
            if (line =~ ignorePattern) {
                hasIgnore = true // Flag @Ignore for the next method
            } else if (line =~ testMethodPattern) {
                // Finalize previous method if it had a where block
                if (currentMethod && (tableRows || pipeValues) && hasIgnore) {
                    ignoredTestMethods[currentMethod] = computeIterations(tableRows, pipeValues)
                }
                // Start new method
                currentMethod = line.trim().find(/def\s+"([^"]+)"/) { it[1] }
                if (hasIgnore) {
                    ignoredTestMethods[currentMethod] = 1 // Default to 1 for @Ignore methods
                }
                inWhereBlock = false
                tableRows = []
                pipeValues = [:]
                hasIgnore = false // Reset after method starts
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
                if (currentMethod && (tableRows || pipeValues) && ignoredTestMethods.containsKey(currentMethod)) {
                    ignoredTestMethods[currentMethod] = computeIterations(tableRows, pipeValues)
                }
                inWhereBlock = false
            }
        }
        // Handle file end
        if (currentMethod && (tableRows || pipeValues) && ignoredTestMethods.containsKey(currentMethod)) {
            ignoredTestMethods[currentMethod] = computeIterations(tableRows, pipeValues)
        }
    }
}

// Print results for ignored methods only
ignoredTestMethods.each { method, count ->
    println "Ignored Test: $method - Iterations: $count"
}
println "Total ignored test methods: ${ignoredTestMethods.size()}"

def computeIterations(tableRows, pipeValues) {
    if (tableRows) {
        return tableRows.size() - 1 // Subtract header row
    } else if (pipeValues) {
        return pipeValues.values().max() ?: 0
    }
    return 1
}

def evaluatePipeExpression(String expression) {
    def total = 0
    def parts = expression.split(/\s*\+\s*/)

    parts.each { part ->
        if (part =~ /Collections\.nCopies\((\d+),\s*[^)]+\)/) {
            def matcher = (part =~ /Collections\.nCopies\((\d+),\s*[^)]+\)/)
            total += matcher[0][1].toInteger()
        } else if (part =~ /^\[.*\]$/) {
            def content = part[1..-2]
            // Parse list respecting quoted commas
            def values = []
            def current = ''
            def inQuotes = false
            content.each { ch ->
                if (ch == '"' && !inQuotes) {
                    inQuotes = true
                } else if (ch == '"' && inQuotes) {
                    inQuotes = false
                } else if (ch == ',' && !inQuotes) {
                    if (current.trim()) values << current.trim()
                    current = ''
                } else {
                    current += ch
                }
            }
            if (current.trim()) values << current.trim()
            total += values.size()
        }
    }
    return total > 0 ? total : 1
}
