package com.cyberpunk.gmtool.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.gtr
import com.cyberpunk.gmtool.data.net.LanConnState
import com.cyberpunk.gmtool.data.net.PendingRequest
import com.cyberpunk.gmtool.data.net.QrCode

private val CRed = Color(0xFFE53935)
private val CCard = Color(0xFF1A1A1A)
private val CWhite = Color(0xFFEEEEEE)
private val CMuted = Color(0xFF9E9E9E)
private val CAmber = Color(0xFFFFB300)

/** حداکثر خط‌های diff که در نوار تأیید نشان داده می‌شود؛ بقیه جمع می‌شوند. */
private const val MAX_DIFF_LINES = 6

/** نمایش کد QR — تولید داخلی، بدون کتابخانه‌ی خارجی. */
@Composable
fun QrImage(content: String, modifier: Modifier = Modifier, sizeDp: Int = 240) {
    val matrix = remember(content) { runCatching { QrCode.encode(content) }.getOrNull() }
    if (matrix == null) {
        Box(
            modifier.size(sizeDp.dp).background(Color.White),
            contentAlignment = Alignment.Center
        ) { Text(gtr("QR error"), color = Color.Red, fontSize = 12.sp) }
        return
    }
    val n = matrix.size
    Box(
        modifier
            .size(sizeDp.dp)
            .background(Color.White)
            .padding(10.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.minDimension / n
            for (y in 0 until n) {
                for (x in 0 until n) {
                    if (matrix[y][x]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cell, y * cell),
                            size = Size(cell + 0.5f, cell + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/** نشانگر کوچک وضعیت اتصال. */
@Composable
fun LanStatusChip(state: LanConnState, label: String = "") {
    val (color, text) = when (state) {
        LanConnState.ONLINE -> Color(0xFF4CAF50) to gtr("connected")
        LanConnState.RECONNECTING -> Color(0xFFFFB300) to gtr("reconnecting")
        LanConnState.STARTING -> Color(0xFFFFB300) to gtr("connecting")
        LanConnState.ERROR -> CRed to gtr("connection error")
        LanConnState.OFFLINE -> CMuted to gtr("offline")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CutCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                if (label.isBlank()) text else "$text • $label",
                color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * نوار درخواست‌های در انتظار تأیید GM.
 * بدون قطع کردن کار GM؛ پایین صفحه می‌نشیند.
 */
@Composable
fun PendingRequestBar(
    requests: List<PendingRequest>,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onApproveAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (requests.isEmpty()) return
    val first = requests.first()
    Card(
        colors = CardDefaults.cardColors(containerColor = CCard),
        shape = CutCornerShape(10.dp),
        modifier = modifier.fillMaxWidth().border(1.dp, CRed, CutCornerShape(10.dp))
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        gtr("%1s requests approval", requests.size),
                        color = CRed, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Right
                    )
                    if (requests.size > 1) {
                        TextButton(onClick = onApproveAll) {
                            Text(gtr("approve all"), color = CRed, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    first.characterName,
                    color = CWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )
                Text(
                    first.summary,
                    color = CMuted, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                )

                // ── آنچه واقعاً اعمال می‌شود ──
                // این فهرست سمت گوشی GM ساخته شده (LanTrust.diffFields) نه سمت
                // کلاینت؛ پس خلاصه‌ی نوشته‌ی بازیکن نمی‌تواند تأیید ناآگاهانه
                // بگیرد. فیلدهای فقط-GM هم که در blocked هستند هرگز اعمال
                // نمی‌شوند و اینجا به GM گزارش می‌شوند.
                if (first.diff.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        gtr("Will apply:"),
                        color = CWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                    // خط‌های diff مخلوط فارسی/انگلیسی‌اند (نام فیلد و آیتم انگلیسی،
                    // برچسب فارسی)؛ FaText تکه‌های لاتین را isolate می‌کند تا نقطه،
                    // دونقطه و فلش جابه‌جا نشوند.
                    first.diff.take(MAX_DIFF_LINES).forEach { line ->
                        FaText(
                            line, color = CMuted, fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (first.diff.size > MAX_DIFF_LINES) {
                        FaText(
                            gtr("+%1s more changes", first.diff.size - MAX_DIFF_LINES),
                            color = CMuted, fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (first.blocked.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    FaText(
                        gtr("Ignored (GM-only): %1s", first.blocked.joinToString("، ")),
                        color = CAmber, fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onApprove(first.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = CRed),
                        shape = CutCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text(gtr("approve"), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onReject(first.id) },
                        shape = CutCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CMuted),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, null, tint = CMuted)
                        Spacer(Modifier.width(4.dp))
                        Text(gtr("reject"), color = CMuted)
                    }
                }
            }
        }
    }
}

/** پیام خصوصی درون‌داستانی GM به یک بازیکن. */
@Composable
fun WhisperDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CCard,
        shape = CutCornerShape(12.dp),
        title = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(gtr("A message only you know"), color = CRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(text, color = CWhite, fontSize = 14.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(gtr("got it"), color = CRed) }
        }
    )
}
