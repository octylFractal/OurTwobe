package net.octyl.ourtwobe

@Export
interface UserProfile {
    val username: String
    val avatarUrl: String
    val servers: Array<Server>
}

@Export
interface Server {
    val id: String
    val name: String
    val iconUrl: String?
}
