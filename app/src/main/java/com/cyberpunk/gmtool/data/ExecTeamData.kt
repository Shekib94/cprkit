package com.cyberpunk.gmtool.data

/**
 * داده‌های مرجع نقش اگزک (Exec) — متن کامل Core برای Teamwork، Team Memberها و
 * جدول‌های Loyalty. هیچ مکانیکی اینجا نیست؛ فقط متن و جدول، تا UI فقط رندر کند.
 *
 * منبع: بخش Exec Role Ability (Teamwork) در کتاب Core، شامل
 * Signing Bonus / Corporate Housing / Corporate Health Insurance / Team Members /
 * Creating Your Team Members و پنج کلاسِ آمادهٔ Team Member.
 */

/** یک کلاس Team Member با جدول استات ۶ ردیفی (d6) و بستهٔ مهارت/سایبرور/تجهیزات. */
data class ExecTeamClass(
    val name: String,                 // نام انگلیسی (هم‌نام تمپلیت NPC در برنامه)
    val nameFa: String,
    val coverJobsFa: String,
    val trueJobFa: String,
    val statRows: List<List<Int>>,    // 6 ردیف × [INT, REF, DEX, TECH, COOL, WILL, MOVE, BODY, EMP]
    val skills2Fa: String,
    val skills4Fa: String,
    val skills6Fa: String,
    val cyberwareFa: String,
    val gearFa: String
)

object ExecTeamData {

    val introFa: List<String> = listOf(
        "در روزهای قدیم، پیش از «تایم آو دِ رِد»، تو یک MBA جاه‌طلب، سریع و بی‌مکث بودی که داشت از نردبان شرکتی بالا می‌رفت. درست است، یعنی روحت را به شرکت می‌فروختی؛ اما واقعیت این بود: کورپوریشن‌ها بر جهان حکومت می‌کردند. دولت‌ها، بازارها، ملت‌ها و ارتش‌ها را کنترل می‌کردند — هر چه بگویی. و تو می‌دانستی هر کسی که کورپوریشن‌ها را کنترل کند، همه‌چیز را کنترل می‌کند. اما وقتی بزرگ‌ترین مگاکورپ‌های روی کره‌ی زمین درگیر جنگی شدند که با هر جنگی که دولت‌های واقعی می‌توانستند راه بیندازند برابری می‌کرد، همه‌چیز عوض شد.",
        "خب، همین حالا زندگی تو به‌عنوان یک مدیر جوانِ رده‌پایین هیچ‌چیز جز ساده نیست. زیردست‌هایی هستند که برای گرفتن جایت حاضرند بکشند — نه شوخی. و بالادستی‌هایی هستند که برای نگه‌داشتن صندلی‌شان حاضرند بیرونت کنند — نه شوخی. و در مورد کشتن هم شوخی نمی‌کنند: هر تازه‌بالا‌آمده‌ای در شرکت، تیم اختصاصی خودش از سولوها و نت‌رانرها را دارد تا پروژه‌های مهم را پوشش دهد. هفته‌ی پیش تو یک تیم ترکیبی از سولوها، نت‌رانرها و تک‌ها را در یک مأموریت رقابتی رهبری کردی تا یک محقق را از یک شرکت رقیب «استخراج» کنی.",
        "به خودت گفتی که به شرکت پیوستی تا جای بهتری بسازی — از داخل، کار کن، گفتی. یا فقط تا وقتی که شرکت خودت را راه بیندازی که... خب، کمی صادق‌تر باشد. اما حالا دیگر مطمئن نیستی. آرمان‌هایت کمی لکه‌دار شده و اوضاع دارد خیلی بی‌امید می‌شود. اما اخلاقیات الان نگرانی‌ات نیست. یک گزارش تا یک ساعت دیگر موعدش است و به نظر می‌رسد آن آدم در واحد فروش دارد برنامه می‌ریزد که دیتابیس تو را برای همیشه از بین ببرد.",
        "تو اول او را می‌کشی."
    )

    val abilityTitle = "توانایی نقش: کار تیمی (Teamwork)"

    val abilityBox =
        "درست مثل یک مدیر اجرایی واقعی، اگزک تیمی می‌سازد که اعضایش کمک می‌کنند به اهدافشان برسند — قانونی یا غیرقانونی، تا جایی که اخلاق اجازه دهد. توانایی نقشِ اگزک «کار تیمی» است. اعضای تیم یک شرح‌وظیفه‌ی آشکار دارند (مثل منشی یا راننده) و هم یک نقش پنهانی (مثل نت‌رانر، بادیگارد یا آدم‌کش). به‌علاوه، مسکن رایگان و یک دست لباس شیک هم می‌گیرند!"

    /** مزایای رنک (Signing Bonus / Housing / Insurance) — متن دقیق Core. */
    val rankPerks: List<Triple<Int, String, String>> = listOf(
        Triple(1, "پاداش امضا (Signing Bonus)", "در رنک ۱ کار تیمی، به‌عنوان هدیه، یک دست لباس شامل کت، تاپ، شلوار و کفش تجاری به اگزک داده می‌شود که او را به‌عنوان عضوی از نخبگان تجاری شناسایی می‌کند. اگزک نمی‌تواند این‌ها را بفروشد؛ وگرنه شک برانگیز می‌شود."),
        Triple(2, "مسکن شرکتی (Corporate Housing)", "در رنک ۲ کار تیمی، اگزک به یکی از آپارتمان‌های شرکت (Corporate Conapts) دسترسی پیدا می‌کند. تا وقتی عضو آن شرکت باشد، می‌تواند بدون پرداخت اجاره یا هیچ هزینه‌ی دیگری آنجا بماند. (سبک زندگی خودش را باید مثل همیشه جداگانه بخرد.) اگر اگزک شرکت را ترک کند و به شرکت دیگری بپیوندد، آن شرکت هم همین پیشنهاد را به او می‌دهد و حتی هزینه‌ی جابه‌جایی همه‌ی وسایلش به خانه‌ی جدید را هم می‌پردازد."),
        Triple(7, "مسکن شرکتی — ارتقا", "در رنک ۷ کار تیمی، مسکن شرکتی اگزک به یک Beaverville House در Executive Zone ارتقا می‌یابد."),
        Triple(10, "مسکن شرکتی — ارتقای شدید", "در رنک ۱۰ کار تیمی، مسکن شرکتی اگزک به‌طور چشمگیر به یک Beaverville McMansion در Executive Zone یا یک پنت‌هاوس لوکس در Corporate Zone ارتقا می‌یابد."),
        Triple(6, "بیمه‌ی درمانی شرکتی (Corporate Health Insurance)", "در رنک ۶ کار تیمی، اگزک پوشش Trauma Team Silver می‌گیرد که شرکت هر ماه پولش را می‌پردازد. اگر اگزک شرکت را ترک کند و به شرکت دیگری بپیوندد، آن شرکت هم همین پیشنهاد را می‌دهد."),
        Triple(8, "بیمه — ارتقا", "در رنک ۸ کار تیمی، شرکت پوشش اگزک را به Trauma Team Executive ارتقا می‌دهد.")
    )

    /** قواعد دقیق Team Memberها (از بخش Team Members در Core). */
    val teamRules: List<Pair<String, String>> = listOf(
        "چند نفر داری؟" to
            "از رنک ۳ به بعد، کار تیمی یک Team Member به اگزک می‌دهد. رنک ۵ و رنک ۹ هر کدام یک Team Member دیگر می‌دهند؛ سقفِ نهایی ۳ Team Member در رنک ۹ است.",
        "چطور ساخته می‌شوند؟" to
            "Team Memberها از یک جدول مخصوص برای هر شغل ریخته می‌شوند. اگزک انتخاب می‌کند چه کلاسی از Team Member می‌خواهد، اما تاس‌ریزی روی جدول تعیین می‌کند HR چه STATهایی برایش استخدام می‌کند.",
        "سه تفاوت با بازیکن‌ها" to
            "• مهارت‌هایشان ارتقا نمی‌یابد. HP‌شان را مثل بازیکن‌ها تعیین و heal می‌کنند.\n• توسط GM کنترل می‌شوند و توانایی‌شان در پیروی از یک دستور به Loyalty و موفقیت در Loyalty Check بستگی دارد.\n• نمی‌توانند زره‌ای غیر از Light Armorjack به تن کنند. سیاست شرکت.",
        "گم‌شدن Team Member" to
            "اگر یک Team Member گم شود، HR تجهیزاتش را پس می‌گیرد و در جلسه‌ی بازی بعدی جایگزینش می‌کند. این «استخدام تازه» STATهای جدیدی دارد، اما Loyalty شروعش به 1 کاهش می‌یابد (شنیده‌اند چه اتفاقی افتاده). به‌علاوه، این کار ۲۰۰eb «هزینه‌ی استخدام» (رشوه) دیگر هم برای اگزک تمام می‌شود.",
        "Loyalty چیست؟" to
            "هرچند Team Memberها برای اگزک کار می‌کنند، پهپادِ بی‌فکر نیستند. بر اساس Loyalty‌شان به رئیس — یا حقوقی که امضا کرده‌اند — وظایف را انجام می‌دهند. Loyalty یک مقدار متغیر است؛ اگزک باید در طول هر جلسه‌ی بازی کارهایی بکند که Loyalty را بالا ببرد یا از دست نده. Loyalty بین جلسه‌های بازی حداکثر ۱۰ است، اما در طول یک جلسه‌ی بازی حد ندارد.",
        "Loyalty Save" to
            "وقتی اگزک وظیفه‌ای به یک Team Member می‌سپارد، GM باید 1d6 زیرِ Loyalty فعلی همان عضو بیندازد. اگر Save شکست بخورد، عضو ممکن است دستور را رد کند یا بی‌نتیجه بگذارد، یا حتی علیه اگزک بچرخد. اگر Loyalty به ۰ یا کمتر برسد، عضو فعالانه تلاش می‌کند در برابر اگزک به دشمنانش خیانت کند. اگر در پایان یک جلسه Loyalty یک عضو زیر ۰ باشد، به HR شکایت می‌کند و بسته به هوس HR یا درخواست انتقال می‌دهد یا بعد از یک رد شدن استعفا می‌دهد؛ هر کدام باشد، او رفته است."
    )

    val loyaltyGain: List<Pair<String, Int>> = listOf(
        "از کار Team Member تعریف کن. (بیش از حد در یک هفته انجامش بدهی، دیگر Loyalty نمی‌دهد.)" to 1,
        "به او پاداش یا perk حداقل ۲۰۰eb ارزش بده." to 4,
        "از او در برابر مدیریت دفاع کن." to 4,
        "۲۰٪ از درآمد یک کار را به او بده." to 6,
        "به Team Member مرخصی با حقوق بده. (باید یک جلسه‌ی بازی کامل باشد.)" to 6,
        "جان خودت را برای Team Member به خطر بینداز." to 8
    )

    val loyaltyLoss: List<Pair<String, Int>> = listOf(
        "در یک جلسه‌ی بازی کامل هیچ Loyalty با Team Member به دست نیاور." to -1,
        "سرزنش یا تحقیرش کن، یا کارش را مسخره کن." to -2,
        "نقش Team Member در یک کار را نادیده بگیر. تولدش را فراموش کن." to -4,
        "بونوس یا perk ای که قول داده بودی را نده." to -6,
        "زیر اتوبوس مدیریت بیندازش." to -6,
        "در میانه‌ی آتش، Team Member را رها کن." to -8
    )

    fun maxTeamMembers(rank: Int): Int = when {
        rank >= 9 -> 3
        rank >= 5 -> 2
        rank >= 3 -> 1
        else -> 0
    }

    val teamMemberClasses: List<ExecTeamClass> = listOf(

        ExecTeamClass(
            name = "Company Bodyguard",
            nameFa = "بادیگارد شرکتی",
            coverJobsFa = "اسکورت، مربی شخصی",
            trueJobFa = "محافظت از اگزک در موقعیت‌های خطرناک",
            statRows = listOf(
        listOf(3, 7, 7, 4, 7, 6, 4, 8, 4),
        listOf(5, 8, 6, 2, 7, 8, 4, 8, 4),
        listOf(4, 8, 5, 3, 7, 8, 6, 6, 3),
        listOf(4, 7, 8, 4, 7, 7, 4, 7, 2),
        listOf(3, 8, 5, 2, 8, 7, 4, 6, 7),
        listOf(5, 7, 7, 2, 7, 6, 5, 7, 4)
    ),
            skills2Fa = "Concentration، Conversation، Education، First Aid، Human Perception، Language (Streetslang)، Local Expert (Your Home)، Persuasion، Stealth",
            skills4Fa = "Athletics، Evasion، Interrogation، Perception، Resist Torture/Drugs، Tactics",
            skills6Fa = "Handgun، Brawling",
            cyberwareFa = "Enhanced Antibodies، Subdermal Armor (SP11)، Cyberaudio Suite، Internal Agent، Homing Tracer",
            gearFa = "Agent، Light Armorjack (SP11)، Very Heavy Pistol، Basic VH Pistol Ammo ×50"
        ),
        ExecTeamClass(
            name = "Company Covert Operative",
            nameFa = "عملیات پنهان شرکتی",
            coverJobsFa = "دستیار شخصی، استایلیست",
            trueJobFa = "نگه‌داشتن دست‌های اگزک از کثیف‌کاری",
            statRows = listOf(
        listOf(4, 8, 5, 4, 6, 8, 5, 7, 3),
        listOf(3, 8, 6, 2, 8, 6, 6, 6, 5),
        listOf(6, 7, 5, 5, 7, 6, 3, 7, 4),
        listOf(5, 6, 2, 3, 6, 8, 7, 6, 4),
        listOf(3, 8, 4, 4, 8, 7, 4, 8, 4),
        listOf(5, 8, 3, 7, 7, 8, 3, 6, 3)
    ),
            skills2Fa = "Athletics، Brawling، Concentration، Conversation، Education، First Aid، Language (Streetslang)، Local Expert (Your Home)، Perception، Persuasion",
            skills4Fa = "Bribery، Bureaucracy، Business، Evasion، Human Perception، Pick Lock، Streetwise، Trading، Wardrobe & Style",
            skills6Fa = "Handgun، Stealth",
            cyberwareFa = "Cybereyes با Low Light/Infrared/UV جفت‌شده، Color Shift، Cyberarm با Grapple Hand، Popup Ranged Weapon (Very Heavy Pistol)، Realskinn™ Covering",
            gearFa = "Agent، Light Armorjack (SP11)، Very Heavy Pistol، Basic VH Pistol Ammo ×50"
        ),
        ExecTeamClass(
            name = "Company Driver",
            nameFa = "راننده شرکتی",
            coverJobsFa = "خدمتکار خودرو، راننده شخصی",
            trueJobFa = "رانندگی، پرواز و نگه‌داری وسایل نقلیه‌ی تیم",
            statRows = listOf(
        listOf(5, 8, 6, 4, 6, 5, 6, 5, 5),
        listOf(5, 7, 7, 5, 5, 7, 4, 7, 3),
        listOf(6, 8, 8, 4, 7, 4, 5, 6, 2),
        listOf(8, 7, 4, 5, 4, 7, 5, 6, 4),
        listOf(7, 8, 3, 5, 7, 6, 4, 6, 4),
        listOf(6, 8, 6, 6, 8, 5, 3, 5, 3)
    ),
            skills2Fa = "Athletics، Concentration، Conversation، Education، First Aid، Human Perception، Language (Streetslang)، Local Expert (Your Home)، Perception، Persuasion",
            skills4Fa = "Brawling، Endurance، Evasion، Land Vehicle Tech، Pilot Air Vehicle، Pilot Sea Vehicle، Sea Vehicle Tech، Stealth، Tracking",
            skills6Fa = "Drive Land Vehicle، Handgun",
            cyberwareFa = "Radar/Sonar Implant، Cyberaudio Suite، Internal Agent، Homing Tracer، Radar Detector",
            gearFa = "Light Armorjack (SP11)، Very Heavy Pistol، Compact Groundcar با Seating Upgrade، Basic VH Pistol Ammo ×50"
        ),
        ExecTeamClass(
            name = "Company Netrunner",
            nameFa = "نت‌رانر شرکتی",
            coverJobsFa = "مهندس I.T.، متخصص تحقیق",
            trueJobFa = "نت‌رانینگ و گردآوری اطلاعات",
            statRows = listOf(
        listOf(6, 7, 8, 7, 5, 4, 5, 5, 3),
        listOf(7, 8, 4, 6, 8, 3, 4, 6, 4),
        listOf(5, 6, 8, 8, 6, 6, 4, 4, 3),
        listOf(7, 8, 5, 6, 4, 6, 6, 5, 5),
        listOf(5, 8, 8, 5, 5, 3, 6, 4, 6),
        listOf(8, 7, 6, 6, 4, 7, 4, 4, 4)
    ),
            skills2Fa = "Interface (Netrunner Role Ability)، Athletics، Brawling، Concentration، Conversation، Evasion، First Aid، Human Perception، Language (Streetslang)، Local Expert (Your Home)، Perception، Persuasion",
            skills4Fa = "Basic Tech، Cryptography، Cybertech، Education، Electronics/Security Tech (x2)، Forgery، Library Search، Handgun، Stealth",
            skills6Fa = "—",
            cyberwareFa = "Neural Link، Chipware Socket، Pain Editor، Interface Plugs، Cybereyes with Virtuality",
            gearFa = "Agent، Light Armorjack (SP11)، Cyberdeck (7 slots: Sword, Sword, Killer, Worm, Worm, Armor)، Very Heavy Pistol، Basic VH Pistol Ammo ×50"
        ),
        ExecTeamClass(
            name = "Company Technician",
            nameFa = "تکنسین شرکتی",
            coverJobsFa = "مهندس I.T.، کارآموز",
            trueJobFa = "تعمیر تجهیزات و سلاح‌های تیم",
            statRows = listOf(
        listOf(8, 8, 5, 7, 3, 4, 4, 5, 6),
        listOf(8, 7, 6, 8, 3, 5, 5, 4, 4),
        listOf(8, 6, 5, 8, 4, 3, 4, 7, 6),
        listOf(8, 7, 5, 7, 4, 4, 6, 5, 5),
        listOf(7, 7, 3, 7, 5, 3, 6, 6, 3),
        listOf(7, 8, 5, 8, 6, 3, 3, 5, 5)
    ),
            skills2Fa = "Athletics، Brawling، Concentration، Conversation، Evasion، First Aid، Human Perception، Language (Streetslang)، Local Expert (Your Home)، Perception، Persuasion، Stealth",
            skills4Fa = "Education، Handgun، Weaponstech (x2)",
            skills6Fa = "Basic Tech، Cybertech، Electronics/Security Tech (x2)",
            cyberwareFa = "Tool Hand، Cyberaudio Suite، Internal Agent، Bug Detector، Audio Recorder",
            gearFa = "Light Armorjack (SP11)، Very Heavy Pistol، Basic VH Pistol Ammo ×50"
        ),
    )

    fun classFor(name: String): ExecTeamClass? =
        teamMemberClasses.firstOrNull { it.name.equals(name, true) }
}
