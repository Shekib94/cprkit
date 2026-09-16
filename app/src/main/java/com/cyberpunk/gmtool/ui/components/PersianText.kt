package com.cyberpunk.gmtool.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * متن فارسی درست‌چین.
 *
 * چرا این فایل لازم شد:
 * تم پروژه `TextDirection.ContentOrRtl` داشت. آن حالت جهت را از **اولین کاراکتر**
 * پاراگراف می‌گیرد؛ پس هر جمله‌ی فارسی که با یک واژه‌ی انگلیسی شروع شود
 * (مثل «Autofire یعنی...» یا «STAT ها ویژگی...») کلاً چپ‌چین رندر می‌شد و
 * نقطه و ویرگول به سمت اشتباه می‌پرید. `TextAlign.Right` هم این را درست نمی‌کند،
 * چون مشکل «تراز» نیست، «جهت پاراگراف» است.
 *
 * راه‌حل: جهت را صریحاً Rtl اعلام کن. آن‌وقت واژه‌های انگلیسی داخل جمله
 * طبق الگوریتم دوجهته‌ی یونیکد خودشان چپ‌به‌راست می‌مانند، ولی خودِ جمله راست‌چین است.
 */
val PersianTextStyle = TextStyle(
    textDirection = TextDirection.Rtl,
    textAlign = TextAlign.Right
)

/** همان، ولی دوسرچین برای پاراگراف‌های بلند. */
val PersianJustifyStyle = TextStyle(
    textDirection = TextDirection.Rtl,
    textAlign = TextAlign.Justify
)

/**
 * واژه‌های لاتین داخل جمله‌ی فارسی را «جدا» می‌کند.
 *
 * چرا لازم است: الگوریتم دوجهته‌ی یونیکد وقتی یک تکه‌ی لاتین در انتهای جمله‌ی
 * فارسی می‌بیند، نقطه و ویرگولِ بعدش را جزو همان تکه‌ی چپ‌به‌راست حساب می‌کند
 * و آن‌ها را به سمت اشتباه پرت می‌کند. مثلاً:
 *     «Primary Effect را +1 می‌کند.»  →  «.را +1 می‌کند Primary Effect»
 *
 * با گذاشتن U+2066 (isolate چپ‌به‌راست) و U+2069 (پایان isolate) دور هر تکه‌ی
 * لاتین، آن تکه یک واحد بسته می‌شود و بقیه‌ی جمله راست‌به‌چپ می‌ماند.
 * این تنها راه درستِ متن مخلوط است؛ TextAlign و TextDirection به‌تنهایی کافی نیستند.
 */
fun isolateLatin(text: String): String {
    // نقطه/دونقطه/اسلش فقط وقتی داخل یک تکه می‌مانند که **بلافاصله** حرف لاتین
    // بعدشان بیاید: «Torture/Drugs» و «d100:48» یک واحد می‌مانند.
    // ولی «Clinic. 3» با فاصله آمده، پس نقطه پایان جمله است و بیرون می‌ماند —
    // دقیقاً همان نقطه‌ای که در توضیح سایبرویر به اول خط می‌پرید.
    val pattern = Regex("[A-Za-z0-9]+(?:[.:&/=+\\-][A-Za-z0-9]+|[ ]+[A-Za-z0-9]+)*")
    return pattern.replace(text) { m -> "\u2066${m.value}\u2069" }
}


/**
 * متن Toast فارسی.
 *
 * Toast را خود اندروید رندر می‌کند نه Compose — پس `FaText` رویش اثری ندارد
 * و `TextAlign` هم در دسترس نیست. تنها راه، اصلاح خودِ رشته است:
 *   • تکه‌های لاتین isolate می‌شوند تا نقطه و ویرگول جابه‌جا نشوند
 *   • یک RLM (U+200F) اول رشته می‌آید تا جهت کل پیام راست‌به‌چپ شود،
 *     حتی وقتی جمله با یک نام انگلیسی مثل «Cyberaudio Suite» شروع می‌شود.
 */
fun faToast(text: String): String {
    val hasPersian = text.any { it in '\u0600'..'\u06FF' }
    if (!hasPersian) return text
    return "\u200F" + isolateLatin(text)
}

/**
 * متن فارسی با جهت درست. جایگزین `Text(..., textAlign = TextAlign.Right)`.
 * همیشه تمام عرض را می‌گیرد وگرنه راست‌چینی دیده نمی‌شود.
 *
 * تکه‌های لاتین خودکار isolate می‌شوند، پس جمله‌های مخلوط بهم نمی‌ریزند.
 */
@Composable
fun FaText(
    text: String,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    justify: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    Text(
        text = isolateLatin(text),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines = maxLines,
        modifier = modifier,
        style = LocalTextStyle.current.merge(
            if (justify) PersianJustifyStyle else PersianTextStyle
        )
    )
}
