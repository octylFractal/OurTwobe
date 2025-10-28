/*
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.octyl.ourtwobe.ffmpeg

import org.bytedeco.ffmpeg.avutil.AVChannelLayout
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.avutil.AV_CHANNEL_ORDER_NATIVE
import org.bytedeco.ffmpeg.global.avutil.av_make_error_string
import org.bytedeco.ffmpeg.global.avutil.av_opt_set
import org.bytedeco.ffmpeg.global.avutil.av_opt_set_bin
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.LongPointer
import org.bytedeco.javacpp.Pointer
import java.nio.ByteBuffer

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
    if (error < 0) {
        error(message(avErr2Str(error)))
    }
}

/**
 * Checked call to [org.bytedeco.ffmpeg.global.avutil.av_opt_set_bin].
 */
fun avOptSetList(obj: Pointer, name: String, ints: IntArray, searchFlags: Int): Int {
    val intsWithTerm = ints.size + 1
    return IntPointer(intsWithTerm.toLong()).use {
        it.put(*ints)
        it.put(ints.size.toLong(), -1)
        av_opt_set_bin(obj, name, BytePointer(it), intsWithTerm * Int.SIZE_BYTES, searchFlags)
    }
}

val LAYOUT_BUFFER = ThreadLocal.withInitial {
    ByteBuffer.allocateDirect(64)
}!!

fun avOptSetChLayoutViaString(
    obj: Pointer,
    name: String,
    layout: AVChannelLayout,
    searchFlags: Int,
): Int {
    val buffer = LAYOUT_BUFFER.get()
    while (true) {
        buffer.clear()
        val ret = avutil.av_channel_layout_describe(
            layout,
            buffer,
            buffer.remaining().toLong()
        )
        checkAv(ret) {
            "Failed to convert channel layout to string: $it"
        }
        if (ret <= buffer.remaining()) {
            buffer.position(0)
            buffer.limit(ret)
            break
        }
        // buffer too small, reallocate
        LAYOUT_BUFFER.set(ByteBuffer.allocateDirect(ret))
    }
    return av_opt_set(
        obj,
        BytePointer(name),
        BytePointer(buffer),
        searchFlags
    )
}
