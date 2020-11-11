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
