package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.StoreCatalog
import com.cyberpunk.gmtool.data.StoreItem
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.ui.components.RuleInfoButton
import com.cyberpunk.gmtool.ui.components.StoreItemInfoButton
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
// توابع توسعه‌ی دامنه‌های تفکیک‌شده‌ی ViewModel در همین پکیج‌اند.
import com.cyberpunk.gmtool.viewmodel.*

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val CardBg = Color(0xFF171717)
private val White = Color(0xFFE8E8E8)
private val Muted = Color(0xFF9E9E9E)

private fun ltr(t: Any?) = "\u2066$t\u2069"

private fun storeReferenceKey(category: String): String = when (category) {
    "Weapons" -> "gear.weapon_types"
    "Armor" -> "gear.armor"
    "Cyberware" -> "gear.cyberware_install"
    "Ammo" -> "gear.ammo"
    "Programs", "Hardware" -> "gear.cyberdeck"
    "Vehicles" -> "vehicle.driving"
    else -> "economy.price_categories"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    character: Character,
    viewModel: CharacterViewModel,
    costModifier: Int = 100
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Weapons") }
    var detail by remember { mutableStateOf<StoreItem?>(null) }

    if (detail != null) {
        ItemDetailsScreen(
            item = detail!!,
            costModifier = costModifier,
            eurodollars = character.eurodollars,
            onBack = { detail = null },
            onLoot = {
                val item = detail ?: return@ItemDetailsScreen
                val r = viewModel.buyItem(character.id, item.name, item.category, 0)
                if (r == "OK") {
                    Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("${item.name} به‌عنوان غنیمت اضافه شد."), Toast.LENGTH_SHORT).show()
                    detail = null
                } else {
                    Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast(r), Toast.LENGTH_SHORT).show()
                }
            },
            onBuy = onBuy@{ price, quality ->
                val item = detail ?: return@onBuy
                val r = viewModel.buyItem(character.id, item.name, item.category, price, quality)
                if (r != "OK") {
                    Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast(r), Toast.LENGTH_SHORT).show()
                    return@onBuy
                }
                when (item.category) {
                    "Armor" -> Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("${item.name} خریده و پوشیده شد (SP تنظیم شد)."), Toast.LENGTH_LONG).show()
                    "Cyberware" -> Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("${item.name} خریداری شد؛ برای فعال شدن باید از Inventory نصب شود."), Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("${item.name} خریداری شد!"), Toast.LENGTH_SHORT).show()
                }
                detail = null
            }
        )
        return
    }

    val itemsToShow = StoreCatalog.byCategory(selectedTab).filter { item ->
        searchQuery.isBlank() ||
            item.name.contains(searchQuery, ignoreCase = true) ||
            // جست‌وجو در زیرنویس هم بگردد تا مثلاً «SMG» تیر پیستول را پیدا کند
            item.subtitle.contains(searchQuery, ignoreCase = true) ||
            item.ammoType.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(Black)) {
        Spacer(Modifier.height(4.dp))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            ScrollableTabRow(
                selectedTabIndex = StoreCatalog.categories.indexOf(selectedTab),
                containerColor = Black,
                contentColor = Red,
                edgePadding = 12.dp,
                indicator = { pos -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(pos[StoreCatalog.categories.indexOf(selectedTab)]), color = Red, height = 3.dp) },
                divider = { HorizontalDivider(color = Red.copy(alpha = 0.3f), thickness = 1.dp) }
            ) {
                StoreCatalog.categories.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(gtr(tab), color = if (selectedTab == tab) Red else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(gtr("Search"), color = Color.Gray) } },
                leadingIcon = { Icon(Icons.Default.Search, gtr("Search"), tint = Color.White) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Red, unfocusedBorderColor = Red.copy(0.7f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                shape = CutCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 0.dp, bottomStart = 12.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Surface(shape = CutCornerShape(10.dp), color = Red, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("≡", color = Black, fontSize = 28.sp, fontWeight = FontWeight.Black) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = Red.copy(.55f))
            Text(gtr("  CORE  "), color = White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            RuleInfoButton(storeReferenceKey(selectedTab))
            HorizontalDivider(Modifier.weight(1f), color = Red.copy(.55f))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(itemsToShow, key = { it.name + it.category }) { item ->
                StoreRow(item, costModifier) { detail = item }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StoreRow(item: StoreItem, costModifier: Int, onClick: () -> Unit) {
    val price = (item.basePrice * costModifier) / 100
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .background(CardBg, CutCornerShape(6.dp))
            .border(1.dp, Red.copy(alpha = 0.25f), CutCornerShape(6.dp))
            .clickable { onClick() }
            .padding(start = 14.dp, end = 8.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // آیکن باکس‌دار (مخصوصاً Programs)
        Box(
            modifier = Modifier.size(40.dp).clip(CutCornerShape(6.dp))
                .background(Red.copy(alpha = 0.16f)).border(1.dp, Red, CutCornerShape(6.dp)).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (item.category) {
                    "Programs" -> "▣"
                    "Weapons" -> "▰"
                    "Cyberware" -> "▤"
                    "Armor" -> "▥"
                    "Vehicles" -> "▮"
                    else -> "▢"
                },
                color = Red, fontSize = 18.sp, fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.width(10.dp))
        // نام + زیرنویس (LTR چون انگلیسی‌اند)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.weight(1f)) {
                Text(gtr(item.name), color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                val sub = item.subtitle.ifBlank { item.category }
                Text(gtr(sub), color = Muted, fontSize = 13.sp, maxLines = 1)
            }
        }
        if (!item.category.equals("Cyberware", true)) StoreItemInfoButton(item.name, item.category)
        // دکمه‌ی قیمت هشت‌ضلعی
        Box(
            modifier = Modifier
                .clip(CutCornerShape(8.dp))
                .background(Red)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(gtr("%1seb", price), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

// ================= صفحه‌ی جزئیات آیتم =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailsScreen(
    item: StoreItem,
    costModifier: Int,
    eurodollars: Int,
    onBack: () -> Unit,
    onLoot: () -> Unit,
    onBuy: (Int, String) -> Unit
) {
    val std = (item.basePrice * costModifier) / 100
    // Weapon Quality changes the Core price category first; Fixer/Haggle modifier applies to the resulting price.
    val poor = (StoreCatalog.qualityPrice(item.basePrice, "Poor") * costModifier) / 100
    val exc = (StoreCatalog.qualityPrice(item.basePrice, "Excellent") * costModifier) / 100
    val isWeapon = item.category == "Weapons"
    val isProgram = item.category == "Programs"

    Column(Modifier.fillMaxSize().background(Black)) {
        // نوار بالا
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = gtr("Back"), tint = Red)
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(Modifier.weight(1f)) {
                    Text(gtr(item.name), color = White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(gtr(item.subtitle.ifBlank { item.category }), color = Muted, fontSize = 12.sp)
                }
            }
            if (!item.category.equals("Cyberware", true)) StoreItemInfoButton(item.name, item.category)
        }
        HorizontalDivider(color = Red.copy(0.4f))

        LazyColumn(Modifier.fillMaxSize().weight(1f).padding(16.dp)) {
            // آمار اسلحه
            if (isWeapon && item.damage.isNotBlank()) {
                item {
                    StatGrid(
                        listOfNotNull(
                            "Standard Cost" to "${std}eb",
                            item.damage.takeIf { it.isNotBlank() }?.let { "Damage" to it },
                            item.weaponSkill.takeIf { it.isNotBlank() }?.let { "Weapon Skill" to it },
                            item.concealed.takeIf { it.isNotBlank() }?.let { "Concealed" to it },
                            item.hands.takeIf { it.isNotBlank() }?.let { "Hands" to it },
                            item.rof.takeIf { it.isNotBlank() }?.let { "Rate of Fire" to it },
                            item.ammoType.takeIf { it.isNotBlank() }?.let { "Ammo Type" to it },
                            item.autofire.takeIf { it.isNotBlank() }?.let { "Autofire" to "+${item.autofire}" }
                        )
                    )
                    if (item.magStd != null) {
                        Spacer(Modifier.height(10.dp))
                        MagTable(item.magStd, item.magExt, item.magDrum)
                    }
                    if (item.modes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        InfoCard("حالت‌های شلیک / کاربرد", item.modes, ltrText = true)
                    }
                    if (item.rangeSingle.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        RangeTable("جدول DV شلیک تک‌تیر (Single Shot)", item.rangeSingle)
                    }
                    if (item.rangeAuto.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        RangeTable("جدول DV رگبار (Autofire)", item.rangeAuto)
                    }
                }
            }
            // آمار برنامه
            if (isProgram) {
                item {
                    StatGrid(listOfNotNull(
                        "Cost" to "${std}eb",
                        "Class" to item.programClass.ifBlank { "Program" },
                        item.atk?.let { "ATK" to "$it" },
                        item.def?.let { "DEF" to "$it" },
                        item.rez?.let { "REZ" to "$it" },
                        item.per?.let { "PER" to "$it" },
                        item.spd?.let { "SPD" to "$it" }
                    ))
                }
            }
            // آمار سایبرویر
            if (item.category == "Cyberware") {
                item {
                    val rule = com.cyberpunk.gmtool.data.CyberwareCatalog.ruleFor(item.name)
                    StatGrid(listOfNotNull(
                        "Standard Cost" to "${std}eb",
                        (rule?.install ?: item.slot).takeIf { it.isNotBlank() }?.let { "Install" to it },
                        rule?.group?.let { "Type" to it },
                        rule?.prerequisite?.let { "Requires" to it },
                        rule?.foundationCapacity?.let { "Provides Slots" to "$it" },
                        rule?.takeIf { !it.noSlot && !it.chipware }?.let { "Slots Used" to "${it.slotsUsed}" },
                        item.humanityLoss?.let { "Chargen HL" to "$it" },
                        (rule?.hlDice ?: item.humanityDice).takeIf { it.isNotBlank() }?.let { "Post-Chargen HL" to it },
                        rule?.takeIf { it.paired }?.let { "Paired" to "Two copies" }
                    ))

                }
            }
            // سایر دسته‌ها
            if (!isWeapon && !isProgram && item.category != "Cyberware") {
                item { InfoCard("قیمت استاندارد", "${ltr(std)} eb") }
            }

            // توضیح فارسی
            if (item.description.isNotBlank()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Card(colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = CutCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Red.copy(0.3f), CutCornerShape(8.dp))) {
                            Column(Modifier.padding(14.dp)) {
                                com.cyberpunk.gmtool.ui.components.FaText(gtr("Description"), color = Red, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(6.dp))
                                com.cyberpunk.gmtool.ui.components.FaText(
                                    com.cyberpunk.gmtool.data.ItemDetailsCatalog.fullDescription(item),
                                    color = White, fontSize = 13.sp, lineHeight = 22.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // کیفیت‌های ساخت اسلحه + دکمه‌های خرید
            if (isWeapon) {
                item {
                    Spacer(Modifier.height(16.dp))
                    QualityRow("Poor (کهنه/قراضه)", poor, eurodollars) { onBuy(poor, "Poor") }
                    Spacer(Modifier.height(8.dp))
                    QualityRow("Standard (استاندارد)", std, eurodollars) { onBuy(std, "Standard") }
                    Spacer(Modifier.height(8.dp))
                    QualityRow("Excellent (عالی/فابریک)", exc, eurodollars) { onBuy(exc, "Excellent") }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

        // دکمه‌های پایین: Loot / Buy
        HorizontalDivider(color = Red.copy(0.3f))
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onLoot, shape = CutCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Muted),
                modifier = Modifier.weight(1f).height(50.dp)) {
                Icon(Icons.Default.CardGiftcard, null, tint = Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr("Loot"), color = White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
            if (!isWeapon) {
                Button(onClick = { onBuy(std, "Standard") }, enabled = eurodollars >= std,
                    colors = ButtonDefaults.buttonColors(containerColor = Red, disabledContainerColor = Color(0xFF3A3A3A)),
                    shape = CutCornerShape(10.dp), modifier = Modifier.weight(1f).height(50.dp)) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr("Buy  %1seb", std), color = if (eurodollars >= std) Color.Black else Muted,
                            fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            } else {
                // برای اسلحه، دکمه‌ی Buy نسخه‌ی استاندارد هم اینجا
                Button(onClick = { onBuy(std, "Standard") }, enabled = eurodollars >= std,
                    colors = ButtonDefaults.buttonColors(containerColor = Red, disabledContainerColor = Color(0xFF3A3A3A)),
                    shape = CutCornerShape(10.dp), modifier = Modifier.weight(1f).height(50.dp)) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(gtr("Buy Standard  %1seb", std), color = if (eurodollars >= std) Color.Black else Muted,
                            fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatGrid(pairs: List<Pair<String, String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Red.copy(0.3f), CutCornerShape(8.dp))) {
        Column(Modifier.padding(12.dp)) {
            pairs.chunked(2).forEach { rowItems ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    rowItems.forEach { (k, v) ->
                        Column(Modifier.weight(1f)) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(k.uppercase(), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(gtr(v), color = White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MagTable(std: Int, ext: Int?, drum: Int?) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Red.copy(0.3f), CutCornerShape(8.dp))) {
        Column(Modifier.padding(12.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text("MAGAZINE (ظرفیت خشاب)", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOfNotNull(
                        "Standard" to std,
                        ext?.let { "Extended" to it },
                        drum?.let { "Drum" to it }
                    ).forEach { (k, v) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(v.toString(), color = Red, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(gtr(k), color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeTable(title: String, rows: List<Pair<String, Int>>) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Red.copy(0.3f), CutCornerShape(8.dp))) {
            Column(Modifier.padding(12.dp)) {
                Text(gtr(title), color = Red, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(Modifier.fillMaxWidth()) {
                        rows.forEach { (range, dv) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(gtr(range), color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(dv.toString(), color = White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(gtr("RANGE (m) →   DV"), color = Muted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, ltrText: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Red.copy(0.3f), CutCornerShape(8.dp))) {
        Column(Modifier.padding(12.dp)) {
            Text(gtr(title), color = Red, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            CompositionLocalProvider(LocalLayoutDirection provides
                if (ltrText) LayoutDirection.Ltr else LayoutDirection.Rtl) {
                Text(gtr(body), color = White, fontSize = 14.sp,
                    textAlign = if (ltrText) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun QualityRow(label: String, price: Int, money: Int, onBuy: () -> Unit) {
    val ok = money >= price
    Row(
        Modifier.fillMaxWidth().background(CardBg, CutCornerShape(8.dp))
            .border(1.dp, Red.copy(0.3f), CutCornerShape(8.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(gtr(label), color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
        }
        Button(onClick = onBuy, enabled = ok,
            colors = ButtonDefaults.buttonColors(containerColor = Red, disabledContainerColor = Color(0xFF3A3A3A)),
            shape = CutCornerShape(8.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(gtr("Buy  %1seb", price),
                    color = if (ok) Color.Black else Muted, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}
