package net.octyl.ourtwobe

data class GuildUpdate(
    val volume: Int?,
    val activeChannel: ApiOptional<String>?
)
