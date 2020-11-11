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

import org.bytedeco.ffmpeg.avformat.AVIOContext
import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.global.avformat
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import java.nio.channels.Channel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel

/**
 * Stores a set of AVIO callbacks. This class will [retain them][Pointer.retainReference]
 * until closed.
 *
 * @param resource the resource that is associated with the callbacks, and should be closed after them
 */
class AvioCallbacks(
    private val readCallback: Read_packet_Pointer_BytePointer_int?,
    private val writeCallback: Write_packet_Pointer_BytePointer_int?,
    private val seekCallback: Seek_Pointer_long_int?,
    resource: AutoCloseable?
) : AutoCloseable {
    private val closer = AutoCloser()

    init {
        readCallback?.let { closer.register(it).retainReference<Pointer>() }
        writeCallback?.let { closer.register(it).retainReference<Pointer>() }
        seekCallback?.let { closer.register(it).retainReference<Pointer>() }
        closer.register(resource)
    }

    /**
     * Allocate a context with the callbacks in this object.
     *
     *
     *
     * Will *not* free the context automatically, that is up to you.
     *
     *
     * @return AVIO context if allocated, `null` if failed
     */
    fun allocateContext(bufferSize: Int, writable: Boolean): AVIOContext? {
        return avformat.avio_alloc_context(
                BytePointer(avutil.av_malloc(bufferSize.toLong())), bufferSize, if (writable) 1 else 0, null,
                readCallback, writeCallback, seekCallback
        )
    }

    @Throws(Exception::class)
    override fun close() {
        closer.close()
    }

    companion object {
        fun forChannel(channel: Channel?): AvioCallbacks {
            return AvioCallbacks(
                    if (channel is ReadableByteChannel) ByteChannelAvioCallbacks.ReadCallback(channel) else null,
                    if (channel is WritableByteChannel) ByteChannelAvioCallbacks.WriteCallback(channel) else null,
                    if (channel is SeekableByteChannel) ByteChannelAvioCallbacks.SeekCallback(channel) else null,
                    channel
            )
        }
    }
}
