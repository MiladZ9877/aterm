package com.qali.aterm.autogent

import com.qali.aterm.api.ApiProviderManager
import com.qali.aterm.api.ApiProviderType
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enhanced learning service that learns prompt → metadata → code relationships
 * and object-based patterns for theme/color/text changes
 */
object EnhancedLearningService {
    private val database = LearningDatabase.getInstance()
    private val codeParser = CodeParser
    private val classifier = TextClassifier
    
    private val learningScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val learningQueue = ConcurrentLinkedQueue<EnhancedLearningTask>()
    
    /**
     * Learn from complete code generation with prompt-to-code flow
     */
    fun learnFromCompleteGeneration(
        userPrompt: String,
        generatedCode: String,
        metadata: Map<String, Any>? = null,
        source: String = LearnedDataSource.NORMAL_FLOW
    ) {
        if (!shouldLearn()) return
        
        AutoAgentLogger.info("EnhancedLearning", "Learning from complete generation", mapOf(
            "promptLength" to userPrompt.length,
            "codeLength" to generatedCode.length,
            "source" to source
        ))
        
        learningQueue.offer(
            EnhancedLearningTask.CompleteGeneration(
                userPrompt = userPrompt,
                generatedCode = generatedCode,
                metadata = metadata,
                source = source
            )
        )
        
        processLearningQueue()
    }
    
    /**
     * Learn from AI API response that includes reason for change
     */
    fun learnFromAIResponseWithReason(
        userPrompt: String,
        oldCode: String?,
        newCode: String,
        reason: String,
        metadata: Map<String, Any>? = null
    ) {
        if (!shouldLearn()) return
        
        learningQueue.offer(
            EnhancedLearningTask.AIResponseWithReason(
                userPrompt = userPrompt,
                oldCode = oldCode,
                newCode = newCode,
                reason = reason,
                metadata = metadata
            )
        )
        
        processLearningQueue()
    }
    
    /**
     * Learn object-based patterns (for theme/color/text changes)
     */
    fun learnObjectPattern(
        objectName: String,
        objectType: String,
        properties: Map<String, String>,
        userPrompt: String,
        context: String
    ) {
        if (!shouldLearn()) return
        
        learningQueue.offer(
            EnhancedLearningTask.ObjectPattern(
                objectName = objectName,
                objectType = objectType,
                properties = properties,
                userPrompt = userPrompt,
                context = context
            )
        )
        
        processLearningQueue()
    }
    
    private fun shouldLearn(): Boolean {
        return ApiProviderManager.selectedProvider != ApiProviderType.AUTOAGENT
    }
    
    private fun processLearningQueue() {
        learningScope.launch {
            while (learningQueue.isNotEmpty()) {
                val task = learningQueue.poll() ?: break
                processEnhancedTask(task)
            }
        }
    }
    
    private suspend fun processEnhancedTask(task: EnhancedLearningTask) = withContext(Dispatchers.IO) {
        try {
            when (task) {
                is EnhancedLearningTask.CompleteGeneration -> {
                    processCompleteGeneration(task)
                }
                is EnhancedLearningTask.AIResponseWithReason -> {
                    processAIResponseWithReason(task)
                }
                is EnhancedLearningTask.ObjectPattern -> {
                    processObjectPattern(task)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EnhancedLearning", "Error processing task", e)
        }
    }
    
    /**
     * Process complete code generation - learn prompt → metadata → code flow
     */
    private suspend fun processCompleteGeneration(task: EnhancedLearningTask.CompleteGeneration) {
        AutoAgentLogger.debug("EnhancedLearning", "Processing complete generation", mapOf("codeLength" to task.generatedCode.length))
        
        // Parse code into chunks
        val chunks = codeParser.parseCodeToChunks(task.generatedCode)
        AutoAgentLogger.info("EnhancedLearning", "Parsed code into chunks", mapOf("chunkCount" to chunks.size))
        
        // Extract metadata from prompt and code
        val extractedMetadata = extractMetadataFromPrompt(task.userPrompt, chunks)
        val combinedMetadata = (task.metadata ?: emptyMap()) + extractedMetadata
        
        // Learn each chunk with its metadata relationship
        chunks.forEach { chunk ->
            // Classify chunk based on metadata
            val chunkClassification = classifier.classifyWithConfidence(
                chunk.content,
                task.userPrompt
            )
            
            // Build metadata JSON with prompt expectations
            val metadataJson = buildMetadataJson(
                userPrompt = task.userPrompt,
                promptExpectations = extractPromptExpectations(task.userPrompt),
                metadata = combinedMetadata,
                chunkType = chunk.type.name,
                chunkName = chunk.name,
                chunkProperties = chunk.properties
            )
            
            // Store with prompt-to-code relationship
            database.insertOrUpdateLearnedData(
                type = chunkClassification.first,
                content = chunk.content,
                source = task.source,
                metadata = metadataJson,
                incrementScore = true,
                userPrompt = task.userPrompt
            )
        }
        
        // Learn theme/text patterns if detected
        val themeProps = codeParser.extractThemeProperties(chunks)
        if (themeProps.isNotEmpty()) {
            learnThemePattern(task.userPrompt, themeProps, chunks)
        }
        
        val textContent = codeParser.extractTextContent(chunks)
        if (textContent.isNotEmpty()) {
            learnTextPattern(task.userPrompt, textContent, chunks)
        }
    }
    
    /**
     * Process AI response with reason - learn debugging patterns
     */
    private suspend fun processAIResponseWithReason(task: EnhancedLearningTask.AIResponseWithReason) {
        // Parse both old and new code
        val oldChunks = task.oldCode?.let { codeParser.parseCodeToChunks(it) } ?: emptyList()
        val newChunks = codeParser.parseCodeToChunks(task.newCode)
        
        // Find differences
        val differences = findCodeDifferences(oldChunks, newChunks)
        
        // Learn each difference with reason
        differences.forEach { diff ->
            val metadataJson = buildString {
                append("{")
                append("\"old_code\":\"${diff.oldContent.replace("\"", "\\\"")}\",")
                append("\"new_code\":\"${diff.newContent.replace("\"", "\\\"")}\",")
                append("\"reason\":\"${task.reason.replace("\"", "\\\"")}\",")
                append("\"change_type\":\"${diff.changeType}\",")
                append("\"user_prompt\":\"${task.userPrompt.replace("\"", "\\\"")}\"")
                append("}")
            }
            
            database.insertOrUpdateLearnedData(
                type = LearnedDataType.FIX_PATCH,
                content = "OLD:\n${diff.oldContent}\n\nNEW:\n${diff.newContent}\n\nREASON: ${task.reason}",
                source = LearnedDataSource.DEBUG_FEEDBACK,
                metadata = metadataJson,
                incrementScore = true,
                userPrompt = task.userPrompt
            )
        }
        
        // Learn reason classification
        learnReasonClassification(task.reason, differences)
    }
    
    /**
     * Process object pattern - learn for theme/color/text changes
     */
    private suspend fun processObjectPattern(task: EnhancedLearningTask.ObjectPattern) {
        // Build object metadata
        val objectMetadata = buildString {
            append("{")
            append("\"object_name\":\"${task.objectName}\",")
            append("\"object_type\":\"${task.objectType}\",")
            append("\"properties\":{")
            task.properties.entries.joinToString(",") { (key, value) ->
                "\"$key\":\"$value\""
            }.let { append(it) }
            append("},")
            append("\"user_prompt\":\"${task.userPrompt.replace("\"", "\\\"")}\",")
            append("\"context\":\"${task.context.replace("\"", "\\\"")}\"")
            append("}")
        }
        
        // Store object pattern
        database.insertOrUpdateLearnedData(
            type = LearnedDataType.METADATA_TRANSFORMATION,
            content = buildObjectContent(task),
            source = LearnedDataSource.NORMAL_FLOW,
            metadata = objectMetadata,
            incrementScore = true,
            userPrompt = task.userPrompt
        )
    }
    
    /**
     * Extract metadata from prompt and code chunks
     */
    private fun extractMetadataFromPrompt(prompt: String, chunks: List<CodeChunk>): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        
        // Extract intent keywords
        val intentKeywords = extractIntentKeywords(prompt)
        metadata["intent"] = intentKeywords
        
        // Extract object mentions
        val objectMentions = chunks.map { it.name }.distinct()
        metadata["objects"] = objectMentions
        
        // Extract property mentions
        val allProperties = chunks.flatMap { it.properties.keys }.distinct()
        metadata["properties"] = allProperties
        
        return metadata
    }
    
    /**
     * Extract prompt expectations
     */
    private fun extractPromptExpectations(prompt: String): List<String> {
        val expectations = mutableListOf<String>()
        
        // Look for expectation patterns
        val expectationPatterns = listOf(
            "should",
            "must",
            "need",
            "require",
            "expect",
            "want",
            "create",
            "build",
            "generate"
        )
        
        prompt.lowercase().split(Regex("[.!?\\n]")).forEach { sentence ->
            expectationPatterns.forEach { pattern ->
                if (sentence.contains(pattern)) {
                    expectations.add(sentence.trim())
                }
            }
        }
        
        return expectations
    }
    
    /**
     * Build metadata JSON with full relationship
     */
    private fun buildMetadataJson(
        userPrompt: String,
        promptExpectations: List<String>,
        metadata: Map<String, Any>,
        chunkType: String,
        chunkName: String,
        chunkProperties: Map<String, String>
    ): String {
        return buildString {
            append("{")
            append("\"user_prompt\":\"${userPrompt.replace("\"", "\\\"")}\",")
            append("\"prompt_expectations\":[${promptExpectations.joinToString(",") { "\"$it\"" }}],")
            append("\"chunk_type\":\"$chunkType\",")
            append("\"chunk_name\":\"$chunkName\",")
            append("\"chunk_properties\":{")
            chunkProperties.entries.joinToString(",") { (key, value) ->
                "\"$key\":\"${value.replace("\"", "\\\"")}\""
            }.let { append(it) }
            append("},")
            append("\"metadata\":{")
            metadata.entries.joinToString(",") { (key, value) ->
                "\"$key\":\"${value.toString().replace("\"", "\\\"")}\""
            }.let { append(it) }
            append("}")
            append("}")
        }
    }
    
    /**
     * Learn theme pattern (for color/style changes)
     */
    private suspend fun learnThemePattern(
        userPrompt: String,
        themeProps: Map<String, String>,
        chunks: List<CodeChunk>
    ) {
        themeProps.forEach { (prop, value) ->
            learnObjectPattern(
                objectName = "theme_${prop}",
                objectType = "theme_property",
                properties = mapOf(prop to value),
                userPrompt = userPrompt,
                context = "Theme change: $prop = $value"
            )
        }
    }
    
    /**
     * Learn text pattern
     */
    private suspend fun learnTextPattern(
        userPrompt: String,
        texts: List<String>,
        chunks: List<CodeChunk>
    ) {
        texts.forEach { text ->
            learnObjectPattern(
                objectName = "text_content",
                objectType = "text",
                properties = mapOf("content" to text),
                userPrompt = userPrompt,
                context = "Text content: $text"
            )
        }
    }
    
    /**
     * Find differences between old and new code chunks
     */
    private fun findCodeDifferences(
        oldChunks: List<CodeChunk>,
        newChunks: List<CodeChunk>
    ): List<CodeDifference> {
        val differences = mutableListOf<CodeDifference>()
        
        // Find modified chunks
        newChunks.forEach { newChunk ->
            val oldChunk = oldChunks.find { it.name == newChunk.name && it.type == newChunk.type }
            if (oldChunk != null && oldChunk.content != newChunk.content) {
                differences.add(CodeDifference(
                    changeType = "modified",
                    oldContent = oldChunk.content,
                    newContent = newChunk.content,
                    chunkName = newChunk.name
                ))
            } else if (oldChunk == null) {
                differences.add(CodeDifference(
                    changeType = "added",
                    oldContent = "",
                    newContent = newChunk.content,
                    chunkName = newChunk.name
                ))
            }
        }
        
        // Find removed chunks
        oldChunks.forEach { oldChunk ->
            if (newChunks.none { it.name == oldChunk.name && it.type == oldChunk.type }) {
                differences.add(CodeDifference(
                    changeType = "removed",
                    oldContent = oldChunk.content,
                    newContent = "",
                    chunkName = oldChunk.name
                ))
            }
        }
        
        return differences
    }
    
    /**
     * Learn reason classification for debugging
     */
    private suspend fun learnReasonClassification(reason: String, differences: List<CodeDifference>) {
        // Classify reason type
        val reasonType = when {
            reason.contains("error", ignoreCase = true) || reason.contains("bug", ignoreCase = true) -> "error_fix"
            reason.contains("improve", ignoreCase = true) || reason.contains("optimize", ignoreCase = true) -> "optimization"
            reason.contains("add", ignoreCase = true) || reason.contains("create", ignoreCase = true) -> "addition"
            reason.contains("remove", ignoreCase = true) || reason.contains("delete", ignoreCase = true) -> "removal"
            reason.contains("change", ignoreCase = true) || reason.contains("modify", ignoreCase = true) -> "modification"
            else -> "general"
        }
        
        // Store reason classification
        differences.forEach { diff ->
            val metadataJson = buildString {
                append("{")
                append("\"reason\":\"${reason.replace("\"", "\\\"")}\",")
                append("\"reason_type\":\"$reasonType\",")
                append("\"change_type\":\"${diff.changeType}\",")
                append("\"chunk_name\":\"${diff.chunkName}\"")
                append("}")
            }
            
            database.insertOrUpdateLearnedData(
                type = LearnedDataType.FIX_PATCH,
                content = "REASON: $reason\n\nCHANGE: ${diff.changeType}\n\n${diff.newContent}",
                source = LearnedDataSource.DEBUG_FEEDBACK,
                metadata = metadataJson,
                incrementScore = true,
                userPrompt = null
            )
        }
    }
    
    /**
     * Extract intent keywords from prompt
     */
    private fun extractIntentKeywords(prompt: String): List<String> {
        val keywords = mutableListOf<String>()
        
        // Common intent patterns
        val intentPatterns = mapOf(
            "create" to listOf("create", "build", "make", "generate"),
            "modify" to listOf("change", "modify", "update", "edit"),
            "fix" to listOf("fix", "repair", "correct", "debug"),
            "theme" to listOf("theme", "color", "style", "design"),
            "text" to listOf("text", "content", "string", "message")
        )
        
        val promptLower = prompt.lowercase()
        intentPatterns.forEach { (intent, patterns) ->
            if (patterns.any { promptLower.contains(it) }) {
                keywords.add(intent)
            }
        }
        
        return keywords
    }
    
    /**
     * Build object content string
     */
    private fun buildObjectContent(task: EnhancedLearningTask.ObjectPattern): String {
        return buildString {
            append("${task.objectType}: ${task.objectName}\n")
            append("Properties:\n")
            task.properties.forEach { (key, value) ->
                append("  $key: $value\n")
            }
            append("\nContext: ${task.context}")
        }
    }
}

sealed class EnhancedLearningTask {
    data class CompleteGeneration(
        val userPrompt: String,
        val generatedCode: String,
        val metadata: Map<String, Any>?,
        val source: String
    ) : EnhancedLearningTask()
    
    data class AIResponseWithReason(
        val userPrompt: String,
        val oldCode: String?,
        val newCode: String,
        val reason: String,
        val metadata: Map<String, Any>?
    ) : EnhancedLearningTask()
    
    data class ObjectPattern(
        val objectName: String,
        val objectType: String,
        val properties: Map<String, String>,
        val userPrompt: String,
        val context: String
    ) : EnhancedLearningTask()
}

data class CodeDifference(
    val changeType: String,
    val oldContent: String,
    val newContent: String,
    val chunkName: String
)

