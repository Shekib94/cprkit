package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CampaignStore
import com.cyberpunk.gmtool.data.CampaignSessionHistory
import com.cyberpunk.gmtool.data.GameSession
import com.cyberpunk.gmtool.data.PersistedSessionState
import com.cyberpunk.gmtool.data.PlayerSessionReward
import com.cyberpunk.gmtool.data.SessionLogEntry
import com.cyberpunk.gmtool.data.SessionStore
import com.cyberpunk.gmtool.data.PersistenceRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SRed = Color(0xFFD32F2F)
private val SBlack = Color(0xFF0F0F0F)
private val SWhite = Color(0xFFE0E0E0)
private val SMuted = Color(0xFFAAAAAA)
private val SCard = Color(0xFF1A1A1A)

@Composable
fun SessionScreen(
    characters: List<Character>,
    viewModel: CharacterViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { SessionStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val generation by viewModel.dataGeneration.collectAsState()
    val epoch = remember(generation) { PersistenceRuntime.currentEpoch() }
    var sessions by remember(generation) { mutableStateOf<List<GameSession>>(emptyList()) }
    var count by remember(generation) { mutableStateOf(1) }
    var activeSessionId by remember(generation) { mutableStateOf<Long?>(null) }
    var openedId by remember(generation) { mutableStateOf<Long?>(null) }
    var activeCampaignId by remember(generation) { mutableStateOf<Long?>(null) }
    var hydrated by remember(generation) { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(generation) {
        val loaded = withContext(Dispatchers.IO) {
            PersistenceRuntime.locked {
                val raw = store.load()
                val campaignStore = CampaignStore(context.applicationContext)
                val campaigns = campaignStore.load()
                val validCampaignIds = campaigns.map { it.id }.toSet()
                val repairedSessions = raw.sessions.map { session ->
                    val campaignFixed = if (session.campaignId != null && session.campaignId !in validCampaignIds) session.copy(campaignId = null) else session
                    campaignFixed.copy(players = campaignFixed.players.map { r ->
                        // Migration from the old one-shot Session reward model.
                        if (r.awardCount == 0 && r.awarded) r.copy(
                            ip = 0, eb = 0, rep = 0, awarded = false, awardCount = 1,
                            awardedIpTotal = r.ip, awardedEbTotal = r.eb, awardedRepTotal = r.rep
                        ) else r.copy(awarded = false)
                    })
                }
                val repaired = raw.copy(
                    sessions = repairedSessions,
                    activeSessionId = raw.activeSessionId?.takeIf { id -> repairedSessions.any { it.id == id && !it.ended } }
                )
                if (repaired != raw) store.save(repaired, epoch)

                // Self-heal a crash between ending a Session and writing Campaign history.
                repaired.sessions.filter { it.ended && it.campaignId != null }.forEach { ended ->
                    val paid = ended.players.filter { it.awardCount > 0 }
                    campaignStore.recordSession(ended.campaignId, CampaignSessionHistory(
                        sessionId = ended.id, name = ended.name, date = ended.date,
                        playerIds = paid.map { it.characterId }, totalIp = paid.sumOf { it.awardedIpTotal },
                        totalEb = paid.sumOf { it.awardedEbTotal }, totalRep = paid.sumOf { it.awardedRepTotal },
                        summary = ended.log.take(5).joinToString(" • ") { it.text }
                    ), epoch)
                }
                repaired to campaigns.firstOrNull { it.active }?.id
            }
        }
        sessions = loaded.first.sessions
        count = loaded.first.count
        activeSessionId = loaded.first.activeSessionId
        openedId = loaded.first.activeSessionId
        activeCampaignId = loaded.second
        hydrated = true
    }

    // Long histories are serialized off the UI thread, and cancelled/restarted while the GM is still typing.
    LaunchedEffect(sessions, count, activeSessionId, generation, hydrated) {
        if (!hydrated) return@LaunchedEffect
        delay(200)
        val snapshot = PersistedSessionState(count, sessions, activeSessionId)
        withContext(Dispatchers.IO) { store.save(snapshot, epoch) }
    }

    LaunchedEffect(characters.map { it.id to it.appliedTransactionIds.size }, hydrated) {
        if (!hydrated) return@LaunchedEffect
        val byId = characters.associateBy { it.id }
        val repaired = sessions.map { session -> session.copy(players = session.players.map { r ->
            val legacyTx = "session_reward_${session.id}_${r.characterId}" in (byId[r.characterId]?.appliedTransactionIds ?: emptyList())
            if (r.awardCount == 0 && legacyTx) r.copy(
                ip = 0, eb = 0, rep = 0, awarded = false, awardCount = 1,
                awardedIpTotal = r.ip, awardedEbTotal = r.eb, awardedRepTotal = r.rep
            ) else if (r.awarded) r.copy(awarded = false) else r
        }) }
        if (repaired != sessions) sessions = repaired
    }

    val allPlayers = characters.filter { it.isAlly }
    val opened = sessions.firstOrNull { it.id == openedId }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize().background(SBlack)) {
            if (!hydrated) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SRed) }
            } else if (opened == null) {
                SessionManager(
                    sessions = sessions,
                    viewModel = viewModel,
                    allPlayers = allPlayers,
                    onBack = onBack,
                    onNew = { scope.launch { activeCampaignId = withContext(Dispatchers.IO) { CampaignStore(context.applicationContext).activeId() }; showNew = true } },
                    onOpen = { openedId = it.id; if (!it.ended) activeSessionId = it.id },
                    onDelete = { deleteId = it.id }
                )
            } else {
                SessionDetail(
                    session = opened,
                    allPlayers = allPlayers,
                    viewModel = viewModel,
                    onBack = { openedId = null },
                    onDelete = { deleteId = opened.id },
                    onUpdate = { updated -> sessions = sessions.map { if (it.id == updated.id) updated else it } },
                    onEnd = { updated ->
                        val ended = updated.copy(ended = true)
                        val nextSessions = sessions.map { if (it.id == updated.id) ended else it }
                        val nextActive = activeSessionId?.takeIf { it != ended.id }
                        val paid = ended.players.filter { it.awardCount > 0 }
                        val history = CampaignSessionHistory(
                            sessionId = ended.id, name = ended.name, date = ended.date,
                            playerIds = paid.map { it.characterId }, totalIp = paid.sumOf { it.awardedIpTotal },
                            totalEb = paid.sumOf { it.awardedEbTotal }, totalRep = paid.sumOf { it.awardedRepTotal },
                            // Log is newest-first; take(5) is the five latest entries.
                            summary = ended.log.take(5).joinToString(" • ") { it.text }
                        )
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) {
                                PersistenceRuntime.locked {
                                    val ok = store.save(PersistedSessionState(count, nextSessions, nextActive), epoch)
                                    if (ok) CampaignStore(context.applicationContext).recordSession(ended.campaignId, history, epoch)
                                    ok
                                }
                            }
                            if (saved) {
                                sessions = nextSessions
                                activeSessionId = nextActive
                                openedId = null
                                PersistenceRuntime.publishExternalDataChange()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showNew) {
        NewSessionDialog(count, onDismiss = { showNew = false }) { name ->
            val date = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US).format(Date())
            val created = GameSession(campaignId = activeCampaignId, name = name, date = date)
            sessions = listOf(created) + sessions
            count += 1
            activeSessionId = created.id
            openedId = created.id
            showNew = false
        }
    }

    deleteId?.let { id ->
        val doomed = sessions.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { deleteId = null },
            containerColor = SCard,
            title = { Text("حذف جلسه", color = SWhite, fontWeight = FontWeight.Bold) },
            text = { Text("این جلسه و یادداشت‌هایش حذف شود؟ رکورد Summary آن نیز از تاریخچه Campaign پاک می‌شود تا تاریخچه یتیم نماند.", color = SMuted) },
            confirmButton = {
                TextButton(onClick = {
                    val nextSessions = sessions.filterNot { it.id == id }
                    val nextActive = activeSessionId?.takeIf { it != id }
                    deleteId = null
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            PersistenceRuntime.locked {
                                val ok = store.save(PersistedSessionState(count, nextSessions, nextActive), epoch)
                                if (ok) CampaignStore(context.applicationContext).removeSessionHistory(id, epoch)
                                ok
                            }
                        }
                        if (saved) {
                            sessions = nextSessions
                            activeSessionId = nextActive
                            if (openedId == id) openedId = null
                            PersistenceRuntime.publishExternalDataChange()
                        }
                    }
                }) { Text("حذف", color = SRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("انصراف", color = SWhite) } }
        )
    }
}

@Composable
private fun SessionManager(
    sessions: List<GameSession>,
    viewModel: CharacterViewModel,
    allPlayers: List<Character>,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onOpen: (GameSession) -> Unit,
    onDelete: (GameSession) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr("SESSION MANAGER"), color = SWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
                Text("برنامه‌ریزی جلسه، انتخاب بازیکنان و ثبت پاداش جداگانه", color = SMuted, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp), textAlign = TextAlign.Right)
            }
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, gtr("Back"), tint = SRed) }
        }
        Spacer(Modifier.height(16.dp))

        // ── پیوستن به سشن GM ──
        // بالای «ساخت جلسه» می‌نشیند: بازیکن نباید مجبور باشد اول یک جلسه
        // بسازد تا بتواند به میز GM وصل شود.
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            shape = CutCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.padding(12.dp)) {
                LanSessionPanel(
                    viewModel = viewModel,
                    characters = allPlayers,
                    joinOnly = true
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Button(onClick = onNew, colors = ButtonDefaults.buttonColors(containerColor = SRed),
            shape = CutCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Add, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("ساخت جلسه‌ی جدید", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز جلسه‌ای ثبت نشده.", color = SMuted, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id }) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(s) }
                            .background(SCard, CutCornerShape(9.dp))
                            .border(1.dp, SRed.copy(.35f), CutCornerShape(9.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onDelete(s) }) { Icon(Icons.Default.Delete, gtr("Delete"), tint = SRed) }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (s.ended) Text("پایان‌یافته", color = SMuted, fontSize = 11.sp)
                                else Text("فعال", color = SRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(gtr(s.name), color = SWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(gtr(s.date), color = SMuted, fontSize = 11.sp)
                            }
                            Text("${s.players.size} بازیکن", color = SMuted, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronLeft, null, tint = SRed)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SessionDetail(
    session: GameSession,
    allPlayers: List<Character>,
    viewModel: CharacterViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (GameSession) -> Unit,
    onEnd: (GameSession) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showPlayers by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, gtr("Delete session"), tint = SRed) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(gtr(session.name), color = SWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr(session.date), color = SMuted, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, gtr("Back"), tint = SRed) }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { showPlayers = true }, border = androidx.compose.foundation.BorderStroke(1.dp, SRed),
                shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.GroupAdd, null, tint = SRed)
                Spacer(Modifier.width(8.dp))
                Text("بازیکنان حاضر در جلسه (${session.players.size})", color = SRed, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))

            // ── سشن محلی (LAN) ──
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                shape = CutCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(12.dp)) {
                    LanSessionPanel(
                        viewModel = viewModel,
                        characters = allPlayers,
                        sessionId = session.id,
                        sessionName = session.name
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            Text("پاداش هر بازیکن", color = SRed, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Text("می‌توانی در یک جلسه چند مأموریت/مرحله را جداگانه پاداش بدهی؛ هر ثبت به مجموع همان جلسه اضافه می‌شود.", color = SMuted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        }

        if (session.players.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                    Text("هنوز بازیکنی به این جلسه اضافه نشده.", color = SMuted, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(session.players, key = { it.characterId }) { reward ->
                val player = allPlayers.firstOrNull { it.id == reward.characterId }
                if (player != null) {
                    PlayerRewardCard(player, reward, canEdit = !session.ended,
                        onChange = { changed ->
                            onUpdate(session.copy(players = session.players.map { if (it.characterId == changed.characterId) changed else it }))
                        },
                        onAward = {
                            if (!session.ended && (reward.ip > 0 || reward.eb > 0 || reward.rep > 0)) scope.launch {
                                viewModel.awardSessionReward(session, reward.characterId).onSuccess(onUpdate)
                            }
                        }
                    )
                }
            }
        }

        item {
            if (session.players.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = !session.ended && session.players.any { (it.ip > 0 || it.eb > 0 || it.rep > 0) && allPlayers.any { p -> p.id == it.characterId } },
                    onClick = { scope.launch { viewModel.awardAllSessionRewards(session).onSuccess(onUpdate) } },
                    colors = ButtonDefaults.buttonColors(containerColor = SRed), shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.CardGiftcard, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("ثبت پاداش فعلی برای همه", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("یادداشت گیم‌مستر", color = SRed, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = logText, onValueChange = { logText = it },
                placeholder = { Text("اتفاق جلسه را بنویس…", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SRed, unfocusedBorderColor = SRed.copy(.5f),
                    focusedTextColor = SWhite, unfocusedTextColor = SWhite),
                shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(90.dp),
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right)
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (logText.isNotBlank()) {
                    val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                    onUpdate(session.copy(log = listOf(SessionLogEntry(time, logText.trim())) + session.log))
                    logText = ""
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = SRed), shape = CutCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp)); Text("افزودن یادداشت", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        items(session.log) { entry ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(SCard, CutCornerShape(8.dp))
                .border(1.dp, SRed.copy(.25f), CutCornerShape(8.dp)).padding(12.dp)) {
                Text(gtr(entry.text), color = SWhite, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
                Spacer(Modifier.width(10.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(entry.time), color = SRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            if (!session.ended) {
                OutlinedButton(onClick = { onEnd(session) }, border = androidx.compose.foundation.BorderStroke(1.dp, SRed),
                    shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("پایان جلسه", color = SRed, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("این جلسه پایان یافته است.", color = SMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }

    if (showPlayers) {
        PlayerPickerDialog(allPlayers, session.players.map { it.characterId }.toSet(), onDismiss = { showPlayers = false }) { ids ->
            val existing = session.players.associateBy { it.characterId }
            val updated = ids.map { id -> existing[id] ?: PlayerSessionReward(characterId = id) }
            onUpdate(session.copy(players = updated))
            showPlayers = false
        }
    }
}

@Composable
private fun PlayerRewardCard(player: Character, reward: PlayerSessionReward, canEdit: Boolean, onChange: (PlayerSessionReward) -> Unit, onAward: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SCard), shape = CutCornerShape(9.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 9.dp).border(1.dp, SRed.copy(.35f), CutCornerShape(9.dp))) {
        Column(Modifier.padding(12.dp)) {
            Text(player.handle.ifBlank { player.name }, color = SWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniRewardStepper("IP", reward.ip, canEdit, { onChange(reward.copy(ip = it)) })
                MiniRewardStepper("€$", reward.eb, canEdit, { onChange(reward.copy(eb = it)) })
                MiniRewardStepper(gtr("Rep"), reward.rep, canEdit, { onChange(reward.copy(rep = it)) })
            }
            if (reward.awardCount > 0) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "پرداخت‌شده در این جلسه: ${reward.awardCount} مرحله • IP ${reward.awardedIpTotal} • €$ ${reward.awardedEbTotal} • REP ${reward.awardedRepTotal}",
                    color = SMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = canEdit && (reward.ip > 0 || reward.eb > 0 || reward.rep > 0), onClick = onAward, colors = ButtonDefaults.buttonColors(containerColor = SRed),
                shape = CutCornerShape(7.dp), modifier = Modifier.fillMaxWidth().height(40.dp)) {
                Text("ثبت این پاداش", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RowScope.MiniRewardStepper(label: String, value: Int, enabled: Boolean, onValue: (Int) -> Unit) {
    Column(Modifier.weight(1f).background(SBlack.copy(.5f), CutCornerShape(7.dp)).padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(gtr(label), color = SMuted, fontSize = 11.sp)
        Text("$value", color = SRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = enabled, onClick = { onValue((value - 1).coerceAtLeast(0)) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Remove, null, tint = SMuted, modifier = Modifier.size(17.dp))
            }
            TextButton(enabled = enabled, onClick = { onValue(value + 1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(34.dp)) { Text("+1", color = SRed, fontSize = 11.sp) }
            TextButton(enabled = enabled, onClick = { onValue(value + 10) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(38.dp)) { Text("+10", color = SRed, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun PlayerPickerDialog(players: List<Character>, selectedInitially: Set<Int>, onDismiss: () -> Unit, onDone: (Set<Int>) -> Unit) {
    val selected = remember { mutableStateMapOf<Int, Boolean>().apply { players.forEach { put(it.id, it.id in selectedInitially) } } }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = SBlack), shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.75f).border(2.dp, SRed, CutCornerShape(12.dp))) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("بازیکنان حاضر", color = SWhite, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(players, key = { it.id }) { p ->
                        Row(Modifier.fillMaxWidth().clickable { selected[p.id] = !(selected[p.id] ?: false) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(p.handle.ifBlank { p.name }, color = SWhite, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
                            Checkbox(checked = selected[p.id] == true, onCheckedChange = { selected[p.id] = it },
                                colors = CheckboxDefaults.colors(checkedColor = SRed))
                        }
                    }
                }
                Button(onClick = { onDone(selected.filterValues { it }.keys) }, colors = ButtonDefaults.buttonColors(containerColor = SRed),
                    modifier = Modifier.fillMaxWidth().height(46.dp), shape = CutCornerShape(8.dp)) {
                    Text("تأیید", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NewSessionDialog(count: Int, onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var name by remember { mutableStateOf("جلسه‌ی $count") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = SBlack), shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, SRed, CutCornerShape(14.dp))) {
            Column(Modifier.padding(20.dp)) {
                Text("جلسه‌ی جدید", color = SWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام جلسه", color = SMuted) },
                    singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SRed, unfocusedBorderColor = SRed.copy(.5f),
                        focusedTextColor = SWhite, unfocusedTextColor = SWhite), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right))
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { if (name.isNotBlank()) onStart(name.trim()) }, colors = ButtonDefaults.buttonColors(containerColor = SRed),
                        shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f).height(46.dp)) {
                        Text("شروع جلسه", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onDismiss, shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f).height(46.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SMuted)) { Text("انصراف", color = SWhite) }
                }
            }
        }
    }
}
