package com.cyberpunk.gmtool.data

/** Core v1.25 reference data used by the in-app Medtech and Media GM assistants. */
object RoleAssistantData {
    data class Pharmaceutical(
        val name: String,
        val effectFa: String,
        val limitFa: String
    )

    val corePharmaceuticals = listOf(
        Pharmaceutical("Antibiotic", "اگر هدف از قبل روند بهبود طبیعی را شروع کرده باشد، به مدت یک هفته هر روز ۲ Hit Point بیشتر heal می‌کند.", "هر نفر در یک زمان فقط از یک مورد مصرف Antibiotic می‌تواند سود ببرد."),
        Pharmaceutical("Rapidetox", "وقتی هدف یک dose Rapidetox می‌زند، فوراً از اثرات آن drug، poison یا intoxicant پاک می‌شود.", "کتاب برای این دارو سقف تعداد مصرف تعیین نکرده است."),
        Pharmaceutical("Speedheal", "وقتی هدف در وضعیت Mortally Wounded نباشد، فوراً به اندازه‌ی BODY + WILL خودش HP پس می‌گیرد.", "هر نفر فقط روزی یک بار می‌تواند از Speedheal سود ببرد."),
        Pharmaceutical("Stim", "وقتی هدف یک dose Stim می‌زند، تا یک ساعت تمام penaltyهای وضعیت Seriously Wounded را نادیده می‌گیرد.", "هر نفر فقط روزی یک بار می‌تواند از Stim سود ببرد."),
        Pharmaceutical("Surge", "وقتی هدف یک dose Surge می‌زند، تا ۲۴ ساعت کامل بدون خواب و بدون افت کار می‌کند.", "هر نفر فقط هفته‌ای یک بار می‌تواند از Surge سود ببرد.")
    )

    data class CryoBenefit(val level: Int, val textFa: String)
    val cryoBenefits = listOf(
        CryoBenefit(1, "یک Cryopump می‌گیری."),
        CryoBenefit(2, "Registered Cryotank Technician می‌شوی و دسترسی نامحدود ۲۴/۷ به یک Cryotank (هر بار یکی) در هر مرکز cryotank داری که کورپوریشن‌های پزشکی یا سازمان‌های دولتی اداره کنند."),
        CryoBenefit(3, "یک Cryotank مال خودت می‌شوی که در اتاقی به انتخاب خودت نصب می‌شود."),
        CryoBenefit(4, "۲ Cryotank دیگر می‌گیری که در همان اتاقِ Cryotank اولت جا می‌شوند؛ Cryopump تو ۲ charge دارد و حداکثر ظرفیت حملش به ۲ نفر در stasis افزایش می‌یابد."),
        CryoBenefit(5, "۳ Cryotank دیگر (در همان اتاق) می‌گیری و Cryopump تو ۳ charge دارد و حداکثر ظرفیت حملش به ۳ نفر در stasis افزایش می‌یابد.")
    )

    data class RumorLevel(
        val name: String,
        val passiveDv: Int,
        val activeDv: Int,
        val descriptionFa: String
    )

    val rumorLevels = listOf(
        RumorLevel("Vague", 7, 13, "فقط حداقل اطلاعات لازم برای شروع تحقیق را می‌دهد."),
        RumorLevel("Typical", 9, 15, "می‌دانی قدم بعدی تحقیق کجاست و تصویری گذرا از حقیقت داری."),
        RumorLevel("Substantial", 11, 17, "علاوه بر Typical، اطلاعات مشخص مثل نام، مکان و زمان دارد."),
        RumorLevel("Detailed", 13, 21, "اطلاعاتی دارد که در صورت راستی‌آزمایی می‌تواند به evidence قابل استفاده در Story تبدیل شود.")
    )

    data class MediaProfile(
        val accessFa: String,
        val audienceFa: String,
        val believability: Int,
        val impactFa: String
    )

    fun mediaProfile(rank: Int): MediaProfile = when (rank.coerceIn(1, 10)) {
        1, 2 -> MediaProfile(
            "رئیس محلی، gang lord، رهبران محله",
            "محله‌ی بلافاصله اطراف",
            2,
            "تغییر کوچک و تدریجی؛ خلافکارهای سطح پایین ممکن است بترسند و کمی رفتارشان را عوض کنند."
        )
        3, 4 -> MediaProfile(
            "رئیس gang شهری، سیاستمدار خرد، Corp Exec، چهره‌ی شناخته‌شده‌ی محله",
            "در screamsheet یا Data Pool محلی شناخته‌شده‌ای",
            3,
            "اثر مستقیم محلی؛ افراد سطح پایین ممکن است بازداشت یا از قدرت کنار گذاشته شوند."
        )
        5, 6 -> MediaProfile(
            "بازیگر مهم شهری، سیاستمدار شهر، سلبریتی محلی",
            "انتشار Citywide؛ ستون‌نویس/همکار ثابت رسانه‌های محلی",
            4,
            "اثر در سطح شهر؛ افراد قدرتمندتر ممکن است سقوط کنند و حتی قانون محلی تصویب شود."
        )
        7, 8 -> MediaProfile(
            "رئیس Corp محلی، شهردار/مدیر شهر، سلبریتی شهری",
            "انتشار Statewide؛ خودت هم یک چهره‌ی کوچک رسانه‌ای هستی",
            5,
            "اثر در چند شهر؛ شرکت‌های متوسط یا دولت‌های محلی ممکن است کنار زده شوند و قوانین منطقه‌ای تغییر کند."
        )
        9 -> MediaProfile(
            "رئیس یک division شرکتی، سیاستمدار ایالتی، سلبریتی معروف",
            "در سطح کشور شناخته‌شده‌ای؛ معمولاً از national newsfeed",
            6,
            "اثر در مقیاس یک کشور/منطقه‌ی بزرگ؛ شرکت‌های بزرگ یا دولت‌های محلی ممکن است سقوط کنند."
        )
        else -> MediaProfile(
            "رهبر مهم جهان، رئیس یک Corporation بزرگ، سلبریتی جهانی",
            "شناخته‌شده در سراسر جهان",
            7,
            "اثر جهانی؛ Megacorpها یا دولت‌های قدرتمند ممکن است سقوط کنند و قوانین بین‌المللی تغییر کند."
        )
    }

    data class NomadInteractionAdvice(
        val level: String,
        val titleFa: String,
        val adviceFa: String,
        val familyCostFa: String
    )

    val nomadInteractionSituations = listOf(
        "وسیله یا تعویض وسیله",
        "مسیر امن / عبور از Badlands",
        "حمل بار یا جابه‌جایی افراد",
        "تعمیر یا بازیابی Family Vehicle",
        "اطلاعات جاده‌ای / کاروان‌ها",
        "کمک مسلحانه یا نجات",
        "مذاکره با Nomad Pack دیگر",
        "Family از PC کمک می‌خواهد",
        "سوخت / قطعه اضطراری",
        "پناه یا مخفی‌کردن Crew"
    )

    fun nomadInteractionAdvice(situation: String, familyNearby: Boolean, urgent: Boolean): NomadInteractionAdvice = when (situation) {
        "وسیله یا تعویض وسیله" -> NomadInteractionAdvice(
            if (familyNearby) "RECOMMENDED" else "OPTIONAL",
            "این دقیقاً حوزه Family Motorpool است",
            if (familyNearby) "اگر Nomad درخواست تعویض Family Vehicle بدهد، طبق Core در صورت نزدیک بودن Family وسیله جایگزین صبح روز بعد می‌رسد. برای نیاز فوری، خود PC باید با وسیله فعلی یا راه‌حل دیگری کنار بیاید." else "Family دور است؛ تماس ممکن است Hook بسازد، اما تحویل فوری وسیله را تضمین نکن.",
            "استفاده از Motorpool حق Role است؛ آن را Favor اضافه حساب نکن مگر درخواست فراتر از Motorpool باشد."
        )
        "مسیر امن / عبور از Badlands" -> NomadInteractionAdvice(
            "RECOMMENDED", "Spotlight مناسب Nomad",
            "Nomadها متخصص roadcraft و جابه‌جایی بین safezoneها هستند. اطلاعات مسیر، وضعیت جاده یا نام یک contact محلی کمک مناسبی است؛ اگر خطر جدی است، Roll مرتبط یا هزینه داستانی بخواه.",
            "اطلاعات عادی Family معمولاً بدهی بزرگ نمی‌سازد؛ اسکورت، تغییر مسیر کاروان یا ریسک برای Pack می‌تواند Favor بخواهد."
        )
        "حمل بار یا جابه‌جایی افراد" -> NomadInteractionAdvice(
            "RECOMMENDED", "Family و Role را وارد لجستیک داستان کن",
            "اگر مسئله اصلی رساندن آدم یا بار است، Nomad باید نقش مرکزی داشته باشد. Family می‌تواند ظرفیت، contact یا نقطه تحویل معرفی کند؛ انجام کل مأموریت به‌جای Crew را رایگان نکن.",
            "هرچه درخواست زمان، خطر یا ظرفیت بیشتری از Pack بگیرد، Favor/تعهد داستانی بزرگ‌تر مناسب‌تر است."
        )
        "تعمیر یا بازیابی Family Vehicle" -> NomadInteractionAdvice(
            "RECOMMENDED", "قانون Motorpool را اجرا کن",
            "Family Vehicle نابودشده را Family طی یک هفته کامل تعمیر می‌کند و معمولاً 500eb انتظار می‌رود. تعمیرات روزمره و Damage عادی مسئولیت خود Nomad است.",
            "بخش 500eb قانون Core است؛ بخشش هزینه می‌تواند Reputation/رابطه داستانی را تحت تأثیر قرار دهد."
        )
        "اطلاعات جاده‌ای / کاروان‌ها" -> NomadInteractionAdvice(
            "OPTIONAL", "یک تماس کوتاه Family می‌تواند کافی باشد",
            "برای شایعه جاده، بسته‌شدن مسیر، Road Gang یا حرکت کاروان، Family منبع طبیعی اطلاعات است. اطلاعات حساس یا خارج از حوزه Pack را خودکار کامل نده.",
            "اطلاعات معمول کم‌هزینه است؛ اطلاعات محرمانه یا ایجاد دردسر برای Pack می‌تواند Favor بخواهد."
        )
        "کمک مسلحانه یا نجات" -> NomadInteractionAdvice(
            if (urgent) "FAMILY COST" else "OPTIONAL", "Family خدمات نجات رایگان نیست",
            "کمک رزمی، Convoy مسلح یا نجات مستقیم باید یک تصمیم داستانی باشد، نه Ability خودکار Moto. اگر Family وارد خطر می‌شود، زمان رسیدن، افراد در دسترس و پیامد را مشخص کن.",
            "معمولاً Favor/بدهی یا مأموریت متقابل مناسب است؛ در خطر شدید حتی Family ممکن است رد کند."
        )
        "مذاکره با Nomad Pack دیگر" -> NomadInteractionAdvice(
            "RECOMMENDED", "هویت Nomad یک مزیت روایی مهم است",
            "Family name، نوع Pack و رابطه قبلی را وارد صحنه کن. Moto خودش Skill اجتماعی نیست؛ برای مذاکره از Skill مناسب استفاده شود، اما تعلق به Family می‌تواند دسترسی یا Context ایجاد کند.",
            "اعتبار Family را خرج‌کردنی فرض نکن؛ نتیجه تعامل را در رابطه دو Pack ثبت کن."
        )
        else -> NomadInteractionAdvice(
            "FAMILY CALLS FIRST", "این بار Family محرک داستان است",
            "Family فقط منبع درخواست‌های PC نیست. یک محموله گم‌شده، عضو مفقود، بدهی قدیمی، درگیری با Road Gang یا نیاز به اسکورت می‌تواند Nomad را مستقیم وارد Hook کند.",
            "پاداش می‌تواند پول، بهبود رابطه، اطلاعات یا فرصت Motorpool باشد؛ از دادن Rank/Vehicle اضافه خارج از قواعد Core خودداری کن."
        )
    }

    data class NomadHook(val titleFa: String, val requestFa: String, val complicationFa: String, val spotlightFa: String, val rewardFa: String)

    val nomadHooks = listOf(
        NomadHook("کاروان ناپدیدشده", "یک Convoy خانوادگی در مسیر Badlands به مقصد نرسیده؛ ردیابی‌اش کنید.", "آخرین مسیر ثبت‌شده از قلمرو یک Road Gang می‌گذرد و محموله برای یک مشتری حساس بوده.", "Nomad مسیرها، نشانه‌های کاروان و رفتار Pack را بهتر از بقیه می‌فهمد.", "پول + بهبود رابطه Family یا اطلاعات درباره یک مسیر امن"),
        NomadHook("بدهی جاده", "یکی از اعضای Family از یک Pack دیگر کمک گرفته و حالا موعد پس‌دادن Favor رسیده.", "درخواست ظاهراً ساده است اما محموله مقصد قانونی روشنی ندارد.", "Nomad باید بین وفاداری Family و ریسک Crew تعادل ایجاد کند.", "تسویه Favor + contact جدید Nomad"),
        NomadHook("وسیله جامانده", "یک Family Vehicle خراب در منطقه خطرناک جامانده و باید قبل از غارت بازیابی شود.", "Vehicle Damage عادی است و Family تعمیر روزمره را بر عهده نمی‌گیرد؛ Scavها هم نزدیک شده‌اند.", "Vehicle Tech و رانندگی Nomad محور صحنه می‌شود.", "حفظ دارایی Family + قطعات/loot مشروع با تأیید GM"),
        NomadHook("مسیر بسته", "یک مسیر اصلی Nomad High Road به‌دلیل حملات پی‌درپی ناامن شده.", "عامل حملات شاید Road Gang نباشد و یک Corp کوچک از آشوب سود می‌برد.", "Nomad می‌تواند مسیر جایگزین، contact و الگوی تردد را پیدا کند.", "دسترسی بهتر به منطقه + اعتبار روایی نزد Pack"),
        NomadHook("عضو گمشده", "یک Cousin جوان بعد از یک delivery ناپدید شده.", "آخرین contact او با یک Fixer شهری بوده که ادعا می‌کند چیزی نمی‌داند.", "Family برای Nomad شخصی است؛ این Hook باید انتخاب‌های عاطفی و عملی ایجاد کند.", "رابطه Family + یک contact یا سرنخ کمپین"),
        NomadHook("بار داغ", "Family برای عبور یک محموله از شهر به Crew نیاز دارد.", "Lawmanها، یک gang و خریدار اصلی هر سه دنبال همان cargo هستند.", "Nomad مسئول مسیر، وسیله و تصمیم‌های لجستیکی است.", "پرداخت job + Favor Family، بدون دادن Vehicle اضافه خارج از Motorpool"),
        NomadHook("سوخت آخر", "یک کاروان Family در میانه Badlands سوخت و قطعه کم آورده است.", "نزدیک‌ترین منبع تحت کنترل یک گروه متخاصم است.", "Nomad باید مسیر، معامله یا بازیابی قطعه را مدیریت کند.", "Favor Family + سهمی از محموله"),
        NomadHook("مسافر ناشناس", "Family از Crew می‌خواهد یک مسافر را بی‌سروصدا به Night City برساند.", "مسافر هویت واقعی‌اش را پنهان کرده و یک Corp دنبالش است.", "Nomad درباره مسیر، ایست‌های امن و اعتماد به مسافر تصمیم می‌گیرد.", "پول + Contact جدید"),
        NomadHook("رقابت کاروان", "یک Pack رقیب مسیر تجاری Family را تصاحب کرده است.", "درگیری مستقیم می‌تواند جنگ بین Packها راه بیندازد.", "Nomad باید راه‌حل رانندگی، مذاکره یا اثبات خرابکاری پیدا کند.", "اعتبار نزد Family + مسیر تجاری"),
        NomadHook("SOS در شن", "یک پیام اضطراری قدیمی از عضو Family دوباره روی فرکانس ظاهر شده است.", "مختصات به منطقه‌ای متروک و احتمالاً تله اشاره می‌کند.", "Nomad کدها و پروتکل‌های Family را می‌شناسد و می‌تواند واقعی‌بودن پیام را بررسی کند.", "نجات عضو Family + سرنخ کمپین")
    )

}
