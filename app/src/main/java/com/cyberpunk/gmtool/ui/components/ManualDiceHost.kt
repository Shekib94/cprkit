package com.cyberpunk.gmtool.ui.components

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.DiceSource
import com.cyberpunk.gmtool.data.gtr
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

private val DRed = Color(0xFFD32F2F)
private val DBlack = Color(0xFF0F0F0F)
private val DWhite = Color(0xFFE8E8E8)
private val DMuted = Color(0xFFAAAAAA)
private val DGold = Color(0xFFFFB300)

/**
 * میزبان تاس دستی.
 *
 * یک بار در ریشه‌ی برنامه نصب می‌شود. وقتی «تاس خودکار» خاموش باشد، خودش را
 * به‌عنوان منبع تاس به [DiceSource] معرفی می‌کند؛ از آن به بعد هر تاسی که
 * قواعد بازی لازم داشته باشد — در Combat، سایبرویر، درمان، هر جا — این دیالوگ
 * را باز می‌کند و منتظر عددی می‌ماند که GM از تاس فیزیکی می‌خواند.
 *
 * نکته‌ی فنی: لایه‌ی قواعد همگام (synchronous) است و نمی‌تواند منتظر Compose
 * بماند. برای همین نخِ فراخوان روی یک صف کوتاه بلاک می‌شود تا UI جواب را
 * بگذارد. چون همه‌ی این فراخوانی‌ها از رویداد کلیک می‌آیند و نه نخ اصلیِ
 * رسم، این بلاک‌شدن رابط را قفل نمی‌کند.
 */
@Composable
fun ManualDiceHost(enabled: Boolean, hapticsEnabled: Boolean, content: @Composable () -> Unit) {
    var pending by remember { mutableStateOf<DiceSource.Request?>(null) }
    val answers = remember { ArrayBlockingQueue<List<Int>>(1) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val context = LocalContext.current

    // وقتی درخواست دستی به عدد واقعی نرسید (فراخوان روی نخ اصلی، انصراف GM،
    // یا پایان مهلت) برنامه ناچار می‌شود تصادفی بریزد. این نباید بی‌صدا باشد:
    // سر میز باید معلوم باشد که آن تاس از تاس فیزیکی نیامده.
    DisposableEffect(context) {
        DiceSource.onManualFallback = { req ->
            val msg = gtr("Manual dice unavailable — %1s rolled randomly", req.expression)
            mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        }
        onDispose { DiceSource.onManualFallback = null }
    }

    // ویبره‌ی تاس خودکار — تنظیمش جدا از حالت تاس است.
    val rootView = LocalView.current
    DisposableEffect(hapticsEnabled) {
        DiceSource.onAutoRoll = if (hapticsEnabled) {
            { rootView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
        } else null
        onDispose { DiceSource.onAutoRoll = null }
    }

    DisposableEffect(enabled) {
        if (enabled) {
            DiceSource.manual = { req ->
                // **مهم:** اگر این از نخ اصلی صدا زده شود، بلاک‌کردن یعنی فریز
                // کامل برنامه (دیالوگ هیچ‌وقت رسم نمی‌شود و کسی جواب نمی‌دهد).
                // پس فقط روی نخ غیراصلی منتظر می‌مانیم؛ روی نخ اصلی درخواست را
                // رد می‌کنیم تا فراخوان به تاس تصادفی برگردد و بازی قفل نشود.
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    emptyList()
                } else {
                    answers.clear()
                    mainHandler.post { pending = req }
                    // تا ۵ دقیقه منتظر GM می‌ماند؛ بعد به تصادفی برمی‌گردد
                    // تا برنامه برای همیشه گیر نکند.
                    val result = answers.poll(5, TimeUnit.MINUTES)
                    mainHandler.post { pending = null }
                    result ?: emptyList()
                }
            }
        } else {
            DiceSource.manual = null
        }
        onDispose { DiceSource.manual = null }
    }

    content()

    pending?.let { req ->
        ManualDiceDialogMulti(
            request = req,
            hapticsEnabled = hapticsEnabled,
            onSubmit = { values ->
                answers.offer(values)
                pending = null
            },
            onCancel = {
                // لیست خالی یعنی «جواب نداد» و DiceSource به تصادفی برمی‌گردد.
                answers.offer(emptyList())
                pending = null
            }
        )
    }
}

/**
 * دیالوگ ورود نتیجه‌ی تاس فیزیکی.
 * دو راه می‌دهد: وارد کردن تک‌تک تاس‌ها، یا وارد کردن مجموع خام.
 */
@Composable
private fun ManualDiceDialogMulti(
    request: DiceSource.Request,
    hapticsEnabled: Boolean,
    onSubmit: (List<Int>) -> Unit,
    onCancel: () -> Unit
) {
    val view = LocalView.current
    // حالت پیش‌فرض برای چند تاس، «مجموع» است چون سر میز سریع‌تر است.
    var sumMode by remember(request) { mutableStateOf(request.count > 1) }
    var each by remember(request) { mutableStateOf(List(request.count) { "" }) }
    var sumInput by remember(request) { mutableStateOf("") }

    val minSum = request.count
    val maxSum = request.count * request.sides
    val sumValue = sumInput.toIntOrNull()
    val sumValid = sumValue != null && sumValue in minSum..maxSum
    val eachValid = each.all { (it.toIntOrNull() ?: 0) in 1..request.sides }

    fun submit() {
        if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        if (sumMode) {
            // مجموع را بین تاس‌ها پخش می‌کنیم تا قواعدی که تک‌تک تاس‌ها را
            // می‌بینند (مثل «دو تا ۶ = Critical») هم چیزی برای خواندن داشته باشند.
            val total = sumValue ?: minSum
            val base = total / request.count
            val rem = total % request.count
            val spread = List(request.count) { i ->
                (base + if (i < rem) 1 else 0).coerceIn(1, request.sides)
            }
            onSubmit(spread)
        } else {
            onSubmit(each.map { it.toIntOrNull() ?: 1 })
        }
    }

    Dialog(onDismissRequest = onCancel) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DBlack),
                shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
                    .border(2.dp, DRed, CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
            ) {
                Column(Modifier.padding(20.dp).fillMaxWidth()) {

                    FaText(gtr("Physical dice"), color = DRed, fontSize = 18.sp,
                        fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(10.dp))

                    // چه تاسی لازم است — بزرگ و واضح
                    Box(
                        Modifier.fillMaxWidth()
                            .background(DRed.copy(alpha = .12f), CutCornerShape(10.dp))
                            .border(1.dp, DRed.copy(alpha = .5f), CutCornerShape(10.dp))
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                request.expression,
                                color = DGold, fontSize = 34.sp, fontWeight = FontWeight.Black
                            )
                        }
                    }

                    if (request.label.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        FaText(request.label, color = DWhite, fontSize = 13.sp,
                            lineHeight = 20.sp, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(Modifier.height(14.dp))

                    if (request.count > 1) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeChip(gtr("total"), sumMode, Modifier.weight(1f)) { sumMode = true }
                            ModeChip(gtr("each die"), !sumMode, Modifier.weight(1f)) { sumMode = false }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (sumMode || request.count == 1) {
                        val label = if (request.count == 1)
                            gtr("result (1-%1s)", request.sides)
                        else gtr("sum (%1s-%2s)", minSum, maxSum)
                        OutlinedTextField(
                            value = sumInput,
                            onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) sumInput = v },
                            label = { Text(label, color = DMuted, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DRed, unfocusedBorderColor = Color.Gray,
                                focusedTextColor = DWhite, unfocusedTextColor = DWhite
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center, fontSize = 26.sp,
                                fontWeight = FontWeight.Black, color = DGold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            each.forEachIndexed { i, v ->
                                OutlinedTextField(
                                    value = v,
                                    onValueChange = { nv ->
                                        if (nv.length <= 3 && nv.all { it.isDigit() })
                                            each = each.toMutableList().also { l -> l[i] = nv }
                                    },
                                    label = { Text(gtr("die %1s", i + 1), color = DMuted, fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DRed, unfocusedBorderColor = Color.Gray,
                                        focusedTextColor = DWhite, unfocusedTextColor = DWhite
                                    ),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Center, fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold, color = DGold
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = CutCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text(gtr("random instead"), color = DMuted, fontSize = 12.sp) }

                        Button(
                            onClick = { submit() },
                            enabled = if (sumMode || request.count == 1) sumValid else eachValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DRed, disabledContainerColor = Color(0xFF333333)
                            ),
                            shape = CutCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text(gtr("apply"), color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(if (selected) DRed else Color.Transparent, CutCornerShape(7.dp))
            .border(1.dp, if (selected) DRed else DMuted.copy(alpha = .4f), CutCornerShape(7.dp))
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.Black else DWhite,
            fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
    }
}
