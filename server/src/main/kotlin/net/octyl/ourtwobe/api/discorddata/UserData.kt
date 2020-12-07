package net.octyl.ourtwobe.api.discorddata

import net.dv8tion.jda.api.entities.User

data class UserData(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String?,
)

fun User.toUserData(): UserData = UserData(
    id, name, discriminator, avatarId
)
