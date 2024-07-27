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

import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.global.avformat.AVSEEK_SIZE
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF
import org.bytedeco.ffmpeg.global.avutil.AVERROR_UNKNOWN
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.slf4j.LoggerFactory
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel

internal object ByteChannelAvioCallbacks {
    private val LOGGER = LoggerFactory.getLogger(ByteChannelAvioCallbacks::class.java)

    internal class ReadCallback(private val channel: ReadableByteChannel) : Read_packet_Pointer_BytePointer_int() {
        override fun call(opaque: Pointer?, buf: BytePointer, bufSize: Int): Int {
            return try {
                val read = channel.read(buf.limit(bufSize.toLong()).asBuffer())
                if (read == -1) {
                    AVERROR_EOF
                } else read
            } catch (t: Throwable) {
                LOGGER.warn("Error reading from channel", t)
                AVERROR_UNKNOWN
            }
        }
    }

    internal class WriteCallback(private val channel: WritableByteChannel) : Write_packet_Pointer_BytePointer_int() {
        override fun call(opaque: Pointer?, buf: BytePointer, bufSize: Int): Int {
            return try {
                channel.write(buf.limit(bufSize.toLong()).asBuffer().asReadOnlyBuffer())
            } catch (t: Throwable) {
                LOGGER.warn("Error writing to channel", t)
                AVERROR_UNKNOWN
            }
        }
    }

    internal class SeekCallback(private val channel: SeekableByteChannel) : Seek_Pointer_long_int() {
        companion object {
            private const val SEEK_SET = 0
            private const val SEEK_CUR = 1
            private const val SEEK_END = 2
        }

        override fun call(opaque: Pointer?, offset: Long, whence: Int): Long {
            return try {
                val base = when (whence) {
                    SEEK_SET -> 0
                    SEEK_CUR -> channel.position()
                    SEEK_END -> channel.size()
                    AVSEEK_SIZE -> return channel.size()
                    else -> return AVERROR_UNKNOWN.toLong()
                }
                channel.position(base + offset)
                0
            } catch (t: Throwable) {
                LOGGER.warn("Error seeking in channel", t)
                AVERROR_UNKNOWN.toLong()
            }
        }
    }
}
