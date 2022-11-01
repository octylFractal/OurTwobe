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

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext

interface Authorization {
    fun isAdmin(user: String): Boolean
    fun canRemoveFrom(user: String, queueOwner: String): Boolean
}

/**
 * Must be nested inside an [Route.authenticate] call.
 */
fun Route.requireAdmin(
    authorization: Authorization,
    userIdExtractor: (call: ApplicationCall) -> String,
    build: Route.() -> Unit
): Route {
    val route = createChild(AuthorizationRouteSelector(authorization.javaClass.simpleName))

    route.intercept(ApplicationCallPipeline.Call) {
        if (!authorization.isAdmin(userIdExtractor(call))) {
            throw ApiErrorException(
                ApiError("user.not.admin", "You are not an admin of OurTwobe."),
                HttpStatusCode.Forbidden
            )
        }
        proceed()
    }

    route.build()
    return route
}

private class AuthorizationRouteSelector(val authorizationName: String) : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Constant
    }

    override fun toString(): String = "(authorize \"$authorizationName\")"
}
