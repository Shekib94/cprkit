package com.cyberpunk.gmtool.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.CoreItemReference
import com.cyberpunk.gmtool.data.CyberwareCatalog
import com.cyberpunk.gmtool.data.ItemDetailsCatalog
import com.cyberpunk.gmtool.data.StoreCatalog
import com.cyberpunk.gmtool.data.StoreItem
import com.cyberpunk.gmtool.data.gtr

private val DRed = Color(0xFFD32F2F)
private val DBg = Color(0xFF121212)
private val DCard = Color(0xFF1D1D1D)
private val DWhite = Color(0xFFE8E8E8)
private val DMuted = Color(0xFFAAAAAA)

/**
 * تنها نمایش‌دهنده‌ی جزئیات آیتم در کل برنامه.
 *
 * قبلاً هر جا یک نسخه‌ی متفاوت بود — فروشگاه، کوله، سازنده‌ی شخصیت، بازار شبانه،
 * Agent — و هرکدام چیدمان و راست‌چینی خودش را داشت. حالا همه همین را صدا می‌زنند.
 *
 * ورودی می‌تواند خود آیتم باشد یا فقط اسمش؛ اگر اسم باشد از کاتالوگ پیدا می‌شود.
 */
@Composable
fun ItemDetailSheet(
    item: StoreItem,
    onDismiss: () -> Unit,
    priceOverride: Int? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val price = priceOverride ?: item.basePrice
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DBg),
                shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
                modifier = Modifier.fillMaxWidth()
                    .border(1.5.dp, DRed, CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp))
            ) {
                Column(Modifier.padding(16.dp)) {

                    // ── سربرگ: نام + قیمت ──
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            FaText(
                                gtr(item.name), color = DWhite, fontSize = 18.sp,
                                fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth()
                            )
                            FaText(
                                gtr(item.category) + if (item.subtitle.isNotBlank()) " • ${item.subtitle}" else "",
                                color = DMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("${price}eb", color = DRed, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = DRed.copy(.45f))
                    Spacer(Modifier.height(10.dp))

                    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {

                        // ── جدول آمار، بسته به دسته ──
                        val stats = statsFor(item, price)
                        if (stats.isNotEmpty()) {
                            StatBlock(stats)
                            Spacer(Modifier.height(10.dp))
                        }

                        // ── خشاب ──
                        if (item.magStd != null) {
                            MagBlock(item.magStd, item.magExt, item.magDrum)
                            Spacer(Modifier.height(10.dp))
                        }

                        // ── حالت‌های شلیک ──
                        if (item.modes.isNotBlank()) {
                            LabeledBox(gtr("Firing modes")) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(item.modes, color = DWhite, fontSize = 13.sp, lineHeight = 20.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // ── جدول برد ──
                        if (item.rangeSingle.isNotEmpty()) {
                            RangeBlock(gtr("Single Shot DV"), item.rangeSingle)
                            Spacer(Modifier.height(10.dp))
                        }
                        if (item.rangeAuto.isNotEmpty()) {
                            RangeBlock(gtr("Autofire DV"), item.rangeAuto)
                            Spacer(Modifier.height(10.dp))
                        }

                        // ── توضیحات ──
                        val desc = ItemDetailsCatalog.fullDescription(item)
                        if (desc.isNotBlank()) {
                            LabeledBox(gtr("Description")) {
                                FaText(
                                    desc, color = DWhite, fontSize = 13.sp, lineHeight = 22.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (footer != null) {
                        Spacer(Modifier.height(12.dp))
                        footer()
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DRed),
                        shape = CutCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(gtr("close"), color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/**
 * نسخه‌ای که فقط اسم می‌گیرد. هر جای برنامه که آیتم را با نام می‌شناسد
 * (کوله، بازار شبانه، Agent، سازنده‌ی شخصیت) همین را صدا می‌زند.
 */
@Composable
fun ItemDetailSheetByName(
    label: String,
    categoryHint: String? = null,
    onDismiss: () -> Unit
) {
    val resolved = CoreItemReference.resolveComposite(label, categoryHint)
    val single = resolved.firstOrNull()
        ?: StoreCatalog.findForInventory(label, categoryHint ?: "")

    if (single == null) {
        // آیتمی که در کاتالوگ Core آمار مستقل ندارد: فقط نام و قیمتِ جدول.
        val display = label.substringBefore(" — ").trim()
        val price = Regex("([0-9,]+)eb", RegexOption.IGNORE_CASE)
            .find(label)?.groupValues?.getOrNull(1)
        Dialog(onDismissRequest = onDismiss) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DBg),
                    shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                        .border(1.5.dp, DRed, CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        FaText(gtr(display), color = DWhite, fontSize = 17.sp,
                            fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
                        if (price != null) {
                            Spacer(Modifier.height(4.dp))
                            FaText(gtr("Table price: %1seb", price), color = DRed, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(10.dp))
                        FaText(
                            gtr("This entry appears in Core as generic goods or a Night Market category and has no independent stats. The app shows only the listed name and price and invents no extra rule."),
                            color = DMuted, fontSize = 12.sp, lineHeight = 21.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = DRed),
                            shape = CutCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(gtr("close"), color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        return
    }

    // بسته‌ای که به چند آیتم باز می‌شود: همان اولی را کامل نشان بده،
    // چون شیت مشترک برای یک آیتم طراحی شده و تکرار کارت‌ها همان بی‌نظمی قبلی بود.
    ItemDetailSheet(item = single, onDismiss = onDismiss)
}

// ─────────────────────── اجزای داخلی ───────────────────────

private fun statsFor(item: StoreItem, price: Int): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    out += "Standard Cost" to "${price}eb"
    when {
        item.category == "Weapons" -> {
            if (item.damage.isNotBlank()) out += "Damage" to item.damage
            if (item.weaponSkill.isNotBlank()) out += "Weapon Skill" to item.weaponSkill
            if (item.concealed.isNotBlank()) out += "Concealed" to item.concealed
            if (item.hands.isNotBlank()) out += "Hands" to item.hands
            if (item.rof.isNotBlank()) out += "Rate of Fire" to item.rof
            if (item.ammoType.isNotBlank()) out += "Ammo Type" to item.ammoType
            if (item.autofire.isNotBlank()) out += "Autofire" to "+${item.autofire}"
        }
        item.category == "Programs" -> {
            out += "Class" to item.programClass.ifBlank { "Program" }
            item.atk?.let { out += "ATK" to "$it" }
            item.def?.let { out += "DEF" to "$it" }
            item.rez?.let { out += "REZ" to "$it" }
            item.per?.let { out += "PER" to "$it" }
            item.spd?.let { out += "SPD" to "$it" }
        }
        item.category == "Cyberware" -> {
            val rule = CyberwareCatalog.ruleFor(item.name)
            (rule?.install ?: item.slot).takeIf { it.isNotBlank() }?.let { out += "Install" to it }
            rule?.group?.let { out += "Type" to it }
            rule?.prerequisite?.let { out += "Requires" to it }
            rule?.foundationCapacity?.let { out += "Provides Slots" to "$it" }
            rule?.takeIf { !it.noSlot && !it.chipware }?.let { out += "Slots Used" to "${it.slotsUsed}" }
            item.humanityLoss?.let { out += "Chargen HL" to "$it" }
            (rule?.hlDice ?: item.humanityDice).takeIf { it.isNotBlank() }
                ?.let { out += "Post-Chargen HL" to it }
        }
    }
    return out
}

@Composable
private fun StatBlock(pairs: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DCard),
        shape = CutCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, DRed.copy(.3f), CutCornerShape(8.dp))
    ) {
        // آمار و اعداد بازی همیشه چپ‌به‌راست‌اند — «5d6» و «500eb» نباید برعکس شوند.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.padding(12.dp)) {
                pairs.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        row.forEach { (k, v) ->
                            Column(Modifier.weight(1f)) {
                                Text(k.uppercase(), color = DMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(v, color = DWhite, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MagBlock(std: Int, ext: Int?, drum: Int?) {
    LabeledBox(gtr("Magazine")) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth()) {
                listOfNotNull(
                    "Standard" to std,
                    ext?.let { "Extended" to it },
                    drum?.let { "Drum" to it }
                ).forEach { (label, v) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$v", color = DRed, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(label, color = DMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeBlock(title: String, rows: List<Pair<String, Int>>) {
    LabeledBox(title) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth()) {
                rows.forEach { (dist, dv) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(dist, color = DMuted, fontSize = 12.sp)
                        Text("DV$dv", color = DWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledBox(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DCard),
        shape = CutCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, DRed.copy(.3f), CutCornerShape(8.dp))
    ) {
        Column(Modifier.padding(12.dp)) {
            FaText(title, color = DRed, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
