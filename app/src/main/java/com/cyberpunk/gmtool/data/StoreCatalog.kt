package com.cyberpunk.gmtool.data

/**
 * فهرست فروشگاه (Night Market) — قیمت‌ها و توضیحات بر پایه‌ی کتاب Core (صص. ۳۴۰ به بعد).
 * قیمت‌ها بر اساس بازار پایه (eb) هستند؛ ضریب فروشگاه روی اعمال می‌شود.
 */
data class StoreItem(
    val name: String,
    val basePrice: Int,
    val category: String,
    val description: String = "",
    // زیرنویس ریز لیست (اسلحه: دمیج+مهارت؛ سایبرویر: محل نصب؛ برنامه: کلاس)
    val subtitle: String = "",
    // آمار سلاح (CP:R Core)
    val damage: String = "",
    val weaponSkill: String = "",
    val concealed: String = "",
    val hands: String = "",
    val rof: String = "",
    val ammoType: String = "",
    val magStd: Int? = null,
    val magExt: Int? = null,
    val magDrum: Int? = null,
    val modes: String = "",
    val autofire: String = "",
    // جدول DV بُرد (فاصله -> DV)
    val rangeSingle: List<Pair<String, Int>> = emptyList(),
    val rangeAuto: List<Pair<String, Int>> = emptyList(),
    // سایبرویر
    val slot: String = "",
    val humanityLoss: Int? = null,
    val humanityDice: String = "",
    // برنامه‌های نت (برای دسته‌ی Programs)
    val programClass: String = "",
    val atk: Int? = null,
    val def: Int? = null,
    val rez: Int? = null,
    val per: Int? = null,
    val spd: Int? = null
)

object StoreCatalog {

    val categories = listOf("Weapons", "Weapon Mods", "Ammo", "Armor", "Cyberware", "Clothing", "Gear", "Drugs", "Hardware", "Programs", "Vehicles")

    // ضریب قیمت کیفیت ساخت (Poor / Standard / Excellent)
    fun qualityPrice(base: Int, quality: String): Int = when (quality) {
        "Poor" -> when (base) { 50 -> 20; 100 -> 50; 500 -> 100; else -> base }
        "Excellent" -> when (base) { 50 -> 100; 100 -> 500; 500 -> 1000; else -> base }
        else -> base
    }

    // جدول‌های DV بُرد بر پایه‌ی CP:R Core
    private fun dv(vararg pairs: Pair<String, Int>) = pairs.toList()
    private val DV_PISTOL = dv("0-6" to 13, "7-12" to 15, "13-25" to 20, "26-50" to 25, "51-100" to 30, "101-200" to 30)
    private val DV_RIFLE = dv("0-6" to 17, "7-12" to 16, "13-25" to 15, "26-50" to 13, "51-100" to 15, "101-200" to 20, "201-400" to 25, "401-800" to 30)
    private val DV_AR_AUTO = dv("0-6" to 22, "7-12" to 20, "13-25" to 17, "26-50" to 20, "51-100" to 25)
    private val DV_SMG_AUTO = dv("0-6" to 20, "7-12" to 17, "13-25" to 20, "26-50" to 25, "51-100" to 30)
    private val DV_SMG = dv("0-6" to 15, "7-12" to 13, "13-25" to 15, "26-50" to 20, "51-100" to 25, "101-200" to 25, "201-400" to 30)
    private val DV_SNIPER = dv("0-6" to 30, "7-12" to 25, "13-25" to 25, "26-50" to 20, "51-100" to 15, "101-200" to 16, "201-400" to 17, "401-800" to 20)
    private val DV_SHOTGUN = dv("0-6" to 13, "7-12" to 15, "13-25" to 20, "26-50" to 25, "51-100" to 30, "101-200" to 35)
    private val DV_BOW = dv("0-6" to 15, "7-12" to 13, "13-25" to 15, "26-50" to 17, "51-100" to 20, "101-200" to 22)
    private val DV_GRENADE_LAUNCHER = dv("0-6" to 16, "7-12" to 15, "13-25" to 15, "26-50" to 17, "51-100" to 20, "101-200" to 22, "201-400" to 25)
    private val DV_ROCKET = dv("0-6" to 17, "7-12" to 16, "13-25" to 15, "26-50" to 15, "51-100" to 20, "101-200" to 20, "201-400" to 25, "401-800" to 30)

    val items: List<StoreItem> = listOf(
        // ================= سلاح‌های گرم =================
        StoreItem("Medium Pistol", 50, "Weapons",
            "پیستول استاندارد و خوش‌دست. آسیب 2d6، خشاب ۱۲ تیر، برد مؤثر متوسط، تک‌تیر. تعادل خوب آسیب، دقت و مهمات ارزان؛ سلاح همراه همیشگی اکثر اِدج‌رانرهای تازه‌کار.",
            subtitle = "2d6 Handgun", damage = "2d6", weaponSkill = "Handgun", concealed = "Pocket",
            hands = "1", rof = "2", ammoType = "Pistol", magStd = 12, magExt = 18, magDrum = 36,
            modes = "Single Shot", rangeSingle = DV_PISTOL),
        StoreItem("Heavy Pistol", 100, "Weapons",
            "پیستول سنگینِ محبوب خیابان. آسیب 3d6، خشاب ۸ تیر. ضربه‌ی سهمگین‌تر از پیستول معمولی با همان قیمت پایه.",
            subtitle = "3d6 Handgun", damage = "3d6", weaponSkill = "Handgun", concealed = "Jacket",
            hands = "1", rof = "2", ammoType = "Pistol", magStd = 8, magExt = 14, magDrum = 28,
            modes = "Single Shot", rangeSingle = DV_PISTOL),
        StoreItem("Very Heavy Pistol", 100, "Weapons",
            "سنگین‌ترین پیستولِ یک‌دستی. آسیب 4d6، خشاب ۸ تیر. هر گلوله قدرتمند است و در برابر زره سبک هم اثر دارد؛ پس‌زدنی و سنگین.",
            subtitle = "4d6 Handgun", damage = "4d6", weaponSkill = "Handgun", concealed = "No",
            hands = "1", rof = "1", ammoType = "Pistol", magStd = 8, magExt = 14, magDrum = 28,
            modes = "Single Shot", rangeSingle = DV_PISTOL),
        StoreItem("SMG", 100, "Weapons",
            "مسلسل سبک دستی. آسیب 2d6، خشاب ۳۰ تیر و قابلیت رگبار (AutoFire) در فاصله نزدیک. کوچک و قابل‌حمل زیر کت؛ انتخاب رایج برای درگیری‌های نزدیک و فضای بسته.",
            subtitle = "2d6 Handgun", damage = "2d6", weaponSkill = "Handgun", concealed = "Longcoat",
            hands = "1", rof = "1", ammoType = "Pistol", magStd = 30, magExt = 40, magDrum = 50,
            modes = "Single Shot • Autofire (3) • Suppressive Fire", autofire = "3",
            rangeSingle = DV_SMG, rangeAuto = DV_SMG_AUTO),
        StoreItem("Heavy SMG", 100, "Weapons",
            "Heavy SMG؛ 3d6، خشاب 40، Single Shot با Handgun و Autofire/Suppressive Fire با Autofire.",
            subtitle = "3d6 Handgun", damage = "3d6", weaponSkill = "Handgun", concealed = "No",
            hands = "1", rof = "1", ammoType = "Pistol", magStd = 40, magExt = 50, magDrum = 60,
            modes = "Single Shot • Autofire (3) • Suppressive Fire", autofire = "3",
            rangeSingle = DV_SMG, rangeAuto = DV_SMG_AUTO),
        StoreItem("Assault Rifle", 500, "Weapons",
            "تفنگ تهاجمی همه‌کاره. آسیب 5d6، خشاب ۲۵ تیر، برد بلند و قابلیت AutoFire (رگبار کامل، +4). ستون فقرات آتش سنگین میدانی؛ در دور و نزدیک مرگبار.",
            subtitle = "5d6 Shoulder Arms", damage = "5d6", weaponSkill = "Shoulder Arms", concealed = "No",
            hands = "2", rof = "1", ammoType = "Rifle", magStd = 25, magExt = 35, magDrum = 45,
            modes = "Single Shot • Autofire (4) • Suppressive Fire", autofire = "4",
            rangeSingle = DV_RIFLE, rangeAuto = DV_AR_AUTO),
        StoreItem("Shotgun", 500, "Weapons",
            "شاتگان پمپی. آسیب 5d6، خشاب ۴ فشنگ. در فاصله نزدیک ویرانگر است و با ساچمه چند هدف را پوشش می‌دهد؛ برد مؤثر کوتاه. عالی برای پاک‌سازی اتاق.",
            subtitle = "5d6 Shoulder Arms", damage = "5d6", weaponSkill = "Shoulder Arms", concealed = "No",
            hands = "2", rof = "1", ammoType = "Shotgun Shells", magStd = 4, magExt = 8, magDrum = 16,
            modes = "Single Shot • Shotgun Shell",
            rangeSingle = DV_SHOTGUN),
        StoreItem("Sniper Rifle", 500, "Weapons",
            "تفنگ تک‌تیرانداز. آسیب 5d6، خشاب ۴ تیر، دقت و برد بسیار بالا. برای حذف یک هدف از فاصله‌ی امن؛ سرعت آتش پایین.",
            subtitle = "5d6 Shoulder Arms", damage = "5d6", weaponSkill = "Shoulder Arms", concealed = "No",
            hands = "2", rof = "1", ammoType = "Rifle", magStd = 4, magExt = 8, magDrum = 12,
            modes = "Single Shot", rangeSingle = DV_SNIPER),
        StoreItem("Rocket Launcher", 500, "Weapons",
            "راکت‌انداز یک‌شلیک. آسیب 8d6 با انفجار گسترده (AoE). ضد زره سنگین، وسایل نقلیه و گروه‌های فشرده‌ی دشمن؛ مهماتش بسیار گران و کمیاب است.",
            subtitle = "8d6 Heavy Weapons", damage = "8d6", weaponSkill = "Heavy Weapons", concealed = "No",
            hands = "2", rof = "1", ammoType = "Rocket", magStd = 1, magExt = 2, magDrum = 3,
            modes = "Single Shot (انفجاری)", rangeSingle = DV_ROCKET),
        StoreItem("Grenade Launcher", 500, "Weapons",
            "نارنجک‌انداز؛ نارنجک‌ها را با برد و قوس دقیق پرتاب می‌کند. برای کوبیدن سنگر، دشمنان پشت کاور و مناطق دور دست.",
            subtitle = "6d6 Heavy Weapons", damage = "6d6", weaponSkill = "Heavy Weapons", concealed = "No",
            hands = "2", rof = "1", ammoType = "Grenade", magStd = 2, magExt = 4, magDrum = 6,
            modes = "Single Shot (انفجاری)", rangeSingle = DV_GRENADE_LAUNCHER),
        StoreItem("Bows / Crossbow", 100, "Weapons",
            "کمان/کراس‌بوی آرام. شلیک بی‌صدا و بدون رد گلوله یا پوکه؛ قابل استفاده با تیرهای ویژه (بیهوشی، انفجاری، کابل). مناسب حذف مخفیانه.",
            subtitle = "4d6 Archery", damage = "4d6", weaponSkill = "Archery", concealed = "No",
            hands = "2", rof = "1", ammoType = "Arrow", magStd = 1,
            modes = "Single Shot", rangeSingle = DV_BOW),
        StoreItem("Flamethrower", 500, "Weapons",
            "آتش‌زا؛ منطقه‌ی روبرو را به آتش می‌کشد و رعب ایجاد می‌کند. سوختش محدود و خطرناک برای خود کاربر هم هست.",
            subtitle = "3d6 Heavy Weapons", damage = "3d6", weaponSkill = "Heavy Weapons", concealed = "No",
            hands = "2", rof = "1", ammoType = "Shotgun Shells", magStd = 4,
            modes = "Incendiary Shotgun Shell only • No Aimed Shots", rangeSingle = DV_SHOTGUN),

        // ================= سلاح‌های سرد =================
        StoreItem("Light Melee Weapon", 50, "Weapons",
            "سلاح سرد سبک: چاقو، باتوم، قمه. آسیب 1d6. سریع، ارزان و قابل‌مخفی در لباس؛ برای دفاع شخصی و حذف بی‌سروصدا.",
            subtitle = "1d6 Melee Weapon", damage = "1d6", weaponSkill = "Melee Weapon", concealed = "Pocket",
            hands = "1", modes = "Melee"),
        StoreItem("Medium Melee Weapon", 50, "Weapons",
            "سلاح سرد متوسط. آسیب 2d6، ROF 2 و معمولاً قابل مخفی‌کردن نیست.",
            subtitle = "2d6 Melee Weapon", damage = "2d6", weaponSkill = "Melee Weapon", concealed = "No",
            hands = "1", rof = "2", modes = "Melee"),
        StoreItem("Heavy Melee Weapon", 100, "Weapons",
            "سلاح سرد سنگین: ماچته، تیغ پهن، باتوم بزرگ. آسیب 3d6. ضربه‌ی مهلک در دست کسی که جنگ تن‌به‌تن می‌داند.",
            subtitle = "3d6 Melee Weapon", damage = "3d6", weaponSkill = "Melee Weapon", concealed = "No",
            hands = "1", modes = "Melee"),
        StoreItem("Very Heavy Melee Weapon", 500, "Weapons",
            "سلاح سرد خیلی سنگین دو دستی: تیغ غول‌پیکر، پتک. آسیب 4d6. دیر نوسان می‌کند اما هر ضربه‌ی فرودآمده فلج‌کننده است؛ نیازمند قدرت بدنی بالا.",
            subtitle = "4d6 Melee Weapon", damage = "4d6", weaponSkill = "Melee Weapon", concealed = "No",
            hands = "2", modes = "Melee"),

        // ================= مهمات و نارنجک =================
        StoreItem("Basic Pistol Ammo x30", 30, "Ammo", "بسته‌ی استاندارد ۳۰ تیر پیستول؛ مهمات ارزان و در دسترس برای Medium/Heavy/Very Heavy Pistol.", subtitle = "Pistol • SMG • Heavy SMG",),
        StoreItem("Basic Pistol Ammo x100", 100, "Ammo", "ذخیره‌ی بزرگ ۱۰۰ تیر پیستول برای درگیری‌های طولانی و آتش پرحجم.", subtitle = "Pistol • SMG • Heavy SMG",),
        StoreItem("Basic Rifle Ammo x50", 50, "Ammo", "بسته‌ی استاندارد ۵۰ تیر تفنگ؛ برای Assault Rifle، Sniper Rifle و سلاح‌های خشاب‌بلند.", subtitle = "Assault Rifle • Sniper Rifle",),
        StoreItem("Basic Rifle Ammo x70", 70, "Ammo", "ذخیره‌ی بزرگ ۷۰ تیر تفنگ؛ مناسب کسی که زیاد AutoFire می‌زند.", subtitle = "Assault Rifle • Sniper Rifle",),
        StoreItem("Basic Shotgun Shells x10", 10, "Ammo", "بسته‌ی ۱۰ فشنگ شاتگان با ساچمه‌ی پراکنده؛ مرگبار در برد کوتاه.", subtitle = "Shotgun (shell)",),
        StoreItem("Basic Slug Shells x10", 10, "Ammo", "۱۰ فشنگ شاتگان با ساچمه‌ی تک‌گلوله (Slug) برای برد بیشتر و آسیب متمرکز.", subtitle = "Shotgun (slug)",),
        StoreItem("Armor Piercing Ammo x10", 100, "Ammo", "مهمات ضدزره؛ بهتر از زره عبور می‌کند و SP دشمن را کم‌اثرتر می‌کند. گران و نایاب در بازار آزاد.", subtitle = "Pistol • SMG • Rifle • Slug • Arrow — not Shells",),
        StoreItem("Incendiary Ammo x10", 100, "Ammo", "فشنگ آتش‌زا؛ هدف و پوشش محیطی را شعله‌ور می‌کند و رعب و آسیب در طول زمان ایجاد می‌کند.", subtitle = "Pistol • SMG • Rifle • Shell • Arrow • Grenade",),
        StoreItem("Flashbang Grenade", 100, "Weapons",
            "Flashbang Grenade؛ آسیب مستقیم ندارد. اهداف در ناحیه DV15 Resist Torture/Drugs می‌دهند؛ شکست = Damaged Eye + Damaged Ear برای یک دقیقه، بدون Bonus Damage.",
            subtitle = "Thrown Grenade • Athletics", damage = "0d6", weaponSkill = "Athletics", concealed = "Jacket", hands = "1", rof = "1", modes = "Flashbang • 10m/yd square", rangeSingle = DV_GRENADE_LAUNCHER),
        StoreItem("Smoke Grenade", 50, "Weapons",
            "Smoke Grenade؛ ناحیه 10m/yd × 10m/yd را یک دقیقه دود می‌گیرد؛ جریمه‌ی معمول کار در دود -4 است.",
            subtitle = "Thrown Grenade • Athletics", damage = "0d6", weaponSkill = "Athletics", concealed = "Jacket", hands = "1", rof = "1", modes = "Smoke • 10m/yd square", rangeSingle = DV_GRENADE_LAUNCHER),
        StoreItem("Teargas Grenade", 50, "Weapons",
            "Teargas Grenade؛ آسیب مستقیم ندارد. هدف دارای چشم طبیعی DV13 Resist Torture/Drugs؛ شکست = Damaged Eye برای یک دقیقه، بدون Bonus Damage.",
            subtitle = "Thrown Grenade • Athletics", damage = "0d6", weaponSkill = "Athletics", concealed = "Jacket", hands = "1", rof = "1", modes = "Teargas • 10m/yd square", rangeSingle = DV_GRENADE_LAUNCHER),
        StoreItem("Armor Piercing Grenade", 100, "Weapons",
            "Armor-Piercing Grenade؛ مهمات استاندارد انفجاری نارنجک با 6d6؛ هر بار که زره را ablate کند، 2 SP کم می‌کند.",
            subtitle = "Thrown Grenade • Athletics • 6d6", damage = "6d6", weaponSkill = "Athletics", concealed = "Jacket", hands = "1", rof = "1", modes = "Explosive • Armor Piercing", rangeSingle = DV_GRENADE_LAUNCHER),

        // ================= زره =================
        StoreItem("Kevlar® Body (SP7)", 50, "Armor",
            "Kevlar® بدن؛ SP7 و بدون Armor Penalty. می‌تواند به شکل لباس، جلیقه یا پوشش سبک ساخته شود.",
            subtitle = "SP7 • Body"),
        StoreItem("Kevlar® Head (SP7)", 50, "Armor",
            "Kevlar® سر؛ SP7 و بدون Armor Penalty.",
            subtitle = "SP7 • Head"),
        StoreItem("Light Armorjack Body (SP11)", 100, "Armor",
            "Light Armorjack بدن؛ SP11 و بدون Armor Penalty. ترکیبی از Kevlar® و شبکه‌های پلاستیکی تقویت‌شده است.",
            subtitle = "SP11 • Body"),
        StoreItem("Light Armorjack Head (SP11)", 100, "Armor",
            "Light Armorjack سر؛ SP11 و بدون Armor Penalty.",
            subtitle = "SP11 • Head"),
        StoreItem("Heavy Armorjack Body (SP13)", 500, "Armor",
            "Heavy Armorjack بدن؛ SP13 و -2 به REF، DEX و MOVE هنگام پوشیدن.",
            subtitle = "SP13 • Body"),
        StoreItem("Heavy Armorjack Head (SP13)", 500, "Armor",
            "Heavy Armorjack سر؛ SP13 و Armor Penalty مجموعه -2 به REF، DEX و MOVE است.",
            subtitle = "SP13 • Head"),
        StoreItem("Leathers Body (SP4)", 20, "Armor",
            "Leathers؛ زره سبک SP4 بدون جریمه‌ی REF/DEX/MOVE.", subtitle = "SP4 • Body"),
        StoreItem("Leathers Head (SP4)", 20, "Armor",
            "Leathers برای سر؛ SP4 و بدون Armor Penalty.", subtitle = "SP4 • Head"),
        StoreItem("Bodyweight Suit (SP11)", 1000, "Armor",
            "Bodyweight Suit؛ هم‌زمان Head و Body را با SP11 می‌پوشاند و Armor Penalty ندارد. هر دو محل SP مستقل دارند ولی با هم تعمیر می‌شوند؛ فقط یک Suit قابل پوشیدن است و یک Hardware-only Option Slot به Cyberdeck متصل اضافه می‌کند.",
            subtitle = "SP11 • Head + Body"),
        StoreItem("Medium Armorjack Body (SP12)", 100, "Armor",
            "Medium Armorjack؛ SP12 و Armor Penalty برابر -2 به REF/DEX/MOVE.", subtitle = "SP12 • Body • Penalty -2"),
        StoreItem("Medium Armorjack Head (SP12)", 100, "Armor",
            "Medium Armorjack سر؛ SP12 و Armor Penalty مجموعه برابر -2.", subtitle = "SP12 • Head • Penalty -2"),
        StoreItem("Flak Body (SP15)", 500, "Armor",
            "Flak؛ SP15 و Armor Penalty برابر -4 به REF/DEX/MOVE.", subtitle = "SP15 • Body • Penalty -4"),
        StoreItem("Flak Head (SP15)", 500, "Armor",
            "Flak سر؛ SP15 و Armor Penalty مجموعه برابر -4.", subtitle = "SP15 • Head • Penalty -4"),
        StoreItem("Metalgear® Body (SP18)", 5000, "Armor",
            "Metalgear®؛ SP18 و Armor Penalty برابر -4 به REF/DEX/MOVE.", subtitle = "SP18 • Body • Penalty -4"),
        StoreItem("Metalgear® Head (SP18)", 5000, "Armor",
            "Metalgear® سر؛ SP18 و Armor Penalty مجموعه برابر -4.", subtitle = "SP18 • Head • Penalty -4"),
        StoreItem("Bulletproof Shield", 100, "Armor",
            "Bulletproof Shield؛ 10 HP دارد و همیشه یک دست را اشغال می‌کند. وقتی بین کاربر و حمله قرار داده شود، Damage را به HP سپر می‌گیرد؛ پس از رسیدن به 0 HP دیگر قابل استفاده نیست.",
            subtitle = "10 HP Cover • Shield"),

        // ================= سایبرویر =================
        StoreItem("Neural Link", 500, "Cyberware",
            "کانون عصبی پایه؛ ۵ Option Slot برای Neuralware. HL 7 (2d6 پس از ساخت کاراکتر).",
            subtitle = "Neuralware", slot = "Clinic", humanityLoss = 7, humanityDice = "2d6"),
        StoreItem("Interface Plugs", 500, "Cyberware",
            "Neuralware Option؛ برای Smartgun، Cyberdeck و ماشین‌آلات. نیازمند Neural Link. HL 7.",
            subtitle = "Neuralware", slot = "Clinic", humanityLoss = 7, humanityDice = "2d6"),
        StoreItem("Sandevistan Speedware", 500, "Cyberware",
            "با یک Action فعال می‌شود؛ تا یک دقیقه +3 Initiative می‌دهد و سپس یک ساعت cooldown دارد. نیازمند Neural Link. HL 7.",
            subtitle = "Neuralware", slot = "Neuralware", humanityLoss = 7),
        StoreItem("Kerenzikov Boostware", 500, "Cyberware",
            "Speedware همیشه‌روشن؛ +2 Initiative. فقط یک Speedware می‌تواند نصب باشد. نیازمند Neural Link. HL 14.",
            subtitle = "Neuralware", slot = "Clinic", humanityLoss = 14, humanityDice = "4d6"),
        StoreItem("Cyberarm", 500, "Cyberware",
            "بازوی مصنوعی پایه با 4 Option Slot و Standard Hand رایگان. HL 7 (2d6 پس از ساخت کاراکتر).",
            subtitle = "Cyberarm", slot = "Hospital", humanityLoss = 7, humanityDice = "2d6"),
        StoreItem("Cyberleg", 100, "Cyberware",
            "پای مصنوعی پایه با ۳ Option Slot؛ مزایای حرکت از Optionهایی مانند Jump Booster/Skate Foot می‌آیند. HL 3.",
            subtitle = "Cyberleg", slot = "Hospital", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Grafted Muscle and Bone Lace", 1000, "Cyberware",
            "پیوند ماهیچه و توری استخوانی؛ قدرت بدنی را برای آسیب تن‌به‌تن و حمل بار بالا می‌برد و استخوان‌ها را تقویت می‌کند.",
            subtitle = "Internal Body Cyberware", slot = "Hospital", humanityLoss = 14, humanityDice = "4d6"),
        StoreItem("Subdermal Armor", 1000, "Cyberware",
            "زره زیرپوستی؛ Head و Body را SP11 می‌کند. با درمان طبیعی روزانه 1 SP ترمیم می‌شود. HL 14.",
            subtitle = "External Body Cyberware", slot = "Hospital", humanityLoss = 14, humanityDice = "4d6"),
        StoreItem("Mantis Blades", 500, "Cyberware",
            "Homebrew / Cyberpunk 2077 Adaptation — جزو Core RED نیست. تیغ‌های جمع‌شونده در ساعد.",
            subtitle = "HOME BREW • 2077 Adaptation", slot = "Clinic", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Wolvers", 500, "Cyberware",
            "Cyberarm Option؛ Heavy Melee Weapon با 3d6، ROF 2 و قابل مخفی‌سازی بدون Check. HL 7.",
            subtitle = "Cyberarm Option", slot = "Clinic", humanityLoss = 7, humanityDice = "2d6"),
        StoreItem("Monowire", 500, "Cyberware",
            "Homebrew / Cyberpunk 2077 Adaptation — جزو Core RED نیست. سیم تک‌رشته‌ای برنده‌ی مچی.",
            subtitle = "HOME BREW • 2077 Adaptation", slot = "Clinic", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Cybereye", 100, "Cyberware",
            "چشم مصنوعی پایه با 3 Option Slot. HL 7 (2d6 پس از ساخت کاراکتر).",
            subtitle = "Cyberoptics", slot = "Clinic", humanityLoss = 7, humanityDice = "2d6"),
        StoreItem("Targeting Scope", 500, "Cyberware",
            "Cybereye Option؛ +1 به Check هنگام Aimed Shot. نیازمند Cybereye. HL 3.",
            subtitle = "Cyberoptics", slot = "Clinic", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("MicroOptics", 100, "Cyberware",
            "Cybereye Option؛ میکروسکوپ تا 400×. نیازمند Cybereye. HL 2.",
            subtitle = "Cyberoptics", slot = "Clinic", humanityLoss = 2, humanityDice = "1d6/2 Round up"),
        StoreItem("TeleOptics", 500, "Cyberware",
            "Cybereye Option؛ دید جزئیات تا 800m/yd و +1 به Single Shot/Aimed Shot علیه هدف 51m/yd یا دورتر. نیازمند Cybereye. HL 3.",
            subtitle = "Cyberoptics", slot = "Clinic", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Cyberaudio Suite", 500, "Cyberware",
            "Cyberaudio Suite پایه با 3 Option Slot. HL 7 (2d6 پس از ساخت کاراکتر).",
            subtitle = "Cyberaudio", slot = "Clinic", humanityLoss = 7, humanityDice = "2d6"),
        StoreItem("Amplified Hearing", 100, "Cyberware",
            "Cyberaudio Option؛ +2 Perception برای Checkهای مبتنی بر شنوایی. نیازمند Cyberaudio Suite. HL 3.",
            subtitle = "Cyberaudio", slot = "Mall", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Audio Recorder (Cyberaudio)", 100, "Cyberware",
            "Cyberaudio Option؛ صدا را روی Memory Chip یا Agent ذخیره می‌کند. نیازمند Cyberaudio Suite. HL 2.",
            subtitle = "Cyberaudio", slot = "Clinic", humanityLoss = 2, humanityDice = "1d6/2 Round up"),
        StoreItem("Voice Stress Analyzer", 100, "Cyberware",
            "Cyberaudio Option؛ +2 Human Perception و Interrogation و تابع تشخیص دروغ. نیازمند Cyberaudio Suite. HL 3.",
            subtitle = "Cyberaudio", slot = "Mall", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Nasal Filters", 100, "Cyberware",
            "ایمنی در برابر گازها، بخارهای سمی و خطرهای مشابه استنشاقی؛ قابل غیرفعال‌کردن بدون Action. HL 2.",
            subtitle = "Internal Body Cyberware", slot = "Clinic", humanityLoss = 2, humanityDice = "1d6/2"),
        StoreItem("Toxin Binders", 100, "Cyberware",
            "+2 به Resist Torture/Drugs؛ نصب چند نسخه فایده‌ی بیشتری ندارد. HL 2.",
            subtitle = "Internal Body Cyberware", slot = "Clinic", humanityLoss = 2, humanityDice = "1d6/2"),
        StoreItem("Biomonitor", 100, "Cyberware",
            "Fashionware؛ نمایش علائم حیاتی و قابلیت اتصال به Agent. HL 0.",
            subtitle = "Fashionware", slot = "Mall", humanityLoss = 0, humanityDice = "N/A"),
        StoreItem("Tool Hand", 100, "Cyberware",
            "Cyberarm Option؛ ابزارهای فنی در انگشت‌ها. می‌تواند تنها Cyberware یک meat arm باشد. HL 3.",
            subtitle = "Cyberarm Option / meat arm", slot = "Clinic", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Internal Agent", 100, "Cyberware",
            "Cyberaudio Option؛ Agent کامل داخلی. برای Chyron display می‌تواند به Cybereye لینک شود؛ در غیر این صورت audio-only. نیازمند Cyberaudio Suite. HL 3.",
            subtitle = "Cyberaudio Option", slot = "Mall", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Subdermal Pocket", 100, "Cyberware",
            "External Body Cyberware؛ فضای 2×4 اینچ با زیپ Realskinn؛ محتویات بدون Check مخفی می‌شوند. HL 3.",
            subtitle = "External Body Cyberware", slot = "Clinic", humanityLoss = 3, humanityDice = "1d6"),
        StoreItem("Hidden Holster", 500, "Cyberware",
            "External Body Cyberware؛ سلاحی که ذاتاً Concealable است بدون Check مخفی و بدون Action کشیده می‌شود. HL 7.",
            subtitle = "External Body Cyberware", slot = "Clinic", humanityLoss = 7, humanityDice = "2d6"),
        StoreItem("Chemskin", 100, "Cyberware",
            "Fashionware؛ رنگ و رنگدانه‌ی دائمی پوست. همراه Techhair به Personal Grooming +2 می‌دهد. HL 0.",
            subtitle = "Fashionware", slot = "Mall", humanityLoss = 0, humanityDice = "N/A"),
        StoreItem("Tech Hair", 100, "Cyberware",
            "Fashionware؛ موی مصنوعی نورانی/تغییررنگ. همراه Chemskin به Personal Grooming +2 می‌دهد. HL 0.",
            subtitle = "Fashionware", slot = "Mall", humanityLoss = 0, humanityDice = "N/A"),
        StoreItem("Light Tattoo", 100, "Cyberware",
            "Fashionware؛ سه نصب یا بیشتر +2 Wardrobe & Style می‌دهد (فقط یک‌بار). HL 0.",
            subtitle = "Fashionware", slot = "Mall", humanityLoss = 0, humanityDice = "N/A"),
        StoreItem("Skinwatch", 100, "Cyberware",
            "Fashionware؛ نمایش دائمی زمان و تاریخ زیر پوست. HL 0.",
            subtitle = "Fashionware", slot = "Mall", humanityLoss = 0, humanityDice = "N/A"),
        StoreItem("Shift Tacts", 100, "Cyberware",
            "Fashionware؛ لنزهای کاشته‌شده‌ی تغییررنگ/طرح که می‌توانند بدون Action غیرفعال شوند. HL 0.",
            subtitle = "Fashionware", slot = "Mall", humanityLoss = 0, humanityDice = "N/A"),
        StoreItem("Chipware Socket", 500, "Cyberware",
            "Neuralware Option برای یک Chipware؛ نیازمند Neural Link. خود Chipware اسلات Neural Link را مصرف نمی‌کند. HL 7.",
            subtitle = "Neuralware Option", slot = "Clinic", humanityLoss = 7, humanityDice = "2d6"),

        // ================= لباس Core: style × piece =================
        *FashionCatalog.storeItems.toTypedArray(),

        // ================= وسایل کاربردی =================
        StoreItem("Agent", 100, "Gear",
            "دستیار هوشمند شخصیِ رایج در Time of the Red که با تعامل با صاحبش عادت‌ها و نیازهای او را یاد می‌گیرد. Agent تماس صوتی و تصویری، ضبط و انتقال تماس، پیام و خبر، جست‌وجو و مسیریابی در Data Pool، تقویم و یادآوری و انجام خودکار کارهای روزمره مثل سفارش کالا را مدیریت می‌کند. شخصیت، نام، صدا و آواتار مجازی قابل تنظیم دارد؛ می‌تواند صوت و تصویر را روی Memory Chip ذخیره کند، با Cyberware و وسایل خانه تبادل داده داشته باشد و منابع معمول مصرفی را پایش و در قیمت بازار دوباره سفارش دهد. همچنین بر اساس هدف‌های کاربر پیشنهاد ارائه می‌کند. از نظر مکانیکی +2 به Library Search می‌دهد و اگر کاربر لباس‌های پیشنهادی فصلی Agent را بپوشد +2 به Wardrobe & Style می‌دهد؛ داشتن چند Agent این بونوس‌ها را روی هم جمع نمی‌کند.", subtitle = "Personal SAAI • +2 Library Search • Wardrobe assistant"),
        StoreItem("Virtuality Goggles", 100, "Gear", "عینک واقعیت مجازی؛ برای ورود بصری به نت و مشاهده‌ی محیط مجازی بدون کاشت مستقیم.", subtitle = "AR overlay • Netrunner support"),
        StoreItem("Tech Bag", 500, "Gear", "کیف ابزار تکنسین؛ مجموعه‌ی ابزار پایه برای کارهای فنی، تعمیر و ساخت.", subtitle = "Required for Tech repairs & upgrades"),
        StoreItem("Medtech Bag", 100, "Gear", "کیف پزشکی با لوازم First Aid و Paramedic؛ برای درمان میدانی و پایدارکردن زخمی.", subtitle = "Required for First Aid & Surgery"),
        StoreItem("Airhypo", 50, "Gear", "سرنگ پرفشار برای تزریق سریع دارو/دُز؛ یک اکشن، و روی هدف ناراضی می‌توان به‌صورت حمله‌ی غوغا تزریق کرد.", subtitle = "Delivers drugs • 1 Action willing target"),
        StoreItem("Radio Scanner/Music Player", 50, "Gear", "اسکنر امواج؛ شنود فرکانس پلیس، گنگ و شرکت برای پیش‌دستی در وقایع.", subtitle = "Scan radio bands • DV13 Electronics"),
        StoreItem("Radio Communicator", 100, "Gear", "بی‌سیم گروهی برای هماهنگی تیم در میدان؛ لازمه‌ی عملیات چندنفره.", subtitle = "Team comms • 1.5km range"),
        StoreItem("Scrambler/Descrambler", 500, "Gear", "رمزگذار/رمزگشای ارتباطات؛ ارتباط امن تیم و شنود/شکستن مکالمات رمز‌شده.", subtitle = "Encrypt comms • counters eavesdropping"),
        StoreItem("Disposable Cell Phone", 50, "Gear", "گوشی یک‌بارمصرف؛ پس از استفاده دور انداخته می‌شود و ردگیری‌اش دشوار است.", subtitle = "Untraceable calls • single use"),
        StoreItem("Flashlight", 20, "Gear", "چراغ‌قوه‌ی قوی؛ روشنایی در فضای تاریک، زیرزمین و بررسی محیط.", subtitle = "Lights dark areas • removes darkness penalty"),
        StoreItem("Grapple Gun", 100, "Gear", "تفنگ قلاب؛ برای بالا رفتن از ساختمان، فرود از ارتفاع و نفوذ عمودی.", subtitle = "Climb 30m • 1 Action"),
        StoreItem("Rope (60m/yds)", 20, "Gear", "طناب مستحکم صعود/فرود؛ همراه ضروری کار با قلاب و عبور از موانع.", subtitle = "Climbing & restraint • Athletics"),
        StoreItem("Duct Tape x5", 20, "Gear", "نوارچسب؛ وصله‌ی فوری هرچیزی، بستن اسیر و راه‌حل اضطراری کلاسیک.", subtitle = "Field repair • restrain a target"),
        StoreItem("Road Flare", 10, "Gear", "منور جاده‌ای؛ علامت‌گذاری، هشدار و نشانه‌گذاری مسیر/نقطه.", subtitle = "Bright light 30m • signalling"),
        StoreItem("Handcuffs", 50, "Gear", "دستبند مهار اسیر؛ برای دستگیری و نگهداری هدف زنده.", subtitle = "Restrain • DV17 Contortionist to escape"),
        StoreItem("Bug Detector", 500, "Gear", "آشکارساز دستگاه شنود و دوربین مخفی؛ پاکسازی اتاق از شنود.", subtitle = "Finds hidden mics • DV15 Electronics"),
        StoreItem("Audio Recorder", 100, "Gear", "ضبط صدا؛ جمع‌آوری مدرک، مکالمه و شواهد.", subtitle = "Records sound • evidence for Media"),
        StoreItem("Video Camera", 100, "Gear", "دوربین فیلم‌برداری حرفه‌ای؛ مدیاها برای مستند و افشاگری استفاده می‌کنند.", subtitle = "Records video • evidence for Media"),
        StoreItem("Binoculars", 50, "Gear", "دوربین دیده‌بانی؛ مشاهده‌ی اهداف و مناطق از فاصله دور.", subtitle = "See distant targets • Perception aid"),
        StoreItem("Smart Glasses", 500, "Gear", "عینک هوشمند با دو Option Slot برای Cybereye Options. وقتی پوشیده شود مزایای optionهای نصب‌شده را در اختیار کاربر می‌گذارد؛ optionهای جفتی در عینک به‌صورت paired در نظر گرفته می‌شوند و فقط یک جفت Smart Glasses را می‌توان هم‌زمان پوشید.", subtitle = "2 Cybereye Option Slots"),
        StoreItem("Glow Paint x5", 20, "Gear", "رنگ شب‌نما؛ علامت‌گذاری مسیر و نقطه در تاریکی برای تیم.", subtitle = "Marks surfaces • visible in the dark"),
        StoreItem("Pocket Amplifier", 50, "Gear", "تقویت‌کننده‌ی صدای جیبی؛ برای اجرای خیابانی راکربوی و بلندگوی همراه.", subtitle = "Amplifies voice • Rockerboy performance"),
        StoreItem("Electric Guitar", 500, "Gear", "ساز راکربوی؛ قلب اجرای زنده و تأثیرگذاری روی مخاطب.", subtitle = "Rockerboy performance instrument"),
        StoreItem("Anti-Smog Breathing Mask", 20, "Gear", "ماسک تصفیه هوا؛ تنفس در محیط آلوده، گاز و مه؛ ارزان و حیاتی.", subtitle = "Filters air • resists gas & smog"),

        // ================= برنامه‌های نت (Programs) =================
        StoreItem("Armor", 50, "Programs",
            "Defender؛ تا وقتی Rezzed است Brain Damage دریافتی را 4 کم می‌کند. فقط یک نسخه هم‌زمان و هر نسخه یک‌بار در هر Netrun.",
            subtitle = "Defender", programClass = "Defender", atk = 0, def = 0, rez = 7),
        StoreItem("Asp", 100, "Programs",
            "Anti-Personnel Black ICE؛ یک Program نصب‌شده روی Cyberdeck دشمن را به‌صورت تصادفی Destroy می‌کند.",
            subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE",
            atk = 2, def = 2, rez = 15, per = 4, spd = 6),
        StoreItem("Banhammer", 50, "Programs",
            "Anti-Program Attacker با ATK 1؛ به Non-Black ICE Program مقدار 3d6 REZ و به Black ICE مقدار 2d6 REZ آسیب می‌زند.",
            subtitle = "Anti-Program Attacker", programClass = "Anti-Program Attacker",
            atk = 1, def = 0, rez = 0),
        StoreItem("DeckKRASH", 100, "Programs",
            "Anti-Personnel Attacker؛ Netrunner دشمن را فوراً Unsafe Jack Out می‌کند و هنگام خروج اثر تمام Black ICEهای Rezzed ملاقات‌شده را می‌گیرد.",
            subtitle = "Anti-Personnel Attacker", programClass = "Anti-Personnel Attacker",
            atk = 0, def = 0, rez = 0),
        StoreItem("Eraser", 20, "Programs",
            "Booster؛ تا وقتی Rezzed است Cloak Checkها +2. این Program چیزی را خودکار پاک نمی‌کند؛ Bonus آن روی Cloak اعمال می‌شود.",
            subtitle = "Booster", programClass = "Booster", atk = 0, def = 0, rez = 7),
        StoreItem("See Ya", 20, "Programs", "Booster؛ تا وقتی Rezzed است Pathfinder Checkها +2.", subtitle = "Booster", programClass = "Booster", atk = 0, def = 0, rez = 7),
        StoreItem("Speedy Gonzalvez", 100, "Programs", "Booster؛ تا وقتی Rezzed است Speed +2.", subtitle = "Booster", programClass = "Booster", atk = 0, def = 0, rez = 7),
        StoreItem("Worm", 50, "Programs", "Booster؛ تا وقتی Rezzed است Backdoor Checkها +2.", subtitle = "Booster", programClass = "Booster", atk = 0, def = 0, rez = 7),
        StoreItem("Flak", 50, "Programs", "Defender؛ ATK تمام Non-Black ICE Attacker Programها علیه تو را تا وقتی Rezzed است 0 می‌کند؛ هر نسخه یک‌بار در Netrun.", subtitle = "Defender", programClass = "Defender", atk = 0, def = 0, rez = 7),
        StoreItem("Shield", 20, "Programs", "Defender؛ اولین Non-Black ICE Program Effect موفق که Brain Damage می‌دهد را متوقف می‌کند و سپس Derezz می‌شود.", subtitle = "Defender", programClass = "Defender", atk = 0, def = 0, rez = 7),
        StoreItem("Sword", 50, "Programs", "Anti-Program Attacker با ATK1؛ 3d6 REZ به Black ICE یا 2d6 به Non-Black ICE.", subtitle = "Anti-Program Attacker", programClass = "Anti-Program Attacker", atk = 1, def = 0, rez = 0),
        StoreItem("Hellbolt", 100, "Programs", "Anti-Personnel Attacker با ATK2؛ 2d6 Brain Damage و در صورت نبود insulation آتش‌گرفتن Cyberdeck/لباس.", subtitle = "Anti-Personnel Attacker", programClass = "Anti-Personnel Attacker", atk = 2, def = 0, rez = 0),
        StoreItem("Nervescrub", 100, "Programs", "Anti-Personnel Attacker؛ INT/REF/DEX دشمن هرکدام 1d6 برای یک ساعت کم می‌شوند (حداقل 1).", subtitle = "Anti-Personnel Attacker", programClass = "Anti-Personnel Attacker", atk = 0, def = 0, rez = 0),
        StoreItem("Poison Flatline", 100, "Programs", "Anti-Personnel Attacker؛ یک Non-Black ICE Program نصب‌شده روی Cyberdeck هدف را تصادفی Destroy می‌کند.", subtitle = "Anti-Personnel Attacker", programClass = "Anti-Personnel Attacker", atk = 0, def = 0, rez = 0),
        StoreItem("Superglue", 100, "Programs", "Anti-Personnel Attacker با ATK2؛ هدف 1d6 Round نمی‌تواند عمیق‌تر برود یا Safe Jack Out کند.", subtitle = "Anti-Personnel Attacker", programClass = "Anti-Personnel Attacker", atk = 2, def = 0, rez = 0),
        StoreItem("Vrizzbolt", 50, "Programs", "Anti-Personnel Attacker با ATK1؛ 1d6 Brain Damage و NET Actionهای Turn بعد را 1 کم می‌کند (حداقل 2).", subtitle = "Anti-Personnel Attacker", programClass = "Anti-Personnel Attacker", atk = 1, def = 0, rez = 0),

        StoreItem("Giant", 1000, "Programs", "Anti-Personnel Black ICE؛ 3d6 Brain Damage و Unsafe Jack Out، سپس اثر Black ICEهای ملاقات‌شده.", subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE", per = 2, spd = 2, atk = 8, def = 4, rez = 25),
        StoreItem("Hellhound", 500, "Programs", "Anti-Personnel Black ICE؛ 2d6 Brain Damage و در صورت نبود insulation آتش‌گرفتن Cyberdeck/لباس.", subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE", per = 6, spd = 6, atk = 6, def = 2, rez = 20),
        StoreItem("Kraken", 1000, "Programs", "Anti-Personnel Black ICE؛ 3d6 Brain Damage و تا پایان Turn بعد عدم پیشروی/Safe Jack Out.", subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE", per = 6, spd = 2, atk = 8, def = 4, rez = 30),
        StoreItem("Liche", 500, "Programs", "Anti-Personnel Black ICE؛ INT/REF/DEX هرکدام 1d6 برای یک ساعت کم می‌شوند.", subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE", per = 8, spd = 2, atk = 6, def = 2, rez = 25),
        StoreItem("Raven", 50, "Programs", "Anti-Personnel Black ICE؛ Defender تصادفی Rezzed را Derezz و 1d6 Brain Damage وارد می‌کند.", subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE", per = 6, spd = 4, atk = 4, def = 2, rez = 15),
        StoreItem("Scorpion", 100, "Programs", "Anti-Personnel Black ICE؛ MOVE هدف 1d6 برای یک ساعت کم می‌شود (حداقل 1).", subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE", per = 2, spd = 6, atk = 2, def = 2, rez = 15),
        StoreItem("Skunk", 500, "Programs", "Anti-Personnel Black ICE؛ تا Derezz شدن، Slide Checkهای هدف -2؛ چند Skunk stack می‌شوند.", subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE", per = 2, spd = 4, atk = 4, def = 2, rez = 10),
        StoreItem("Wisp", 50, "Programs", "Anti-Personnel Black ICE؛ 1d6 Brain Damage و NET Actionهای Turn بعد -1 (حداقل 2).", subtitle = "Anti-Personnel Black ICE", programClass = "Anti-Personnel Black ICE", per = 4, spd = 4, atk = 4, def = 2, rez = 15),
        StoreItem("Dragon", 1000, "Programs",
            "مرگبارترین Black ICE ضد‌برنامه؛ با 6d6 به برنامه‌های دشمن آسیب می‌زند و اگر برنامه‌ای را Derezz کند آن برنامه برای همیشه Destroyed می‌شود.",
            subtitle = "Anti-Program Black ICE", programClass = "Anti-Program Black ICE",
            atk = 6, def = 6, rez = 30, per = 6, spd = 4),
        StoreItem("Killer", 500, "Programs", "Anti-Program Black ICE؛ 4d6 به Program و اگر برای Derezz کافی باشد آن را Destroy می‌کند.", subtitle = "Anti-Program Black ICE", programClass = "Anti-Program Black ICE", per = 4, spd = 8, atk = 6, def = 2, rez = 20),
        StoreItem("Sabertooth", 1000, "Programs", "Anti-Program Black ICE؛ 6d6 به Program و اگر برای Derezz کافی باشد آن را Destroy می‌کند.", subtitle = "Anti-Program Black ICE", programClass = "Anti-Program Black ICE", per = 8, spd = 6, atk = 6, def = 2, rez = 25),

        // ================= وسایل نقلیه Core =================
        *VehicleCatalog.storeItems.toTypedArray(),

        // ================= دارو و مواد =================

        // ================= Core Weapon Attachments =================
        StoreItem("Bayonet", 100, "Weapon Mods", "سرنیزه برای سلاح‌های غیر Exotic مبتنی بر Shoulder Arms. سلاح نصب‌شده در دست می‌تواند به‌عنوان Light Melee Weapon استفاده شود و تا وقتی سرنیزه متصل است زیر لباس قابل مخفی‌کردن نیست.", subtitle="Attachment • Shoulder Arms"),
        StoreItem("Drum Magazine", 500, "Weapon Mods", "خشاب Drum ظرفیت سلاح را به مقدار ستون Drum جدول Clip افزایش می‌دهد. هم‌زمان فقط یک نوع خشاب روی سلاح قرار می‌گیرد و نصب آن قابلیت مخفی‌کردن سلاح زیر لباس را از بین می‌برد.", subtitle="Attachment • Magazine"),
        StoreItem("Extended Magazine", 100, "Weapon Mods", "خشاب Extended ظرفیت مهمات را به مقدار ستون Extended جدول Clip می‌رساند. با Bow/Crossbow سازگار نیست؛ هم‌زمان فقط یک خشاب نصب می‌شود و سلاح دیگر زیر لباس قابل مخفی‌کردن نیست.", subtitle="Attachment • Magazine"),
        StoreItem("Grenade Launcher Underbarrel", 500, "Weapon Mods", "برای سلاح‌های غیر Exotic با Shoulder Arms. هنگام گرفتن سلاح با دو دست یک Grenade Launcher تک‌گلوله‌ای زیر لوله در اختیار می‌گذارد. دو Attachment Slot مصرف می‌کند و مانع Concealment می‌شود.", subtitle="Attachment • 2 slots"),
        StoreItem("Infrared Nightvision Scope", 500, "Weapon Mods", "دوربین فروسرخ/دید در شب برای سلاح غیر Exotic. جریمه‌های تیراندازی ناشی از تاریکی، دود و مه را هنگام نشانه‌روی از دوربین به صفر می‌رساند و گوشت گرم را از فلز سرد تفکیک می‌کند، اما از میان Cover دید ایجاد نمی‌کند.", subtitle="Attachment • Optic"),
        StoreItem("Shotgun Underbarrel", 500, "Weapon Mods", "برای سلاح‌های غیر Exotic با Shoulder Arms؛ با گرفتن دو دستی، یک Shotgun زیرلوله‌ای با ظرفیت دو شلیک فراهم می‌کند. دو Attachment Slot مصرف می‌کند و Concealment را از بین می‌برد.", subtitle="Attachment • 2 slots"),
        StoreItem("Smartgun Link", 500, "Weapon Mods", "سلاح را به Smartgun تبدیل می‌کند. برای استفاده به Interface Plugs یا Subdermal Grip متصل به Neural Link نیاز است و در Ranged Attack با اتصال معتبر +1 به Check می‌دهد. نصب/بازکردن یک ساعت طول می‌کشد و دو Attachment Slot مصرف می‌کند.", subtitle="Attachment • +1 Ranged Attack • 2 slots"),
        StoreItem("Sniping Scope", 100, "Weapon Mods", "جزئیات را تا 800m/yd قابل مشاهده می‌کند. برای Single Shot یا Aimed Shot علیه هدف 51m/yd یا دورتر +1 به Check می‌دهد و با TeleOptics جمع نمی‌شود.", subtitle="Attachment • Long range"),

        // ================= Core Exotic Weapons =================
        StoreItem("Air Pistol",100,"Weapons","Exotic Medium Pistol برای شلیک paintball. مهمات عادی آن آسیب نمی‌زند؛ Acid Paintball نیز HP Damage نمی‌دهد اما با هر Hit یک SP از زره محل اصابت کم می‌کند. این آسیب Critical Injury ایجاد نمی‌کند.", subtitle="Exotic Medium Pistol", damage="0d6", weaponSkill="Handgun", hands="1", rof="2", ammoType="Pistol", magStd=12, rangeSingle=DV_PISTOL),
        StoreItem("Battleglove",1000,"Weapons","دستکش رزمی سنگین با سه Option Slot برای Cyberarm/Cyberlimb options. پوشیدن یا درآوردن آن یک Action است و هنگام پوشیدن، optionهای بازو یا دست زیر آن قابل استفاده نیستند. قابل Conceal نیست.", subtitle="Exotic • 3 cyberlimb slots"),
        StoreItem("Constitution Arms Hurricane",5000,"Weapons","Exotic Shotgun با ROF 2 و drum شانزده‌تایی. Aimed Shot ندارد، Reload آن دو Action می‌خواهد و برای شلیک بدون mount به BODY 11+ نیاز دارد.", subtitle="Exotic Shotgun • ROF 2 • 16 shots", damage="5d6", weaponSkill="Shoulder Arms", hands="2", rof="2", ammoType="Shotgun Shells", magStd=16, modes="Shotgun Shell", rangeSingle=DV_SHOTGUN),
        StoreItem("Dartgun",100,"Weapons","Exotic Very Heavy Pistol با خشاب 8 که فقط Non-Basic Arrow Ammunition می‌پذیرد. با وجود ظاهر پیستول، برای حمل انواع تیر ویژه طراحی شده است.", subtitle="Exotic VH Pistol • special arrows", weaponSkill="Handgun", hands="1", rof="1", ammoType="Arrow", magStd=8, rangeSingle=DV_PISTOL),
        StoreItem("Kendachi Mono-Three",5000,"Weapons","Excellent Quality Exotic Very Heavy Melee Weapon دو دستی. با biometric key درست، در برابر زره کمتر از SP11 آن زره را نادیده می‌گیرد؛ SP11 و بالاتر عادی Resolve می‌شوند. بدون کلید همچنان یک Excellent Quality VH Melee Weapon است.", subtitle="Excellent Exotic VH Melee • 4d6", damage="4d6", weaponSkill="Melee Weapon", hands="2", rof="1"),
        StoreItem("Malorian Arms 3516",10000,"Weapons","Excellent Quality Exotic Very Heavy Pistol بسیار کمیاب؛ Single Shot آن 5d6 است و Smartgun Link دائمی دارد. برای کارکرد باید Smartgun Link آن از طریق Interface Plugs یا Subdermal Grip متصل باشد.", subtitle="Excellent Exotic VH Pistol • 5d6", damage="5d6", weaponSkill="Handgun", hands="1", rof="1", ammoType="Pistol", magStd=8, rangeSingle=DV_PISTOL),
        StoreItem("Microwaver",500,"Weapons","Exotic Very Heavy Pistol بدون Damage معمول. در Hit هدف DV15 Cybertech می‌دهد؛ شکست باعث می‌شود GM دو Cyberware یا وسیله الکترونیکی حمل‌شده را برای یک دقیقه از کار بیندازد. باتری 8 شلیک دارد و شارژ کامل یک ساعت طول می‌کشد.", subtitle="Exotic • EMP effect", weaponSkill="Handgun", hands="1", rof="1", ammoType="Battery", magStd=8, rangeSingle=DV_PISTOL),
        StoreItem("Militech Cowboy U-56",5000,"Weapons","Exotic Grenade Launcher با ROF 2 و خشاب چهار نارنجک که همه انواع Grenade Ammunition را می‌پذیرد. Reload دو Action و شلیک بدون mount نیازمند BODY 11+ است.", subtitle="Exotic Grenade Launcher • ROF 2 • 4", damage="6d6", weaponSkill="Heavy Weapons", rof="2", magStd=4, rangeSingle=DV_GRENADE_LAUNCHER),
        StoreItem("Rhinemetall EMG-86 Railgun",5000,"Weapons","Exotic Assault Rifle با Heavy Weapons، خشاب 4، بدون Autofire و Aimed Shot. Damage آن زره زیر SP11 را نادیده می‌گیرد؛ Reload دو Action است و برای استفاده بدون mount به BODY 11+ نیاز دارد.", subtitle="Exotic Railgun • 5d6", damage="5d6", weaponSkill="Heavy Weapons", rof="1", magStd=4, rangeSingle=DV_RIFLE),
        StoreItem("Shrieker",500,"Weapons","Exotic Very Heavy Pistol صوتی. کاربر بدون محافظ گوش خودش Damaged Ear می‌گیرد؛ هدف Hit شده به‌جای Damage باید DV15 Resist Torture/Drugs بدهد و در شکست Damaged Ear می‌گیرد. باتری 8 شلیک و شارژ یک‌ساعته دارد.", subtitle="Exotic sonic pistol", weaponSkill="Handgun", hands="1", rof="1", ammoType="Battery", magStd=8, rangeSingle=DV_PISTOL),
        StoreItem("Stun Baton",100,"Weapons","Exotic Medium Melee یک‌دستی غیرکشنده. اگر Damage هدف را زیر 1 HP ببرد، به‌جای آن هدف در 1 HP Unconscious می‌شود؛ Critical Injury ایجاد نمی‌کند و Armor را ablate نمی‌کند.", subtitle="Exotic Medium Melee • nonlethal", damage="2d6", weaponSkill="Melee Weapon", rof="2"),
        StoreItem("Stun Gun",100,"Weapons","Exotic Heavy Pistol غیرکشنده. اگر Damage هدف را زیر 1 HP ببرد او را در 1 HP Unconscious می‌کند؛ Critical Injury و Armor Ablation ندارد. باتری 8 شلیک و شارژ یک‌ساعته دارد.", subtitle="Exotic Heavy Pistol • nonlethal", damage="3d6", weaponSkill="Handgun", hands="1", rof="2", ammoType="Battery", magStd=8, rangeSingle=DV_PISTOL),
        StoreItem("Tsunami Arms Helix",5000,"Weapons","Exotic Assault Rifle تمام-Autofire. Single Shot و Aimed Shot ندارد، هر Attack بیست گلوله از خشاب 40تایی مصرف می‌کند و روی Hit، 2d6 در مقدار Beat کردن DV تا سقف ×5 ضرب می‌شود. Reload دو Action و BODY 11+ بدون mount لازم است.", subtitle="Exotic Autofire • 40 rounds", weaponSkill="Autofire", magStd=40, autofire="5", rangeAuto=DV_AR_AUTO),

        // ================= Complete Core Ammunition =================
        StoreItem("Biotoxin Ammunition",500,"Ammo","برای Arrow و Grenade. Damage عادی نمی‌دهد؛ هدف گوشتی DV15 Resist Torture/Drugs می‌دهد و در شکست 3d6 مستقیم به HP می‌گیرد، بدون تعامل یا ablation زره.", subtitle="Arrow • Grenade — DV15, 3d6 direct HP"),
        StoreItem("EMP Ammunition",500,"Ammo","فقط Grenade. Damage عادی ندارد؛ هدف DV15 Cybertech می‌دهد و در شکست GM دو Cyberware یا وسیله الکترونیکی حمل‌شده را برای یک دقیقه غیرفعال می‌کند.", subtitle="Grenade only — DV15 Cybertech"),
        StoreItem("Expansive Ammunition",100,"Ammo","برای Arrow، Bullet و Slug. اگر Foreign Object Critical Injury ایجاد شود، قربانی دوباره روی جدول Critical Injury می‌ریزد تا نتیجه‌ای غیر از Foreign Object بگیرد؛ Injury دوم Bonus Damage ندارد.", subtitle="Pistol • SMG • Rifle • Slug • Arrow — Critical effect"),
        StoreItem("Flashbang Ammunition",100,"Ammo","فقط Grenade؛ Damage عادی ندارد. اهداف Hit شده DV15 Resist Torture/Drugs می‌دهند؛ شکست Damaged Eye و Damaged Ear را برای یک دقیقه اعمال می‌کند و Bonus Damage این Injuryها اعمال نمی‌شود.", subtitle="Grenade only — DV15"),
        StoreItem("Poison Ammunition",100,"Ammo","برای Arrow و Grenade. به‌جای Damage عادی، هدف گوشتی DV13 Resist Torture/Drugs می‌دهد؛ شکست 2d6 Damage مستقیم HP می‌زند و Armor ablate نمی‌شود.", subtitle="Arrow • Grenade — DV13, 2d6 direct HP"),
        StoreItem("Rubber Ammunition",10,"Ammo","برای Arrow، Bullet و Slug. Damage را به شکل عادی وارد می‌کند، اما Critical Injury ایجاد نمی‌کند و Armor را ablate نمی‌کند.", subtitle="Pistol • SMG • Rifle • Slug • Arrow — non-lethal"),
        StoreItem("Sleep Ammunition",500,"Ammo","برای Arrow و Grenade. Damage عادی ندارد؛ هدف گوشتی DV13 Resist Torture/Drugs می‌دهد و در شکست Prone و برای یک دقیقه Unconscious می‌شود، مگر با Damage یا Action فرد دیگر بیدار شود.", subtitle="Arrow • Grenade — DV13, sleep"),
        StoreItem("Smart Ammunition",500,"Ammo","برای Arrow، Bullet و Rocket و نیازمند Targeting Scope Cyberware. اگر Single Shot حداکثر 4 کمتر از DV Miss شود، یک شانس دوم با 1d10+10 در برابر همان DV می‌دهد؛ LUCK می‌تواند اضافه شود و هدف واجد شرایط همچنان می‌تواند Dodge کند.", subtitle="Pistol • SMG • Rifle • Arrow • Rocket — needs Targeting Scope"),
        StoreItem("Smoke Ammunition",50,"Ammo","فقط Grenade. در برخورد یک ناحیه 10×10m/yd را برای یک دقیقه با دود می‌پوشاند؛ جریمه معمول انجام کار در دید مسدودشده توسط دود -4 است.", subtitle="Grenade only — smoke 10×10m"),
        StoreItem("Teargas Ammunition",50,"Ammo","فقط Grenade. Damage عادی ندارد؛ افراد با چشم طبیعی DV13 Resist Torture/Drugs می‌دهند و در شکست برای یک دقیقه Damaged Eye می‌گیرند، بدون Bonus Damage.", subtitle="Grenade only — DV13 Damaged Eye"),

        // ================= Missing Core General Gear =================
        StoreItem("Auto Level Dampening Ear Protectors",1000,"Gear","محافظ گوش فشرده با تنظیم خودکار سطح صدا. هنگام پوشیدن، کاربر در برابر ناشنوایی و افکت‌های ناشی از صدای خطرناک مثل flashbang مصون است.", subtitle="Hearing protection"),
        StoreItem("Braindance Viewer",1000,"Gear","دستگاه مشاهده Braindance؛ تجربه ضبط‌شده را از دید بازیگر همراه با حواس و احساسات ثبت‌شده بازپخش می‌کند.", subtitle="Braindance playback"),
        StoreItem("Carryall",20,"Gear","کیف مقاوم ripstop در اندازه‌های مختلف از messenger bag تا duffel بسیار بزرگ؛ برای حمل تجهیزات و غنیمت.", subtitle="Utility bag"),
        StoreItem("Chemical Analyzer",1000,"Gear","با یک Action ترکیب دقیق شیمیایی ماده را آزمایش و با پایگاه داده گسترده تطبیق می‌دهد و اغلب مواد را بلافاصله شناسایی می‌کند.", subtitle="Chemical analysis"),
        StoreItem("Computer",50,"Gear","رایانه laptop/desktop برای کار راحت‌تر با متن، فایل‌ها و Data Pool. جایگزین Agent نیست اما برای کارهای رومیزی و دسترسی پایدار مناسب است.", subtitle="Computer • Data Pool"),
        StoreItem("Cryopump",5000,"Gear","ابزار Medtech در اندازه briefcase با body bag و پمپ سرمایشی. قرار دادن هدف willing/unconscious یک Action و یک charge مصرف می‌کند؛ فرد در stasis تا یک هفته Death Save نمی‌دهد. کیسه 15 HP Cover دارد و امکان Surgery/Stabilization در stasis را فراهم می‌کند؛ هر charge با 50eb پر می‌شود و فقط Medtech می‌تواند آن را راه‌اندازی کند.", subtitle="Medtech • cryostasis • 15 HP cover"),
        StoreItem("Cryotank",5000,"Gear","محفظه تمام‌قد پزشکی. با موفقیت DV13 Medical Tech یک نفر را نامحدود در stasis نگه می‌دارد؛ فرد unconscious است و تا وقتی داخل تانک سالم بماند با دو برابر نرخ معمول heal می‌شود. تانک 30 HP Cover دارد و فقط Medtech می‌تواند آن را راه‌اندازی کند.", subtitle="Medtech • DV13 • 30 HP cover"),
        StoreItem("Cyberdeck (Poor Quality)",100,"Gear","Cyberdeck ارزان با 5 Slot برای Programs/Hardware. برای Netrunning به Neural Link و Interface Plugs نیاز دارد.", subtitle="Cyberdeck • 5 slots"),
        StoreItem("Cyberdeck (Standard Quality)",500,"Gear","Cyberdeck استاندارد با 7 Slot برای Programs و Hardware؛ برای کارکرد Netrunner به Neural Link و Interface Plugs نیاز دارد.", subtitle="Cyberdeck • 7 slots"),
        StoreItem("Cyberdeck (Excellent Quality)",1000,"Gear","Cyberdeck رده‌بالا با 9 Slot برای Programs و Hardware؛ برای کارکرد به Neural Link و Interface Plugs نیاز دارد.", subtitle="Cyberdeck • 9 slots"),
        StoreItem("Drum Synthesizer",500,"Gear","پدهای تخت الکترونیکی متصل به پردازنده که تقریباً هر نوع drum را شبیه‌سازی می‌کنند؛ برای شنیده‌شدن به amplification نیاز دارد.", subtitle="Instrument"),
        StoreItem("Food Stick",10,"Gear","غذای خشک فشرده در طعم‌های معمولاً نامطبوع؛ یک وعده غذا محسوب می‌شود.", subtitle="One meal"),
        StoreItem("Glow Stick",10,"Gear","لوله نور یک‌بارمصرف که محدوده حدود 4m/yd را تا ده ساعت روشن می‌کند.", subtitle="Light • 4m/yd • 10h"),
        StoreItem("Homing Tracer",500,"Gear","گیرنده‌ای که tracer لینک‌شده را تا حدود یک مایل دنبال می‌کند؛ یک tracer دکمه‌ای همراه دارد و replacement tracerها 50eb هستند.", subtitle="Tracker • 1 mile"),
        StoreItem("Inflatable Bed & Sleep-bag",20,"Gear","تشک خودبادشونده همراه sleeping bag نازک که برای حمل در بسته‌ای بسیار کوچک جمع می‌شود.", subtitle="Camping"),
        StoreItem("Kibble Pack",10,"Gear","یک بسته غذای خشک شبیه خوراک حیوانات، کافی برای یک وعده؛ معمولاً با شماره محصول شناخته می‌شود نه نام جذاب.", subtitle="One meal"),
        StoreItem("Linear Frame Beta",5000,"Gear","Powered exoskeleton؛ هنگام اتصال BODY را تا 14 بالا می‌برد اما HP و Death Save را تغییر نمی‌دهد و نمی‌تواند BODY را بالاتر از 14 ببرد. برای کارکرد دو Interface Plugs لازم است.", subtitle="External frame • BODY 14"),
        StoreItem("Linear Frame Sigma",1000,"Gear","Powered exoskeleton؛ هنگام اتصال BODY را تا 12 بالا می‌برد اما HP و Death Save را تغییر نمی‌دهد و نمی‌تواند BODY را بالاتر از 12 ببرد. یک Interface Plugs لازم دارد.", subtitle="External frame • BODY 12"),
        StoreItem("Lock Picking Set",20,"Gear","کیف کوچک ابزارهای لازم برای بازکردن قفل‌های مکانیکی.", subtitle="Lock tools"),
        StoreItem("Medscanner",1000,"Gear","اسکنر پزشکی با probe و contact برای تشخیص آسیب و بیماری در وضعیت‌هایی که Surgery لازم نیست. +2 به First Aid و Paramedic می‌دهد و با خودش stack نمی‌شود.", subtitle="+2 First Aid/Paramedic"),
        StoreItem("Memory Chips",10,"Gear","وافرهای باریک ذخیره‌سازی داده برای متن، صوت، تصویر و داده‌های Cyberware/Agent؛ ظرفیت و اندازه مدل‌ها متفاوت است.", subtitle="Data storage"),
        StoreItem("MRE",10,"Gear","بسته غذای self-heating؛ با آب و فعال‌کردن tab در حدود دو دقیقه یک وعده گرم و مغذی آماده می‌کند.", subtitle="One hot meal"),
        StoreItem("Personal CarePak",20,"Gear","بسته بهداشت شخصی شامل مسواک خمیردندان‌دار، دستمال‌های بدن، depilatory paste، شانه و اقلام مشابه.", subtitle="Personal hygiene"),
        StoreItem("Radar Detector",500,"Gear","در صورت وجود active radar beam در شعاع 100m/yd هشدار می‌دهد.", subtitle="Detect radar • 100m/yd"),
        StoreItem("Techscanner",1000,"Gear","اسکنر تعمیر و عیب‌یابی ماشین‌آلات و الکترونیک. +2 به Basic Tech، Cybertech، Land/Sea/Air Vehicle Tech، Electronics/Security Tech و Weaponstech می‌دهد و با خودش stack نمی‌شود.", subtitle="Technical scanner • +2 Tech skills"),
        StoreItem("Techtool",100,"Gear","ابزار همه‌کاره جمع‌وجور با تیغه کوچک، انبردست، پیچ‌گوشتی‌ها، فایل و cutter؛ مجموعه پایه برای تعمیرات میدانی.", subtitle="All-in-one tool"),
        StoreItem("Tent & Camping Equipment",50,"Gear","چادر لوله‌ای یک‌نفره، میخ پلاستیکی، ظرف self-heating قابل شارژ برای جوشاندن آب و یک spork فلزی ارزان؛ بسته پایه کمپینگ.", subtitle="Camping kit"),
        StoreItem("Vial of Biotoxin",500,"Gear","یک ویال کامل با یک Action روی Light Melee Weapon مالیده می‌شود و 30 دقیقه دوام دارد. Hit گوشتی به‌جای Damage معمول DV15 Resist Torture/Drugs می‌دهد؛ شکست 3d6 مستقیم HP وارد می‌کند و Armor درگیر نمی‌شود.", subtitle="DV15 • 3d6 direct HP"),
        StoreItem("Vial of Poison",100,"Gear","یک ویال کامل با یک Action روی Light Melee Weapon اعمال می‌شود و 30 دقیقه دوام دارد. Hit گوشتی DV13 Resist Torture/Drugs می‌دهد؛ شکست 2d6 مستقیم HP می‌زند و Armor ablate نمی‌شود.", subtitle="DV13 • 2d6 direct HP"),

        // ================= Core Street Drugs =================
        StoreItem("Black Lace",50,"Drugs","Primary Effect بیست‌وچهار ساعت دوام دارد؛ هنگام مصرف 2d6 Humanity Loss موقت ایجاد می‌کند و اثر Seriously Wounded را نادیده می‌گیرد. Secondary Effect با DV17 می‌تواند Humanity از دست‌رفته را دائمی و کاربر را addicted کند؛ در اعتیاد و خارج از Primary Effect، REF دو واحد کم می‌شود.", subtitle="24h • Secondary DV17"),
        StoreItem("Blue Glass",20,"Drugs","Primary Effect چهار ساعت است و GM گاهی flash-outهای توهمی اعلام می‌کند که Action آن Turn را از کاربر می‌گیرد. Secondary DV15 اعتیاد ایجاد می‌کند؛ معتاد معمولاً دوره‌های flash-out دارد، در حالی که مصرف دوباره موقتاً آن‌ها را مهار می‌کند.", subtitle="4h • Secondary DV15"),
        StoreItem("Boost",50,"Drugs","Primary Effect بیست‌وچهار ساعت INT را +2 می‌کند و می‌تواند آن را بالاتر از 8 ببرد. Secondary DV17 اعتیاد ایجاد می‌کند و در حالت اعتیاد INT دو واحد کاهش می‌یابد.", subtitle="24h • +2 INT • Secondary DV17"),
        StoreItem("Smash",10,"Drugs","Primary Effect چهار ساعت حالت سرخوشی ایجاد کرده و +2 به Dance، Contortionist، Conversation، Human Perception، Persuasion و Acting می‌دهد. Secondary DV15 اعتیاد ایجاد می‌کند و خارج از اثر، همین Skillها -2 می‌گیرند.", subtitle="4h • social/party skill bonuses • DV15"),
        StoreItem("Synthcoke",20,"Drugs","Primary Effect چهار ساعت REF را +1 می‌کند و می‌تواند بالاتر از 8 ببرد، همراه با paranoid ideation. Secondary DV15 اعتیاد ایجاد می‌کند؛ در اعتیاد REF خارج از Primary Effect دو واحد کم می‌شود.", subtitle="4h • +1 REF • Secondary DV15"),

        // ================= Core Cyberdeck Hardware =================
        StoreItem("Backup Drive",100,"Hardware","دو Hardware Slot مصرف می‌کند و Non-Black ICE Programهایی را که در آستانه Destroy شدن هستند ذخیره می‌کند. Netrunner با Meat Action می‌تواند برنامه‌های ذخیره‌شده را در صورت داشتن Slot دوباره نصب کند؛ جداکردن Drive محتویاتش را پاک می‌کند.", subtitle="Cyberdeck Hardware • 2 slots"),
        StoreItem("DNA Lock",100,"Hardware","دو Slot مصرف می‌کند و Cyberdeck را با biometric key مثل اثر انگشت، iris یا خون قفل می‌کند. Deck قفل‌شده بدون کلید فقط با DV17 Electronics/Security Tech قابل دسترسی است.", subtitle="Cyberdeck Hardware • 2 slots • DV17"),
        StoreItem("Hardened Circuitry",100,"Hardware","Cyberdeck را در برابر disable، inoperable یا destruction ناشی از EMP و Non-Black ICE Program Effects محافظت می‌کند.", subtitle="Cyberdeck Hardware • EMP protection"),
        StoreItem("Insulated Wiring",100,"Hardware","مانع آتش‌گرفتن Cyberdeck یا لباس Netrunner بر اثر Program Effect می‌شود.", subtitle="Cyberdeck Hardware • fire protection"),
        StoreItem("KRASH Barrier",100,"Hardware","دو Slot مصرف می‌کند و Cyberdeck را در برابر Program Effectهایی که Netrunner را وادار به Jack Out امن یا ناامن می‌کنند مصون می‌سازد.", subtitle="Cyberdeck Hardware • 2 slots"),
        StoreItem("Range Upgrade",100,"Hardware","برد اتصال Cyberdeck به Access Point را تا 8m افزایش می‌دهد.", subtitle="Cyberdeck Hardware • 8m range"),

        // ================= Missing Core Cyberware =================
        StoreItem("EMP Threading",10,"Cyberware","Fashionware؛ خطوط نقره‌ای مدارمانند زیر/روی پوست که عمدتاً جنبه زیبایی دارند و حفاظت مکانیکی در برابر EMP ایجاد نمی‌کنند.",subtitle="Fashionware",slot="Mall",humanityLoss=0),
        StoreItem("Braindance Recorder",500,"Cyberware","Neuralware option برای ضبط تجربه Braindance از دید کاربر روی Memory Chip یا Agent لینک‌شده. به Neural Link نیاز دارد.",subtitle="Neuralware",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Chemical Analyzer Chipware",500,"Cyberware","Chipware؛ در Chipware Socket با یک Action ترکیب شیمیایی مواد را شناسایی می‌کند. به Chipware Socket نیاز دارد.",subtitle="Chipware",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Memory Chip Chipware",10,"Cyberware","Chipware استاندارد ذخیره داده؛ هنگام نصب در Socket، cyberware می‌تواند داده را روی آن ذخیره یا از آن بخواند.",subtitle="Chipware",humanityLoss=0),
        StoreItem("Olfactory Boost",100,"Cyberware","Chipware که حس بویایی را تقویت می‌کند و اجازه می‌دهد Tracking برای دنبال‌کردن بو نیز استفاده شود. نیازمند Chipware Socket.",subtitle="Chipware",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Pain Editor",1000,"Cyberware","Chipware که گیرنده‌های درد را پویا خاموش می‌کند و هنگام نصب اجازه می‌دهد اثر Seriously Wounded Wound State نادیده گرفته شود. نیازمند Chipware Socket.",subtitle="Chipware",humanityLoss=14,humanityDice="4d6"),
        StoreItem("Skill Chip",500,"Cyberware","Chipware برای یک Skill مشخص؛ آن Skill را در +3 در اختیار کاربر می‌گذارد مگر اینکه Skill واقعی او بالاتر باشد. Skillهای x2 نسخه 1000eb دارند. نیازمند Chipware Socket.",subtitle="Chipware",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Tactile Boost",100,"Cyberware","Chipware تقویت حس لمس؛ با قرارداشتن دست روی سطح، حرکت را تا 20m/yd حس می‌کند. در حالت detector آن دست برای کار دیگری قابل استفاده نیست.",subtitle="Chipware",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Anti-Dazzle",100,"Cyberware","Cybereye option جفتی که کاربر را در برابر blindness و افکت‌های ناشی از flash خطرناک مثل flashbang مصون می‌کند.",subtitle="Cyberoptics • paired",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Chyron",100,"Cyberware","Cybereye option که یک subscreen کوچک در میدان دید برای پیام، ویدیو و خروجی cyberware/electronics نمایش می‌دهد.",subtitle="Cyberoptics",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Color Shift",100,"Cyberware","Cybereye cosmetic option برای تغییر نامحدود رنگ و pattern با یک Action؛ می‌تواند به دما یا تغییرات هورمونی واکنش نشان دهد.",subtitle="Cyberoptics",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Image Enhance",500,"Cyberware","Cybereye option جفتی؛ +2 به Perception، Lip Reading و Conceal/Reveal Object در Checkهای وابسته به بینایی می‌دهد و stack نمی‌شود.",subtitle="Cyberoptics • paired",slot="Mall",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Low Light/Infrared/UV",500,"Cyberware","Cybereye option جفتی و دو-slot-per-eye؛ جریمه تاریکی، دود، مه و obscurement غیرجامد را به صفر می‌رساند و تفاوت گوشت گرم و فلز سرد را نشان می‌دهد، اما از Cover عبور نمی‌کند.",subtitle="Cyberoptics • paired • 2 slots/eye",slot="Mall",humanityLoss=3,humanityDice="1d6"),
        StoreItem("MicroVideo",500,"Cyberware","Cybereye camera برای ضبط صوت و ویدیو روی Memory Chip یا Agent؛ دو Option Slot مصرف می‌کند.",subtitle="Cyberoptics • 2 slots",slot="Clinic",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Radiation Detector",1000,"Cyberware","Cybereye option که منابع radiation تا 100m/yd را به شکل readout/درخشش روی دید کاربر نشان می‌دهد.",subtitle="Cyberoptics",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Virtuality Cybereye",100,"Cyberware","Cybereye option جفتی برای نمایش cyberspace imagery روی دید واقعی؛ جایگزین دائمی Virtuality Goggles.",subtitle="Cyberoptics • paired",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Bug Detector Cyberaudio",100,"Cyberware","Cyberaudio option که در فاصله 2m/yd از tap، bug یا listening device هشدار می‌دهد.",subtitle="Cyberaudio",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Homing Tracer Cyberaudio",100,"Cyberware","Cyberaudio option برای دنبال‌کردن tracer لینک‌شده تا یک مایل؛ یک tracer کوچک همراه دارد و replacementها 50eb هستند.",subtitle="Cyberaudio",slot="Clinic",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Level Damper",100,"Cyberware","Cyberaudio option با noise compensation خودکار؛ مصونیت در برابر deafness و افکت صدای بسیار بلند مانند flashbang.",subtitle="Cyberaudio",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Radio Communicator Cyberaudio",100,"Cyberware","Cyberaudio option برای ارتباط رادیویی با برد یک مایل.",subtitle="Cyberaudio",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Radio Scanner/Music Player Cyberaudio",50,"Cyberware","Cyberaudio option؛ با Action باندهای فعال رادیویی در یک مایل را scan/tune می‌کند و music را از Data Pool یا Memory Chip پخش می‌کند. کانال scrambled به descrambler نیاز دارد.",subtitle="Cyberaudio",slot="Clinic",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Radar Detector Cyberaudio",500,"Cyberware","Cyberaudio option که active radar beam تا 100m/yd را تشخیص می‌دهد.",subtitle="Cyberaudio",slot="Clinic",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Scrambler/Descrambler Cyberaudio",100,"Cyberware","Cyberaudio option برای scramble ارتباط خروجی و decode ارتباط سازگار؛ نیازمند Cyberaudio Suite.",subtitle="Cyberaudio",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("AudioVox",500,"Cyberware","Internal Body vocal synthesizer؛ +2 Acting و هنگام آواز +2 Play Instrument می‌دهد؛ چند نصب stack نمی‌شود.",subtitle="Internal Body",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Contraceptive Implant",10,"Cyberware","Internal Body implant برای جلوگیری از بارداری ناخواسته.",subtitle="Internal Body",slot="Mall",humanityLoss=0),
        StoreItem("Enhanced Antibodies",500,"Cyberware","Internal Body؛ بعد از Stabilization، در هر روز استراحت و فعالیت سبک به‌جای BODY، دو برابر BODY HP heal می‌کند.",subtitle="Internal Body",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Cybersnake",1000,"Cyberware","Internal Body weapon در گلو/مری؛ Very Heavy Melee Weapon با 4d6 و ROF1 که بدون Check قابل Conceal است.",subtitle="Internal Body • 4d6",slot="Hospital",humanityLoss=14,humanityDice="4d6"),
        StoreItem("Gills",1000,"Cyberware","Internal Body cyberware که اجازه تنفس زیر آب را می‌دهد.",subtitle="Internal Body",slot="Hospital",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Independent Air Supply",1000,"Cyberware","Internal Body؛ 30 دقیقه هوا ذخیره دارد. refill از هوای محیط یک ساعت طول می‌کشد یا tank پر 50eb را می‌توان با یک Action جایگزین کرد.",subtitle="Internal Body",slot="Hospital",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Radar/Sonar Implant",1000,"Cyberware","Internal Body scanner که تا 50m/yd محیط را برای moving threat جدید پایش می‌کند، حتی زیر آب؛ پشت Cover را نمی‌بیند و جهت منبع جدید را هشدار می‌دهد.",subtitle="Internal Body",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Vampyres",500,"Cyberware","Internal Body fangs؛ Excellent Quality Light Melee Weapon قابل Conceal. یک Vial Poison/Biotoxin را می‌توان در دهان پنهان و بدون Action روی دندان‌ها اعمال کرد؛ هر application یک ویال و 30 دقیقه دوام دارد.",subtitle="Internal Body • weapon",slot="Clinic",humanityLoss=14,humanityDice="4d6"),
        StoreItem("Skin Weave",500,"Cyberware","External Body؛ Head و Body را SP7 می‌کند. بالاترین SP هر location اعمال می‌شود و هنگام ablation همه منابع SP همان location هم‌زمان ablate می‌شوند؛ با هر روز healing موفق 1 SP در هر دو location ترمیم می‌کند.",subtitle="External Body • SP7",slot="Hospital",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Standard Hand",100,"Cyberware","Cyberlimb option شبیه دست طبیعی؛ روی meat arm در شمار محدودیت cyberware آن دست حساب نمی‌شود و Cyberarm Option Slot مصرف نمی‌کند.",subtitle="Cyberlimb",slot="Clinic",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Big Knucks",100,"Cyberware","Cyberarm option؛ Medium Melee Weapon مخفی 2d6/ROF2. هنگام استفاده دست نمی‌تواند وسیله دیگری بگیرد و می‌تواند تنها cyberware یک meat arm باشد.",subtitle="Cyberarm • 2d6",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Grapple Hand",100,"Cyberware","Cyberarm option؛ با Action grapple تا 30m/yd شلیک می‌کند. خط 10 HP و ظرفیت دو برابر وزن کاربر دارد، penalty climbing را حذف می‌کند و retract بدون Action است.",subtitle="Cyberarm",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Popup Grenade Launcher",500,"Cyberware","Cyberarm option دو-slot؛ Grenade Launcher یک‌دستی تک‌نارنجک، قابل Conceal و draw/stow بدون Action. هنگام بازبودن دست همان بازو قابل استفاده نیست.",subtitle="Cyberarm • 2 slots",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Popup Melee Weapon",500,"Cyberware","Cyberarm option دو-slot برای Light/Medium/Heavy Melee Weapon یک‌دستی؛ سلاح بدون Check مخفی و بدون Action draw/stow می‌شود.",subtitle="Cyberarm • 2 slots",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Popup Shield",500,"Cyberware","Cyberarm option سه-slot؛ Bulletproof Shield تاشو داخل بازو. بدون Action باز/بسته می‌شود اگر بیش از 0 HP داشته باشد؛ هنگام بازبودن بازو فقط نقش shield دارد.",subtitle="Cyberarm • 3 slots",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Popup Ranged Weapon",500,"Cyberware","Cyberarm option دو-slot برای یک Ranged Weapon یک‌دستی ارائه‌شده توسط کاربر؛ دائماً نصب، بدون Check مخفی و بدون Action draw/stow می‌شود.",subtitle="Cyberarm • 2 slots",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Quick Change Mount",100,"Cyberware","Cyberarm option؛ اجازه می‌دهد Cyberarm روی socket باز با یک Action نصب یا جدا شود. Humanity Loss فقط اولین بار استفاده از یک بازوی جدید اعمال می‌شود.",subtitle="Cyberarm",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Rippers",500,"Cyberware","Cyberarm option؛ ناخن‌های carbo-glass بازشونده، Medium Melee Weapon 2d6/ROF2 و مخفی. می‌تواند تنها cyberware یک meat arm باشد.",subtitle="Cyberarm • 2d6",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Scratchers",100,"Cyberware","Cyberarm option؛ ناخن‌های carbo-glass، Light Melee Weapon 1d6/ROF2 و مخفی؛ قابل نصب به‌عنوان تنها cyberware meat arm.",subtitle="Cyberarm • 1d6",slot="Mall",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Shoulder Cam",500,"Cyberware","Cyberarm option دو-slot؛ دوربین popup شانه‌ای مستقل برای ضبط audio/video روی Memory Chip یا Agent، قابل Conceal و draw/stow بدون Action.",subtitle="Cyberarm • 2 slots",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Slice N Dice",500,"Cyberware","Cyberarm option؛ monofilament whip در شست، Medium Melee Weapon 2d6/ROF2 و مخفی؛ می‌تواند تنها cyberware meat arm باشد.",subtitle="Cyberarm • 2d6",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Subdermal Grip",100,"Cyberware","Cyberarm/Neuralware option زیر کف دست برای استفاده از Smartgun؛ جایگزین ارزان Interface Plugs، نیازمند Neural Link و یک Neuralware Slot.",subtitle="Neuralware/Cyberarm",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Techscanner Cyberarm",500,"Cyberware","Cyberarm option دو-slot؛ +2 به Basic Tech، Cybertech، Land/Sea/Air Vehicle Tech، Electronics/Security Tech و Weaponstech و stack نمی‌شود.",subtitle="Cyberarm • 2 slots",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Standard Foot",100,"Cyberware","Cyberleg option شبیه پای طبیعی؛ روی meat leg در شمار محدودیت cyberware حساب نمی‌شود و Cyberleg Option Slot مصرف نمی‌کند.",subtitle="Cyberlimb",slot="Clinic",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Grip Foot",500,"Cyberware","Cyberleg option جفتی با سطح traction پیشرفته؛ penalty معمول climbing را حذف می‌کند.",subtitle="Cyberleg • paired",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Jump Booster",500,"Cyberware","Cyberleg option جفتی؛ hydraulics penalty movement هنگام jumping را حذف می‌کند و در هر Cyberleg دو Slot مصرف می‌کند.",subtitle="Cyberleg • paired • 2 slots",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Skate Foot",500,"Cyberware","Cyberleg option جفتی؛ inline skate مخفی که هنگام Run Action حرکت را 6m/yd افزایش می‌دهد.",subtitle="Cyberleg • paired",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Talon Foot",500,"Cyberware","Cyberleg option؛ تیغه پا به‌عنوان Light Melee Weapon که بدون Check مخفی می‌شود و می‌تواند تنها cyberware meat leg باشد.",subtitle="Cyberleg • weapon",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Web Foot",500,"Cyberware","Cyberleg option جفتی؛ webbing بین انگشتان penalty حرکت هنگام swimming را حذف می‌کند.",subtitle="Cyberleg • paired",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Hardened Shielding",1000,"Cyberware","Cyberlimb option؛ cyberlimb و optionهای داخل آن در برابر EMP و Non-Black ICE effects که آن‌ها را inoperable می‌کنند مصون می‌شوند.",subtitle="Cyberlimb",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Plastic Covering",100,"Cyberware","Cyberlimb cosmetic coating در رنگ و pattern مختلف؛ Option Slot مصرف نمی‌کند.",subtitle="Cyberlimb cosmetic",slot="Mall",humanityLoss=0),
        StoreItem("Realskinn Covering",500,"Cyberware","پوشش پوست مصنوعی برای Cyberarm/Cyberleg؛ Option Slot مصرف نمی‌کند.",subtitle="Cyberlimb cosmetic",slot="Mall",humanityLoss=0),
        StoreItem("Superchrome Covering",1000,"Cyberware","پوشش فلزی براق Cyberlimb؛ +2 Wardrobe & Style می‌دهد، فقط یک بار، و Option Slot مصرف نمی‌کند.",subtitle="Cyberlimb cosmetic • +2 Wardrobe",slot="Mall",humanityLoss=0),
        StoreItem("Artificial Shoulder Mount",1000,"Cyberware","Borgware؛ امکان نصب دو Cyberarm اضافه زیر جفت بازوی اصلی. هر کاربر فقط یک Mount می‌تواند داشته باشد.",subtitle="Borgware",slot="Hospital",humanityLoss=14,humanityDice="4d6"),
        StoreItem("Implanted Linear Frame Sigma",1000,"Cyberware","Borgware؛ BODY را به 12 می‌رساند و HP/Death Save را مطابق BODY جدید تغییر می‌دهد. BODY 6 و Grafted Muscle and Bone Lace پیش‌نیاز است.",subtitle="Borgware • BODY 12",slot="Hospital",humanityLoss=14,humanityDice="4d6"),
        StoreItem("Implanted Linear Frame Beta",5000,"Cyberware","Borgware؛ BODY را به 14 می‌رساند و HP/Death Save را تغییر می‌دهد. BODY 8 و دو Grafted Muscle and Bone Lace پیش‌نیاز است.",subtitle="Borgware • BODY 14",slot="Hospital",humanityLoss=14,humanityDice="4d6"),
        StoreItem("MultiOptic Mount",1000,"Cyberware","Borgware؛ امکان نصب تا پنج Cybereye اضافه را فراهم می‌کند؛ چشم‌ها جدا خرید/نصب می‌شوند و فقط یک Mount مجاز است.",subtitle="Borgware",slot="Hospital",humanityLoss=14,humanityDice="4d6"),
        StoreItem("Sensor Array",1000,"Cyberware","Borgware؛ پنج Cyberaudio Option اضافه برای Cyberaudio Suite فراهم می‌کند؛ فقط یکی قابل نصب است و خود Array Cyberaudio Slot مصرف نمی‌کند.",subtitle="Borgware",slot="Clinic",humanityLoss=14,humanityDice="4d6"),
        // Core cyberware that was missing from the old store list. Names are disambiguated from similarly named Gear/Weapons.
        StoreItem("Dartgun (Cybereye)",500,"Cyberware","Cybereye Option؛ یک Dartgun تک‌تیر پنهان در چشم. به Cybereye نیاز دارد و 3 Option Slot مصرف می‌کند.",subtitle="Cyberoptics • 3 slots",slot="Clinic",humanityLoss=2,humanityDice="1d6/2"),
        StoreItem("Cyberdeck (Cyberarm)",500,"Cyberware","Cyberarm Option؛ یک Cyberdeck داخل Cyberarm نصب می‌شود. به Cyberarm نیاز دارد و 3 Option Slot مصرف می‌کند.",subtitle="Cyberarm • 3 slots",slot="Clinic",humanityLoss=3,humanityDice="1d6"),
        StoreItem("Medscanner (Cyberarm)",500,"Cyberware","Cyberarm Option؛ Medscanner داخلی برای تشخیص آسیب و بیماری؛ +2 به First Aid و Paramedic. به Cyberarm نیاز دارد و 2 Option Slot مصرف می‌کند.",subtitle="Cyberarm • 2 slots • +2 First Aid/Paramedic",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Midnight Lady™ Sexual Implant",100,"Cyberware","Internal Cyberware؛ ایمپلنت جنسی انتخابی. یک Internal Option Slot مصرف می‌کند.",subtitle="Internal",slot="Clinic",humanityLoss=7,humanityDice="2d6"),
        StoreItem("Mr. Studd™ Sexual Implant",100,"Cyberware","Internal Cyberware؛ ایمپلنت جنسی انتخابی. یک Internal Option Slot مصرف می‌کند.",subtitle="Internal",slot="Clinic",humanityLoss=7,humanityDice="2d6"),

    )

    val allItems: List<StoreItem> get() = items + FashionCatalog.storeItems + VehicleCatalog.storeItems

    fun byCategory(cat: String): List<StoreItem> = allItems.filter { it.category == cat }

    private fun detailKey(value: String): String = value.lowercase()
        .replace("®", "").replace("™", "")
        .replace("armor", "")
        .replace("body armor", "body")
        .replace(Regex("\\s+"), " ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    /**
     * Resolve an owned item only inside its own category. This intentionally avoids the old broad
     * substring lookup where e.g. "Light Armorjack ... Armor" could match the NET Program named "Armor".
     */
    fun findForInventory(name: String, category: String): StoreItem? {
        val sameCategory = allItems.filter { it.category.equals(category, true) }
        sameCategory.firstOrNull { it.name.equals(name, true) }?.let { return it }
        val key = detailKey(name)
        sameCategory.firstOrNull { detailKey(it.name) == key }?.let { return it }
        // Old save aliases used "Body Armor" / "Head Armor" in a few armor names.
        if (category.equals("Armor", true)) {
            val compact = name.lowercase().replace(" armor", "").replace(Regex("\\s+"), " ").trim()
            sameCategory.firstOrNull {
                val candidate = it.name.lowercase().replace(" armor", "").replace(Regex("\\s+"), " ").trim()
                candidate == compact || compact.contains(candidate) || candidate.contains(compact)
            }?.let { return it }
        }
        // Last resort is still category-scoped and chooses the longest specific name, never a generic cross-category token.
        return sameCategory.filter {
            val a = detailKey(it.name); a.length >= 5 && (key.contains(a) || a.contains(key))
        }.maxByOrNull { detailKey(it.name).length }
    }
}

/** Cyberware rules used by installation and Humanity calculations. */
object CyberwareCatalog {
    data class Rule(
        val hl: Int,
        val hlDice: String = "",
        val install: String = "",
        val group: String,
        val prerequisite: String? = null,
        val slotsUsed: Int = 1,
        val foundationCapacity: Int? = null,
        val noSlot: Boolean = false,
        val maxCopies: Int? = null,
        val paired: Boolean = false,
        val meatLimbAllowed: Boolean = false,
        val borgware: Boolean = false,
        val speedware: Boolean = false,
        val chipware: Boolean = false
    )

    private fun r(hl:Int, dice:String, install:String, group:String, prerequisite:String?=null,
                  slots:Int=1, capacity:Int?=null, noSlot:Boolean=false, max:Int?=null,
                  paired:Boolean=false, meat:Boolean=false, borg:Boolean=false,
                  speed:Boolean=false, chip:Boolean=false) = Rule(
        hl=hl, hlDice=dice, install=install, group=group, prerequisite=prerequisite,
        slotsUsed=slots, foundationCapacity=capacity, noSlot=noSlot, maxCopies=max,
        paired=paired, meatLimbAllowed=meat, borgware=borg, speedware=speed, chipware=chip
    )

    // Core Rulebook v1.25, cyberware tables pp.111-117. Keys are aliases matched longest-first.
    private val rules = linkedMapOf(
        // Fashionware — independent 7-slot body category.
        "biomonitor" to r(0,"","Mall","Fashionware"),
        "chemskin" to r(0,"","Mall","Fashionware"),
        "emp threading" to r(0,"","Mall","Fashionware"),
        "light tattoo" to r(0,"","Mall","Fashionware"),
        "shift tacts" to r(0,"","Mall","Fashionware"),
        "skinwatch" to r(0,"","Mall","Fashionware"),
        "tech hair" to r(0,"","Mall","Fashionware"),
        "techhair" to r(0,"","Mall","Fashionware"),

        // Neuralware and Chipware.
        "neural link" to r(7,"2d6","Clinic","Neuralware",capacity=5,noSlot=true,max=1),
        "braindance recorder" to r(7,"2d6","Clinic","Neuralware","Neural Link"),
        "chipware socket" to r(7,"2d6","Clinic","Neuralware","Neural Link",max=1),
        "interface plugs" to r(7,"2d6","Clinic","Neuralware","Neural Link"),
        "kerenzikov" to r(14,"4d6","Clinic","Neuralware","Neural Link",max=1,speed=true),
        "sandevistan" to r(7,"2d6","Clinic","Neuralware","Neural Link",max=1,speed=true),
        "chemical analyzer" to r(3,"1d6","N/A","Chipware","Chipware Socket",noSlot=true,chip=true),
        "memory chip chipware" to r(0,"","N/A","Chipware",noSlot=true,chip=true),
        "olfactory boost" to r(7,"2d6","N/A","Chipware","Chipware Socket",noSlot=true,chip=true),
        "pain editor" to r(14,"4d6","N/A","Chipware","Chipware Socket",noSlot=true,chip=true),
        "skill chip" to r(7,"2d6","N/A","Chipware","Chipware Socket",noSlot=true,chip=true),
        "tactile boost" to r(7,"2d6","N/A","Chipware","Chipware Socket",noSlot=true,chip=true),

        // Cyberoptics.
        "cybereye" to r(7,"2d6","Clinic","Cyberoptics",capacity=3,noSlot=true,max=7), // normal body supports 2; MultiOptic adds up to 5 more.
        "anti-dazzle" to r(2,"1d6/2","Mall","Cyberoptics","Cybereye",paired=true),
        "chyron" to r(2,"1d6/2","Mall","Cyberoptics","Cybereye"),
        "color shift" to r(2,"1d6/2","Mall","Cyberoptics","Cybereye"),
        "dartgun (cybereye)" to r(2,"1d6/2","Clinic","Cyberoptics","Cybereye",slots=3),
        "image enhance" to r(3,"1d6","Mall","Cyberoptics","Cybereye",paired=true),
        "low light/infrared/uv" to r(3,"1d6","Mall","Cyberoptics","Cybereye",slots=2,paired=true),
        "low light / infrared / uv" to r(3,"1d6","Mall","Cyberoptics","Cybereye",slots=2,paired=true),
        "microoptics" to r(2,"1d6/2","Clinic","Cyberoptics","Cybereye"),
        "microvideo" to r(2,"1d6/2","Clinic","Cyberoptics","Cybereye",slots=2),
        "radiation detector" to r(3,"1d6","Clinic","Cyberoptics","Cybereye"),
        "targeting scope" to r(3,"1d6","Clinic","Cyberoptics","Cybereye"),
        "teleoptics" to r(3,"1d6","Clinic","Cyberoptics","Cybereye"),
        "virtuality cybereye" to r(2,"1d6/2","Mall","Cyberoptics","Cybereye",paired=true),

        // Cyberaudio.
        "cyberaudio suite" to r(7,"2d6","Clinic","Cyberaudio",capacity=3,noSlot=true,max=1),
        "amplified hearing" to r(3,"1d6","Mall","Cyberaudio","Cyberaudio Suite",max=1),
        "audio recorder (cyberaudio)" to r(2,"1d6/2","Clinic","Cyberaudio","Cyberaudio Suite"),
        "bug detector cyberaudio" to r(2,"1d6/2","Mall","Cyberaudio","Cyberaudio Suite"),
        "homing tracer cyberaudio" to r(2,"1d6/2","Clinic","Cyberaudio","Cyberaudio Suite"),
        "internal agent" to r(3,"1d6","Mall","Cyberaudio","Cyberaudio Suite",max=1),
        "level damper" to r(2,"1d6/2","Mall","Cyberaudio","Cyberaudio Suite"),
        "radio communicator cyberaudio" to r(2,"1d6/2","Mall","Cyberaudio","Cyberaudio Suite"),
        "radio scanner/music player cyberaudio" to r(2,"1d6/2","Clinic","Cyberaudio","Cyberaudio Suite"),
        "radar detector cyberaudio" to r(2,"1d6/2","Clinic","Cyberaudio","Cyberaudio Suite"),
        "scrambler/descrambler cyberaudio" to r(2,"1d6/2","Mall","Cyberaudio","Cyberaudio Suite"),
        "voice stress analyzer" to r(3,"1d6","Mall","Cyberaudio","Cyberaudio Suite",max=1),

        // Internal body — independent 7-slot category.
        "audiovox" to r(3,"1d6","Clinic","Internal"),
        "contraceptive implant" to r(0,"","Mall","Internal"),
        "enhanced antibodies" to r(2,"1d6/2","Mall","Internal"),
        "cybersnake" to r(14,"4d6","Hospital","Internal"),
        "gills" to r(7,"2d6","Hospital","Internal"),
        "grafted muscle and bone lace" to r(14,"4d6","Hospital","Internal"),
        "independent air supply" to r(2,"1d6/2","Hospital","Internal"),
        "midnight lady" to r(7,"2d6","Clinic","Internal"),
        "mr. studd" to r(7,"2d6","Clinic","Internal"),
        "nasal filters" to r(2,"1d6/2","Clinic","Internal",max=1),
        "radar/sonar implant" to r(7,"2d6","Clinic","Internal"),
        "toxin binders" to r(2,"1d6/2","Clinic","Internal",max=1),
        "vampyres" to r(14,"4d6","Clinic","Internal"),

        // External body — independent 7-slot category.
        "hidden holster" to r(7,"2d6","Clinic","External"),
        "skin weave" to r(7,"2d6","Hospital","External",max=1),
        "subdermal armor" to r(14,"4d6","Hospital","External",max=1),
        "subdermal pocket" to r(3,"1d6","Clinic","External"),

        // Cyberarms.
        "cyberarm" to r(7,"2d6","Hospital","Cyberarm",capacity=4,noSlot=true,max=4),
        "mantis blades" to r(3,"1d6","Clinic","Cyberarm","Cyberarm",slots=2), // project Homebrew/2077 adaptation
        "monowire" to r(3,"1d6","Clinic","Cyberarm","Cyberarm"),             // project Homebrew/2077 adaptation
        "standard hand" to r(2,"1d6/2","Clinic","Cyberarm",noSlot=true,meat=true),
        "big knucks" to r(3,"1d6","Clinic","Cyberarm",meat=true),
        "cyberdeck (cyberarm)" to r(3,"1d6","Clinic","Cyberarm","Cyberarm",slots=3),
        "grapple hand" to r(3,"1d6","Clinic","Cyberarm","Cyberarm"),
        "medscanner (cyberarm)" to r(7,"2d6","Clinic","Cyberarm","Cyberarm",slots=2),
        "popup grenade launcher" to r(7,"2d6","Clinic","Cyberarm","Cyberarm",slots=2),
        "popup melee weapon" to r(7,"2d6","Clinic","Cyberarm","Cyberarm",slots=2),
        "popup shield" to r(7,"2d6","Clinic","Cyberarm","Cyberarm",slots=3),
        "popup ranged weapon" to r(7,"2d6","Clinic","Cyberarm","Cyberarm",slots=2),
        "quick change mount" to r(7,"2d6","Clinic","Cyberarm","Cyberarm"),
        "rippers" to r(3,"1d6","Clinic","Cyberarm",meat=true),
        "scratchers" to r(2,"1d6/2","Mall","Cyberarm",meat=true),
        "shoulder cam" to r(7,"2d6","Clinic","Cyberarm","Cyberarm",slots=2),
        "slice n dice" to r(3,"1d6","Clinic","Cyberarm",meat=true),
        "slice ‘n dice" to r(3,"1d6","Clinic","Cyberarm",meat=true),
        "subdermal grip" to r(3,"1d6","Clinic","Cyberarm","Neural Link",meat=true),
        "techscanner cyberarm" to r(7,"2d6","Clinic","Cyberarm","Cyberarm",slots=2),
        "tool hand" to r(3,"1d6","Clinic","Cyberarm",meat=true),
        "wolvers" to r(7,"2d6","Clinic","Cyberarm",meat=true),

        // Cyberlegs and general cyberlimb options.
        "cyberleg" to r(3,"1d6","Hospital","Cyberleg",capacity=3,noSlot=true,max=2),
        "standard foot" to r(2,"1d6/2","Clinic","Cyberleg",noSlot=true,meat=true),
        "grip foot" to r(3,"1d6","Clinic","Cyberleg","Cyberleg",paired=true),
        "jump booster" to r(3,"1d6","Clinic","Cyberleg","Cyberleg",slots=2,paired=true),
        "skate foot" to r(3,"1d6","Clinic","Cyberleg","Cyberleg",paired=true),
        "talon foot" to r(3,"1d6","Clinic","Cyberleg",meat=true),
        "web foot" to r(3,"1d6","Clinic","Cyberleg","Cyberleg",paired=true),
        "hardened shielding" to r(3,"1d6","Clinic","Cyberlimb","Cyberarm or Cyberleg"),
        "plastic covering" to r(0,"","Mall","Cyberlimb","Cyberarm or Cyberleg",noSlot=true),
        "realskinn covering" to r(0,"","Mall","Cyberlimb","Cyberarm or Cyberleg",noSlot=true),
        "superchrome covering" to r(0,"","Mall","Cyberlimb","Cyberarm or Cyberleg",noSlot=true),

        // Borgware.
        "artificial shoulder mount" to r(14,"4d6","Hospital","Borgware",noSlot=true,max=1,borg=true),
        "implanted linear frame beta" to r(14,"4d6","Hospital","Borgware",noSlot=true,max=1,borg=true),
        "implanted linear frame sigma" to r(14,"4d6","Hospital","Borgware",noSlot=true,max=1,borg=true),
        "multioptic mount" to r(14,"4d6","Hospital","Borgware",noSlot=true,max=1,borg=true),
        "sensor array" to r(14,"4d6","Clinic","Borgware","Cyberaudio Suite",noSlot=true,max=1,borg=true)
    )

    private fun normalize(name:String): String = name.lowercase()
        .replace("™","").replace("®","").replace("ß","beta").replace("∑","sigma")
        .replace("’", "'").replace(Regex("\\s+"), " ").trim()

    private fun entry(itemName: String): Map.Entry<String, Rule>? {
        val n = normalize(itemName)
        return rules.entries.sortedByDescending { it.key.length }.firstOrNull { n.contains(it.key) }
    }

    fun ruleFor(itemName: String): Rule? = entry(itemName)?.value
    fun humanityLossFor(itemName: String): Int = ruleFor(itemName)?.hl ?: StoreCatalog.items.firstOrNull { it.name.equals(itemName,true) }?.humanityLoss ?: 0
    fun humanityDiceFor(itemName:String): String = ruleFor(itemName)?.hlDice ?: StoreCatalog.items.firstOrNull { it.name.equals(itemName,true) }?.humanityDice.orEmpty()
    fun installFor(itemName:String): String = ruleFor(itemName)?.install ?: StoreCatalog.items.firstOrNull { it.name.equals(itemName,true) }?.slot.orEmpty()
    fun maxReductionFor(itemName: String): Int = ruleFor(itemName)?.let { if (it.hl == 0) 0 else if (it.borgware) 4 else 2 } ?: 0
    fun prerequisiteFor(itemName: String): String? = ruleFor(itemName)?.prerequisite
    fun isSpeedware(itemName: String): Boolean = ruleFor(itemName)?.speedware == true
    fun isFashionware(itemName: String): Boolean = ruleFor(itemName)?.group == "Fashionware"
    fun isChipware(itemName:String): Boolean = ruleFor(itemName)?.chipware == true
    fun isPaired(itemName:String): Boolean = ruleFor(itemName)?.paired == true
    fun slotsUsed(itemName:String): Int = ruleFor(itemName)?.slotsUsed ?: 0
    fun groupFor(itemName:String): String? = ruleFor(itemName)?.group

    fun details(itemName:String): List<Pair<String,String>> {
        val r = ruleFor(itemName) ?: return emptyList()
        return listOfNotNull(
            "Install" to r.install,
            "Category" to r.group,
            r.prerequisite?.let { "Requires" to it },
            if (!r.noSlot && r.group !in setOf("Chipware","Borgware")) "Option Slots" to r.slotsUsed.toString() else null,
            r.foundationCapacity?.let { "Provides Slots" to it.toString() },
            if (r.paired) "Paired" to "Requires two matching pieces" else null,
            if (r.speedware) "Speedware" to "Only one installed" else null
        )
    }

    private fun installed(items:List<InventoryItem>) = items.filter { it.category.equals("Cyberware",true) && it.equipped }
    private fun countInstalled(items:List<InventoryItem>, needle:String) = installed(items).count { normalize(it.name).contains(normalize(needle)) }
    private fun samePhysicalName(a:String,b:String) = normalize(a) == normalize(b)

    private fun availableFoundationIds(character:Character, group:String, slotsNeeded:Int): List<String> {
        val inv = character.inventory
        fun usedOn(id:String) = installed(inv).filter { it.installedIn == id }.sumOf { ruleFor(it.name)?.slotsUsed ?: 0 }
        return when(group) {
            "Cyberoptics" -> installed(inv).filter { normalize(it.name).contains("cybereye") }.filter { usedOn(it.instanceId) + slotsNeeded <= 3 }.map { it.instanceId }
            "Cyberarm" -> installed(inv).filter { normalize(it.name) == "cyberarm" }.filter { usedOn(it.instanceId) + slotsNeeded <= 4 }.map { it.instanceId }
            "Cyberleg" -> installed(inv).filter { normalize(it.name) == "cyberleg" }.filter { usedOn(it.instanceId) + slotsNeeded <= 3 }.map { it.instanceId }
            "Neuralware" -> installed(inv).filter { normalize(it.name).contains("neural link") }.filter { usedOn(it.instanceId) + slotsNeeded <= 5 }.map { it.instanceId }
            "Cyberaudio" -> {
                val extra = if (countInstalled(inv,"Sensor Array") > 0) 5 else 0
                installed(inv).filter { normalize(it.name).contains("cyberaudio suite") }.filter { usedOn(it.instanceId) + slotsNeeded <= 3 + extra }.map { it.instanceId }
            }
            "Cyberlimb" -> installed(inv).filter { normalize(it.name) == "cyberarm" || normalize(it.name) == "cyberleg" }.filter { foundation ->
                val cap = if (normalize(foundation.name) == "cyberarm") 4 else 3
                usedOn(foundation.instanceId) + slotsNeeded <= cap
            }.map { it.instanceId }
            else -> emptyList()
        }
    }

    /** Validation happens at installation, never at purchase. */
    fun validationError(itemName:String, character:Character, inventoryIndex:Int?=null): String? {
        val r = ruleFor(itemName) ?: return null
        val inv = character.inventory
        val installed = installed(inv)

        // Foundational body limits.
        if (normalize(itemName) == "cybereye") {
            val maxEyes = if (countInstalled(inv,"MultiOptic Mount") > 0) 7 else 2
            if (countInstalled(inv,"Cybereye") >= maxEyes) return "ظرفیت Cybereye کامل است: $maxEyes چشم. MultiOptic Mount سقف را تا 7 افزایش می‌دهد."
        }
        if (normalize(itemName) == "cyberarm") {
            val maxArms = if (countInstalled(inv,"Artificial Shoulder Mount") > 0) 4 else 2
            if (countInstalled(inv,"Cyberarm") >= maxArms) return "ظرفیت Cyberarm کامل است: $maxArms بازو. Artificial Shoulder Mount سقف را تا 4 افزایش می‌دهد."
        }
        if (normalize(itemName) == "cyberleg" && countInstalled(inv,"Cyberleg") >= 2) return "حداکثر دو Cyberleg روی بدن معمولی قابل نصب است."

        r.prerequisite?.let { req ->
            val ok = when(req) {
                "Cyberarm or Cyberleg" -> countInstalled(inv,"Cyberarm") + countInstalled(inv,"Cyberleg") > 0
                "Cybereye" -> countInstalled(inv,"Cybereye") > 0
                "Cyberarm" -> countInstalled(inv,"Cyberarm") > 0
                "Cyberleg" -> countInstalled(inv,"Cyberleg") > 0
                "Neural Link" -> countInstalled(inv,"Neural Link") > 0
                "Chipware Socket" -> countInstalled(inv,"Chipware Socket") > 0
                "Cyberaudio Suite" -> countInstalled(inv,"Cyberaudio Suite") > 0
                else -> countInstalled(inv,req) > 0
            }
            if (!ok) return "پیش‌نیاز نصب: $req"
        }

        if (r.speedware && installed.any { isSpeedware(it.name) }) return "فقط یک Speedware می‌تواند هم‌زمان نصب باشد."
        r.maxCopies?.let { max ->
            if (installed.count { samePhysicalName(it.name,itemName) } >= max) return "این Cyberware بیش از $max بار هم‌زمان قابل نصب نیست."
        }

        if (normalize(itemName).contains("linear frame sigma")) {
            if (character.stats.body < 6 || countInstalled(inv,"Grafted Muscle and Bone Lace") < 1)
                return "Linear Frame Sigma به BODY 6 و یک Grafted Muscle and Bone Lace نیاز دارد."
        }
        if (normalize(itemName).contains("linear frame beta")) {
            if (character.stats.body < 8 || countInstalled(inv,"Grafted Muscle and Bone Lace") < 2)
                return "Linear Frame Beta به BODY 8 و دو Grafted Muscle and Bone Lace نیاز دارد."
        }

        // A meat limb may contain only one of the explicitly allowed options. If a cyberlimb exists,
        // the option is preferentially assigned there and normal slots apply.
        if (r.meatLimbAllowed && r.group == "Cyberarm" && countInstalled(inv,"Cyberarm") == 0) {
            val meat = installed.count { ruleFor(it.name)?.meatLimbAllowed == true && ruleFor(it.name)?.group == "Cyberarm" && it.installedIn == null }
            if (meat >= 2) return "هر دست گوشتی فقط یک Cyberware مجازِ meat-arm می‌پذیرد؛ هر دو دست پر هستند."
            return null
        }
        if (r.meatLimbAllowed && r.group == "Cyberleg" && countInstalled(inv,"Cyberleg") == 0) {
            val meat = installed.count { ruleFor(it.name)?.meatLimbAllowed == true && ruleFor(it.name)?.group == "Cyberleg" && it.installedIn == null }
            if (meat >= 2) return "هر پای گوشتی فقط یک Cyberware مجازِ meat-leg می‌پذیرد؛ هر دو پا پر هستند."
            return null
        }

        // Paired options need two purchased copies and two suitable foundations with room.
        if (r.paired) {
            val availableCopies = inv.count { it.category.equals("Cyberware",true) && !it.equipped && samePhysicalName(it.name,itemName) }
            if (availableCopies < 2) return "این Cyberware باید Pair باشد؛ دو نسخه‌ی خریداری‌شده لازم است."
            val foundations = availableFoundationIds(character,r.group,r.slotsUsed)
            if (foundations.distinct().size < 2) return "برای نصب Pair به دو ${if(r.group=="Cyberoptics") "Cybereye" else "Cyberleg"} با Option Slot کافی نیاز است."
            return null
        }

        // Independent body categories are seven slots each.
        if (!r.noSlot && r.group in setOf("Fashionware","Internal","External")) {
            val used = installed.filter { ruleFor(it.name)?.group == r.group }.sumOf { ruleFor(it.name)?.slotsUsed ?: 0 }
            if (used + r.slotsUsed > 7) return "Option Slot کافی در ${r.group} وجود ندارد ($used/7 مصرف شده)."
        }

        if (!r.noSlot && r.group in setOf("Neuralware","Cyberoptics","Cyberaudio","Cyberarm","Cyberleg","Cyberlimb")) {
            if (availableFoundationIds(character,r.group,r.slotsUsed).isEmpty()) return "Option Slot کافی در ${r.group} وجود ندارد."
        }

        // Only one Chipware can occupy the single Chipware Socket at a time.
        if (r.chipware && installed.any { ruleFor(it.name)?.chipware == true }) return "Chipware Socket در حال حاضر یک Chipware فعال دارد؛ ابتدا آن را خارج کن."
        return null
    }

    fun foundationForInstall(itemName:String, character:Character): String? {
        val r = ruleFor(itemName) ?: return null
        if (r.foundationCapacity != null || r.group in setOf("Fashionware","Internal","External","Borgware","Chipware")) return null
        // Some Cyberlimb options use no Option Slot but are still physically attached to a limb.
        if (r.group in setOf("Cyberarm","Cyberleg","Cyberlimb","Neuralware","Cyberoptics","Cyberaudio"))
            return availableFoundationIds(character,r.group, if (r.noSlot) 0 else r.slotsUsed).firstOrNull()
        return null
    }

    fun pairedFoundationIds(itemName:String, character:Character): List<String> {
        val r=ruleFor(itemName) ?: return emptyList()
        if (!r.paired) return emptyList()
        return availableFoundationIds(character,r.group,r.slotsUsed).distinct().take(2)
    }

    /** Post-character-generation HL uses the dice in parentheses, not the chargen average. */
    fun rollHumanityLoss(itemName:String): Int {
        val dice = humanityDiceFor(itemName).lowercase().replace(" ","")
        if (dice.isBlank() || humanityLossFor(itemName) == 0) return 0
        val m = Regex("(\\d+)d6(?:/(\\d+))?").matchEntire(dice) ?: return humanityLossFor(itemName)
        val count = m.groupValues[1].toInt()
        val divisor = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toInt() ?: 1
        val raw = DiceSource.roll(count, 6, "Humanity Loss — $itemName").sum()
        return kotlin.math.ceil(raw.toDouble()/divisor).toInt()
    }

    /** FAQ exception: Chipware and a previously-used Cyberarm on a Quick Change Mount do not pay HL again. */
    fun shouldPayHumanityOnInstall(item:InventoryItem, character:Character): Boolean {
        val r = ruleFor(item.name) ?: return false
        if (r.hl == 0) return false
        if (r.chipware && item.cyberwareInstallCount > 0) return false
        if (normalize(item.name) == "cyberarm" && item.cyberwareInstallCount > 0) {
            val hasQuickChange = character.inventory.any { normalize(it.name).contains("quick change mount") && it.installedIn == item.instanceId && it.cyberwareInstallCount > 0 }
            if (hasQuickChange) return false
        }
        return true
    }
}

object ArmorCatalog {
    fun spFor(itemName: String): Int? { val n=itemName.lowercase(); return when {
        n.contains("metAlgear".lowercase()) -> 18; n.contains("flak") -> 15;
        n.contains("heavy armorjack") || n.contains("sp13") -> 13;
        n.contains("medium armorjack") || n.contains("sp12") -> 12;
        n.contains("light armorjack") || n.contains("bodyweight") || n.contains("sp11") || n.contains("fashion armor") -> 11;
        n.contains("kevlar") || n.contains("sp7") -> 7; n.contains("leather") || n.contains("sp4") -> 4;
        else -> null }
    }
    fun penaltyFor(itemName: String): Int? { val n=itemName.lowercase(); return when {
        n.contains("medium armorjack") || n.contains("heavy armorjack") -> -2
        n.contains("flak") || n.contains("metalgear") -> -4
        else -> if (spFor(itemName) != null) 0 else null
    } }
    fun isShield(itemName: String): Boolean = itemName.contains("shield", true)
}
