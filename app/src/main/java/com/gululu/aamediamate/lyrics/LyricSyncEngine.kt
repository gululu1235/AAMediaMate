package com.gululu.aamediamate.lyrics

import kotlinx.coroutines.*

object LyricSyncEngine {
    private var currentJob: Job? = null

    fun start(lyrics: List<LyricLine>, startPositionMs: Long, onLineChanged: (String) -> Unit) {
        currentJob?.cancel()

        currentJob = CoroutineScope(Dispatchers.Default).launch {
            val startTime = System.currentTimeMillis() - startPositionMs

            for (line in lyrics) {
                val delayMs = (line.timeSec * 1000 - (System.currentTimeMillis() - startTime))
                if (delayMs > 0) delay(delayMs.toLong())
                onLineChanged(line.text)
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
    }
}