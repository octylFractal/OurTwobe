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
