package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.NpcData
import com.cyberpunk.gmtool.data.NpcDifficulty
import com.cyberpunk.gmtool.data.NpcTemplate
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val CardBg = Color(0xFF1A1A1A)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewNpcScreen(
    viewModel: CharacterViewModel,
    onBack: () -> Unit,
    onNpcCreated: (Int) -> Unit
) {
    var mode by remember { mutableStateOf("menu") } // menu | pregen | empty | bulk | bulkFull
    var selectedCategory by remember { mutableStateOf(NpcData.categories.firstOrNull().orEmpty()) }
    var bulkCount by remember { mutableStateOf(5) }
    val context = LocalContext.current

    // صفحه‌ی تمام‌صفحه‌ی ساخت دسته‌ای (Bulk NPC Creation) مثل اپ مرجع
    if (mode == "bulkFull") {
        BulkNpcScreen(
            viewModel = viewModel,
            onBack = { mode = "menu" }
        )
        return
    }

    Scaffold(
        topBar = { CyberpunkHeader(title = gtr("New NPC"), onBackClick = {
            if (mode == "menu") onBack() else mode = "menu"
        }) },
        containerColor = Black
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (mode) {
                "menu" -> NpcModeMenu(
                    onPregenerated = { mode = "pregen" },
                    onEmpty = {
                        val npc = viewModel.addNpc(NpcData.emptyNpc())
                        Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("NPC خالی ساخته شد"), Toast.LENGTH_SHORT).show()
                        onNpcCreated(npc.id)
                    },
                    onBulk = { bulkCount = 5; mode = "bulkFull" }
                )
                "pregen" -> PregeneratedList(
                    selectedCategory = selectedCategory,
                    onCategory = { selectedCategory = it },
                    onPick = { template ->
                        val npc = viewModel.addNpc(NpcData.toCharacter(template))
                        Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("${template.name} اضافه شد"), Toast.LENGTH_SHORT).show()
                        onNpcCreated(npc.id)
                    }
                )
                "bulk" -> BulkPanel(count = bulkCount, onCount = { bulkCount = it }, onGenerate = {
                    val pool = NpcData.allTemplates.filter { it.category != NpcData.CAT_BOSS }
                    val batch = (1..bulkCount).map {
                        val t = pool.random()
                        NpcData.toCharacter(t).copy(name = "${t.name} #${(100..999).random()}") // allow-raw-random: یکتا کردن نام NPC تکراری — تاس قواعد نیست
                    }
                    viewModel.addMany(batch)
                    Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("$bulkCount NPC ساخته شد"), Toast.LENGTH_LONG).show()
                    onBack()
                })
            }
        }
    }
}

@Composable
private fun NpcModeMenu(onPregenerated: () -> Unit, onEmpty: () -> Unit, onBulk: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item {
            NpcOptionCard(
                icon = Icons.Default.Group,
                title = gtr("Pregenerated"),
                desc = "انتخاب از فهرست NPCهای آماده‌ی پروژه، دسته‌بندی‌شده بر اساس سختی، سایبرسایکوها، بک‌آپ لاومن، تیم اگزک و باس‌ها."
            ) { onPregenerated() }
        }
        item {
            NpcOptionCard(
                icon = Icons.Default.PersonAdd,
                title = gtr("Empty"),
                desc = "یک شیت NPC خالی؛ خودت استت، مهارت و تجهیزات را پر می‌کنی."
            ) { onEmpty() }
        }
        item {
            NpcOptionCard(
                icon = Icons.Default.Casino,
                title = gtr("Bulk"),
                desc = "یک‌جا چندین NPC تصادفی برای پرکردن صحنه ساخته می‌شود."
            ) { onBulk() }
        }
    }
}

@Composable
private fun NpcOptionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, onClick: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            Modifier.fillMaxWidth().clickable { onClick() }
                .background(CardBg, CutCornerShape(12.dp)).border(2.dp, Red.copy(alpha = 0.6f), CutCornerShape(12.dp))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Red, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(title), color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(gtr(desc), color = Muted, fontSize = 13.sp, lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            }
        }
    }
}

@Composable
private fun PregeneratedList(
    selectedCategory: String,
    onCategory: (String) -> Unit,
    onPick: (NpcTemplate) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // فیلتر دسته
        Row(
            Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NpcData.categories.forEach { cat ->
                FilterChip(
                    selected = cat == selectedCategory,
                    onClick = { onCategory(cat) },
                    label = {
                        val english = cat.substringAfter("(", cat).substringBefore(")")
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(gtr(english), fontSize = 11.sp, color = if (cat == selectedCategory) Black else White)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Red, containerColor = CardBg
                    )
                )
            }
        }
        val list = NpcData.templatesByCategory(selectedCategory)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(list, key = { index, t -> "${t.category}-${t.name}-$index" }) { _, t ->
                TemplateCard(t) { onPick(t) }
            }
        }
    }
}

@Composable
private fun TemplateCard(t: NpcTemplate, onClick: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            Modifier.fillMaxWidth().clickable { onClick() }
                .background(CardBg, CutCornerShape(10.dp)).border(1.dp, Red.copy(alpha = 0.4f), CutCornerShape(10.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = gtr("Add"), tint = Red, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(t.name), color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(gtr(t.difficulty.label), color = Color(t.difficulty.color), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    // این NPCها ساخته‌ی همین پروژه‌اند، نه بازتولید کتاب.
                    // برچسب می‌گذاریم تا کسی دنبالشان در Core نگردد.
                    Text(
                        gtr("Homebrew"),
                        color = Color(0xFF9575CD), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF9575CD).copy(alpha = .14f), CutCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    t.description.take(90) + if (t.description.length > 90) "…" else "",
                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
            }
        }
    }
}

@Composable
private fun BulkPanel(count: Int, onCount: (Int) -> Unit, onGenerate: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Casino, contentDescription = null, tint = Red, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("ساخت گروهی NPC", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("چند NPC تصادفی از همه‌ی رده‌ها (به‌جز باس) می‌سازی؟", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onCount((count - 1).coerceAtLeast(1)) },
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                shape = CutCornerShape(8.dp)) { Text("−", color = Red, fontSize = 22.sp) }
            Text("  $count  ", color = White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { onCount((count + 1).coerceAtMost(30)) },
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                shape = CutCornerShape(8.dp)) { Text("+", color = Red, fontSize = 22.sp) }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onGenerate,
            colors = ButtonDefaults.buttonColors(containerColor = Red),
            shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.Casino, contentDescription = null, tint = Black)
            Spacer(Modifier.width(8.dp))
            Text(gtr("GENERATE"), color = Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
