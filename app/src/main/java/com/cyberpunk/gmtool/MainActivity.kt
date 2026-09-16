package com.cyberpunk.gmtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberpunk.gmtool.ui.LanguageState
import com.cyberpunk.gmtool.ui.LocalizedApp
import com.cyberpunk.gmtool.ui.theme.CyberpunkGMToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ثبت آخرین خطای کشنده روی حافظه‌ی داخلی (filesDir/last-crash.txt).
        // داشبورد آن را نشان می‌دهد، پس «برنامه بسته شد» دیگر بی‌سرنخ نیست.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            com.cyberpunk.gmtool.data.CrashLog.record(applicationContext, error)
            previousHandler?.uncaughtException(thread, error)
        }
        setContent {
            val languageState: LanguageState = viewModel()
            CyberpunkGMToolTheme {
                // چیدمان کل برنامه همیشه چپ‌به‌راست است تا ترتیب تب‌ها و
                // جهت دکمه‌ها با زبان دستگاه (فارسی) وارونه نشود.
                // متن فارسی هرجا لازم باشد به‌صورت موضعی راست‌چین می‌شود.
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF0F0F0F)
                    ) {
                        LocalizedApp(language = languageState.language) {
                            CyberpunkNavGraph()
                        }
                    }
                }
            }
        }
    }
}
