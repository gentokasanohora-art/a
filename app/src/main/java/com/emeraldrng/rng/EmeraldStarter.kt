package com.emeraldrng.rng

object EmeraldStarter {

    data class StarterResult(
        val frame: Long, val pid: Long, val nature: Int, val natureName: String,
        val ivs: IntArray, val genderValue: Int, val isShiny: Boolean, val shinyType: String,
        val poochyenaFrame: Long, val calibration: Int
    )

    fun searchWithPoochyena(
        startFrame: Long = 0L, maxFrames: Long = 300_000L,
        tid: Int = 0, sid: Int = 0, targetNature: Int? = null,
        minIVs: IntArray? = null, onlyShiny: Boolean = false,
        poochyenaOffset: Long = 140L, maxResults: Int = 30
    ): List<StarterResult> {
        val results = mutableListOf<StarterResult>()
        for (frame in startFrame until startFrame + maxFrames) {
            val s = EmeraldRNG.generate(frame = frame, method = "Method1", tid = tid, sid = sid)
            if (targetNature != null && s.nature != targetNature) continue
            if (onlyShiny && !s.isShiny) continue
            if (minIVs != null) {
                var ok = true
                for (i in 0..5) if (minIVs[i] >= 0 && s.ivs[i] < minIVs[i]) ok = false
                if (!ok) continue
            }
            results.add(StarterResult(frame, s.pid, s.nature, s.natureName, s.ivs, s.genderValue,
                s.isShiny, s.shinyType, frame - poochyenaOffset, 0))
            if (results.size >= maxResults) break
        }
        return results
    }

    fun findCalibration(
        approximateFrame: Long, actualNature: Int, actualIVs: IntArray? = null,
        tid: Int = 0, sid: Int = 0, searchRange: Int = 20
    ): List<Pair<Long, Int>> {
        val found = mutableListOf<Pair<Long, Int>>()
        for (offset in -searchRange..searchRange) {
            val testFrame = approximateFrame + offset
            val spread = EmeraldRNG.generate(frame = testFrame, method = "Method1", tid = tid, sid = sid)
            if (spread.nature != actualNature) continue
            if (actualIVs != null) {
                var match = true
                for (i in 0..5) if (actualIVs[i] >= 0 && spread.ivs[i] != actualIVs[i]) { match = false; break }
                if (!match) continue
            }
            found.add(testFrame to offset)
        }
        return found
    }

    fun suggestNextTarget(previousTarget: Long, actualHitFrame: Long) =
        previousTarget - (actualHitFrame - previousTarget)
}
