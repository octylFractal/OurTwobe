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

import org.bytedeco.ffmpeg.global.avutil.AV_OPT_SEARCH_CHILDREN
import org.bytedeco.ffmpeg.global.avutil.av_opt_set_int

class Resampler(
    inputFormat: Format,
    outputFormat: Format
) : FilterGraph(inputFormat, outputFormat, listOf(
    Filter("aresample", "resampler") {
        av_opt_set_int(it, "async", 1000, AV_OPT_SEARCH_CHILDREN)
    }
))
