package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CombatRules
import com.cyberpunk.gmtool.data.CriticalInjuries
import com.cyberpunk.gmtool.data.DiceSource
import com.cyberpunk.gmtool.data.GameRules
import com.cyberpunk.gmtool.data.MedtechCareRules
import com.cyberpunk.gmtool.data.RoleAssistantData
import com.cyberpunk.gmtool.data.RoleHelperData
import androidx.compose.runtime.CompositionLocalProvider
import com.cyberpunk.gmtool.ui.components.FaText
import com.cyberpunk.gmtool.ui.components.HelperCard
import com.cyberpunk.gmtool.ui.components.HelperToggle
import com.cyberpunk.gmtool.ui.components.PickerRows
import com.cyberpunk.gmtool.ui.components.RollBox
import com.cyberpunk.gmtool.ui.components.RoleInnerBox
import com.cyberpunk.gmtool.ui.components.RuleInfoButton
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.viewmodel.addInventoryItem
import com.cyberpunk.gmtool.viewmodel.administerPharmaceutical
import com.cyberpunk.gmtool.viewmodel.applyRecoveryDay
import com.cyberpunk.gmtool.viewmodel.careForInjury
import com.cyberpunk.gmtool.viewmodel.stabilizeCharacter
import com.cyberpunk.gmtool.viewmodel.removeInventoryItem
import com.cyberpunk.gmtool.viewmodel.teachParamedic
import kotlinx.coroutines.launch

// رنگ‌های محلی این فایل (هم‌خانواده‌ی بقیه‌ی صفحات GM)
private val MdRed = Color(0xFFD32F2F)
private val MdCardColor = Color(0xFF1A1A1A)
private val MdCardSoft = Color(0xFF202020)
private val MdWhite = Color(0xFFEAEAEA)
private val MdMuted = Color(0xFFAAAAAA)
private val MdGreen = Color(0xFF66BB6A)
private val MdAmber = Color(0xFFFFC107)

private val MdRtl = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Right)
private val MdRtlJustify = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Justify)

/**
 * دستیار Medtech برای GM — نسخه‌ی عملی.
 *
 * تفاوت با نسخه‌ی قبلی: اینجا فقط توضیح و سناریو نیست؛ بیمار از لیست خودِ
 * کاراکترهای برنامه انتخاب می‌شود و وضعیتش **از همان برگه** خوانده می‌شود
 * (Seriously Wounded، Critical Injuries، HP). درمان هم روی همان برگه اعمال
 * می‌شود: پایدارسازی، Quick Fix/Treatment هر جراحت، Pharmaceutical و
 * بازیابی روزانه‌ی HP.
 */
@Composable
fun MedtechGmAssistant(viewModel: CharacterViewModel) {
    val characters by viewModel.characters.collectAsState()
    val scope = rememberCoroutineScope()

    val medtechs = characters.filter { it.role.equals("Medtech", true) && it.isAlly }
    var selectedId by remember(medtechs.map { it.id }) { mutableStateOf(medtechs.firstOrNull()?.id) }
    val medtech = medtechs.firstOrNull { it.id == selectedId } ?: medtechs.firstOrNull()

    var targetId by remember { mutableStateOf<Int?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    var preferCyber by remember { mutableStateOf(false) }
    var addictionRisk by remember { mutableStateOf(false) }
    var fasting by remember { mutableStateOf(false) }
    var medSituation by remember { mutableStateOf(RoleHelperData.medtechSituations.first()) }
    var medBag by remember { mutableStateOf(true) }
    var medSafe by remember { mutableStateOf(true) }
    var medRush by remember { mutableStateOf(false) }

    // در حالت تاس دستی، عملیات باید از نخ پس‌زمینه اجرا شود.
    fun rules(block: () -> Unit) {
        if (DiceSource.isManual) scope.launch(kotlinx.coroutines.Dispatchers.Default) { block() } else block()
    }

    if (medtech == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            FaText("هیچ Medtech بازیکن پیدا نشد. اول یک کاراکتر با نقش Medtech بساز.", color = MdMuted, modifier = Modifier.fillMaxWidth())
        }
        return
    }

    // همه‌ی کاراکترها در لیست بیمار می‌آیند (حتی مرده‌ها) تا GM مجبور نباشد دنبال کسی بگردد.
    val patients = characters
    val target = targetId?.let { id -> characters.firstOrNull { it.id == id } } ?: patients.firstOrNull()
    val surgery = medtech.roleAbilityPoints["surgery"] ?: 0
    val pharmaPoints = medtech.roleAbilityPoints["pharma"] ?: 0
    val cryo = medtech.roleAbilityPoints["cryo"] ?: 0
    val medicalTech = MedtechCareRules.medicalTechLevel(medtech)
    val surgerySkill = (surgery * 2).coerceAtMost(10)
    val learned = MedtechCareRules.learnedPharmaceuticals(medtech)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FaText("دستیار Medtech برای GM", color = MdWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            FaText(
                "این صفحه درمان واقعی است: بیمار را از لیست کاراکترها انتخاب کن، وضعیتش از همان برگه خوانده می‌شود و موفقیت، جراحت/HP را همان‌جا عوض می‌کند. اعداد از MedtechCareRules می‌آیند تا با تب COMBAT یکی باشد.",
                color = MdMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), justify = true
            )
        }

        // ── ۱) انتخاب Medtech ──
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MdCardColor), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FaText("۱. درمانگر (Medtech)", color = MdRed, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    medtechs.forEach { c ->
                        FilterChip(
                            selected = c.id == medtech.id,
                            onClick = { selectedId = c.id; result = null },
                            label = { Text(gtr("%1s • Medicine %2s", c.handle.ifBlank { c.name }, c.roleRank)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        gtr("Medicine Rank %1s • Surgery %2s → Skill %3s • Pharmaceuticals %4s • Cryosystem %5s • Medical Tech %6s • IP %7s",
                            medtech.roleRank, surgery, surgerySkill, pharmaPoints, cryo, medicalTech, medtech.improvementPoints),
                        color = MdWhite, fontSize = 12.sp
                    )
                    val paramedic = MedtechCareRules.paramedicLevel(medtech)
                    val firstAid = MedtechCareRules.firstAidLevel(medtech)
                    FaText(
                        if (paramedic > 0) "روند درمان: Paramedic $paramedic ${if (firstAid > 0) "• First Aid $firstAid" else ""} • ⚠ تا وقتی جراحتی Treatment نشده، DC جریمه‌ها و Death Save سر جای خود است."
                        else "روند درمان: Paramedic ندارد؛ فقط First Aid $firstAid. برای Stabilize سخت (DV13/DV15) و Quick Fixهای جدول، دوره‌ی Paramedic لازم است (۲ هفته + ۶۰ IP).",
                        color = if (paramedic > 0) MdMuted else MdAmber, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(), justify = true
                    )
                    if (MedtechCareRules.needsParamedicTraining(medtech)) {
                        Button(
                            onClick = { result = viewModel.teachParamedic(medtech.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MdRed),
                            shape = CutCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(gtr("ثبت دوره‌ی Paramedic (۲ هفته + ۶۰ IP)"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }
                    // این کلید واقعاً کار می‌کند: آیتم «Medtech Bag» را در GEAR همین
                    // کاراکتر می‌سازد/برمی‌دارد و همان لحظه روی DV پایدارسازی اثر می‌گذارد.
                    val hasBag = MedtechCareRules.hasTraumaTeamKit(medtech)
                    val bagIndex = medtech.inventory.indexOfFirst { it.name.contains("Medtech Bag", true) || it.name.contains("Trauma Team", true) }
                    HelperToggle(
                        if (hasBag) "Medtech Bag همراه است → +${MedtechCareRules.TRAUMA_TEAM_KIT_BONUS} روی پایدارسازی (برای برداشتن، خاموش کن)"
                        else "Medtech Bag همراه نیست — روشن کن تا در GEAR اضافه شود",
                        hasBag
                    ) { wants ->
                        if (wants) {
                            if (bagIndex < 0) viewModel.addInventoryItem(medtech.id, "Medtech Bag", "Gear")
                            result = "Medtech Bag به GEAR کاراکتر اضافه شد؛ +${MedtechCareRules.TRAUMA_TEAM_KIT_BONUS} روی پایدارسازی اعمال می‌شود."
                        } else {
                            if (bagIndex >= 0) viewModel.removeInventoryItem(medtech.id, bagIndex)
                            result = "Medtech Bag از GEAR برداشته شد؛ دیگر بونوس تجهیزات اعمال نمی‌شود."
                        }
                    }
                }
            }
        }

        // ── ۲) انتخاب بیمار از لیست کاراکترها ──
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MdCardColor), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FaText("۲. بیمار", color = MdRed, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    FaText(
                        "بیمار از لیست کاراکترهای برنامه انتخاب می‌شود تا وضعیت واقعی‌اش (Seriously Wounded / Critical Injury) همین‌جا بیاید و درمان هم روی همان برگه بنشیند. NPCهای بدون برگه را می‌توانی پایین همین صفحه به‌عنوان «بیمار ثبت‌شده» نگه داری.",
                        color = MdMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true
                    )
                    // انتخاب بیمار با منوی کشویی: فهرست هر چقدر بلند شود صفحه شلوغ نمی‌شود.
                    var patientMenu by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { patientMenu = true }, enabled = patients.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                target?.let { gtr("بیمار: %1s%2s", it.handle.ifBlank { it.name }, if (it.isAlly) "" else " (NPC)") } ?: "بیمار انتخاب نشده",
                                color = MdWhite, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                            )
                        }
                        DropdownMenu(expanded = patientMenu, onDismissRequest = { patientMenu = false }, modifier = Modifier.heightIn(max = 420.dp)) {
                            patients.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(gtr("%1s%2s", p.handle.ifBlank { p.name }, if (p.isAlly) "" else " (NPC)"), color = if (p.id == target?.id) MdRed else MdWhite) },
                                    onClick = { targetId = p.id; result = null; patientMenu = false }
                                )
                            }
                        }
                    }
                    if (patients.isEmpty()) {
                        FaText("هنوز کاراکتری ساخته نشده.", color = MdMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    }
                    target?.let { p -> PatientVitalsCard(p) }
                }
            }
        }

        // ── ۳) اقدامات درمانی روی همین بیمار ──
        target?.let { patient ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MdCardSoft), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FaText("۳. درمان", color = MdRed, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

                        // پایدارسازی
                        val mortally = GameRules.isMortallyWounded(patient.hp) && !patient.isDead
                        val seriously = GameRules.isSeriouslyWounded(patient.hp, patient.maxHp)
                        val stabilizeTarget = MedtechCareRules.stabilizeTarget(patient.hp, patient.maxHp, patient.isDead)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            FaText("Stabilize — بستن خون‌ریزی و پایدارسازی", color = MdWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                            RuleInfoButton("medical.stabilize")
                        }
                        FaText(
                            "وضعیت فعلی: ${stabilizeTarget.labelFa} → DV${stabilizeTarget.dv} • مهارت درمانگر: ${MedtechCareRules.bestStabilizeSkill(medtech).first} ${MedtechCareRules.bestStabilizeSkill(medtech).second}",
                            color = if (mortally || seriously) MdAmber else MdMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { rules { result = viewModel.stabilizeCharacter(patient.id, medtech.id) } },
                            enabled = patient.hp > 0 || mortally,
                            colors = ButtonDefaults.buttonColors(containerColor = MdRed),
                            shape = CutCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                gtr("STABILIZE • TECH + %1s vs DV%2s", MedtechCareRules.bestStabilizeSkill(medtech).first, stabilizeTarget.dv),
                                color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp
                            )
                        }

                        // جراحت‌های بحرانی همین کاراکتر
                        if (patient.criticalInjuries.isEmpty()) {
                            FaText("Critical Injury فعالی روی این کاراکتر نیست.", color = MdGreen, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                        } else {
                            Spacer(Modifier.height(2.dp))
                            FaText("Critical Injuries (${patient.criticalInjuries.size}) — با کلیک روی هر ردیف، Quick Fix یا Treatment می‌ریزی:", color = MdWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                            patient.criticalInjuries.forEach { key ->
                                val injury = CriticalInjuries.byKey(key) ?: return@forEach
                                val active = CriticalInjuries.effectActive(patient, key)
                                val qf = CriticalInjuries.quickFixOptions(key)
                                val tr = CriticalInjuries.treatmentOptions(key)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14181F)),
                                    shape = CutCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(gtr("%1s • %2s", injury.faName, injury.enName), color = if (active) MdRed else MdGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Text(if (active) "اثر فعال" else "خنثی‌شده", color = if (active) MdRed else MdGreen, fontSize = 10.sp)
                                        }
                                        FaText(injury.effectFa, color = MdMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                                        FaText(
                                            if (qf.isNotEmpty()) "Quick Fix: ${qf.joinToString(" یا ") { gtr("%1s DV%2s", it.skill, it.dv) }}" else "Quick Fix: ندارد (فقط Treatment)",
                                            color = MdWhite, fontSize = 11.sp, modifier = Modifier.fillMaxWidth()
                                        )
                                        FaText(
                                            if (tr.isNotEmpty()) "Treatment: ${tr.joinToString(" یا ") { gtr("%1s DV%2s", it.skill, it.dv) }} • ۴ ساعت" else "Treatment جدا لازم ندارد (Quick Fix اثر را دائمی برمی‌دارد)",
                                            color = MdWhite, fontSize = 11.sp, modifier = Modifier.fillMaxWidth()
                                        )
                                        // Cybertech وقتی اندام جایگزین‌شده باشد
                                        val cyberOk = patient.inventory.any { it.equipped && it.category.equals("Cyberware", true) }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { rules { result = viewModel.careForInjury(medtech.id, patient.id, key, treatment = false, preferCybertech = preferCyber) } },
                                                enabled = (qf.isNotEmpty() || preferCyber) && !patient.isDead,
                                                modifier = Modifier.weight(1f)
                                            ) { Text(gtr("QUICK FIX (۱ دقیقه)"), color = MdRed, fontSize = 10.sp) }
                                            OutlinedButton(
                                                onClick = { rules { result = viewModel.careForInjury(medtech.id, patient.id, key, treatment = true, preferCybertech = preferCyber) } },
                                                enabled = (tr.isNotEmpty() || preferCyber) && !patient.isDead,
                                                modifier = Modifier.weight(1f)
                                            ) { Text(gtr("TREATMENT (۴ ساعت)"), color = MdGreen, fontSize = 10.sp) }
                                        }
                                        if (cyberOk) {
                                            HelperToggle("جایگزینی با Cybertech (اندام سایبری)", preferCyber) { preferCyber = it }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── ۴) Pharmaceutical ──
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MdCardSoft), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FaText("۴. Pharmaceutical — ساخت و تزریق", color = MdRed, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        FaText(
                            "DV13 • ۲۰۰eb مواد • ۱ ساعت • تعداد dose در هر ساخت = Medical Tech ($medicalTech). یادگرفته‌های این Medtech: ${learned.ifEmpty { listOf("هیچ‌کدام") }.joinToString(" • ")}",
                            color = MdWhite, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true
                        )
                        RoleAssistantData.corePharmaceuticals.forEach { drug ->
                            val isLearned = learned.any { it.equals(drug.name, true) }
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF14181F)), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(drug.name, color = if (isLearned) MdWhite else MdMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        if (!isLearned) Text("یادنگرفته", color = MdAmber, fontSize = 10.sp)
                                    }
                                    FaText(drug.effectFa, color = MdMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                                    FaText(drug.limitFa, color = MdMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        onClick = {
                                            rules {
                                                result = viewModel.administerPharmaceutical(
                                                    patientId = patient.id, medtechId = medtech.id, drugName = drug.name,
                                                    addictionRisk = addictionRisk, addictionName = drug.name
                                                )
                                            }
                                        },
                                        enabled = !patient.isDead,
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isLearned) MdRed else Color(0xFF37474F)),
                                        shape = CutCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(gtr("ساخت و تزریق %1s", drug.name), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                                }
                            }
                        }
                        HelperToggle("تست اعتیاد هم بزن (فقط داروهای اعتیادآور)", addictionRisk) { addictionRisk = it }
                    }
                }
            }

            // ── ۵) بازیابی ──
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MdCardSoft), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FaText("۵. Recovery — بازیابی HP", color = MdRed, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        FaText(
                            "با استراحت کامل روزی ${MedtechCareRules.naturalRecoveryPerDay(patient)} HP (BODY بیمار) برمی‌گردد. HP و جراحت‌ها دو خط جدا هستند: HP بالا می‌رود ولی جراحت درمان‌نشده اثرش را نگه می‌دارد.",
                            color = MdWhite, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true
                        )
                        val injured = MedtechCareRules.activityDamageInjuries(patient)
                        if (injured.isNotEmpty()) {
                            FaText(
                                "⚠ ${injured.joinToString(" • ")} هنوز فعال است: اگر بیمار در آن روز بیش از ۴ متر/یارد پیاده حرکت کند، آسیب دوباره مستقیم به HP می‌خورد.",
                                color = MdAmber, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true
                            )
                        }
                        HelperToggle("بیمار مریض/بی‌اشتهاست (بازیابی نصف می‌شود)", fasting) { fasting = it }
                        Button(
                            onClick = { result = viewModel.applyRecoveryDay(patient.id, fasting) },
                            colors = ButtonDefaults.buttonColors(containerColor = MdGreen),
                            shape = CutCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (fasting) "ثبت یک روز بیماری (نصف بازیابی)" else "ثبت یک روز استراحت کامل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    }
                }
            }

            // ── ۶) نتیجه ──
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MdCardColor), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FaText("نتیجه‌ی آخرین اقدام", color = MdRed, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (result != null) Text(gtr("پاک کردن"), color = MdMuted, fontSize = 11.sp, modifier = Modifier.clickable { result = null })
                        }
                        FaText(
                            result ?: "هنوز اقدامی انجام نشده. یک اقدام بالا انتخاب کن؛ نتیجه و فرمول کامل همین‌جا نوشته می‌شود.",
                            color = if (result == null) MdMuted else MdWhite, fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(), justify = true
                        )
                    }
                }
            }
        }

        // ── ۷) فهرست بیماران این Medtech (تابلوی منتقل‌شده از تب BIO) ──
        item {
            MedtechPatientBoard(medtech = medtech, viewModel = viewModel, characters = characters)
        }

        // ── ۸) ابزار صحنه و مرجع ──
        item { CharacterSheetLifepathCard(medtech, "Medtech") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MdCardColor), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    FaText("قواعد درمان در یک نگاه", color = MdRed, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.fillMaxWidth())
                    // هر بند یک تیتر کوتاه دارد و متنش راست‌نویس و روان است.
                    MedtechCareRules.quickGuide.forEach { (title, body) ->
                        RoleInnerBox {
                            FaText(title, color = MdWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                            FaText(body, color = MdMuted, fontSize = 11.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                        }
                    }
                    RoleAssistantData.cryoBenefits.filter { it.level <= cryo }.forEach {
                        FaText(gtr("Cryo %1s: %2s", it.level, it.textFa), color = MdMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    }
                }
            }
        }
        item {
            HelperCard("بیمار در چه وضعی است؟", "وضعیت را انتخاب کن؛ مسیر درست درمان و تأثیر شرایط صحنه را می‌گوید.") {
                PickerRows(RoleHelperData.medtechSituations, medSituation) { medSituation = it }
                Spacer(Modifier.height(2.dp))
                HelperToggle("Medtech Bag و تجهیزات هست", medBag) { medBag = it }
                HelperToggle("محل امن است", medSafe) { medSafe = it }
                HelperToggle("زمان کم است", medRush) { medRush = it }
                Spacer(Modifier.height(2.dp))
                val (verdict, tone) = RoleHelperData.medtechAdvice(medSituation, medBag, medSafe, medRush)
                com.cyberpunk.gmtool.ui.components.DecisionBox(verdict, tone)
            }
        }
        item {
            HelperCard("ساخت صحنه‌ی Medtech") {
                RollBox("یک بیمار بساز", RoleHelperData.medtechPatients, "برای وقتی که همین حالا یک صحنه لازم داری.")
                Spacer(Modifier.height(6.dp))
                RollBox("یک پیچیدگی اضافه کن", RoleHelperData.medtechComplications)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MdCardColor), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    FaText("فرصت درخشش Medtech", color = MdRed, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.fillMaxWidth())
                    FaText(
                        "• درمان را به یک دکمه‌ی Heal تقلیل نده؛ انتخاب بین Stabilize، Quick Fix، Surgery و بیمارستان خودش صحنه است.\n• هر جراحت یک پنجره‌ی زمانی دارد: خون‌ریزی، دنده، شیء خارجی و آسیب نخاع هرکدام تصمیم متفاوتی می‌خواهند.\n• کمبود دارو یا ابزار می‌تواند مسئله‌ی داستانی باشد، اما Modifier یا هزینه‌ی خارج از Core تحمیل نکن.",
                        color = MdMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), justify = true
                    )
                }
            }
        }
    }
}

/** خلاصه‌ی وضعیت بیمار — همان چیزی که در تب COMBAT نوشته می‌شود. */
@Composable
private fun PatientVitalsCard(p: Character) {
    val mortally = GameRules.isMortallyWounded(p.hp)
    val seriously = GameRules.isSeriouslyWounded(p.hp, p.maxHp)
    val ratio = if (p.maxHp > 0) (p.hp.toFloat() / p.maxHp).coerceIn(0f, 1f) else 0f
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF14181F)), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(gtr("%1s • HP %2s/%3s", p.handle.ifBlank { p.name }, p.hp, p.maxHp), color = MdWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                // Core فقط سه وضعیت زخم دارد (Lightly / Seriously / Mortally)؛
                // کاراکتری که HP کامل دارد «زخمی» نیست و نباید Lightly Wounded
                // برچسب بخورد.
                Text(
                    when {
                        p.isDead -> "مرده"
                        mortally -> "در آستانه‌ی مرگ (Mortally Wounded)"
                        seriously -> "زخم شدید (Seriously Wounded)"
                        p.hp < p.maxHp -> "زخم سطحی (Lightly Wounded)"
                        else -> "سالم"
                    },
                    color = if (p.isDead || mortally) MdRed else if (seriously) MdAmber else if (p.hp < p.maxHp) MdAmber else MdGreen,
                    fontWeight = FontWeight.Bold, fontSize = 10.sp
                )
            }
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF2A2A2A), CutCornerShape(4.dp))) {
                Box(
                    Modifier.fillMaxWidth(ratio).height(8.dp)
                        .background(if (mortally || p.isDead) MdRed else MdAmber, CutCornerShape(4.dp))
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    gtr("BODY %1s • جریمه‌ی Death Save %2s • جریمه‌ی همه‌ی Actionها %3s • جراحت بحرانی %4s مورد",
                        p.stats.body, p.deathSavePenalty, CombatRules.allActionsPenalty(p), p.criticalInjuries.size),
                    color = MdMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
            }
        }
    }
}

/**
 * تابلوی بیماران Medtech — از تب BIO منتقل شد.
 * از این نسخه، هر ردیف می‌تواند به یک کاراکتر واقعی وصل شود.
 */
@Composable
private fun MedtechPatientBoard(medtech: Character, viewModel: CharacterViewModel, characters: List<Character>) {
    val rows = medtech.medtechPatients
    // مقدار ذخیره‌شده همان کلید انگلیسی می‌ماند (سازگاری با داده‌ی قبلی)،
    // ولی روی صفحه برچسب فارسی خوانده می‌شود.
    val states = listOf("Critical", "Stabilized", "Surgery", "Cryo", "Recovering")
    val stateFa = mapOf(
        "Critical" to "بحرانی", "Stabilized" to "پایدارشده", "Surgery" to "نیاز به جراحی",
        "Cryo" to "کرایو", "Recovering" to "در حال بهبود"
    )
    val stateColor = mapOf(
        "Critical" to MdRed, "Stabilized" to MdAmber, "Surgery" to Color(0xFF29B6F6),
        "Cryo" to Color(0xFF4FC3F7), "Recovering" to MdGreen
    )
    var pickId by remember { mutableStateOf<Int?>(null) }
    var pickMenu by remember { mutableStateOf(false) }
    val linkable = characters.filter { it.id != medtech.id }

    Card(colors = CardDefaults.cardColors(containerColor = MdCardColor), modifier = Modifier.fillMaxWidth()) {
      // کل تابلوی بیماران راست‌نویس است: فرم فارسی وقتی چپ‌چین باشد خواندنش سخت است.
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FaText("بیماران ثبت‌شده‌ی این Medtech", color = MdRed, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val linked = pickId?.let { id -> characters.firstOrNull { it.id == id } }
                        viewModel.addMedtechPatient(
                            medtech.id,
                            name = linked?.handle?.ifBlank { linked.name } ?: "",
                            linkedCharacterId = pickId
                        )
                        pickId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MdRed),
                    shape = CutCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text(gtr("+ بیمار"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
            FaText(
                "برای بیمار بازیکن‌ها از «اتصال به کاراکتر» استفاده کن؛ آن‌وقت HP و Critical Injuryها از خود برگه می‌آید و درمان هم روی همان اعمال می‌شود. برای NPC بدون برگه، اسم را دستی بنویس.",
                color = MdMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("اتصال به کاراکتر:", color = MdMuted, fontSize = 11.sp)
                Surface(color = Color(0xFF1E1E1E), shape = CutCornerShape(5.dp), modifier = Modifier.clickable { pickMenu = true }) {
                    Text(
                        pickId?.let { id -> characters.firstOrNull { it.id == id }?.handle?.ifBlank { characters.first { c -> c.id == id }.name } } ?: "بدون اتصال (NPC)",
                        color = MdWhite, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                androidx.compose.material3.DropdownMenu(expanded = pickMenu, onDismissRequest = { pickMenu = false }) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("بدون اتصال (NPC)") }, onClick = { pickId = null; pickMenu = false })
                    linkable.forEach { c ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(gtr("%1s%2s", c.handle.ifBlank { c.name }, if (c.isAlly) "" else " (NPC)")) },
                            onClick = { pickId = c.id; pickMenu = false }
                        )
                    }
                }
            }

            if (rows.isEmpty()) FaText("هنوز بیماری ثبت نشده.", color = MdMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())

            rows.forEach { pt ->
                val linked = pt.linkedCharacterId?.let { id -> characters.firstOrNull { it.id == id } }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF14181F)), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = pt.name,
                                onValueChange = { v -> viewModel.updateMedtechPatient(medtech.id, pt.id) { it.copy(name = v) } },
                                placeholder = { Text(gtr("نام بیمار"), fontSize = 13.sp, textAlign = TextAlign.Right) },
                                singleLine = true, textStyle = MdRtl,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MdRed, unfocusedBorderColor = MdRed.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = MdRed
                                )
                            )
                            IconButton(onClick = { viewModel.deleteMedtechPatient(medtech.id, pt.id) }) {
                                Icon(Icons.Default.Close, contentDescription = gtr("حذف بیمار"), tint = MdMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (linked != null) {
                            PatientVitalsCard(linked)
                        } else {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text("بدون اتصال به کاراکتر — HP و جراحت‌ها دستی پیگیری می‌شوند.", color = MdMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            states.forEach { st ->
                                val on = pt.state == st
                                Surface(
                                    color = if (on) (stateColor[st] ?: MdRed) else Color(0xFF1E1E1E),
                                    shape = CutCornerShape(5.dp),
                                    modifier = Modifier.clickable {
                                        viewModel.updateMedtechPatient(medtech.id, pt.id) { it.copy(state = st, inCryo = (st == "Cryo")) }
                                    }
                                ) {
                                    Text(stateFa[st] ?: st, color = if (on) Color.Black else MdMuted, fontSize = 11.sp,
                                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                        OutlinedTextField(
                            value = pt.injury,
                            onValueChange = { v -> viewModel.updateMedtechPatient(medtech.id, pt.id) { it.copy(injury = v) } },
                            label = { Text(gtr("آسیب فعلی (مثلاً زخم شدید یا اندام از‌دست‌رفته)"), textAlign = TextAlign.Right) },
                            singleLine = true, textStyle = MdRtl, modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MdRed, unfocusedBorderColor = MdRed.copy(alpha = 0.3f),
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = MdRed,
                                focusedLabelColor = MdRed, unfocusedLabelColor = MdMuted
                            )
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MdCounter(gtr("جراحی موفق"), pt.surgerySuccess, Modifier.weight(1f)) { d ->
                                viewModel.updateMedtechPatient(medtech.id, pt.id) { it.copy(surgerySuccess = (it.surgerySuccess + d).coerceAtLeast(0)) }
                            }
                            MdCounter(gtr("روز تا بهبودی"), pt.daysLeft, Modifier.weight(1f)) { d ->
                                viewModel.updateMedtechPatient(medtech.id, pt.id) { it.copy(daysLeft = (it.daysLeft + d).coerceAtLeast(0)) }
                            }
                        }
                        OutlinedTextField(
                            value = pt.note,
                            onValueChange = { v -> viewModel.updateMedtechPatient(medtech.id, pt.id) { it.copy(note = v) } },
                            label = { Text(gtr("یادداشت"), textAlign = TextAlign.Right) },
                            minLines = 2, textStyle = MdRtl, modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MdRed, unfocusedBorderColor = MdRed.copy(alpha = 0.3f),
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = MdRed,
                                focusedLabelColor = MdRed, unfocusedLabelColor = MdMuted
                            )
                        )
                    }
                }
            }
        }
    }
      }
}

@Composable
private fun MdCounter(label: String, value: Int, modifier: Modifier = Modifier, onDelta: (Int) -> Unit) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF14181F)), shape = CutCornerShape(8.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FaText(label, color = MdMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
            Text("$value", color = MdWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = Color(0xFF1E1E1E), shape = CutCornerShape(4.dp), modifier = Modifier.clickable { onDelta(-1) }) {
                    Text("−", color = MdRed, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                }
                Surface(color = Color(0xFF1E1E1E), shape = CutCornerShape(4.dp), modifier = Modifier.clickable { onDelta(1) }) {
                    Text("+", color = MdGreen, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                }
            }
        }
    }
}
