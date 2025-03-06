@Grab('org.apache.tika:tika-core:2.9.1')
@Grab('org.apache.tika:tika-langdetect-optimaize:2.9.1') // Correct dependency for OptimaizeLangDetector
import org.apache.tika.language.detect.LanguageDetector
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector

// Function to detect language
def detectLanguage(String text) {
    LanguageDetector detector = new OptimaizeLangDetector().loadModels() // Load language models
    def result = detector.detect(text) // Detect the language
    return result.language // Return ISO 639-1 code (e.g., "en", "fr")
}

// Test cases
def englishText = "Hello, how are you?"
def frenchText = "Bonjour, comment vas-tu?"
def spanishText = "Hola, ¿cómo estás?"
def creoleText = "Bonjou la! koman ou ye? Привет"

println "English text language: ${detectLanguage(englishText)}" // Expected: en
println "French text language: ${detectLanguage(frenchText)}"   // Expected: fr
println "Spanish text language: ${detectLanguage(spanishText)}" // Expected: es
println "Creole text language: ${detectLanguage(creoleText)}" // Expected: es