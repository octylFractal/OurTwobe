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
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.UnauthorizedResponse
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.client.utils.EmptyContent
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.HttpStatusCodeContent
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.octyl.ourtwobe.InternalPeeker
import net.octyl.ourtwobe.MODULES
import net.octyl.ourtwobe.datapipe.DataPipe
import net.octyl.ourtwobe.datapipe.GuildManager
import net.octyl.ourtwobe.datapipe.GuildSettings
import net.octyl.ourtwobe.datapipe.GuildState
import net.octyl.ourtwobe.discord.DiscordApi
import net.octyl.ourtwobe.discord.PlayerCommand
import net.octyl.ourtwobe.youtube.YouTubeItemResolver
import org.slf4j.event.Level
import java.time.Duration

private val logger = KotlinLogging.logger { }

fun Application.module(
    authorization: Authorization,
    internalPeeker: InternalPeeker,
    guildManager: GuildManager,
    youTubeItemResolver: YouTubeItemResolver
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
        basic("discord") {
            skipWhen { it.sessions.get<Session>() != null }
            realm = "Discord-based access"
            validate { credentials ->
                if (credentials.name != "discord") {
                    return@validate null
                }
                api.getMe(credentials.password)?.let {
                    sessions.set(Session(userId = it.id))

                    Authenticated
                }
            }
        }
    }

    install(CallLogging) {
        level = Level.DEBUG
    }

    install(StatusPages) {
        status(HttpStatusCode.NotFound) {
            call.respond(ApiError("not.found", "${call.request.path()} was not found"))
        }
        status(HttpStatusCode.InternalServerError) {
            call.respond(ApiError("internal.server.error", "An error has occurred in OurTwobe."))
        }
        status(HttpStatusCode.Unauthorized) {
            if (subject is UnauthorizedResponse) {
                call.respond(UnauthorizedResponse())
            }
        }
        exception<ApiErrorException> {
            call.respond(it.statusCode, it.error)
        }
        exception<JsonProcessingException> {
            logger.debug(it) { "Caught JSON formatting error" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("invalid.json", "Your JSON is not valid")
            )
        }
        exception<Throwable> {
            logger.warn(it) { "Caught exception in OurTwobe API handler" }
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
                    post {
                        val userId = extractUserId(call)
                        val body = call.receive<QueueSubmit>()
                        val (_, state) = call.getGuildState()

                        youTubeItemResolver.resolveItems(body.url).collect {
                            logger.info { "$userId queued ${it.title}" }
                            state.queueManager.insert(userId, it)
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

object Authenticated : Principal

private fun ApplicationCall.requireSession(): Session = sessions.get() ?: error("No session available!")
