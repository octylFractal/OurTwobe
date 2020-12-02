package net.octyl.ourtwobe.api.discorddata

import net.dv8tion.jda.api.entities.VoiceChannel

data class ChannelData(
    val id: String,
    val name: String,
)

fun VoiceChannel.toChannelData(): ChannelData = ChannelData(
    id, name,
)
