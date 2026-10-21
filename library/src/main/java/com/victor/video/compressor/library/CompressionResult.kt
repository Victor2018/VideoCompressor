package com.victor.video.compressor.library

import kotlinx.coroutines.CancellationException
import java.io.File

sealed interface CompressionResult {

    class Success(val outFile: File): CompressionResult

    object Cancelled: CompressionResult

    class Started(val inputFile: File): CompressionResult

    class Error(val error: CompressionException): CompressionResult

    class Progress(val progress: Float): CompressionResult
}

internal suspend fun runAsResult(block: suspend () -> File): CompressionResult {
    return runCatching {
        CompressionResult.Success(block.invoke())
    }.getOrElse { error ->
        return when (error) {
            is CancellationException ->
                CompressionResult.Cancelled

            is CompressionException ->
                CompressionResult.Error(error)

            else ->
                CompressionResult.Error(CompressionException(error.localizedMessage, error))
        }
    }
}
