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

package net.octyl.ourtwobe.datapipe

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildAvailableEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.octyl.ourtwobe.api.discorddata.ChannelData
import net.octyl.ourtwobe.api.discorddata.GuildData
import net.octyl.ourtwobe.api.discorddata.toChannelData
import net.octyl.ourtwobe.api.discorddata.toGuildData
import net.octyl.ourtwobe.discord.PlayerCommand
import net.octyl.ourtwobe.discord.QueuePlayer
import java.util.concurrent.ConcurrentHashMap

class GuildManager(
    private val jda: JDA,
) {
    private val queueScope = CoroutineScope(CoroutineName("QueuePlayer") + Dispatchers.Default)
    private val state = ConcurrentHashMap<String, GuildState>()

    private fun initState(guild: Guild) {
        state.computeIfAbsent(guild.id) { guildId ->
            guild.audioManager.closeAudioConnection()
            val guildSettingsHolder = GuildSettingsHolder()
            val queueManager = QueueManager()
            val queuePlayer = QueuePlayer(guildId, jda, queueManager, guildSettingsHolder)
            queueScope.launch {
                queuePlayer.play(
                    guildSettingsHolder.settings
                        .map { it.volume }
                        .distinctUntilChanged()
                        .map { it / 100.0 }
                        .stateIn(this)
                )
            }
            GuildState(queuePlayer, queueManager, guildSettingsHolder).also { state ->
                queueScope.launch {
                    state.guildSettingsHolder.settings
                        .map { it.activeChannel }
                        .distinctUntilChanged()
                        .collect {
                            when (it) {
                                null -> queuePlayer.sendCommand(PlayerCommand.Disconnect)
                                else -> queuePlayer.sendCommand(PlayerCommand.JoinChannel(it))
                            }
                        }
                }
            }
        }
    }

    init {
        jda.addEventListener(object {
            @SubscribeEvent
            fun onGuildReady(event: GuildReadyEvent) {
                initState(event.guild)
            }

            @SubscribeEvent
            fun onGuildJoin(event: GuildJoinEvent) {
                initState(event.guild)
            }

            @SubscribeEvent
            fun onGuildAvailable(event: GuildAvailableEvent) {
                initState(event.guild)
            }
        })
        jda.guilds.forEach(this::initState)
    }

    fun getGuildDatas(viewer: String): Set<GuildData> = state.keys.mapNotNullTo(mutableSetOf()) { guildId ->
        getGuildData(guildId, viewer)
    }

    private fun getVisibleGuild(guildId: String, viewer: String): Guild? =
        jda.getGuildById(guildId)?.takeIf { it.getMemberById(viewer) != null }

    fun getGuildData(guildId: String, viewer: String): GuildData? =
        getVisibleGuild(guildId, viewer)?.toGuildData()

    fun getChannelDatas(guildId: String, viewer: String): List<ChannelData>? =
        getVisibleGuild(guildId, viewer)?.let { guild ->
            guild.voiceChannels.map { it.toChannelData() }
        }

    fun canSee(guildId: String, viewer: String) = getVisibleGuild(guildId, viewer) != null

    fun canSeeChannel(guildId: String, viewer: String, channelId: String): Boolean {
        return getVisibleGuild(guildId, viewer)?.getVoiceChannelById(channelId) != null
    }

    fun getState(guildId: String): GuildState? = state[guildId]
}
