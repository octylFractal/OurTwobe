package net.octyl.ourtwobe

import net.dv8tion.jda.api.JDA

class InternalPeeker(
    private val jda: JDA,
) {
    fun getGuilds(): Set<String> = jda.guildCache.mapTo(mutableSetOf()) { it.id }
}
