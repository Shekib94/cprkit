package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.cyberpunk.gmtool.ui.components.RuleInfoButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.CriticalInjuries
import com.cyberpunk.gmtool.data.GmData
import com.cyberpunk.gmtool.data.LifeData
import com.cyberpunk.gmtool.data.RoleAssistantData
import com.cyberpunk.gmtool.data.RoleLoreData
import com.cyberpunk.gmtool.data.NomadFamilyProfile
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.ui.components.CoreItemDetailDialog
import com.cyberpunk.gmtool.ui.components.FaText
// توابع توسعه‌ی دامنه‌های تفکیک‌شده‌ی ViewModel در همین پکیج‌اند.
import com.cyberpunk.gmtool.viewmodel.*
import kotlinx.coroutines.launch

/** بسته‌بندی قطعات لاتین/عددی داخل متن فارسی تا بیدی بهم نریزد */
// نسخه‌ی مشترک و اصلاح‌شده در ui/components/PersianText.kt است.
// نسخه‌ی محلی قبلی نقطه‌ی پایان جمله را داخل تکه‌ی لاتین می‌گرفت.
private fun ltrWrap(text: String): String =
    com.cyberpunk.gmtool.ui.components.isolateLatin(text)

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)
private val CardBg = Color(0xFF1A1A1A)

private enum class GmTool { HOME, MAP, BATTLE_MAP, INJURIES, ENCOUNTER, WEATHER, CLOCKS, COMBAT, LIFE, NET_ARCH, NIGHT_MARKET, CAMPAIGN, ECONOMY, ROLE_HELPERS, SOLO_JOBS, HIREABLE_ALLIES, NOMAD_ASSISTANT, MEDTECH_ASSISTANT, MEDIA_ASSISTANT, EXEC_ASSISTANT, LAWMAN_ASSISTANT, FIXER_ASSISTANT,  }

@Composable
fun GmToolsScreen(onBack: () -> Unit, viewModel: CharacterViewModel? = null, onFullscreenChanged: (Boolean) -> Unit = {}) {
    var tool by remember { mutableStateOf(GmTool.HOME) }
    var battleMapFullscreen by remember { mutableStateOf(false) }
    val reloadGeneration = viewModel?.dataGeneration?.collectAsState()?.value ?: 0L

    Scaffold(
        topBar = {
            if (!battleMapFullscreen) {
                CyberpunkHeader(title = if (tool == GmTool.HOME) "GM TOOLS" else toolName(tool),
                    onBackClick = {
                        // در صفحه‌ی کمکی هر نقش، عقب باید اول به منوی ROLE HELPERS برگردد،
                        // نه اینکه یک‌راست به GM TOOLS بپرد.
                        val rolePages = setOf(
                            GmTool.NOMAD_ASSISTANT, GmTool.MEDTECH_ASSISTANT, GmTool.MEDIA_ASSISTANT,
                            GmTool.EXEC_ASSISTANT, GmTool.LAWMAN_ASSISTANT, GmTool.FIXER_ASSISTANT
                        )
                        tool = when {
                            tool == GmTool.HOME -> { onBack(); return@CyberpunkHeader }
                            tool in rolePages -> GmTool.ROLE_HELPERS
                            else -> GmTool.HOME
                        }
                    })
            }
        },
        containerColor = Black
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            key(reloadGeneration) { when (tool) {
                GmTool.HOME -> ToolGrid { tool = it }
                GmTool.MAP -> MapView()
                GmTool.BATTLE_MAP -> viewModel?.let { BattleMapScreen(it, fullscreen = battleMapFullscreen, onFullscreenChanged = { fs -> battleMapFullscreen = fs; onFullscreenChanged(fs) }) }
                GmTool.INJURIES -> InjuriesList()
                GmTool.ENCOUNTER -> RandomEncounterTool()
                GmTool.WEATHER -> WeatherTool()
                GmTool.CLOCKS -> ClocksTool()
                GmTool.COMBAT -> QuickCombat()
                GmTool.LIFE -> LifeReference()
                GmTool.NET_ARCH -> NetArchitectureTool(viewModel)
                GmTool.NIGHT_MARKET -> NightMarketTool(viewModel)
                GmTool.CAMPAIGN -> viewModel?.let { CampaignScreen(it) { tool = GmTool.HOME } }
                GmTool.ECONOMY -> viewModel?.let { EconomyTool(it) }
                GmTool.ROLE_HELPERS -> viewModel?.let { vm ->
                    RoleHelpersHub(
                        vm,
                        onNomad = { tool = GmTool.NOMAD_ASSISTANT },
                        onMedtech = { tool = GmTool.MEDTECH_ASSISTANT },
                        onMedia = { tool = GmTool.MEDIA_ASSISTANT },
                        onExec = { tool = GmTool.EXEC_ASSISTANT },
                        onLawman = { tool = GmTool.LAWMAN_ASSISTANT },
                        onFixer = { tool = GmTool.FIXER_ASSISTANT }
                    )
                }
                GmTool.SOLO_JOBS -> GigsScreen()
                GmTool.HIREABLE_ALLIES -> HireableAlliesScreen()
                GmTool.NOMAD_ASSISTANT -> viewModel?.let { NomadGmAssistant(it) }
                // Medtech: تاس‌های درمان (Stabilize/Quick Fix/Pharmaceutical) از خود صفحه ریخته می‌شوند
                GmTool.MEDTECH_ASSISTANT -> viewModel?.let { MedtechGmAssistant(it) }
                GmTool.MEDIA_ASSISTANT -> viewModel?.let { MediaGmAssistant(it) }
                GmTool.EXEC_ASSISTANT -> viewModel?.let { ExecGmAssistant(it) }
                GmTool.LAWMAN_ASSISTANT -> viewModel?.let { LawmanGmAssistant(it) }
                GmTool.FIXER_ASSISTANT -> viewModel?.let { FixerGmAssistant(it) }
            } }
        }
    }
}

private fun toolName(t: GmTool) = when (t) {
    GmTool.MAP -> "NIGHT CITY MAP"
    GmTool.BATTLE_MAP -> "BATTLE MAP"
    GmTool.INJURIES -> "CRITICAL INJURIES"
    GmTool.ENCOUNTER -> "RANDOM ENCOUNTERS"
    GmTool.WEATHER -> "WEATHER"
    GmTool.CLOCKS -> "CLOCKS"
    GmTool.COMBAT -> "QUICK & DIRTY COMBAT"
    GmTool.LIFE -> "LIFESTYLE & HOUSING"
    GmTool.NET_ARCH -> "NET ARCHITECTURE"
    GmTool.NIGHT_MARKET -> "NIGHT MARKET"
    GmTool.CAMPAIGN -> "CAMPAIGN MANAGER"
    GmTool.ECONOMY -> "ECONOMY MANAGER"
    GmTool.ROLE_HELPERS -> "ROLE HELPERS"
    GmTool.SOLO_JOBS -> "GIGS"
    GmTool.HIREABLE_ALLIES -> "HIREABLE ALLIES"
    GmTool.NOMAD_ASSISTANT -> "NOMAD FAMILY & MOTO"
    GmTool.MEDTECH_ASSISTANT -> "MEDTECH ASSISTANT"
    GmTool.MEDIA_ASSISTANT -> "MEDIA ASSISTANT"
    GmTool.EXEC_ASSISTANT -> "EXEC ASSISTANT"
    GmTool.LAWMAN_ASSISTANT -> "LAWMAN ASSISTANT"
    GmTool.FIXER_ASSISTANT -> "FIXER ASSISTANT"
    else -> "GM TOOLS"
}

private fun toolHelp(t: GmTool): String = when (t) {
    GmTool.BATTLE_MAP -> "بتل‌مپ یک لایه تاکتیکی مستقل است. هر تصویر را Import کن، با دو نقطه و یک فاصله واقعی کالیبره کن؛ مختصات، Grid استاندارد RED، فاصله‌ها و توکن‌ها در مختصات خود تصویر ذخیره می‌شوند و به اندازه صفحه گوشی وابسته نیستند."
    GmTool.MAP -> "نقشه برای پیدا کردن سریع محله‌ها و مکان‌های مهم نایت‌سیتی است. فاصله و زمان سفر را GM با توجه به صحنه تعیین می‌کند؛ خود نقشه جایگزین قضاوت GM نیست."
    GmTool.INJURIES -> "Critical Injury آسیب جدی جدا از کم‌شدن HP است. وقتی جراحت بحرانی ایجاد می‌شود، اثر مخصوص خودش را دارد و معمولاً ۵ آسیب اضافه مستقیم به HP وارد می‌کند. Quick Fix موقتی است؛ Treatment درمان اصلی و پایدار است."
    GmTool.ENCOUNTER -> "Encounter تصادفی فقط یک جرقه برای ساخت صحنه است. نتیجه را با وضعیت محله، ساعت، دشمنان و داستان فعلی تطبیق بده؛ لازم نیست هر نتیجه حتماً به نبرد ختم شود."
    GmTool.WEATHER -> "آب‌وهوا بیشتر ابزار فضاسازی GM است. اگر شرایط جوی واقعاً دید، حرکت یا رانندگی را سخت کند، DV مناسب را بر اساس موقعیت تعیین کن؛ این صفحه به‌تنهایی Modifier اجباری جدید نمی‌سازد."
    GmTool.CLOCKS -> "Clock برای نشان‌دادن پیشرفت یک خطر، پروژه یا رویداد است. هر Segment یعنی یک قدم به نتیجه نزدیک‌تر شده‌ای. این ابزار مدیریتی است و خودش قانون تازه‌ای به Core اضافه نمی‌کند."
    GmTool.COMBAT -> "Quick Combat برای حل سریع درگیری‌های فرعی GM است. برای نبرد کامل بازیکنان از Combat اصلی استفاده کن، چون آنجا Armor، Critical Injury، Action، Range و دفاع با جزئیات بیشتری مدیریت می‌شوند."
    GmTool.LIFE -> "Lifestyle کیفیت خوراک و زندگی روزمره را نشان می‌دهد و Housing محل سکونت است. این دو هزینه‌های جداگانه ماهانه دارند. انتخاب ارزان‌تر فقط پول ذخیره نمی‌کند؛ روی شرایط داستانی زندگی شخصیت هم اثر می‌گذارد."
    GmTool.NET_ARCH -> "NET Architecture محیطی است که Netrunner داخل آن Floor به Floor حرکت می‌کند. Password، File، Control Node و Black ICE روی Floorها قرار می‌گیرند. Netrunner با Interface و NET Actions با آن‌ها تعامل می‌کند."
    GmTool.NIGHT_MARKET -> "در اقتصاد RED هر چیزی همیشه روی قفسه فروشگاه نیست. Night Market راه اصلی دسترسی به کالاهای گران‌تر است و Fixer با Operator می‌تواند دسترسی به بازار و کالاها را بهتر کند."
    GmTool.CAMPAIGN -> "Campaign Manager برای یادداشت و سازمان‌دهی Session، Encounter و اتفاقات داستان است. داده‌های این بخش ابزار GM هستند و به‌خودی‌خود STAT یا قانون شخصیت‌ها را تغییر نمی‌دهند."
    GmTool.ECONOMY -> "Economy Manager هزینه‌های ماهانه، خدمات، فروش و Haggle را مدیریت می‌کند. Haggle مزیت Role Ability فیکسر است و نتیجه‌اش با Rank تغییر می‌کند؛ Trading عادی به‌تنهایی مزایای Operator را نمی‌دهد."
    GmTool.ROLE_HELPERS -> "دستیار Role Ability برای Nomad، Medtech، Media، Exec، Lawman و Fixer؛ اطلاعات کاراکتر از Character Sheet خوانده می‌شود."
    GmTool.SOLO_JOBS -> "دو بخش: کارهای انفرادی هر Role با REP و پاداش، و کارهای گروهی که سناریوی کامل و آماده‌ی اجرا هستند."
    GmTool.HIREABLE_ALLIES -> "NPCهای همراه قابل استخدام در چهار Tier. Min REP راهنمای دسترسی است و GM می‌تواند Favor/Contact/Deal را جایگزین پول کند."
    GmTool.NOMAD_ASSISTANT -> "این دستیار اطلاعات Role Lifepath را مستقیم از همان Character Sheet/BIO می‌خواند تا Pack Size، نوع Pack و کار Pack هیچ‌وقت دو نسخه‌ی متفاوت نداشته باشند. Moto و Motorpool طبق Core نمایش داده می‌شوند؛ Favor/Debt فقط یادداشت GM است."
    GmTool.MEDTECH_ASSISTANT -> "درمان واقعی: بیمار را از لیست کاراکترها انتخاب می‌کنی، وضعیتش از برگه خوانده می‌شود و Stabilize / Quick Fix / Treatment / Pharmaceutical روی همان برگه اعمال می‌شود؛ Role Lifepath و Medicine allocation همان Medtech هم اینجاست. تخصص، شریک، کلینیک، مشتریان و منبع تجهیزات دوباره تاس نمی‌خورند؛ بنابراین اطلاعات GM همیشه با BIO یکسان می‌ماند."
    GmTool.EXEC_ASSISTANT -> "دستیار Exec: Teamwork، Team Memberها (با ساخت NPC یک‌کلیکی)، منابع سازمانی، فشار شرکتی و صحنه‌های آماده — با همان قالب بقیه‌ی نقش‌ها."
    GmTool.LAWMAN_ASSISTANT -> "دستیار Lawman: توانایی Backup طبق Core (1d10 ≤ Rank، بعد 1d6 برای رسیدن، ۶ = یک رده بالاتر) + ساخت نیروهای پشتیبان به‌عنوان NPC و افزودن به نبرد، پرونده‌ها و صحنه‌های آماده."
    GmTool.FIXER_ASSISTANT -> "دستیار Fixer: Operator (Contacts & Clients، Reach، Haggle، Grease)، شبکه‌ی واقعی از برگه، کارها و صحنه‌های آماده."
    GmTool.MEDIA_ASSISTANT -> "این صفحه Role Lifepath و Credibility همان Media را مستقیم از Character Sheet می‌خواند. نوع رسانه، بستر انتشار، اخلاق و نوع Story دوباره تولید نمی‌شوند؛ Investigation Board نیز از همان Character ذخیره‌شده خوانده می‌شود."
    else -> "از این صفحه ابزار موردنیاز GM را انتخاب کن. دکمه i کنار هر بخش توضیح ساده همان ابزار را نشان می‌دهد."
}

@Composable
private fun ToolGrid(onOpen: (GmTool) -> Unit) {
    val tools = listOf(
        Triple(Icons.Default.Place, "MAP", "نقشه‌ی تعاملی نایت‌سیتی") to GmTool.MAP,
        Triple(Icons.Default.GridOn, "BATTLE MAP", "گرید، فاصله و توکن تاکتیکی") to GmTool.BATTLE_MAP,
        Triple(Icons.Default.MedicalServices, "CRITICAL INJURIES", "۲۲ جراحت بحرانی") to GmTool.INJURIES,
        Triple(Icons.Default.Timer, "CLOCKS", "ساعت‌های شمارش معکوس") to GmTool.CLOCKS,
        Triple(Icons.Default.Bolt, "QUICK COMBAT", "نبرد سریع & کثیف") to GmTool.COMBAT,
        Triple(Icons.Default.Shuffle, "ENCOUNTERS", "برخوردهای تصادفی") to GmTool.ENCOUNTER,
        Triple(Icons.Default.Cloud, "WEATHER", "آب‌وهوا") to GmTool.WEATHER,
        Triple(Icons.Default.Home, "LIFESTYLE", "خواب/لایف‌استایل/مسکن") to GmTool.LIFE,
        Triple(Icons.Default.Dns, "NET ARCH", "سازنده و ذخیره‌ی معماری نت") to GmTool.NET_ARCH,
        Triple(Icons.Default.ShoppingCart, "NIGHT MARKET", "ساخت بازار و موجودی تصادفی") to GmTool.NIGHT_MARKET,
        Triple(Icons.Default.Folder, "CAMPAIGN", "کمپین، Encounter و تاریخچه") to GmTool.CAMPAIGN,
        Triple(Icons.Default.AccountBalanceWallet, "ECONOMY", "هزینه، خدمات، فروش و Haggle") to GmTool.ECONOMY,
        Triple(Icons.Default.Groups, "ROLE HELPERS", "Nomad • Medtech • Media • Exec • Lawman • Fixer") to GmTool.ROLE_HELPERS,
        Triple(Icons.Default.Work, "GIGS", "کارهای انفرادی و سناریوهای کامل گروهی") to GmTool.SOLO_JOBS,
        Triple(Icons.Default.PersonAdd, "HIREABLE ALLIES", "NPC همراه در چهار Tier") to GmTool.HIREABLE_ALLIES
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text("ابزارهای گیم‌مستر", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Text("همه‌ی ابزارهای اجرای بازی، یک‌جا", color = Muted, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        }
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tools) { (data, target) ->
                val (icon, en, fa) = data
                ToolCell(icon, en, fa, toolHelp(target)) { onOpen(target) }
            }
        }
    }
}

@Composable
private fun ToolCell(icon: ImageVector, en: String, fa: String, help: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).background(CardBg, CutCornerShape(12.dp))
            .border(1.5.dp, Red.copy(alpha = 0.7f), CutCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = White, modifier = Modifier.size(36.dp))
            RuleInfoButton(en, help, Modifier.align(Alignment.TopEnd).padding(3.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            en, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                fa, color = Muted, fontSize = 10.sp, lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp),
                textAlign = TextAlign.Center, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
            )
        }
    }
}

// ================= نقشه‌ی آفلاین (همان MapApp تب Agent) =================
@Composable
private fun MapView() {
    // نقشه‌ی تعاملی آفلاینِ باندل‌شده (زوم/پان، شهر و ۸ منطقه، هات‌اسپات)
    MapApp()
}

// ================= جراحات بحرانی (سبک مرجع) =================
private fun injuryIcon(key: String): ImageVector = when (key) {
    "dismembered_arm" -> Icons.Default.Healing
    "dismembered_hand" -> Icons.Default.PanTool
    "collapsed_lung" -> Icons.Default.Bloodtype
    "broken_ribs" -> Icons.Default.GridView
    "broken_arm" -> Icons.Default.Healing
    "foreign_object_body" -> Icons.Default.Warning
    "broken_leg" -> Icons.Default.DirectionsWalk
    "torn_muscle" -> Icons.Default.FitnessCenter
    "spinal_injury" -> Icons.Default.LinearScale
    "crushed_fingers" -> Icons.Default.TouchApp
    "dismembered_leg" -> Icons.Default.AirlineSeatLegroomReduced
    "lost_eye" -> Icons.Default.VisibilityOff
    "brain_injury" -> Icons.Default.Psychology
    "damaged_eye" -> Icons.Default.Visibility
    "concussion" -> Icons.Default.BlurOn
    "broken_jaw" -> Icons.Default.SentimentDissatisfied
    "foreign_object_head" -> Icons.Default.WarningAmber
    "whiplash" -> Icons.Default.SwapVert
    "cracked_skull" -> Icons.Default.SentimentVeryDissatisfied
    "damaged_ear" -> Icons.Default.HearingDisabled
    "crushed_windpipe" -> Icons.Default.RecordVoiceOver
    "lost_ear" -> Icons.Default.HeadsetMic
    else -> Icons.Default.Warning
}

@Composable
private fun InjuryIconBox(key: String, size: Int = 64, onDark: Boolean = true) {
    val borderC = if (onDark) White else Red
    Box(
        Modifier.size(size.dp).clip(CutCornerShape(8.dp))
            .border(2.dp, borderC, CutCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(injuryIcon(key), contentDescription = null, tint = borderC, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun InjuriesList() {
    var filter by remember { mutableStateOf(0) } // 0=همه، 1=سر، 2=بدن
    var detail by remember { mutableStateOf<CriticalInjuries.Injury?>(null) }
    // عملیات قواعدیِ تاس‌دار در حالت «تاس دستی» باید بیرون از نخ اصلی اجرا شوند؛
    // DiceSource برای گرفتن عدد از GM نخ فراخوان را بلاک می‌کند و روی نخ اصلی این
    // ممکن نیست (پس ناچار به تصادفی می‌افتد و هشدار می‌دهد).
    val rulesScope = rememberCoroutineScope()
    fun rules(block: () -> Unit) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            rulesScope.launch(kotlinx.coroutines.Dispatchers.Default) { block() }
        } else block()
    }

    val list = when (filter) {
        1 -> CriticalInjuries.headInjuries
        2 -> CriticalInjuries.bodyInjuries
        else -> CriticalInjuries.all.sortedBy { it.enName }
    }

    fun rollAndOpen(body: Boolean) {
        val pool = if (body) CriticalInjuries.bodyInjuries else CriticalInjuries.headInjuries
        val value = com.cyberpunk.gmtool.data.DiceSource.roll(2, 6, "جدول جراحت بحرانی (2d6)").sum()
        // نتیجه‌ی 2d6 را به ردیفِ آن رول نزدیک می‌کنیم؛ اگر دقیقاً موجود نبود همان نزدیک‌ترین
        // اگر رول به ردیف آزادی نخورد (همه‌ی جراحت‌ها قبلاً خورده)، انتخاب هم باید
        // یک تاس واقعی باشد تا در حالت دستی بی‌صدا از خودِ برنامه درنیاید.
        detail = pool.firstOrNull { it.roll == value }
            ?: pool[com.cyberpunk.gmtool.data.DiceSource.rollOne(pool.size, "انتخاب جراحت (d${pool.size})") - 1]
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // فیلتر سر/بدن
            item {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("همه (۲۲)" to 0, "جراحات سر" to 1, "جراحات بدن" to 2).forEach { (label, v) ->
                            FilterChip(
                                selected = filter == v,
                                onClick = { filter = v },
                                label = { Text(gtr(label), color = if (filter == v) Black else White, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Red, containerColor = CardBg)
                            )
                        }
                    }
                }
            }
            items(list.size) { i ->
                val inj = list[i]
                InjuryRow(inj) { detail = inj }
            }
        }

        // دکمه‌های رول پایین (Body Injury / Head Injury)
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Black.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RollInjuryButton("جراحت بدن", Icons.Default.BrightnessLow) { rules { rollAndOpen(body = true) } }
            RollInjuryButton("جراحت سر", Icons.Default.BrightnessHigh) { rules { rollAndOpen(body = false) } }
        }
    }

    detail?.let { inj -> InjuryDetailDialog(inj) { detail = null } }
}

@Composable
private fun InjuryRow(inj: CriticalInjuries.Injury, onClick: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // نام (سمت راست)
            Column(Modifier.weight(1f)) {
                Text(inj.faName, color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(inj.enName), color = Muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right)
                }
            }
            Spacer(Modifier.width(12.dp))
            // آیکن (سمت چپ)
            InjuryIconBox(inj.key, size = 60)
        }
    }
}

@Composable
private fun RowScope.RollInjuryButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Red),
        shape = CutCornerShape(10.dp),
        modifier = Modifier.weight(1f).height(52.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Icon(Icons.Default.Casino, contentDescription = null, tint = Black, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(gtr(label), color = Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun InjuryDetailDialog(inj: CriticalInjuries.Injury, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Black),
            shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
                .border(2.dp, Red, CutCornerShape(14.dp))
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxSize().padding(22.dp)) {
                    // سربرگ: نام + آیکن
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(inj.faName, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(gtr(inj.enName), color = Red, fontSize = 15.sp,
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Right)
                            }
                            Text(if (inj.body) "جراحت بدن" else "جراحت سر",
                                color = Muted, fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Right)
                        }
                        Spacer(Modifier.width(14.dp))
                        InjuryIconBox(inj.key, size = 92)
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Red.copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))

                    LazyColumn(Modifier.weight(1f)) {
                        item { DetailBlock("اثر جراحت", "Injury Effect", inj.effectFa) }
                        item { DetailBlock("ترمیم سریع", "Quick Fix", inj.quickFixFa) }
                        item { DetailBlock("درمان", "Treatment", inj.treatmentFa, last = true) }
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        shape = CutCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("بستن", color = Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBlock(faLabel: String, enLabel: String, body: String, last: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(bottom = if (last) 8.dp else 18.dp)) {
        Text(gtr(faLabel), color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(gtr(enLabel), color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        }
        Spacer(Modifier.height(6.dp))
        Text(ltrWrap(body), color = White, fontSize = 15.sp, lineHeight = 26.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right,
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl))
    }
}

// ================= لیست تصادفی (برخورد/آب‌وهوا) =================
@Composable
private fun RandomList(title: String, options: List<String>) {
    var current by remember { mutableStateOf<String?>(null) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Casino, null, tint = Red, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(gtr(title), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(10.dp))
                .border(1.dp, Red.copy(alpha = 0.5f), CutCornerShape(10.dp)).padding(20.dp),
                contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        if (current != null) ltrWrap(current!!) else "برای رول دکمه را بزن",
                        color = if (current != null) White else Muted,
                        fontSize = 17.sp, lineHeight = 28.sp, textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { current = options.random() },
                colors = ButtonDefaults.buttonColors(containerColor = Red),
                shape = CutCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Default.Casino, null, tint = Black)
                Spacer(Modifier.width(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr("ROLL 1d%1s", options.size), color = Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}



@Composable
private fun WeatherTool() {
    var index by remember { mutableIntStateOf(-1) }
    val effects = listOf(
        "دید دور کمی مختل است؛ GM فقط وقتی Smog واقعاً روی تشخیص هدف یا رانندگی اثر دارد Modifier موقعیتی اعمال کند.",
        "سطوح خیس و باران اسیدی را در توصیف لحاظ کن؛ برای حضور کوتاه اثر عددی خودکار ندارد، اما تجهیزات/لباس نامناسب می‌تواند پیامد روایی داشته باشد.",
        "دید و شنیدن سخت‌تر است. GM می‌تواند برای Perception، تیراندازی دوربرد، رانندگی یا ارتباط صوتی در شرایط واقعاً نامناسب Modifier موقعیتی بدهد.",
        "اثر مکانیکی پیش‌فرض ندارد؛ گرما و رطوبت برای خستگی، استتار، تعقیب و فضای صحنه استفاده شود.",
        "باد می‌تواند روی اشیای سبک، دود، پهپادها و رانندگی در فضای باز اثر بگذارد؛ Modifier فقط وقتی شرایط صحنه توجیهش می‌کند.",
        "دید به‌شدت محدود است؛ فاصله تشخیص، Perception بصری، تیراندازی دوربرد و رانندگی سریع را GM متناسب با صحنه سخت‌تر کند.",
        "اثر مکانیکی پیش‌فرض ندارد؛ شرایط مناسب دید و سفر است مگر عامل دیگری در صحنه وجود داشته باشد."
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
            Icon(Icons.Default.Casino,null,tint=Red,modifier=Modifier.size(64.dp)); Spacer(Modifier.height(16.dp))
            Text("آب‌وهوای نایت‌سیتی",color=White,fontSize=20.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            if(index>=0) Column(Modifier.fillMaxWidth().background(CardBg,CutCornerShape(10.dp)).border(1.dp,Red.copy(.5f),CutCornerShape(10.dp)).padding(18.dp)) {
                Text(GmData.weather[index],color=White,fontSize=16.sp,lineHeight=25.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                Spacer(Modifier.height(12.dp)); Text("اثر در بازی / یادداشت GM",color=Red,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                Text(effects[index],color=Muted,fontSize=13.sp,lineHeight=22.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
            } else Text("برای تعیین آب‌وهوا تاس را بزن.",color=Muted)
            Spacer(Modifier.height(20.dp)); Button(onClick={index=(0 until GmData.weather.size).random()},colors=ButtonDefaults.buttonColors(containerColor=Red),shape=CutCornerShape(12.dp),modifier=Modifier.fillMaxWidth().height(54.dp)){Text(gtr("🎲  ROLL 1d7"),color=Black,fontWeight=FontWeight.Bold)}
        }
    }
}

// ================= برخوردهای تصادفی پیشرفته =================
@Composable
private fun RandomEncounterTool() {
    var highLevel by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<GmData.Encounter?>(null) }
    val pool = GmData.encounters.filter { it.highLevel == highLevel }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // جدول برخوردها ساخته‌ی این برنامه است، نه جدول رسمی Core.
                com.cyberpunk.gmtool.ui.components.FaText(
                    gtr("Homebrew — this table is app content, not a Core table."),
                    color = Color(0xFF9575CD), fontSize = 10.sp, lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !highLevel,
                        onClick = { highLevel = false; current = null },
                        label = { Text("خیابانی / روزمره") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red, containerColor = CardBg),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = highLevel,
                        onClick = { highLevel = true; current = null },
                        label = { Text("خطرناک / سطح بالا") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red, containerColor = CardBg),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "نحوه اجرا: ۱) نتیجه را با محله و داستان فعلی تطبیق بده. ۲) مشخص کن طرف مقابل چه می‌خواهد و آیا گفت‌وگو/فرار ممکن است. ۳) فقط اگر درگیری شکل گرفت NPC مناسب را وارد Combat کن. ۴) فرصت و پیامد پایین کارت را به‌عنوان سرنخ و Consequence استفاده کن. سطح خطر را با تعداد و قدرت Crew تنظیم کن؛ این جدول جایگزین Encounter Balance نیست.",
                    color = Muted, fontSize = 12.sp, lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
            }

            item {
                Button(
                    onClick = { current = pool.randomOrNull() },
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = CutCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Default.Casino, null, tint = Black)
                    Spacer(Modifier.width(8.dp))
                    Text(gtr("ROLL 1d20"), color = Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            current?.let { e ->
                item {
                    Column(
                        Modifier.fillMaxWidth().background(CardBg, CutCornerShape(12.dp))
                            .border(1.5.dp, Red, CutCornerShape(12.dp)).padding(16.dp)
                    ) {
                        Text(gtr(e.title), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Spacer(Modifier.height(8.dp))
                        Text("منبع / طرف‌ها: ${e.source}", color = Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text("سطح خطر: ${"★".repeat(e.threat)}${"☆".repeat(5 - e.threat)}", color = Red,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Right)
                        Spacer(Modifier.height(10.dp))
                        Text("فرصت / پاداش", color = Red, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(gtr(e.opportunity), color = White, lineHeight = 23.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Spacer(Modifier.height(8.dp))
                        Text("پیامد احتمالی", color = Red, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(gtr(e.consequence), color = White, lineHeight = 23.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                }
            }

            item {
                Text("جدول کامل", color = Red, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Right)
            }
            items(pool.size) { i ->
                val e = pool[i]
                Row(
                    Modifier.fillMaxWidth().clickable { current = e }
                        .background(CardBg, CutCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${i + 1}. ${e.title}", color = White, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(gtr(e.source), color = Muted, fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                    Text("★".repeat(e.threat), color = Red, fontSize = 11.sp)
                }
            }
        }
    }
}

// ================= NET Architecture Generator =================
private enum class NetDifficulty(val label: String, val dv: Int, val fairInterface: Int) {
    BASIC("Basic", 6, 2), STANDARD("Standard", 8, 4), UNCOMMON("Uncommon", 10, 6), ADVANCED("Advanced", 12, 8)
}

private data class NetArchitecture(
    val id: Long,
    val name: String,
    val difficulty: NetDifficulty,
    val floors: List<String>,
    val branches: Int,
    val createdAt: Long
)

private data class NetRunState(
    val architectureId: Long,
    val interfaceRank: Int = 4,
    val currentFloor: Int = 0,
    val round: Int = 1,
    val actionsRemaining: Int = 3,
    val jackedIn: Boolean = false,
    val unlockedPasswords: Set<Int> = emptySet(),
    val identifiedFiles: Set<Int> = emptySet(),
    val copiedFiles: Set<Int> = emptySet(),
    val controlledNodes: Set<Int> = emptySet(),
    val iceRez: Map<String, Int> = emptyMap(),
    val encounteredIce: Set<String> = emptySet(),
    val slidIce: Set<String> = emptySet(),
    val activePrograms: Set<String> = emptySet(),
    val usedProgramsThisRound: Set<String> = emptySet(),
    val spentProgramsThisRun: Set<String> = emptySet(),
    val programRez: Map<String, Int> = emptyMap(),
    val backupSavedPrograms: Set<String> = emptySet(),
    val nextTurnActionPenalty: Int = 0,
    val safeProgressBlockedUntilRound: Int = 0,
    val cloakScore: Int = 0,
    val pathfinderScore: Int = 0,
    val virusProgress: Int = 0,
    val virusRequired: Int = 0,
    val virusDv: Int = 0,
    val slideUsedThisTurn: Boolean = false,
    val lastResult: String = "",
    val notes: String = ""
)

private data class BlackIceStat(val per: Int, val spd: Int, val atk: Int, val def: Int, val rez: Int, val effect: String)

private val coreBlackIce = mapOf(
    "Asp" to BlackIceStat(4,6,2,2,15,"Destroy one random installed Program."),
    "Giant" to BlackIceStat(2,2,8,4,25,"3d6 brain damage and unsafe Jack Out."),
    "Hellhound" to BlackIceStat(6,6,6,2,20,"2d6 brain damage; may ignite deck/clothing."),
    "Kraken" to BlackIceStat(6,2,8,4,30,"3d6 brain damage; cannot safely Jack Out/progress until end of next Turn."),
    "Liche" to BlackIceStat(8,2,6,2,25,"INT, REF and DEX each -1d6 for one hour, minimum 1."),
    "Raven" to BlackIceStat(6,4,4,2,15,"Derezz one random Rezzed Defender, then 1d6 brain damage."),
    "Scorpion" to BlackIceStat(2,6,2,2,15,"MOVE -1d6 for one hour, minimum 1."),
    "Skunk" to BlackIceStat(2,4,4,2,10,"-2 to Slide Checks while affected; multiple Skunks stack."),
    "Wisp" to BlackIceStat(4,4,4,2,15,"1d6 brain damage and -1 NET Action next Turn, minimum 2."),
    "Dragon" to BlackIceStat(6,4,6,6,30,"6d6 damage to a Program; destroys it if enough to Derezz."),
    "Killer" to BlackIceStat(4,8,6,2,20,"4d6 damage to a Program; destroys it if enough to Derezz."),
    "Sabertooth" to BlackIceStat(8,6,6,2,25,"6d6 damage to a Program; destroys it if enough to Derezz.")
)

private fun netActionsForInterface(rank: Int): Int = when (rank.coerceIn(1,10)) {
    in 1..3 -> 2
    in 4..6 -> 3
    in 7..9 -> 4
    else -> 5
}

private fun floorDv(floor: String): Int? = Regex("DV(6|8|10|12)", RegexOption.IGNORE_CASE).find(floor)?.groupValues?.get(1)?.toIntOrNull()

private fun floorIceKeys(floorIndex: Int, floor: String): List<Pair<String, BlackIceStat>> {
    val out = mutableListOf<Pair<String, BlackIceStat>>()
    coreBlackIce.forEach { (name, stat) ->
        val match = Regex("\\b${Regex.escape(name)}(?:\\s*[×x]\\s*(\\d+))?", RegexOption.IGNORE_CASE).find(floor)
        if (match != null) {
            val count = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 1
            repeat(count) { i -> out += "${floorIndex}:${name}:${i}" to stat }
        }
    }
    return out
}

/**
 * کمک‌تابع داخلی تولید Net Architecture (مثل 3d6 طبقه).
 * تاس از DiceSource می‌آید تا در حالت «تاس دستی» GM آن را بریزد؛ قبلاً مستقیم
 * `(1..sides).random()` بود و در حالت دستی بی‌صدا عدد ساختگی می‌ساخت.
 */
private fun rollDice(count: Int, sides: Int): Int =
    com.cyberpunk.gmtool.data.DiceSource.roll(count, sides, "Net Architecture — ${count}d$sides").sum()

private fun netBodyFloor(diff: NetDifficulty, roll: Int): String {
    val basic = mapOf(3 to "Hellhound", 4 to "Sabertooth", 5 to "Raven ×2", 6 to "Hellhound", 7 to "Wisp", 8 to "Raven",
        9 to "Password DV6", 10 to "File DV6", 11 to "Control Node DV6", 12 to "Password DV6", 13 to "Skunk", 14 to "Asp",
        15 to "Scorpion", 16 to "Killer + Skunk", 17 to "Wisp ×3", 18 to "Liche")
    val standard = mapOf(3 to "Hellhound ×2", 4 to "Hellhound + Killer", 5 to "Skunk ×2", 6 to "Sabertooth", 7 to "Scorpion", 8 to "Hellhound",
        9 to "Password DV8", 10 to "File DV8", 11 to "Control Node DV8", 12 to "Password DV8", 13 to "Asp", 14 to "Killer",
        15 to "Liche", 16 to "Asp", 17 to "Raven ×3", 18 to "Liche + Raven")
    val uncommon = mapOf(3 to "Kraken", 4 to "Hellhound + Scorpion", 5 to "Hellhound + Killer", 6 to "Raven ×2", 7 to "Sabertooth", 8 to "Hellhound",
        9 to "Password DV10", 10 to "File DV10", 11 to "Control Node DV10", 12 to "Password DV10", 13 to "Killer", 14 to "Liche",
        15 to "Dragon", 16 to "Asp + Raven", 17 to "Dragon + Wisp", 18 to "Giant")
    val advanced = mapOf(3 to "Hellhound ×3", 4 to "Asp ×2", 5 to "Hellhound + Liche", 6 to "Wisp ×3", 7 to "Hellhound + Sabertooth", 8 to "Kraken",
        9 to "Password DV12", 10 to "File DV12", 11 to "Control Node DV12", 12 to "Password DV12", 13 to "Giant", 14 to "Dragon",
        15 to "Killer + Scorpion", 16 to "Kraken", 17 to "Raven + Wisp + Hellhound", 18 to "Dragon ×2")
    return when (diff) { NetDifficulty.BASIC -> basic; NetDifficulty.STANDARD -> standard; NetDifficulty.UNCOMMON -> uncommon; NetDifficulty.ADVANCED -> advanced }[roll] ?: ("File DV" + diff.dv)
}

private fun generateNetArchitecture(diff: NetDifficulty, name: String): NetArchitecture {
    val floorCount = rollDice(3, 6)
    var branches = 0
    while (com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "Net Architecture — شاخه (d10)") >= 7 && branches < 3) branches++
    val lobby = listOf("File DV6", "Password DV6", "Password DV8", "Skunk", "Wisp", "Killer")
    val floors = MutableList(floorCount) { index ->
        if (index < 2) lobby.random() else netBodyFloor(diff, rollDice(3, 6))
    }
    return NetArchitecture(System.currentTimeMillis(), name.ifBlank { "NET " + System.currentTimeMillis().toString().takeLast(4) }, diff, floors, branches, System.currentTimeMillis())
}

@Composable
private fun NetArchitectureTool(viewModel: CharacterViewModel? = null) {
    // عملیات قواعدیِ تاس‌دار در حالت «تاس دستی» باید بیرون از نخ اصلی اجرا شوند؛
    // DiceSource برای گرفتن عدد از GM نخ فراخوان را بلاک می‌کند و روی نخ اصلی این
    // ممکن نیست (پس ناچار به تصادفی می‌افتد و هشدار می‌دهد).
    val rulesScope = rememberCoroutineScope()
    fun rules(block: () -> Unit) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            rulesScope.launch(kotlinx.coroutines.Dispatchers.Default) { block() }
        } else block()
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gm_net_architectures", android.content.Context.MODE_PRIVATE) }
    val saved = remember {
        val list = mutableStateListOf<NetArchitecture>()
        runCatching {
            val arr = org.json.JSONArray(prefs.getString("items", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val floors = o.getJSONArray("floors")
                list.add(NetArchitecture(
                    o.getLong("id"), o.getString("name"), NetDifficulty.valueOf(o.getString("difficulty")),
                    List(floors.length()) { j -> floors.getString(j) }, o.optInt("branches", 0), o.optLong("createdAt", o.getLong("id"))
                ))
            }
        }
        list
    }
    var difficulty by remember { mutableStateOf(NetDifficulty.STANDARD) }
    var name by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<NetArchitecture?>(saved.firstOrNull()) }
    var runState by remember { mutableStateOf<NetRunState?>(null) }
    var showRun by remember { mutableStateOf(false) }
    val netCharacters = viewModel?.characters?.collectAsState()?.value.orEmpty().filter { it.isAlly }
    var linkedNetrunnerId by remember { mutableStateOf<Int?>(null) }
    var linkedDeckId by remember { mutableStateOf<String?>(null) }
    var netItemDetailLabel by remember { mutableStateOf<String?>(null) }
    var enemyInterface by remember { mutableStateOf(4) }
    var enemyAttackerProgram by remember { mutableStateOf("Hellbolt") }

    fun loadRun(a: NetArchitecture): NetRunState {
        val raw = prefs.getString("run_${a.id}", null) ?: return NetRunState(a.id)
        return runCatching {
            val o = org.json.JSONObject(raw)
            fun intSet(key: String): Set<Int> {
                val arr = o.optJSONArray(key) ?: return emptySet()
                return (0 until arr.length()).map { arr.optInt(it) }.toSet()
            }
            val rezObj = o.optJSONObject("iceRez")
            val rez = buildMap<String, Int> {
                if (rezObj != null) {
                    val keys = rezObj.keys()
                    while (keys.hasNext()) { val k = keys.next(); put(k, rezObj.optInt(k)) }
                }
            }
            val programRezObj = o.optJSONObject("programRez")
            val programRez = buildMap<String, Int> {
                if (programRezObj != null) { val keys = programRezObj.keys(); while (keys.hasNext()) { val k = keys.next(); put(k, programRezObj.optInt(k)) } }
            }
            NetRunState(
                architectureId = a.id,
                interfaceRank = o.optInt("interfaceRank", 4).coerceIn(1,10),
                currentFloor = o.optInt("currentFloor", 0).coerceIn(0, (a.floors.size - 1).coerceAtLeast(0)),
                round = o.optInt("round", 1).coerceAtLeast(1),
                actionsRemaining = o.optInt("actionsRemaining", 3).coerceAtLeast(0),
                jackedIn = o.optBoolean("jackedIn", false),
                unlockedPasswords = intSet("unlockedPasswords"),
                identifiedFiles = intSet("identifiedFiles"),
                copiedFiles = intSet("copiedFiles"),
                controlledNodes = intSet("controlledNodes"),
                iceRez = rez,
                encounteredIce = intSet("encounteredIceLegacy").map { it.toString() }.toSet() + (o.optJSONArray("encounteredIce")?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.toSet() } ?: emptySet()),
                slidIce = o.optJSONArray("slidIce")?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.toSet() } ?: emptySet(),
                activePrograms = o.optJSONArray("activePrograms")?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.toSet() } ?: emptySet(),
                usedProgramsThisRound = o.optJSONArray("usedProgramsThisRound")?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.toSet() } ?: emptySet(),
                spentProgramsThisRun = o.optJSONArray("spentProgramsThisRun")?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.toSet() } ?: emptySet(),
                programRez = programRez,
                backupSavedPrograms = o.optJSONArray("backupSavedPrograms")?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.toSet() } ?: emptySet(),
                nextTurnActionPenalty = o.optInt("nextTurnActionPenalty", 0).coerceAtLeast(0),
                safeProgressBlockedUntilRound = o.optInt("safeProgressBlockedUntilRound", 0).coerceAtLeast(0),
                cloakScore = o.optInt("cloakScore", 0), pathfinderScore = o.optInt("pathfinderScore", 0),
                virusProgress = o.optInt("virusProgress", 0), virusRequired = o.optInt("virusRequired", 0), virusDv = o.optInt("virusDv", 0),
                slideUsedThisTurn = o.optBoolean("slideUsed", false),
                lastResult = o.optString("lastResult", ""),
                notes = o.optString("notes", "")
            )
        }.getOrElse { NetRunState(a.id) }
    }

    fun saveRun(st: NetRunState) {
        fun arr(values: Set<Int>) = org.json.JSONArray().apply { values.sorted().forEach { put(it) } }
        fun strArr(values: Set<String>) = org.json.JSONArray().apply { values.sorted().forEach { put(it) } }
        val rez = org.json.JSONObject().apply { st.iceRez.forEach { (k,v) -> put(k,v) } }
        val programRez = org.json.JSONObject().apply { st.programRez.forEach { (k,v) -> put(k,v) } }
        val o = org.json.JSONObject()
            .put("interfaceRank", st.interfaceRank).put("currentFloor", st.currentFloor).put("round", st.round)
            .put("actionsRemaining", st.actionsRemaining).put("jackedIn", st.jackedIn)
            .put("unlockedPasswords", arr(st.unlockedPasswords)).put("identifiedFiles", arr(st.identifiedFiles))
            .put("copiedFiles", arr(st.copiedFiles)).put("controlledNodes", arr(st.controlledNodes))
            .put("iceRez", rez).put("encounteredIce", strArr(st.encounteredIce)).put("slidIce", strArr(st.slidIce))
            .put("activePrograms", strArr(st.activePrograms)).put("usedProgramsThisRound", strArr(st.usedProgramsThisRound)).put("spentProgramsThisRun", strArr(st.spentProgramsThisRun))
            .put("programRez", programRez).put("backupSavedPrograms", strArr(st.backupSavedPrograms))
            .put("nextTurnActionPenalty", st.nextTurnActionPenalty).put("safeProgressBlockedUntilRound", st.safeProgressBlockedUntilRound)
            .put("cloakScore", st.cloakScore).put("pathfinderScore", st.pathfinderScore)
            .put("virusProgress", st.virusProgress).put("virusRequired", st.virusRequired).put("virusDv", st.virusDv)
            .put("slideUsed", st.slideUsedThisTurn).put("lastResult", st.lastResult).put("notes", st.notes)
        prefs.edit().putString("run_${st.architectureId}", o.toString()).apply()
    }

    fun updateRun(transform: (NetRunState) -> NetRunState) {
        val st = runState ?: return
        val next = transform(st)
        runState = next
        saveRun(next)
    }

    fun persist() {
        val arr = org.json.JSONArray()
        saved.forEach { a ->
            arr.put(org.json.JSONObject().put("id", a.id).put("name", a.name).put("difficulty", a.difficulty.name)
                .put("branches", a.branches).put("createdAt", a.createdAt)
                .put("floors", org.json.JSONArray(a.floors)))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("سازنده‌ی NET Architecture", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Text("Core: تعداد Floor با 3d6 ساخته می‌شود؛ دو Floor اول Lobby هستند و بقیه بر اساس Difficulty پر می‌شوند.",
                    color = Muted, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            }
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("نام شبکه / مکان") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Red, unfocusedBorderColor = Muted,
                        focusedTextColor = White, unfocusedTextColor = White, focusedLabelColor = Red, unfocusedLabelColor = Muted)
                )
            }
            item {
                Text(gtr("Difficulty"), color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NetDifficulty.entries.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { d ->
                                FilterChip(
                                    selected = difficulty == d, onClick = { difficulty = d },
                                    label = { Text(gtr("%1s • DV%2s • Interface %3s+", d.label, d.dv, d.fairInterface)) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red, containerColor = CardBg),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        rules {
                            val a = generateNetArchitecture(difficulty, name.trim())
                            saved.add(0, a); current = a; name = ""; persist()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red), shape = CutCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(gtr("GENERATE & SAVE"), color = Black, fontWeight = FontWeight.Bold) }
            }

            current?.let { a ->
                item {
                    Column(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(10.dp)).border(1.5.dp, Red, CutCornerShape(10.dp)).padding(14.dp)) {
                        Text(gtr(a.name), color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(gtr("%1s • %2s Floors • %3s Branch(es)", a.difficulty.label, a.floors.size, a.branches), color = Red,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        if (a.branches > 0) Text("Branchها بعد از Floor 2 شروع می‌شوند؛ تقسیم Floorها بین شاخه‌ها را مطابق فضای Meatspace تنظیم کن.",
                            color = Muted, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Right)
                        Spacer(Modifier.height(8.dp))
                        a.floors.forEachIndexed { i, floor ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}", color = Black, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.size(30.dp).background(Red, CutCornerShape(5.dp)).wrapContentSize(Alignment.Center))
                                Spacer(Modifier.width(8.dp))
                                // گزارش تست ۵.۳: جزئیات هر Floor (DV و ICEهایش با دکمه‌ی i)
                                // همان‌جا در فهرست معماری، بدون شروع Run دیده می‌شود.
                                Column(Modifier.weight(1f)) {
                                    Text(gtr(floor), color = White, textAlign = TextAlign.Left)
                                    val floorIceNames = floorIceKeys(i, floor).map { (k, _) -> k.split(":").getOrElse(1) { "ICE" } }
                                    val floorDetail = buildList {
                                        floorDv(floor)?.let { add("DV$it") }
                                        if (floorIceNames.isNotEmpty()) add("ICE: " + floorIceNames.joinToString(", "))
                                    }
                                    if (floorDetail.isNotEmpty()) Text(floorDetail.joinToString(" • "), color = Muted, fontSize = 10.sp, textAlign = TextAlign.Left)
                                    if (floorIceNames.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        floorIceNames.forEach { iceN ->
                                            TextButton(onClick = { netItemDetailLabel = iceN }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                                Text("$iceN  i", color = Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            val loaded = loadRun(a)
                            val initialIce = buildMap<String, Int> {
                                a.floors.forEachIndexed { index, floor -> floorIceKeys(index, floor).forEach { (key, stat) -> put(key, loaded.iceRez[key] ?: stat.rez) } }
                            }
                            val ready = loaded.copy(iceRez = initialIce)
                            runState = ready; saveRun(ready); showRun = true
                        }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) {
                            Text(gtr("START / RESUME NETRUN"), color = Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showRun && runState?.architectureId == a.id) {
                    item {
                        val st = runState ?: return@item
                        // گزارش تست ۵.۲: اگر دقیقاً یک Netrunner در فهرست باشد، خودکار
                        // Linked می‌شود (همراه با اولین Cyberdeck و Interface برابر رنکش).
                        // قبلاً تا دستی انتخاب نمی‌کردی، خسارت مغزی ICE TURN بی‌صدا روی
                        // هیچ شخصیتی اعمال نمی‌شد.
                        LaunchedEffect(st.architectureId, netCharacters.map { it.id }.joinToString(",")) {
                            if (netCharacters.none { it.id == linkedNetrunnerId }) {
                                val runners = netCharacters.filter { it.role.equals("Netrunner", true) }
                                if (runners.size == 1) {
                                    val c = runners[0]
                                    linkedNetrunnerId = c.id
                                    linkedDeckId = c.inventory.firstOrNull { it.category.equals("Gear", true) && it.name.contains("Cyberdeck", true) }?.instanceId
                                    updateRun { it.copy(interfaceRank = c.roleRank.coerceIn(1, 10), actionsRemaining = netActionsForInterface(c.roleRank)) }
                                }
                            }
                        }
                        val linkedNetrunner = netCharacters.firstOrNull { it.id == linkedNetrunnerId }
                        val linkedDeck = linkedNetrunner?.inventory?.firstOrNull { it.instanceId == linkedDeckId && it.category.equals("Gear", true) && it.name.contains("Cyberdeck", true) }
                        val linkedDeckItems = if (linkedDeck != null) linkedNetrunner.inventory.filter { it.installedIn == linkedDeck.instanceId } else emptyList()
                        val loadedPrograms = linkedDeckItems.filter { it.category.equals("Programs", true) }.map { it.name }
                        val loadedHardware = linkedDeckItems.filter { it.category.equals("Hardware", true) }.map { it.name }
                        val activeDeckCapacity = linkedDeck?.let { com.cyberpunk.gmtool.data.EquipmentUseRules.deckCapacity(it.name) } ?: 0
                        val activeDeckUsed = linkedDeck?.let { com.cyberpunk.gmtool.data.EquipmentUseRules.deckSlotsUsed(linkedNetrunner!!.inventory, it.instanceId) } ?: 0
                        val floorIndex = st.currentFloor.coerceIn(0, (a.floors.size - 1).coerceAtLeast(0))
                        val floor = a.floors.getOrElse(floorIndex) { "—" }
                        val maxActions = netActionsForInterface(st.interfaceRank)
                        val dv = floorDv(floor)
                        val ice = floorIceKeys(floorIndex, floor)
                        val liveIce = ice.filter { (key, _) -> (st.iceRez[key] ?: 0) > 0 }
                        Column(Modifier.fillMaxWidth().background(Color(0xFF121212), CutCornerShape(12.dp)).border(2.dp, Red, CutCornerShape(12.dp)).padding(14.dp)) {
                            Text(gtr("NETRUN SESSION"), color = Red, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                            if (viewModel != null && netCharacters.isNotEmpty()) {
                                Text(gtr("LINKED NETRUNNER"), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    netCharacters.filter { it.role.equals("Netrunner", true) }.take(4).forEach { c ->
                                        FilterChip(selected = linkedNetrunnerId == c.id, onClick = {
                                            linkedNetrunnerId = c.id
                                            val firstDeck = c.inventory.firstOrNull { it.category.equals("Gear", true) && it.name.contains("Cyberdeck", true) }
                                            linkedDeckId = firstDeck?.instanceId
                                            updateRun { it.copy(interfaceRank = c.roleRank.coerceIn(1,10), actionsRemaining = netActionsForInterface(c.roleRank)) }
                                        }, label = { Text(c.name.take(10), fontSize = 9.sp) })
                                    }
                                }
                                linkedNetrunner?.let { runner ->
                                    val decks = runner.inventory.filter { it.category.equals("Gear", true) && it.name.contains("Cyberdeck", true) }
                                    if (decks.isEmpty()) Text("این Netrunner Cyberdeck در Inventory ندارد.", color = Red, fontSize = 10.sp)
                                    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        decks.take(3).forEach { deck ->
                                            FilterChip(selected = linkedDeckId == deck.instanceId, onClick = { linkedDeckId = deck.instanceId }, label = { Text(deck.name.replace("Cyberdeck", "Deck").take(18), fontSize = 8.sp) })
                                        }
                                    }
                                }
                                if (linkedDeck != null) {
                                    val range = if (loadedHardware.any { it.equals("Range Upgrade", true) }) 8 else 6
                                    Text(gtr("%1s • Slots %2s/%3s • Access Point range %4sm/yd", linkedDeck.name, activeDeckUsed, activeDeckCapacity, range), color = Red, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                                    if (loadedHardware.isNotEmpty()) {
                                        Text(gtr("Hardware"), color = Muted, fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            loadedHardware.take(5).forEach { hw ->
                                                TextButton(onClick = { netItemDetailLabel = hw }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text(hw.take(16), color = Red, fontSize = 8.sp) }
                                            }
                                        }
                                    }
                                    if (loadedPrograms.isNotEmpty()) {
                                        Text(gtr("Programs"), color = Muted, fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            loadedPrograms.take(5).forEach { pr ->
                                                TextButton(onClick = { netItemDetailLabel = pr }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text(pr.take(16), color = Red, fontSize = 8.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                            Text(gtr("Round %1s • Floor %2s/%3s • %4s", st.round, floorIndex + 1, a.floors.size, if (st.jackedIn) gtr("JACKED IN") else gtr("OFFLINE")), color = White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { updateRun { it.copy(interfaceRank = (it.interfaceRank - 1).coerceAtLeast(1), actionsRemaining = minOf(it.actionsRemaining, netActionsForInterface((it.interfaceRank - 1).coerceAtLeast(1))) ) } }) { Text("−", color = Red) }
                                Text(gtr("Interface %1s • %2s NET Actions/Turn", st.interfaceRank, maxActions), color = White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                OutlinedButton(onClick = { updateRun { it.copy(interfaceRank = (it.interfaceRank + 1).coerceAtMost(10)) } }) { Text("+", color = Red) }
                            }
                            Text(gtr("NET Actions remaining: %1s/%2s", st.actionsRemaining, maxActions), color = if (st.actionsRemaining > 0) Red else Muted, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                            // گزارش تست ۵.۲: نتیجه‌ی آخرین اقدام نزدیک همان دکمه‌های بالا
                            // (Jack In، Pathfinder، Cloak، Activate…) هم نمایش داده می‌شود،
                            // نه فقط ته پنل.
                            if (st.lastResult.isNotBlank()) Text(gtr(st.lastResult), color = White, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth().background(CardBg, CutCornerShape(6.dp)).padding(8.dp), textAlign = TextAlign.Right)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { updateRun { cur ->
                                    val nextActions = (netActionsForInterface(cur.interfaceRank) - cur.nextTurnActionPenalty).coerceAtLeast(2)
                                    cur.copy(round = cur.round + 1, actionsRemaining = nextActions, nextTurnActionPenalty = 0, slideUsedThisTurn = false, usedProgramsThisRound = emptySet(), lastResult = gtr("New NET Turn • %1s NET Actions", nextActions))
                                } }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.weight(1f)) { Text(gtr("NEW NET TURN"), color = Black, fontSize = 11.sp) }
                                OutlinedButton(onClick = {
                                    if (!st.jackedIn && st.actionsRemaining > 0) updateRun { it.copy(jackedIn = true, actionsRemaining = it.actionsRemaining - 1, lastResult = "Jack In: 1 NET Action used.") }
                                    else if (st.jackedIn && st.actionsRemaining > 0) {
                                        if (st.round <= st.safeProgressBlockedUntilRound) updateRun { it.copy(lastResult = "Safe Jack Out فعلاً توسط اثر NET/ICE مسدود است؛ Unsafe Jack Out هنوز ممکن است.") }
                                        else {
                                            val freshIce = buildMap<String, Int> { a.floors.forEachIndexed { index, f -> floorIceKeys(index, f).forEach { (key, stat) -> put(key, stat.rez) } } }
                                            updateRun { it.copy(jackedIn = false, currentFloor = 0, actionsRemaining = it.actionsRemaining - 1, unlockedPasswords = emptySet(), identifiedFiles = emptySet(), controlledNodes = emptySet(), iceRez = freshIce, encounteredIce = emptySet(), slidIce = emptySet(), activePrograms = emptySet(), usedProgramsThisRound = emptySet(), spentProgramsThisRun = emptySet(), programRez = emptyMap(), backupSavedPrograms = emptySet(), nextTurnActionPenalty = 0, safeProgressBlockedUntilRound = 0, cloakScore = 0, pathfinderScore = 0, slideUsedThisTurn = false, lastResult = "Safe Jack Out: Architecture defenses reset; copied Files remain saved.") }
                                        }
                                    }
                                }, enabled = st.actionsRemaining > 0, modifier = Modifier.weight(1f)) { Text(if (st.jackedIn) "SAFE JACK OUT" else "JACK IN", color = Red, fontSize = 11.sp) }
                            }
                            Spacer(Modifier.height(10.dp))
                            Column(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(8.dp)).padding(12.dp)) {
                                Text(gtr("FLOOR %1s", floorIndex + 1), color = Red, fontWeight = FontWeight.Bold)
                                Text(gtr(floor), color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (dv != null) Text(gtr("DV %1s", dv), color = Muted)
                                if (floor.contains("Password", true)) Text(if (floorIndex in st.unlockedPasswords) "PASSWORD: OPEN" else "PASSWORD: BLOCKING PROGRESS", color = if (floorIndex in st.unlockedPasswords) White else Red, fontSize = 12.sp)
                                if (floor.contains("File", true)) Text(gtr("FILE: %1s%2s", if (floorIndex in st.identifiedFiles) gtr("IDENTIFIED") else gtr("UNIDENTIFIED"), if (floorIndex in st.copiedFiles) gtr(" • COPIED") else ""), color = Muted, fontSize = 12.sp)
                                if (floor.contains("Control Node", true)) Text(gtr("CONTROL NODE: %1s", if (floorIndex in st.controlledNodes) gtr("CONTROLLED") else gtr("UNCONTROLLED")), color = Muted, fontSize = 12.sp)
                            }
                            if (dv != null && st.jackedIn) {
                                Spacer(Modifier.height(6.dp))
                                Button(onClick = {
                                    if (st.actionsRemaining <= 0) return@Button
                                    val die = com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie
                                    val ability = when { floor.contains("Password", true) -> "Backdoor"; floor.contains("Control Node", true) -> "Control"; floor.contains("File", true) -> "Eye-Dee"; else -> "Interface" }
                                    val programBonus = if (ability == "Backdoor" && "Worm" in st.activePrograms) 2 else 0
                                    val total = st.interfaceRank + die + programBonus; val ok = total > dv
                                    updateRun { cur ->
                                        var next = cur.copy(actionsRemaining = cur.actionsRemaining - 1, lastResult = gtr("%1s: Interface %2s + d10 %3s + Program %4s = %5s vs DV%6s → %7s", ability, cur.interfaceRank, die, programBonus, total, dv, if (ok) gtr("SUCCESS") else gtr("FAIL")))
                                        if (ok && floor.contains("Password", true)) next = next.copy(unlockedPasswords = next.unlockedPasswords + floorIndex)
                                        if (ok && floor.contains("File", true)) next = next.copy(identifiedFiles = next.identifiedFiles + floorIndex)
                                        if (ok && floor.contains("Control Node", true)) next = next.copy(controlledNodes = next.controlledNodes + floorIndex)
                                        next
                                    }
                                }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) { Text(gtr("ROLL %1s", when { floor.contains("Password",true)->gtr("BACKDOOR"); floor.contains("Control Node",true)->gtr("CONTROL"); floor.contains("File",true)->gtr("EYE-DEE"); else->"INTERFACE" }), color = Black) }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    RuleInfoButton(when {
                                        floor.contains("Password", true) -> "net.backdoor"
                                        floor.contains("Control Node", true) -> "net.control"
                                        floor.contains("File", true) -> "net.eye_dee"
                                        else -> "net.interface_check"
                                    })
                                }
                                if (floor.contains("File", true) && floorIndex in st.identifiedFiles) {
                                    OutlinedButton(onClick = { updateRun { it.copy(copiedFiles = it.copiedFiles + floorIndex, lastResult = "File copied to Cyberdeck — no NET Action required.") } }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) { Text(gtr("COPY FILE — FREE"), color = Red) }
                                }
                            }
                            if (st.jackedIn) {
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(gtr("INTERFACE / PROGRAMS"), color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    RuleInfoButton("net.net_actions")
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    OutlinedButton(onClick = {
                                        if (st.actionsRemaining <= 0) return@OutlinedButton
                                        val die=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val bonus=if("See Ya" in st.activePrograms) 2 else 0; val score=st.interfaceRank+die+bonus
                                        updateRun { it.copy(actionsRemaining=it.actionsRemaining-1, pathfinderScore=score, lastResult=gtr("PATHFINDER: Interface %1s + d10 %2s + booster %3s = %4s. GM reveals floors up to this Check / first stronger obstruction.", st.interfaceRank, die, bonus, score)) }
                                    }, enabled=st.actionsRemaining>0, modifier=Modifier.weight(1f)) { Text(gtr("PATHFINDER"), color=Red, fontSize=9.sp) }
                                    OutlinedButton(onClick = {
                                        if (st.actionsRemaining <= 0) return@OutlinedButton
                                        val die=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val bonus=if("Eraser" in st.activePrograms) 2 else 0; val score=st.interfaceRank+die+bonus
                                        updateRun { it.copy(actionsRemaining=it.actionsRemaining-1, cloakScore=score, lastResult=gtr("CLOAK: %1s. Enemy Pathfinder must beat this to discover your actions/Virus.", score)) }
                                    }, enabled=st.actionsRemaining>0, modifier=Modifier.weight(1f)) { Text(gtr("CLOAK"), color=Red, fontSize=9.sp) }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                    Text(gtr("Pathfinder"), color = Muted, fontSize = 9.sp); RuleInfoButton("net.pathfinder")
                                    Text(gtr("Cloak"), color = Muted, fontSize = 9.sp); RuleInfoButton("net.cloak")
                                }
                                val availablePrograms = if (linkedDeck != null) loadedPrograms else listOf("Eraser","See Ya","Speedy Gonzalvez","Worm","Armor","Flak","Shield")
                                val rezzablePrograms = availablePrograms.filter { name ->
                                    val d = com.cyberpunk.gmtool.data.CoreItemReference.resolve(name, "Programs")
                                    d?.programClass?.contains("Booster", true) == true || d?.programClass?.contains("Defender", true) == true
                                }
                                if (linkedDeck != null && rezzablePrograms.isEmpty()) Text("هیچ Booster/Defender روی این Deck Load نشده است.", color=Muted, fontSize=10.sp)
                                rezzablePrograms.forEach { program ->
                                    val active = program in st.activePrograms
                                    // گزارش تست ۵.۳: دکمه‌ی «i» آشکار کنار هر Program؛ قبلاً فقط
                                    // خودِ متن دکمه کلیک‌پذیر بود و کسی پیدایش نمی‌کرد.
                                    Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(onClick = {
                                        if (st.actionsRemaining <= 0) return@OutlinedButton
                                        val oncePerRun = program in setOf("Armor", "Flak", "Shield")
                                        updateRun { cur -> cur.copy(
                                            actionsRemaining=cur.actionsRemaining-1,
                                            activePrograms=if(active) cur.activePrograms-program else cur.activePrograms+program,
                                            programRez=if(!active && program !in cur.programRez) cur.programRez + (program to (com.cyberpunk.gmtool.data.CoreItemReference.resolve(program, "Programs")?.rez ?: 7)) else cur.programRez,
                                            usedProgramsThisRound=if(active) cur.usedProgramsThisRound else cur.usedProgramsThisRound+program,
                                            spentProgramsThisRun=if(!active && oncePerRun) cur.spentProgramsThisRun+program else cur.spentProgramsThisRun,
                                            lastResult=gtr("%1s: %2s • 1 NET Action", if(active) gtr("DEACTIVATED") else gtr("ACTIVATED"), program)
                                        ) }
                                    }, enabled=st.actionsRemaining>0 && (active || (program !in st.usedProgramsThisRound && (program !in setOf("Armor","Flak","Shield") || program !in st.spentProgramsThisRun))), modifier=Modifier.weight(1f)) {
                                        Text("${if(active) "DEACTIVATE" else "ACTIVATE"} $program${if(active) " • REZZED" else ""}", color=if(active) White else Red, fontSize=10.sp)
                                    }
                                    NetItemInfoButton(onClick = { netItemDetailLabel = program })
                                    }
                                }
                                if (linkedNetrunner != null && viewModel != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(gtr("ENEMY NETRUNNER ATTACKER"), color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(onClick = { enemyInterface = (enemyInterface - 1).coerceAtLeast(1) }) { Text("−", color = Red) }
                                        Text(gtr("Enemy Interface %1s", enemyInterface), color = White, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                        OutlinedButton(onClick = { enemyInterface = (enemyInterface + 1).coerceAtMost(10) }) { Text("+", color = Red) }
                                    }
                                    // گزارش تست ۵.۳: شش FilterChip تنگ با برچسب بریده، جای
                                    // خودش را به کشوی برنامه با نام کامل + دکمه‌ی i می‌دهد.
                                    var enemyProgramMenu by remember { mutableStateOf(false) }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.weight(1f)) {
                                            OutlinedButton(onClick = { enemyProgramMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                                Text(gtr("Enemy Program: %1s", enemyAttackerProgram), color = Red, fontSize = 10.sp)
                                            }
                                            DropdownMenu(expanded = enemyProgramMenu, onDismissRequest = { enemyProgramMenu = false }) {
                                                listOf("DeckKRASH", "Hellbolt", "Nervescrub", "Poison Flatline", "Superglue", "Vrizzbolt").forEach { pr ->
                                                    DropdownMenuItem(text = { Text(pr, fontSize = 12.sp) }, onClick = { enemyAttackerProgram = pr; enemyProgramMenu = false })
                                                }
                                            }
                                        }
                                        NetItemInfoButton(onClick = { netItemDetailLabel = enemyAttackerProgram })
                                    }
                                    Button(onClick = {
                                        if (!st.jackedIn) return@Button
                                        val prog = com.cyberpunk.gmtool.data.CoreItemReference.resolve(enemyAttackerProgram, "Programs") ?: return@Button
                                        val flakActive = "Flak" in st.activePrograms
                                        val atkBonus = if (flakActive) 0 else (prog.atk ?: 0)
                                        val attack = enemyInterface + atkBonus + com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie
                                        val defense = st.interfaceRank + com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie
                                        if (attack <= defense) { updateRun { it.copy(lastResult = gtr("Enemy %1s: %2s vs %3s → MISS%4s", prog.name, attack, defense, if(flakActive) gtr(" • Flak reduced Program ATK to 0") else "")) }; return@Button }
                                        var next = st
                                        var note = gtr("Enemy %1s: %2s vs %3s → HIT", prog.name, attack, defense)
                                        fun brain(expr: String) {
                                            var dmg = com.cyberpunk.gmtool.data.CombatRules.rollDamage(expr).total
                                            if ("Shield" in next.activePrograms) {
                                                dmg = 0
                                                next = next.copy(activePrograms = next.activePrograms - "Shield")
                                                note += " • Shield stopped first brain-damage Program Effect and Derezzed"
                                            } else if ("Armor" in next.activePrograms) {
                                                dmg = (dmg - 4).coerceAtLeast(0)
                                                note += " • Armor -4 brain damage"
                                            }
                                            if (dmg > 0) viewModel.directCombatHpDamage(linkedNetrunner.id, dmg)
                                            note += gtr(" • %1s brain HP", dmg)
                                        }
                                        when (prog.name) {
                                            "DeckKRASH" -> {
                                                if (loadedHardware.any { it.equals("KRASH Barrier", true) }) note += " • KRASH Barrier blocks forced Jack Out"
                                                else {
                                                    val dangerous = next.encounteredIce.filter { key -> key !in next.slidIce && (next.iceRez[key] ?: 0) > 0 }
                                                    var totalBrain = 0
                                                    dangerous.forEach { k ->
                                                        val n = k.split(":").getOrElse(1) { "ICE" }
                                                        val expr = when(n) { "Giant","Kraken" -> "3d6"; "Hellhound" -> "2d6"; "Raven","Wisp" -> "1d6"; else -> "" }
                                                        if (expr.isNotBlank()) { var dmg = com.cyberpunk.gmtool.data.CombatRules.rollDamage(expr).total; if ("Armor" in next.activePrograms) dmg=(dmg-4).coerceAtLeast(0); totalBrain += dmg }
                                                    }
                                                    if (totalBrain > 0) viewModel.directCombatHpDamage(linkedNetrunner.id, totalBrain)
                                                    val freshIce = buildMap<String, Int> { a.floors.forEachIndexed { index, f -> floorIceKeys(index, f).forEach { (k, si) -> put(k, si.rez) } } }
                                                    next = next.copy(jackedIn=false,currentFloor=0,unlockedPasswords=emptySet(),identifiedFiles=emptySet(),controlledNodes=emptySet(),iceRez=freshIce,encounteredIce=emptySet(),slidIce=emptySet(),activePrograms=emptySet(),usedProgramsThisRound=emptySet(),spentProgramsThisRun=emptySet(),programRez=emptyMap(),backupSavedPrograms=emptySet(),nextTurnActionPenalty=0,safeProgressBlockedUntilRound=0)
                                                    note += gtr(" • forced UNSAFE JACK OUT resolved • %1s ICE brain HP", totalBrain)
                                                }
                                            }
                                            "Hellbolt" -> { brain("2d6"); if (loadedHardware.none { it.equals("Insulated Wiring", true) }) viewModel.addCombatEffect(linkedNetrunner.id, "on_fire_2", -1) else note += " • Insulated Wiring prevents fire" }
                                            "Nervescrub" -> { viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("INT" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "REF" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "DEX" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"))); note += " • INT/REF/DEX reduced for 1 hour" }
                                            "Poison Flatline" -> {
                                                val candidates = loadedPrograms.filter { com.cyberpunk.gmtool.data.CoreItemReference.resolve(it,"Programs")?.programClass?.contains("Black ICE",true) != true }
                                                val destroyed = candidates.randomOrNull()
                                                if (destroyed != null) { next = next.copy(activePrograms=next.activePrograms-destroyed, programRez=next.programRez+(destroyed to 0)); note += gtr(" • %1s destroyed", destroyed) } else note += " • no Non-Black ICE Program target"
                                            }
                                            "Superglue" -> { val rounds=com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "Superglue — 1d6 دور"); next=next.copy(safeProgressBlockedUntilRound=maxOf(next.safeProgressBlockedUntilRound,next.round+rounds)); note += gtr(" • deeper progress/Safe Jack Out blocked %1s rounds", rounds) }
                                            "Vrizzbolt" -> { brain("1d6"); next=next.copy(nextTurnActionPenalty=maxOf(next.nextTurnActionPenalty,1)); note += " • -1 NET Action next Turn (min 2)" }
                                        }
                                        updateRun { next.copy(lastResult = note) }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) { Text(gtr("RESOLVE ENEMY PROGRAM"), color = Black, fontSize = 9.sp) }
                                }
                                if (st.backupSavedPrograms.isNotEmpty() && loadedHardware.any { it.equals("Backup Drive", true) }) {
                                    OutlinedButton(onClick = {
                                        if (linkedNetrunner == null) return@OutlinedButton
                                        val restored = st.backupSavedPrograms.filter { name -> loadedPrograms.any { it.equals(name, true) } }
                                        updateRun { cur -> cur.copy(programRez = cur.programRez + restored.associateWith { com.cyberpunk.gmtool.data.CoreItemReference.resolve(it, "Programs")?.rez ?: 7 }, backupSavedPrograms = cur.backupSavedPrograms - restored.toSet(), lastResult = gtr("BACKUP DRIVE: restored %1s to installed deck state. Rezzing still costs a NET Action.", restored.joinToString())) }
                                    }, modifier = Modifier.fillMaxWidth().padding(top=4.dp)) { Text(gtr("MEAT ACTION: RESTORE BACKUP PROGRAMS"), color=Red, fontSize=9.sp) }
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("خروج اضطراری", color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
                                    RuleInfoButton("net.unsafe_jack_out")
                                }
                                OutlinedButton(onClick = {
                                    if (!st.jackedIn) return@OutlinedButton
                                    val dangerous = st.encounteredIce.filter { key -> key !in st.slidIce && (st.iceRez[key] ?: 0) > 0 }
                                    val names = dangerous.map { it.split(":").getOrElse(1){"ICE"} }
                                    var totalBrain = 0
                                    // گزارش تست ۵.۲: بدون Netrunner لینک‌شده، خسارت مغزی تعقیب بی‌صدا رد می‌شد.
                                    val unsafeNoRunnerWarning = if (linkedNetrunner == null && names.any { it in setOf("Giant", "Kraken", "Hellhound", "Raven", "Wisp", "Liche", "Scorpion") }) " • ⚠ Netrunner لینک نشده — خسارت مغزی ICEهای تعقیب‌کننده اعمال نشد" else ""
                                    if (linkedNetrunner != null && viewModel != null) {
                                        dangerous.forEach { k ->
                                            val n = k.split(":").getOrElse(1){"ICE"}
                                            val expr = when(n){"Giant","Kraken"->"3d6";"Hellhound"->"2d6";"Raven","Wisp"->"1d6";else->""}
                                            if(expr.isNotBlank()) { var dmg=com.cyberpunk.gmtool.data.CombatRules.rollDamage(expr).total; if("Armor" in st.activePrograms) dmg=(dmg-4).coerceAtLeast(0); totalBrain += dmg }
                                            if(n=="Hellhound" && loadedHardware.none{it.equals("Insulated Wiring",true)}) viewModel.addCombatEffect(linkedNetrunner.id,"on_fire_2",-1)
                                            if(n=="Liche") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id,mapOf("INT" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "REF" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "DEX" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6")))
                                            if(n=="Scorpion") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id,mapOf("MOVE" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6 MOVE")))
                                        }
                                        if(totalBrain>0) viewModel.directCombatHpDamage(linkedNetrunner.id,totalBrain)
                                    }
                                    val freshIce = buildMap<String, Int> { a.floors.forEachIndexed { index, f -> floorIceKeys(index, f).forEach { (key, stat) -> put(key, stat.rez) } } }
                                    updateRun { it.copy(jackedIn=false,currentFloor=0,unlockedPasswords=emptySet(),identifiedFiles=emptySet(),controlledNodes=emptySet(),iceRez=freshIce,encounteredIce=emptySet(),slidIce=emptySet(),activePrograms=emptySet(),usedProgramsThisRound=emptySet(),spentProgramsThisRun=emptySet(),programRez=emptyMap(),backupSavedPrograms=emptySet(),nextTurnActionPenalty=0,safeProgressBlockedUntilRound=0,lastResult=gtr("UNSAFE JACK OUT resolved: %1s%2s. Architecture reset.", if(names.isEmpty()) gtr("no pursuing ICE") else names.joinToString(), if(totalBrain>0) gtr(" • %1s total brain HP applied", totalBrain) else "") + unsafeNoRunnerWarning) }
                                }, modifier=Modifier.fillMaxWidth().padding(top=5.dp)) { Text(gtr("UNSAFE JACK OUT"), color=Red, fontSize=10.sp) }
                            }
                            if (ice.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(gtr("BLACK ICE"), color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    RuleInfoButton("net.black_ice")
                                }
                                // گزارش تست ۵.۲: نتیجه‌ی ZAP/SLIDE/ICE TURN دقیقاً بالای
                                // همان ردیف‌های حمله دیده می‌شود.
                                if (st.lastResult.isNotBlank()) Text(gtr(st.lastResult), color = White, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth().background(CardBg, CutCornerShape(6.dp)).padding(8.dp), textAlign = TextAlign.Right)
                                ice.forEach { (key, stat) ->
                                    val iceName = key.split(":").getOrElse(1) { "ICE" }
                                    val rezNow = st.iceRez[key] ?: stat.rez
                                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(CardBg, CutCornerShape(6.dp)).padding(8.dp)) {
                                        Text(gtr("%1s • REZ %2s/%3s • PER %4s SPD %5s ATK %6s DEF %7s", iceName, rezNow, stat.rez, stat.per, stat.spd, stat.atk, stat.def), color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { netItemDetailLabel = iceName })
                                        Text(gtr(stat.effect), color = Muted, fontSize = 10.sp)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                            RuleInfoButton("net.zap")
                                            OutlinedButton(onClick = {
                                                if (st.actionsRemaining <= 0 || rezNow <= 0) return@OutlinedButton
                                                val aDie=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val dDie=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val atk=st.interfaceRank+aDie; val defense=stat.def+dDie
                                                val dmg=if(atk>defense) com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "ZAP — 1d6 آسیب") else 0
                                                updateRun { cur -> cur.copy(actionsRemaining=cur.actionsRemaining-1, encounteredIce=cur.encounteredIce+key, iceRez=cur.iceRez+(key to (rezNow-dmg).coerceAtLeast(0)), lastResult=gtr("ZAP %1s: %2s vs %3s → %4s", iceName, atk, defense, if(dmg>0) gtr("%1s REZ damage", dmg) else "MISS")) }
                                            }, enabled = st.jackedIn && st.actionsRemaining > 0 && rezNow > 0, modifier = Modifier.weight(1f)) { Text(gtr("ZAP"), color = Red, fontSize = 10.sp) }
                                            OutlinedButton(onClick = {
                                                if (st.actionsRemaining <= 0 || st.slideUsedThisTurn || rezNow <= 0) return@OutlinedButton
                                                val aDie=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val dDie=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val me=st.interfaceRank+aDie; val foe=stat.per+dDie; val ok=me>foe
                                                updateRun { it.copy(actionsRemaining=it.actionsRemaining-1, slideUsedThisTurn=true, encounteredIce=it.encounteredIce+key, slidIce=if(ok) it.slidIce+key else it.slidIce, lastResult=gtr("SLIDE vs %1s: %2s vs PER %3s → %4s", iceName, me, foe, if(ok) gtr("ESCAPED THIS ICE") else gtr("FAIL"))) }
                                            }, enabled = st.jackedIn && st.actionsRemaining > 0 && !st.slideUsedThisTurn && rezNow > 0, modifier = Modifier.weight(1f)) { Text(gtr("SLIDE"), color = Red, fontSize = 10.sp) }
                                            OutlinedButton(onClick = {
                                                if (rezNow <= 0) return@OutlinedButton
                                                val iDie=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val nDie=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val atk=stat.atk+iDie; val def=st.interfaceRank+nDie
                                                val hit = atk > def
                                                // گزارش تست ۵.۲: در نبودِ Netrunner لینک‌شده، اثرهای مغزی و
                                                // مجازات‌های ICE بی‌صدا رد می‌شدند؛ حالا در نتیجه هشدار می‌آید.
                                                val iceEffectsNeedRunner = iceName in setOf("Giant", "Kraken", "Hellhound", "Raven", "Wisp", "Liche", "Scorpion")
                                                val noRunnerWarning = if (hit && iceEffectsNeedRunner && (linkedNetrunner == null || viewModel == null)) " • ⚠ Netrunner لینک نشده — خسارت/اثر مغزی روی هیچ شخصیتی اعمال نشد (بخش LINKED NETRUNNER بالا)" else ""
                                                var brainDamage = 0
                                                var defenderDerezzed: String? = null
                                                var programDamaged: String? = null
                                                var programDamage = 0
                                                if (hit) {
                                                    if (linkedNetrunner != null && viewModel != null) {
                                                        val expr = when (iceName) { "Giant", "Kraken" -> "3d6"; "Hellhound" -> "2d6"; "Raven", "Wisp" -> "1d6"; else -> "" }
                                                        if (expr.isNotBlank()) {
                                                            brainDamage = com.cyberpunk.gmtool.data.CombatRules.rollDamage(expr).total
                                                            if ("Armor" in st.activePrograms) brainDamage = (brainDamage - 4).coerceAtLeast(0)
                                                            if (brainDamage > 0) viewModel.directCombatHpDamage(linkedNetrunner.id, brainDamage)
                                                        }
                                                        if (iceName == "Hellhound" && loadedHardware.none { it.equals("Insulated Wiring", true) }) viewModel.addCombatEffect(linkedNetrunner.id, "on_fire_2", -1)
                                                        if (iceName == "Liche") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("INT" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "REF" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "DEX" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6")))
                                                        if (iceName == "Scorpion") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("MOVE" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6 MOVE")))
                                                    }
                                                    if (iceName in setOf("Dragon","Killer","Sabertooth")) {
                                                        val targets = st.activePrograms.toList()
                                                        if (targets.isNotEmpty()) { programDamaged = targets.random(); programDamage = com.cyberpunk.gmtool.data.CombatRules.rollDamage(if(iceName=="Killer") "4d6" else "6d6").total }
                                                    }
                                                    if (iceName == "Raven") defenderDerezzed = st.activePrograms.filter { it in setOf("Armor","Flak","Shield") }.randomOrNull()
                                                }
                                                updateRun { cur ->
                                                    var next = cur.copy(encounteredIce=cur.encounteredIce+key)
                                                    if (hit && iceName == "Wisp") next = next.copy(nextTurnActionPenalty=maxOf(next.nextTurnActionPenalty,1))
                                                    if (hit && iceName == "Kraken") next = next.copy(safeProgressBlockedUntilRound=maxOf(next.safeProgressBlockedUntilRound,next.round+1))
                                                    if (defenderDerezzed != null) next = next.copy(activePrograms=next.activePrograms-defenderDerezzed!!)
                                                    if (programDamaged != null) {
                                                        val oldRez=next.programRez[programDamaged!!] ?: (com.cyberpunk.gmtool.data.CoreItemReference.resolve(programDamaged!!,"Programs")?.rez ?: 7)
                                                        val left=(oldRez-programDamage).coerceAtLeast(0)
                                                        val destroyed=left==0
                                                        val backup=destroyed && loadedHardware.any{it.equals("Backup Drive",true)} && com.cyberpunk.gmtool.data.CoreItemReference.resolve(programDamaged!!,"Programs")?.programClass?.contains("Black ICE",true)!=true
                                                        next=next.copy(programRez=next.programRez+(programDamaged!! to left), activePrograms=if(destroyed) next.activePrograms-programDamaged!! else next.activePrograms, backupSavedPrograms=if(backup) next.backupSavedPrograms+programDamaged!! else next.backupSavedPrograms)
                                                    }
                                                    val giantForcesOut = hit && iceName == "Giant" && loadedHardware.none { it.equals("KRASH Barrier", true) }
                                                    if (giantForcesOut) {
                                                        val dangerous = next.encounteredIce.filter { k -> k != key && k !in next.slidIce && (next.iceRez[k] ?: 0) > 0 }
                                                        var extraBrain = 0
                                                        if (linkedNetrunner != null && viewModel != null) {
                                                            dangerous.forEach { k ->
                                                                val n = k.split(":").getOrElse(1) { "ICE" }
                                                                val expr = when(n) { "Giant","Kraken" -> "3d6"; "Hellhound" -> "2d6"; "Raven","Wisp" -> "1d6"; else -> "" }
                                                                if (expr.isNotBlank()) {
                                                                    var dmg = com.cyberpunk.gmtool.data.CombatRules.rollDamage(expr).total
                                                                    if ("Armor" in next.activePrograms) dmg = (dmg - 4).coerceAtLeast(0)
                                                                    extraBrain += dmg
                                                                }
                                                                if (n == "Hellhound" && loadedHardware.none { it.equals("Insulated Wiring", true) }) viewModel.addCombatEffect(linkedNetrunner.id, "on_fire_2", -1)
                                                                if (n == "Liche") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("INT" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "REF" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "DEX" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6")))
                                                                if (n == "Scorpion") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("MOVE" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6 MOVE")))
                                                            }
                                                            if (extraBrain > 0) viewModel.directCombatHpDamage(linkedNetrunner.id, extraBrain)
                                                        }
                                                        val freshIce = buildMap<String, Int> { a.floors.forEachIndexed { index, f -> floorIceKeys(index, f).forEach { (k, si) -> put(k, si.rez) } } }
                                                        next.copy(jackedIn=false,currentFloor=0,unlockedPasswords=emptySet(),identifiedFiles=emptySet(),controlledNodes=emptySet(),iceRez=freshIce,encounteredIce=emptySet(),slidIce=emptySet(),activePrograms=emptySet(),usedProgramsThisRound=emptySet(),spentProgramsThisRun=emptySet(),programRez=emptyMap(),backupSavedPrograms=emptySet(),nextTurnActionPenalty=0,safeProgressBlockedUntilRound=0,lastResult=gtr("ICE TURN — Giant HIT • forced UNSAFE JACK OUT resolved automatically%1s. Architecture reset.", if(brainDamage+extraBrain>0) gtr(" • %1s total brain HP", brainDamage+extraBrain) else "") + noRunnerWarning)
                                                    } else {
                                                        val giantNote = if(hit && iceName=="Giant" && loadedHardware.any{it.equals("KRASH Barrier",true)}) " • KRASH Barrier blocks forced Jack Out" else ""
                                                        next.copy(lastResult=gtr("ICE TURN — %1s: ATK %2s vs Interface %3s → %4s", iceName, atk, def, if(hit) gtr("HIT: %1s%2s%3s%4s%5s", stat.effect, if(brainDamage>0) gtr(" • %1s HP", brainDamage) else "", defenderDerezzed?.let{" • $it Derezzed"}?:"", programDamaged?.let{gtr(" • %1s takes %2s REZ", it, programDamage)}?:"", giantNote) else "MISS") + noRunnerWarning)
                                                    }
                                                }
                                            }, enabled = rezNow > 0, modifier = Modifier.weight(1f)) { Text(gtr("ICE TURN"), color = Muted, fontSize = 10.sp) }
                                        }
                                        val antiProgram = loadedPrograms.mapNotNull { n -> com.cyberpunk.gmtool.data.CoreItemReference.resolve(n, "Programs") }
                                            .filter { it.programClass.contains("Anti-Program", true) && it.programClass.contains("Attacker", true) }
                                        antiProgram.forEach { prog ->
                                            // گزارش تست ۵.۳: دکمه‌ی «i» کنار حمله‌ی Anti-Program.
                                            Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            OutlinedButton(onClick = {
                                                if (st.actionsRemaining <= 0 || rezNow <= 0) return@OutlinedButton
                                                val pDie=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val dDie=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val attack=st.interfaceRank+(prog.atk ?: 0)+pDie; val defense=stat.def+dDie
                                                val dmg = if (attack > defense) when (prog.name.lowercase()) {
                                                    "sword" -> com.cyberpunk.gmtool.data.CombatRules.rollDamage("3d6").total
                                                    "banhammer" -> com.cyberpunk.gmtool.data.CombatRules.rollDamage("2d6").total
                                                    else -> 0
                                                } else 0
                                                updateRun { cur -> cur.copy(actionsRemaining=cur.actionsRemaining-1, encounteredIce=cur.encounteredIce+key, iceRez=cur.iceRez+(key to (rezNow-dmg).coerceAtLeast(0)), lastResult=gtr("%1s vs %2s: %3s vs %4s → %5s", prog.name, iceName, attack, defense, if(dmg>0) gtr("%1s REZ", dmg) else "MISS")) }
                                            }, enabled = st.jackedIn && st.actionsRemaining > 0 && rezNow > 0, modifier = Modifier.weight(1f)) {
                                                Text(gtr("ATTACK WITH %1s", prog.name), color=Red, fontSize=9.sp)
                                            }
                                            NetItemInfoButton(onClick = { netItemDetailLabel = prog.name })
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { updateRun { it.copy(currentFloor = (it.currentFloor - 1).coerceAtLeast(0), lastResult = "Moved up one Floor (movement is not a NET Action).") } }, enabled = floorIndex > 0 && st.jackedIn, modifier = Modifier.weight(1f)) { Text(gtr("▲ UP"), color = Red) }
                                val passwordBlocks = floor.contains("Password", true) && floorIndex !in st.unlockedPasswords
                                Button(onClick = {
                                    val targetFloor = (floorIndex + 1).coerceAtMost(a.floors.lastIndex)
                                    if (st.round <= st.safeProgressBlockedUntilRound) { updateRun { it.copy(lastResult = "حرکت به Floor عمیق‌تر تا پایان اثر Kraken/Superglue مسدود است.") }; return@Button }
                                    val targetIce = floorIceKeys(targetFloor, a.floors[targetFloor])
                                    var resultText = "Moved deeper (movement is not a NET Action)."
                                    var encountered = st.encounteredIce
                                    var actionPenalty = st.nextTurnActionPenalty
                                    var blockUntil = st.safeProgressBlockedUntilRound
                                    var programRezState = st.programRez
                                    var activeProgramState = st.activePrograms
                                    var backupState = st.backupSavedPrograms
                                    var forcedUnsafe = false
                                    var forcedByGiantKey: String? = null
                                    targetIce.forEach { (iceKey, iceStat) ->
                                        if (!forcedUnsafe && iceKey !in encountered && (st.iceRez[iceKey] ?: iceStat.rez) > 0) {
                                            val speedBonus = if ("Speedy Gonzalvez" in activeProgramState) 2 else 0
                                            val me = st.interfaceRank + speedBonus + com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie
                                            val foe = iceStat.spd + com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie
                                            encountered = encountered + iceKey
                                            val n = iceKey.split(":").getOrElse(1) { "ICE" }
                                            if (foe > me) {
                                                resultText += gtr(" • %1s wins SPEED %2s>%3s: immediate Effect", n, foe, me)
                                                if (n == "Wisp") actionPenalty = maxOf(actionPenalty, 1)
                                                if (n == "Kraken") blockUntil = maxOf(blockUntil, st.round + 1)
                                                if (linkedNetrunner != null && viewModel != null) {
                                                    val expr = when(n) { "Giant","Kraken" -> "3d6"; "Hellhound" -> "2d6"; "Raven","Wisp" -> "1d6"; else -> "" }
                                                    if (expr.isNotBlank()) {
                                                        var dmg = com.cyberpunk.gmtool.data.CombatRules.rollDamage(expr).total
                                                        if ("Armor" in activeProgramState) dmg = (dmg - 4).coerceAtLeast(0)
                                                        if (dmg > 0) viewModel.directCombatHpDamage(linkedNetrunner.id, dmg)
                                                        resultText += gtr(" (%1s brain HP)", dmg)
                                                    }
                                                    if (n == "Hellhound" && loadedHardware.none { it.equals("Insulated Wiring", true) }) viewModel.addCombatEffect(linkedNetrunner.id, "on_fire_2", -1)
                                                    if (n == "Liche") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("INT" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "REF" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "DEX" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6")))
                                                    if (n == "Scorpion") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("MOVE" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6 MOVE")))
                                                } else if (linkedNetrunner == null && n in setOf("Giant", "Kraken", "Hellhound", "Raven", "Wisp", "Liche", "Scorpion")) {
                                                    // گزارش تست ۵.۲: بدون Netrunner لینک‌شده اثر ICE بی‌صدا رد نشود.
                                                    resultText += " • ⚠ Netrunner لینک نشده — خسارت/اثر مغزی اعمال نشد"
                                                }
                                                if (n == "Raven") {
                                                    val target = activeProgramState.filter { it in setOf("Armor","Flak","Shield") }.randomOrNull()
                                                    if (target != null) { activeProgramState = activeProgramState - target; resultText += gtr(" • %1s Derezzed", target) }
                                                }
                                                if (n in setOf("Dragon","Killer","Sabertooth")) {
                                                    val targets = activeProgramState.toList()
                                                    if (targets.isNotEmpty()) {
                                                        val target = targets.random()
                                                        val pdmg = com.cyberpunk.gmtool.data.CombatRules.rollDamage(if (n == "Killer") "4d6" else "6d6").total
                                                        val oldRez = programRezState[target] ?: (com.cyberpunk.gmtool.data.CoreItemReference.resolve(target,"Programs")?.rez ?: 7)
                                                        val left = (oldRez - pdmg).coerceAtLeast(0)
                                                        val destroyed = left == 0
                                                        val canBackup = destroyed && loadedHardware.any { it.equals("Backup Drive", true) } && com.cyberpunk.gmtool.data.CoreItemReference.resolve(target,"Programs")?.programClass?.contains("Black ICE",true) != true
                                                        programRezState = programRezState + (target to left)
                                                        if (destroyed) activeProgramState = activeProgramState - target
                                                        if (canBackup) backupState = backupState + target
                                                        resultText += gtr(" • %1s takes %2s REZ%3s%4s", target, pdmg, if(destroyed) " and is destroyed" else "", if(canBackup) " (saved by Backup Drive)" else "")
                                                    } else resultText += " • no Rezzed Program target"
                                                }
                                                if (n == "Giant") {
                                                    if (loadedHardware.any { it.equals("KRASH Barrier", true) }) resultText += " • KRASH Barrier prevents forced Jack Out"
                                                    else { forcedUnsafe = true; forcedByGiantKey = iceKey }
                                                }
                                            } else resultText += gtr(" • %1s SPEED: %2s vs %3s, no free hit", n, me, foe)
                                        }
                                    }
                                    if (forcedUnsafe) {
                                        val dangerous = encountered.filter { key -> key != forcedByGiantKey && key !in st.slidIce && (st.iceRez[key] ?: coreBlackIce[key.split(":").getOrElse(1){""}]?.rez ?: 0) > 0 }
                                        var extraBrain = 0
                                        if (linkedNetrunner != null && viewModel != null) {
                                            dangerous.forEach { k ->
                                                val n = k.split(":").getOrElse(1) { "ICE" }
                                                val expr = when(n) { "Giant","Kraken" -> "3d6"; "Hellhound" -> "2d6"; "Raven","Wisp" -> "1d6"; else -> "" }
                                                if (expr.isNotBlank()) {
                                                    var dmg = com.cyberpunk.gmtool.data.CombatRules.rollDamage(expr).total
                                                    if ("Armor" in activeProgramState) dmg = (dmg - 4).coerceAtLeast(0)
                                                    extraBrain += dmg
                                                }
                                                if (n == "Hellhound" && loadedHardware.none { it.equals("Insulated Wiring", true) }) viewModel.addCombatEffect(linkedNetrunner.id, "on_fire_2", -1)
                                                if (n == "Liche") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("INT" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "REF" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6"), "DEX" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6")))
                                                if (n == "Scorpion") viewModel.applyTemporaryStatPenalty(linkedNetrunner.id, mapOf("MOVE" to com.cyberpunk.gmtool.data.DiceSource.rollOne(6, "کاهش موقت — 1d6 MOVE")))
                                            }
                                            if (extraBrain > 0) viewModel.directCombatHpDamage(linkedNetrunner.id, extraBrain)
                                        }
                                        val freshIce = buildMap<String, Int> { a.floors.forEachIndexed { index, f -> floorIceKeys(index, f).forEach { (k, si) -> put(k, si.rez) } } }
                                        updateRun { it.copy(jackedIn=false,currentFloor=0,unlockedPasswords=emptySet(),identifiedFiles=emptySet(),controlledNodes=emptySet(),iceRez=freshIce,encounteredIce=emptySet(),slidIce=emptySet(),activePrograms=emptySet(),usedProgramsThisRound=emptySet(),spentProgramsThisRun=emptySet(),programRez=emptyMap(),backupSavedPrograms=emptySet(),nextTurnActionPenalty=0,safeProgressBlockedUntilRound=0,lastResult=gtr("%1s • Giant forced UNSAFE JACK OUT automatically%2s. Architecture reset.", resultText, if(extraBrain>0) gtr(" • %1s additional brain HP", extraBrain) else "")) }
                                    } else {
                                        updateRun { it.copy(currentFloor=targetFloor, encounteredIce=encountered, nextTurnActionPenalty=actionPenalty, safeProgressBlockedUntilRound=blockUntil, programRez=programRezState, activePrograms=activeProgramState, backupSavedPrograms=backupState, lastResult=resultText) }
                                    }
                                }, enabled = st.jackedIn && floorIndex < a.floors.lastIndex && !passwordBlocks, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.weight(1f)) { Text(gtr("▼ DEEPER"), color = Black) }
                            }
                            if (floorIndex == a.floors.lastIndex && st.jackedIn) {
                                Column(Modifier.fillMaxWidth().padding(top=7.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(gtr("BOTTOM: VIRUS"), color=Red, fontSize=11.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f))
                                        RuleInfoButton("net.virus")
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                        OutlinedButton(onClick={ updateRun { it.copy(virusRequired=(it.virusRequired-1).coerceAtLeast(0)) } }, modifier=Modifier.weight(1f)){ Text(gtr("ACT−"),color=Red) }
                                        Text(gtr("%1s/%2s Actions", st.virusProgress, st.virusRequired),color=White,modifier=Modifier.weight(2f),textAlign=TextAlign.Center)
                                        OutlinedButton(onClick={ updateRun { it.copy(virusRequired=it.virusRequired+1) } }, modifier=Modifier.weight(1f)){ Text(gtr("ACT+"),color=Red) }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                        OutlinedButton(onClick={ updateRun { it.copy(virusDv=(it.virusDv-1).coerceAtLeast(0)) } }, modifier=Modifier.weight(1f)){ Text(gtr("DV−"),color=Red) }
                                        Text(gtr("DV %1s", st.virusDv),color=White,modifier=Modifier.weight(2f),textAlign=TextAlign.Center)
                                        OutlinedButton(onClick={ updateRun { it.copy(virusDv=it.virusDv+1) } }, modifier=Modifier.weight(1f)){ Text(gtr("DV+"),color=Red) }
                                    }
                                    Button(onClick={
                                        if(st.actionsRemaining<=0 || st.virusRequired<=0) return@Button
                                        val spend=minOf(st.actionsRemaining, st.virusRequired-st.virusProgress)
                                        val completed=st.virusProgress+spend>=st.virusRequired
                                        if(completed){ val die=com.cyberpunk.gmtool.data.CombatRules.rollD10().totalDie; val total=st.interfaceRank+die; updateRun { it.copy(actionsRemaining=it.actionsRemaining-spend,virusProgress=if(total>st.virusDv) st.virusRequired else 0,lastResult=gtr("VIRUS CHECK: %1s vs DV%2s → %3s", total, st.virusDv, if(total>st.virusDv) gtr("SUCCESS • destroy DV = %1s", total) else gtr("FAIL • progress lost"))) } }
                                        else updateRun { it.copy(actionsRemaining=it.actionsRemaining-spend,virusProgress=it.virusProgress+spend,lastResult=gtr("Virus coding: +%1s NET Actions", spend)) }
                                    }, enabled=st.actionsRemaining>0 && st.virusRequired>0 && st.virusProgress<st.virusRequired, colors=ButtonDefaults.buttonColors(containerColor=Red), modifier=Modifier.fillMaxWidth()){ Text(gtr("SPEND NET ACTIONS ON VIRUS"),color=Black,fontSize=10.sp) }
                                }
                            }
                            if (st.lastResult.isNotBlank()) Text(gtr(st.lastResult), color = White, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Right)
                            OutlinedTextField(value = st.notes, onValueChange = { value -> updateRun { it.copy(notes = value) } }, label = { Text(gtr("GM NETRUN notes")) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 2, textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right))
                        }
                    }
                }
            }

            if (saved.isNotEmpty()) {
                item { Text("شبکه‌های ذخیره‌شده", color = Red, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Right) }
                items(saved.size, key = { saved[it].id }) { i ->
                    val a = saved[i]
                    Row(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(8.dp)).clickable { current = a; showRun = false; runState = null }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(gtr(a.name), color = White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                            Text(gtr("%1s • %2s Floors", a.difficulty.label, a.floors.size), color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        }
                        IconButton(onClick = { saved.removeAt(i); if (current?.id == a.id) current = saved.firstOrNull(); persist() }) {
                            Icon(Icons.Default.Delete, null, tint = Muted)
                        }
                    }
                }
            }
        }
    }

    netItemDetailLabel?.let { label ->
        CoreItemDetailDialog(label = label, onDismiss = { netItemDetailLabel = null })
    }
}

// ================= Night / Midnight Market Generator =================
private enum class MarketTier(val label: String) { NIGHT("Night Market"), MIDNIGHT("Midnight Market") }
private data class MarketSnapshot(val id: Long, val name: String, val tier: MarketTier, val categories: List<String>, val items: List<String>, val soldIndices: Set<Int> = emptySet())

private fun randomMarketCategories(): List<String> {
    val all = listOf("Food & Drugs", "Personal Electronics", "Weapons & Armor", "Cyberware", "Clothing & Fashionware", "Survival Gear")
    return all.shuffled().take(2)
}

private fun pickCatalog(predicate: (com.cyberpunk.gmtool.data.StoreItem) -> Boolean, fallback: String): String {
    val hit = com.cyberpunk.gmtool.data.StoreCatalog.items.filter(predicate).randomOrNull()
    return hit?.let { gtr("%1s — %2seb", it.name, it.basePrice) } ?: fallback
}

/** Core Night Market d100 shelf table (pp. 338–339), converted into concrete app inventory where possible. */
private fun coreNightMarketEntry(category: String, roll: Int): String = when (category) {
    "Food & Drugs" -> when (roll) {
        in 1..5 -> "Canned Goods — 10eb"
        in 6..10 -> "Packaged Goods — 10eb"
        in 11..15 -> "Frozen Goods — 10eb"
        in 16..20 -> "Bags of Grain — 20eb"
        in 21..25 -> "Kibble Pack — 10eb"
        in 26..30 -> "Bags of Prepak — 20eb"
        in 31..35 -> pickCatalog({ it.category == "Drugs" && it.basePrice <= 20 }, "Street Drug ≤20eb")
        in 36..40 -> "Poor Quality Alcohol — 10eb"
        in 41..45 -> "Alcohol — 20eb"
        in 46..50 -> "Excellent Quality Alcohol — 100eb"
        in 51..55 -> "MRE — 10eb"
        in 56..60 -> "Live Chicken — 50eb"
        in 61..65 -> "Live Fish — 50eb"
        in 66..70 -> "Fresh Fruits — 50eb"
        in 71..75 -> "Fresh Vegetables — 50eb"
        in 76..80 -> "Root Vegetables — 20eb"
        in 81..85 -> "Live Pigs — 100eb"
        in 86..90 -> "Exotic Fruits — 100eb"
        in 91..95 -> "Exotic Vegetables — 100eb"
        else -> pickCatalog({ it.category == "Drugs" && it.basePrice == 50 }, "Street Drug — 50eb")
    }
    "Personal Electronics" -> when (roll) {
        in 1..5 -> "Agent — 100eb"
        in 6..10 -> pickCatalog({ it.category in listOf("Programs", "Hardware") && it.basePrice <= 100 }, "Program/Hardware ≤100eb")
        in 11..15 -> "Audio Recorder — 100eb"
        in 16..20 -> "Bug Detector — 500eb"
        in 21..25 -> "Chemical Analyzer — 1,000eb"
        in 26..30 -> "Computer — 50eb"
        in 31..35 -> "Cyberdeck (Standard Quality) — 500eb"
        in 36..40 -> "Disposable Cell Phone — 50eb"
        in 41..45 -> "Electric Guitar or Other Instrument — 500eb"
        in 46..50 -> pickCatalog({ it.category in listOf("Programs", "Hardware") && it.basePrice == 500 }, "Program/Hardware — 500eb")
        in 51..55 -> "Medscanner — 1,000eb"
        in 56..60 -> "Homing Tracer — 500eb"
        in 61..65 -> "Radio Communicator — 100eb"
        in 66..70 -> "Techscanner — 1,000eb"
        in 71..75 -> "Smart Glasses — 500eb"
        in 76..80 -> "Radar Detector — 500eb"
        in 81..85 -> "Scrambler/Descrambler — 500eb"
        in 86..90 -> "Radio Scanner/Music Player — 50eb"
        in 91..95 -> "Braindance Viewer — 1,000eb"
        else -> "Virtuality Goggles — 100eb"
    }
    "Weapons & Armor" -> when (roll) {
        in 1..5 -> "Medium Pistol — 50eb"
        in 6..10 -> listOf("Heavy Pistol — 100eb", "Very Heavy Pistol — 100eb").random()
        in 11..15 -> "SMG — 100eb"
        in 16..20 -> "Heavy SMG — 100eb"
        in 21..25 -> "Shotgun — 500eb"
        in 26..30 -> "Assault Rifle — 500eb"
        in 31..35 -> "Sniper Rifle — 500eb"
        in 36..40 -> "Bows / Crossbow — 100eb"
        in 41..45 -> listOf("Grenade Launcher — 500eb", "Rocket Launcher — 500eb").random()
        in 46..50 -> pickCatalog({ it.category == "Ammo" && it.basePrice <= 500 }, "Ammunition ≤500eb")
        in 51..55 -> pickCatalog({ it.category == "Weapons" && it.name in listOf("Air Pistol", "Battleglove", "Constitution Arms Hurricane Assault Weapon", "Dartgun", "Flamethrower", "Kendachi Mono-Three", "Malorian Arms 3516", "Microwaver", "Militech Cowboy U-56 Grenade Launcher", "Rhinemetall EMG-86 Railgun", "Shrieker", "Stun Baton", "Stun Gun", "Tsunami Arms Helix") }, "Exotic Weapon — GM choice")
        in 56..60 -> "Light Melee Weapon — 50eb"
        in 61..65 -> "Medium Melee Weapon — 50eb"
        in 66..70 -> "Heavy Melee Weapon — 100eb"
        in 71..75 -> "Very Heavy Melee Weapon — 500eb"
        in 76..80 -> pickCatalog({ it.category == "Armor" && it.basePrice <= 100 }, "Armor ≤100eb")
        in 81..85 -> pickCatalog({ it.category == "Armor" && it.basePrice == 500 }, "Armor — 500eb")
        in 86..90 -> pickCatalog({ it.category == "Armor" && it.basePrice == 1000 }, "Armor — 1,000eb")
        in 91..95 -> pickCatalog({ it.category == "Weapon Mods" && it.basePrice <= 100 }, "Weapon Attachment ≤100eb")
        else -> pickCatalog({ it.category == "Weapon Mods" && it.basePrice >= 500 }, "Weapon Attachment ≥500eb")
    }
    "Cyberware" -> when (roll) {
        in 1..5 -> "Cybereye — 100eb"
        in 6..10 -> "Cyberaudio Suite — 500eb"
        in 11..15 -> "Neural Link — 500eb"
        in 16..20 -> "Cyberarm — 500eb"
        in 21..25 -> "Cyberleg — 100eb"
        in 26..30 -> pickCatalog({ it.category == "Cyberware" && it.slot == "Hospital" && it.basePrice == 1000 && it.name !in listOf("Cybereye", "Cyberarm", "Cyberleg") }, "External Cyberware — 1,000eb")
        in 31..35 -> pickCatalog({ it.category == "Cyberware" && it.basePrice <= 500 && it.subtitle.contains("External", true) }, "External Cyberware ≤500eb")
        in 36..40 -> pickCatalog({ it.category == "Cyberware" && it.basePrice == 1000 && it.subtitle.contains("Internal", true) }, "Internal Cyberware — 1,000eb")
        in 41..45 -> pickCatalog({ it.category == "Cyberware" && it.basePrice <= 500 && it.subtitle.contains("Internal", true) }, "Internal Cyberware ≤500eb")
        in 46..50 -> pickCatalog({ it.category == "Cyberware" && it.basePrice == 1000 && it.subtitle.contains(gtr("Cyberopt"), true) }, "Cybereye Option — 1,000eb")
        in 51..55 -> pickCatalog({ it.category == "Cyberware" && it.basePrice <= 500 && it.subtitle.contains(gtr("Cyberopt"), true) }, "Cybereye Option ≤500eb")
        in 56..60 -> pickCatalog({ it.category == "Cyberware" && it.basePrice == 1000 && it.subtitle.contains("Cyberaudio", true) }, "Cyberaudio Option — 1,000eb")
        in 61..65 -> pickCatalog({ it.category == "Cyberware" && it.basePrice <= 500 && it.subtitle.contains("Cyberaudio", true) }, "Cyberaudio Option ≤500eb")
        in 66..70 -> pickCatalog({ it.category == "Cyberware" && it.basePrice == 1000 && it.subtitle.contains(gtr("Neural"), true) }, "Neuralware Option — 1,000eb")
        in 71..75 -> pickCatalog({ it.category == "Cyberware" && it.basePrice <= 500 && it.subtitle.contains(gtr("Neural"), true) }, "Neuralware Option ≤500eb")
        in 76..80 -> pickCatalog({ it.category == "Cyberware" && it.basePrice == 1000 && it.subtitle.contains("Cyberlimb", true) }, "Cyberlimb Option — 1,000eb")
        in 81..85 -> pickCatalog({ it.category == "Cyberware" && it.basePrice <= 500 && (it.subtitle.contains("Cyberarm", true) || it.subtitle.contains("Cyberleg", true) || it.subtitle.contains("Cyberlimb", true)) }, "Cyberlimb Option ≤500eb")
        in 86..90 -> pickCatalog({ it.category == "Cyberware" && (it.humanityLoss ?: 1) == 0 }, "Fashionware — GM choice")
        in 91..95 -> pickCatalog({ it.category == "Cyberware" && it.subtitle.contains(gtr("Borg"), true) }, "Borgware — GM choice")
        else -> pickCatalog({ it.category == "Cyberware" }, "Cyberware — GM choice")
    }
    "Clothing & Fashionware" -> when (roll) {
        in 1..5 -> "Bag Lady Chic"
        in 6..10 -> "Gang Colors"
        in 11..15 -> "Generic Chic"
        in 16..20 -> "Bohemian"
        in 21..25 -> "Leisurewear"
        in 26..30 -> "Nomad Leathers"
        in 31..35 -> "Asia Pop"
        in 36..40 -> "Urban Flash"
        in 41..45 -> "Businesswear"
        in 46..50 -> "High Fashion"
        in 51..55 -> "Biomonitor — 100eb"
        in 56..60 -> "Chemskin — 100eb"
        in 61..65 -> "EMP Threading — 10eb"
        in 66..70 -> "Light Tattoo — 100eb"
        in 71..75 -> "Shift Tacts — 100eb"
        in 76..80 -> "Skinwatch — 100eb"
        in 81..85 -> "Techhair — 100eb"
        in 86..90 -> pickCatalog({ it.category == "Cyberware" && (it.humanityLoss ?: 1) == 0 }, "Fashionware — GM choice")
        in 91..95 -> "Generic Chic"
        else -> "Gang Colors"
    }
    else -> when (roll) {
        in 1..5 -> "Anti-Smog Breathing Mask — 20eb"
        in 6..10 -> "Auto Level Dampening Ear Protectors — 1,000eb"
        in 11..15 -> "Binoculars — 50eb"
        in 16..20 -> "Carryall — 20eb"
        in 21..25 -> "Flashlight — 20eb"
        in 26..30 -> "Duct Tape — 20eb"
        in 31..35 -> "Inflatable Bed & Sleep-bag — 20eb"
        in 36..40 -> "Lock Picking Set — 20eb"
        in 41..45 -> "Handcuffs — 50eb"
        in 46..50 -> "Medtech Bag — 100eb"
        in 51..55 -> "Tent & Camping Equipment — 50eb"
        in 56..60 -> "Rope (60m/yds) — 20eb"
        in 61..65 -> "Techtool — 100eb"
        in 66..70 -> "Personal CarePak — 20eb"
        in 71..75 -> "Radiation Suit — 1,000eb"
        in 76..80 -> "Road Flare — 10eb"
        in 81..85 -> "Grapple Gun — 100eb"
        in 86..90 -> "Tech Bag — 500eb"
        in 91..95 -> "Shovel or Axe — 50eb"
        else -> "Airhypo — 50eb"
    }
}

private fun generateMarket(tier: MarketTier): MarketSnapshot {
    if (tier == MarketTier.MIDNIGHT) {
        val count = com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "Midnight Market — تعداد آیتم (1d10+5)") + 5
        val catalog = com.cyberpunk.gmtool.data.StoreCatalog.items
        val rare = catalog.filter { it.basePrice >= 1000 || it.category == "Vehicles" }.ifEmpty { catalog }
        val picks = rare.shuffled().take(count.coerceAtMost(rare.size)).map { "${it.name} — ${it.basePrice}eb" }
        return MarketSnapshot(System.currentTimeMillis(), gtr("Midnight Market %1s", System.currentTimeMillis().toString().takeLast(4)), tier,
            listOf("Rare & Sought-after • GM choice"), picks)
    }

    val cats = randomMarketCategories()
    val picks = mutableListOf<String>()
    cats.forEach { cat ->
        val count = com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "Night Market — تعداد آیتم دسته")
        val usedRolls = mutableSetOf<Int>()
        repeat(count) {
            var r: Int
            do { r = com.cyberpunk.gmtool.data.DiceSource.rollOne(100, "Night Market — d100") } while (!usedRolls.add(r))
            picks += "[$cat • d100:$r] ${coreNightMarketEntry(cat, r)}"
        }
    }
    return MarketSnapshot(System.currentTimeMillis(), gtr("Night Market %1s", System.currentTimeMillis().toString().takeLast(4)), tier, cats, picks)
}

@Composable
private fun marketCatalogItem(line: String): com.cyberpunk.gmtool.data.StoreItem? {
    val clean = line.substringAfter("] ", line).substringBefore(" — ").trim()
    val alias = if (clean.equals("Bows / Crossbow", true)) "Bow" else clean
    return com.cyberpunk.gmtool.data.CoreItemReference.resolve(alias)
}

private fun marketPrice(line: String): Int? = Regex("([0-9,]+)eb").find(line)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()

@Composable
private fun NightMarketTool(viewModel: CharacterViewModel? = null) {
    // عملیات قواعدیِ تاس‌دار در حالت «تاس دستی» باید بیرون از نخ اصلی اجرا شوند؛
    // DiceSource برای گرفتن عدد از GM نخ فراخوان را بلاک می‌کند و روی نخ اصلی این
    // ممکن نیست (پس ناچار به تصادفی می‌افتد و هشدار می‌دهد).
    val rulesScope = rememberCoroutineScope()
    fun rules(block: () -> Unit) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            rulesScope.launch(kotlinx.coroutines.Dispatchers.Default) { block() }
        } else block()
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gm_night_markets", android.content.Context.MODE_PRIVATE) }
    val saved = remember {
        val list = mutableStateListOf<MarketSnapshot>()
        runCatching {
            val arr = org.json.JSONArray(prefs.getString("items", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i); val ca = o.getJSONArray("categories"); val ia = o.getJSONArray("items")
                val sold = o.optJSONArray("sold")?.let { sa -> (0 until sa.length()).map { j -> sa.getInt(j) }.toSet() } ?: emptySet()
                list.add(MarketSnapshot(o.getLong("id"), o.getString("name"), MarketTier.valueOf(o.getString("tier")),
                    List(ca.length()) { j -> ca.getString(j) }, List(ia.length()) { j -> ia.getString(j) }, sold))
            }
        }
        list
    }
    var tier by remember { mutableStateOf(MarketTier.NIGHT) }
    var current by remember { mutableStateOf<MarketSnapshot?>(saved.firstOrNull()) }
    val characters = viewModel?.characters?.collectAsState()?.value.orEmpty().filter { it.isAlly }
    var buyerId by remember { mutableStateOf<Int?>(null) }
    var purchaseMessage by remember { mutableStateOf<String?>(null) }
    var itemDetailLabel by remember { mutableStateOf<String?>(null) }
    val buyer = characters.firstOrNull { it.id == buyerId }

    fun persist() {
        val arr = org.json.JSONArray()
        saved.forEach { m -> arr.put(org.json.JSONObject().put("id", m.id).put("name", m.name).put("tier", m.tier.name)
            .put("categories", org.json.JSONArray(m.categories)).put("items", org.json.JSONArray(m.items)).put("sold", org.json.JSONArray(m.soldIndices.toList()))) }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(gtr("Night Market Generator"), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                com.cyberpunk.gmtool.ui.components.FaText(
                    "Night Market مطابق Core دو دسته‌ی متفاوت کالا رول می‌کند و برای هر دسته 1d10 نوع کالا از جدول d100 می‌سازد. Midnight Market شامل 1d10+5 آیتم نادر و مورد انتخاب GM است.",
                    color = Muted, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MarketTier.entries.forEach { t ->
                        FilterChip(selected = tier == t, onClick = { tier = t }, label = { Text(gtr(t.label), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red, containerColor = CardBg), modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Button(onClick = { rules { val m = generateMarket(tier); saved.add(0, m); current = m; persist() } },
                    colors = ButtonDefaults.buttonColors(containerColor = Red), shape = CutCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Black); Spacer(Modifier.width(7.dp))
                    Text(gtr("GENERATE & SAVE MARKET"), color = Black, fontWeight = FontWeight.Bold)
                }
            }
            if (viewModel != null && characters.isNotEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(8.dp)).padding(10.dp)) {
                        Text("خریدار", color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            characters.take(4).forEach { c ->
                                FilterChip(selected = buyerId == c.id, onClick = { buyerId = c.id }, label = { Text(c.name.take(12)) })
                            }
                        }
                        buyer?.let { Text(gtr("%1s • %2seb", it.name, it.eurodollars), color = White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) }
                        purchaseMessage?.let { Text(it, color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) }
                    }
                }
            }
            current?.let { m ->
                item {
                    Column(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(10.dp)).border(1.5.dp, Red, CutCornerShape(10.dp)).padding(14.dp)) {
                        Text(gtr(m.name), color = White, fontWeight = FontWeight.Bold, fontSize = 19.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(m.categories.joinToString(" • "), color = Red, modifier = Modifier.fillMaxWidth().padding(top = 3.dp), textAlign = TextAlign.Right)
                        Spacer(Modifier.height(10.dp))
                        m.items.forEachIndexed { i, item ->
                            val def = marketCatalogItem(item)
                            val price = def?.basePrice ?: marketPrice(item)
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (viewModel != null && buyer != null && def != null && price != null) {
                                    val sold = i in m.soldIndices
                                    TextButton(onClick = {
                                        val result = viewModel.buyItem(buyer.id, def.name, def.category, price)
                                        if (result == "OK") {
                                            val updated = m.copy(soldIndices = m.soldIndices + i)
                                            current = updated
                                            val idx = saved.indexOfFirst { it.id == m.id }; if (idx >= 0) saved[idx] = updated
                                            persist()
                                            purchaseMessage = "خرید ${def.name} انجام شد • -${price}eb • موجودی این ورودی فروخته شد"
                                        } else purchaseMessage = result
                                    }, enabled = !sold && buyer.eurodollars >= price) { Text(if(sold) "SOLD" else gtr("BUY %1seb", price), color = if(!sold && buyer.eurodollars >= price) Red else Muted, fontSize = 10.sp) }
                                }
                                // خودِ سطر جزئیات را باز می‌کند؛ دکمه‌ی ⓘ جداگانه حذف شد چون
                                // دقیقاً همان دیالوگ را باز می‌کرد و فقط عرض سطر را می‌خورد.
                                // برچسب کاملاً لاتین است ("[Cyberware • d100:48] Cybereye — 100eb")
                                // پس باید چپ‌به‌راست بماند؛ راست‌چین کردنش قیمت را به سطر بعد می‌انداخت.
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        "${i + 1}. $item", color = White, fontSize = 13.sp, lineHeight = 20.sp,
                                        textAlign = TextAlign.Left,
                                        modifier = Modifier.weight(1f).clickable { itemDetailLabel = def?.name ?: item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (saved.isNotEmpty()) {
                item { Text("مارکت‌های ذخیره‌شده", color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) }
                items(saved.size, key = { saved[it].id }) { i ->
                    val m = saved[i]
                    Row(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(8.dp)).clickable { current = m }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(gtr(m.name), color = White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                            Text(gtr("%1s • %2s item types", m.tier.label, m.items.size), color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        }
                        IconButton(onClick = { saved.removeAt(i); if (current?.id == m.id) current = saved.firstOrNull(); persist() }) { Icon(Icons.Default.Delete, null, tint = Muted) }
                    }
                }
            }
        }
    }

    itemDetailLabel?.let { label ->
        CoreItemDetailDialog(label = label, onDismiss = { itemDetailLabel = null })
    }
}

// ================= ساعت‌ها (Clocks) =================
// Clocks یک ابزار روایی GM است، نه مکانیک الزامی Core. هر Segment یک قدم تا رخداد را نشان می‌دهد.
private data class ClockData(val id: Long, val name: String, val segments: Int, val filled: Int = 0)

@Composable
private fun ClocksTool() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gm_clocks", android.content.Context.MODE_PRIVATE) }
    val clocks = remember {
        val loaded = mutableStateListOf<ClockData>()
        runCatching {
            val raw = prefs.getString("clocks", null)
            if (!raw.isNullOrBlank()) {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    loaded.add(ClockData(o.getLong("id"), o.getString("name"), o.getInt("segments"), o.getInt("filled")))
                }
            }
        }
        if (loaded.isEmpty()) loaded.addAll(listOf(
            ClockData(1L, "زمان رسیدن پشتیبان", 6),
            ClockData(2L, "هشدار امنیتی", 4),
            ClockData(3L, "بسته‌شدن قرارداد", 8)
        ))
        loaded
    }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(clocks.toList()) {
        val arr = org.json.JSONArray()
        clocks.forEach { c ->
            arr.put(org.json.JSONObject().put("id", c.id).put("name", c.name)
                .put("segments", c.segments).put("filled", c.filled))
        }
        prefs.edit().putString("clocks", arr.toString()).apply()
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text("ساعت پیشرفت / شمارش", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Right)
                Text("هر خانه یک قدم تا رخداد است. با + پیشرفت را ثبت کن؛ وقتی همه‌ی خانه‌ها پر شوند رخداد اتفاق افتاده است.",
                    color = Muted, fontSize = 12.sp, lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), textAlign = TextAlign.Right)
            }
            Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = Red),
                shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(46.dp)) {
                Icon(Icons.Default.Add, null, tint = Black); Spacer(Modifier.width(6.dp))
                Text("ساعت جدید", color = Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
        items(clocks.size, key = { clocks[it].id }) { i ->
            val c = clocks[i]
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp)
                        .background(CardBg, CutCornerShape(8.dp))
                        .border(1.dp, if (c.filled >= c.segments) Red else Red.copy(alpha = 0.35f), CutCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(gtr(c.name), color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                            Text(if (c.filled >= c.segments) "رخداد کامل شد" else "${c.segments - c.filled} قدم باقی مانده",
                                color = if (c.filled >= c.segments) Red else Muted, fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        }
                        IconButton(onClick = { clocks.removeAt(i) }) { Icon(Icons.Default.Delete, gtr("Delete"), tint = Muted) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(c.segments) { segment ->
                            Box(
                                Modifier.weight(1f).height(18.dp)
                                    .background(if (segment < c.filled) Red else Color(0xFF333333), CutCornerShape(4.dp))
                                    .border(1.dp, Red.copy(.45f), CutCornerShape(4.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { clocks[i] = c.copy(filled = 0) }) { Text("ریست", color = Muted) }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { clocks[i] = c.copy(filled = (c.filled - 1).coerceAtLeast(0)) }) {
                            Icon(Icons.Default.RemoveCircle, null, tint = Muted)
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text("${c.filled}/${c.segments}", color = Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { clocks[i] = c.copy(filled = (c.filled + 1).coerceAtMost(c.segments)) }) {
                            Icon(Icons.Default.AddCircle, null, tint = Red)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var segments by remember { mutableStateOf(6) }
        Dialog(onDismissRequest = { showAdd = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(12.dp))) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.padding(18.dp)) {
                        Text("ساعت جدید", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام رخداد") },
                            singleLine = true, colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Red, unfocusedBorderColor = Red.copy(alpha = 0.5f),
                                focusedTextColor = White, unfocusedTextColor = White,
                                focusedLabelColor = Red, unfocusedLabelColor = Muted,
                                cursorColor = Red
                            ), modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right))
                        Spacer(Modifier.height(12.dp))
                        Text("تعداد خانه‌ها: $segments", color = White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { segments = (segments - 1).coerceAtLeast(2) }) { Icon(Icons.Default.Remove, null, tint = Muted) }
                            listOf(4, 6, 8).forEach { n ->
                                FilterChip(selected = segments == n, onClick = { segments = n }, label = { Text("$n") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red, containerColor = CardBg))
                                Spacer(Modifier.width(6.dp))
                            }
                            IconButton(onClick = { segments = (segments + 1).coerceAtMost(12) }) { Icon(Icons.Default.Add, null, tint = Red) }
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = {
                            if (name.isNotBlank()) {
                                clocks.add(ClockData(System.currentTimeMillis(), name.trim(), segments))
                                showAdd = false
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = CutCornerShape(8.dp)) { Text("ساخت", color = Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

// ================= نبرد سریع =================
@Composable
private fun QuickCombat() {
    var result by remember { mutableStateOf("") }
    // عملیات قواعدیِ تاس‌دار در حالت «تاس دستی» باید بیرون از نخ اصلی اجرا شوند؛
    // DiceSource برای گرفتن عدد از GM نخ فراخوان را بلاک می‌کند و روی نخ اصلی این
    // ممکن نیست (پس ناچار به تصادفی می‌افتد و هشدار می‌دهد).
    val rulesScope = rememberCoroutineScope()
    fun rules(block: () -> Unit) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            rulesScope.launch(kotlinx.coroutines.Dispatchers.Default) { block() }
        } else block()
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Bolt, null, tint = Red, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text(gtr("QUICK & DIRTY COMBAT"), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text("برای درگیری‌های ساده: اختلاف توان دو طرف را تخمین بزن و \u20661d10\u2069 بریز.",
                color = Muted, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl))
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().background(CardBg, CutCornerShape(10.dp))
            .border(1.dp, Red.copy(alpha = 0.5f), CutCornerShape(10.dp)).padding(20.dp),
            contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(ltrWrap(result.ifBlank { "نتیجه‌ی رول اینجا نمایش داده می‌شود" }),
                    color = if (result.isNotBlank()) White else Muted, fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl))
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            rules {
                val r = com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "جدول نتیجه‌ی درگیری (d10)")
                result = when {
                    r >= 9 -> "\u20661d10 = $r\u2069  →  پیروزی قاطع بازیکن‌ها"
                    r >= 6 -> "\u20661d10 = $r\u2069  →  پیروزی با هزینه‌ای"
                    r >= 3 -> "\u20661d10 = $r\u2069  →  بن‌بست / عقب‌نشینی"
                    else -> "\u20661d10 = $r\u2069  →  شکست و عارضه"
                }
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = Red),
            shape = CutCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(gtr("ROLL COMBAT"), color = Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

// ================= مرجع لایف‌استایل و مسکن (جدول کتاب‌گونه) =================
private data class LifeDetail(
    val enTitle: String,
    val faTitle: String,
    val tag: String,
    val body: String
)

@Composable
private fun LifeReference() {
    var section by remember { mutableStateOf(0) }
    var detail by remember { mutableStateOf<LifeDetail?>(null) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = section, containerColor = Black, contentColor = Red,
            indicator = { TabRowDefaults.SecondaryIndicator(color = Red, height = 3.dp) }) {
            listOf("لایف‌استایل", "مسکن", "خواب و استراحت").forEachIndexed { i, t ->
                Tab(selected = section == i, onClick = { section = i },
                    text = { Text(gtr(t), color = if (section == i) Red else Muted, fontWeight = FontWeight.Bold) })
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item { Spacer(Modifier.height(14.dp)) }
            when (section) {
                // ---------- جدول لایف‌استایل ----------
                0 -> {
                    item {
                        InfoPanel(LifeData.lifestyleNote)
                    }
                    item { TableHeader(listOf("لایف‌استایل", "هزینه‌ی ماهانه")) }
                    items(LifeData.lifestyles.size) { i ->
                        val l = LifeData.lifestyles[i]
                        TableRow(
                            title = l.faName,
                            subtitle = l.enName,
                            right = "${l.cost} ادی",
                            onClick = {
                                detail = LifeDetail(l.enName, l.faName, "${l.cost} ادی در ماه", l.full)
                            }
                        )
                    }
                }
                // ---------- جدول املاک (مسکن) ----------
                1 -> {
                    item { TableHeader(listOf("املاک", "اجاره (ماهانه)", "خرید")) }
                    items(LifeData.housing.size) { i ->
                        val h = LifeData.housing[i]
                        TableRow(
                            title = h.faName,
                            subtitle = h.enName,
                            right = h.rentLabel,
                            farRight = h.buyLabel,
                            onClick = {
                                detail = LifeDetail(h.enName, h.faName,
                                    "اجاره: ${h.rentLabel}   •   خرید: ${h.buyLabel}", h.full)
                            }
                        )
                    }
                    item { InfoPanel(LifeData.realEstateNote) }
                }
                // ---------- قوانین خواب ----------
                2 -> {
                    item { SectionHeader("قوانین خواب و استراحت") }
                    items(LifeData.sleepRules.size) { i ->
                        val s = LifeData.sleepRules[i]
                        RuleRow(s.faName) {
                            detail = LifeDetail("Sleep & Rest", s.faName, "", s.desc)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    detail?.let { d ->
        LifeDetailDialog(d) { detail = null }
    }
}

@Composable
private fun TableHeader(cols: List<String>) {
    // نوار قرمز توپر جای زیادی می‌گرفت و تم را سنگین می‌کرد؛
    // حالا فقط یک ردیف برچسبِ کم‌رنگ با یک خط نازک زیرش.
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                cols.forEachIndexed { idx, c ->
                    val weight = if (cols.size == 3 && idx == 0) 2f else 1.4f
                    Text(
                        gtr(c), color = Red.copy(alpha = .85f), fontWeight = FontWeight.Bold,
                        fontSize = 11.sp, letterSpacing = 0.5.sp,
                        textAlign = if (idx == 0) TextAlign.Right else TextAlign.Center,
                        modifier = Modifier.weight(weight)
                    )
                }
            }
        }
        HorizontalDivider(color = Red.copy(alpha = .3f), thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SectionHeader(t: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(gtr(t), color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            textAlign = TextAlign.Right)
    }
}

@Composable
private fun TableRow(
    title: String,
    subtitle: String,
    right: String,
    farRight: String? = null,
    onClick: () -> Unit
) {
    // جدول RTL: ستون‌ها از راست = نام، وسط = اجاره، چپ = خرید
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp)
                .background(CardBg, CutCornerShape(8.dp))
                .border(1.dp, Red.copy(alpha = 0.18f), CutCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // نام (سمت راست)
            Column(Modifier.weight(2f)) {
                Text(gtr(title), color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(subtitle), color = Muted, fontSize = 11.sp)
                }
            }
            // اجاره (ستون میانی) — اعداد LTR
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(gtr(right), color = Red, fontSize = 14.sp, fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1.4f).fillMaxWidth())
            }
            // خرید (سمت چپ)
            if (farRight != null) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(farRight), color = Muted, fontSize = 13.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1.4f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun RuleRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() }
            .background(CardBg, CutCornerShape(8.dp)).border(1.dp, Red.copy(alpha = 0.18f), CutCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(gtr(title), color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
        }
        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Red, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun InfoPanel(text: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color(0xFF161616), CutCornerShape(10.dp))
                .border(1.dp, Red.copy(alpha = 0.28f), CutCornerShape(10.dp)).padding(14.dp)
        ) {
            com.cyberpunk.gmtool.ui.components.FaText(gtr(text), color = White, fontSize = 13.sp,
                lineHeight = 23.sp, justify = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LifeDetailDialog(d: LifeDetail, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Black),
            shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f)
                .border(2.dp, Red, CutCornerShape(14.dp))
        ) {
            Column(Modifier.fillMaxSize()) {
                // سربرگ — بلوک قرمز توپر جای خود را به تیره + خط تأکید داد
                Column(Modifier.fillMaxWidth().background(Color(0xFF161616)).padding(16.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(gtr(d.faTitle), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr(d.enTitle), color = Muted, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
                    }
                }
                HorizontalDivider(color = Red, thickness = 2.dp)
                if (d.tag.isNotBlank()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr(d.tag), color = Red, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp))
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }
                HorizontalDivider(color = Red.copy(alpha = 0.3f), thickness = 1.dp)
                // متن کامل (اسکرول‌خور)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp)) {
                        item {
                            com.cyberpunk.gmtool.ui.components.FaText(d.body, color = White,
                                fontSize = 15.sp, lineHeight = 26.sp, justify = true,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp)
                ) {
                    Text("بستن", color = Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}



// ================= ROLE LIFEPATH SYNC HELPERS =================
private fun roleLifepathPairs(character: Character): List<Pair<String, String>> =
    character.lifepath.roleLifepath.lineSequence().mapNotNull { raw ->
        val line = raw.trim()
        if (line.isBlank()) return@mapNotNull null
        val clean = line.replace(Regex("^[^\\p{L}\\p{N}]+"), "").trim()
        val split = clean.indexOf(':')
        if (split <= 0) "Role Lifepath" to clean
        else clean.substring(0, split).trim() to clean.substring(split + 1).trim()
    }.toList()

private fun lifepathValue(character: Character, vararg labels: String): String {
    val pairs = roleLifepathPairs(character)
    return pairs.firstOrNull { (k, _) -> labels.any { wanted -> k.contains(wanted, ignoreCase = true) } }?.second.orEmpty()
}

@Composable
internal fun CharacterSheetLifepathCard(character: Character, roleLabel: String) {
    val pairs = roleLifepathPairs(character)
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
        // همه‌ی متن‌ها راست‌به‌چپ خوانده می‌شوند؛ قبلاً تیتر و توضیح‌ها چپ‌چین
        // بودند و با متن فارسی قاطی می‌شدند.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    gtr("Role Lifepath — از روی برگه‌ی %1s", roleLabel),
                    color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
                Text(
                    "این بخش فقط خواندنی است: هر تغییری در تب BIO بدهی، همین‌جا همان مقدار دیده می‌شود. این صفحه خودش تاسی نمی‌ریزد.",
                    color = Muted, fontSize = 11.sp, lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
                if (pairs.isEmpty()) {
                    Text(
                        "برای این کاراکتر هنوز Role Lifepath ثبت نشده. از تب BIO بخش Role Lifepath آن را بساز یا ویرایش کن.",
                        color = Muted, fontSize = 11.sp, lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                } else pairs.forEach { (label, value) ->
                    Column(Modifier.fillMaxWidth().background(Black, CutCornerShape(6.dp)).padding(9.dp)) {
                        Text(
                            gtr(label), color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                        )
                        Text(
                            value, color = White, fontSize = 12.sp, lineHeight = 19.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }
    }
}

// ================= NOMAD GM ASSISTANT =================
@Composable
private fun NomadGmAssistant(viewModel: CharacterViewModel) {
    val characters by viewModel.characters.collectAsState()
    val nomads = characters.filter { it.role.equals("Nomad", true) && it.isAlly }
    var selectedId by remember(nomads.map { it.id }) { mutableStateOf(nomads.firstOrNull()?.id) }
    val selected = nomads.firstOrNull { it.id == selectedId } ?: nomads.firstOrNull()
    var situation by remember { mutableStateOf(RoleAssistantData.nomadInteractionSituations.first()) }
    var familyNearby by remember { mutableStateOf(true) }
    var urgent by remember { mutableStateOf(false) }
    var hookIndex by remember { mutableIntStateOf(0) }

    // همان قالب مشترک Role Helper: تیتر، کارت کاراکتر، کارت‌های بخش‌دار.
    com.cyberpunk.gmtool.ui.components.RoleHelperPage {
        item {
            com.cyberpunk.gmtool.ui.components.RoleHelperHeader(
                "دستیار Nomad برای GM",
                "Moto، Family و Motorpool مستقیم از Character Sheet خوانده می‌شود: پروفایل Family/Pack، وضعیت Favor و Debt، تصمیم «الان Family وارد داستان شود؟» و Story Hook. Favor و Debt قانون رسمی نیستند و فقط یادداشت GM هستند."
            )
        }
        item {
            com.cyberpunk.gmtool.ui.components.RoleHelperCharacterCard(
                title = "کاراکتر Nomad",
                characters = nomads,
                selectedId = selectedId,
                onSelect = { selectedId = it },
                summary = { c ->
                    gtr(
                        "Moto Rank %1s • Motorpool: %2s انتخاب • Upgrade: %3s",
                        c.roleRank.coerceIn(1, 10), c.nomadMotorpool.size, c.nomadVehicleUpgrades.size
                    )
                },
                chipLabel = { c -> gtr("%1s • Moto %2s", c.handle.ifBlank { c.name }, c.roleRank) }
            )
        }
        selected?.let { c ->
            val rank = c.roleRank.coerceIn(1, 10)
            val profile = c.nomadFamilyProfile ?: NomadFamilyProfile()
            val advice = RoleAssistantData.nomadInteractionAdvice(situation, familyNearby, urgent)
            val hook = RoleAssistantData.nomadHooks[hookIndex % RoleAssistantData.nomadHooks.size]

            item { CharacterSheetLifepathCard(c, "Nomad") }

            item {
                com.cyberpunk.gmtool.ui.components.RoleHelperCard("Moto و Motorpool — از برگه") {
                    com.cyberpunk.gmtool.ui.components.FaText(
                        "RAW: Moto به Drive Land Vehicle، Pilot Air/Sea Vehicle و Air/Land/Sea Vehicle Tech Checkها اضافه می‌شود. پیش از Rank 10 فقط یک وسیله در Motorpool فعال است؛ هر انتخاب اضافه در BIO ثبت می‌شود.",
                        color = com.cyberpunk.gmtool.ui.components.RoleUi.Muted, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(), justify = true
                    )
                    com.cyberpunk.gmtool.ui.components.RoleInnerBox {
                        Text(gtr("Moto Rank: %1s", rank), color = com.cyberpunk.gmtool.ui.components.RoleUi.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        c.nomadMotorpool.forEach { v ->
                            Text(gtr("• %1s", v), color = com.cyberpunk.gmtool.ui.components.RoleUi.White, fontSize = 12.sp)
                        }
                        if (c.nomadVehicleUpgrades.isNotEmpty()) {
                            Text(gtr("Upgradeها: %1s", c.nomadVehicleUpgrades.joinToString("، ")), color = com.cyberpunk.gmtool.ui.components.RoleUi.Amber, fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                com.cyberpunk.gmtool.ui.components.RoleHelperCard("قواعد Core — Moto و Family Motorpool") {
                    RoleLoreData.nomadMotorpoolRules.forEach { (t, b) ->
                        com.cyberpunk.gmtool.ui.components.RoleInnerBox {
                            Text(gtr(t), color = com.cyberpunk.gmtool.ui.components.RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            com.cyberpunk.gmtool.ui.components.FaText(
                                b, color = com.cyberpunk.gmtool.ui.components.RoleUi.Muted, fontSize = 10.sp,
                                lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true
                            )
                        }
                    }
                    com.cyberpunk.gmtool.ui.components.FaText(
                        "آشنایی نامد: رنک Moto به Drive Land Vehicle، Pilot Air Vehicle، Pilot Sea Vehicle و Air/Land/Sea Vehicle Tech اضافه می‌شود (در تب STATS خودکار اعمال شده است).",
                        color = com.cyberpunk.gmtool.ui.components.RoleUi.Muted, fontSize = 10.sp,
                        lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true
                    )
                }
            }

            item {
                com.cyberpunk.gmtool.ui.components.RoleHelperCard("جدول Family Motorpool (Core)") {
                    RoleLoreData.nomadFamilyBands.forEach { band ->
                        val current = rank in band.range
                        com.cyberpunk.gmtool.ui.components.RoleInnerBox {
                            Text(
                                gtr(band.ranksLabelFa) + if (current) "  ← رنک فعلی" else "",
                                color = if (current) com.cyberpunk.gmtool.ui.components.RoleUi.Red
                                else com.cyberpunk.gmtool.ui.components.RoleUi.White,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp
                            )
                            Text(
                                band.vehicles.joinToString(" • "),
                                color = if (current) com.cyberpunk.gmtool.ui.components.RoleUi.White
                                else com.cyberpunk.gmtool.ui.components.RoleUi.Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    com.cyberpunk.gmtool.ui.components.FaText(
                        "هم‌زمان فقط یک Family Vehicle بیرون است؛ با Rank 10 همه بیرون می‌آیند، خرید با قیمت بازار و ارتقا با ۱٬۰۰۰eb انجام می‌شود.",
                        color = com.cyberpunk.gmtool.ui.components.RoleUi.Amber, fontSize = 10.sp,
                        lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true
                    )
                }
            }

            item {
                com.cyberpunk.gmtool.ui.components.RoleHelperCard("ارتقاهای وسایل نقلیه (Core)") {
                    com.cyberpunk.gmtool.ui.components.FaText(
                        RoleLoreData.nomadUpgradePricingNote, color = com.cyberpunk.gmtool.ui.components.RoleUi.Amber,
                        fontSize = 10.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true
                    )
                    RoleLoreData.nomadUpgradeRefs.groupBy { it.groupFa }.forEach { (group, items) ->
                        Text(
                            gtr(group), color = com.cyberpunk.gmtool.ui.components.RoleUi.Red,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()
                        )
                        items.forEach { up ->
                            com.cyberpunk.gmtool.ui.components.RoleInnerBox {
                                Text(
                                    gtr("%1s • Moto %2s+", up.name, up.rankRequired),
                                    color = com.cyberpunk.gmtool.ui.components.RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 11.sp
                                )
                                com.cyberpunk.gmtool.ui.components.FaText(
                                    up.textFa, color = com.cyberpunk.gmtool.ui.components.RoleUi.Muted, fontSize = 10.sp,
                                    lineHeight = 18.sp, modifier = Modifier.fillMaxWidth(), justify = true
                                )
                            }
                        }
                    }
                }
            }

            item {
                com.cyberpunk.gmtool.ui.components.RoleHelperCard("پروفایل Family / Pack") {
                    com.cyberpunk.gmtool.ui.components.FaText(
                        "این بخش برای تداوم داستان ذخیره می‌شود و STAT جدیدی به Nomad نمی‌دهد.",
                        color = com.cyberpunk.gmtool.ui.components.RoleUi.Muted, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(), justify = true
                    )
                    NomadProfileField("نام Family / Pack", profile.familyName, randomOptions = listOf("Aldecaldo Spur", "Iron Trail", "Red Mesa", "Dust Serpents", "Chrome Stallions")) { v -> viewModel.updateNomadFamilyProfile(c.id) { it.copy(familyName = v) } }
                    val sheetPackSize = lifepathValue(c, "اندازه قبیله", "اندازه Pack")
                    val sheetDomain = lifepathValue(c, "نوع قبیله", "نوع Pack", "زمینی", "هوایی", "دریایی")
                    val sheetBusiness = lifepathValue(c, "کار قبیله", "شغل قبیله", "کار Pack", "فعالیت قبیله", "فعالیت Pack")
                    NomadSheetValue("اندازه Pack — از BIO", sheetPackSize.ifBlank { "در Role Lifepath پیدا نشد" })
                    NomadSheetValue("نوع Pack — از BIO", sheetDomain.ifBlank { "در Role Lifepath پیدا نشد" })
                    NomadSheetValue("کار اصلی Pack — از BIO", sheetBusiness.ifBlank { "در Role Lifepath پیدا نشد" })
                    NomadProfileField("موقعیت فعلی Family", profile.currentLocation, randomOptions = listOf("حاشیه Night City", "Badlands شمالی", "Badlands جنوبی", "در راه بازگشت به کمپ")) { v -> viewModel.updateNomadFamilyProfile(c.id) { it.copy(currentLocation = v) } }
                    NomadProfileField("رابطه با PC", profile.relationship, randomOptions = listOf("بسیار صمیمی", "قابل اعتماد", "خوب", "Normal", "کشیده و سرد")) { v -> viewModel.updateNomadFamilyProfile(c.id) { it.copy(relationship = v) } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NomadCounter("Favor", profile.favor, Modifier.weight(1f)) { d -> viewModel.updateNomadFamilyProfile(c.id) { it.copy(favor = (it.favor + d).coerceAtLeast(0)) } }
                        NomadCounter("Debt", profile.debt, Modifier.weight(1f)) { d -> viewModel.updateNomadFamilyProfile(c.id) { it.copy(debt = (it.debt + d).coerceAtLeast(0)) } }
                    }
                    NomadProfileField("مشکل فعلی Family", profile.currentProblem, minLines = 2, randomOptions = listOf("محموله گم شده", "عضو Family دستگیر شده", "کمبود بیمار", "قرارداد سوخته")) { v -> viewModel.updateNomadFamilyProfile(c.id) { it.copy(currentProblem = v) } }
                    NomadProfileField("NPCهای مهم Family", profile.importantNpcs, minLines = 2, randomOptions = listOf("رهبر سخت‌گیر Pack", "راننده بدبین", "مکانیک قابل‌اعتماد", "خواهر کوچک Nomad")) { v -> viewModel.updateNomadFamilyProfile(c.id) { it.copy(importantNpcs = v) } }
                    NomadProfileField("آخرین تعامل / قول", profile.lastInteraction, minLines = 2, randomOptions = listOf("قول یک Favor داده شد", "Pack چشمی رد شد", "خون از یک پرونده پاک شد")) { v -> viewModel.updateNomadFamilyProfile(c.id) { it.copy(lastInteraction = v) } }
                }
            }

            item {
                com.cyberpunk.gmtool.ui.components.HelperCard(
                    "آیا الان Family را وارد داستان کنم؟",
                    "وضعیت صحنه را انتخاب کن تا سطح دخالت، هزینه و قدم بعدی را بگوید."
                ) {
                    com.cyberpunk.gmtool.ui.components.PickerRows(RoleAssistantData.nomadInteractionSituations, situation) { situation = it }
                    com.cyberpunk.gmtool.ui.components.HelperToggle("Family در منطقه نزدیک است", familyNearby) { familyNearby = it }
                    com.cyberpunk.gmtool.ui.components.HelperToggle("درخواست فوری / پرخطر است", urgent) { urgent = it }
                    com.cyberpunk.gmtool.ui.components.RoleInnerBox {
                        Text(gtr(advice.level), color = com.cyberpunk.gmtool.ui.components.RoleUi.Red, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        FaText(advice.titleFa, color = com.cyberpunk.gmtool.ui.components.RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                        FaText(advice.adviceFa, color = com.cyberpunk.gmtool.ui.components.RoleUi.Muted, fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                        FaText("هزینه/تعهد پیشنهادی: ${advice.familyCostFa}", color = com.cyberpunk.gmtool.ui.components.RoleUi.White, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), justify = true)
                    }
                }
            }

            item {
                com.cyberpunk.gmtool.ui.components.RoleHelperCard("Nomad Spotlight — چه زمانی نوبت اوست؟") {
                    com.cyberpunk.gmtool.ui.components.FaText(
                        "• وقتی مسیر، سفر، عبور از Badlands یا تعقیب‌وگریز مهم است.\n• وقتی Crew باید آدم یا محموله را سریع از شهری به شهر دیگر ببرد.\n• وقتی یک وسیله خراب شود و کسی باید آن را در حرکت تعمیر کند.\n• وقتی به یک Family یا Pack نیاز است: نفر، اطلاعات مسیر یا پناهگاه.",
                        color = com.cyberpunk.gmtool.ui.components.RoleUi.Muted, fontSize = 12.sp, lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth(), justify = true
                    )
                }
            }

            item {
                com.cyberpunk.gmtool.ui.components.RoleHelperCard("Story Hook Generator") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(gtr("یک Hook تازه"), color = com.cyberpunk.gmtool.ui.components.RoleUi.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Button(onClick = { hookIndex = (hookIndex + 1) % RoleAssistantData.nomadHooks.size }, colors = ButtonDefaults.buttonColors(containerColor = Red), shape = CutCornerShape(6.dp)) {
                            Text(gtr("بعدی"), color = Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    com.cyberpunk.gmtool.ui.components.RoleInnerBox {
                        Text(hook.titleFa, color = com.cyberpunk.gmtool.ui.components.RoleUi.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        NomadHookLine("درخواست Family", hook.requestFa)
                        NomadHookLine("پیچیدگی", hook.complicationFa)
                        NomadHookLine("Spotlight Nomad", hook.spotlightFa)
                        NomadHookLine("پاداش پیشنهادی", hook.rewardFa)
                    }
                }
            }
        }
    }
}

@Composable
private fun NomadSheetValue(label: String, value: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxWidth().background(Black, CutCornerShape(6.dp)).padding(10.dp)) {
            Text(gtr(label), color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = White, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Text("Read-only • منبع واحد: Character Sheet / BIO", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun NomadProfileField(label: String, value: String, minLines: Int = 1, randomOptions: List<String> = emptyList(), onChange: (String) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (randomOptions.isNotEmpty()) IconButton(onClick = { onChange(randomOptions.random()) }) { Icon(Icons.Default.Casino, "رندوم 1d10", tint = Red) }
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(gtr(label)) },
            minLines = minLines,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Red, focusedLabelColor = Red, unfocusedBorderColor = Color.DarkGray, focusedTextColor = White, unfocusedTextColor = White)
        )
        }
    }
}

@Composable
private fun NomadCounter(label: String, value: Int, modifier: Modifier = Modifier, onDelta: (Int) -> Unit) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Black)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$label: $value", color = White, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onDelta(-1) }) { Text("−", color = Red, fontSize = 20.sp) }
                TextButton(onClick = { onDelta(1) }) { Text("+", color = Red, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun NomadHookLine(label: String, text: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text("$label: $text", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
    }
}

/**
 * گزارش تست ۵.۳: دکمه‌ی «i» کوچک برای باز کردن برگه‌ی جزئیات آیتم‌های NET
 * (برنامه‌ها، ICE، Hardware). هم‌شکل با RuleInfoButton اما به‌جای دیالوگ
 * قانون، netItemDetailLabel را ست می‌کند.
 */
@Composable
private fun NetItemInfoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(32.dp)) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Red.copy(alpha = .14f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Red.copy(alpha = .7f))
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Text("i", color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
