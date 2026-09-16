package com.cyberpunk.gmtool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.gtr

private val HRed = Color(0xFFD32F2F)
private val HWhite = Color(0xFFE0E0E0)
private val HMuted = Color(0xFFAAAAAA)
private val HCard = Color(0xFF161616)
private val HDim = Color(0xFF1F1F1F)
private val HGold = Color(0xFFFFB300)
private val HGreen = Color(0xFF4CAF50)

/**
 * ابزارهای مشترک دستیارهای Role.
 *
 * تب نومد از این جهت خوب بود که فقط متن نبود — دکمه داشت، انتخاب داشت،
 * چیز تصادفی تولید می‌کرد. این فایل همان الگو را برای بقیه‌ی Roleها فراهم می‌کند
 * تا هر دستیار به‌جای دیوار متن، یک ابزار تصمیم‌گیری سر میز باشد.
 */

/** کارت استاندارد دستیار. */
@Composable
fun HelperCard(
    title: String,
    subtitle: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .background(HCard, CutCornerShape(9.dp))
            .border(1.dp, HRed.copy(.2f), CutCornerShape(9.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        FaText(title, color = HRed, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth())
        if (subtitle.isNotBlank()) {
            FaText(subtitle, color = HMuted, fontSize = 10.sp, lineHeight = 17.sp,
                modifier = Modifier.fillMaxWidth())
        }
        content()
    }
}

/**
 * تولیدکننده‌ی تصادفی: یک دکمه که هر بار یک گزینه‌ی تازه از فهرست بیرون می‌دهد.
 * برای وقتی که GM سر میز گیر می‌کند و همان لحظه یک ایده لازم دارد.
 */
@Composable
fun RollBox(
    label: String,
    options: List<String>,
    hint: String = ""
) {
    var index by remember(options) { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = {
                if (options.isNotEmpty()) {
                    var next = options.indices.random()
                    if (options.size > 1 && next == index) next = (next + 1) % options.size
                    index = next
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = HRed),
            shape = CutCornerShape(7.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Casino, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        if (index >= 0 && index < options.size) {
            Box(
                Modifier.fillMaxWidth()
                    .background(HGold.copy(.10f), CutCornerShape(7.dp))
                    .border(1.dp, HGold.copy(.45f), CutCornerShape(7.dp))
                    .padding(11.dp)
            ) {
                FaText(options[index], color = HWhite, fontSize = 13.sp, lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth())
            }
        } else if (hint.isNotBlank()) {
            FaText(hint, color = HMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** انتخاب یکی از چند گزینه — به‌شکل ردیف‌های تمام‌عرض، نه چیپ‌های نامرتب. */
@Composable
fun PickerRows(
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o ->
            val on = o == selected
            Row(
                Modifier.fillMaxWidth()
                    .background(if (on) HRed.copy(.16f) else Color.Transparent, CutCornerShape(6.dp))
                    .border(1.dp, if (on) HRed.copy(.7f) else HMuted.copy(.18f), CutCornerShape(6.dp))
                    .clickable { onPick(o) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(7.dp)
                        .background(if (on) HRed else HMuted.copy(.4f), CutCornerShape(2.dp))
                )
                Spacer(Modifier.width(9.dp))
                FaText(
                    o, color = if (on) HWhite else HMuted, fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * پاسخ شرطی: بر اساس چند کلید روشن/خاموش، یک توصیه‌ی مشخص می‌دهد.
 * جای «متن ثابتی که همیشه یکی است» را می‌گیرد.
 */
@Composable
fun DecisionBox(verdict: String, tone: String = "neutral") {
    val c = when (tone) {
        "yes" -> HGreen
        "no" -> HRed
        else -> HGold
    }
    Box(
        Modifier.fillMaxWidth()
            .background(c.copy(.10f), CutCornerShape(7.dp))
            .border(1.dp, c.copy(.5f), CutCornerShape(7.dp))
            .padding(11.dp)
    ) {
        FaText(verdict, color = HWhite, fontSize = 12.sp, lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth())
    }
}

/** سوییچ ساده با برچسب فارسی. */
@Composable
fun HelperToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(HDim, CutCornerShape(6.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HRed, checkedTrackColor = HRed.copy(.35f)
            )
        )
        Spacer(Modifier.width(8.dp))
        FaText(label, color = HWhite, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

/** شمارنده‌ی کوچک (مثل Favor/Debt نومد) برای هر Role. */
@Composable
fun HelperCounter(
    label: String, value: Int, modifier: Modifier = Modifier, onDelta: (Int) -> Unit
) {
    Column(
        modifier
            .background(HDim, CutCornerShape(7.dp))
            .border(1.dp, HRed.copy(.2f), CutCornerShape(7.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FaText(label, color = HMuted, fontSize = 10.sp)
        Text("$value", color = HGold, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { onDelta(-1) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text("−", color = HRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            TextButton(onClick = { onDelta(1) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text("+", color = HRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/** یک سناریوی آماده با دکمه‌ی «بعدی». */
@Composable
fun ScenarioBox(
    title: String,
    lines: List<Pair<String, String>>,
    onNext: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FaText(title, color = HWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onNext) {
                Icon(Icons.Default.Refresh, null, tint = HRed, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(gtr("next"), color = HRed, fontSize = 11.sp)
            }
        }
        lines.forEach { (k, v) ->
            Column(Modifier.fillMaxWidth()) {
                FaText(k, color = HRed, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth())
                FaText(v, color = HWhite, fontSize = 12.sp, lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
