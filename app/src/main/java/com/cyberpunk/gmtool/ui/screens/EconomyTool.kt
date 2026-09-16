package com.cyberpunk.gmtool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Restaurant
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
import com.cyberpunk.gmtool.data.gtr
import com.cyberpunk.gmtool.ui.components.CoreItemDetailDialog
import com.cyberpunk.gmtool.ui.components.FaText
import com.cyberpunk.gmtool.ui.components.RuleInfoButton
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel

private val ERed = Color(0xFFD32F2F)
private val EWhite = Color(0xFFE0E0E0)
private val EMuted = Color(0xFFAAAAAA)
private val ECard = Color(0xFF161616)
private val EDim = Color(0xFF1F1F1F)
private val EGold = Color(0xFFFFB300)
private val EGreen = Color(0xFF4CAF50)

private val lifestyles = linkedMapOf(
    "Kibble" to 100, "Generic Prepak" to 300, "Good Prepak" to 600, "Fresh Food" to 1500
)
private val housing = linkedMapOf(
    "Living on The Street" to 0, "Living on The Street in a Vehicle" to 0,
    "Cube Hotel" to 500, "Cargo Container" to 1000, "Studio Apartment" to 1500,
    "Two-Bedroom Apartment" to 2500, "Corporate Conapt" to 0, "Upscale Conapt" to 7500,
    "Luxury Penthouse" to 15000, "Corporate Beaverville House" to 0,
    "Corporate Beaverville McMansion" to 0
)
private val services = linkedMapOf(
    "Bodysculpting (Standard)" to 500, "Bodysculpting (Exotic)" to 1000, "Braindance" to 20,
    "Drink, Dive/Good Bar" to 10, "Drink, Excellent Bar" to 20, "Drink, Executive Bar" to 50,
    "Cyberware Installation (Mall)" to 100, "Cyberware Installation (Clinic)" to 500,
    "Cyberware Installation (Hospital)" to 1000, "Hospital Treatment DV10" to 50,
    "Hospital Treatment DV13" to 100, "Hospital Treatment DV15" to 500,
    "Hospital Treatment DV17+" to 1000, "Hotel / Night" to 100, "Luxury Hotel / Night" to 500,
    "Interactive Braindance" to 50, "Live Concert / Sporting Event" to 100,
    "Professional Service, Good / Hour" to 100, "Professional Service, Excellent / Hour" to 500,
    "Professional Service, World Class / Job" to 5000, "Restaurant, Fast Food" to 10,
    "Restaurant, Good" to 20, "Restaurant, Excellent" to 50, "Restaurant, World Class" to 500,
    "Taxi" to 20, "Therapy (Standard)" to 500, "Therapy (Extreme/Addiction)" to 1000,
    "Trauma Team Silver / Month" to 500, "Trauma Team Executive / Month" to 1000, "Video Game" to 50
)

private fun serviceHelp(name: String) = when {
    name.startsWith("Bodysculpting") -> "تغییر ظاهر بدن توسط متخصص؛ Standard تغییرات معمول و Exotic تغییرات غیرانسانی/پیچیده‌تر است."
    name.contains("Braindance") -> "هزینه تجربه یا سرگرمی Braindance؛ پرداخت هزینه به‌تنهایی اثر Skill یا درمانی ایجاد نمی‌کند."
    name.startsWith("Drink") -> "قیمت تقریبی یک نوشیدنی بر اساس سطح بار؛ برای ثبت سریع خرج‌های روزمره."
    name.contains("Cyberware Installation") -> "هزینه نصب Cyberware در سطح مرکز درمانی مربوط؛ Humanity Loss و الزامات نصب همچنان طبق قواعد اعمال می‌شود."
    name.startsWith("Hospital Treatment") -> "هزینه درمان بیمارستانی برای DV درج‌شده؛ موفقیت و زمان درمان را طبق قواعد مربوط اجرا کن."
    name.contains("Hotel") -> "هزینه یک شب اقامت در سطح مشخص‌شده؛ جایگزین Housing ماهانه نیست."
    name.contains("Professional Service") -> "دستمزد یک متخصص با کیفیت درج‌شده؛ نتیجه کار در صورت نیاز هنوز می‌تواند Check بخواهد."
    name.startsWith("Restaurant") -> "هزینه یک وعده غذا در سطح رستوران مشخص‌شده."
    name == "Taxi" -> "کرایه معمول یک سفر تاکسی داخل شهر؛ مسیرهای خاص یا خطرناک می‌توانند گران‌تر باشند."
    name.startsWith("Therapy") -> "هزینه Therapy؛ زمان، Medtech واجد شرایط و DV درمان همچنان باید طبق قواعد اجرا شوند."
    name.startsWith("Trauma Team") -> "حق اشتراک ماهانه Trauma Team در سطح مشخص؛ مزایای دقیق پلن را طبق Reference اجرا کن."
    else -> "هزینه رایج این خدمت در اقتصاد بازی؛ GM شرایط، زمان و Check لازم را جداگانه لحاظ کند."
}

@Composable
fun EconomyTool(viewModel: CharacterViewModel) {
    val chars by viewModel.characters.collectAsState()
    val pcs = chars.filter { it.isAlly }
    var buyerId by remember { mutableStateOf<Int?>(pcs.firstOrNull()?.id) }
    val pc = pcs.firstOrNull { it.id == buyerId }
    var tab by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var itemDetail by remember { mutableStateOf<String?>(null) }
    var pendingSaleBonus by remember(buyerId) { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
        ) {
            // ───── انتخاب شخصیت + موجودی ─────
            item {
                FaText(
                    "هزینه‌های ماهانه، خدمات، فروش و Haggle",
                    color = EMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pcs.forEach { c ->
                        val on = buyerId == c.id
                        Text(
                            c.name.take(14),
                            color = if (on) Color.Black else EWhite,
                            fontSize = 12.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .background(if (on) ERed else EDim, CutCornerShape(7.dp))
                                .border(1.dp, if (on) ERed else EMuted.copy(.35f), CutCornerShape(7.dp))
                                .clickable { buyerId = c.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            pc?.let { c ->
                item {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(ECard, CutCornerShape(9.dp))
                            .border(1.dp, ERed.copy(.25f), CutCornerShape(9.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FaText(gtr("Cash on hand"), color = EMuted, fontSize = 11.sp,
                            modifier = Modifier.weight(1f))
                        Text("${c.eurodollars}eb", color = EGold, fontSize = 19.sp,
                            fontWeight = FontWeight.Black)
                    }
                }
            }

            // ───── تب‌ها ─────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MONTHLY", "SERVICES", "SELL / HAGGLE").forEachIndexed { i, t ->
                        val on = tab == i
                        Box(
                            Modifier.weight(1f)
                                .background(if (on) ERed else Color.Transparent, CutCornerShape(7.dp))
                                .border(1.dp, if (on) ERed else EMuted.copy(.4f), CutCornerShape(7.dp))
                                .clickable { tab = i }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                gtr(t), color = if (on) Color.Black else EWhite,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, maxLines = 1
                            )
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FaText(
                        when (tab) {
                            0 -> "هزینه‌های زندگی ماهانه"; 1 -> "خدمات و هزینه‌های رایج"
                            else -> "فروش و مذاکره فیکسر"
                        },
                        color = ERed, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (tab == 2) RuleInfoButton("fixer.haggle")
                    else RuleInfoButton(
                        if (tab == 0) "هزینه ماهانه" else "خدمات",
                        if (tab == 0) "غذا و مسکن دو هزینه‌ی جدا هستند و می‌توانند جداگانه پرداخت شوند. اگر اول ماه پول لایف‌استایل را ندهی یک هفته مهلت داری؛ وگرنه هر روزی که نمی‌پردازی در شروع روز باید تست مرگ بدهی."
                        else "این فهرست قیمت خدمات رایج را برای مدیریت سریع GM نگه می‌دارد. پرداخت سرویس به معنی موفقیت خودکار Skill Check یا درمان نیست؛ اگر قانون آن سرویس Check یا زمان لازم دارد، همان قانون همچنان اجرا می‌شود."
                    )
                }
            }

            pc?.let { c ->
                when (tab) {
                    0 -> item {
                        MonthlyCard(c.id, c.lifestyle, c.housing, c.eurodollars, viewModel) { message = it }
                    }
                    1 -> items(services.entries.size) { i ->
                        val x = services.entries.elementAt(i)
                        ServiceRow(x.key, x.value, c.eurodollars) {
                            message = if (viewModel.payExpense(c.id, x.value))
                                "پرداخت ${x.value}eb ثبت شد" else "پول کافی نیست"
                        }
                    }
                    else -> {
                        item {
                            HagglePanel(
                                c.role, c.roleRank, c.stats.cool,
                                c.skills.firstOrNull { it.name.equals("Trading", true) }?.level ?: 0
                            ) { text, saleBonus -> message = text; pendingSaleBonus = saleBonus }
                        }
                        items(c.inventory.size) { i ->
                            val inv = c.inventory[i]
                            val base = com.cyberpunk.gmtool.data.CoreItemReference
                                .resolve(inv.name, inv.category)?.basePrice
                            val sellPrice = base?.let { it * (100 + pendingSaleBonus) / 100 }
                            SellRow(inv.name, base, sellPrice, pendingSaleBonus,
                                onDetail = { itemDetail = inv.name },
                                onSell = {
                                    if (sellPrice != null && viewModel.sellInventoryItem(c.id, i, sellPrice)) {
                                        message = "${inv.name} فروخته شد • +${sellPrice}eb"
                                        pendingSaleBonus = 0
                                    }
                                })
                        }
                    }
                }
            }

            message?.let {
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(ERed.copy(.10f), CutCornerShape(8.dp))
                            .border(1.dp, ERed.copy(.4f), CutCornerShape(8.dp))
                            .padding(11.dp)
                    ) {
                        FaText(it, color = EWhite, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    itemDetail?.let { label ->
        CoreItemDetailDialog(label = label, onDismiss = { itemDetail = null })
    }
}

// ─────────────────────── هزینه‌های ماهانه ───────────────────────

@Composable
private fun MonthlyCard(
    id: Int, currentLife: String, currentHouse: String, cash: Int,
    vm: CharacterViewModel, msg: (String) -> Unit
) {
    var life by remember(currentLife) { mutableStateOf(currentLife) }
    var house by remember(currentHouse) { mutableStateOf(currentHouse) }
    val foodCost = lifestyles[life] ?: 100
    val rentCost = housing[house] ?: 0
    val total = foodCost + rentCost

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ── غذا ──
        SectionCard(gtr("Food"), Icons.Default.Restaurant, foodCost) {
            lifestyles.forEach { (n, p) ->
                ChoiceRow(n, if (p == 0) "—" else "${p}eb", life == n) { life = n }
            }
        }

        // ── مسکن ──
        SectionCard(gtr("Housing"), Icons.Default.Bed, rentCost) {
            housing.forEach { (n, p) ->
                ChoiceRow(n, if (p == 0) "—" else "${p}eb", house == n) { house = n }
            }
        }

        // ── جمع و پرداخت ──
        Column(
            Modifier.fillMaxWidth()
                .background(ECard, CutCornerShape(9.dp))
                .border(1.dp, ERed.copy(.3f), CutCornerShape(9.dp))
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FaText(gtr("Monthly total"), color = EWhite, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${total}eb", color = EGold, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth()) {
                FaText(gtr("Food %1seb • Rent %2seb", foodCost, rentCost),
                    color = EMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = ERed.copy(.2f))

            // غذا و اجاره جدا پرداخت می‌شوند — چون سر میز همیشه با هم پرداخت نمی‌شوند.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PayButton(
                    label = gtr("PAY FOOD"), amount = foodCost,
                    enabled = cash >= foodCost && foodCost > 0, modifier = Modifier.weight(1f)
                ) {
                    vm.setLifestyleAndHousing(id, life, house, rentCost)
                    msg(if (vm.payExpense(id, foodCost)) "هزینه‌ی غذا ${foodCost}eb پرداخت شد" else "پول کافی نیست")
                }
                PayButton(
                    label = gtr("PAY RENT"), amount = rentCost,
                    enabled = cash >= rentCost && rentCost > 0, modifier = Modifier.weight(1f)
                ) {
                    vm.setLifestyleAndHousing(id, life, house, rentCost)
                    msg(if (vm.payExpense(id, rentCost)) "اجاره ${rentCost}eb پرداخت شد" else "پول کافی نیست")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.setLifestyleAndHousing(id, life, house, rentCost) },
                    shape = CutCornerShape(7.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ERed),
                    modifier = Modifier.weight(1f)
                ) { Text(gtr("SAVE PLAN"), color = EWhite, fontSize = 12.sp) }

                Button(
                    onClick = {
                        vm.setLifestyleAndHousing(id, life, house, rentCost)
                        msg(if (vm.payExpense(id, total)) "هزینه‌ی کامل ماه ${total}eb پرداخت شد" else "پول کافی نیست")
                    },
                    enabled = cash >= total && total > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ERed, disabledContainerColor = Color(0xFF3A3A3A)
                    ),
                    shape = CutCornerShape(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(gtr("PAY BOTH"), color = if (cash >= total) Color.Black else EMuted,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PayButton(
    label: String, amount: Int, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Button(
        onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = EGreen.copy(.85f), disabledContainerColor = Color(0xFF2A2A2A)
        ),
        shape = CutCornerShape(7.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = if (enabled) Color.Black else EMuted,
                fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text("${amount}eb", color = if (enabled) Color.Black else EMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SectionCard(
    title: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    cost: Int, content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .background(ECard, CutCornerShape(9.dp))
            .border(1.dp, ERed.copy(.18f), CutCornerShape(9.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = ERed, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, color = ERed, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            Text("${cost}eb", color = EGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(2.dp))
        content()
    }
}

/** یک ردیف انتخاب: نام سمت راست، قیمت سمت چپ. ستون‌ها هم‌تراز می‌مانند. */
@Composable
private fun ChoiceRow(name: String, price: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) ERed.copy(.16f) else Color.Transparent, CutCornerShape(6.dp))
            .border(
                1.dp,
                if (selected) ERed.copy(.7f) else EMuted.copy(.18f),
                CutCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(7.dp)
                .background(if (selected) ERed else EMuted.copy(.4f), CutCornerShape(2.dp))
        )
        Spacer(Modifier.width(9.dp))
        // نام‌ها انگلیسی‌اند؛ LTR نگهشان می‌داریم تا «— Living on The Street» نشوند.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                name,
                color = if (selected) EWhite else EMuted,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(price, color = if (selected) EGold else EMuted, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────── خدمات ───────────────────────

@Composable
private fun ServiceRow(name: String, price: Int, cash: Int, onPay: () -> Unit) {
    val afford = cash >= price
    Column(
        Modifier.fillMaxWidth()
            .background(ECard, CutCornerShape(8.dp))
            .border(1.dp, ERed.copy(.15f), CutCornerShape(8.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(name, color = EWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.width(8.dp))
            Text("${price}eb", color = EGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        FaText(serviceHelp(name), color = EMuted, fontSize = 10.sp, lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth())
        Button(
            onClick = onPay, enabled = afford,
            colors = ButtonDefaults.buttonColors(
                containerColor = ERed, disabledContainerColor = Color(0xFF2E2E2E)
            ),
            shape = CutCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(if (afford) gtr("PAY") else gtr("not enough"),
                color = if (afford) Color.Black else EMuted,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────── فروش ───────────────────────

@Composable
private fun SellRow(
    name: String, base: Int?, sellPrice: Int?, bonus: Int,
    onDetail: () -> Unit, onSell: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .background(ECard, CutCornerShape(8.dp))
            .border(1.dp, ERed.copy(.15f), CutCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sellPrice != null) {
            Button(
                onClick = onSell,
                colors = ButtonDefaults.buttonColors(containerColor = ERed),
                shape = CutCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(gtr("SELL %1seb", sellPrice), color = Color.Black,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(9.dp))
        }
        Column(Modifier.weight(1f).clickable(onClick = onDetail)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(name, color = EWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth())
            }
            if (base == null) {
                FaText(gtr("catalog price unknown"), color = EMuted, fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth())
            } else if (bonus > 0) {
                Text("Haggle +$bonus% ready", color = EGreen, fontSize = 10.sp)
            }
        }
    }
}

// ─────────────────────── Haggle ───────────────────────

@Composable
private fun HagglePanel(
    role: String, rank: Int, cool: Int, trading: Int,
    onResult: (String, Int) -> Unit
) {
    var oppCool by remember { mutableStateOf("6") }
    var oppTrading by remember { mutableStateOf("6") }
    var oppRank by remember { mutableStateOf("0") }
    val isFixer = role.equals("Fixer", true)

    Column(
        Modifier.fillMaxWidth()
            .background(ECard, CutCornerShape(9.dp))
            .border(1.dp, ERed.copy(.3f), CutCornerShape(9.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(gtr("FIXER HAGGLE"), color = ERed, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        if (isFixer) {
            Text(gtr("COOL %1s + Trading %2s + Operator %3s", cool, trading, rank),
                color = EWhite, fontSize = 12.sp)
        } else {
            FaText(gtr("This character is not a Fixer; plain Trading gives no Operator benefit."),
                color = EGold, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
        }

        FaText(
            "روش اجرا: COOL و Trading طرف مقابل و Rank Operator او را وارد کن. دکمه رول، 1d10 هر دو طرف را می‌ریزد و مجموع را مقایسه می‌کند. در تساوی، مذاکره موفق نیست.",
            color = EMuted, fontSize = 10.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallNum("Opp COOL", oppCool) { oppCool = it }
            SmallNum("Trading", oppTrading) { oppTrading = it }
            SmallNum("Operator", oppRank) { oppRank = it }
        }

        Button(
            enabled = isFixer,
            onClick = {
                val ar = com.cyberpunk.gmtool.data.CombatRules.rollD10()
                val dr = com.cyberpunk.gmtool.data.CombatRules.rollD10()
                val a = cool + trading + rank + ar.totalDie
                val d = (oppCool.toIntOrNull() ?: 0) + (oppTrading.toIntOrNull() ?: 0) +
                    (oppRank.toIntOrNull() ?: 0) + dr.totalDie
                val success = a > d
                val saleBonus = if (success && rank <= 2) 10 else if (success && rank == 9) 20 else 0
                val deal = when {
                    !success -> "Haggle شکست خورد"
                    rank <= 2 -> "موفق: 10% بهتر در خرید/فروش • برای SELL بعدی فعال شد"
                    rank <= 4 -> "موفق: در خرید 5+ کالای یکسان، یکی رایگان"
                    rank <= 6 -> "موفق: دستمزد هر نفر برای Job تا 20% بیشتر"
                    rank <= 8 -> "موفق: Luxury/Super Luxury نصف الان، نصف یک ماه بعد"
                    rank == 9 -> "موفق: 20% بهتر در خرید/فروش • برای SELL بعدی فعال شد"
                    else -> "موفق: دستمزد Dangerous Job می‌تواند دو برابر شود"
                }
                onResult("Haggle $a vs $d • $deal", saleBonus)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = ERed, disabledContainerColor = Color(0xFF2E2E2E)
            ),
            shape = CutCornerShape(7.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(gtr("ROLL OPPOSED HAGGLE"),
                color = if (isFixer) Color.Black else EMuted, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SmallNum(label: String, v: String, set: (String) -> Unit) =
    OutlinedTextField(
        v, set,
        label = { Text(gtr(label), fontSize = 10.sp) },
        modifier = Modifier.width(105.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = EWhite, unfocusedTextColor = EWhite,
            focusedBorderColor = ERed, unfocusedBorderColor = EMuted
        )
    )
