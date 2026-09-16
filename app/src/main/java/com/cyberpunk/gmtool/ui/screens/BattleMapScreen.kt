package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cyberpunk.gmtool.R
import com.cyberpunk.gmtool.data.BattleMapDefinition
import com.cyberpunk.gmtool.data.battleMapDrawableResId
import com.cyberpunk.gmtool.data.BattleMapStore
import com.cyberpunk.gmtool.data.BattleMapToken
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

private val BmRed = Color(0xFFD32F2F)
private val BmBg = Color(0xFF0F0F0F)
private val BmCard = Color(0xFF191919)
private val BmCyan = Color(0xFF65E6E3)
private val BmLibraryCard = Color(0xFF111B24)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val tokenPalette = listOf(
    0xFF2196F3L, 0xFFE53935L, 0xFF43A047L, 0xFFFFB300L,
    0xFF8E24AAL, 0xFF00ACC1L, 0xFFFB8C00L, 0xFF7CB342L,
    0xFF5E35B1L, 0xFFD81B60L, 0xFF00897BL, 0xFF6D4C41L
)

private enum class BattleMapMode { PAN, MEASURE, PLACE_TOKEN, PLACE_ENV_TOKEN }
private data class TokenMoveUndo(val mapId: String, val token: BattleMapToken)
private data class EnvironmentTokenSpec(
    val id: String,
    val label: String,
    val category: String,
    val drawableRes: Int,
    val defaultWidth: Int = 1,
    val defaultHeight: Int = 1,
    val layer: String = "environment"
)

private val environmentTokenSpecs = listOf(
    EnvironmentTokenSpec("camera_oneway", "دوربین یک‌طرفه", "امنیت", R.drawable.env_camera_oneway, 1, 1, "environment"),
    EnvironmentTokenSpec("camera_360", "دوربین ۳۶۰ درجه", "امنیت", R.drawable.env_camera_360, 1, 1, "environment"),
    EnvironmentTokenSpec("motion_sensor", "حسگر حرکتی", "امنیت", R.drawable.env_motion_sensor, 1, 1, "environment"),
    EnvironmentTokenSpec("turret_auto", "برجک خودکار", "امنیت", R.drawable.env_turret_auto, 1, 1, "environment"),
    EnvironmentTokenSpec("drone_surveillance", "پهپاد شناسایی", "امنیت", R.drawable.env_drone_surveillance, 1, 1, "environment"),
    EnvironmentTokenSpec("drone_combat", "پهپاد رزمی", "امنیت", R.drawable.env_drone_combat, 1, 1, "environment"),
    EnvironmentTokenSpec("patrol_bot", "ربات گشت", "امنیت", R.drawable.env_patrol_bot, 1, 1, "environment"),
    EnvironmentTokenSpec("access_panel", "پنل دسترسی", "امنیت", R.drawable.env_access_panel, 1, 1, "environment"),
    EnvironmentTokenSpec("terminal", "ترمینال", "فناوری", R.drawable.env_terminal, 1, 1, "environment"),
    EnvironmentTokenSpec("server_rack", "رک سرور", "فناوری", R.drawable.env_server_rack, 1, 2, "environment"),
    EnvironmentTokenSpec("control_panel", "پنل کنترل", "فناوری", R.drawable.env_control_panel, 1, 1, "environment"),
    EnvironmentTokenSpec("generator", "ژنراتور", "فناوری", R.drawable.env_generator, 2, 2, "environment"),
    EnvironmentTokenSpec("worklight", "چراغ کار", "فناوری", R.drawable.env_worklight, 1, 1, "environment"),
    EnvironmentTokenSpec("floodlight", "نورافکن", "فناوری", R.drawable.env_floodlight, 1, 1, "environment"),
    EnvironmentTokenSpec("door_closed", "در بسته", "در و دسترسی", R.drawable.env_door_closed, 1, 2, "environment"),
    EnvironmentTokenSpec("reinforced_door", "در تقویت‌شده", "در و دسترسی", R.drawable.env_reinforced_door, 1, 2, "environment"),
    EnvironmentTokenSpec("airlock", "ایرلاک", "در و دسترسی", R.drawable.env_airlock, 1, 2, "environment"),
    EnvironmentTokenSpec("door_open", "در باز", "در و دسترسی", R.drawable.env_door_open, 1, 2, "environment"),
    EnvironmentTokenSpec("security_door_open", "در امنیتی باز", "در و دسترسی", R.drawable.env_security_door_open, 1, 2, "environment"),
    EnvironmentTokenSpec("elevator_door", "در آسانسور", "در و دسترسی", R.drawable.env_elevator_door, 1, 2, "environment"),
    EnvironmentTokenSpec("hatch", "دریچه", "در و دسترسی", R.drawable.env_hatch, 1, 1, "environment"),
    EnvironmentTokenSpec("gate_barrier", "راهبند", "در و دسترسی", R.drawable.env_gate_barrier, 1, 3, "environment"),
    EnvironmentTokenSpec("fence", "فنس", "در و دسترسی", R.drawable.env_fence, 1, 3, "environment"),
    EnvironmentTokenSpec("concrete_barrier", "مانع بتنی", "کاور و محیط", R.drawable.env_concrete_barrier, 1, 2, "environment"),
    EnvironmentTokenSpec("sandbags", "کیسه شن", "کاور و محیط", R.drawable.env_sandbags, 1, 2, "environment"),
    EnvironmentTokenSpec("concrete_block", "بلوک بتنی", "کاور و محیط", R.drawable.env_concrete_block, 1, 2, "environment"),
    EnvironmentTokenSpec("low_wall", "دیوار کوتاه", "کاور و محیط", R.drawable.env_low_wall, 1, 3, "environment"),
    EnvironmentTokenSpec("tall_wall", "دیوار بلند", "کاور و محیط", R.drawable.env_tall_wall, 1, 3, "environment"),
    EnvironmentTokenSpec("pillar", "ستون", "کاور و محیط", R.drawable.env_pillar, 1, 1, "environment"),
    EnvironmentTokenSpec("rubble", "آوار", "کاور و محیط", R.drawable.env_rubble, 2, 2, "environment"),
    EnvironmentTokenSpec("crate", "جعبه بار", "کاور و محیط", R.drawable.env_crate, 1, 1, "environment"),
    EnvironmentTokenSpec("container", "کانتینر", "کاور و محیط", R.drawable.env_container, 2, 4, "environment"),
    EnvironmentTokenSpec("barrels", "بشکه‌ها", "کاور و محیط", R.drawable.env_barrels, 1, 2, "environment"),
    EnvironmentTokenSpec("cargo_pallet", "پالت بار", "کاور و محیط", R.drawable.env_cargo_pallet, 2, 2, "environment"),
    EnvironmentTokenSpec("supply_crates", "جعبه‌های تدارکات", "کاور و محیط", R.drawable.env_supply_crates, 2, 2, "environment"),
    EnvironmentTokenSpec("car", "خودرو", "وسایل نقلیه", R.drawable.env_car, 2, 4, "environment"),
    EnvironmentTokenSpec("van", "ون", "وسایل نقلیه", R.drawable.env_van, 2, 4, "environment"),
    EnvironmentTokenSpec("truck", "کامیون", "وسایل نقلیه", R.drawable.env_truck, 2, 5, "environment"),
    EnvironmentTokenSpec("motorcycle", "موتورسیکلت", "وسایل نقلیه", R.drawable.env_motorcycle, 1, 2, "environment"),
    EnvironmentTokenSpec("av", "AV", "وسایل نقلیه", R.drawable.env_av, 2, 4, "environment"),
    EnvironmentTokenSpec("boat", "قایق", "وسایل نقلیه", R.drawable.env_boat, 2, 4, "environment"),
    EnvironmentTokenSpec("jetski", "جت‌اسکی", "وسایل نقلیه", R.drawable.env_jetski, 1, 2, "environment"),
    EnvironmentTokenSpec("atv", "ATV", "وسایل نقلیه", R.drawable.env_atv, 2, 3, "environment"),
    EnvironmentTokenSpec("buggy", "باگی", "وسایل نقلیه", R.drawable.env_buggy, 2, 3, "environment"),
    EnvironmentTokenSpec("armored_buggy", "باگی زرهی", "وسایل نقلیه", R.drawable.env_armored_buggy, 2, 4, "environment"),
    EnvironmentTokenSpec("armored_vehicle", "خودروی زرهی", "وسایل نقلیه", R.drawable.env_armored_vehicle, 3, 5, "environment"),
    EnvironmentTokenSpec("explosive_barrel", "بشکه انفجاری", "خطرات و افکت‌ها", R.drawable.env_explosive_barrel, 1, 1, "effects"),
    EnvironmentTokenSpec("flammable_canister", "مخزن اشتعال‌پذیر", "خطرات و افکت‌ها", R.drawable.env_flammable_canister, 1, 1, "effects"),
    EnvironmentTokenSpec("electrical_hazard", "خطر برق", "خطرات و افکت‌ها", R.drawable.env_electrical_hazard, 1, 1, "effects"),
    EnvironmentTokenSpec("toxic_hazard", "مواد سمی", "خطرات و افکت‌ها", R.drawable.env_toxic_hazard, 2, 2, "effects"),
    EnvironmentTokenSpec("fire", "آتش", "خطرات و افکت‌ها", R.drawable.env_fire, 2, 2, "effects"),
    EnvironmentTokenSpec("smoke_dark", "دود تیره", "خطرات و افکت‌ها", R.drawable.env_smoke_dark, 2, 2, "effects"),
    EnvironmentTokenSpec("smoke_light", "دود روشن", "خطرات و افکت‌ها", R.drawable.env_smoke_light, 2, 2, "effects"),
    EnvironmentTokenSpec("medkit", "کیت پزشکی", "خطرات و افکت‌ها", R.drawable.env_medkit, 1, 1, "effects"),
    EnvironmentTokenSpec("table", "میز", "مبلمان", R.drawable.env_table, 2, 2, "environment"),
    EnvironmentTokenSpec("chair", "صندلی", "مبلمان", R.drawable.env_chair, 1, 1, "environment"),
    EnvironmentTokenSpec("couch", "مبل", "مبلمان", R.drawable.env_couch, 1, 2, "environment"),
    EnvironmentTokenSpec("bed", "تخت", "مبلمان", R.drawable.env_bed, 1, 2, "environment"),
    EnvironmentTokenSpec("locker", "کمد", "مبلمان", R.drawable.env_locker, 1, 1, "environment"),
    EnvironmentTokenSpec("shelf", "قفسه", "مبلمان", R.drawable.env_shelf, 1, 2, "environment"),
    EnvironmentTokenSpec("stairs", "پله", "مبلمان", R.drawable.env_stairs, 2, 2, "environment"),
    EnvironmentTokenSpec("elevator", "آسانسور", "مبلمان", R.drawable.env_elevator, 2, 2, "environment"),
    EnvironmentTokenSpec("plant", "گیاه", "مبلمان", R.drawable.env_plant, 1, 1, "environment"),
    EnvironmentTokenSpec("vending_machine", "دستگاه فروش", "مبلمان", R.drawable.env_vending_machine, 1, 1, "environment"),
    EnvironmentTokenSpec("notice_board", "تابلوی اعلانات", "مبلمان", R.drawable.env_notice_board, 1, 2, "environment")
)

private val environmentCategories = environmentTokenSpecs.map { it.category }.distinct()

private fun environmentSpec(id: String?): EnvironmentTokenSpec? = environmentTokenSpecs.firstOrNull { it.id == id }
private fun tokenColorName(argb: Long): String = when (argb) {
    0xFF2196F3L -> "آبی"; 0xFFE53935L -> "قرمز"; 0xFF43A047L -> "سبز"; 0xFFFFB300L -> "زرد"
    0xFF8E24AAL -> "بنفش"; 0xFF00ACC1L -> "فیروزه‌ای"; 0xFFFB8C00L -> "نارنجی"; 0xFF7CB342L -> "سبز روشن"
    0xFF5E35B1L -> "نیلی"; 0xFFD81B60L -> "صورتی"; 0xFF00897BL -> "سبزآبی"; 0xFF6D4C41L -> "قهوه‌ای"
    else -> "خاکستری"
}

@Composable
fun BattleMapScreen(
    viewModel: CharacterViewModel,
    fullscreen: Boolean = false,
    onFullscreenChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { BattleMapStore(context) }
    var maps by remember { mutableStateOf(store.load()) }
    var active by remember { mutableStateOf(maps.firstOrNull { it.id == store.lastActiveMapId() } ?: maps.firstOrNull()) }
    var mode by remember { mutableStateOf(BattleMapMode.PAN) }
    var selectedCharacterId by remember { mutableStateOf<Int?>(null) }
    var selectedEnvironmentType by remember { mutableStateOf<String?>(null) }
    var selectedMapTokenId by remember { mutableStateOf<String?>(null) }
    // توکنی که پیش‌نمایش بردِ حرکتش روشن است (با نگه‌داشتن روی توکن).
    var moveRangeTokenId by remember { mutableStateOf<String?>(null) }
    var editingEnvironmentTokenId by remember { mutableStateOf<String?>(null) }
    var measureA by remember { mutableStateOf<Offset?>(null) }
    var measureB by remember { mutableStateOf<Offset?>(null) }
    var showScaleDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(true) }
    var mapSearch by remember { mutableStateOf("") }
    var mapTierFilter by remember { mutableIntStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    var scaleText by remember { mutableStateOf("2") }
    var gridSizeText by remember { mutableStateOf("60") }
    var gridOriginXText by remember { mutableStateOf("0") }
    var gridOriginYText by remember { mutableStateOf("0") }
    var gridColorArgb by remember { mutableLongStateOf(0xFFFFFFFFL) }
    var resetViewGeneration by remember { mutableIntStateOf(0) }
    var tokenUndoStack by remember { mutableStateOf<List<TokenMoveUndo>>(emptyList()) }
    var message by remember { mutableStateOf("نقشه فقط برای جای‌گذاری توکن، حرکت و اندازه‌گیری فاصله است.") }
    val roster by viewModel.characters.collectAsState()
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(fullscreen, activity, active?.id) {
        if (fullscreen && activity != null) {
            val previous = activity.requestedOrientation
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            val current = active
            activity.requestedOrientation = if (current != null && current.imageHeight > current.imageWidth)
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = previous
            }
        } else onDispose { }
    }

    BackHandler(enabled = fullscreen) { onFullscreenChanged(false); showDrawer = false }

    fun persist(updated: BattleMapDefinition) {
        store.upsert(updated)
        active = updated
        store.setLastActiveMapId(updated.id)
        maps = store.load()
    }

    fun characterColor(characterId: Int, map: BattleMapDefinition): Long {
        map.tokens.firstOrNull { it.characterId == characterId }?.let { return it.colorArgb }
        val used = map.tokens.filterNot { it.isEnvironment }.map { it.colorArgb }.toSet()
        return tokenPalette.firstOrNull { it !in used } ?: tokenPalette[(characterId.hashCode() and Int.MAX_VALUE) % tokenPalette.size]
    }

    fun undoLastTokenMove() {
        val m = active ?: return
        val index = tokenUndoStack.indexOfLast { it.mapId == m.id }
        if (index < 0) { message = "حرکت قبلی برای برگشت وجود ندارد."; return }
        val entry = tokenUndoStack[index]
        persist(m.copy(tokens = m.tokens.map { if (it.id == entry.token.id) entry.token else it }))
        tokenUndoStack = tokenUndoStack.filterIndexed { i, _ -> i != index }
        message = "حرکت ${entry.token.label} برگشت داده شد."
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val imported = store.importImage(uri, "Imported Battle Map")
            maps = store.load(); active = imported; store.setLastActiveMapId(imported.id)
            mode = BattleMapMode.PAN
            scaleText = imported.calibration.metersPerCell.toString()
            gridSizeText = imported.calibration.pixelsPerCell.roundToInt().toString()
            resetViewGeneration++
            message = "تصویر با کیفیت اصلی وارد شد و Grid برنامه روی آن قرار گرفت؛ از تنظیمات اندازه Grid و متر هر خانه را عوض کن."
        }.onFailure { message = "خطا در Import: ${it.message ?: "تصویر نامعتبر"}" }
    }

    Box(Modifier.fillMaxSize().background(BmBg)) {
        Column(Modifier.fillMaxSize()) {
            if (!fullscreen) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(gtr("BATTLE MAP"), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showMapPicker = true }) { Icon(Icons.Default.Map, gtr("Maps"), tint = Color.LightGray) }
                    IconButton(onClick = { importLauncher.launch(arrayOf("image/*")) }) { Icon(Icons.Default.AddPhotoAlternate, gtr("Import map"), tint = BmRed) }
                    IconButton(onClick = { onFullscreenChanged(true) }) { Icon(Icons.Default.Fullscreen, gtr("Fullscreen"), tint = Color.White) }
                }
            }

            active?.let { map ->
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    // پرترهٔ هر شخصیتِ روی نقشه: بازیکن‌ها از روی نقش و NPCها از
                    // روی دسته‌بندی‌شان انتخاب می‌شوند — همان منطقی که بقیه‌ی برنامه دارد.
                    val tokenPortraits = remember(roster, map.tokens) {
                        map.tokens.mapNotNull { tk ->
                            val cid = tk.characterId ?: return@mapNotNull null
                            val ch = roster.firstOrNull { it.id == cid } ?: return@mapNotNull null
                            val res = if (ch.isAlly) {
                                com.cyberpunk.gmtool.data.ImageProvider.getRoleImage(ch.role, true)
                            } else {
                                com.cyberpunk.gmtool.data.ImageProvider.getNpcImage(ch.npcCategory, ch.role, ch.name)
                            }
                            cid to res
                        }.toMap()
                    }
                    // خانه‌هایی که شخصیتِ انتخاب‌شده می‌تواند با یک Move Action برود.
                    // قاعده‌ی Core: MOVE × ۲ متر. متر هر خانه از تنظیمات همین نقشه
                    // خوانده می‌شود، پس اگر GM اندازه‌ی خانه را عوض کند، برد هم عوض می‌شود.
                    val moveRangeCells: Set<Pair<Int, Int>> = remember(moveRangeTokenId, map, roster) {
                        val tk = map.tokens.firstOrNull { it.id == moveRangeTokenId }
                        val cid = tk?.characterId
                        val ch = cid?.let { id -> roster.firstOrNull { it.id == id } }
                        val cal = map.calibration
                        if (tk == null || ch == null || cal.pixelsPerCell <= 0f) emptySet()
                        else {
                            val meters = com.cyberpunk.gmtool.data.CombatRules.effectiveMove(ch) * 2
                            val perCell = cal.metersPerCell.coerceAtLeast(.01f)
                            // تعداد خانه‌ها؛ رو به پایین تا بیشتر از حق حرکت نشان ندهد.
                            val reach = kotlin.math.floor(meters / perCell).toInt()
                            if (reach <= 0) emptySet() else {
                                val (col, row) = tk.gridColumn to tk.gridRow
                                if (col == null || row == null) emptySet() else {
                                    // مرزهای واقعی نقشه، تا هاله بیرون از تصویر نزند.
                                    val cell = cal.pixelsPerCell
                                    val maxCol = kotlin.math.ceil((map.imageWidth - cal.originX) / cell).toInt() - 1
                                    val maxRow = kotlin.math.ceil((map.imageHeight - cal.originY) / cell).toInt() - 1
                                    val minCol = kotlin.math.floor(-cal.originX / cell).toInt()
                                    val minRow = kotlin.math.floor(-cal.originY / cell).toInt()
                                    buildSet {
                                        for (c in (col - reach)..(col + reach)) {
                                            for (r in (row - reach)..(row + reach)) {
                                                if (c == col && r == row) continue
                                                if (c < minCol || c > maxCol || r < minRow || r > maxRow) continue
                                                // همان معیار فاصله‌ای که خود نقشه استفاده می‌کند
                                                val d = kotlin.math.max(
                                                    kotlin.math.abs(c - col),
                                                    kotlin.math.abs(r - row)
                                                )
                                                if (d <= reach) add(c to r)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    BattleMapCanvas(
                        modifier = Modifier.fillMaxSize(), map = map, mode = mode,
                        measureA = measureA, measureB = measureB,
                        selectedTokenId = selectedMapTokenId,
                        resetViewGeneration = resetViewGeneration,
                        portraits = tokenPortraits,
                        moveRange = moveRangeCells,
                        onTokenTapped = { tokenId ->
                            val token = map.tokens.firstOrNull { it.id == tokenId }
                            val nowSelected = selectedMapTokenId != tokenId
                            selectedMapTokenId = if (nowSelected) tokenId else null
                            // یک کلیک روی توکن شخصیت = نمایش بردِ حرکت.
                            // عمداً از long-press استفاده نمی‌کنیم؛ روی صفحه‌های ضعیف نامطمئن است.
                            moveRangeTokenId =
                                if (nowSelected && token?.characterId != null) tokenId else null
                            val ch = token?.characterId?.let { id -> roster.firstOrNull { it.id == id } }
                            message = when {
                                token == null -> message
                                token.isEnvironment && token.locked ->
                                    "توکن ${token.label} قفل است؛ برای ویرایش نگه‌دار."
                                ch != null && nowSelected -> {
                                    val mv = com.cyberpunk.gmtool.data.CombatRules.effectiveMove(ch)
                                    "${ch.handle.ifBlank { ch.name }} • برد حرکت ${mv * 2} متر — نقطهٔ مقصد را لمس کن."
                                }
                                nowSelected -> "توکن ${token.label} انتخاب شد؛ نقطهٔ مقصد را لمس کن."
                                else -> "انتخاب لغو شد."
                            }
                        },
                        onTokenLongPressed = { tokenId ->
                            // فقط برای توکن‌های محیطی؛ توکن شخصیت با یک کلیک ساده کار می‌کند.
                            val token = map.tokens.firstOrNull { it.id == tokenId }
                            if (token?.isEnvironment == true) {
                                selectedMapTokenId = tokenId
                                editingEnvironmentTokenId = tokenId
                            }
                        },
                        onMapTap = { imagePoint ->
                            when (mode) {
                                BattleMapMode.PAN -> {
                                    val tokenId = selectedMapTokenId
                                    val old = map.tokens.firstOrNull { it.id == tokenId }
                                    if (tokenId != null && old != null) {
                                        if (old.isEnvironment && old.locked) {
                                            selectedMapTokenId = null
                                            message = "${old.label} قفل است؛ برای Unlock روی آن نگه‌دار."
                                        } else {
                                            tokenUndoStack = (tokenUndoStack + TokenMoveUndo(map.id, old)).takeLast(20)
                                            val moved = BattleMapStore.snapToken(map, old, imagePoint.x, imagePoint.y)
                                            if (old.isEnvironment && !environmentTokenFits(map, old, moved)) {
                                                message = "این مقصد برای اندازهٔ ${old.label} مناسب نیست."
                                            } else {
                                                val oldPoint = tokenPoint(map, old); val newPoint = tokenPoint(map, moved)
                                                val meters = map.calibration.distanceMeters(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y)
                                                persist(map.copy(tokens = map.tokens.map { if (it.id == tokenId) moved else it }))
                                                selectedMapTokenId = null
                                                message = "${old.label}: ${"%.1f".format(meters)} متر حرکت"
                                            }
                                        }
                                    }
                                }
                                BattleMapMode.MEASURE -> if (measureA == null || measureB != null) { measureA = imagePoint; measureB = null } else measureB = imagePoint
                                BattleMapMode.PLACE_TOKEN -> {
                                    val c = roster.firstOrNull { it.id == selectedCharacterId }
                                    if (c != null) {
                                        val base = BattleMapToken(characterId = c.id, label = c.name.ifBlank { c.handle.ifBlank { "#${c.id}" } }, x = imagePoint.x, y = imagePoint.y, colorArgb = characterColor(c.id, map))
                                        val token = BattleMapStore.snapToken(map, base, imagePoint.x, imagePoint.y)
                                        persist(map.copy(tokens = map.tokens.filterNot { it.characterId == c.id && !it.isEnvironment } + token))
                                        mode = BattleMapMode.PAN
                                        selectedMapTokenId = token.id
                                        message = "توکن ${token.label} وسط خانه قرار گرفت."
                                    }
                                }
                                BattleMapMode.PLACE_ENV_TOKEN -> {
                                    val spec = environmentSpec(selectedEnvironmentType)
                                    if (spec != null) {
                                        val base = BattleMapToken(
                                            label = spec.label,
                                            x = imagePoint.x,
                                            y = imagePoint.y,
                                            environmentType = spec.id,
                                            gridWidth = spec.defaultWidth,
                                            gridHeight = spec.defaultHeight,
                                            rotationDegrees = 0f,
                                            locked = false,
                                            tokenLayer = spec.layer
                                        )
                                        val token = BattleMapStore.snapToken(map, base, imagePoint.x, imagePoint.y)
                                        if (!environmentTokenFits(map, base, token)) {
                                            message = "این نقطه برای اندازهٔ پیش‌فرض ${spec.label} مناسب نیست."
                                        } else {
                                            persist(map.copy(tokens = map.tokens + token))
                                            mode = BattleMapMode.PAN
                                            selectedMapTokenId = token.id
                                            message = "${spec.label} روی تقاطع خطوط شبکه قرار گرفت."
                                        }
                                    }
                                }
                            }
                        }
                    )

                    if (fullscreen) {
                        val fsDistance = if (measureA != null && measureB != null) map.calibration.distanceMeters(measureA!!.x, measureA!!.y, measureB!!.x, measureB!!.y) else null
                        val fsCells = if (measureA != null && measureB != null) map.calibration.distanceCells(measureA!!.x, measureA!!.y, measureB!!.x, measureB!!.y) else null
                        val placing = mode == BattleMapMode.PLACE_TOKEN || mode == BattleMapMode.PLACE_ENV_TOKEN
                        Box(Modifier.align(Alignment.TopStart).padding(10.dp)) { SmallMapFab(Icons.Default.FullscreenExit, "خروج") { onFullscreenChanged(false); showDrawer = false } }
                        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { SmallMapFab(if (showDrawer) Icons.Default.ChevronRight else Icons.Default.ChevronLeft, "منو") { showDrawer = !showDrawer } }
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                            color = Color(0xE61A1A1A), shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    fsDistance != null -> Color(0xFF7EE787)
                                    placing -> BmRed
                                    else -> Color.White.copy(alpha = .18f)
                                }
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when {
                                        fsDistance != null && fsCells != null -> "فاصله  ${"%.1f".format(fsDistance)} متر  •  ${"%.1f".format(fsCells)} خانه"
                                        mode == BattleMapMode.MEASURE && measureA != null -> "نقطهٔ دوم فاصله را انتخاب کن"
                                        mode == BattleMapMode.PLACE_TOKEN -> "حالا روی نقشه بزن تا توکن آنجا بنشیند"
                                        mode == BattleMapMode.PLACE_ENV_TOKEN -> "نقطه را لمس کن؛ توکن GM روی تقاطع شبکه می‌نشیند"
                                        selectedMapTokenId != null -> "توکن انتخاب شد؛ نقطهٔ مقصد را لمس کن"
                                        else -> "هر خانه ${map.calibration.metersPerCell} متر"
                                    },
                                    color = when {
                                        fsDistance != null -> Color(0xFF7EE787)
                                        placing -> BmRed
                                        else -> Color.White
                                    },
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                                // راه خروج، اگر GM پشیمان شد
                                if (placing) {
                                    Text(
                                        "لغو",
                                        color = Color.LightGray, fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                mode = BattleMapMode.PAN
                                                selectedCharacterId = null
                                                selectedEnvironmentType = null
                                                message = "افزودن لغو شد."
                                            }
                                            .padding(end = 14.dp, start = 2.dp, top = 8.dp, bottom = 8.dp)
                                    )
                                }
                            }
                        }
                        Row(Modifier.align(Alignment.BottomCenter).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallMapFab(Icons.Default.OpenWith, "حرکت", mode == BattleMapMode.PAN) { mode = BattleMapMode.PAN }
                            SmallMapFab(Icons.Default.Straighten, "فاصله", mode == BattleMapMode.MEASURE) { mode = BattleMapMode.MEASURE; measureA = null; measureB = null }
                            SmallMapFab(Icons.Default.Undo, "برگشت") { undoLastTokenMove() }
                            SmallMapFab(Icons.Default.CenterFocusStrong, "جا دادن") { resetViewGeneration++ }
                        }
                    }
                }

                if (!fullscreen) {
                    val distance = if (measureA != null && measureB != null) map.calibration.distanceMeters(measureA!!.x, measureA!!.y, measureB!!.x, measureB!!.y) else null
                    val cells = if (measureA != null && measureB != null) map.calibration.distanceCells(measureA!!.x, measureA!!.y, measureB!!.x, measureB!!.y) else null
                    Surface(color = BmCard, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                            Text(map.name + "  •  " + map.district, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                            Text(if (distance != null && cells != null) "فاصله: ${"%.1f".format(distance)} متر  •  ${"%.1f".format(cells)} خانه" else "هر خانه: ${map.calibration.metersPerCell} متر", color = if (distance != null) Color(0xFF7EE787) else Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            BattleTool(Icons.Default.OpenWith, "حرکت", mode == BattleMapMode.PAN, Modifier.weight(1f)) { mode = BattleMapMode.PAN }
                            BattleTool(Icons.Default.Straighten, "فاصله", mode == BattleMapMode.MEASURE, Modifier.weight(1f)) { mode = BattleMapMode.MEASURE; measureA = null; measureB = null }
                            BattleTool(Icons.Default.Tune, "شبکه", false, Modifier.weight(1f)) {
                                gridSizeText = map.calibration.pixelsPerCell.roundToInt().toString()
                                gridOriginXText = map.calibration.originX.roundToInt().toString()
                                gridOriginYText = map.calibration.originY.roundToInt().toString()
                                gridColorArgb = map.calibration.gridColorArgb
                                showGridDialog = true
                            }
                            BattleTool(Icons.Default.Straighten, "مقیاس", false, Modifier.weight(1f)) { scaleText = map.calibration.metersPerCell.toString(); showScaleDialog = true }
                            BattleTool(Icons.Default.Groups, "توکن‌ها", false, Modifier.weight(1f)) { showDrawer = true }
                        }
                    }
                    Text(gtr(message), color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp))
                }
            } ?: Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Button(onClick = { importLauncher.launch(arrayOf("image/*")) }) { Text(gtr("Import Battle Map")) } }
        }

        if (showDrawer) active?.let { drawerMap ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)).clickable { showDrawer = false })
            SimpleBattleMapDrawer(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(if (fullscreen) .62f else .92f),
                map = drawerMap, roster = roster, selectedTokenId = selectedMapTokenId,
                onClose = { showDrawer = false },
                onMaps = { showMapPicker = true },
                onImport = { importLauncher.launch(arrayOf("image/*")) },
                onScale = { scaleText = drawerMap.calibration.metersPerCell.toString(); showScaleDialog = true },
                onGrid = {
                    gridSizeText = drawerMap.calibration.pixelsPerCell.roundToInt().toString()
                    gridOriginXText = drawerMap.calibration.originX.roundToInt().toString()
                    gridOriginYText = drawerMap.calibration.originY.roundToInt().toString()
                    gridColorArgb = drawerMap.calibration.gridColorArgb
                    showGridDialog = true
                },
                onAddCharacter = { cId ->
                    selectedCharacterId = cId
                    selectedEnvironmentType = null
                    mode = BattleMapMode.PLACE_TOKEN
                    showDrawer = false
                    message = "خانهٔ مقصد را لمس کن؛ توکن در مرکز خانه قرار می‌گیرد."
                },
                onAddEnvironment = { type ->
                    selectedEnvironmentType = type
                    selectedCharacterId = null
                    mode = BattleMapMode.PLACE_ENV_TOKEN
                    showDrawer = false
                    message = "نقطهٔ نزدیک را لمس کن؛ توکن محیطی روی تقاطع خطوط شبکه می‌نشیند."
                },
                onRemove = { tokenId ->
                    persist(drawerMap.copy(tokens = drawerMap.tokens.filterNot { it.id == tokenId }))
                    if (selectedMapTokenId == tokenId) selectedMapTokenId = null
                },
                onRemoveLastEnvironment = { type ->
                    val m = drawerMap
                    val last = m.tokens.lastOrNull { it.environmentType == type }
                    if (last != null) persist(m.copy(tokens = m.tokens.filterNot { it.id == last.id }))
                },
                onChangeColor = { tokenId ->
                    val m = drawerMap; m.tokens.firstOrNull { it.id == tokenId }?.let { t ->
                        val i = tokenPalette.indexOf(t.colorArgb).takeIf { it >= 0 } ?: 0; val next = tokenPalette[(i + 1) % tokenPalette.size]
                        persist(m.copy(tokens = m.tokens.map { if (it.id == tokenId) it.copy(colorArgb = next) else it }))
                    }
                },
                onEditEnvironment = { tokenId ->
                    editingEnvironmentTokenId = tokenId
                    selectedMapTokenId = tokenId
                    showDrawer = false
                }
            )
        }
    }

    val mapForDialog = active
    if (showMapPicker) {
        BattleMapLibraryDialog(
            maps = maps,
            activeMapId = active?.id,
            search = mapSearch,
            tierFilter = mapTierFilter,
            onSearchChange = { mapSearch = it },
            onTierChange = { mapTierFilter = it },
            onDismiss = { showMapPicker = false },
            onImport = { showMapPicker = false; importLauncher.launch(arrayOf("image/*")) },
            onSelect = { m ->
                active = m
                selectedMapTokenId = null
                store.setLastActiveMapId(m.id)
                showMapPicker = false
                resetViewGeneration++
            },
            onDelete = { m ->
                store.delete(m)
                maps = store.load()
                if (active?.id == m.id) active = maps.firstOrNull()
            }
        )
    }

    if (showScaleDialog && mapForDialog != null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showScaleDialog = false },
                title = { Text("مقیاس فاصله", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                text = {
                    Column {
                        Text("این عدد مشخص می‌کند هر خانه چند متر است؛ اندازهٔ خود شبکه عوض نمی‌شود.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(scaleText, { scaleText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6) }, label = { Text("متر در هر خانه") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("1", "2", "4", "5", "10", "15").forEach { v -> AssistChip(onClick = { scaleText = v }, label = { Text(gtr(v)) }) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { scaleText.toFloatOrNull()?.takeIf { it > 0f }?.let { persist(BattleMapStore.updateScale(mapForDialog, it)); showScaleDialog = false; message = "هر خانه = $it متر" } }) { Text("اعمال") } },
                dismissButton = { TextButton(onClick = { showScaleDialog = false }) { Text("لغو") } }
            )
        }
    }

    if (showGridDialog && mapForDialog != null) {
        var opacity by remember(mapForDialog.id, mapForDialog.calibration.gridOpacity) { mutableFloatStateOf(mapForDialog.calibration.gridOpacity) }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showGridDialog = false },
                title = { Text("تنظیم شبکه", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                text = {
                    Column {
                        Text("شبکه متعلق به خود برنامه است. اندازهٔ خانه‌ها را با تصویر هماهنگ کن؛ تصویر می‌تواند شبکهٔ داخلی داشته باشد یا نداشته باشد.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(gridSizeText, { gridSizeText = it.filter(Char::isDigit).take(4) }, label = { Text("اندازهٔ خانه روی تصویر (px)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(gridOriginXText, { gridOriginXText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(6) }, label = { Text("جابجایی افقی") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(gridOriginYText, { gridOriginYText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(6) }, label = { Text("جابجایی عمودی") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("شفافیت", fontSize = 11.sp); Slider(opacity, { opacity = it }, valueRange = .08f..0.85f, modifier = Modifier.weight(1f)) }
                        Spacer(Modifier.height(6.dp))
                        Text("رنگ خطوط شبکه", fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                            val presets = listOf(
                                "سفید" to 0xFFFFFFFFL,
                                "مشکی" to 0xFF000000L,
                                "قرمز" to 0xFFD32F2FL,
                                "فیروزه‌ای" to 0xFF00E5FFL,
                                "زرد" to 0xFFFFD740L,
                                "بنفش" to 0xFFB388FFL
                            )
                            items(presets) { (label, argb) ->
                                val selected = gridColorArgb == argb
                                Surface(
                                    modifier = Modifier.size(width = 58.dp, height = 42.dp).clickable { gridColorArgb = argb },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(argb),
                                    border = androidx.compose.foundation.BorderStroke(if (selected) 3.dp else 1.dp, if (selected) BmRed else Color.Gray)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(gtr(label), color = if (argb == 0xFFFFFFFFL || argb == 0xFFFFD740L) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("نمایش شبکه", modifier = Modifier.weight(1f)); Switch(mapForDialog.calibration.gridVisible, { persist(mapForDialog.copy(calibration = mapForDialog.calibration.copy(gridVisible = it))) }) }
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val px = gridSizeText.toFloatOrNull()?.coerceIn(12f, 500f) ?: mapForDialog.calibration.pixelsPerCell
                    val ox = gridOriginXText.toFloatOrNull() ?: mapForDialog.calibration.originX
                    val oy = gridOriginYText.toFloatOrNull() ?: mapForDialog.calibration.originY
                    persist(BattleMapStore.updateGridGeometry(mapForDialog, px, opacity, ox, oy, gridColorArgb)); showGridDialog = false; message = "شبکه تنظیم شد."
                }) { Text("اعمال") } },
                dismissButton = { TextButton(onClick = { showGridDialog = false }) { Text("لغو") } }
            )
        }
    }

    val editingToken = active?.tokens?.firstOrNull { it.id == editingEnvironmentTokenId && it.isEnvironment }
    if (editingToken != null) {
        var editWidth by remember(editingToken.id) { mutableIntStateOf(editingToken.gridWidth.coerceAtLeast(1)) }
        var editHeight by remember(editingToken.id) { mutableIntStateOf(editingToken.gridHeight.coerceAtLeast(1)) }
        var editRotation by remember(editingToken.id) { mutableFloatStateOf(((editingToken.rotationDegrees % 360f) + 360f) % 360f) }
        var editLocked by remember(editingToken.id) { mutableStateOf(editingToken.locked) }
        var editLayer by remember(editingToken.id) { mutableStateOf(if (editingToken.tokenLayer == "effects") "effects" else "environment") }
        val spec = environmentSpec(editingToken.environmentType)

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { editingEnvironmentTokenId = null },
                title = {
                    Text(
                        "ویرایش ${editingToken.label}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right,
                        fontWeight = FontWeight.Black
                    )
                },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (spec != null) {
                                    Image(
                                        painter = painterResource(spec.drawableRes),
                                        contentDescription = spec.label,
                                        modifier = Modifier.size(76.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("نقطهٔ Anchor روی Grid ثابت می‌ماند.", color = Color.LightGray, fontSize = 11.sp)
                                    Text("حرکت فقط با لمس توکن و سپس لمس مقصد انجام می‌شود.", color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                        }
                        item {
                            Text("اندازه بر اساس فاصلهٔ نقاط Grid", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = Color.White.copy(alpha = .04f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(onClick = { editWidth = (editWidth - 1).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, null) }
                                        Text("عرض  $editWidth", fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { editWidth = (editWidth + 1).coerceAtMost(12) }) { Icon(Icons.Default.Add, null) }
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = Color.White.copy(alpha = .04f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(onClick = { editHeight = (editHeight - 1).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, null) }
                                        Text("طول  $editHeight", fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { editHeight = (editHeight + 1).coerceAtMost(12) }) { Icon(Icons.Default.Add, null) }
                                    }
                                }
                            }
                        }
                        item {
                            Text("چرخش: ${editRotation.roundToInt()}°", fontWeight = FontWeight.Bold)
                            Slider(
                                value = editRotation,
                                onValueChange = { editRotation = it },
                                valueRange = 0f..359f,
                                steps = 358
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { editRotation = ((editRotation - 15f) + 360f) % 360f },
                                    modifier = Modifier.weight(1f)
                                ) { Text("−15°") }
                                OutlinedButton(
                                    onClick = { editRotation = (editRotation + 15f) % 360f },
                                    modifier = Modifier.weight(1f)
                                ) { Text("+15°") }
                                OutlinedButton(
                                    onClick = { editRotation = 0f },
                                    modifier = Modifier.weight(1f)
                                ) { Text("0°") }
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (editLocked) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = if (editLocked) Color(0xFFFFB300) else Color.LightGray)
                                Spacer(Modifier.width(8.dp))
                                Text("قفل روی نقشه", modifier = Modifier.weight(1f))
                                Switch(checked = editLocked, onCheckedChange = { editLocked = it })
                            }
                            Text(
                                if (editLocked) "توکن قفل‌شده با Tap جابه‌جا نمی‌شود؛ Long Press برای ویرایش همچنان فعال است."
                                else "توکن باز است و با Tap → Tap روی Grid جابه‌جا می‌شود.",
                                color = Color.Gray, fontSize = 9.sp
                            )
                        }
                        item {
                            Text("لایه", fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = editLayer == "environment",
                                    onClick = { editLayer = "environment" },
                                    label = { Text("محیط") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = editLayer == "effects",
                                    onClick = { editLayer = "effects" },
                                    label = { Text("افکت / روی افراد") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val m = active
                                        if (m != null) {
                                        val duplicate = editingToken.copy(
                                            id = java.util.UUID.randomUUID().toString(),
                                            gridColumn = editingToken.gridColumn?.plus(1),
                                            gridRow = editingToken.gridRow?.plus(1),
                                            x = editingToken.x + m.calibration.pixelsPerCell,
                                            y = editingToken.y + m.calibration.pixelsPerCell,
                                            gridWidth = editWidth,
                                            gridHeight = editHeight,
                                            rotationDegrees = editRotation,
                                            locked = false,
                                            tokenLayer = editLayer
                                        )
                                        persist(m.copy(tokens = m.tokens + duplicate))
                                        selectedMapTokenId = duplicate.id
                                        editingEnvironmentTokenId = duplicate.id
                                        message = "یک کپی از ${editingToken.label} ساخته شد."
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("کپی")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val m = active
                                        if (m != null) {
                                        persist(m.copy(tokens = m.tokens.filterNot { it.id == editingToken.id }))
                                        selectedMapTokenId = null
                                        editingEnvironmentTokenId = null
                                        message = "${editingToken.label} حذف شد."
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BmRed)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("حذف")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val m = active
                        if (m != null) {
                        persist(
                            m.copy(
                                tokens = m.tokens.map {
                                    if (it.id == editingToken.id) it.copy(
                                        gridWidth = editWidth.coerceIn(1, 12),
                                        gridHeight = editHeight.coerceIn(1, 12),
                                        rotationDegrees = editRotation,
                                        locked = editLocked,
                                        tokenLayer = editLayer
                                    ) else it
                                }
                            )
                        )
                        editingEnvironmentTokenId = null
                        message = "تنظیمات ${editingToken.label} ذخیره شد."
                        }
                    }) { Text("ذخیره") }
                },
                dismissButton = {
                    TextButton(onClick = { editingEnvironmentTokenId = null }) { Text("لغو") }
                }
            )
        }
    }

}


private fun battleMapTierSubtitle(tier: Int): String = when (tier) {
    1 -> "مجموعه T1"
    2 -> "مجموعه T2"
    3 -> "مجموعه T3"
    4 -> "مجموعه T4"
    5 -> "مجموعه T5"
    else -> "نقشه‌های سفارشی"
}

@Composable
private fun BattleMapLibraryDialog(
    maps: List<BattleMapDefinition>,
    activeMapId: String?,
    search: String,
    tierFilter: Int,
    onSearchChange: (String) -> Unit,
    onTierChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onSelect: (BattleMapDefinition) -> Unit,
    onDelete: (BattleMapDefinition) -> Unit
) {
    val q = search.trim().lowercase()
    val visibleMaps = remember(maps, q, tierFilter) {
        maps.filter { m ->
            val tierOk = tierFilter == 0 || m.tier == tierFilter
            val haystack = (m.name + " " + m.district + " " + (m.pageName ?: "") + " " + (m.mapSetId ?: "") + " " + m.tags.joinToString(" ")).lowercase()
            tierOk && (q.isBlank() || haystack.contains(q))
        }.sortedWith(compareBy<BattleMapDefinition> { it.tier }.thenBy { it.mapNumber }.thenBy { it.pageOrder })
    }
    val tiers = remember(maps) { maps.map { it.tier }.filter { it > 0 }.distinct().sorted() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Color(0xFF08121A)).padding(8.dp)
        ) {
            val wide = maxWidth >= 760.dp
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0A151E),
                shape = RoundedCornerShape(if (wide) 24.dp else 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BmCyan.copy(alpha = .22f))
            ) {
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        BattleMapTierRail(
                            tiers = tiers,
                            selected = tierFilter,
                            maps = maps,
                            onSelect = onTierChange,
                            modifier = Modifier.width(190.dp).fillMaxHeight()
                        )
                        VerticalDivider(color = Color.White.copy(alpha = .08f))
                        BattleMapLibraryContent(
                            visibleMaps = visibleMaps,
                            activeMapId = activeMapId,
                            search = search,
                            tierFilter = tierFilter,
                            onSearchChange = onSearchChange,
                            onTierChange = onTierChange,
                            tiers = tiers,
                            showTierChips = false,
                            onDismiss = onDismiss,
                            onImport = onImport,
                            onSelect = onSelect,
                            onDelete = onDelete,
                            columns = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    BattleMapLibraryContent(
                        visibleMaps = visibleMaps,
                        activeMapId = activeMapId,
                        search = search,
                        tierFilter = tierFilter,
                        onSearchChange = onSearchChange,
                        onTierChange = onTierChange,
                        tiers = tiers,
                        showTierChips = true,
                        onDismiss = onDismiss,
                        onImport = onImport,
                        onSelect = onSelect,
                        onDelete = onDelete,
                        columns = 1,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun BattleMapTierRail(
    tiers: List<Int>,
    selected: Int,
    maps: List<BattleMapDefinition>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.background(Color(0xFF09131C)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Map, null, tint = BmCyan, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(gtr("BATTLE MAPS"), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(gtr("Cyberpunk RED"), color = Color(0xFF91A7BA), fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        TierRailItem("همه نقشه‌ها", "${maps.size} نقشه", selected == 0) { onSelect(0) }
        Spacer(Modifier.height(6.dp))
        tiers.forEach { tier ->
            val count = maps.count { it.tier == tier }
            TierRailItem("T$tier", "${battleMapTierSubtitle(tier)} • $count", selected == tier) { onSelect(tier) }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(gtr("Plan. Play. Survive."), color = BmCyan.copy(alpha = .65f), fontSize = 9.sp)
    }
}

@Composable
private fun TierRailItem(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) BmCyan.copy(alpha = .12f) else Color.Transparent,
        shape = RoundedCornerShape(13.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, BmCyan.copy(alpha = .72f)) else null
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(gtr(title), color = if (selected) BmCyan else Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(gtr(subtitle), color = Color(0xFF8297A9), fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun BattleMapLibraryContent(
    visibleMaps: List<BattleMapDefinition>,
    activeMapId: String?,
    search: String,
    tierFilter: Int,
    onSearchChange: (String) -> Unit,
    onTierChange: (Int) -> Unit,
    tiers: List<Int>,
    showTierChips: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onSelect: (BattleMapDefinition) -> Unit,
    onDelete: (BattleMapDefinition) -> Unit,
    columns: Int,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "بستن", tint = Color.White) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (tierFilter == 0) "کتابخانه نقشه‌های نبرد" else "T$tierFilter — ${battleMapTierSubtitle(tierFilter)}",
                        color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                    )
                    Text("${visibleMaps.size} نقشه • نام، مکان و تگ‌ها قابل جست‌وجو هستند", color = Color(0xFF8FA5B8), fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onImport, colors = ButtonDefaults.filledTonalButtonColors(containerColor = BmCyan.copy(alpha = .13f))) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = BmCyan, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(gtr("Import"), color = Color.White, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = BmCyan) },
                trailingIcon = if (search.isNotBlank()) {{ IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } }} else null,
                label = { Text("جستجوی نقشه، مکان یا تگ") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmCyan, focusedLabelColor = BmCyan)
            )
            if (showTierChips) {
                LazyRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = tierFilter == 0, onClick = { onTierChange(0) }, label = { Text("همه") }) }
                    items(tiers) { tier -> FilterChip(selected = tierFilter == tier, onClick = { onTierChange(tier) }, label = { Text("T$tier") }) }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (visibleMaps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("نقشه‌ای پیدا نشد", color = Color.LightGray)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridItems(visibleMaps, key = { it.id }) { map ->
                        BattleMapLibraryCard(map, map.id == activeMapId, onSelect, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleMapLibraryCard(
    map: BattleMapDefinition,
    active: Boolean,
    onSelect: (BattleMapDefinition) -> Unit,
    onDelete: (BattleMapDefinition) -> Unit
) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? = remember(map.imagePath, map.updatedAt) {
        runCatching {
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bmp = if (map.imagePath.startsWith("res:")) {
                val name = map.imagePath.removePrefix("res:")
                battleMapDrawableResId(name)?.let { BitmapFactory.decodeResource(context.resources, it, options) }
            } else BitmapFactory.decodeFile(java.io.File(context.filesDir, map.imagePath).absolutePath, options)
            bmp?.asImageBitmap()
        }.getOrNull()
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(map) },
        shape = RoundedCornerShape(16.dp),
        color = BmLibraryCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) BmCyan.copy(alpha = .95f) else Color(0xFF294050))
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.55f).background(Color(0xFF071018))) {
                if (bitmap != null) Image(bitmap = bitmap, contentDescription = map.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Default.Map, null, tint = Color.DarkGray, modifier = Modifier.align(Alignment.Center).size(48.dp))
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    color = Color(0xE611242A), shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BmCyan.copy(alpha = .55f))
                ) {
                    // گزارش تست ۲.۳: نقشه‌های یک مجموعه‌ی چندصفحه‌ای همگی بدرقه‌ی یکسان
                    // «T2-5» داشتند و صفحه‌ها از هم تشخیص‌پذیر نبودند. مثل کتاب، حرف صفحه
                    // اضافه می‌شود: pageOrder=1 → «T2-5a». همه‌ی مجموعه‌های رسمی برنامه
                    // چندصفحه‌اند، پس به‌محض وجود mapSetId حرف می‌آید.
                    val pageLetter = if (map.mapSetId != null) ('a' + (map.pageOrder.coerceAtLeast(1) - 1).coerceAtMost(25)).toString() else ""
                    Text(if (map.tier > 0) "T${map.tier}-${map.mapNumber}$pageLetter" else "CUSTOM", color = BmCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                if (active) Surface(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), color = BmCyan, shape = CircleShape) { Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.padding(4.dp).size(14.dp)) }
            }
            Column(Modifier.padding(10.dp)) {
                Text(map.name.substringAfter("—").trim(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                map.pageName?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color(0xFFA7BED0), fontSize = 9.sp, maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    map.tags.filter { it.any { ch -> ch.code > 127 } }.distinct().take(4).forEach { tag ->
                        Surface(color = Color(0xFF172A38), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF29495B))) {
                            Text(gtr(tag), color = Color(0xFFC4D6E2), fontSize = 8.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), maxLines = 1)
                        }
                    }
                }
                if (map.imagePath.startsWith("battle_maps/")) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { onDelete(map) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.DeleteOutline, "حذف", tint = Color.Gray, modifier = Modifier.size(17.dp)) }
                    }
                }
            }
        }
    }
}

private fun environmentTokenFits(map: BattleMapDefinition, token: BattleMapToken, snapped: BattleMapToken): Boolean {
    if (!token.isEnvironment || !map.calibration.calibrated || map.calibration.pixelsPerCell <= 0f) return true
    val center = tokenPoint(map, snapped)
    val cell = map.calibration.pixelsPerCell
    val halfW = cell * token.gridWidth.coerceAtLeast(1) / 2f
    val halfH = cell * token.gridHeight.coerceAtLeast(1) / 2f
    val rad = Math.toRadians(token.rotationDegrees.toDouble())
    val halfX = (kotlin.math.abs(kotlin.math.cos(rad)) * halfW + kotlin.math.abs(kotlin.math.sin(rad)) * halfH).toFloat()
    val halfY = (kotlin.math.abs(kotlin.math.sin(rad)) * halfW + kotlin.math.abs(kotlin.math.cos(rad)) * halfH).toFloat()
    return center.x - halfX >= 0f &&
        center.y - halfY >= 0f &&
        center.x + halfX <= map.imageWidth.toFloat() &&
        center.y + halfY <= map.imageHeight.toFloat()
}

private fun tokenPoint(map: BattleMapDefinition, token: BattleMapToken): Offset {
    val c = map.calibration
    if (c.calibrated && token.gridColumn != null && token.gridRow != null) {
        val (x, y) = if (token.isEnvironment) c.pointOfIntersection(token.gridColumn, token.gridRow)
        else c.centerOfCell(token.gridColumn, token.gridRow)
        return Offset(x, y)
    }
    return Offset(token.x, token.y)
}

@Composable
private fun SimpleBattleMapDrawer(
    modifier: Modifier,
    map: BattleMapDefinition,
    roster: List<com.cyberpunk.gmtool.data.Character>,
    selectedTokenId: String?,
    onClose: () -> Unit,
    onMaps: () -> Unit,
    onImport: () -> Unit,
    onScale: () -> Unit,
    onGrid: () -> Unit,
    onAddCharacter: (Int) -> Unit,
    onAddEnvironment: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRemoveLastEnvironment: (String) -> Unit,
    onChangeColor: (String) -> Unit,
    onEditEnvironment: (String) -> Unit
) {
    var section by remember(map.id) { mutableIntStateOf(0) }
    // ۰ = توکن‌ها (پیش‌فرض، چون بیشترین استفاده را دارد) ، ۱ = تنظیمات نقشه
    var topTab by remember(map.id) { mutableIntStateOf(0) }
    var environmentCategory by remember(map.id) { mutableStateOf(environmentCategories.firstOrNull().orEmpty()) }
    val selectedToken = map.tokens.firstOrNull { it.id == selectedTokenId }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier,
            color = Color(0xF71A1A1A),
            tonalElevation = 10.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .10f))
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = BmRed.copy(alpha = .18f)) {
                        Icon(Icons.Default.Groups, null, tint = BmRed, modifier = Modifier.padding(9.dp).size(21.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ابزار نقشه", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        Text("لمس توکن: انتخاب و نمایش برد حرکت • لمس دوباره: لغو", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "بستن", tint = Color.White) }
                }

                Spacer(Modifier.height(10.dp))
                // دو زبانه‌ی اصلی: تنظیمات نقشه از توکن‌ها جدا شد.
                // قبلاً چهار دکمه‌ی تنظیمات همیشه بالای پنل بودند و در حالت افقی
                // تقریباً کل ارتفاع را می‌خوردند؛ فهرست توکن‌ها ته صفحه له می‌شد.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrawerTopTab("توکن‌ها", Icons.Default.Groups, topTab == 0, Modifier.weight(1f)) { topTab = 0 }
                    DrawerTopTab("تنظیمات نقشه", Icons.Default.Tune, topTab == 1, Modifier.weight(1f)) { topTab = 1 }
                }
                Spacer(Modifier.height(10.dp))

                if (topTab == 1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrawerQuickAction(Icons.Default.Map, "نقشه‌ها", onMaps, Modifier.weight(1f))
                        DrawerQuickAction(Icons.Default.AddPhotoAlternate, "ورود تصویر", onImport, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrawerQuickAction(Icons.Default.Tune, "شبکه", onGrid, Modifier.weight(1f))
                        DrawerQuickAction(Icons.Default.Straighten, "${map.calibration.metersPerCell} متر", onScale, Modifier.weight(1f))
                    }
                    Spacer(Modifier.weight(1f))
                }

                if (topTab == 0) {
                if (selectedToken != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = BmRed.copy(alpha = .10f),
                        shape = RoundedCornerShape(13.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BmRed.copy(alpha = .35f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (selectedToken.isEnvironment) {
                                val spec = environmentSpec(selectedToken.environmentType)
                                if (spec != null) {
                                    Image(
                                        painter = painterResource(spec.drawableRes),
                                        contentDescription = spec.label,
                                        modifier = Modifier.size(42.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            } else {
                                Surface(Modifier.size(28.dp), shape = CircleShape, color = Color(selectedToken.colorArgb)) {}
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(gtr(selectedToken.label), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                                Text(
                                    if (selectedToken.isEnvironment)
                                        "${selectedToken.gridWidth.coerceAtLeast(1)}×${selectedToken.gridHeight.coerceAtLeast(1)} نقطه • ${selectedToken.rotationDegrees.roundToInt()}°${if (selectedToken.locked) " • قفل" else ""}"
                                    else "توکن کاراکتر • مرکز خانه",
                                    color = Color.Gray, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                                )
                            }
                            if (selectedToken.isEnvironment) {
                                IconButton(onClick = { onEditEnvironment(selectedToken.id) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Edit, "ویرایش", tint = Color.LightGray)
                                }
                            } else {
                                IconButton(onClick = { onChangeColor(selectedToken.id) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Palette, "رنگ", tint = Color.LightGray)
                                }
                            }
                            IconButton(onClick = { onRemove(selectedToken.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.DeleteOutline, "حذف", tint = BmRed)
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = .10f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrawerSectionButton("افراد", section == 0, Modifier.weight(1f)) { section = 0 }
                    DrawerSectionButton("محیط", section == 1, Modifier.weight(1f)) { section = 1 }
                }
                Spacer(Modifier.height(8.dp))

                if (section == 0) {
                    // ردیف‌ها عمداً بلندند و کل ردیف کلیک‌پذیر است؛ دکمه‌ی کوچک
                    // کنار اسکرول روی صفحه‌ی لمسی به‌سختی گرفته می‌شد.
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(roster, key = { it.id }) { c ->
                            val token = map.tokens.firstOrNull { it.characterId == c.id && !it.isEnvironment }
                            val onMap = token != null
                            Surface(
                                color = if (onMap) BmRed.copy(alpha = .10f) else Color.White.copy(alpha = .045f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (onMap) BmRed.copy(alpha = .45f) else Color.White.copy(alpha = .09f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                                    // کل ردیف = افزودن. برای حذف، دکمه‌ی سمت چپ.
                                    .clickable(enabled = !onMap) { onAddCharacter(c.id) }
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        Modifier.size(40.dp).clickable(enabled = onMap) { token?.let { onChangeColor(it.id) } },
                                        shape = CircleShape,
                                        color = Color(token?.colorArgb ?: 0xFF4A4A4AL),
                                        border = androidx.compose.foundation.BorderStroke(
                                            2.dp,
                                            if (onMap) Color.White.copy(alpha = .85f) else Color.White.copy(alpha = .22f)
                                        )
                                    ) {}
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            c.name.ifBlank { c.handle.ifBlank { "#${c.id}" } },
                                            color = Color.White, fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                                            textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            if (onMap) "روی نقشه • ${tokenColorName(token!!.colorArgb)}"
                                            else "برای گذاشتن روی نقشه بزن",
                                            color = if (onMap) BmCyan.copy(alpha = .85f) else Color.Gray,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (onMap) {
                                        IconButton(
                                            onClick = { onRemove(token!!.id) },
                                            modifier = Modifier.size(44.dp)
                                        ) { Icon(Icons.Default.DeleteOutline, "حذف", tint = Color.LightGray) }
                                    } else {
                                        // هدف لمسی بزرگ و واضح به‌جای آیکون ریز
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = BmRed,
                                            modifier = Modifier.size(44.dp)
                                                .clickable { onAddCharacter(c.id) }
                                        ) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Add, "افزودن", tint = Color.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(environmentCategories) { category ->
                            FilterChip(
                                selected = environmentCategory == category,
                                onClick = { environmentCategory = category },
                                label = { Text(gtr(category), fontSize = 12.sp) },
                                modifier = Modifier.height(40.dp)
                            )
                        }
                    }
                    Text(
                        "توکن‌ها روی تقاطع Grid می‌نشینند. بعد از افزودن: لمس → مقصد؛ نگه‌داشتن → اندازه، چرخش، قفل و کپی.",
                        color = Color.Gray, fontSize = 11.sp, lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Right
                    )
                    val specs = environmentTokenSpecs.filter { it.category == environmentCategory }
                    // گزارش تست ۲.۲: با کارت‌های افقیِ جمع‌وجور، ستون‌ها می‌توانند با
                    // عرض پنل تطبیق شوند (Adaptive) به‌جای دو ستون ثابت.
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 235.dp),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        gridItems(specs, key = { it.id }) { spec ->
                            val count = map.tokens.count { it.environmentType == spec.id }
                            Surface(
                                color = Color.White.copy(alpha = .045f),
                                shape = RoundedCornerShape(13.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .07f))
                            ) {
                                // گزارش تست ۲.۲: کارت عمودی با تصویر ۷۶dp تمام‌عرض خیلی بلند
                                // بود و فهرست توکن‌ها بی‌پایان به نظر می‌رسید. حالا افقی و
                                // کوتاه است: تصویر مربع کوچک + متن + دکمه‌های −/+ در یک ردیف.
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(spec.drawableRes),
                                        contentDescription = spec.label,
                                        modifier = Modifier.size(40.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(gtr(spec.label), color = Color.White, fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("${spec.defaultWidth}×${spec.defaultHeight}" + if (count > 0) " • روی نقشه: $count" else "",
                                            color = if (count > 0) BmCyan.copy(alpha = .85f) else Color.Gray, fontSize = 10.sp, maxLines = 1)
                                    }
                                    IconButton(
                                        onClick = { if (count > 0) onRemoveLastEnvironment(spec.id) },
                                        enabled = count > 0,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, "حذف آخرین", tint = if (count > 0) Color.LightGray else Color.DarkGray, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onAddEnvironment(spec.id) }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Add, "افزودن", tint = BmRed, modifier = Modifier.size(19.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                } // topTab == 0
            }
        }
    }
}

/** زبانه‌ی بالای پنل نقشه: توکن‌ها / تنظیمات. بزرگ و با هدف لمسی راحت. */
@Composable
private fun DrawerTopTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(46.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) BmRed else Color.White.copy(alpha = .05f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if (selected) BmRed else Color.White.copy(alpha = .10f)
        )
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                label,
                color = if (selected) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DrawerSectionButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) BmRed.copy(alpha = .22f) else Color.White.copy(alpha = .045f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) BmRed.copy(alpha = .7f) else Color.White.copy(alpha = .08f))
    ) {
        Text(gtr(label), color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun DrawerQuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = Color.White.copy(alpha = .055f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .10f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = BmRed, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(gtr(label), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SmallMapFab(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean=false, onClick:()->Unit){ Surface(shape=CircleShape,color=if(selected)BmRed else Color(0xDD191919),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=.25f))){IconButton(onClick=onClick,modifier=Modifier.size(42.dp)){Icon(icon,label,tint=Color.White,modifier=Modifier.size(21.dp))}} }

@Composable
private fun BattleTool(icon: androidx.compose.ui.graphics.vector.ImageVector,label:String,selected:Boolean,modifier:Modifier,onClick:()->Unit){ OutlinedButton(onClick=onClick,modifier=modifier.height(46.dp),contentPadding=PaddingValues(horizontal=2.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(selected)BmRed.copy(alpha=.2f) else Color.Transparent)){Icon(icon,null,tint=if(selected)BmRed else Color.LightGray,modifier=Modifier.size(15.dp));Spacer(Modifier.width(2.dp));Text(gtr(label),fontSize=9.sp,color=Color.White)} }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BattleMapCanvas(
    modifier: Modifier = Modifier,
    map: BattleMapDefinition,
    mode: BattleMapMode,
    measureA: Offset?,
    measureB: Offset?,
    selectedTokenId: String?,
    resetViewGeneration: Int,
    /** شناسه‌ی شخصیت → منبع پرترهٔ او، تا توکن به‌جای دایره چهره نشان دهد. */
    portraits: Map<Int, Int> = emptyMap(),
    /** خانه‌های قابل حرکت (ستون به سطر) برای پیش‌نمایش برد حرکت. */
    moveRange: Set<Pair<Int, Int>> = emptySet(),
    onMapTap: (Offset) -> Unit,
    onTokenTapped: (String) -> Unit,
    onTokenLongPressed: (String) -> Unit
) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? = remember(map.imagePath, map.updatedAt) {
        runCatching {
            val bmp = if (map.imagePath.startsWith("res:")) {
                val name = map.imagePath.removePrefix("res:")
                battleMapDrawableResId(name)?.let { BitmapFactory.decodeResource(context.resources, it) }
            } else {
                BitmapFactory.decodeFile(java.io.File(context.filesDir, map.imagePath).absolutePath)
            }
            bmp?.asImageBitmap()
        }.getOrNull()
    }
    val environmentBitmaps = remember {
        environmentTokenSpecs.mapNotNull { spec ->
            runCatching { BitmapFactory.decodeResource(context.resources, spec.drawableRes)?.asImageBitmap() }
                .getOrNull()?.let { spec.id to it }
        }.toMap()
    }

    // پرتره‌ها یک بار رمزگشایی و کوچک می‌شوند. تصاویر نقش‌ها بزرگ‌اند و رمزگشایی
    // آن‌ها در هر فریمِ رسم، نقشه را کند می‌کرد.
    val portraitBitmaps: Map<Int, ImageBitmap> = remember(portraits) {
        portraits.mapNotNull { (charId, resId) ->
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeResource(context.resources, resId, opts)?.asImageBitmap()
            }.getOrNull()?.let { charId to it }
        }.toMap()
    }

    var zoom by remember(map.id) { mutableFloatStateOf(1f) }
    var pan by remember(map.id) { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(resetViewGeneration, map.id) {
        zoom = 1f
        pan = Offset.Zero
    }

    fun geometry(): Pair<Float, Offset> {
        val iw = map.imageWidth.coerceAtLeast(1).toFloat()
        val ih = map.imageHeight.coerceAtLeast(1).toFloat()
        val base = min(canvasSize.width / iw, canvasSize.height / ih)
        val scale = base * zoom
        return scale to Offset(
            (canvasSize.width - iw * scale) / 2f + pan.x,
            (canvasSize.height - ih * scale) / 2f + pan.y
        )
    }

    fun screenToImage(p: Offset): Offset {
        val (scale, off) = geometry()
        return Offset(
            ((p.x - off.x) / scale).coerceIn(0f, map.imageWidth.toFloat()),
            ((p.y - off.y) / scale).coerceIn(0f, map.imageHeight.toFloat())
        )
    }

    fun hitToken(screen: Offset): BattleMapToken? {
        val (scale, off) = geometry()
        val cell = (map.calibration.pixelsPerCell.takeIf { it > 0f } ?: 40f) * scale
        return map.tokens
            .sortedBy { if (it.isEnvironment && it.tokenLayer == "effects") 3 else if (!it.isEnvironment) 2 else 1 }
            .lastOrNull { t ->
                val point = tokenPoint(map, t)
                val center = Offset(off.x + point.x * scale, off.y + point.y * scale)
                if (t.isEnvironment) {
                    // گزارش تست ۲.۱: آزمون قدیمی دایره‌ای به شعاع max(w,h)*0.58 بود؛ یک درِ
                    // ۱×۵ عملاً چند خانه اطرافش را هم می‌گرفت و لمس‌ها به جای نقشه روی توکن
                    // می‌افتادند. حالا دقیقاً همان مستطیلی آزمون می‌شود که توکن در آن رسم
                    // می‌شود (gridWidth × gridHeight، چرخیده حول مرکزش): نقطه‌ی لمس را با
                    // وارونِ همان چرخش به دستگاه توکن می‌بریم و آزمون مستطیل ساده می‌کنیم.
                    val halfW = cell * t.gridWidth.coerceAtLeast(1) / 2f
                    val halfH = cell * t.gridHeight.coerceAtLeast(1) / 2f
                    val rad = Math.toRadians(-(t.rotationDegrees % 360f).toDouble())
                    val cos = kotlin.math.cos(rad).toFloat()
                    val sin = kotlin.math.sin(rad).toFloat()
                    val dx = screen.x - center.x
                    val dy = screen.y - center.y
                    val lx = dx * cos - dy * sin
                    val ly = dx * sin + dy * cos
                    val pad = 8f // تلورانس انگشت: لبه‌های توکن راحت‌تر گرفته شوند
                    kotlin.math.abs(lx) <= halfW + pad && kotlin.math.abs(ly) <= halfH + pad
                } else {
                    val radius = (cell * .46f).coerceIn(24f, 54f)
                    (center - screen).getDistance() <= radius
                }
            }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .background(Color.Black)
            .pointerInput(map.id, mode, map.tokens) {
                if (mode == BattleMapMode.PAN) {
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        val iw = map.imageWidth.coerceAtLeast(1).toFloat()
                        val ih = map.imageHeight.coerceAtLeast(1).toFloat()
                        val base = min(canvasSize.width / iw, canvasSize.height / ih)
                        if (base <= 0f) return@detectTransformGestures
                        val oldScale = base * zoom
                        val oldOff = Offset(
                            (canvasSize.width - iw * oldScale) / 2f + pan.x,
                            (canvasSize.height - ih * oldScale) / 2f + pan.y
                        )
                        val imageUnder = Offset(
                            (centroid.x - oldOff.x) / oldScale,
                            (centroid.y - oldOff.y) / oldScale
                        )
                        val newZoom = (zoom * zoomChange).coerceIn(1f, 12f)
                        val newScale = base * newZoom
                        val baseOff = Offset(
                            (canvasSize.width - iw * newScale) / 2f,
                            (canvasSize.height - ih * newScale) / 2f
                        )
                        val desired = Offset(
                            centroid.x - imageUnder.x * newScale,
                            centroid.y - imageUnder.y * newScale
                        )
                        zoom = newZoom
                        pan = (desired - baseOff) + panChange
                    }
                }
            }
            .pointerInput(map.id, mode, map.tokens, selectedTokenId) {
                detectTapGestures(
                    onLongPress = { screen ->
                        if (mode == BattleMapMode.PAN) {
                            hitToken(screen)?.let { onTokenLongPressed(it.id) }
                        }
                    },
                    onTap = { screen ->
                        when (mode) {
                            BattleMapMode.PAN -> {
                                val hit = hitToken(screen)
                                if (hit != null) onTokenTapped(hit.id)
                                else onMapTap(screenToImage(screen))
                            }
                            else -> onMapTap(screenToImage(screen))
                        }
                    }
                )
            }
    ) {
        canvasSize = size
        val iw = map.imageWidth.coerceAtLeast(1).toFloat()
        val ih = map.imageHeight.coerceAtLeast(1).toFloat()
        val base = min(size.width / iw, size.height / ih)
        val scale = base * zoom
        val off = Offset(
            (size.width - iw * scale) / 2f + pan.x,
            (size.height - ih * scale) / 2f + pan.y
        )

        bitmap?.let {
            drawImage(
                it,
                dstOffset = androidx.compose.ui.unit.IntOffset(off.x.roundToInt(), off.y.roundToInt()),
                dstSize = androidx.compose.ui.unit.IntSize((iw * scale).roundToInt(), (ih * scale).roundToInt())
            )
        }

        val cal = map.calibration
        if (cal.gridVisible && cal.pixelsPerCell > 0f) {
            val cell = cal.pixelsPerCell * scale
            if (cell >= 6f) {
                val ox = off.x + cal.originX * scale
                val oy = off.y + cal.originY * scale
                var x = ox + floor((off.x - ox) / cell) * cell
                while (x <= off.x + iw * scale) {
                    drawLine(
                        Color(cal.gridColorArgb).copy(alpha = cal.gridOpacity.coerceIn(.05f, .85f)),
                        Offset(x, off.y),
                        Offset(x, off.y + ih * scale),
                        1f
                    )
                    x += cell
                }
                var y = oy + floor((off.y - oy) / cell) * cell
                while (y <= off.y + ih * scale) {
                    drawLine(
                        Color(cal.gridColorArgb).copy(alpha = cal.gridOpacity.coerceIn(.05f, .85f)),
                        Offset(off.x, y),
                        Offset(off.x + iw * scale, y),
                        1f
                    )
                    y += cell
                }
            }
        }

        fun p(i: Offset): Offset = Offset(off.x + i.x * scale, off.y + i.y * scale)

        if (measureA != null) {
            drawCircle(Color(0xFF4FC3F7), 7f, p(measureA))
            if (measureB != null) {
                drawLine(Color(0xFF4FC3F7), p(measureA), p(measureB), 4f)
                drawCircle(Color(0xFF4FC3F7), 7f, p(measureB))
            }
        }

        val selectedEnvironment = map.tokens.firstOrNull { it.id == selectedTokenId && it.isEnvironment && !it.locked }
        if (selectedEnvironment != null && cal.calibrated && cal.pixelsPerCell > 0f) {
            val cellPx = cal.pixelsPerCell
            val minCol = kotlin.math.floor((0f - cal.originX) / cellPx).toInt()
            val maxCol = kotlin.math.ceil((map.imageWidth - cal.originX) / cellPx).toInt()
            val minRow = kotlin.math.floor((0f - cal.originY) / cellPx).toInt()
            val maxRow = kotlin.math.ceil((map.imageHeight - cal.originY) / cellPx).toInt()
            for (col in minCol..maxCol) {
                for (row in minRow..maxRow) {
                    val (ix, iy) = cal.pointOfIntersection(col, row)
                    val candidate = selectedEnvironment.copy(x = ix, y = iy, gridColumn = col, gridRow = row)
                    if (environmentTokenFits(map, selectedEnvironment, candidate)) {
                        val point = p(Offset(ix, iy))
                        drawCircle(BmCyan.copy(alpha = .28f), 2.8f, point)
                    }
                }
            }
        }

        // ── هاله‌ی برد حرکت ──
        // قبل از توکن‌ها رسم می‌شود تا زیرشان بماند و نقشه هم از زیرش پیدا باشد.
        if (moveRange.isNotEmpty() && cal.pixelsPerCell > 0f) {
            val cellPx = cal.pixelsPerCell * scale
            moveRange.forEach { (c, r) ->
                val (cx, cy) = cal.centerOfCell(c, r)
                val p0 = p(Offset(cx, cy))
                val half = cellPx / 2f
                drawRect(
                    BmCyan.copy(alpha = .16f),
                    topLeft = Offset(p0.x - half, p0.y - half),
                    size = Size(cellPx, cellPx)
                )
                drawRect(
                    BmCyan.copy(alpha = .45f),
                    topLeft = Offset(p0.x - half, p0.y - half),
                    size = Size(cellPx, cellPx),
                    style = Stroke(1.5f)
                )
            }
        }

        val orderedTokens = map.tokens.sortedBy {
            when {
                it.isEnvironment && it.tokenLayer == "effects" -> 3
                !it.isEnvironment -> 2
                else -> 1
            }
        }

        orderedTokens.forEach { t ->
            val center = p(tokenPoint(map, t))
            val baseCell = cal.pixelsPerCell.takeIf { it > 0f }?.times(scale) ?: 40f

            if (t.isEnvironment) {
                val w = baseCell * t.gridWidth.coerceAtLeast(1)
                val h = baseCell * t.gridHeight.coerceAtLeast(1)
                val image = environmentBitmaps[t.environmentType]
                if (t.id == selectedTokenId) {
                    drawRect(
                        if (t.locked) Color(0xFFFFB300).copy(alpha = .35f) else Color.White.copy(alpha = .25f),
                        topLeft = Offset(center.x - w / 2f - 4f, center.y - h / 2f - 4f),
                        size = Size(w + 8f, h + 8f),
                        style = Stroke(3f)
                    )
                }
                if (image != null) {
                    withTransform({
                        rotate(t.rotationDegrees, pivot = center)
                    }) {
                        drawImage(
                            image,
                            dstOffset = androidx.compose.ui.unit.IntOffset(
                                (center.x - w / 2f).roundToInt(),
                                (center.y - h / 2f).roundToInt()
                            ),
                            dstSize = androidx.compose.ui.unit.IntSize(
                                w.coerceAtLeast(4f).roundToInt(),
                                h.coerceAtLeast(4f).roundToInt()
                            )
                        )
                    }
                } else {
                    drawRect(
                        Color(0xCC151515),
                        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                        size = Size(w, h)
                    )
                    drawRect(
                        Color(0xFFE53935),
                        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                        size = Size(w, h),
                        style = Stroke(2.5f)
                    )
                }
            } else {
                // توکن شخصیت.
                // پرتره مربع است و دقیقاً اندازه‌ی یک خانه‌ی شبکه؛ چون هر دو ضلع
                // از baseCell می‌آیند، با بزرگ/کوچک شدن شبکه هم عرض و هم ارتفاع
                // با هم تغییر می‌کنند و نسبت تصویر بهم نمی‌ریزد.
                val portrait = t.characterId?.let { portraitBitmaps[it] }
                val selected = t.id == selectedTokenId

                if (portrait != null) {
                    val side = baseCell            // یک خانه‌ی کامل، مربع
                    val half = side / 2f
                    val topLeft = Offset(center.x - half, center.y - half)
                    val dst = Rect(topLeft, Size(side, side))

                    if (selected) {
                        drawRect(
                            Color.White.copy(alpha = .22f),
                            topLeft = Offset(topLeft.x - 5f, topLeft.y - 5f),
                            size = Size(side + 10f, side + 10f)
                        )
                    }
                    // پس‌زمینه‌ی تیره، اگر تصویر شفافیت داشت روی نقشه گم نشود.
                    drawRect(Color(0xE6101010), topLeft = topLeft, size = Size(side, side))

                    // تصویر را «وسط‌برش» می‌کنیم: ضلع کوتاه‌تر منبع را کامل می‌گیریم
                    // تا پرتره کشیده نشود و خانه هم کامل پر شود.
                    val srcSide = kotlin.math.min(portrait.width, portrait.height)
                    val srcX = (portrait.width - srcSide) / 2
                    val srcY = (portrait.height - srcSide) / 2
                    clipRect(dst.left, dst.top, dst.right, dst.bottom) {
                        drawImage(
                            portrait,
                            srcOffset = androidx.compose.ui.unit.IntOffset(srcX, srcY),
                            srcSize = androidx.compose.ui.unit.IntSize(srcSide, srcSide),
                            dstOffset = androidx.compose.ui.unit.IntOffset(
                                topLeft.x.roundToInt(), topLeft.y.roundToInt()
                            ),
                            dstSize = androidx.compose.ui.unit.IntSize(
                                side.roundToInt().coerceAtLeast(4),
                                side.roundToInt().coerceAtLeast(4)
                            )
                        )
                    }
                    // قاب رنگی: تنها راه تشخیص دو نفر با نقش یکسان.
                    drawRect(
                        Color(t.colorArgb),
                        topLeft = topLeft,
                        size = Size(side, side),
                        style = Stroke(if (selected) 5f else 3.5f)
                    )
                } else {
                    val radius = (baseCell * .34f).coerceIn(14f, 38f)
                    if (selected) {
                        drawCircle(Color.White.copy(alpha = .20f), radius + 8f, center)
                    }
                    drawCircle(Color(0xE6101010), radius, center)
                    drawCircle(
                        Color(t.colorArgb),
                        radius,
                        center,
                        style = Stroke(if (selected) 5f else 3.5f)
                    )
                    drawCircle(Color.White.copy(alpha = .9f), 2.5f, center)
                }
            }
        }
    }
}
