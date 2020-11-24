package net.octyl.ourtwobe

import net.octyl.ourtwobe.interop.Export

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
