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

package net.octyl.ourtwobe.api

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UnauthorizedResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.http.content.HttpStatusCodeContent
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.cio.ChannelWriteException
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import mu.KotlinLogging
import net.octyl.ourtwobe.InternalPeeker
import net.octyl.ourtwobe.MODULES
import net.octyl.ourtwobe.datapipe.DataPipe
import net.octyl.ourtwobe.datapipe.GuildManager
import net.octyl.ourtwobe.datapipe.GuildSettings
import net.octyl.ourtwobe.datapipe.GuildState
import net.octyl.ourtwobe.discord.DiscordApi
import net.octyl.ourtwobe.discord.PlayerCommand
import net.octyl.ourtwobe.files.FileItemResolver
import net.octyl.ourtwobe.youtube.YouTubeItemResolver
import org.slf4j.event.Level
import java.nio.ByteBuffer
import java.time.Duration

private val logger = KotlinLogging.logger { }

// Limit uploads to 1GB, all audio, even ridiculous ten hour mixes should be under this
private const val MAX_MULTIPART_SIZE = 1L * 1024L * 1024L * 1024L

fun Application.module(
    authorization: Authorization,
    internalPeeker: InternalPeeker,
    guildManager: GuildManager,
    youTubeItemResolver: YouTubeItemResolver,
    fileItemResolver: FileItemResolver,
) {
    val api = DiscordApi()

    install(Compression)
    install(AutoHeadResponse)

    install(ContentNegotiation) {
        jackson {
            registerModules(MODULES)
            disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
        }
        serverSentEvents()
    }

    install(Sessions) {
        cookie<Session>("session", storage = SimpleSessionStorage()) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = Duration.ofDays(1L).toSeconds()
        }
    }

    install(Authentication) {
        bearer("discord") {
            skipWhen { it.sessions.get<Session>() != null }
            realm = "Discord-based access"
            authenticate { credentials ->
                api.getMe(credentials.token)?.let {
                    sessions.set(Session(userId = it.id))

                    Authenticated
                }
            }
        }
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        status(HttpStatusCode.NotFound) { _ ->
            call.respond(ApiError("not.found", "${call.request.path()} was not found"))
        }
        status(HttpStatusCode.InternalServerError) { _ ->
            call.respond(ApiError("internal.server.error", "An error has occurred in OurTwobe."))
        }
        status(HttpStatusCode.Unauthorized) { _ ->
            if (content is UnauthorizedResponse) {
                call.respond(UnauthorizedResponse())
            }
        }
        exception<ApiErrorException> { call, cause ->
            call.respond(cause.statusCode, cause.error)
        }
        exception<JsonProcessingException> { call, cause ->
            if (call.request.path().endsWith("/data-pipe")) {
                logger.warn(cause) { "JSON formatting error in data pipe!" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiError("internal.server.error", "An error has occurred in OurTwobe.")
                )
                return@exception
            }
            logger.debug(cause) { "Caught JSON formatting error" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("invalid.json", "Your JSON is not valid")
            )
        }
        exception<Throwable> { call, cause ->
            if (cause is ChannelWriteException) {
                // Client disconnected, not a server issue
                logger.debug { "Client disconnected from ${call.request.path()}" }
                return@exception
            }
            logger.warn(cause) { "Caught exception in OurTwobe API handler" }
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("internal.server.error", "An error has occurred in OurTwobe.")
            )
        }
    }

    fun extractUserId(call: ApplicationCall) = call.requireSession().userId

    fun guildNotFoundError(guildId: String): Nothing = throw ObjectNotFoundException("Guild", guildId)
    fun userNotFoundError(userId: String): Nothing = throw ObjectNotFoundException("User", userId)

    fun ApplicationCall.getGuildState(): Pair<String, GuildState> {
        val userId = extractUserId(this)
        val guildId = parameters["guildId"]!!
        return guildId to (guildManager.getState(guildId)?.takeIf { guildManager.canSee(guildId, userId) }
            ?: guildNotFoundError(guildId))
    }

    routing {
        authenticate("discord") {
            route("/authenticate") {
                // Currently these two calls are the exact same
                // In the future we might require cookie-only for /check
                get {
                    call.respond(HttpStatusCode.NoContent, EmptyContent)
                }
                get("/check") {
                    call.respond(HttpStatusCode.NoContent, EmptyContent)
                }
            }
            get("/guilds/{guildId}/data-pipe") {
                val (_, state) = call.getGuildState()

                call.response.header(HttpHeaders.CacheControl, "no-cache")

                val pipe = DataPipe()

                coroutineScope {
                    launch {
                        // let the state manager take over pumping events into it
                        // this method will block until connection close
                        state.pumpEventsToPipe(pipe)
                    }
                    val flow = EventFlow(pipe.consumeMessages())
                    flow.invokeOnClose {
                        pipe.close()
                    }
                    call.respond(HttpStatusCode.OK, flow)
                }
            }

            // ADMIN ONLY routes
            requireAdmin(authorization, ::extractUserId) {
                route("/internal") {
                    get("/guilds") {
                        call.respond(internalPeeker.getGuilds())
                    }
                }
            }

            route("/discord") {
                route("/guilds") {
                    get {
                        val userId = extractUserId(call)
                        call.respond(guildManager.getGuildDatas(userId))
                    }
                    route("/{guildId}") {
                        get {
                            val userId = extractUserId(call)
                            val guildId = call.parameters["guildId"]!!
                            call.respond(guildManager.getGuildData(guildId, userId) ?: guildNotFoundError(guildId))
                        }
                        get("/channels") {
                            val userId = extractUserId(call)
                            val guildId = call.parameters["guildId"]!!
                            call.respond(guildManager.getChannelDatas(guildId, userId) ?: guildNotFoundError(guildId))
                        }
                    }
                }
                get("/users/{userId}") {
                    val viewerId = extractUserId(call)
                    val userId = call.parameters["userId"]!!
                    call.respond(guildManager.getUserData(viewerId, userId) ?: userNotFoundError(userId))
                }
            }

            route("/guilds/{guildId}") {
                put {
                    val userId = extractUserId(call)
                    val body = call.receive<GuildUpdate>()
                    val (guildId, state) = call.getGuildState()
                    body.activeChannel?.value?.let {
                        if (!guildManager.canSeeChannel(guildId, userId, it)) {
                            throw ObjectNotFoundException(
                                "Channel", "$it in $guildId"
                            )
                        }
                    }
                    state.guildSettingsHolder.updateSettings { settings ->
                        var (volume, activeChannel) = settings
                        body.volume?.let {
                            volume = it
                        }
                        body.activeChannel?.let {
                            activeChannel = it.value
                        }
                        GuildSettings(volume, activeChannel)
                    }
                    call.respond(HttpStatusCodeContent(HttpStatusCode.NoContent))
                }
                post("/skip") {
                    val body = call.receive<ItemSkip>()
                    val (_, state) = call.getGuildState()

                    state.queuePlayer.sendCommand(PlayerCommand.Skip(body.itemId))

                    call.respond(HttpStatusCodeContent(HttpStatusCode.NoContent))
                }
                route("/queue") {
                    post("/youtube") {
                        val userId = extractUserId(call)
                        val body = call.receive<QueueSubmit>()
                        val (_, state) = call.getGuildState()

                        youTubeItemResolver.resolveItems(body.url).collect {
                            state.queueManager.insert(userId, it)
                        }
                        call.respond(HttpStatusCodeContent(HttpStatusCode.NoContent))
                    }
                    post("/file") {
                        val userId = extractUserId(call)
                        val (_, state) = call.getGuildState()
                        call.receiveMultipart(MAX_MULTIPART_SIZE).forEachPart {
                            val (filename, fileContent) = try {
                                if (it !is PartData.FileItem) {
                                    throw ApiErrorException(
                                        ApiError("upload.invalid", "Only file parts are supported"),
                                        HttpStatusCode.BadRequest
                                    )
                                }
                                val filename = it.originalFileName ?: throw ApiErrorException(
                                    ApiError("upload.noname", "Uploaded file must have a name"),
                                    HttpStatusCode.BadRequest
                                )
                                filename to ByteBuffer.wrap(it.provider().readBuffer().readByteArray())
                            } finally {
                                it.dispose()
                            }
                            val item = fileItemResolver.resolveItem(filename, fileContent)
                            state.queueManager.insert(userId, item)
                        }
                        call.respond(HttpStatusCodeContent(HttpStatusCode.NoContent))
                    }
                    delete {
                        val userId = extractUserId(call)
                        val body = call.receive<ItemRemove>()
                        val (_, state) = call.getGuildState()

                        state.queueManager.removeById(userId, authorization, body.itemId)

                        call.respond(HttpStatusCodeContent(HttpStatusCode.NoContent))
                    }
                }
            }
        }
    }
}

object Authenticated

private fun ApplicationCall.requireSession(): Session = sessions.get() ?: error("No session available!")
