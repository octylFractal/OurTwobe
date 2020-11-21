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

package net.octyl.ourtwobe.discord.audio

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import net.dv8tion.jda.api.audio.OpusPacket
import net.dv8tion.jda.api.audio.factory.DefaultSendSystem
import net.dv8tion.jda.api.audio.factory.IAudioSendSystem
import net.dv8tion.jda.api.audio.factory.IPacketProvider
import net.dv8tion.jda.internal.audio.AudioConnection
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.net.NoRouteToHostException
import java.net.SocketException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors
import java.util.concurrent.locks.LockSupport

private val OPUS_FRAME_DURATION = Duration.ofMillis(
    OpusPacket.OPUS_FRAME_TIME_AMOUNT.toLong()
)
private val CATCH_UP_TRIGGER = OPUS_FRAME_DURATION.multipliedBy(3)

/**
 * A re-implementation of [DefaultSendSystem] that tries to be more accurate.
 *
 * Partially derived from said class, which is licensed under the Apache 2 license.
 */
class NanoSendSystem(
    private val packetProvider: IPacketProvider
) : IAudioSendSystem {
    private val executor = Executors.newSingleThreadExecutor(ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("NSS Audio Thread")
        .setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2)
        .setThreadFactory { runnable ->
            Thread {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap)
                }
                runnable.run()
            }
        }
        .setUncaughtExceptionHandler { _, throwable: Throwable ->
            LoggerFactory.getLogger(javaClass)
                .error("Uncaught exception in audio send thread", throwable)
            start()
        }
        .build())
    private val scope = CoroutineScope(executor.asCoroutineDispatcher() + SupervisorJob())
    private var contextMap: ConcurrentMap<String, String>? = null

    override fun setContextMap(contextMap: ConcurrentMap<String, String>?) {
        this.contextMap = contextMap
    }

    override fun start() {
        val udpSocket = packetProvider.udpSocket
        // produce 20ms pulses
        val ticker = flow {
            var base = System.nanoTime()
            while (true) {
                val spinDeadline = base + OPUS_FRAME_DURATION.toNanos()
                val deadline = spinDeadline - 2_500_000L
                while (System.nanoTime() < deadline) {
                    currentCoroutineContext().ensureActive()
                    runInterruptible {
                        LockSupport.parkNanos(deadline - System.nanoTime())
                    }
                }
                while (System.nanoTime() < spinDeadline) {
                    Thread.onSpinWait()
                }
                emit(Unit)
                base = spinDeadline
            }
        }
            .flowOn(Dispatchers.Default)
            .buffer()
        scope.launch(CoroutineName("${packetProvider.identifier} Socket")) {
            var sentPacket = true
            var lastFrameSent = Instant.now()
            ticker.collect {
                if (udpSocket.isClosed) {
                    cancel()
                    return@collect
                }
                try {
                    val changeTalking = !sentPacket
                        || Duration.between(lastFrameSent, Instant.now()) < OPUS_FRAME_DURATION
                    val packet = packetProvider.getNextPacket(changeTalking)
                    sentPacket = packet != null
                    if (sentPacket) {
                        udpSocket.send(packet)
                    }
                } catch (e: NoRouteToHostException) {
                    packetProvider.onConnectionLost()
                } catch (e: SocketException) {
                    // Most likely the socket has been closed due to the audio connection be closed.
                    // Next iteration will kill loop.
                } catch (e: Exception) {
                    AudioConnection.LOG.error("Error while sending udp audio data", e)
                }
                val now = Instant.now()
                lastFrameSent = when {
                    now > (lastFrameSent + CATCH_UP_TRIGGER) -> lastFrameSent + OPUS_FRAME_DURATION
                    else -> now
                }
            }
        }
    }

    override fun shutdown() {
        scope.cancel()
        executor.shutdown()
    }
}
