package com.cyberpunk.gmtool.data

import kotlin.math.ceil
import kotlin.random.Random // allow-raw-random: پارامتر تزریق Random? فقط برای تست قطعیت قواعد؛ تولید تاس بازی صرفاً از DiceSource

data class CheckRoll(
    val first: Int,
    val extra: Int? = null,
    val totalDie: Int,
    val criticalSuccess: Boolean = false,
    val criticalFailure: Boolean = false
)

data class DamageRoll(
    val dice: List<Int>,
    val total: Int,
    val critical: Boolean
)

data class DamageResolution(
    val rawDamage: Int,
    val armorUsed: Int,
    val hpDamage: Int,
    val armorAblated: Int,
    val criticalBonus: Int,
    val injuryKey: String? = null
)

object CombatRules {
    /**
     * چک d10 استاندارد با Critical.
     *
     * از [DiceSource] رد می‌شود تا تنظیم «تاس خودکار» واقعاً کار کند؛ در حالت
     * دستی، GM عدد تاس فیزیکی را وارد می‌کند. پارامتر [random] فقط برای تست
     * باقی مانده و در بازی استفاده نمی‌شود.
     */
    fun rollD10(random: Random? = null, label: String = ""): CheckRoll {
        fun d10(tag: String): Int =
            random?.nextInt(1, 11) ?: DiceSource.rollOne(10, tag)
        val first = d10(label.ifBlank { "چک d10" })
        return when (first) {
            10 -> {
                val extra = d10("Critical Success — تاس اضافه")
                CheckRoll(first, extra, first + extra, criticalSuccess = true)
            }
            1 -> {
                val extra = d10("Critical Failure — تاس اضافه")
                CheckRoll(first, extra, first - extra, criticalFailure = true)
            }
            else -> CheckRoll(first, null, first)
        }
    }

    /**
     * پاداش‌های Initiative یک کاراکتر.
     *
     * Core: Initiative = 1d10 + REF. سرعت‌افزار به آن اضافه می‌شود (Kerenzikov
     * همیشه +۲ و Sandevistan در ۶۰ ثانیه‌ی فعال +۳)، توانایی Solo
     * (`initiativeReaction`) هم اضافه می‌شود و جریمه‌ی زره کم می‌شود.
     *
     * چرا یک‌جا و نه دو جا: قبلاً همین فرمول دو نسخه داشت — یکی در `CombatTab`
     * برای دکمه‌ی «ROLL SELF» که Sandevistan را حساب می‌کرد و یکی در ViewModel
     * برای «ROLL ALL» که نمی‌کرد. یعنی سرعت‌افزار فعال بسته به اینکه کدام دکمه
     * زده شود، دو عدد مختلف می‌داد.
     */
    data class InitiativeBonus(
        val ref: Int,
        val kerenzikov: Int,
        val sandevistan: Int,
        val solo: Int,
        val armorPenalty: Int
    ) {
        /** همان عددی که روی صفحه «Speedware» نامیده می‌شود. */
        val speedware: Int get() = kerenzikov + sandevistan
    }

    data class InitiativeRoll(val dice: CheckRoll, val bonus: InitiativeBonus) {
        val total: Int get() = dice.totalDie + bonus.ref + bonus.speedware + bonus.solo + bonus.armorPenalty

        /** برچسب تاس؛ روی Critical مقدار تاس اضافه هم دیده می‌شود (`10+4`). */
        val die: String get() = when {
            dice.criticalSuccess -> "10+${dice.extra}"
            dice.criticalFailure -> "1-${dice.extra}"
            else -> dice.first.toString()
        }
    }

    /**
     * پاداش Initiative؛ [sandevistanActiveUntil] زمان پایان اثر Sandevistan است
     * (۰ یعنی فعال نیست) و از وضعیت زمان‌اجرای همان کاراکتر می‌آید.
     */
    fun initiativeBonus(
        character: Character,
        sandevistanActiveUntil: Long = 0L,
        now: Long = System.currentTimeMillis()
    ): InitiativeBonus = InitiativeBonus(
        ref = EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref),
        kerenzikov = if (character.inventory.any { it.equipped && it.name.contains("Kerenzikov", true) }) 2 else 0,
        sandevistan = if (sandevistanActiveUntil > now) 3 else 0,
        solo = character.roleAbilityPoints["initiativeReaction"] ?: 0,
        armorPenalty = GameRules.armorPenalty(character.inventory)
    )

    /** یک رول کامل Initiative (تاس + پاداش‌ها) — تنها مسیر محاسبه در برنامه. */
    fun rollInitiative(
        character: Character,
        sandevistanActiveUntil: Long = 0L,
        random: Random? = null,
        label: String = "Initiative"
    ): InitiativeRoll = InitiativeRoll(
        dice = rollD10(random, label),
        bonus = initiativeBonus(character, sandevistanActiveUntil)
    )

    fun rollDamage(expression: String, random: Random? = null, label: String = ""): DamageRoll {
        val m = Regex("""(\d+)d6(?:\s*([+-])\s*(\d+))?""", RegexOption.IGNORE_CASE).find(expression)
            ?: return DamageRoll(emptyList(), 0, false)
        val count = m.groupValues[1].toIntOrNull()?.coerceAtLeast(0) ?: 0
        val dice = if (random != null) List(count) { random.nextInt(1, 7) }
            else DiceSource.roll(count, 6, label.ifBlank { "آسیب $expression" })
        val flat = m.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
        val sign = m.groupValues.getOrNull(2)
        val total = (dice.sum() + if (sign == "-") -flat else flat).coerceAtLeast(0)
        return DamageRoll(dice, total, dice.count { it == 6 } >= 2)
    }

    fun rangeDv(table: List<Pair<String, Int>>, distance: Int): Int? {
        for ((range, dv) in table) {
            val parts = range.split("-")
            if (parts.size == 2) {
                val lo = parts[0].trim().toIntOrNull() ?: continue
                val hi = parts[1].trim().toIntOrNull() ?: continue
                if (distance in lo..hi) return dv
            }
        }
        return null
    }

    fun weaponAttackStat(character: Character, weapon: Weapon, autofire: Boolean): Int {
        val skill = if (autofire) "Autofire" else weapon.type
        return when {
            skill.equals("Melee Weapon", true) ||
                skill.equals("Brawling", true) ||
                skill.equals("Martial Arts", true) ||
                skill.equals("Athletics", true) ->
                EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex)
            else -> EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref)
        }
    }

    fun skillLevel(character: Character, skillName: String): Int {
        character.skills.firstOrNull { it.name.equals(skillName, true) }?.let {
            return it.level + StreetDrugRules.skillModifier(character, skillName) + EquipmentUseRules.gearSkillBonus(character, skillName)
        }
        if (skillName.equals("Martial Arts", true)) {
            return character.skills.filter { it.name.startsWith("Martial Arts", true) }.maxOfOrNull { it.level } ?: 0
        }
        return 0
    }

    fun bodyDamageExpression(body: Int, hasCyberarm: Boolean = false): String = when {
        body >= 11 -> "4d6"
        body >= 7 -> "3d6"
        body >= 5 || hasCyberarm -> "2d6"
        else -> "1d6"
    }

    fun qualityAttackModifier(weapon: Weapon): Int = if (weapon.quality.equals("Excellent", true)) 1 else 0

    /** Penalties that apply to every Action regardless of the governing STAT. */
    fun allActionsPenalty(character: Character): Int {
        val wound = when {
            GameRules.isMortallyWounded(character.hp) -> -4
            GameRules.isSeriouslyWounded(character.hp, character.maxHp) && !StreetDrugRules.ignoresSeriouslyWounded(character) -> -2
            else -> 0
        }
        val brain = if (CriticalInjuries.effectActive(character, "brain_injury") || CriticalInjuries.effectActive(character, "concussion")) -2 else 0
        val grapple = if (character.combatEffects.keys.any { it.startsWith("grappled_by_") || it.startsWith("grappling_") }) -2 else 0
        return wound + brain + grapple
    }

    fun attackPenalty(character: Character): Int {
        val armor = GameRules.armorPenalty(character.inventory)
        val wound = when {
            GameRules.isMortallyWounded(character.hp) -> -4
            GameRules.isSeriouslyWounded(character.hp, character.maxHp) && !StreetDrugRules.ignoresSeriouslyWounded(character) -> -2
            else -> 0
        }
        val brain = if (CriticalInjuries.effectActive(character, "brain_injury") || CriticalInjuries.effectActive(character, "concussion")) -2 else 0
        val grapple = if (character.combatEffects.keys.any { it.startsWith("grappled_by_") || it.startsWith("grappling_") }) -2 else 0
        return armor + wound + brain + grapple
    }

    fun rangedInjuryPenalty(character: Character): Int = when {
        CriticalInjuries.effectActive(character, "lost_eye") -> -4
        CriticalInjuries.effectActive(character, "damaged_eye") || character.combatEffects.containsKey("damaged_eye") -> -2
        else -> 0
    }

    fun meleeInjuryPenalty(character: Character): Int =
        if (CriticalInjuries.effectActive(character, "torn_muscle")) -2 else 0

    fun effectiveMove(character: Character): Int {
        var move = character.stats.move + GameRules.armorPenalty(character.inventory)
        if (GameRules.isMortallyWounded(character.hp)) move -= 6
        if (CriticalInjuries.effectActive(character, "collapsed_lung")) move -= 2
        if (CriticalInjuries.effectActive(character, "broken_leg")) move -= 4
        if (CriticalInjuries.effectActive(character, "dismembered_leg")) move -= 6
        return move.coerceAtLeast(1)
    }

    fun canDodgeRanged(character: Character): Boolean =
        EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref) >= 8 && !CriticalInjuries.effectActive(character, "dismembered_leg")

    fun defenderEvasionTotal(character: Character, roll: CheckRoll): Int =
        EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex) + skillLevel(character, "Evasion") + attackPenalty(character) + roll.totalDie

    fun effectiveActionStat(character: Character, stat: String): Int = when (stat.uppercase()) {
        "REF" -> EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref)
        "DEX" -> EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex)
        "WILL" -> EquipmentUseRules.effectiveStat(character, "WILL", character.stats.will)
        "TECH" -> EquipmentUseRules.effectiveStat(character, "TECH", character.stats.tech)
        else -> 0
    }

    fun canAimWeapon(weapon: Weapon, autofire: Boolean, shotgunShell: Boolean, explosive: Boolean): Boolean {
        if (autofire || shotgunShell || explosive) return false
        return weapon.name !in setOf(
            "Constitution Arms Hurricane", "Flamethrower", "Rhinemetall EMG-86 Railgun", "Tsunami Arms Helix"
        )
    }

    fun autofireAmmoCost(weapon: Weapon): Int = if (weapon.name.equals("Tsunami Arms Helix", true)) 20 else 10
    fun reloadActionCost(weapon: Weapon): Int = if (weapon.name in setOf("Constitution Arms Hurricane", "Militech Cowboy U-56", "Rhinemetall EMG-86 Railgun", "Tsunami Arms Helix")) 2 else 1
    fun minimumBodyToFire(weapon: Weapon): Int = if (weapon.name in setOf("Constitution Arms Hurricane", "Militech Cowboy U-56", "Rhinemetall EMG-86 Railgun", "Tsunami Arms Helix")) 11 else 0
    fun isAutofireOnly(weapon: Weapon): Boolean = weapon.name.equals("Tsunami Arms Helix", true)
    fun ignoresArmorBelowSp11(weapon: Weapon): Boolean = weapon.name.equals("Rhinemetall EMG-86 Railgun", true)
    fun isStunWeapon(weapon: Weapon): Boolean = weapon.name.equals("Stun Baton", true) || weapon.name.equals("Stun Gun", true)
    fun cannotCauseCritical(weapon: Weapon): Boolean = isStunWeapon(weapon) || weapon.name.equals("Flamethrower", true) || weapon.name.equals("Air Pistol", true)

    /**
     * انتخاب جراحت بحرانی از بین گزینه‌های ممکن.
     *
     * قاعده‌ی Core: «2d6 بریز؛ اگر جراحتی آمد که هدف همین حالا دارد، آن‌قدر
     * دوباره بریز تا یکی آزاد بیاید».
     *
     * چرا این تابع جدا شد: قبلاً در حالت نادری که هیچ رولی به گزینه‌های آزاد
     * نمی‌خورد، کد به `available.random()` می‌افتاد — و `random()` از
     * [DiceSource] رد نمی‌شد. یعنی در حالت «تاس دستی» برنامه بی‌صدا و بدون
     * پرسیدن از GM یک جراحت بحرانی از خودش درمی‌آورد؛ دقیقاً همان چیزی که
     * آن حالت آمده بود جلویش را بگیرد. حالا انتخاب آخر هم یک درخواست تاس
     * صریح است (dN روی گزینه‌های باقی‌مانده) و در لاگ تاس دیده می‌شود.
     *
     * @param gmRoll رولی که GM در حالت دستی برای جدول وارد کرده؛ اولویت دارد.
     */
    private fun pickInjury(
        available: List<CriticalInjuries.Injury>,
        gmRoll: Int? = null
    ): CriticalInjuries.Injury? {
        if (available.isEmpty()) return null
        if (gmRoll != null) available.firstOrNull { it.roll == gmRoll }?.let { return it }
        repeat(MAX_INJURY_REROLLS) {
            val roll = DiceSource.roll(2, 6, "جدول Critical Injury (2d6)").sum()
            available.firstOrNull { it.roll == roll }?.let { return it }
        }
        val pick = DiceSource.rollOne(
            available.size,
            "انتخاب جراحت از ${available.size} گزینه‌ی باقی‌مانده (d${available.size})"
        )
        return available[(pick - 1).coerceIn(0, available.size - 1)]
    }

    /** سقف تلاش برای رول مجدد روی جدول جراحت‌ها پیش از انتخاب با dN. */
    private const val MAX_INJURY_REROLLS = 30

    fun resolveDamage(
        target: Character,
        rawDamage: Int,
        head: Boolean,
        melee: Boolean,
        critical: Boolean,
        halvesArmor: Boolean = melee,
        injuryRoll: Int? = null,
        armorPiercing: Boolean = false,
        rubber: Boolean = false,
        expansive: Boolean = false,
        ignoreArmorBelowSp11: Boolean = false,
        stunWeapon: Boolean = false
    ): Pair<Character, DamageResolution> {
        if (target.isDead) return target to DamageResolution(rawDamage, 0, 0, 0, 0, null)

        val wasMortallyWounded = GameRules.isMortallyWounded(target.hp)
        val fullSp = GameRules.effectiveArmorSp(target, head)
        val armorIgnored = ignoreArmorBelowSp11 && fullSp < 11
        val armorUsed = if (armorIgnored) 0 else if (halvesArmor) ceil(fullSp / 2.0).toInt() else fullSp
        val penetrates = rawDamage > armorUsed
        val throughArmor = if (penetrates) rawDamage - armorUsed else 0
        val headMultiplier = if (head) {
            if (CriticalInjuries.effectActive(target, "cracked_skull")) 3 else 2
        } else 1
        val baseHpDamage = throughArmor * headMultiplier
        val criticalBonus = 5
        val wasUnconscious = target.combatEffects.containsKey("unconscious")
        var next = target
        val ablation = if (!rubber && !stunWeapon && !armorIgnored && penetrates && fullSp > 0) if (armorPiercing) 2 else 1 else 0
        if (ablation > 0) next = GameRules.ablateArmor(next, head, ablation)

        // Normal criticals happen even when armor stops all ordinary weapon damage.
        // A Mortally Wounded target also suffers a Critical Injury whenever damaged by a melee/ranged attack.
        val wouldDamageMortallyWounded = wasMortallyWounded && baseHpDamage > 0 && !rubber && !stunWeapon
        val mustTakeCritical = (!rubber && !stunWeapon && critical) || wouldDamageMortallyWounded
        val table = if (head) CriticalInjuries.headInjuries else CriticalInjuries.bodyInjuries
        val injury = if (mustTakeCritical) {
            pickInjury(table.filterNot { it.key in target.criticalInjuries }, injuryRoll)
        } else null

        // A natural Critical and the forced Critical from damaging someone already Mortally Wounded
        // both deal the Critical Injury's +5 direct HP damage, but never stack into +10 on the same hit.
        val addCriticalBonus = injury != null && ((!rubber && !stunWeapon && critical) || wouldDamageMortallyWounded)
        val finalDamage = (baseHpDamage + if (addCriticalBonus) criticalBonus else 0).coerceAtLeast(0)
        val ordinaryHpAfter = (next.hp - finalDamage).coerceAtLeast(0)
        val stunTriggered = stunWeapon && target.hp >= 1 && ordinaryHpAfter < 1
        val hpAfter = if (stunTriggered) 1 else ordinaryHpAfter
        next = next.copy(hp = hpAfter)
        if (stunTriggered) next = next.copy(combatEffects = next.combatEffects + ("unconscious" to -1))
        // Taking damage wakes a previously unconscious target; do not undo unconsciousness just caused by a Stun weapon.
        if (finalDamage > 0 && wasUnconscious && !stunWeapon) next = next.copy(combatEffects = next.combatEffects - "unconscious")

        val deathPenaltyKeys = setOf(
            "dismembered_arm", "dismembered_hand", "collapsed_lung", "spinal_injury", "dismembered_leg",
            "lost_eye", "brain_injury", "whiplash", "cracked_skull", "crushed_windpipe", "lost_ear"
        )
        var penaltyIncrease = 0
        if (injury != null) {
            next = next.copy(criticalInjuries = next.criticalInjuries + injury.key)
            if (injury.key in deathPenaltyKeys) penaltyIncrease += 1
        }
        if (expansive && injury?.key?.startsWith("foreign_object") == true) {
            val extraPool = table.filter { !it.key.startsWith("foreign_object") && it.key !in next.criticalInjuries }
            val extra = pickInjury(extraPool, null)
            if (extra != null) {
                next = next.copy(criticalInjuries = next.criticalInjuries + extra.key)
                if (extra.key in deathPenaltyKeys) penaltyIncrease += 1
            }
        }
        // Every melee/ranged hit suffered while already Mortally Wounded raises the Death Save Penalty by 1.
        if (wouldDamageMortallyWounded) penaltyIncrease += 1
        if (penaltyIncrease > 0) next = next.copy(deathSavePenalty = next.deathSavePenalty + penaltyIncrease)

        val actualHpDamage = (target.hp - hpAfter).coerceAtLeast(0)
        return GameRules.syncDerived(next) to DamageResolution(
            rawDamage = rawDamage,
            armorUsed = armorUsed,
            hpDamage = actualHpDamage,
            armorAblated = ablation,
            criticalBonus = if (addCriticalBonus) criticalBonus else 0,
            injuryKey = injury?.key
        )
    }
}
