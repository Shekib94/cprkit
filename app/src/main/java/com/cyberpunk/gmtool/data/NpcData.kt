package com.cyberpunk.gmtool.data

/**
 * کتابخانه‌ی NPCهای آماده (Pregenerated) — محتوای سفارشی GM پروژه.
 * دسته‌بندی سختی و ساختار شیت برای اجرای سریع Cyberpunk RED طراحی شده است.
 *
 * مقادیر skill در اینجا به‌صورت **جمعِ کلِ تاس رزولوشن** (STAT + Level) ذخیره می‌شوند،
 * چون شیت ساده‌شده‌ی NPC فقط دو تب دارد (Name & Gear / Stats & Skills) و اعدادِ
 * درشت همان چیزی است که GM هنگام بازی نیاز دارد (مثل اپ رسمی Companion).
 */

enum class NpcDifficulty(val label: String, val color: Long) {
    EASY("آسان (Easy)", 0xFF4CAF50),
    MEDIUM("معمولی (Medium)", 0xFFFFC107),
    HARD("سخت (Hard)", 0xFFFB8C00),
    VERY_HARD("خیلی سخت (Very Hard)", 0xFFD32F2F)
}

data class NpcTemplate(
    val name: String,
    val category: String,          // Gang / Corporate / Cyberpsycho / Lawman Backup / Exec Team / Boss
    val difficulty: NpcDifficulty,
    val role: String = "Solo",
    val stats: Stats = Stats(),
    val skillTotals: Map<String, Int> = emptyMap(),  // عدد درشتِ هر مهارت (STAT+level)
    val gear: List<String> = emptyList(),
    val headArmorSp: Int = 0,
    val bodyArmorSp: Int = 0,
    val description: String = "",
    // متن‌های کوتاه BIO (به‌صورت پیش‌فرض در شیت NPC نمایش داده می‌شوند)
    val personality: String = "",
    val motivation: String = "",
    val identifying: String = ""
)

object NpcData {

    // ---------------- دسته‌بندی‌ها (برای فیلتر) ----------------
    const val CAT_GANG = "گنگ (Gang)"
    const val CAT_CORPORATE = "شرکتی (Corporate)"
    const val CAT_MILITARY = "نظامی/مزدور (Military)"
    const val CAT_CYBERPSYCHO = "سایبرسایکو (Cyberpsycho)"
    const val CAT_LAWMAN = "بک‌آپ لاومن (Lawman Backup)"
    const val CAT_EXEC = "تیم اگزک (Exec Team)"
    const val CAT_BOSS = "غول/باس (Boss)"

    val categories = listOf(
        CAT_GANG, CAT_CORPORATE, CAT_MILITARY, CAT_CYBERPSYCHO, CAT_LAWMAN, CAT_EXEC, CAT_BOSS
    )

    // ============================================================
    //  بخش ۱ — NPCهای رده‌بندی‌شده بر اساس سختی
    // ============================================================

    private val gang = listOf(
        NpcTemplate(
            "Maelstrom Initiate", CAT_GANG, NpcDifficulty.EASY, role = "Solo",
            stats = Stats(5, 6, 6, 5, 5, 5, 5, 6, 6, 5),
            skillTotals = mapOf("Autofire" to 8, "Handgun" to 8, "Brawling" to 8, "Melee Weapon" to 8,
                "Evasion" to 10, "Perception" to 7, "Concentration" to 7, "Athletics" to 8),
            gear = listOf("Light Pistol", "Heavy Melee Weapon", "Light Armorjack Body Armor (SP11)",
                "Light Armorjack Head Armor (SP11)", "Chemskin", "Interface Plugs"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "تازه‌وارد مایل‌استروم؛ تهاجمی اما بی‌تجربه. با گروه حمله می‌کند و در نبرد تن‌به‌تن خطا می‌دهد."
        ),
        NpcTemplate(
            "Maelstrom Crusher", CAT_GANG, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(5, 7, 7, 5, 6, 7, 5, 6, 8, 4),
            skillTotals = mapOf("Autofire" to 10, "Brawling" to 12, "Melee Weapon" to 11,
                "Evasion" to 12, "Perception" to 9, "Concentration" to 9, "Athletics" to 11, "Shoulder Arms" to 10),
            gear = listOf("Shotgun", "Heavy Melee Weapon", "Grafted Muscle and Bone Lace",
                "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)", "Cyberarm"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "بازوی سایبر و عضلات پیوندی؛ به نبرد نزدیک تمایل دارد و ضربات سنگین می‌زند."
        ),
        NpcTemplate(
            "6th Street Gunner", CAT_GANG, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 8, 6, 6, 6, 6, 5, 6, 6, 6),
            skillTotals = mapOf("Autofire" to 12, "Shoulder Arms" to 12, "Handgun" to 10,
                "Evasion" to 10, "Perception" to 9, "Concentration" to 9, "Tactics" to 9),
            gear = listOf("Assault Rifle", "Heavy Pistol", "Basic Rifle Ammunition x70",
                "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)", "Neural Link", "Sandevistan"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "کهنه‌سرباز خیابانی؛ ساندویستان فعال می‌کند و رگبار دقیق می‌بندد."
        ),
        NpcTemplate(
            "Valentinos Enforcer", CAT_GANG, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 7, 7, 5, 7, 6, 6, 7, 7, 6),
            skillTotals = mapOf("Handgun" to 12, "Brawling" to 11, "Melee Weapon" to 11,
                "Evasion" to 11, "Persuasion" to 10, "Perception" to 9, "Athletics" to 10),
            gear = listOf("Heavy Pistol", "Very Heavy Pistol", "Heavy Melee Weapon",
                "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)", "Subdermal Pocket"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "مرد خانواده؛ کاریزماتیک و در نبرد خیابانی بی‌رحم."
        ),
        NpcTemplate(
            "Tyger Claw Netrunner", CAT_GANG, NpcDifficulty.HARD, role = "Netrunner",
            stats = Stats(8, 7, 6, 8, 7, 7, 5, 6, 5, 6),
            skillTotals = mapOf("Concentration" to 14, "Electronics/Security Tech (x2)" to 13,
                "Cryptography" to 12, "Cybertech" to 11, "Handgun" to 9, "Perception" to 10, "Hacking" to 14),
            gear = listOf("Cyberdeck (7 Slots)", "Interface Plugs", "Neural Link", "Light Pistol",
                "Light Armorjack Body Armor (SP11)", "Program: Sword", "Program: Worm"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "در نت می‌کشد؛ قبل از ورود فیزیکی، سایبرویر قفل‌ها و دوربین‌ها را دستکاری می‌کند."
        ),
        NpcTemplate(
            "Animals Berserker", CAT_GANG, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(5, 7, 7, 5, 7, 8, 5, 6, 10, 3),
            skillTotals = mapOf("Brawling" to 16, "Melee Weapon" to 14, "Evasion" to 13,
                "Concentration" to 12, "Athletics" to 15, "Resist Torture/Drugs" to 14, "Perception" to 10),
            gear = listOf("Heavy Melee Weapon", "Grafted Muscle and Bone Lace", "Cyberleg", "Cyberleg",
                "Body Armor (SP13)", "Head Armor (SP13)", "Smash (Combat Drug) x2"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "غول بیومنطقی؛ زیر ضربات داروی اسمش، ۸ بدن و مشت‌های مرگبار. زره سنگین."
        ),
        NpcTemplate(
            "Arasaka Security Guard", CAT_CORPORATE, NpcDifficulty.EASY, role = "Solo",
            stats = Stats(5, 6, 6, 5, 5, 5, 5, 6, 6, 5),
            skillTotals = mapOf("Handgun" to 9, "Autofire" to 8, "Perception" to 9,
                "Concentration" to 7, "Evasion" to 9, "Brawling" to 8),
            gear = listOf("Heavy Pistol", "SMG", "Light Armorjack Body Armor (SP11)",
                "Light Armorjack Head Armor (SP11)", "Radio Communicator"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "نگهبان شیفت؛ طبق پروتکل عمل می‌کند و در نبرد گروهی پایدار است."
        ),
        NpcTemplate(
            "Arasaka Security Officer", CAT_CORPORATE, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 8, 6, 6, 7, 6, 6, 6, 7, 6),
            skillTotals = mapOf("Autofire" to 12, "Handgun" to 12, "Shoulder Arms" to 11,
                "Tactics" to 11, "Perception" to 11, "Concentration" to 10, "Evasion" to 11),
            gear = listOf("Assault Rifle", "Very Heavy Pistol", "Body Armor (SP13)",
                "Head Armor (SP13)", "Neural Link", "Sandevistan", "Radio Communicator x4"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "افسر امنیتی؛ تیم ۴ نفره را هدایت و از ساندویستان برای واکنش برتر استفاده می‌کند."
        ),
        NpcTemplate(
            "Zetatech Netrunner", CAT_CORPORATE, NpcDifficulty.HARD, role = "Netrunner",
            stats = Stats(8, 7, 6, 9, 7, 7, 6, 6, 5, 6),
            skillTotals = mapOf("Concentration" to 15, "Electronics/Security Tech (x2)" to 14,
                "Hacking" to 15, "Cryptography" to 13, "Perception" to 11, "Handgun" to 9),
            gear = listOf("Cyberdeck (10 Slots)", "Interface Plugs", "Neural Link",
                "Body Armor (SP13)", "Program: Sword", "Program: Worm", "Program: Armor"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "هکر پشتیبان شرکتی؛ زیرساخت‌های امنیتی را با دایس‌های بالا می‌شکند."
        ),
        NpcTemplate(
            "Trauma Team Medic", CAT_CORPORATE, NpcDifficulty.MEDIUM, role = "Medtech",
            stats = Stats(7, 7, 6, 8, 6, 6, 6, 6, 6, 7),
            skillTotals = mapOf("Paramedic (x2)" to 14, "First Aid" to 12, "Cybertech" to 11,
                "Handgun" to 9, "Evasion" to 10, "Perception" to 10, "Deduction" to 10),
            gear = listOf("Medtech Bag", "Airhypo", "Biomonitor", "Heavy Pistol",
                "Body Armor (SP13)", "Head Armor (SP13)", "Medic (drug) x3"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "تیمی که هم شفا می‌دهد و هم با آتش پوششی خط می‌گیرد؛ اولویت نجات مشتری."
        ),
        NpcTemplate(
            "Militech Spec Ops", CAT_MILITARY, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(7, 9, 8, 7, 8, 8, 6, 8, 8, 6),
            skillTotals = mapOf("Autofire" to 16, "Shoulder Arms" to 16, "Tactics" to 14,
                "Evasion" to 14, "Perception" to 13, "Concentration" to 13, "Brawling" to 13, "Stealth" to 13),
            gear = listOf("Assault Rifle", "Rocket Launcher", "Heavy Pistol", "Grenade Launcher",
                "Body Armor (SP13)", "Head Armor (SP13)", "Neural Link", "Sandevistan", "Kerenzikov", "Combat Drug x2"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "نیروی ویژه‌ی میلی‌تک؛ زره سنگین، تسلیحات منفجره و تخصص کامل رزمی. واقعاً مرگبار."
        ),
        NpcTemplate(
            "Barghest Outrider", CAT_GANG, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(6, 8, 7, 6, 7, 7, 6, 7, 7, 5),
            skillTotals = mapOf("Autofire" to 13, "Shoulder Arms" to 13, "Drive Land Vehicle" to 14,
                "Evasion" to 12, "Perception" to 11, "Tactics" to 11, "Brawling" to 11),
            gear = listOf("Assault Rifle", "Heavy Pistol", "SMG", "Body Armor (SP13)",
                "Head Armor (SP13)", "Neural Link", "Bulletproof Shield"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "سوارکار بارگست؛ از روی خودرو شلیک می‌کند و با سپر ضدگلوله پیشروی می‌کند."
        ),
        NpcTemplate(
            "Pyromaniac (Pyro)", CAT_GANG, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(6, 7, 7, 7, 7, 7, 5, 6, 7, 4),
            skillTotals = mapOf("Shoulder Arms" to 12, "Autofire" to 11, "Brawling" to 12,
                "Evasion" to 12, "Concentration" to 11, "Demolitions (x2)" to 13, "Perception" to 10),
            gear = listOf("Shotgun", "Incendiary Shotgun Shell Ammunition x10", "Flamethrower",
                "Body Armor (SP13)", "Head Armor (SP13)", "Grenade Launcher", "Smoke Grenade x2"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "مینی‌باسِ آتش؛ زمین بازی را به آتش می‌کشد و بازیکنان را از پوشش بیرون می‌کشد."
        ),
        NpcTemplate(
            "Reclaimer Chief", CAT_GANG, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(7, 8, 7, 6, 7, 8, 6, 7, 8, 6),
            skillTotals = mapOf("Brawling" to 14, "Melee Weapon" to 14, "Autofire" to 13,
                "Tactics" to 13, "Evasion" to 13, "Perception" to 12, "Concentration" to 12, "Athletics" to 13),
            gear = listOf("Assault Rifle", "Heavy Melee Weapon", "Body Armor (SP13)",
                "Head Armor (SP13)", "Neural Link", "Grafted Muscle and Bone Lace", "Cyberarm"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "فرمانده‌ی دسته‌ی ری‌کلیمر؛ با تیغ سایبری و تجربه‌ی تاکتیکی می‌جنگد."
        ),
        NpcTemplate(
            "Netrunner Lieutenant", CAT_GANG, NpcDifficulty.MEDIUM, role = "Netrunner",
            stats = Stats(7, 7, 6, 7, 6, 6, 5, 6, 5, 6),
            skillTotals = mapOf("Concentration" to 12, "Hacking" to 12, "Electronics/Security Tech (x2)" to 11,
                "Cryptography" to 10, "Perception" to 9, "Handgun" to 8),
            gear = listOf("Cyberdeck (7 Slots)", "Interface Plugs", "Light Pistol",
                "Light Armorjack Body Armor (SP11)", "Program: Sword"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "هکر پشتیبانی در سطح خیابان؛ مزاحم شبکه‌ی تیم می‌شود."
        ),
        NpcTemplate(
            "Solo Lieutenant", CAT_GANG, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 8, 7, 5, 7, 7, 6, 7, 7, 6),
            skillTotals = mapOf("Autofire" to 12, "Handgun" to 12, "Shoulder Arms" to 11,
                "Evasion" to 12, "Tactics" to 11, "Perception" to 10, "Brawling" to 11),
            gear = listOf("Assault Rifle", "Heavy Pistol", "Light Armorjack Body Armor (SP11)",
                "Light Armorjack Head Armor (SP11)", "Neural Link", "Sandevistan"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "معاون باند؛ رهبری نیروها را در نبرد بر عهده دارد."
        ),

        // ── Maelstrom ──────────────────────────────────────────
        NpcTemplate(
            "Maelstrom Chrome Priest", CAT_GANG, NpcDifficulty.HARD, role = "Tech",
            stats = Stats(7, 7, 6, 9, 4, 7, 5, 6, 8, 2),
            skillTotals = mapOf("Cybertech" to 15, "Basic Tech" to 14, "Handgun" to 12,
                "Brawling" to 11, "Perception" to 10, "Interrogation" to 11, "Evasion" to 9),
            gear = listOf("Heavy Pistol", "Basic Pistol Ammunition x30", "Medium Armorjack Body Armor (SP12)",
                "Cyberarm", "Cyberarm", "Neural Link", "Techtool", "Medtech Bag"),
            headArmorSp = 0, bodyArmorSp = 12,
            description = "کسی که تصمیم می‌گیرد چه کسی چه قطعه‌ای بگیرد. با ابزار جراحی می‌جنگد.",
            personality = "آرام و روش‌مند؛ بدن را ماشینی می‌بیند که باید بهینه شود.",
            motivation = "رساندن هر عضو گنگ به «کمال فلزی»، چه بخواهد چه نخواهد.",
            identifying = "پیش‌بند چرمیِ لکه‌دار و دو بازوی سایبری با ابزار جراحی به‌جای انگشت."),
        NpcTemplate(
            "Maelstrom Door Gunner", CAT_GANG, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(4, 7, 6, 5, 5, 7, 5, 6, 9, 3),
            skillTotals = mapOf("Autofire" to 13, "Heavy Weapons" to 12, "Shoulder Arms" to 12,
                "Brawling" to 11, "Perception" to 9, "Resist Torture/Drugs" to 10),
            gear = listOf("Assault Rifle", "Basic Rifle Ammunition x70", "Medium Armorjack Body Armor (SP12)",
                "Light Armorjack Head Armor (SP11)", "Grafted Muscle and Bone Lace"),
            headArmorSp = 11, bodyArmorSp = 12,
            description = "نگهبان درِ کلاب. اول شلیک می‌کند، سؤال هیچ‌وقت.",
            personality = "پرخاشگر و بی‌حوصله؛ گفت‌وگو را ضعف می‌داند.",
            motivation = "حفظ قلمرو و ترساندن هر غریبه‌ای که نزدیک شود.",
            identifying = "چشم قرمز تک و جلیقه‌ی فلزی پر از خط گلوله."),

        // ── Tyger Claws ────────────────────────────────────────
        NpcTemplate(
            "Tyger Claw Blade", CAT_GANG, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 8, 9, 6, 7, 6, 5, 8, 6, 5),
            skillTotals = mapOf("Melee Weapon" to 14, "Martial Arts" to 13, "Athletics" to 12,
                "Evasion" to 13, "Stealth" to 11, "Perception" to 9),
            gear = listOf("Very Heavy Melee Weapon", "Light Pistol", "Basic Pistol Ammunition x30",
                "Light Armorjack Body Armor (SP11)", "Kerenzikov", "Neural Link"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "شمشیرزن. سریع نزدیک می‌شود و فاصله را نمی‌دهد.",
            personality = "منضبط و ساکت؛ به سلسله‌مراتب گنگ وفادار است.",
            motivation = "بالا رفتن در گنگ از راه نشان دادن مهارت، نه خشونت کور.",
            identifying = "خالکوبی ببر روی گردن و کاتانای مونو با دسته‌ی قرمز."),
        NpcTemplate(
            "Tyger Claw Enforcer", CAT_GANG, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(6, 9, 8, 6, 7, 7, 5, 8, 8, 4),
            skillTotals = mapOf("Handgun" to 15, "Melee Weapon" to 14, "Martial Arts" to 13,
                "Evasion" to 13, "Intimidation" to 12, "Perception" to 11, "Tactics" to 11),
            gear = listOf("Very Heavy Pistol", "Basic Pistol Ammunition x30", "Heavy Melee Weapon",
                "Medium Armorjack Body Armor (SP12)", "Light Armorjack Head Armor (SP11)",
                "Sandevistan", "Neural Link", "Cyberaudio Suite"),
            headArmorSp = 11, bodyArmorSp = 12,
            description = "کسی که برای جمع کردن بدهی فرستاده می‌شود. مذاکره نمی‌کند.",
            personality = "خونسرد و تهدیدآمیز؛ خشونت را ابزار می‌داند نه تفریح.",
            motivation = "حفظ آبروی گنگ؛ هر بدهی پرداخت‌نشده توهین است.",
            identifying = "کت ابریشمی براق و دستکش بدون انگشت با بندهای فلزی."),

        // ── Valentinos ─────────────────────────────────────────
        NpcTemplate(
            "Valentinos Wheelman", CAT_GANG, NpcDifficulty.MEDIUM, role = "Nomad",
            stats = Stats(6, 8, 7, 7, 7, 6, 6, 7, 6, 6),
            skillTotals = mapOf("Drive Land Vehicle" to 15, "Handgun" to 12, "Basic Tech" to 11,
                "Evasion" to 11, "Perception" to 10, "Streetwise" to 11),
            gear = listOf("Heavy Pistol", "Basic Pistol Ammunition x30", "SMG",
                "Light Armorjack Body Armor (SP11)", "Neural Link", "Interface Plugs"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "راننده‌ی فرار. تا وقتی ماشین سالم است، گنگ گیر نمی‌افتد.",
            personality = "شوخ و پرحرف تا لحظه‌ای که پا روی گاز بگذارد.",
            motivation = "وفاداری به خانواده و ماشینی که خودش ساخته.",
            identifying = "ماشین قرمزِ براق با تزیینات طلایی و صلیب آویزان از آینه."),
        NpcTemplate(
            "Valentinos Jefe", CAT_GANG, NpcDifficulty.HARD, role = "Fixer",
            stats = Stats(8, 6, 6, 6, 9, 8, 6, 6, 7, 7),
            skillTotals = mapOf("Persuasion" to 16, "Handgun" to 13, "Streetwise" to 15,
                "Human Perception" to 14, "Interrogation" to 13, "Perception" to 11, "Trading" to 14),
            gear = listOf("Very Heavy Pistol", "Basic Pistol Ammunition x30",
                "Medium Armorjack Body Armor (SP12)", "Neural Link", "Cyberaudio Suite", "Agent"),
            headArmorSp = 0, bodyArmorSp = 12,
            description = "رهبر محله. با حرف کار را تمام می‌کند و اگر نشد، سی نفر پشتش هستند.",
            personality = "گرم و مهمان‌نواز در ظاهر؛ کینه‌ای و حسابگر در باطن.",
            motivation = "محافظت از محله و خانواده — با هر هزینه‌ای برای بیرونی‌ها.",
            identifying = "کت‌شلوار سفید، انگشترهای طلا و صلیب بزرگ روی سینه."),

        // ── Animals ────────────────────────────────────────────
        NpcTemplate(
            "Animals Bruiser", CAT_GANG, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(4, 6, 6, 4, 6, 7, 5, 6, 11, 4),
            skillTotals = mapOf("Brawling" to 15, "Melee Weapon" to 13, "Athletics" to 13,
                "Resist Torture/Drugs" to 12, "Intimidation" to 12, "Perception" to 9),
            gear = listOf("Heavy Melee Weapon", "Light Armorjack Body Armor (SP11)",
                "Grafted Muscle and Bone Lace", "Wolvers"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "بدون سلاح گرم. نزدیک می‌شود و با دست کار را تمام می‌کند.",
            personality = "ساده و مستقیم؛ به قدرت بدنی افتخار می‌کند و از کروم بدش می‌آید.",
            motivation = "اثبات برتری بدن طبیعیِ پرورش‌یافته بر ماشین.",
            identifying = "هیکل غول‌آسا، رگ‌های برجسته و هیچ قطعه‌ی فلزی روی بدن."),

        // ── 6th Street ─────────────────────────────────────────
        NpcTemplate(
            "6th Street Sergeant", CAT_GANG, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(7, 8, 6, 7, 7, 8, 5, 6, 8, 5),
            skillTotals = mapOf("Shoulder Arms" to 15, "Autofire" to 14, "Tactics" to 14,
                "Handgun" to 13, "Perception" to 12, "First Aid" to 11, "Evasion" to 11),
            gear = listOf("Assault Rifle", "Basic Rifle Ammunition x70", "Heavy Pistol",
                "Medium Armorjack Body Armor (SP12)", "Medium Armorjack Head Armor (SP12)",
                "Neural Link", "Cyberaudio Suite", "Grenade"),
            headArmorSp = 12, bodyArmorSp = 12,
            description = "کهنه‌سرباز واقعی. گروه را مثل جوخه می‌چرخاند و پوشش آتش می‌دهد.",
            personality = "منظم و فرماندهی؛ به «نظم در برابر آشوب» باور دارد.",
            motivation = "محافظت از محله در برابر گنگ‌ها و شرکت‌ها، با روش نظامی.",
            identifying = "ژاکت ارتشی رنگ‌ورورفته با نشان‌های دوخته و رادیوی روی شانه."),

        // ── Scavengers ─────────────────────────────────────────
        NpcTemplate(
            "Scav Harvester", CAT_GANG, NpcDifficulty.MEDIUM, role = "Medtech",
            stats = Stats(7, 6, 7, 8, 4, 6, 5, 6, 6, 2),
            skillTotals = mapOf("Medical Tech" to 14, "First Aid" to 13, "Handgun" to 11,
                "Stealth" to 12, "Brawling" to 11, "Perception" to 11, "Cybertech" to 12),
            gear = listOf("SMG", "Basic Pistol Ammunition x30", "Heavy Melee Weapon",
                "Light Armorjack Body Armor (SP11)", "Medtech Bag", "Airhypo", "Cryopump"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "آدم‌ربای قطعه‌فروش. هدف را زنده می‌خواهد چون بافتِ زنده گران‌تر است.",
            personality = "بی‌احساس و کاسب‌مسلک؛ آدم را فهرست قطعات می‌بیند.",
            motivation = "پر کردن سفارش خریدار؛ هر بدن یک فاکتور است.",
            identifying = "ون یخچال‌دار، دستکش لاستیکی و بوی ضدعفونی‌کننده."),
        NpcTemplate(
            "Scav Snatcher", CAT_GANG, NpcDifficulty.EASY, role = "Solo",
            stats = Stats(5, 6, 7, 5, 5, 5, 5, 7, 6, 3),
            skillTotals = mapOf("Stealth" to 12, "Brawling" to 11, "Handgun" to 10,
                "Athletics" to 11, "Perception" to 10, "Evasion" to 10),
            gear = listOf("Medium Pistol", "Basic Pistol Ammunition x30", "Light Melee Weapon",
                "Kevlar Body Armor (SP7)", "Airhypo"),
            headArmorSp = 0, bodyArmorSp = 7,
            description = "در کوچه کمین می‌کند و تنهاها را می‌برد. به‌تنهایی خطر بزرگی نیست.",
            personality = "ترسو و فرصت‌طلب؛ در نبرد برابر فرار می‌کند.",
            motivation = "پول سریع؛ هر هدفِ تنها یک حقوق است.",
            identifying = "کاپشن گشاد، سرنگ در آستین و نگاه مدام به اطراف."),

        // ── Voodoo Boys ────────────────────────────────────────
        NpcTemplate(
            "Voodoo Boys Netrunner", CAT_GANG, NpcDifficulty.HARD, role = "Netrunner",
            stats = Stats(9, 6, 6, 8, 6, 8, 5, 6, 5, 4),
            skillTotals = mapOf("Interface" to 16, "Electronics/Security Tech" to 14,
                "Stealth" to 12, "Handgun" to 11, "Perception" to 11, "Cryptography" to 13),
            gear = listOf("Medium Pistol", "Basic Pistol Ammunition x30",
                "Light Armorjack Body Armor (SP11)", "Cyberdeck", "Neural Link", "Interface Plugs"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "پشت دیوار NET کار می‌کند. اگر تیم Netrunner ندارد، این یکی کابوس است.",
            personality = "مرموز و کم‌حرف؛ اطلاعات را ارز می‌داند.",
            motivation = "رسیدن به لایه‌های عمیق NET و آنچه پشت Blackwall است.",
            identifying = "کلاه‌خود نوری، خالکوبی مدار روی شقیقه و دو پلاگ گردنی."),

        NpcTemplate(
            "Arasaka Netrunner", CAT_CORPORATE, NpcDifficulty.HARD, role = "Netrunner",
            stats = Stats(9, 7, 6, 9, 6, 8, 5, 6, 6, 5),
            skillTotals = mapOf("Interface" to 16, "Electronics/Security Tech" to 15,
                "Handgun" to 12, "Perception" to 12, "Cryptography" to 14, "Stealth" to 11),
            gear = listOf("Heavy Pistol", "Basic Pistol Ammunition x30",
                "Medium Armorjack Body Armor (SP12)", "Cyberdeck", "Neural Link", "Interface Plugs"),
            headArmorSp = 0, bodyArmorSp = 12,
            description = "پشتیبان NET تیم امنیتی. دوربین‌ها و قفل‌ها را علیه مهاجم برمی‌گرداند.",
            personality = "دقیق و بی‌احساس؛ همه‌چیز را مسئله‌ی فنی می‌بیند.",
            motivation = "اجرای پروتکل و حفاظت از دارایی شرکت.",
            identifying = "کت‌شلوار مشکی مرتب و پلاگ‌های پشت گردن."),
        NpcTemplate(
            "Militech Field Medic", CAT_CORPORATE, NpcDifficulty.MEDIUM, role = "Medtech",
            stats = Stats(7, 7, 7, 8, 6, 7, 5, 7, 7, 6),
            skillTotals = mapOf("Medical Tech" to 15, "First Aid" to 14, "Shoulder Arms" to 12,
                "Handgun" to 12, "Perception" to 11, "Athletics" to 11),
            gear = listOf("SMG", "Basic Pistol Ammunition x30", "Medium Armorjack Body Armor (SP12)",
                "Light Armorjack Head Armor (SP11)", "Medtech Bag", "Airhypo", "Cryopump"),
            headArmorSp = 11, bodyArmorSp = 12,
            description = "زخمی‌ها را سر پا نگه می‌دارد. تا وقتی او زنده است، تیم مقابل نمی‌میرد.",
            personality = "خونسرد زیر آتش؛ اول بیمار، بعد تهدید.",
            motivation = "هیچ‌کس از تیمش روی زمین نمی‌ماند — حتی به قیمت ریسک خودش.",
            identifying = "نوار قرمز روی بازو و کیف پزشکی سنگین روی کمر."),
        NpcTemplate(
            "Corporate Negotiator", CAT_CORPORATE, NpcDifficulty.MEDIUM, role = "Exec",
            stats = Stats(8, 5, 5, 6, 9, 7, 6, 6, 5, 7),
            skillTotals = mapOf("Persuasion" to 16, "Human Perception" to 14, "Bureaucracy" to 14,
                "Handgun" to 10, "Perception" to 11, "Trading" to 14, "Accounting" to 12),
            gear = listOf("Light Pistol", "Basic Pistol Ammunition x30",
                "Light Armorjack Body Armor (SP11)", "Agent", "Neural Link"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "با دو محافظ می‌آید و سعی می‌کند کار را بدون خون تمام کند.",
            personality = "مؤدب و صبور؛ هر جمله‌اش از قبل حساب‌شده است.",
            motivation = "بستن قرارداد با کمترین هزینه برای شرکت.",
            identifying = "کیف‌دستی قفل‌دار و لبخندی که هیچ‌وقت به چشم‌ها نمی‌رسد."),

        NpcTemplate(
            "Trauma Team Paramedic", CAT_MILITARY, NpcDifficulty.HARD, role = "Medtech",
            stats = Stats(7, 8, 7, 8, 6, 8, 5, 8, 8, 6),
            skillTotals = mapOf("Medical Tech" to 16, "First Aid" to 15, "Shoulder Arms" to 13,
                "Athletics" to 13, "Perception" to 12, "Evasion" to 12),
            gear = listOf("Assault Rifle", "Basic Rifle Ammunition x70", "Very Heavy Pistol",
                "Metalgear Body Armor (SP13)", "Metalgear Head Armor (SP13)",
                "Medtech Bag", "Airhypo", "Cryopump", "Neural Link"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "برای نجات مشترکِ طلایی می‌آید. سر راهش نایست.",
            personality = "حرفه‌ای و عجول؛ فقط مشترک برایش مهم است، نه صحنه.",
            motivation = "اجرای قرارداد در کمترین زمان؛ تلفات جانبی مسئله‌ی او نیست.",
            identifying = "زره سفید-قرمز Trauma Team و AV بالای سر."),
        NpcTemplate(
            "Mercenary Sniper", CAT_MILITARY, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(7, 9, 7, 7, 6, 8, 5, 6, 7, 4),
            skillTotals = mapOf("Shoulder Arms" to 16, "Stealth" to 14, "Perception" to 14,
                "Tactics" to 13, "Athletics" to 11, "Evasion" to 11),
            gear = listOf("Sniper Rifle", "Basic Rifle Ammunition x50", "Heavy Pistol",
                "Medium Armorjack Body Armor (SP12)", "Sniping Scope", "Neural Link", "Kerenzikov"),
            headArmorSp = 0, bodyArmorSp = 12,
            description = "از فاصله‌ی امن کار می‌کند. تا پیدایش نکنی، هر نوبت یکی کم می‌شود.",
            personality = "صبور و ساکت؛ ساعت‌ها منتظر یک شلیک می‌ماند.",
            motivation = "قرارداد؛ هدف فقط یک نام روی کاغذ است.",
            identifying = "پوشش استتار، حضور نامرئی و گلوله‌ای که از ناکجا می‌آید."),
    )

    // ============================================================
    //  بخش ۲ — سایبرسایکوها (۱۲ عدد متنوع)
    // ============================================================

    private val cyberpsychos = listOf(
        NpcTemplate(
            "Nikki Cull — Brain-Dance Cyberpsycho", CAT_CYBERPSYCHO, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(5, 8, 8, 5, 4, 7, 6, 8, 10, 0),
            skillTotals = mapOf("Autofire" to 14, "Brawling" to 15, "Evasion" to 13,
                "Concentration" to 6, "Conversation" to 2, "Drive Land Vehicle" to 10,
                "Education" to 7, "Melee Weapon" to 13, "Shoulder Arms" to 12),
            gear = listOf("Armor Piercing Grenade x2", "Chipware Socket", "Cyberarm", "Cyberarm",
                "Cyberleg", "Cyberleg", "Cybersnake", "Grafted Muscle and Bone Lace", "Grenade Launcher",
                "Head Armor (SP13)", "Body Armor (SP13)", "Kerenzikov", "Sandevistan"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "جایزه‌ی سرِ دینو دینوویچ. چهار سایبرپنجه و سایبراسنیک؛ حرکتی فوق‌العاده سریع، هم در نبرد تن‌به‌تن و هم در آتش سنگین مرگبار است."
        ),
        NpcTemplate(
            "Razor Jax — Chrome Berserker", CAT_CYBERPSYCHO, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(4, 8, 9, 5, 5, 8, 5, 8, 10, 0),
            skillTotals = mapOf("Brawling" to 17, "Melee Weapon" to 16, "Evasion" to 14,
                "Athletics" to 16, "Concentration" to 8, "Resist Torture/Drugs" to 15, "Perception" to 9),
            gear = listOf("Mantis Blades", "Mantis Blades", "Grafted Muscle and Bone Lace",
                "Cyberleg", "Cyberleg", "Subdermal Armor", "Head Armor (SP13)", "Body Armor (SP13)"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "تیغ‌های آخوندکی دوبل؛ در فاز ۱ با سرعت فاصله را می‌بندد و در فاز ۲ تکه‌تکه می‌کند."
        ),
        NpcTemplate(
            "Saito — The Ghost in the Wires", CAT_CYBERPSYCHO, NpcDifficulty.HARD, role = "Netrunner",
            stats = Stats(9, 7, 6, 9, 6, 6, 6, 7, 5, 1),
            skillTotals = mapOf("Concentration" to 16, "Hacking" to 16, "Stealth" to 14,
                "Electronics/Security Tech (x2)" to 15, "Cryptography" to 13, "Perception" to 11, "Handgun" to 9),
            gear = listOf("Cyberdeck (10 Slots)", "Interface Plugs", "Neural Link", "Light Pistol",
                "Cloaking Infiltrator Cyberware", "Optical Camo", "Program: Worm", "Program: Sword"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "سایبرسایکوی نامرئی؛ با استلت و نفوذ شبکه پیش از حمله، قربانی را منزوی و خاموش می‌کند."
        ),
        NpcTemplate(
            "Brickjaw — The Living Tank", CAT_CYBERPSYCHO, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(4, 6, 6, 4, 6, 9, 5, 6, 12, 1),
            skillTotals = mapOf("Brawling" to 18, "Concentration" to 12, "Athletics" to 16,
                "Resist Torture/Drugs" to 17, "Evasion" to 10, "Melee Weapon" to 13, "Perception" to 8),
            gear = listOf("Grafted Muscle and Bone Lace", "Subdermal Armor", "Cyberarm",
                "Heavy Melee Weapon", "Body Armor (SP13)", "Head Armor (SP13)"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "بدن ۱۲ و زره زیرپوستی؛ تقریباً توقف‌ناپذیر. گلوله‌های سبک عملاً بی‌اثرند؛ به آتش سنگین نیاز است."
        ),
        NpcTemplate(
            "Wirework Lily — Strings of Death", CAT_CYBERPSYCHO, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(6, 9, 9, 6, 6, 6, 6, 8, 7, 2),
            skillTotals = mapOf("Melee Weapon" to 16, "Brawling" to 14, "Evasion" to 15,
                "Stealth" to 13, "Perception" to 10, "Concentration" to 10, "Athletics" to 12),
            gear = listOf("Monowire", "Neural Link", "Kerenzikov", "Optical Camo",
                "Light Armorjack Body Armor (SP11)", "Grafted Muscle and Bone Lace"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "مونوسیم کشنده؛ یک ضربه از پشت می‌تواند غافلگیرکننده تمامش کند. از استلت استفاده کن."
        ),
        NpcTemplate(
            "Havok — The Bomber", CAT_CYBERPSYCHO, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(6, 7, 6, 8, 7, 7, 5, 6, 7, 2),
            skillTotals = mapOf("Demolitions (x2)" to 16, "Shoulder Arms" to 12, "Brawling" to 11,
                "Evasion" to 12, "Concentration" to 12, "Perception" to 10, "Autofire" to 11),
            gear = listOf("Grenade Launcher", "Incendiary Grenade x4", "Armor Piercing Grenade x3",
                "Frag Grenade x4", "Rocket Launcher", "Body Armor (SP13)", "Kerenzikov"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "مواد منفجره متعدد؛ منطقه را مین‌گذاری و از نارنجک‌انداز انفجار گسترده ایجاد می‌کند."
        ),
        NpcTemplate(
            "Ghoul — The Scavenger", CAT_CYBERPSYCHO, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 8, 7, 8, 6, 6, 5, 7, 6, 2),
            skillTotals = mapOf("Brawling" to 12, "Melee Weapon" to 13, "Cybertech" to 12,
                "Stealth" to 12, "Evasion" to 12, "Perception" to 10, "First Aid" to 10),
            gear = listOf("Scalpel", "Heavy Melee Weapon", "Cyberarm", "Buzzsaw",
                "Light Armorjack Body Armor (SP11)", "Medtech Bag"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "اسکَورجر بیمار؛ با اره‌ی چرخشی سایبری برای برداشتن اعضا حمله می‌کند."
        ),
        NpcTemplate(
            "Silent Requiem — The Sniper", CAT_CYBERPSYCHO, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(7, 9, 6, 6, 6, 7, 6, 6, 6, 2),
            skillTotals = mapOf("Shoulder Arms" to 17, "Concentration" to 14, "Stealth" to 15,
                "Perception" to 14, "Evasion" to 12, "Electronics/Security Tech (x2)" to 11),
            gear = listOf("Sniper Rifle", "Smart Link", "Targeting Scope", "Neural Link",
                "Kerenzikov", "Light Armorjack Head Armor (SP11)"),
            headArmorSp = 11, bodyArmorSp = 0,
            description = "تک‌تیرانداز سایکو؛ از فاصله‌ی دور با خسارت سنگین شکار می‌کند. خط دیدش را قطع کن."
        ),
        NpcTemplate(
            "Ironclad — The Reforged", CAT_CYBERPSYCHO, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(5, 8, 7, 6, 6, 8, 5, 7, 9, 1),
            skillTotals = mapOf("Brawling" to 16, "Autofire" to 13, "Melee Weapon" to 13,
                "Athletics" to 14, "Resist Torture/Drugs" to 14, "Evasion" to 12, "Concentration" to 11),
            gear = listOf("Subdermal Armor", "Cyberarm", "Cyberarm", "Cyberleg", "Assault Rifle",
                "Grafted Muscle and Bone Lace", "Body Armor (SP13)", "Head Armor (SP13)"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "بیش از حد کروم؛ زره دوگانه‌ی زیرپوستی و سطحی. ترکیبی از دوام و آتش."
        ),
        NpcTemplate(
            "Flicker — Speed Demon", CAT_CYBERPSYCHO, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(5, 10, 9, 5, 7, 6, 7, 10, 6, 2),
            skillTotals = mapOf("Autofire" to 15, "Brawling" to 13, "Evasion" to 16,
                "Melee Weapon" to 14, "Athletics" to 15, "Concentration" to 9, "Perception" to 10),
            gear = listOf("Sandevistan", "Kerenzikov", "SMG", "SMG", "Mantis Blades",
                "Grafted Muscle and Bone Lace", "Light Armorjack Body Armor (SP11)"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "دو شتاب‌دهنده‌ی عصبی هم‌زمان؛ در یک نوبت چندین بار حرکت و شلیک می‌کند. او را زود غافلگیر کن."
        ),
        NpcTemplate(
            "Doc Blight — The Butcher", CAT_CYBERPSYCHO, NpcDifficulty.MEDIUM, role = "Medtech",
            stats = Stats(7, 7, 6, 9, 5, 6, 5, 6, 6, 1),
            skillTotals = mapOf("Paramedic (x2)" to 13, "Cybertech" to 14, "Brawling" to 11,
                "Melee Weapon" to 12, "First Aid" to 12, "Deduction" to 11, "Perception" to 9),
            gear = listOf("Bonesaw", "Medtech Bag", "Airhypo", "Cyberarm", "Scalpel x3",
                "Light Armorjack Body Armor (SP11)"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "مدتک سایکو؛ قربانی را برای «درمان» می‌دزدد و روی میز جراحی سلاخی می‌کند."
        ),

        NpcTemplate(
            "Borg Wreck", CAT_CYBERPSYCHO, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(3, 8, 6, 4, 2, 9, 5, 7, 12, 0),
            skillTotals = mapOf("Brawling" to 16, "Melee Weapon" to 15, "Athletics" to 13,
                "Resist Torture/Drugs" to 15, "Perception" to 10, "Evasion" to 10),
            gear = listOf("Cyberarm", "Cyberarm", "Cyberleg", "Cyberleg", "Wolvers",
                "Subdermal Armor", "Grafted Muscle and Bone Lace", "Sandevistan"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "تقریباً چیزی از بدن اصلی نمانده. سلاح گرم ندارد و لازم هم ندارد.",
            personality = "هیچ گفت‌وگویی ممکن نیست؛ فقط صدا و حرکت را دنبال می‌کند.",
            motivation = "درد. هر چیزی که نزدیک شود باید متوقف شود.",
            identifying = "چهار اندام فلزی ناهماهنگ و صدای سرویس‌نشده‌ی موتورها."),
        NpcTemplate(
            "Sandevistan Slasher", CAT_CYBERPSYCHO, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(4, 10, 9, 5, 3, 7, 5, 9, 7, 0),
            skillTotals = mapOf("Melee Weapon" to 16, "Martial Arts" to 14, "Athletics" to 14,
                "Evasion" to 15, "Stealth" to 12, "Perception" to 10),
            gear = listOf("Very Heavy Melee Weapon", "Wolvers", "Light Armorjack Body Armor (SP11)",
                "Sandevistan", "Kerenzikov", "Neural Link"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "در یک نوبت از آن‌سوی اتاق می‌رسد. سرعت، خودِ تهدید است.",
            personality = "بی‌قرار و لرزان؛ زمان برایش کند شده و دنیا آزاردهنده است.",
            motivation = "توقف صدای درون سر، با هر حرکتی که لازم باشد.",
            identifying = "لرزش دائم دست‌ها و رد محوی که هنگام حرکت به جا می‌گذارد."),
    )

    // ============================================================
    //  بخش ۳ — بک‌آپ لاومن (توانایی نقش Backup، چند مدل)
    // ============================================================

    private val lawmanBackup = listOf(
        NpcTemplate(
            "Local Beat Cop (×4)", CAT_LAWMAN, NpcDifficulty.EASY, role = "Lawman",
            stats = Stats(6, 6, 6, 5, 6, 6, 5, 6, 6, 6),
            skillTotals = mapOf("Handgun" to 10, "Brawling" to 9, "Evasion" to 9,
                "Perception" to 9, "Concentration" to 9, "Shoulder Arms" to 8, "Interrogation" to 9),
            gear = listOf("Heavy Pistol", "Basic H Pistol Ammunition x50", "Handcuffs",
                "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)", "Radio Communicator"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "گشت محلی؛ در رنک‌های پایین Backup با ۴ افسر می‌رسی. برای جمعیت و حضور تعداد خوب‌اند."
        ),
        NpcTemplate(
            "County Mounty (×2)", CAT_LAWMAN, NpcDifficulty.MEDIUM, role = "Lawman",
            stats = Stats(6, 7, 6, 6, 7, 6, 6, 7, 7, 6),
            skillTotals = mapOf("Shoulder Arms" to 12, "Autofire" to 11, "Drive Land Vehicle" to 12,
                "Perception" to 10, "Evasion" to 10, "Brawling" to 10, "Tactics" to 10),
            gear = listOf("Assault Rifle", "Shotgun", "Basic Rifle Ammunition x70", "Flashlight",
                "Body Armor (SP13)", "Head Armor (SP13)", "Radio Communicator"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "کلانترِ شهرستان؛ دو نفر با خودروی گشت و تسلیحات سنگین‌تر می‌رسند."
        ),
        NpcTemplate(
            "National Law Enforcement (×2)", CAT_LAWMAN, NpcDifficulty.HARD, role = "Lawman",
            stats = Stats(7, 8, 7, 6, 7, 7, 6, 7, 8, 6),
            skillTotals = mapOf("Autofire" to 14, "Shoulder Arms" to 13, "Tactics" to 13,
                "Evasion" to 12, "Perception" to 12, "Brawling" to 12, "Interrogation" to 12),
            gear = listOf("Assault Rifle", "Very Heavy Pistol", "Body Armor (SP13)",
                "Head Armor (SP13)", "Neural Link", "Sandevistan", "Radio Communicator", "Kerenzikov"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "نیروی ویژه‌ی NLE؛ رنک‌های بالای Backup. به‌شدت مسلح و آموزش‌دیده."
        ),

        NpcTemplate(
            "NCPD Detective", CAT_LAWMAN, NpcDifficulty.MEDIUM, role = "Lawman",
            stats = Stats(8, 7, 6, 7, 7, 7, 6, 6, 6, 6),
            skillTotals = mapOf("Human Perception" to 15, "Perception" to 14, "Handgun" to 13,
                "Interrogation" to 14, "Streetwise" to 13, "Evasion" to 10, "Tracking" to 12),
            gear = listOf("Very Heavy Pistol", "Basic Pistol Ammunition x30",
                "Light Armorjack Body Armor (SP11)", "Agent", "Neural Link", "Cyberaudio Suite"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "تیرانداز نیست؛ سؤال می‌پرسد و جواب‌ها را کنار هم می‌گذارد.",
            personality = "خسته ولی تیزبین؛ دروغ را از فاصله می‌شنود.",
            motivation = "بستن پرونده‌ای که ده سال باز مانده.",
            identifying = "کت بارانی چروک و دفترچه‌ی کاغذی به‌جای Agent."),
        NpcTemplate(
            "MaxTac Operative", CAT_LAWMAN, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(7, 10, 8, 7, 6, 9, 5, 8, 9, 2),
            skillTotals = mapOf("Autofire" to 17, "Handgun" to 16, "Shoulder Arms" to 16,
                "Evasion" to 15, "Perception" to 14, "Tactics" to 14, "Athletics" to 13),
            gear = listOf("Assault Rifle", "Basic Rifle Ammunition x70", "Very Heavy Pistol",
                "Metalgear Body Armor (SP13)", "Metalgear Head Armor (SP13)",
                "Sandevistan", "Kerenzikov", "Neural Link", "Grenade"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "برای سایبرسایکو می‌آید. اگر کرو سر راه باشد، فرقی نمی‌کند.",
            personality = "خودش سایبرسایکوی مهارشده است؛ سرد و بی‌مکث.",
            motivation = "خنثی کردن تهدید — تعریفِ «تهدید» با خودش است.",
            identifying = "زره سنگین آبی-مشکی MaxTac و ماسک تمام‌صورت."),
    )

    // ============================================================
    //  بخش ۴ — تیم اگزک (توانایی نقش Teamwork، چند مدل)
    // ============================================================

    private val execTeam = listOf(
        NpcTemplate(
            "Exec Bodyguard", CAT_EXEC, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 8, 7, 5, 7, 7, 6, 7, 8, 6),
            skillTotals = mapOf("Brawling" to 13, "Handgun" to 12, "Evasion" to 12,
                "Perception" to 12, "Autofire" to 11, "Concentration" to 11, "Athletics" to 12),
            gear = listOf("Very Heavy Pistol", "Heavy Melee Weapon", "Bulletproof Shield",
                "Body Armor (SP13)", "Head Armor (SP13)", "Subdermal Pocket"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "محافظ شخصی؛ اولویت حفظ جان کارفرما. با سپر پوشش می‌دهد."
        ),
        NpcTemplate(
            "Exec Driver", CAT_EXEC, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 8, 7, 6, 7, 6, 6, 7, 7, 6),
            skillTotals = mapOf("Drive Land Vehicle" to 15, "Autofire" to 11, "Evasion" to 11,
                "Perception" to 11, "Shoulder Arms" to 11, "Brawling" to 10),
            gear = listOf("SMG", "Heavy Pistol", "Light Armorjack Body Armor (SP11)",
                "Light Armorjack Head Armor (SP11)", "Neural Link"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "راننده‌ی عملیات؛ خروج امن را تضمین می‌کند و از خودرو آتش پوششی می‌بندد."
        ),
        NpcTemplate(
            "Exec Netrunner", CAT_EXEC, NpcDifficulty.HARD, role = "Netrunner",
            stats = Stats(8, 7, 6, 8, 7, 7, 6, 6, 5, 7),
            skillTotals = mapOf("Concentration" to 14, "Hacking" to 14, "Electronics/Security Tech (x2)" to 13,
                "Cryptography" to 12, "Perception" to 11, "Handgun" to 9),
            gear = listOf("Cyberdeck (7 Slots)", "Interface Plugs", "Neural Link",
                "Light Armorjack Body Armor (SP11)", "Program: Sword", "Program: Worm"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "هکر تیم؛ در حین عملیات پشتیبانی شبکه‌ای و دفاعی فراهم می‌کند."
        ),
        NpcTemplate(
            "Exec Security Chief", CAT_EXEC, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(7, 9, 7, 6, 8, 8, 6, 7, 8, 7),
            skillTotals = mapOf("Autofire" to 15, "Shoulder Arms" to 14, "Tactics" to 15,
                "Perception" to 14, "Evasion" to 13, "Brawling" to 13, "Concentration" to 13),
            gear = listOf("Assault Rifle", "Very Heavy Pistol", "Body Armor (SP13)",
                "Head Armor (SP13)", "Neural Link", "Sandevistan", "Radio Communicator x4"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "رئیس امنیت؛ کل تیم را هماهنگ و با شایستگی رزمی بالا رهبری می‌کند."
        ),

        NpcTemplate(
            "Corporate Bodyguard", CAT_EXEC, NpcDifficulty.MEDIUM, role = "Solo",
            stats = Stats(6, 8, 7, 6, 6, 8, 5, 7, 8, 5),
            skillTotals = mapOf("Handgun" to 14, "Brawling" to 13, "Perception" to 13,
                "Evasion" to 12, "Athletics" to 12, "Human Perception" to 12),
            gear = listOf("Very Heavy Pistol", "Basic Pistol Ammunition x30",
                "Medium Armorjack Body Armor (SP12)", "Neural Link", "Kerenzikov", "Cyberaudio Suite"),
            headArmorSp = 0, bodyArmorSp = 12,
            description = "بین مدیر و گلوله می‌ایستد. تا مدیر امن نشود، عقب نمی‌کشد.",
            personality = "ساکت و همیشه در حال اسکن اطراف.",
            motivation = "مشتری زنده می‌ماند؛ بقیه‌ی چیزها قابل مذاکره‌اند.",
            identifying = "کت‌شلوار تیره، گوشی سیمی در گوش و دستی که همیشه آزاد است."),
        NpcTemplate(
            "Corporate Analyst", CAT_EXEC, NpcDifficulty.EASY, role = "Exec",
            stats = Stats(9, 5, 5, 8, 6, 6, 6, 6, 4, 6),
            skillTotals = mapOf("Accounting" to 15, "Bureaucracy" to 14, "Library Search" to 14,
                "Human Perception" to 11, "Perception" to 10, "Handgun" to 8),
            gear = listOf("Light Pistol", "Basic Pistol Ammunition x30", "Agent", "Neural Link"),
            headArmorSp = 0, bodyArmorSp = 0,
            description = "نمی‌جنگد. ولی می‌داند پول کجا رفته و همین او را هدف می‌کند.",
            personality = "عصبی و پرحرف؛ زیر فشار همه‌چیز را می‌گوید.",
            motivation = "زنده ماندن و حفظ شغل — به همین ترتیب.",
            identifying = "عینک ضخیم، پیراهن اتوکشیده و دست‌های لرزان."),
    )

    // ---------------- NPCهای متخصصِ نقش‌های کمتر (حداقل یکی از هر نقش) ----------------
    private val roleSpecialists = listOf(
        NpcTemplate(
            "Street Fixer", CAT_GANG, NpcDifficulty.MEDIUM, role = "Fixer",
            stats = Stats(6, 6, 6, 7, 7, 7, 8, 6, 5, 6),
            skillTotals = mapOf("Trading" to 13, "Persuasion" to 13, "Human Perception" to 12,
                "Streetwise" to 12, "Handgun" to 9, "Evasion" to 9, "Perception" to 10, "Concentration" to 10),
            gear = listOf("Heavy Pistol", "Light Armorjack Body Armor (SP11)", "Agent", "Chemskin"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "دلال خیابانی؛ چیزی که لازم داری را پیدا یا جور می‌کند، البته به قیمت خودش.",
            personality = "چرب‌زبان، معامله‌گر و همیشه لبخند‌به‌لب؛ هیچ حرفی را بی‌حساب نمی‌زند.",
            motivation = "رسیدن به پول و اعتبار بیشتر؛ هر معامله یک پله برای اوست.",
            identifying = "کت چرمی نوک‌مدادی، انگشتر نئونی و همیشه یک Agent در دست."
        ),
        NpcTemplate(
            "Back-Alley Medtech", CAT_GANG, NpcDifficulty.MEDIUM, role = "Medtech",
            stats = Stats(7, 6, 6, 6, 7, 8, 5, 6, 5, 8),
            skillTotals = mapOf("First Aid" to 13, "Paramedic" to 12, "Medical Tech" to 12,
                "Science" to 11, "Handgun" to 9, "Perception" to 10, "Evasion" to 9, "Concentration" to 11),
            gear = listOf("Medtech Bag", "Airhypo", "Speedheal", "Medium Pistol",
                "Light Armorjack Body Armor (SP11)"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "پزشک غیرقانونی؛ زخمی‌های گنگ را در کلینیک مخفی وصله‌پینه می‌کند.",
            personality = "سرد و حرفه‌ای زیر فشار؛ با لحنی آرام حتی هنگام دوختن زخم.",
            motivation = "نجات جان آدم‌های خودی و ثابت‌کردن مهارتش در خیابان.",
            identifying = "دست‌های همیشه آغشته به ماده‌ی ضدعفونی، روپوش لکه‌دار و کیف پزشکی بزرگ."
        ),
        NpcTemplate(
            "Riot Rockerboy", CAT_GANG, NpcDifficulty.EASY, role = "Rockerboy",
            stats = Stats(5, 7, 6, 7, 6, 6, 7, 5, 6, 5),
            skillTotals = mapOf("Play Instrument" to 12, "Persuasion" to 10, "Streetwise" to 9,
                "Wardrobe & Style" to 9, "Brawling" to 9, "Evasion" to 9, "Perception" to 8, "Handgun" to 8),
            gear = listOf("Pocket Amp", "Electric Guitar", "Medium Pistol", "Urban Flash Outfit", "Glow Paint x5"),
            headArmorSp = 0, bodyArmorSp = 0,
            description = "خواننده‌ی خیابانی و شورشی؛ با موسیقی مردم را علیه شرکت‌ها می‌شوراند.",
            personality = "بلندپرواز، پرشور و کاریزماتیک؛ روی صحنه یا وسط شورش بی‌باک است.",
            motivation = "شنیدن صدای حقیقت در تمام شهر و برانداختن نظم شرکتی.",
            identifying = "موی نئونی، خالکوبی‌های درخشان و گیتاری که همه‌جا با خودش می‌کشد."
        ),
        NpcTemplate(
            "Junkheap Tech", CAT_GANG, NpcDifficulty.MEDIUM, role = "Tech",
            stats = Stats(8, 7, 5, 6, 7, 7, 6, 6, 5, 5),
            skillTotals = mapOf("Technique" to 13, "Electronics/Security Tech" to 12, "Cybertech" to 11,
                "Shoulder Arms" to 10, "Maker" to 12, "Perception" to 10, "Evasion" to 9, "Handgun" to 9),
            gear = listOf("Tech Bag", "Tool Hand", "Very Heavy Pistol", "Heavy Melee Weapon",
                "Light Armorjack Body Armor (SP11)"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "تکنسین گنگ؛ اسلحه، زره و سایبرویر خراب را در کارگاهش سرهم می‌کند.",
            personality = "ساکت و غرق در کار؛ با دستگاه‌ها راحت‌تر از آدم‌ها حرف می‌زند.",
            motivation = "ساختن ابزار کامل و باز کردن قفل هر سیستم بسته‌ای.",
            identifying = "دست‌ابزار سایبری چندکاره، روغن روی صورت و عینک جوشکاری بالای پیشانی."
        ),
        NpcTemplate(
            "Nomad Scout", CAT_MILITARY, NpcDifficulty.MEDIUM, role = "Nomad",
            stats = Stats(7, 8, 6, 6, 6, 6, 6, 7, 5, 6),
            skillTotals = mapOf("Drive Land Vehicle" to 14, "Shoulder Arms" to 11, "Autofire" to 10,
                "Tracking" to 12, "Wilderness Survival" to 12, "Perception" to 11, "Evasion" to 10, "Brawling" to 10),
            gear = listOf("Assault Rifle", "Heavy Pistol", "Performance Bike",
                "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"),
            headArmorSp = 11, bodyArmorSp = 11,
            description = "دیده‌بان کوچ‌نشین؛ جاده را مثل کف دست می‌شناسد و بدرقه‌ی کاروان است.",
            personality = "هوشیار، کم‌حرف و وفادار به خانواده‌ی جاده‌اش.",
            motivation = "حفاظت از کاروان و باز کردن مسیر امن برای خانواده.",
            identifying = "ژاکت چرمی وصله‌دار، گردن‌بند نماد خانواده و موتوری که خودش تعمیرش کرده."
        )
    )

    // ============================================================
    //  باس‌ها: چند مینی‌باس/باس پیش از باس نهایی
    // ============================================================

    private val bosses = listOf(
        NpcTemplate(
            "Royce — Maelstrom Boss", CAT_BOSS, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(8, 8, 7, 6, 8, 8, 6, 6, 8, 3),
            skillTotals = mapOf("Autofire" to 15, "Shoulder Arms" to 14, "Brawling" to 15,
                "Melee Weapon" to 14, "Evasion" to 13, "Perception" to 12, "Concentration" to 14,
                "Tactics" to 13, "Athletics" to 15, "Heavy Weapons" to 13, "Intimidation" to 15),
            gear = listOf("Shotgun", "Cyberarm", "Grafted Muscle and Bone Lace",
                "Heavy Armorjack Body Armor (SP13)", "Heavy Armorjack Head Armor (SP13)",
                "Mantis Blades", "Sandevistan", "Basic Slug Shells x10"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "رئیس گنگ مایل‌استروم؛ نیمی از صورتش با فلز قرمز جوش خورده و با قدرت ترس حکومت می‌کند.",
            personality = "پرخاشگر، بی‌ثبات و عادت‌کرده به اطاعت بی‌چون‌وچرا؛ با کوچک‌ترین توهین منفجر می‌شود.",
            motivation = "قبضه‌کردن کل تجارت اسلحه و گوشت سایبری واتسون و ثابت‌کردن برتری مایل‌استروم.",
            identifying = "سر تراشیده با ایمپلنت‌های قرمز درخشان، بازوی سایبر غول‌پیکر و جلیقه‌ی چرم سیاه با نئون قرمز."
        ),
        NpcTemplate(
            "Placide — Voodoo Boys Netrunner", CAT_BOSS, NpcDifficulty.HARD, role = "Netrunner",
            stats = Stats(7, 7, 6, 9, 7, 8, 6, 6, 6, 4),
            skillTotals = mapOf("Concentration" to 16, "Hacking" to 17, "Electronics/Security Tech (x2)" to 15,
                "Cryptography" to 15, "Perception" to 13, "Evasion" to 12, "Handgun" to 11,
                "Tactics" to 14, "Stealth" to 13),
            gear = listOf("Cyberdeck (10 Slots)", "Interface Plugs", "Neural Link",
                "Light Armorjack Body Armor (SP11)", "Program: Dragon", "Program: Asp",
                "Program: DeckKRASH", "Very Heavy Pistol", "Virtuality Goggles"),
            headArmorSp = 0, bodyArmorSp = 11,
            description = "فرمانده‌ی میدانی وودو بویز؛ ساکت و کشنده، با یک نگاه تیمت را در نت تکه‌تکه می‌کند.",
            personality = "خونسرد، کم‌حرف و بی‌رحم؛ آدم‌ها را مثل داده‌های قابل‌حذف می‌بیند.",
            motivation = "محافظت از شبکه‌ی مخفی وودو بویز و رسیدن به لایه‌های عمیق‌تر نت قدیم.",
            identifying = "قد بلند، هودی مشکی با نئون سبز، پورت‌های داده روی سر تراشیده و انعکاس کد سبز روی صورت."
        ),
        NpcTemplate(
            "Sasquatch — Animals Gang Leader", CAT_BOSS, NpcDifficulty.HARD, role = "Solo",
            stats = Stats(10, 8, 6, 4, 9, 9, 5, 6, 10, 2),
            skillTotals = mapOf("Brawling" to 18, "Melee Weapon" to 17, "Athletics" to 18,
                "Evasion" to 13, "Perception" to 12, "Concentration" to 15, "Shoulder Arms" to 12,
                "Resist Torture/Drugs" to 17, "Intimidation" to 17),
            gear = listOf("Very Heavy Melee Weapon", "Grafted Muscle and Bone Lace",
                "Subdermal Armor", "Heavy Armorjack Body Armor (SP13)", "Heavy Armorjack Head Armor (SP13)",
                "Shotgun", "Black Lace", "Basic Shotgun Shells x10"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "غول زن گنگ انیمالز؛ کوهی از عضله و سایبر-عضله که با دست خالی در می‌زند و در نمی‌آید.",
            personality = "پرخاشجوی بی‌باک و عاشق نبرد تن‌به‌تن؛ درد را نادیده می‌گیرد و می‌خندد.",
            motivation = "قوی‌تر شدن، شکستن هر حریفی و غارت فروشگاه‌ها در حمله‌ی گنگ به مرکز خرید.",
            identifying = "هیکل غول‌پیکر، موی بسیار کوتاه، تانک‌تاپ سفید با مهار، رنگ جنگی روی صورت و مشت‌های پوشیده از نوار."
        ),
        NpcTemplate(
            "Oda — Arasaka Cyberninja", CAT_BOSS, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(8, 9, 9, 8, 9, 8, 6, 7, 8, 4),
            skillTotals = mapOf("Autofire" to 16, "Melee Weapon" to 17, "Brawling" to 15,
                "Evasion" to 17, "Perception" to 15, "Concentration" to 16, "Tactics" to 16,
                "Athletics" to 16, "Stealth" to 16, "Shoulder Arms" to 14, "Martial Arts" to 15),
            gear = listOf("Mantis Blades", "Sandevistan", "Kerenzikov Boostware", "Neural Link",
                "Subdermal Armor", "Heavy Armorjack Body Armor (SP13)", "Heavy Armorjack Head Armor (SP13)",
                "Assault Rifle", "Targeting Scope", "Flashbang Grenade x2"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "نینجای سایبری آراساکا؛ محافظ شخصی هاناکو، با مَ‌تیس بلید و ساندویستان به‌سرعت فکر ظاهر و ناپدید می‌شود.",
            personality = "منضبط، شرافتمند و مرگبار؛ فقط به آراساکا و سوگندش وفادار است.",
            motivation = "انجام بی‌نقص مأموریت و حفاظت از جان هاناکو آراساکا به هر قیمت.",
            identifying = "زره سایبری مشکی و قرمز یکپارچه، وی‌زور قرمز درخشان، مَ‌تیس بلیدهای جمع‌شونده و حرکات فوق‌سریع."
        ),
        NpcTemplate(
            "Adam Smasher — The Legend", CAT_BOSS, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(8, 10, 8, 8, 10, 10, 6, 8, 12, 0),
            skillTotals = mapOf("Autofire" to 18, "Shoulder Arms" to 18, "Brawling" to 18,
                "Melee Weapon" to 17, "Concentration" to 16, "Evasion" to 16, "Perception" to 16,
                "Tactics" to 17, "Athletics" to 18, "Resist Torture/Drugs" to 18, "Heavy Weapons" to 17),
            gear = listOf("Grafted Muscle and Bone Lace", "Cyberarm", "Cyberarm", "Cyberleg", "Cyberleg",
                "Subdermal Armor", "Sandevistan", "Kerenzikov", "Assault Cannon", "Rocket Launcher",
                "Smart Shotgun", "Mantis Blades", "Head Armor (SP13)", "Body Armor (SP13)", "Targeting Scope"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "باس نهایی. بدن ۱۲، زره سنگین، تسلیحات انفجاری و سرعت ساندویستان. فقط برای تیم‌های کاملاً مجهز و سطح بالا.",
            personality = "بی‌رحم، خودبرتربین و بی‌اعتنا به انسانیت؛ شکار اِدج‌رانرها برایش سرگرمی است.",
            motivation = "خدمت به آراساکا و شکار بزرگ‌بعدی؛ ثابت‌کردن که ماشین کامل است و انسان ضعیف.",
            identifying = "بدن سایبری عظیم تمام‌فلزی، شانه‌های غول‌پیکر توپ‌دار، چشمان قرمز و صدای موتور پاها به‌جای قدم."
        ),

        NpcTemplate(
            "Fixer Kingpin", CAT_BOSS, NpcDifficulty.HARD, role = "Fixer",
            stats = Stats(9, 6, 6, 7, 9, 8, 7, 6, 6, 7),
            skillTotals = mapOf("Persuasion" to 17, "Human Perception" to 16, "Streetwise" to 16,
                "Trading" to 16, "Handgun" to 12, "Perception" to 13, "Bureaucracy" to 13),
            gear = listOf("Very Heavy Pistol", "Basic Pistol Ammunition x30",
                "Medium Armorjack Body Armor (SP12)", "Agent", "Neural Link", "Cyberaudio Suite"),
            headArmorSp = 0, bodyArmorSp = 12,
            description = "خودش نمی‌جنگد. ولی می‌داند کرو کجا می‌خوابد و چه کسی را دوست دارد.",
            personality = "آرام و بی‌نهایت صبور؛ تهدیدش هیچ‌وقت صریح نیست.",
            motivation = "گسترش شبکه؛ هر کسی روزی به کار می‌آید.",
            identifying = "همیشه در یک میز ثابت، با دو نفر که هیچ‌وقت نمی‌نشینند."),
        NpcTemplate(
            "Gang Warlord", CAT_BOSS, NpcDifficulty.VERY_HARD, role = "Solo",
            stats = Stats(7, 9, 7, 6, 8, 9, 5, 7, 10, 3),
            skillTotals = mapOf("Autofire" to 16, "Handgun" to 16, "Melee Weapon" to 15,
                "Intimidation" to 16, "Tactics" to 14, "Perception" to 13, "Evasion" to 13),
            gear = listOf("Assault Rifle", "Basic Rifle Ammunition x70", "Very Heavy Pistol",
                "Very Heavy Melee Weapon", "Metalgear Body Armor (SP13)",
                "Metalgear Head Armor (SP13)", "Sandevistan", "Subdermal Armor", "Neural Link"),
            headArmorSp = 13, bodyArmorSp = 13,
            description = "رهبر یک گنگ بزرگ. هیچ‌وقت تنها نیست و هیچ‌وقت اول حمله نمی‌کند.",
            personality = "باهوش و بی‌رحم؛ خشونت را حساب‌شده خرج می‌کند.",
            motivation = "گسترش قلمرو و حذف هر کسی که قدرتش را به چالش بکشد.",
            identifying = "زره سفارشی با نشان گنگ و همیشه دو محافظ در دو قدمی."),
    )

    // ============================================================
    //  بخش ۴ب — Team Memberهای کتاب Core (نقش اگزک / Teamwork)
    // ============================================================
    //
    // پنج کلاس Team Member کتاب، هر کدام با جدول استات ۶ ردیفی (d6) و بستهٔ
    // مهارت/سایبرور/تجهیزات خودش. تمپلیت‌ها با «ردیف ۳» جدول ساخته می‌شوند
    // (رولِ میانه‌ی کتاب) تا یک NPC آماده‌ی قابل بازی داشته باشیم؛ جدول کامل
    // ردیف‌ها در تب BIO و دستیار GM نشان داده می‌شود تا GM بتواند همان رول
    // واقعی d6 را هم بیندازد. هیچ عددی از خودمان اضافه نشده.

    private fun execRowToStats(row: List<Int>) = Stats(
        int = row[0], ref = row[1], dex = row[2], tech = row[3], cool = row[4],
        will = row[5], luck = 5, move = row[6], body = row[7], emp = row[8]
    )

    private fun execSkillTotals(row: List<Int>, cls: ExecTeamClass): Map<String, Int> {
        val st = execRowToStats(row)
        fun split(joined: String): List<String> =
            joined.split("،").map { it.trim() }.filter { it.isNotBlank() && it != "—" }
        val levels = LinkedHashMap<String, Int>()
        split(cls.skills2Fa).forEach { levels[it] = 2 }
        split(cls.skills4Fa).forEach { levels[it] = 4 }
        split(cls.skills6Fa).forEach { levels[it] = 6 }
        return levels.map { (raw, lvl) ->
            // پسوند «(x2)» فقط هزینه‌ی IP را عوض می‌کند؛ برای STAT نادیده گرفته می‌شود.
            val clean = raw.replace(Regex("\\s*\\(x2\\)\\s*$"), "").trim()
            val statName = SkillCatalog.statFor(clean)
            raw to (statValue(st, statName) + lvl)
        }.toMap()
    }

    private val execCoreTeam: List<NpcTemplate> = ExecTeamData.teamMemberClasses.map { cls ->
        // ردیف ۳ جدول کتاب: برای همه‌ی کلاس‌ها یک رول میانه و متعادل است.
        val row = cls.statRows.getOrElse(2) { cls.statRows.first() }
        NpcTemplate(
            name = cls.name,
            category = CAT_EXEC,
            difficulty = NpcDifficulty.MEDIUM,
            role = when {
                cls.name.contains("Netrunner", true) -> "Netrunner"
                cls.name.contains("Technician", true) -> "Tech"
                else -> "Solo"
            },
            stats = execRowToStats(row),
            skillTotals = execSkillTotals(row, cls),
            gear = cls.gearFa.split("،").map { it.trim() }.filter { it.isNotBlank() },
            headArmorSp = 11,
            bodyArmorSp = 11,
            description = cls.trueJobFa + " • نقش پوششی: " + cls.coverJobsFa,
            personality = "کارمند شرکت؛ حرفه‌ای، محتاط و پایبند به سیاست‌های داخلی.",
            motivation = "انجام‌دادن کار و نگه‌داشتن شغل؛ Loyalty با اگزک تعیین می‌کند تا کجا پیش می‌رود.",
            identifying = "لباس تجاری شرکت، Agent شخصی و تجهیزات استاندارد واحد."
        )
    }

    val allTemplates: List<NpcTemplate> = gang + cyberpsychos + lawmanBackup + execTeam + execCoreTeam + roleSpecialists + bosses

    fun templatesByCategory(category: String): List<NpcTemplate> =
        allTemplates.filter { it.category == category }

    private fun pregeneratedRoleBonus(role: String, skillName: String): Int = when {
        role.equals("Nomad", true) && skillName in setOf(
            "Drive Land Vehicle", "Pilot Air Vehicle", "Pilot Sea Vehicle",
            "Air Vehicle Tech", "Land Vehicle Tech", "Sea Vehicle Tech"
        ) -> 4 // Pregenerated NPCs are created at Role Rank 4 below.
        else -> 0
    }

    private fun statValue(stats: Stats, statName: String): Int = when (statName) {
        "Intelligence" -> stats.int
        "Reflexes" -> stats.ref
        "Dexterity" -> stats.dex
        "Technique" -> stats.tech
        "Cool" -> stats.cool
        "Willpower" -> stats.will
        "Luck" -> stats.luck
        "Movement" -> stats.move
        "Body" -> stats.body
        "Empathy" -> stats.emp
        else -> 0
    }

    // تبدیل قالب آماده به مدل Character (isAlly = false یعنی NPC).
    // skillTotals در قالب‌ها Skill Base (STAT + Skill Level) است؛ Character باید فقط Level را ذخیره کند.
    fun toCharacter(template: NpcTemplate): Character {
        val parsed = EquipmentParser.parse(template.gear)
        val skills = template.skillTotals.map { (rawName, total) ->
            val name = rawName.replace(Regex("\\s*\\(x2\\)\\s*$", RegexOption.IGNORE_CASE), "").trim()
            if (SkillCatalog.isKnownSkill(name)) {
                val stat = SkillCatalog.statFor(name)
                val level = (total - statValue(template.stats, stat) - pregeneratedRoleBonus(template.role, name)).coerceIn(0, 10)
                SkillData(name = name, stat = stat, level = level)
            } else {
                // Homebrew/pseudo skills in pregenerated NPC data already contain the final displayed Base.
                // Mark them stat-less so the UI does not add INT a second time.
                SkillData(name = rawName, stat = "None", level = total.coerceAtLeast(0))
            }
        }.sortedBy { it.name }

        return Character(
            name = template.name,
            handle = template.name,
            role = template.role,
            creationMethod = "Pregenerated NPC",
            improvementPoints = 0,
            roleRank = 4,
            reputation = 0,
            hp = StreetratData.calculateMaxHp(template.stats.body, template.stats.will),
            maxHp = StreetratData.calculateMaxHp(template.stats.body, template.stats.will),
            headArmorSp = template.headArmorSp,
            bodyArmorSp = template.bodyArmorSp,
            currentHumanity = template.stats.emp * 10,
            maxHumanity = template.stats.emp * 10,
            currentLuck = template.stats.luck,
            maxLuck = template.stats.luck,
            eurodollars = 0,
            isAlly = false,
            npcCategory = template.category,
            npcTier = template.difficulty.label,
            notes = template.description,
            lifepath = LifepathData(
                personality = template.personality.ifBlank { template.description.take(120) },
                clothingStyle = template.identifying.ifBlank { "لباس عملیاتی سیاه با جلیقه‌ی ضدگلوله" },
                affectation = template.identifying,
                valueMost = template.motivation,
                roleLifepath = template.description
            ),
            stats = template.stats,
            skills = skills,
            weapons = parsed.weapons,
            inventory = parsed.inventory
        )
    }

    // ساخت یک NPC خالی (Empty) برای ویرایش آزادانه
    fun emptyNpc(name: String = "Empty NPC"): Character {
        val stats = Stats(6, 6, 6, 6, 6, 6, 6, 6, 6, 6)
        val baseLevels = StreetratData.baseLifeSkills.associateWith { 2 }
        val skills = baseLevels.map { (n, level) ->
            SkillData(name = n, stat = SkillCatalog.statFor(n), level = level)
        }
        return Character(
            name = name,
            handle = name,
            role = "Solo",
            creationMethod = "Empty NPC",
            roleRank = 0,
            hp = StreetratData.calculateMaxHp(stats.body, stats.will),
            maxHp = StreetratData.calculateMaxHp(stats.body, stats.will),
            currentHumanity = stats.emp * 10,
            maxHumanity = stats.emp * 10,
            currentLuck = stats.luck,
            maxLuck = stats.luck,
            isAlly = false,
            npcCategory = "سفارشی (Custom)",
            npcTier = "",
            notes = "",
            stats = stats,
            skills = skills
        )
    }
}
