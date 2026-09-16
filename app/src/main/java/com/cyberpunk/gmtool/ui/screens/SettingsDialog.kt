package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.cyberpunk.gmtool.R
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberpunk.gmtool.ui.LanguageState

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)

@Composable
fun SettingsDialog(
    viewModel: CharacterViewModel,
    onDismiss: () -> Unit
) {
    var confirmWipe by remember { mutableStateOf(false) }
    val languageState: LanguageState = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saveSlots by remember { mutableStateOf(viewModel.listGameSaves()) }
    var saveLabel by remember { mutableStateOf("") }
    var pendingLoad by remember { mutableStateOf<com.cyberpunk.gmtool.data.GameSaveSlot?>(null) }
    var saveBusy by remember { mutableStateOf(false) }
    var showAllSaves by remember { mutableStateOf(false) }

    fun refreshSaves() { saveSlots = viewModel.listGameSaves() }
    fun shareSave(file: java.io.File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export CPRGMTOOLS Game Save"))
    }

    val importGameLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("فایل خالی است")
            }.mapCatching { json -> viewModel.importGameSaveAsSlot(json).getOrThrow() }
            result.onSuccess {
                refreshSaves()
                Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("Backup وارد شد؛ برای اعمال آن Load را بزن."), Toast.LENGTH_LONG).show()
            }.onFailure { Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast(it.message ?: "فایل Backup معتبر نیست"), Toast.LENGTH_LONG).show() }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Black),
            shape = CutCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Text("SETTINGS / تنظیمات", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Red, thickness = 2.dp)
                Spacer(Modifier.height(16.dp))

                // --- تاس خودکار ---
                SettingsToggleRow(
                    icon = Icons.Default.Casino,
                    title = "تاس خودکار (Auto Dice)",
                    description = "روشن: برنامه خودش تاس می‌ریزد.\nخاموش: تاس واقعی بریز و عدد را دستی وارد کن.",
                    checked = viewModel.autoDice
                ) { viewModel.updateAutoDice(it) }

                Spacer(Modifier.height(12.dp))

                // --- لرزش ---
                SettingsToggleRow(
                    icon = Icons.Default.Vibration,
                    title = "لرزش هنگام تاس (Haptics)",
                    description = "لرزش کوتاه هنگام ریختن تاس و تایید.",
                    checked = viewModel.hapticsEnabled
                ) { viewModel.updateHaptics(it) }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Red.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))

                // --- انتخاب زبان ---
                LanguageSettingSection(
                    current = languageState.language,
                    onSelect = { languageState.changeLanguage(it) }
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Red.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))

                Text("SAVE / LOAD کامل بازی", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "کاراکترها، NPCها، کمپین‌ها، سشن‌ها، Combat Queue و State فعال Combat/GM Tools با هم ذخیره می‌شوند. Autosave چرخشی تقریباً هر یک دقیقه ساخته می‌شود و Load دارای rollback خودکار و بازیابی Crash است.",
                    color = Muted, fontSize = 12.sp, textAlign = TextAlign.Right
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = saveLabel, onValueChange = { saveLabel = it },
                    label = { Text("نام Save (اختیاری)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Red, unfocusedBorderColor = Color(0xFF444444),
                        focusedTextColor = White, unfocusedTextColor = White
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !saveBusy,
                        onClick = {
                            saveBusy = true
                            scope.launch {
                                val label = saveLabel.trim().ifBlank { gtr("Manual %1s", SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())) }
                                viewModel.createGameSave(label).onSuccess {
                                    saveLabel = ""
                                    refreshSaves()
                                    Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("بازی کامل ذخیره شد"), Toast.LENGTH_SHORT).show()
                                }.onFailure { Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast(it.message ?: "Save ناموفق بود"), Toast.LENGTH_LONG).show() }
                                saveBusy = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Default.Save, null, tint = Color.Black); Spacer(Modifier.width(6.dp)); Text(gtr("SAVE"), color = Color.Black, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { importGameLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red),
                        shape = CutCornerShape(8.dp), modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Default.FileUpload, null, tint = Red); Spacer(Modifier.width(6.dp)); Text(gtr("IMPORT"), color = Red) }
                }

                if (saveSlots.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    (if (showAllSaves) saveSlots else saveSlots.take(10)).forEach { slot ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .border(1.dp, Color(0xFF333333), CutCornerShape(8.dp)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.deleteGameSave(slot); refreshSaves() }) {
                                Icon(Icons.Default.Delete, gtr("Delete save"), tint = Muted)
                            }
                            IconButton(onClick = { shareSave(slot.file) }) {
                                Icon(Icons.Default.Share, gtr("Export save"), tint = Red)
                            }
                            TextButton(onClick = { pendingLoad = slot }) { Text(gtr("LOAD"), color = Red, fontWeight = FontWeight.Bold) }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(slot.label.ifBlank { if (slot.automatic) "Autosave" else "Save" }, color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                val stamp = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(slot.createdAt))
                                Text((if (slot.automatic) "AUTO • " else "") + stamp, color = Muted, fontSize = 10.sp)
                            }
                        }
                    }
                    if (saveSlots.size > 10) {
                        TextButton(onClick = { showAllSaves = !showAllSaves }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showAllSaves) "نمایش ۱۰ Save آخر" else "نمایش همه Saveها (${saveSlots.size})", color = Red)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Red.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))

                // --- پاک کردن داده‌ها ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Red.copy(alpha = 0.6f), CutCornerShape(8.dp))
                        .clickable { confirmWipe = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Red)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("پاک کردن همه کاراکترها", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("حذف کامل و غیرقابل بازگشت ذخیره‌ها", color = Muted, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Red.copy(alpha = 0.4f))
                Spacer(Modifier.height(14.dp))

                // --- درباره‌ی برنامه و سلب‌مسئولیت حقوقی ---
                // نام کامل از resources می‌آید تا با برچسب لانچر و README یکی بماند.
                Text(
                    context.getString(R.string.app_full_name),
                    color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "v1.0 • منبع قواعد: Cyberpunk RED Core Rulebook",
                    color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // متن انگلیسی و چپ‌چین است؛ نیازی به FaText ندارد.
                Text(
                    context.getString(R.string.app_legal_notice),
                    color = Muted.copy(alpha = 0.8f), fontSize = 9.sp, lineHeight = 14.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = CutCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(gtr("DONE"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    pendingLoad?.let { slot ->
        AlertDialog(
            onDismissRequest = { pendingLoad = null },
            title = { Text("Load بازی", color = White) },
            text = { Text("وضعیت فعلی با «${slot.label}» جایگزین می‌شود. قبل از Load یک Save اضطراری از وضعیت فعلی ساخته می‌شود.", color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    saveBusy = true
                    scope.launch {
                        viewModel.restoreGameSave(slot).onSuccess {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("بازی با موفقیت Load شد"), Toast.LENGTH_LONG).show()
                            pendingLoad = null
                            saveBusy = false
                            onDismiss()
                        }.onFailure {
                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast(it.message ?: "Load ناموفق بود"), Toast.LENGTH_LONG).show()
                            saveBusy = false
                        }
                    }
                }) { Text(gtr("LOAD"), color = Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingLoad = null }) { Text("انصراف", color = Muted) } },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("پاک کردن همه داده‌ها", color = White) },
            text = { Text("همه کاراکترهای ذخیره‌شده برای همیشه حذف می‌شوند. مطمئنی؟", color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.wipeAllCharacters()
                    confirmWipe = false
                    onDismiss()
                }) { Text("حذف همه", color = Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) { Text("انصراف", color = Muted) }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333333), CutCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Red)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(gtr(title), color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(gtr(description), color = Muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = cprSwitchColors()
        )
    }
}
