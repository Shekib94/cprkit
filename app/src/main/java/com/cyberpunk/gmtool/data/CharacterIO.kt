package com.cyberpunk.gmtool.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * خروجی‌گرفتن / ورود کاراکتر و NPC به صورت فایل JSON، و چاپ شیت رسمی به PDF.
 */
object CharacterIO {

    private val gson = Gson()
    private val charListType = object : TypeToken<List<Character>>() {}.type

    // ---------------- JSON ----------------

    fun toJson(characters: List<Character>): String = gson.toJson(characters)

    fun singleToJson(character: Character): String = gson.toJson(listOf(character))

    /** خروجی لیست کاراکترها (پذیرنده‌ی هر فایل خروجی‌شده‌ی لیستی یا تک‌کاراکتره) */
    fun parseList(json: String): List<Character> {
        val trimmed = json.trim()
        return try {
            val parsed: List<Character> = if (trimmed.startsWith("[")) {
                gson.fromJson(trimmed, charListType) ?: emptyList()
            } else {
                listOfNotNull(gson.fromJson(trimmed, Character::class.java))
            }
            parsed.map { raw ->
                raw.copy(
                    addictions = raw.addictions ?: emptyList(),
                    // Gson فیلدهای غایب در JSON قدیمی را null می‌گذارد حتی اگر نوع
                    // غیرnullable باشد؛ uid خالی یعنی «بعداً در syncDerived ساخته می‌شود».
                    sheetUid = raw.sheetUid ?: "",
                    addictionRelapseUntil = raw.addictionRelapseUntil ?: emptyMap(),
                    addictionRelapseRisk = raw.addictionRelapseRisk ?: emptyList(),
                    activeDrugEffects = raw.activeDrugEffects ?: emptyMap(),
                    drugHumanityHeld = raw.drugHumanityHeld ?: emptyMap(),
                    combatEffects = raw.combatEffects ?: emptyMap(),
                    appliedTransactionIds = raw.appliedTransactionIds ?: emptyList(),
                    inventory = raw.inventory ?: emptyList(),
                    weapons = raw.weapons ?: emptyList(),
                    medtechPharmaceuticals = raw.medtechPharmaceuticals ?: emptyList(),
                    mediaCases = raw.mediaCases ?: emptyList()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** فایل را در حافظه‌ی اپ ذخیره می‌کند و مسیر فایل را برمی‌گرداند */
    fun writeExportFile(context: Context, fileBaseName: String, json: String): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val safe = fileBaseName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "character" }
        val file = File(dir, "$safe.cpr.json")
        file.writeText(json)
        return file
    }

    // ---------------- PDF (۳ صفحه شیت رسمی) ----------------

    fun printToPdf(context: Context, character: Character): File {
        val document = PdfDocument()
        val w = 595   // A4 @72dpi
        val h = 842

        var pageNumber = 1

        // ===== صفحه ۱: استت‌ها، مهارت‌ها، سلاح‌ها و زره =====
        var info = PdfDocument.PageInfo.Builder(w, h, pageNumber).create()
        var page = document.startPage(info)
        var canvas = page.canvas
        drawPage1(canvas, character, w, h)
        document.finishPage(page)

        // ===== صفحه ۲: تجهیزات، سایبرویر، لایف‌پث =====
        info = PdfDocument.PageInfo.Builder(w, h, ++pageNumber).create()
        page = document.startPage(info)
        canvas = page.canvas
        drawPage2(canvas, character, w, h)
        document.finishPage(page)

        // ===== صفحه ۳: اطلاعات NPC / نقش / منابع مبارزه =====
        info = PdfDocument.PageInfo.Builder(w, h, ++pageNumber).create()
        page = document.startPage(info)
        canvas = page.canvas
        drawPage3(canvas, character, w, h)
        document.finishPage(page)

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val safe = character.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "character" }
        val file = File(dir, "${safe}_Sheet.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    // ----- کمک‌کننده‌های ترسیم -----
    private val red = Color.rgb(0xC6, 0x28, 0x28)
    private val black = Color.BLACK
    private val gray = Color.rgb(0x55, 0x55, 0x55)

    private fun paint(
        size: Float = 10f, bold: Boolean = false,
        color: Int = black, align: Paint.Align = Paint.Align.LEFT
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        textAlign = align
        isFakeBoldText = bold
    }

    private fun line(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, c: Int = gray, width: Float = 1f) {
        val p = Paint().apply { color = c; strokeWidth = width }
        canvas.drawLine(x1, y1, x2, y2, p)
    }

    private fun rect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, c: Int = red, width: Float = 2f) {
        val p = Paint().apply { color = c; style = Paint.Style.STROKE; strokeWidth = width }
        canvas.drawRect(left, top, right, bottom, p)
    }

    private fun header(canvas: Canvas, c: Character, title: String, w: Int) {
        val titlePaint = paint(20f, true, black, Paint.Align.CENTER)
        canvas.drawText("${c.name}  —  ${c.role}", w / 2f, 45f, titlePaint)
        val sub = paint(11f, false, gray, Paint.Align.CENTER)
        canvas.drawText("$title   •   ${c.creationMethod}   •   Rank ${c.roleRank}",
            w / 2f, 65f, sub)
        line(canvas, 30f, 75f, w - 30f, 75f, red, 2f)
    }

    private fun drawPage1(canvas: Canvas, c: Character, w: Int, h: Int) {
        header(canvas, c, "STATS & COMBAT", w)
        val margin = 35f
        var y = 110f

        // --- استت‌ها (۱۰ خانه) ---
        val statLabels = listOf("INT", "REF", "DEX", "TECH", "COOL", "WILL", "LUCK", "MOVE", "BODY", "EMP")
        val statValues = listOf(c.stats.int, c.stats.ref, c.stats.dex, c.stats.tech, c.stats.cool,
            c.stats.will, c.stats.luck, c.stats.move, c.stats.body, c.stats.emp)
        val cellW = (w - margin * 2 - 9f * 4f) / 10f
        statLabels.forEachIndexed { i, label ->
            val x = margin + i * (cellW + 4f)
            rect(canvas, x, y, x + cellW, y + 52f, red, 1.5f)
            canvas.drawText(label, x + cellW / 2f, y + 18f, paint(9f, true, red, Paint.Align.CENTER))
            canvas.drawText("${statValues[i]}", x + cellW / 2f, y + 42f,
                paint(16f, true, black, Paint.Align.CENTER))
        }
        y += 70f

        // --- منابع ---
        val res = listOf(
            "HP" to "${c.hp}/${c.maxHp}",
            "Humanity" to "${c.currentHumanity}/${c.maxHumanity}",
            "Luck" to "${c.currentLuck}/${c.maxLuck}",
            "Armor Head/Body" to "${c.headArmorSp} / ${c.bodyArmorSp}",
            "Eurodollars" to "${c.eurodollars}eb"
        )
        res.forEachIndexed { i, (k, v) ->
            val col = i % 2
            val x = margin + col * (w - margin * 2) / 2f
            val yy = y + (i / 2) * 26f
            canvas.drawText("$k:  $v", x, yy, paint(11f, true, black))
        }
        y += (res.size / 2 + 1) * 26f + 10f

        // --- مهارت‌ها (دو ستون) ---
        val sectTitle = paint(13f, true, red)
        canvas.drawText("SKILLS", margin, y, sectTitle)
        line(canvas, margin, y + 6f, w - margin, y + 6f, red, 1.5f)
        y += 28f
        val sorted = c.skills.sortedBy { it.name }
        val half = (sorted.size + 1) / 2
        val rowH = 18f
        val colW = (w - margin * 2) / 2f
        sorted.forEachIndexed { i, s ->
            val col = if (i < half) 0 else 1
            val row = if (col == 0) i else i - half
            val x = margin + col * colW
            val yy = y + row * rowH
            canvas.drawText(s.name, x, yy, paint(10f, false, black))
            canvas.drawText("${s.level}", x + colW - 30f, yy, paint(10f, true, red))
        }
        y += half * rowH + 12f

        // --- سلاح‌ها و زره ---
        canvas.drawText("WEAPONS & ARMOR", margin, y, sectTitle)
        line(canvas, margin, y + 6f, w - margin, y + 6f, red, 1.5f)
        y += 26f
        // سرستون
        canvas.drawText("WEAPON", margin, y, paint(9f, true, gray))
        canvas.drawText("DMG", w - 170f, y, paint(9f, true, gray))
        canvas.drawText("ROF", w - 110f, y, paint(9f, true, gray))
        canvas.drawText("AMMO", w - 60f, y, paint(9f, true, gray))
        y += 16f
        val wpns = c.weapons.ifEmpty { listOf(Weapon(name = "—")) }
        wpns.take(14).forEach { wpn ->
            canvas.drawText(wpn.name, margin, y, paint(10f))
            canvas.drawText(wpn.damage.ifBlank { "-" }, w - 170f, y, paint(10f))
            canvas.drawText("${wpn.rof}", w - 110f, y, paint(10f))
            canvas.drawText(if (wpn.magazineSize > 0) "${wpn.currentAmmo}/${wpn.magazineSize}" else "-",
                w - 60f, y, paint(10f))
            y += 18f
        }
    }

    private fun drawPage2(canvas: Canvas, c: Character, w: Int, h: Int) {
        header(canvas, c, "GEAR, CYBERWARE & LIFEPATH", w)
        val margin = 35f
        var y = 100f
        val sectTitle = paint(13f, true, red)

        // --- تجهیزات بر اساس دسته ---
        val byCategory = c.inventory.groupBy { it.category }
        canvas.drawText("INVENTORY & CYBERWARE", margin, y, sectTitle)
        line(canvas, margin, y + 6f, w - margin, y + 6f, red, 1.5f)
        y += 26f
        val order = listOf("Weapon", "Armor", "Cyberware", "Ammo", "Clothing", "Vehicles", "Utilities")
        order.forEach { cat ->
            val items = byCategory[cat] ?: return@forEach
            canvas.drawText("• $cat", margin, y, paint(11f, true, black))
            y += 17f
            items.take(12).forEach { item ->
                val extra = item.sp?.let { "  (SP $it)" } ?: ""
                canvas.drawText("   - ${item.name}$extra", margin, y, paint(9.5f, false, black))
                y += 15.5f
            }
            y += 4f
        }

        y += 10f
        // --- لایف‌پث ---
        canvas.drawText("LIFEPATH", margin, y, sectTitle)
        line(canvas, margin, y + 6f, w - margin, y + 6f, red, 1.5f)
        y += 26f
        val lp = c.lifepath
        val lpFields = listOf(
            "Cultural Origins" to lp.culturalOrigins,
            "Personality" to lp.personality,
            "Clothing Style" to lp.clothingStyle,
            "Hairstyle" to lp.hairstyle,
            "Feeling About People" to lp.feelingsAboutPeople,
            "Most Valued Person" to lp.valuedPerson,
            "Most Valued Possession" to lp.valuedPossession,
            "Family Background" to lp.familyBackground,
            "Life Goals" to lp.lifeGoals,
            "Friends" to lp.friends,
            "Enemies" to lp.enemies,
            "Tragic Love Affairs" to lp.tragicLoveAffairs
        )
        lpFields.forEach { (k, v) ->
            if (v.isNotBlank()) {
                canvas.drawText("$k:", margin, y, paint(10f, true, black))
                val wrapped = wrapText(v.ifBlank { "-" }, w - margin - 150f)
                wrapped.take(2).forEach { line2 ->
                    y += 15f
                    canvas.drawText(line2, margin + 150f, y, paint(9.5f, false, gray))
                }
                y += 18f
            }
        }
        if (lp.roleLifepath.isNotBlank()) {
            canvas.drawText("Role Lifepath:", margin, y, paint(10f, true, red))
            y += 16f
            wrapText(lp.roleLifepath, w - margin * 2).take(6).forEach { line2 ->
                canvas.drawText(line2, margin, y, paint(9.5f, false, gray))
                y += 15f
            }
        }
    }

    private fun drawPage3(canvas: Canvas, c: Character, w: Int, h: Int) {
        header(canvas, c, "NOTES & COMBAT REFERENCE", w)
        val margin = 35f
        var y = 105f
        val sectTitle = paint(13f, true, red)

        if (!c.isAlly) {
            canvas.drawText("NPC FILE", margin, y, sectTitle)
            line(canvas, margin, y + 6f, w - margin, y + 6f, red, 1.5f)
            y += 28f
            canvas.drawText("Category:", margin, y, paint(11f, true, black))
            canvas.drawText(c.npcCategory.ifBlank { "-" }, margin + 130f, y, paint(11f))
            y += 20f
            canvas.drawText("Threat:", margin, y, paint(11f, true, black))
            canvas.drawText(c.npcTier.ifBlank { "-" }, margin + 130f, y, paint(11f))
            y += 28f

            canvas.drawText("Behavior / GM Notes:", margin, y, paint(11f, true, red))
            y += 18f
            wrapText(c.notes.ifBlank { "—" }, w - margin * 2).forEach { line2 ->
                canvas.drawText(line2, margin, y, paint(10.5f, false, black))
                y += 17f
            }
            y += 20f
        }

        // --- یادداشت‌ها (خطوط خالی) ---
        canvas.drawText("NOTES", margin, y, sectTitle)
        line(canvas, margin, y + 6f, w - margin, y + 6f, red, 1.5f)
        y += 30f
        repeat(if (c.isAlly) 32 else 20) {
            line(canvas, margin, y, w - margin, y, gray, 0.8f)
            y += 20f
        }
    }

    private fun wrapText(text: String, maxWidth: Float): List<String> {
        val p = paint(9.5f)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (p.measureText(test) > maxWidth && current.isNotEmpty()) {
                lines.add(current)
                current = word
            } else {
                current = test
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }
}
