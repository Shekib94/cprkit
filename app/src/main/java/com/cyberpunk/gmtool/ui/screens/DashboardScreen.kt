package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CharacterIO
import com.cyberpunk.gmtool.data.CrashLog
import com.cyberpunk.gmtool.data.ImageProvider
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel

enum class BottomTab { PLAYERS, NPCS, SESSION, GM_TOOLS, REFERENCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    charactersList: List<Character>,
    viewModel: CharacterViewModel,
    onCharacterSelect: (Character) -> Unit,
    onNewCharacterClick: () -> Unit,
    onNewNpcClick: () -> Unit,
    onImportClick: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteCharacter: (Character) -> Unit
) {
    var selectedTab by remember { mutableStateOf(BottomTab.PLAYERS) }
    var gmFullscreen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Character?>(null) }
    val context = LocalContext.current

    val cyberpunkRed = Color(0xFFC62828)
    val darkBackground = Color(0xFF0F0F0F)

    val players = charactersList.filter { it.isAlly }
    val npcs = charactersList.filter { !it.isAlly }
    val shownList = if (selectedTab == BottomTab.PLAYERS) players else npcs

    Scaffold(
        bottomBar = {
            if (!gmFullscreen) CustomBottomNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, cyberpunkRed = cyberpunkRed)
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (selectedTab == BottomTab.GM_TOOLS) {
                // GM Tools تمام‌صفحه با هدر و فوتر داخلی خودش (هدر بیرونی حذف می‌شود)
                GmToolsScreen(onBack = { selectedTab = BottomTab.PLAYERS }, viewModel = viewModel, onFullscreenChanged = { gmFullscreen = it })
            } else if (selectedTab == BottomTab.REFERENCE) {
                RuleReferenceScreen()
            } else if (selectedTab == BottomTab.SESSION) {
                // صفحه‌ی مدیریت جلسه تمام‌صفحه
                SessionScreen(characters = charactersList, viewModel = viewModel, onBack = { selectedTab = BottomTab.PLAYERS })
            } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = gtr("Back"), tint = cyberpunkRed)
                    }
                    Text(
                        text = if (selectedTab == BottomTab.PLAYERS) "Players" else "Non-Player Characters",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))
                viewModel.saveError?.let { err ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1010)), modifier = Modifier.fillMaxWidth()) {
                        Text("⚠ هشدار داده/ذخیره‌سازی: $err", color = Color.White, modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // آخرین خطای کشنده (اگر وجود دارد). بدون این، «برنامه بسته شد» هیچ
                // سرنخی باقی نمی‌گذاشت؛ حالا متن خطا همین‌جا خوانده می‌شود.
                var crashDismissed by remember { mutableStateOf(false) }
                val prefsRepair = remember { com.cyberpunk.gmtool.data.PrefsGuard.read(context) }
                val lastCrash = remember {
                    // اگر خطای کشنده‌ای ثبت نشده ولی کلید خرابِ تنظیمات ترمیم شده،
                    // همین را نشان بده: معمولاً همان چیزی است که صفحه را می‌بست
                    // (نوع اشتباه در SharedPreferences → ClassCastException).
                    CrashLog.read(context) ?: prefsRepair?.let {
                        "بدون خطای کشنده. کلیدهای ترمیم‌شده‌ی تنظیمات (نوع اشتباه در حافظه‌ی تنظیمات):\n$it"
                    }
                }
                if (lastCrash != null && !crashDismissed) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0B0B)),
                        shape = CutCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, cyberpunkRed, CutCornerShape(10.dp))
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text("آخرین خطای برنامه (برای عیب‌یابی)", color = cyberpunkRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                lastCrash.take(900),
                                color = Color(0xFFFFCDD2), fontSize = 10.sp, lineHeight = 15.sp,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 170.dp).verticalScroll(rememberScrollState())
                            )
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { CrashLog.clear(context); crashDismissed = true }, modifier = Modifier.weight(1f)) {
                                    Text("پاک کردن", color = cyberpunkRed, fontSize = 11.sp)
                                }
                                OutlinedButton(onClick = { crashDismissed = true }, modifier = Modifier.weight(1f)) {
                                    Text("بستن", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (selectedTab == BottomTab.PLAYERS || selectedTab == BottomTab.NPCS) {
                    if (shownList.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (selectedTab == BottomTab.PLAYERS) Icons.Default.Person else Icons.Default.Group,
                                    contentDescription = null, tint = cyberpunkRed, modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                androidx.compose.runtime.CompositionLocalProvider(
                                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (selectedTab == BottomTab.PLAYERS) "هنوز بازیکنی نساخته‌اید." else "هنوز NPCای نساخته‌اید.",
                                            color = Color.LightGray, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text("برای شروع، دکمه‌ی New را بزنید.",
                                            color = Color.Gray, fontSize = 13.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        }
                    } else {
                        // دکمه‌های New/Import روی محتوا شناورند؛ بدون این padding آخرین کاراکتر
                        // زیرشان پنهان می‌ماند و اسکرول هم تا وقتی لیست از صفحه بلندتر نشود شروع نمی‌شد.
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(shownList, key = { it.id }) { char ->
                                CharacterListItem(
                                    character = char,
                                    cyberpunkRed = cyberpunkRed,
                                    onClick = { onCharacterSelect(char) },
                                    onDelete = { pendingDelete = char },
                                    onExport = {
                                        val file = CharacterIO.writeExportFile(
                                            context, char.name, CharacterIO.singleToJson(char)
                                        )
                                        shareFile(context, file, "application/json", "اشتراک‌گذاری کاراکتر")
                                    },
                                    onSaveDownloads = {
                                        val file = CharacterIO.writeExportFile(
                                            context, char.name, CharacterIO.singleToJson(char)
                                        )
                                        val savedName = saveExportToDownloads(context, file, "application/json")
                                        if (savedName != null) {
                                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("فایل $savedName در Downloads ذخیره شد."), Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("ذخیره در Downloads انجام نشد."), Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onPrint = {
                                        val file = CharacterIO.printToPdf(context, char)
                                        shareFile(context, file, "application/pdf", "چاپ/ذخیره شیت")
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(gtr("🚧 Coming Soon 🚧"), color = Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            }

            if (selectedTab == BottomTab.PLAYERS || selectedTab == BottomTab.NPCS) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).fillMaxWidth().padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedTab == BottomTab.PLAYERS) onNewCharacterClick() else onNewNpcClick()
                        },
                        shape = CutCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cyberpunkRed),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = gtr("New"), tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text(gtr("New"), fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    if (selectedTab == BottomTab.NPCS || selectedTab == BottomTab.PLAYERS) {
                        Button(
                            onClick = onImportClick,
                            shape = CutCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, cyberpunkRed),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = gtr("Import"), tint = cyberpunkRed)
                            Spacer(Modifier.width(8.dp))
                            Text(gtr("Import"), fontSize = 16.sp, color = cyberpunkRed, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { char ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (char.isAlly) "حذف کاراکتر" else "حذف NPC", color = Color.White) },
            text = { Text("آیا از حذف «${char.name}» مطمئن هستید؟ این عمل قابل بازگشت نیست.", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCharacter(char)
                    pendingDelete = null
                }) { Text("حذف", color = cyberpunkRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("انصراف", color = Color.Gray) }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

}

// ----------------- به‌اشتراک‌گذاری فایل -----------------
private fun shareFile(context: android.content.Context, file: java.io.File, mime: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

/**
 * یک کپی مستقل از فایل Export را در پوشه‌ی عمومی Downloads دستگاه می‌سازد.
 * در Android 10+ از MediaStore استفاده می‌شود و نیازی به مجوز Storage ندارد.
 * در نسخه‌های قدیمی‌تر، اگر دسترسی مستقیم ممکن نباشد null برمی‌گرداند تا UI پیام واضح نشان دهد.
 */
private fun saveExportToDownloads(
    context: android.content.Context,
    file: java.io.File,
    mime: String
): String? = runCatching {
    val resolver = context.contentResolver
    val displayName = file.name

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CPRGMTOOLS")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri, "w")!!.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
        displayName
    } else {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()
        if (!downloads.canWrite()) return@runCatching null
        val appFolder = java.io.File(downloads, "CPRGMTOOLS").apply { mkdirs() }
        var target = java.io.File(appFolder, displayName)
        if (target.exists()) {
            val base = displayName.substringBeforeLast('.', displayName)
            val ext = displayName.substringAfterLast('.', "")
            var index = 2
            while (target.exists()) {
                val suffix = if (ext.isBlank()) "$base ($index)" else "$base ($index).$ext"
                target = java.io.File(appFolder, suffix)
                index++
            }
        }
        file.copyTo(target, overwrite = false)
        target.name
    }
}.getOrNull()

// ----------------- آیتم لیست -----------------
@Composable
fun CharacterListItem(
    character: Character,
    cyberpunkRed: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit = {},
    onSaveDownloads: () -> Unit = {},
    onPrint: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                .border(1.dp, Color.White, CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                .background(Color.DarkGray)
        ) {
            Image(
                painter = painterResource(
                    id = if (character.isAlly)
                        ImageProvider.getRoleImage(character.role, isPortrait = true)
                    else
                        ImageProvider.getNpcImage(character.npcCategory, character.role, character.name)
                ),
                contentDescription = gtr("Avatar"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(cyberpunkRed)
                    .align(Alignment.CenterStart)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = character.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (!character.isAlly && character.npcTier.isNotBlank())
                    "${character.npcCategory} • ${character.npcTier}"
                else gtr("%1s • Rank %2s", character.role, character.roleRank),
                color = Color.Gray, fontSize = 13.sp
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = gtr("Options"), tint = Color.White)
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color(0xFF1A1A1A)).border(1.dp, cyberpunkRed, CutCornerShape(8.dp))
            ) {
                DropdownMenuItem(
                    text = { Text(gtr("Export / Share"), color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) },
                    onClick = { menuExpanded = false; onExport() }
                )
                DropdownMenuItem(
                    text = { Text(gtr("Save to Downloads"), color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = cyberpunkRed) },
                    onClick = { menuExpanded = false; onSaveDownloads() }
                )
                DropdownMenuItem(
                    text = { Text(gtr("Print (PDF)"), color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = Color.White) },
                    onClick = { menuExpanded = false; onPrint() }
                )
                DropdownMenuItem(
                    text = { Text(gtr("Delete"), color = cyberpunkRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = cyberpunkRed) },
                    onClick = { menuExpanded = false; onDelete() }
                )
            }
        }
    }
}

@Composable
fun CustomBottomNav(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    cyberpunkRed: Color
) {
    val tabs = listOf(
        Triple(BottomTab.PLAYERS, "Players", Icons.Default.Person),
        Triple(BottomTab.NPCS, "NPCs", Icons.Default.Group),
        Triple(BottomTab.SESSION, "Session", Icons.Default.EventNote),
        Triple(BottomTab.GM_TOOLS, "GM Tools", Icons.Default.Build),
        Triple(BottomTab.REFERENCE, "Reference", Icons.Default.MenuBook)
    )

    NavigationBar(containerColor = Color(0xFF0F0F0F), contentColor = cyberpunkRed) {
        tabs.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(gtr(label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = cyberpunkRed,
                    indicatorColor = cyberpunkRed,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
