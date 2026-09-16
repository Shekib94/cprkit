package com.cyberpunk.gmtool.data.net

/**
 * مولد QR Code — پیاده‌سازی مستقل، بدون کتابخانه‌ی خارجی.
 * حالت بایت، تصحیح خطای سطح M، نسخه‌ی خودکار (۱ تا ۲۰).
 *
 * عمداً هیچ dependency جدیدی به پروژه اضافه نشده است.
 */
object QrCode {

    /** خروجی: ماتریس مربعی از true (سیاه) و false (سفید). */
    fun encode(text: String): Array<BooleanArray> {
        val data = text.toByteArray(Charsets.UTF_8)
        val version = pickVersion(data.size) ?: throw IllegalArgumentException("متن برای QR خیلی بلند است")
        val size = 17 + version * 4

        val ecLevel = 1 // M
        val totalCodewords = capacityCodewords(version)
        val ecPerBlock = EC_PER_BLOCK[version][ecLevel]
        val numBlocks = NUM_BLOCKS[version][ecLevel]
        val totalEc = ecPerBlock * numBlocks
        val dataCodewords = totalCodewords - totalEc

        // ── بیت‌استریم ──
        val bits = BitBuffer()
        bits.append(0b0100, 4) // حالت بایت
        val lenBits = if (version < 10) 8 else 16
        bits.append(data.size, lenBits)
        for (b in data) bits.append(b.toInt() and 0xFF, 8)

        val capacityBits = dataCodewords * 8
        if (bits.size > capacityBits) throw IllegalArgumentException("متن برای QR خیلی بلند است")
        bits.append(0, minOf(4, capacityBits - bits.size))
        while (bits.size % 8 != 0) bits.append(0, 1)
        var pad = 0xEC
        while (bits.size < capacityBits) {
            bits.append(pad, 8)
            pad = if (pad == 0xEC) 0x11 else 0xEC
        }

        val allData = bits.toBytes()

        // ── تقسیم به بلوک و تولید کد تصحیح خطا ──
        val shortBlockLen = dataCodewords / numBlocks
        val numLongBlocks = dataCodewords % numBlocks
        val dataBlocks = ArrayList<IntArray>()
        val ecBlocks = ArrayList<IntArray>()
        var offset = 0
        for (i in 0 until numBlocks) {
            val len = shortBlockLen + if (i >= numBlocks - numLongBlocks) 1 else 0
            val block = IntArray(len) { allData[offset + it] }
            offset += len
            dataBlocks.add(block)
            ecBlocks.add(reedSolomon(block, ecPerBlock))
        }

        // ── درهم‌بافی ──
        val interleaved = ArrayList<Int>(totalCodewords)
        val maxData = dataBlocks.maxOf { it.size }
        for (i in 0 until maxData) {
            for (b in dataBlocks) if (i < b.size) interleaved.add(b[i])
        }
        for (i in 0 until ecPerBlock) {
            for (b in ecBlocks) interleaved.add(b[i])
        }

        // ── چیدن ماتریس ──
        val modules = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        placeFinder(modules, reserved, 0, 0, size)
        placeFinder(modules, reserved, size - 7, 0, size)
        placeFinder(modules, reserved, 0, size - 7, size)
        placeTiming(modules, reserved, size)
        placeAlignment(modules, reserved, version, size)
        reserveFormat(reserved, size)
        if (version >= 7) reserveVersion(reserved, size)

        // ماژول تیره‌ی ثابت
        modules[size - 8][8] = true
        reserved[size - 8][8] = true

        placeData(modules, reserved, interleaved, size)

        // ── انتخاب بهترین ماسک ──
        var bestMask = 0
        var bestScore = Int.MAX_VALUE
        var bestMatrix = modules
        for (mask in 0..7) {
            val m = modules.map { it.copyOf() }.toTypedArray()
            applyMask(m, reserved, mask, size)
            drawFormat(m, ecLevel, mask, size)
            if (version >= 7) drawVersion(m, version, size)
            val score = penalty(m, size)
            if (score < bestScore) { bestScore = score; bestMask = mask; bestMatrix = m }
        }
        return bestMatrix
    }

    // ─────────────────────── ساختارهای ثابت ───────────────────────

    private fun placeFinder(m: Array<BooleanArray>, r: Array<BooleanArray>, x: Int, y: Int, size: Int) {
        for (dy in -1..7) for (dx in -1..7) {
            val px = x + dx; val py = y + dy
            if (px !in 0 until size || py !in 0 until size) continue
            val on = (dx in 0..6 && (dy == 0 || dy == 6)) ||
                     (dy in 0..6 && (dx == 0 || dx == 6)) ||
                     (dx in 2..4 && dy in 2..4)
            m[py][px] = on
            r[py][px] = true
        }
    }

    private fun placeTiming(m: Array<BooleanArray>, r: Array<BooleanArray>, size: Int) {
        for (i in 8 until size - 8) {
            val on = i % 2 == 0
            if (!r[6][i]) { m[6][i] = on; r[6][i] = true }
            if (!r[i][6]) { m[i][6] = on; r[i][6] = true }
        }
    }

    private fun placeAlignment(m: Array<BooleanArray>, r: Array<BooleanArray>, version: Int, size: Int) {
        if (version < 2) return
        val pos = alignmentPositions(version)
        for (py in pos) for (px in pos) {
            if ((px == 6 && py == 6) || (px == 6 && py == size - 7) || (px == size - 7 && py == 6)) continue
            for (dy in -2..2) for (dx in -2..2) {
                val x = px + dx; val y = py + dy
                if (x !in 0 until size || y !in 0 until size) continue
                val on = dx == -2 || dx == 2 || dy == -2 || dy == 2 || (dx == 0 && dy == 0)
                m[y][x] = on
                r[y][x] = true
            }
        }
    }

    private fun alignmentPositions(version: Int): IntArray {
        if (version < 2) return IntArray(0)
        val n = version / 7 + 2
        val last = version * 4 + 10
        val first = 6
        if (n == 2) return intArrayOf(first, last)
        var step = (last - first) / (n - 1)
        if (step % 2 != 0) step++
        val res = IntArray(n)
        res[0] = first
        for (i in 1 until n) res[i] = last - (n - 1 - i) * step
        return res
    }

    private fun reserveFormat(r: Array<BooleanArray>, size: Int) {
        for (i in 0..8) {
            if (i != 6) { r[8][i] = true; r[i][8] = true }
        }
        for (i in 0..7) r[8][size - 1 - i] = true
        for (i in 0..6) r[size - 1 - i][8] = true
        r[8][8] = true
    }

    private fun reserveVersion(r: Array<BooleanArray>, size: Int) {
        for (i in 0..5) for (j in 0..2) {
            r[i][size - 11 + j] = true
            r[size - 11 + j][i] = true
        }
    }

    private fun placeData(m: Array<BooleanArray>, r: Array<BooleanArray>, cw: List<Int>, size: Int) {
        var bitIdx = 0
        val total = cw.size * 8
        var col = size - 1
        var upward = true
        while (col > 0) {
            if (col == 6) col--
            val range = if (upward) (size - 1) downTo 0 else 0 until size
            for (row in range) {
                for (c in 0..1) {
                    val x = col - c
                    if (r[row][x]) continue
                    val on = if (bitIdx < total) {
                        val b = cw[bitIdx / 8]
                        ((b shr (7 - bitIdx % 8)) and 1) == 1
                    } else false
                    m[row][x] = on
                    bitIdx++
                }
            }
            upward = !upward
            col -= 2
        }
    }

    private fun applyMask(m: Array<BooleanArray>, r: Array<BooleanArray>, mask: Int, size: Int) {
        for (y in 0 until size) for (x in 0 until size) {
            if (r[y][x]) continue
            val flip = when (mask) {
                0 -> (x + y) % 2 == 0
                1 -> y % 2 == 0
                2 -> x % 3 == 0
                3 -> (x + y) % 3 == 0
                4 -> (y / 2 + x / 3) % 2 == 0
                5 -> (x * y) % 2 + (x * y) % 3 == 0
                6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0
                else -> ((x + y) % 2 + (x * y) % 3) % 2 == 0
            }
            if (flip) m[y][x] = !m[y][x]
        }
    }

    private fun drawFormat(m: Array<BooleanArray>, ecLevel: Int, mask: Int, size: Int) {
        val ecBits = when (ecLevel) { 0 -> 1; 1 -> 0; 2 -> 3; else -> 2 }
        val data = (ecBits shl 3) or mask
        var rem = data
        for (i in 0 until 10) rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        val bits = ((data shl 10) or rem) xor 0x5412

        // ترتیب استاندارد: پرارزش‌ترین بیت اول قرار می‌گیرد (بیت ۱۴ → ۰).
        // نسخه‌ی اول این تابع بیت‌ها را برعکس می‌چید و QR اصلاً خوانده نمی‌شد.
        val copy1 = arrayOf(
            8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5, 8 to 7, 8 to 8,
            7 to 8, 5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8
        )
        val copy2 = Array(15) { i ->
            if (i < 7) (size - 1 - i) to 8 else 8 to (size - 8 + (i - 7))
        }
        for (i in 0 until 15) {
            val bit = ((bits shr (14 - i)) and 1) == 1
            val (y1, x1) = copy1[i]; m[y1][x1] = bit
            val (y2, x2) = copy2[i]; m[y2][x2] = bit
        }
        m[size - 8][8] = true
    }

    private fun drawVersion(m: Array<BooleanArray>, version: Int, size: Int) {
        var rem = version
        for (i in 0 until 12) rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25)
        val bits = (version shl 12) or rem
        for (i in 0 until 18) {
            val on = ((bits shr i) and 1) == 1
            val a = i / 3
            val b = i % 3
            m[a][size - 11 + b] = on
            m[size - 11 + b][a] = on
        }
    }

    private fun penalty(m: Array<BooleanArray>, size: Int): Int {
        var score = 0
        // قانون ۱ — پنج ماژول هم‌رنگ پشت سر هم
        for (y in 0 until size) {
            var run = 1
            for (x in 1 until size) {
                if (m[y][x] == m[y][x - 1]) run++ else { if (run >= 5) score += 3 + (run - 5); run = 1 }
            }
            if (run >= 5) score += 3 + (run - 5)
        }
        for (x in 0 until size) {
            var run = 1
            for (y in 1 until size) {
                if (m[y][x] == m[y - 1][x]) run++ else { if (run >= 5) score += 3 + (run - 5); run = 1 }
            }
            if (run >= 5) score += 3 + (run - 5)
        }
        // قانون ۲ — بلوک ۲×۲ هم‌رنگ
        for (y in 0 until size - 1) for (x in 0 until size - 1) {
            val v = m[y][x]
            if (v == m[y][x + 1] && v == m[y + 1][x] && v == m[y + 1][x + 1]) score += 3
        }
        // قانون ۴ — نسبت تیره به کل
        var dark = 0
        for (y in 0 until size) for (x in 0 until size) if (m[y][x]) dark++
        val percent = dark * 100 / (size * size)
        score += (Math.abs(percent - 50) / 5) * 10
        return score
    }

    // ─────────────────────── Reed–Solomon ───────────────────────

    private val EXP = IntArray(512)
    private val LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP[i] = x
            LOG[x] = i
            x = x shl 1
            if (x and 0x100 != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) EXP[i] = EXP[i - 255]
    }

    private fun mul(a: Int, b: Int): Int =
        if (a == 0 || b == 0) 0 else EXP[LOG[a] + LOG[b]]

    private fun reedSolomon(data: IntArray, ecLen: Int): IntArray {
        val gen = IntArray(ecLen + 1)
        gen[0] = 1
        for (i in 0 until ecLen) {
            for (j in i + 1 downTo 1) {
                gen[j] = gen[j - 1] xor mul(gen[j], EXP[i])
            }
            gen[0] = mul(gen[0], EXP[i])
        }
        val res = IntArray(ecLen)
        for (b in data) {
            val factor = b xor res[0]
            System.arraycopy(res, 1, res, 0, ecLen - 1)
            res[ecLen - 1] = 0
            for (i in 0 until ecLen) res[i] = res[i] xor mul(gen[ecLen - 1 - i], factor)
        }
        return res
    }

    // ─────────────────────── جدول ظرفیت ───────────────────────

    private fun pickVersion(byteLen: Int): Int? {
        for (v in 1..20) {
            val total = capacityCodewords(v)
            val ec = EC_PER_BLOCK[v][1] * NUM_BLOCKS[v][1]
            val dataCw = total - ec
            val lenBits = if (v < 10) 8 else 16
            val needBits = 4 + lenBits + byteLen * 8
            if (needBits <= dataCw * 8) return v
        }
        return null
    }

    private fun capacityCodewords(version: Int): Int {
        val size = 17 + version * 4
        var modules = size * size
        modules -= 3 * 64                       // سه الگوی یاب + جداکننده
        modules -= 2 * (size - 16)              // الگوهای زمان‌بندی
        val align = alignmentPositions(version).size
        if (version >= 2) {
            val alignCount = align * align - 3
            modules -= alignCount * 25
            modules += (align - 2) * 2 * 5      // هم‌پوشانی با زمان‌بندی
        }
        modules -= 31                            // اطلاعات قالب + ماژول تیره
        if (version >= 7) modules -= 36
        return modules / 8
    }

    /** [version][ecLevel] — ecLevel: 0=L 1=M 2=Q 3=H */
    private val EC_PER_BLOCK = arrayOf(
        intArrayOf(0, 0, 0, 0),
        intArrayOf(7, 10, 13, 17), intArrayOf(10, 16, 22, 28), intArrayOf(15, 26, 18, 22),
        intArrayOf(20, 18, 26, 16), intArrayOf(26, 24, 18, 22), intArrayOf(18, 16, 24, 28),
        intArrayOf(20, 18, 18, 26), intArrayOf(24, 22, 22, 26), intArrayOf(30, 22, 20, 24),
        intArrayOf(18, 26, 24, 28), intArrayOf(20, 30, 28, 24), intArrayOf(24, 22, 26, 28),
        intArrayOf(26, 22, 24, 22), intArrayOf(30, 24, 20, 24), intArrayOf(22, 24, 30, 24),
        intArrayOf(24, 28, 24, 30), intArrayOf(28, 28, 28, 28), intArrayOf(30, 26, 28, 28),
        intArrayOf(28, 26, 26, 26), intArrayOf(28, 26, 30, 28)
    )

    private val NUM_BLOCKS = arrayOf(
        intArrayOf(0, 0, 0, 0),
        intArrayOf(1, 1, 1, 1), intArrayOf(1, 1, 1, 1), intArrayOf(1, 1, 2, 2),
        intArrayOf(1, 2, 2, 4), intArrayOf(1, 2, 4, 4), intArrayOf(2, 4, 4, 4),
        intArrayOf(2, 4, 6, 5), intArrayOf(2, 4, 6, 6), intArrayOf(2, 5, 8, 8),
        intArrayOf(4, 5, 8, 8), intArrayOf(4, 5, 8, 11), intArrayOf(4, 8, 10, 11),
        intArrayOf(4, 9, 12, 16), intArrayOf(4, 9, 16, 16), intArrayOf(6, 10, 12, 18),
        intArrayOf(6, 10, 17, 16), intArrayOf(6, 11, 16, 19), intArrayOf(6, 13, 18, 21),
        intArrayOf(7, 14, 21, 25), intArrayOf(8, 16, 20, 25)
    )

    private class BitBuffer {
        private val bytes = ArrayList<Int>()
        var size = 0
            private set

        fun append(value: Int, len: Int) {
            for (i in len - 1 downTo 0) {
                val bit = (value shr i) and 1
                if (size % 8 == 0) bytes.add(0)
                if (bit == 1) bytes[size / 8] = bytes[size / 8] or (1 shl (7 - size % 8))
                size++
            }
        }

        fun toBytes(): IntArray = IntArray(bytes.size) { bytes[it] }
    }
}
