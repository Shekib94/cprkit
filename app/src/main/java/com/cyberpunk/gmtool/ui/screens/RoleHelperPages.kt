package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.BackupRules
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.ExecTeamClass
import com.cyberpunk.gmtool.data.ExecTeamData
import com.cyberpunk.gmtool.data.NpcData
import com.cyberpunk.gmtool.data.RoleHelperData
import com.cyberpunk.gmtool.data.RoleLoreData
import com.cyberpunk.gmtool.data.execScenarios
import com.cyberpunk.gmtool.data.fixerScenarios
import com.cyberpunk.gmtool.data.lawmanScenarios
import com.cyberpunk.gmtool.ui.components.DecisionBox
import com.cyberpunk.gmtool.ui.components.FaText
import com.cyberpunk.gmtool.ui.components.HelperCard
import com.cyberpunk.gmtool.ui.components.HelperToggle
import com.cyberpunk.gmtool.ui.components.PickerRows
import com.cyberpunk.gmtool.ui.components.RoleGloryCard
import com.cyberpunk.gmtool.ui.components.RoleHelperCard
import com.cyberpunk.gmtool.ui.components.RoleHelperCharacterCard
import com.cyberpunk.gmtool.ui.components.RoleHelperHeader
import com.cyberpunk.gmtool.ui.components.RoleHelperPage
import com.cyberpunk.gmtool.ui.components.RoleInnerBox
import com.cyberpunk.gmtool.ui.components.RoleHelperChipRow
import com.cyberpunk.gmtool.ui.components.RoleScenarioCard
import com.cyberpunk.gmtool.ui.components.RoleUi
import com.cyberpunk.gmtool.ui.components.RollBox
import com.cyberpunk.gmtool.ui.components.RuleInfoButton
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.viewmodel.adjustTeamLoyalty
import com.cyberpunk.gmtool.viewmodel.addCombatEffect
import com.cyberpunk.gmtool.viewmodel.execTeamMembers
import com.cyberpunk.gmtool.viewmodel.hireExecTeamMember
import com.cyberpunk.gmtool.viewmodel.teamMemberLoyalty
import com.cyberpunk.gmtool.viewmodel.spawnNpcTemplates

/**
 * صفحات Role Helper برای Exec / Lawman / Fixer.
 *
 * قبلاً این سه نقش در GmRoleExtras به‌شکل AlertDialog با متن فشرده باز می‌شدند
 * و بقیه‌ی نقش‌ها صفحه‌ی کامل با کارت‌های یکسان داشتند. حالا هر سه هم از همان
 * قالب (RoleHelperScaffold) استفاده می‌کنند: تیتر، کارت انتخاب کاراکتر، کارت‌های
 * اطلاعاتی، کارت صحنه‌ی آماده و همان ابزارهای تعاملی. هیچ قاعده‌ای عوض نشده.
 */

// ═══════════════════════════════ EXEC ═══════════════════════════════

@Composable
fun ExecGmAssistant(viewModel: CharacterViewModel) {
    val characters by viewModel.characters.collectAsState()
    val execs = characters.filter { it.role.equals("Exec", true) && it.isAlly }
    var selectedId by remember(execs.map { it.id }) { mutableStateOf(execs.firstOrNull()?.id) }
    val exec = execs.firstOrNull { it.id == selectedId } ?: execs.firstOrNull()
    var resource by remember { mutableStateOf(RoleHelperData.execResources.first()) }
    var hasAuthority by remember { mutableStateOf(true) }
    var timeShort by remember { mutableStateOf(false) }
    var scenarioIndex by remember { mutableIntStateOf(0) }
    var spawnResult by remember { mutableStateOf<String?>(null) }
    var execClassIndex by remember { mutableIntStateOf(0) }
    var execHireMsg by remember { mutableStateOf<String?>(null) }
    var execAddToEncounter by remember { mutableStateOf(true) }
    var loyaltyPickMemberId by remember { mutableStateOf<Int?>(null) }
    var loyaltySaveMsg by remember { mutableStateOf<String?>(null) }
    val rank = exec?.roleRank ?: 4
    val teamClasses = ExecTeamData.teamMemberClasses
    val team = viewModel.execTeamMembers()
    val hireMember = rememberAutoDiceRoller(6) { d6 ->
        execHireMsg = viewModel.hireExecTeamMember(teamClasses[execClassIndex].name, d6 + 1, execAddToEncounter)
    }
    val loyaltySave = rememberAutoDiceRoller(6) { d6 ->
        val member = team.firstOrNull { it.id == loyaltyPickMemberId }
        if (member == null) {
            loyaltySaveMsg = "اول یک Team Member انتخاب کن."
        } else {
            val loy = teamMemberLoyalty(member)
            loyaltySaveMsg = if (d6 <= loy)
                gtr("LOYALTY SAVE — %1s: تاس %2s ≤ Loyalty %3s → دستور را انجام می‌دهد.", member.name, d6, loy)
            else
                gtr("LOYALTY SAVE — %1s: تاس %2s > Loyalty %3s → دستور را رد می‌کند یا خرابش می‌کند؛ GM تصمیم می‌گیرد.", member.name, d6, loy)
        }
    }

    RoleHelperPage {
        item {
            RoleHelperHeader(
                "دستیار Exec برای GM",
                "Teamwork و منابع سازمانی همان Exec از Character Sheet خوانده می‌شود. Team Memberها NPC تحت کنترل GM هستند، نه دارایی آزاد بازیکن؛ Loyalty و شرح وظیفه را وارد صحنه کن."
            )
        }
        item {
            RoleHelperCharacterCard(
                title = "کاراکتر Exec",
                characters = execs,
                selectedId = selectedId,
                onSelect = { selectedId = it },
                summary = { c -> gtr("Teamwork Rank %1s • Contacts %2s • بودجه‌ی آزاد %3s eb", c.roleRank, c.contacts.size, c.eurodollars) }
            )
        }
        exec?.let { c -> item { CharacterSheetLifepathCard(c, "Exec") } }
        // ── استخدام Team Member (Core: 1d6+1 Loyalty) ──
        item {
            RoleHelperCard("استخدام Team Member (Teamwork)") {
                FaText(
                    "Team Memberها نیروهای شرکت زیر دست Exec هستند. کلاس را انتخاب کن؛ با یک کلیک همان NPC از تمپلیت آماده ساخته می‌شود، Loyalty شروعش با 1d6 + 1 تعیین می‌شود و اگر بخواهی مستقیم به فهرست شرکت‌کننده‌های نبرد هم اضافه می‌شود.",
                    color = RoleUi.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true
                )
                RoleHelperChipRow(
                    labels = teamClasses.mapIndexed { i, c -> i to c.name },
                    selectedId = execClassIndex,
                    onSelect = { execClassIndex = it; execHireMsg = null },
                    prefix = "کلاس تیم:"
                )
                val cls = teamClasses[execClassIndex]
                RoleInnerBox {
                    Text(gtr(cls.name + " — " + cls.nameFa), color = RoleUi.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FaText("نقش پوششی: " + cls.coverJobsFa, color = RoleUi.White, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    FaText("کار واقعی: " + cls.trueJobFa, color = RoleUi.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    Spacer(Modifier.height(4.dp))
                    ExecStatTable(cls)
                    Spacer(Modifier.height(4.dp))
                    FaText("+۲: " + cls.skills2Fa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    FaText("+۴: " + cls.skills4Fa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    if (cls.skills6Fa != "—") FaText("+۶: " + cls.skills6Fa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    FaText("سایبرور: " + cls.cyberwareFa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    FaText("تجهیزات: " + cls.gearFa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                }
                HelperToggle("همراه با افزودن به فهرست نبرد", execAddToEncounter) { execAddToEncounter = it }
                Button(
                    onClick = { hireMember() },
                    colors = ButtonDefaults.buttonColors(containerColor = RoleUi.Red),
                    shape = CutCornerShape(6.dp), modifier = Modifier.fillMaxWidth()
                ) { Text(gtr("HIRE • استخدام (1d6 + 1 Loyalty)"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                execHireMsg?.let { FaText(it, color = RoleUi.Green, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true) }
            }
        }

        // ── فهرست Team Memberهای ساخته‌شده + Loyalty ──
        item {
            RoleHelperCard("Team Memberهای فعلی (%1s)".replace("%1s", team.size.toString())) {
                if (team.isEmpty()) {
                    FaText("هنوز Team Memberی ساخته نشده. از کارت بالا یکی استخدام کن.", color = RoleUi.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                }
                team.forEach { member ->
                    val loy = teamMemberLoyalty(member)
                    RoleInnerBox {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(member.name, color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(gtr("HP %1s/%2s • skills %3s • Loyalty %4s", member.hp, member.maxHp, member.skills.size, loy),
                                    color = RoleUi.Muted, fontSize = 10.sp)
                            }
                            Text(
                                gtr("Loyalty %1s", loy),
                                color = when {
                                    loy <= 0 -> RoleUi.Red
                                    loy <= 2 -> RoleUi.Amber
                                    else -> RoleUi.Green
                                },
                                fontWeight = FontWeight.Bold, fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.adjustTeamLoyalty(member.id, 1); loyaltySaveMsg = null },
                                shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                            ) { Text("+1 تعریف", color = RoleUi.Green, fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { viewModel.adjustTeamLoyalty(member.id, -1); loyaltySaveMsg = null },
                                shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                            ) { Text("-1 سرزنش", color = RoleUi.Red, fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { loyaltyPickMemberId = member.id; loyaltySave() },
                                shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                            ) { Text("Loyalty Save (1d6)", color = RoleUi.White, fontSize = 10.sp) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.adjustTeamLoyalty(member.id, -8) },
                                shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                            ) { Text("-8 رهاشدن زیر آتش", color = RoleUi.Red, fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { viewModel.adjustTeamLoyalty(member.id, 8) },
                                shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                            ) { Text("+8 خطر برای او", color = RoleUi.Green, fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { spawnResult = viewModel.spawnNpcTemplates(member.name, 1, addToEncounter = true) },
                                shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                            ) { Text("نسخه‌ی نبرد", color = RoleUi.Red, fontSize = 10.sp) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { spawnResult = "«${member.name}» گم شد → جایگزین در جلسه‌ی بعد با Loyalty ۱ و ۲۰۰eb هزینه‌ی استخدام استخدام می‌شود." },
                                shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                            ) { Text("گم شد (۲۰۰eb)", color = RoleUi.Amber, fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = {
                                    viewModel.deleteCharacter(member.id)
                                    spawnResult = "«${member.name}» از فهرست حذف شد."
                                },
                                shape = CutCornerShape(4.dp), modifier = Modifier.weight(1f)
                            ) { Text("حذف از فهرست", color = RoleUi.Muted, fontSize = 10.sp) }
                        }
                    }
                }
                loyaltySaveMsg?.let { FaText(it, color = RoleUi.White, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true) }
                spawnResult?.let { FaText(it, color = RoleUi.Green, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true) }
            }
        }

        // ── مزایا و قواعد Core ──
        item {
            RoleHelperCard("مزایا و قواعد Teamwork (Core)") {
                FaText("هر Rank که Teamwork بالا می‌رود مزایای تازه‌ای می‌دهد:", color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                ExecTeamData.rankPerks.forEach { (r, title, body) ->
                    RoleInnerBox {
                        Text(gtr("Rank %1s — %2s", r, title), color = RoleUi.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        FaText(body, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    }
                }
                Spacer(Modifier.height(4.dp))
                ExecTeamData.teamRules.forEach { (t, b) ->
                    RoleInnerBox {
                        Text(t, color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        FaText(b, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    }
                }
            }
        }

        // ── جدول‌های Loyalty ──
        item {
            RoleHelperCard("جدول‌های Loyalty (Core)") {
                FaText("به‌دست‌آوردن Loyalty", color = RoleUi.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                ExecTeamData.loyaltyGain.forEach { (text, gain) ->
                    Text(gtr("+%1s — %2s", gain, text), color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.height(6.dp))
                FaText("از دست دادن Loyalty", color = RoleUi.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                ExecTeamData.loyaltyLoss.forEach { (text, loss) ->
                    Text(gtr("%1s — %2s", loss, text), color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.height(6.dp))
                FaText(
                    "Loyalty بین جلسه‌های بازی حداکثر ۱۰ است، اما در طول یک جلسه حد ندارد. Loyalty صفر یا کمتر یعنی عضو به دشمنان اگزک خیانت می‌کند.",
                    color = RoleUi.Amber, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true
                )
            }
        }

        item {
            HelperCard("چه منبعی را خرج کنم؟", "انتخاب کن تا هزینه، محدودیت و قدم بعدی را بگوید.") {
                PickerRows(RoleHelperData.execResources, resource) { resource = it }
                HelperToggle("اختیار سازمانی دارد", hasAuthority) { hasAuthority = it }
                HelperToggle("زمان کم است", timeShort) { timeShort = it }
                Spacer(Modifier.height(2.dp))
                val (verdict, tone) = RoleHelperData.execCost(resource, rank)
                DecisionBox(verdict, tone)
            }
        }
        item { RoleScenarioCard(execScenarios, scenarioIndex) { scenarioIndex = (scenarioIndex + 1) % execScenarios.size } }
        item {
            HelperCard("فشار سازمانی بساز") {
                RollBox("فشار بعدی", RoleHelperData.execPressures, "برای وقتی که همین حالا یک صحنه لازم داری.")
            }
        }
        item {
            RoleGloryCard(
                "Exec وقتی می‌درخشد که منابع شرکت جواب را خودکار ندهند، بلکه انتخاب بسازند: کدام عضو تیم؟ کدام بودجه؟ چه کسی را ناراضی می‌کنی؟ اطلاعات واقعی و قدرت سازمانی را به تصمیم تبدیل کن."
            )
        }
    }
}

// ══════════════════════════════ LAWMAN ══════════════════════════════

@Composable
fun LawmanGmAssistant(viewModel: CharacterViewModel) {
    val characters by viewModel.characters.collectAsState()
    val lawmen = characters.filter { it.role.equals("Lawman", true) && it.isAlly }
    var selectedId by remember(lawmen.map { it.id }) { mutableStateOf(lawmen.firstOrNull()?.id) }
    val lawman = lawmen.firstOrNull { it.id == selectedId } ?: lawmen.firstOrNull()
    var requestedRank by remember { mutableStateOf(0) }
    var callResult by remember { mutableStateOf<String?>(null) }
    var callSucceeded by remember { mutableStateOf(false) }
    var arrivalResult by remember { mutableStateOf<String?>(null) }
    var arrivingUnitTier by remember { mutableStateOf<Int?>(null) }
    var scenarioIndex by remember { mutableIntStateOf(0) }
    var scene by remember { mutableStateOf(RoleHelperData.lawmanScenes.first()) }
    var hasWarrant by remember { mutableStateOf(true) }
    var spawnResult by remember { mutableStateOf<String?>(null) }
    val rank = (lawman?.roleRank ?: 4).coerceIn(1, 10)
    // Core: می‌توانی رده‌ی Rank خودت یا هر رده‌ی پایین‌تر را بخواهی.
    val chosenRank = when {
        requestedRank in 1..rank -> requestedRank
        else -> rank
    }

    RoleHelperPage {
        item {
            RoleHelperHeader(
                "دستیار Lawman برای GM",
                "توانایی Backup این Lawman از Rank همان برگه می‌آید: 1d10 مساوی یا کمتر از Rank = کسی پاسخ می‌دهد، بعد 1d6 = رسیدن در چند Rounds، و ۶ روی آن تاس یعنی نیروی یک رده بالاتر. نیروها با یک Combat Number واحد می‌جنگند و نمی‌توانند از گلوله Dodge کنند."
            )
        }
        item {
            RoleHelperCharacterCard(
                title = "کاراکتر Lawman",
                characters = lawmen,
                selectedId = selectedId,
                onSelect = { selectedId = it; callResult = null; arrivalResult = null },
                summary = { c ->
                    val pending = BackupRules.pendingRounds(c)
                    gtr("Backup Rank %1s • واحد فعلی: %2s%3s", c.roleRank, BackupRules.unitForRank(c.roleRank).tierName,
                        if (pending != null) " • ⏳ نیرو در راه: $pending راند" else "")
                }
            )
        }
        lawman?.let { c -> item { CharacterSheetLifepathCard(c, "Lawman") } }

        // ── تماس با Backup (RAW) ──
        item {
            RoleHelperCard("Backup — تماس و رسیدن نیرو") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FaText(gtr("Rank فعلی: %1s", rank), color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    RuleInfoButton("lawman.backup")
                }
                FaText(
                    "می‌توانی نیروی Rank خودت یا رده‌های پایین‌تر را بخواهی؛ روی رده‌ی زیر کلیک کن تا انتخاب شود.",
                    color = RoleUi.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true
                )
                com.cyberpunk.gmtool.ui.components.RoleHelperChipRow(
                    labels = (1..rank).map { r -> r to gtr("Rank %1s", r) },
                    selectedId = chosenRank,
                    onSelect = { requestedRank = it; callResult = null; arrivalResult = null },
                    prefix = "رده‌ی درخواستی:"
                )
                // کارت واحدی که با این انتخاب می‌آید
                val unit = BackupRules.unitForRank(chosenRank)
                RoleInnerBox {
                    Text(gtr("%1s • %2s", unit.tierName, unit.tierNameFa), color = RoleUi.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(gtr("تعداد %1s • %2s", unit.count, unit.gearFa), color = RoleUi.White, fontSize = 11.sp)
                    Text(gtr("Combat Number %1s • SP %2s • HP %3s • MOVE/BODY %4s • %5s", unit.combatNumber, unit.sp, unit.hp, unit.moveBody, unit.transportFa), color = RoleUi.Muted, fontSize = 11.sp)
                    if (unit.templateNoteFa.isNotBlank()) FaText(unit.templateNoteFa, color = RoleUi.Amber, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
                }
                // تاس‌ها با همان الگوی استاندارد برنامه ریخته می‌شوند: اگر GM
                // «تاس فیزیکی» را در تنظیمات روشن کرده باشد، دیالوگ ورود عدد باز
                // می‌شود و هیچ عددی از خود برنامه نمی‌آید.
                val rollCall = rememberAutoDiceRoller(10) { die ->
                    val ok = BackupRules.callSucceeds(chosenRank, die)
                    callSucceeded = ok
                    callResult = if (ok)
                        gtr("CALL BACKUP: تاس %1s ≤ Rank %2s → کسی پاسخ داد. حالا ARRIVAL (1d6) را بریز.", die, chosenRank)
                    else gtr("CALL BACKUP: تاس %1s > Rank %2s → کسی جواب نداد. این توانایی Action می‌گیرد؛ نوبت بعد می‌توانی دوباره تلاش کنی.", die, chosenRank)
                    arrivalResult = null
                }
                val rollArrival = rememberAutoDiceRoller(6) { d6 ->
                    val upgraded = BackupRules.upgradesTier(d6)
                    val tierRank = if (upgraded) (chosenRank + 1).coerceAtMost(10) else chosenRank
                    arrivingUnitTier = tierRank
                    val unit2 = BackupRules.unitForRank(tierRank)
                    arrivalResult = if (upgraded)
                        gtr("ARRIVAL: تاس ۶ → رده یک پله بالا رفت. در %1s راند «%2s» می‌رسد (تعداد %3s).", d6, unit2.tierName, unit2.count)
                    else gtr("ARRIVAL: نیروها در %1s راند می‌رسند → «%2s» (تعداد %3s).", d6, unit2.tierName, unit2.count)
                    lawman?.let { c -> viewModel.addCombatEffect(c.id, BackupRules.pendingEffectKey(d6), -1) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { rollCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoleUi.Red),
                        shape = CutCornerShape(6.dp), modifier = Modifier.weight(1f)
                    ) { Text(gtr("CALL BACKUP (1d10)"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { rollArrival() },
                        enabled = callSucceeded,
                        shape = CutCornerShape(6.dp), modifier = Modifier.weight(1f)
                    ) { Text(gtr("ARRIVAL (1d6)"), color = RoleUi.Red, fontSize = 11.sp) }
                }
                callResult?.let { FaText(it, color = if (callSucceeded) RoleUi.Green else RoleUi.Amber, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true) }
                arrivalResult?.let { FaText(it, color = RoleUi.White, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true) }
                arrivingUnitTier?.let { tier ->
                    val unit2 = BackupRules.unitForRank(tier)
                    val count = if (BackupRules.upgradedCountIsDoubled(chosenRank) && tier != chosenRank) unit2.count * 2 else unit2.count
                    Button(
                        onClick = { spawnResult = viewModel.spawnNpcTemplates(unit2.npcTemplateName, count, addToEncounter = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoleUi.Green),
                        shape = CutCornerShape(6.dp), modifier = Modifier.fillMaxWidth()
                    ) { Text(gtr("ساخت %1s × %2s و افزودن به نبرد", unit2.npcTemplateName, count), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                }
                spawnResult?.let { FaText(it, color = RoleUi.Green, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true) }
                lawman?.let { c ->
                    if (BackupRules.isPending(c)) {
                        Button(
                            onClick = {
                                viewModel.updateCharacter(c.id) { ch ->
                                    ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("backup_en_route_") })
                                }
                                spawnResult = "وضعیت «نیرو در راه» پاک شد."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoleUi.CardSoft),
                            shape = CutCornerShape(6.dp), modifier = Modifier.fillMaxWidth()
                        ) { Text(gtr("نیروها رسیدند — پاک‌کردن وضعیت"), color = RoleUi.Muted, fontSize = 11.sp) }
                    }
                }
            }
        }

        // ── راهنمای سریع قواعد Backup ──
        item {
            RoleHelperCard("Backup چطور کار می‌کند؟ (خلاصه‌ی Core)") {
                BackupRules.quickGuide.forEach { (title, body) ->
                    FaText("• $title — $body", color = RoleUi.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                }
            }
        }

        // ── جدول رده‌ها ──
        item {
            RoleHelperCard("رده‌های Backup (Core)") {
                BackupRules.tiers.distinctBy { it.tierName }.forEach { u ->
                    RoleInnerBox {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(gtr("%1s • %2s", u.tierName, u.tierNameFa), color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(gtr("×%1s", u.count), color = RoleUi.Red, fontSize = 11.sp)
                        }
                        FaText(u.descriptionFa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                        Text(gtr("CN %1s • SP %2s • HP %3s • MOVE/BODY %4s • %5s", u.combatNumber, u.sp, u.hp, u.moveBody, u.transportFa), color = RoleUi.Muted, fontSize = 10.sp)
                        Text(gtr("تمپلیت: %1s", u.npcTemplateName), color = RoleUi.Muted, fontSize = 10.sp)
                    }
                }
            }
        }

        // ── رنک ۱۰: تفاوت‌ها و مهارت‌های Combat Number ──
        item {
            RoleHelperCard("رنک ۱۰ — پلیس ملی / Interpol / Netwatch") {
                RoleLoreData.lawmanRank10Rules.forEach { (t, b) ->
                    RoleInnerBox {
                        Text(t, color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        FaText(b, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    }
                }
            }
        }

        // ── کادر «Backup in Action» کتاب ──
        item {
            RoleHelperCard("نمونه‌ی اجرا: Backup in Action") {
                RoleLoreData.lawmanExample.forEach { paragraph ->
                    FaText(paragraph, color = RoleUi.White, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                }
            }
        }

        item {
            HelperCard("الان در چه صحنه‌ای هستیم؟", "انتخاب کن تا هزینه، محدودیت و قدم بعدی را بگوید.") {
                PickerRows(RoleHelperData.lawmanScenes, scene) { scene = it }
                HelperToggle("حکم قانونی/اختیار دارد", hasWarrant) { hasWarrant = it }
                Spacer(Modifier.height(2.dp))
                val (verdict, tone) = RoleHelperData.lawmanAdvice(scene, rank, hasWarrant)
                DecisionBox(verdict, tone)
            }
        }
        item { RoleScenarioCard(lawmanScenarios, scenarioIndex) { scenarioIndex = (scenarioIndex + 1) % lawmanScenarios.size } }
        item {
            HelperCard("یک پرونده بساز") {
                RollBox("پرونده‌ی بعدی", RoleHelperData.lawmanCases, "برای وقتی که همین حالا یک صحنه لازم داری.")
            }
        }
        item {
            RoleHelperCard("تمپلیت‌های Backup در برنامه") {
                NpcData.templatesByCategory(NpcData.CAT_LAWMAN).forEach { t ->
                    RoleInnerBox {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            FaText(t.name, color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(t.difficulty.name, color = RoleUi.Amber, fontSize = 10.sp)
                        }
                        FaText(t.description, color = RoleUi.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                        OutlinedButton(
                            onClick = { spawnResult = viewModel.spawnNpcTemplates(t.name, 1, addToEncounter = true) },
                            shape = CutCornerShape(6.dp), modifier = Modifier.fillMaxWidth()
                        ) { Text(gtr("ساخت NPC و افزودن به نبرد"), color = RoleUi.Red, fontSize = 11.sp) }
                    }
                }
            }
        }
        item {
            RoleGloryCard(
                "Lawman را فقط تیراندازِ دارای Backup نکن. صحنه‌ای بده که اختیار قانونی، شاهد، مدرک، زمان رسیدن نیروها و مسئولیت تصمیم مهم باشند. پرونده‌ی خوب باید بعد از نبرد هم ادامه داشته باشد."
            )
        }
    }
}

// ═══════════════════════════════ FIXER ═══════════════════════════════

@Composable
fun FixerGmAssistant(viewModel: CharacterViewModel) {
    val characters by viewModel.characters.collectAsState()
    val fixers = characters.filter { it.role.equals("Fixer", true) && it.isAlly }
    var selectedId by remember(fixers.map { it.id }) { mutableStateOf(fixers.firstOrNull()?.id) }
    val fixer = fixers.firstOrNull { it.id == selectedId } ?: fixers.firstOrNull()
    var request by remember { mutableStateOf(RoleHelperData.fixerRequests.first()) }
    var timeShort by remember { mutableStateOf(false) }
    var scenarioIndex by remember { mutableIntStateOf(0) }
    val rank = fixer?.roleRank ?: 4

    RoleHelperPage {
        item {
            RoleHelperHeader(
                "دستیار Fixer برای GM",
                "Operator چهار اهرم دارد: Contacts & Clients، Reach، Haggle و Grease. Rank تعیین می‌کند چه شبکه و سطح کالایی واقعاً در دسترس است؛ Hoggle قیمت را عوض می‌کند ولی دسترسی را جادو نمی‌کند."
            )
        }
        item {
            RoleHelperCharacterCard(
                title = "کاراکتر Fixer",
                characters = fixers,
                selectedId = selectedId,
                onSelect = { selectedId = it },
                summary = { c -> gtr("Operator Rank %1s • Contactها %2s • نقدینگی %3s eb • Reputation %4s", c.roleRank, c.contacts.size, c.eurodollars, c.reputation) }
            )
        }
        fixer?.let { c -> item { CharacterSheetLifepathCard(c, "Fixer") } }
        fixer?.let { c ->
            item {
                RoleHelperCard("شبکه‌ی همین Fixer (از برگه)") {
                    if (c.contacts.isEmpty()) {
                        FaText("هنوز Contactی روی برگه ثبت نشده. از BIO → بخش Contactها اضافه کن تا اینجا زنده دیده شود.", color = RoleUi.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    }
                    c.contacts.forEach { contact ->
                        RoleInnerBox {
                            Text(gtr(contact.name), color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (contact.detail.isNotBlank()) FaText(contact.detail, color = RoleUi.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                        }
                    }
                }
            }
        }
        item {
            HelperCard("مشتری چه می‌خواهد؟", "انتخاب کن تا هزینه، محدودیت و قدم بعدی را بگوید.") {
                PickerRows(RoleHelperData.fixerRequests, request) { request = it }
                HelperToggle("زمان کم است", timeShort) { timeShort = it }
                Spacer(Modifier.height(2.dp))
                val (verdict, tone) = RoleHelperData.fixerAdvice(request, rank, timeShort)
                DecisionBox(verdict, tone)
            }
        }
        // ── ردههای اپراتور (Core): چهار مقدار هر رده ──
        item {
            RoleHelperCard("رده‌های اپراتور (Core)") {
                RoleInnerBox {
                    val b = RoleLoreData.fixerBandFor(rank)
                    Text(gtr("رنک فعلی %1s — %2s", rank, b.ranksLabelFa), color = RoleUi.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    FaText("Contacts & Clients: " + b.contactsFa, color = RoleUi.White, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    FaText("Reach: " + b.reachFa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    FaText("Haggle: " + b.haggleFa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    FaText("Grease: " + b.greaseFa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                }
                RoleLoreData.fixerBands.forEach { b ->
                    val current = rank in b.range
                    RoleInnerBox {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (current) b.ranksLabelFa + "  ← رنک فعلی" else b.ranksLabelFa,
                                color = if (current) RoleUi.Red else RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FaText("Reach: " + b.reachFa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 16.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                        FaText("Haggle: " + b.haggleFa, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 16.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    }
                }
            }
        }

        // ── قواعد دقیق توانایی اپراتور ──
        item {
            RoleHelperCard("اپراتور چطور کار می‌کند؟") {
                RoleLoreData.fixerOperatorParts.forEach { (t, b) ->
                    RoleInnerBox {
                        Text(t, color = RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        FaText(b, color = RoleUi.Muted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    }
                }
                Spacer(Modifier.height(4.dp))
                FaText(
                    "دسته‌های قیمتی Core: " + RoleLoreData.fixerPriceCategories.joinToString(" • "),
                    color = RoleUi.Amber, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth(), justify = true
                )
            }
        }

        item { RoleScenarioCard(fixerScenarios, scenarioIndex) { scenarioIndex = (scenarioIndex + 1) % fixerScenarios.size } }
        item {
            HelperCard("یک کار بساز") {
                RollBox("کار بعدی", RoleHelperData.fixerJobs, "برای وقتی که همین حالا یک صحنه لازم داری.")
            }
        }
        item {
            RoleGloryCard(
                "Fixer باید احساس کند شبکه‌اش زنده است. هر Contact اسم، انگیزه و قیمت خودش را داشته باشد؛ بعضی معامله‌ها با eb بسته می‌شوند و بعضی با Favor، اعتبار یا معرفی. Reach باید واقعاً درهای جدید باز کند."
            )
        }
    }
}
