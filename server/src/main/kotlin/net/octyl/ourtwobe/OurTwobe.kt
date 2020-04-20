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

package net.octyl.ourtwobe

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.util.Snowflake
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.BadRequestResponse
import io.javalin.http.UnauthorizedResponse
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import io.swagger.v3.oas.models.info.Info
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.http.HttpStatus
import java.nio.file.Files
import java.nio.file.Path

fun main() {
    val discordToken = Files.readString(Path.of("./secrets/discord-token.txt")).trim()
    val bot = DiscordClientBuilder.create(discordToken).build()
    bot.login()
    val scope = CoroutineScope(Dispatchers.Default + CoroutineName("OurTwobeMain"))
    val firebase = initFirebase()
    val firestore: Firestore = FirestoreClient.getFirestore(firebase)
    val app = Javalin.create {
        it.showJavalinBanner = false
        it.registerPlugin(OpenApiPlugin(
            OpenApiOptions(
                Info()
                    .title("OurTwobe")
                    .version("1.0.0")
                    .description("The second coming of OurTube")
            )
                .ignorePath("/.meta/*")
                .path("/.meta/docs/api")
        ))
    }
    JavalinJackson.configure(JACKSON)
    val client = OkHttpClient.Builder().build()
    app.routes {
        addReDocRoute()
        post("/login/discord", documented(
            document()
                .operation {
                    it.summary("Login via Discord")
                    it.description("Exchanges a Discord OAuth token for a Firebase custom token")
                }
                .body<TokenHolder> {
                    it.description("A Discord OAuth token")
                }
                .result<TokenHolder>("200") {
                    it.description("A Firebase custom token")
                }
                .result<ApiError>("400") {
                    it.description("Bad request.")
                }
        ) { ctx ->
            val tokenHolder = try {
                ctx.req.inputStream.use { JACKSON.readValue<TokenHolderImpl>(it) }
            } catch (e: JsonProcessingException) {
                throw BadRequestResponse("Failed to deserialize token")
            }
            val response = client.newCall(Request.Builder()
                    .get().url("https://discordapp.com/api/v6/users/@me")
                    .header(HttpHeader.AUTHORIZATION.toString(), "Bearer " + tokenHolder.token)
                    .build())
                .execute()
            if (!response.isSuccessful) {
                if (response.code == 401) {
                    throw UnauthorizedResponse()
                }
                throw BadRequestResponse(response.body?.string() ?: "Unknown error with Discord")
            }
            val body = response.body ?: error("No body given")
            val discordUser = body.charStream().use {
                JACKSON.readValue<DiscordUser>(it)
            }
            scope.launch {
                saveProfileToDatabase(bot, firestore, discordUser)
            }
            ctx.status(HttpStatus.OK_200)
            ctx.json(TokenHolderImpl(FirebaseAuth.getInstance(firebase).createCustomToken(
                discordUser.id
            )))
        })
    }
    app.exception(BadRequestResponse::class.java) { err, ctx ->
        ctx.json(ApiError("bad.request", err.message ?: "Unknown error"))
    }
    app.exception(UnauthorizedResponse::class.java) { err, ctx ->
        ctx.json(ApiError("not.authorized", err.message ?: "Unknown error"))
    }
    app.start(13445)
}

suspend fun saveProfileToDatabase(bot: DiscordClient, firestore: Firestore, discordUser: DiscordUser) {
    val discordUserSnowflake = Snowflake.of(discordUser.id)
    val guilds = bot.guilds
        .filterWhen { guild ->
            guild.members.any { it.id == discordUserSnowflake }
        }
        .map { ServerImpl(it.id.asString(), it.name) }
        .collectList()
        .awaitSingle()
        .toTypedArray<Server>()
    firestore.collection("profiles").document(discordUser.id).set(
        UserProfileImpl(
            discordUser.username,
            discordUser.avatar,
            guilds
        ).convertToMapViaJackson()
    ).await()
}

class UserProfileImpl(
    override val username: String,
    override val avatar: String,
    override val servers: Array<Server>
) : UserProfile

class ServerImpl(
    override val id: String,
    override val name: String
) : Server

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiscordUser(
    val id: String,
    val username: String,
    val avatar: String,
)
