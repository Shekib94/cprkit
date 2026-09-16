package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.ImageProvider
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.ui.components.CoreItemDetailDialog

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)
private val CardBg = Color(0xFF1A1A1A)

private enum class PhoneApp {
    HOME, CONTACTS, NOTES, OBJECTIVES,
    DATA, DATA_DETAIL, GOODS, GOODS_DETAIL, MAP
}

// مباحث Data Pool
private enum class DataTopic(val titleFa: String, val titleEn: String) {
    CRITICAL("جراحات بحرانی", "Critical Injuries"),
    CORPS("کمپانی‌ها", "Corporations"),
    FACTIONS("سازمان‌ها", "Factions"),
    GANGS("گنگ‌های نایت‌سیتی", "Gangs"),
    CURRENCY("واحد پول", "Currencies"),
    DEATH("تست مرگ و وضعیت‌های زخم", "Death Save & Wounds")
}

// مباحث Goods & Services
private enum class GoodsTopic(val titleFa: String, val titleEn: String) {
    DRUGS("داروها و مواد", "Drugs"),
    FOOD("غذا و لایف‌استایل", "Food & Lifestyle"),
    HOUSING("مسکن", "Housing"),
    JOBS("کارها", "Jobs"),
    SERVICES("خدمات", "Services"),
    STORE("فروشگاه", "Store"),
    THERAPY("روان‌درمانی", "Therapy")
}

/** پیچاندن یک قطعه‌ی انگلیسی/عددی در جداکننده‌ی دوجهته تا متن فارسی بهم نریزد */
private const val LRI = "\u2066" // Left-to-Right Isolate
private const val PDI = "\u2069" // Pop Directional Isolate
private fun ltr(s: String): String = LRI + s + PDI

@Composable
fun AgentTab(character: Character, viewModel: CharacterViewModel) {
    var app by remember { mutableStateOf(PhoneApp.HOME) }
    var dataTopic by remember { mutableStateOf(DataTopic.CRITICAL) }
    var goodsTopic by remember { mutableStateOf(GoodsTopic.DRUGS) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column(Modifier.fillMaxSize().background(Black)) {
        // نوار وضعیت گوشی
        Row(
            Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app != PhoneApp.HOME) {
                val backTarget = when (app) {
                    PhoneApp.DATA_DETAIL -> PhoneApp.DATA
                    PhoneApp.GOODS_DETAIL -> PhoneApp.GOODS
                    else -> PhoneApp.HOME
                }
                IconButton(onClick = { app = backTarget }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = gtr("Back"), tint = Red)
                }
            }
            // وسط نوار: اسم و لقب کاراکتر (به‌جای AGENT OS)
            Column(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        character.name.ifBlank { character.handle.ifBlank { "Edgerunner" } },
                        color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text("@${character.handle.ifBlank { "edgerunner" }}", color = Red, fontSize = 11.sp, maxLines = 1)
                }
            }
            // راست نوار: پول (به‌جای ENCRYPTED)
            Box(
                Modifier.background(Red.copy(alpha = 0.15f), CutCornerShape(4.dp))
                    .border(1.dp, Red, CutCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text("€\$ ${character.eurodollars}", color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        when (app) {
            PhoneApp.HOME -> PhoneHome(
                character = character,
                onOpen = { app = it }
            )
            PhoneApp.CONTACTS -> ContactsApp(character, viewModel)
            PhoneApp.NOTES -> NotesApp(character, viewModel)
            PhoneApp.OBJECTIVES -> ObjectivesApp(character, viewModel)
            PhoneApp.MAP -> MapApp()
            PhoneApp.DATA -> DataPoolApp(onOpen = { topic -> dataTopic = topic; app = PhoneApp.DATA_DETAIL })
            PhoneApp.DATA_DETAIL -> DataTopicDetail(dataTopic)
            PhoneApp.GOODS -> GoodsHub(onOpen = { topic -> goodsTopic = topic; app = PhoneApp.GOODS_DETAIL })
            PhoneApp.GOODS_DETAIL -> GoodsTopicDetail(goodsTopic)
        }
    }
    }
}

@Composable
private fun PhoneHome(
    character: Character,
    onOpen: (PhoneApp) -> Unit
) {
    // بازیکن: تصویر قدی نقش. NPC: پرتره‌ی اختصاصی همان NPC، نه تصویر قدی نقش Solo.
    val isNpc = character.npcCategory.isNotBlank()
    val wallpaper = if (isNpc) {
        ImageProvider.getNpcImage(character.npcCategory, character.role, character.name)
    } else {
        ImageProvider.getRoleImage(character.role, isPortrait = false)
    }
    // ۴ آیکون داک پایین
    val apps = listOf<@Composable () -> Unit>(
        { HomeIcon(Icons.Default.Contacts, gtr("CONTACTS"), "مخاطبین") { onOpen(PhoneApp.CONTACTS) } },
        { HomeIcon(Icons.Default.Info, gtr("DATA POOL"), "دیتاپول") { onOpen(PhoneApp.DATA) } },
        { HomeIcon(Icons.Default.ShoppingCart, gtr("GOODS"), "کالا و خدمات") { onOpen(PhoneApp.GOODS) } },
        { HomeIcon(Icons.Default.Map, gtr("MAP"), "نقشه") { onOpen(PhoneApp.MAP) } },
        { SideHomeIcon(Icons.Default.EditNote, gtr("NOTES"), "یادداشت") { onOpen(PhoneApp.NOTES) } },
        { SideHomeIcon(Icons.Default.Checklist, gtr("OBJECTIVES"), "اهداف") { onOpen(PhoneApp.OBJECTIVES) } }
    )

    // چیدمان: ۴ آیکون پایین، ۲ آیکون دو طرفِ میانی — تا تصویر قدی بهتر دیده شود
    val bottomApps = apps.subList(0, 4) // CONTACTS, DATA POOL, GOODS, MAP
    val sideApps = apps.subList(4, 6)   // NOTES, OBJECTIVES

    Box(Modifier.fillMaxSize()) {
        // والپیپر کاراکتر؛ برای NPC از پرتره‌ی اختصاصی استفاده می‌شود
        Image(
            painter = painterResource(id = wallpaper),
            contentDescription = gtr("Phone wallpaper"),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.6f)
        )
        // گرادیان تیره ملایم فقط در لبه‌ها برای خوانایی
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.8f)))
            )
        )

        // اهداف و یادداشت درست بالای داک پایین؛ تصویر کاراکتر در مرکز آزاد می‌ماند.
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 34.dp).padding(bottom = 112.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Box(Modifier.size(84.dp)) { sideApps[1]() } // OBJECTIVES
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(84.dp)) { sideApps[0]() } // NOTES
        }

        // داک پایین: ۴ آیکون
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bottomApps.forEach { app ->
                Box(Modifier.weight(1f)) { app() }
            }
        }
    }
}

// آیکون‌های داک پایین (بزرگ‌تر، فقط آیکن + برچسب انگلیسی)
@Composable
private fun HomeIcon(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(58.dp)
                .background(Color.Black.copy(alpha = 0.6f), CutCornerShape(16.dp))
                .border(1.5.dp, Red.copy(alpha = 0.9f), CutCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = title, tint = White, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(gtr(title), color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// آیکون‌های بالای داک (برچسب انگلیسی برای یکدستی Agent)
@Composable
private fun SideHomeIcon(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(60.dp)
                .background(Red.copy(alpha = 0.25f), CutCornerShape(18.dp))
                .border(1.5.dp, Red, CutCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = title, tint = White, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(gtr(title), color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ================= مخاطبین =================
@Composable
private fun ContactsApp(character: Character, viewModel: CharacterViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { AppHeader("CONTACTS / مخاطبین") }
        items(character.contacts.size) { i ->
            val c = character.contacts[i]
            ListRow(c.name, c.detail) { viewModel.deleteContact(character.id, c.id) }
        }
        item {
            Spacer(Modifier.height(12.dp))
            ActionButton("افزودن مخاطب") { showAdd = true }
            Spacer(Modifier.height(80.dp))
        }
    }
    if (showAdd) TwoFieldDialog("مخاطب جدید", "نام", "توضیحات/شغل",
        onDismiss = { showAdd = false }) { n, d ->
        viewModel.addContact(character.id, n, d); showAdd = false
    }
}

// ================= نوت‌ها =================
@Composable
private fun NotesApp(character: Character, viewModel: CharacterViewModel) {
    var editing by remember { mutableStateOf<com.cyberpunk.gmtool.data.PhoneNote?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { AppHeader("NOTES / یادداشت‌ها") }
        items(character.phoneNotes.size) { i ->
            val n = character.phoneNotes[i]
            ListRow(n.title.ifBlank { "(بدون عنوان)" }, n.body) { viewModel.deleteNote(character.id, n.id) }
        }
        item {
            Spacer(Modifier.height(12.dp))
            ActionButton("یادداشت جدید") { showAdd = true }
            Spacer(Modifier.height(80.dp))
        }
    }
    if (showAdd) TwoFieldDialog("یادداشت جدید", "عنوان", "متن",
        onDismiss = { showAdd = false }) { t, b ->
        viewModel.addNote(character.id, t, b); showAdd = false
    }
}

// ================= آبجکتیوها =================
@Composable
private fun ObjectivesApp(character: Character, viewModel: CharacterViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    val types = listOf("Task", "Gig", "Bounty", "Personal")
    var type by remember { mutableStateOf(types[0]) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            AppHeader("OBJECTIVES / اهداف")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { t ->
                    FilterChip(
                        selected = type == t, onClick = { type = t },
                        label = { Text(gtr(t), color = if (type == t) Black else White, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red, containerColor = CardBg)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(character.objectives.size) { i ->
            val o = character.objectives[i]
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.toggleObjective(character.id, o.id) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = o.done, onCheckedChange = { viewModel.toggleObjective(character.id, o.id) },
                    colors = CheckboxDefaults.colors(checkedColor = Red))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.weight(1f)) {
                        Text(gtr(o.title), color = if (o.done) Muted else White, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text("${o.type} • ${o.body}", color = Muted, fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                }
                IconButton(onClick = { viewModel.deleteObjective(character.id, o.id) }) {
                    Icon(Icons.Default.Delete, null, tint = Red)
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            ActionButton("هدف جدید") { showAdd = true }
            Spacer(Modifier.height(80.dp))
        }
    }
    if (showAdd) TwoFieldDialog("هدف جدید ($type)", "عنوان هدف", "جزئیات",
        onDismiss = { showAdd = false }) { t, b ->
        viewModel.addObjective(character.id, t, type, b); showAdd = false
    }
}

// ================= دیتاپول (فهرست مرجع) =================
@Composable
private fun DataPoolApp(onOpen: (DataTopic) -> Unit) {
    val topics = listOf(
        DataTopic.CRITICAL to "۲۲ جراحت بحرانی برای سر و بدن، به‌همراه اثر، جریمه و درمان.",
        DataTopic.CORPS to "مگاکورپ‌هایی که نایت‌سیتی و جهان را می‌گردانند.",
        DataTopic.FACTIONS to "سازمان‌های غیرشرکتی قدرتمند (پلیس، آمبولانس زرهی، دیده‌بان شبکه).",
        DataTopic.GANGS to "گنگ‌های اصلی نایت‌سیتی و منطقه‌ی نفوذشان.",
        DataTopic.CURRENCY to "یورودلار، واحد پول رسمیت‌یافته و اقتصاد شبک‌محور.",
        DataTopic.DEATH to "تست مرگ (Death Save) و وضعیت‌های زخم (Wound States)."
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { AppHeader("DATA POOL / مرجع سریع") }
        items(topics.size) { i ->
            val (topic, desc) = topics[i]
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpen(topic) }
                    .background(CardBg, CutCornerShape(8.dp)).border(1.dp, Red.copy(alpha = 0.3f), CutCornerShape(8.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr(topic.titleEn), color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                    Text(topic.titleFa, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(Modifier.height(3.dp))
                    Text(gtr(desc), color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Red)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ================= صفحه‌ی جزئیات هر مبحث =================
@Composable
private fun DataTopicDetail(topic: DataTopic) {
    // جراحات بحرانی دقیقاً همان منوی GM Tools را استفاده می‌کند تا دو منبع UI جدا نداشته باشیم.
    if (topic == DataTopic.CRITICAL) {
        InjuriesList()
        return
    }

    var detail by remember { mutableStateOf<Pair<String, String>?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            AppHeader("${topic.titleEn} / ${topic.titleFa}")
            Text("برای دیدن توضیحات روی هر گزینه بزن.", color = Muted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Right)
        }
        when (topic) {
            DataTopic.CORPS -> items(corpData.size) { i ->
                val (en, fa, desc) = corpData[i]
                ReferenceButton(primary = fa, secondary = en) { detail = "$fa / $en" to desc }
            }
            DataTopic.FACTIONS -> items(factionData.size) { i ->
                val (en, fa, desc) = factionData[i]
                ReferenceButton(primary = fa, secondary = en) { detail = "$fa / $en" to desc }
            }
            DataTopic.GANGS -> items(gangData.size) { i ->
                val (en, fa, desc) = gangData[i]
                ReferenceButton(primary = fa, secondary = en) { detail = "$fa / $en" to desc }
            }
            DataTopic.CURRENCY -> items(currencyData.size) { i ->
                val (en, fa, desc) = currencyData[i]
                ReferenceButton(primary = fa, secondary = en) { detail = "$fa / $en" to desc }
            }
            DataTopic.DEATH -> {
                item { SectionTitle("وضعیت‌های زخم / Wound States") }
                items(woundData.size) { i ->
                    val (en, fa, desc) = woundData[i]
                    ReferenceButton(primary = fa, secondary = en) { detail = "$fa / $en" to desc }
                }
                item { SectionTitle("تست مرگ / Death Save") }
                items(deathData.size) { i ->
                    val (name, desc) = deathData[i]
                    ReferenceButton(primary = name, secondary = "Death Save") { detail = name to desc }
                }
            }
            DataTopic.CRITICAL -> Unit
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    detail?.let { (title, body) -> ReferenceDetailDialog(title, body) { detail = null } }
}

@Composable
private fun ReferenceButton(primary: String, secondary: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick() }
            .background(CardBg, CutCornerShape(8.dp)).border(1.dp, Red.copy(.35f), CutCornerShape(8.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            com.cyberpunk.gmtool.ui.components.FaText(gtr(primary), color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth())
            if (secondary.isNotBlank()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(secondary), color = Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Red)
    }
}

@Composable
private fun ReferenceDetailDialog(title: String, body: String, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(12.dp))) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.padding(18.dp)) {
                    com.cyberpunk.gmtool.ui.components.FaText(gtr(title), color = Red, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    com.cyberpunk.gmtool.ui.components.FaText(gtr(body), color = White, fontSize = 14.sp, lineHeight = 23.sp,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Red),
                        modifier = Modifier.fillMaxWidth(), shape = CutCornerShape(8.dp)) {
                        Text("بستن", color = Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(gtr(t), color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
            textAlign = TextAlign.Right)
    }
}

@Composable
private fun InfoNote(t: String) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .background(Red.copy(alpha = 0.12f), CutCornerShape(8.dp))
            .border(1.dp, Red.copy(alpha = 0.5f), CutCornerShape(8.dp)).padding(12.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            com.cyberpunk.gmtool.ui.components.FaText(gtr(t), color = White, fontSize = 13.sp, lineHeight = 20.sp,
                justify = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** کارت جزئیات: عنوان انگلیسی در خط خودش (چپ‌به‌راست و راست‌چین نباشد)، خطوط فارسی زیرش */
@Composable
private fun DetailCard(title: String, lines: List<String>) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .background(CardBg, CutCornerShape(8.dp)).border(1.dp, Red.copy(alpha = 0.3f), CutCornerShape(8.dp)).padding(12.dp)
    ) {
        // نام انگلیسی در یک خط LTR جدا تا با متن فارسی قاطی نشود
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(gtr(title), color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        }
        Spacer(Modifier.height(4.dp))
        lines.forEach { line ->
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(gtr(line), color = White, fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            }
        }
    }
}

// ---------- داده‌های مرجع (برگرفته از کتاب Core) ----------

// (نام انگلیسی، اثر فارسی، درمان فارسی)
private val headInjuries = listOf(
    Triple("Lost Eye", "کاملاً از دست می‌رود؛ ${ltr("-4")} به حمله‌ی تفنگی و چک‌های ادراک بصری. جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "سرپایی ندارد — جراحی ${ltr("DV17")}."),
    Triple("Brain Injury", "${ltr("-2")} به همه‌ی اکشن‌ها. جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "سرپایی ندارد — جراحی ${ltr("DV17")}."),
    Triple("Damaged Eye", "${ltr("-2")} به حمله‌ی تفنگی و چک‌های ادراک بصری.", "پارامدیک ${ltr("DV15")} یا جراحی ${ltr("DV13")}."),
    Triple("Concussion", "${ltr("-2")} به همه‌ی اکشن‌ها.", "کمک‌های اولیه/پارامدیک ${ltr("DV13")}؛ درمان سرپایی اثر را دائمی حذف می‌کند."),
    Triple("Broken Jaw", "${ltr("-4")} به همه‌ی اکشن‌هایی که به حرف‌زدن نیاز دارند.", "پارامدیک ${ltr("DV13")} یا پارامدیک/جراحی ${ltr("DV13")}."),
    Triple("Foreign Object (Head)", "اگر در یک نوبت بیش از ۴ متر پیاده حرکت کنی، در پایان نوبت ${ltr("5")} آسیب پاداش دوباره وارد می‌شود.", "کمک‌های اولیه/پارامدیک ${ltr("DV13")}؛ درمان سرپایی اثر را دائمی حذف می‌کند."),
    Triple("Whiplash", "جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "پارامدیک ${ltr("DV13")} یا پارامدیک/جراحی ${ltr("DV13")}."),
    Triple("Cracked Skull", "شلیک هدف‌گرفته به سر، آسیب پس از SP را به‌جای ضریب ۲، با ضریب ۳ حساب می‌کند. جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "پارامدیک ${ltr("DV15")} یا پارامدیک/جراحی ${ltr("DV15")}."),
    Triple("Damaged Ear", "اگر بیش از ۴ متر پیاده حرکت کنی، نوبت بعد نمی‌توانی Move Action بگیری؛ به‌علاوه ${ltr("-2")} به ادراک شنوایی.", "پارامدیک ${ltr("DV13")} یا جراحی ${ltr("DV13")}."),
    Triple("Crushed Windpipe", "نمی‌توانی حرف بزنی. جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "سرپایی ندارد — جراحی ${ltr("DV15")}."),
    Triple("Lost Ear", "کاملاً از دست می‌رود؛ حرکت بیش از ۴ متر یعنی نوبت بعد بدون Move Action؛ ${ltr("-4")} به ادراک شنوایی. جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "سرپایی ندارد — جراحی ${ltr("DV17")}.")
)

private val bodyInjuries = listOf(
    Triple("Dismembered Arm", "بازو کاملاً جدا می‌شود و وسیله‌ی آن دست فوراً می‌افتد. جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "سرپایی ندارد — جراحی ${ltr("DV17")}."),
    Triple("Dismembered Hand", "دست کاملاً جدا می‌شود و وسیله فوراً می‌افتد. جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "سرپایی ندارد — جراحی ${ltr("DV17")}."),
    Triple("Collapsed Lung", "${ltr("-2")} به MOVE (کمینه ۱). جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "پارامدیک ${ltr("DV15")} یا جراحی ${ltr("DV15")}."),
    Triple("Broken Ribs", "اگر در یک نوبت بیش از ۴ متر پیاده حرکت کنی، در پایان نوبت ${ltr("5")} آسیب پاداش دوباره وارد می‌شود.", "پارامدیک ${ltr("DV13")} یا پارامدیک ${ltr("DV15")} / جراحی ${ltr("DV13")}."),
    Triple("Broken Arm", "بازو قابل استفاده نیست؛ وسیله‌ی آن دست فوراً می‌افتد.", "پارامدیک ${ltr("DV13")} یا پارامدیک ${ltr("DV15")} / جراحی ${ltr("DV13")}."),
    Triple("Foreign Object (Body)", "اگر در یک نوبت بیش از ۴ متر پیاده حرکت کنی، در پایان نوبت ${ltr("5")} آسیب پاداش دوباره وارد می‌شود.", "کمک‌های اولیه/پارامدیک ${ltr("DV13")}؛ درمان سرپایی اثر را دائمی حذف می‌کند."),
    Triple("Broken Leg", "${ltr("-4")} به MOVE (کمینه ۱).", "پارامدیک ${ltr("DV13")} یا پارامدیک ${ltr("DV15")} / جراحی ${ltr("DV13")}."),
    Triple("Torn Muscle", "${ltr("-2")} به حمله‌های غوغا (Melee).", "کمک‌های اولیه/پارامدیک ${ltr("DV13")}؛ درمان سرپایی اثر را دائمی حذف می‌کند."),
    Triple("Spinal Injury", "نوبت بعد نمی‌توانی اکشن بگیری (فقط حرکت). جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "پارامدیک ${ltr("DV15")} یا جراحی ${ltr("DV15")}."),
    Triple("Crushed Fingers", "${ltr("-4")} به همه‌ی اکشن‌هایی که به آن دست نیاز دارند.", "پارامدیک ${ltr("DV13")} یا جراحی ${ltr("DV15")}."),
    Triple("Dismembered Leg", "پا کاملاً جدا می‌شود؛ ${ltr("-6")} به MOVE (کمینه ۱) و نمی‌توانی جاخالی بدهی. جریمه‌ی پایه‌ی تست مرگ ${ltr("+1")}.", "سرپایی ندارد — جراحی ${ltr("DV17")}.")
)

// (نام انگلیسی، نام/معرفی فارسی، توضیح)
private val gangData = listOf(
    Triple("Maelstrom", "مِیل‌استروم", "گنگ بدن‌سازهای سایکوتیک و عاشق کروم در Watson (ناحیه‌ی All Foods). به سایبرویر تهاجمی و کروم جنگی معروفند؛ بی‌رحم و غیرقابل‌پیش‌بینی."),
    Triple("Valentinos", "والنتینوها", "گنگ خانوادگی و مذهبی با ریشه‌های لاتین در Heywood. به وفاداری و غرور شهرت دارند؛ سبک‌شان پر زرق‌وبرق و ماشین‌های لوکس است."),
    Triple("6th Street", "سکست‌استریت", "کهنه‌سربازان و شبه‌نظامیانِ خودخوانده‌ی نظم؛ در چند منطقه پخشند. خود را قانون می‌دانند و معمولاً سنگین‌مسلح‌اند."),
    Triple("Tyger Claws", "تایگر کلاز", "گنگ بزرگ ژاپنی/آسیای شرقی با کافه‌ها، باشگاه‌ها و تجارت شبانه در Watson و سایر مناطق. خونسرد، سازمان‌یافته و خطرناک."),
    Triple("Animals", "انیمال‌ها", "جنگجوهای غول‌پیکرِ بدنساز که عمداً کمتر کروم نصب می‌کنند و به قدرت خام بدن و داروهای بدنسازی متکی‌اند؛ به‌عنوان امنیت و عضله استخدام می‌شوند."),
    Triple("Voodoo Boys", "وودو بویز", "نت‌رانرهای نخبه‌ی Pacifica با ریشه‌های کریول/هائیتی؛ بر اعماق Old Net و دِیمون‌های مرگبار مسلط‌اند."),
    Triple("Barghest", "بارگِست", "شبه‌نظامیانِ منطقه‌ی Dogtown در Pacifica؛ سربازان سابق و مزدورانِ بی‌رحم که شهرِ محصور را کنترل می‌کنند (از الحاقیه‌ی Phantom Liberty).")
)

// (نام انگلیسی، توضیح فارسی) — مگاکورپ‌ها
private val corpData = listOf(
    Triple("Arasaka", "آراساکا", "بزرگ‌ترین مگاکورپ جهان؛ ژاپنی، تخصصش امنیت، بانکداری و تسلیحات. برجش در مرکز شهر سیطره‌ی بصری نایت‌سیتی است و نفوذ سیاسی و نظامی فوق‌العاده دارد."),
    Triple("Militech", "میلی‌تک", "غول تسلیحات و تجهیزات نظامی آمریکایی؛ تأمین‌کننده‌ی ارتش‌ها، مزدوران و حتی بخشی از تسلیحات خیابان. رقیب اصلی آراساکا."),
    Triple("Biotechnica", "بایوتکنیکا", "شرکت بیوتکنولوژی و کشاورزی ایتالیایی؛ تولید سوخت زیستی CHOOH2، غذای مصنوعی (SCOP) و دارو و مهندسی ژنتیک. انحصار داروهای روان‌درمانی را دارد."),
    Triple("Zetatech", "زتاتک", "متخصص هوافضا، هوانوردی و رایانش؛ تأمین‌کننده‌ی درون‌ها (AVها)، سیستم‌های پردازش و امنیت دیجیتال."),
    Triple("Kang Tao", "کانگ تائو", "مگاکورپ چینیِ تسلیحات و فناوری؛ رقیب شرقی آراساکا و میلی‌تک با سلاح‌های هوشمند (Smart) پیشرفته."),
    Triple("Petrochem", "پتروکم", "غول انرژی؛ پالایش و توزیع سوخت CHOOH2 و بهره‌برداری از منابع؛ رقیب دیرینه‌ی تری‌فکشنال/بیوتکنیکا."),
    Triple("Night Corp", "نایت کورپ", "شرکتِ عملاً مالک نایت‌سیتی؛ سازنده‌ی بزرگراه‌های شهر و تأمین‌کننده‌ی زیرساخت و ترابری عمومی، ریشه در خاندان بنیان‌گذار شهر دارد.")
)

// سازمان‌های غیرشرکتی
private val factionData = listOf(
    Triple("Trauma Team", "تیم تروما", "سرویس آمبولانس و بیمه‌ی پزشکی زرهی؛ با اشتراک (پلن‌های برنزی تا پلاتینی)، ظرف چند دقیقه یک تیم مسلح و مسعف برای نجات مشترک از راه می‌رسد."),
    Triple("NCPD", "پلیس نایت‌سیتی", "پلیس شهر نایت‌سیتی؛ خصوصی‌شده و کم‌بودجه. بیشتر به مناطقی که پول دارند خدمت می‌کند و در محله‌های گنگ‌نشین حضوری ندارد."),
    Triple("NetWatch", "نت‌واچ", "سازمان نظارت بر شبکه؛ مأمورانش با نت‌رانرهای غیرقانونی می‌جنگند، ویروس‌ها و دِیمون‌های Old Net را مهار می‌کنند و دیوار آتش بزرگ (Blackwall) را نگه می‌دارند.")
)

private val currencyData = listOf(
    Triple("Eurodollar (eb / €$)", "یورودلار", "یکای اصلی پول در عصر RED؛ یک پول دیجیتال و بین‌المللی که روی شبکه جابه‌جا می‌شود. مردم عامیانه به آن «ادی» (Eddies) می‌گویند."),
    Triple("Network Economy", "اقتصاد شبکه‌محور", "بیشتر تراکنش‌ها دیجیتال‌اند؛ پول نقد فیزیکی کمیاب است. موجودی‌ات در تب GEAR/INVENTORY با €$ نشان داده می‌شود."),
    Triple("Rent & Lifestyle", "اجاره و لایف‌استایل", "هزینه‌ی سبک زندگی و مسکن اول هر ماه پرداخت می‌شود؛ در بخش Bio گزینه‌ی Lifestyle & Housing این هزینه قابل تنظیم است."),
    Triple("Old Money", "پول کهنه", "دلار قدیمی آمریکا و ارزهای ملی پیش از فروپاشی دیگر ارزش رسمی ندارند و در مبادلات جدی پذیرفته نمی‌شوند.")
)

// (نام انگلیسی، معادل فارسی، توضیح)
private val woundData = listOf(
    Triple("Lightly Wounded", "زخم خفیف", "وقتی HP از کامل کمتر است. جریمه‌ای ندارد. پایدارکردن: ${ltr("DV10")}."),
    Triple("Seriously Wounded", "زخم جدی", "وقتی کمتر از نصف HP (گرد به بالا) داری. ${ltr("-2")} به همه‌ی اکشن‌ها. پایدارکردن: ${ltr("DV13")}."),
    Triple("Mortally Wounded", "زخم مرگبار", "وقتی HP به صفر یا کمتر برسد. ${ltr("-4")} به همه‌ی اکشن‌ها و ${ltr("-6")} به MOVE؛ هر نوبت باید تست مرگ بدهی و با هر حمله یک جراحت بحرانی می‌گیری. پایدارکردن: ${ltr("DV15")} تا به ۱ HP و بیهوشی برگردی."),
    Triple("Dead", "مرگ", "یک تست مرگ شکست‌خورده = مرگ؛ راه برگشتی نیست.")
)

private val deathData = listOf(
    "جریمه‌ی پایه (Base Death Save)" to "برابر با امتیاز BODY کاراکتر است. بعضی جراحت‌های بحرانی این جریمه را ${ltr("+1")} زیاد می‌کنند و تا پایدارشدن روی هم جمع می‌شود.",
    "تست مرگ" to "وقتی Mortally Wounded شوی (HP صفر یا کمتر)، در شروع هر نوبت یک ${ltr("1d10")} می‌ریزی. اگر نتیجه بالاتر از عددِ جریمه‌ی تست مرگ آمد، زنده می‌مانی و می‌توانی اکشن بگیری؛ در غیر این صورت می‌میری.",
    "پایدارکردن (Stabilization)" to "با چک Skill پزشکی (Paramedic/First Aid) بر DV مربوط به وضعیت زخم، کاراکتر به ۱ HP برمی‌گردد و بیهوش می‌شود؛ پس از آن جریمه‌های تست مرگ به مقدار پایه ریست می‌شوند."
)

// ================= کالا و خدمات (فهرست) =================
@Composable
private fun GoodsHub(onOpen: (GoodsTopic) -> Unit) {
    val topics = listOf(
        GoodsTopic.DRUGS to "داروهای خیابانی و روزمره، اثر و قیمت و اعتیاد.",
        GoodsTopic.FOOD to "هزینه‌ی خوراک و سبک زندگی ماهانه.",
        GoodsTopic.HOUSING to "اجاره‌ی ماهانه‌ی انواع مسکن.",
        GoodsTopic.JOBS to "گیگ‌ها و دستمزد بر پایه‌ی خطر.",
        GoodsTopic.SERVICES to "پارامدیک، تعمیر کروم، کرایه و سایر خدمات.",
        GoodsTopic.STORE to "خرید سلاح، زره، سایبرویر و تجهیزات.",
        GoodsTopic.THERAPY to "روان‌درمانی و بازیابی Humanity."
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { AppHeader("GOODS & SERVICES / کالا و خدمات") }
        items(topics.size) { i ->
            val (topic, desc) = topics[i]
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpen(topic) }
                    .background(CardBg, CutCornerShape(8.dp)).border(1.dp, Red.copy(alpha = 0.3f), CutCornerShape(8.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr(topic.titleEn), color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                    Text(topic.titleFa, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(Modifier.height(3.dp))
                    Text(gtr(desc), color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Red)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun GoodsTopicDetail(topic: GoodsTopic) {
    var detail by remember { mutableStateOf<Pair<String, String>?>(null) }
    var coreItemDetail by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            AppHeader("${topic.titleEn} / ${topic.titleFa}")
            Text("برای دیدن قیمت و توضیحات روی هر گزینه بزن.", color = Muted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Right)
        }
        when (topic) {
            GoodsTopic.DRUGS -> {
                items(drugData.size) { i ->
                    val d = drugData[i]
                    ReferenceButton(primary = d.second, secondary = d.first) {
                        coreItemDetail = d.first
                    }
                }
                item { InfoNote("داروهای رزمی/خیابانی اعتیادآورند؛ هر بار مصرف، در برابر اثر ثانویه (Secondary Effect) تاس بریز. اعتیاد را فقط با روان‌درمانی (Therapy) می‌توان ترک کرد.") }
            }
            GoodsTopic.FOOD -> items(foodData.size) { i ->
                val d = foodData[i]
                ReferenceButton(primary = d.second, secondary = d.first) { detail = "${d.second} / ${d.first}" to d.third }
            }
            GoodsTopic.HOUSING -> items(housingData.size) { i ->
                val d = housingData[i]
                ReferenceButton(primary = d.second, secondary = d.first) { detail = "${d.second} / ${d.first}" to d.third }
            }
            GoodsTopic.JOBS -> items(jobData.size) { i ->
                val d = jobData[i]
                ReferenceButton(primary = d.second, secondary = d.first) { detail = "${d.second} / ${d.first}" to d.third }
            }
            GoodsTopic.SERVICES -> items(serviceData.size) { i ->
                val d = serviceData[i]
                ReferenceButton(primary = d.second, secondary = d.first) { detail = "${d.second} / ${d.first}" to d.third }
            }
            GoodsTopic.THERAPY -> {
                items(therapyData.size) { i ->
                    val t = therapyData[i]
                    ReferenceButton(primary = t.third, secondary = t.first) {
                        detail = "${t.third} / ${t.first}" to "هزینه/زمان: ${t.second}\n\n${t.fourth}"
                    }
                }
                item { InfoNote("Humanity بدون کندن سایبرویر هرگز کامل برنمی‌گردد: هر قطعه سایبرویر معمولی سقف Humanity را ۲ و هر قطعه Borgware آن را ۴ پایین نگه می‌دارد.") }
            }
            GoodsTopic.STORE -> item {
                ReferenceButton(primary = "فروشگاه", secondary = "Store") {
                    detail = "فروشگاه / Store" to "برای خرید سلاح، مهمات، زره، سایبرویر، لباس، وسایل و وسایل نقلیه به تب INVENTORY برو و دکمه‌ی Store را بزن."
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
    detail?.let { (title, body) -> ReferenceDetailDialog(title, body) { detail = null } }
    coreItemDetail?.let { label -> CoreItemDetailDialog(label = label, categoryHint = "Drugs", onDismiss = { coreItemDetail = null }) }
}

// ---------- داده‌های کالا و خدمات (برگرفته از کتاب Core) ----------

// (نام، قیمت، توضیح اثر، نکته)
private val drugData = listOf(
    Quadruple("Black Lace", "بلک لیس", "${ltr("50eb")} هر دُز", "برای جزئیات دقیق Core روی ردیف بزن."),
    Quadruple("Blue Glass", "بلو گلس", "${ltr("20eb")} هر دُز", "برای جزئیات دقیق Core روی ردیف بزن."),
    Quadruple("Boost", "بوست", "${ltr("50eb")} هر دُز", "برای جزئیات دقیق Core روی ردیف بزن."),
    Quadruple("Smash", "اسمش", "${ltr("10eb")} هر دُز", "برای جزئیات دقیق Core روی ردیف بزن."),
    Quadruple("Synthcoke", "سینت‌کوک", "${ltr("20eb")} هر دُز", "برای جزئیات دقیق Core روی ردیف بزن.")
)

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// (نام انگلیسی، عنوان فارسی، توضیح)
private val foodData = listOf(
    Triple("Generic Food (Kibble)", "غذای جنریک (کیبل)", "تقریباً ${ltr("100eb")} در ماه؛ ماده‌ی غذایی فشرده و بی‌طعم که سیر می‌کند ولی لذتی ندارد."),
    Triple("Prepacked Food", "غذای بسته‌بندی (پرپک)", "معمولی ${ltr("300eb")} و خوب ${ltr("600eb")} در ماه؛ وعده‌های آماده‌ی طعم‌دار و متنوع."),
    Triple("Fresh Food", "غذای تازه", "${ltr("1500eb")} در ماه؛ میوه، سبزی و گوشت واقعی که گران و کمیاب است.")
)

private val housingData = listOf(
    Triple("Cargo Container", "کانتینر بار", "${ltr("1000eb")} در ماه؛ یک کانتینر فلزی تبدیل‌شده در حاشیه‌ی شهر. سقف دارد و بیشتر نیست."),
    Triple("Studio Apartment", "آپارتمان استودیو", "${ltr("1500eb")} در ماه؛ یک اتاق کوچک با امکانات پایه."),
    Triple("2-Bedroom Apt.", "آپارتمان دوخوابه", "${ltr("2500eb")} در ماه؛ جای راحت برای زندگی یا پایگاه تیم کوچک."),
    Triple("Luxury Conapt", "کونپت لوکس", "${ltr("7500eb")} در ماه؛ آپارتمان هوشمند مدرن با امکانات رفاهی و امنیت."),
    Triple("Penthouse", "پنت‌هاوس", "${ltr("15000eb")} در ماه؛ قله‌ی زندگی لاکچری با چشم‌انداز شهر و سرویس کامل.")
)

private val jobData = listOf(
    Triple("Minor Gigs", "گیگ‌های کوچک", "کارهای ساده (پیک، مراقبت، دزدی جزئی)؛ معمولاً ${ltr("100–500eb")}."),
    Triple("Standard Gigs", "گیگ‌های معمولی", "کارهای خطر متوسط با درگیری محدود؛ ${ltr("1000eb")} به بالا، رایج‌ترین دستمزد ادج‌رانرها."),
    Triple("High-Risk Jobs", "کارهای پرخطر", "ترور، نفوذ به شرکتی، رویارویی با گنگ بزرگ؛ ${ltr("5000eb")} و بالاتر، بسته به خطر و فیکسر."),
    Triple("Getting Work", "یافتن کار", "گیگ‌ها عمدتاً از طریق فیکسرها می‌آیند؛ اعتبار (Reputation) و روابط تعیین‌کننده‌ی دستمزد و کیفیت کارند.")
)

private val serviceData = listOf(
    Triple("Paramedic / First Aid", "خدمات پزشکی", "درمان سرپایی و پایدارکردن؛ هزینه بر اساس مهارت و شرایط، معمولاً چند ده تا چند صد eb. Trauma Team برای مشترکین سررسید می‌رسد."),
    Triple("Cyberware Repair", "تعمیر سایبرویر", "تعمیر سایبرویر آسیب‌دیده با مهارت Cybertech/Tech؛ اگر نقطه‌ی جراحت کروم باشد می‌توان به‌جای پزشکی از آن استفاده کرد."),
    Triple("Rent & Utilities", "اجاره و خدمات شهری", "اجاره‌ی مسکن و هزینه‌ی جاری ماهانه (آب، برق، شبکه)؛ اول هر ماه پرداخت می‌شود و در بخش Bio قابل تنظیم است."),
    Triple("Chrome Wash", "شست‌وشوی کروم", "تمیزکاری و نگهداری سایبرویر؛ خدمت رایج در کلینیک‌ها برای جلوگیری از خرابی."),
    Triple("Transport", "حمل‌ونقل", "کرایه‌ی تاکسی/درون یا استفاده از حمل‌ونقل عمومی Night Corp برای جابه‌جایی در شهر.")
)

// (نام انگلیسی، قیمت، عنوان، توضیح)
private val therapyData = listOf(
    Quadruple("Standard Humanity Loss", "یک هفته، ${ltr("500eb")}", "روان‌درمانی استاندارد", "ترکیب مدیریت استرس/خشم، هیپنوتیزم و بازبرنامه‌ریزی جزئی مغز با دارو. بازیابی ${ltr("2d6")} Humanity ازدست‌رفته. مواد لازم برای مدتک: ${ltr("100eb")} با چک Medical Tech به DV15."),
    Quadruple("Extreme Humanity Loss", "یک هفته، ${ltr("1000eb")}", "روان‌درمانی شدید", "بازبرنامه‌ریزی مستقیم و شدید مغز با داروهای پیشرفته. بازیابی ${ltr("4d6")} Humanity. مواد لازم برای مدتک: ${ltr("500eb")} با چک Medical Tech به DV17."),
    Quadruple("Addiction Therapy", "یک هفته، ${ltr("1000eb")}", "ترک اعتیاد", "روان‌درمانی فشرده به‌همراه داروهای ضداعتیاد در محیط امن؛ فرد از یک اعتیاد رها می‌شود، اما تا یک سال پس از ترک در برابر اثر ثانویه‌ی همان ماده خودکار شکست می‌خورد.")
)

// ---------- اجزای مشترک ----------
@Composable
private fun AppHeader(t: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column {
        Text(gtr(t), color = Red, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        HorizontalDivider(color = Red.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
    }
    }
}

@Composable
private fun ListRow(title: String, subtitle: String, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .background(CardBg, CutCornerShape(8.dp)).border(1.dp, Red.copy(alpha = 0.3f), CutCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.weight(1f)) {
                Text(gtr(title), color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Text(gtr(subtitle), color = Muted, fontSize = 13.sp, lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            }
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, gtr("Delete"), tint = Red) }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Red),
        shape = CutCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Icon(Icons.Default.Add, null, tint = Black); Spacer(Modifier.width(8.dp))
        Text(gtr(label), color = Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TwoFieldDialog(title: String, label1: String, label2: String,
                           onDismiss: () -> Unit = {},
                           onDone: (String, String) -> Unit) {
    var f1 by remember { mutableStateOf("") }
    var f2 by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Black),
            shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(14.dp))) {
            Column(Modifier.padding(20.dp)) {
                Text(gtr(title), color = Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = f1, onValueChange = { f1 = it }, label = { Text(gtr(label1)) },
                    singleLine = true, colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Red, unfocusedBorderColor = Red.copy(alpha = 0.5f),
                        focusedTextColor = White, unfocusedTextColor = White,
                        focusedLabelColor = Red, unfocusedLabelColor = Muted,
                        cursorColor = Red
                    ), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = f2, onValueChange = { f2 = it }, label = { Text(gtr(label2)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Red, unfocusedBorderColor = Red.copy(alpha = 0.5f),
                        focusedTextColor = White, unfocusedTextColor = White,
                        focusedLabelColor = Red, unfocusedLabelColor = Muted,
                        cursorColor = Red
                    ), modifier = Modifier.fillMaxWidth().height(110.dp))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = CutCornerShape(8.dp)) { Text("انصراف", color = White) }
                    OutlinedButton(onClick = { f1 = ""; f2 = "" }, modifier = Modifier.weight(1f),
                        shape = CutCornerShape(8.dp)) { Text("پاک", color = Muted) }
                    Button(onClick = { if (f1.isNotBlank()) onDone(f1, f2) },
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("ذخیره", color = Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
