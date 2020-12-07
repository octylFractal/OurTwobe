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

package net.octyl.ourtwobe.api

import com.google.common.cache.CacheBuilder
import io.ktor.sessions.SessionStorage
import io.ktor.util.cio.toByteArray
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import java.time.Duration

class SimpleSessionStorage : SessionStorage {
    private val cache = CacheBuilder.newBuilder()
        // Don't keep too many sessions active
        .maximumSize(1_000L)
        // Expire sessions after 1 day otherwise
        .expireAfterWrite(Duration.ofDays(1L))
        .build<String, ByteArray>()

    override suspend fun invalidate(id: String) {
        cache.invalidate(id)
    }

    override suspend fun <R> read(id: String, consumer: suspend (ByteReadChannel) -> R): R =
        cache.getIfPresent(id)?.let { data -> consumer(ByteReadChannel(data)) }
            ?: throw NoSuchElementException("Session $id not found")

    override suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit) {
        coroutineScope {
            val channel = writer(Dispatchers.Unconfined, autoFlush = true) {
                provider(channel)
            }.channel

            cache.put(id, channel.toByteArray())
        }
    }
}
