package com.qali.aterm.autogent

import java.util.regex.Pattern

/**
 * Mediapipe-style text classification for learned data
 * Uses pattern matching and rule-based classification similar to Mediapipe
 */
object TextClassifier {
    
    /**
     * Classify text content into learned data types
     */
    fun classify(content: String, context: String? = null): String {
        // Normalize content
        val normalized = content.trim()
        
        // Check for code snippets (function definitions, class definitions, code blocks)
        if (isCodeSnippet(normalized)) {
            return LearnedDataType.CODE_SNIPPET
        }
        
        // Check for API usage patterns
        if (isApiUsage(normalized)) {
            return LearnedDataType.API_USAGE
        }
        
        // Check for fixes/patches
        if (isFixPatch(normalized, context)) {
            return LearnedDataType.FIX_PATCH
        }
        
        // Default to metadata/transformation
        return LearnedDataType.METADATA_TRANSFORMATION
    }
    
    /**
     * Check if content is a code snippet
     */
    private fun isCodeSnippet(content: String): Boolean {
        // Patterns for code snippets
        val codePatterns = listOf(
            Pattern.compile("^\\s*(fun|function|def|class|interface|enum|struct|trait)\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*\\{", Pattern.MULTILINE),
            Pattern.compile("^\\s*\\[", Pattern.MULTILINE),
            Pattern.compile("^\\s*import\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*package\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*const\\s+|^\\s*let\\s+|^\\s*var\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*return\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*if\\s*\\(|^\\s*if\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*for\\s*\\(|^\\s*for\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*while\\s*\\(|^\\s*while\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*switch\\s*\\(|^\\s*when\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE), // Code blocks
            Pattern.compile("^\\s*<[^>]+>", Pattern.MULTILINE) // XML/HTML tags
        )
        
        return codePatterns.any { it.matcher(content).find() }
    }
    
    /**
     * Check if content is API usage
     */
    private fun isApiUsage(content: String): Boolean {
        // Patterns for API usage
        val apiPatterns = listOf(
            Pattern.compile("\\.[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(", Pattern.MULTILINE), // Method calls
            Pattern.compile("API|api|Api", Pattern.MULTILINE),
            Pattern.compile("http[s]?://", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fetch|request|response|axios|http", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.[a-zA-Z_][a-zA-Z0-9_]*\\s*=\\s*", Pattern.MULTILINE), // Property assignments
            Pattern.compile("new\\s+[A-Z][a-zA-Z0-9_]*\\s*\\(", Pattern.CASE_INSENSITIVE), // Constructor calls
            Pattern.compile("@[A-Z][a-zA-Z0-9_]*", Pattern.MULTILINE) // Annotations
        )
        
        // Check for API-like patterns but not full code snippets
        val hasApiPattern = apiPatterns.any { it.matcher(content).find() }
        val isFullCode = isCodeSnippet(content)
        
        return hasApiPattern && !isFullCode
    }
    
    /**
     * Check if content is a fix/patch
     */
    private fun isFixPatch(content: String, context: String?): Boolean {
        // Patterns for fixes/patches
        val fixPatterns = listOf(
            Pattern.compile("fix|Fix|FIX|bug|Bug|BUG|error|Error|ERROR", Pattern.MULTILINE),
            Pattern.compile("patch|Patch|PATCH|correct|Correct|CORRECT", Pattern.MULTILINE),
            Pattern.compile("resolve|Resolve|RESOLVE|solve|Solve|SOLVE", Pattern.MULTILINE),
            Pattern.compile("issue|Issue|ISSUE|problem|Problem|PROBLEM", Pattern.MULTILINE),
            Pattern.compile("^-\\s+.*\\n\\+\\s+.*", Pattern.MULTILINE), // Diff format
            Pattern.compile("before:|after:|old:|new:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("changed|Changed|CHANGED|updated|Updated|UPDATED", Pattern.MULTILINE)
        )
        
        // Check context for fix-related keywords
        val contextHasFix = context?.let {
            fixPatterns.any { pattern -> pattern.matcher(it).find() }
        } ?: false
        
        return fixPatterns.any { it.matcher(content).find() } || contextHasFix
    }
    
    /**
     * Extract key features from content for better classification
     */
    fun extractFeatures(content: String): Map<String, Any> {
        val features = mutableMapOf<String, Any>()
        
        // Count code-like structures
        features["hasFunctions"] = Pattern.compile("\\bfun\\s+|\\bfunction\\s+|\\bdef\\s+", Pattern.CASE_INSENSITIVE).matcher(content).find()
        features["hasClasses"] = Pattern.compile("\\bclass\\s+|\\binterface\\s+", Pattern.CASE_INSENSITIVE).matcher(content).find()
        features["hasImports"] = Pattern.compile("^\\s*import\\s+", Pattern.MULTILINE).matcher(content).find()
        features["hasApiCalls"] = Pattern.compile("\\.[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(", Pattern.MULTILINE).matcher(content).find()
        features["hasComments"] = Pattern.compile("//|#|/\\*|<!--", Pattern.MULTILINE).matcher(content).find()
        features["lineCount"] = content.lines().size
        features["hasDiffFormat"] = Pattern.compile("^-\\s+.*\\n\\+\\s+.*", Pattern.MULTILINE).matcher(content).find()
        
        return features
    }
    
    /**
     * Classify with confidence score
     */
    fun classifyWithConfidence(content: String, context: String? = null): Pair<String, Float> {
        val classification = classify(content, context)
        val features = extractFeatures(content)
        
        // Calculate confidence based on feature matches
        var confidence = 0.5f
        
        when (classification) {
            LearnedDataType.CODE_SNIPPET -> {
                if (features["hasFunctions"] == true || features["hasClasses"] == true) {
                    confidence = 0.9f
                } else if (features["hasImports"] == true) {
                    confidence = 0.7f
                }
            }
            LearnedDataType.API_USAGE -> {
                if (features["hasApiCalls"] == true) {
                    confidence = 0.8f
                } else {
                    confidence = 0.6f
                }
            }
            LearnedDataType.FIX_PATCH -> {
                if (features["hasDiffFormat"] == true) {
                    confidence = 0.9f
                } else {
                    confidence = 0.7f
                }
            }
            LearnedDataType.METADATA_TRANSFORMATION -> {
                confidence = 0.6f
            }
        }
        
        return Pair(classification, confidence)
    }
}

