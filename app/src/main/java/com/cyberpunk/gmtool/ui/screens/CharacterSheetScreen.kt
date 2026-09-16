package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.ImageProvider
import com.cyberpunk.gmtool.ui.components.RuleInfoButton
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import kotlin.math.max
import kotlin.math.min

enum class MoneyDialogState { CLOSED, MAIN, WITHDRAW, DEPOSIT }

@Composable
fun CharacterSheetScreen(
    character: Character,
    viewModel: CharacterViewModel,
    onBackClick: () -> Unit,
    initialTab: String = "BIO",
    onSwitchCharacter: (Int, String) -> Unit = { _, _ -> }
) {
    val darkBackground = Color(0xFF0F0F0F)
    val cyberpunkRed = Color(0xFFC62828)
    val cardBackground = Color(0xFF151515)

    // از روی جریان زنده‌ی کاراکترها، کاراکتر فعلی را همیشه تازه نگه می‌داریم
    val roster by viewModel.characters.collectAsState()
    val activeCharacter = roster.firstOrNull { it.id == character.id } ?: character

    var currentTab by remember(character.id, initialTab) { mutableStateOf(initialTab.takeIf { it in setOf("BIO", "STATS", "GEAR", "AGENT", "COMBAT") } ?: "BIO") }
    var showCharacterSwitcher by remember { mutableStateOf(false) }

    // آیا داخل صفحه‌ی Store هستیم؟ (هدر اصلی به‌جای هدر تکراری استور استفاده می‌شود)
    var inStore by remember { mutableStateOf(false) }

    var showHumanityDialog by remember { mutableStateOf(false) }
    var showLuckDialog by remember { mutableStateOf(false) }
    var showReputationDialog by remember { mutableStateOf(false) }
    var showHpDialog by remember { mutableStateOf(false) }
    var moneyDialogState by remember { mutableStateOf(MoneyDialogState.CLOSED) }
    var showAdjustDialog by remember { mutableStateOf(false) }

    var costModifier by remember { mutableStateOf(100) }

    Scaffold(
        topBar = {
            CyberpunkHeader(
                title = when {
                    inStore -> "STORE"
                    currentTab == "GEAR" -> "INVENTORY"
                    else -> currentTab
                },
                onBackClick = if (inStore) ({ inStore = false }) else onBackClick,
                rightContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // دکمه‌ی تنظیم ضریب قیمت (فقط داخل Store) — بازه ۰ تا ۱۰۰۰ درصد
                        if (inStore) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(34.dp)
                                    .border(1.dp, cyberpunkRed, CutCornerShape(8.dp))
                                    .clickable { showAdjustDialog = true }
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = gtr("Adjust Cost"), tint = cyberpunkRed, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("$costModifier%", color = cyberpunkRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        val c = activeCharacter
                        val buttonText = when (currentTab) {
                            "STATS" -> gtr("%1s/%2s LUCK", c.currentLuck, c.maxLuck)
                            "AGENT" -> gtr("%1s Reputation", c.reputation)
                            "COMBAT" -> "${c.hp} / ${c.maxHp}"
                            "GEAR" -> "€$ ${c.eurodollars}"
                            else -> gtr("%1s/%2s Humanity", c.currentHumanity, c.maxHumanity)
                        }
                        val buttonIcon = when (currentTab) {
                            "STATS" -> Icons.Default.Star
                            "AGENT" -> Icons.Default.CameraAlt
                            "COMBAT" -> Icons.Default.Favorite
                            "GEAR" -> Icons.Default.ShoppingCart
                            else -> Icons.Default.VolunteerActivism
                        }
                        val onButtonClick = {
                            when (currentTab) {
                                "STATS" -> showLuckDialog = true
                                "AGENT" -> showReputationDialog = true
                                "COMBAT" -> showHpDialog = true
                                "GEAR" -> moneyDialogState = MoneyDialogState.MAIN
                                else -> showHumanityDialog = true
                            }
                        }
                        val headerHelpKey = when {
                            inStore -> "economy.price_categories"
                            currentTab == "STATS" -> "character.luck"
                            currentTab == "AGENT" -> "character.reputation"
                            currentTab == "COMBAT" -> "character.hp"
                            currentTab == "GEAR" -> "economy.eurodollars"
                            else -> "character.humanity"
                        }

                        RuleInfoButton(headerHelpKey, Modifier.size(28.dp))
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = onButtonClick,
                            shape = CutCornerShape(size = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cyberpunkRed.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .border(width = 1.dp, color = cyberpunkRed, shape = CutCornerShape(size = 8.dp))
                        ) {
                            Icon(imageVector = buttonIcon, contentDescription = currentTab, tint = cyberpunkRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = buttonText, color = cyberpunkRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
                                    .background(Color.DarkGray)
                                    .border(width = 1.dp, color = cyberpunkRed, shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
                                    .clickable { showCharacterSwitcher = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = if (activeCharacter.isAlly)
                                        ImageProvider.getRoleImage(activeCharacter.role, isPortrait = true)
                                    else
                                        ImageProvider.getNpcImage(activeCharacter.npcCategory, activeCharacter.role, activeCharacter.name)),
                                    contentDescription = gtr("Current Character Avatar"),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            DropdownMenu(
                                expanded = showCharacterSwitcher,
                                onDismissRequest = { showCharacterSwitcher = false },
                                modifier = Modifier.background(color = cardBackground).border(width = 1.dp, color = cyberpunkRed, shape = CutCornerShape(size = 8.dp))
                                    .widthIn(min = 270.dp)
                            ) {
                                roster.forEach { rosterChar ->
                                    DropdownMenuItem(
                                        text = { RosterMenuItem(character = rosterChar, cyberpunkRed = cyberpunkRed) },
                                        onClick = {
                                            showCharacterSwitcher = false
                                            onSwitchCharacter(rosterChar.id, currentTab)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            CharacterSheetBottomNav(
                selectedTab = currentTab,
                onTabSelected = { newTab -> inStore = false; currentTab = newTab },
                cyberpunkRed = cyberpunkRed
            )
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentTab) {
                "BIO" -> BioTab(character = activeCharacter, viewModel = viewModel, onBack = {})
                "STATS" -> StatsTab(character = activeCharacter, viewModel = viewModel)
                "GEAR" -> GearTab(
                    character = activeCharacter,
                    viewModel = viewModel,
                    costModifier = costModifier,
                    inStore = inStore,
                    onStoreStateChange = { inStore = it }
                )
                "AGENT" -> AgentTab(character = activeCharacter, viewModel = viewModel)
                "COMBAT" -> CombatTab(character = activeCharacter, viewModel = viewModel)
            }
        }
    }

    // ---------- دیالوگ‌ها ----------
    val id = activeCharacter.id

    if (showHumanityDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showHumanityDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            HumanityScreen(
                character = activeCharacter,
                viewModel = viewModel,
                onBack = { showHumanityDialog = false }
            )
        }
    }
    if (showLuckDialog) {
        UniversalStatDialog(
            title = gtr("Luck"),
            currentVal = activeCharacter.currentLuck,
            maxVal = activeCharacter.maxLuck,
            cyberpunkRed = cyberpunkRed,
            onDismiss = { showLuckDialog = false },
            onUpdate = { isCurrent, delta ->
                if (isCurrent) viewModel.adjustCurrentLuck(id, delta)
                else viewModel.adjustMaxLuck(id, delta)
            },
            extraActionLabel = "Refill Luck (Session)",
            onExtraAction = { viewModel.refillLuck(id) }
        )
    }
    if (showReputationDialog) {
        UniversalStatDialog(
            title = gtr("Reputation"),
            currentVal = activeCharacter.reputation,
            maxVal = 10,
            cyberpunkRed = cyberpunkRed,
            showMaxControls = false,
            onDismiss = { showReputationDialog = false },
            onUpdate = { isCurrent, delta -> viewModel.adjustReputation(id, delta) }
        )
    }
    if (showHpDialog) {
        UniversalStatDialog(
            title = gtr("Health"),
            currentVal = activeCharacter.hp,
            maxVal = activeCharacter.maxHp,
            cyberpunkRed = cyberpunkRed,
            onDismiss = { showHpDialog = false },
            onUpdate = { isCurrent, delta ->
                if (isCurrent) viewModel.adjustHp(id, delta)
                else viewModel.adjustMaxHp(id, delta)
            }
        )
    }
    if (moneyDialogState != MoneyDialogState.CLOSED) {
        EurodollarDialog(
            currentMoney = activeCharacter.eurodollars,
            state = moneyDialogState,
            cyberRed = cyberpunkRed,
            cyberBlack = darkBackground,
            onDismiss = { moneyDialogState = MoneyDialogState.CLOSED },
            onStateChange = { moneyDialogState = it },
            onTransaction = { amount -> viewModel.adjustEurodollars(id, amount) }
        )
    }
    if (showAdjustDialog) {
        AdjustCostDialog(
            currentModifier = costModifier,
            cyberRed = cyberpunkRed,
            cyberBlack = darkBackground,
            onDismiss = { showAdjustDialog = false },
            onModifierChange = { costModifier = it }
        )
    }
}

@Composable
fun UniversalStatDialog(
    title: String,
    currentVal: Int,
    maxVal: Int,
    cyberpunkRed: Color,
    onDismiss: () -> Unit,
    onUpdate: (Boolean, Int) -> Unit,
    showMaxControls: Boolean = true,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null
) {
    var isCurrentSelected by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
            shape = CutCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, cyberpunkRed, CutCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = cyberpunkRed.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "—", color = cyberpunkRed, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onUpdate(isCurrentSelected || !showMaxControls, -1) }.padding(horizontal = 16.dp)
                    )
                    Box(
                        modifier = Modifier.border(1.dp, Color.White, CutCornerShape(8.dp)).padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { isCurrentSelected = true }) {
                                Text(text = currentVal.toString(), color = if (isCurrentSelected) cyberpunkRed else Color.LightGray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Text(text = gtr("Current"), color = if (isCurrentSelected) cyberpunkRed else Color.Gray, fontSize = 12.sp)
                            }
                            if (showMaxControls) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "/", color = Color.White, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { isCurrentSelected = false }) {
                                    Text(text = maxVal.toString(), color = if (!isCurrentSelected) Color.White else Color.LightGray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    Text(text = gtr("Max"), color = if (!isCurrentSelected) Color.White else Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Text(
                        text = "+", color = cyberpunkRed, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onUpdate(isCurrentSelected || !showMaxControls, 1) }.padding(horizontal = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (extraActionLabel != null && onExtraAction != null) {
                    Button(
                        onClick = onExtraAction,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = CutCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    ) {
                        Text(text = extraActionLabel, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = cyberpunkRed), shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(48.dp)
                ) {
                    Text(text = gtr("DONE"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun RosterMenuItem(character: Character, cyberpunkRed: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)).background(Color.DarkGray).border(1.dp, cyberpunkRed, CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = if (character.isAlly)
                    ImageProvider.getRoleImage(character.role, isPortrait = true)
                else
                    ImageProvider.getNpcImage(character.npcCategory, character.role, character.name)),
                contentDescription = gtr("%1s Avatar", character.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = character.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(text = character.handle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CharacterSheetBottomNav(selectedTab: String, onTabSelected: (String) -> Unit, cyberpunkRed: Color) {
    val tabs = listOf(
        Pair("BIO", Icons.Default.Person), Pair("STATS", Icons.Default.List), Pair("GEAR", Icons.Default.BusinessCenter),
        Pair("AGENT", Icons.Default.Phone), Pair("COMBAT", Icons.Default.GpsFixed)
    )
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0F0F0F)).border(1.dp, Color(0xFF1A1A1A)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (title, icon) ->
            val isSelected = selectedTab == title
            val bgColor = if (isSelected) cyberpunkRed else Color.Transparent
            val contentColor = if (isSelected) Color.Black else Color.Gray
            Column(
                modifier = Modifier
                    .clip(CutCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(title) }
                    .padding(horizontal = 9.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = contentColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = title, color = contentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

// =======================================================
// دیالوگ مدیریت یورو-دلار
// =======================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EurodollarDialog(
    currentMoney: Int,
    state: MoneyDialogState,
    cyberRed: Color,
    cyberBlack: Color,
    onDismiss: () -> Unit,
    onStateChange: (MoneyDialogState) -> Unit,
    onTransaction: (Int) -> Unit
) {
    var inputValue by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cyberBlack),
            shape = CutCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, cyberRed, CutCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(gtr("Eurodollars"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    MoneyDialogState.MAIN -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray, CutCornerShape(4.dp)).padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("€$", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(currentMoney.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onStateChange(MoneyDialogState.WITHDRAW) },
                                shape = CutCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cyberRed),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text(gtr("— WITHDRAW"), color = Color.Black, fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = { onStateChange(MoneyDialogState.DEPOSIT) },
                                shape = CutCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cyberRed),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text(gtr("+ DEPOSIT"), color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                    }

                    MoneyDialogState.WITHDRAW, MoneyDialogState.DEPOSIT -> {
                        val isDeposit = state == MoneyDialogState.DEPOSIT
                        val labelText = if (isDeposit) "Deposit Amount" else "Withdrawal Amount"
                        val btnText = if (isDeposit) "+ DEPOSIT" else "— WITHDRAW"

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(48.dp).background(Color(0xFF2A2A2A), CutCornerShape(8.dp)).clickable {
                                val current = inputValue.toIntOrNull() ?: 0
                                if (current > 0) inputValue = (current - 1).toString()
                            }, contentAlignment = Alignment.Center) { Text("—", color = Color.White, fontWeight = FontWeight.Bold) }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = inputValue,
                                onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) inputValue = it },
                                label = { Text(gtr(labelText), color = Color.LightGray, fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = cyberRed, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(modifier = Modifier.size(48.dp).background(cyberRed, CutCornerShape(8.dp)).clickable {
                                val current = inputValue.toIntOrNull() ?: 0
                                inputValue = (current + 1).toString()
                            }, contentAlignment = Alignment.Center) { Text("+", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val amount = inputValue.toIntOrNull() ?: 0
                                if (amount > 0) {
                                    val finalAmount = if (isDeposit) amount else -amount
                                    onTransaction(finalAmount)
                                }
                                onStateChange(MoneyDialogState.MAIN)
                            },
                            shape = CutCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cyberRed),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(gtr(btnText), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// =======================================================
// دیالوگ تغییر درصد قیمت فروشگاه
// =======================================================
@Composable
fun AdjustCostDialog(
    currentModifier: Int,
    cyberRed: Color,
    cyberBlack: Color,
    onDismiss: () -> Unit,
    onModifierChange: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cyberBlack),
            shape = CutCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, cyberRed, CutCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(gtr("Store Cost Modifier"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = cyberRed, thickness = 2.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().background(cyberRed).padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val btnModifier = Modifier.height(36.dp)
                    val btnColor = Color(0xFF250A0A)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(100, 10, 1).forEach { step ->
                            Box(contentAlignment = Alignment.Center,
                                modifier = btnModifier.background(btnColor, CutCornerShape(4.dp))
                                    .clickable { onModifierChange(max(0, currentModifier - step)) }
                                    .padding(horizontal = 6.dp)) {
                                Text("-$step%", color = cyberRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("$currentModifier%", color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 10, 100).forEach { step ->
                            Box(contentAlignment = Alignment.Center,
                                modifier = btnModifier.background(btnColor, CutCornerShape(4.dp))
                                    .clickable { onModifierChange(min(1000, currentModifier + step)) }
                                    .padding(horizontal = 6.dp)) {
                                Text("+$step%", color = cyberRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                HorizontalDivider(color = cyberRed, thickness = 2.dp)

                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val isFree = currentModifier == 0
                    val isStd = currentModifier == 100
                    val isMax = currentModifier == 1000

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).height(48.dp)
                            .background(if (isFree) cyberRed else cyberBlack, CutCornerShape(8.dp))
                            .border(2.dp, if (isFree) cyberRed else Color.Gray, CutCornerShape(8.dp))
                            .clickable { onModifierChange(0) }
                    ) {
                        Text(gtr("Free"), color = if (isFree) Color.Black else Color.LightGray, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).height(48.dp)
                            .background(if (isStd) cyberRed else cyberBlack, CutCornerShape(8.dp))
                            .border(2.dp, if (isStd) cyberRed else Color.Gray, CutCornerShape(8.dp))
                            .clickable { onModifierChange(100) }
                    ) {
                        Text(gtr("Standard"), color = if (isStd) Color.Black else Color.LightGray, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).height(48.dp)
                            .background(if (isMax) cyberRed else cyberBlack, CutCornerShape(8.dp))
                            .border(2.dp, if (isMax) cyberRed else Color.Gray, CutCornerShape(8.dp))
                            .clickable { onModifierChange(1000) }
                    ) {
                        Text(gtr("Max 1000%"), color = if (isMax) Color.Black else Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = cyberRed, thickness = 2.dp)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(60.dp).background(cyberBlack).clickable { onDismiss() }
                ) {
                    Text(gtr("DONE"), color = cyberRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
