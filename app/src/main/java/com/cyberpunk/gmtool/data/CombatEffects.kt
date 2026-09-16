package com.cyberpunk.gmtool.data

/**
 * طبقه‌بندی اثرهای فعال روی یک کاراکتر.
 *
 * چرا این فایل: کارت ACTIVE COMBAT EFFECTS هر چیزی که داخل `combatEffects`
 * بود را خام و با کلید انگلیسی نشان می‌داد — از کلیدهای داخلی برنامه مثل
 * `MOVED THIS TURN 7` یا `USED MA IRON GRIP` تا اثرهای غیرنظامی مثل
 * `BACKUP EN ROUTE 3` و `QUICKFIX BROKEN LEG`. GM نمی‌توانست بفهمد کدام‌شان
 * واقعاً روی همین نبرد اثر می‌گذارد. حالا هر کلید یکی از این سه است:
 *
 * - [Kind.COMBAT]: با برچسب فارسی و توضیح مکانیکی دقیق نشان داده می‌شود.
 * - [Kind.NON_COMBAT]: از نبرد بیرون است (دارو، درمان، پشتیبان در راه) و فقط
 *   شمرده می‌شود، چون در تب‌های خودش دنبال می‌شود.
 * - [Kind.INTERNAL]: شمارنده‌ی داخلی برنامه است (چه کسی این نوبت حرکت کرده،
 *   کدام تکنیک Martial Arts مصرف شده). نمایش‌دادنش فقط سر میز را شلوغ می‌کند.
 */
object CombatEffects {

    enum class Kind { COMBAT, NON_COMBAT, INTERNAL }

    /** برچسب فارسی + اثر مکانیکی دقیق برای کارت نبرد. */
    data class Entry(
        val kind: Kind,
        val labelFa: String = "",
        val detailFa: String = ""
    )

    private fun combat(label: String, detail: String) = Entry(Kind.COMBAT, label, detail)
    private val internal = Entry(Kind.INTERNAL)
    private val nonCombat = Entry(Kind.NON_COMBAT)

    /** شمارنده‌های داخلی — هیچ‌وقت به GM نشان داده نمی‌شوند. */
    private val internalPrefixes = listOf(
        "moved_this_turn_", "run_used_this_turn", "turn_hit_", "turn_melee_count_",
        "used_ma_", "judo_melee_hit_since_turn", "judo_dodged_from_",
        "combat_shield_hp_", "combat_shield_kind_", "combat_shield_damage_",
        "in_vehicle_", "vehicle_running_", "vehicle_lost_control_", "vehicle_seat_",
        "death_save_done", "critical_effect_removed_", "dropped_weapon_",
        "in_cover", "held_action", "choke_chain_"
    )

    /** اثرهای غیرنظامی: دارو، درمان، پشتیبان لاومن، پرونده‌های Medtech. */
    private val nonCombatPrefixes = listOf(
        "quickfix_", "backup_en_route_", "drug_active", "antibiotic", "stim", "surge",
        "rapidetox", "speedheal", "medical_", "medtech_", "paramedic_", "recovery_"
    )

    fun kind(key: String): Kind = entry(key).kind

    fun entry(key: String): Entry = when {
        key == "prone" -> combat(
            "زمین‌افتاده (Prone)",
            "حملهٔ درگیری به او +2 می‌گیرد و در دفاع فقط Evasion دارد؛ برای بلندشدن یک Action لازم است (Recovery DV13 تا هم‌زمان بایستد)."
        )
        key == "unconscious" -> combat(
            "بی‌هوش (Unconscious)",
            "هیچ Action و Move ندارد و در دفاع کاری نمی‌تواند بکند تا بیدار شود."
        )
        key == "on_fire_2" -> combat(
            "در آتش — ۲ آسیب",
            "پایان هر Turn ۲ آسیب مستقیم به HP می‌خورد (SP اثری ندارد)؛ خاموش‌کردن یک Action می‌گیرد."
        )
        key == "on_fire_4" -> combat(
            "در آتش — ۴ آسیب",
            "پایان هر Turn ۴ آسیب مستقیم به HP می‌خورد (SP اثری ندارد)؛ خاموش‌کردن یک Action می‌گیرد."
        )
        key == "suppressed" -> combat(
            "زیر آتش سرکوب (Suppressed)",
            "نتیجهٔ Suppressive Fire: در نوبتِ خودش فرصت پاسخ‌دادن ندارد؛ اگر Cover نگیرد، در نوبت بعد مجبور است."
        )
        key == "must_seek_cover_this_turn" -> combat(
            "اجبار به Cover در همین Turn",
            "این Turn فقط می‌تواند به Cover برود یا از خط آتش بیرون بیاید؛ Action عادی ندارد."
        )
        key == "no_action_this_turn" -> combat(
            "بدون Action در این Turn",
            "مثل Spinal Injury: هیچ Action نمی‌گیرد (Move سرجای خودش است)."
        )
        key == "no_move_this_turn" -> combat(
            "بدون Move در این Turn",
            "فقط درجا می‌تواند Action بزند؛ حرکت ندارد."
        )
        key == "no_action_next_turn" -> combat(
            "بدون Action در Turn بعد",
            "جریمهٔ پایان‌نوبتی است؛ در Turn بعدی خودکار اعمال می‌شود."
        )
        key == "no_move_next_turn" -> combat(
            "بدون Move در Turn بعد",
            "جریمهٔ پایان‌نوبتی است؛ در Turn بعدی خودکار اعمال می‌شود."
        )
        key == "stabilized" -> combat(
            "پایدارشده (Stabilized)",
            "مرگ متوقف شده ولی وضعیت بحرانی باقی است؛ باید درمان کامل (Treatment) بگیرد."
        )
        key == "damaged_eye" -> combat(
            "آسیب چشم",
            "‏-2 به همهٔ حمله‌های دوربرد و چک‌های Perception وابسته به بینایی."
        )
        key == "damaged_ear" -> combat(
            "آسیب گوش",
            "هر نوبتی که پیاده بیش از ۴ متر/یارد حرکت کند، نوبت بعد نمی‌تواند جابه‌جایی تازه انجام دهد."
        )
        key.startsWith("grappled_by_") -> combat(
            "در گِرَپل (گرفته‌شده توسط ${who(key, "grappled_by_")})",
            "‏-2 به همهٔ Actionها؛ برای رهاکردن گرپل یک Action لازم است (فرار از گرپل)."
        )
        key.startsWith("grappling_") -> combat(
            "گِرَپل زده روی ${who(key, "grappling_")}",
            "‏-2 به همهٔ Actionها؛ همین‌جا Choke و Throw و Human Shield معنا پیدا می‌کنند."
        )
        key.startsWith("iron_grip_by_") -> combat(
            "Iron Grip (Aikido) از طرف ${who(key, "iron_grip_by_")}",
            "تا وقتی گرپل برقرار است حملهٔ Ranged ممنوع است و فرار از گرپل -2 می‌خورد."
        )
        key.startsWith("human_shield_") -> combat(
            "Human Shield (سپر انسانی: ${who(key, "human_shield_")})",
            "شلیک به این کاراکتر اول به بدن سپر می‌خورد، نه به خودش."
        )
        key == "judo_counter_ready" -> combat(
            "Judo — Counter Throw آماده",
            "دشمنِ درگیر در این Turn Dodge زده؛ می‌توانی به‌جای حمله Counter Throw بزنی."
        )
        internalPrefixes.any { key.startsWith(it) } -> internal
        nonCombatPrefixes.any { key.startsWith(it) } -> nonCombat
        else -> nonCombat
    }

    /** شناسهٔ عددی انتهای کلید (مثل نام یا id هدف) برای متن فارسی. */
    private fun who(key: String, prefix: String): String = key.removePrefix(prefix).replace('_', ' ')

    /**
     * آیا GM می‌تواند این اثر را دستی پاک کند؟
     *
     * فقط وضعیت‌های سبک و مشروط؛ چیزهایی مثل گِرَپل یا سپر باید از دکمهٔ خودشان
     * تمام شوند تا state نبرد با واقعیت نخواند.
     */
    fun canClearManually(key: String): Boolean = key in setOf(
        "prone", "suppressed", "must_seek_cover_this_turn",
        "no_action_this_turn", "no_move_this_turn",
        "no_action_next_turn", "no_move_next_turn"
    )

    /** اثرهای رزمی یک کاراکتر، مرتب‌شده با ثابت‌ها اول. */
    fun combatEntries(effects: Map<String, Int>): List<Pair<String, Int>> =
        effects.entries
            .filter { entry(it.key).kind == Kind.COMBAT }
            .sortedBy { it.key }
            .map { it.key to it.value }

    fun hiddenNonCombatCount(effects: Map<String, Int>): Int =
        effects.keys.count { entry(it).kind == Kind.NON_COMBAT }
}
