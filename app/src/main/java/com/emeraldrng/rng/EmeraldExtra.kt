package com.emeraldrng.rng

object EmeraldExtra {

    private const val MULT = 0x41C64E6DL
    private const val INCR = 0x6073L
    private const val MASK = 0xFFFFFFFFL

    fun next(seed: Long) = (seed * MULT + INCR) and MASK
    fun advance(seed: Long, count: Long): Long {
        var s = seed
        repeat(count.toInt().coerceAtLeast(0)) { s = next(s) }
        return s
    }

    val STATIC_OFFSETS = mapOf(
        "レジロック" to 102L, "レジアイス" to 102L, "レジスチル" to 78L,
        "レックウザ" to 95L, "グラードン" to 287L, "カイオーガ" to 283L,
        "ラティアス(南の島)" to 404L, "ラティオス(南の島)" to 431L, "ダンバル" to 4L
    )

    fun generateStatic(species: String, frame: Long, tid: Int = 0, sid: Int = 0): EmeraldRNG.Spread {
        val offset = STATIC_OFFSETS[species] ?: 0L
        return EmeraldRNG.generate(frame = frame + offset, method = "Method1", tid = tid, sid = sid)
    }

    fun generateGift(species: String, frame: Long, tid: Int = 0, sid: Int = 0): EmeraldRNG.Spread {
        val offset = when (species) {
            "キモリ", "アチャモ", "ミズゴロウ" -> 0L
            "ダンバル" -> 4L
            else -> 0L
        }
        return EmeraldRNG.generate(frame = frame + offset, method = "Method1", tid = tid, sid = sid)
    }

    data class IdPair(val tid: Int, val sid: Int, val frame: Long)
    fun searchTIDSID(targetTID: Int? = null, targetSID: Int? = null, maxFrames: Long = 500_000L): List<IdPair> {
        val results = mutableListOf<IdPair>()
        var seed = 0L
        for (frame in 0..maxFrames) {
            val tid = ((next(seed) ushr 16) and 0xFFFF).toInt()
            seed = next(seed)
            val sid = ((next(seed) ushr 16) and 0xFFFF).toInt()
            seed = next(seed)
            if ((targetTID == null || tid == targetTID) && (targetSID == null || sid == targetSID)) {
                results.add(IdPair(tid, sid, frame))
                if (results.size >= 20) break
            }
        }
        return results
    }

    fun calcFeebasTiles(seed: Long): List<Int> {
        val tiles = mutableListOf<Int>()
        var s = seed
        repeat(6) {
            s = next(s)
            tiles.add(((s ushr 16) % 447).toInt())
        }
        return tiles.distinct().sorted()
    }

    fun isMirageIslandActive(tid: Int, sid: Int, seed: Long): Boolean {
        return ((next(seed) ushr 16).toInt() and 0xFFFF) == (tid xor sid)
    }

    fun hasPokerus(seed: Long): Boolean = ((next(seed) ushr 16) and 0xFFFF) < 3

    fun filterByGenderRatio(spreads: List<EmeraldRNG.Spread>, ratio: String): List<EmeraldRNG.Spread> {
        return spreads.filter {
            val gv = it.genderValue
            when (ratio) {
                "12.5%" -> gv < 31
                "25%" -> gv < 63
                "50%" -> gv < 127
                "75%" -> gv < 191
                else -> true
            }
        }
    }

    fun searchHiddenPower(targetType: String, minPower: Int = 70, maxResults: Int = 20): List<EmeraldRNG.Spread> {
        return EmeraldRNG.search(maxFrames = 300_000L, maxResults = maxResults * 3)
            .filter { it.hpType == targetType && it.hpPower >= minPower }
            .take(maxResults)
    }

    fun generateRange(startFrame: Long, count: Int, method: String = "Method1", tid: Int = 0, sid: Int = 0) =
        (0 until count).map { EmeraldRNG.generate(frame = startFrame + it, method = method, tid = tid, sid = sid) }

    fun exportToText(spreads: List<EmeraldRNG.Spread>): String = buildString {
        appendLine("Frame,PID,Nature,IVs,HPType,HPPower,Shiny")
        spreads.forEach {
            appendLine("${it.frame},0x${it.pid.toString(16)},${it.natureName},${it.ivString()},${it.hpType},${it.hpPower},${it.shinyType}")
        }
    }
}
