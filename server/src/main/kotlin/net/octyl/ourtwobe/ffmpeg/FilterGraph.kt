/*
 * This file is part of beat-dropper, licensed under the MIT License (MIT).
 *
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.octyl.ourtwobe.ffmpeg

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.bytedeco.ffmpeg.avfilter.AVFilterContext
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avfilter.av_buffersink_get_frame
import org.bytedeco.ffmpeg.global.avfilter.av_buffersrc_add_frame_flags
import org.bytedeco.ffmpeg.global.avfilter.av_buffersrc_close
import org.bytedeco.ffmpeg.global.avfilter.avfilter_get_by_name
import org.bytedeco.ffmpeg.global.avfilter.avfilter_graph_alloc
import org.bytedeco.ffmpeg.global.avfilter.avfilter_graph_alloc_filter
import org.bytedeco.ffmpeg.global.avfilter.avfilter_graph_config
import org.bytedeco.ffmpeg.global.avfilter.avfilter_graph_free
import org.bytedeco.ffmpeg.global.avfilter.avfilter_init_dict
import org.bytedeco.ffmpeg.global.avfilter.avfilter_link
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EAGAIN
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF
import org.bytedeco.ffmpeg.global.avutil.AV_OPT_SEARCH_CHILDREN
import org.bytedeco.ffmpeg.global.avutil.av_frame_alloc
import org.bytedeco.ffmpeg.global.avutil.av_frame_clone
import org.bytedeco.ffmpeg.global.avutil.av_frame_free
import org.bytedeco.ffmpeg.global.avutil.av_frame_unref
import org.bytedeco.ffmpeg.global.avutil.av_opt_set
import org.bytedeco.ffmpeg.global.avutil.av_opt_set_int
import org.bytedeco.ffmpeg.global.avutil.av_opt_set_q

abstract class FilterGraph(
    inputFormat: Format,
    outputFormat: Format,
    filters: Iterable<Filter>
) : AutoCloseable {

    private val closer = AutoCloser()

    private val graph: AVFilterGraph
    private val bufferCtx: AVFilterContext
    protected val bufferSinkCtx: AVFilterContext
    private val outputFrame: AVFrame

    init {
        graph = closer.register(
            avfilter_graph_alloc(),
            { avfilter_graph_free(it) }
        ) ?: error("Unable to allocate filter graph")

        val allFilters = sequence {
            yield(Filter("abuffer", "src") { ctx ->
                av_opt_set(ctx, "channel_layout", channelLayoutName(inputFormat.channelLayout), AV_OPT_SEARCH_CHILDREN)
                av_opt_set_int(ctx, "sample_fmt", inputFormat.sampleFormat.toLong(), AV_OPT_SEARCH_CHILDREN)
                av_opt_set_q(ctx, "time_base", inputFormat.timeBase, AV_OPT_SEARCH_CHILDREN)
                av_opt_set_int(ctx, "sample_rate", inputFormat.sampleRate.toLong(), AV_OPT_SEARCH_CHILDREN)
            })
            yieldAll(filters)
            yield(Filter("aformat", "format") { ctx ->
                av_opt_set(ctx, "channel_layouts", channelLayoutName(outputFormat.channelLayout), AV_OPT_SEARCH_CHILDREN)
                avOptSetList(ctx, "sample_fmts", intArrayOf(outputFormat.sampleFormat), AV_OPT_SEARCH_CHILDREN)
                avOptSetList(ctx, "sample_rates", intArrayOf(outputFormat.sampleRate), AV_OPT_SEARCH_CHILDREN)
                av_opt_set_q(ctx, "time_base", outputFormat.timeBase, AV_OPT_SEARCH_CHILDREN)
            })
            yield(Filter("abuffersink", "sink") { ctx ->
                avOptSetList(ctx, "channel_layouts", longArrayOf(outputFormat.channelLayout), AV_OPT_SEARCH_CHILDREN)
                avOptSetList(ctx, "sample_fmts", intArrayOf(outputFormat.sampleFormat), AV_OPT_SEARCH_CHILDREN)
                avOptSetList(ctx, "sample_rates", intArrayOf(outputFormat.sampleRate), AV_OPT_SEARCH_CHILDREN)
                av_opt_set_q(ctx, "time_base", outputFormat.timeBase, AV_OPT_SEARCH_CHILDREN)
            })
        }.toList()

        check(allFilters.size == allFilters.distinctBy { it.name }.size) {
            val duplicate = allFilters.groupingBy { it.name }
                .eachCount()
                .filterValues { it > 1 }
                .values
                .toSortedSet()
            "Duplicate name(s) detected in filters list: $duplicate"
        }

        val ctxMap = allFilters.associate { filter ->
            val avFilter = avfilter_get_by_name(filter.id) ?: error("Unable to find filter ${filter.id}")
            val ctx = avfilter_graph_alloc_filter(graph, avFilter, filter.name)
                ?: error("Unable to allocate filter context for ${filter.name} (${filter.id})")
            filter.configure(ctx)

            filter.name to ctx
        }

        bufferCtx = ctxMap["src"] ?: error("No src context")
        bufferSinkCtx = ctxMap["sink"] ?: error("No sink context")

        for ((name, ctx) in ctxMap) {
            checkAv(avfilter_init_dict(ctx, null as AVDictionary?)) {
                "Unable to initialize ${name}: $it"
            }
        }

        var lastPair: Map.Entry<String, AVFilterContext>? = null
        for (pair in ctxMap) {
            if (lastPair != null) {
                val (lastName, lastCtx) = lastPair
                val (name, ctx) = pair
                checkAv(avfilter_link(lastCtx, 0, ctx, 0)) {
                    "Unable to link $lastName and $name: $it"
                }
            }
            lastPair = pair
        }

        checkAv(avfilter_graph_config(graph, null)) {
            "Unable to configure filter graph: $it"
        }

        outputFrame = closer.register(av_frame_alloc()) { av_frame_free(it) }
            ?: error("Unable to allocate output frame")
    }

    /**
     * Push a single frame into the graph, and get the [0, N] frames it produces.
     */
    fun pushFrame(frame: AVFrame, flags: Int = 0): Flow<AVFrame> {
        val ourFrame = av_frame_clone(frame) ?: error("Unable to clone frame")
        checkAv(av_buffersrc_add_frame_flags(bufferCtx, ourFrame, flags)) {
            av_frame_unref(frame)
            "Unable to push frame into graph"
        }
        av_frame_free(ourFrame)

        return resultFrameSequence()
    }

    fun pushFinalFrame(pts: Long, flags: Int = 0): Flow<AVFrame> {
        checkAv(av_buffersrc_close(bufferCtx, pts, flags)) {
            "Unable to push final frame into graph"
        }

        return resultFrameSequence()
    }

    private fun resultFrameSequence(): Flow<AVFrame> {
        return flow {
            var error: Int
            while (true) {
                error = av_buffersink_get_frame(bufferSinkCtx, outputFrame)
                when {
                    error == AVERROR_EAGAIN() || error == AVERROR_EOF -> return@flow
                    error < 0 -> error("Error filtering frame: ${avErr2Str(error)}")
                }
                emit(outputFrame)
                av_frame_unref(outputFrame)
            }
        }
    }

    override fun close() {
        closer.close()
    }

}
