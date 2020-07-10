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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.lifecycle.DisconnectEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.octyl.ourtwobe.pipe.GuildDataPipe
import net.octyl.ourtwobe.pipe.GuildUsersPipe
import net.octyl.ourtwobe.pipe.ProfilePipe
import net.octyl.ourtwobe.pipe.VoiceChannelDataPipe
import net.octyl.ourtwobe.pipe.VoiceStatePipe
import reactor.core.scheduler.Schedulers
import java.nio.file.Files
import java.nio.file.Path

private val LOGGER = KotlinLogging.logger {}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
suspend fun main() {
    LOGGER.info("Spinning up Ourtwobe...")
    val discordToken = withContext(Dispatchers.IO) {
        Files.readString(Path.of("./secrets/discord-token.txt")).trim()
    }
    val bot = DiscordClientBuilder.create(discordToken)
        // we use coroutines for all of our work, which switch threads anyways
        .setEventScheduler(Schedulers.immediate())
        .build()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("OurTwobeMain"))

    val firebase = initFirebase()
    val firestore: Firestore = FirestoreClient.getFirestore(firebase)

    coroutineScope {
        val pipes = listOf(
            GuildDataPipe(firestore),
            GuildUsersPipe(firestore),
            ProfilePipe(firestore),
            VoiceChannelDataPipe(firestore),
            VoiceStatePipe(firestore),
        )

        for (pipe in pipes) {
            // async-launch all of the pipes
            launch {
                // but remember to start them in the outer scope so they don't hold up initialization
                pipe.start(scope, bot)
            }
        }
    }

    scope.launch {
        bot.eventDispatcher.on<MessageCreateEvent>()
            .filter { event ->
                event.guildId.isEmpty &&
                    event.message.author.orElse(null)?.id != bot.selfId.orElseThrow()
            }
            .collect {
                it.message.channel
                    .flatMap { channel -> channel.createMessage("~no u~") }
                    .awaitFirst()
            }
    }
    scope.launch {
        bot.eventDispatcher.on<ReadyEvent>()
            .onEach {
                LOGGER.info { "Connected to Discord!" }
            }
            .flatMapConcat {
                bot.eventDispatcher.on<GuildCreateEvent>()
                    .take(it.guilds.size)
            }
            .collect {
                LOGGER.info { "Connected to guild: '${it.guild.name}' (${it.guild.id})" }
            }
    }
    scope.launch {
        bot.eventDispatcher.on<DisconnectEvent>()
            .collect {
                LOGGER.warn { "Disconnected from Discord :(" }
            }
    }
    bot.login()
        .asFlow()
        .catch {
            LOGGER.error(it) { "Error logging in to discord" }
        }
        .launchIn(scope)

    serveApi(FirebaseAuth.getInstance(firebase))
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiscordUser(
    val id: String,
    val username: String,
    val avatar: String,
)
