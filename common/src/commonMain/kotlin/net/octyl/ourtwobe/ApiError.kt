package net.octyl.ourtwobe

@Export
data class ApiError(
    /**
     * The error code.
     */
    val error: String,
    /**
     * The human-readable message.
     */
    val message: String
)
