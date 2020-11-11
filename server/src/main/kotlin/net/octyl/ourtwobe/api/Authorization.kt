package net.octyl.ourtwobe.api

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext
import io.ktor.util.KtorExperimentalAPI
import net.octyl.ourtwobe.ApiError

interface Authorization {
    fun isAdmin(user: String): Boolean
}

/**
 * Must be nested inside an [Route.authenticate] call.
 */
@OptIn(KtorExperimentalAPI::class)
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
                HttpStatusCode.Unauthorized
            )
        }
        proceed()
    }

    route.build()
    return route
}

private class AuthorizationRouteSelector(val authorizationName: String) : RouteSelector(RouteSelectorEvaluation.qualityConstant) {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Constant
    }

    override fun toString(): String = "(authorize \"$authorizationName\")"
}
