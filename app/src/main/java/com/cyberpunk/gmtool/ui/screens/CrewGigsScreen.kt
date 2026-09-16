package com.cyberpunk.gmtool.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.CrewGig
import com.cyberpunk.gmtool.data.CrewGigData
import com.cyberpunk.gmtool.data.GigCheck
import com.cyberpunk.gmtool.data.GigNet
import com.cyberpunk.gmtool.data.GigSection
import com.cyberpunk.gmtool.data.gtr

private val GRed = Color(0xFFE53935)
private val GWhite = Color(0xFFEEEEEE)
private val GMutedC = Color(0xFF9E9E9E)
private val GCard = Color(0xFF161616)
private val GDim = Color(0xFF202020)
private val GGold = Color(0xFFFFB300)
private val GGreen = Color(0xFF4CAF50)

/**
 * تب GIGS — دو حالت:
 *   • انفرادی: بانک کارهای تک‌نفره‌ی هر Role (همان صفحه‌ی قبلی، با توضیح بیشتر)
 *   • گروهی:   سناریوهای کامل و آماده‌ی اجرا برای کل کرو
 */
@Composable
fun GigsScreen() {
    var mode by remember { mutableStateOf("crew") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                Text(
                    gtr("GIGS"), color = GWhite, fontSize = 22.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
                Text(
                    gtr("Solo gigs per Role, plus full crew scenarios"),
                    color = GMutedC, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton(gtr("crew gigs"), Icons.Default.Groups, mode == "crew", Modifier.weight(1f)) { mode = "crew" }
                    ModeButton(gtr("solo gigs"), Icons.Default.Person, mode == "solo", Modifier.weight(1f)) { mode = "solo" }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (mode == "solo") SoloJobsScreen() else CrewGigsList()
        }
    }
}

@Composable
private fun ModeButton(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val bg = if (selected) GRed else Color.Transparent
    val fg = if (selected) Color.Black else GWhite
    Row(
        modifier
            .background(bg, CutCornerShape(8.dp))
            .border(1.dp, if (selected) GRed else GMutedC.copy(.5f), CutCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(gtr(label), color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ─────────────────────── فهرست کارهای گروهی ───────────────────────

@Composable
private fun CrewGigsList() {
    var opened by remember { mutableStateOf<CrewGig?>(null) }
    val current = opened

    if (current != null) {
        CrewGigDetail(current) { opened = null }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GDim),
                shape = CutCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        gtr("Each gig is a complete scenario: hook, scenes with set DVs, NET architecture and several endings."),
                        color = GWhite, fontSize = 11.sp, lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        gtr("No lock has a single route. Wherever NET is needed, a non-Netrunner path is written too."),
                        color = GGreen, fontSize = 10.sp, lineHeight = 17.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                }
            }
        }
        items(CrewGigData.gigs) { g ->
            Card(
                colors = CardDefaults.cardColors(containerColor = GCard),
                shape = CutCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, GRed.copy(.4f), CutCornerShape(10.dp))
                    .clickable { opened = g }
            ) {
                Column(Modifier.padding(13.dp)) {
                    Text(
                        gtr(g.title), color = GWhite, fontSize = 17.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                    Text(
                        gtr(g.subtitle), color = GMutedC, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Pill(gtr(g.threat), GRed)
                        Pill(gtr("%1s eb", g.payEb), GGold)
                        Pill("IP ${g.ip}", GMutedC)
                        Pill(gtr(g.length), GMutedC)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        gtr(g.twist), color = GWhite.copy(.85f), fontSize = 11.sp, lineHeight = 18.sp,
                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(.13f), CutCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

// ─────────────────────── جزئیات یک کار ───────────────────────

@Composable
private fun CrewGigDetail(g: CrewGig, onBack: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        gtr(g.title), color = GWhite, fontSize = 20.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                    Text(
                        gtr(g.subtitle), color = GRed, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                }
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = GRed) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pill(g.threat, GRed); Pill(gtr("%1s eb", g.payEb), GGold)
                Pill("IP ${g.ip}", GMutedC); Pill("REP ${g.rep}", GMutedC); Pill(g.length, GMutedC)
            }
        }

        // بازیکنان و صحنه
        item {
            GigCard(gtr("Players and the scene"), Icons.Default.Info) {
                Line(gtr("Hired by"), gtr(g.hiredBy))
                Line(gtr("Adversary"), gtr(g.adversary))
                Line(gtr("Reward"), gtr(g.reward))
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .background(GGold.copy(.10f), CutCornerShape(6.dp))
                        .border(1.dp, GGold.copy(.4f), CutCornerShape(6.dp))
                        .padding(9.dp)
                ) {
                    Column {
                        Text(gtr("Twist"), color = GGold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(gtr(g.twist), color = GWhite, fontSize = 12.sp, lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                }
            }
        }

        item { GigCard(gtr("Hook"), Icons.Default.Link) { Body(gtr(g.hook)) } }

        items(g.sections) { s -> SectionCard(s) }

        if (g.nets.isNotEmpty()) items(g.nets) { n -> NetCard(n) }

        item { GigCard(gtr("Resolution"), Icons.Default.Flag) { Body(gtr(g.resolution)) } }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1420)),
                shape = CutCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF7E57C2).copy(.55f), CutCornerShape(10.dp))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoStories, null, tint = Color(0xFF9575CD), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(gtr("How to tie this into ZEROES"), color = Color(0xFF9575CD),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(gtr(g.zeroesHook), color = GWhite, fontSize = 12.sp, lineHeight = 21.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
            }
        }

        item {
            GigCard(gtr("GM notes"), Icons.Default.TipsAndUpdates) {
                g.gmNotes.forEach {
                    Text("• " + gtr(it), color = GWhite, fontSize = 12.sp, lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), textAlign = TextAlign.Right)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(s: GigSection) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GCard),
        shape = CutCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, GRed.copy(.35f), CutCornerShape(10.dp))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(gtr(s.title), color = GRed, fontSize = 14.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            Spacer(Modifier.height(6.dp))
            Text(gtr(s.body), color = GWhite, fontSize = 12.sp, lineHeight = 21.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)

            if (s.checks.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                s.checks.forEach { CheckRow(it) }
            }

            if (s.gmTip.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .background(GRed.copy(.08f), CutCornerShape(6.dp))
                        .border(1.dp, GRed.copy(.3f), CutCornerShape(6.dp))
                        .padding(9.dp)
                ) {
                    Column {
                        Text(gtr("Running tip"), color = GRed, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(gtr(s.gmTip), color = GWhite.copy(.9f), fontSize = 11.sp, lineHeight = 19.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(c: GigCheck) {
    Column(
        Modifier.fillMaxWidth()
            .padding(bottom = 7.dp)
            .background(GDim, CutCornerShape(6.dp))
            .padding(9.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(gtr(c.skill), color = GWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
            Spacer(Modifier.width(6.dp))
            Text(gtr(c.dv), color = GRed, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(3.dp))
        Text(gtr(c.result), color = GWhite.copy(.88f), fontSize = 11.sp, lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        if (c.alt.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("↳ ", color = GGreen, fontSize = 11.sp)
                Text(gtr(c.alt), color = GGreen.copy(.9f), fontSize = 10.sp, lineHeight = 18.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
            }
        }
    }
}

@Composable
private fun NetCard(n: GigNet) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101A1A)),
        shape = CutCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00BCD4).copy(.45f), CutCornerShape(10.dp))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lan, null, tint = Color(0xFF00BCD4), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(gtr(n.name), color = Color(0xFF00BCD4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            n.floors.forEachIndexed { i, f ->
                Text("${i + 1}. " + gtr(f), color = GWhite, fontSize = 11.sp, lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth()
                    .background(GGreen.copy(.10f), CutCornerShape(6.dp))
                    .border(1.dp, GGreen.copy(.4f), CutCornerShape(6.dp))
                    .padding(9.dp)
            ) {
                Column {
                    Text(gtr("Without a Netrunner"), color = GGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Text(gtr(n.bypass), color = GWhite, fontSize = 11.sp, lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
            }
        }
    }
}

@Composable
private fun GigCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GCard),
        shape = CutCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = GRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(gtr(title), color = GRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(gtr(label), color = GRed, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        Text(gtr(value), color = GWhite, fontSize = 12.sp, lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
    }
}

@Composable
private fun Body(text: String) {
    Text(gtr(text), color = GWhite, fontSize = 12.sp, lineHeight = 21.sp,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
}
