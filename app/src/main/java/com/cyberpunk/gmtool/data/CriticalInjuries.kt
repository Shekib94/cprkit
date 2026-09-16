package com.cyberpunk.gmtool.data

/**
 * ۲۲ جراحت بحرانی Cyberpunk RED — ۱۱ جراحت بدن و ۱۱ جراحت سر.
 * ترجمه‌ی کامل و خلاصه‌نشده‌ی فارسی از جدول کتاب Core (ص. ۲۲۱).
 * روی یک تاس 2d6 مشخص می‌شود: ستون Roll نتیجه‌ی ۲ تا ۱۲ است.
 */
object CriticalInjuries {

    data class Injury(
        val key: String,        // کلید یکتا برای آیکن
        val enName: String,     // نام انگلیسی رسمی
        val faName: String,     // نام فارسی
        val body: Boolean,      // true = جراحت بدن، false = جراحت سر
        val roll: Int,          // نتیجه‌ی 2d6
        val effectFa: String,   // اثر جراحت (فارسی کامل)
        val quickFixFa: String, // ترمیم سریع (Quick Fix)
        val treatmentFa: String // درمان نهایی (Treatment)
    )

    private fun body(key: String, en: String, fa: String, roll: Int, effect: String, qf: String, tr: String) =
        Injury(key, en, fa, true, roll, effect, qf, tr)

    private fun head(key: String, en: String, fa: String, roll: Int, effect: String, qf: String, tr: String) =
        Injury(key, en, fa, false, roll, effect, qf, tr)

    val bodyInjuries = listOf(
        body("dismembered_arm", "Dismembered Arm", "از دست رفتن بازو", 2,
            "بازو کاملاً جدا می‌شود. هر وسیله‌ای که در دستِ آن بازو داری، فوراً می‌افتد. جریمه‌ی پایه‌ی تست مرگ (Base Death Save) یک واحد بالا می‌رود.",
            "ندارد (N/A)", "جراحی DV17"),
        body("dismembered_hand", "Dismembered Hand", "از دست رفتن دست", 3,
            "دست کاملاً جدا می‌شود. هر وسیله‌ای که در دستِ قطع‌شده داری، فوراً می‌افتد. جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "ندارد (N/A)", "جراحی DV17"),
        body("collapsed_lung", "Collapsed Lung", "فروپاشی ریه", 4,
            "به امتیاز MOVE یک ۲- می‌خوری (کمینه‌ی MOVE برابر ۱ است). جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "پارامدیک DV15", "جراحی DV15"),
        body("broken_ribs", "Broken Ribs", "شکستگی دنده", 5,
            "در پایان هر نوبتی که در آن پیاده بیش از ۴ متر/یارد حرکت کنی، این آسیبِ پاداشِ جراحت بحرانی دوباره مستقیم به HPات وارد می‌شود.",
            "پارامدیک DV13", "پارامدیک DV15 یا جراحی DV13"),
        body("broken_arm", "Broken Arm", "شکستگی بازو", 6,
            "بازوی شکسته قابل استفاده نیست. هر وسیله‌ای که در دستِ آن بازو داری، فوراً می‌افتد.",
            "پارامدیک DV13", "پارامدیک DV15 یا جراحی DV13"),
        body("foreign_object_body", "Foreign Object", "شیء خارجی (بدن)", 7,
            "در پایان هر نوبتی که در آن پیاده بیش از ۴ متر/یارد حرکت کنی، این آسیبِ پاداشِ جراحت بحرانی دوباره مستقیم به HPات وارد می‌شود.",
            "کمک‌های اولیه یا پارامدیک DV13", "ترمیم سریع اثر جراحت را برای همیشه برمی‌دارد (Quick Fix اثر را دائمی حذف می‌کند)"),
        body("broken_leg", "Broken Leg", "شکستگی پا", 8,
            "به امتیاز MOVE یک ۴- می‌خوری (کمینه‌ی MOVE برابر ۱ است).",
            "پارامدیک DV13", "پارامدیک DV15 یا جراحی DV13"),
        body("torn_muscle", "Torn Muscle", "پارگی ماهیچه", 9,
            "به همه‌ی حمله‌های غوغا (Melee) یک ۲- می‌خوری.",
            "کمک‌های اولیه یا پارامدیک DV13", "ترمیم سریع اثر جراحت را برای همیشه برمی‌دارد (Quick Fix اثر را دائمی حذف می‌کند)"),
        body("spinal_injury", "Spinal Injury", "آسیب نخاع", 10,
            "نوبت بعد نمی‌توانی اکشن (Action) بگیری، اما همچنان می‌توانی یک حرکت جابه‌جایی (Move Action) انجام دهی. جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "پارامدیک DV15", "جراحی DV15"),
        body("crushed_fingers", "Crushed Fingers", "له‌شدن انگشتان", 11,
            "به همه‌ی اکشن‌هایی که به آن دست نیاز دارند یک ۴- می‌خوری.",
            "پارامدیک DV13", "جراحی DV15"),
        body("dismembered_leg", "Dismembered Leg", "از دست رفتن پا", 12,
            "پا کاملاً جدا می‌شود. به امتیاز MOVE یک ۶- می‌خوری (کمینه‌ی MOVE برابر ۱ است) و دیگر نمی‌توانی جاخالی بدهی (داج). جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "ندارد (N/A)", "جراحی DV17")
    )

    val headInjuries = listOf(
        head("lost_eye", "Lost Eye", "از دست رفتن چشم", 2,
            "چشم کاملاً از دست می‌رود. به همه‌ی حمله‌های دوربرد (Ranged) و چک‌های ادراک (Perception) که به بینایی وابسته‌اند یک ۴- می‌خوری. جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "ندارد (N/A)", "جراحی DV17"),
        head("brain_injury", "Brain Injury", "آسیب مغزی", 3,
            "به همه‌ی اکشن‌ها یک ۲- می‌خوری. جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "ندارد (N/A)", "جراحی DV17"),
        head("damaged_eye", "Damaged Eye", "آسیب چشم", 4,
            "به همه‌ی حمله‌های دوربرد (Ranged) و چک‌های ادراک (Perception) که به بینایی وابسته‌اند یک ۲- می‌خوری.",
            "پارامدیک DV15", "جراحی DV13"),
        head("concussion", "Concussion", "ضربه‌مغزی", 5,
            "به همه‌ی اکشن‌ها یک ۲- می‌خوری.",
            "کمک‌های اولیه یا پارامدیک DV13", "ترمیم سریع اثر جراحت را برای همیشه برمی‌دارد (Quick Fix اثر را دائمی حذف می‌کند)"),
        head("broken_jaw", "Broken Jaw", "شکستگی فک", 6,
            "به همه‌ی اکشن‌هایی که به حرف‌زدن نیاز دارند یک ۴- می‌خوری.",
            "پارامدیک DV13", "پارامدیک یا جراحی DV13"),
        head("foreign_object_head", "Foreign Object", "شیء خارجی (سر)", 7,
            "در پایان هر نوبتی که در آن پیاده بیش از ۴ متر/یارد حرکت کنی، این آسیبِ پاداشِ جراحت بحرانی دوباره مستقیم به HPات وارد می‌شود.",
            "کمک‌های اولیه یا پارامدیک DV13", "ترمیم سریع اثر جراحت را برای همیشه برمی‌دارد (Quick Fix اثر را دائمی حذف می‌کند)"),
        head("whiplash", "Whiplash", "آسیب گردن (Whiplash)", 8,
            "جریمه‌ی پایه‌ی تست مرگ (Base Death Save) یک واحد بالا می‌رود.",
            "پارامدیک DV13", "پارامدیک یا جراحی DV13"),
        head("cracked_skull", "Cracked Skull", "شکستگی جمجمه", 9,
            "شلیک‌های هدف‌گرفته به سرِ تو، آسیبی که پس از کسر SP باقی می‌ماند را به‌جای ضریب ۲، با ضریب ۳ حساب می‌کنند. جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "پارامدیک DV15", "پارامدیک یا جراحی DV15"),
        head("damaged_ear", "Damaged Ear", "آسیب گوش", 10,
            "هر زمان در یک نوبت پیاده بیش از ۴ متر/یارد حرکت کنی، نوبت بعد نمی‌توانی حرکت جابه‌جایی (Move Action) بگیری. به‌علاوه به چک‌های ادراک شنوایی یک ۲- می‌خوری.",
            "پارامدیک DV13", "جراحی DV13"),
        head("crushed_windpipe", "Crushed Windpipe", "له‌شدن نای", 11,
            "نمی‌توانی حرف بزنی. جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "ندارد (N/A)", "جراحی DV15"),
        head("lost_ear", "Lost Ear", "از دست رفتن گوش", 12,
            "گوش کاملاً از دست می‌رود. هر زمان در یک نوبت پیاده بیش از ۴ متر/یارد حرکت کنی، نوبت بعد نمی‌توانی حرکت جابه‌جایی (Move Action) بگیری. به چک‌های ادراک شنوایی هم یک ۴- می‌خوری. جریمه‌ی پایه‌ی تست مرگ یک واحد بالا می‌رود.",
            "ندارد (N/A)", "جراحی DV17")
    )


    data class CareOption(val skill: String, val dv: Int)

    /** Structured Core care data used by Combat UI; avoids parsing the localized description strings. */
    fun quickFixOptions(key: String): List<CareOption> = when (key) {
        "collapsed_lung" -> listOf(CareOption("Paramedic", 15))
        "broken_ribs", "broken_arm", "broken_leg", "crushed_fingers", "whiplash", "broken_jaw", "damaged_ear" -> listOf(CareOption("Paramedic", 13))
        "foreign_object_body", "foreign_object_head", "torn_muscle", "concussion" -> listOf(CareOption("First Aid", 13), CareOption("Paramedic", 13))
        "spinal_injury", "damaged_eye", "cracked_skull" -> listOf(CareOption("Paramedic", 15))
        else -> emptyList()
    }

    fun treatmentOptions(key: String): List<CareOption> = when (key) {
        "dismembered_arm", "dismembered_hand", "dismembered_leg", "lost_eye", "brain_injury", "lost_ear" -> listOf(CareOption("Surgery", 17))
        "collapsed_lung", "spinal_injury", "crushed_fingers", "crushed_windpipe" -> listOf(CareOption("Surgery", 15))
        "cracked_skull" -> listOf(CareOption("Paramedic", 15), CareOption("Surgery", 15))
        "broken_ribs", "broken_arm", "broken_leg" -> listOf(CareOption("Paramedic", 15), CareOption("Surgery", 13))
        "damaged_eye", "damaged_ear" -> listOf(CareOption("Surgery", 13))
        "broken_jaw", "whiplash" -> listOf(CareOption("Paramedic", 13), CareOption("Surgery", 13))
        // These three plus Foreign Object are fully resolved by their successful Quick Fix.
        "foreign_object_body", "foreign_object_head", "torn_muscle", "concussion" -> emptyList()
        else -> emptyList()
    }

    fun quickFixIsPermanent(key: String): Boolean = key in setOf(
        "foreign_object_body", "foreign_object_head", "torn_muscle", "concussion"
    )

    /** Injury remains recorded, but a Quick Fix can suppress its mechanical effect. */
    fun effectActive(character: Character, key: String): Boolean =
        key in character.criticalInjuries &&
            !character.combatEffects.containsKey("quickfix_$key") &&
            !character.combatEffects.containsKey("critical_effect_removed_$key")

    val all: List<Injury> = headInjuries + bodyInjuries

    fun byKey(key: String): Injury? = all.firstOrNull { it.key == key }
}
