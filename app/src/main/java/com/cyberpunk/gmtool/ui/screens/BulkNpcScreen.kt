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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.ImageProvider
import com.cyberpunk.gmtool.data.NpcData
import com.cyberpunk.gmtool.data.NpcTemplate
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel

private val BRed = Color(0xFFD32F2F)
private val BRedDim = Color(0xFF7A2A24)
private val BBlack = Color(0xFF0D0D0D)
private val BWhite = Color(0xFFECECEC)
private val BMuted = Color(0xFFB5B5B5)

/** نام کوتاه تیر (مثل اپ مرجع: Mook (Easy) / Lieutenant (Medium) / Mini Boss (Hard) / Boss) */
private fun tierLabel(t: NpcTemplate): String = when (t.difficulty.name) {
    "EASY" -> gtr("Mook (Easy)")
    "MEDIUM" -> gtr("Lieutenant (Medium)")
    "HARD" -> gtr("Mini Boss (Hard)")
    else -> gtr("Boss (Very Hard)")
}

@Composable
fun BulkNpcScreen(
    viewModel: CharacterViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val counts = remember { mutableStateMapOf<String, Int>() }
    // فیلتر دسته (مثل دکمه‌ی Filter مرجع)
    var catFilter by remember { mutableStateOf("همه") }
    var showFilter by remember { mutableStateOf(false) }

    val templates = NpcData.allTemplates
    val visible = templates.filter { t ->
        (catFilter == "همه" || t.category == catFilter) &&
            (query.isBlank() || t.name.contains(query, ignoreCase = true))
    }
    val total = counts.values.sum()

    Column(Modifier.fillMaxSize().background(BBlack)) {
        // هدر
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, gtr("Back"), tint = BRed, modifier = Modifier.size(28.dp))
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(gtr("Bulk NPC Creation"), color = BWhite, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        HorizontalDivider(color = BRed.copy(0.6f), thickness = 2.dp)

        // سرچ
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, gtr("Search"), tint = BWhite) },
            trailingIcon = {
                if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, gtr("Clear"), tint = BWhite)
                }
            },
            placeholder = { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(gtr("Search"), color = Color.Gray, fontSize = 18.sp) } },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BRed, unfocusedBorderColor = BRed,
                focusedTextColor = BWhite, unfocusedTextColor = BWhite),
            shape = CutCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().padding(14.dp).height(64.dp)
        )

        if (showFilter) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("همه") + NpcData.categories).forEach { c ->
                    FilterChip(
                        selected = catFilter == c,
                        onClick = { catFilter = c; showFilter = false },
                        label = { Text(if (c == "همه") "همه" else c.substringBefore(" ("), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BRed,
                            selectedLabelColor = Color.Black,
                            labelColor = BMuted)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            items(visible, key = { it.name }) { t ->
                BulkRow(
                    template = t,
                    count = counts[t.name] ?: 0,
                    onMinus = {
                        val v = (counts[t.name] ?: 0) - 1
                        if (v <= 0) counts.remove(t.name) else counts[t.name] = v
                    },
                    onPlus = { counts[t.name] = (counts[t.name] ?: 0) + 1 }
                )
            }
            item { Spacer(Modifier.height(110.dp)) }
        }
    }

    // نوار پایین: Filter / Create
    Row(
        Modifier.fillMaxSize().padding(20.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Button(
            onClick = { showFilter = !showFilter },
            colors = ButtonDefaults.buttonColors(containerColor = BRed),
            shape = CutCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(62.dp)
        ) {
            Icon(Icons.Default.FilterList, null, tint = Color.Black, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(gtr("Filter"), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Button(
            onClick = {
                if (total == 0) {
                    Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("حداقل یک NPC انتخاب کن."), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val batch = mutableListOf<com.cyberpunk.gmtool.data.Character>()
                counts.forEach { (name, n) ->
                    val t = templates.first { it.name == name }
                    repeat(n) {
                        batch.add(NpcData.toCharacter(t).copy(
                            name = if (n > 1) "${t.name} #${(100..999).random()}" else t.name // allow-raw-random: یکتا کردن نام NPC تکراری — تاس قواعد نیست
                        ))
                    }
                }
                viewModel.addMany(batch)
                Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast("$total NPC ساخته شد."), Toast.LENGTH_LONG).show()
                onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = BRed),
            shape = CutCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(62.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = Color.Black, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(if (total > 0) gtr("Create (%1s)", total) else "Create",
                    color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun BulkRow(template: NpcTemplate, count: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    val img = ImageProvider.getNpcImage(template.category, template.role, template.name)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(58.dp).clip(CutCornerShape(6.dp))
                .background(Color.DarkGray)
                .border(2.dp, BWhite, CutCornerShape(6.dp))
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(img),
                contentDescription = template.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(12.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.weight(1f)) {
                Text(gtr(template.name), color = BWhite, fontSize = 20.sp, fontWeight = FontWeight.Black,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tierLabel(template), color = BMuted, fontSize = 14.sp)
            }
        }
        // دکمه‌ی منفی
        Box(
            Modifier.size(54.dp).clip(CutCornerShape(10.dp))
                .background(if (count > 0) BRed else BRedDim)
                .clickable(enabled = count > 0) { onMinus() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Remove, null, tint = Color.Black, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.width(14.dp))
        Text("$count", color = BWhite, fontSize = 26.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier.size(54.dp).clip(CutCornerShape(10.dp)).background(BRed)
                .clickable { onPlus() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(28.dp)) }
    }
}
