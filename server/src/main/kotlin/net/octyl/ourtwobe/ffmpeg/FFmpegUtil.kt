/*
 * This file is part of beat-dropper, licensed under the MIT License (MIT).
 *
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.octyl.ourtwobe.ffmpeg

import org.bytedeco.ffmpeg.global.avutil.av_make_error_string
import org.bytedeco.ffmpeg.global.avutil.av_opt_set_bin
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.LongPointer
import org.bytedeco.javacpp.Pointer

private const val AV_ERROR_MAX_STRING_SIZE = 64L

fun avErr2Str(error: Int): String {
    BytePointer(AV_ERROR_MAX_STRING_SIZE).use { buffer ->
        return av_make_error_string(
            buffer,
            AV_ERROR_MAX_STRING_SIZE,
            error
        ).string
    }
}

inline fun checkAv(error: Int, message: (error: String) -> String) {
    if (error != 0) {
        error(message(avErr2Str(error)))
    }
}

/**
 * Checked call to [org.bytedeco.ffmpeg.global.avutil.av_opt_set_bin].
 */
fun avOptSetList(obj: Pointer, name: String, ints: IntArray, search_flags: Int): Int {
    val intsWithTerm = ints + -1
    return IntPointer(*intsWithTerm).use {
        av_opt_set_bin(obj, name, BytePointer(it), intsWithTerm.size * Int.SIZE_BYTES, search_flags)
    }
}

/**
 * Checked call to [org.bytedeco.ffmpeg.global.avutil.av_opt_set_bin].
 */
fun avOptSetList(obj: Pointer, name: String, longs: LongArray, search_flags: Int): Int {
    val longsWithTerm = longs + -1
    return LongPointer(*longsWithTerm).use {
        av_opt_set_bin(obj, name, BytePointer(it), longsWithTerm.size * Long.SIZE_BYTES, search_flags)
    }
}

// ffmpeg accepts channels by hex mask
fun channelLayoutName(channelLayout: Long): String = "0x${channelLayout.toString(16)}"
