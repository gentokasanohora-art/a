package com.emeraldrng.rng

/**
 * Emerald RNG 完全版コア
 * - Method1/2/4
 * - 高速PID逆算（Lattice風定数使用）
 * - IV逆算
 * - 検索・色違い・めざパ・時間変換 すべて入り
 */
object EmeraldRNG {

    private const val MULT = 0x41C64E6DL
    private const val INCR = 0x6073L
    private const val INVERSE_MULT = 0xEEB9EB65L
    private const val MASK = 0xFFFFFFFFL

    // Lattice風高速逆算用定数
    private const val PID_LAG0 = 0x7ED7L
    private const val PID_LAG1 = 0xD33L
    private const val PID_LOWER = 0x50F5A0BL
    private const val PID_UPPER = 0x50F40B4L

    private val NATURES = arrayOf(
        "がんばりや", "さみしがり", "ゆうかん", "いじっぱり", "やんちゃ",
        "ずぶとい", "すなお", "のんき", "わんぱく", "のうてんき",
        "おくびょう", "せっかち", "まじめ", "ようき", "むじゃき",
        "ひかえめ", "おっとり", "れいせい", "てれや", "うっかりや",
        "おだやか", "おとなしい", "なまいき", "しんちょう", "きまぐれ"
    )

    private val HP_TYPES = arrayOf(
        "かくとう", "ひこう", "どく", "じめん", "いわ", "むし", "ゴースト", "はがね",
        "ほのお", "みず", "くさ", "でんき", "エスパー", "こおり", "ドラゴン", "あく"
    )

    data class Spread(
        val frame: Long,
        val seed: Long,
        val pid: Long,
        val nature: Int,
        val natureName: String,
        val ability: Int,
        val genderValue: Int,
        val ivs: IntArray,
        val hpType: String,
        val hpPower: Int,
        val isShiny: Boolean,
        val shinyType: String,
        val method: String
    ) {
        fun ivString() = ivs.joinToString("/")
    }

    fun next(seed: Long): Long = (seed * MULT + INCR) and MASK
    fun prev(seed: Long): Long = ((seed - INCR) * INVERSE_MULT) and MASK

    fun advance(seed: Long, frames: Long): Long {
        var s = seed and MASK
        var f = frames
        while (f > 0) { s = next(s); f-- }
        return s
    }

    fun rewind(seed: Long, frames: Long): Long {
        var s = seed and MASK
        var f = frames
        while (f > 0) { s = prev(s); f-- }
        return s
    }

    fun generate(
        initialSeed: Long = 0L,
        frame: Long = 0L,
        method: String = "Method1",
        tid: Int = 0,
        sid: Int = 0
    ): Spread {
        var seed = advance(initialSeed, frame)
        val pidLow = (next(seed) ushr 16).toInt()
        seed = next(seed)
        val pidHigh = (next(seed) ushr 16).toInt()
        seed = next(seed)
        val pid = (pidHigh.toLong() shl 16) or pidLow.toLong()

        when (method) { "Method2" -> seed = next(seed) }

        val iv1: Int
        val iv2: Int
        if (method == "Method4") {
            iv1 = (next(seed) ushr 16).toInt()
            seed = next(seed)
            seed = next(seed)
            iv2 = (next(seed) ushr 16).toInt()
        } else {
            iv1 = (next(seed) ushr 16).toInt()
            seed = next(seed)
            iv2 = (next(seed) ushr 16).toInt()
        }

        val ivs = extractIVs(iv1, iv2)
        val nature = (pid % 25).toInt()
        val (hpType, hpPower) = calcHiddenPower(ivs)
        val shinyType = getShinyType(pid, tid, sid)

        return Spread(
            frame = frame,
            seed = advance(initialSeed, frame),
            pid = pid,
            nature = nature,
            natureName = NATURES.getOrElse(nature) { "不明" },
            ability = (pid and 1).toInt(),
            genderValue = (pid and 0xFF).toInt(),
            ivs = ivs,
            hpType = hpType,
            hpPower = hpPower,
            isShiny = shinyType != "なし",
            shinyType = shinyType,
            method = method
        )
    }

    fun getShinyType(pid: Long, tid: Int, sid: Int): String {
        val value = tid xor sid xor ((pid ushr 16).toInt()) xor (pid and 0xFFFF).toInt()
        return when {
            value == 0 -> "四角"
            value < 8 -> "星"
            else -> "なし"
        }
    }

    fun calcHiddenPower(ivs: IntArray): Pair<String, Int> {
        fun bit(iv: Int, n: Int) = (iv shr n) and 1
        val type = (bit(ivs[0], 0) + 2 * bit(ivs[1], 0) + 4 * bit(ivs[2], 0) +
                8 * bit(ivs[5], 0) + 16 * bit(ivs[3], 0) + 32 * bit(ivs[4], 0)) * 15 / 63
        val power = 30 + (bit(ivs[0], 1) + 2 * bit(ivs[1], 1) + 4 * bit(ivs[2], 1) +
                8 * bit(ivs[5], 1) + 16 * bit(ivs[3], 1) + 32 * bit(ivs[4], 1)) * 40 / 63
        return HP_TYPES.getOrElse(type) { "不明" } to power
    }

    /** 高速PID逆算（Lattice風） */
    fun recoverFromPIDFast(pid: Long): List<Long> {
        val results = mutableListOf<Long>()
        val first = (pid and 0xFFFFL) shl 16
        val second = pid and 0xFFFF0000L

        val tmp = (((second * INVERSE_MULT - first) ushr 16) and 0xFFFFL) * PID_LAG0
        val lo = (tmp + PID_LOWER) ushr 16
        val up = (tmp + PID_UPPER) ushr 16
        if (lo != up) return emptyList()

        var lbits = (lo * PID_LAG1) % PID_LAG0
        while (lbits < 0x10000L) {
            val seed = second or lbits
            if ((next(seed) and 0xFFFF0000L) == first) {
                results.add(prev(seed))
            }
            lbits += PID_LAG0
        }
        return results.distinct()
    }

    /** 従来の確実なPID逆算 */
    fun recoverFromPID(pid: Long): List<Long> {
        val results = mutableListOf<Long>()
        val first = (pid and 0xFFFFL) shl 16
        val second = pid and 0xFFFF0000L
        for (low in 0 until 0x10000) {
            val seed = second or low.toLong()
            if ((next(seed) and 0xFFFF0000L) == first) {
                results.add(prev(seed))
            }
        }
        return results.distinct()
    }

    fun recoverFromIVs(ivs: IntArray, method: String = "Method1"): List<Long> {
        val word1 = (ivs[0] and 0x1F) or ((ivs[1] and 0x1F) shl 5) or ((ivs[2] and 0x1F) shl 10)
        val word2 = (ivs[5] and 0x1F) or ((ivs[3] and 0x1F) shl 5) or ((ivs[4] and 0x1F) shl 10)
        val gap = if (method == "Method1") 0 else 1
        val results = mutableListOf<Long>()
        for (low in 0 until 0x10000) {
            var seed = (word1.toLong() shl 16) or low.toLong()
            repeat(gap) { seed = next(seed) }
            if (((next(seed) ushr 16) and 0xFFFF).toInt() == word2) {
                results.add((word1.toLong() shl 16) or low.toLong())
            }
        }
        return results.distinct()
    }

    fun findFrame(targetSeed: Long, initialSeed: Long = 0L, max: Long = 20_000_000L): Long? {
        var current = initialSeed and MASK
        for (frame in 0..max) {
            if (current == targetSeed and MASK) return frame
            current = next(current)
        }
        return null
    }

    fun framesToSeconds(frames: Long, fps: Double = 59.7275) = frames / fps
    fun secondsToFrames(seconds: Double, fps: Double = 59.7275) = (seconds * fps).toLong()
    fun formatTime(frames: Long): String {
        val sec = framesToSeconds(frames)
        return "%d分 %05.2f秒".format((sec / 60).toInt(), sec % 60)
    }

    fun search(
        initialSeed: Long = 0L,
        startFrame: Long = 0L,
        maxFrames: Long = 500_000L,
        method: String = "Method1",
        tid: Int = 0,
        sid: Int = 0,
        nature: Int? = null,
        minIVs: IntArray? = null,
        maxIVs: IntArray? = null,
        onlyShiny: Boolean = false,
        hpType: String? = null,
        maxResults: Int = 40
    ): List<Spread> {
        val list = mutableListOf<Spread>()
        for (frame in startFrame until startFrame + maxFrames) {
            val sp = generate(initialSeed, frame, method, tid, sid)
            if (nature != null && sp.nature != nature) continue
            if (onlyShiny && !sp.isShiny) continue
            if (hpType != null && sp.hpType != hpType) continue
            if (minIVs != null) {
                var ok = true
                for (i in 0..5) if (minIVs[i] >= 0 && sp.ivs[i] < minIVs[i]) ok = false
                if (!ok) continue
            }
            if (maxIVs != null) {
                var ok = true
                for (i in 0..5) if (maxIVs[i] >= 0 && sp.ivs[i] > maxIVs[i]) ok = false
                if (!ok) continue
            }
            list.add(sp)
            if (list.size >= maxResults) break
        }
        return list
    }

    private fun extractIVs(iv1: Int, iv2: Int) = intArrayOf(
        iv1 and 0x1F, (iv1 ushr 5) and 0x1F, (iv1 ushr 10) and 0x1F,
        (iv2 ushr 5) and 0x1F, (iv2 ushr 10) and 0x1F, iv2 and 0x1F
    )

    fun natureName(n: Int) = NATURES.getOrElse(n) { "不明" }
}
