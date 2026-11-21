package com.emeraldrng.rng

object EmeraldAdvanced {

    private const val MULT = 0x41C64E6DL
    private const val INCR = 0x6073L
    private const val MASK = 0xFFFFFFFFL

    fun next(seed: Long) = (seed * MULT + INCR) and MASK
    fun advance(seed: Long, frames: Long): Long {
        var s = seed
        repeat(frames.toInt().coerceAtLeast(0)) { s = next(s) }
        return s
    }

    data class EggResult(
        val pid: Long, val nature: Int, val natureName: String,
        val ability: Int, val genderValue: Int,
        val isShiny: Boolean, val shinyType: String, val calibration: Int
    )

    fun generateEggPID(prngSeed: Long, timerValue: Int, calibration: Int = 19, tid: Int = 0, sid: Int = 0): EggResult {
        var seed = advance(prngSeed, calibration.toLong())
        val pidLow = (next(seed) ushr 16).toInt()
        val pidHigh = timerValue and 0xFFFF
        val pid = (pidHigh.toLong() shl 16) or pidLow.toLong()
        val nature = (pid % 25).toInt()
        val shinyType = EmeraldRNG.getShinyType(pid, tid, sid)
        return EggResult(pid, nature, EmeraldRNG.natureName(nature), (pid and 1).toInt(),
            (pid and 0xFF).toInt(), shinyType != "なし", shinyType, calibration)
    }

    fun findEggCalibration(prngSeed: Long, approximateFrame: Long, actualPid: Long, timerValue: Int, range: Int = 8): List<Int> {
        val found = mutableListOf<Int>()
        for (cal in (19 - range)..(19 + range)) {
            val result = generateEggPID(prngSeed, timerValue, cal)
            if (result.pid == actualPid) found.add(cal)
        }
        return found
    }

    fun calculatePaintingSeed(framesSinceBoot: Long) = advance(0L, framesSinceBoot)

    data class BattleVideoInfo(val initialSeed: Long, val advanceCount: Long)
    fun calcBattleVideoSetup(desiredSeed: Long, currentSeed: Long = 0L): BattleVideoInfo {
        var seed = currentSeed
        var advances = 0L
        while (seed != desiredSeed && advances < 10_000_000L) {
            seed = next(seed); advances++
        }
        return BattleVideoInfo(currentSeed, advances)
    }

    data class RoamerSpread(val frame: Long, val pid: Long, val ivs: IntArray, val natureName: String, val locationHint: String)
    fun generateRoamer(frame: Long, tid: Int = 0, sid: Int = 0): RoamerSpread {
        val base = EmeraldRNG.generate(frame = frame, method = "Method1", tid = tid, sid = sid)
        val loc = (advance(0L, frame + 3) ushr 16) and 0xFFFF
        val hint = when (loc % 5) {
            0L -> "水路付近"; 1L -> "草原"; 2L -> "山岳"; 3L -> "海岸"; else -> "その他"
        }
        return RoamerSpread(frame, base.pid, base.ivs, base.natureName, hint)
    }

    data class SweetScentResult(val estimatedAdvance: Int, val note: String)
    fun calcSweetScentAdvance(hasPressure: Boolean = false): SweetScentResult {
        return SweetScentResult(if (hasPressure) 2 else 1, "実際の消費は状況により±1〜3ずれることがあります")
    }

    data class ACESeedInfo(val targetSeed: Long, val targetSeedHex: String, val note: String)
    fun prepareACESeedWrite(targetSeed: Long) = ACESeedInfo(
        targetSeed, "0x%08X".format(targetSeed),
        "ACEでPRNGシードをこの値に書き換えた後、目標フレームまで待機"
    )
}
