package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.DiceSource
import com.cyberpunk.gmtool.data.RoleAbilitiesData
import com.cyberpunk.gmtool.data.SkillCatalog
import com.cyberpunk.gmtool.data.SkillData
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val DarkRedBg = Color(0xFF250A0A)

@Composable
fun StatsTab(character: Character, viewModel: CharacterViewModel) {
    var selectedInfoStat by remember { mutableStateOf<String?>(null) }
    var statToRoll by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var skillToRoll by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var selectedInfoSkill by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isGrouped by remember { mutableStateOf(false) }
    var hideZeroes by remember { mutableStateOf(false) }
    var isDense by remember { mutableStateOf(false) }

    // حالت ادیت: null = بسته، "stat" / "skill" / "role" = باز
    var editMode by remember { mutableStateOf<String?>(null) }

    // استات‌های واقعی کاراکتر
    val coreStats = listOf(
        Triple("INT", character.stats.int, com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "INT", character.stats.int)),
        Triple("REF", character.stats.ref, com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref)),
        Triple("DEX", character.stats.dex, com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex)),
        Triple("TECH", character.stats.tech, character.stats.tech),
        Triple("COOL", character.stats.cool, character.stats.cool),
        Triple("WILL", character.stats.will, character.stats.will),
        Triple("LUCK", character.currentLuck, character.maxLuck),
        Triple("MOVE", character.stats.move, com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "MOVE", character.stats.move)),
        Triple("BODY", character.stats.body, character.stats.body),
        Triple("EMP", character.stats.emp, character.maxHumanity / 10)
    )

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
     Column(modifier = Modifier.fillMaxSize()) {
        // نوار ۱۰تایی استات‌ها
        Row(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            coreStats.forEachIndexed { index, stat ->
                val isDarkBg = index % 2 != 0
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(if (isDarkBg) DarkRedBg else Color.Black),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.weight(1.3f).fillMaxWidth().clickable { selectedInfoStat = stat.first },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(gtr(stat.first), color = Color.LightGray, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.rotate(90f))
                    }
                    HorizontalDivider(color = Color(0xFF150505), thickness = 2.dp)
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().clickable {
                            // برای تاس، مقدار فعلی را می‌فرستیم
                            statToRoll = stat.first to (if (stat.first == "LUCK") stat.second else stat.third)
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        if (stat.first == "LUCK") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stat.second}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                HorizontalDivider(color = Color.White, modifier = Modifier.width(14.dp).padding(vertical = 1.dp))
                                Text("${stat.third}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("${stat.third}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Red.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(gtr("Search"), color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = gtr("Search"), tint = Color.LightGray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Default.Close, contentDescription = gtr("Clear"), tint = Color.LightGray, modifier = Modifier.clickable { searchQuery = "" })
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = Red, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterSwitch("Group", isGrouped) { isGrouped = it }
            FilterSwitch("Dense", isDense) { isDense = it }
            FilterSwitch("Hide Zeroes", hideZeroes) { hideZeroes = it }
        }

        // فهرست کاملِ همه‌ی مهارت‌ها (از کاتالوگ)؛ مهارت‌های کاراکتر روی آن منطبق می‌شوند
        val ownedByName = character.skills.associateBy { it.name }
        val allSkills = com.cyberpunk.gmtool.data.SkillCatalog.allSkillNames().map { name ->
            ownedByName[name] ?: com.cyberpunk.gmtool.data.SkillData(name = name, stat = com.cyberpunk.gmtool.data.SkillCatalog.statFor(name), level = 0)
        }

        val filtered = allSkills.filter { skill ->
            val matchZero = if (hideZeroes) skill.level > 0 else true
            val matchSearch = if (searchQuery.isNotEmpty()) skill.name.contains(searchQuery, ignoreCase = true) else true
            matchZero && matchSearch
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (isGrouped) {
                    val grouped = filtered.groupBy { it.stat }
                    grouped.forEach { (statName, list) ->
                        item { GroupHeader(statName, Red) }
                        items(list, key = { it.name }) { skill ->
                            SkillRow(character, skill, viewModel, dense = isDense,
                                onClick = { selectedInfoSkill = skill.name },
                                onRoll = { skillToRoll = skill.name to skillTotal(character, skill) })
                        }
                    }
                } else {
                    items(filtered, key = { it.name }) { skill ->
                        SkillRow(character, skill, viewModel, dense = isDense,
                            onClick = { selectedInfoSkill = skill.name },
                            onRoll = { skillToRoll = skill.name to skillTotal(character, skill) })
                    }
                }
                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }
     }

     // دکمه‌ی شناور ادیت — ثابت روی صفحه، درست بالای فوتر ناوبری
     FloatingActionButton(
         onClick = { editMode = "menu" },
         containerColor = Red,
         contentColor = Color.Black,
         shape = CircleShape,
         modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp).size(60.dp)
     ) {
         Icon(Icons.Default.Edit, contentDescription = gtr("Edit"), modifier = Modifier.size(26.dp))
     }
    }

    // منوی انتخاب نوع ادیت
    if (editMode == "menu") {
        EditMenuDialog(
            onDismiss = { editMode = null },
            onPick = { editMode = it }
        )
    }
    if (editMode == "stat") {
        EditStatsDialog(character = character, viewModel = viewModel, onDismiss = { editMode = null })
    }
    if (editMode == "skill") {
        EditSkillsDialog(character = character, viewModel = viewModel, onDismiss = { editMode = null })
    }
    if (editMode == "role") {
        EditRoleAbilityDialog(character = character, viewModel = viewModel, onDismiss = { editMode = null })
    }

    selectedInfoStat?.let { key ->
        StatInfoDialog(statKey = key, cyberRed = Red, cyberBlack = Black, onDismiss = { selectedInfoStat = null })
    }
    selectedInfoSkill?.let { name ->
        SkillInfoDialog(skillName = name, cyberRed = Red, cyberBlack = Black, onDismiss = { selectedInfoSkill = null })
    }
    statToRoll?.let { (name, value) ->
        StatRollDialog(statName = name, statValue = value, cyberRed = Red, cyberBlack = Black, onDismiss = { statToRoll = null })
    }
    skillToRoll?.let { (name, value) ->
        StatRollDialog(statName = name, statValue = value, cyberRed = Red, cyberBlack = Black, onDismiss = { skillToRoll = null })
    }
}

private fun statValueOf(character: Character, statName: String): Int = when (statName) {
    "Intelligence" -> com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "INT", character.stats.int)
    "Reflexes" -> com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref)
    "Dexterity" -> com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex)
    "Technique" -> character.stats.tech
    "Cool" -> character.stats.cool
    "Willpower" -> character.stats.will
    "Movement" -> com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "MOVE", character.stats.move)
    "Body" -> character.stats.body
    "Empathy" -> character.stats.emp
    else -> 0
}

private fun skillTotal(character: Character, skill: SkillData): Int {
    val armorPenalty = com.cyberpunk.gmtool.data.GameRules.armorPenalty(character.inventory)
    val statPenalty = if (skill.stat == "Reflexes" || skill.stat == "Dexterity") armorPenalty else 0
    val woundPenalty = if (com.cyberpunk.gmtool.data.GameRules.isSeriouslyWounded(character.hp, character.maxHp) && !com.cyberpunk.gmtool.data.StreetDrugRules.ignoresSeriouslyWounded(character)) -2 else 0
    val roleBonus = when {
        character.role.equals("Solo", true) && skill.name.equals("Perception", true) -> character.roleAbilityPoints["threatDetection"] ?: 0
        character.role.equals("Nomad", true) && skill.name in setOf(
            "Drive Land Vehicle", "Pilot Air Vehicle", "Pilot Sea Vehicle", "Air Vehicle Tech", "Land Vehicle Tech", "Sea Vehicle Tech"
        ) -> character.roleRank
        character.role.equals("Tech", true) && skill.name in setOf(
            "Basic Tech", "Cybertech", "Electronics/Security Tech", "Weaponstech", "Land Vehicle Tech", "Sea Vehicle Tech", "Air Vehicle Tech"
        ) -> character.roleAbilityPoints["field"] ?: 0
        else -> 0
    }
    val drugBonus = com.cyberpunk.gmtool.data.StreetDrugRules.skillModifier(character, skill.name)
    val gearBonus = com.cyberpunk.gmtool.data.EquipmentUseRules.gearSkillBonus(character, skill.name)
    return skill.level + statValueOf(character, skill.stat) + statPenalty + woundPenalty + roleBonus + drugBonus + gearBonus
}

@Composable
private fun FilterSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(gtr(label), color = Color.LightGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = cprSwitchColors())
    }
}

@Composable
private fun SkillRow(
    character: Character,
    skill: SkillData,
    viewModel: CharacterViewModel,
    dense: Boolean = false,
    onClick: () -> Unit,
    onRoll: () -> Unit
) {
    val isZero = skill.level == 0
    val total = skillTotal(character, skill)
    val vPad = if (dense) 2.dp else 5.dp
    val boxSize = if (dense) 38.dp else 46.dp
    val nameSize = if (dense) 15.sp else 17.sp
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = vPad).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // باکس سطح مهارت (فقط نمایش؛ تغییر از دکمه‌ی شناور ادیت انجام می‌شود)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(boxSize)
                .background(if (isZero) Color(0xFF1A0505) else Red, CutCornerShape(8.dp))
                .border(2.dp, Red, CutCornerShape(8.dp))
        ) {
            Text(skill.level.toString(), color = if (isZero) Red else Color.Black,
                fontSize = if (dense) 18.sp else 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(gtr(skill.name), color = Color.White, fontSize = nameSize, fontWeight = FontWeight.Bold)
            // در حالت فشرده (Dense) نام استت حذف می‌شود تا فضا کمتر بگیرد
            if (!dense) Text(gtr(skill.stat), color = Color.Gray, fontSize = 13.sp)
        }
        // دکمه تاس
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(96.dp).height(if (dense) 38.dp else 44.dp).background(Red, CutCornerShape(8.dp)).clickable { onRoll() }
        ) {
            Text("+$total", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================
// منوی شناور ادیت: انتخاب بین استت / مهارت / قابلیت نقش
// ============================================================
@Composable
private fun EditMenuDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(14.dp))) {
            Column(Modifier.padding(20.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("تغییر مقادیر", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(14.dp))
                EditMenuRow(Icons.Default.Tune, "استت‌ها (Stats)", "تغییر INT، REF، DEX و …") { onPick("stat") }
                EditMenuRow(Icons.Default.Add, "مهارت‌ها (Skills)", "افزایش/کاهش سطح هر مهارت") { onPick("skill") }
                EditMenuRow(Icons.Default.Star, "قابلیت نقش (Role Ability)", "رنک نقش و توان‌های نقش") { onPick("role") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text("بستن", color = Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).background(Red.copy(alpha = 0.15f), CutCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Red, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.weight(1f)) {
                Text(gtr(title), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Text(gtr(sub), color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            }
        }
    }
}

// دکمه‌ی کوچک +/− برای ادیت
@Composable
private fun Stepper(value: Int, min: Int, max: Int, onDelta: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−") { if (value > min) onDelta(-1) }
        Box(Modifier.width(46.dp), contentAlignment = Alignment.Center) {
            Text("$value", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        StepperButton("+") { if (value < max) onDelta(1) }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).background(Red, CutCornerShape(8.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(gtr(label), color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
}

// ---------- دیالوگ ادیت استت‌ها ----------
@Composable
private fun EditStatsDialog(character: Character, viewModel: CharacterViewModel, onDismiss: () -> Unit) {
    val rows = listOf(
        "INT" to character.stats.int, "REF" to character.stats.ref, "DEX" to character.stats.dex,
        "TECH" to character.stats.tech, "COOL" to character.stats.cool, "WILL" to character.stats.will,
        "MOVE" to character.stats.move, "BODY" to character.stats.body, "EMP" to character.stats.emp
    )
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp).border(2.dp, Red, CutCornerShape(14.dp))) {
            Column(Modifier.padding(20.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("استت‌ها (Stats)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Text("دامنه‌ی مجاز: ۲ تا ۱۰. تغییر EMP سقف Humanity را به‌روز می‌کند.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Right)
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(rows.size) { i ->
                        val (k, v) = rows[i]
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Stepper(value = v, min = 2, max = 10) { d -> viewModel.adjustStat(character.id, k, d) }
                            Spacer(Modifier.weight(1f))
                            Text(gtr(k), color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text("تمام", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ---------- دیالوگ ادیت مهارت‌ها (همه‌ی مهارت‌ها شامل صفر) ----------
@Composable
private fun EditSkillsDialog(character: Character, viewModel: CharacterViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val owned = character.skills.associateBy { it.name }
    val all = SkillCatalog.allSkillNames().map { it to (owned[it]?.level ?: 0) }
        .filter { query.isBlank() || it.first.contains(query, ignoreCase = true) }
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).border(2.dp, Red, CutCornerShape(14.dp))) {
            Column(Modifier.padding(20.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("مهارت‌ها (Skills)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text(gtr("Search..."), color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Red, unfocusedBorderColor = Red.copy(0.5f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(all.size) { i ->
                        val (name, lvl) = all[i]
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Stepper(value = lvl, min = 0, max = 10) { d ->
                                viewModel.setSkillLevelByName(character.id, name, lvl + d)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(gtr(name), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text("تمام", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ---------- دیالوگ ادیت رنک نقش + نمایش توان‌ها ----------
@Composable
private fun EditRoleAbilityDialog(character: Character, viewModel: CharacterViewModel, onDismiss: () -> Unit) {
    val roleAb = RoleAbilitiesData.forRole(character.role)
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).border(2.dp, Red, CutCornerShape(14.dp))) {
            Column(Modifier.padding(20.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("قابلیت نقش (Role Ability)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Stepper(value = character.roleRank, min = 1, max = 10) { d ->
                        viewModel.adjustRoleRank(character.id, d)
                    }
                    Spacer(Modifier.weight(1f))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr("%1s  •  Rank", character.role), color = Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Red.copy(alpha = 0.4f))
                LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                    if (roleAb == null) {
                        item { Text("برای این نقش توان تعریف نشده است.", color = Color.Gray) }
                    } else {
                        items(roleAb.abilities.size) { i ->
                            val ab = roleAb.abilities[i]
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(gtr(ab.titleEn), color = Red, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                }
                                Text(ab.titleFa, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(gtr(ab.desc), color = Color.Gray, fontSize = 12.sp, lineHeight = 19.sp,
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Right)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text("تمام", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun GroupHeader(statName: String, cyberRed: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        HorizontalDivider(color = cyberRed, modifier = Modifier.weight(1f), thickness = 1.dp)
        Text(gtr(statName), color = Color.LightGray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
        HorizontalDivider(color = cyberRed, modifier = Modifier.weight(1f), thickness = 1.dp)
    }
}

@Composable
fun StatInfoDialog(statKey: String, cyberRed: Color, cyberBlack: Color, onDismiss: () -> Unit) {
    val statDetails = when (statKey) {
        "INT" -> "Intelligence" to "میزان هوش عمومی؛ شامل زیرکی، آگاهی، ادراک و توانایی یادگیری."
        "REF" -> "Reflexes" to "زمان واکنش و هماهنگی؛ روی ضربه زدن با سلاح‌های دوربرد اثر می‌گذارد."
        "DEX" -> "Dexterity" to "شایستگی فیزیکی؛ روی مبارزه تن‌به‌تن، جاخالی دادن و فعالیت‌های ورزشی اثر دارد."
        "TECH" -> "Technique" to "توانایی کار با ابزار و دستگاه‌ها؛ تعمیر، سیم‌کشی و تکنولوژی."
        "COOL" -> "Cool" to "توانایی تأثیرگذاری روی افراد با شخصیت و کاریزما؛ تعاملات اجتماعی."
        "WILL" -> "Willpower" to "اراده و رویارویی با خطر و استرس؛ در تحمل آسیب مهم است."
        "LUCK" -> "Luck" to "امتیازاتی که می‌توانی برای تغییر نتیجه تاس‌ها خرج کنی؛ در شروع جلسه بعد پر می‌شود."
        "MOVE" -> "Movement" to "سرعت حرکت: دویدن، جهش، شنا کردن."
        "BODY" -> "Body" to "اندازه و سرسختی بدنی؛ در تعیین جان (HP) بسیار مهم است."
        "EMP" -> "Empathy" to "توانایی ارتباط و اهمیت دادن به دیگران؛ خنثی‌کننده‌ی سایبرسایکوزیس."
        else -> "Unknown" to "توضیحاتی یافت نشد."
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = cyberBlack), shape = CutCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, cyberRed, CutCornerShape(16.dp))) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gtr(statDetails.first), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = cyberRed.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(Modifier.height(16.dp))
                Text(gtr(statDetails.second), color = Color(0xFFE0E0E0), fontSize = 15.sp, lineHeight = 24.sp,
                    style = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Justify, textDirection = androidx.compose.ui.text.style.TextDirection.Rtl))
            }
        }
    }
}

@Composable
fun SkillInfoDialog(skillName: String, cyberRed: Color, cyberBlack: Color, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = cyberBlack), shape = CutCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, cyberRed, CutCornerShape(16.dp))) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gtr(skillName), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = cyberRed.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(Modifier.height(16.dp))
                val statName = com.cyberpunk.gmtool.data.SkillCatalog.statFor(skillName)
                Text(com.cyberpunk.gmtool.data.SkillDescriptions.forSkill(skillName),
                    color = Color(0xFFE0E0E0), fontSize = 15.sp, lineHeight = 26.sp,
                    style = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Justify, textDirection = androidx.compose.ui.text.style.TextDirection.Rtl))
                Spacer(Modifier.height(12.dp))
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    Text("استت پایه: $statName   •   فرمول چک: $statName + سطح مهارت + 1d10 در برابر درجه‌ی سختی (DV).",
                        color = cyberRed, fontSize = 12.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// دیالوگ تاس (مشترک استت و مهارت) — از تاس خودکار/دستی پیروی می‌کند
@Composable
fun StatRollDialog(statName: String, statValue: Int, cyberRed: Color, cyberBlack: Color, onDismiss: () -> Unit) {
    var baseRoll by remember { mutableStateOf(0) }
    var secondRoll by remember { mutableStateOf<Int?>(null) }
    var ready by remember { mutableStateOf(false) }

    // مقادیر CompositionLocal باید در سطح composable خوانده شوند
    val autoDice = LocalAutoDice.current
    val hapticsEnabled = LocalHaptics.current
    val dice = remember { ManualDiceState() }
    val view = androidx.compose.ui.platform.LocalView.current

    fun doRoll() {
        if (autoDice) {
            // تاس خودکار هم از DiceSource می‌آید (تنها منبع تاس)؛ ویبره‌اش همان‌جا
            // در onAutoRoll زده می‌شود. در حالت دستی دیالوگ زیر عدد GM را می‌گیرد.
            baseRoll = DiceSource.rollOne(10, "چک استت/مهارت")
            if (baseRoll == 10 || baseRoll == 1) {
                secondRoll = DiceSource.rollOne(
                    10,
                    if (baseRoll == 10) "Critical Success — تاس اضافه" else "Critical Failure — تاس اضافه"
                )
            } else secondRoll = null
            ready = true
        } else {
            dice.request(10) { value ->
                baseRoll = value
                if (value == 10 || value == 1) {
                    dice.request(10) { value2 -> secondRoll = value2; ready = true }
                } else {
                    secondRoll = null
                    ready = true
                }
            }
        }
    }

    LaunchedEffect(statName) { doRoll() }

    if (dice.active) {
        ManualDiceDialog(
            sides = dice.sides,
            onDismiss = { dice.cancel(); onDismiss() },
            onSubmit = { value ->
                if (hapticsEnabled) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                }
                dice.submit(value)
            }
        )
        return
    }

    if (!ready) {
        // در حالت خودکار به سرعت مقدار آماده می‌شود؛ در حالت دستی دیالوگ باز است
        Dialog(onDismissRequest = onDismiss) {
            Card(colors = CardDefaults.cardColors(containerColor = cyberBlack), shape = CutCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(2.dp, cyberRed, CutCornerShape(12.dp))) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cyberRed)
                }
            }
        }
        return
    }

    val diceSum = when (baseRoll) {
        10 -> 10 + (secondRoll ?: 0)
        1 -> 1 - (secondRoll ?: 0)
        else -> baseRoll
    }
    val total = statValue + diceSum
    val rollText = when (baseRoll) {
        10 -> gtr("%1s (Critical Success)", diceSum)
        1 -> gtr("%1s (Critical Failure)", diceSum)
        else -> gtr("%1s (Roll)", diceSum)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = cyberBlack), shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, cyberRed, CutCornerShape(12.dp))) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gtr(statName), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = cyberRed, thickness = 2.dp)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    DiamondDice(baseRoll, baseRoll == 10)
                    secondRoll?.let {
                        Spacer(Modifier.width(24.dp))
                        DiamondDice(it, false)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(gtr(rollText), color = Color.LightGray, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("+ $statValue ($statName)", color = Color.LightGray, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.width(150.dp))
                Spacer(Modifier.height(12.dp))
                Text(gtr("TOTAL"), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.background(cyberRed, CutCornerShape(8.dp)).padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(total.toString(), color = cyberBlack, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = cyberRed), modifier = Modifier.fillMaxWidth()) {
                    Text(gtr("CLOSE"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DiamondDice(value: Int, isCritical: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(64.dp).rotate(45f).background(Red, CutCornerShape(8.dp))
    ) {
        Text(value.toString(), color = Black, fontWeight = FontWeight.Bold, fontSize = 28.sp, modifier = Modifier.rotate(-45f).offset(y = (-2).dp))
        if (isCritical) {
            Text("★", color = Black, fontSize = 14.sp, modifier = Modifier.rotate(-45f).offset(x = (-14).dp, y = (-14).dp))
        }
    }
}
