package net.octyl.ourtwobe

/**
 * An entry in a song queue.
 */
@Export
interface QueueEntry {
    val id: String
    val submissionTime: Int
    val name: String?
    val thumbnailUrl: String?

    /**
     * `[0, 1)` value determining how far along the current song has played.
     */
    val progress: Double
}
