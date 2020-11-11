package net.octyl.ourtwobe.youtube.audio

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging

private val BASE_COMMAND = listOf("youtube-dl", "-q", "-o", "-", "-f", "bestaudio,best")

class YouTubeDlProcessBinding(id: String) : AutoCloseable {
    private val logger = KotlinLogging.logger { }
    private val errorJob: Job
    val process =
        ProcessBuilder(BASE_COMMAND + "https://youtu.be/$id")
            .start()!!
            .also { process ->
                // don't need to listen to that
                process.outputStream.close()

                errorJob = GlobalScope.launch(CoroutineName("YouTubeDl") + Dispatchers.Default) {
                    try {
                        val text = withContext(Dispatchers.IO) {
                            process.errorStream.bufferedReader().use { it.readText() }
                        }
                        val exitCode = process.onExit().await().exitValue()
                        if (exitCode != 0 && "Broken pipe" !in text) {
                            logger.warn("Error from ytdl: $text")
                        }
                    } finally {
                        // kill the entire download process when finished
                        cancel()
                    }
                }
            }

    override fun close() {
        try {
            process.destroy()
        } finally {
            runBlocking {
                // wait for the error job to finish from process kill
                withTimeoutOrNull(10L) {
                    errorJob.join()
                }
                // or force it, if it's not moving quickly
                errorJob.cancelAndJoin()
            }
        }
    }
}
