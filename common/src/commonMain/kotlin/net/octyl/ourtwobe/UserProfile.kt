package net.octyl.ourtwobe

@Export
interface UserProfile {
    val username: String
    val avatar: String
    val servers: Array<Server>
}

@Export
interface Server {
    val id: String
    val name: String
}
