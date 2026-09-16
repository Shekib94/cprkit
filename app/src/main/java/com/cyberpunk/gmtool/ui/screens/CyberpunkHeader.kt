package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberpunkHeader(
    title: String,
    onBackClick: (() -> Unit)? = null, // اگر null باشه، دکمه برگشت رو نشون نمی‌ده
    rightContent: @Composable RowScope.() -> Unit = {} // دکمه‌های سمت راست (مثل LUCK یا تنظیمات) میان اینجا
) {
    val cyberpunkRed = Color(0xFFC62828)
    val darkBackground = Color(0xFF0F0F0F)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(darkBackground)
            // این خط اضافه شد تا هدر بره زیر ساعت گوشی و قاطی نشه
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        // این کامپوننت به صورت هوشمند فاصله‌ی ساعتِ گوشی رو رعایت می‌کنه
        TopAppBar(
            title = {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = gtr("Back"),
                            tint = cyberpunkRed
                        )
                    }
                }
            },
            actions = rightContent, // هر چیزی که برای سمت راست بفرستیم اینجا قرار می‌گیره
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )

        // خط قرمزِ معروفِ سایبرپانکی زیر هدر
        Divider(
            color = cyberpunkRed,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}