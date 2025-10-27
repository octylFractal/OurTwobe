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

package net.octyl.ourtwobe.files

import net.octyl.ourtwobe.datapipe.FileContentKey
import net.octyl.ourtwobe.datapipe.PlayableItem
import net.octyl.ourtwobe.ffmpeg.AvioCallbacks
import net.octyl.ourtwobe.ffmpeg.FileItemInfo
import java.nio.ByteBuffer

class FileItemResolver {
    fun resolveItem(filename: String, fileContent: ByteBuffer): PlayableItem {
        val info = FileItemInfo.load(filename, AvioCallbacks.forReadingByteBuffer(fileContent))
        return PlayableItem(
            contentKey = FileContentKey(filename, fileContent),
            title = info.title ?: filename,
            thumbnail = info.thumbnail,
            duration = info.duration,
        )
    }
}
