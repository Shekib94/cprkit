package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onEnterClick: () -> Unit,
    onExitClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val cyberRed = Color(0xFFD32F2F)
    val cyberBlack = Color(0xFF0F0F0F)
    val cyberTextWhite = Color(0xFFE0E0E0)

    Box(modifier = Modifier.fillMaxSize().background(cyberBlack)) {
        // دکمه تنظیمات گوشه‌ی بالا (با فاصله از نوار وضعیت گوشی)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(12.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = gtr("Settings"),
                tint = cyberRed,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // نام برنامه روی صفحه‌ی ورود (زیر آیکن لانچر «CPR KIT» است).
            Text(
                text = "CYBER KIT",
                color = cyberRed,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(bottom = 60.dp)
            )

            Button(
                onClick = onEnterClick,
                colors = ButtonDefaults.buttonColors(containerColor = cyberRed),
                shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
            ) {
                Text(text = gtr("ENTER SYSTEM"), color = cyberBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExitClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .border(2.dp, cyberRed, CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
            ) {
                Text(text = "EXIT", color = cyberRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
