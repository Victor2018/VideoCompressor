package com.victor.video.compressor.library.data

import android.util.Size

/**
 * Metadata decoded from the input video.
 *
 * [isFastStartOptimized] describes MP4 atom order only. It does not guarantee codec or network
 * streaming compatibility.
 */
class Metadata internal constructor(
    internal val width: Int,
    internal val height: Int,
    internal val rotation: Int,
    val bitrate: Int,
    val isFastStartOptimized: Boolean
) {
    val actualWidth: Int get() =
        if (rotation == 90 || rotation == 270) height else width

    val actualHeight: Int get() =
        if (rotation == 90 || rotation == 270) width else height

    val size: Size get() =
        Size(actualWidth, actualHeight)
}
