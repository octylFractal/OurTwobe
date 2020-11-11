package net.octyl.ourtwobe.discord

import net.octyl.ourtwobe.api.Authorization

class DiscordIdAuthorization(
    private val owner: String,
) : Authorization {
    override fun isAdmin(user: String) = owner == user
}
