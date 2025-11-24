package com.qali.aterm.autogent

import java.util.regex.Pattern

/**
 * Syntax-based code parser that breaks code into meaningful chunks
 * for classification and learning
 */
object CodeParser {
    
    /**
     * Parse code into syntax-based chunks
     */
    fun parseCodeToChunks(code: String, language: String? = null): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        
        // Detect language if not provided
        val detectedLanguage = language ?: detectLanguage(code)
        
        when (detectedLanguage.lowercase()) {
            "kotlin", "java" -> chunks.addAll(parseKotlinJava(code))
            "javascript", "typescript", "js", "ts" -> chunks.addAll(parseJavaScript(code))
            "python", "py" -> chunks.addAll(parsePython(code))
            "xml", "html" -> chunks.addAll(parseXML(code))
            "css" -> chunks.addAll(parseCSS(code))
            "json" -> chunks.addAll(parseJSON(code))
            else -> chunks.addAll(parseGeneric(code))
        }
        
        return chunks
    }
    
    /**
     * Detect programming language from code
     */
    private fun detectLanguage(code: String): String {
        return when {
            code.contains("fun ") || code.contains("class ") && code.contains(":") -> "kotlin"
            code.contains("function ") || code.contains("const ") || code.contains("let ") -> "javascript"
            code.contains("def ") || code.contains("import ") && !code.contains("package") -> "python"
            code.contains("<?xml") || code.contains("<html") || code.contains("<") && code.contains(">") -> "xml"
            code.contains("{") && code.contains(":") && code.contains(";") && !code.contains("function") -> "css"
            code.trim().startsWith("{") || code.trim().startsWith("[") -> "json"
            else -> "generic"
        }
    }
    
    /**
     * Parse Kotlin/Java code
     */
    private fun parseKotlinJava(code: String): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        
        // Extract classes
        val classPattern = Pattern.compile("(?:class|interface|enum|data class|sealed class)\\s+(\\w+)(?:[^\\{]*)\\{([^\\}]*)\\}", Pattern.DOTALL)
        val classMatcher = classPattern.matcher(code)
        while (classMatcher.find()) {
            chunks.add(CodeChunk(
                type = ChunkType.CLASS,
                name = classMatcher.group(1),
                content = classMatcher.group(0),
                properties = extractProperties(classMatcher.group(2))
            ))
        }
        
        // Extract functions
        val functionPattern = Pattern.compile("(?:fun|function|public|private|protected)?\\s*(?:\\w+\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*(?:[^\\{]*)\\{([^\\}]*)\\}", Pattern.DOTALL)
        val functionMatcher = functionPattern.matcher(code)
        while (functionMatcher.find()) {
            chunks.add(CodeChunk(
                type = ChunkType.FUNCTION,
                name = functionMatcher.group(1),
                content = functionMatcher.group(0),
                properties = extractProperties(functionMatcher.group(2))
            ))
        }
        
        // Extract objects/instances
        val objectPattern = Pattern.compile("(?:object|val|var)\\s+(\\w+)\\s*(?:[:=][^;\\n]*)?", Pattern.MULTILINE)
        val objectMatcher = objectPattern.matcher(code)
        while (objectMatcher.find()) {
            chunks.add(CodeChunk(
                type = ChunkType.OBJECT,
                name = objectMatcher.group(1),
                content = objectMatcher.group(0),
                properties = emptyMap()
            ))
        }
        
        return chunks
    }
    
    /**
     * Parse JavaScript/TypeScript code
     */
    private fun parseJavaScript(code: String): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        
        // Extract classes
        val classPattern = Pattern.compile("class\\s+(\\w+)(?:[^\\{]*)\\{([^\\}]*)\\}", Pattern.DOTALL)
        val classMatcher = classPattern.matcher(code)
        while (classMatcher.find()) {
            chunks.add(CodeChunk(
                type = ChunkType.CLASS,
                name = classMatcher.group(1),
                content = classMatcher.group(0),
                properties = extractProperties(classMatcher.group(2))
            ))
        }
        
        // Extract functions
        val functionPattern = Pattern.compile("(?:function|const|let|var)\\s*(\\w+)\\s*[=\\(](?:[^\\{]*)\\{([^\\}]*)\\}", Pattern.DOTALL)
        val functionMatcher = functionPattern.matcher(code)
        while (functionMatcher.find()) {
            chunks.add(CodeChunk(
                type = ChunkType.FUNCTION,
                name = functionMatcher.group(1),
                content = functionMatcher.group(0),
                properties = extractProperties(functionMatcher.group(2))
            ))
        }
        
        // Extract objects
        val objectPattern = Pattern.compile("(?:const|let|var)\\s+(\\w+)\\s*=\\s*\\{([^\\}]*)\\}", Pattern.DOTALL)
        val objectMatcher = objectPattern.matcher(code)
        while (objectMatcher.find()) {
            chunks.add(CodeChunk(
                type = ChunkType.OBJECT,
                name = objectMatcher.group(1),
                content = objectMatcher.group(0),
                properties = extractObjectProperties(objectMatcher.group(2))
            ))
        }
        
        return chunks
    }
    
    /**
     * Parse Python code
     */
    private fun parsePython(code: String): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        
        // Extract classes
        val classPattern = Pattern.compile("class\\s+(\\w+)(?:[^:]*):([^\\n]*(?:\\n(?:\\s{4,}|\\t)[^\\n]*)*)", Pattern.MULTILINE)
        val classMatcher = classPattern.matcher(code)
        while (classMatcher.find()) {
            chunks.add(CodeChunk(
                type = ChunkType.CLASS,
                name = classMatcher.group(1),
                content = classMatcher.group(0),
                properties = extractProperties(classMatcher.group(2))
            ))
        }
        
        // Extract functions
        val functionPattern = Pattern.compile("def\\s+(\\w+)\\s*\\([^)]*\\):([^\\n]*(?:\\n(?:\\s{4,}|\\t)[^\\n]*)*)", Pattern.MULTILINE)
        val functionMatcher = functionPattern.matcher(code)
        while (functionMatcher.find()) {
            chunks.add(CodeChunk(
                type = ChunkType.FUNCTION,
                name = functionMatcher.group(1),
                content = functionMatcher.group(0),
                properties = extractProperties(functionMatcher.group(2))
            ))
        }
        
        return chunks
    }
    
    /**
     * Parse XML/HTML
     */
    private fun parseXML(code: String): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        
        // Extract elements
        val elementPattern = Pattern.compile("<(\\w+)([^>]*)>([^<]*)</\\1>", Pattern.DOTALL)
        val elementMatcher = elementPattern.matcher(code)
        while (elementMatcher.find()) {
            val attributes = extractXMLAttributes(elementMatcher.group(2))
            chunks.add(CodeChunk(
                type = ChunkType.OBJECT,
                name = elementMatcher.group(1),
                content = elementMatcher.group(0),
                properties = attributes
            ))
        }
        
        return chunks
    }
    
    /**
     * Parse CSS
     */
    private fun parseCSS(code: String): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        
        // Extract CSS rules
        val rulePattern = Pattern.compile("([^{]+)\\{([^}]+)\\}", Pattern.MULTILINE)
        val ruleMatcher = rulePattern.matcher(code)
        while (ruleMatcher.find()) {
            val selector = ruleMatcher.group(1).trim()
            val properties = extractCSSProperties(ruleMatcher.group(2))
            chunks.add(CodeChunk(
                type = ChunkType.OBJECT,
                name = selector,
                content = ruleMatcher.group(0),
                properties = properties
            ))
        }
        
        return chunks
    }
    
    /**
     * Parse JSON
     */
    private fun parseJSON(code: String): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        
        // Simple JSON object extraction
        val objectPattern = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"([^\"]+)\"", Pattern.MULTILINE)
        val objectMatcher = objectPattern.matcher(code)
        val properties = mutableMapOf<String, String>()
        while (objectMatcher.find()) {
            properties[objectMatcher.group(1)] = objectMatcher.group(2)
        }
        
        if (properties.isNotEmpty()) {
            chunks.add(CodeChunk(
                type = ChunkType.OBJECT,
                name = "json_object",
                content = code,
                properties = properties
            ))
        }
        
        return chunks
    }
    
    /**
     * Generic parser for unknown languages
     */
    private fun parseGeneric(code: String): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        
        // Extract any identifiable patterns
        val identifierPattern = Pattern.compile("(\\w+)\\s*[:=]\\s*([^;\\n]+)", Pattern.MULTILINE)
        val identifierMatcher = identifierPattern.matcher(code)
        val properties = mutableMapOf<String, String>()
        while (identifierMatcher.find()) {
            properties[identifierMatcher.group(1)] = identifierMatcher.group(2).trim()
        }
        
        if (properties.isNotEmpty()) {
            chunks.add(CodeChunk(
                type = ChunkType.OBJECT,
                name = "generic_object",
                content = code,
                properties = properties
            ))
        }
        
        return chunks
    }
    
    /**
     * Extract properties from code block
     */
    private fun extractProperties(code: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        
        // Extract variable assignments
        val varPattern = Pattern.compile("(?:val|var|let|const)\\s+(\\w+)\\s*[:=]\\s*([^;\\n]+)", Pattern.MULTILINE)
        val varMatcher = varPattern.matcher(code)
        while (varMatcher.find()) {
            properties[varMatcher.group(1)] = varMatcher.group(2).trim()
        }
        
        return properties
    }
    
    /**
     * Extract object properties
     */
    private fun extractObjectProperties(code: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        
        // Extract key-value pairs
        val kvPattern = Pattern.compile("(\\w+)\\s*[:=]\\s*([^,}]+)", Pattern.MULTILINE)
        val kvMatcher = kvPattern.matcher(code)
        while (kvMatcher.find()) {
            properties[kvMatcher.group(1)] = kvMatcher.group(2).trim().trim('"', '\'')
        }
        
        return properties
    }
    
    /**
     * Extract XML attributes
     */
    private fun extractXMLAttributes(attributes: String): Map<String, String> {
        val props = mutableMapOf<String, String>()
        val attrPattern = Pattern.compile("(\\w+)=\"([^\"]+)\"")
        val attrMatcher = attrPattern.matcher(attributes)
        while (attrMatcher.find()) {
            props[attrMatcher.group(1)] = attrMatcher.group(2)
        }
        return props
    }
    
    /**
     * Extract CSS properties
     */
    private fun extractCSSProperties(css: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        val propPattern = Pattern.compile("([^:]+):\\s*([^;]+);", Pattern.MULTILINE)
        val propMatcher = propPattern.matcher(css)
        while (propMatcher.find()) {
            properties[propMatcher.group(1).trim()] = propMatcher.group(2).trim()
        }
        return properties
    }
    
    /**
     * Extract theme-related properties (colors, styles, etc.)
     */
    fun extractThemeProperties(chunks: List<CodeChunk>): Map<String, String> {
        val themeProps = mutableMapOf<String, String>()
        
        chunks.forEach { chunk ->
            chunk.properties.forEach { (key, value) ->
                // Check for color-related properties
                if (key.contains("color", ignoreCase = true) ||
                    key.contains("background", ignoreCase = true) ||
                    key.contains("theme", ignoreCase = true) ||
                    key.contains("style", ignoreCase = true) ||
                    value.matches(Regex("#[0-9A-Fa-f]{3,6}")) ||
                    value.matches(Regex("rgb\\([^)]+\\)")) ||
                    value.matches(Regex("rgba\\([^)]+\\)"))) {
                    themeProps[key] = value
                }
            }
        }
        
        return themeProps
    }
    
    /**
     * Extract text content from code chunks
     */
    fun extractTextContent(chunks: List<CodeChunk>): List<String> {
        val texts = mutableListOf<String>()
        
        chunks.forEach { chunk ->
            // Extract string literals
            val stringPattern = Pattern.compile("\"([^\"]+)\"|'([^']+)'")
            val stringMatcher = stringPattern.matcher(chunk.content)
            while (stringMatcher.find()) {
                val text = stringMatcher.group(1) ?: stringMatcher.group(2)
                if (text != null && text.length > 3) {
                    texts.add(text)
                }
            }
        }
        
        return texts
    }
}

data class CodeChunk(
    val type: ChunkType,
    val name: String,
    val content: String,
    val properties: Map<String, String>
)

enum class ChunkType {
    CLASS,
    FUNCTION,
    OBJECT,
    VARIABLE,
    PROPERTY
}

