package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NewCharacterScreen(
    onBack: () -> Unit,
    onMethodSelected: (String) -> Unit
) {
    val cyberpunkRed = Color(0xFFC62828)
    val darkBackground = Color(0xFF0F0F0F)
    val cardBackground = Color(0xFF151515)

    // استفاده از Scaffold به عنوان اسکلتِ اصلی صفحه
    Scaffold(
        topBar = {
            // فراخوانیِ هدری که ساختیم
            CyberpunkHeader(
                title = gtr("New Character"),
                onBackClick = onBack
            )
        },
        containerColor = darkBackground // رنگ پس‌زمینه‌ی کل صفحه
    ) { paddingValues ->

        // محتوای میانی (Main Content)
        // متغیر paddingValues به صورت اتوماتیک فاصله‌ی هدرِ بالا رو حساب می‌کنه تا کارت‌ها نچسبن به سقف
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // <--- این خط جادوییه!
                .padding(16.dp),
            verticalArrangement = Arrangement.Center, // کارت‌ها رو میاره وسط صفحه
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MethodCard(
                title = gtr("Streetrats (Template)"),
                description = "ساخت سریع و متوازن با استفاده از الگوهای آماده. دارای آمار، مهارت‌ها و تجهیزات از پیش تعیین شده برای شروع بی‌دردسر.",
                cyberpunkRed = cyberpunkRed,
                cardBackground = cardBackground,
                onClick = { onMethodSelected("Streetrat Template") }
            )

            MethodCard(
                title = gtr("Edgerunners (Fast & Dirty)"),
                description = "آمار (Stats) به صورت تصادفی تعیین می‌شوند، اما امتیاز مهارت‌ها را خودتان پخش می‌کنید. تجهیزات از پیش تعیین شده است.",
                cyberpunkRed = cyberpunkRed,
                cardBackground = cardBackground,
                onClick = { onMethodSelected("Edgerunners (Fast & Dirty)") }
            )

            MethodCard(
                title = gtr("Complete Package (Calculated)"),
                description = "ساخت صفر تا صد کاراکتر. آمار و مهارت‌ها را با امتیازات پایه می‌خرید و تجهیزات و سایبرورها را با بودجه‌ی اولیه تهیه می‌کنید.",
                cyberpunkRed = cyberpunkRed,
                cardBackground = cardBackground,
                onClick = { onMethodSelected("Complete Package (Calculated)") }
            )
        }
    }
}

@Composable
fun MethodCard(
    title: String,
    description: String,
    cyberpunkRed: Color,
    cardBackground: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clip(CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
            .background(cardBackground)
            .border(1.dp, cyberpunkRed.copy(alpha = 0.7f), CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Left
            )

            Spacer(modifier = Modifier.height(12.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = description,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}