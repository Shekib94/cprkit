package com.cyberpunk.gmtool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.gtr
import com.cyberpunk.gmtool.data.net.LanConnState
import com.cyberpunk.gmtool.data.net.LanRole
import com.cyberpunk.gmtool.data.net.LanTicket
import com.cyberpunk.gmtool.ui.components.LanStatusChip
import com.cyberpunk.gmtool.ui.components.PendingRequestBar
import com.cyberpunk.gmtool.ui.components.QrImage
import com.cyberpunk.gmtool.ui.components.QrScannerDialog
import com.cyberpunk.gmtool.ui.components.rememberLocalNetworkGate
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import kotlinx.coroutines.launch

private val CRed = Color(0xFFE53935)
private val CCard = Color(0xFF1A1A1A)
private val CWhite = Color(0xFFEEEEEE)
private val CMuted = Color(0xFF9E9E9E)
private val CDim = Color(0xFF202020)
private val CYellow = Color(0xFFFFC107)
private val CGreen = Color(0xFF4CAF50)

/**
 * پنل «سشن محلی» داخل تب Session.
 *
 * قاعده‌ی مهم: تا وقتی GM سشن نساخته، این بخش فقط دو دکمه است و
 * هیچ تأثیری روی بقیه‌ی برنامه ندارد. بازی می‌تواند کاملاً آفلاین ادامه یابد.
 */
@Composable
fun LanSessionPanel(
    viewModel: CharacterViewModel,
    characters: List<Character>,
    sessionId: Long = 0L,
    sessionName: String = "",
    /**
     * حالت «فقط پیوستن»: برای فهرست جلسات، جایی که بازیکن هنوز هیچ سشنی نساخته.
     * دکمه‌ی ساخت سشن پنهان می‌شود چون میزبانی به یک سشن واقعی نیاز دارد،
     * ولی پیوستن هیچ پیش‌نیازی ندارد و نباید پشت ساختِ سشن قفل باشد.
     */
    joinOnly: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val lan = viewModel.lan
    val role by lan.role.collectAsState()
    val state by lan.state.collectAsState()
    val slots by lan.slots.collectAsState()
    val pending by lan.pending.collectAsState()
    val suspended by lan.syncSuspended.collectAsState()
    val myId by lan.myCharacterId.collectAsState()

    var qrTicket by remember { mutableStateOf<LanTicket?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf<String?>(null) }
    var pickCharacter by remember { mutableStateOf<LanTicket?>(null) }
    var confirmIdentity by remember { mutableStateOf<LanTicket?>(null) }

    // از اندروید ۱۷ (targetSdk 37) دسترسی به شبکه‌ی محلی مجوز زمان‌اجرا لازم دارد؛
    // بدون آن ساخت سشن/پیوستن با خطای socket شکست می‌خورد. هر دو مسیر (میزبانی و
    // پیوستن) از همین دروازه رد می‌شوند.
    val lanGate = rememberLocalNetworkGate()

    val players = characters.filter { it.isAlly }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxWidth()) {

            // ───────── سربرگ ─────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LanStatusChip(state, if (role == LanRole.GM) gtr("GM") else if (role == LanRole.PLAYER) gtr("player") else "")
                Spacer(Modifier.weight(1f))
                Text(
                    gtr("LOCAL SESSION"),
                    color = CRed, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            com.cyberpunk.gmtool.ui.components.FaText(
                if (joinOnly) gtr("Connect to the GM's hotspot, then scan the code the GM shows you.")
                else gtr("GM turns on a hotspot; players join from this tab. No internet needed."),
                color = CMuted, fontSize = 10.sp, lineHeight = 17.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            // وقتی مجوز رد شده، باید روشن باشد که چرا سشن وصل نمی‌شود و کاربر
            // کجا می‌تواند دستی روشنش کند (سیستم دیگر دیالوگ نشان نمی‌دهد).
            if (lanGate.denied.value) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(CYellow.copy(alpha = .12f), CutCornerShape(8.dp))
                        .border(1.dp, CYellow.copy(alpha = .5f), CutCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        com.cyberpunk.gmtool.ui.components.FaText(
                            gtr("Local network permission was denied. Android 17+ blocks local network access by default — allow it in Settings → Apps → %1s → Permissions → Nearby devices, then try again.", "GM Kit"),
                            color = CYellow, fontSize = 10.sp, lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        com.cyberpunk.gmtool.ui.components.FaText(
                            gtr("Android 17+ requires the local network permission for the session. If the system dialog never appears, some vendor ROMs (Xiaomi/OPPO/vivo/Huawei) keep a separate “local network” switch in app settings."),
                            color = CMuted, fontSize = 9.sp, lineHeight = 15.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            when (role) {
                // ═════════════ هنوز نقشی انتخاب نشده ═════════════
                LanRole.NONE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!joinOnly) {
                            Button(
                                onClick = { lanGate.run { lan.startHosting(sessionId, sessionName) } },
                                colors = ButtonDefaults.buttonColors(containerColor = CRed),
                                shape = CutCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Wifi, null, tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text(gtr("host session"), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = { showScanner = true },
                            shape = CutCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode, null, tint = CRed)
                            Spacer(Modifier.width(6.dp))
                            Text(gtr("join session"), color = CWhite)
                        }
                    }
                    if (lan.hasSavedTicket()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            scope.launch { lan.tryAutoReconnect(scope, "") }
                        }) {
                            Text(
                                gtr("reconnect as %1s", lan.savedLabel()),
                                color = CRed, fontSize = 11.sp
                            )
                        }
                    }
                }

                // ═════════════ GM ═════════════
                LanRole.GM -> {
                    GmHostPanel(
                        players = players,
                        slots = slots,
                        suspended = suspended,
                        // فاز ۱: GM حاضران را تأیید می‌کند؛ فقط بعد از این
                        // slotها شبکه‌ای می‌شوند و QR صادر می‌شود.
                        onConfirmAttendees = { selected, newCount ->
                            selected.forEach { lan.enableSlot(it.id, it.handle.ifBlank { it.name }) }
                            val newTickets = List(newCount) { lan.newPlayerTicket() }
                            qrTicket = selected.firstOrNull()?.let { lan.ticketFor(it.id) }
                                ?: newTickets.firstOrNull()
                        },
                        onShowQrForSlot = { slot -> qrTicket = lan.ticketForSlot(slot.slotId) },
                        onDisablePlayer = { ch -> lan.disableSlot(ch.id) },
                        onRemoveSlot = { slotId -> lan.removeSlotIfUnused(slotId) },
                        onResetConnection = { ch -> lan.resetSlotConnection(ch.id) },
                        onSuspend = { lan.setSyncSuspended(it) },
                        onStop = { lan.stopHosting() }
                    )
                    if (pending.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        PendingRequestBar(
                            requests = pending,
                            onApprove = { lan.approve(it) },
                            onReject = { lan.reject(it) },
                            onApproveAll = { lan.approveAll() }
                        )
                    }
                }

                // ═════════════ بازیکن ═════════════
                LanRole.PLAYER -> {
                    val me = characters.firstOrNull { it.id == myId }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CCard),
                        shape = CutCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                me?.handle?.ifBlank { me.name } ?: gtr("waiting for GM"),
                                color = CWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                gtr("Your sheet updates automatically. Changes you make need GM approval."),
                                color = CMuted, fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { lan.leaveSession(scope) },
                                shape = CutCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CMuted),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.WifiOff, null, tint = CMuted)
                                Spacer(Modifier.width(6.dp))
                                Text(gtr("leave session"), color = CMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    // ───────── دیالوگ QR ─────────
    qrTicket?.let { t ->
        AlertDialog(
            onDismissRequest = { qrTicket = null },
            containerColor = CCard,
            shape = CutCornerShape(12.dp),
            title = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(t.label, color = CRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QrImage(LanTicket.encode(t))
                    Spacer(Modifier.height(10.dp))
                    Text(t.code, color = CWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${t.host}:${t.port}", color = CMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            gtr("The player scans this from Session tab, Join."),
                            color = CMuted, fontSize = 10.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { qrTicket = null }) { Text(gtr("close"), color = CRed) }
            }
        )
    }

    // ───────── اسکنر ─────────
    if (showScanner) {
        QrScannerDialog(
            onResult = { raw ->
                showScanner = false
                LanTicket.decode(raw)?.let { confirmIdentity = it }
            },
            onManualEntry = { showScanner = false; manualCode = "" },
            onDismiss = { showScanner = false }
        )
    }

    // ───────── ورود دستی کد ─────────
    manualCode?.let { current ->
        var host by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { manualCode = null },
            containerColor = CCard,
            shape = CutCornerShape(12.dp),
            title = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(gtr("enter code manually"), color = CRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = host, onValueChange = { host = it },
                        label = { Text(gtr("GM address"), color = CMuted) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = code, onValueChange = { code = it.uppercase() },
                        label = { Text(gtr("code"), color = CMuted) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    manualCode = null
                    // در حالت دستی، نقش را از خود سرور نمی‌دانیم؛
                    // کد را می‌فرستیم و سرور تصمیم می‌گیرد.
                    confirmIdentity = LanTicket(
                        host = host.trim(), sessionId = 0L,
                        slotId = "", code = code.trim(), label = ""
                    )
                }) { Text(gtr("connect"), color = CRed) }
            },
            dismissButton = {
                TextButton(onClick = { manualCode = null }) { Text(gtr("cancel"), color = CMuted) }
            }
        )
    }

    // ───────── تأیید هویت پس از اسکن ─────────
    confirmIdentity?.let { t ->
        val isNew = LanTicket.isNewPlayerSlot(t.slotId)
        AlertDialog(
            onDismissRequest = { confirmIdentity = null },
            containerColor = CCard,
            shape = CutCornerShape(12.dp),
            title = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        if (isNew) gtr("join as new player") else gtr("Is this you?"),
                        color = CRed, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            },
            text = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column {
                        Text(
                            if (isNew) gtr("Pick which character to send to the GM.")
                            else t.label,
                            color = CWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                        )
                        if (!isNew) {
                            Spacer(Modifier.height(6.dp))
                            // سه حالت ممکن: برگه را دارم (انتخاب از فهرست)،
                            // برگه را ندارم (GM می‌فرستد)، بی‌خیال.
                            Text(
                                gtr("Have this sheet? Pick it from your list so the GM compares both copies. Don't have it? The GM will send it to you."),
                                color = CMuted, fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmIdentity = null
                    if (isNew) pickCharacter = t
                    else lanGate.run { scope.launch { viewModel.lan.joinWithTicket(t, scope, null, "") } }
                }) {
                    Text(
                        if (isNew) gtr("choose")
                        else gtr("I don't have it — GM sends it"),
                        color = CRed
                    )
                }
            },
            dismissButton = {
                Row {
                    if (!isNew) {
                        TextButton(onClick = {
                            confirmIdentity = null
                            pickCharacter = t
                        }) { Text(gtr("I have this sheet"), color = CYellow) }
                    }
                    TextButton(onClick = { confirmIdentity = null }) { Text(gtr("cancel"), color = CMuted) }
                }
            }
        )
    }

    // ───────── انتخاب شخصیت برای «بازیکن جدید» ─────────
    pickCharacter?.let { t ->
        AlertDialog(
            onDismissRequest = { pickCharacter = null },
            containerColor = CCard,
            shape = CutCornerShape(12.dp),
            title = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(gtr("which character?"), color = CRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column {
                        if (players.isEmpty()) {
                            Text(gtr("You have no character yet."), color = CMuted, fontSize = 12.sp)
                        }
                        players.forEach { ch ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(CDim, CutCornerShape(7.dp))
                                    .clickable {
                                        lanGate.run {
                                            pickCharacter = null
                                            scope.launch {
                                                viewModel.lan.joinWithTicket(t, scope, ch, ch.name, ch.id)
                                            }
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null, tint = CRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ch.handle.ifBlank { ch.name }, color = CWhite,
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                                    )
                                    Text(
                                        "${ch.role} • Rank ${ch.roleRank}", color = CMuted, fontSize = 10.sp,
                                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickCharacter = null }) { Text(gtr("cancel"), color = CMuted) }
            }
        )
    }
}

// ───────────────────────── پنل GM ─────────────────────────

@Composable
private fun GmHostPanel(
    players: List<Character>,
    slots: List<com.cyberpunk.gmtool.data.net.LanSlot>,
    suspended: Boolean,
    /** تأیید فهرست حاضران: شخصیت‌های تیک‌خورده + تعداد «بازیکن جدید». */
    onConfirmAttendees: (List<Character>, Int) -> Unit,
    onShowQrForSlot: (com.cyberpunk.gmtool.data.net.LanSlot) -> Unit,
    onDisablePlayer: (Character) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onResetConnection: (Character) -> Unit,
    onSuspend: (Boolean) -> Unit,
    onStop: () -> Unit
) {
    // تا GM فهرست حاضران را تأیید نکند هیچ QR صادر نمی‌شود: صفحه‌ی سشن
    // نباید از همان اول فهرستِ همه‌ی شخصیت‌ها را به شکل slot نشان دهد.
    val networkedSlots = slots.filter { it.networked }
    var checked by remember { mutableStateOf(setOf<Int>()) }
    var newCount by remember { mutableStateOf(0) }
    var showAddMore by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        // ═══ فاز ۱: انتخاب حاضران (اول جلسه، یا «افزودن» وسط جلسه) ═══
        if (networkedSlots.isEmpty() || showAddMore) {
            val isAdding = networkedSlots.isNotEmpty()
            Text(
                if (isAdding) gtr("Add more attendees or new players.")
                else gtr("First pick who sits at this table, then confirm — QR codes appear after that."),
                color = CMuted, fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
            )
            Spacer(Modifier.height(8.dp))

            players.forEach { ch ->
                val isChecked = ch.id in checked
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isChecked) CDim else Color.Transparent, CutCornerShape(7.dp))
                        .clickable {
                            checked = if (isChecked) checked - ch.id else checked + ch.id
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { on ->
                            checked = if (on) checked + ch.id else checked - ch.id
                        },
                        colors = CheckboxDefaults.colors(checkedColor = CRed, checkmarkColor = CWhite)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            ch.handle.ifBlank { ch.name }, color = CWhite,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                        )
                        Text(
                            "${ch.role} • Rank ${ch.roleRank}", color = CMuted, fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            // «بازیکن جدید» بدون سقف — تعداد آدم‌های سر میز به برنامه ربطی ندارد.
            repeat(newCount) { i ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(CDim, CutCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { newCount = newCount - 1 }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, gtr("remove"), tint = CMuted, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        gtr("new player %1s", i + 1), color = CYellow,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Right
                    )
                }
            }
            OutlinedButton(
                onClick = { newCount++ },
                shape = CutCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, null, tint = CRed)
                Spacer(Modifier.width(6.dp))
                Text(gtr("new player"), color = CWhite)
            }

            Spacer(Modifier.height(8.dp))
            val total = checked.size + newCount
            Button(
                onClick = {
                    val selected = players.filter { it.id in checked }
                    onConfirmAttendees(selected, newCount)
                    checked = setOf(); newCount = 0; showAddMore = false
                },
                enabled = total > 0,
                shape = CutCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CRed, disabledContainerColor = CDim),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isAdding) gtr("add %1s to session", total) else gtr("confirm %1s attendees", total),
                    color = CWhite, fontWeight = FontWeight.Bold
                )
            }
            if (isAdding) {
                TextButton(
                    onClick = { showAddMore = false; checked = setOf(); newCount = 0 },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(gtr("cancel"), color = CMuted) }
            }
        }

        // ═══ فاز ۲: میز — فقط slotهایی که واقعاً شبکه‌ای شده‌اند ═══
        if (networkedSlots.isNotEmpty()) {
            if (!showAddMore) {
                Spacer(Modifier.height(10.dp))
                Text(
                    gtr("Table"), color = CRed, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
                Spacer(Modifier.height(6.dp))
            }
            val playerById = players.associateBy { it.id }
            networkedSlots.forEach { slot ->
                val isNewSlot = LanTicket.isNewPlayerSlot(slot.slotId)
                val ch = if (isNewSlot) null else playerById[slot.characterId]
                Row(
                    Modifier.fillMaxWidth()
                        .background(CDim, CutCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR همیشه در دسترس است، نه فقط لحظه‌ی ساخت slot
                    IconButton(onClick = { onShowQrForSlot(slot) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.QrCode, gtr("show QR"), tint = CRed)
                    }
                    // آزاد کردن نقش: گوشی بازیکن مرده یا عوض شده و بدون خداحافظی رفته.
                    if (slot.connected && ch != null) {
                        IconButton(onClick = { onResetConnection(ch) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.LinkOff, gtr("free this role"), tint = CMuted)
                        }
                    }
                    // حذف فقط وقتی کسی وصل نشده؛ برگه‌ی فعال وسط جلسه
                    // نباید زیر پای بازیکن غیب شود.
                    if (!slot.connected) {
                        IconButton(
                            onClick = {
                                if (isNewSlot) onRemoveSlot(slot.slotId)
                                else ch?.let(onDisablePlayer)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, gtr("remove"), tint = CMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            ch?.handle?.ifBlank { ch.name } ?: slot.label,
                            color = if (isNewSlot) CYellow else CWhite,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                        )
                        Text(
                            if (slot.connected) gtr("connected") else gtr("waiting for scan"),
                            color = if (slot.connected) CGreen else CMuted, fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                        )
                    }
                    Icon(
                        if (slot.connected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        null,
                        tint = if (slot.connected) CGreen else CMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
            }

            if (!showAddMore) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { showAddMore = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PersonAdd, null, tint = CRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(gtr("add attendee"), color = CRed)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth()
                .background(CDim, CutCornerShape(7.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = suspended, onCheckedChange = onSuspend,
                colors = SwitchDefaults.colors(checkedThumbColor = CRed)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    gtr("suspend sync"), color = CWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
                Text(
                    gtr("Changes are saved but not sent until you release."),
                    color = CMuted, fontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onStop,
            shape = CutCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CMuted),
            modifier = Modifier.fillMaxWidth()
        ) { Text(gtr("close local session"), color = CMuted) }

        Spacer(Modifier.height(6.dp))
        Text(
            gtr("Hotspot plus server drains the battery. Keep a power bank for long games."),
            color = CMuted, fontSize = 9.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
        )
    }
}
