package com.victor.video.compressor.library.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import kotlin.math.ln
import kotlin.math.pow

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2025-2035, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: CompressUtil
 * Author: Victor
 * Date: 2026/10/21 14:25
 * Description: 
 * -----------------------------------------------------------------
 */

object CompressUtil {

    suspend fun copyFileToLocal(context: Context,uri: Uri): File? {
        val fileName = generateFileName(context,uri) ?: return null
        val file = File(context.filesDir, fileName)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val outputStream = file.outputStream()

        withContext(Dispatchers.IO) {
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
        }

        return file
    }

    fun generateFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                val name = cursor.getString(nameIndex)
                val parts = name.split(".")

                return buildString {
                    append(parts.dropLast(1).joinToString("."))
                    append("_")
                    append(System.currentTimeMillis() / 1000)
                    append(".")
                    append(parts.last())
                }
            }
        }
        return null
    }

    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0s"

        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }

    fun formatBitrate(bitrate: Int): String {
        if (bitrate <= 0) return "N/A"
        val mbps = bitrate / 1_000_000.0
        return if (mbps >= 1.0) "${DecimalFormat("#.##").format(mbps)} Mbps" else "${DecimalFormat("#.##").format(bitrate / 1000.0)} Kbps"
    }

    fun File.getBitrate(): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        } finally {
            retriever.release()
        }
    }

    fun File.formatLength(): String {
        val bytes = this.length()
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val size = bytes / 1024.0.pow(digitGroups.toDouble())
        val formatter = DecimalFormat("#,##0.#")

        return "${formatter.format(size)} ${units[digitGroups]}"
    }
}