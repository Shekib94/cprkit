package com.cyberpunk.gmtool.data

/**
 * توانایی Backup لاومن (Core: Role Ability «Backup»).
 *
 * قواعد دقیقی که اینجا کد شده‌اند:
 * • در خطر (یا پیش از شروع Initiative، یا در نبرد با یک Action) تماس می‌گیری و
 *   یک **d10** می‌ریزی؛ اگر نتیجه **مساوی یا کمتر از Rank** باشد، کسی پاسخ می‌دهد.
 *   اگر کسی جواب نداد، نوبت بعد می‌توانی دوباره تلاش کنی.
 * • بعد از پاسخ، **1d6** می‌ریزی: نیروها در همان تعداد Rounds می‌رسند.
 * • اگر آن d6 عدد **۶** بیاید، نیرویی که می‌رسد **یک رده بالاتر** است
 *   (و در Rank ۱۰ دو گروه جدا می‌رسند).
 * • نیروهای Backup به‌جای STAT+Skill یک **Combat Number** واحد دارند که برای
 *   حمله و دفاع استفاده می‌شود و **نمی‌توانند از گلوله Dodge کنند**.
 *
 * نکته‌ی مهم برای GM: لازم نیست همه‌ی این NPCها از قبل ساخته شوند. تابلوی
 * زیر می‌گوید «کدام رده با این Rank می‌آید» و با یک دکمه همان تعداد NPC از
 * تمپلیت آماده در Characters ساخته می‌شود (تا در نبرد/نقشه استفاده شوند).
 */
object BackupRules {

    data class Unit(
        val tierName: String,
        val tierNameFa: String,
        val count: Int,
        val gearFa: String,
        val combatNumber: Int,
        val sp: Int,
        val hp: Int,
        val moveBody: Int,
        val transportFa: String,
        /** نام تمپلیت آماده در NpcData که با یک دکمه به NPCها اضافه می‌شود. */
        val npcTemplateName: String,
        /** اگر تمپلیت آماده دقیقاً همین واحد نباشد، اینجا شفاف گفته می‌شود. */
        val templateNoteFa: String = "",
        /** توصیف کامل همین رده از کتاب Core (برای نمایش در BIO و دستیار GM). */
        val descriptionFa: String = ""
    )

    /** رده‌های Backup بر اساس Rank (Core). */
    private val corporateSecurity = Unit(
        "Corporate Security", "امنیت شرکتی (Renta-Cop)", 4,
        "Heavy Pistol • Kevlar", 8, 7, 20, 4, "پیاده", "Corporate Bodyguard",
        "تمپلیت «Corporate Bodyguard» نزدیک‌ترین گزینه در برنامه است.",
        descriptionFa = "امنیت شرکتی: چهار رنتا-کاپ محلی روی صحنه، پیاده می‌رسند. Heavy Pistol حمل می‌کنند و Kevlar پوشیده‌اند."
    )
    private val localBeatCops = Unit(
        "Local Beat Cops", "پلیس محله", 4,
        "Heavy Pistol • Kevlar", 10, 7, 25, 5, "دو Compact Groundcar", "Local Beat Cop (×4)",
        descriptionFa = "پلیس محله: چهار پلیس محلی. با دو Compact Groundcar می‌رسند. Heavy Pistol حمل می‌کنند و Kevlar پوشیده‌اند."
    )
    private val countyMounties = Unit(
        "Sheriff's Department", "کلانتر شهرستان (County Mounties)", 2,
        "Heavy Pistol • Assault Rifle • Heavy Armorjack", 14, 13, 35, 4, "High Performance Groundcar", "County Mounty (×2)",
        descriptionFa = "کلانتری شهرستان: دو «County Mounty» محلی که در حاشیه‌ی شهر و بزرگراه‌های اطراف گشت می‌زنند. با یک High Performance Groundcar می‌رسند، Heavy Pistol و Assault Rifle دارند و Heavy Armorjack پوشیده‌اند."
    )
    private val recoveryMarshal = Unit(
        "Recovery Zone Marshal", "مارشالِ Recovery Zone", 1,
        "Very Heavy Pistol • Assault Rifle • Grenade Launcher • Flak Armor", 16, 15, 50, 6, "Superbike", "NCPD Detective",
        "مارشال در تمپلیت‌های برنامه نیست؛ «NCPD Detective» به‌عنوان پایه ساخته می‌شود — CN/SP/HP کارت را دستی هم‌راستا کن.",
        descriptionFa = "مارشال Recovery Zone: مثل مارشال‌های غرب قدیم، این‌ها لاومن‌های تنها هستند که در Recovery Zoneها و شهرهای تازه گشت می‌زنند. یک نفر با Superbike می‌رسد و Very Heavy Pistol، Assault Rifle و Grenade Launcher حمل می‌کند و Flak Armor پوشیده است.",
    )
    private val cswat = Unit(
        "C-SWAT (Psycho Squad)", "C-SWAT / جوخه‌ی سایکو", 2,
        "Assault Rifle • Rocket Launcher • Metalgear", 15, 18, 35, 4, "AV-4 (از هوا)", "MaxTac Operative",
        descriptionFa = "C-SWAT: دو ضربه‌زن سنگین از جوخه‌ی سایکو. Assault Rifle و Rocket Launcher حمل می‌کنند و Metalgear پوشیده‌اند. با AV-4 از هوا می‌رسند."
    )
    private val nationalLawEnforcement = Unit(
        "National Law Enforcement", "پلیس ملی / Interpol / Netwatch", 2,
        "Very Heavy Pistol • Assault Rifle • Light Armorjack", 14, 11, 35, 6, "AV-4", "National Law Enforcement (×2)",
        descriptionFa = "پلیس ملی / Interpol / FBI / Netwatch: این‌ها ضربه‌زن‌های جدی‌اند که زیر کنترل دولت‌های ملی یا گروه‌های انتظامی بین‌المللی کار می‌کنند. دوتایی سفر می‌کنند، با AV-4 می‌رسند و Very Heavy Pistol و Assault Rifle دارند و Light Armorjack پوشیده‌اند."
    )

    /** رده‌های ممکن به ترتیب Rank (۱ تا ۱۰). */
    val tiers: List<Unit> = listOf(
        corporateSecurity, corporateSecurity,   // 1-2
        localBeatCops, localBeatCops,           // 3-4
        countyMounties, countyMounties, countyMounties, // 5-7
        recoveryMarshal,                        // 8
        cswat,                                  // 9
        nationalLawEnforcement                  // 10
    )

    fun unitForRank(rank: Int): Unit = tiers[(rank - 1).coerceIn(0, tiers.lastIndex)]

    /** یک رده بالاتر — برای وقتی که تاسِ 1d6 عدد ۶ می‌آید. */
    fun upgradedUnit(rank: Int): Unit = unitForRank(rank + 1)

    /** اگر Rank برابر ۱۰ باشد و ۶ بیاید، Core می‌گوید دو گروه جدا می‌رسند. */
    fun upgradedCountIsDoubled(rank: Int): Boolean = rank >= 10

    /** تماس موفق است اگر تاس مساوی یا کمتر از Rank باشد (۱۰ روی Rank ۱۰ هم موفق است). */
    fun callSucceeds(rank: Int, d10: Int): Boolean = d10 <= rank

    /** تعداد Rounds تا رسیدن نیروها = خودِ تاس 1d6. */
    fun arrivalRounds(d6: Int): Int = d6.coerceIn(1, 6)

    /** آیا این تاسِ 1d6 رده را یک پله بالا می‌برد؟ */
    fun upgradesTier(d6: Int): Boolean = d6 == 6

    /** کلید اثر روی کاراکتر Lawman برای «تماس ثبت‌شده، منتظر رسیدن نیرو». */
    fun pendingEffectKey(rounds: Int): String = "backup_en_route_$rounds"

    fun isPending(character: Character): Boolean =
        character.combatEffects.keys.any { it.startsWith("backup_en_route_") }

    fun pendingRounds(character: Character): Int? =
        character.combatEffects.keys.firstOrNull { it.startsWith("backup_en_route_") }
            ?.removePrefix("backup_en_route_")?.toIntOrNull()

    val quickGuide: List<Pair<String, String>> = listOf(
        "تماس با Backup" to "در خطر (یا قبل از Initiative، یا با یک Action وسط نبرد) یک d10 بریز. نتیجه مساوی یا کمتر از Rank = کسی پاسخ می‌دهد. اگر جواب نداد، نوبت بعد می‌توانی دوباره تلاش کنی.",
        "زمان رسیدن" to "بعد از پاسخ، یک d6 بریز؛ همان تعداد Rounds تا رسیدن نیروها. اگر ۶ بیاید، نیروی یک رده بالاتر می‌آید (در Rank ۱۰ دو گروه).",
        "رده‌ی نیرو" to "۱-۲ امنیت شرکتی، ۳-۴ پلیس محله، ۵-۷ کلانتر شهرستان، ۸ مارشال، ۹ C-SWAT، ۱۰ پلیس ملی.",
        "طرز کار در نبرد" to "نیروها با یک Combat Number واحد (برای حمله و دفاع) می‌جنگند و نمی‌توانند از گلوله Dodge کنند. Core می‌گوید Backup جایگزینِ اقدام‌های خود لاومن نمی‌شود؛ فقط آتش پشتیبان است.",
        "سوءاستفاده" to "اگر بی‌دلیل هر صحنه Backup بخواهی، GM می‌تواند پاسخ را سخت‌تر کند — این توانایی «تاکسی و خدمتکار» نیست.",
        "در این برنامه" to "لازم نیست همه‌ی NPCها از قبل ساخته شوند: بعد از موفقیت، از همین صفحه دکمه‌ی «ساخت NPC نیروها» همان تعداد کاراکتر را از تمپلیت آماده می‌سازد تا در نبرد/نقشه استفاده شوند."
    )
}
