package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.SkillCatalog
import com.cyberpunk.gmtool.data.Stats
import com.cyberpunk.gmtool.data.StreetratData
import com.cyberpunk.gmtool.ui.components.CoreItemDetailDialog
import com.cyberpunk.gmtool.ui.components.RuleInfoButton

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)
private val CardBg = Color(0xFF1A1A1A)

const val TOTAL_STAT_POINTS = 62
const val TOTAL_SKILL_POINTS = 86
const val STAT_MIN = 2
const val STAT_MAX = 8
const val SKILL_MAX = 6

private val parameterizedSkillRandomPools = mapOf(
    "Language" to listOf("Japanese", "Mandarin", "Spanish", "French", "German", "Russian", "Korean", "Arabic", "Portuguese", "Italian"),
    "Local Expert" to listOf("City Center", "Watson", "Westbrook", "Heywood", "Santo Domingo", "Pacifica", "South Night City", "The Glen", "Little Europe", "Combat Zone"),
    "Science" to listOf("Chemistry", "Biology", "Physics", "Genetics", "Cybernetics", "Pharmacology", "Forensics", "Materials Science", "Computer Science", "Environmental Science"),
    "Play Instrument" to listOf("Guitar", "Bass", "Drums", "Keyboard", "Violin", "Saxophone", "Trumpet", "Synthesizer", "Cello", "Percussion")
)

private fun randomParameterizedSkillDetail(type: String): String =
    parameterizedSkillRandomPools[type]?.randomOrNull().orEmpty()

private fun parameterizedSkillTypeFa(type: String): String = when (type) {
    "Language" -> "زبان"
    "Local Expert" -> "دانش محلی"
    "Science" -> "علوم"
    "Play Instrument" -> "نوازندگی"
    else -> type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterBuilderScreen(
    methodName: String,
    roleName: String,
    onBack: () -> Unit,
    /**
     * بررسی یکتایی handle در فهرست شخصیت‌ها؛ مثل صفحه‌ی Streetrat.
     * گزارش تست ۴.۲: معیار یکتایی فقط Handle است و نام تکراری اشکالی ندارد.
     */
    isHandleTaken: (String) -> Boolean = { _ -> false },
    /** (legalName, streetHandle, stats, skillLevels, selectedChoices) */
    onCreate: (String, String, Stats, Map<String, Int>, List<String>) -> Unit
) {
    val isCompletePackage = methodName.contains("Complete", ignoreCase = true)
    val context = LocalContext.current
    var itemDetailLabel by remember { mutableStateOf<String?>(null) }
    var selectedInfoStat by remember { mutableStateOf<String?>(null) }
    var selectedInfoSkill by remember { mutableStateOf<String?>(null) }

    // ── هویت ──
    // قبلاً Edgerunner و Complete Package بدون پرسیدن نام و Handle شخصیت
    // می‌ساختند (handle خودکارِ «<نقش> Edgerunner»): هم فهرست پر از
    // اسم‌های یکسان می‌شد، هم LAN هیچ راهی برای تشخیص برگه‌ها نداشت.
    var legalName by remember { mutableStateOf("") }
    var streetHandle by remember { mutableStateOf("") }

    // --- استت‌ها ---
    var stats by remember {
        mutableStateOf(
            if (isCompletePackage) Stats(6, 6, 6, 6, 6, 6, 6, 6, 6, 6)
            else StreetratData.rollEdgerunnerStats(roleName)
        )
    }

    fun rerollAllEdgerunner() {
        stats = StreetratData.rollEdgerunnerStats(roleName)
    }

    val statTotal = listOf(
        stats.int, stats.ref, stats.dex, stats.tech, stats.cool,
        stats.will, stats.luck, stats.move, stats.body, stats.emp
    ).sum()

    fun adjustStat(statKey: String, delta: Int) {
        if (!isCompletePackage) return // در Edgerunners استت‌ها با تاس تعیین می‌شوند
        stats = when (statKey) {
            "INT" -> stats.copy(int = (stats.int + delta).coerceIn(STAT_MIN, STAT_MAX))
            "REF" -> stats.copy(ref = (stats.ref + delta).coerceIn(STAT_MIN, STAT_MAX))
            "DEX" -> stats.copy(dex = (stats.dex + delta).coerceIn(STAT_MIN, STAT_MAX))
            "TECH" -> stats.copy(tech = (stats.tech + delta).coerceIn(STAT_MIN, STAT_MAX))
            "COOL" -> stats.copy(cool = (stats.cool + delta).coerceIn(STAT_MIN, STAT_MAX))
            "WILL" -> stats.copy(will = (stats.will + delta).coerceIn(STAT_MIN, STAT_MAX))
            "LUCK" -> stats.copy(luck = (stats.luck + delta).coerceIn(STAT_MIN, STAT_MAX))
            "MOVE" -> stats.copy(move = (stats.move + delta).coerceIn(STAT_MIN, STAT_MAX))
            "BODY" -> stats.copy(body = (stats.body + delta).coerceIn(STAT_MIN, STAT_MAX))
            "EMP" -> stats.copy(emp = (stats.emp + delta).coerceIn(STAT_MIN, STAT_MAX))
            else -> stats
        }
    }

    // --- مهارت‌ها ---
    val baseAllowedSkillNames = remember(roleName) {
        if (isCompletePackage) SkillCatalog.allSkillNames()
        else SkillCatalog.allowedSkillsForRole(roleName)
    }
    val customSkillNames = remember { mutableStateListOf<String>() }
    val allowedSkillNames = (baseAllowedSkillNames + customSkillNames).distinct().sorted()
    var showCustomSkillDialog by remember { mutableStateOf(false) }
    var customSkillType by remember { mutableStateOf("Language") }
    var customSkillDetail by remember { mutableStateOf(randomParameterizedSkillDetail("Language")) }

    val skillLevels = remember { mutableStateMapOf<String, Int>() }
    val pendingChoices = remember(roleName, isCompletePackage) {
        if (isCompletePackage) emptyList() else StreetratData.getEquipmentForRole(roleName).pendingChoices
    }
    val selectedEquipmentChoices = remember { mutableStateMapOf<Int, String>() }

    // تضمین حداقل ۲ در ۱۳ مهارت پایه
    LaunchedEffect(Unit) {
        StreetratData.baseLifeSkills.forEach { skill ->
            if (skillLevels[skill] == null) skillLevels[skill] = 2
        }
    }

    val spentPoints = skillLevels.entries.sumOf { (name, level) -> SkillCatalog.creationPointCost(name, level) }
    val remainingSkillPoints = TOTAL_SKILL_POINTS - spentPoints

    fun adjustSkill(name: String, delta: Int) {
        val current = skillLevels[name] ?: 0
        val next = (current + delta).coerceAtLeast(0)
        if (delta > 0) {
            if (next > SKILL_MAX) return
            val stepCost = if (SkillCatalog.isDifficult(name)) 2 else 1
            if (remainingSkillPoints < stepCost) {
                Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("امتیاز مهارت کافی نیست!"), Toast.LENGTH_SHORT).show()
                return
            }
        }
        // در هر سه روش ساخت، ۱۳ مهارت پایه باید حداقل Level 2 باشند.
        if (StreetratData.baseLifeSkills.contains(name) && next < 2) return
        skillLevels[name] = next
    }

    Scaffold(
        topBar = {
            CyberpunkHeader(title = gtr("Build: %1s", roleName), onBackClick = onBack)
        },
        containerColor = Black
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // ===== بخش هویت =====
                item {
                    SectionHeader("IDENTITY")
                    OutlinedTextField(
                        value = legalName,
                        onValueChange = { legalName = it },
                        label = { Text(gtr("Legal Name (Optional)"), color = Muted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Red,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        shape = CutCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = streetHandle,
                        onValueChange = { streetHandle = it },
                        label = { Text(gtr("Street Handle (Required)"), color = Red) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Red,
                            unfocusedBorderColor = Red.copy(alpha = 0.5f),
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        shape = CutCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ===== بخش استت‌ها =====
                item {
                    SectionHeader("STATS", "character.stats")
                    if (isCompletePackage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "امتیاز استت باقی‌مانده: ",
                                color = Muted, fontSize = 14.sp
                            )
                            Text(
                                "${TOTAL_STAT_POINTS - statTotal}",
                                color = if (statTotal == TOTAL_STAT_POINTS) Red else Color.Yellow,
                                fontWeight = FontWeight.Bold, fontSize = 18.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text("(هر استت، از جمله LUCK، بین $STAT_MIN و $STAT_MAX)", color = Muted, fontSize = 11.sp)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("استت‌ها با تاس 1d10 برای هر کدام تعیین می‌شوند.", color = Muted, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { rerollAllEdgerunner() },
                                colors = ButtonDefaults.buttonColors(containerColor = Red),
                                shape = CutCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = gtr("Reroll"), tint = Black, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(gtr("REROLL"), color = Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                val statRows = listOf(
                    Triple("INT", stats.int, "Intelligence"),
                    Triple("REF", stats.ref, "Reflexes"),
                    Triple("DEX", stats.dex, "Dexterity"),
                    Triple("TECH", stats.tech, "Technique"),
                    Triple("COOL", stats.cool, "Cool"),
                    Triple("WILL", stats.will, "Willpower"),
                    Triple("LUCK", stats.luck, "Luck"),
                    Triple("MOVE", stats.move, "Movement"),
                    Triple("BODY", stats.body, "Body"),
                    Triple("EMP", stats.emp, "Empathy")
                )
                items(statRows) { (key, value, label) ->
                    StatRow(key, label, value, isCompletePackage, onInfo = { selectedInfoStat = key }) { delta -> adjustStat(key, delta) }
                }

                // ===== بخش مهارت‌ها =====
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader("SKILLS", "character.skills")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("امتیاز مهارت باقی‌مانده: ", color = Muted, fontSize = 14.sp)
                        Text(
                            "$remainingSkillPoints",
                            color = if (remainingSkillPoints == 0) Red else Color.Yellow,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text("(سقف هر مهارت $SKILL_MAX در شروع)", color = Muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (isCompletePackage) {
                        OutlinedButton(
                            onClick = {
                                if (customSkillDetail.isBlank()) customSkillDetail = randomParameterizedSkillDetail(customSkillType)
                                showCustomSkillDialog = true
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Red)
                            Spacer(Modifier.width(6.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    "افزودن مهارت پارامتری (زبان / دانش محلی / علوم / نوازندگی)",
                                    color = Red,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                items(allowedSkillNames, key = { it }) { name ->
                    val lvl = skillLevels[name] ?: 0
                    SkillAllocRow(
                        name = name,
                        level = lvl,
                        statName = SkillCatalog.statFor(name),
                        onDec = { adjustSkill(name, -1) },
                        onInc = { adjustSkill(name, 1) },
                        onInfo = { selectedInfoSkill = name }
                    )
                }

                if (pendingChoices.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(18.dp))
                        SectionHeader("STARTING PACKAGE CHOICES", "character.creation_methods")
                        Text("برای هر گروه دقیقاً یک گزینه را انتخاب کن.", color = Muted, fontSize = 12.sp)
                    }
                    items(pendingChoices.indices.toList(), key = { "choice-$it" }) { index ->
                        val group = pendingChoices[index]
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(gtr(group.category), color = White, fontWeight = FontWeight.Bold)
                            group.options.forEach { option ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { selectedEquipmentChoices[index] = option }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedEquipmentChoices[index] == option,
                                        onClick = { selectedEquipmentChoices[index] = option }
                                    )
                                    Text(option.replace("||", " + "), color = White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { itemDetailLabel = option }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                        Text("ⓘ", color = Red, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            // دکمه ساخت
            Surface(color = Black, tonalElevation = 8.dp) {
                Button(
                    onClick = {
                        val ready = if (isCompletePackage) {
                            statTotal == TOTAL_STAT_POINTS
                        } else true
                        if (streetHandle.isBlank()) {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("Handle is required in Night City!"), Toast.LENGTH_SHORT).show()
                        } else if (isHandleTaken(streetHandle)) {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("یک شخصیت دیگر با همین Handle در فهرست هست؛ Handle را عوض کن. (نام تکراری اشکالی ندارد)"), Toast.LENGTH_LONG).show()
                        } else if (!ready) {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("باید همه‌ی ۶۲ امتیاز استت توزیع شود."), Toast.LENGTH_LONG).show()
                        } else if (StreetratData.baseLifeSkills.any { (skillLevels[it] ?: 0) < 2 }) {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("۱۳ مهارت پایه باید حداقل Level 2 باشند."), Toast.LENGTH_LONG).show()
                        } else if (remainingSkillPoints != 0) {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("باید هر ۸۶ امتیاز مهارت توزیع شود (باقی‌مانده: $remainingSkillPoints)."), Toast.LENGTH_LONG).show()
                        } else if (pendingChoices.indices.any { selectedEquipmentChoices[it].isNullOrBlank() }) {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("همه‌ی انتخاب‌های Starter Package را مشخص کن."), Toast.LENGTH_LONG).show()
                        } else {
                            onCreate(
                                legalName.trim(), streetHandle.trim(),
                                stats, skillLevels.toMap(),
                                pendingChoices.indices.mapNotNull { selectedEquipmentChoices[it] }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text(gtr("CREATE CHARACTER"), color = Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }

    if (showCustomSkillDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showCustomSkillDialog = false },
                title = {
                    Text(
                        "افزودن مهارت پارامتری",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "نوع مهارت را انتخاب کن. مقدار پیشنهادی از قبل پر شده و با دکمهٔ تاس می‌توانی یک گزینهٔ تصادفی دیگر انتخاب کنی؛ در صورت نیاز می‌توانی متن را هم ویرایش کنی.",
                            color = Muted,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right,
                            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl)
                        )
                        Spacer(Modifier.height(12.dp))

                        val types = listOf("Language", "Local Expert", "Science", "Play Instrument")
                        types.chunked(2).forEach { rowTypes ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTypes.forEach { type ->
                                    FilterChip(
                                        selected = customSkillType == type,
                                        onClick = {
                                            customSkillType = type
                                            customSkillDetail = randomParameterizedSkillDetail(type)
                                        },
                                        label = {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(parameterizedSkillTypeFa(type), fontSize = 12.sp)
                                                Text(gtr(type), fontSize = 9.sp, color = Muted)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customSkillDetail,
                                onValueChange = { customSkillDetail = it },
                                label = { Text("مقدار مهارت") },
                                placeholder = { Text("مثلاً Japanese / Night City / Chemistry / Guitar") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    textAlign = TextAlign.Start,
                                    textDirection = TextDirection.ContentOrRtl
                                )
                            )
                            FilledTonalIconButton(
                                onClick = { customSkillDetail = randomParameterizedSkillDetail(customSkillType) },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = "انتخاب تصادفی", tint = Red)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "انتخاب فعلی: ${parameterizedSkillTypeFa(customSkillType)} — $customSkillDetail",
                            color = White,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right,
                            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val detail = customSkillDetail.trim().ifBlank { randomParameterizedSkillDetail(customSkillType) }
                        if (detail.isNotBlank()) {
                            val full = "$customSkillType ($detail)"
                            if (!customSkillNames.contains(full)) customSkillNames += full
                            skillLevels.putIfAbsent(full, 0)
                            customSkillDetail = randomParameterizedSkillDetail(customSkillType)
                            showCustomSkillDialog = false
                        }
                    }) { Text("افزودن") }
                },
                dismissButton = { TextButton(onClick = { showCustomSkillDialog = false }) { Text("انصراف") } }
            )
        }
    }
    itemDetailLabel?.let { label ->
        CoreItemDetailDialog(label = label, onDismiss = { itemDetailLabel = null })
    }
    selectedInfoStat?.let { key ->
        StatInfoDialog(statKey = key, cyberRed = Red, cyberBlack = Black, onDismiss = { selectedInfoStat = null })
    }
    selectedInfoSkill?.let { name ->
        SkillInfoDialog(skillName = name, cyberRed = Red, cyberBlack = Black, onDismiss = { selectedInfoSkill = null })
    }
}

@Composable
private fun SectionHeader(title: String, referenceKey: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(gtr(title), color = Red, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (referenceKey != null) RuleInfoButton(referenceKey)
    }
    HorizontalDivider(color = Red.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun StatRow(key: String, label: String, value: Int, editable: Boolean, onInfo: () -> Unit, onAdjust: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(CardBg, CutCornerShape(6.dp)).border(1.dp, Color(0xFF333333), CutCornerShape(6.dp)).clickable { onInfo() }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(gtr(key), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(56.dp))
        Text(gtr(label), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (editable) {
            StepperButton("−") { onAdjust(-1) }
            Text("$value", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            StepperButton("+") { onAdjust(1) }
        } else {
            Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                Text("$value", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun SkillAllocRow(name: String, level: Int, statName: String, onDec: () -> Unit, onInc: () -> Unit, onInfo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .background(if (level > 0) CardBg else Color(0xFF120505), CutCornerShape(6.dp))
            .clickable { onInfo() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(gtr(name), color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(gtr(statName), color = Muted, fontSize = 11.sp)
        }
        StepperButton("−", onDec)
        Text("$level", color = if (level > 0) Red else Muted, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        StepperButton("+", onInc)
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp).background(Color(0xFF2A2A2A), CutCornerShape(6.dp))
            .border(1.dp, Red.copy(alpha = 0.5f), CutCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(gtr(label), color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
