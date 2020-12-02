package net.octyl.ourtwobe.api.discorddata

import net.dv8tion.jda.api.entities.Guild

data class GuildData(
    val id: String,
    val name: String,
    val iconUrl: String?,
)

fun Guild.toGuildData(): GuildData = GuildData(
    id, name, iconUrl,
)
