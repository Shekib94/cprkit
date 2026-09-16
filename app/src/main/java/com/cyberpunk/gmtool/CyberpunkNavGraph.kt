package com.cyberpunk.gmtool

import com.cyberpunk.gmtool.data.gtr


import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cyberpunk.gmtool.data.CharacterFactory
import com.cyberpunk.gmtool.data.LifepathGenerator
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.ui.screens.DashboardScreen
import com.cyberpunk.gmtool.ui.screens.NewCharacterScreen
import com.cyberpunk.gmtool.ui.screens.RoleSelectionScreen
import com.cyberpunk.gmtool.ui.screens.StreetratRegistrationScreen
import com.cyberpunk.gmtool.ui.screens.WelcomeScreen

@Composable
fun CyberpunkNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: CharacterViewModel = viewModel()
) {
    val context = LocalContext.current
    val charactersList by viewModel.characters.collectAsState()
    var showSettings by remember { androidx.compose.runtime.mutableStateOf(false) }

    // ── سشن محلی: پیام‌ها، نجواها و بیدار نگه‌داشتن صفحه ──
    val lanRole by viewModel.lan.role.collectAsState()
    val lanToast by viewModel.lan.toast.collectAsState()
    val lanWhispers by viewModel.lan.whispers.collectAsState()

    // وقتی سشن فعال است صفحه نباید خاموش شود؛ قطع شدن سرور یعنی قطع همه.
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(lanRole) {
        val active = lanRole != com.cyberpunk.gmtool.data.net.LanRole.NONE
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }

    // تلاش یک‌باره برای اتصال مجدد خودکار با بلیت ذخیره‌شده
    val lanScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (viewModel.lan.role.value == com.cyberpunk.gmtool.data.net.LanRole.NONE &&
            viewModel.lan.hasSavedTicket()
        ) {
            viewModel.lan.tryAutoReconnect(lanScope, "")
        }
    }

    lanToast?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.lan.clearToast() },
            containerColor = Color(0xFF1A1A1A),
            shape = androidx.compose.foundation.shape.CutCornerShape(10.dp),
            text = {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    androidx.compose.material3.Text(msg, color = Color(0xFFEEEEEE), fontSize = 13.sp)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.lan.clearToast() }) {
                    androidx.compose.material3.Text(
                        com.cyberpunk.gmtool.data.gtr("got it"), color = Color(0xFFE53935)
                    )
                }
            }
        )
    }

    if (lanWhispers.isNotEmpty()) {
        com.cyberpunk.gmtool.ui.components.WhisperDialog(
            text = lanWhispers.first(),
            onDismiss = { viewModel.lan.consumeWhisper(0) }
        )
    }

    if (showSettings) {
        com.cyberpunk.gmtool.ui.screens.SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.cyberpunk.gmtool.ui.screens.LocalAutoDice provides viewModel.autoDice,
        com.cyberpunk.gmtool.ui.screens.LocalHaptics provides viewModel.hapticsEnabled
    ) {
      // میزبان تاس دستی دور کل برنامه: وقتی «تاس خودکار» خاموش باشد، هر تاسی
      // که قواعد لازم داشته باشند از GM پرسیده می‌شود — در هر صفحه‌ای.
      com.cyberpunk.gmtool.ui.components.ManualDiceHost(
          enabled = !viewModel.autoDice,
          hapticsEnabled = viewModel.hapticsEnabled
      ) {
        NavHost(
            navController = navController,
            startDestination = "welcome"
        ) {

        // ۱. صفحه شروع
        composable(route = "welcome") {
            WelcomeScreen(
                onEnterClick = { navController.navigate("dashboard") },
                onExitClick = { (context as? Activity)?.finish() },
                onSettingsClick = { showSettings = true }
            )
        }

        // ۲. داشبورد (لیست بازیکنان و NPCها)
        composable(route = "dashboard") {
            // انتخاب فایل JSON برای ایمپورت
            val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        val json = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()?.use { it.readText() } ?: ""
                        val imported = com.cyberpunk.gmtool.data.CharacterIO.parseList(json)
                        val count = viewModel.importCharacters(imported)
                        Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast(if (count > 0) gtr("%1s characters/NPCs imported", count) else gtr("file is not valid")),
                            Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, com.cyberpunk.gmtool.ui.components.faToast(gtr("error reading file")), Toast.LENGTH_LONG).show()
                    }
                }
            }

            DashboardScreen(
                charactersList = charactersList,
                viewModel = viewModel,
                onCharacterSelect = { selectedChar ->
                    navController.navigate("character_sheet/${selectedChar.id}")
                },
                onNewCharacterClick = { navController.navigate("new_character") },
                onNewNpcClick = { navController.navigate("new_npc") },
                onImportClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                onBackClick = {
                    navController.navigate("welcome") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onDeleteCharacter = { char -> viewModel.deleteCharacter(char.id) }
            )
        }

        // ۲.۵. ساخت NPC جدید
        composable(route = "new_npc") {
            com.cyberpunk.gmtool.ui.screens.NewNpcScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNpcCreated = { id ->
                    navController.navigate("character_sheet/$id") {
                        popUpTo("new_npc") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // NPCها از همان CharacterSheetScreen کامل استفاده می‌کنند؛ شیت ساده‌ی قدیمی حذف شد.

        // ۳. انتخاب متد ساخت
        composable(route = "new_character") {
            NewCharacterScreen(
                onBack = { navController.popBackStack() },
                onMethodSelected = { methodName ->
                    val encoded = android.net.Uri.encode(methodName)
                    navController.navigate("role_selection/$encoded")
                }
            )
        }

        // ۴. انتخاب نقش
        composable(route = "role_selection/{methodName}") { backStackEntry ->
            val methodName = android.net.Uri.decode(backStackEntry.arguments?.getString("methodName") ?: "Unknown Method")

            RoleSelectionScreen(
                methodName = methodName,
                onBack = { navController.popBackStack() },
                onRoleSelected = { _, selectedRole ->
                    when {
                        methodName.contains("Edgerunner", ignoreCase = true) ->
                            navController.navigate("character_builder/${android.net.Uri.encode(selectedRole)}/edge")
                        methodName.contains("Complete", ignoreCase = true) ->
                            navController.navigate("character_builder/${android.net.Uri.encode(selectedRole)}/complete")
                        else ->
                            navController.navigate("streetrat_registration/${android.net.Uri.encode(selectedRole)}")
                    }
                }
            )
        }

        // ۴.۵. سازنده‌ی کاراکتر (Edgerunners و Complete Package)
        composable(route = "character_builder/{roleName}/{method}") { backStackEntry ->
            val roleName = android.net.Uri.decode(backStackEntry.arguments?.getString("roleName") ?: "Solo")
            val methodCode = backStackEntry.arguments?.getString("method") ?: "edge"
            val methodName = if (methodCode == "complete")
                "Complete Package (Calculated)" else "Edgerunners (Fast & Dirty)"

            com.cyberpunk.gmtool.ui.screens.CharacterBuilderScreen(
                methodName = methodName,
                roleName = roleName,
                onBack = { navController.popBackStack() },
                isHandleTaken = { h -> viewModel.handleTaken(h) },
                onCreate = { legalName, handle, stats, skillLevels, selectedChoices ->
                    val baseLifepath = LifepathGenerator.generateFullLifepath().apply {
                        roleLifepath = com.cyberpunk.gmtool.data.RoleLifepathGenerator.generateForRole(roleName)
                    }
                    val newCharacter = com.cyberpunk.gmtool.data.CharacterFactory.createCustom(
                        legalName = legalName,
                        handle = handle,
                        roleName = roleName,
                        creationMethod = methodName,
                        stats = stats,
                        skillLevels = skillLevels,
                        startingEurodollars = null,
                        selectedChoices = selectedChoices,
                        lifepath = baseLifepath
                    )
                    val savedCharacter = viewModel.addCharacter(newCharacter)
                    navController.navigate("character_sheet/${savedCharacter.id}") {
                        popUpTo("dashboard")
                    }
                }
            )
        }

        // ۵. فرم ثبت‌نام و تولید کاراکتر
        composable(route = "streetrat_registration/{roleName}") { backStackEntry ->
            val roleName = android.net.Uri.decode(backStackEntry.arguments?.getString("roleName") ?: "Solo")

            StreetratRegistrationScreen(
                roleName = roleName,
                isHandleTaken = { h -> viewModel.handleTaken(h) },
                onCharacterGenerated = { legalName, handle, stats, skillLevels, equipment, choices ->
                    // لایف‌پث پایه هنگام ورود به شهر تولید می‌شود
                    val baseLifepath = LifepathGenerator.generateFullLifepath().apply {
                        roleLifepath = com.cyberpunk.gmtool.data.RoleLifepathGenerator.generateForRole(roleName)
                    }

                    val newCharacter = CharacterFactory.create(
                        legalName = legalName,
                        handle = handle,
                        roleName = roleName,
                        creationMethod = "Streetrat Template",
                        stats = stats,
                        skillLevels = skillLevels,
                        equipment = equipment,
                        selectedChoices = choices,
                        lifepath = baseLifepath
                    )

                    val savedCharacter = viewModel.addCharacter(newCharacter)

                    navController.navigate("character_sheet/${savedCharacter.id}") {
                        popUpTo("dashboard")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ۶. کاراکتر شیت
        composable(
            route = "character_sheet/{characterId}?tab={tab}",
            arguments = listOf(
                navArgument("characterId") { type = NavType.IntType },
                navArgument("tab") { type = NavType.StringType; defaultValue = "BIO" }
            )
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getInt("characterId") ?: -1
            val initialTab = backStackEntry.arguments?.getString("tab") ?: "BIO"
            val activeChar = charactersList.firstOrNull { it.id == characterId }

            if (activeChar != null) {
                com.cyberpunk.gmtool.ui.screens.CharacterSheetScreen(
                    character = activeChar,
                    viewModel = viewModel,
                    initialTab = initialTab,
                    onBackClick = { navController.popBackStack() },
                    onSwitchCharacter = { newId, currentTab ->
                        navController.navigate("character_sheet/$newId?tab=$currentTab") {
                            popUpTo("character_sheet/{characterId}?tab={tab}") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        } // NavHost
      } // ManualDiceHost
    } // CompositionLocalProvider
}
