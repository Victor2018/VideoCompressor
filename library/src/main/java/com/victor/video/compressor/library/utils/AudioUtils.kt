package com.victor.video.compressor.library.utils

import android.media.MediaFormat

/**
 * Same as
 * https://developer.android.com/reference/android/media/MediaFormat#getInteger(java.lang.String,%20int)
 * but without API 29+ requirement.
 */
fun MediaFormat.getIntegerCompat(name: String, defaultValue: Int): Int {
    return if (containsKey(name)) getInteger(name) else defaultValue
}