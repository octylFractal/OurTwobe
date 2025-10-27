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

package net.octyl.ourtwobe

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.octyl.ourtwobe.api.module
import net.octyl.ourtwobe.datapipe.GuildManager
import net.octyl.ourtwobe.discord.DiscordIdAuthorization
import net.octyl.ourtwobe.discord.audio.NanoSendSystem
import net.octyl.ourtwobe.files.FileItemResolver
import net.octyl.ourtwobe.util.OptimizedAnnotatedEventManager
import net.octyl.ourtwobe.youtube.api.YouTubeApi
import net.octyl.ourtwobe.youtube.YouTubeItemResolver
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val LOGGER = KotlinLogging.logger {}

val EVENT_POOL: ExecutorService = Executors.newSingleThreadExecutor(ThreadFactoryBuilder()
    .setDaemon(true)
    .setNameFormat("jda-event-thread-%d")
    .build())

fun main() {
    LOGGER.info("Spinning up OurTwobe...")
    val config = loadConfig(Path.of("./secrets/config.properties"))

    val jda = JDABuilder.createLight(config.discord.token)
        .enableIntents(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_VOICE_STATES,
        )
        .enableCache(
            CacheFlag.VOICE_STATE,
        )
        .setChunkingFilter(ChunkingFilter.ALL)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setEventManager(OptimizedAnnotatedEventManager())
        .setEventPool(EVENT_POOL)
        .setActivity(Activity.playing("your music!"))
        .setAudioSendFactory(::NanoSendSystem)
        .build()

    val embeddedServer = embeddedServer(Netty, port = 13445, host = "127.0.0.1") {
        module(
            DiscordIdAuthorization(config.owner),
            InternalPeeker(jda),
            GuildManager(config.youTube, jda),
            YouTubeItemResolver(YouTubeApi(config.youTube.token)),
            FileItemResolver(),
        )
    }
    embeddedServer.start()

    jda.awaitReady()
    LOGGER.info("OurTwobe is online and ready to go!")
    val shutdown = CountDownLatch(1)
    jda.addEventListener(object {
        @SubscribeEvent
        fun onStatusChange(event: StatusChangeEvent) {
            if (event.newStatus == JDA.Status.SHUTDOWN) {
                shutdown.countDown()
                jda.removeEventListener(this)
            }
        }
    })
    shutdown.await()
    LOGGER.warn("JDA shutdown received, entire server is going down!")
    embeddedServer.stop(1000L, 5000L)
}
