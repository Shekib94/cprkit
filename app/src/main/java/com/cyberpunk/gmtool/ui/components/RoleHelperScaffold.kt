package com.cyberpunk.gmtool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.HelperScenario
import com.cyberpunk.gmtool.data.gtr

/**
 * قالب مشترک صفحات Role Helper (Nomad / Medtech / Media / Exec / Lawman / Fixer).
 *
 * چرا: قبلاً سه نقش (Exec/Lawman/Fixer) به‌شکل دیالوگ با متن‌های فشرده باز
 * می‌شدند و بقیه صفحه‌ی کامل با کارت‌های یکسان بودند؛ کاربر حس می‌کرد وارد
 * یک بازی دیگر شده. از این پس هر شش نقش از همین قالب استفاده می‌کنند:
 * رنگ، اندازه‌ی تیتر، کارت‌ها، انتخاب کاراکتر و کارت صحنه یکی است.
 */
object RoleUi {
    val Red = Color(0xFFD32F2F)
    val Green = Color(0xFF66BB6A)
    val Amber = Color(0xFFFFC107)
    val White = Color(0xFFEAEAEA)
    val Muted = Color(0xFFAAAAAA)
    val Card = Color(0xFF1A1A1A)
    val CardSoft = Color(0xFF202020)
    val Inner = Color(0xFF14181F)
    val Pill = Color(0xFF1E1E1E)
}

/** اسکلت صفحه: LazyColumn با فاصله و پدینگ یکسان در همه‌ی نقش‌ها. */
@Composable
fun RoleHelperPage(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

/** تیتر صفحه + زیرعنوان توضیحی. */
@Composable
fun RoleHelperHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(gtr(title), color = RoleUi.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        FaText(subtitle, color = RoleUi.Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), justify = true)
    }
}

/** کارت استاندارد با تیتر قرمز. */
@Composable
fun RoleHelperCard(
    title: String? = null,
    containerColor: Color = RoleUi.Card,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                FaText(title, color = RoleUi.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
            }
            content()
        }
    }
}

/**
 * کارت انتخاب کاراکتر همان نقش: یک FilterChip برای هر کاراکتر + یک خط خلاصه.
 * [summary] از برگه‌ی همان کاراکتر ساخته می‌شود (نه داده‌ی تکراری).
 */
@Composable
fun RoleHelperCharacterCard(
    title: String,
    characters: List<Character>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    summary: (Character) -> String,
    chipLabel: (Character) -> String = { gtr("%1s • Rank %2s", it.handle.ifBlank { it.name }, it.roleRank) }
) {
    RoleHelperCard(title) {
        if (characters.isEmpty()) {
            FaText("هیچ کاراکتر بازیکن با این نقش پیدا نشد. اول یک کاراکتر با همین Role بساز.", color = RoleUi.Muted, modifier = Modifier.fillMaxWidth())
            return@RoleHelperCard
        }
        characters.forEach { c ->
            FilterChip(
                selected = c.id == selectedId,
                onClick = { onSelect(c.id) },
                label = { Text(chipLabel(c)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        val current = characters.firstOrNull { it.id == selectedId } ?: characters.first()
        Text(summary(current), color = RoleUi.White, fontSize = 12.sp)
    }
}

/** ردیف چیپ‌های کاراکتر (برای انتخاب سریع). */
@Composable
fun RoleHelperChipRow(
    labels: List<Pair<Int, String>>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    prefix: String? = null
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        prefix?.let { Text(it, color = RoleUi.Muted, fontSize = 11.sp) }
        labels.forEach { (id, label) ->
            val on = id == selectedId
            androidx.compose.material3.Surface(
                color = if (on) RoleUi.Red else RoleUi.Pill,
                shape = CutCornerShape(5.dp),
                modifier = Modifier.clickable { onSelect(id) }
            ) {
                Text(
                    label,
                    color = if (on) Color.Black else RoleUi.White,
                    fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/** کارت «صحنه‌ی آماده» با دکمه‌ی سناریوی بعدی — در همه‌ی نقش‌ها یکسان. */
@Composable
fun RoleScenarioCard(scenarios: List<HelperScenario>, index: Int, onNext: () -> Unit) {
    if (scenarios.isEmpty()) return
    val s = scenarios[index.coerceIn(0, scenarios.lastIndex)]
    RoleHelperCard("صحنه‌ی آماده‌ی GM") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FaText(s.title, color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(gtr("سناریوی بعدی"), color = RoleUi.Red, fontSize = 12.sp, modifier = Modifier.clickable { onNext() })
        }
        FaText(s.situation, color = RoleUi.White, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth(), justify = true)
        FaText("اجرای پیشنهادی: ${s.run}", color = RoleUi.Muted, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth(), justify = true)
        FaText("پیچش: ${s.complication}", color = RoleUi.Muted, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth(), justify = true)
        FaText("پاداش/پیامد: ${s.payoff}", color = RoleUi.Muted, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth(), justify = true)
    }
}

/** کارت «فرصت درخشش» — پایان همه‌ی صفحات Role Helper. */
@Composable
fun RoleGloryCard(text: String) {
    RoleHelperCard("فرصت درخشش Role") {
        FaText(text, color = RoleUi.Muted, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth(), justify = true)
    }
}

/** جعبه‌ی تیره‌ی داخلی برای ردیف‌های اطلاعاتی (مثل کارت‌های Medtech). */
@Composable
fun RoleInnerBox(modifier: Modifier = Modifier, padding: Dp = 10.dp, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = RoleUi.Inner), shape = CutCornerShape(8.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}
