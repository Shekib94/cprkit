package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.ImageProvider
import com.cyberpunk.gmtool.ui.components.RuleInfoButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    methodName: String, // <--- این متغیر اضافه شد تا بفهمه از کدوم روش اومدیم
    onBack: () -> Unit,
    onRoleSelected: (String, String) -> Unit // متغیر اول: اسم روش، متغیر دوم: اسم نقش
) {
    val cyberpunkRed = Color(0xFFC62828)
    val darkBackground = Color(0xFF0F0F0F)
    val componentBackground = Color(0xFF1A1A1A)

    val roles = listOf(
        "Solo", "Rockerboy", "Netrunner", "Tech", "Medtech",
        "Media", "Exec", "Lawman", "Fixer", "Nomad"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(roles[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing) // <--- این خط جادویی رو اضافه کن
            .padding(16.dp)
    ) {
        // ۱. هدر و دکمه برگشت
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = gtr("Back"), tint = cyberpunkRed)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = methodName, // <--- اسم روش به صورت داینامیک اینجا نوشته می‌شه
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = gtr("Select your Role"),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // ۲. منوی کشویی انتخاب نقش (بدون ارور رنگ)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedRole,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = componentBackground,
                    unfocusedContainerColor = componentBackground,
                    disabledContainerColor = componentBackground,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = cyberpunkRed,
                    unfocusedIndicatorColor = Color.DarkGray
                ),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                    .border(1.dp, if (expanded) cyberpunkRed else Color.DarkGray, CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(componentBackground)
            ) {
                roles.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(text = selectionOption, color = Color.White) },
                        onClick = {
                            selectedRole = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("راهنمای کوتاه نقش انتخاب‌شده", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
            RuleInfoButton(
                when (selectedRole) {
                    "Solo" -> "role.solo"
                    "Rockerboy" -> "role.rockerboy"
                    "Netrunner" -> "net.net_actions"
                    "Tech" -> "role.tech"
                    "Medtech" -> "role.medtech"
                    "Media" -> "role.media"
                    "Exec" -> "role.exec"
                    "Lawman" -> "role.lawman"
                    "Fixer" -> "role.fixer"
                    "Nomad" -> "role.nomad"
                    else -> "character.creation_methods"
                }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        // ۳. کادر نمایش عکس قدی کاراکتر
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(CutCornerShape(16.dp))
                .border(2.dp, Color(0xFF222222), CutCornerShape(16.dp))
                .background(Color(0xFF050505)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = ImageProvider.getRoleImage(selectedRole, isPortrait = false)),
                contentDescription = gtr("%1s Full Body Image", selectedRole),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ۴. دکمه انتخاب (Select)
        Button(
            // وقتی روی دکمه کلیک می‌شه، هم اسم روش (مثلاً Streetrat) و هم اسم نقش (مثلاً Solo) رو می‌فرستیم مرحله بعد
            onClick = { onRoleSelected(methodName, selectedRole) },
            shape = CutCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cyberpunkRed),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                text = gtr("SELECT %1s", selectedRole),
                fontSize = 18.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}