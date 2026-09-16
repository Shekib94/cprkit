package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CombatRules
import com.cyberpunk.gmtool.data.DiceSource
import com.cyberpunk.gmtool.data.EquipmentUseRules
import com.cyberpunk.gmtool.data.GameRules
import com.cyberpunk.gmtool.ui.components.FaText
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.viewmodel.clearCombatEncounter
import com.cyberpunk.gmtool.viewmodel.nextCombatTurn
import com.cyberpunk.gmtool.viewmodel.previousCombatTurn
import com.cyberpunk.gmtool.viewmodel.resolveSuppressiveCover
import com.cyberpunk.gmtool.viewmodel.restartCombatParticipantsPreservingLink
import com.cyberpunk.gmtool.viewmodel.rollCombatInitiative
import com.cyberpunk.gmtool.viewmodel.setCombatParticipants
import kotlinx.coroutines.launch

/**
 * بخش‌های «۱) شرکت‌کننده‌های نبرد» و «۲) نوبت و Initiative» داخل تب COMBAT.
 *
 * چرا این‌جا و نه یک تب جدا: قبلاً Turn یک تب مستقل در نوار پایین بود و برای
 * یک نبرد باید بین دو تب می‌پریدی. حالا همان کنترل‌ها بخش ۱ و ۲ تب COMBAT
 * هستند و تب TURN از نوار پایین برداشته شده است. بالای این دو بخش، کارت‌های
 * HP/SP بدون شماره می‌آیند؛ شماره‌گذاری از «شرکت‌کننده‌های نبرد» شروع می‌شود.
 * ترتیب بخش‌های تب COMBAT:
 * ۱) شرکت‌کننده‌های نبرد، ۲) نوبت و Initiative، ۳) اکشن (حمله + دفاع)،
 * ۴) آسیب منطقه‌ای، ۵) کاور/سپر، ۶) نبرد خودرویی.
 *
 * این کامپوزبل عمداً هیچ اسکرول خودش را ندارد تا داخل اسکرول تب COMBAT بنشیند.
 */
private val TRed = Color(0xFFD32F2F)
private val TBlack = Color(0xFF0F0F0F)
private val TCard = Color(0xFF1A1A1A)
private val TWhite = Color(0xFFE0E0E0)
private val TMuted = Color(0xFFAAAAAA)

/** تیتر بخش‌های درون‌متنی؛ هم‌شکل بقیه‌ی تیترهای تب COMBAT. */
@Composable
private fun SectionTitle(title: String, number: Int? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                if (number != null) "$number • $title" else title,
                color = TRed, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CombatTurnSection(
    character: Character,
    viewModel: CharacterViewModel,
    attackActionUsed: Int,
    attackActionCap: Int?,
    gmOverrideAction: Boolean,
    onActionSpent: (Int) -> Unit,
    onResetTurnTracking: () -> Unit,
    onResult: (String) -> Unit,
    onHelp: (Pair<String, String>) -> Unit
) {
    val allCharacters by viewModel.characters.collectAsState()
    val scope = rememberCoroutineScope()
    val encounterCharacters = allCharacters.filter { it.id in viewModel.combatParticipantIds }
    val aliveParticipants = encounterCharacters.count { !it.isDead }
    val woundedParticipants = encounterCharacters.count { !it.isDead && it.hp < it.maxHp }
    val pcParticipants = encounterCharacters.count { it.isAlly }
    val npcParticipants = encounterCharacters.size - pcParticipants


    var lastInitiativeRoll by remember { mutableStateOf<String?>(null) }
    var turnControllerResult by remember { mutableStateOf<String?>(null) }
    var showRosterDialog by remember { mutableStateOf(false) }
    var encounterDraftIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var sandeActiveUntil by remember(character.id) { mutableLongStateOf(0L) }
    var sandeCooldownUntil by remember(character.id) { mutableLongStateOf(0L) }
    var rosterExpanded by remember(character.id) { mutableStateOf(viewModel.combatParticipantIds.isEmpty()) }

    fun rules(block: () -> Unit) {
        if (DiceSource.isManual) scope.launch(kotlinx.coroutines.Dispatchers.Default) { block() } else block()
    }

    fun rollInitiative() {
        val r = CombatRules.rollInitiative(character, sandeActiveUntil)
        lastInitiativeRoll = gtr(
            "%1s  [%2s + REF %3s + Speedware %4s + Solo %5s]",
            r.total, r.die, r.bonus.ref, r.bonus.speedware, r.bonus.solo
        )
    }

    SectionTitle("PARTICIPANTS / شرکت‌کننده‌های نبرد", 1)

    // ── شرکت‌کننده‌های نبرد ──
    Card(colors = CardDefaults.cardColors(containerColor = TCard), shape = CutCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { rosterExpanded = !rosterExpanded }
            .border(1.dp, if (rosterExpanded) TRed.copy(alpha = .8f) else TMuted.copy(alpha = .25f), CutCornerShape(10.dp))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rosterExpanded) "▾" else "▸", color = TRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                FaText("شرکت‌کننده‌های نبرد", color = if (rosterExpanded) TRed else TWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                FaText(
                    when {
                        viewModel.combatParticipantIds.isEmpty() -> "هنوز کسی انتخاب نشده"
                        viewModel.combatInitiative.isEmpty() -> gtr("%1s نفر آماده • ترتیب هنوز ریخته نشده", viewModel.combatParticipantIds.size)
                        else -> gtr("%1s نفر • %2s بازیکن / %3s NPC", encounterCharacters.size, pcParticipants, npcParticipants)
                    },
                    color = TMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth()
                )
            }
            IconButton(onClick = {
                onHelp(
                    "شرکت‌کننده‌های نبرد" to
                        "افرادی که در این درگیری حاضرند را انتخاب کن. بعد از انتخاب، ترتیب نوبت (Initiative) ریخته می‌شود و تا وقتی پایان نبرد را نزنی همین می‌ماند. کاراکتر مرده وارد ترتیب نمی‌شود."
                )
            }, modifier = Modifier.size(30.dp)) { Text("i", color = TRed, fontWeight = FontWeight.Bold) }
        }
    }
    if (rosterExpanded) {
        if (viewModel.combatParticipantIds.isNotEmpty()) {
            FaText(
                gtr("%1s نفر • %2s سالم • %3s زخمی", encounterCharacters.size, aliveParticipants, encounterCharacters.size - woundedParticipants),
                color = TMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            if (viewModel.combatLinkedEncounterId != null) {
                FaText(gtr("متصل به کمپین %1s • انکوانتر %2s", viewModel.combatLinkedCampaignId, viewModel.combatLinkedEncounterId), color = TMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { encounterDraftIds = viewModel.combatParticipantIds; showRosterDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TRed),
                shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f)
            ) { Text(if (viewModel.combatParticipantIds.isEmpty()) "انتخاب افراد و شروع" else "ویرایش / شروع دوباره", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            OutlinedButton(
                onClick = {
                    if (viewModel.combatLinkedEncounterId != null) {
                        scope.launch {
                            viewModel.finishLinkedEncounter("Combat ended by GM").onSuccess {
                                turnControllerResult = "انکوانتر بسته شد و در Session ثبت شد."
                            }.onFailure { turnControllerResult = it.message ?: "بستن انکوانتر ناموفق بود." }
                        }
                    } else {
                        viewModel.clearCombatEncounter()
                        turnControllerResult = "نبرد بسته شد؛ ترتیب و فهرست پاک شدند."
                    }
                },
                enabled = viewModel.combatParticipantIds.isNotEmpty(),
                shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f)
            ) { Text("پایان نبرد", color = if (viewModel.combatParticipantIds.isNotEmpty()) TRed else TMuted, fontSize = 11.sp) }
        }
    }

    SectionTitle("INITIATIVE / نوبت و ترتیب", 2)
    // ── وضعیت نوبت: چه کسی، کدام Round، Action مصرف شد یا نه ──
    val activeEntry = viewModel.combatInitiative.getOrNull(viewModel.combatTurnIndex)
    val activeCharacter = allCharacters.firstOrNull { it.id == activeEntry?.characterId }
    val activeName = activeCharacter?.let { it.handle.ifBlank { it.name } } ?: "—"
    Card(colors = CardDefaults.cardColors(containerColor = TCard), shape = CutCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.5.dp, TRed.copy(alpha = .6f), CutCornerShape(10.dp))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FaText(gtr("ROUND %1s", viewModel.combatRound), color = TRed, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            FaText(
                "نوبتِ الان: $activeName   •   Action این شیت: ${if (attackActionUsed > 0) "مصرف‌شده" else "آزاد"}",
                color = TWhite, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()
            )
            if (activeCharacter != null) {
                val mortally = GameRules.isMortallyWounded(activeCharacter.hp)
                val fire = when {
                    activeCharacter.combatEffects.containsKey("on_fire_4") -> " • در آتش (۴)"
                    activeCharacter.combatEffects.containsKey("on_fire_2") -> " • در آتش (۲)"
                    else -> ""
                }
                val locked = buildList {
                    if (activeCharacter.combatEffects.containsKey("no_action_this_turn")) add("بدون Action")
                    if (activeCharacter.combatEffects.containsKey("no_move_this_turn")) add("بدون Move")
                    if (activeCharacter.combatEffects.containsKey("prone")) add("زمین‌افتاده")
                }.joinToString(" • ")
                FaText(
                    "HP ${activeCharacter.hp}/${activeCharacter.maxHp}${if (mortally) " • در آستانه‌ی مرگ" else ""}$fire${if (locked.isNotBlank()) " • $locked" else ""}",
                    color = TMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth()
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val ending = activeName
                        val oldRound = viewModel.combatRound
                        viewModel.nextCombatTurn()
                        val nextEntry = viewModel.combatInitiative.getOrNull(viewModel.combatTurnIndex)
                        val next = allCharacters.firstOrNull { it.id == nextEntry?.characterId }?.let { it.handle.ifBlank { it.name } } ?: "—"
                        turnControllerResult = "نوبتِ $ending تمام شد. نوبت بعدی: $next" +
                            if (viewModel.combatRound != oldRound) " • Round ${viewModel.combatRound} شروع شد" else ""
                        onResetTurnTracking()
                    },
                    enabled = viewModel.combatInitiative.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = TRed),
                    shape = CutCornerShape(8.dp), modifier = Modifier.weight(1.6f)
                ) { Text(gtr("END TURN → NEXT"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                OutlinedButton(
                    onClick = { showQueueDialog = true },
                    enabled = viewModel.combatInitiative.isNotEmpty(),
                    shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f)
                ) { Text(gtr("QUEUE"), color = if (viewModel.combatInitiative.isNotEmpty()) TRed else TMuted, fontSize = 11.sp) }
            }
            turnControllerResult?.let { FaText(it, color = TWhite, fontSize = 10.sp, modifier = Modifier.fillMaxWidth()) }
        }
    }

    // ── ریختن ترتیب نوبت ──
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { rules { viewModel.rollCombatInitiative() }; showQueueDialog = true },
            enabled = viewModel.combatParticipantIds.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = TRed),
            shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Warning, null, tint = Color.Black)
            Spacer(Modifier.width(6.dp))
            Text("ریختن ترتیب همه (ROLL ALL)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        OutlinedButton(
            onClick = { rules { rollInitiative() } },
            shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f)
        ) { Text("تاس من (ROLL SELF)", color = TRed, fontSize = 11.sp) }
    }
    lastInitiativeRoll?.let { FaText(it, color = TWhite, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) }

    // ── Sandevistan ──
    val hasSande = character.inventory.any { it.equipped && it.name.contains("Sandevistan", true) }
    if (hasSande) {
        val now = System.currentTimeMillis()
        OutlinedButton(
            onClick = {
                val t = System.currentTimeMillis()
                if (t >= sandeCooldownUntil && attackActionUsed == 0) {
                    sandeActiveUntil = t + 60_000L
                    sandeCooldownUntil = t + 3_600_000L
                    onActionSpent(1)
                }
            },
            enabled = now >= sandeCooldownUntil && (attackActionUsed == 0 || gmOverrideAction),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        ) { Text(if (now < sandeActiveUntil) "SANDEVISTAN ACTIVE (+3 INIT)" else "ACTIVATE SANDEVISTAN", color = TRed, fontSize = 11.sp) }
    }

    // ── Suppressive Fire: اجبار پناه گرفتن در نوبت بعد ──
    val mustSeekCover = character.combatEffects.containsKey("must_seek_cover_this_turn")
    if (mustSeekCover) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0B0B)), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FaText("Suppressive Fire — مجبوری به سمت Cover بروی", color = TRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                FaText(
                    "در نوبت بعدی، Move Action باید صرف رسیدن به Cover شود. اگر یک Move کافی نبود، Run هم لازم است تا در Cover (یا نزدیک‌ترین نقطه) بایستی.",
                    color = TWhite, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true
                )
                OutlinedButton(
                    onClick = { viewModel.resolveSuppressiveCover(character.id); onResult("اجبار Suppressive Fire ثبت شد: به Cover رسید یا نزدیک‌ترین نقطه.") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("حل شد — به Cover رسیدم", color = TRed, fontSize = 11.sp) }
            }
        }
    }

    if (showRosterDialog) {
        Dialog(onDismissRequest = { showRosterDialog = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = TBlack), modifier = Modifier.fillMaxWidth().border(2.dp, TRed, CutCornerShape(12.dp))) {
                Column(Modifier.padding(18.dp).fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState())) {
                    Text(gtr("ENCOUNTER SETUP"), color = TRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            "افراد حاضر در این نبرد را انتخاب کن. بعد از تأیید، Round ۱ و ترتیب نوبت ساخته می‌شود.",
                            color = TMuted, fontSize = 12.sp, lineHeight = 19.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp), textAlign = TextAlign.Right
                        )
                    }
                    val aliveAllies = allCharacters.filter { it.isAlly && !it.isDead }.map { it.id }.toSet()
                    val aliveNpcs = allCharacters.filter { !it.isAlly && !it.isDead }.map { it.id }.toSet()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(onClick = { encounterDraftIds = aliveAllies }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 3.dp)) { Text("بازیکن‌ها", color = TRed, fontSize = 10.sp) }
                        OutlinedButton(onClick = { encounterDraftIds = aliveNpcs }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 3.dp)) { Text("NPCها", color = TRed, fontSize = 10.sp) }
                        OutlinedButton(onClick = { encounterDraftIds = aliveAllies + aliveNpcs }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 3.dp)) { Text("همه", color = TRed, fontSize = 10.sp) }
                        OutlinedButton(onClick = { encounterDraftIds = emptySet() }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 3.dp)) { Text("پاک", color = TMuted, fontSize = 10.sp) }
                    }
                    RosterGroup("بازیکن‌ها / Allies", allCharacters.filter { it.isAlly }, encounterDraftIds) { ids -> encounterDraftIds = ids }
                    RosterGroup("NPCها / Enemies", allCharacters.filter { !it.isAlly }, encounterDraftIds) { ids -> encounterDraftIds = ids }
                    Spacer(Modifier.height(12.dp))
                    Text("${encounterDraftIds.size} نفر انتخاب شده", color = if (encounterDraftIds.isEmpty()) TMuted else TWhite, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showRosterDialog = false }, modifier = Modifier.weight(1f)) { Text("انصراف", color = TRed) }
                        Button(
                            onClick = {
                                if (viewModel.combatLinkedEncounterId != null) viewModel.restartCombatParticipantsPreservingLink(encounterDraftIds, true)
                                else viewModel.setCombatParticipants(encounterDraftIds, true)
                                showRosterDialog = false
                                showQueueDialog = true
                                rosterExpanded = false
                                turnControllerResult = "نبرد شروع شد؛ Round ۱ و ترتیب نوبت ساخته شد."
                            },
                            enabled = encounterDraftIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = TRed),
                            modifier = Modifier.weight(1.35f)
                        ) { Text("شروع نبرد", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                    }
                }
            }
        }
    }

    if (showQueueDialog) {
        Dialog(onDismissRequest = { showQueueDialog = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = TBlack), modifier = Modifier.fillMaxWidth().border(2.dp, TRed, CutCornerShape(12.dp))) {
                Column(Modifier.padding(18.dp).fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(gtr("INITIATIVE QUEUE — ROUND %1s", viewModel.combatRound), color = TRed, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showQueueDialog = false }) { Icon(Icons.Default.Close, contentDescription = "بستن", tint = TMuted) }
                    }
                    if (viewModel.combatInitiative.isEmpty()) {
                        Text("اول افراد نبرد را انتخاب کن و «ریختن ترتیب همه» را بزن.", color = TMuted, modifier = Modifier.padding(vertical = 14.dp))
                    } else {
                        viewModel.combatInitiative.forEachIndexed { index, entry ->
                            val c = allCharacters.firstOrNull { it.id == entry.characterId }
                            if (c != null) {
                                val active = index == viewModel.combatTurnIndex
                                Card(colors = CardDefaults.cardColors(containerColor = if (active) TRed.copy(alpha = 0.18f) else TCard), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}", color = if (active) TRed else TMuted, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(c.handle.ifBlank { c.name }, color = TWhite, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                            Text(entry.rollText + if (entry.tieBreak > 0) gtr(" • تساوی %1s", entry.tieBreak) else "", color = TMuted, fontSize = 10.sp)
                                        }
                                        Text("${entry.total}", color = TRed, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.previousCombatTurn() }, enabled = viewModel.combatInitiative.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("قبلی", color = TRed) }
                        Button(onClick = { viewModel.nextCombatTurn(); onResetTurnTracking() }, enabled = viewModel.combatInitiative.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = TRed), modifier = Modifier.weight(1f)) { Text("بعدی", color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                    OutlinedButton(onClick = { rules { viewModel.rollCombatInitiative() } }, enabled = viewModel.combatParticipantIds.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("ریختن دوباره‌ی ترتیب", color = TRed)
                    }
                    Button(
                        onClick = { showQueueDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = TRed),
                        shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 4.dp)
                    ) { Text("بستن", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp) }
                }
            }
        }
    }

}

@Composable
private fun RosterGroup(
    title: String,
    list: List<Character>,
    draft: Set<Int>,
    onChange: (Set<Int>) -> Unit
) {
    Text(gtr(title), color = TWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
    list.forEach { c ->
        val canJoin = !c.isDead
        Row(
            Modifier.fillMaxWidth().clickable(enabled = canJoin) { onChange(if (c.id in draft) draft - c.id else draft + c.id) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = c.id in draft, enabled = canJoin, onCheckedChange = { onChange(if (c.id in draft) draft - c.id else draft + c.id) })
            Column(Modifier.weight(1f)) {
                Text(c.handle.ifBlank { c.name }, color = if (canJoin) TWhite else TMuted)
                if (!canJoin) Text("مرده — وارد ترتیب نمی‌شود", color = TMuted, fontSize = 9.sp)
            }
            Text(gtr("HP %1s/%2s", c.hp, c.maxHp), color = TMuted, fontSize = 11.sp)
        }
    }
}
