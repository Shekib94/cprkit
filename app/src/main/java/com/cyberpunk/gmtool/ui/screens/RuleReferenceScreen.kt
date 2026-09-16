package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.RuleReferenceData
import com.cyberpunk.gmtool.data.RuleReferenceEntry

private val RefRed = Color(0xFFC62828)
private val RefBlack = Color(0xFF0F0F0F)
private val RefCard = Color(0xFF181818)
private val RefMuted = Color(0xFFB5B5B5)

@Composable
fun RuleReferenceScreen() {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var category by remember { mutableStateOf("همه") }
    var expanded by remember { mutableStateOf<String?>(null) }

    val q = query.text.trim()
    val filtered = remember(q, category) {
        RuleReferenceData.allEntries.filter { entry ->
            val inCategory = category == "همه" || entry.category == category
            val haystack = buildString {
                append(entry.title); append(' '); append(entry.short); append(' ')
                append(entry.full); append(' '); append(entry.tags.joinToString(" "))
            }
            inCategory && (q.isBlank() || haystack.contains(q, ignoreCase = true))
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize().background(RefBlack).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, null, tint = RefRed, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("مرجع سریع Cyberpunk RED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("توضیح فارسی روان برای استفاده کنار میز بازی", color = RefMuted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = RefRed) },
                placeholder = { Text("جستجو: Aimed Shot، درمان، Haggle...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RefRed,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = RefRed
                )
            )

            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                items(listOf("همه") + RuleReferenceData.categories) { cat ->
                    val selected = category == cat
                    FilterChip(
                        selected = selected,
                        onClick = { category = cat },
                        label = { Text(if (cat == "همه") cat else categoryFa(cat), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RefRed,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF202020),
                            labelColor = RefMuted
                        )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("${filtered.size} موضوع", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Left)
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.key }) { entry ->
                    ReferenceCard(entry, expanded == entry.key) {
                        expanded = if (expanded == entry.key) null else entry.key
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("موضوعی پیدا نشد. عبارت کوتاه‌تری جستجو کن.", color = RefMuted, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceCard(entry: RuleReferenceEntry, isExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RefCard),
        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, if (isExpanded) RefRed else Color(0xFF333333), CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)).clickable { onToggle() }
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(gtr(entry.title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(3.dp))
                    Surface(shape = CircleShape, color = RefRed.copy(alpha = .14f)) {
                        Text(categoryFa(entry.category), color = RefRed, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = RefRed)
            }
            Spacer(Modifier.height(8.dp))
            Text(gtr(entry.short), color = RefMuted, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Right)
            if (isExpanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = RefRed.copy(alpha = .35f))
                Spacer(Modifier.height(10.dp))
                Text(gtr(entry.full), color = Color(0xFFE0E0E0), fontSize = 14.sp, lineHeight = 23.sp, textAlign = TextAlign.Right)
                Spacer(Modifier.height(9.dp))
                Text("راهنمای خلاصه و بازنویسی‌شده بر پایه Core Rulebook؛ برای استثناها و متن کامل قانون به کتاب مراجعه کن.", color = Color.Gray, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
    }
}

private fun categoryFa(category: String): String = when (category) {
    "Combat" -> "کامبت"
    "Medical" -> "درمان"
    "Netrunning" -> "نت‌رانینگ"
    "Roles & Economy" -> "نقش‌ها و اقتصاد"
    "Vehicles" -> "وسایل نقلیه"
    "Gear & Cyberware" -> "تجهیزات و سایبرویر"
    "Core Checks" -> "قواعد پایه"
    "Character Creation" -> "ساخت شخصیت"
    else -> category
}
