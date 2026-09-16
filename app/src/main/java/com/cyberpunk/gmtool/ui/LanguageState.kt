package com.cyberpunk.gmtool.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.AndroidViewModel
import com.cyberpunk.gmtool.data.AppLanguage
import com.cyberpunk.gmtool.data.LocalStrings
import com.cyberpunk.gmtool.data.LocalizationLoader
import com.cyberpunk.gmtool.data.SettingsRepository

/**
 * نگه‌دارنده‌ی زبان جاری برنامه.
 *
 * تغییر زبان بلافاصله کل UI را دوباره می‌سازد (بدون ری‌استارت Activity)
 * چون `language` یک MutableState است.
 */
class LanguageState(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)

    var language by mutableStateOf(settings.appLanguage)
        private set

    /** تغییر زبان برنامه.
     *  نام عمداً `setLanguage` نیست: پراپرتی `language` خودش یک setter
     *  با همان امضای JVM می‌سازد و باعث «Platform declaration clash» می‌شود. */
    fun changeLanguage(new: AppLanguage) {
        if (new == language) return
        settings.appLanguage = new
        language = new
    }
}

/**
 * ریشه‌ی زبان: جدول ترجمه و جهت چیدمان را برای کل درخت فراهم می‌کند.
 *
 * در MainActivity به‌جای فراخوانی مستقیم CyberpunkNavGraph بنویسید:
 *
 *     LocalizedApp(language = languageState.language) {
 *         CyberpunkNavGraph()
 *     }
 *
 * توجه: چون برنامه از قبل RTL را دستی مدیریت می‌کند
 * (CompositionLocalProvider های داخل StoreScreen و ...)، جهت پیش‌فرض
 * را در حالت فارسی روی Rtl می‌گذاریم؛ آن Provider های محلی همچنان
 * کار خودشان را می‌کنند و چیزی نمی‌شکند.
 */
@Composable
fun LocalizedApp(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val strings = remember(language) {
        LocalizationLoader.load(context, language)
    }
    // snapshot سراسری برای gtr() — در هر ترکیب مجدد هم هم‌گام می‌ماند.
    // (داخل remember نمی‌گذاریم چون remember ممکن است اجرا نشود.)
    androidx.compose.runtime.SideEffect {
        com.cyberpunk.gmtool.data.GlobalStrings.current = strings
    }
    // چیدمان (layout) همیشه چپ‌به‌راست می‌ماند تا ترتیب تب‌ها، دکمه‌ها و
    // نوار پایین در هیچ زبانی وارونه نشود. راست‌چین‌شدنِ *متن* فارسی
    // به‌صورت موضعی با LayoutDirection.Rtl / TextDirection.Rtl انجام می‌شود.
    val direction = LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides direction,
        content = content
    )
}
