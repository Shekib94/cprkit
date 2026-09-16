package com.cyberpunk.gmtool.data



enum class VehicleDomain { LAND, SEA, AIR }

data class VehicleSpec(
    val name: String,
    val domain: VehicleDomain,
    val familyRank: Int,
    val sdp: Int,
    val seats: Int,
    val combatMove: Int,
    val narrativeMph: Int,
    val narrativeKph: Int,
    val basePrice: Int,
    val description: String,
    val tags: Set<String> = emptySet(),
    val pricingNote: String = ""
)

data class VehicleUpgrade(
    val name: String,
    val rankRequired: Int,
    val description: String,
    val repeatable: Boolean = false,
    val eligible: (VehicleSpec) -> Boolean = { true },
    val prerequisite: String? = null
)

object VehicleCatalog {
    val vehicles = listOf(
        VehicleSpec("Roadbike", VehicleDomain.LAND, 1, 35, 2, 20, 100, 161, 20000, "موتورسیکلت CHOOH² رایج.", setOf("BIKE")),
        VehicleSpec("Superbike", VehicleDomain.LAND, 7, 35, 2, 60, 300, 483, 100000, "موتورسیکلت خیابانی Exotic با سرعت بسیار بالا.", setOf("BIKE")),
        VehicleSpec("Compact Groundcar", VehicleDomain.LAND, 1, 50, 4, 20, 100, 161, 30000, "خودروی زمینی CHOOH² رایج.", setOf("GROUNDCAR")),
        VehicleSpec("High Performance Groundcar", VehicleDomain.LAND, 5, 50, 4, 40, 200, 322, 50000, "خودروی اسپرت CHOOH² با کارایی بالا.", setOf("GROUNDCAR")),
        VehicleSpec("Super Groundcar", VehicleDomain.LAND, 9, 50, 2, 60, 300, 483, 100000, "خودروی اسپرت Exotic با سرعت بسیار بالا.", setOf("GROUNDCAR")),
        VehicleSpec("Jetski", VehicleDomain.SEA, 1, 35, 2, 20, 60, 97, 20000, "شناور شخصی CHOOH².", setOf("JETSKI")),
        VehicleSpec("Speedboat", VehicleDomain.SEA, 5, 50, 4, 20, 60, 97, 30000, "قایق تندروی CHOOH².", setOf("BOAT")),
        VehicleSpec("Cabin Cruiser", VehicleDomain.SEA, 7, 60, 4, 10, 15, 24, 60000, "قایق لوکس با اتاق‌های قابل سفارشی‌سازی؛ حداقل دو اتاق و دو صندلی برای هر اتاق.", setOf("BOAT", "ROOM_VEHICLE"), "30,000eb per room • minimum 2 rooms"),
        VehicleSpec("Yacht", VehicleDomain.SEA, 9, 100, 16, 10, 15, 24, 200000, "کشتی تفریحی لوکس با اتاق‌های سفارشی؛ حداقل چهار اتاق و چهار صندلی برای هر اتاق.", setOf("BOAT", "ROOM_VEHICLE"), "50,000eb per room • minimum 4 rooms"),
        VehicleSpec("Gyrocopter", VehicleDomain.AIR, 1, 35, 2, 20, 100, 161, 20000, "روتورکرافت کوچک CHOOH².", setOf("GYRO")),
        VehicleSpec("Helicopter", VehicleDomain.AIR, 5, 60, 4, 40, 200, 322, 40000, "هلیکوپتر کامل CHOOH².", setOf("HELICOPTER")),
        VehicleSpec("AV-4 Multipurpose Aerodyne", VehicleDomain.AIR, 7, 100, 6, 40, 200, 322, 50000, "Aerodyne چندمنظوره با رانش عمودی.", setOf("AV4")),
        VehicleSpec("AV-9 Super Aerodyne", VehicleDomain.AIR, 9, 60, 2, 60, 300, 483, 100000, "Aerodyne Exotic با سرعت بسیار بالا.", setOf("AV9")),
        VehicleSpec("Aerozep", VehicleDomain.AIR, 9, 100, 4, 20, 100, 161, 60000, "کشتی هوایی باری با اتاق‌های سفارشی؛ حداقل دو اتاق و دو صندلی برای هر اتاق.", setOf("AEROZEP", "ROOM_VEHICLE"), "30,000eb per room • minimum 2 rooms")
    )

    fun byName(name: String): VehicleSpec? = vehicles.firstOrNull { it.name.equals(name, true) }
    fun availableForMoto(rank: Int): List<VehicleSpec> = vehicles.filter { it.familyRank <= rank.coerceIn(1, 10) }

    val upgrades = listOf(
        VehicleUpgrade("Armored Chassis", 5, "SP13 به بدنه می‌دهد؛ شیشه را زرهی نمی‌کند."),
        VehicleUpgrade("Bulletproof Glass", 1, "شیشه‌ها را Thin Bulletproof Glass با 15 HP می‌کند؛ بار دوم 30 HP. هر پنجره HP جدا دارد.", repeatable = true),
        VehicleUpgrade("Communications Center", 1, "کنسول Agent و مجموعه ارتباطی/ضدشنود/ردیابی و ضبط صوت داخل وسیله."),
        VehicleUpgrade("NOS", 1, "با Action یک Move Action اضافه هنگام رانندگی می‌دهد؛ هر مخزن روزی یک‌بار. قابل تکرار.", repeatable = true),
        VehicleUpgrade("Onboard Flamethrower", 1, "Flamethrower ثابت بیرونی؛ راننده با Action شلیک می‌کند و هنگام حرکت Reload نمی‌شود.", repeatable = true),
        VehicleUpgrade("Onboard Machine gun", 1, "Assault Rifle ثابت 30تیر فقط Autofire؛ راننده با Action شلیک می‌کند.", repeatable = true),
        VehicleUpgrade("Seating Upgrade", 1, "دو صندلی اضافه می‌کند؛ روی Bike/Jetski/Gyrocopter قابل تکرار نیست/مجاز نیست.", repeatable = true, eligible = { it.tags.none { t -> t in setOf("BIKE", "JETSKI", "GYRO") } }),
        VehicleUpgrade("Security Upgrade", 5, "قفل‌های بیومتریک DV17 و قابلیت cloak در حالت توقف پس از یک دقیقه؛ دیدن آن DV17 Perception."),
        VehicleUpgrade("Smuggling Upgrade", 1, "دو Hidden Holster و یک فضای بزرگ قاچاق با DV17 Conceal/Reveal؛ برای Bike/Jetski/Gyrocopter مجاز نیست.", repeatable = true, eligible = { it.tags.none { t -> t in setOf("BIKE", "JETSKI", "GYRO") } }),
        VehicleUpgrade("Heavy Chassis", 1, "+20 SDP و توان یدک‌کشی تا 10 تن؛ برای Bike/Jetski/Gyrocopter مجاز نیست.", eligible = { it.tags.none { t -> t in setOf("BIKE", "JETSKI", "GYRO") } }),
        VehicleUpgrade("Onboard Rocket Pod", 5, "Rocket Launcher ثابت با drum سه‌راکتی؛ راننده با Action شلیک می‌کند.", repeatable = true, eligible = { it.tags.none { t -> t in setOf("BIKE", "JETSKI", "GYRO") } }, prerequisite = "Heavy Chassis"),
        VehicleUpgrade("Vehicle Heavy Weapon Mount", 5, "یک صندلی را به mount گردان برای سلاح دو‌دستی تبدیل می‌کند؛ بار اول Family یک Heavy Weapon مشخص هدیه می‌دهد.", repeatable = true, eligible = { it.tags.none { t -> t in setOf("BIKE", "JETSKI", "GYRO") } }, prerequisite = "Heavy Chassis"),
        VehicleUpgrade("Onboard Melee Weapon", 1, "Very Heavy Melee Weapon ثابت بیرونی برای Land/Sea؛ راننده با Action حمله می‌کند.", repeatable = true, eligible = { it.domain == VehicleDomain.LAND || it.domain == VehicleDomain.SEA }),
        VehicleUpgrade("Hover Upgrade", 5, "وسیله زمینی را قادر می‌کند روی سطح آب با سرعت Cabin Cruiser حرکت کند.", eligible = { it.domain == VehicleDomain.LAND }),
        VehicleUpgrade("AV-4 Engine Upgrade", 7, "به وسیله زمینی قابلیت پرواز می‌دهد؛ در هوا مثل AV-4 حرکت و با Pilot Air Vehicle کنترل می‌شود.", eligible = { it.domain == VehicleDomain.LAND }),
        VehicleUpgrade("Combat Plow", 1, "در ram از جلو وسیله و سرنشینان damage/Whiplash نمی‌گیرند؛ با NOS، ram +2d6.", eligible = { (it.domain == VehicleDomain.LAND || it.domain == VehicleDomain.SEA) && it.tags.none { t -> t in setOf("BIKE", "JETSKI") } }),
        VehicleUpgrade("Enhanced Interface Plug Integration", 5, "فقط Bike؛ با Interface Plugs و REF8+ امکان Evasion برای حملات قابل dodge علیه موتور/راننده/مسافر.", eligible = { "BIKE" in it.tags }),
        VehicleUpgrade("Deployable Spike Strip", 1, "فقط Groundcar؛ trailing vehicle باید DV17 Drive Land Vehicle بدهد یا 4d6 به weak point بگیرد.", repeatable = true, eligible = { "GROUNDCAR" in it.tags }),
        VehicleUpgrade("Housing Capacity", 1, "برای Groundcar/AV-4 یک Kombi با تخت/توالت/دوش/آشپزخانه می‌سازد؛ برای Cabin/Yacht/Aerozep یک اتاق اضافه.", eligible = { "GROUNDCAR" in it.tags || "AV4" in it.tags || "BOAT" in it.tags && it.name in setOf("Cabin Cruiser", "Yacht") || "AEROZEP" in it.tags }, prerequisite = null)
    )

    fun upgradesFor(vehicle: VehicleSpec, rank: Int, installed: List<String>): List<VehicleUpgrade> = upgrades.filter { up ->
        if (up.rankRequired > rank || !up.eligible(vehicle)) return@filter false
        if (!up.repeatable && installed.any { it.equals(up.name, true) }) return@filter false
        if (up.name == "Bulletproof Glass" && installed.count { it.equals(up.name, true) } >= 2) return@filter false
        if (up.name == "Housing Capacity" && vehicle.name in setOf("Compact Groundcar", "High Performance Groundcar") && installed.none { it.equals("Heavy Chassis", true) }) return@filter false
        // Core ص۱۶۴: نصب چندباره‌ی Heavy Weapon Mount فقط روی وسیله‌های بزرگ
        // (Cabin Cruiser / Yacht / Aerozep) یا Groundcarِ دارای Housing Capacity مجاز است.
        if (up.name == "Vehicle Heavy Weapon Mount" && installed.any { it.equals(up.name, true) }) {
            val bigPlatform = vehicle.name in setOf("Cabin Cruiser", "Yacht") || "AEROZEP" in vehicle.tags
            val roomyGroundcar = "GROUNDCAR" in vehicle.tags && installed.any { it.equals("Housing Capacity", true) }
            if (!bigPlatform && !roomyGroundcar) return@filter false
        }
        if (up.prerequisite != null && installed.none { it.equals(up.prerequisite, true) }) return@filter false
        true
    }

    val storeItems: List<StoreItem> = vehicles.map { v ->
        StoreItem(
            name = v.name,
            basePrice = v.basePrice,
            category = "Vehicles",
            description = buildString {
                append(v.description)
                append(" SDP ${v.sdp}، Seats ${v.seats}، Combat MOVE ${v.combatMove}، سرعت روایی ${v.narrativeMph} MPH / ${v.narrativeKph} KPH.")
                if (v.pricingNote.isNotBlank()) append(" قیمت: ${v.pricingNote}؛ عدد فروشگاه حداقل پیکربندی قابل خرید را نشان می‌دهد.")
            },
            subtitle = gtr("%1s • SDP %2s • Seats %3s • MOVE %4s%5s", v.domain.name, v.sdp, v.seats, v.combatMove, if (v.familyRank > 0) " • Moto ${v.familyRank}+" else "")
        )
    }
}
