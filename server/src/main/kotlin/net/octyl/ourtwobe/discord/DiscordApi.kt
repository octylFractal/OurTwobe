/*
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
