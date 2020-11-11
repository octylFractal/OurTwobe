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

package net.octyl.ourtwobe.discord

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.audio.SpeakingMode
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.octyl.ourtwobe.datapipe.DataPipeEvent
import net.octyl.ourtwobe.datapipe.GuildSettingsHolder
import net.octyl.ourtwobe.datapipe.QueueManager
import net.octyl.ourtwobe.discord.audio.QueueSendHandler
import net.octyl.ourtwobe.util.exhaustive
import kotlin.coroutines.coroutineContext

class QueuePlayer(
    private val guildId: String,
    private val jda: JDA,
    private val queueManager: QueueManager,
    private val guildSettingsHolder: GuildSettingsHolder,
) {
    private val logger = KotlinLogging.logger { }
    private val messaging = Channel<PlayerCommand>(Channel.BUFFERED)
    private val _events = MutableStateFlow<DataPipeEvent.ProgressItem?>(null)
    val events: StateFlow<DataPipeEvent.ProgressItem?> = _events

    private val sendHandler = QueueSendHandler()

    init {
        jda.addEventListener(object {
            @SubscribeEvent
            fun onVoiceLeave(event: GuildVoiceLeaveEvent) {
                if (event.member.user.id == jda.selfUser.id && event.channelJoined == null) {
                    guildSettingsHolder.updateSettings {
                        it.copy(activeChannel = null)
                    }
                }
            }

            @SubscribeEvent
            fun onVoiceMove(event: GuildVoiceMoveEvent) {
                if (event.member.user.id == jda.selfUser.id) {
                    guildSettingsHolder.updateSettings {
                        it.copy(activeChannel = event.channelJoined.id)
                    }
                }
            }
        })
    }

    /**
     * Process player commands and play audio as needed.
     */
    suspend fun play() {
        supervisorScope {
            var queueDrainJob: Job? = null
            val audioManager = jda.getGuildById(guildId)!!.audioManager
            audioManager.setSpeakingMode(SpeakingMode.SOUNDSHARE)
            audioManager.sendingHandler = sendHandler
            for (command in messaging) {
                try {
                    exhaustive(when (command) {
                        is PlayerCommand.JoinChannel -> {
                            val channel = jda.getGuildById(guildId)!!.getVoiceChannelById(command.channel)!!
                            logger.info("Joining channel '${channel.name}' (${channel.id})")
                            queueDrainJob = launch { drainQueue(channel) }
                            audioManager.openAudioConnection(channel)
                        }
                        is PlayerCommand.Disconnect -> {
                            logger.info("Disconnecting or disconnected from voice")
                            audioManager.closeAudioConnection()
                            queueDrainJob?.cancelAndJoin()
                            _events.value?.let {
                                _events.value = it.copy(progress = 100.0)
                            }
                        }
                    })
                } catch (e: Exception) {
                    logger.warn(e) { "Error processing command $command" }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun drainQueue(channel: VoiceChannel) {
        while (coroutineContext.isActive) {
            val nextItem = coroutineScope {
                queueManager.remove(produce {
                    val updates = channelFlow {
                        send(Unit)
                        val listener = object {
                            @SubscribeEvent
                            fun onVoiceUpdate(event: GenericGuildVoiceEvent) {
                                if (event.guild.id == guildId) {
                                    sendBlocking(Unit)
                                }
                            }
                        }
                        jda.addEventListener(listener)
                        invokeOnClose {
                            jda.removeEventListener(listener)
                        }
                        delay(Long.MAX_VALUE)
                    }
                    updates.collect {
                        send(channel.members.mapTo(mutableSetOf()) { it.id })
                    }
                })
            }
            logger.info("Playing '${nextItem.title}' (${nextItem.youtubeId})")
            try {
                sendHandler.play(nextItem).collect {
                    _events.value = it
                }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to play '${nextItem.title}' (${nextItem.youtubeId})" }
            }
        }
    }

    suspend fun sendCommand(command: PlayerCommand) {
        messaging.send(command)
    }
}

sealed class PlayerCommand {
    data class JoinChannel(val channel: String) : PlayerCommand()

    /**
     * Player was disconnected from the audio channel for some reason.
     *
     * This will cancel the currently playing song.
     */
    object Disconnect : PlayerCommand()
}
