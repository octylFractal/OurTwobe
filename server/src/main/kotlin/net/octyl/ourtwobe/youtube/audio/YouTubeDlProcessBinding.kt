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

package net.octyl.ourtwobe.youtube.audio

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
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
import net.octyl.ourtwobe.Config

private val BASE_COMMAND = listOf(
    "yt-dlp", "-q", "-o", "-", "-f", "bestaudio"
)

class YouTubeDlProcessBinding(config: Config.YouTube, id: String) : AutoCloseable {
    private val logger = KotlinLogging.logger { }
    private val errorJob: Job
    @OptIn(DelicateCoroutinesApi::class)
    val process =
        ProcessBuilder(BASE_COMMAND + ArrayList<String>().apply {
            config.cookies?.let {
                add("--cookies")
                add(it.toAbsolutePath().toString())
            }
            if (config.forceIpv4) {
                add("--force-ipv4")
            }
            add("https://youtu.be/$id")
        } )
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
