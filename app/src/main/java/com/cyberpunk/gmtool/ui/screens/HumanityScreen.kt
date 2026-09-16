package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CyberwareCatalog
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
// توابع توسعه‌ی دامنه‌های تفکیک‌شده‌ی ViewModel در همین پکیج‌اند.
import com.cyberpunk.gmtool.viewmodel.*
import kotlinx.coroutines.launch

private val HRed = Color(0xFFD32F2F)
private val HRedChip = Color(0xFFE0554C)
private val HBadge = Color(0xFF120808)
private val HBlack = Color(0xFF0D0D0D)
private val HCard = Color(0xFF171717)
private val HWhite = Color(0xFFEAEAEA)
private val HMuted = Color(0xFFB0B0B0)

/** پنجره‌ی تمام‌صفحه‌ی Humanity + Therapy (مثل اپ مرجع) */
@Composable
fun HumanityScreen(
    character: Character,
    viewModel: CharacterViewModel,
    onBack: () -> Unit
) {
    var showTherapy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val allCharacters by viewModel.characters.collectAsState()
    val therapyMedtechs = allCharacters.filter { it.id != character.id && it.role.equals("Medtech", true) }
    var selectedMedtechId by remember(character.id, therapyMedtechs.map { it.id }) { mutableStateOf(therapyMedtechs.firstOrNull()?.id) }
    var selectedAddiction by remember(character.id, character.addictions) { mutableStateOf(character.addictions.firstOrNull()) }

    // تراپی یک Check d10 می‌ریزد (و درمان دارویی d6). در حالت «تاس دستی» DiceSource
    // نخ فراخوان را بلاک می‌کند تا GM عدد را بدهد، و این روی نخ اصلی ممکن نیست؛
    // پس عملیات را به نخ پس‌زمینه می‌بریم و نتیجه را روی Main نشان می‌دهیم.
    val rulesScope = rememberCoroutineScope()
    fun rules(block: () -> Unit) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            rulesScope.launch(kotlinx.coroutines.Dispatchers.Default) { block() }
        } else block()
    }

    // سایبرویرهای نصب‌شده (equipped) — منبع خودکار لیست کسرها
    val cyberware = remember(character.inventory) {
        character.inventory.filter { it.category.equals("Cyberware", true) && it.equipped }
    }
    val lossRows = cyberware.mapNotNull { item ->
        val l = CyberwareCatalog.humanityLossFor(item.name)
        if (l > 0) item.name to l else null
    }
    val maxRows = cyberware.mapNotNull { item ->
        val m = CyberwareCatalog.maxReductionFor(item.name)
        if (m != 0) item.name to m else null
    }

    Column(Modifier.fillMaxSize().background(HBlack)) {
        // هدر: برگشت، عنوان، پول
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (showTherapy) showTherapy = false else onBack()
            }) {
                Icon(Icons.Default.ArrowBack, gtr("Back"), tint = HRed, modifier = Modifier.size(28.dp))
            }
            Text(
                if (showTherapy) "تراپی (Therapy)" else "انسانیت (Humanity)",
                color = HWhite, fontSize = 24.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Row(
                Modifier.clip(CutCornerShape(8.dp)).background(HRed).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("€$ ${character.eurodollars}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
        HorizontalDivider(color = HRed.copy(alpha = 0.6f), thickness = 2.dp)

        if (!showTherapy) {
            // ===================== پنل Humanity =====================
            LazyColumn(Modifier.fillMaxSize().padding(14.dp)) {
                item {
                    Column(Modifier.fillMaxWidth()) {
                        // نوار عنوان
                        Box(Modifier.fillMaxWidth().clip(CutCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            .background(HCard).border(2.dp, HRed, CutCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            .padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                            Text(gtr("Humanity"), color = HWhite, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                        // مقدار Current / Max
                        Box(Modifier.fillMaxWidth().background(HCard)
                            .border(2.dp, HRed).padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BigVal(character.currentHumanity, "Current")
                                Text("/", color = HWhite, fontSize = 44.sp, fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 18.dp))
                                BigVal(character.maxHumanity, "Max")
                            }
                        }
                        // ── وضعیت سایبرسایکوزیس و مسیر درمان ──
                        val psyStatus = com.cyberpunk.gmtool.data.CyberpsychosisRules.statusLine(character)
                        if (psyStatus.isNotBlank()) {
                            val isPsy = com.cyberpunk.gmtool.data.CyberpsychosisRules.isCyberpsycho(character)
                            val tone = if (isPsy) HRed else Color(0xFFFFB300)
                            Column(Modifier.fillMaxWidth().background(HCard).border(2.dp, tone).padding(14.dp)) {
                                com.cyberpunk.gmtool.ui.components.FaText(
                                    psyStatus, color = tone, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp, modifier = Modifier.fillMaxWidth()
                                )
                                if (isPsy) {
                                    val plan = com.cyberpunk.gmtool.data.CyberpsychosisRules.therapyPlan(character)
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = tone.copy(alpha = .4f))
                                    Spacer(Modifier.height(10.dp))
                                    com.cyberpunk.gmtool.ui.components.FaText(
                                        "مسیر بازگشت", color = HWhite, fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    com.cyberpunk.gmtool.ui.components.FaText(
                                        "برای رسیدن به انسانیت ${plan.target} (قابل بازی شدن دوباره) " +
                                        "${plan.needed} واحد لازم است — تخمین خوش‌بینانه بر پایه‌ی میانگین تاس:",
                                        color = HWhite, fontSize = 12.sp, lineHeight = 20.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    com.cyberpunk.gmtool.ui.components.FaText(
                                        "• تراپی استاندارد (2d6 در هفته): حدود ${plan.standardWeeks} هفته و ${plan.standardCost}eb",
                                        color = HWhite, fontSize = 12.sp, lineHeight = 20.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    com.cyberpunk.gmtool.ui.components.FaText(
                                        "• تراپی شدید (4d6 در هفته): حدود ${plan.extremeWeeks} هفته و ${plan.extremeCost}eb",
                                        color = HWhite, fontSize = 12.sp, lineHeight = 20.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    com.cyberpunk.gmtool.ui.components.FaText(
                                        "هر هفته یک Medical Tech Check لازم است (استاندارد DV15، شدید DV17). " +
                                        "هفته‌ای که Check شکست بخورد هزینه‌اش هدر می‌رود و به این عددها اضافه می‌شود.",
                                        color = HMuted, fontSize = 11.sp, lineHeight = 19.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (plan.blockedByCyberware) {
                                        Spacer(Modifier.height(8.dp))
                                        com.cyberpunk.gmtool.ui.components.FaText(
                                            "توجه: با سایبرویرِ نصب‌شده‌ی فعلی، تراپی بیشتر از ${plan.ceiling} " +
                                            "نمی‌تواند برگرداند. برای عبور از این سقف باید قطعه‌ای برداشته شود.",
                                            color = tone, fontSize = 11.sp, lineHeight = 19.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                        // Humanity Loss
                        Box(Modifier.fillMaxWidth().background(HCard)
                            .border(2.dp, HRed).padding(16.dp)) {
                            Column {
                                Text("Humanity Loss — کسر انسانیت", color = HWhite, fontSize = 18.sp,
                                    fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center)
                                Spacer(Modifier.height(14.dp))
                                if (lossRows.isEmpty()) {
                                    Text("سایبرویر کسرکننده‌ای نصب نشده است.", color = HMuted, fontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                } else {
                                    LossChips(lossRows, neg = true)
                                }
                            }
                        }
                        // Max Humanity Reductions
                        Box(Modifier.fillMaxWidth().background(HCard)
                            .border(2.dp, HRed).padding(16.dp)) {
                            Column {
                                Text("Max Humanity Reductions — کاهش سقف انسانیت", color = HWhite, fontSize = 17.sp,
                                    fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center)
                                Spacer(Modifier.height(14.dp))
                                if (maxRows.isEmpty()) {
                                    Text("سایبرویر کاهنده‌ی سقفی نصب نشده است.", color = HMuted, fontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                } else {
                                    LossChips(maxRows, neg = true)
                                }
                            }
                        }
                        // دکمه تراپی
                        Box(Modifier.fillMaxWidth().clip(CutCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                            .background(HCard).border(2.dp, HRed, CutCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                            .padding(16.dp)) {
                            Button(onClick = { showTherapy = true },
                                colors = ButtonDefaults.buttonColors(containerColor = HRed),
                                shape = CutCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                Icon(Icons.Default.HealthAndSafety, null, tint = Color.Black, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("گزینه‌های تراپی (Therapy Options)", color = Color.Black,
                                    fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(30.dp)) }
            }
        } else {
            // ===================== صفحه‌ی Therapy =====================
            TherapyContent(
                medtechs = therapyMedtechs,
                addictions = character.addictions,
                selectedMedtechId = selectedMedtechId,
                selectedAddiction = selectedAddiction,
                onSelectMedtech = { selectedMedtechId = it },
                onSelectAddiction = { selectedAddiction = it },
                onReceive = { kind, labelFa, costFa ->
                    val doctorId = selectedMedtechId
                    rules {
                        val r = if (doctorId == null) "یک Medtech دیگر را به‌عنوان درمانگر انتخاب کن."
                        else viewModel.receiveTherapy(character.id, kind, doctorId, if (kind == "addiction") selectedAddiction else null)
                        val msg = when {
                            r.startsWith("OK|") -> {
                                val parts = r.split("|")
                                val healed = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                val extra = parts.drop(2).joinToString(" • ")
                                if (healed == 0) "$labelFa موفق بود. $extra".trim()
                                else "$labelFa موفق بود؛ $healed Humanity بازیابی شد. $extra".trim()
                            }
                            r.startsWith("FAIL|") -> {
                                val parts = r.split("|")
                                "تراپی شکست خورد؛ 1d10=${parts.getOrNull(1)}, Total=${parts.getOrNull(2)}, DV=${parts.getOrNull(3)}. هزینه پرداخت شد و هفته/مواد از دست رفت."
                            }
                            else -> r
                        }
                        // Toast فقط روی نخ اصلی مجاز است؛ در حالت خودکار هم همین‌جا
                        // (که همان نخ اصلی است) اجرا می‌شود، پس رفتار عوض نمی‌شود.
                        rulesScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast(msg), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun BigVal(v: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.border(3.dp, HWhite, CutCornerShape(10.dp)).padding(horizontal = 26.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center) {
            Text("$v", color = HWhite, fontSize = 40.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        Text(gtr(label), color = HWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LossChips(rows: List<Pair<String, Int>>, neg: Boolean) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (name, v) ->
            Row(
                Modifier.clip(CutCornerShape(8.dp)).background(HRedChip)
                    .clickable {}.padding(end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.padding(start = 6.dp).clip(CutCornerShape(6.dp)).background(HBadge)
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(if (neg) "-$v" else "+$v", color = HRedChip, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(10.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(name), color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TherapyContent(
    medtechs: List<Character>,
    addictions: List<String>,
    selectedMedtechId: Int?,
    selectedAddiction: String?,
    onSelectMedtech: (Int) -> Unit,
    onSelectAddiction: (String) -> Unit,
    onReceive: (String, String, String) -> Unit
) {
    var medtechMenu by remember { mutableStateOf(false) }
    var addictionMenu by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("درمانگر را صریح انتخاب کن؛ Therapy یک هفته‌ی کامل Doctor و Patient را درگیر می‌کند و Medtech نمی‌تواند روی خودش Therapy انجام دهد.", color = HMuted, fontSize = 13.sp, lineHeight = 21.sp)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { medtechMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        val selected = medtechs.firstOrNull { it.id == selectedMedtechId }
                        Text(selected?.let { gtr("MEDTECH • %1s", it.handle.ifBlank { it.name }) } ?: "SELECT MEDTECH", color = HRed)
                    }
                    DropdownMenu(expanded = medtechMenu, onDismissRequest = { medtechMenu = false }) {
                        medtechs.forEach { d -> DropdownMenuItem(text = { Text(d.handle.ifBlank { d.name }) }, onClick = { onSelectMedtech(d.id); medtechMenu = false }) }
                    }
                }
                if (addictions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { addictionMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(gtr("ADDICTION • %1s", selectedAddiction ?: "SELECT"), color = HRed)
                        }
                        DropdownMenu(expanded = addictionMenu, onDismissRequest = { addictionMenu = false }) {
                            addictions.forEach { a -> DropdownMenuItem(text = { Text(gtr(a)) }, onClick = { onSelectAddiction(a); addictionMenu = false }) }
                        }
                    }
                }
            }
        }
        item { TherapySection("ترک اعتیاد", gtr("Addiction"),
            "1,000eb", gtr("DV15"), "500eb",
            "یک هفته روان‌درمانی فشرده به‌همراه دوره‌ی داروهای ضداعتیاد در محیطی امن.\n\n" +
                "بیمار از یکی از اعتیادهایش رها می‌شود؛ اما تا یک سال پس از ترک، هر بار که علیه " +
                "اثر ثانویه‌ی ماده‌ی اعتیادآور تاس بریزد، خودکار در آن تاس شکست می‌خورد.",
            kind = "addiction", onReceive = onReceive) }
        item { TherapySection("کسر استاندارد انسانیت", "Standard Humanity Loss",
            "500eb", "DV15", "100eb",
            "یک هفته روان‌درمانی فشرده با مشاوره‌ی مدیریت استرس و خشم، هیپنوتیزم و بازبرنامه‌ریزی " +
                "مغزی جزئی، به‌کمک داروها و محیط امن (که می‌تواند با برین‌دنس درمانی باشد).\n\n" +
                "بیمار 2d6 از Humanity از‌دست‌رفته را بازمی‌یابد. بدون برداشتن سایبرویر، انسانیت " +
                "کاملاً برنمی‌گردد؛ هر سایبرویر سقف را ۲ و هر Borgware آن را ۴ کم می‌کند. سایبرویری " +
                "که هنگام نصب کسر Humanity نداشته باشد، سقف را هم کم نمی‌کند.",
            kind = "standard", onReceive = onReceive) }
        item { TherapySection("کسر شدید انسانیت", "Extreme Humanity Loss",
            "1,000eb", gtr("DV17"), "500eb",
            "یک هفته روان‌درمانی فشرده با جلسات بازبرنامه‌ریزی مستقیم و شدید مغز؛ تنها با جدیدترین " +
                "داروها و محیط امن (قابل اجرا با برین‌دنس درمانی) ممکن است.\n\n" +
                "بیمار 4d6 از Humanity از‌دست‌رفته را بازمی‌یابد. قواعد سقف مانند تراپی استاندارد است: " +
                "بدون برداشتن سایبرویر، انسانیت کامل برنمی‌گردد.",
            kind = "extreme", onReceive = onReceive) }
        item {
            // بخش توضیحی تراپی و شما
            Box(Modifier.fillMaxWidth()) {
                Column {
                    Box(Modifier.fillMaxWidth().background(HRed).padding(14.dp),
                        contentAlignment = Alignment.Center) {
                        Text("تراپی و شما!  (THERAPY AND YOU!)", color = Color.Black, fontSize = 19.sp,
                            fontWeight = FontWeight.Black)
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(Modifier.padding(18.dp)) {
                            Text("همدلی (EMP) یک ویژگی است و به‌ازای هر نقطه EMP، ۱۰ امتیاز Humanity دارید. " +
                                "مثلاً کاراکتری با ۵ EMP، ۵۰ Humanity دارد. با نصب سایبرنتیک و کسر Humanity، " +
                                "وقتی رقم دهگان Humanity عوض شود (مثلاً از ۵۰ به ۴۶)، EMP عملاً یک واحد کم می‌شود (۵→۴).",
                                color = HWhite, fontSize = 14.sp, lineHeight = 24.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("اینجا نقش تراپی روشن می‌شود: وقتی می‌خواهی همه‌ی سایبرنتیک‌هایت را نگه داری " +
                                "بدون اینکه همدلی‌ات کم شود، یا می‌خواهی اعتیاد را ترک کنی. ارزان یا آسان نیست، " +
                                "اما نسبت به درمان‌های گذشته بسیار سریع است؛ بیشتر این پیشرفت‌ها مدیون داروهایی " +
                                "است که بایوتکنیکا در جنگ چهارم شرکتی ابداع کرد.",
                                color = HWhite, fontSize = 14.sp, lineHeight = 24.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("یک مدتک ماهر می‌تواند با مهارت Medical Tech (از توانایی نقش Medicine) " +
                                "خودش تراپی انجام دهد. هر تراپی یک هفته‌ی کامل طول می‌کشد و در آن هفته پزشک و " +
                                "بیمار کار دیگری نمی‌توانند بکنند. پایان هفته پزشک علیه DV تراپی تاس می‌ریزد: " +
                                "موفقیت = اثر تراپی؛ شکست = آن هفته هدر می‌رود و مواد مصرفی از بین می‌روند. " +
                                "این مواد فقط توسط بایوتکنیکا کنترل و مستقیم از شرکت خریداری می‌شوند. قیمت‌های " +
                                "بالا بدون اقامت شبانه در بیمارستان است؛ اقامت هر شب ۱۰۰eb هزینه دارد.",
                                color = HWhite, fontSize = 14.sp, lineHeight = 24.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("مدتک‌ها نمی‌توانند روی خودشان تراپی انجام دهند.",
                                color = HRed, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TherapySection(
    titleFa: String, titleEn: String,
    cost: String, dv: String, materials: String,
    desc: String, kind: String,
    onReceive: (String, String, String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(HRed).padding(14.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(titleFa, color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Black)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(titleEn), color = Color(0xFF3A0D0A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.padding(18.dp)) {
            StatLine("هزینه (Cost)", cost)
            Spacer(Modifier.height(6.dp))
            StatLine("سختی Medical Tech", dv)
            Spacer(Modifier.height(6.dp))
            StatLine("هزینه‌ی مواد (Materials Cost)", materials)
            Spacer(Modifier.height(14.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                com.cyberpunk.gmtool.ui.components.FaText(gtr(desc), color = HWhite, fontSize = 14.sp, lineHeight = 24.sp,
                    justify = true, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(onClick = { onReceive(kind, titleFa, cost.replace(",", "").removeSuffix("eb")) },
                    colors = ButtonDefaults.buttonColors(containerColor = HRed),
                    shape = CutCornerShape(10.dp),
                    modifier = Modifier.height(50.dp).padding(horizontal = 10.dp)) {
                    Icon(Icons.Default.Favorite, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("دریافت تراپی (Receive Therapy)", color = Color.Black,
                        fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
        HorizontalDivider(color = HRed.copy(alpha = 0.4f))
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            com.cyberpunk.gmtool.ui.components.FaText("$label:", color = HWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(value, color = HWhite, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}
