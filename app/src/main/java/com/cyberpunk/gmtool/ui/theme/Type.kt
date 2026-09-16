package com.cyberpunk.gmtool.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

// ==========================================
// --- تنظیمات تایپوگرافی (متن‌ها) در اپلیکیشن ---
// ==========================================

val Typography = Typography(

    // استایل متن‌های معمولی (بدنه اصلی)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        textDirection = TextDirection.Rtl
    ),

    // استایل تیترهای بزرگ (مثل اسم تب‌ها یا نام کاراکتر)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl
    ),

    // استایل متن‌های ریز (مثل زیرنویس‌ها، لیبل دکمه‌ها یا توضیحات کوچک)
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        textDirection = TextDirection.Rtl
    )

    /*
    نکته برای آینده:
    اگر بعداً خواستی فونت کاستوم (مثلا یه فونت سایبرپانکی) به پروژه اضافه کنی،
    کافیه فونت رو تو پوشه res/font بذاری و اینجا یه متغیر براش بسازی:
    val CyberFont = FontFamily(Font(R.font.cyberpunk_font))
    و بعد کلمه FontFamily.Default رو با CyberFont جایگزین کنی!
    */
)