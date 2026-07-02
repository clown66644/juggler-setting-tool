package com.example.jugglersettingtool.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val TAG = "JugglerSettingTool"
    private const val LOG_FILE_NAME = "error_log.txt"
    private const val ENTRY_SEPARATOR = "\n--------------------\n\n"

    fun log(context: Context, category: String, message: String) {
        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            logFile(context).appendText("[$timeStamp] [$category] $message$ENTRY_SEPARATOR")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write app log.", e)
        }
    }

    fun readLogs(context: Context): String =
        try {
            val file = logFile(context)
            if (file.exists()) {
                file.readText()
            } else {
                "ログはありません。"
            }
        } catch (e: Exception) {
            "ログの読み込みに失敗しました: ${e.message}"
        }

    fun clearLogs(context: Context): Boolean =
        try {
            val file = logFile(context)
            !file.exists() || file.delete()
        } catch (e: Exception) {
            false
        }

    private fun logFile(context: Context): File = File(context.filesDir, LOG_FILE_NAME)
}
