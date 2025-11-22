package com.rk.libcommons

import android.content.Context
import java.io.File
import com.qali.aterm.BuildConfig
import com.rk.settings.Settings
import com.qali.aterm.ui.screens.settings.WorkingMode

private fun getFilesDir(): File{
    return if (application == null){
        if (BuildConfig.DEBUG){
            File("/data/data/com.qali.aterm.debug/files")
        }else{
            File("/data/data/com.qali.aterm/files")
        }
    }else{
        application!!.filesDir
    }
}

fun localDir(): File {
    return File(getFilesDir().parentFile, "local").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun alpineDir(): File{
    return localDir().child("alpine").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun alpineHomeDir(): File{
    return alpineDir().child("root").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

/**
 * Returns the rootfs directory based on the current working mode.
 * - ALPINE (0): returns alpine directory
 * - ANDROID (1): returns alpine directory (Android uses same structure)
 * - UBUNTU (2): returns ubuntu directory
 */
fun getRootfsDir(): File {
    return when (Settings.working_Mode) {
        WorkingMode.ALPINE, WorkingMode.ANDROID -> alpineDir()
        WorkingMode.UBUNTU -> localDir().child("ubuntu").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        else -> alpineDir() // Default fallback
    }
}

/**
 * Returns the rootfs directory for a specific working mode.
 */
fun getRootfsDirForMode(workingMode: Int): File {
    return when (workingMode) {
        WorkingMode.ALPINE, WorkingMode.ANDROID -> alpineDir()
        WorkingMode.UBUNTU -> localDir().child("ubuntu").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        else -> alpineDir() // Default fallback
    }
}

/**
 * Returns the rootfs directory for a specific session ID.
 * Uses the provided working mode if available, otherwise falls back to current working mode.
 * 
 * @param sessionId The session ID (for future use if needed)
 * @param workingMode Optional working mode for the session. If null, uses current working mode.
 */
fun getRootfsDirForSession(sessionId: String, workingMode: Int? = null): File {
    return if (workingMode != null) {
        getRootfsDirForMode(workingMode)
    } else {
        // Fallback to current working mode
        getRootfsDir()
    }
}

/**
 * Returns the home directory within the rootfs based on the current working mode.
 */
fun getRootfsHomeDir(): File {
    return when (Settings.working_Mode) {
        WorkingMode.ALPINE, WorkingMode.ANDROID -> alpineHomeDir()
        WorkingMode.UBUNTU -> getRootfsDir().child("root").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        else -> alpineHomeDir() // Default fallback
    }
}

fun localBinDir(): File {
    return localDir().child("bin").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun localLibDir(): File {
    return localDir().child("lib").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun File.child(fileName:String):File {
    return File(this,fileName)
}

fun File.createFileIfNot():File{
    if (exists().not()){
        createNewFile()
    }
    return this
}