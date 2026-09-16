package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.ChoiceGroup
import com.cyberpunk.gmtool.data.ImageProvider
import com.cyberpunk.gmtool.data.StartingEquipment
import com.cyberpunk.gmtool.data.Stats
import com.cyberpunk.gmtool.data.StreetratData
import com.cyberpunk.gmtool.ui.components.CoreItemDetailDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreetratRegistrationScreen(
    roleName: String,
    onCharacterGenerated: (String, String, Stats, Map<String, Int>, StartingEquipment, List<String>) -> Unit,
    onBack: () -> Unit,
    /**
     * بررسی یکتایی handle در فهرست شخصیت‌ها. گزارش تست ۴.۲: معیار یکتایی
     * فقط Handle است (کلید جفت‌سازی LAN)؛ نام تکراری اشکالی ندارد.
     */
    isHandleTaken: (String) -> Boolean = { _ -> false }
) {
    val cyberpunkRed = Color(0xFFC62828)
    val darkBackground = Color(0xFF0F0F0F)
    val cardBackground = Color(0xFF151515)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var legalName by remember { mutableStateOf("") }
    var streetHandle by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var showFinalPopup by remember { mutableStateOf(false) }

    var rolledStats by remember { mutableStateOf<Stats?>(null) }
    var generatedSkills by remember { mutableStateOf<Map<String, Int>?>(null) }
    var diceRoll by remember { mutableStateOf(1) }
    var generatedEquipment by remember { mutableStateOf<StartingEquipment?>(null) }
    var showGearChoiceDialog by remember { mutableStateOf(false) }
    var itemDetailLabel by remember { mutableStateOf<String?>(null) }
    var pendingChoices by remember { mutableStateOf<List<ChoiceGroup>>(emptyList()) }

    // اصلاح باگ: از mutableStateMapOf استفاده می‌کنیم تا تغییرات در Compose دیده شود
    val userSelections = remember { mutableStateMapOf<Int, String>() }

    val initialHousingAndLifestyle = StreetratData.getInitialLifestyleAndHousing(roleName)
    val startingHousing = initialHousingAndLifestyle.first
    val startingLifestyle = initialHousingAndLifestyle.second

    Scaffold(
        topBar = { CyberpunkHeader(title = gtr("NCPD Citizen Registry"), onBackClick = onBack) },
        containerColor = darkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
                    .border(2.dp, cyberpunkRed, CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
            ) {
                Image(
                    painter = painterResource(id = ImageProvider.getRoleImage(roleName, isPortrait = true)),
                    contentDescription = gtr("Role Portrait"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = gtr("ROLE: %1s", roleName.uppercase()), color = cyberpunkRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = legalName,
                onValueChange = { legalName = it },
                label = { Text(gtr("Legal Name (Optional)"), color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyberpunkRed,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = CutCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = streetHandle,
                onValueChange = { streetHandle = it },
                label = { Text(gtr("Street Handle (Required)"), color = cyberpunkRed) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyberpunkRed,
                    unfocusedBorderColor = cyberpunkRed.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = CutCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = CutCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.DarkGray, CutCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(gtr("TEMPLATE LOCK:"), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(gtr("• Stats: System Generated (1d10)"), color = Color.LightGray, fontSize = 14.sp)
                    Text(gtr("• Skills & Gear: Standard Issue"), color = Color.LightGray, fontSize = 14.sp)
                    Text(gtr("• Lifepath: Randomized (after entry)"), color = Color.LightGray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (streetHandle.isBlank()) {
                        Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("Handle is required in Night City!"), Toast.LENGTH_SHORT).show()
                    } else if (isHandleTaken(streetHandle)) {
                        Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("یک شخصیت دیگر با همین Handle در فهرست هست؛ Handle را عوض کن. (نام تکراری اشکالی ندارد)"), Toast.LENGTH_LONG).show()
                    } else {
                        isGenerating = true
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                            delay(1200)

                            // از DiceSource رد می‌شود تا در حالت تاس دستی،
                            // GM عدد تاس فیزیکی ساخت شخصیت را وارد کند.
                            diceRoll = com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "ساخت شخصیت — جدول STAT")
                            rolledStats = StreetratData.getStatsForRole(roleName, diceRoll)
                            val langRoll = com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "ساخت شخصیت — زبان")
                            generatedSkills = StreetratData.getSkillsForRole(roleName, langRoll)
                            generatedEquipment = StreetratData.getEquipmentForRole(roleName)

                            isGenerating = false

                            if (generatedEquipment?.pendingChoices?.isNotEmpty() == true) {
                                pendingChoices = generatedEquipment!!.pendingChoices
                                userSelections.clear()
                                showGearChoiceDialog = true
                            } else {
                                showFinalPopup = true
                            }
                        }
                    }
                },
                shape = CutCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cyberpunkRed),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(gtr("ACCESSING DATABANKS..."), color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Casino, contentDescription = gtr("Roll"), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(gtr("ROLL & GENERATE"), color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // پاپ‌آپ نهایی
    if (showFinalPopup) {
        Dialog(onDismissRequest = { }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = CutCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(2.dp, cyberpunkRed, CutCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(gtr("REGISTRATION COMPLETE"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(gtr("Identity: %1s '%2s'", legalName, streetHandle), color = cyberpunkRed, fontSize = 16.sp)
                    Text(gtr("Role: %1s", roleName), color = Color.LightGray, fontSize = 16.sp)
                    Text(gtr("Stat Roll: %1s", diceRoll), color = Color.Yellow, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(gtr("Housing: %1s", startingHousing), color = Color.LightGray, fontSize = 16.sp)
                    Text(gtr("Lifestyle: %1s", startingLifestyle), color = Color.LightGray, fontSize = 16.sp)

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showFinalPopup = false
                            val stats = rolledStats
                            val skills = generatedSkills
                            val equipment = generatedEquipment
                            if (stats != null && skills != null && equipment != null) {
                                onCharacterGenerated(
                                    legalName, streetHandle, stats, skills, equipment,
                                    userSelections.values.toList()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cyberpunkRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp)
                    ) {
                        Text(gtr("ACCEPT & ENTER CITY"), color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("Life is unfair. Deal with it."), Toast.LENGTH_LONG).show() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cyberpunkRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cyberpunkRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp)
                    ) {
                        Text(gtr("REROLL LIFEPATH"))
                    }
                }
            }
        }
    }

    // پاپ‌آپ انتخاب تجهیزات
    if (showGearChoiceDialog) {
        Dialog(onDismissRequest = { }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = CutCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(2.dp, Color(0xFFFBC02D), CutCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(gtr("CHOOSE YOUR GEAR"), color = Color(0xFFFBC02D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    pendingChoices.forEachIndexed { index, choiceGroup ->
                        Text(text = "${choiceGroup.category}:", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            choiceGroup.options.forEach { option ->
                                val isSelected = userSelections[index] == option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFFFBC02D) else Color.DarkGray,
                                            shape = CutCornerShape(size = 8.dp)
                                        )
                                        .clickable { userSelections[index] = option },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = option.replace("||", " + "),
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 2,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { itemDetailLabel = option }, contentPadding = PaddingValues(2.dp), modifier = Modifier.width(30.dp)) {
                                            Text("ⓘ", color = if (isSelected) Color.Black else Color(0xFFFBC02D), fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (userSelections.size == pendingChoices.size) {
                                showGearChoiceDialog = false
                                showFinalPopup = true
                            } else {
                                Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("Make all selections to proceed."), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(8.dp)
                    ) {
                        Text(gtr("EQUIP & PROCEED"), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    itemDetailLabel?.let { label ->
        CoreItemDetailDialog(label = label, onDismiss = { itemDetailLabel = null })
    }

}
