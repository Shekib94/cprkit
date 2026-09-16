package com.cyberpunk.gmtool.data

object LifepathGenerator {

    // --- جدول ۱: اصالت فرهنگی ---
    fun rollCulturalOrigins(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "آمریکای شمالی (زبان مادری: ${listOf("چینی", "کری", "کریول", "انگلیسی", "فرانسوی", "ناواهو", "اسپانیایی").random()})"
            2 -> "آمریکای مرکزی/جنوبی (زبان مادری: ${listOf("کریول", "انگلیسی", "آلمانی", "گوارانی", "مایایی", "پرتغالی", "کچوا", "اسپانیایی").random()})"
            3 -> "اروپای غربی (زبان مادری: ${listOf("هلندی", "انگلیسی", "فرانسوی", "آلمانی", "ایتالیایی", "نروژی", "پرتغالی", "اسپانیایی").random()})"
            4 -> "اروپای شرقی (زبان مادری: ${listOf("انگلیسی", "فنلاندی", "لهستانی", "رومانیایی", "روسی", "اوکراینی").random()})"
            5 -> "خاورمیانه و شمال آفریقا (زبان مادری: ${listOf("عربی", "بربر", "انگلیسی", "فارسی", "فرانسوی", "عبری", "ترکی").random()})"
            6 -> "آفریقای زیرِ صحرا (زبان مادری: ${listOf("عربی", "انگلیسی", "فرانسوی", "هوسا", "لینگالا", "اورومو", "پرتغالی", "سواحیلی", "توی", "یوروبا").random()})"
            7 -> "آسیای جنوبی (زبان مادری: ${listOf("بنگالی", "دری", "انگلیسی", "هندی", "نپالی", "سینهالی", "تامیلی", "اردو").random()})"
            8 -> "آسیای جنوب شرقی (زبان مادری: ${listOf("عربی", "برمه‌ای", "انگلیسی", "فیلیپینی", "هندی", "اندونزیایی", "خمر", "مالایی", "ویتنامی").random()})"
            9 -> "آسیای شرقی (زبان مادری: ${listOf("چینی کانتونی", "انگلیسی", "ژاپنی", "کره‌ای", "چینی ماندارین", "مغولی").random()})"
            10 -> "اقیانوسیه و جزایر پاسیفیک (زبان مادری: ${listOf("انگلیسی", "فرانسوی", "هاوایی", "مائوری", "پاما-نیونگان", "تاهیتی").random()})"
            else -> "نامشخص"
        }
    }

    // --- جدول ۲: ویژگی شخصیتی ---
    fun rollPersonality(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "خجالتی و تودار (Shy and secretive)"
            2 -> "سرکش، جامعه‌گریز و خشن (Rebellious, antisocial, violent)"
            3 -> "مغرور، متکبر و دست‌نیافتنی (Arrogant, proud, aloof)"
            4 -> "دمدمی‌مزاج، شتاب‌زده و کله‌خراب (Moody, rash, headstrong)"
            5 -> "وسواسی، بهانه‌گیر و مضطرب (Picky, fussy, nervous)"
            6 -> "استوار، متین و جدی (Stable and serious)"
            7 -> "سرخوش، سبک‌سر و حواس‌پرت (Silly and fluff-headed)"
            8 -> "موذی، مکار و فریبکار (Sneaky and deceptive)"
            9 -> "اهل تفکر، منطقی و بی‌احساس (Intellectual and detached)"
            10 -> "خون‌گرم، صمیمی و اجتماعی (Friendly and outgoing)"
            else -> "نامشخص"
        }
    }

    // --- جدول ۳: استایل لباس ---
    fun rollClothingStyle(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "شیک و ساده (استاندارد، رنگارنگ، ماژولار)"
            2 -> "راحتی و ورزشی (راحت، چابک، اسپرت)"
            3 -> "زرق‌وبرق شهری (خیابانی، جلب‌توجه‌کننده، تکنولوژیک)"
            4 -> "رسمیِ کاری (نشان‌دهنده قدرت و ابهت)"
            5 -> "مد سطح بالا (اختصاصی، طراح‌دوز، لوکس)"
            6 -> "بوهمین (محلی، رترو، آزاد و رها)"
            7 -> "ژنده‌پوش (بی‌خانمان، پاره‌پوره، ولگرد)"
            8 -> "رنگ‌های گنگی (خطرناک، خشن، شورشی)"
            9 -> "چرم‌های نومد (وسترن، زمخت، قبیله‌ای)"
            10 -> "پاپ آسیایی (روشن، لباس‌نمایشی، جوان‌پسند)"
            else -> "نامشخص"
        }
    }

    // --- جدول ۴: مدل مو ---
    fun rollHairstyle(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "موهاک (Mohawk)"
            2 -> "بلند و ژولیده (Long and ratty)"
            3 -> "کوتاه و سیخ‌سیخی (Short and spiked)"
            4 -> "وحشی و درهم‌ریخته (Wild and all over)"
            5 -> "طاس (Bald)"
            6 -> "راه‌راه رنگ‌آمیزی‌شده (Striped)"
            7 -> "رنگ‌های جیغ و وحشی (Wild colors)"
            8 -> "مرتب و کوتاه (Neat and short)"
            9 -> "کوتاه و فر (Short and curly)"
            10 -> "بلند و لخت (Long and straight)"
            else -> "نامشخص"
        }
    }

    // --- جدول ۵: آیتم همیشگی ---
    fun rollAffectation(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "خالکوبی (Tattoos)"
            2 -> "عینک آینه‌ای (Mirrorshades)"
            3 -> "زخم‌های آیینی (Ritual scars)"
            4 -> "دستکش‌های میخ‌دار (Spiked gloves)"
            5 -> "حلقه‌ی بینی (Nose rings)"
            6 -> "پیرسینگ زبان یا سایر اعضا (Tongue/piercings)"
            7 -> "ایمپلنت‌های عجیب ناخن (Fingernail implants)"
            8 -> "چکمه یا پاشنه‌های میخ‌دار (Spiked boots)"
            9 -> "دستکش‌های بدون انگشت (Fingerless gloves)"
            10 -> "لنزهای تماسی عجیب (Strange contacts)"
            else -> "نامشخص"
        }
    }

    // --- جدول ۶: باارزش‌ترین چیز ---
    fun rollValueMost(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "پول (Money)"
            2 -> "شرافت (Honor)"
            3 -> "قول و قرارت (Your word)"
            4 -> "صداقت (Honesty)"
            5 -> "دانش (Knowledge)"
            6 -> "انتقام (Vengeance)"
            7 -> "عشق (Love)"
            8 -> "قدرت (Power)"
            9 -> "خانواده (Family)"
            10 -> "دوستی (Friendship)"
            else -> "نامشخص"
        }
    }

    // --- جدول ۷: احساست به آدم‌ها ---
    fun rollFeelingsAboutPeople(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1, 2 -> "خنثی هستم. (I stay neutral.)"
            3 -> "تقریباً از همه خوشم می‌آید. (I like almost everyone.)"
            4 -> "تقریباً از همه متنفرم. (I hate almost everyone.)"
            5 -> "آدم‌ها ابزارند. استفاده کن و دور بینداز. (People are tools.)"
            6 -> "هر انسانی یک فرد ارزشمند است. (Every person is valuable.)"
            7 -> "آدم‌ها موانعی هستند که باید نابود شوند. (People are obstacles.)"
            8 -> "آدم‌ها غیرقابل اعتمادند. تکیه نکن. (People are untrustworthy.)"
            9 -> "همه‌شان را نابود کن و بگذار سوسک‌ها دنیا را بگیرند."
            10 -> "آدم‌ها فوق‌العاده‌اند! (People are wonderful!)"
            else -> "نامشخص"
        }
    }

    // --- جدول ۸: مهم‌ترین شخص زندگی ---
    fun rollValuedPerson(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "یکی از والدین (A parent)"
            2 -> "برادر یا خواهر (A brother or sister)"
            3 -> "معشوقه (A lover)"
            4 -> "یک دوست (A friend)"
            5 -> "خودت (Yourself)"
            6 -> "یک حیوان خانگی (A pet)"
            7 -> "یک معلم یا مرشد (A teacher/mentor)"
            8 -> "یک چهره‌ی عمومی (A public figure)"
            9 -> "یک قهرمان شخصی (A personal hero)"
            10 -> "هیچ‌کس (No one)"
            else -> "نامشخص"
        }
    }

    // --- جدول ۹: باارزش‌ترین دارایی ---
    fun rollValuedPossession(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "یک سلاح (A weapon)"
            2 -> "یک ابزار (A tool)"
            3 -> "یک تکه لباس (A piece of clothing)"
            4 -> "یک عکس (A photograph)"
            5 -> "یک کتاب یا دفتر خاطرات (A book or diary)"
            6 -> "یک فایل ضبط‌شده / نوار (A recording)"
            7 -> "یک ساز موسیقی (A musical instrument)"
            8 -> "یک قطعه جواهر (A piece of jewelry)"
            9 -> "یک اسباب‌بازی (A toy)"
            10 -> "یک نامه (A letter)"
            else -> "نامشخص"
        }
    }

    // --- جدول ۱۰: پیشینه اصلی خانواده ---
    fun rollFamilyBackground(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "مدیران ارشد شرکتی (Corporate Execs): ثروتمند و قدرتمند با خانه‌های لوکس."
            2 -> "مدیران میانی شرکتی (Corporate Managers): مرفه با خانه‌های بزرگ."
            3 -> "تکنسین‌های شرکتی (Corporate Techs): طبقه متوسط با خانه‌های راحت."
            4 -> "قبیله نومد (Nomad Pack): بزرگ‌شده در تریلرها و جاده‌ها."
            5 -> "گنگسترها (Ganger Family): خانه‌ای خشن و وحشیانه. گنگ به شما کشتن آموخت."
            6 -> "ساکنان منطقه مبارزه (Combat Zoners): در ساختمانی مخروبه اما محافظت‌شده."
            7 -> "بی‌خانمان شهری (Urban Homeless): زندگی در ماشین‌ها یا زباله‌دان‌ها."
            8 -> "موش‌های مگاستراکچر (Warren Rats): بزرگ‌شده در سازه‌های عظیم پس از جنگ."
            9 -> "بازپس‌گیرندگان (Reclaimers): زندگی پیشگامانه در شهرهای متروکه."
            10 -> "اج‌رانرها (Edgerunners): خانه‌تان مدام بر اساس «شغل» والدینتان عوض می‌شد."
            else -> "نامشخص"
        }
    }

    // --- جدول ۱۱: محیط کودکی ---
    fun rollChildhoodEnvironment(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "بزرگ‌شده در خیابان (The Street)، بدون هیچ سرپرستی."
            2 -> "منطقه امن شرکتی (Corp Zone)، جداشده از بقیه شهر."
            3 -> "قبیله نومد (Nomad pack)، همیشه در حال کوچ."
            4 -> "قبیله نومد ترابری (ریشه در کشتی‌ها یا کاروان‌ها)."
            5 -> "محله‌ای رو به زوال، در حال جنگ با بوسترها برای بقا."
            6 -> "قلب منطقه مبارزه (Combat Zone)، زندگی در مخروبه."
            7 -> "مگاستراکچر تحت کنترل یک شرکت یا خودِ شهر."
            8 -> "ویرانه‌های یک شهر متروکه که توسط بازپس‌گیرندگان اداره می‌شود."
            9 -> "ملت‌های شناور (Drift Nation)، شهری روی آب."
            10 -> "ستاره‌خراش لوکس شرکتی، در اوج آسمان."
            else -> "نامشخص"
        }
    }

    // --- جدول ۱۲: بحران خانوادگی ---
    fun rollFamilyCrisis(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "خانواده‌تان همه چیز را به خاطر یک خیانت از دست داد."
            2 -> "خانواده‌تان همه چیز را به خاطر مدیریت افتضاح از دست داد."
            3 -> "خانواده‌تان از خانه یا شرکت اصلی خود تبعید شد."
            4 -> "تمام اعضای خانواده زندانی هستند و فقط شما فرار کردید."
            5 -> "خانواده‌تان ناپدید شدند. شما تنها بازمانده‌اید."
            6 -> "خانواده‌تان قتل‌عام شدند و شما تنها بازمانده‌اید."
            7 -> "خانواده‌تان درگیر یک توطئه یا مافیا است."
            8 -> "خانواده‌تان به خاطر فقر و بدشانسی از هم پاشید."
            9 -> "خانواده‌تان گرفتار یک کینه‌ی خونین و ارثی است."
            10 -> "شما وارث یک بدهی سنگین خانوادگی هستید."
            else -> "نامشخص"
        }
    }

    // --- جدول ۱۳: دوستان ---
    fun rollFriends(): String {
        val count = maxOf(0, DiceSource.rollOne(10, "Lifepath — جدول d10") - 7)
        if (count == 0) return "بدون دوست (None)"

        val friendsList = mutableListOf<String>()
        for (i in 1..count) {
            val relation = when (DiceSource.rollOne(10, "Lifepath — جدول d10")) {
                1 -> "مثل یک برادر/خواهر بزرگتر"
                2 -> "مثل یک برادر/خواهر کوچکتر"
                3 -> "یک معلم یا مرشد"
                4 -> "یک شریک یا همکار"
                5 -> "یک معشوقه سابق"
                6 -> "یک دشمن قدیمی"
                7 -> "مثل یک پدر/مادر"
                8 -> "دوست دوران کودکی"
                9 -> "کسی که از خیابان می‌شناسی"
                10 -> "کسی با هدف یا علاقه مشترک"
                else -> "نامشخص"
            }
            friendsList.add("$i. $relation")
        }
        return friendsList.joinToString("\n")
    }

    // --- جدول ۱۴: دشمنان + انتقام شیرین ---
    fun rollEnemies(): String {
        val count = maxOf(0, DiceSource.rollOne(10, "Lifepath — جدول d10") - 7)
        if (count == 0) return "بدون دشمن (None)"

        val enemiesList = mutableListOf<String>()
        for (i in 1..count) {
            val who = when (DiceSource.rollOne(10, "Lifepath — جدول d10")) {
                1 -> "دوست سابق"
                2 -> "معشوقه سابق"
                3 -> "فامیل دورافتاده"
                4 -> "دشمن دوران کودکی"
                5 -> "کسی که برایت کار می‌کرد"
                6 -> "کسی که برایش کار می‌کردی"
                7 -> "شریک یا همکار"
                8 -> "مدیر ارشد شرکتی"
                9 -> "مقام دولتی"
                10 -> "عضو بوسترگنگ"
                else -> "نامشخص"
            }
            val cause = when (DiceSource.rollOne(10, "Lifepath — جدول d10")) {
                1 -> "باعث آبروریزی یا از دست دادن مقامش شدی."
                2 -> "باعث شدی عشق یا فامیلش را از دست بدهد."
                3 -> "یک تحقیر عمومی بزرگ برایش رقم زدی."
                4 -> "به او اتهام بزدلی زدی."
                5 -> "به او خیانت کردی یا رهایش کردی."
                6 -> "پیشنهاد کار یا پیشنهاد عاشقانه‌اش را رد کردی."
                7 -> "فقط از هم متنفرید."
                8 -> "یکی از شما رقیب عشقیِ دیگری بود."
                9 -> "یکی از شما رقیب کاریِ دیگری بود."
                10 -> "برایش پاپوش دوختی تا دستگیر شود."
                else -> "نامشخص"
            }
            val backup = when (DiceSource.rollOne(10, "Lifepath — جدول d10")) {
                1, 2 -> "فقط خودش."
                3 -> "خودش به همراه یک دوست صمیمی."
                4 -> "خودش و چند نفر از دوستانش."
                5 -> "خودش و گروهی از رفقایش."
                6 -> "یک گنگ کامل (حداقل ۶ نفر)."
                7 -> "پلیس محلی یا ماموران قانون."
                8 -> "یک رئیس گنگ قدرتمند یا یک شرکت کوچک."
                9 -> "یک شرکت بزرگ و قدرتمند."
                10 -> "کل شهر یا یک آژانس دولتی بزرگ."
                else -> "نامشخص"
            }
            val revenge = when (DiceSource.rollOne(10, "Lifepath — جدول d10")) {
                1, 2 -> "از این آشغال دوری می‌کند."
                3, 4 -> "با خشم قاتلانه حمله می‌کند!"
                5, 6 -> "به‌طور غیرمستقیم از پشت خنجر می‌زند."
                7, 8 -> "به‌صورت کلامی حمله می‌کند."
                9 -> "پاپوش می‌دوزد تا او را متهم کند."
                10 -> "کمر به قتل یا نقص عضو طرف می‌بندد."
                else -> "نامشخص"
            }
            enemiesList.add("دشمن $i: $who\n 🔹 دلیل: $cause\n ⚠️ قدرت: $backup\n 🩸 واکنش: $revenge")
        }
        return enemiesList.joinToString("\n\n")
    }

    // --- جدول ۱۵: عشق‌های نافرجام ---
    fun rollTragicLoveAffairs(): String {
        val count = maxOf(0, DiceSource.rollOne(10, "Lifepath — جدول d10") - 7)
        if (count == 0) return "بدون عشق نافرجام (None)"

        val affairsList = mutableListOf<String>()
        for (i in 1..count) {
            val fate = when (DiceSource.rollOne(10, "Lifepath — جدول d10")) {
                1 -> "در یک حادثه کشته شد."
                2 -> "به طرز مرموزی ناپدید شد."
                3 -> "رابطه‌تان به جایی نرسید و تمام شد."
                4 -> "یک هدف شخصی بین شما فاصله انداخت."
                5 -> "ربوده شد."
                6 -> "دیوانه شد یا به سایبرسایکوزیس مبتلا شد."
                7 -> "خودکشی کرد."
                8 -> "در یک درگیری خیابانی کشته شد."
                9 -> "یک رقیب تو را کنار زد و او را دزدید."
                10 -> "زندانی یا تبعید شده است."
                else -> "نامشخص"
            }
            affairsList.add("عشق $i: $fate")
        }
        return affairsList.joinToString(separator = "\n")
    }

    // --- جدول ۱۶: اهداف زندگی ---
    fun rollLifeGoals(): String {
        val roll = DiceSource.rollOne(10, "Lifepath — جدول d10")
        return when (roll) {
            1 -> "پاک کردن یک شهرت بد و لکه‌دار."
            2 -> "به دست آوردن قدرت و کنترل."
            3 -> "فرار از خیابان (The Street) به هر قیمتی که شده."
            4 -> "تحمیل درد و رنج به هر کسی که سر راهت قرار بگیرد."
            5 -> "پشت سر گذاشتن گذشته و تلاش برای فراموش کردن آن."
            6 -> "شکار مقصران زندگی فلاکت‌بارت و تقاص گرفتن."
            7 -> "پس گرفتن چیزی که حق مسلم توست."
            8 -> "نجات دادن افراد مهم گذشته‌ات."
            9 -> "رسیدن به شهرت و شناخته شدن."
            10 -> "تبدیل شدن به کسی که از او می‌ترسند و احترام می‌گذارند."
            else -> "نامشخص"
        }
    }

    // --- ساخت مسیر زندگی (نهایی) ---
    fun generateFullLifepath(): LifepathData {
        return LifepathData(
            culturalOrigins = rollCulturalOrigins(),
            personality = rollPersonality(),
            clothingStyle = rollClothingStyle(),
            hairstyle = rollHairstyle(),
            affectation = rollAffectation(),
            valueMost = rollValueMost(),
            feelingsAboutPeople = rollFeelingsAboutPeople(),
            valuedPerson = rollValuedPerson(),
            valuedPossession = rollValuedPossession(),
            familyBackground = rollFamilyBackground(),
            childhoodEnvironment = rollChildhoodEnvironment(),
            familyCrisis = rollFamilyCrisis(),
            friends = rollFriends(),
            enemies = rollEnemies(),
            tragicLoveAffairs = rollTragicLoveAffairs(),
            lifeGoals = rollLifeGoals()
        )
    }
}