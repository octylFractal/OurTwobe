package net.octyl.ourtwobe.youtube

import io.ktor.http.HttpStatusCode
import net.octyl.ourtwobe.api.ApiError
import net.octyl.ourtwobe.api.ApiErrorException

class InvalidSubmission(url: String) : ApiErrorException(
    ApiError("submission.url.invalid", "The submitted item ($url) is invalid"),
    HttpStatusCode.BadRequest,
)
