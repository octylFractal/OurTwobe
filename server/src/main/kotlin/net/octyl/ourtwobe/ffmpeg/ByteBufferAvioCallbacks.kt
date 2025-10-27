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

import mu.KotlinLogging
import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int
import org.bytedeco.ffmpeg.global.avformat.AVSEEK_SIZE
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF
import org.bytedeco.ffmpeg.global.avutil.AVERROR_UNKNOWN
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import java.nio.ByteBuffer

private val LOGGER = KotlinLogging.logger { }

/**
 * ByteBuffer-backed AVIO callbacks. Assumes the buffer is at position 0 and limit set to the size of the data.
 */
internal class ByteBufferAvioCallbacks(
    private val buffer: ByteBuffer,
) {
    companion object {
        private const val SEEK_SET = 0
        private const val SEEK_CUR = 1
        private const val SEEK_END = 2
    }

    fun read(): Read_packet_Pointer_BytePointer_int {
        return object : Read_packet_Pointer_BytePointer_int() {
            override fun call(opaque: Pointer?, buf: BytePointer, bufSize: Int): Int {
                return try {
                    val toRead = minOf(bufSize, buffer.remaining())
                    if (toRead == 0) {
                        return AVERROR_EOF
                    }
                    val sink = buf.limit(bufSize.toLong()).asBuffer()
                    sink.put(buffer.slice().limit(toRead))
                    buffer.position(buffer.position() + toRead)
                    toRead
                } catch (t: Throwable) {
                    LOGGER.warn("Error reading from ByteBuffer", t)
                    AVERROR_UNKNOWN
                }
            }
        }
    }

    fun seek(): Seek_Pointer_long_int {
        return object : Seek_Pointer_long_int() {

            override fun call(opaque: Pointer?, offset: Long, whence: Int): Long {
                return try {
                    val base = when (whence) {
                        SEEK_SET -> 0
                        SEEK_CUR -> buffer.position()
                        SEEK_END -> buffer.limit()
                        AVSEEK_SIZE -> return buffer.limit().toLong()
                        else -> return AVERROR_UNKNOWN.toLong()
                    }
                    val newPos = base + offset
                    buffer.position(newPos.toInt())
                    newPos
                } catch (t: Throwable) {
                    LOGGER.warn("Error seeking in ByteBuffer", t)
                    AVERROR_UNKNOWN.toLong()
                }
            }
        }
    }
}
