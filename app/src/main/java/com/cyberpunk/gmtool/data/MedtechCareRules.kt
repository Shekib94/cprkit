package com.cyberpunk.gmtool.data

/**
 * قواعد درمان Medtech (Core، بخش Trauma Team / Paramedic / Pharmaceuticals).
 *
 * چرا این فایل جدا است: قاعده‌ی اصلی پروژه این است که «هیچ عددی در UI ساخته
 * نشود». درمان واقعی بازیکن‌ها سه جا مصرف می‌شود (کادر Medtech در Role Helper،
 * تب BIO و بعداً هر جای دیگر) و باید همه‌جا یک DV، یک فرمول و یک منبع تاس
 * بدهد. پس همه‌ی محاسبات اینجاست و UI فقط صدا می‌زند و متن نتیجه را نشان می‌دهد.
 *
 * پایه‌ی قواعدی که اینجا کد شده:
 * • Stabilize: TECH + First Aid/Paramedic در برابر DV10 (Lightly) / DV13
 *   (Seriously Wounded) / DV15 (Mortally Wounded). موفقیت در DV15 بیمار را به
 *   ۱ HP و بیهوشی می‌برد؛ Critical Injuryها دست‌نخورده می‌مانند.
 * • Paramedic یک مهارت آموزش‌دیده است: کسی که آن را ندارد باید دوره‌ی آموزش
 *   ببیند (۲ هفته + ۶۰ IP) وگرنه فقط First Aid در دسترس اوست.
 * • Quick Fix (۱ دقیقه) اثر جراحت را موقتاً/برای همیشه کنار می‌زند؛
 *   Treatment (۴ ساعت) جراحت را کاملاً برمی‌دارد و روی خودِ درمانگر ممنوع است.
 * • Pharmaceuticalها (Speedheal/Antibiotic/Rapidetox/Stim/Surge) با
 *   TECH + Medical Tech ساخته می‌شوند: DV13، ۲۰۰eb مواد، ۱ ساعت، و تعداد
 *   dose در هر ساخت = مقدار Medical Tech.
 */
object MedtechCareRules {

    // ── پارامتریک: آموزش Paramedic ──
    /** هزینه‌ی یادگیری Paramedic: ۲ هفته آموزش + ۶۰ IP (Core). */
    const val PARAMEDIC_TRAINING_WEEKS = 2
    const val PARAMEDIC_IP_COST = 60

    /** Trauma Team Kit: بسته‌ی کامل درمانی که کیفیت تجهیزات را بالا می‌برد. */
    const val TRAUMA_TEAM_KIT_BONUS = 2
    const val TRAUMA_TEAM_KIT_EFFECT = "trauma_team_kit"

    // ─────────────────────────── پایدارسازی ───────────────────────────

    data class StabilizeTarget(val dv: Int, val labelFa: String)

    /** DV پایدارسازی از وضعیت زخم بیمار می‌آید، نه از خودِ بازیکن. */
    fun stabilizeTarget(hp: Int, maxHp: Int, isDead: Boolean): StabilizeTarget = when {
        isDead -> StabilizeTarget(17, "Dead — بازگرداندن مرده تصمیم GM است، در قواعد Stabilize نمی‌گنجد")
        GameRules.isMortallyWounded(hp) -> StabilizeTarget(15, "Mortally Wounded")
        GameRules.isSeriouslyWounded(hp, maxHp) -> StabilizeTarget(13, "Seriously Wounded")
        else -> StabilizeTarget(10, "Lightly Wounded")
    }

    fun paramedicLevel(c: Character): Int = CombatRules.skillLevel(c, "Paramedic")

    fun firstAidLevel(c: Character): Int = CombatRules.skillLevel(c, "First Aid")

    /** بهترین مهارت پزشک برای Stabilize و مقدارش. */
    fun bestStabilizeSkill(c: Character): Pair<String, Int> {
        val p = paramedicLevel(c)
        val f = firstAidLevel(c)
        return if (p >= f) "Paramedic" to p else "First Aid" to f
    }

    /** کسی که نه Paramedic دارد نه First Aid، پزشک صحنه نیست. */
    fun canStabilize(c: Character): Boolean = bestStabilizeSkill(c).second > 0

    /**
     * آیا این شخصیت باید دوره‌ی Paramedic ببیند؟
     * (چه از راه نقش Medtech و چه با ۶۰ IP و ۲ هفته آموزش)
     */
    fun needsParamedicTraining(c: Character): Boolean = paramedicLevel(c) == 0

    /** آیا حداقل مواد اولیه معاینه/خون‌بندی همراه بیمارستان را دارد؟ */
    fun hasTraumaTeamKit(c: Character): Boolean =
        c.combatEffects.containsKey(TRAUMA_TEAM_KIT_EFFECT) ||
            c.inventory.any { it.equipped && (it.name.contains("Medtech Bag", true) || it.name.contains("Trauma Team", true)) }

    data class StabilizeResult(
        val success: Boolean,
        val total: Int,
        val dv: Int,
        val skillName: String,
        val skillLevel: Int,
        val stat: Int,
        val die: Int,
        val bonus: Int,
        val penalty: Int,
        val who: String,
        val target: StabilizeTarget,
        val hpAfter: Int,
        /** کسی که Paramedic ندارد، First Aid می‌ریزد ولی GM باید دوره را یادآوری کند. */
        val needsTraining: Boolean
    ) {
        val detailFa: String
            get() = buildString {
                append("$who • TECH $stat + $skillName $skillLevel + تاس $die")
                if (bonus != 0) append(" + تجهیزات $bonus")
                if (penalty != 0) append(" + جریمه‌ی وضعیت $penalty")
                append(" = $total در برابر DV$dv")
                append(
                    if (success) {
                        if (target.dv == 15) " • بیمار به ۱ HP برگشت و ۱ دقیقه بیهوش است؛ Critical Injuryها باقی می‌مانند."
                        else " • بیمار پایدار شد (خون‌ریزی/روند مرگ متوقف)."
                    } else " • پایدارسازی نشد؛ بیمار همان وضعیت را دارد."
                )
            }
    }

    /**
     * پایدارسازی واقعی با تاس از [DiceSource].
     * توجه: در حالت تاس دستی، این تابع باید از نخ پس‌زمینه صدا زده شود
     * (الگوی `scope.launch(Dispatchers.Default)`) وگرنه DiceSource تصادفی می‌ریزد.
     */
    fun rollStabilize(patient: Character, healer: Character): StabilizeResult {
        val kit = hasTraumaTeamKit(healer)
        val (skillName, skill) = bestStabilizeSkill(healer)
        val stat = EquipmentUseRules.effectiveStat(healer, "TECH", healer.stats.tech)
        val die = CombatRules.rollD10()
        val penalty = CombatRules.allActionsPenalty(healer)
        val bonus = if (kit) TRAUMA_TEAM_KIT_BONUS else 0
        val total = stat + skill + die.totalDie + penalty + bonus
        val target = stabilizeTarget(patient.hp, patient.maxHp, patient.isDead)
        val success = !patient.isDead && total > target.dv
        val hpAfter = if (success && target.dv == 15) 1 else patient.hp.coerceAtLeast(if (patient.isDead) 0 else 1)
        return StabilizeResult(
            success = success, total = total, dv = target.dv, skillName = skillName, skillLevel = skill,
            stat = stat, die = die.totalDie, bonus = bonus, penalty = penalty,
            who = healer.handle.ifBlank { healer.name }, target = target, hpAfter = hpAfter,
            needsTraining = skillName == "First Aid" && paramedicLevel(healer) == 0
        )
    }

    // ───────────────────── Pharmaceuticalها ─────────────────────

    /** Medical Tech = Pharmaceuticals + Cryosystem Operation (برای ساخت دارو). */
    fun medicalTechLevel(c: Character): Int =
        ((c.roleAbilityPoints["pharma"] ?: 0) + (c.roleAbilityPoints["cryo"] ?: 0)).coerceIn(0, 10)

    /** داروهایی که این Medtech رسماً یاد گرفته است. */
    fun learnedPharmaceuticals(c: Character): List<String> =
        c.medtechPharmaceuticals.filter { name -> RoleAssistantData.corePharmaceuticals.any { it.name.equals(name, true) } }

    /** همه‌ی گزینه‌های دارویی قابل ساخت/تزریق (تزریق داروی یادنگرفته هشدار می‌گیرد). */
    val pharmaceuticalNames: List<String> get() = RoleAssistantData.corePharmaceuticals.map { it.name }

    fun pharmaceuticalInfo(name: String): RoleAssistantData.Pharmaceutical? =
        RoleAssistantData.corePharmaceuticals.firstOrNull { it.name.equals(name, true) }

    data class PharmaceuticalUse(
        val name: String,
        val success: Boolean,
        val roll: Int,
        val total: Int,
        val dv: Int,
        val doses: Int,
        val learned: Boolean,
        val hpDelta: Int,
        /** کلید اثر موقت که باید روی بیمار ثبت شود. */
        val addEffect: Pair<String, Int>? = null,
        /** اثر موقتی که باید پاک شود (Rapidetox). */
        val removeEffectKey: String? = null,
        val blockedFa: String? = null
    ) {
        val detailFa: String
            get() = buildString {
                append("ساخت/تزریق $name • TECH + Medical Tech + تاس $roll = $total در برابر DV$dv")
                when {
                    !success -> append(" • ناموفق؛ مواد هدر رفت (یک ساعت کار).")
                    blockedFa != null -> append(" • ساخت موفق بود ولی اثر روی بیمار نمی‌نشیند: $blockedFa")
                    hpDelta > 0 -> append(" • موفق • بیمار $hpDelta HP پس گرفت (اکنون روی برگه ثبت شد).")
                    removeEffectKey != null -> append(" • موفق • اثر موردنظر از بیمار پاک شد.")
                    else -> append(" • موفق • اثر دارو روی بیمار ثبت شد. هر ساخت $doses dose می‌دهد.")
                }
                if (success && !learned) append(" (این دارو در لیست Pharmaceuticalهای یادگرفته‌شده نبود؛ تزریق آزمایشی ثبت شد.)")
            }
    }

    /**
     * ساخت/تزریق دارو. DV ساخت دارو ۱۳ است و تعداد dose هر ساخت برابر
     * Medical Tech همین Medtech؛ اثر دارویی طبق جدول Core اعمال می‌شود.
     */
    fun rollPharmaceutical(patient: Character, medtech: Character, drugName: String): PharmaceuticalUse {
        val info = pharmaceuticalInfo(drugName)
        val learned = learnedPharmaceuticals(medtech).any { it.equals(drugName, true) }
        val die = CombatRules.rollD10()
        val stat = EquipmentUseRules.effectiveStat(medtech, "TECH", medtech.stats.tech)
        val tech = medicalTechLevel(medtech)
        val penalty = CombatRules.allActionsPenalty(medtech)
        val total = stat + tech + die.totalDie + penalty
        val dv = 13
        val success = total > dv
        val doses = tech.coerceAtLeast(1)

        fun result(hp: Int = 0, effect: Pair<String, Int>? = null, remove: String? = null, blocked: String? = null) =
            PharmaceuticalUse(drugName, success, die.totalDie, total, dv, doses, learned, hp, effect, remove, blocked)

        if (!success) return result()
        return when {
            drugName.equals("Speedheal", true) ->
                if (GameRules.isMortallyWounded(patient.hp))
                    result(blocked = "بیمار Mortally Wounded است و Speedheal روی او اثر ندارد.")
                else result(
                    hp = EquipmentUseRules.effectiveStat(patient, "BODY", patient.stats.body) +
                        EquipmentUseRules.effectiveStat(patient, "WILL", patient.stats.will)
                )
            drugName.equals("Antibiotic", true) -> result(effect = "antibiotic" to (7 * 24))
            drugName.equals("Stim", true) -> result(effect = "stim" to 1)
            drugName.equals("Surge", true) -> result(effect = "surge" to 24)
            drugName.equals("Rapidetox", true) -> result(remove = "drug_active")
            else -> result()
        }
    }

    // ───────────────────── بازیابی طبیعی ─────────────────────

    /** استراحت کامل: روزی BODY HP. بدون استراحت کامل پیشرفتی ثبت نمی‌شود. */
    fun naturalRecoveryPerDay(patient: Character): Int = patient.stats.body.coerceAtLeast(1)

    /**
     * اثر حرکتی که ریسک بازگشت آسیب دارد (Broken Ribs / Foreign Object).
     * GM آن را وقتی می‌دهد که بیمار بیش از ۴ متر/یارد حرکت کند.
     */
    fun activityDamageInjuries(patient: Character): List<String> =
        patient.criticalInjuries.filter {
            (it == "broken_ribs" || it == "foreign_object_body" || it == "foreign_object_head") &&
                CriticalInjuries.effectActive(patient, it)
        }

    // ───────────────────── متن راهنما ─────────────────────

    /**
     * خلاصه‌ی درمان برای GM — کوتاه و روشن.
     * چرا کوتاه: قبلاً هر بند یک پاراگراف فشرده بود و سر میز خواندنش سخت بود؛
     * حالا هر بند فقط همان تصمیمی است که GM باید بگیرد.
     */
    val quickGuide: List<Pair<String, String>> = listOf(
        "Stabilize — خون‌ریزی را بند بیاور" to
            "DV از وضعیت بیمار می‌آید: زخم سطحی ۱۰، زخم شدید ۱۳، در آستانه‌ی مرگ ۱۵. تاس: TECH + First Aid (یا Paramedic). موفقیت در DV15 بیمار را به ۱ HP برمی‌گرداند و بیهوش می‌شود.",
        "Quick Fix — یک دقیقه" to
            "یک دقیقه وقت می‌گیرد و اثر جراحت را موقتاً کنار می‌زند (در سه جراحت خاص، برای همیشه). بعد از آن، سر جایش برمی‌گردد مگر Treatment بخورد.",
        "Treatment — چهار ساعت" to
            "چهار ساعت کار و جراحت کامل از برگه پاک می‌شود. کسی که خودش زخمی است نمی‌تواند درمانگر باشد. سخت‌ترین جراحت‌ها Surgery با DV17 می‌خواهند.",
        "Paramedic — دوره‌ی مهارت" to
            "Paramedic یک مهارت آموزش‌دیده است؛ هرکس نداشته باشد (حتی Medtech) اول باید دوره‌اش را ببیند: ۲ هفته و ۶۰ IP.",
        "Pharmaceutical — ساخت دارو" to
            "TECH + Medical Tech در برابر DV13، ۲۰۰eb مواد، ۱ ساعت کار. تعداد دوز برابر سطح Medical Tech است و تزریق با Airhypo یک Action می‌گیرد.",
        "Recovery — استراحت" to
            "بعد از پایدارسازی، هر روز استراحت کامل BODY HP برمی‌گرداند. Diet و Exercise هفتگی کمک دوم است، ولی جراحت درمان‌نشده سر جایش می‌ماند.",
        "Cryo — خریدن زمان" to
            "برای انتقال بیمار یا خریدن وقت. مزایای Cryosystem Operation همان Medtech از خودش خوانده می‌شود."
    )
}
