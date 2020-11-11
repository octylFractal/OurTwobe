package net.octyl.ourtwobe.discord

import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import net.octyl.ourtwobe.MODULES

private const val DISCORD_BASE_URL = "https://discord.com/api/v6"

class DiscordApi {
    private val client = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModules(MODULES)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
    }

    suspend fun getMe(token: String): DiscordUser? {
        val response: HttpResponse = client.get("$DISCORD_BASE_URL/users/@me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return response.takeIf { it.status.isSuccess() }?.receive()
    }
}

data class DiscordUser(
    val id: String,
    val avatar: String?,
)
