package com.cyberpunk.gmtool.ui.screens

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.DiceSource

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)

// تنظیمات سراسری تاس (در NavGraph مقداردهی می‌شوند)
val LocalAutoDice = compositionLocalOf { true }
val LocalHaptics = compositionLocalOf { true }

/** رنگ استاندارد سوییچ در کل اپ: روشن = تامب مشکی روی ریل قرمز؛ خاموش = تامب قرمز روی ریل تیره */
@Composable
fun cprSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.Black,
    checkedTrackColor = Red,
    checkedBorderColor = Red,
    checkedIconColor = Red,
    uncheckedThumbColor = Red,
    uncheckedTrackColor = Color(0xFF2A2A2A),
    uncheckedBorderColor = Color(0xFF555555),
    uncheckedIconColor = Color.Transparent
)

/**
 * نسخه‌ی ساده: حالت تاس را از تنظیمات سراسری می‌خواند.
 * خروجی یک تابع «ریختن تاس» است که در حالت خودکار عدد تصادفی و در حالت دستی
 * دیالوگ ورود عدد تاس فیزیکی را باز می‌کند.
 */
@Composable
fun rememberAutoDiceRoller(sides: Int = 10, onResult: (Int) -> Unit): () -> Unit =
    rememberDiceRoller(
        autoDice = LocalAutoDice.current,
        hapticsEnabled = LocalHaptics.current,
        sides = sides,
        onResult = onResult
    )

// وضعیت درخواست تاس دستی (وقتی تاس خودکار خاموش است)
class ManualDiceState {
    var sides by mutableStateOf(10)
    var active by mutableStateOf(false)
    internal var callback: ((Int) -> Unit)? = null

    fun request(sides: Int = 10, onResult: (Int) -> Unit) {
        this.sides = sides
        this.callback = onResult
        active = true
    }

    fun submit(value: Int) {
        val cb = callback
        active = false
        callback = null
        cb?.invoke(value)
    }

    fun cancel() {
        active = false
        callback = null
    }
}

/**
 * یک «ریزنده‌ی تاس» می‌سازد.
 * - اگر تاس خودکار روشن باشد: بلافاصله عدد تصادفی تولید و onResult صدا زده می‌شود.
 * - اگر خاموش باشد: دیالوگ ورود دستی عدد تاس فیزیکی باز می‌شود.
 */
@Composable
fun rememberDiceRoller(
    autoDice: Boolean,
    hapticsEnabled: Boolean = true,
    sides: Int = 10,
    onResult: (Int) -> Unit
): () -> Unit {
    val state = remember { ManualDiceState() }
    val view = LocalView.current

    val rollNow: () -> Unit = {
        if (autoDice) {
            // تاس خودکار هم از DiceSource می‌آید تا «هیچ تاسی بیرون از آن ساخته
            // نمی‌شود» یک قاعده‌ی واقعی باشد، نه فقط یک کامنت. ویبره‌ی حالت
            // خودکار همان‌جا (onAutoRoll) زده می‌شود، پس اینجا دوباره ویبره نمی‌دهیم.
            onResult(DiceSource.rollOne(sides))
        } else {
            state.request(sides) { value -> onResult(value) }
        }
    }

    if (state.active) {
        ManualDiceDialog(
            sides = state.sides,
            onDismiss = { state.cancel() },
            onSubmit = { value ->
                if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                state.submit(value)
            }
        )
    }

    return rollNow
}

// دیالوگ ورود دستی نتیجه‌ی تاس فیزیکی
@Composable
fun ManualDiceDialog(sides: Int, onDismiss: () -> Unit, onSubmit: (Int) -> Unit) {
    var input by remember { mutableStateOf("") }
    val value = input.toIntOrNull()
    val isValid = value != null && value in 1..sides

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Black),
            shape = CutCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("تاس فیزیکی", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "تاس ۱ تا $sides وجهی را بریزید و عدد رو شده را وارد کنید.",
                    color = Color(0xFFAAAAAA), fontSize = 14.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) input = it },
                    label = { Text("نتیجه تاس (1-$sides)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Red, unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Red),
                    modifier = Modifier.fillMaxWidth(0.6f)
                )

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = CutCornerShape(8.dp)) {
                        Text("انصراف", color = Color.Gray)
                    }
                    Button(
                        onClick = { if (isValid) onSubmit(value!!) },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = Red, disabledContainerColor = Color(0xFF333333)),
                        shape = CutCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تایید", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
