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
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildAvailableEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
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
                queuePlayer.play()
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

    fun canSee(guildId: String, viewer: String) = jda.getGuildById(guildId)?.getMemberById(viewer) != null

    fun getState(guildId: String): GuildState? = state[guildId]
}
