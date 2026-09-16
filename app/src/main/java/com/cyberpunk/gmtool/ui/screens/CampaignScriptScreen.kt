package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Campaign
import com.cyberpunk.gmtool.data.CampaignScriptData
import com.cyberpunk.gmtool.data.SettingsRepository
import com.cyberpunk.gmtool.data.SessionScript

private val SRed = Color(0xFFD32F2F); private val SWhite = Color(0xFFE0E0E0)
private val SMuted = Color(0xFF9E9E9E); private val SCard = Color(0xFF1A1A1A)
private val SField = Color(0xFF242424); private val SGold = Color(0xFFFFC107)

/**
 * کتابِ کمپین آماده‌ی «صفرها» — GM-ONLY.
 * ورود فقط با رمز (PIN)؛ دفعه‌ی اول رمز جدید تنظیم می‌شود.
 */
@Composable
fun CampaignScriptScreen(onBack: () -> Unit, onInstantiate: (Campaign) -> Unit, requirePin: Boolean = true) {
    var unlocked by remember { mutableStateOf(!requirePin) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (!unlocked) GmPinGate(onUnlocked = { unlocked = true }, onBack = onBack)
        else ScriptBrowser(onBack = onBack, onInstantiate = onInstantiate)
    }
}

@Composable
fun GmPinGate(onUnlocked: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context.applicationContext) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, null, tint = SRed, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(10.dp))
        Text("کتاب کمپین — GM ONLY", color = SWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("رمز GM را وارد کن", color = SMuted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = pin, onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) { pin = it; error = null } },
            label = { Text("رمز") }, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = darkScriptFields()
        )
        error?.let { Spacer(Modifier.height(6.dp)); Text(it, color = SRed, fontSize = 12.sp) }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                when {
                    pin.length < 4 -> error = "رمز حداقل ۴ رقم است"
                    settings.checkPin(pin) -> onUnlocked()
                    else -> { error = "رمز درست نیست"; pin = "" }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SRed),
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = CutCornerShape(10.dp)
        ) { Text("ورود", color = Color.Black, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("بازگشت", color = SMuted) }
        Spacer(Modifier.height(16.dp))
        Text(
            "⚠ این بخش اسپویل کامل کمپین را دارد و فقط برای GM است.",
            color = SMuted, fontSize = 10.sp, textAlign = TextAlign.Center
        )
    }
}

/** تغییر رمز روی همین دستگاه: نیاز به رمز فعلی دارد. */
@Composable
private fun ChangePinDialog(settings: SettingsRepository, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SCard,
        title = { Text("تغییر رمز GM", color = SWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (settings.usesDefaultPin) Text("الان رمز پیش‌فرض فعال است؛ با تعیین رمز جدید فقط همین دستگاه عوض می‌شود.", color = SMuted, fontSize = 11.sp)
                OutlinedTextField(current, { if (it.length <= 8 && it.all(Char::isDigit)) current = it }, label = { Text("رمز فعلی") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, colors = darkScriptFields())
                OutlinedTextField(newPin, { if (it.length <= 8 && it.all(Char::isDigit)) newPin = it }, label = { Text("رمز جدید (۴-۸ رقم)") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, colors = darkScriptFields())
                OutlinedTextField(confirm, { if (it.length <= 8 && it.all(Char::isDigit)) confirm = it }, label = { Text("تکرار رمز جدید") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, colors = darkScriptFields())
                error?.let { Text(it, color = SRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    !settings.checkPin(current) -> error = "رمز فعلی درست نیست"
                    newPin.length < 4 -> error = "رمز جدید حداقل ۴ رقم است"
                    newPin != confirm -> error = "تکرار رمز یکسان نیست"
                    else -> { settings.gmPinHash = SettingsRepository.sha256(newPin); onDismiss() }
                }
            }) { Text("ذخیره", color = SRed, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = SMuted) } }
    )
}

@Composable
private fun ScriptBrowser(onBack: () -> Unit, onInstantiate: (Campaign) -> Unit) {
    var selected by remember { mutableStateOf<SessionScript?>(null) }
    var showChangePin by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context.applicationContext) }
    val s = selected
    if (s == null) {
        val grouped = CampaignScriptData.sessions.groupBy { it.arc }
        LazyColumn(
            Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SRed) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("کتاب کمپین: صفرها", color = SWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text("۱۵ جلسه • سه پرده • مپ‌ها از کتابخانه‌ی Battle Map", color = SMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                    IconButton(onClick = { showChangePin = true }) { Icon(Icons.Default.Password, null, tint = SMuted) }
                }
            }
            item {
                ScriptCard("راهنمای اجرای کمپین — قبل از Session 1", Icons.Default.Shield) {
                    Text("این نسخه برای اجرای طولانی با Core و گروه ۵ نفره‌ی شما بازطراحی شده است. نکات زیر قانون تازه نیستند؛ راهنمای GM هستند.", color = SWhite, fontSize = 12.sp, lineHeight = 20.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    CampaignScriptData.gmPrinciples.forEach { Text("• $it", color = SMuted, fontSize = 11.sp, lineHeight = 19.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) }
                    Spacer(Modifier.height(8.dp))
                    Text("فرصت درخشش هر نقش", color = SRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    CampaignScriptData.roleSpotlights.forEach { Text("• $it", color = SWhite, fontSize = 11.sp, lineHeight = 19.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) }
                }
            }
            item {
                Button(
                    onClick = { onInstantiate(CampaignScriptData.buildCampaign()) },
                    colors = ButtonDefaults.buttonColors(containerColor = SGold),
                    shape = CutCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("افزودن این کمپین به Campaign Manager", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
            grouped.forEach { (arc, list) ->
                item {
                    Text(
                        arc, color = SRed, fontWeight = FontWeight.Black, fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), textAlign = TextAlign.Right
                    )
                }
                items(list, key = { it.number }) { session ->
                    SessionRow(session) { selected = session }
                }
            }
        }
    } else SessionDetail(s, onBack = { selected = null })
    if (showChangePin) ChangePinDialog(settings, onDismiss = { showChangePin = false })
}

@Composable
private fun SessionRow(s: SessionScript, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SCard), shape = CutCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).border(1.dp, SRed.copy(.4f), CutCornerShape(10.dp))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(SRed.copy(.15f), CutCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("${s.number}", color = SRed, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(gtr(s.title), color = SWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Text(gtr("%1s • IP %2s • %3s eb • REP +%4s", s.threat, s.ip, s.eb, s.rep), color = SMuted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = SMuted)
        }
    }
}

@Composable
private fun SessionDetail(s: SessionScript, onBack: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 60.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = SRed) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("جلسه ${s.number} — ${s.title}", color = SWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Text(gtr(s.arc), color = SRed, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
            }
        }
        item { ScriptCard("خلاصه و پاداش", Icons.Default.Info) {
            Text("Threat راهنمای شدت صحنه است؛ IP/eb/REP را بعد از نتیجه واقعی Session بده، نه صرفاً چون Beat نوشته‌شده تمام شد.", color = SMuted, fontSize = 10.sp, lineHeight = 17.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(gtr(s.synopsis), color = SWhite, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Stat(gtr("Threat"), s.threat, Modifier.weight(1f)); Stat("IP", "${s.ip}", Modifier.weight(1f))
                Stat("eb", "${s.eb}", Modifier.weight(1f)); Stat("REP", "+${s.rep}", Modifier.weight(1f))
            }
        } }
        item { ScriptCard("ورود بازیکنان", Icons.Default.Login) {
            Text("این متن نقطه‌ی شروع پیشنهادی است. اگر Session قبلی نتیجه متفاوتی داشت، ورودی را با همان Consequence بازنویسی کن.", color = SMuted, fontSize = 10.sp, lineHeight = 17.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(gtr(s.entry), color = SWhite, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        } }
        item { ScriptCard("بریفینگ فیکسر (لجر)", Icons.Default.RecordVoiceOver) {
            Text("دیالوگ پیشنهادی است، نه متن اجباری. اطلاعات کلیدی را منتقل کن و بگذار بازیکن‌ها سؤال و مذاکره کنند.", color = SMuted, fontSize = 10.sp, lineHeight = 17.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(gtr(s.fixerBriefing), color = SGold.copy(.9f), fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        } }
        item { Text("صحنه‌ها و مپ‌ها", color = SRed, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Right) }
        items(s.scenes, key = { it.title + it.mapName }) { sc ->
            Card(colors = CardDefaults.cardColors(containerColor = SCard), shape = CutCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.End) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        sc.mapId?.let {
                            Row(Modifier.background(Color(0xFF0D0D0D), CutCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Map, null, tint = SRed, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(gtr(sc.mapName), color = SRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(gtr(sc.title), color = SWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 8.dp), textAlign = TextAlign.Right)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(gtr(sc.text), color = SWhite, fontSize = 12.sp, lineHeight = 20.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    if (sc.gmNotes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(gtr("GM: %1s", sc.gmNotes), color = SMuted, fontSize = 11.sp, lineHeight = 18.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth().background(SField, CutCornerShape(6.dp)).padding(8.dp))
                    }
                }
            }
        }
        item { ScriptCard("آب‌وهوا", Icons.Default.Cloud) {
            Text("آب‌وهوا پیش‌فرضاً فضاسازی است. فقط وقتی Task مشخصی واقعاً سخت‌تر می‌شود، طبق Core DV/شرایط صحنه را تغییر بده؛ Modifier عمومی تازه نساز.", color = SMuted, fontSize = 10.sp, lineHeight = 17.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(gtr(s.weather), color = SWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(gtr(s.weatherEffect), color = SMuted, fontSize = 12.sp, lineHeight = 20.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        } }
        item { ScriptCard("جستجو و لوت", Icons.Default.Search) {
            Text("سرنخ ضروری را پشت یک Roll واحد قفل نکن. Roll بهتر باید کیفیت/سرعت/جزئیات بیشتری بدهد؛ شکست مسیر داستان را نبندد.", color = SMuted, fontSize = 10.sp, lineHeight = 17.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            s.searches.forEach { srch ->
                Text(gtr("%1s — DV %2s", srch.area, srch.dv), color = SWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Right)
                SearchTier("موفق", srch.base); SearchTier("خوب (+۳)", srch.good); SearchTier("عالی (10 طبیعی / +۶)", srch.great)
            }
            if (s.loot.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("غنیمت‌های اصلی:", color = SRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                s.loot.forEach { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = SGold, modifier = Modifier.size(12.dp)); Text(it, color = SWhite, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp), textAlign = TextAlign.Right) } }
            }
        } }
        item { ScriptCard("ساعت‌ها و پیامدها", Icons.Default.Schedule) { Text("Clockها ابزار GM برای نشان دادن پیشرفت خطر/رابطه‌اند و STAT نیستند. فقط وقتی Fiction جلو می‌رود Segment اضافه کن.", color = SMuted, fontSize = 10.sp, lineHeight = 17.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(6.dp)); Text(gtr(s.clocks), color = SWhite, fontSize = 12.sp, lineHeight = 20.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) } }
        item { ScriptCard("لایه‌ی بقا", Icons.Default.BatteryAlert) { Text("این بخش اکنون فقط فشار اقتصادی/داستانی سازگار با Core را پیشنهاد می‌دهد؛ فرسودگی تصادفی و Modifierهای عمومی House Rule حذف شده‌اند.", color = SMuted, fontSize = 10.sp, lineHeight = 17.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(6.dp)); Text(gtr(s.survival), color = SMuted, fontSize = 12.sp, lineHeight = 20.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) } }
        item { ScriptCard("چک‌لیست اپ (بعد از جلسه)", Icons.Default.Checklist) {
            s.checklist.forEach { Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.CheckCircle, null, tint = SRed, modifier = Modifier.size(14.dp).padding(top = 3.dp)); Text(it, color = SWhite, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp).weight(1f), textAlign = TextAlign.Right) } }
        } }
    }
}

@Composable
private fun ScriptCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SCard), shape = CutCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = SRed, modifier = Modifier.size(16.dp)); Text(gtr(title), color = SRed, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp)) }
            Spacer(Modifier.height(8.dp)); content()
        }
    }
}

@Composable
private fun SearchTier(label: String, text: String) {
    if (text.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(gtr(text), color = SWhite, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
        Text(gtr(label), color = SRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(SField, CutCornerShape(7.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = SRed, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(gtr(label), color = SMuted, fontSize = 9.sp)
    }
}

@Composable
private fun darkScriptFields() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SWhite, unfocusedTextColor = SWhite, focusedBorderColor = SRed,
    unfocusedBorderColor = SMuted, focusedLabelColor = SRed, unfocusedLabelColor = SMuted,
    cursorColor = SRed
)
