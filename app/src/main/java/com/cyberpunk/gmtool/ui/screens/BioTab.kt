package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.ImageProvider
import com.cyberpunk.gmtool.data.LifepathData
import com.cyberpunk.gmtool.data.LifepathGenerator
import com.cyberpunk.gmtool.data.RoleLifepathGenerator
import com.cyberpunk.gmtool.data.VehicleCatalog
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.ui.components.CoreItemDetailDialog
import com.cyberpunk.gmtool.ui.components.FaText
// توابع توسعه‌ی دامنه‌های تفکیک‌شده‌ی ViewModel (vehicle/combat/…) در همین پکیج‌اند.
import com.cyberpunk.gmtool.viewmodel.*
import kotlinx.coroutines.launch

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)
private val CardBg = Color(0xFF1A1A1A)

private val RtlJustify = TextStyle(textAlign = TextAlign.Justify, textDirection = TextDirection.Rtl)
private val Rtl = TextStyle(textDirection = TextDirection.Rtl)

@Composable
fun BioTab(character: Character, viewModel: CharacterViewModel, onBack: () -> Unit) {
    var showSpendIpDialog by remember { mutableStateOf(false) }
    var showIpDialog by remember { mutableStateOf(false) }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }
    var isLifepathExpanded by remember { mutableStateOf(false) }
    var isLifestyleExpanded by remember { mutableStateOf(false) }

    // لایف‌پث زنده‌ی کاراکتر (از VM) — دیگر با ری‌کامپوز پاک نمی‌شود
    val lifepath = character.lifepath
    fun updateLp(newLp: LifepathData) = viewModel.updateLifepath(character.id, newLp)

    // جدول‌های Lifepath تاس واقعی می‌ریزند (d10/d6) و در حالت «تاس دستی» باید عدد
    // را GM بدهد. DiceSource برای پرسیدن از GM نخ فراخوان را بلاک می‌کند، پس این
    // عملیات نباید روی نخ اصلی بماند؛ وگرنه برنامه ناچار می‌شود تصادفی بریزد
    // (و هشدارش را هم می‌دهد). الگو همان performAttack در CombatTab است.
    val rulesScope = rememberCoroutineScope()
    fun rules(block: () -> Unit) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            rulesScope.launch(kotlinx.coroutines.Dispatchers.Default) { block() }
        } else block()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Black).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .height(210.dp)
                        .background(Color.DarkGray, shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                        .border(width = 1.dp, color = Red, shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = if (character.isAlly)
                            ImageProvider.getRoleImage(character.role, isPortrait = true)
                        else
                            ImageProvider.getNpcImage(character.npcCategory, character.role, character.name)),
                        contentDescription = gtr("%1s Avatar", character.name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier.weight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyberpunkEditBox("Name", character.name) { viewModel.setName(character.id, it) }
                    CyberpunkEditBox("Handle (Street Name)", character.handle) { viewModel.setHandle(character.id, it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyberpunkOutlinedBox(
                            "IP (اضافه/کم)", character.improvementPoints.toString(),
                            modifier = Modifier.weight(1f).clickable { showIpDialog = true }
                        )
                        CyberpunkOutlinedBox("HP", "${character.hp}/${character.maxHp}", modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = { showSpendIpDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, null, tint = Black, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(gtr("Spend IP"), color = Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }

        // دراپ‌داون نقش
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, CutCornerShape(4.dp))
                        .border(1.dp, Red, CutCornerShape(4.dp))
                        .clickable { isRoleDropdownExpanded = !isRoleDropdownExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(gtr("%1s Rank %2s", character.role, character.roleRank), color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (isRoleDropdownExpanded) "▲" else "▼", color = Red, fontSize = 16.sp)
                }

                if (isRoleDropdownExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(Black)
                            .border(1.dp, Color.DarkGray, CutCornerShape(4.dp))
                            .padding(16.dp)
                    ) {
                        DynamicRoleAbility(character = character, roleLevel = character.roleRank, viewModel = viewModel)
                    }
                }
            }
        }

        // ---------- بخش مخصوص NPC ----------
        if (!character.isAlly) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    // Faction / NPC Type
                    ReadOnlyBox("Faction / NPC Type",
                        listOf(character.npcCategory, character.npcTier).filter { it.isNotBlank() }.joinToString(" • "))
                    Spacer(Modifier.height(10.dp))

                    // آکاردئون شخصیت
                    NpcInfoAccordion("Personality — ویژگی شخصیتی",
                        character.lifepath.personality.ifBlank { character.notes.ifBlank { "—" } })
                    NpcInfoAccordion("Motivation — انگیزه",
                        npcField(character, "motivation").ifBlank { "—" })
                    NpcInfoAccordion("Identifying Features — ویژگی‌های ظاهری",
                        npcField(character, "identifying").ifBlank {
                            character.lifepath.clothingStyle.ifBlank { "—" }
                        })
                }
            }
        }

        // آکاردئون لایف‌پث
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp))
                    .background(Color(0xFF111111))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isLifepathExpanded = !isLifepathExpanded }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(gtr("LIFEPATH"), color = Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (isLifepathExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = gtr("Expand"), tint = Red
                    )
                }

                if (isLifepathExpanded) {
                    HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)
                    Column(modifier = Modifier.padding(12.dp)) {
                        LifepathRow("اصالت فرهنگی (Cultural Origins)", lifepath.culturalOrigins) {
                            rules { updateLp(lifepath.copy(culturalOrigins = LifepathGenerator.rollCulturalOrigins())) }
                        }
                        LifepathRow("ویژگی شخصیتی (Personality)", lifepath.personality) {
                            rules { updateLp(lifepath.copy(personality = LifepathGenerator.rollPersonality())) }
                        }
                        LifepathRow("استایل لباس (Clothing Style)", lifepath.clothingStyle) {
                            rules { updateLp(lifepath.copy(clothingStyle = LifepathGenerator.rollClothingStyle())) }
                        }
                        LifepathRow("مدل مو (Hairstyle)", lifepath.hairstyle) {
                            rules { updateLp(lifepath.copy(hairstyle = LifepathGenerator.rollHairstyle())) }
                        }
                        LifepathRow("آیتم همیشگی (Affectation)", lifepath.affectation) {
                            rules { updateLp(lifepath.copy(affectation = LifepathGenerator.rollAffectation())) }
                        }
                        LifepathRow("باارزش‌ترین چیز (Value Most)", lifepath.valueMost) {
                            rules { updateLp(lifepath.copy(valueMost = LifepathGenerator.rollValueMost())) }
                        }
                        LifepathRow("احساس به آدم‌ها (Feelings About People)", lifepath.feelingsAboutPeople) {
                            rules { updateLp(lifepath.copy(feelingsAboutPeople = LifepathGenerator.rollFeelingsAboutPeople())) }
                        }
                        LifepathRow("مهم‌ترین شخص زندگی (Valued Person)", lifepath.valuedPerson) {
                            rules { updateLp(lifepath.copy(valuedPerson = LifepathGenerator.rollValuedPerson())) }
                        }
                        LifepathRow("باارزش‌ترین دارایی (Valued Possession)", lifepath.valuedPossession) {
                            rules { updateLp(lifepath.copy(valuedPossession = LifepathGenerator.rollValuedPossession())) }
                        }
                        LifepathRow("پیشینه خانواده (Family Background)", lifepath.familyBackground) {
                            rules { updateLp(lifepath.copy(familyBackground = LifepathGenerator.rollFamilyBackground())) }
                        }
                        LifepathRow("محیط کودکی (Childhood Environment)", lifepath.childhoodEnvironment) {
                            rules { updateLp(lifepath.copy(childhoodEnvironment = LifepathGenerator.rollChildhoodEnvironment())) }
                        }
                        LifepathRow("بحران خانوادگی (Family Crisis)", lifepath.familyCrisis) {
                            rules { updateLp(lifepath.copy(familyCrisis = LifepathGenerator.rollFamilyCrisis())) }
                        }
                        LifepathRow("دوستان (Friends)", lifepath.friends) {
                            rules { updateLp(lifepath.copy(friends = LifepathGenerator.rollFriends())) }
                        }
                        LifepathRow("دشمنان (Enemies)", lifepath.enemies) {
                            rules { updateLp(lifepath.copy(enemies = LifepathGenerator.rollEnemies())) }
                        }
                        LifepathRow("عشق‌های نافرجام (Tragic Love Affairs)", lifepath.tragicLoveAffairs) {
                            rules { updateLp(lifepath.copy(tragicLoveAffairs = LifepathGenerator.rollTragicLoveAffairs())) }
                        }
                        LifepathRow("اهداف زندگی (Life Goals)", lifepath.lifeGoals) {
                            rules { updateLp(lifepath.copy(lifeGoals = LifepathGenerator.rollLifeGoals())) }
                        }
                        LifepathRow("مسیر زندگیِ نقش (Role Lifepath)", lifepath.roleLifepath) {
                            rules { updateLp(lifepath.copy(roleLifepath = RoleLifepathGenerator.generateForRole(character.role))) }
                        }
                    }
                }
            }
        }

        // آکاردئون لایف‌استایل و مسکن
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp))
                    .background(Color(0xFF111111))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isLifestyleExpanded = !isLifestyleExpanded }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(gtr("LIFESTYLE & HOUSING"), color = Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (isLifestyleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = gtr("Expand"), tint = Red
                    )
                }
                if (isLifestyleExpanded) {
                    HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(Modifier.padding(12.dp)) {
                            Text("سبک زندگی (ماهانه)", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            com.cyberpunk.gmtool.data.LifeData.lifestyles.forEach { ls ->
                                LifestyleRow(
                                    title = ls.faName,
                                    cost = gtr("%1s eb", ls.cost),
                                    selected = character.lifestyle == ls.key,
                                    onClick = { viewModel.setLifestyle(character.id, ls.key) }
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("محل سکونت (اجاره‌ی ماهانه)", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            com.cyberpunk.gmtool.data.LifeData.housing.forEach { h ->
                                LifestyleRow(
                                    title = h.faName,
                                    cost = h.rentLabel,
                                    selected = character.housing == h.key,
                                    onClick = { viewModel.setHousing(character.id, h.key) }
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("اجاره‌ی فعلی: ماهانه ${character.monthlyRent} eb", color = Red, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    if (showSpendIpDialog) {
        ImprovementScreen(character = character, viewModel = viewModel, onDismiss = { showSpendIpDialog = false })
    }
    if (showIpDialog) {
        IpAdjustDialog(character = character, viewModel = viewModel, onDismiss = { showIpDialog = false })
    }
}

// =======================================================
// افزودن/کم‌کردن امتیاز IP (مثل دیالوگ پول)
// =======================================================
/** فیلدهای متنی NPC از lifepath (motivation و identifying) */
private fun npcField(c: Character, key: String): String = when (key) {
    "motivation" -> c.lifepath.valueMost
    "identifying" -> c.lifepath.affectation.ifBlank { c.lifepath.clothingStyle }
    else -> ""
}

@Composable
private fun ReadOnlyBox(label: String, value: String) {
    Column(
        Modifier.fillMaxWidth()
            .border(1.dp, Red.copy(alpha = 0.8f), CutCornerShape(4.dp))
            .background(Color(0xFF0B0B0B))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(gtr(label), color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NpcInfoAccordion(title: String, body: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .border(1.dp, Red.copy(alpha = 0.8f), CutCornerShape(4.dp))
            .background(Color(0xFF0B0B0B))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(gtr(title), color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = gtr("Expand"), tint = Red
            )
        }
        if (expanded) {
            HorizontalDivider(color = Color(0xFF333333))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(gtr(body), color = Color.LightGray, fontSize = 14.sp, lineHeight = 23.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth().padding(14.dp))
            }
        }
    }
}

@Composable
fun IpAdjustDialog(character: Character, viewModel: CharacterViewModel, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("0") }
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(16.dp))) {
            Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gtr("Improvement Points (IP)"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("فعلی: ${character.improvementPoints}", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(48.dp).background(Color(0xFF2A2A2A), CutCornerShape(8.dp)).clickable {
                        val v = input.toIntOrNull() ?: 0; if (v > 0) input = (v - 1).toString()
                    }, contentAlignment = Alignment.Center) { Text("—", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = input, onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) input = it },
                        label = { Text("مقدار", color = Color.LightGray) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Red, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(48.dp).background(Red, CutCornerShape(8.dp)).clickable {
                        val v = input.toIntOrNull() ?: 0; input = (v + 1).toString()
                    }, contentAlignment = Alignment.Center) { Text("+", color = Black, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { val v = input.toIntOrNull() ?: 0; if (v > 0) viewModel.adjustImprovementPoints(character.id, -v); onDismiss() },
                        shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f).height(48.dp)) { Text("کم کن", color = White) }
                    Button(onClick = { val v = input.toIntOrNull() ?: 0; if (v > 0) viewModel.adjustImprovementPoints(character.id, v); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Red), shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f).height(48.dp)) { Text("اضافه کن", color = Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// =======================================================
// صفحه‌ی تمام‌صفحه‌ی Improvement (Spend IP) — دو تب: Skills و Role Abilities
// =======================================================
@Composable
fun ImprovementScreen(character: Character, viewModel: CharacterViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        var tab by remember { mutableStateOf(0) } // 0 = Skills, 1 = Role Abilities
        var search by remember { mutableStateOf("") }
        var infoAbility by remember { mutableStateOf<com.cyberpunk.gmtool.data.RoleAbilityEntry?>(null) }
        var infoSkill by remember { mutableStateOf<String?>(null) }
        var toast by remember { mutableStateOf<String?>(null) }

        Column(Modifier.fillMaxSize().background(Black)) {
            // هدر
            Row(Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, gtr("Back"), tint = Red) }
                Text(gtr("Improvement"), color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = Red.copy(0.15f), shape = CutCornerShape(6.dp),
                    modifier = Modifier.border(1.dp, Red, CutCornerShape(6.dp))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = Red, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(gtr("%1s IP", character.improvementPoints), color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            HorizontalDivider(color = Red, thickness = 1.dp)

            // تب‌ها
            TabRow(selectedTabIndex = tab, containerColor = Black, contentColor = Red,
                indicator = { pos -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = Red, height = 3.dp) },
                divider = { HorizontalDivider(color = Red.copy(0.3f)) }) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(gtr("Skills"), color = if (tab == 0) Red else Muted, fontWeight = FontWeight.Bold) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(gtr("Role Abilities"), color = if (tab == 1) Red else Muted, fontWeight = FontWeight.Bold) })
            }

            if (tab == 0) {
                // جستجو (همه‌ی مهارت‌ها شامل صفر)
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    placeholder = { Text(gtr("Search..."), color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Red, unfocusedBorderColor = Red.copy(0.5f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
                val allNames = com.cyberpunk.gmtool.data.SkillCatalog.allSkillNames()
                val owned = character.skills.associateBy { it.name }
                val rows = allNames.map { name ->
                    name to (owned[name]?.level ?: 0)
                }.filter { (name, lvl) ->
                    search.isBlank() || name.contains(search, ignoreCase = true)
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(rows.size) { i ->
                        val (name, lvl) = rows[i]
                        val cost = (lvl + 1) * 10
                        val maxed = lvl >= 10
                        val canAfford = !maxed && character.improvementPoints >= cost
                        val stat = com.cyberpunk.gmtool.data.SkillCatalog.statFor(name)
                        Row(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(8.dp))
                            .border(1.dp, Red.copy(0.25f), CutCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            // دکمه‌ی توضیح
                            IconButton(onClick = { infoSkill = name }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Info, gtr("Info"), tint = Red, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f).clickable { infoSkill = name }.padding(horizontal = 4.dp)) {
                                Text(gtr(name), color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    if (maxed) gtr("Level %1s (MAX)", lvl) else gtr("Level %1s → %2s  •  %3s IP  •  %4s", lvl, lvl + 1, cost, stat),
                                    color = Muted, fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val ok = viewModel.upgradeSkillWithIP(character.id, name)
                                    toast = if (ok) null else ("IP کافی نیست یا به سقف رسیده‌ای")
                                },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(containerColor = Red, disabledContainerColor = Color(0xFF2A2A2A)),
                                shape = CutCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = if (canAfford) Black else Muted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (maxed) "MAX" else "$cost", color = if (canAfford) Black else Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                // تب Role Abilities — توان‌های نقشِ خودِ کاراکتر
                val roleAb = com.cyberpunk.gmtool.data.RoleAbilitiesData.forRole(character.role)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(gtr("Role Rank: %1s", character.roleRank), color = Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("ارتقای رنک نقش، توان‌های نقش را قوی‌تر می‌کند. برای دیدن توضیح هر توان رویش بزن.", color = Muted, fontSize = 12.sp, style = Rtl)
                    }
                    if (roleAb == null) {
                        item { Text("برای این نقش توان تعریف‌نشده است.", color = Muted) }
                    } else {
                        // ارتقای رنک نقش
                        item {
                            val nextRank = character.roleRank + 1
                            val cost = nextRank * 60
                            val canAfford = nextRank <= 10 && character.improvementPoints >= cost
                            Row(Modifier.fillMaxWidth().background(Red.copy(0.12f), CutCornerShape(10.dp))
                                .border(1.dp, Red, CutCornerShape(10.dp)).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("ارتقای رنک نقش (${roleAb.roleFa})", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (nextRank > 10) "به رنک ۱۰ (سقف) رسیده‌ای"
                                        else gtr("Rank %1s → %2s  •  %3s IP", character.roleRank, nextRank, cost),
                                        color = Muted, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { val ok = viewModel.upgradeRoleRankWithIP(character.id); if (!ok) toast = "IP کافی نیست یا در سقفی" },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(containerColor = Red, disabledContainerColor = Color(0xFF2A2A2A)),
                                    shape = CutCornerShape(8.dp)) {
                                    Icon(Icons.Default.Star, null, tint = if (canAfford) Black else Muted, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (nextRank > 10) "MAX" else "ارتقا", color = if (canAfford) Black else Muted, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        items(roleAb.abilities.size) { i ->
                            val ab = roleAb.abilities[i]
                            Column(Modifier.fillMaxWidth().clickable { infoAbility = ab }
                                .background(CardBg, CutCornerShape(8.dp)).border(1.dp, Red.copy(0.3f), CutCornerShape(8.dp)).padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(ab.titleFa, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Info, null, tint = Red, modifier = Modifier.size(18.dp))
                                }
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(gtr(ab.titleEn), color = Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // دیالوگ توضیح مهارت
        infoSkill?.let { name ->
            Dialog(onDismissRequest = { infoSkill = null }) {
                Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(14.dp))) {
                    Column(Modifier.padding(20.dp)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(gtr(name), color = Red, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(10.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(com.cyberpunk.gmtool.data.SkillDescriptions.forSkill(name),
                                color = White, fontSize = 14.sp, lineHeight = 23.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { infoSkill = null }, colors = ButtonDefaults.buttonColors(containerColor = Red),
                            shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text("بستن", color = Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // دیالوگ توضیح رول‌ابیلتی
        infoAbility?.let { ab ->
            Dialog(onDismissRequest = { infoAbility = null }) {
                Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(14.dp))) {
                    Column(Modifier.padding(20.dp)) {
                        Text(ab.titleFa, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(gtr(ab.titleEn), color = Red, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(12.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(gtr(ab.desc), color = White, fontSize = 14.sp, lineHeight = 24.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { infoAbility = null }, colors = ButtonDefaults.buttonColors(containerColor = Red),
                            shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text("بستن", color = Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // toast ساده
        toast?.let {
            androidx.compose.runtime.LaunchedEffect(it) {
                kotlinx.coroutines.delay(1600); toast = null
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Surface(color = Red, shape = CutCornerShape(8.dp), modifier = Modifier.padding(bottom = 40.dp)) {
                    Text(it, color = Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
            }
        }
    }
}

// =======================================================
// انتخاب موتور نقش
// =======================================================
@Composable
fun DynamicRoleAbility(character: Character, roleLevel: Int, viewModel: CharacterViewModel) {
    // نگاشت امتیازات قابلیت نقش (در VM ذخیره می‌شود و پاک نمی‌شود)
    val points = remember(character.id, character.roleAbilityPoints) {
        mutableStateMapOf<String, Int>().apply { putAll(character.roleAbilityPoints) }
    }
    fun setPoint(key: String, value: Int) {
        val v = value.coerceAtLeast(0)
        points[key] = v
        viewModel.setRoleAbilityPoint(character.id, key, v)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        when (character.role.lowercase()) {
            "solo" -> SoloRoleAbilities(character = character, viewModel = viewModel, roleLevel = roleLevel, points = points, onSet = ::setPoint)
            "nomad" -> NomadRoleAbilities(character = character, roleLevel = roleLevel, viewModel = viewModel)
            "fixer" -> FixerRoleAbilities(character = character, roleLevel = roleLevel)
            "netrunner" -> NetrunnerRoleAbilities(roleLevel = roleLevel)
            "medtech" -> MedtechRoleAbilities(character = character, roleLevel = roleLevel, points = points, onSet = ::setPoint, viewModel = viewModel)
            "rockerboy" -> RockerboyRoleAbilities(roleLevel = roleLevel)
            "tech" -> TechRoleAbilities(roleLevel = roleLevel, points = points, onSet = ::setPoint)
            "media" -> MediaRoleAbilities(character = character, roleLevel = roleLevel, viewModel = viewModel)
            "exec" -> ExecRoleAbilities(character = character, roleLevel = roleLevel, viewModel = viewModel)
            "lawman" -> LawmanRoleAbilities(roleLevel = roleLevel)
            else -> {
                Text("نقش (Role)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("اطلاعات کلاس ${character.role} در حال توسعه است.", color = Muted, style = Rtl)
            }
        }
    }
}

// =======================================================
// سولو (Combat Awareness) — رفع باگ پیش‌فرض ۸ و ذخیره‌سازی
// =======================================================
@Composable
fun SoloRoleAbilities(character: Character, viewModel: CharacterViewModel, roleLevel: Int, points: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>, onSet: (String, Int) -> Unit) {
    var fumbleRecovery by remember { mutableStateOf((points["fumble"] ?: 0) >= 4) }
    var selectedAbilityInfo by remember { mutableStateOf<String?>(null) }
    var showSoloExample by remember { mutableStateOf(false) }
    val lore = com.cyberpunk.gmtool.data.RoleLoreData

    val allocated = listOf(
        points["damageDeflection"] ?: 0,
        points["initiativeReaction"] ?: 0,
        points["precisionAttack"] ?: 0,
        points["spotWeakness"] ?: 0,
        points["threatDetection"] ?: 0
    ).sum() + if (fumbleRecovery) 4 else 0
    val remaining = (roleLevel - allocated).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش سولو (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.soloIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: آگاهی رزمی» ──
        LoreBlock(lore.soloAbilityTitle, lore.soloAbilityBox)
        Spacer(Modifier.height(14.dp))

        // ── تخصیص امتیاز (همان مکانیک دقیق قبلی؛ دست نخورده) ──
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, Red, CutCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
            Text(gtr("[%1s] Points Remaining", remaining), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(gtr("Fumble Recovery (4 pts)"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                modifier = Modifier.clickable { selectedAbilityInfo = "Fumble Recovery" }.weight(1f).fillMaxWidth(),
                textAlign = TextAlign.Right)
            Switch(
                checked = fumbleRecovery,
                onCheckedChange = { checked ->
                    if (checked && remaining >= 4) { fumbleRecovery = true; onSet("fumble", 4) }
                    else if (!checked) { fumbleRecovery = false; onSet("fumble", 0) }
                },
                colors = cprSwitchColors()
            )
        }
        HorizontalDivider(color = Red.copy(alpha = 0.4f))
        lore.soloAbilityNote["Fumble Recovery"]?.let { (t, b) -> LoreBlock(t, b, compact = true) }

        AbilityCounterRow("Damage Deflection (points spent; 2 = -1 dmg)", points["damageDeflection"] ?: 0,
            onIncrement = { if (remaining >= 2) onSet("damageDeflection", (points["damageDeflection"] ?: 0) + 2) },
            onDecrement = { onSet("damageDeflection", ((points["damageDeflection"] ?: 0) - 2).coerceAtLeast(0)) },
            onClickTitle = { selectedAbilityInfo = "Damage Deflection" })
        lore.soloAbilityNote["Damage Deflection"]?.let { (t, b) -> LoreBlock(t, b, compact = true) }

        AbilityCounterRow("Initiative Reaction", points["initiativeReaction"] ?: 0,
            onIncrement = { if (remaining > 0) onSet("initiativeReaction", (points["initiativeReaction"] ?: 0) + 1) },
            onDecrement = { onSet("initiativeReaction", (points["initiativeReaction"] ?: 0) - 1) },
            onClickTitle = { selectedAbilityInfo = "Initiative Reaction" })
        lore.soloAbilityNote["Initiative Reaction"]?.let { (t, b) -> LoreBlock(t, b, compact = true) }

        AbilityCounterRow("Precision Attack (points spent; 3 = +1)", points["precisionAttack"] ?: 0,
            onIncrement = { if (remaining >= 3) onSet("precisionAttack", (points["precisionAttack"] ?: 0) + 3) },
            onDecrement = { onSet("precisionAttack", ((points["precisionAttack"] ?: 0) - 3).coerceAtLeast(0)) },
            onClickTitle = { selectedAbilityInfo = "Precision Attack" })
        lore.soloAbilityNote["Precision Attack"]?.let { (t, b) -> LoreBlock(t, b, compact = true) }

        AbilityCounterRow("Spot Weakness", points["spotWeakness"] ?: 0,
            onIncrement = { if (remaining > 0) onSet("spotWeakness", (points["spotWeakness"] ?: 0) + 1) },
            onDecrement = { onSet("spotWeakness", (points["spotWeakness"] ?: 0) - 1) },
            onClickTitle = { selectedAbilityInfo = "Spot Weakness" })
        lore.soloAbilityNote["Spot Weakness"]?.let { (t, b) -> LoreBlock(t, b, compact = true) }

        AbilityCounterRow("Threat Detection", points["threatDetection"] ?: 0,
            onIncrement = { if (remaining > 0) onSet("threatDetection", (points["threatDetection"] ?: 0) + 1) },
            onDecrement = { onSet("threatDetection", (points["threatDetection"] ?: 0) - 1) },
            onClickTitle = { selectedAbilityInfo = "Threat Detection" })
        lore.soloAbilityNote["Threat Detection"]?.let { (t, b) -> LoreBlock(t, b, compact = true) }

        // ── نمونه‌ی اجرا در بازی (کادر کنارِ توضیح آگاهی رزمی در Core) ──
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showSoloExample = !showSoloExample }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showSoloExample) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "نمونه‌ی اجرا در بازی (از کتاب)",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showSoloExample) {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp)
                    .background(Color(0xFF0B0B0B), CutCornerShape(4.dp))
                    .border(1.dp, Red.copy(alpha = 0.45f), CutCornerShape(4.dp)).padding(12.dp)
            ) {
                lore.soloExample.forEach { paragraph ->
                    FaText(paragraph, color = White, fontSize = 12.sp, lineHeight = 20.sp, justify = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    selectedAbilityInfo?.let { AbilityInfoDialog(it, Red) { selectedAbilityInfo = null } }
}

@Composable
fun TechRoleAbilities(roleLevel: Int, points: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>, onSet: (String, Int) -> Unit) {
    var selectedTechInfo by remember { mutableStateOf<String?>(null) }
    var showSpecialties by remember { mutableStateOf(false) }
    val lore = com.cyberpunk.gmtool.data.RoleLoreData
    // همان محاسبه‌ی قبلی؛ هیچ مکانیکی عوض نشده.
    val allocated = listOf(points["field"] ?: 0, points["upgrade"] ?: 0, points["fab"] ?: 0, points["invent"] ?: 0).sum()
    val remaining = (roleLevel * 2 - allocated).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش تک (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.techIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: سازنده» ──
        LoreBlock(lore.techAbilityTitle, lore.techAbilityBox)
        Spacer(Modifier.height(14.dp))

        // ── سیستم تخصیص امتیاز (همان مکانیک دقیق قبلی؛ دست نخورده) ──
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, Muted, CutCornerShape(4.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
            Text(gtr("[%1s] Points Remaining", remaining), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))

        MakerCounterRow("Field Expertise", points["field"] ?: 0, remaining, roleLevel,
            onInc = { onSet("field", (points["field"] ?: 0) + 1) },
            onDec = { onSet("field", (points["field"] ?: 0) - 1) },
            onInfo = { selectedTechInfo = "Field Expertise" })
        MakerCounterRow("Upgrade Expertise", points["upgrade"] ?: 0, remaining, roleLevel,
            onInc = { onSet("upgrade", (points["upgrade"] ?: 0) + 1) },
            onDec = { onSet("upgrade", (points["upgrade"] ?: 0) - 1) },
            onInfo = { selectedTechInfo = "Upgrade Expertise" })
        MakerCounterRow("Fabrication Expertise", points["fab"] ?: 0, remaining, roleLevel,
            onInc = { onSet("fab", (points["fab"] ?: 0) + 1) },
            onDec = { onSet("fab", (points["fab"] ?: 0) - 1) },
            onInfo = { selectedTechInfo = "Fabrication Expertise" })
        MakerCounterRow("Invention Expertise", points["invent"] ?: 0, remaining, roleLevel,
            onInc = { onSet("invent", (points["invent"] ?: 0) + 1) },
            onDec = { onSet("invent", (points["invent"] ?: 0) - 1) },
            onInfo = { selectedTechInfo = "Invention Expertise" })

        // ── جدول DV/زمان ارتقا، ساخت و اختراع (همان جدول کتاب) ──
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Text(
                "DV و زمانِ ارتقا / ساخت / اختراع",
                color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp, style = Rtl,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("دسته‌ی قیمت", color = Muted, fontSize = 11.sp, style = Rtl, modifier = Modifier.weight(1f))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text("DV", color = Muted, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                }
                Text("زمان", color = Muted, fontSize = 11.sp, style = Rtl, modifier = Modifier.width(78.dp))
            }
            HorizontalDivider(color = Red.copy(alpha = 0.5f))
            lore.techDvTable.forEach { (costLabel, dv, time) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(costLabel, color = White, fontSize = 12.sp, style = Rtl, modifier = Modifier.weight(1f))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            dv, color = Black, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier.width(34.dp)
                                .background(Red, CutCornerShape(3.dp)).padding(vertical = 2.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(time, color = Muted, fontSize = 11.sp, style = Rtl, modifier = Modifier.width(78.dp))
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
        }

        // ── متن کامل چهار تخصص (Core) — بازشو تا کارت شلوغ نشود ──
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showSpecialties = !showSpecialties }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showSpecialties) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "تخصص‌ها به تفصیل (از کتاب)",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showSpecialties) {
            lore.techSpecialties.forEach { (title, body) -> LoreBlock(title, body, compact = true) }
        }

        // ── نقل‌قول پایان بخش تک در کتاب ──
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF0B0B0B), CutCornerShape(4.dp))
                .border(1.dp, Red.copy(alpha = 0.45f), CutCornerShape(4.dp)).padding(12.dp)
        ) {
            FaText(lore.techQuote, color = Color(0xFFFFC107), fontSize = 13.sp, lineHeight = 21.sp, justify = true, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(4.dp))
    }

    selectedTechInfo?.let {
        SimpleInfoDialog(title = it, description = techInfo(it), onDismiss = { selectedTechInfo = null })
    }
}

private fun techInfo(name: String): String = when (name) {
    "Field Expertise" -> "رنک خود را به چک‌های Basic Tech، Cybertech، Electronics/Security Tech، Weaponstech و مهارت‌های وسایل نقلیه برای اهداف غیرساخت‌وساز اضافه کن. با حداقل ۱ رنک می‌توانی به‌جای تعمیر کامل، در یک اکشن وسیله را موقتاً (Jury-rig) تعمیر کنی."
    "Upgrade Expertise" -> "آیتم را ارتقا می‌دهد: کاهش Humanity Loss، افزایش اسلات‌ها، قابلیت Conceal برای سلاح، ارتقا به کیفیت Excellent و... . تاس: TECH + مهارت TECH مرتبط + رنک این تخصص + 1d10."
    "Fabrication Expertise" -> "ساخت یک آیتم موجود یا اختراع‌شده. تاس: TECH + مهارت TECH مرتبط + رنک این تخصص + 1d10. متریال یک رده قیمتی پایین‌تر از آیتم لازم است."
    "Invention Expertise" -> "اختراع آیتمی جدید! مکانیسم را برای GM توصیف کن؛ اگر بپذیرد، تاس: TECH + مهارت TECH مرتبط + رنک این تخصص + 1d10. سپس با Fabrication/Upgrade ساخته می‌شود."
    else -> ""
}

// =======================================================
// مدتک (Medicine) — سقف ۵ برای هر تخصص
// =======================================================
@Composable
fun MedtechRoleAbilities(
    character: Character,
    roleLevel: Int,
    points: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>,
    onSet: (String, Int) -> Unit,
    viewModel: CharacterViewModel
) {
    var selectedInfo by remember { mutableStateOf<String?>(null) }
    var pharmaRoll by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val green = Color(0xFF4CAF50)
    val surgeryPoints = points["surgery"] ?: 0
    val pharmaPoints = points["pharma"] ?: 0
    val cryoPoints = points["cryo"] ?: 0
    val allocated = surgeryPoints + pharmaPoints + cryoPoints
    val remaining = (roleLevel - allocated).coerceAtLeast(0)
    val surgerySkill = (surgeryPoints * 2).coerceAtMost(10)
    val medicalTechSkill = (pharmaPoints + cryoPoints).coerceAtMost(10)
    val unlocked = character.medtechPharmaceuticals.take(pharmaPoints)
    val synthRoll = rememberAutoDiceRoller(10) { d10 ->
        pharmaRoll = d10 to (character.stats.tech + medicalTechSkill + d10)
    }

    val lore = com.cyberpunk.gmtool.data.RoleLoreData
    var showMedtechDetail by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش مدتک (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.medtechIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: پزشکی» ──
        LoreBlock(lore.medtechAbilityTitle, lore.medtechAbilityBox)
        Spacer(Modifier.height(10.dp))
        LoreBlock("بخش پزشکی کتاب چه چیز دیگری به مدتک می‌دهد؟", lore.medtechMedicineSectionNote, compact = true)
        Spacer(Modifier.height(14.dp))

        Text(gtr("MEDTECH ASSISTANT"), color = Red, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text("Medicine Rank را بین Surgery، Pharmaceuticals و Cryosystem Operation تقسیم کن. این پنل هم allocation را نگه می‌دارد و هم هنگام بازی مسیر درست Rule را جلوی GM می‌گذارد.", color = Muted, fontSize = 13.sp, lineHeight = 22.sp, style = RtlJustify)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistStatBox("Surgery", "$surgerySkill", Modifier.weight(1f))
            AssistStatBox("Medical Tech", "$medicalTechSkill", Modifier.weight(1f))
            AssistStatBox("Remaining", "$remaining", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))

        MakerCounterRow("Surgery", surgeryPoints, remaining, 5,
            onInc = { onSet("surgery", surgeryPoints + 1) },
            onDec = { onSet("surgery", surgeryPoints - 1) },
            onInfo = { selectedInfo = "Surgery" })
        MakerCounterRow("Pharmaceuticals", pharmaPoints, remaining, 5,
            onInc = { onSet("pharma", pharmaPoints + 1) },
            onDec = { onSet("pharma", pharmaPoints - 1) },
            onInfo = { selectedInfo = "Pharmaceuticals" })

        if (pharmaPoints > 0) {
            Spacer(Modifier.height(8.dp))
            Text("داروهای Core — به ازای هر Pharmaceuticals Point یک داروی متفاوت انتخاب کن (${unlocked.size}/$pharmaPoints)", color = green, fontSize = 12.sp, lineHeight = 18.sp, style = Rtl)
            Spacer(Modifier.height(8.dp))
            com.cyberpunk.gmtool.data.RoleAssistantData.corePharmaceuticals.forEach { drug ->
                val selected = drug.name in unlocked
                Surface(
                    color = if (selected) Color(0xFF18351C) else CardBg,
                    shape = CutCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
                        .clickable(enabled = selected || unlocked.size < pharmaPoints) { viewModel.toggleMedtechPharmaceutical(character.id, drug.name) }
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (selected) "✓ ${drug.name}" else drug.name, color = if (selected) green else White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(if (selected) "UNLOCKED" else "TAP TO LEARN", color = Muted, fontSize = 10.sp)
                        }
                        Text(drug.effectFa, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        Text(drug.limitFa, color = Color(0xFFFFC107), fontSize = 11.sp, lineHeight = 17.sp)
                    }
                }
            }
            Text("ساخت Pharmaceutical: چک Medical Tech در DV13 (TECH + Medical Tech + 1d10). با ۲۰۰eb مواد، در ۱ ساعت به تعدادِ برابر مهارت Medical Tech خودت dose می‌سازی. شکست = مواد از بین می‌رود. با این تخصص نمی‌شود Street Drug ساخت.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp, style = RtlJustify)
            Spacer(Modifier.height(8.dp))
            RollButton("SYNTHESIZE • DV13", "+ ${character.stats.tech + medicalTechSkill}") { synthRoll() }
            pharmaRoll?.let { (die, total) ->
                Text(gtr("Roll: %1s • Total: %2s • %3s", die, total, if (total > 13) "SUCCESS — $medicalTechSkill dose" else "FAIL — materials lost"), color = if (total > 13) green else Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        MakerCounterRow("Cryosystem Operation", cryoPoints, remaining, 5,
            onInc = { onSet("cryo", cryoPoints + 1) },
            onDec = { onSet("cryo", cryoPoints - 1) },
            onInfo = { selectedInfo = "Cryosystem Operation" })
        if (cryoPoints > 0) {
            com.cyberpunk.gmtool.data.RoleAssistantData.cryoBenefits.filter { it.level <= cryoPoints }.forEach {
                RoleSubItemWithDescription(gtr("Cryo %1s", it.level), it.textFa)
            }
        }

        // ── قواعد تزریق دارو (Core) ──
        Spacer(Modifier.height(14.dp))
        lore.medtechDoseRules.forEach { (t, b) -> LoreBlock(t, b, compact = true) }

        // ── جدول سطح‌های Cryosystem Operation (۱ تا ۵) ──
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "مزایای سطح‌های Cryosystem Operation",
                    color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                    modifier = Modifier.weight(1f)
                )
                Text(gtr("امتیاز فعلی: %1s", cryoPoints), color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            lore.medtechCryoLevels.forEach { (level, text) ->
                val unlockedNow = cryoPoints >= level
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(if (unlockedNow) Red.copy(alpha = 0.14f) else Color.Transparent, CutCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            gtr("Level %1s", level),
                            color = if (unlockedNow) Black else Red, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            modifier = Modifier.width(58.dp)
                                .background(if (unlockedNow) Red else Red.copy(alpha = 0.15f), CutCornerShape(3.dp))
                                .padding(vertical = 3.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    FaText(
                        text, color = if (unlockedNow) White else Muted, fontSize = 12.sp, lineHeight = 19.sp,
                        justify = true, modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
        }

        // ── متن کامل تخصص‌ها (Core) — بازشو، تا کارت شلوغ نشود ──
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showMedtechDetail = !showMedtechDetail }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showMedtechDetail) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "تخصص‌های پزشکی به تفصیل (از کتاب)",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showMedtechDetail) {
            lore.medtechSpecialtyDetails.forEach { (t, b) -> LoreBlock(t, b, compact = true) }
        }

        Spacer(Modifier.height(16.dp))
        Text("راهنمای سریع GM", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        InfoItem("Stabilize", "Lightly Wounded DV10، Seriously Wounded DV13، Mortally Wounded DV15 با TECH + First Aid یا Paramedic. Mortally Wounded بعد از موفقیت فوراً 1 HP می‌شود و 1 دقیقه Unconscious است.")
        InfoItem("Critical Injury", "Quick Fix یک دقیقه و اثر را تا پایان روز حذف می‌کند؛ Treatment چهار ساعت و اثر را دائمی حذف می‌کند. Treatment روی خودت ممنوع است. Skill و DV را از خود Critical Injury بگیر؛ شدیدترین‌ها Surgery می‌خواهند.")
        InfoItem("Cyberware Surgery", "Installation/harvest چهار ساعت است. Mall DV13، Clinic DV15، Hospital DV17 با Surgery. شکست، cyberware را نابود و ۲ ساعت زمان را تلف می‌کند. نصب روی خودت فقط وقتی installation نوع Mall است مجاز است.")
        InfoItem("Therapy", "یک هفته‌ی کامل برای Doctor و Patient؛ Doctor باید Medtech دیگری با Medical Tech باشد. Addiction/Standard DV15 و Extreme DV17. Medtech نمی‌تواند روی خودش Therapy انجام دهد؛ شکست هفته و materials را هدر می‌دهد.")
        InfoItem("Airhypo", "تزریق یک dose به هدف willing یک Action است. برای هدف unwilling یک Melee Weapon Attack با Airhypo انجام می‌شود و روی hit به‌جای damage، dose تزریق می‌شود. Pharmaceutical را فقط Medtech می‌تواند درست administer کند.")

        Spacer(Modifier.height(20.dp))
    }

    selectedInfo?.let {
        SimpleInfoDialog(title = it, description = medtechInfo(it), onDismiss = { selectedInfo = null })
    }
}

private fun medtechInfo(name: String): String = when (name) {
    "Surgery" -> "به ازای هر امتیاز در Surgery، ۲ امتیاز در مهارت Surgery می‌گیری (تا حداکثر ۱۰). Surgery یک مهارت TECH است که برای درمان شدیدترین Critical Injuryها و کاشتن سایبرور استفاده می‌شود و فقط از طریق همین تخصص پزشکی در دست مدتک‌ها است."
    "Pharmaceuticals" -> "به ازای هر امتیاز در Medical Tech (Pharmaceuticals)، ۱ امتیاز در مهارت Medical Tech می‌گیری (تا حداکثر ۱۰) و به یک داروی متفاوت دسترسی پیدا می‌کنی. حداکثر ۵ امتیاز در این تخصص. ساخت دارو: چک Medical Tech در DV13؛ با ۲۰۰eb متریال، در یک ساعت به تعدادِ برابر مهارت Medical Tech خودت dose می‌سازی. با این تخصص نمی‌شود Street Drug ساخت."
    "Cryosystem Operation" -> "به ازای هر امتیاز در Medical Tech (Cryosystem Operation)، ۱ امتیاز در مهارت Medical Tech می‌گیری (تا حداکثر ۱۰). حداکثر ۵ امتیاز در این تخصص و با هر سطح، مزایای جدول Cryo (Cryopump، دسترسی به Cryotank، Cryotank شخصی، ظرفیت stasis) باز می‌شود."
    else -> ""
}

@Composable
private fun AssistStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = CardBg, shape = CutCornerShape(5.dp), modifier = modifier.border(1.dp, Color(0xFF444444), CutCornerShape(5.dp))) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(gtr(label), color = Muted, fontSize = 10.sp)
            Text(value, color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

// =======================================================
// فیکسر — تاس حالا در لحظه‌ی کلیک ریخته می‌شود
// =======================================================
@Composable
fun FixerRoleAbilities(character: Character, roleLevel: Int) {
    val lore = com.cyberpunk.gmtool.data.RoleLoreData
    var showHaggleDialog by remember { mutableStateOf(false) }
    var showAllBands by remember { mutableStateOf(false) }
    var rollResult by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    // همان محاسبه‌ی قبلی: COOL + Trading + Operator Rank (بدون تغییر مکانیک)
    val trading = character.skills.firstOrNull { it.name.equals("Trading", true) }?.level ?: 0
    val band = lore.fixerBandFor(roleLevel)
    val rollHaggle = rememberAutoDiceRoller(10) { d10 ->
        val bonus = roleLevel + character.stats.cool + trading
        rollResult = Triple(d10, bonus, d10 + bonus)
        showHaggleDialog = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش فیکسر (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.fixerIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: اپراتور» ──
        LoreBlock(lore.fixerAbilityTitle, lore.fixerAbilityBox)
        Spacer(Modifier.height(10.dp))
        lore.fixerOperatorParts.forEach { (t, b) -> LoreBlock(t, b, compact = true) }

        // ── دسته‌های قیمتی (برای فهم Reach) ──
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red.copy(alpha = 0.6f), CutCornerShape(6.dp)).padding(10.dp)
        ) {
            Text(
                "دسته‌های قیمتی کالا (Core)", color = Red, fontWeight = FontWeight.Bold,
                fontSize = 12.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                lore.fixerPriceCategories.joinToString("  •  "),
                color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth()
            )
        }

        // ── جدول رده‌ی Current: چهار مقدار همین رنک ──
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    gtr("رنک فعلی: %1s — اپراتور", roleLevel),
                    color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp, style = Rtl,
                    modifier = Modifier.weight(1f)
                )
                Text(band.ranksLabelFa, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            LoreRow("مخاطبین و مشتری‌ها (Contacts & Clients)", "", band.contactsFa)
            LoreRow("دسترسی (Reach)", "", band.reachFa)
            LoreRow("چانه‌زنی (Haggle)", "", band.haggleFa)
            LoreRow("Grease", "", band.greaseFa)
        }

        // ── همه‌ی رده‌ها ──
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showAllBands = !showAllBands }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showAllBands) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "همه‌ی رده‌ها (رنک ۱ تا ۱۰)",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showAllBands) {
            lore.fixerBands.forEach { b ->
                val current = roleLevel in b.range
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(Color(0xFF111111), CutCornerShape(4.dp))
                        .border(1.dp, if (current) Red else Color(0xFF333333), CutCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        if (current) b.ranksLabelFa + "  ← رنک فعلی" else b.ranksLabelFa,
                        color = if (current) Red else White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        style = Rtl, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    LoreRow("Contacts & Clients", "", b.contactsFa)
                    LoreRow("Reach", "", b.reachFa)
                    LoreRow("Haggle", "", b.haggleFa)
                    LoreRow("Grease", "", b.greaseFa)
                }
            }
        }

        // ── دکمه‌ی همیشگی Haggle — همان تاس قبلی، سر جای خودش ──
        Spacer(Modifier.height(14.dp))
        FaText(
            "قاعده‌ی معامله: اگر چک موفق شود، می‌توانی ۱ معامله با دسته‌ی رنک اپراتور خودت یا پایین‌تر انجام دهی و در هر معامله فقط ۱ معامله‌ی فیکسری ممکن است.",
            color = Color(0xFFFFC107), fontSize = 11.sp, lineHeight = 18.sp, justify = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        RollButton("HAGGLE ROLL", "+ $roleLevel") { rollHaggle() }
    }

    if (showHaggleDialog && rollResult != null) {
        val (d10, bonus, total) = rollResult!!
        DiceRollDialog(
            title = gtr("Haggle Roll"), roll = d10, total = total,
            breakdowns = listOf(gtr("%1s (1d10 Roll)", d10), gtr("+ %1s (Operator Rank)", roleLevel), gtr("+ %1s (COOL)", character.stats.cool), gtr("+ %1s (Trading)", trading), gtr("(طرف مقابل هم COOL + Trading + Operator Rank + 1d10 می‌ریزد)")),
            onDismiss = { showHaggleDialog = false }
        )
    }
}

// =======================================================
// نت‌رانر
// =======================================================
@Composable
fun NetrunnerRoleAbilities(roleLevel: Int) {
    var showDialog by remember { mutableStateOf(false) }
    var rollResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // همان محاسبه‌ی قبلی؛ هیچ مکانیکی عوض نشده.
    val netActions = when (roleLevel) {
        in 1..3 -> 2
        in 4..6 -> 3
        in 7..9 -> 4
        else -> 5
    }
    val lore = com.cyberpunk.gmtool.data.RoleLoreData
    val rollInterface = rememberAutoDiceRoller(10) { d10 ->
        rollResult = d10 to (d10 + roleLevel)
        showDialog = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش نت‌رانر (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.netrunnerIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: اینترفیس» ──
        LoreBlock(lore.netrunnerAbilityTitle, lore.netrunnerAbilityBox)
        Spacer(Modifier.height(12.dp))
        lore.netrunnerRules.forEach { (t, b) -> LoreBlock(t, b, compact = true) }
        Spacer(Modifier.height(14.dp))

        // ── جدول NET Action در هر نوبت، با برجسته‌سازی رده‌ی رنک فعلی ──
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    gtr("NET Actions Per Turn: %1s", netActions),
                    color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp, style = Rtl,
                    modifier = Modifier.weight(1f)
                )
                Text(gtr("رنک فعلی: %1s", roleLevel), color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Red.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            lore.netActionsBands.forEach { (label, range, count) ->
                val current = roleLevel in range
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(if (current) Red.copy(alpha = 0.14f) else Color.Transparent, CutCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (current) label + "  ← رنک فعلی" else label,
                        color = if (current) Red else White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        style = Rtl, modifier = Modifier.weight(1f)
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            gtr("%1s NET Action", count), color = if (current) Black else Red,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier
                                .background(if (current) Red else Red.copy(alpha = 0.15f), CutCornerShape(3.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // ── لیست توانایی‌های اینترفیس (متن کامل جدول کتاب) ──
        listOf(
            "Activate/Deactivate" to "فعال یا غیرفعال کردن برنامه‌های سایبردک.",
            "Backdoor" to lore.netrunnerAbilityData["Backdoor"],
            "Cloak" to lore.netrunnerAbilityData["Cloak"],
            "Control" to lore.netrunnerAbilityData["Control"],
            "Eye-Dee" to lore.netrunnerAbilityData["Eye-Dee"],
            "Jack In/Out" to "ورود/خروج ایمن (در شعاع ۶ متری اکسس‌پوینت).",
            "Pathfinder" to lore.netrunnerAbilityData["Pathfinder"],
            "Scanner" to lore.netrunnerAbilityData["Scanner"],
            "Slide" to lore.netrunnerAbilityData["Slide"],
            "Virus" to lore.netrunnerAbilityData["Virus"],
            "Zap" to lore.netrunnerAbilityData["Zap"]
        ).forEach { (t, d) -> RoleSubItemWithDescription(t, d ?: "") }
        Spacer(Modifier.height(16.dp))

        // ── دکمه‌ی همیشگی INTERFACE — همان تاس قبلی، سر جای خودش (انتهای توضیحات) ──
        Button(
            onClick = { rollInterface() },
            colors = ButtonDefaults.buttonColors(containerColor = Red),
            shape = CutCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(gtr("INTERFACE"), color = Black, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 2.sp) }
    }

    if (showDialog && rollResult != null) {
        DiceRollDialog("Interface Roll", rollResult!!.first, rollResult!!.second,
            listOf(gtr("%1s (1d10 Roll)", rollResult!!.first), gtr("+ %1s (Interface Rank)", roleLevel))) { showDialog = false }
    }
}

// =======================================================
// راکربوی
// =======================================================
@Composable
fun RockerboyRoleAbilities(roleLevel: Int) {
    var showDialog by remember { mutableStateOf(false) }
    var rollResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showAllBands by remember { mutableStateOf(false) }
    val rollImpact = rememberAutoDiceRoller(10) { d10 ->
        rollResult = d10 to (d10 + roleLevel)
        showDialog = true
    }
    val lore = com.cyberpunk.gmtool.data.RoleLoreData
    val band = lore.rockerboyBandFor(roleLevel)

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.rockerboyIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: تأثیر کاریزماتیک» ──
        LoreBlock(lore.rockerboyAbilityTitle, lore.rockerboyAbilityBox)
        Spacer(Modifier.height(16.dp))

        // ── قواعد کامل توانایی ──
        Text(
            "تأثیر کاریزماتیک چطور کار می‌کند؟",
            color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp, style = Rtl,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        lore.rockerboyRules.forEach { (title, body) -> LoreBlock(title, body, compact = true) }
        Spacer(Modifier.height(16.dp))

        // ── جدول رده‌ها بر اساس رنک همین کاراکتر (فقط نمایش؛ هیچ DV عوض نمی‌شود) ──
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    gtr("رنک فعلی: %1s", roleLevel),
                    color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp, style = Rtl,
                    modifier = Modifier.weight(1f)
                )
                Text(band.ranksLabelFa, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            LoreRow("مکان‌هایی که می‌توانی اجرا کنی", "", band.venueFa)
            LoreRow("تأثیر روی یک طرفدار", "DV8", band.singleFanFa)
            LoreRow("تأثیر روی یک گروه کوچک از طرفداران (تا ۶ نفر)", "DV10", band.smallGroupFa)
            LoreRow("تأثیر روی یک گروه عظیم از طرفداران", "DV12", band.hugeGroupFa)
        }

        // ── باقیِ رده‌ها: کامل و بدون خلاصه، برای وقتی که رنک عوض می‌شود ──
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showAllBands = !showAllBands }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showAllBands) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "همه‌ی رده‌ها (رنک ۱ تا ۱۰)",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showAllBands) {
            lore.rockerboyBands.forEach { b ->
                val current = roleLevel in b.range
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(Color(0xFF111111), CutCornerShape(4.dp))
                        .border(1.dp, if (current) Red else Color(0xFF333333), CutCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        if (current) b.ranksLabelFa + "  ← رنک فعلی" else b.ranksLabelFa,
                        color = if (current) Red else White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        style = Rtl, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    LoreRow("مکان‌های اجرا", "", b.venueFa)
                    LoreRow("یک طرفدار", "DV8", b.singleFanFa)
                    LoreRow("گروه کوچک (تا ۶ نفر)", "DV10", b.smallGroupFa)
                    LoreRow("گروه عظیم", "DV12", b.hugeGroupFa)
                }
            }
        }

        // ── دکمه‌ی همیشگی Charismatic Impact — همان تاس قبلی، سر جای خودش (انتهای توضیحات) ──
        Spacer(Modifier.height(16.dp))
        RollButton("CHARISMATIC IMPACT", "+ $roleLevel") { rollImpact() }
    }

    if (showDialog && rollResult != null) {
        DiceRollDialog("Charismatic Roll", rollResult!!.first, rollResult!!.second,
            listOf(gtr("%1s (1d10 Roll)", rollResult!!.first), gtr("+ %1s (Charismatic Impact Rank)", roleLevel))) { showDialog = false }
    }
}

@Composable
fun MediaRoleAbilities(character: Character, roleLevel: Int, viewModel: CharacterViewModel) {
    val profile = com.cyberpunk.gmtool.data.RoleAssistantData.mediaProfile(roleLevel)
    val lore = com.cyberpunk.gmtool.data.RoleLoreData
    var showMediaRanks by remember { mutableStateOf(false) }
    var showMediaPublish by remember { mutableStateOf(false) }
    var passiveResult by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var activeRumor by remember { mutableStateOf(com.cyberpunk.gmtool.data.RoleAssistantData.rumorLevels[0]) }
    val investigationSkills = remember(character.id, character.skills) {
        character.skills.map { it.name }.distinct().sorted().ifEmpty { listOf("Library Search", "Conversation", "Interrogation") }
    }
    var activeSkill by remember(character.id) { mutableStateOf(investigationSkills.firstOrNull { it.equals("Library Search", true) } ?: investigationSkills.first()) }
    var activeResult by remember { mutableStateOf<String?>(null) }
    var rumorMenu by remember { mutableStateOf(false) }
    var skillMenu by remember { mutableStateOf(false) }
    val passiveRoll = rememberAutoDiceRoller(10) { d10 ->
        val total = roleLevel + d10
        val hit = com.cyberpunk.gmtool.data.RoleAssistantData.rumorLevels.filter { total > it.passiveDv }.maxByOrNull { it.passiveDv }
        passiveResult = total to (hit?.let { gtr("%1s rumor", it.name) } ?: "No rumor")
    }
    val activeRoll = rememberAutoDiceRoller(10) { d10 ->
        val skill = character.skills.firstOrNull { it.name.equals(activeSkill, true) }?.level ?: 0
        val statName = com.cyberpunk.gmtool.data.SkillCatalog.statFor(activeSkill)
        val stat = when (statName.uppercase()) {
            "REF" -> character.stats.ref
            "DEX" -> character.stats.dex
            "TECH" -> character.stats.tech
            "COOL" -> character.stats.cool
            "WILL" -> character.stats.will
            "LUCK" -> character.stats.luck
            "MOVE" -> character.stats.move
            "BODY" -> character.stats.body
            "EMP" -> character.stats.emp
            else -> character.stats.int
        }
        val total = stat + skill + d10
        activeResult = gtr("%1s: %2s + %3s + %4s = %5s vs DV%6s → %7s", activeSkill, stat, skill, d10, total, activeRumor.activeDv, if (total > activeRumor.activeDv) "SUCCESS" else "FAIL")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش مدیا (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.mediaIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: اعتبار» ──
        LoreBlock(lore.mediaAbilityTitle, lore.mediaAbilityBox)
        Spacer(Modifier.height(14.dp))

        Text(gtr("MEDIA DESK"), color = Red, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text("Credibility یک Skill Check عمومی نیست. این Ability سه کار اصلی می‌کند: Rumorهای passive، دسترسی/مخاطب، و Believability هنگام Publish. این پنل همان workflow را مرحله‌به‌مرحله نگه می‌دارد.", color = Muted, fontSize = 13.sp, lineHeight = 22.sp, style = RtlJustify)
        Spacer(Modifier.height(12.dp))
        AssistStatBox("Credibility Rank", roleLevel.toString(), Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        InfoItem("Access / Sources", profile.accessFa)
        InfoItem("Audience", profile.audienceFa)
        InfoItem("Base Believability", "${profile.believability}/10")
        InfoItem("Maximum Impact", profile.impactFa)

        // ── همه‌ی رده‌های Credibility (رنک ۱ تا ۱۰) با برجسته‌سازی رنک فعلی ──
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showMediaRanks = !showMediaRanks }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showMediaRanks) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "همه‌ی رده‌ها (رنک ۱ تا ۱۰) — جدول Credibility کتاب",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showMediaRanks) {
            lore.mediaRanks.forEach { (label, range) ->
                val pr = com.cyberpunk.gmtool.data.RoleAssistantData.mediaProfile(range.first)
                val current = roleLevel in range
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(Color(0xFF111111), CutCornerShape(4.dp))
                        .border(1.dp, if (current) Red else Color(0xFF333333), CutCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        if (current) label + "  ← رنک فعلی" else label,
                        color = if (current) Red else White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        style = Rtl, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    LoreRow("Access / Sources", "", pr.accessFa)
                    LoreRow("Audience", "", pr.audienceFa)
                    LoreRow("Believability", gtr("%1s/10", pr.believability), "")
                    LoreRow("Impact", "", pr.impactFa)
                }
            }
        }

        // ── قواعد انتشار Story/Scoop (Core) — بازشو ──
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showMediaPublish = !showMediaPublish }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showMediaPublish) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "قواعد انتشار Story / Scoop (از کتاب)",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showMediaPublish) {
            lore.mediaPublishing.forEach { (t, b) -> LoreBlock(t, b, compact = true) }
            lore.mediaPublishingLimits.forEach { (t, b) -> LoreBlock(t, b, compact = true) }
        }

        Spacer(Modifier.height(14.dp))
        Text(gtr("1) PASSIVE RUMORS"), color = White, fontWeight = FontWeight.Bold)
        lore.mediaRumorRules.forEach { (t, b) -> LoreBlock(t, b, compact = true) }
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Text(
                "جدول شایعه‌ها (Rumor Table)",
                color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("نوع شایعه", color = Muted, fontSize = 11.sp, style = Rtl, modifier = Modifier.weight(1f))
                Text("Passive", color = Muted, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                Text("Active", color = Muted, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = Red.copy(alpha = 0.5f))
            com.cyberpunk.gmtool.data.RoleAssistantData.rumorLevels.forEach { r ->
                val isPicked = r.name == activeRumor.name
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .background(if (isPicked) Red.copy(alpha = 0.12f) else Color.Transparent, CutCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isPicked) gtr("%1s (انتخاب‌شده)", r.name) else r.name,
                            color = if (isPicked) Red else White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            style = Rtl, modifier = Modifier.weight(1f)
                        )
                        Text(gtr("%1s", r.passiveDv), color = White, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                        Text(gtr("%1s", r.activeDv), color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    }
                    FaText(r.descriptionFa, color = Muted, fontSize = 11.sp, lineHeight = 17.sp, justify = true, modifier = Modifier.fillMaxWidth())
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
            Spacer(Modifier.height(8.dp))
            FaText(lore.mediaRumorNote, color = Color(0xFFFFC107), fontSize = 12.sp, lineHeight = 19.sp, justify = true, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(7.dp))
        RollButton("SECRET PASSIVE RUMOR", "+ $roleLevel") { passiveRoll() }
        passiveResult?.let { Text(gtr("GM result: %1s → %2s", it.first, it.second), color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp)) }

        Spacer(Modifier.height(16.dp))
        Text(gtr("2) ACTIVE INVESTIGATION"), color = White, fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { rumorMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(gtr("Rumor: %1s • DV%2s", activeRumor.name, activeRumor.activeDv)) }
            DropdownMenu(expanded = rumorMenu, onDismissRequest = { rumorMenu = false }) {
                com.cyberpunk.gmtool.data.RoleAssistantData.rumorLevels.forEach { r ->
                    DropdownMenuItem(text = { Text(gtr("%1s — Active DV%2s", r.name, r.activeDv)) }, onClick = { activeRumor = r; rumorMenu = false })
                }
            }
        }
        Text(activeRumor.descriptionFa, color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(7.dp))
        Box {
            OutlinedButton(onClick = { skillMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(gtr("Investigation Skill: %1s", activeSkill)) }
            DropdownMenu(expanded = skillMenu, onDismissRequest = { skillMenu = false }) {
                investigationSkills.forEach { sk -> DropdownMenuItem(text = { Text(gtr(sk)) }, onClick = { activeSkill = sk; skillMenu = false }) }
            }
        }
        Spacer(Modifier.height(7.dp))
        RollButton("ACTIVE RUMOR CHECK", "DV${activeRumor.activeDv}") { activeRoll() }
        activeResult?.let { Text(it, color = if (it.endsWith("SUCCESS")) Color(0xFF4CAF50) else Red, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp)) }

        Spacer(Modifier.height(18.dp))
        Text(gtr("3) INVESTIGATION BOARD"), color = White, fontWeight = FontWeight.Bold)
        Text("Statusها فقط GM helper هستند؛ Rule رسمی را تغییر نمی‌دهند. Evidence Count برای Believability واقعی استفاده می‌شود.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp, style = Rtl)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.addMediaCase(character.id) }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null, tint = Black); Spacer(Modifier.width(6.dp)); Text(gtr("ADD INVESTIGATION"), color = Black, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        character.mediaCases.forEach { case ->
            MediaCaseCard(character, case, profile.believability, viewModel)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text(gtr("GM Reminder"), color = White, fontWeight = FontWeight.Bold)
        InfoItem("Evidence bonus", "حداقل ۱ piece قابل راستی‌آزمایی و قابل فهم برای عموم: +1 به شانس Believability. بیش از ۴ piece متمایز: +2 دیگر. این دو stack می‌شوند. LUCK روی Believability ممنوع است.")
        InfoItem("Same topic", "بعد از انتشار، Media نمی‌تواند دوباره روی دقیقاً همان موضوع Story منتشر کند مگر اطلاعات جدیدی به گفتگو اضافه کرده باشد.")
        InfoItem("Rumor ≠ Truth", "Rumor ذاتاً ممکن است غلط باشد و هرگز کل Story نیست. موفقیت Rumor Check فقط lead می‌دهد، نه اثبات حقیقت.")

        // ── کادر کنارِ صفحه‌ی Credibility در کتاب ──
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF0B0B0B), CutCornerShape(4.dp))
                .border(1.dp, Red.copy(alpha = 0.45f), CutCornerShape(4.dp)).padding(12.dp)
        ) {
            Text(
                "Credibility تأثیر بزرگی دارد",
                color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            FaText(lore.mediaImpactSidebar, color = White, fontSize = 12.sp, lineHeight = 20.sp, justify = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MediaCaseCard(
    character: Character,
    mediaCase: com.cyberpunk.gmtool.data.MediaCase,
    baseBelievability: Int,
    viewModel: CharacterViewModel
) {
    var publishResult by remember(mediaCase.id, mediaCase.evidenceCount, mediaCase.published) { mutableStateOf<String?>(null) }
    var statusMenu by remember { mutableStateOf(false) }
    val evidenceBonus = (if (mediaCase.evidenceCount >= 1) 1 else 0) + (if (mediaCase.evidenceCount > 4) 2 else 0)
    val chance = (baseBelievability + evidenceBonus).coerceAtMost(10)
    val publishRoll = rememberAutoDiceRoller(10) { d10 ->
        val ok = d10 <= chance
        publishResult = gtr("Believability: %1s vs %2s/10 → %3s", d10, chance, if (ok) "AUDIENCE BUYS IT" else "AUDIENCE DOESN'T BUY IT")
        viewModel.updateMediaCase(character.id, mediaCase.id) { it.copy(published = true, status = "Published") }
    }
    Surface(color = CardBg, shape = CutCornerShape(7.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF444444), CutCornerShape(7.dp))) {
        Column(Modifier.padding(10.dp)) {
            OutlinedTextField(
                value = mediaCase.title,
                onValueChange = { v -> viewModel.updateMediaCase(character.id, mediaCase.id) { it.copy(title = v) } },
                label = { Text(gtr("Story / Case")) }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = mediaCase.topic,
                onValueChange = { v -> viewModel.updateMediaCase(character.id, mediaCase.id) { it.copy(topic = v) } },
                label = { Text(gtr("Exact Topic")) }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = mediaCase.sources,
                onValueChange = { v -> viewModel.updateMediaCase(character.id, mediaCase.id) { it.copy(sources = v) } },
                label = { Text(gtr("Sources / Interviews")) }, modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = mediaCase.evidence,
                onValueChange = { v -> viewModel.updateMediaCase(character.id, mediaCase.id) { it.copy(evidence = v) } },
                label = { Text(gtr("Verifiable Evidence notes")) }, modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(gtr("Distinct evidence: %1s", mediaCase.evidenceCount), color = White, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.updateMediaCase(character.id, mediaCase.id) { it.copy(evidenceCount = (it.evidenceCount - 1).coerceAtLeast(0)) } }) { Icon(Icons.Default.Remove, null, tint = White) }
                IconButton(onClick = { viewModel.updateMediaCase(character.id, mediaCase.id) { it.copy(evidenceCount = it.evidenceCount + 1) } }) { Icon(Icons.Default.Add, null, tint = White) }
            }
            Box {
                OutlinedButton(onClick = { statusMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(gtr("Status: %1s", mediaCase.status)) }
                DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                    listOf("Lead", "Investigating", "Corroborated", "Ready to Publish", "Published").forEach { st ->
                        DropdownMenuItem(text = { Text(gtr(st)) }, onClick = { viewModel.updateMediaCase(character.id, mediaCase.id) { it.copy(status = st, published = st == "Published") }; statusMenu = false })
                    }
                }
            }
            Text(gtr("Believability now: %1s/10 (base %2s + evidence %3s). LUCK cannot be spent.", chance, baseBelievability, evidenceBonus), color = Color(0xFFFFC107), fontSize = 11.sp, modifier = Modifier.padding(vertical = 7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { publishRoll() }, enabled = mediaCase.topic.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.weight(1f)) {
                    Text(if (mediaCase.published) "REPUBLISH W/ NEW INFO" else "PUBLISH", color = Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { viewModel.deleteMediaCase(character.id, mediaCase.id) }) { Icon(Icons.Default.Delete, null, tint = Red) }
            }
            publishResult?.let { Text(it, color = if (it.contains("BUYS IT")) Color(0xFF4CAF50) else Red, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp)) }
        }
    }
}

// =======================================================
// اگزک
// =======================================================
@Composable
fun ExecRoleAbilities(character: Character, roleLevel: Int, viewModel: CharacterViewModel) {
    val lore = com.cyberpunk.gmtool.data.ExecTeamData
    val allChars by viewModel.characters.collectAsState()
    val team = allChars.filter { !it.isAlly && it.npcCategory == com.cyberpunk.gmtool.data.NpcData.CAT_EXEC }
    val maxTeam = lore.maxTeamMembers(roleLevel)
    var selectedClass by remember { mutableStateOf(lore.teamMemberClasses.first()) }
    var classMenu by remember { mutableStateOf(false) }
    var showClassTable by remember { mutableStateOf(false) }
    var addToEncounter by remember { mutableStateOf(false) }
    var hireMsg by remember { mutableStateOf<String?>(null) }
    // Loyalty شروع یک Team Member طبق کتاب: 1d6 + 1
    val rollLoyalty = rememberAutoDiceRoller(6) { d6 ->
        hireMsg = viewModel.hireExecTeamMember(selectedClass.name, d6 + 1, addToEncounter)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش اگزک (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.introFa.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: کار تیمی» ──
        LoreBlock(lore.abilityTitle, lore.abilityBox)
        Spacer(Modifier.height(14.dp))

        // ── سهمیه‌ی Team Member بر اساس رنک ──
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, Red, CutCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gtr("Team Members: %1s / %2s", team.size, maxTeam), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (maxTeam == 0) "از رنک ۳ به بعد اولین Team Member را می‌گیری (رنک ۵ و ۹ یکی دیگر هم می‌دهند؛ سقف ۳)."
                    else "سقف این رنک: %1s نفر. اعضای ساخته‌شده در GM Tools → ROLE HELPERS → Exec مدیریت می‌شوند.".replace("%1s", maxTeam.toString()),
                    color = Muted, fontSize = 11.sp, style = Rtl, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── استخدام Team Member (دکمه‌ی Hire که حالا واقعاً NPC می‌سازد) ──
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Text(
                "استخدام Team Member", color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            FaText(
                "کلاس را انتخاب کن؛ HR همان کلاس را با جدول STAT مخصوص خودش استخدام می‌کند و Loyalty شروعش با 1d6 + 1 تعیین می‌شود. NPC ساخته‌شده در فهرست کاراکترها (دسته‌ی «تیم اگزک») ذخیره می‌شود.",
                color = Muted, fontSize = 11.sp, lineHeight = 18.sp, justify = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(onClick = { classMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(gtr("کلاس: %1s", selectedClass.name), color = Red, fontSize = 13.sp)
                }
                DropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                    lore.teamMemberClasses.forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(gtr("%1s — %2s", cls.name, cls.nameFa)) },
                            onClick = { selectedClass = cls; classMenu = false; showClassTable = true }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            InfoItem("نقش پوششی", selectedClass.coverJobsFa)
            InfoItem("کار واقعی", selectedClass.trueJobFa)
            InfoItem("مهارت‌ها +۲/+۴/+۶", selectedClass.skills2Fa + "  |  " + selectedClass.skills4Fa + "  |  " + selectedClass.skills6Fa)
            InfoItem("سایبرور", selectedClass.cyberwareFa)
            InfoItem("تجهیزات", selectedClass.gearFa)

            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().clickable { showClassTable = !showClassTable }
                    .background(CardBg, CutCornerShape(4.dp))
                    .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (showClassTable) "▾" else "▸", color = Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "جدول STAT همین کلاس (1d6)", color = White, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, style = Rtl, modifier = Modifier.weight(1f)
                )
            }
            if (showClassTable) {
                Spacer(Modifier.height(8.dp))
                ExecStatTable(selectedClass)
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clickable { addToEncounter = !addToEncounter }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = addToEncounter, onCheckedChange = { addToEncounter = it })
                Text("مستقیم به فهرست شرکت‌کننده‌های نبرد هم اضافه شود", color = White, fontSize = 12.sp, style = Rtl)
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { rollLoyalty() },
                colors = ButtonDefaults.buttonColors(containerColor = Red),
                shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(gtr("HIRE • استخدام با 1d6+1 Loyalty"), color = Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            hireMsg?.let {
                Spacer(Modifier.height(8.dp))
                FaText(it, color = Color(0xFF4CAF50), fontSize = 12.sp, lineHeight = 19.sp, justify = true, modifier = Modifier.fillMaxWidth())
            }
        }

        // ── مزایای رنک (Signing Bonus / Housing / Insurance) ──
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Text(
                "مزایای رنک کار تیمی", color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            lore.rankPerks.forEach { (rank, title, body) ->
                val active = roleLevel >= rank
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(if (active) Red.copy(alpha = 0.14f) else Color.Transparent, CutCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                gtr("Rank %1s", rank),
                                color = if (active) Black else Red, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                modifier = Modifier.background(if (active) Red else Red.copy(alpha = 0.15f), CutCornerShape(3.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (active) title + "  ✓" else title,
                            color = if (active) Red else White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            style = Rtl, modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    FaText(body, color = if (active) White else Muted, fontSize = 12.sp, lineHeight = 19.sp, justify = true, modifier = Modifier.fillMaxWidth())
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
        }

        // ── قواعد Team Memberها (Core) ──
        Spacer(Modifier.height(12.dp))
        lore.teamRules.forEach { (t, b) -> LoreBlock(t, b, compact = true) }

        // ── جدول‌های Loyalty ──
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Text(
                "Loyalty چطور بالا و پایین می‌رود؟", color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("به‌دست‌آوردن Loyalty", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp, style = Rtl, modifier = Modifier.fillMaxWidth())
            lore.loyaltyGain.forEach { (text, gain) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Text(text, color = White, fontSize = 12.sp, lineHeight = 18.sp, style = Rtl, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            gtr("+%1s", gain), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier.background(Color(0xFF4CAF50), CutCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("از دست دادن Loyalty", color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp, style = Rtl, modifier = Modifier.fillMaxWidth())
            lore.loyaltyLoss.forEach { (text, loss) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Text(text, color = White, fontSize = 12.sp, lineHeight = 18.sp, style = Rtl, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            gtr("%1s", loss), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier.background(Red, CutCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // ── Team Memberهای ساخته‌شده (فقط نمایش؛ مدیریت در GM Tools) ──
        if (team.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LoreBlock(
                "Team Memberهای فعلی",
                team.joinToString("\n") { c ->
                    "• ${c.name} — Loyalty: ${com.cyberpunk.gmtool.viewmodel.teamMemberLoyalty(c)}" +
                        if (c.hp < c.maxHp) " (HP ${c.hp}/${c.maxHp})" else ""
                }
            )
        }

        // ── همه‌ی کلاس‌های Team Member با جدول کامل ──
        Spacer(Modifier.height(12.dp))
        Text(
            "پنج کلاس Team Member (کتاب Core)", color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            style = Rtl, modifier = Modifier.fillMaxWidth()
        )
        lore.teamMemberClasses.forEach { cls ->
            var expanded by remember(cls.name) { mutableStateOf(false) }
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp)
                    .background(Color(0xFF111111), CutCornerShape(4.dp))
                    .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp))
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (expanded) "▾" else "▸", color = Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(gtr(cls.name), color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, style = Rtl, modifier = Modifier.fillMaxWidth())
                        Text(gtr(cls.nameFa), color = Muted, fontSize = 11.sp, style = Rtl, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (expanded) {
                    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                        FaText("نقش پوششی: ${cls.coverJobsFa}", color = Muted, fontSize = 11.sp, justify = true, modifier = Modifier.fillMaxWidth())
                        FaText("کار واقعی: ${cls.trueJobFa}", color = Muted, fontSize = 11.sp, justify = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        ExecStatTable(cls)
                        Spacer(Modifier.height(8.dp))
                        FaText("مهارت‌های +۲: ${cls.skills2Fa}", color = White, fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth())
                        FaText("مهارت‌های +۴: ${cls.skills4Fa}", color = White, fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth())
                        if (cls.skills6Fa != "—") FaText("مهارت‌های +۶: ${cls.skills6Fa}", color = White, fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        FaText("سایبرور: ${cls.cyberwareFa}", color = Muted, fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth())
                        FaText("تجهیزات: ${cls.gearFa}", color = Muted, fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** جدول STAT شش‌ردیفی کلاس‌های Team Member (همان جدول 1d6 کتاب). */
@Composable
internal fun ExecStatTable(cls: com.cyberpunk.gmtool.data.ExecTeamClass) {
    val headers = listOf("INT", "REF", "DEX", "TECH", "COOL", "WILL", "MOVE", "BODY", "EMP")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF0B0B0B), CutCornerShape(4.dp))
                .border(1.dp, Red.copy(alpha = 0.5f), CutCornerShape(4.dp))
                .horizontalScroll(rememberScrollState())
        ) {
            Row(Modifier.background(Red.copy(alpha = 0.25f))) {
                Text("1d6", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(34.dp).padding(vertical = 5.dp), textAlign = TextAlign.Center)
                headers.forEach { h ->
                    Text(h, color = Red, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(34.dp).padding(vertical = 5.dp), textAlign = TextAlign.Center)
                }
            }
            cls.statRows.forEachIndexed { idx, row ->
                Row(Modifier.background(if (idx % 2 == 0) Color(0xFF141414) else Color.Transparent)) {
                    Text(gtr("%1s", idx + 1), color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(34.dp).padding(vertical = 4.dp), textAlign = TextAlign.Center)
                    row.forEach { v ->
                        Text(gtr("%1s", v), color = White, fontSize = 11.sp, modifier = Modifier.width(34.dp).padding(vertical = 4.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun NpcItem(title: String, role: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF333333), CutCornerShape(4.dp))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(gtr(title), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(gtr(role), color = Muted, fontSize = 13.sp)
        }
    }
}

// =======================================================
// لاومن (Backup)
// =======================================================
@Composable
fun LawmanRoleAbilities(roleLevel: Int) {
    val lore = com.cyberpunk.gmtool.data.RoleLoreData
    val rules = com.cyberpunk.gmtool.data.BackupRules
    var d10 by remember { mutableStateOf(0) }
    var d6 by remember { mutableStateOf(0) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRoundsDialog by remember { mutableStateOf(false) }
    var showAvailableDialog by remember { mutableStateOf(false) }
    var showExample by remember { mutableStateOf(false) }
    var showRank10 by remember { mutableStateOf(false) }

    val rollBackup = rememberAutoDiceRoller(10) { value -> d10 = value; showBackupDialog = true }
    val rollArrival = rememberAutoDiceRoller(6) { value -> d6 = value; showRoundsDialog = true }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش لاومن (ترجمه‌ی کامل Core، بدون خلاصه‌سازی) ──
        lore.lawmanIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── کادر «توانایی نقش: پشتیبان» ──
        LoreBlock(lore.lawmanAbilityTitle, lore.lawmanAbilityBox)
        Spacer(Modifier.height(14.dp))

        // ── دو تاس همیشگی: تماس و رسیدن نیرو ──
        RollButton("درخواست پشتیبان (CALL BACKUP)", "1d10 ≤ $roleLevel", diceSides = 10) { rollBackup() }
        Spacer(Modifier.height(12.dp))
        RollButton("زمان رسیدن نیروها (ARRIVAL)", "1d6", diceSides = 6) { rollArrival() }
        Spacer(Modifier.height(12.dp))

        // ── جدول رده‌های Backup با برجسته‌سازی رده‌ی همین رنک ──
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    gtr("رده‌ی پشتیبان رنک %1s", roleLevel),
                    color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp, style = Rtl,
                    modifier = Modifier.weight(1f)
                )
                Text(gtr("Combat Number %1s", rules.unitForRank(roleLevel).combatNumber), color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            rules.tiers.distinctBy { it.tierName }.forEach { u ->
                val current = u.tierName == rules.unitForRank(roleLevel).tierName
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(if (current) Red.copy(alpha = 0.14f) else Color.Transparent, CutCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (current) u.tierName + "  ← رده‌ی فعلی" else u.tierName,
                            color = if (current) Red else White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            style = Rtl, modifier = Modifier.weight(1f)
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                gtr("×%1s", u.count), color = if (current) Black else Red, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                modifier = Modifier.background(if (current) Red else Red.copy(alpha = 0.15f), CutCornerShape(3.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    FaText(u.descriptionFa, color = if (current) White else Muted, fontSize = 12.sp, lineHeight = 19.sp, justify = true, modifier = Modifier.fillMaxWidth())
                    Text(
                        gtr("CN %1s • SP %2s • HP %3s • MOVE/BODY %4s • %5s", u.combatNumber, u.sp, u.hp, u.moveBody, u.transportFa),
                        color = Muted, fontSize = 10.sp
                    )
                    if (u.templateNoteFa.isNotBlank()) FaText(u.templateNoteFa, color = Color(0xFFFFC107), fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
        }

        // ── قواعد کامل Backup ──
        Spacer(Modifier.height(12.dp))
        lore.lawmanRules.forEach { (t, b) -> LoreBlock(t, b, compact = true) }

        // ── رنک ۱۰: تفاوت‌ها و مهارت‌های Combat Number ──
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showRank10 = !showRank10 }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showRank10) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "رنک ۱۰ چه فرقی دارد؟ (پلیس ملی / Interpol / Netwatch)",
                color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showRank10) {
            lore.lawmanRank10Rules.forEach { (t, b) -> LoreBlock(t, b, compact = true) }
        }

        // ── کادر «Backup in Action» کتاب ──
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showExample = !showExample }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showExample) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "نمونه‌ی اجرا در بازی: Backup in Action",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showExample) {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp)
                    .background(Color(0xFF0B0B0B), CutCornerShape(4.dp))
                    .border(1.dp, Red.copy(alpha = 0.45f), CutCornerShape(4.dp)).padding(12.dp)
            ) {
                lore.lawmanExample.forEach { paragraph ->
                    FaText(paragraph, color = White, fontSize = 12.sp, lineHeight = 20.sp, justify = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { showAvailableDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = CardBg), shape = CutCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, Color.DarkGray, CutCornerShape(8.dp))) {
            Text("نیروهای پشتیبان در دسترس", color = Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp, style = Rtl)
        }
    }

    if (showBackupDialog) {
        val success = d10 <= roleLevel
        LawmanDiceDialog(10, d10,
            if (success) "موفقیت! بی‌سیم تایید شد، نیروی پشتیبانی در راه است." else "شکست! کسی به درخواست پاسخ نداد — نوبت بعد می‌توانی دوباره تلاش کنی.",
            success) { showBackupDialog = false }
    }
    if (showRoundsDialog) {
        val upgraded = d6 == 6
        val unit = rules.unitForRank(roleLevel)
        val arrives = if (upgraded) rules.upgradedUnit(roleLevel) else unit
        val count = if (upgraded && rules.upgradedCountIsDoubled(roleLevel)) arrives.count * 2 else arrives.count
        val msg = if (upgraded)
            gtr("تاس ۶ → رده یک پله بالا رفت: در %1s راند «%2s» می‌رسد (تعداد %3s).", d6, arrives.tierName, count)
        else gtr("نیروهای پشتیبان در %1s راند می‌رسند → «%2s» (تعداد %3s).", d6, arrives.tierName, count)
        LawmanDiceDialog(6, d6, msg, true) { showRoundsDialog = false }
    }
    if (showAvailableDialog) {
        Dialog(onDismissRequest = { showAvailableDialog = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(12.dp),
                modifier = Modifier.fillMaxHeight(0.8f).border(1.dp, Red, CutCornerShape(12.dp))) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(gtr("Lawman Backup"), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        gtr("با رنک %1s این رده‌ها در دسترس‌اند (رنک خودت یا پایین‌تر):", roleLevel),
                        color = Muted, fontSize = 12.sp, style = Rtl, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(rules.tiers.distinctBy { it.tierName }.filter { rules.tiers.indexOfFirst { t -> t.tierName == it.tierName } + 1 <= roleLevel }) { u ->
                            NpcItem(u.tierName + " ×" + u.count, gtr("CN %1s • SP %2s • HP %3s", u.combatNumber, u.sp, u.hp))
                            FaText(u.descriptionFa, color = Muted, fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        }
                    }
                    Button(onClick = { showAvailableDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) {
                        Text("بستن", color = Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =======================================================
// نومد
// =======================================================
@Composable
fun NomadRoleAbilities(character: Character, roleLevel: Int, viewModel: CharacterViewModel) {
    val lore = com.cyberpunk.gmtool.data.RoleLoreData
    var showMotorpoolEditor by remember { mutableStateOf(false) }
    var showAvailableVehicles by remember { mutableStateOf(false) }
    var showAvailableUpgrades by remember { mutableStateOf(false) }
    var showUpgradeRef by remember { mutableStateOf(false) }
    var showMotorpoolRules by remember { mutableStateOf(false) }
    var itemDetailLabel by remember { mutableStateOf<String?>(null) }
    var nomadMsg by remember { mutableStateOf<String?>(null) }

    val rank = roleLevel.coerceIn(1, 10)
    val vehicles = character.nomadMotorpool.filter { com.cyberpunk.gmtool.data.VehicleCatalog.byName(it) != null }
    val legacyVehicles = character.nomadMotorpool.filter { com.cyberpunk.gmtool.data.VehicleCatalog.byName(it) == null }
    val parsedUpgrades = character.nomadVehicleUpgrades.mapNotNull { raw ->
        val parts = raw.split("||", limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else null
    }
    val legacyUpgrades = character.nomadVehicleUpgrades.filter { !it.contains("||") }
    val usedPicks = vehicles.size + parsedUpgrades.size + legacyVehicles.size + legacyUpgrades.size
    val remainingPicks = (rank - usedPicks).coerceAtLeast(0)
    val familyOwned = character.ownedVehicles.filter { it.source == "FAMILY" }
    val outNow = familyOwned.firstOrNull { it.familyCheckedOut && it.destroyedUntil <= 0L }
    val pendingSwap = familyOwned.firstOrNull { it.id == character.pendingFamilyVehicleId }
    val band = lore.nomadFamilyBands.firstOrNull { rank in it.range }

    // ── چک مهارت‌های آشنایی (بونوس Moto داخل عدد هست) ──
    val familiaritySkills = listOf(
        "Drive Land Vehicle", "Pilot Air Vehicle", "Pilot Sea Vehicle",
        "Air Vehicle Tech", "Land Vehicle Tech", "Sea Vehicle Tech"
    )
    var selectedSkill by remember(character.id) { mutableStateOf(familiaritySkills.first()) }
    var skillMenu by remember { mutableStateOf(false) }
    var driveResult by remember { mutableStateOf<String?>(null) }
    val rollFamiliarity = rememberAutoDiceRoller(10) { d10 ->
        val total = nomadSkillTotal(character, selectedSkill)
        driveResult = gtr("%1s: 1d10(%2s) + %3s = %4s", selectedSkill, d10, total, d10 + total)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── توضیح کامل نقش نامد ──
        lore.nomadIntro.forEach { paragraph ->
            FaText(
                paragraph, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                justify = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }

        // ── توانایی نقش: موتو ──
        LoreBlock(lore.nomadAbilityTitle, lore.nomadAbilityBox)
        Spacer(Modifier.height(10.dp))

        // ── آشنایی با وسایل نقلیه + رولر زنده ──
        LoreBlock(lore.nomadFamiliarityTitle, lore.nomadFamiliarityBody)
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp)
        ) {
            Text(
                gtr("رنک موتو: %1s — این عدد به چک‌های زیر اضافه می‌شود", rank),
                color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp, style = Rtl,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(onClick = { skillMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(gtr("مهارت: %1s", selectedSkill), color = Red, fontSize = 12.sp)
                }
                DropdownMenu(expanded = skillMenu, onDismissRequest = { skillMenu = false }) {
                    familiaritySkills.forEach { sk ->
                        DropdownMenuItem(
                            text = { Text(gtr("%1s (مجموع %2s)", sk, nomadSkillTotal(character, sk))) },
                            onClick = { selectedSkill = sk; skillMenu = false; driveResult = null }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                gtr("مجموع فعلی (شامل رنک موتو): %1s", nomadSkillTotal(character, selectedSkill)),
                color = White, fontSize = 11.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            RollButton("چک رانندگی / مکانیک با بونوس Moto", "+ $rank Moto") { rollFamiliarity() }
            driveResult?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Color(0xFF4CAF50), fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(8.dp))
        FaText(lore.nomadFamiliarityNote, color = Muted, fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth())

        // ── Family Motorpool: وضعیت زنده ──
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red, CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ناوگان خانواده (Family Motorpool)", color = Red, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, style = Rtl, modifier = Modifier.weight(1f)
                )
                Text(band?.ranksLabelFa ?: "", color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                gtr("پیک‌های Moto: %1s از %2s استفاده شده • باقی‌مانده %3s", usedPicks, rank, remainingPicks),
                color = if (remainingPicks > 0) Color(0xFF4CAF50) else Muted,
                fontSize = 12.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Text(
                "هر پیک یا یک وسیله‌ی تازه به ناوگان اضافه می‌کند یا یک ارتقا روی وسیله‌ای موجود.",
                color = Muted, fontSize = 10.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Red.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    outNow != null -> gtr("بیرون از Motorpool: %1s", outNow.specName)
                    rank >= 10 -> "رنک ۱۰: هر تعداد Family Vehicle سالم می‌تواند هم‌زمان بیرون باشد."
                    else -> "الآن هیچ Family Vehicleی بیرون نیست."
                },
                color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold, style = Rtl,
                modifier = Modifier.fillMaxWidth()
            )
            if (pendingSwap != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    gtr("در انتظار تعویض: %1s (صبح روز بعد)", pendingSwap.specName),
                    color = Color(0xFFFFC107), fontSize = 11.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { nomadMsg = viewModel.completeFamilyVehicleSwapNextMorning(character.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("صبح شد — تعویض را انجام بده", color = Red, fontSize = 11.sp) }
            }
            familyOwned.filter { it.destroyedUntil > 0L }.forEach { v ->
                Spacer(Modifier.height(4.dp))
                Text(
                    gtr("%1s در تعمیر Family است — %2s روز مانده", v.specName, v.destroyedUntil),
                    color = Red, fontSize = 11.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
                )
            }
            if (familyOwned.any { it.destroyedUntil > 0L }) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { nomadMsg = viewModel.advanceFamilyRepairDay(character.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("یک روز درون‌بازی جلو برو", color = White, fontSize = 11.sp) }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showMotorpoolEditor = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = CutCornerShape(6.dp), modifier = Modifier.weight(1f)
                ) { Text(gtr("ویرایش ناوگان (%1s)", vehicles.size), color = Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                OutlinedButton(
                    onClick = { showMotorpoolRules = !showMotorpoolRules },
                    shape = CutCornerShape(6.dp), modifier = Modifier.weight(1f)
                ) { Text("قواعد Motorpool", color = Red, fontSize = 11.sp) }
            }
        }
        if (showMotorpoolRules) {
            Spacer(Modifier.height(8.dp))
            lore.nomadMotorpoolRules.forEach { (t, b) -> LoreBlock(t, b, compact = true) }
        }

        // ── جدول Family Motorpool (رنک → وسیله) ──
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF111111), CutCornerShape(6.dp))
                .border(1.dp, Red.copy(alpha = 0.6f), CutCornerShape(6.dp)).padding(12.dp)
        ) {
            Text(
                "جدول Family Motorpool (Core)", color = Red, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            lore.nomadFamilyBands.forEach { b ->
                val current = rank in b.range
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(if (current) Red.copy(alpha = 0.14f) else Color.Transparent, CutCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (current) b.ranksLabelFa + "  ← رنک فعلی" else b.ranksLabelFa,
                        color = if (current) Red else White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        style = Rtl, modifier = Modifier.width(120.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        b.vehicles.joinToString(" • "), color = if (current) White else Muted,
                        fontSize = 11.sp, modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── ابزارهای دستیار: وسایل مجاز / ارتقاهای مجاز ──
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showAvailableVehicles = true }, shape = CutCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                Text(gtr("وسایل رنک %1s", rank), color = Red, fontSize = 11.sp)
            }
            OutlinedButton(onClick = { showAvailableUpgrades = true }, shape = CutCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                Text(gtr("ارتقاهای رنک %1s", rank), color = Red, fontSize = 11.sp)
            }
        }

        // ── فهرست وسایل فعلی ──
        if (vehicles.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Family Vehicleهای ثبت‌شده", color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                style = Rtl, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            familyOwned.forEach { v ->
                val spec = com.cyberpunk.gmtool.data.VehicleCatalog.byName(v.specName) ?: return@forEach
                val ups = parsedUpgrades.filter { it.first == v.specName }.map { it.second }
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(CardBg, CutCornerShape(4.dp))
                        .border(1.dp, if (v.familyCheckedOut) Red else Color(0xFF333333), CutCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            spec.name + if (v.familyCheckedOut) "  ● بیرون" else "",
                            color = if (v.familyCheckedOut) Red else White, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, modifier = Modifier.weight(1f).clickable { itemDetailLabel = spec.name }
                        )
                        Text(gtr("SDP %1s/%2s", v.currentSdp, spec.sdp), color = Muted, fontSize = 10.sp)
                    }
                    Text(
                        gtr("%1s • سرنشین %2s • MOVE %3s", spec.domain.name, spec.seats, spec.combatMove),
                        color = Muted, fontSize = 10.sp
                    )
                    if (ups.isNotEmpty()) Text("Upgrades: " + ups.joinToString(" • "), color = White, fontSize = 10.sp)
                    if (v.destroyedUntil > 0L) Text(gtr("در تعمیر: %1s روز", v.destroyedUntil), color = Red, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { nomadMsg = viewModel.requestFamilyVehicle(character.id, v.id) },
                            enabled = v.destroyedUntil <= 0L && !v.familyCheckedOut,
                            shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                        ) { Text(if (rank >= 10) "بیرون آوردن" else "تحویل/سوآپ", color = Red, fontSize = 10.sp) }
                        OutlinedButton(
                            onClick = { nomadMsg = viewModel.sendDestroyedFamilyVehicleForRepair(character.id, v.id) },
                            enabled = v.currentSdp <= 0 && v.destroyedUntil <= 0L,
                            shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                        ) { Text("تعمیر Family (500eb)", color = Color(0xFFFFC107), fontSize = 10.sp) }
                    }
                }
            }
        }
        if (nomadMsg != null) {
            Spacer(Modifier.height(8.dp))
            FaText(nomadMsg!!, color = Color(0xFF4CAF50), fontSize = 12.sp, lineHeight = 19.sp, justify = true, modifier = Modifier.fillMaxWidth())
        }

        // ── فهرست کامل ارتقاهای کتاب (بازشو) ──
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clickable { showUpgradeRef = !showUpgradeRef }
                .background(CardBg, CutCornerShape(4.dp))
                .border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showUpgradeRef) "▾" else "▸", color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                "همه‌ی ارتقاهای وسایل نقلیه (کتاب Core)",
                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = Rtl,
                modifier = Modifier.weight(1f)
            )
        }
        if (showUpgradeRef) {
            FaText(lore.nomadUpgradePricingNote, color = Color(0xFFFFC107), fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            lore.nomadUpgradeRefs.groupBy { it.groupFa }.forEach { (group, items) ->
                Text(
                    group, color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp, style = Rtl,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
                items.forEach { up ->
                    val unlocked = rank >= up.rankRequired
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(Color(0xFF0B0B0B), CutCornerShape(4.dp))
                            .border(1.dp, if (unlocked) Red.copy(alpha = 0.45f) else Color(0xFF2A2A2A), CutCornerShape(4.dp))
                            .padding(10.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                up.name, color = if (unlocked) White else Muted, fontWeight = FontWeight.Bold,
                                fontSize = 12.sp, modifier = Modifier.weight(1f)
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    gtr("Moto %1s+", up.rankRequired),
                                    color = if (unlocked) Black else Muted, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                    modifier = Modifier.background(if (unlocked) Red else Color(0xFF2A2A2A), CutCornerShape(3.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        FaText(up.textFa, color = if (unlocked) Muted else Color(0xFF666666), fontSize = 11.sp, lineHeight = 18.sp, justify = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // ═══════════════ ویرایش ناوگان (افزودن/حذف وسیله و ارتقا) ═══════════════
    if (showMotorpoolEditor) {
        Dialog(onDismissRequest = { showMotorpoolEditor = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).border(2.dp, Red, CutCornerShape(12.dp))) {
                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        Text("ناوگان خانواده (Family Motorpool)", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(
                            gtr("رنک Moto: %1s • استفاده‌شده: %2s از %3s • باقی‌مانده: %4s", rank, usedPicks, rank, remainingPicks),
                            color = if (remainingPicks > 0) Color(0xFF4CAF50) else Red, fontSize = 12.sp, style = Rtl
                        )
                        Text(
                            "هر پیک یا یک وسیله‌ی تازه است یا یک ارتقا. با حذف یک ارتقا، پیکش آزاد می‌شود.",
                            color = Muted, fontSize = 10.sp, style = Rtl
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("افزودن وسیله‌ی FamilY (رنک $rank یا پایین‌تر)", color = Red, fontWeight = FontWeight.Bold, style = Rtl)
                    }
                    itemsIndexed(com.cyberpunk.gmtool.data.VehicleCatalog.availableForMoto(rank), key = { i, spec -> "add_${i}_${spec.name}" }) { _, spec ->
                        val already = vehicles.contains(spec.name)
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .background(CardBg, CutCornerShape(4.dp)).padding(8.dp)
                        ) {
                            Text(
                                gtr("%1s • Moto %2s+", spec.name, spec.familyRank),
                                color = if (already) Muted else White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().clickable { itemDetailLabel = spec.name }
                            )
                            Text(
                                gtr("SDP %1s • سرنشین %2s • MOVE %3s (برای جزئیات روی نام بزن)", spec.sdp, spec.seats, spec.combatMove),
                                color = Muted, fontSize = 10.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    if (!already && remainingPicks > 0) {
                                        viewModel.setNomadMotorpool(character.id, character.nomadMotorpool + spec.name, character.nomadVehicleUpgrades)
                                    }
                                },
                                enabled = !already && remainingPicks > 0,
                                shape = CutCornerShape(4.dp), modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    when {
                                        already -> "در ناوگان ثبت شده"
                                        remainingPicks <= 0 -> "پیک آزادی نداری"
                                        else -> "افزودن به ناوگان (۱ پیک)"
                                    },
                                    color = when {
                                        already -> Color(0xFF4CAF50)
                                        remainingPicks <= 0 -> Muted
                                        else -> Red
                                    }, fontSize = 10.sp
                                )
                            }
                        }
                    }
                    if (vehicles.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text("وسایل ناوگان و ارتقاهایشان", color = Red, fontWeight = FontWeight.Bold, style = Rtl)
                        }
                        itemsIndexed(vehicles, key = { i, name -> "vehicle_${i}_$name" }) { _, name ->
                            val spec = com.cyberpunk.gmtool.data.VehicleCatalog.byName(name) ?: return@itemsIndexed
                            val installed = parsedUpgrades.filter { it.first == name }.map { it.second }
                            Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(gtr(name), color = White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        TextButton(onClick = {
                                            val keptUpgrades = character.nomadVehicleUpgrades.filterNot { it.startsWith("$name||") }
                                            viewModel.setNomadMotorpool(character.id, character.nomadMotorpool.filterNot { it == name }, keptUpgrades)
                                        }) { Text(gtr("حذف وسیله"), color = Red, fontSize = 10.sp) }
                                    }
                                    if (installed.isNotEmpty()) {
                                        // هر ارتقا یک خط با دکمه‌ی «برداشتن» تا پیک آزاد شود
                                        installed.forEach { upName ->
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text("• " + upName, color = White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                                TextButton(onClick = {
                                                    val keep = character.nomadVehicleUpgrades.filterNot { it == "$name||$upName" }
                                                    viewModel.setNomadMotorpool(character.id, character.nomadMotorpool, keep)
                                                }) { Text("برداشتن", color = Color(0xFFFFC107), fontSize = 9.sp) }
                                            }
                                        }
                                    }
                                    val options = com.cyberpunk.gmtool.data.VehicleCatalog.upgradesFor(spec, rank, installed)
                                    if (options.isEmpty()) {
                                        Text("در رنک فعلی یا با پیش‌نیازهای موجود، ارتقای دیگری برای این وسیله باز نیست.", color = Muted, fontSize = 10.sp, style = Rtl)
                                    }
                                    options.forEach { up ->
                                        TextButton(
                                            onClick = {
                                                if (remainingPicks > 0) {
                                                    viewModel.setNomadMotorpool(
                                                        character.id, character.nomadMotorpool,
                                                        character.nomadVehicleUpgrades + "$name||${up.name}"
                                                    )
                                                }
                                            },
                                            enabled = remainingPicks > 0,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(Modifier.fillMaxWidth()) {
                                                Text(
                                                    gtr("+ %1s • Moto %2s+", up.name, up.rankRequired),
                                                    color = if (remainingPicks > 0) Red else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                                )
                                                FaText(up.description, color = Muted, fontSize = 9.sp, lineHeight = 15.sp, justify = true, modifier = Modifier.fillMaxWidth())
                                                up.prerequisite?.let { Text("پیش‌نیاز: $it", color = Color(0xFFFFC107), fontSize = 9.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (legacyVehicles.isNotEmpty() || legacyUpgrades.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text(gtr("LEGACY ENTRIES"), color = Red, fontWeight = FontWeight.Bold)
                            (legacyVehicles + legacyUpgrades).forEach { Text("• $it", color = Muted, fontSize = 11.sp) }
                            TextButton(onClick = {
                                viewModel.setNomadMotorpool(character.id, vehicles, parsedUpgrades.map { "${it.first}||${it.second}" })
                            }) { Text(gtr("REMOVE LEGACY ENTRIES"), color = Red) }
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "اگر Family Vehicle نابود شود، Family در یک هفته آن را کامل تعمیر می‌کند و معمولاً ۵۰۰eb هزینه دارد (GM می‌تواند در صورت بی‌پولی ببخشد؛ ولی آبرو آسیب می‌بیند).",
                            color = Muted, fontSize = 10.sp, lineHeight = 17.sp, style = Rtl, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showMotorpoolEditor = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Red),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("بستن", color = Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    // ═══════════════ وسایل مجاز در این رنک ═══════════════
    if (showAvailableVehicles) {
        Dialog(onDismissRequest = { showAvailableVehicles = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Black), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f).border(2.dp, Red, CutCornerShape(10.dp))) {
                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        Text("وسایل قابل دسترس", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            gtr("وسایلی که Moto Rank %1s اجازه می‌دهد (رنک موتو یا پایین‌تر).", rank),
                            color = Muted, fontSize = 11.sp, style = Rtl
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    itemsIndexed(com.cyberpunk.gmtool.data.VehicleCatalog.availableForMoto(rank), key = { i, spec -> "available_${i}_${spec.name}" }) { _, spec ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { itemDetailLabel = spec.name }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(gtr("%1s • Moto %2s+", spec.name, spec.familyRank), color = White, fontWeight = FontWeight.Bold)
                                Text(
                                    gtr("%1s • SDP %2s • سرنشین %3s • MOVE %4s • %5s MPH", spec.domain.name, spec.sdp, spec.seats, spec.combatMove, spec.narrativeMph),
                                    color = Muted, fontSize = 10.sp
                                )
                                FaText(spec.description, color = Muted, fontSize = 10.sp, justify = true, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    item {
                        val locked = com.cyberpunk.gmtool.data.VehicleCatalog.vehicles.filter { it.familyRank > rank }
                        if (locked.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("در رنک‌های بالاتر", color = Red, fontWeight = FontWeight.Bold, style = Rtl)
                            locked.forEach { Text(gtr("🔒 %1s • Moto %2s+", it.name, it.familyRank), color = Muted, fontSize = 11.sp) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showAvailableVehicles = false }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) {
                            Text("بستن", color = Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ═══════════════ ارتقاهای مجاز در این رنک ═══════════════
    if (showAvailableUpgrades) {
        Dialog(onDismissRequest = { showAvailableUpgrades = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Black), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f).border(2.dp, Red, CutCornerShape(10.dp))) {
                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        Text("ارتقاهای قابل دسترس", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            gtr("ارتقاهای بازشده تا Moto Rank %1s. نصب از کارت «ویرایش ناوگان» انجام می‌شود.", rank),
                            color = Muted, fontSize = 11.sp, style = Rtl
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    itemsIndexed(com.cyberpunk.gmtool.data.VehicleCatalog.upgrades.filter { it.rankRequired <= rank }, key = { i, up -> "upgrade_${i}_${up.name}" }) { _, up ->
                        Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(gtr("%1s • Moto %2s+", up.name, up.rankRequired), color = White, fontWeight = FontWeight.Bold)
                                FaText(up.description, color = Muted, fontSize = 10.sp, lineHeight = 17.sp, justify = true, modifier = Modifier.fillMaxWidth())
                                up.prerequisite?.let { Text("پیش‌نیاز: $it", color = Color(0xFFFFC107), fontSize = 10.sp) }
                            }
                        }
                    }
                    item {
                        val locked = com.cyberpunk.gmtool.data.VehicleCatalog.upgrades.filter { it.rankRequired > rank }
                        if (locked.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("در رنک‌های بالاتر", color = Red, fontWeight = FontWeight.Bold, style = Rtl)
                            locked.forEach { Text(gtr("🔒 %1s • Moto %2s+", it.name, it.rankRequired), color = Muted, fontSize = 11.sp) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showAvailableUpgrades = false }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) {
                            Text("بستن", color = Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    itemDetailLabel?.let { label -> CoreItemDetailDialog(label = label, categoryHint = "Vehicles", onDismiss = { itemDetailLabel = null }) }
}

/**
 * مجموع مهارت با همان حساب STATS (شامل بونوس آشنایی نامد = رنک موتو).
 * فقط برای همین کارت است تا رولر با عدد تب STATS یکی باشد.
 */
private fun nomadSkillTotal(character: Character, skillName: String): Int {
    val skill = character.skills.firstOrNull { it.name.equals(skillName, true) }
    val level = skill?.level ?: 0
    val statName = com.cyberpunk.gmtool.data.SkillCatalog.statFor(skillName)
    val stat = when (statName) {
        "Reflexes" -> com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref)
        "Dexterity" -> com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex)
        "Technique" -> character.stats.tech
        "Intelligence" -> com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "INT", character.stats.int)
        "Cool" -> character.stats.cool
        "Willpower" -> character.stats.will
        "Movement" -> com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(character, "MOVE", character.stats.move)
        "Body" -> character.stats.body
        "Empathy" -> character.stats.emp
        else -> 0
    }
    val armorPenalty = com.cyberpunk.gmtool.data.GameRules.armorPenalty(character.inventory)
    val statPenalty = if (statName == "Reflexes" || statName == "Dexterity") armorPenalty else 0
    val woundPenalty = if (com.cyberpunk.gmtool.data.GameRules.isSeriouslyWounded(character.hp, character.maxHp) &&
        !com.cyberpunk.gmtool.data.StreetDrugRules.ignoresSeriouslyWounded(character)) -2 else 0
    val moto = if (character.role.equals("Nomad", true)) character.roleRank else 0
    return level + stat + statPenalty + woundPenalty + moto
}

// =======================================================
// کامپوننت‌های مشترک
// =======================================================
@Composable
fun RollButton(label: String, badge: String, diceSides: Int = 10, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Black),
        shape = CutCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(60.dp).border(1.dp, Red, CutCornerShape(8.dp))
    ) {
        // چیدمان دکمه: [تاس] [برچسب] [مقدار] — برچسب وزن می‌گیرد و مقدار هرگز
        // نمی‌شکند؛ وگرنه متن‌های لاتین مثل «1d10 ≤ 4» حرف‌به‌حرف می‌شکستند.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Box(modifier = Modifier.size(36.dp).border(1.dp, Red, CutCornerShape(4.dp)).background(Red.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text("d$diceSides", color = Red, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                gtr(label), color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp,
                maxLines = 2, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    gtr(badge), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.border(1.dp, Red.copy(alpha = 0.5f), CutCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun InfoItem(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(gtr(title), color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold, style = Rtl, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(gtr(description), color = Muted, fontSize = 13.sp, lineHeight = 22.sp, style = RtlJustify, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LoreBlock(title: String, body: String, compact: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = if (compact) 4.dp else 6.dp)
            .background(Color(0xFF0B0B0B), CutCornerShape(4.dp))
            .border(1.dp, Red.copy(alpha = 0.45f), CutCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Text(
            title, color = Red, fontWeight = FontWeight.Bold,
            fontSize = if (compact) 13.sp else 14.sp, style = Rtl,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        FaText(body, color = White, fontSize = 13.sp, lineHeight = 22.sp, justify = true, modifier = Modifier.fillMaxWidth())
    }
}

/** یک سطر جدول تأثیر کاریزماتیک: تیتر + نشان DV (لاتین، جدا) + متن. */
@Composable
private fun LoreRow(title: String, dv: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, style = Rtl, modifier = Modifier.weight(1f))
            if (dv.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        dv, color = Black, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.background(Red, CutCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        FaText(body, color = Muted, fontSize = 12.sp, lineHeight = 20.sp, justify = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun RoleSubItemWithDescription(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF333333), CutCornerShape(4.dp))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(gtr(title), color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(gtr(description), color = White, fontSize = 13.sp, style = Rtl)
        }
    }
}

@Composable
fun AbilityCounterRow(title: String, value: Int, onIncrement: () -> Unit, onDecrement: () -> Unit, onClickTitle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(gtr(title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            modifier = Modifier.clickable { onClickTitle() }.weight(1f).fillMaxWidth(), textAlign = TextAlign.Right)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > 0) onDecrement() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "-", tint = Red)
            }
            Text("$value", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "+", tint = Red)
            }
        }
    }
}

@Composable
fun MakerCounterRow(title: String, points: Int, pointsRemaining: Int, max: Int, onInc: () -> Unit, onDec: () -> Unit, onInfo: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).border(1.dp, Color(0xFF333333), CutCornerShape(4.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(gtr(title), color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f).clickable { onInfo() })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { if (points > 0) onDec() }, enabled = points > 0, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) { Text("−", color = Color.White, fontSize = 18.sp) }
            Text("$points", color = Red, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Button(onClick = { if (pointsRemaining > 0 && points < max) onInc() }, enabled = pointsRemaining > 0 && points < max,
                modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                Text("+", color = Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun AbilityInfoDialog(abilityName: String, cyberpunkRed: Color = Red, onDismiss: () -> Unit) {
    val description = when (abilityName) {
        "Fumble Recovery" -> "با صرف ۴ امتیاز، از شکست‌های بحرانی (تاس ۱) هنگام حمله چشم‌پوشی می‌کنی (هرچند آن تاس‌ها همچنان عدد ۱ محسوب می‌شوند)."
        "Damage Deflection" -> "۲ امتیاز: اولین آسیب این راند ۱ کمتر. ۴: ۲ کمتر. ۶: ۳ کمتر. ۸: ۴ کمتر. ۱۰: ۵ کمتر."
        "Initiative Reaction" -> "هر امتیاز، ۱+ به تاس‌های Initiative اضافه می‌کند."
        "Precision Attack" -> "۳ امتیاز: ۱+ به تمام حملات. ۶: ۲+. ۹: ۳+."
        "Spot Weakness" -> "هر امتیاز، ۱+ به آسیب اولین حمله موفق در هر راند (قبل از زره)."
        "Threat Detection" -> "هر امتیاز، ۱+ به تمام چک‌های Perception."
        else -> "توضیحاتی در دسترس نیست."
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)).background(Black).border(2.dp, cyberpunkRed, CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)).padding(20.dp)) {
            Column {
                Text(gtr(abilityName), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = cyberpunkRed.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))
                Text(gtr(description), color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp, style = RtlJustify,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onDismiss, shape = CutCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = cyberpunkRed), modifier = Modifier.fillMaxWidth()) {
                    Text(gtr("Close"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SimpleInfoDialog(title: String, description: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Red, CutCornerShape(12.dp))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(gtr(title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    Text(gtr(description), color = Muted, fontSize = 14.sp, lineHeight = 22.sp, style = RtlJustify)
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) {
                    Text("بستن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LawmanDiceDialog(diceType: Int, rollResult: Int, message: String, isSuccess: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, if (isSuccess) Red else Color.DarkGray, CutCornerShape(12.dp))) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("پرتاب تاس 1d$diceType", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Box(modifier = Modifier.size(80.dp).background(if (isSuccess) Red else Color.DarkGray, CutCornerShape(8.dp)).border(2.dp, Color.White, CutCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text(rollResult.toString(), color = Black, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
                Text(gtr(message), color = if (isSuccess) Color.White else Color.LightGray, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp, style = TextStyle(textAlign = TextAlign.Center, textDirection = TextDirection.Rtl))
                Spacer(Modifier.height(28.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("بستن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DiceRollDialog(title: String, roll: Int, total: Int, breakdowns: List<String>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(12.dp))) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gtr(title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = Red, thickness = 2.dp, modifier = Modifier.padding(vertical = 12.dp))
                Box(modifier = Modifier.size(80.dp).background(Red, CutCornerShape(8.dp)).border(2.dp, Color.White, CutCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text(roll.toString(), color = Black, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                breakdowns.forEach { line -> Text(gtr(line), color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(gtr("TOTAL SCORE: "), color = Color.White, fontSize = 16.sp)
                    Text(total.toString(), color = Red, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(gtr("CLOSE"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NomadMenuButton(title: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Black), shape = CutCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, Red.copy(alpha = 0.5f), CutCornerShape(8.dp))) {
        Text(gtr(title), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun ItemButton(title: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.border(1.dp, Color(0xFF4CAF50), CutCornerShape(4.dp)).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(gtr(title), color = White, fontSize = 11.sp)
    }
}

@Composable
fun CyberpunkOutlinedBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(top = 6.dp)) {
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, Red, CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(gtr(label), color = Color.White, fontSize = 11.sp,
            modifier = Modifier.offset(x = 12.dp, y = (-7).dp).background(Black).padding(horizontal = 4.dp))
    }
}

// جعبه‌ی متنِ قابل‌ویرایش (برای Name و Handle)
@Composable
fun CyberpunkEditBox(label: String, initial: String, onCommit: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    Box(modifier = Modifier.padding(top = 6.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; onCommit(it) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Red,
                unfocusedBorderColor = Red.copy(alpha = 0.6f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Red,
                focusedLabelColor = Red,
                unfocusedLabelColor = Color.White
            ),
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(gtr(label), color = Color.White, fontSize = 11.sp,
            modifier = Modifier.offset(x = 12.dp, y = (-7).dp).background(Black).padding(horizontal = 4.dp))
    }
}

@Composable
fun LifestyleRow(title: String, cost: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(if (selected) Red.copy(alpha = 0.15f) else Color(0xFF1A1A1A), CutCornerShape(6.dp))
            .border(1.dp, if (selected) Red else Color(0xFF333333), CutCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected, onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Red, unselectedColor = Color.Gray)
        )
        Text(gtr(title), color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(cost, color = if (selected) Red else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LifepathRow(label: String, value: String, onRollClick: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(gtr(label), color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                Text(value.ifBlank { "—" }, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            }
            IconButton(onClick = onRollClick) {
                Icon(Icons.Filled.Casino, contentDescription = gtr("Roll Dice"), tint = Red)
            }
        }
    }
}
