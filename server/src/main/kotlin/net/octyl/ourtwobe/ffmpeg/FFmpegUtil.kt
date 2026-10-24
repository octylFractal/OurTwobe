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

import org.bytedeco.ffmpeg.global.avutil.AV_OPT_ARRAY_REPLACE
import org.bytedeco.ffmpeg.global.avutil.av_make_error_string
import org.bytedeco.ffmpeg.global.avutil.av_opt_set_array
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.IntPointer
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
    if (error < 0) {
        error(message(avErr2Str(error)))
    }
}

/**
 * Checked call to [av_opt_set_array], writing [count] elements of [valType] from [values] starting at index 0.
 */
fun avOptSetArray(obj: Pointer, name: String, valType: Int, count: Int, values: Pointer, searchFlags: Int) {
    checkAv(av_opt_set_array(obj, name, searchFlags or AV_OPT_ARRAY_REPLACE, 0, count, valType, values)) {
        "Unable to set option '$name': $it"
    }
}

/**
 * [avOptSetArray] for int-backed [valType]s, such as `AV_OPT_TYPE_INT` or `AV_OPT_TYPE_SAMPLE_FMT`.
 */
fun avOptSetArray(obj: Pointer, name: String, valType: Int, values: IntArray, searchFlags: Int) {
    IntPointer(*values).use {
        avOptSetArray(obj, name, valType, values.size, it, searchFlags)
    }
}
