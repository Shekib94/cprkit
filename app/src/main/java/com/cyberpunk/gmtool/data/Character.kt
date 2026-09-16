package com.cyberpunk.gmtool.data

import java.io.Serializable

// ============================================================
// مدل اصلی کاراکتر — منبع واحد حقیقت برای همه چیز
// (هر چیزی که کاربر ویرایش می‌کند باید در همین کلاس ذخیره شود)
// ============================================================
data class Character(
    val id: Int = 0,
    val name: String = "Unknown",
    val handle: String = "Unknown",            // لقب خیابانی
    val avatarUrl: String = "",                // لینک عکس (فعلا خالی، از ImageProvider استفاده می‌شود)
    val role: String = "Solo",                 // نقش: Solo, Netrunner, ...
    val creationMethod: String = "Streetrat Template",
    /**
     * شناسه‌ی پایدار برگه در همه‌ی دستگاه‌ها.
     *
     * چرا لازم شد: `id` یک شماره‌ی محلیِ هر گوشی است، پس در سشن LAN دو گوشی
     * برای یک شخصیتِ واحد id متفاوت دارند و «آیا این برگه را دارم؟» با id
     * جواب‌دادنی نبود — نتیجه‌اش همان باگِ سر میز بود: هر اسکن یک شخصیت
     * تکراری می‌ساخت. uid یک بار هنگام ساخت برگه ساخته می‌شود (در
     * `GameRules.syncDerived`)، با Export/Import و با شبکه جابه‌جا می‌شود و
     * هرگز عوض نمی‌شود؛ معیار اصلیِ «همان برگه» بودن همین است.
     */
    val sheetUid: String = "",

    // --- پیشرفت ---
    val improvementPoints: Int = 0,            // امتیاز ارتقای آزاد (IP)
    val roleRank: Int = 4,                     // رتبه‌ی قابلیت نقش (Rank)
    val reputation: Int = 0,

    // --- سلامت و منابع ---
    val hp: Int = 40,
    val maxHp: Int = 40,
    val headArmorSp: Int = 11,                 // SP زره سر
    val bodyArmorSp: Int = 11,                 // SP زره بدن
    val currentHumanity: Int = 50,
    val maxHumanity: Int = 50,
    val baseEmp: Int = 5,                    // EMP طبیعی پیش از Humanity Loss
    val baseBody: Int = 0,                   // BODY طبیعی پیش از Grafted Muscle/Linear Frame؛ 0 برای migration
    val currentLuck: Int = 6,                  // امتیاز شانس مصرف‌شونده (پر می‌شود در جلسه بعد)
    val maxLuck: Int = 6,
    val eurodollars: Int = 500,
    // Complete Package also receives a separate 800eb fashion budget during creation.
    val fashionBudget: Int = 0,
    val deathSavePenalty: Int = 0,
    val isDead: Boolean = false,
    val addictions: List<String> = emptyList(),
    val addictionRelapseUntil: Map<String, Long> = emptyMap(), // legacy field kept for save compatibility
    val addictionRelapseRisk: List<String> = emptyList(),      // one in-game year after successful addiction therapy
    // Street Drug state. Value is remaining Primary Effect duration in in-game hours.
    val activeDrugEffects: Map<String, Int> = emptyMap(),
    // Temporary Humanity held by effects such as Black Lace. Returned only when its Secondary Check succeeds.
    val drugHumanityHeld: Map<String, Int> = emptyMap(),
    // Temporary one-hour stat penalties caused by effects such as Liche/Scorpion. Values are penalty magnitudes.
    val temporaryStatPenalties: Map<String, Int> = emptyMap(),
    val temporaryStatPenaltyHours: Int = 0,
    val therapyWeeksSpent: Int = 0,
    val criticalInjuries: List<String> = emptyList(), // keys from CriticalInjuries
    // Temporary combat effects. Value = remaining personal turns; -1 = until explicitly cleared.
    val combatEffects: Map<String, Int> = emptyMap(),
    val appliedTransactionIds: List<String> = emptyList(), // idempotency ledger for rewards/loot
    val nomadMotorpool: List<String> = emptyList(),
    val nomadVehicleUpgrades: List<String> = emptyList(),
    val pendingFamilyVehicleId: String? = null, // swap requested; becomes available the following in-game morning
    val ownedVehicles: List<OwnedVehicle> = emptyList(),

    val isAlly: Boolean = true,

    // --- مخصوص NPC ---
    val npcCategory: String = "",       // مثلاً "Gang" / "Cyberpsycho" / "Lawman Backup" / "Exec Team" / "Boss"
    val npcTier: String = "",           // "Easy" / "Medium" / "Hard" / "Very Hard"
    val notes: String = "",             // توضیحات اضافی / رفتار مبارزه

    val stats: Stats = Stats(),
    val skills: List<SkillData> = emptyList(), // مهارت‌های مخصوص همین کاراکتر

    val weapons: List<Weapon> = emptyList(),   // سلاح‌های کاراکتر
    val inventory: List<InventoryItem> = emptyList(), // تجهیزات/آیتم‌های خریداری‌شده
    val lifepath: LifepathData = LifepathData(),

    // --- امتیازات تخصیص‌یافته به قابلیت‌های نقش (مثلا Combat Awareness سولو) ---
    val roleAbilityPoints: Map<String, Int> = emptyMap(),
    // Core Pharmaceuticals unlocked by the Medtech; one distinct choice per Pharmaceuticals point.
    val medtechPharmaceuticals: List<String> = emptyList(),
    val medtechPatients: List<MedtechPatient> = emptyList(),
    // Persistent investigation board for Media characters (GM helper; does not replace Credibility rules).
    val mediaCases: List<MediaCase> = emptyList(),
    // Persistent GM-facing Nomad Family helper. Narrative fields are GM aids, not extra Core mechanics.
    val nomadFamilyProfile: NomadFamilyProfile? = null,
    val soloCombatState: SoloCombatState? = null,

    val description: String = "",

    // --- لایف‌استایل و مسکن ---
    val lifestyle: String = "Kibble",       // سبک زندگی
    val housing: String = "Cargo Container",// محل سکونت
    val monthlyRent: Int = 1000,            // اجاره‌ی ماهانه (eb)

    // --- اطلاعات گوشی (Agent) ---
    val phoneNotes: List<PhoneNote> = emptyList(),
    val objectives: List<Objective> = emptyList(),
    val contacts: List<Contact> = emptyList()
) : Serializable

data class PhoneNote(val id: Long = 0, val title: String = "", val body: String = "") : Serializable
data class Objective(val id: Long = 0, val title: String = "", val type: String = "Task", val body: String = "", val done: Boolean = false) : Serializable
data class Contact(val id: Long = 0, val name: String = "", val detail: String = "") : Serializable

// Medtech/Media campaign helpers. Defaults keep older saves compatible.


/** Narrative campaign helper for a Nomad's Family/Pack.
 *  Fields such as relationship/favor/debt are GM tracking aids and are not extra Core stats.
 */
/**
 * تابلوی رزم سولو — وضعیت زنده‌ای که سر میز عوض می‌شود.
 * برخلاف تخصیص امتیاز که یک‌بار انجام می‌شود، این‌ها در طول درگیری تغییر می‌کنند.
 */
data class SoloCombatState(
    /** آیا Interface/آماده‌باش رزمی این صحنه فعال شده */
    val onAlert: Boolean = false,
    /** تعداد دفعاتی که Damage Deflection این صحنه استفاده شده (برای یادآوری GM) */
    val deflectionUsed: Int = 0,
    /** هدف‌های شناسایی‌شده با Spot Weakness — نام و یادداشت ضعف */
    val markedTargets: List<String> = emptyList(),
    /** یادداشت وضعیت صحنه: پناه، موقعیت، تهدید */
    val sceneNote: String = "",
    /** شمارندهٔ کشته/ازکارافتاده در این صحنه */
    val takedowns: Int = 0
) : Serializable

data class NomadFamilyProfile(
    val familyName: String = "",
    val packSize: String = "",
    val domain: String = "Land",
    val primaryBusiness: String = "Cargo transport",
    val currentLocation: String = "",
    val relationship: String = "Normal",
    val favor: Int = 0,
    val debt: Int = 0,
    val currentProblem: String = "",
    val importantNpcs: String = "",
    val lastInteraction: String = ""
) : Serializable

/**
 * بیمار تحت درمان Medtech — وضعیت زنده‌ای که بین جلسات می‌ماند.
 * Surgery و Cryo در RED چندجلسه‌ای‌اند، پس GM باید یادش بماند کی روی تخت است.
 */
data class MedtechPatient(
    val id: Long = 0L,
    val name: String = "",
    /**
     * اگر این بیمار یکی از کاراکترهای ثبت‌شده باشد، id او همین‌جاست.
     * با این اتصال، HP و Critical Injuryها مستقیماً از همان برگه خوانده
     * می‌شوند (منبع حقیقت یکی است) و بعد از درمان هم روی همان برگه اعمال می‌شود.
     */
    val linkedCharacterId: Int? = null,
    /** Stabilized / Surgery / Cryo / Recovering / Critical */
    val state: String = "Critical",
    /** آسیب فعلی: زخم جدی، اندام ازدست‌رفته و... */
    val injury: String = "",
    /** تعداد Surgery Check موفق تا اینجا */
    val surgerySuccess: Int = 0,
    /** آیا در Cryotank است */
    val inCryo: Boolean = false,
    /** روزهای باقی‌ماندهٔ نقاهت */
    val daysLeft: Int = 0,
    val note: String = ""
) : Serializable

data class MediaCase(
    val id: Long = 0L,
    val title: String = "",
    val topic: String = "",
    val rumorLevel: String = "Vague",
    val sources: String = "",
    val evidence: String = "",
    val evidenceCount: Int = 0,
    val status: String = "Investigating",
    val published: Boolean = false
) : Serializable

// استات‌های ده‌گانه (به ترتیب رسمی روی شیت: INT REF DEX TECH COOL WILL LUCK MOVE BODY EMP)
data class Stats(
    val int: Int = 6,
    val ref: Int = 6,
    val dex: Int = 6,
    val tech: Int = 6,
    val cool: Int = 6,
    val will: Int = 6,
    val luck: Int = 6,
    val move: Int = 6,
    val body: Int = 6,
    val emp: Int = 6
) : Serializable


// Persistent vehicle instance: purchased vehicles and Family Motorpool loans share the same combat/repair state.
data class OwnedVehicle(
    val id: String = "",
    val specName: String,
    val customName: String = "",
    val source: String = "PURCHASED", // PURCHASED / FAMILY
    val currentSdp: Int,
    val upgrades: List<String> = emptyList(),
    val active: Boolean = false,               // UI-selected vehicle; not the Family Motorpool checkout state
    val familyCheckedOut: Boolean = false,     // only meaningful for source == FAMILY
    val destroyedUntil: Long = 0L              // Family repair days remaining (0 = available); kept Long for save compatibility
) : Serializable

// آیتم موجودی (تجهیزات خریداری‌شده از فروشگاه یا تجهیزات اولیه)
data class InventoryItem(
    val name: String,
    val category: String = "Utilities",  // Ammo/Armor/Clothing/Cyberware/Utilities/Vehicles/Weapon
    val sp: Int? = null,
    val currentSp: Int? = null,            // legacy/migration field for single-location armor
    val currentHeadSp: Int? = null,        // independent SP by hit location (Bodyweight/Skin Weave/Subdermal need both)
    val currentBodySp: Int? = null,
    val quantity: Int = 1,
    val equipped: Boolean = false,
    // Cyberware installation state. Purchased cyberware may stay in inventory uninstalled.
    val instanceId: String = "",
    val installedIn: String? = null,       // instanceId of a foundational piece (Cybereye/Cyberarm/etc.)
    val cyberwareInstallCount: Int = 0,
    val humanityLossPaid: Int = 0          // lifetime HL paid by this physical piece; never refunded on removal
) : Serializable

// داستان پس‌زمینه
data class LifepathData(
    var culturalOrigins: String = "",
    var personality: String = "",
    var clothingStyle: String = "",
    var hairstyle: String = "",
    var affectation: String = "",
    var valueMost: String = "",
    var feelingsAboutPeople: String = "",
    var valuedPerson: String = "",
    var valuedPossession: String = "",
    var familyBackground: String = "",
    var childhoodEnvironment: String = "",
    var familyCrisis: String = "",
    var lifeGoals: String = "",
    var friends: String = "None",
    var enemies: String = "None",
    var tragicLoveAffairs: String = "None",
    var roleLifepath: String = ""
) : Serializable
