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
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.basic
import io.ktor.auth.principal
import io.ktor.features.CallLogging
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
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.octyl.ourtwobe.ApiError
import net.octyl.ourtwobe.GuildUpdate
import net.octyl.ourtwobe.InternalPeeker
import net.octyl.ourtwobe.MODULES
import net.octyl.ourtwobe.QueueSubmit
import net.octyl.ourtwobe.datapipe.DataPipe
import net.octyl.ourtwobe.datapipe.DataPipeEvent
import net.octyl.ourtwobe.datapipe.GuildManager
import net.octyl.ourtwobe.datapipe.GuildSettings
import net.octyl.ourtwobe.datapipe.GuildState
import net.octyl.ourtwobe.discord.DiscordApi
import net.octyl.ourtwobe.discord.DiscordUser
import net.octyl.ourtwobe.util.TokenGenerator
import net.octyl.ourtwobe.youtube.YouTubeItemResolver
import org.slf4j.event.Level
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

fun Application.module(
    authorization: Authorization,
    internalPeeker: InternalPeeker,
    guildManager: GuildManager,
    youTubeItemResolver: YouTubeItemResolver
) {
    val tokenGen = TokenGenerator()
    val dataPipes = ConcurrentHashMap<String, DataPipe>()
    val api = DiscordApi()

    install(ContentNegotiation) {
        jackson {
            registerModules(MODULES)
            disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
        }
        serverSentEvents()
    }

    install(Authentication) {
        basic("discord") {
            realm = "Discord-based access"
            validate { credentials ->
                if (credentials.name != "discord") {
                    return@validate null
                }
                api.getMe(credentials.password)?.let { DiscordUserPrincipal(it) }
            }
        }
        basic("communication") {
            realm = "Communication-based access"
            validate { credentials ->
                val pipe = dataPipes[credentials.password]
                if (credentials.name == "communication" && pipe != null) {
                    DataPipePrincipal(pipe)
                } else {
                    null
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

    fun extractUserId(call: ApplicationCall) = when (val principal = call.authentication.principal) {
        is DiscordUserPrincipal -> principal.user.id
        is DataPipePrincipal -> principal.pipe.user.id
        // authentication should be wrapping everything
        else -> error("This shouldn't be possible!")
    }

    fun ApplicationCall.getGuildState(): GuildState {
        val userId = extractUserId(this)
        val guildId = parameters["guildId"]!!
        return guildManager.getState(guildId)?.takeIf { guildManager.canSee(guildId, userId) }
            ?: throw ApiErrorException(
                ApiError("guild.not.found", "Guild $guildId not found"), HttpStatusCode.NotFound
            )
    }

    routing {
        authenticate("discord") {
            get("/guilds/{guildId}/data-pipe") {
                val (user) = call.requirePrincipal<DiscordUserPrincipal>()
                val state = call.getGuildState()

                call.response.header(HttpHeaders.CacheControl, "no-cache")

                val token = tokenGen.newToken()
                val pipe = DataPipe(user)
                dataPipes[token] = pipe
                pipe.invokeOnClose {
                    dataPipes.remove(token)
                }

                coroutineScope {
                    // this needs to be async so that if the event buffer fills up, the call can proceed to empty it
                    launch {
                        // send out initial events
                        pipe.sendData(DataPipeEvent.CommunicationToken(token))

                        // and let the state manager take over pumping events into it
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
                get("/guilds") {
                    call.respond(internalPeeker.getGuilds())
                }
            }
        }

        authenticate("communication") {
            route("/guilds/{guildId}") {
                put {
                    val body = call.receive<GuildUpdate>()
                    val state = call.getGuildState()
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
                post("/queue") {
                    val (pipe) = call.requirePrincipal<DataPipePrincipal>()
                    val body = call.receive<QueueSubmit>()
                    val state = call.getGuildState()

                    youTubeItemResolver.resolveItems(body.url).collect {
                        logger.info { "${pipe.user.id} queued ${it.title}" }
                        state.queueManager.insert(pipe.user.id, it)
                    }
                    call.respond(HttpStatusCodeContent(HttpStatusCode.NoContent))
                }
            }
        }
    }
}

data class DataPipePrincipal(
    val pipe: DataPipe,
) : Principal

data class DiscordUserPrincipal(
    val user: DiscordUser,
) : Principal

inline fun <reified P : Principal> ApplicationCall.requirePrincipal(): P =
    principal<P>() ?: error("Principal of type ${P::class.qualifiedName} not present")
