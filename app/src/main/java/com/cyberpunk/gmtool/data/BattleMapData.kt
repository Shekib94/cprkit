package com.cyberpunk.gmtool.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.cyberpunk.gmtool.R
import com.google.gson.Gson
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/**
 * Device-independent battle-map geometry.
 * Grid geometry is stored in SOURCE IMAGE pixels, never screen pixels.
 * Physical scale (metersPerCell) is deliberately independent from grid geometry, so the GM can
 * change 2m -> 4m -> 15m per cell without moving the overlay or tokens.
 */
data class BattleMapCalibration(
    val metersPerCell: Float = 2f,
    /** New canonical grid size in source-image pixels. */
    val cellSizePixels: Float = 0f,
    /** Pass-10 compatibility. Used only when cellSizePixels is absent. */
    val pixelsPerMeter: Float = 0f,
    val originX: Float = 0f,
    val originY: Float = 0f,
    val calibrated: Boolean = false,
    val gridVisible: Boolean = true,
    val gridOpacity: Float = 0.30f,
    /** Stable ARGB grid colour. White remains the migration/default colour for old saves. */
    val gridColorArgb: Long = 0xFFFFFFFFL
) {
    val pixelsPerCell: Float
        get() = when {
            cellSizePixels > 0f -> cellSizePixels
            pixelsPerMeter > 0f -> pixelsPerMeter * metersPerCell.coerceAtLeast(.01f)
            else -> 0f
        }

    fun distanceCells(ax: Float, ay: Float, bx: Float, by: Float): Float =
        if (pixelsPerCell > 0f) max(abs(bx - ax), abs(by - ay)) / pixelsPerCell else 0f

    fun distanceMeters(ax: Float, ay: Float, bx: Float, by: Float): Float =
        distanceCells(ax, ay, bx, by) * metersPerCell

    fun cellForPoint(x: Float, y: Float): Pair<Int, Int> {
        val cell = pixelsPerCell.coerceAtLeast(1f)
        return floor((x - originX) / cell).toInt() to floor((y - originY) / cell).toInt()
    }

    fun centerOfCell(column: Int, row: Int): Pair<Float, Float> {
        val cell = pixelsPerCell.coerceAtLeast(1f)
        return (originX + (column + .5f) * cell) to (originY + (row + .5f) * cell)
    }

    /** Nearest grid-line intersection. Environment tokens live on vertices, not cell centers. */
    fun intersectionForPoint(x: Float, y: Float): Pair<Int, Int> {
        val cell = pixelsPerCell.coerceAtLeast(1f)
        return kotlin.math.round((x - originX) / cell).toInt() to kotlin.math.round((y - originY) / cell).toInt()
    }

    fun pointOfIntersection(column: Int, row: Int): Pair<Float, Float> {
        val cell = pixelsPerCell.coerceAtLeast(1f)
        return (originX + column * cell) to (originY + row * cell)
    }
}

data class BattleMapToken(
    val id: String = UUID.randomUUID().toString(),
    val characterId: Int? = null,
    val label: String = "Token",
    /** Legacy/free image coordinates, retained for uncalibrated maps and migration. */
    val x: Float = 0f,
    val y: Float = 0f,
    /** Canonical position for calibrated maps: cell coordinates. */
    val gridColumn: Int? = null,
    val gridRow: Int? = null,
    /** Stable ARGB token colour saved with the map. */
    val colorArgb: Long = 0xFF2196F3L,
    /** null = Player/NPC token. Non-null = reusable GM environment token id. */
    val environmentType: String? = null,
    /** Environment-token footprint in grid intervals. Old saves migrate to 1x1. */
    val gridWidth: Int = 1,
    val gridHeight: Int = 1,
    /** Rotation around the snapped grid intersection. */
    val rotationDegrees: Float = 0f,
    /** Locked environment tokens cannot be moved with tap-to-move until explicitly unlocked. */
    val locked: Boolean = false,
    /** Draw-order bucket. map < environment < characters < effects. */
    val tokenLayer: String = "characters"
) {
    val isEnvironment: Boolean get() = !environmentType.isNullOrBlank()
}

data class BattleMapDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Battle Map",
    val district: String = "Custom",
    val tier: Int = 0,
    val mapNumber: Int = 0,
    val tags: List<String> = emptyList(),
    val mapSetId: String? = null,
    val pageName: String? = null,
    val pageOrder: Int = 0,
    val imagePath: String = "",
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val calibration: BattleMapCalibration = BattleMapCalibration(),
    val tokens: List<BattleMapToken> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


/** Static resource lookup so release shrinking cannot remove built-in battle maps. */
fun battleMapDrawableResId(resourceName: String): Int? = when (resourceName) {
    "battlemap_t1_10_parking_garage" -> R.drawable.battlemap_t1_10_parking_garage
    "battlemap_t1_11_scav_den_deep_sewer" -> R.drawable.battlemap_t1_11_scav_den_deep_sewer
    "battlemap_t1_11_scav_den_main" -> R.drawable.battlemap_t1_11_scav_den_main
    "battlemap_t1_11_scav_den_sewer" -> R.drawable.battlemap_t1_11_scav_den_sewer
    "battlemap_t1_1_abandoned_garage" -> R.drawable.battlemap_t1_1_abandoned_garage
    "battlemap_t1_2_warehouse_yard" -> R.drawable.battlemap_t1_2_warehouse_yard
    "battlemap_t1_4_industrial_repair_complex" -> R.drawable.battlemap_t1_4_industrial_repair_complex
    "battlemap_t1_5_back_alley" -> R.drawable.battlemap_t1_5_back_alley
    "battlemap_t1_6_megabuilding_apartment" -> R.drawable.battlemap_t1_6_megabuilding_apartment
    "battlemap_t1_7_club" -> R.drawable.battlemap_t1_7_club
    "battlemap_t1_8_night_market" -> R.drawable.battlemap_t1_8_night_market
    "battlemap_t1_9_ripperdoc_clinic" -> R.drawable.battlemap_t1_9_ripperdoc_clinic
    "battlemap_t2_10_casino_floor1" -> R.drawable.battlemap_t2_10_casino_floor1
    "battlemap_t2_10_casino_vip" -> R.drawable.battlemap_t2_10_casino_vip
    "battlemap_t2_1_trauma_clinic" -> R.drawable.battlemap_t2_1_trauma_clinic
    "battlemap_t2_2_metro_station" -> R.drawable.battlemap_t2_2_metro_station
    "battlemap_t2_3_fight_club_basement" -> R.drawable.battlemap_t2_3_fight_club_basement
    "battlemap_t2_3_fight_club_ground" -> R.drawable.battlemap_t2_3_fight_club_ground
    "battlemap_t2_4_abandoned_factory" -> R.drawable.battlemap_t2_4_abandoned_factory
    "battlemap_t2_7_cargo_dock" -> R.drawable.battlemap_t2_7_cargo_dock
    "battlemap_t2_7_smuggling_warehouse_ground" -> R.drawable.battlemap_t2_7_smuggling_warehouse_ground
    "battlemap_t2_7_smuggling_warehouse_upper" -> R.drawable.battlemap_t2_7_smuggling_warehouse_upper
    "battlemap_t2_8_ncpd_floor1" -> R.drawable.battlemap_t2_8_ncpd_floor1
    "battlemap_t2_8_ncpd_floor2" -> R.drawable.battlemap_t2_8_ncpd_floor2
    "battlemap_t2_9_radio_station" -> R.drawable.battlemap_t2_9_radio_station
    "battlemap_t3_1_office_executive" -> R.drawable.battlemap_t3_1_office_executive
    "battlemap_t3_1_office_openplan" -> R.drawable.battlemap_t3_1_office_openplan
    "battlemap_t3_1_office_lobby" -> R.drawable.battlemap_t3_1_office_lobby
    "battlemap_t3_1_office_parking" -> R.drawable.battlemap_t3_1_office_parking
    "battlemap_t3_2_lab_admin" -> R.drawable.battlemap_t3_2_lab_admin
    "battlemap_t3_2_lab_main" -> R.drawable.battlemap_t3_2_lab_main
    "battlemap_t3_2_lab_sublevel" -> R.drawable.battlemap_t3_2_lab_sublevel
    "battlemap_t3_3_data_center_ops" -> R.drawable.battlemap_t3_3_data_center_ops
    "battlemap_t3_3_data_center_halls" -> R.drawable.battlemap_t3_3_data_center_halls
    "battlemap_t3_3_data_center_plant" -> R.drawable.battlemap_t3_3_data_center_plant
    "battlemap_t3_4_hotel_lobby" -> R.drawable.battlemap_t3_4_hotel_lobby
    "battlemap_t3_4_hotel_suites" -> R.drawable.battlemap_t3_4_hotel_suites
    "battlemap_t3_4_hotel_rooftop" -> R.drawable.battlemap_t3_4_hotel_rooftop
    "battlemap_t3_5_clinic_front" -> R.drawable.battlemap_t3_5_clinic_front
    "battlemap_t3_5_clinic_surgical" -> R.drawable.battlemap_t3_5_clinic_surgical
    "battlemap_t3_5_clinic_organfarm" -> R.drawable.battlemap_t3_5_clinic_organfarm
    "battlemap_t3_6a_plaza_ground" -> R.drawable.battlemap_t3_6a_plaza_ground
    "battlemap_t3_6b_plaza_gallery" -> R.drawable.battlemap_t3_6b_plaza_gallery
    "battlemap_t3_6c_plaza_parking" -> R.drawable.battlemap_t3_6c_plaza_parking
    "battlemap_t3_7a_studio_offices" -> R.drawable.battlemap_t3_7a_studio_offices
    "battlemap_t3_7b_studio_soundstages" -> R.drawable.battlemap_t3_7b_studio_soundstages
    "battlemap_t3_7c_studio_broadcast" -> R.drawable.battlemap_t3_7c_studio_broadcast
    "battlemap_t3_8a_compound_grounds" -> R.drawable.battlemap_t3_8a_compound_grounds
    "battlemap_t3_8b_compound_mansion" -> R.drawable.battlemap_t3_8b_compound_mansion
    "battlemap_t3_8c_compound_upper_bunker" -> R.drawable.battlemap_t3_8c_compound_upper_bunker
    "battlemap_t4_1a_maelstrom_hall" -> R.drawable.battlemap_t4_1a_maelstrom_hall
    "battlemap_t4_1b_maelstrom_surgery" -> R.drawable.battlemap_t4_1b_maelstrom_surgery
    "battlemap_t4_1c_maelstrom_basement" -> R.drawable.battlemap_t4_1c_maelstrom_basement
    "battlemap_t4_2a_tiger_claws_club" -> R.drawable.battlemap_t4_2a_tiger_claws_club
    "battlemap_t4_2b_tiger_claws_bathhouse" -> R.drawable.battlemap_t4_2b_tiger_claws_bathhouse
    "battlemap_t4_2c_tiger_claws_offices" -> R.drawable.battlemap_t4_2c_tiger_claws_offices
    "battlemap_t4_5c_av_vault" -> R.drawable.battlemap_t4_5c_av_vault
    "battlemap_t4_6a_bank_hall" -> R.drawable.battlemap_t4_6a_bank_hall
    "battlemap_t4_6b_bank_exec" -> R.drawable.battlemap_t4_6b_bank_exec
    "battlemap_t4_6c_bank_deposit" -> R.drawable.battlemap_t4_6c_bank_deposit
    "battlemap_t4_6d_bank_vault" -> R.drawable.battlemap_t4_6d_bank_vault
    "battlemap_t4_7a_prison_yard" -> R.drawable.battlemap_t4_7a_prison_yard
    "battlemap_t4_7b_prison_cells" -> R.drawable.battlemap_t4_7b_prison_cells
    "battlemap_t4_7c_prison_medical" -> R.drawable.battlemap_t4_7c_prison_medical
    "battlemap_t4_7d_prison_containment" -> R.drawable.battlemap_t4_7d_prison_containment
    "battlemap_t4_8a_arasaka_portal" -> R.drawable.battlemap_t4_8a_arasaka_portal
    "battlemap_t4_8b_arasaka_cavern" -> R.drawable.battlemap_t4_8b_arasaka_cavern
    "battlemap_t4_8c_arasaka_command" -> R.drawable.battlemap_t4_8c_arasaka_command
    "battlemap_t4_8d_arasaka_reactor" -> R.drawable.battlemap_t4_8d_arasaka_reactor
    "battlemap_t4_9a_militech_perimeter" -> R.drawable.battlemap_t4_9a_militech_perimeter
    "battlemap_t4_9b_militech_ops" -> R.drawable.battlemap_t4_9b_militech_ops
    "battlemap_t4_9c_militech_training" -> R.drawable.battlemap_t4_9c_militech_training
    "battlemap_t4_9d_militech_bay" -> R.drawable.battlemap_t4_9d_militech_bay
    "battlemap_t4_9e_militech_bunker" -> R.drawable.battlemap_t4_9e_militech_bunker
    "battlemap_t4_10a_shelter_entry" -> R.drawable.battlemap_t4_10a_shelter_entry
    "battlemap_t4_10b_shelter_habitation" -> R.drawable.battlemap_t4_10b_shelter_habitation
    "battlemap_t4_10c_shelter_hydroponics" -> R.drawable.battlemap_t4_10c_shelter_hydroponics
    "battlemap_t4_10d_shelter_power" -> R.drawable.battlemap_t4_10d_shelter_power
    "battlemap_t4_10e_shelter_core" -> R.drawable.battlemap_t4_10e_shelter_core
    "battlemap_t5_1a_militech_lobby" -> R.drawable.battlemap_t5_1a_militech_lobby
    "battlemap_t5_1b_militech_showroom" -> R.drawable.battlemap_t5_1b_militech_showroom
    "battlemap_t5_1c_militech_security" -> R.drawable.battlemap_t5_1c_militech_security
    "battlemap_t5_1d_militech_research" -> R.drawable.battlemap_t5_1d_militech_research
    "battlemap_t5_1e_militech_executive" -> R.drawable.battlemap_t5_1e_militech_executive
    "battlemap_t5_1f_militech_boss" -> R.drawable.battlemap_t5_1f_militech_boss
    "battlemap_t5_2a_arasaka_tower_lobby" -> R.drawable.battlemap_t5_2a_arasaka_tower_lobby
    "battlemap_t5_2b_arasaka_admin" -> R.drawable.battlemap_t5_2b_arasaka_admin
    "battlemap_t5_2c_arasaka_garrison" -> R.drawable.battlemap_t5_2c_arasaka_garrison
    "battlemap_t5_2d_arasaka_biotech" -> R.drawable.battlemap_t5_2d_arasaka_biotech
    "battlemap_t5_2e_arasaka_data_fortress" -> R.drawable.battlemap_t5_2e_arasaka_data_fortress
    "battlemap_t5_2f_arasaka_executive" -> R.drawable.battlemap_t5_2f_arasaka_executive
    "battlemap_t5_2g_arasaka_vault" -> R.drawable.battlemap_t5_2g_arasaka_vault
    "battlemap_t5_2h_arasaka_adam_smasher_room" -> R.drawable.battlemap_t5_2h_arasaka_adam_smasher_room
    "battlemap_t4_3_sixth_street_yard" -> R.drawable.battlemap_t4_3_sixth_street_yard
    "battlemap_t4_3_sixth_street_armory" -> R.drawable.battlemap_t4_3_sixth_street_armory
    "battlemap_t4_3_sixth_street_bunker" -> R.drawable.battlemap_t4_3_sixth_street_bunker
    "battlemap_t4_4_voodoo_surface" -> R.drawable.battlemap_t4_4_voodoo_surface
    "battlemap_t4_4_voodoo_mezzanine" -> R.drawable.battlemap_t4_4_voodoo_mezzanine
    "battlemap_t4_4_voodoo_platform" -> R.drawable.battlemap_t4_4_voodoo_platform
    "battlemap_t4_4_voodoo_vault" -> R.drawable.battlemap_t4_4_voodoo_vault
    "battlemap_t4_5_av_apron" -> R.drawable.battlemap_t4_5_av_apron
    "battlemap_t4_5_av_hangar" -> R.drawable.battlemap_t4_5_av_hangar
    else -> null
}

class BattleMapStore(private val context: Context) {
    private val gson = Gson()
    private val uiPrefs = context.getSharedPreferences("battle_map_ui", Context.MODE_PRIVATE)
    private val store = AtomicJsonFileStore(File(context.filesDir, "battle_maps_v1.json"))
    private val assetDir = File(context.filesDir, "battle_maps").apply { mkdirs() }

    fun load(): List<BattleMapDefinition> = PersistenceRuntime.locked {
        val result = store.read()
        if (result.text.isNullOrBlank()) {
            val builtins = builtinMaps()
            store.write(gson.toJson(builtins))
            return@locked builtins
        }
        val parsed = runCatching { gson.fromJson(result.text, Array<BattleMapDefinition>::class.java)?.toList().orEmpty() }
            .getOrElse {
                store.readBackupText()?.let { raw ->
                    runCatching { gson.fromJson(raw, Array<BattleMapDefinition>::class.java)?.toList().orEmpty() }.getOrNull()
                }.orEmpty()
            }
        val normalized = parsed.map(::normalizeLegacyMap)
        val builtins = builtinMaps()
        val builtinIds = builtins.mapTo(mutableSetOf()) { it.id }
        val mergedBuiltins = builtins.map { builtin ->
            normalized.firstOrNull { it.id == builtin.id }?.let { saved ->
                builtin.copy(
                    calibration = saved.calibration,
                    tokens = saved.tokens,
                    createdAt = saved.createdAt,
                    updatedAt = maxOf(saved.updatedAt, builtin.updatedAt)
                )
            } ?: builtin
        }
        // Keep imported/user maps, but retire obsolete built-ins from earlier development passes.
        val userMaps = normalized.filter { it.id !in builtinIds && !it.id.startsWith("builtin_") }
        val merged = mergedBuiltins + userMaps
        store.write(gson.toJson(merged))
        merged
    }

    fun save(items: List<BattleMapDefinition>): Boolean = PersistenceRuntime.locked {
        store.write(gson.toJson(items.distinctBy { it.id }))
    }

    fun upsert(map: BattleMapDefinition): Boolean {
        val all = load()
        return save(listOf(map.copy(updatedAt = System.currentTimeMillis())) + all.filterNot { it.id == map.id })
    }

    fun delete(map: BattleMapDefinition): Boolean {
        if (map.imagePath.startsWith("battle_maps/")) File(context.filesDir, map.imagePath).delete()
        return save(load().filterNot { it.id == map.id })
    }

    /**
     * Copies a user image byte-for-byte into app-owned storage.
     * The stored filename extension is derived from the decoded image format, never from the picker name.
     * This avoids the old `.img`/wrong-extension problem without recompressing or degrading user maps.
     */
    fun importImage(uri: Uri, name: String = "Imported Map", district: String = "Custom"): BattleMapDefinition {
        val id = UUID.randomUUID().toString()
        val tmp = File(assetDir, "$id.import.tmp")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "تصویر قابل خواندن نیست" }
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tmp.absolutePath, opts)
        require(opts.outWidth > 0 && opts.outHeight > 0) {
            tmp.delete()
            "فایل انتخاب‌شده تصویر معتبر نیست"
        }
        val extension = when (opts.outMimeType?.lowercase()) {
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/webp" -> "webp"
            else -> "img"
        }
        val file = File(assetDir, "$id.$extension")
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
        val defaultCell = (maxOf(opts.outWidth, opts.outHeight) / 18f).coerceIn(24f, 180f)
        return BattleMapDefinition(
            id = id, name = name, district = district,
            imagePath = "battle_maps/${file.name}", imageWidth = opts.outWidth, imageHeight = opts.outHeight,
            calibration = BattleMapCalibration(
                metersPerCell = 2f, cellSizePixels = defaultCell, originX = 0f, originY = 0f,
                calibrated = true, gridVisible = true, gridOpacity = 0.30f
            )
        ).also { upsert(it) }
    }

    fun fileFor(map: BattleMapDefinition): File? = map.imagePath.takeIf { it.startsWith("battle_maps/") }?.let { File(context.filesDir, it) }

    fun lastActiveMapId(): String? = uiPrefs.getString("active_map_id", null)
    fun setLastActiveMapId(id: String) { uiPrefs.edit().putString("active_map_id", id).apply() }

    private fun normalizeLegacyMap(map: BattleMapDefinition): BattleMapDefinition {
        val c = map.calibration
        val normalizedCalibration =
            if (c.calibrated && c.cellSizePixels <= 0f && c.pixelsPerMeter > 0f)
                c.copy(cellSizePixels = c.pixelsPerMeter * c.metersPerCell)
            else c
        val normalizedTokens = map.tokens.map { token ->
            token.copy(
                gridWidth = token.gridWidth.coerceAtLeast(1),
                gridHeight = token.gridHeight.coerceAtLeast(1),
                rotationDegrees = ((token.rotationDegrees % 360f) + 360f) % 360f,
                tokenLayer = when {
                    token.isEnvironment && token.tokenLayer == "effects" -> "effects"
                    token.isEnvironment -> "environment"
                    else -> "characters"
                }
            )
        }
        return map.copy(calibration = normalizedCalibration, tokens = normalizedTokens)
    }

    private fun builtinMaps(): List<BattleMapDefinition> = listOf(
        BattleMapDefinition(
            id = "builtin_warehouse_yard", name = "T1-1 — گاراژ متروکه", district = "T1 • گاراژ • متروکه • صنعتی • داخلی • محوطه",
            tier = 1, mapNumber = 1, tags = listOf("گاراژ", "متروکه", "صنعتی", "تعمیرگاه"),
            imagePath = "res:battlemap_t1_1_abandoned_garage", imageWidth = 1536, imageHeight = 1024,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 1L, updatedAt = 1L
        ),
        BattleMapDefinition(
            id = "builtin_t1_2_warehouse_yard", name = "T1-2 — محوطه انبار", district = "T1 • انبار • صنعتی • محوطه • لجستیک",
            tier = 1, mapNumber = 2, tags = listOf("محوطه انبار", "انبار", "صنعتی", "لجستیک"),
            imagePath = "res:battlemap_t1_2_warehouse_yard", imageWidth = 1536, imageHeight = 1024,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 2L, updatedAt = 2L
        ),
        BattleMapDefinition(
            id = "builtin_t1_4_industrial_repair_complex", name = "T1-4 — مجتمع تعمیرات صنعتی", district = "T1 • تعمیرگاه • صنعتی • کانتینر • پارکینگ",
            tier = 1, mapNumber = 4, tags = listOf("مجتمع تعمیرات صنعتی", "تعمیرگاه", "صنعتی", "کانتینر", "پارکینگ"),
            imagePath = "res:battlemap_t1_4_industrial_repair_complex", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 4L, updatedAt = 4L
        ),
        BattleMapDefinition(
            id = "builtin_t1_5_back_alley", name = "T1-5 — کوچه پشتی", district = "T1 • کوچه • شهری • غذا • پارکینگ • خیابان",
            tier = 1, mapNumber = 5, tags = listOf("کوچه پشتی", "کوچه", "شهری", "خیابان", "غذا"),
            imagePath = "res:battlemap_t1_5_back_alley", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 5L, updatedAt = 5L
        ),
        BattleMapDefinition(
            id = "builtin_t1_6_megabuilding_apartment", name = "T1-6 — آپارتمان مگابیلدینگ", district = "T1 • مسکونی • آپارتمان • مگابیلدینگ • داخلی • راهرو",
            tier = 1, mapNumber = 6, tags = listOf("آپارتمان", "مگابیلدینگ", "مسکونی", "داخلی", "راهرو"),
            imagePath = "res:battlemap_t1_6_megabuilding_apartment", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 6L, updatedAt = 6L
        ),
        BattleMapDefinition(
            id = "builtin_t1_7_club", name = "T1-7 — کلاب", district = "T1 • کلاب • بار • داخلی • سرگرمی",
            tier = 1, mapNumber = 7, tags = listOf("کلاب", "بار", "داخلی", "سرگرمی"),
            imagePath = "res:battlemap_t1_7_club", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 7L, updatedAt = 7L
        ),
        BattleMapDefinition(
            id = "builtin_t1_8_night_market", name = "T1-8 — بازار شبانه", district = "T1 • بازار • فروشگاه • غذا • تجاری • خیابانی",
            tier = 1, mapNumber = 8, tags = listOf("بازار شبانه", "بازار", "فروشگاه", "غذا", "تجاری", "خیابانی", "فضای باز"),
            imagePath = "res:battlemap_t1_8_night_market", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 8L, updatedAt = 8L
        ),
        BattleMapDefinition(
            id = "builtin_t1_9_ripperdoc_clinic", name = "T1-9 — کلینیک ریپرداک", district = "T1 • کلینیک • پزشکی • سایبروِر • جراحی • داخلی",
            tier = 1, mapNumber = 9, tags = listOf("کلینیک", "ریپرداک", "پزشکی", "درمانگاه", "سایبروِر", "جراحی", "داخلی"),
            imagePath = "res:battlemap_t1_9_ripperdoc_clinic", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 9L, updatedAt = 9L
        ),
        BattleMapDefinition(
            id = "builtin_t1_10_parking_garage", name = "T1-10 — پارکینگ طبقاتی", district = "T1 • پارکینگ • خودرو • شهری • رمپ • فضای باز",
            tier = 1, mapNumber = 10, tags = listOf("پارکینگ طبقاتی", "پارکینگ", "خودرو", "شهری", "رمپ", "فضای باز"),
            imagePath = "res:battlemap_t1_10_parking_garage", imageWidth = 1495, imageHeight = 1052,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 10L, updatedAt = 10L
        ),
        BattleMapDefinition(
            id = "builtin_t1_11_scav_den_main", name = "T1-11 — لانه اسکاونجرها — ساختمان", district = "T1 • اسکاونجر • ساختمان • مخفیگاه • داخلی",
            tier = 1, mapNumber = 11, tags = listOf("لانه اسکاونجرها", "اسکاونجر", "ساختمان", "مخفیگاه"), mapSetId = "t1_11_scavengers_den", pageName = "ساختمان", pageOrder = 1,
            imagePath = "res:battlemap_t1_11_scav_den_main", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 11L, updatedAt = 11L
        ),
        BattleMapDefinition(
            id = "builtin_t1_11_scav_den_sewer", name = "T1-11 — لانه اسکاونجرها — فاضلاب", district = "T1 • اسکاونجر • فاضلاب • زیرزمین • تونل",
            tier = 1, mapNumber = 11, tags = listOf("لانه اسکاونجرها", "اسکاونجر", "فاضلاب", "زیرزمین", "تونل"), mapSetId = "t1_11_scavengers_den", pageName = "فاضلاب", pageOrder = 2,
            imagePath = "res:battlemap_t1_11_scav_den_sewer", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 11L, updatedAt = 11L
        ),
        BattleMapDefinition(
            id = "builtin_t1_11_scav_den_deep_sewer", name = "T1-11 — لانه اسکاونجرها — فاضلاب عمیق", district = "T1 • اسکاونجر • فاضلاب عمیق • زیرزمین • تونل",
            tier = 1, mapNumber = 11, tags = listOf("لانه اسکاونجرها", "اسکاونجر", "فاضلاب عمیق", "زیرزمین", "تونل"), mapSetId = "t1_11_scavengers_den", pageName = "فاضلاب عمیق", pageOrder = 3,
            imagePath = "res:battlemap_t1_11_scav_den_deep_sewer", imageWidth = 1536, imageHeight = 1085,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 11L, updatedAt = 11L
        ),
        BattleMapDefinition(
            id = "builtin_t2_1_trauma_clinic", name = "T2-1 — کلینیک تروما", district = "T2 • کلینیک • بیمارستان • اورژانس • پزشکی",
            tier = 2, mapNumber = 1, tags = listOf("کلینیک تروما", "کلینیک", "بیمارستان", "پزشکی", "جراحی", "اورژانس", "آمبولانس", "هلی‌پد", "داخلی"),
            imagePath = "res:battlemap_t2_1_trauma_clinic", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 21L, updatedAt = 21L
        ),
        BattleMapDefinition(
            id = "builtin_t2_2_metro_station", name = "T2-2 — ایستگاه مترو", district = "T2 • مترو • ایستگاه • قطار • حمل‌ونقل • زیرزمینی",
            tier = 2, mapNumber = 2, tags = listOf("ایستگاه مترو", "مترو", "ایستگاه", "قطار", "حمل‌ونقل", "زیرزمینی", "شهری"),
            imagePath = "res:battlemap_t2_2_metro_station", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 22L, updatedAt = 22L
        ),
        BattleMapDefinition(
            id = "builtin_t2_3_fight_club_ground", name = "T2-3 — باشگاه مبارزه — همکف", district = "T2 • باشگاه مبارزه • باشگاه • داخلی",
            tier = 2, mapNumber = 3, tags = listOf("باشگاه مبارزه", "فایت کلاب", "باشگاه", "مبارزه", "همکف"), mapSetId = "t2_3_fight_club", pageName = "همکف", pageOrder = 1,
            imagePath = "res:battlemap_t2_3_fight_club_ground", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 23L, updatedAt = 23L
        ),
        BattleMapDefinition(
            id = "builtin_t2_3_fight_club_basement", name = "T2-3 — باشگاه مبارزه — طبقه زیرین", district = "T2 • باشگاه مبارزه • رینگ • زیرزمین • مبارزات غیرقانونی",
            tier = 2, mapNumber = 3, tags = listOf("باشگاه مبارزه", "فایت کلاب", "رینگ", "زیرزمین", "طبقه زیرین", "مبارزات غیرقانونی"), mapSetId = "t2_3_fight_club", pageName = "طبقه زیرین", pageOrder = 2,
            imagePath = "res:battlemap_t2_3_fight_club_basement", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 23L, updatedAt = 23L
        ),
        BattleMapDefinition(
            id = "builtin_t2_4_abandoned_factory", name = "T2-4 — کارخانه متروکه", district = "T2 • کارخانه • متروکه • صنعتی • خط تولید • انبار",
            tier = 2, mapNumber = 4, tags = listOf("کارخانه متروکه", "کارخانه", "متروکه", "صنعتی", "خط تولید", "انبار", "کارگاه", "بارگیری", "جرثقیل"),
            imagePath = "res:battlemap_t2_4_abandoned_factory", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 24L, updatedAt = 24L
        ),
        BattleMapDefinition(
            id = "builtin_t2_7_cargo_dock", name = "T2-7 — اسکله باربری — محوطه اسکله", district = "T2 • اسکله • باربری • کانتینر • بندر • فضای باز",
            tier = 2, mapNumber = 7, tags = listOf("اسکله باربری", "اسکله", "بندر", "باربری", "کانتینر", "جرثقیل", "کشتی", "فضای باز"), mapSetId = "t2_7_cargo_dock", pageName = "محوطه اسکله", pageOrder = 1,
            imagePath = "res:battlemap_t2_7_cargo_dock", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 27L, updatedAt = 27L
        ),
        BattleMapDefinition(
            id = "builtin_t2_7_smuggling_warehouse_ground", name = "T2-7 — اسکله باربری — انبار قاچاق — همکف", district = "T2 • اسکله • انبار قاچاق • انبار • داخلی",
            tier = 2, mapNumber = 7, tags = listOf("اسکله باربری", "انبار قاچاق", "قاچاق", "انبار", "همکف", "اسلحه", "بار"), mapSetId = "t2_7_cargo_dock", pageName = "انبار قاچاق — همکف", pageOrder = 2,
            imagePath = "res:battlemap_t2_7_smuggling_warehouse_ground", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 27L, updatedAt = 27L
        ),
        BattleMapDefinition(
            id = "builtin_t2_7_smuggling_warehouse_upper", name = "T2-7 — اسکله باربری — انبار قاچاق — طبقه دوم", district = "T2 • اسکله • انبار قاچاق • دفتر • امنیت • طبقه دوم",
            tier = 2, mapNumber = 7, tags = listOf("اسکله باربری", "انبار قاچاق", "قاچاق", "طبقه دوم", "دفتر", "امنیت", "اسلحه‌خانه", "اتاق کنترل"), mapSetId = "t2_7_cargo_dock", pageName = "انبار قاچاق — طبقه دوم", pageOrder = 3,
            imagePath = "res:battlemap_t2_7_smuggling_warehouse_upper", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 27L, updatedAt = 27L
        ),
        BattleMapDefinition(
            id = "builtin_t2_8_ncpd_floor1", name = "T2-8 — اداره پلیس NCPD — طبقه اول", district = "T2 • NCPD • پلیس • اداری • امنیت",
            tier = 2, mapNumber = 8, tags = listOf("اداره پلیس", "NCPD", "پلیس", "طبقه اول", "اداری", "امنیت", "اتاق فرمان", "سرور"), mapSetId = "t2_8_ncpd", pageName = "طبقه اول", pageOrder = 1,
            imagePath = "res:battlemap_t2_8_ncpd_floor1", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 28L, updatedAt = 28L
        ),
        BattleMapDefinition(
            id = "builtin_t2_8_ncpd_floor2", name = "T2-8 — اداره پلیس NCPD — طبقه دوم", district = "T2 • NCPD • پلیس • اداری • امنیت • طبقه دوم",
            tier = 2, mapNumber = 8, tags = listOf("اداره پلیس", "NCPD", "پلیس", "طبقه دوم", "اداری", "امنیت", "دفتر", "اتاق جلسه"), mapSetId = "t2_8_ncpd", pageName = "طبقه دوم", pageOrder = 2,
            imagePath = "res:battlemap_t2_8_ncpd_floor2", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 28L, updatedAt = 28L
        ),
        BattleMapDefinition(
            id = "builtin_t2_9_radio_station", name = "T2-9 — ایستگاه رادیویی", district = "T2 • رادیو • مخابرات • آنتن • برج • ارتباطات",
            tier = 2, mapNumber = 9, tags = listOf("ایستگاه رادیویی", "رادیو", "مخابرات", "آنتن", "برج", "ماهواره", "ارتباطات", "محوطه امنیتی"),
            imagePath = "res:battlemap_t2_9_radio_station", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 29L, updatedAt = 29L
        ),
        BattleMapDefinition(
            id = "builtin_t2_10_casino_floor1", name = "T2-10 — کازینو — طبقه اول", district = "T2 • کازینو • قمار • سرگرمی • طبقه اول",
            tier = 2, mapNumber = 10, tags = listOf("کازینو", "قمار", "سرگرمی", "طبقه اول", "میز بازی", "بار", "امنیت"), mapSetId = "t2_10_casino", pageName = "طبقه اول", pageOrder = 1,
            imagePath = "res:battlemap_t2_10_casino_floor1", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 30L, updatedAt = 30L
        ),
        BattleMapDefinition(
            id = "builtin_t2_10_casino_vip", name = "T2-10 — کازینو — طبقه VIP", district = "T2 • کازینو • VIP • امنیت • سوئیت • طبقه دوم",
            tier = 2, mapNumber = 10, tags = listOf("کازینو", "VIP", "وی‌آی‌پی", "طبقه VIP", "لانژ", "سوئیت", "اتاق امنیت", "اتاق شمارش", "تراس"), mapSetId = "t2_10_casino", pageName = "طبقه VIP", pageOrder = 2,
            imagePath = "res:battlemap_t2_10_casino_vip", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 30L, updatedAt = 30L
        ),
        BattleMapDefinition(
            id = "builtin_t3_1_office_executive", name = "T3-1 — دفتر اجرایی شرکتی", district = "T3 • اداری • شرکتی • دفتر اجرایی • لابی • پذیرش • سرور • آسانسور",
            tier = 3, mapNumber = 1, tags = listOf("اداری", "شرکتی", "دفتر اجرایی", "دفتر", "مدیر", "لابی", "پذیرش", "اتاق سرور", "امنیت", "آسانسور", "Corporate", "Executive", "Office", "HQ", "Lobby", "Reception", "Server Room", "Security", "Elevator", "Heist", "Infiltration", "Espionage"),
            mapSetId = "t3_1_office", pageName = "Executive Office", pageOrder = 1,
            imagePath = "res:battlemap_t3_1_office_executive", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 31L, updatedAt = 31L
        ),
        BattleMapDefinition(
            id = "builtin_t3_1_office_openplan", name = "T3-1 — دفتر اداری فضای باز", district = "T3 • اداری • شرکتی • فضای باز • اتاق جلسه • کنفرانس • محل کار • آسانسور",
            tier = 3, mapNumber = 1, tags = listOf("اداری", "دفتر", "فضای باز", "شرکت", "شرکتی", "اتاق جلسه", "اتاق کنفرانس", "محل کار", "Corporate", "Office", "Open Plan", "Workplace", "Meeting Room", "Conference Room", "Break Room", "Elevator", "Indoor", "Infiltration"),
            mapSetId = "t3_1_office", pageName = "Open Plan Office", pageOrder = 2,
            imagePath = "res:battlemap_t3_1_office_openplan", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 32L, updatedAt = 32L
        ),
        BattleMapDefinition(
            id = "builtin_t3_1_office_lobby", name = "T3-1 — لابی و دفاتر مدیریتی", district = "T3 • لابی • پذیرش • اداری • شرکتی • دفتر مدیریت • جلسه • کنفرانس • آسانسور",
            tier = 3, mapNumber = 1, tags = listOf("لابی", "پذیرش", "اداری", "شرکتی", "دفتر مدیریت", "اتاق جلسه", "سالن کنفرانس", "Corporate", "Office", "Lobby", "Reception", "Executive", "Meeting Room", "Conference Room", "Elevator", "Indoor", "Extraction", "Espionage"),
            mapSetId = "t3_1_office", pageName = "Office Lobby", pageOrder = 3,
            imagePath = "res:battlemap_t3_1_office_lobby", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 33L, updatedAt = 33L
        ),
        BattleMapDefinition(
            id = "builtin_t3_1_office_parking", name = "T3-1 — پارکینگ و بخش خدمات شرکت", district = "T3 • پارکینگ • شرکتی • زیرزمین • خودرو • موتورسیکلت • انبار • تأسیسات • آسانسور",
            tier = 3, mapNumber = 1, tags = listOf("پارکینگ", "شرکت", "شرکتی", "زیرزمین", "خودرو", "موتورسیکلت", "انبار", "تأسیسات", "آسانسور", "Parking", "Corporate", "Office", "Garage", "Basement", "Vehicle", "Motorcycle", "Storage", "Maintenance", "Elevator", "Indoor", "Heist", "Escape"),
            mapSetId = "t3_1_office", pageName = "Office Parking", pageOrder = 4,
            imagePath = "res:battlemap_t3_1_office_parking", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 34L, updatedAt = 34L
        ),
        BattleMapDefinition(
            id = "builtin_t3_2_lab_admin", name = "T3-2 — بخش اداری آزمایشگاه", district = "T3 • آزمایشگاه • اداری • پذیرش • آموزش • سرور • تأسیسات",
            tier = 3, mapNumber = 2, tags = listOf("آزمایشگاه", "اداری", "پذیرش", "دفتر", "کلاس", "آموزش", "سرور", "تأسیسات", "Lab", "Laboratory", "Admin", "Office", "Reception", "Training", "Server", "Utility", "Research"),
            mapSetId = "t3_2_lab", pageName = "Lab Admin", pageOrder = 1,
            imagePath = "res:battlemap_t3_2_lab_admin", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 40L, updatedAt = 40L
        ),
        BattleMapDefinition(
            id = "builtin_t3_2_lab_main", name = "T3-2 — آزمایشگاه اصلی پژوهشی", district = "T3 • آزمایشگاه • پژوهش • پزشکی • رباتیک • اسکن • سردخانه • مواد خطرناک",
            tier = 3, mapNumber = 2, tags = listOf("آزمایشگاه", "پژوهش", "پزشکی", "رباتیک", "اسکنر", "سردخانه", "مواد خطرناک", "سرور", "Lab", "Laboratory", "Research", "Medical", "Robotics", "Scanner", "Cold Storage", "Hazardous", "Server"),
            mapSetId = "t3_2_lab", pageName = "Main Lab", pageOrder = 2,
            imagePath = "res:battlemap_t3_2_lab_main", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 41L, updatedAt = 41L
        ),
        BattleMapDefinition(
            id = "builtin_t3_2_lab_sublevel", name = "T3-2 — زیرسطح محرمانه آزمایشگاه", district = "T3 • آزمایشگاه • زیرزمین • محرمانه • بیوتک • امنیت • سلول • تأسیسات",
            tier = 3, mapNumber = 2, tags = listOf("آزمایشگاه", "زیرزمین", "محرمانه", "بیوتک", "امنیت", "سلول", "آزمایش مخفی", "تأسیسات", "Lab", "Sublevel", "Secret", "Biotech", "Security", "Cells", "Underground", "Black Site"),
            mapSetId = "t3_2_lab", pageName = "Lab Sublevel", pageOrder = 3,
            imagePath = "res:battlemap_t3_2_lab_sublevel", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 42L, updatedAt = 42L
        ),
        BattleMapDefinition(
            id = "builtin_t3_3_data_center_ops", name = "T3-3 — مرکز عملیات دیتاسنتر", district = "T3 • دیتاسنتر • عملیات • امنیت • کنترل • سرور • اداری",
            tier = 3, mapNumber = 3, tags = listOf("دیتاسنتر", "مرکز داده", "عملیات", "کنترل", "امنیت", "سرور", "اداری", "Data Center", "Operations", "NOC", "Control", "Security", "Server", "Admin", "Network"),
            mapSetId = "t3_3_datacenter", pageName = "Data Center Ops", pageOrder = 1,
            imagePath = "res:battlemap_t3_3_data_center_ops", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 43L, updatedAt = 43L
        ),
        BattleMapDefinition(
            id = "builtin_t3_3_data_center_halls", name = "T3-3 — سالن‌های سرور دیتاسنتر", district = "T3 • دیتاسنتر • سالن سرور • شبکه • رک • امنیت • زیرساخت",
            tier = 3, mapNumber = 3, tags = listOf("دیتاسنتر", "مرکز داده", "سالن سرور", "سرور", "رک", "شبکه", "امنیت", "Data Center", "Server Hall", "Server", "Rack", "Network", "Security", "Infrastructure", "Netrunner"),
            mapSetId = "t3_3_datacenter", pageName = "Data Center Halls", pageOrder = 2,
            imagePath = "res:battlemap_t3_3_data_center_halls", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 44L, updatedAt = 44L
        ),
        BattleMapDefinition(
            id = "builtin_t3_3_data_center_plant", name = "T3-3 — تأسیسات زیرساخت دیتاسنتر", district = "T3 • دیتاسنتر • تأسیسات • برق • خنک‌کننده • باتری • گاوصندوق داده",
            tier = 3, mapNumber = 3, tags = listOf("دیتاسنتر", "تأسیسات", "برق", "ولتاژ بالا", "خنک‌کننده", "باتری", "گاوصندوق داده", "کارگاه", "Data Center", "Plant", "Power", "Cooling", "Battery", "Data Vault", "Workshop", "Utility"),
            mapSetId = "t3_3_datacenter", pageName = "Data Center Plant", pageOrder = 3,
            imagePath = "res:battlemap_t3_3_data_center_plant", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 45L, updatedAt = 45L
        ),
        BattleMapDefinition(
            id = "builtin_t3_4_hotel_lobby", name = "T3-4 — لابی هتل لوکس", district = "T3 • هتل • لابی • پذیرش • رستوران • سالن • آسانسور",
            tier = 3, mapNumber = 4, tags = listOf("هتل", "لابی", "پذیرش", "لوکس", "رستوران", "بار", "سالن", "آسانسور", "Hotel", "Lobby", "Reception", "Luxury", "Restaurant", "Bar", "Elevator", "Social"),
            mapSetId = "t3_4_hotel", pageName = "Hotel Lobby", pageOrder = 1,
            imagePath = "res:battlemap_t3_4_hotel_lobby", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 46L, updatedAt = 46L
        ),
        BattleMapDefinition(
            id = "builtin_t3_4_hotel_suites", name = "T3-4 — طبقه سوئیت‌های هتل", district = "T3 • هتل • سوئیت • اتاق مهمان • راهرو • خدمات • آسانسور",
            tier = 3, mapNumber = 4, tags = listOf("هتل", "سوئیت", "اتاق مهمان", "راهرو", "خدمات", "آسانسور", "Hotel", "Suites", "Guest Room", "Corridor", "Service", "Elevator", "Extraction", "Stealth"),
            mapSetId = "t3_4_hotel", pageName = "Hotel Suites", pageOrder = 2,
            imagePath = "res:battlemap_t3_4_hotel_suites", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 47L, updatedAt = 47L
        ),
        BattleMapDefinition(
            id = "builtin_t3_4_hotel_rooftop", name = "T3-4 — روف‌تاپ و استخر هتل", district = "T3 • هتل • پشت‌بام • استخر • بار • هلی‌پد • آسانسور",
            tier = 3, mapNumber = 4, tags = listOf("هتل", "پشت‌بام", "روف‌تاپ", "استخر", "بار", "هلی‌پد", "آسانسور", "Hotel", "Rooftop", "Pool", "Bar", "Helipad", "Elevator", "Extraction", "Chase"),
            mapSetId = "t3_4_hotel", pageName = "Hotel Rooftop", pageOrder = 3,
            imagePath = "res:battlemap_t3_4_hotel_rooftop", imageWidth = 1536, imageHeight = 1077,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 48L, updatedAt = 48L
        ),
        BattleMapDefinition(
            id = "builtin_t3_5_clinic_front", name = "T3-5 — بخش پذیرش و درمانگاه کلینیک", district = "T3 • کلینیک • پذیرش • اتاق معاینه • درمان • خدمات",
            tier = 3, mapNumber = 5, tags = listOf("کلینیک", "پذیرش", "درمانگاه", "اتاق معاینه", "پزشکی", "Medtech", "Clinic", "Reception", "Exam Room", "Medical", "Treatment", "Front"),
            mapSetId = "t3_5_clinic", pageName = "Clinic Front", pageOrder = 1,
            imagePath = "res:battlemap_t3_5_clinic_front", imageWidth = 1536, imageHeight = 711,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 49L, updatedAt = 49L
        ),
        BattleMapDefinition(
            id = "builtin_t3_5_clinic_surgical", name = "T3-5 — بخش جراحی کلینیک", district = "T3 • کلینیک • جراحی • اتاق عمل • بستری • پزشکی • سردخانه",
            tier = 3, mapNumber = 5, tags = listOf("کلینیک", "جراحی", "اتاق عمل", "بستری", "پزشکی", "خون", "Medtech", "Clinic", "Surgery", "Operating Room", "Ward", "Medical", "Morgue"),
            mapSetId = "t3_5_clinic", pageName = "Clinic Surgical", pageOrder = 2,
            imagePath = "res:battlemap_t3_5_clinic_surgical", imageWidth = 1536, imageHeight = 711,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 50L, updatedAt = 50L
        ),
        BattleMapDefinition(
            id = "builtin_t3_5_clinic_organfarm", name = "T3-5 — بخش کشت و نگهداری اعضای زیستی", district = "T3 • کلینیک • ارگان‌فارم • زیرزمین • بیوتک • کشت عضو • آزمایشگاه",
            tier = 3, mapNumber = 5, tags = listOf("کلینیک", "ارگان فارم", "کشت عضو", "اعضای زیستی", "بیوتک", "زیرزمین", "آزمایشگاه", "Medtech", "Clinic", "Organ Farm", "Biotech", "Underground", "Lab", "Black Clinic"),
            mapSetId = "t3_5_clinic", pageName = "Clinic Organ Farm", pageOrder = 3,
            imagePath = "res:battlemap_t3_5_clinic_organfarm", imageWidth = 1536, imageHeight = 711,
            calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
            createdAt = 51L, updatedAt = 51L
        ),
        BattleMapDefinition(
            id = "builtin_t4_3_sixth_street_yard", name = "T4-3a — محوطه‌ی پایگاه سیکس استریت", district = "T4 • سیکس استریت • پایگاه • محوطه • خودرو • کانتینر",
            tier = 4, mapNumber = 3, tags = listOf("سیکس استریت", "محوطه پایگاه", "پایگاه", "خودرو", "کانتینر", "Sixth Street", "Yard"), mapSetId = "t4_3_sixth_street", pageName = "محوطه‌ی پایگاه", pageOrder = 1,
            imagePath = "res:battlemap_t4_3_sixth_street_yard", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 53L, updatedAt = 53L
        ),
        BattleMapDefinition(
            id = "builtin_t4_3_sixth_street_armory", name = "T4-3b — زرادخانه‌ی سیکس استریت", district = "T4 • سیکس استریت • زرادخانه • انبار • تسلیحات",
            tier = 4, mapNumber = 3, tags = listOf("سیکس استریت", "زرادخانه", "اسلحه", "انبار", "Sixth Street", "Armory"), mapSetId = "t4_3_sixth_street", pageName = "زرادخانه", pageOrder = 2,
            imagePath = "res:battlemap_t4_3_sixth_street_armory", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 54L, updatedAt = 54L
        ),
        BattleMapDefinition(
            id = "builtin_t4_3_sixth_street_bunker", name = "T4-3c — پناهگاه سیکس استریت", district = "T4 • سیکس استریت • پناهگاه • زیرزمینی • میدان تیر",
            tier = 4, mapNumber = 3, tags = listOf("سیکس استریت", "پناهگاه", "بانکر", "میدان تیر", "Sixth Street", "Bunker"), mapSetId = "t4_3_sixth_street", pageName = "پناهگاه", pageOrder = 3,
            imagePath = "res:battlemap_t4_3_sixth_street_bunker", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 55L, updatedAt = 55L
        ),
        BattleMapDefinition(
            id = "builtin_t4_4_voodoo_surface", name = "T4-4a — سطح پایگاه وودو بویز", district = "T4 • وودو بویز • سطح • آبگرفتگی • مخفیگاه",
            tier = 4, mapNumber = 4, tags = listOf("وودو بویز", "سطح پایگاه", "آبگرفتگی", "Voodoo Boys", "Surface"), mapSetId = "t4_4_voodoo_boys", pageName = "سطح پایگاه", pageOrder = 1,
            imagePath = "res:battlemap_t4_4_voodoo_surface", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 56L, updatedAt = 56L
        ),
        BattleMapDefinition(
            id = "builtin_t4_4_voodoo_mezzanine", name = "T4-4b — نیم‌طبقه‌ی وودو بویز", district = "T4 • وودو بویز • نیم‌طبقه • مخفیگاه • اتاق‌ها",
            tier = 4, mapNumber = 4, tags = listOf("وودو بویز", "نیم‌طبقه", "مخفیگاه", "Voodoo Boys", "Mezzanine"), mapSetId = "t4_4_voodoo_boys", pageName = "نیم‌طبقه", pageOrder = 2,
            imagePath = "res:battlemap_t4_4_voodoo_mezzanine", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 57L, updatedAt = 57L
        ),
        BattleMapDefinition(
            id = "builtin_t4_4_voodoo_platform", name = "T4-4c — سکوی متروی وودو بویز", district = "T4 • وودو بویز • مترو • سکو • قطار",
            tier = 4, mapNumber = 4, tags = listOf("وودو بویز", "مترو", "سکو", "قطار", "Voodoo Boys", "Platform"), mapSetId = "t4_4_voodoo_boys", pageName = "سکوی مترو", pageOrder = 3,
            imagePath = "res:battlemap_t4_4_voodoo_platform", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 58L, updatedAt = 58L
        ),
        BattleMapDefinition(
            id = "builtin_t4_4_voodoo_vault", name = "T4-4d — خزانه‌ی وودو بویز", district = "T4 • وودو بویز • خزانه • سرور • زیرزمین",
            tier = 4, mapNumber = 4, tags = listOf("وودو بویز", "خزانه", "سرور", "زیرزمین", "Voodoo Boys", "Vault"), mapSetId = "t4_4_voodoo_boys", pageName = "خزانه", pageOrder = 4,
            imagePath = "res:battlemap_t4_4_voodoo_vault", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 59L, updatedAt = 59L
        ),
        BattleMapDefinition(
            id = "builtin_t4_5_av_apron", name = "T4-5a — محوطه‌ی فرود AV", district = "T4 • AV • محوطه فرود • هلی‌پد • حمل‌ونقل",
            tier = 4, mapNumber = 5, tags = listOf("AV", "محوطه فرود", "هلی‌پد", "پرواز", "Apron"), mapSetId = "t4_5_av", pageName = "محوطه‌ی فرود AV", pageOrder = 1,
            imagePath = "res:battlemap_t4_5_av_apron", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 60L, updatedAt = 60L
        ),
        BattleMapDefinition(
            id = "builtin_t4_5_av_hangar", name = "T4-5b — آشیانه‌ی AV", district = "T4 • AV • آشیانه • هواگرد • تعمیرات",
            tier = 4, mapNumber = 5, tags = listOf("AV", "آشیانه", "هواگرد", "تعمیرات", "Hangar"), mapSetId = "t4_5_av", pageName = "آشیانه‌ی AV", pageOrder = 2,
            imagePath = "res:battlemap_t4_5_av_hangar", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 61L, updatedAt = 61L
        ),
        BattleMapDefinition(
            id = "builtin_t3_6a_plaza_ground", name = "T3-6a — طبقه همکف پلازا", district = "T3 • پلازا • تجاری • لابی • فروشگاه • فضای عمومی",
            tier = 3, mapNumber = 6, tags = listOf("پلازا", "تجاری", "لابی", "فروشگاه", "فضای عمومی", "Plaza", "Ground"), mapSetId = "t3_6_plaza", pageName = "طبقه همکف پلازا", pageOrder = 1,
            imagePath = "res:battlemap_t3_6a_plaza_ground", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 62L, updatedAt = 62L
        ),
        BattleMapDefinition(
            id = "builtin_t3_6b_plaza_gallery", name = "T3-6b — گالری پلازا", district = "T3 • پلازا • گالری • فروشگاه • رستوران • فضای عمومی",
            tier = 3, mapNumber = 6, tags = listOf("پلازا", "گالری", "فروشگاه", "رستوران", "Plaza", "Gallery"), mapSetId = "t3_6_plaza", pageName = "گالری پلازا", pageOrder = 2,
            imagePath = "res:battlemap_t3_6b_plaza_gallery", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 63L, updatedAt = 63L
        ),
        BattleMapDefinition(
            id = "builtin_t3_6c_plaza_parking", name = "T3-6c — پارکینگ پلازا", district = "T3 • پلازا • پارکینگ • خودرو • خدمات • زیرزمین",
            tier = 3, mapNumber = 6, tags = listOf("پلازا", "پارکینگ", "خودرو", "زیرزمین", "Plaza", "Parking"), mapSetId = "t3_6_plaza", pageName = "پارکینگ پلازا", pageOrder = 3,
            imagePath = "res:battlemap_t3_6c_plaza_parking", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 64L, updatedAt = 64L
        ),
        BattleMapDefinition(
            id = "builtin_t3_7a_studio_offices", name = "T3-7a — دفاتر استودیو", district = "T3 • استودیو • دفاتر • رسانه • اداری",
            tier = 3, mapNumber = 7, tags = listOf("استودیو", "دفاتر", "رسانه", "اداری", "Studio", "Offices"), mapSetId = "t3_7_studio", pageName = "دفاتر استودیو", pageOrder = 1,
            imagePath = "res:battlemap_t3_7a_studio_offices", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 65L, updatedAt = 65L
        ),
        BattleMapDefinition(
            id = "builtin_t3_7b_studio_soundstages", name = "T3-7b — استیج‌های ضبط استودیو", district = "T3 • استودیو • ضبط • صدا • صحنه • رسانه",
            tier = 3, mapNumber = 7, tags = listOf("استودیو", "ضبط", "صدا", "صحنه", "رسانه", "Studio", "Soundstage"), mapSetId = "t3_7_studio", pageName = "استیج‌های ضبط", pageOrder = 2,
            imagePath = "res:battlemap_t3_7b_studio_soundstages", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 66L, updatedAt = 66L
        ),
        BattleMapDefinition(
            id = "builtin_t3_7c_studio_broadcast", name = "T3-7c — بخش پخش استودیو", district = "T3 • استودیو • پخش • کنترل • رسانه • ارتباطات",
            tier = 3, mapNumber = 7, tags = listOf("استودیو", "پخش", "کنترل", "رسانه", "Studio", "Broadcast"), mapSetId = "t3_7_studio", pageName = "بخش پخش", pageOrder = 3,
            imagePath = "res:battlemap_t3_7c_studio_broadcast", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 67L, updatedAt = 67L
        ),
        BattleMapDefinition(
            id = "builtin_t3_8a_compound_grounds", name = "T3-8a — محوطه مجتمع", district = "T3 • مجتمع • محوطه • ویلایی • هلی‌پد • فضای باز",
            tier = 3, mapNumber = 8, tags = listOf("مجتمع", "محوطه", "ویلایی", "هلی‌پد", "Compound", "Grounds"), mapSetId = "t3_8_compound", pageName = "محوطه مجتمع", pageOrder = 1,
            imagePath = "res:battlemap_t3_8a_compound_grounds", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 68L, updatedAt = 68L
        ),
        BattleMapDefinition(
            id = "builtin_t3_8b_compound_mansion", name = "T3-8b — عمارت مجتمع", district = "T3 • مجتمع • عمارت • مسکونی • لوکس • داخلی",
            tier = 3, mapNumber = 8, tags = listOf("مجتمع", "عمارت", "مسکونی", "لوکس", "Compound", "Mansion"), mapSetId = "t3_8_compound", pageName = "عمارت مجتمع", pageOrder = 2,
            imagePath = "res:battlemap_t3_8b_compound_mansion", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 69L, updatedAt = 69L
        ),
        BattleMapDefinition(
            id = "builtin_t3_8c_compound_upper_bunker", name = "T3-8c — طبقه بالایی و پناهگاه مجتمع", district = "T3 • مجتمع • طبقه بالا • پناهگاه • امنیتی • زیرزمینی",
            tier = 3, mapNumber = 8, tags = listOf("مجتمع", "پناهگاه", "امنیتی", "زیرزمینی", "Compound", "Bunker"), mapSetId = "t3_8_compound", pageName = "طبقه بالا و پناهگاه", pageOrder = 3,
            imagePath = "res:battlemap_t3_8c_compound_upper_bunker", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 70L, updatedAt = 70L
        ),
        BattleMapDefinition(
            id = "builtin_t4_1a_maelstrom_hall", name = "T4-1a — تالار میلستروم", district = "T4 • میلستروم • پایگاه • صنعتی • مخفیگاه",
            tier = 4, mapNumber = 1, tags = listOf("میلستروم", "تالار", "پایگاه", "صنعتی", "Maelstrom", "Hall"), mapSetId = "t4_1_maelstrom", pageName = "تالار میلستروم", pageOrder = 1,
            imagePath = "res:battlemap_t4_1a_maelstrom_hall", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 71L, updatedAt = 71L
        ),
        BattleMapDefinition(
            id = "builtin_t4_1b_maelstrom_surgery", name = "T4-1b — جراحی میلستروم", district = "T4 • میلستروم • جراحی • سایبروِر • کلینیک سیاه",
            tier = 4, mapNumber = 1, tags = listOf("میلستروم", "جراحی", "سایبروِر", "کلینیک سیاه", "Maelstrom", "Surgery"), mapSetId = "t4_1_maelstrom", pageName = "جراحی میلستروم", pageOrder = 2,
            imagePath = "res:battlemap_t4_1b_maelstrom_surgery", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 72L, updatedAt = 72L
        ),
        BattleMapDefinition(
            id = "builtin_t4_1c_maelstrom_basement", name = "T4-1c — زیرزمین میلستروم", district = "T4 • میلستروم • زیرزمین • زندان • صنعتی • مخفیگاه",
            tier = 4, mapNumber = 1, tags = listOf("میلستروم", "زیرزمین", "زندان", "صنعتی", "Maelstrom", "Basement"), mapSetId = "t4_1_maelstrom", pageName = "زیرزمین میلستروم", pageOrder = 3,
            imagePath = "res:battlemap_t4_1c_maelstrom_basement", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 73L, updatedAt = 73L
        ),
        BattleMapDefinition(
            id = "builtin_t4_2a_tiger_claws_club", name = "T4-2a — کلاب تایگر کلاوز", district = "T4 • تایگر کلاوز • کلاب • بار • سرگرمی",
            tier = 4, mapNumber = 2, tags = listOf("تایگر کلاوز", "کلاب", "بار", "Tiger Claws", "Club"), mapSetId = "t4_2_tiger_claws", pageName = "کلاب تایگر کلاوز", pageOrder = 1,
            imagePath = "res:battlemap_t4_2a_tiger_claws_club", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 74L, updatedAt = 74L
        ),
        BattleMapDefinition(
            id = "builtin_t4_2b_tiger_claws_bathhouse", name = "T4-2b — حمام تایگر کلاوز", district = "T4 • تایگر کلاوز • حمام • اسپا • داخلی",
            tier = 4, mapNumber = 2, tags = listOf("تایگر کلاوز", "حمام", "اسپا", "Tiger Claws", "Bathhouse"), mapSetId = "t4_2_tiger_claws", pageName = "حمام تایگر کلاوز", pageOrder = 2,
            imagePath = "res:battlemap_t4_2b_tiger_claws_bathhouse", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 75L, updatedAt = 75L
        ),
        BattleMapDefinition(
            id = "builtin_t4_2c_tiger_claws_offices", name = "T4-2c — دفاتر تایگر کلاوز", district = "T4 • تایگر کلاوز • دفاتر • قمار • زرادخانه",
            tier = 4, mapNumber = 2, tags = listOf("تایگر کلاوز", "دفاتر", "قمار", "زرادخانه", "Tiger Claws", "Offices"), mapSetId = "t4_2_tiger_claws", pageName = "دفاتر تایگر کلاوز", pageOrder = 3,
            imagePath = "res:battlemap_t4_2c_tiger_claws_offices", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 76L, updatedAt = 76L
        ),
        BattleMapDefinition(
            id = "builtin_t4_5c_av_vault", name = "T4-5c — خزانه AV", district = "T4 • AV • خزانه • امنیتی • کنترل",
            tier = 4, mapNumber = 5, tags = listOf("AV", "خزانه", "امنیتی", "کنترل", "Vault"), mapSetId = "t4_5_av", pageName = "خزانه AV", pageOrder = 3,
            imagePath = "res:battlemap_t4_5c_av_vault", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 77L, updatedAt = 77L
        ),
        BattleMapDefinition(
            id = "builtin_t4_6a_bank_hall", name = "T4-6a — تالار بانک", district = "T4 • بانک • تالار • عمومی • امنیتی",
            tier = 4, mapNumber = 6, tags = listOf("بانک", "تالار", "عمومی", "Bank", "Hall"), mapSetId = "t4_6_bank", pageName = "تالار بانک", pageOrder = 1,
            imagePath = "res:battlemap_t4_6a_bank_hall", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 78L, updatedAt = 78L
        ),
        BattleMapDefinition(
            id = "builtin_t4_6b_bank_exec", name = "T4-6b — طبقه مدیریت بانک", district = "T4 • بانک • مدیریت • اداری • اجرایی",
            tier = 4, mapNumber = 6, tags = listOf("بانک", "مدیریت", "اداری", "Executive", "Bank"), mapSetId = "t4_6_bank", pageName = "طبقه مدیریت بانک", pageOrder = 2,
            imagePath = "res:battlemap_t4_6b_bank_exec", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 79L, updatedAt = 79L
        ),
        BattleMapDefinition(
            id = "builtin_t4_6c_bank_deposit", name = "T4-6c — بخش امانات بانک", district = "T4 • بانک • امانات • امنیتی • خدمات",
            tier = 4, mapNumber = 6, tags = listOf("بانک", "امانات", "امنیتی", "Deposit", "Bank"), mapSetId = "t4_6_bank", pageName = "بخش امانات بانک", pageOrder = 3,
            imagePath = "res:battlemap_t4_6c_bank_deposit", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 80L, updatedAt = 80L
        ),
        BattleMapDefinition(
            id = "builtin_t4_6d_bank_vault", name = "T4-6d — خزانه بانک", district = "T4 • بانک • خزانه • امنیت بالا • زیرزمین",
            tier = 4, mapNumber = 6, tags = listOf("بانک", "خزانه", "امنیت بالا", "Vault", "Bank"), mapSetId = "t4_6_bank", pageName = "خزانه بانک", pageOrder = 4,
            imagePath = "res:battlemap_t4_6d_bank_vault", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 81L, updatedAt = 81L
        ),
        BattleMapDefinition(
            id = "builtin_t4_7a_prison_yard", name = "T4-7a — محوطه زندان", district = "T4 • زندان • محوطه • امنیتی • فضای باز",
            tier = 4, mapNumber = 7, tags = listOf("زندان", "محوطه", "امنیتی", "Prison", "Yard"), mapSetId = "t4_7_prison", pageName = "محوطه زندان", pageOrder = 1,
            imagePath = "res:battlemap_t4_7a_prison_yard", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 82L, updatedAt = 82L
        ),
        BattleMapDefinition(
            id = "builtin_t4_7b_prison_cells", name = "T4-7b — بند سلول‌های زندان", district = "T4 • زندان • سلول • بند • امنیتی",
            tier = 4, mapNumber = 7, tags = listOf("زندان", "سلول", "بند", "Prison", "Cells"), mapSetId = "t4_7_prison", pageName = "بند سلول‌های زندان", pageOrder = 2,
            imagePath = "res:battlemap_t4_7b_prison_cells", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 83L, updatedAt = 83L
        ),
        BattleMapDefinition(
            id = "builtin_t4_7c_prison_medical", name = "T4-7c — بخش پزشکی زندان", district = "T4 • زندان • پزشکی • درمان • امنیتی",
            tier = 4, mapNumber = 7, tags = listOf("زندان", "پزشکی", "درمان", "Prison", "Medical"), mapSetId = "t4_7_prison", pageName = "بخش پزشکی زندان", pageOrder = 3,
            imagePath = "res:battlemap_t4_7c_prison_medical", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 84L, updatedAt = 84L
        ),
        BattleMapDefinition(
            id = "builtin_t4_7d_prison_containment", name = "T4-7d — بخش مهار زندان", district = "T4 • زندان • مهار • امنیت بالا • آزمایشگاهی",
            tier = 4, mapNumber = 7, tags = listOf("زندان", "مهار", "امنیت بالا", "Prison", "Containment"), mapSetId = "t4_7_prison", pageName = "بخش مهار زندان", pageOrder = 4,
            imagePath = "res:battlemap_t4_7d_prison_containment", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 85L, updatedAt = 85L
        ),
        BattleMapDefinition(
            id = "builtin_t4_8a_arasaka_portal", name = "T4-8a — ورودی تأسیسات آراساکا", district = "T4 • آراساکا • ورودی • تأسیسات • امنیتی",
            tier = 4, mapNumber = 8, tags = listOf("آراساکا", "ورودی", "تأسیسات", "Arasaka", "Portal"), mapSetId = "t4_8_arasaka", pageName = "ورودی تأسیسات آراساکا", pageOrder = 1,
            imagePath = "res:battlemap_t4_8a_arasaka_portal", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 86L, updatedAt = 86L
        ),
        BattleMapDefinition(
            id = "builtin_t4_8b_arasaka_cavern", name = "T4-8b — غار تأسیسات آراساکا", district = "T4 • آراساکا • غار • تأسیسات • زیرزمینی",
            tier = 4, mapNumber = 8, tags = listOf("آراساکا", "غار", "زیرزمینی", "Arasaka", "Cavern"), mapSetId = "t4_8_arasaka", pageName = "غار تأسیسات آراساکا", pageOrder = 2,
            imagePath = "res:battlemap_t4_8b_arasaka_cavern", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 87L, updatedAt = 87L
        ),
        BattleMapDefinition(
            id = "builtin_t4_8c_arasaka_command", name = "T4-8c — مرکز فرماندهی آراساکا", district = "T4 • آراساکا • فرماندهی • کنترل • امنیتی",
            tier = 4, mapNumber = 8, tags = listOf("آراساکا", "فرماندهی", "کنترل", "Arasaka", "Command"), mapSetId = "t4_8_arasaka", pageName = "مرکز فرماندهی آراساکا", pageOrder = 3,
            imagePath = "res:battlemap_t4_8c_arasaka_command", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 88L, updatedAt = 88L
        ),
        BattleMapDefinition(
            id = "builtin_t4_8d_arasaka_reactor", name = "T4-8d — راکتور آراساکا", district = "T4 • آراساکا • راکتور • انرژی • تأسیسات",
            tier = 4, mapNumber = 8, tags = listOf("آراساکا", "راکتور", "انرژی", "Arasaka", "Reactor"), mapSetId = "t4_8_arasaka", pageName = "راکتور آراساکا", pageOrder = 4,
            imagePath = "res:battlemap_t4_8d_arasaka_reactor", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 89L, updatedAt = 89L
        ),
        BattleMapDefinition(
            id = "builtin_t4_9a_militech_perimeter", name = "T4-9a — محوطه پیرامونی میلی‌تک", district = "T4 • میلی‌تک • پیرامونی • نظامی • محوطه",
            tier = 4, mapNumber = 9, tags = listOf("میلی‌تک", "محوطه", "نظامی", "Militech", "Perimeter"), mapSetId = "t4_9_militech", pageName = "محوطه پیرامونی میلی‌تک", pageOrder = 1,
            imagePath = "res:battlemap_t4_9a_militech_perimeter", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 90L, updatedAt = 90L
        ),
        BattleMapDefinition(
            id = "builtin_t4_9b_militech_ops", name = "T4-9b — مرکز عملیات میلی‌تک", district = "T4 • میلی‌تک • عملیات • فرماندهی • نظامی",
            tier = 4, mapNumber = 9, tags = listOf("میلی‌تک", "عملیات", "فرماندهی", "Militech", "Ops"), mapSetId = "t4_9_militech", pageName = "مرکز عملیات میلی‌تک", pageOrder = 2,
            imagePath = "res:battlemap_t4_9b_militech_ops", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 91L, updatedAt = 91L
        ),
        BattleMapDefinition(
            id = "builtin_t4_9c_militech_training", name = "T4-9c — مرکز آموزش میلی‌تک", district = "T4 • میلی‌تک • آموزش • تمرین • نظامی",
            tier = 4, mapNumber = 9, tags = listOf("میلی‌تک", "آموزش", "تمرین", "Militech", "Training"), mapSetId = "t4_9_militech", pageName = "مرکز آموزش میلی‌تک", pageOrder = 3,
            imagePath = "res:battlemap_t4_9c_militech_training", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 92L, updatedAt = 92L
        ),
        BattleMapDefinition(
            id = "builtin_t4_9d_militech_bay", name = "T4-9d — آشیانه نظامی میلی‌تک", district = "T4 • میلی‌تک • آشیانه • خودرو زرهی • تعمیرات",
            tier = 4, mapNumber = 9, tags = listOf("میلی‌تک", "آشیانه", "خودرو زرهی", "Militech", "Bay"), mapSetId = "t4_9_militech", pageName = "آشیانه نظامی میلی‌تک", pageOrder = 4,
            imagePath = "res:battlemap_t4_9d_militech_bay", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 93L, updatedAt = 93L
        ),
        BattleMapDefinition(
            id = "builtin_t4_9e_militech_bunker", name = "T4-9e — پناهگاه زیرزمینی میلی‌تک", district = "T4 • میلی‌تک • پناهگاه • فرماندهی • زیرزمینی",
            tier = 4, mapNumber = 9, tags = listOf("میلی‌تک", "پناهگاه", "زیرزمینی", "Militech", "Bunker"), mapSetId = "t4_9_militech", pageName = "پناهگاه زیرزمینی میلی‌تک", pageOrder = 5,
            imagePath = "res:battlemap_t4_9e_militech_bunker", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 94L, updatedAt = 94L
        ),
        BattleMapDefinition(
            id = "builtin_t4_10a_shelter_entry", name = "T4-10a — ورودی پناهگاه", district = "T4 • پناهگاه • ورودی • زیرزمینی • امنیتی",
            tier = 4, mapNumber = 10, tags = listOf("پناهگاه", "ورودی", "زیرزمینی", "Shelter", "Entry"), mapSetId = "t4_10_shelter", pageName = "ورودی پناهگاه", pageOrder = 1,
            imagePath = "res:battlemap_t4_10a_shelter_entry", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 95L, updatedAt = 95L
        ),
        BattleMapDefinition(
            id = "builtin_t4_10b_shelter_habitation", name = "T4-10b — بخش مسکونی پناهگاه", district = "T4 • پناهگاه • مسکونی • خوابگاه • زیرزمینی",
            tier = 4, mapNumber = 10, tags = listOf("پناهگاه", "مسکونی", "خوابگاه", "Shelter", "Habitation"), mapSetId = "t4_10_shelter", pageName = "بخش مسکونی پناهگاه", pageOrder = 2,
            imagePath = "res:battlemap_t4_10b_shelter_habitation", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 96L, updatedAt = 96L
        ),
        BattleMapDefinition(
            id = "builtin_t4_10c_shelter_hydroponics", name = "T4-10c — بخش کشت هیدروپونیک پناهگاه", district = "T4 • پناهگاه • هیدروپونیک • کشاورزی • تأسیسات",
            tier = 4, mapNumber = 10, tags = listOf("پناهگاه", "هیدروپونیک", "کشاورزی", "Shelter", "Hydroponics"), mapSetId = "t4_10_shelter", pageName = "بخش کشت هیدروپونیک", pageOrder = 3,
            imagePath = "res:battlemap_t4_10c_shelter_hydroponics", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 97L, updatedAt = 97L
        ),
        BattleMapDefinition(
            id = "builtin_t4_10d_shelter_power", name = "T4-10d — بخش تأسیسات برق پناهگاه", district = "T4 • پناهگاه • برق • ژنراتور • تأسیسات",
            tier = 4, mapNumber = 10, tags = listOf("پناهگاه", "برق", "ژنراتور", "Shelter", "Power"), mapSetId = "t4_10_shelter", pageName = "بخش تأسیسات برق", pageOrder = 4,
            imagePath = "res:battlemap_t4_10d_shelter_power", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 98L, updatedAt = 98L
        ),
        BattleMapDefinition(
            id = "builtin_t4_10e_shelter_core", name = "T4-10e — هسته مرکزی پناهگاه", district = "T4 • پناهگاه • هسته • کنترل • امنیتی",
            tier = 4, mapNumber = 10, tags = listOf("پناهگاه", "هسته", "کنترل", "Shelter", "Core"), mapSetId = "t4_10_shelter", pageName = "هسته مرکزی پناهگاه", pageOrder = 5,
            imagePath = "res:battlemap_t4_10e_shelter_core", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 99L, updatedAt = 99L
        ),
        BattleMapDefinition(
            id = "builtin_t5_1a_militech_lobby", name = "T5-1a — لابی میلی‌تک", district = "T5 • میلی‌تک • لابی • نظامی • امنیت بالا",
            tier = 5, mapNumber = 1, tags = listOf("میلی‌تک", "لابی", "نظامی", "امنیت بالا", "Militech", "Lobby"), mapSetId = "t5_1_militech", pageName = "لابی میلی‌تک", pageOrder = 1,
            imagePath = "res:battlemap_t5_1a_militech_lobby", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 100L, updatedAt = 100L
        ),
        BattleMapDefinition(
            id = "builtin_t5_1b_militech_showroom", name = "T5-1b — نمایشگاه میلی‌تک", district = "T5 • میلی‌تک • نمایشگاه • تسلیحات • نظامی",
            tier = 5, mapNumber = 1, tags = listOf("میلی‌تک", "نمایشگاه", "تسلیحات", "Militech", "Showroom"), mapSetId = "t5_1_militech", pageName = "نمایشگاه میلی‌تک", pageOrder = 2,
            imagePath = "res:battlemap_t5_1b_militech_showroom", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 101L, updatedAt = 101L
        ),
        BattleMapDefinition(
            id = "builtin_t5_1c_militech_security", name = "T5-1c — بخش امنیت میلی‌تک", district = "T5 • میلی‌تک • امنیت • پادگان • نظامی",
            tier = 5, mapNumber = 1, tags = listOf("میلی‌تک", "امنیت", "پادگان", "Militech", "Security"), mapSetId = "t5_1_militech", pageName = "بخش امنیت میلی‌تک", pageOrder = 3,
            imagePath = "res:battlemap_t5_1c_militech_security", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 102L, updatedAt = 102L
        ),
        BattleMapDefinition(
            id = "builtin_t5_1d_militech_research", name = "T5-1d — بخش تحقیقات میلی‌تک", district = "T5 • میلی‌تک • تحقیقات • آزمایشگاه • فناوری",
            tier = 5, mapNumber = 1, tags = listOf("میلی‌تک", "تحقیقات", "آزمایشگاه", "Militech", "Research"), mapSetId = "t5_1_militech", pageName = "بخش تحقیقات میلی‌تک", pageOrder = 4,
            imagePath = "res:battlemap_t5_1d_militech_research", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 103L, updatedAt = 103L
        ),
        BattleMapDefinition(
            id = "builtin_t5_1e_militech_executive", name = "T5-1e — بخش مدیریت ارشد میلی‌تک", district = "T5 • میلی‌تک • مدیریت ارشد • فرماندهی • امنیت بالا",
            tier = 5, mapNumber = 1, tags = listOf("میلی‌تک", "مدیریت ارشد", "فرماندهی", "Militech", "Executive"), mapSetId = "t5_1_militech", pageName = "مدیریت ارشد میلی‌تک", pageOrder = 5,
            imagePath = "res:battlemap_t5_1e_militech_executive", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 104L, updatedAt = 104L
        ),
        BattleMapDefinition(
            id = "builtin_t5_1f_militech_boss", name = "T5-1f — مقر فرمانده میلی‌تک", district = "T5 • میلی‌تک • باس • فرمانده • نبرد نهایی",
            tier = 5, mapNumber = 1, tags = listOf("میلی‌تک", "فرمانده", "باس", "نبرد نهایی", "Militech", "Boss"), mapSetId = "t5_1_militech", pageName = "مقر فرمانده میلی‌تک", pageOrder = 6,
            imagePath = "res:battlemap_t5_1f_militech_boss", imageWidth = 1536, imageHeight = 711, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 105L, updatedAt = 105L
        ),
        BattleMapDefinition(
            id = "builtin_t5_2a_arasaka_tower_lobby", name = "T5-2a — لابی برج آراساکا", district = "T5 • آراساکا • برج • لابی • امنیت بالا",
            tier = 5, mapNumber = 2, tags = listOf("آراساکا", "برج", "لابی", "امنیت بالا", "Arasaka", "Tower", "Lobby"), mapSetId = "t5_2_arasaka_tower", pageName = "لابی برج آراساکا", pageOrder = 1,
            imagePath = "res:battlemap_t5_2a_arasaka_tower_lobby", imageWidth = 1536, imageHeight = 709, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 106L, updatedAt = 106L
        ),
        BattleMapDefinition(
            id = "builtin_t5_2b_arasaka_admin", name = "T5-2b — بخش اداری برج آراساکا", district = "T5 • آراساکا • برج • اداری • دفاتر",
            tier = 5, mapNumber = 2, tags = listOf("آراساکا", "برج", "اداری", "دفاتر", "Arasaka", "Admin"), mapSetId = "t5_2_arasaka_tower", pageName = "بخش اداری برج آراساکا", pageOrder = 2,
            imagePath = "res:battlemap_t5_2b_arasaka_admin", imageWidth = 1536, imageHeight = 709, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 107L, updatedAt = 107L
        ),
        BattleMapDefinition(
            id = "builtin_t5_2c_arasaka_garrison", name = "T5-2c — پادگان آراساکا", district = "T5 • آراساکا • پادگان • زرادخانه • امنیت بالا",
            tier = 5, mapNumber = 2, tags = listOf("آراساکا", "پادگان", "زرادخانه", "امنیت بالا", "Arasaka", "Garrison"), mapSetId = "t5_2_arasaka_tower", pageName = "پادگان آراساکا", pageOrder = 3,
            imagePath = "res:battlemap_t5_2c_arasaka_garrison", imageWidth = 1536, imageHeight = 709, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 108L, updatedAt = 108L
        ),
        BattleMapDefinition(
            id = "builtin_t5_2d_arasaka_biotech", name = "T5-2d — بخش بیوتک آراساکا", district = "T5 • آراساکا • بیوتک • آزمایشگاه • پزشکی",
            tier = 5, mapNumber = 2, tags = listOf("آراساکا", "بیوتک", "آزمایشگاه", "پزشکی", "Arasaka", "Biotech"), mapSetId = "t5_2_arasaka_tower", pageName = "بخش بیوتک آراساکا", pageOrder = 4,
            imagePath = "res:battlemap_t5_2d_arasaka_biotech", imageWidth = 1536, imageHeight = 709, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 109L, updatedAt = 109L
        ),
        BattleMapDefinition(
            id = "builtin_t5_2e_arasaka_data_fortress", name = "T5-2e — دژ داده آراساکا", district = "T5 • آراساکا • دژ داده • سرور • نت‌آرک • امنیت بالا",
            tier = 5, mapNumber = 2, tags = listOf("آراساکا", "دژ داده", "سرور", "نت آرک", "Arasaka", "Data Fortress"), mapSetId = "t5_2_arasaka_tower", pageName = "دژ داده آراساکا", pageOrder = 5,
            imagePath = "res:battlemap_t5_2e_arasaka_data_fortress", imageWidth = 1536, imageHeight = 709, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 110L, updatedAt = 110L
        ),
        BattleMapDefinition(
            id = "builtin_t5_2f_arasaka_executive", name = "T5-2f — بخش اجرایی آراساکا", district = "T5 • آراساکا • اجرایی • مدیریت • لوکس",
            tier = 5, mapNumber = 2, tags = listOf("آراساکا", "اجرایی", "مدیریت", "لوکس", "Arasaka", "Executive"), mapSetId = "t5_2_arasaka_tower", pageName = "بخش اجرایی آراساکا", pageOrder = 6,
            imagePath = "res:battlemap_t5_2f_arasaka_executive", imageWidth = 1536, imageHeight = 709, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 111L, updatedAt = 111L
        ),
        BattleMapDefinition(
            id = "builtin_t5_2g_arasaka_vault", name = "T5-2g — خزانه آراساکا", district = "T5 • آراساکا • خزانه • امنیت فوق‌سنگین • زیرزمین",
            tier = 5, mapNumber = 2, tags = listOf("آراساکا", "خزانه", "امنیت فوق سنگین", "Arasaka", "Vault"), mapSetId = "t5_2_arasaka_tower", pageName = "خزانه آراساکا", pageOrder = 7,
            imagePath = "res:battlemap_t5_2g_arasaka_vault", imageWidth = 1536, imageHeight = 709, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 112L, updatedAt = 112L
        ),
        BattleMapDefinition(
            id = "builtin_t5_2h_arasaka_adam_smasher_room", name = "T5-2h — اتاق آدام اسمشر — برج آراساکا", district = "T5 • آراساکا • آدام اسمشر • باس • نبرد نهایی",
            tier = 5, mapNumber = 2, tags = listOf("آراساکا", "آدام اسمشر", "باس", "نبرد نهایی", "Arasaka", "Adam Smasher", "Boss"), mapSetId = "t5_2_arasaka_tower", pageName = "اتاق آدام اسمشر", pageOrder = 8,
            imagePath = "res:battlemap_t5_2h_arasaka_adam_smasher_room", imageWidth = 1536, imageHeight = 709, calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, calibrated = true, gridVisible = true), createdAt = 113L, updatedAt = 113L
        )
    )

    private fun starterMap() = BattleMapDefinition(
        id = "builtin_warehouse_yard", name = "T1-1 — گاراژ متروکه", district = "T1 • گاراژ • متروکه • صنعتی",
        tier = 1, mapNumber = 1, tags = listOf("گاراژ", "متروکه", "صنعتی", "تعمیرگاه"),
        imagePath = "res:battlemap_t1_1_abandoned_garage", imageWidth = 1536, imageHeight = 1024,
        calibration = BattleMapCalibration(metersPerCell = 2f, cellSizePixels = 64f, originX = 0f, originY = 0f, calibrated = true, gridVisible = true),
        createdAt = 1L, updatedAt = 1L
    )

    companion object {
        /**
         * Two-point calibration. knownMeters is converted to cells using the CURRENT metersPerCell,
         * then the image-space cell size is stored independently. Changing scale later won't move the grid.
         */
        fun calibrate(map: BattleMapDefinition, ax: Float, ay: Float, bx: Float, by: Float, knownMeters: Float): BattleMapDefinition {
            require(knownMeters > 0f)
            val pixelDistance = max(abs(bx - ax), abs(by - ay))
            require(pixelDistance >= 2f)
            val metersPerCell = map.calibration.metersPerCell.coerceAtLeast(.25f)
            val knownCells = knownMeters / metersPerCell
            require(knownCells > 0f)
            return map.copy(calibration = map.calibration.copy(
                cellSizePixels = pixelDistance / knownCells,
                pixelsPerMeter = 0f,
                originX = ax,
                originY = ay,
                calibrated = true
            ))
        }

        fun updateScale(map: BattleMapDefinition, metersPerCell: Float): BattleMapDefinition =
            map.copy(calibration = map.calibration.copy(metersPerCell = metersPerCell.coerceIn(.25f, 100f)))

        fun updateGridGeometry(
            map: BattleMapDefinition,
            cellSizePixels: Float,
            opacity: Float = map.calibration.gridOpacity,
            originX: Float = map.calibration.originX,
            originY: Float = map.calibration.originY,
            gridColorArgb: Long = map.calibration.gridColorArgb
        ): BattleMapDefinition = map.copy(calibration = map.calibration.copy(
            cellSizePixels = cellSizePixels.coerceIn(12f, 500f),
            pixelsPerMeter = 0f,
            originX = originX, originY = originY,
            calibrated = true,
            gridOpacity = opacity.coerceIn(.05f, .8f),
            gridColorArgb = gridColorArgb
        ))

        fun snapToken(map: BattleMapDefinition, token: BattleMapToken, imageX: Float, imageY: Float): BattleMapToken {
            val cal = map.calibration
            if (!cal.calibrated || cal.pixelsPerCell <= 0f) return token.copy(x = imageX, y = imageY, gridColumn = null, gridRow = null)
            return if (token.isEnvironment) {
                val (col, row) = cal.intersectionForPoint(imageX, imageY)
                val (px, py) = cal.pointOfIntersection(col, row)
                token.copy(x = px, y = py, gridColumn = col, gridRow = row)
            } else {
                val (col, row) = cal.cellForPoint(imageX, imageY)
                val (cx, cy) = cal.centerOfCell(col, row)
                token.copy(x = cx, y = cy, gridColumn = col, gridRow = row)
            }
        }
    }
}
