package net.octyl.ourtwobe.api

import io.ktor.http.HttpStatusCode
import java.util.Locale

class ObjectNotFoundException(val type: String, val id: String) : ApiErrorException(
    ApiError("${type.toLowerCase(Locale.ROOT)}.not.found", "$type $id not found"), HttpStatusCode.NotFound
)
