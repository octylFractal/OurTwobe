package net.octyl.ourtwobe

@Export
interface GuildData {
    val id: String
    val name: String
    val iconUrl: String?
    val volume: Int
    val activeChannel: String?
}
