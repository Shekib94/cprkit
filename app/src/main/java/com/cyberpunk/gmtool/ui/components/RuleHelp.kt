package com.cyberpunk.gmtool.ui.components

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.RuleReferenceData

private val HelpRed = Color(0xFFD32F2F)
private val HelpBlack = Color(0xFF0F0F0F)
private val HelpMuted = Color(0xFFB0B0B0)

/** Small, reusable beginner-help control. Text is explanatory/paraphrased, not a book quotation. */
@Composable
fun RuleInfoButton(title: String, text: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier.size(32.dp)) {
        Surface(shape = CircleShape, color = HelpRed.copy(alpha = .14f), border = BorderStroke(1.dp, HelpRed.copy(alpha = .7f))) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Text("i", color = HelpRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
    if (open) RuleHelpDialog(title, text) { open = false }
}


/** Pulls the short popup from the central reference, so inline help and the Reference tab stay in sync. */
@Composable
fun RuleInfoButton(entryKey: String, modifier: Modifier = Modifier) {
    val entry = RuleReferenceData.get(entryKey) ?: return
    RuleInfoButton(title = entry.title, text = entry.short, modifier = modifier)
}

/** Item-specific popup sourced from the same StoreCatalog-backed central reference. */
@Composable
fun StoreItemInfoButton(name: String, category: String, modifier: Modifier = Modifier) {
    val entry = RuleReferenceData.getForStoreItem(name, category) ?: return
    RuleInfoButton(title = entry.title, text = entry.short, modifier = modifier)
}

@Composable
fun RuleHelpDialog(title: String, text: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                colors = CardDefaults.cardColors(containerColor = HelpBlack),
                shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
                modifier = Modifier.fillMaxWidth().border(1.5.dp, HelpRed, CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(gtr(title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = HelpRed.copy(alpha = .45f))
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
                        Text(gtr(text), color = HelpMuted, fontSize = 14.sp, lineHeight = 23.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = HelpRed), modifier = Modifier.fillMaxWidth()) {
                        Text("فهمیدم", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
