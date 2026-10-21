package com.victor.video.compressor.library

class CompressionException(
    override val message: String?,
    override val cause: Throwable?
): Exception()