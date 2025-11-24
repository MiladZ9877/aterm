package com.qali.aterm.autogent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Logger for AutoAgent activities for debugging and analysis
 */
object AutoAgentLogger {
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val maxLogs = 1000 // Keep last 1000 log entries
    
    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val category: String,
        val message: String,
        val metadata: Map<String, Any>? = null
    )
    
    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
    
    /**
     * Log AutoAgent activity
     */
    fun log(
        level: LogLevel,
        category: String,
        message: String,
        metadata: Map<String, Any>? = null
    ) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            message = message,
            metadata = metadata
        )
        
        logQueue.offer(entry)
        
        // Keep only last maxLogs entries
        while (logQueue.size > maxLogs) {
            logQueue.poll()
        }
        
        // Also log to Android logcat
        val logTag = "AutoAgent"
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(logTag, "[$category] $message")
            LogLevel.INFO -> android.util.Log.i(logTag, "[$category] $message")
            LogLevel.WARNING -> android.util.Log.w(logTag, "[$category] $message")
            LogLevel.ERROR -> android.util.Log.e(logTag, "[$category] $message")
        }
    }
    
    /**
     * Get all logs
     */
    fun getAllLogs(): List<LogEntry> {
        return logQueue.toList()
    }
    
    /**
     * Get logs by category
     */
    fun getLogsByCategory(category: String): List<LogEntry> {
        return logQueue.filter { it.category == category }
    }
    
    /**
     * Get logs by level
     */
    fun getLogsByLevel(level: LogLevel): List<LogEntry> {
        return logQueue.filter { it.level == level }
    }
    
    /**
     * Get recent logs (last N entries)
     */
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return logQueue.toList().takeLast(count)
    }
    
    /**
     * Get formatted debug information
     */
    suspend fun getDebugInfo(): String = withContext(Dispatchers.IO) {
        val logs = getAllLogs()
        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        
        buildString {
            appendLine("=== AutoAgent Debug Information ===")
            appendLine()
            
            // Statistics
            appendLine("--- Statistics ---")
            appendLine("Total Log Entries: ${logs.size}")
            appendLine("Debug: ${logs.count { it.level == LogLevel.DEBUG }}")
            appendLine("Info: ${logs.count { it.level == LogLevel.INFO }}")
            appendLine("Warning: ${logs.count { it.level == LogLevel.WARNING }}")
            appendLine("Error: ${logs.count { it.level == LogLevel.ERROR }}")
            appendLine()
            
            // Categories
            val categories = logs.map { it.category }.distinct()
            appendLine("--- Categories (${categories.size}) ---")
            categories.forEach { category ->
                val count = logs.count { it.category == category }
                appendLine("$category: $count entries")
            }
            appendLine()
            
            // Learning Statistics
            appendLine("--- Learning Statistics ---")
            try {
                val database = LearningDatabase.getInstance()
                val stats = AutoAgentLearningService.getLearningStats()
                appendLine("Code Snippets: ${stats.typeCounts[LearnedDataType.CODE_SNIPPET] ?: 0}")
                appendLine("API Usage: ${stats.typeCounts[LearnedDataType.API_USAGE] ?: 0}")
                appendLine("Fix Patches: ${stats.typeCounts[LearnedDataType.FIX_PATCH] ?: 0}")
                appendLine("Metadata Transformations: ${stats.typeCounts[LearnedDataType.METADATA_TRANSFORMATION] ?: 0}")
                appendLine("Total Score: ${stats.totalScore}")
            } catch (e: Exception) {
                appendLine("Error loading stats: ${e.message}")
            }
            appendLine()
            
            // Classification Model Status
            appendLine("--- Classification Model ---")
            val selectedModel = ClassificationModelManager.getSelectedModel()
            if (selectedModel != null) {
                appendLine("Selected: ${selectedModel.name}")
                appendLine("Type: ${selectedModel.modelType}")
                appendLine("Downloaded: ${selectedModel.isDownloaded}")
                appendLine("Ready: ${ClassificationModelManager.isModelReady()}")
            } else {
                appendLine("No model selected")
            }
            appendLine()
            
            // Recent Activity (last 50 entries)
            appendLine("--- Recent Activity (last 50) ---")
            logs.takeLast(50).forEach { entry ->
                val timeStr = dateFormat.format(Date(entry.timestamp))
                val levelStr = entry.level.name.padEnd(7)
                appendLine("[$timeStr] [$levelStr] [${entry.category}] ${entry.message}")
                entry.metadata?.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }
            }
            appendLine()
            
            // Error Logs
            val errors = logs.filter { it.level == LogLevel.ERROR }
            if (errors.isNotEmpty()) {
                appendLine("--- Errors (${errors.size}) ---")
                errors.takeLast(20).forEach { entry ->
                    val timeStr = dateFormat.format(Date(entry.timestamp))
                    appendLine("[$timeStr] [${entry.category}] ${entry.message}")
                    entry.metadata?.forEach { (key, value) ->
                        appendLine("  $key: $value")
                    }
                }
                appendLine()
            }
            
            // Learning Events
            val learningLogs = logs.filter { it.category.contains("Learning", ignoreCase = true) }
            if (learningLogs.isNotEmpty()) {
                appendLine("--- Learning Events (${learningLogs.size}) ---")
                learningLogs.takeLast(30).forEach { entry ->
                    val timeStr = dateFormat.format(Date(entry.timestamp))
                    appendLine("[$timeStr] ${entry.message}")
                }
                appendLine()
            }
        }
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        logQueue.clear()
    }
    
    // Convenience methods
    fun debug(category: String, message: String, metadata: Map<String, Any>? = null) {
        log(LogLevel.DEBUG, category, message, metadata)
    }
    
    fun info(category: String, message: String, metadata: Map<String, Any>? = null) {
        log(LogLevel.INFO, category, message, metadata)
    }
    
    fun warning(category: String, message: String, metadata: Map<String, Any>? = null) {
        log(LogLevel.WARNING, category, message, metadata)
    }
    
    fun error(category: String, message: String, metadata: Map<String, Any>? = null) {
        log(LogLevel.ERROR, category, message, metadata)
    }
}

