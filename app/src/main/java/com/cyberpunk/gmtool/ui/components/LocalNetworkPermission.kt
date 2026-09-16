package com.cyberpunk.gmtool.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * مجوز دسترسی به شبکه‌ی محلی (Local Network Protections).
 *
 * ============================================================
 * مسئله‌ای که این فایل حل می‌کند:
 * سرور سشن محلی روی HTTP ساده و IP داخل شبکه کار می‌کند. تا اندروید ۱۶ هر اپی که
 * `INTERNET` داشت به‌طور ضمنی به شبکه‌ی محلی هم دسترسی داشت. از اندروید ۱۷
 * (API 37) به بعد — یعنی همان `targetSdk` این پروژه — دسترسی به شبکه‌ی محلی
 * **به‌صورت پیش‌فرض بسته است** و اپ باید مجوز زمان‌اجرا
 * `android.permission.ACCESS_LOCAL_NETWORK` (گروه NEARBY_DEVICES) را از کاربر
 * بگیرد. بدون آن، ساخت سشن و پیوستن بازیکن‌ها با خطای socket شکست می‌خورد —
 * دقیقاً همان چیزی که این پروژه گفته نباید هیچ‌وقت بی‌صدا بشکند.
 *
 * نکته‌های مستندات اندروید:
 *  • اگر اپ روی SDK 36 یا کمتر target کند، دسترسی ضمنی از راه `INTERNET` می‌آید و
 *    **نباید** این مجوز را declare/request کند؛ پس این دروازه فقط برای target >= 37
 *    معنا دارد و روی نسخه‌های پایین‌تر کاملاً بی‌اثر است.
 *  • APIهای system-mediated (مثل انتخاب‌گرهای سیستمی) مستثنا هستند؛ سوکت خام و
 *    HTTP روی LAN این پروژه نه.
 *  • بعضی ROMهای سازنده (شیائومی/اوپو/هواوی…) از قدیم یک کلید دستیِ «شبکه‌ی محلی»
 *    جدا از مجوزهای اندروید دارند؛ آن با کد حل نمی‌شود و در راهنمای همان صفحه
 *    به کاربر گفته شده است.
 */
object LocalNetworkPermission {

    /**
     * نام مجوز. از ثابتِ `Manifest.permission` استفاده نمی‌کنیم تا روی compileSdk
     * قدیمی‌تر هم بدون گاردِ نسخه کامپایل شود (مقدارش همان رشته است).
     */
    const val NAME: String = "android.permission.ACCESS_LOCAL_NETWORK"

    /** روی اندروید ۱۷ (API 37) و بالاتر، مجوز زمان‌اجرا لازم است. */
    val isRequired: Boolean get() = Build.VERSION.SDK_INT >= 37

    fun isGranted(context: Context): Boolean =
        !isRequired ||
            ContextCompat.checkSelfPermission(context, NAME) == PackageManager.PERMISSION_GRANTED
}

/**
 * دروازه‌ی اجرای کارهای شبکه‌ی محلی (ساخت سشن، پیوستن به سشن).
 *
 * الگوی استفاده در UI:
 * ```
 * val lanGate = rememberLocalNetworkGate()
 * Button(onClick = { lanGate.run { viewModel.lan.startHosting(id, name) } })
 * if (lanGate.denied.value) { /* راهنمای دستی */ }
 * ```
 */
class LocalNetworkGate internal constructor(
    private val context: Context,
    /** کاربر مجوز را رد کرده — UI باید راهنمای روشن‌کردن دستی را نشان دهد. */
    val denied: MutableState<Boolean>,
    private val launchRequest: (onResult: (Boolean) -> Unit) -> Unit
) {
    val required: Boolean get() = LocalNetworkPermission.isRequired

    /** آیا الان باید اول مجوز گرفت؟ (هر بار از سیستم می‌پرسد، نه از حافظه) */
    val needsGrant: Boolean
        get() = required && !LocalNetworkPermission.isGranted(context)

    /**
     * کار شبکه‌ی محلی را اجرا می‌کند.
     * اگر مجوز لازم نباشد یا از قبل داده شده باشد، مستقیم اجرا می‌شود؛ وگرنه اول
     * پرسیده می‌شود و بعد از تأیید کاربر همان کار اجرا می‌شود. رد شدن یعنی هیچ
     * کاری انجام نمی‌شود و [denied] روشن می‌ماند تا UI راهنما نشان دهد.
     */
    fun run(block: () -> Unit) {
        if (!needsGrant) {
            block()
            return
        }
        launchRequest { ok ->
            if (ok) {
                denied.value = false
                block()
            } else {
                denied.value = true
            }
        }
    }
}

/**
 * یک [LocalNetworkGate] می‌سازد.
 *
 * وضعیت مجوز هر بار در لحظه‌ی اقدام از خود سیستم خوانده می‌شود، پس اگر کاربر
 * مجوز را از تنظیمات سیستم دستی روشن کند، بدون ری‌استارت برنامه کار می‌کند.
 */
@Composable
fun rememberLocalNetworkGate(): LocalNetworkGate {
    val context = LocalContext.current
    val denied = remember { mutableStateOf(false) }
    // نتیجه‌ی درخواست به آخرین اقدامِ در جریان برمی‌گردد.
    val pending = remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        val cb = pending.value
        pending.value = null
        cb?.invoke(ok)
    }

    return remember(context) {
        LocalNetworkGate(context, denied) { onResult ->
            pending.value = onResult
            launcher.launch(LocalNetworkPermission.NAME)
        }
    }
}
