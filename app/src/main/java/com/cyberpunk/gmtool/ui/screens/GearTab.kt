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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sell
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import com.cyberpunk.gmtool.ui.components.RuleInfoButton
import com.cyberpunk.gmtool.ui.components.StoreItemInfoButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.InventoryItem
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// توابع توسعه‌ی دامنه‌های تفکیک‌شده‌ی ViewModel در همین پکیج‌اند.
import com.cyberpunk.gmtool.viewmodel.*
import kotlinx.coroutines.launch

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)
private val CardBg = Color(0xFF1A1A1A)


private fun gearHelp(tab: String): String = when (tab) {
    "Weapons" -> "سلاح‌ها ابزار حمله‌اند. برای هر حمله به Skill مناسب، ROF، Damage، Range و مهمات توجه کن. بعضی سلاح‌های Exotic استثناهای مخصوص خودشان را دارند."
    "Armor" -> "زره با SP آسیب ورودی را کم می‌کند. اگر آسیب از SP عبور کند، معمولاً SP همان محل ۱ واحد Ablate می‌شود. زره سر و بدن جداگانه پیگیری می‌شوند."
    "Cyberware" -> "Cyberware قابلیت‌های بدن را گسترش می‌دهد، اما نصب آن معمولاً Humanity Loss دارد و بعضی قطعات به Foundational Cyberware یا Option Slot نیاز دارند. خرید با نصب یکی نیست."
    "Clothing" -> "لباس فقط ظاهر نیست؛ در RED سبک پوشش می‌تواند در موقعیت‌های اجتماعی و هویت شخصیت مهم باشد. هر لباسی زره محسوب نمی‌شود."
    "Ammo" -> "نوع مهمات می‌تواند رفتار حمله را تغییر دهد. قبل از شلیک مطمئن شو مهمات با سلاح سازگار است و خشاب به‌اندازه کافی گلوله دارد."
    "Gear" -> "Gear ابزارهای عمومی مأموریت است. بسیاری از آن‌ها Damage نمی‌دهند اما می‌توانند برای Skill Check، ارتباط، نفوذ یا حل مسئله حیاتی باشند."
    "Drugs" -> "Street Drugها اثر اولیه و خطر Secondary Effect دارند. داروهای Pharmaceutical مدتی جدا هستند و استفاده درست از آن‌ها به توانایی Medtech مربوط است."
    "Hardware" -> "Hardware قطعات فیزیکی Cyberdeck است و Slot اشغال می‌کند. قبل از نصب ظرفیت Deck و پیش‌نیاز هر قطعه را بررسی کن."
    "Programs" -> "Programها ابزار Netrunner در NET هستند. Program فعال REZ دارد و بعضی Programها برای Attack، Defense یا Utility استفاده می‌شوند. Black ICE قواعد متفاوتی دارد."
    "Vehicles" -> "وسیله نقلیه MOVE و SDP خودش را دارد. رانندگی سخت، Maneuver و تصادف می‌توانند Check و Damage ایجاد کنند. Nomadها از Moto برای Family Motorpool و Upgradeها استفاده می‌کنند."
    else -> "این بخش موجودی شخصیت را نشان می‌دهد."
}
@Composable
fun GearTab(
    character: Character,
    viewModel: CharacterViewModel,
    costModifier: Int = 100,
    inStore: Boolean = false,
    onStoreStateChange: (Boolean) -> Unit = {}
) {
    // تب‌های دسته‌بندی موجودی — بدون "All"
    val tabs = listOf("Weapons", "Armor", "Cyberware", "Clothing", "Ammo", "Gear", "Drugs", "Hardware", "Programs", "Vehicles")
    var selectedTab by remember { mutableStateOf(tabs[0]) }
    var selectedItemName by remember { mutableStateOf<String?>(null) }
    var selectedItemCategory by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var pendingInstallIndex by remember { mutableStateOf<Int?>(null) }
    var pendingInstallKind by remember { mutableStateOf<String?>(null) }
    // فروش آیتم: ایندکس موجودی که کاربر می‌خواهد بفروشد
    var pendingSellIndex by remember { mutableStateOf<Int?>(null) }
    // نصبی که منتظر تأیید GM است: (ایندکس آیتم، متن هشدار)
    var pendingHumanityWarn by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var pendingSellWeaponId by remember { mutableStateOf<Int?>(null) }

    // عملیات قواعدیِ تاس‌دار (نصب سایبرویر → Humanity Loss، داروهای خیابانی، تراپی)
    // در حالت «تاس دستی» باید بیرون از نخ اصلی اجرا شوند؛ DiceSource برای گرفتن عدد
    // از GM نخ فراخوان را بلاک می‌کند و روی نخ اصلی این ممکن نیست (پس تصادفی می‌ریزد).
    val rulesScope = rememberCoroutineScope()
    fun rules(block: () -> Unit) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            rulesScope.launch(kotlinx.coroutines.Dispatchers.Default) { block() }
        } else block()
    }

    // داخل Store هستیم؟ (هدر در سطح بالای صفحه هندل می‌شود؛ اینجا فقط محتوا)
    if (inStore) {
        StoreScreen(
            character = character,
            viewModel = viewModel,
            costModifier = costModifier
        )
        return
    }

    // سلاح‌ها (مدل Weapon) و آیتم‌های موجودی (مدل InventoryItem)
    val weapons = character.weapons
    val inventory = character.inventory

    Column(modifier = Modifier.fillMaxSize().background(Black)) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
            containerColor = Black,
            contentColor = Red,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]), color = Red, height = 3.dp)
            },
            divider = { HorizontalDivider(color = Red.copy(alpha = 0.3f), thickness = 1.dp) }
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(gtr(tab), color = if (selectedTab == tab) Red else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("راهنمای ${selectedTab}", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
            RuleInfoButton(selectedTab, gearHelp(selectedTab))
        }
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            // آیتم‌های فهرست‌شده در این تب
            val isEmpty = if (selectedTab == "Weapons") weapons.isEmpty()
            else inventory.none { it.category.equals(selectedTab, ignoreCase = true) }

            if (isEmpty) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        "هنوز چیزی در این دسته نداری.\nبرای خرید از دکمه‌ی «فروشگاه» استفاده کن.",
                        color = Muted, fontSize = 16.sp, lineHeight = 25.sp,
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    if (selectedTab == "Drugs" && character.activeDrugEffects.isNotEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, Red.copy(alpha=.45f), CutCornerShape(8.dp))) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(gtr("ACTIVE STREET DRUGS"), color = Red, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        RuleInfoButton("gear.street_drugs")
                                    }
                                    character.activeDrugEffects.forEach { (drug, hours) ->
                                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(gtr(drug), color = White, fontWeight = FontWeight.Bold)
                                                Text(if (hours > 0) "$hours ساعت از Primary Effect باقی مانده" else "Primary Effect تمام شده • Secondary Check آماده است", color = Muted, fontSize = 11.sp)
                                                if (drug.equals("Blue Glass", true)) Text(if (character.addictions.any { it.equals("Blue Glass", true) } && hours > 0) "Addicted + Primary active: flash-out suppressed." else "GM may trigger a flash-out; during it you lose your Action for that Turn.", color = Muted, fontSize = 9.sp)
                                                if (drug.equals("Black Lace", true) && hours > 0) Text(gtr("Seriously Wounded penalties are ignored while Primary Effect is active."), color = Muted, fontSize = 9.sp)
                                            }
                                            StoreItemInfoButton(drug, "Drugs")
                                            if (hours <= 0) {
                                                OutlinedButton(onClick = {
                                                    rules {
                                                        val r = viewModel.resolveStreetDrugSecondary(character.id, drug)
                                                        actionMessage = if (r.startsWith("OK|")) "Secondary Check موفق شد. اثر ثانویه اعمال نشد." else if (r.startsWith("FAIL|")) "Secondary Check شکست خورد • Addiction/Secondary Effect ثبت شد." else r
                                                    }
                                                }) { Text(gtr("SECONDARY"), color = Red, fontSize = 10.sp) }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = {
                                        val r = viewModel.advanceStreetDrugHour(character.id)
                                        actionMessage = when {
                                            r.startsWith("READY|") -> "زمان Drug جلو رفت. Secondary Check آماده: ${r.substringAfter('|')}"
                                            r == "OK" -> "یک ساعت از Primary Effectها کم شد."
                                            else -> r
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) { Text(gtr("ADVANCE 1 IN-GAME HOUR"), color = Red, fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                    if (selectedTab == "Weapons") {
                        items(weapons, key = { "w${it.id}" }) { w ->
                            WeaponRow(w.name, w.category, gtr("%1s • ROF %2s", w.damage, w.rof) +
                                (if (w.magazineSize > 0) " • ${w.currentAmmo}/${w.magazineSize}" else "") +
                                (if (w.name.equals("Battleglove", true) && w.cyberOptions.isNotEmpty()) gtr(" • Options %1s", w.cyberOptions.size) else ""),
                                equipped = w.isEquipped,
                                onToggle = { viewModel.setWeaponEquipped(character.id, w.id, !w.isEquipped) },
                                onClick = { selectedItemName = w.name; selectedItemCategory = "Weapons" },
                                onSell = { pendingSellWeaponId = w.id })
                        }
                    } else {
                        // آیتم‌های موجودی به‌همراه ایندکس اصلی برای دکمه‌ی تجهیز
                        val indexed = inventory.mapIndexedNotNull { idx, item ->
                            if (item.category.equals(selectedTab, ignoreCase = true)) idx to item else null
                        }
                        items(indexed, key = { "g${it.first}${it.second.name}" }) { (idx, item) ->
                            val canEquip = item.category.equals("Armor", true) || item.category.equals("Cyberware", true) || item.category.equals("Weapon Mods", true) || item.category.equals("Programs", true) || item.category.equals("Hardware", true) || item.category.equals("Drugs", true) || (item.category.equals("Gear", true) && item.name.equals("Smart Glasses", true))
                            // SP فعلیِ همین قطعه‌ی زره؛ هر تیری که رد شود این عدد کم می‌شود.
                            // محاسبه در GameRules است تا فقط محلی خوانده شود که این
                            // قطعه واقعاً می‌پوشاند (وگرنه زره‌ی بدن همیشه سالم دیده می‌شد).
                            val gr = com.cyberpunk.gmtool.data.GameRules
                            GearRow(
                                name = item.name,
                                category = item.category,
                                equipped = item.equipped,
                                canEquip = canEquip,
                                spLabel = gr.armorSpLabel(item),
                                spWorn = gr.isArmorWorn(item),
                                onToggleEquip = {
                                    when {
                                        item.category.equals("Gear", true) && item.name.equals("Smart Glasses", true) -> {
                                            val result = viewModel.toggleSmartGlasses(character.id, idx)
                                            actionMessage = if (result.startsWith("OK|WORN")) "Smart Glasses پوشیده شد؛ Cyberoptic Optionهای داخل آن اکنون فعال‌اند." else if (result.startsWith("OK|REMOVED")) "Smart Glasses برداشته شد؛ Optionهای داخل آن غیرفعال‌اند اما از عینک خارج نشده‌اند." else result
                                        }
                                        item.category.equals("Weapon Mods", true) -> { pendingInstallIndex = idx; pendingInstallKind = "weapon" }
                                        item.category.equals("Programs", true) || item.category.equals("Hardware", true) -> {
                                            if (item.installedIn != null) {
                                                val result = viewModel.unloadCyberdeckItem(character.id, idx)
                                                actionMessage = if (result == "OK") "از Cyberdeck خارج شد." else result
                                            } else { pendingInstallIndex = idx; pendingInstallKind = "deck" }
                                        }
                                        item.category.equals("Cyberware", true) && item.installedIn?.startsWith("battleglove:") == true -> {
                                            val result = viewModel.uninstallBattlegloveOption(character.id, idx)
                                            actionMessage = if (result == "OK") "${item.name} از Battleglove خارج شد." else result
                                        }
                                        item.category.equals("Cyberware", true) && item.installedIn != null && character.inventory.any { g -> g.instanceId == item.installedIn && g.name.equals("Smart Glasses", true) } -> {
                                            val result = viewModel.uninstallSmartGlassesOption(character.id, idx)
                                            actionMessage = if (result.startsWith("OK|")) "${item.name} از Smart Glasses خارج شد." else result
                                        }
                                        item.category.equals("Cyberware", true) && !item.equipped && item.installedIn == null && com.cyberpunk.gmtool.data.CyberwareCatalog.groupFor(item.name) == "Cyberoptics" && character.inventory.any { g -> g.category.equals("Gear", true) && g.name.equals("Smart Glasses", true) } -> {
                                            pendingInstallIndex = idx; pendingInstallKind = "cyberoptic"
                                        }
                                        item.category.equals("Cyberware", true) && !item.equipped && item.installedIn == null && com.cyberpunk.gmtool.data.CyberwareCatalog.groupFor(item.name) in setOf("Cyberarm","Cyberlimb") && character.weapons.any { w -> w.name.equals("Battleglove", true) } -> {
                                            pendingInstallIndex = idx; pendingInstallKind = "cyberlimbOption"
                                        }
                                        item.category.equals("Drugs", true) -> rules {
                                            val result = viewModel.useStreetDrug(character.id, idx)
                                            actionMessage = if (result.startsWith("OK|")) {
                                                val parts = result.split('|')
                                                val drug = parts.getOrNull(1) ?: item.name
                                                val hours = parts.getOrNull(2) ?: "?"
                                                val hl = parts.getOrNull(3)?.toIntOrNull() ?: 0
                                                "$drug مصرف شد • Primary Effect: $hours ساعت" + if (hl > 0) " • Humanity Loss موقت: $hl" else ""
                                            } else result
                                        }
                                        else -> {
                                            // نصب سایبرویری که می‌تواند انسانیت را صفر کند، اول تأیید می‌خواهد.
                                            val warn = if (item.category.equals("Cyberware", true) && !item.equipped)
                                                com.cyberpunk.gmtool.data.CyberpsychosisRules.installWarning(item.name, character)
                                            else null
                                            if (warn != null) {
                                                pendingHumanityWarn = idx to warn
                                            } else rules {
                                                val result = viewModel.toggleEquipItem(character.id, idx)
                                                actionMessage = when {
                                                    result.startsWith("OK|") -> {
                                                        val loss = result.split('|').getOrNull(1)?.toIntOrNull() ?: 0
                                                        if (loss > 0) "نصب انجام شد • Humanity Loss: $loss" else "نصب انجام شد • Humanity Loss جدید: 0"
                                                    }
                                                    result == "OK" -> null
                                                    else -> result
                                                }
                                            }
                                        }
                                    }
                                },
                                onClick = { selectedItemName = item.name; selectedItemCategory = item.category },
                                // سایبرورِ نصب‌شده را نمی‌شود فروخت؛ اول باید حذف نصب شود.
                                onSell = if (item.category.equals("Cyberware", true) && item.equipped) null
                                         else ({ pendingSellIndex = idx })
                            )
                        }
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }

            Button(
                onClick = { onStoreStateChange(true) },
                shape = CutCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).height(56.dp).width(160.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = gtr("Store"), tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text(gtr("Store"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
    selectedItemName?.let { itemName ->
        val category = selectedItemCategory ?: selectedTab
        val core = com.cyberpunk.gmtool.data.StoreCatalog.findForInventory(itemName, category)
        val owned = character.inventory.firstOrNull { it.name.equals(itemName, true) && it.category.equals(category, true) }
        Dialog(
            onDismissRequest = { selectedItemName = null; selectedItemCategory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(Modifier.fillMaxSize().background(Black)) {
                Row(Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedItemName = null; selectedItemCategory = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = gtr("Back"), tint = Red, modifier = Modifier.size(30.dp))
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr("Item Details"), color = White, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        if (core != null) {
                            Surface(color = Red, shape = CutCornerShape(8.dp)) {
                                Text("€\$  ${core.basePrice}", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                            }
                        }
                    }
                }
                HorizontalDivider(color = Red, thickness = 2.dp)
                LazyColumn(Modifier.fillMaxSize().weight(1f).padding(horizontal = 24.dp, vertical = 18.dp)) {
                    item {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(gtr(itemName), color = White, fontSize = 29.sp, fontWeight = FontWeight.Medium)
                            Text(gtr(category), color = Muted, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(22.dp))
                        if (core != null) {
                            val stats = mutableListOf<Pair<String,String>>()
                            stats += "STANDARD COST" to "${core.basePrice}eb"
                            if (category.equals("Armor", true)) {
                                com.cyberpunk.gmtool.data.ArmorCatalog.spFor(itemName)?.let { stats += "SP" to it.toString() }
                                com.cyberpunk.gmtool.data.ArmorCatalog.penaltyFor(itemName)?.let { stats += "ARMOR PENALTY" to it.toString() }
                            }
                            if (category.equals("Cyberware", true)) {
                                val r = com.cyberpunk.gmtool.data.CyberwareCatalog.ruleFor(itemName)
                                if (r != null) {
                                    stats += "INSTALL" to r.install
                                    stats += "TYPE" to r.group
                                    r.prerequisite?.let { stats += "REQUIRES" to it }
                                    if (r.foundationCapacity != null) stats += "PROVIDES SLOTS" to r.foundationCapacity.toString()
                                    else if (!r.noSlot && !r.chipware) stats += "SLOTS USED" to r.slotsUsed.toString()
                                    stats += "CHARGEN HL" to r.hl.toString()
                                    if (r.hlDice.isNotBlank()) stats += "HL DICE" to r.hlDice
                                    if (r.paired) stats += "PAIR" to "2 copies"
                                }
                                if (owned != null) {
                                    stats += "INSTALLED" to if (owned.equipped) "YES" else "NO"
                                    stats += "INSTALL COUNT" to owned.cyberwareInstallCount.toString()
                                    stats += "HL PAID (LIFETIME)" to owned.humanityLossPaid.toString()
                                }
                            }
                            if (category.equals("Weapons", true)) {
                                if (core.damage.isNotBlank()) stats += "DAMAGE" to core.damage
                                if (core.weaponSkill.isNotBlank()) stats += "SKILL" to core.weaponSkill
                                if (core.rof.isNotBlank()) stats += "ROF" to core.rof
                            }
                            OwnedDetailStatGrid(stats)
                            Spacer(Modifier.height(16.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Red.copy(alpha=0.35f), CutCornerShape(8.dp))) {
                                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                                        com.cyberpunk.gmtool.ui.components.FaText(gtr("Description"), color = Red, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                        Spacer(Modifier.height(8.dp))
                                        com.cyberpunk.gmtool.ui.components.FaText(
                                            com.cyberpunk.gmtool.data.ItemDetailsCatalog.fullDescription(core).ifBlank { "برای این آیتم توضیح ثبت نشده است." },
                                            color = White, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("برای این آیتم رکورد Core پیدا نشد؛ نام قدیمی Save را بررسی کن.", color = Red)
                        }
                        Spacer(Modifier.height(40.dp))
                    }
                }
                HorizontalDivider(color = Red, thickness = 2.dp)
                Button(onClick = { selectedItemName = null; selectedItemCategory = null }, colors = ButtonDefaults.buttonColors(containerColor = Red), shape = CutCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(18.dp).height(54.dp)) {
                    Text(gtr("Close"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
            }
        }
    }
    pendingInstallIndex?.let { invIdx ->
        val invItem = character.inventory.getOrNull(invIdx)
        if (invItem != null && pendingInstallKind == "weapon") {
            AlertDialog(
                onDismissRequest = { pendingInstallIndex = null; pendingInstallKind = null },
                containerColor = CardBg,
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(gtr("Install %1s", invItem.name), color = White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); RuleInfoButton("gear.weapon_attachments") } },
                text = {
                    LazyColumn {
                        items(character.weapons, key = { "attach_${it.id}" }) { w ->
                            val used = com.cyberpunk.gmtool.data.EquipmentUseRules.attachmentSlotsUsed(w)
                            val err = com.cyberpunk.gmtool.data.EquipmentUseRules.canInstallAttachment(w, invItem.name)
                            TextButton(
                                onClick = {
                                    val result = viewModel.installWeaponAttachment(character.id, invIdx, w.id)
                                    actionMessage = if (result.startsWith("OK|")) "${invItem.name} روی ${w.name} نصب شد • Slots ${result.substringAfterLast('|')}" else result
                                    pendingInstallIndex = null; pendingInstallKind = null
                                }, enabled = err == null, modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(gtr(w.name), color = if (err == null) White else Muted, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                    Text(err ?: gtr("Attachment Slots: %1s/3", used), color = if (err == null) Red else Muted, fontSize = 11.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pendingInstallIndex = null; pendingInstallKind = null }) { Text("بستن", color = Red) } }
            )
        } else if (invItem != null && pendingInstallKind == "cyberlimbOption") {
            val gloves = character.weapons.filter { it.name.equals("Battleglove", true) }
            AlertDialog(
                onDismissRequest = { pendingInstallIndex = null; pendingInstallKind = null }, containerColor = CardBg,
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(gtr("Install %1s", invItem.name), color = White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); RuleInfoButton("gear.external_option_hosts") } },
                text = { Column {
                    Text("محل نصب را انتخاب کن. Option داخل Battleglove کاشته نمی‌شود و Humanity Loss ندارد؛ برای استفاده Battleglove باید Equipped باشد.", color=Muted, fontSize=11.sp, textAlign=TextAlign.Right)
                    TextButton(onClick={
                        rules {
                            val result=viewModel.installCyberware(character.id,invIdx); actionMessage=if(result.startsWith("OK|")) "${invItem.name} داخل بدن نصب شد • Humanity Loss: ${result.split('|').getOrNull(1)?:"0"}" else result
                        }
                        pendingInstallIndex=null; pendingInstallKind=null
                    }, modifier=Modifier.fillMaxWidth()){ Text(gtr("INSTALL IN BODY / CYBERLIMB"), color=Red) }
                    gloves.forEach { glove ->
                        val err=com.cyberpunk.gmtool.data.EquipmentUseRules.canInstallInBattleglove(glove,invItem)
                        TextButton(onClick={
                            val result=viewModel.installBattlegloveOption(character.id,invIdx,glove.id); actionMessage=if(result.startsWith("OK|")) "${invItem.name} داخل Battleglove نصب شد • Slots ${result.substringAfterLast('|')}" else result
                            pendingInstallIndex=null; pendingInstallKind=null
                        }, enabled=err==null, modifier=Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth()) { Text(gtr("BATTLEGLOVE%1s", if(glove.isEquipped) " • EQUIPPED" else ""),color=if(err==null)White else Muted,textAlign=TextAlign.Right,modifier=Modifier.fillMaxWidth()); Text(err?:gtr("Slots %1s/3", com.cyberpunk.gmtool.data.EquipmentUseRules.battlegloveSlotsUsed(glove)),color=if(err==null)Red else Muted,fontSize=10.sp,textAlign=TextAlign.Right,modifier=Modifier.fillMaxWidth()) } }
                    }
                } }, confirmButton={ TextButton(onClick={pendingInstallIndex=null;pendingInstallKind=null}){Text("بستن",color=Red)} }
            )
        } else if (invItem != null && pendingInstallKind == "cyberoptic") {
            val glasses = character.inventory.mapIndexedNotNull { i, it -> if (it.category.equals("Gear", true) && it.name.equals("Smart Glasses", true)) i to it else null }
            AlertDialog(
                onDismissRequest = { pendingInstallIndex = null; pendingInstallKind = null },
                containerColor = CardBg,
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(gtr("Install %1s", invItem.name), color = White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); RuleInfoButton("gear.external_option_hosts") } },
                text = {
                    Column {
                        Text("محل نصب را انتخاب کن. نصب داخل Smart Glasses Humanity Loss ندارد چون Option داخل بدن کاشته نمی‌شود؛ عینک برای استفاده باید پوشیده باشد.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Right)
                        TextButton(onClick = {
                            rules {
                                val result = viewModel.installCyberware(character.id, invIdx)
                                actionMessage = if (result.startsWith("OK|")) "${invItem.name} داخل بدن نصب شد • Humanity Loss: ${result.split('|').getOrNull(1) ?: "0"}" else result
                            }
                            pendingInstallIndex = null; pendingInstallKind = null
                        }, modifier = Modifier.fillMaxWidth()) { Text(gtr("INSTALL IN BODY / CYBEREYE"), color = Red) }
                        glasses.forEach { (gIdx, g) ->
                            val err = com.cyberpunk.gmtool.data.EquipmentUseRules.canInstallInSmartGlasses(character, invItem, g)
                            TextButton(onClick = {
                                val result = viewModel.installSmartGlassesOption(character.id, invIdx, gIdx)
                                actionMessage = if (result.startsWith("OK|")) "${invItem.name} داخل Smart Glasses نصب شد • Slots ${result.substringAfterLast('|')}" else result
                                pendingInstallIndex = null; pendingInstallKind = null
                            }, enabled = err == null, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(gtr("SMART GLASSES%1s", if (g.equipped) " • WORN" else ""), color = if (err == null) White else Muted, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                    Text(err ?: gtr("Slots %1s/2", com.cyberpunk.gmtool.data.EquipmentUseRules.smartGlassesSlotsUsed(character, g.instanceId)), color = if (err == null) Red else Muted, fontSize = 10.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pendingInstallIndex = null; pendingInstallKind = null }) { Text("بستن", color = Red) } }
            )
        } else if (invItem != null && pendingInstallKind == "deck") {
            val decks = character.inventory.mapIndexedNotNull { i, it -> if (it.category.equals("Gear", true) && it.name.contains("Cyberdeck", true)) i to it else null }
            AlertDialog(
                onDismissRequest = { pendingInstallIndex = null; pendingInstallKind = null },
                containerColor = CardBg,
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(gtr("Load %1s", invItem.name), color = White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); RuleInfoButton("gear.cyberdeck") } },
                text = {
                    if (decks.isEmpty()) Text("ابتدا یک Cyberdeck لازم داری.", color = Muted) else LazyColumn {
                        items(decks, key = { "deck_${it.first}" }) { (deckIdx, deck) ->
                            val cap = com.cyberpunk.gmtool.data.EquipmentUseRules.deckCapacityFor(character, deck.name, invItem)
                            val used = com.cyberpunk.gmtool.data.EquipmentUseRules.deckSlotsUsed(character.inventory, deck.instanceId)
                            val need = com.cyberpunk.gmtool.data.EquipmentUseRules.deckSlotCost(invItem)
                            TextButton(onClick = {
                                val result = viewModel.loadCyberdeckItem(character.id, invIdx, deckIdx)
                                actionMessage = if (result.startsWith("OK|")) "${invItem.name} روی ${deck.name} Load شد • ${result.substringAfterLast('|')} slots" else result
                                pendingInstallIndex = null; pendingInstallKind = null
                            }, enabled = used + need <= cap, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(gtr(deck.name), color = White, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                    Text("Slots $used/$cap • نیاز $need", color = if (used + need <= cap) Red else Muted, fontSize = 11.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pendingInstallIndex = null; pendingInstallKind = null }) { Text("بستن", color = Red) } }
            )
        }
    }

    // ---------- دیالوگ فروش آیتم ----------
        // ── هشدار سایبرسایکوزیس ──
    pendingHumanityWarn?.let { (idx, warnText) ->
        AlertDialog(
            onDismissRequest = { pendingHumanityWarn = null },
            containerColor = CardBg,
            title = {
                com.cyberpunk.gmtool.ui.components.FaText(
                    "هشدار انسانیت", color = Red, fontWeight = FontWeight.Black, fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                com.cyberpunk.gmtool.ui.components.FaText(
                    warnText, color = White, fontSize = 13.sp, lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val result = viewModel.toggleEquipItem(character.id, idx)
                    actionMessage = when {
                        result.startsWith("OK|") -> {
                            val loss = result.split('|').getOrNull(1)?.toIntOrNull() ?: 0
                            if (loss > 0) "نصب انجام شد • Humanity Loss: $loss" else "نصب انجام شد"
                        }
                        result == "OK" -> null
                        else -> result
                    }
                    pendingHumanityWarn = null
                }) { Text("با این حال نصب کن", color = Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingHumanityWarn = null }) { Text("انصراف", color = Muted) }
            }
        )
    }

pendingSellIndex?.let { sellIdx ->
        val item = character.inventory.getOrNull(sellIdx)
        if (item == null) { pendingSellIndex = null; return@let }
        val core = com.cyberpunk.gmtool.data.StoreCatalog.findForInventory(item.name, item.category)
        val base = core?.basePrice ?: 0
        // قاعده‌ی Core: کالای دست‌دوم معمولاً نصف قیمت لیست معامله می‌شود.
        var price by remember(sellIdx) { mutableStateOf((base / 2).toString()) }
        SellItemDialog(
            itemName = item.name,
            category = item.category,
            basePrice = base,
            price = price,
            onPriceChange = { price = it },
            onHalf = { price = (base / 2).toString() },
            onFull = { price = base.toString() },
            onDismiss = { pendingSellIndex = null },
            onConfirm = {
                val p = price.toIntOrNull() ?: 0
                val ok = viewModel.sellInventoryItem(character.id, sellIdx, p)
                actionMessage = if (ok) gtr("Sold %1s for %2seb", item.name, p)
                                else gtr("Could not sell this item.")
                pendingSellIndex = null
            }
        )
    }

    // ---------- دیالوگ فروش سلاح ----------
    pendingSellWeaponId?.let { wid ->
        val w = character.weapons.firstOrNull { it.id == wid }
        if (w == null) { pendingSellWeaponId = null; return@let }
        val core = com.cyberpunk.gmtool.data.StoreCatalog.findForInventory(w.name, "Weapons")
        val base = core?.basePrice ?: 0
        var wprice by remember(wid) { mutableStateOf((base / 2).toString()) }
        SellItemDialog(
            itemName = w.name,
            category = "Weapons",
            basePrice = base,
            price = wprice,
            onPriceChange = { wprice = it },
            onHalf = { wprice = (base / 2).toString() },
            onFull = { wprice = base.toString() },
            onDismiss = { pendingSellWeaponId = null },
            onConfirm = {
                val p = wprice.toIntOrNull() ?: 0
                val ok = viewModel.sellWeapon(character.id, wid, p)
                actionMessage = if (ok) gtr("Sold %1s for %2seb", w.name, p) else gtr("Could not sell this item.")
                pendingSellWeaponId = null
            }
        )
    }

    actionMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { actionMessage = null },
            containerColor = CardBg,
            title = { Text(when { msg.contains("Humanity Loss") -> "Cyberware"; msg.contains("Primary Effect") || msg.contains("Secondary") || msg.contains("Drug") -> "Street Drug"; msg.contains("شد") -> "Item Action"; else -> "Action Result" }, color = White, fontWeight = FontWeight.Bold) },
            text = { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text(gtr(msg), color = White, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) } },
            confirmButton = { TextButton(onClick = { actionMessage = null }) { Text("باشه", color = Red) } }
        )
    }

}

@Composable
private fun OwnedDetailStatGrid(stats: List<Pair<String,String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Red.copy(alpha=0.35f), CutCornerShape(8.dp))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            stats.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { (label, value) ->
                        Column(Modifier.weight(1f)) {
                            Text(gtr(label), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(value, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WeaponRow(name: String, category: String, subtitle: String, equipped: Boolean, onToggle: () -> Unit, onClick: () -> Unit, onSell: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .background(if (equipped) Red.copy(alpha = 0.18f) else CardBg, CutCornerShape(6.dp))
            .border(if (equipped) 2.dp else 1.dp, if (equipped) Red else Red.copy(alpha = 0.3f), CutCornerShape(6.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(gtr(name), color = if (equipped) Color.White else White, fontSize = 15.sp, fontWeight = if (equipped) FontWeight.Bold else FontWeight.Medium)
            androidx.compose.material3.Text(if (equipped) "✓ تجهیز شده • $subtitle" else subtitle, color = if (equipped) Red else Muted, fontSize = 12.sp)
        }
        // دکمه‌ی فروش سلاح
        if (onSell != null) {
            IconButton(onClick = onSell, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Sell, contentDescription = gtr("Sell"), tint = Red.copy(alpha = 0.85f), modifier = Modifier.size(19.dp))
            }
        }
        if (equipped) {
            OutlinedButton(
                onClick = onToggle,
                shape = CutCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Red),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("درآوردن", color = Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onToggle,
                shape = CutCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("تجهیز", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun GearRow(
    name: String,
    category: String,
    equipped: Boolean,
    canEquip: Boolean,
    onToggleEquip: () -> Unit,
    onClick: () -> Unit,
    onSell: (() -> Unit)? = null,
    /** «۸ / ۱۱» برای زره — تا SP فعلی همان‌جا دیده شود. */
    spLabel: String? = null,
    spWorn: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .background(if (equipped) Red.copy(alpha = 0.18f) else CardBg, CutCornerShape(6.dp))
            .border(if (equipped) 2.dp else 1.dp, if (equipped) Red else Red.copy(alpha = 0.3f), CutCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(gtr(name), color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            val sub = if (equipped) "✓ تجهیز شده" else category
            androidx.compose.material3.Text(gtr(sub), color = if (equipped) Red else Muted, fontSize = 12.sp)
            if (spLabel != null) {
                // برچسب دوقسمتی («سر ۹ • بدن ۷») فارسی و عدد قاطی دارد، پس از FaText
                // رد می‌شود تا اعداد جابه‌جا نشوند. حالت ساده هم همان‌جا درست می‌ماند.
                com.cyberpunk.gmtool.ui.components.FaText(
                    "SP $spLabel",
                    color = if (spWorn) Color(0xFFFFB300) else Muted,
                    fontSize = 11.sp,
                    fontWeight = if (spWorn) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (!category.equals("Cyberware", true)) StoreItemInfoButton(name, category)
        // دکمه‌ی فروش: فقط وقتی آیتم قابل فروش است (سایبرورِ نصب‌شده فروختنی نیست)
        if (onSell != null) {
            IconButton(onClick = onSell, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Sell,
                    contentDescription = gtr("Sell"),
                    tint = Red.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        if (canEquip) {
            // دکمه‌ی پوشیدن/درآوردن کنار آیتم
            if (equipped) {
                OutlinedButton(
                    onClick = onToggleEquip,
                    shape = CutCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(when { category.equals("Cyberware", true) -> "حذف نصب"; category.equals("Programs", true) || category.equals("Hardware", true) -> "غیرفعال کردن"; category.equals("Drugs", true) -> "پایان اثر"; else -> "درآوردن" }, fontSize = 12.sp, color = Red, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onToggleEquip,
                    shape = CutCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(when { category.equals("Cyberware", true) -> "نصب"; category.equals("Weapon Mods", true) -> "نصب"; category.equals("Programs", true) || category.equals("Hardware", true) -> "فعال کردن"; category.equals("Drugs", true) -> "مصرف"; else -> "تجهیز" }, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        } else {
            androidx.compose.material3.Text(gtr(category), color = Muted, fontSize = 12.sp)
        }
    }
}

/**
 * دیالوگ فروش آیتم.
 * قاعده‌ی Core: کالای دست‌دوم معمولاً حدود نصف قیمت لیست معامله می‌شود،
 * اما عدد نهایی با گیم‌مستر است (چانه‌زنی، کمیابی، وضعیت کالا).
 */
@Composable
private fun SellItemDialog(
    itemName: String,
    category: String,
    basePrice: Int,
    price: String,
    onPriceChange: (String) -> Unit,
    onHalf: () -> Unit,
    onFull: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = CutCornerShape(10.dp),
        title = {
            Text(gtr("Sell Item"), color = Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
        },
        text = {
            Column {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(itemName), color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(gtr(category), color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                if (basePrice > 0) {
                    Text(gtr("List price: %1seb", basePrice), color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onHalf,
                            shape = CutCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
                        ) { Text(gtr("Half (%1seb)", basePrice / 2), fontSize = 12.sp) }
                        OutlinedButton(
                            onClick = onFull,
                            shape = CutCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted)
                        ) { Text(gtr("Full (%1seb)", basePrice), fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text(gtr("This item has no catalog price; set the amount yourself."),
                        color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { v -> if (v.all(Char::isDigit) && v.length <= 7) onPriceChange(v) },
                    label = { Text(gtr("Sale price (eb)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Red,
                        unfocusedBorderColor = Red.copy(alpha = 0.4f),
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = Red,
                        focusedLabelColor = Red,
                        unfocusedLabelColor = Muted
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    gtr("The item is removed from inventory and the amount is added to your eurodollars."),
                    color = Muted, fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = (price.toIntOrNull() ?: -1) >= 0,
                shape = CutCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text(gtr("Sell"), color = Color.Black, fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(gtr("Cancel"), color = Muted) }
        }
    )
}
