package net.octyl.ourtwobe.api

import io.ktor.http.HttpStatusCode
import net.octyl.ourtwobe.ApiError

class ApiErrorException(
    val error: ApiError,
    val statusCode: HttpStatusCode,
) : RuntimeException("${error.error}: ${error.message} ($statusCode)")
