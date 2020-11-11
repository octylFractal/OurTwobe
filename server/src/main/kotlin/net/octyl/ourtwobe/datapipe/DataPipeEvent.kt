package net.octyl.ourtwobe.datapipe

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.CaseFormat

sealed class DataPipeEvent(
    eventType: String? = null,
) {
    @JsonIgnore
    val eventType = eventType ?: CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, javaClass.kotlin.simpleName!!)!!

    /**
     * Prepare the data object for serialization.
     */
    open fun toSerializedForm(): Any = this

    data class CommunicationToken(val token: String) : DataPipeEvent()

    data class GuildSettings(
        val volume: Int,
        val activeChannel: String?,
    ) : DataPipeEvent()

    data class QueueItem(
        val owner: String,
        val item: PlayableItem,
    ) : DataPipeEvent()

    data class ProgressItem(
        val item: PlayableItem,
        val progress: Double,
    ) : DataPipeEvent()
}
