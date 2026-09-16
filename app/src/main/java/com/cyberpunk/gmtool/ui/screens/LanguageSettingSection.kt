package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr



import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.AppLanguage

// هم‌رنگ با بقیه‌ی SettingsDialog
private val Red = Color(0xFFD32F2F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)

/**
 * بخش انتخاب زبان برنامه — داخل SettingsDialog استفاده می‌شود.
 *
 *     LanguageSettingSection(
 *         current = languageState.language,
 *         onSelect = { languageState.changeLanguage(it) }
 *     )
 */
@Composable
fun LanguageSettingSection(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, contentDescription = null, tint = Red)
            Spacer(Modifier.width(8.dp))
            Text(
                "زبان برنامه / LANGUAGE",
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "حالت «فارسی» همه‌ی متن‌ها را ترجمه می‌کند. حالت «دوزبانه» همان " +
            "رفتار اصلی برنامه است: سرخط‌ها انگلیسی، توضیحات فارسی.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Right
        )

        Spacer(Modifier.height(10.dp))

        LanguageOption(
            selected = current == AppLanguage.PERSIAN,
            title = "فارسی",
            subtitle = "تمام رابط، قوانین و کاتالوگ به فارسی",
            onClick = { onSelect(AppLanguage.PERSIAN) }
        )

        LanguageOption(
            selected = current == AppLanguage.BILINGUAL,
            title = "دوزبانه (پیش‌فرض)",
            subtitle = "سرخط‌ها انگلیسی، توضیحات فارسی",
            onClick = { onSelect(AppLanguage.BILINGUAL) }
        )

        val context = LocalContext.current
        LanguageOption(
            selected = current == AppLanguage.ENGLISH,
            title = gtr("English"),
            subtitle = "Coming soon — به‌زودی",
            enabled = false,
            onClick = {
                Toast.makeText(
                    context, com.cyberpunk.gmtool.ui.components.faToast("نسخه‌ی تمام‌انگلیسی هنوز آماده نیست — به‌زودی"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "نام مهارت‌ها و آیتم‌ها داخل فایل Save همیشه انگلیسی می‌ماند، " +
            "پس تغییر زبان به Saveهای شما آسیب نمی‌زند.",
            color = Muted,
            fontSize = 11.sp,
            textAlign = TextAlign.Right
        )
    }
}

@Composable
private fun LanguageOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                if (selected) Red.copy(alpha = 0.10f) else Color(0xFF1A1A1A),
                CutCornerShape(8.dp)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (selected) Red else if (enabled) Color(0xFF333333) else Color(0xFF262626)
                ),
                CutCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = Red,
                unselectedColor = Color(0xFF666666),
                disabledUnselectedColor = Color(0xFF444444)
            )
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) Red else if (enabled) White else Color(0xFF6E6E6E),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = if (enabled) Muted else Color(0xFF5C5C5C),
                fontSize = 11.sp
            )
        }
    }
}
