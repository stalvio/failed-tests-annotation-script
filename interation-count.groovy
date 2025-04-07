def folderPath = "path/to/your/target/folder" // Replace with your folder path
def testMethodPattern = ~/def\s+"[^"]+"\s*\(/
def wherePattern = ~/where\s*:/
def tableHeaderPattern = ~/^\s*[^\/].*\|/
def dataPipePattern = ~/^\s*(\w+)\s*<<\s*(.+)/

def testMethods = [:]

new File(folderPath).eachFileRecurse { file ->
    if (file.name.endsWith('.groovy')) {
        println "Processing file: ${file.name}"
        def currentMethod = null
        def inWhereBlock = false
        def tableRows = []
        def pipeValues = [:]

        file.eachLine { line ->
            println "Line: $line"
            if (line =~ testMethodPattern) {
                if (currentMethod && (tableRows || pipeValues)) {
                    testMethods[currentMethod] = computeIterations(tableRows, pipeValues)
                }
                currentMethod = line.trim().find(/def\s+"([^"]+)"/) { it[1] }
                testMethods[currentMethod] = 1
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
                println "Parsed $varName: '$expression' -> $count"
            } else if (inWhereBlock && line =~ /^\s*$/) {
                if (tableRows || pipeValues) {
                    testMethods[currentMethod] = computeIterations(tableRows, pipeValues)
                }
                inWhereBlock = false
            }
        }
        if (inWhereBlock && (tableRows || pipeValues)) {
            testMethods[currentMethod] = computeIterations(tableRows, pipeValues)
        }
    }
}

testMethods.each { method, count ->
    println "Test: $method - Iterations: $count"
}
println "Total test methods: ${testMethods.size()}"

def computeIterations(tableRows, pipeValues) {
    if (tableRows) {
        return tableRows.size() - 1
    } else if (pipeValues) {
        def max = pipeValues.values().max()
        println "Pipe values: $pipeValues, Max: $max"
        return max ?: 0
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
            println "nCopies part: $part -> ${matcher[0][1].toInteger()}"
        } else if (part =~ /^\[.*\]$/) {
            def content = part[1..-2]
            def values = content.split(',').collect { it.trim() }.findAll { it }
            total += values.size()
            println "List part: $part -> ${values.size()} (values: $values)"
        }
    }
    return total > 0 ? total : 1
}
