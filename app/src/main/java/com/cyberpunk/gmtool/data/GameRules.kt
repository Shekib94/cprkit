package com.cyberpunk.gmtool.data

import kotlin.math.ceil

/** Core-rule helpers kept separate from UI so every screen uses the same math. */
object GameRules {
    /**
     * شناسه‌ی تازه برای [Character.sheetUid].
     * اینجا است تا هم لایه‌ی داده و هم تست‌ها یک منبع واحد داشته باشند.
     */
    fun newSheetUid(): String = java.util.UUID.randomUUID().toString()

    fun maxHp(body: Int, will: Int): Int = 10 + 5 * ceil((body + will) / 2.0).toInt()

    /** آستانهٔ Seriously Wounded: کمتر یا مساوی نصف HP کامل (گرد به بالا). */
    fun seriouslyWoundedThreshold(maxHp: Int): Int = ceil(maxHp / 2.0).toInt()

    /**
     * Core: «You are Seriously Wounded if your Current Hit Points are equal to or less
     * than 1/2 your Full Hit Point total.» — یعنی خودِ نصف هم شامل است.
     *
     * قبلاً اینجا `hp in 1 until threshold` بود؛ یک واحد کم می‌آورد و کسی که دقیقاً
     * روی نصف HP می‌ایستاد پنالتی -2 (و DV پایدارسازی 13) نمی‌گرفت. تستِ مرز در
     * `RulesRegressionTest` این را قفل می‌کند.
     */
    fun isSeriouslyWounded(hp: Int, maxHp: Int): Boolean =
        seriouslyWoundedThreshold(maxHp).let { hp in 1..it }

    fun isMortallyWounded(hp: Int): Boolean = hp <= 0

    fun baseDeathSavePenalty(character: Character): Int {
        val penaltyKeys = setOf(
            "dismembered_arm", "dismembered_hand", "collapsed_lung", "spinal_injury", "dismembered_leg",
            "lost_eye", "brain_injury", "whiplash", "cracked_skull", "crushed_windpipe", "lost_ear"
        )
        return character.criticalInjuries.count { it in penaltyKeys && CriticalInjuries.effectActive(character, it) }
    }
    fun currentEmpathy(humanity: Int): Int = (humanity.coerceAtLeast(0) / 10).coerceAtLeast(0)

    fun isExternalLinearFrame(item: InventoryItem): Boolean =
        item.category.equals("Gear", true) &&
            (item.name.equals("Linear Frame Sigma", true) || item.name.equals("Linear Frame Beta", true))

    private fun hasImplantedBeta(character: Character): Boolean = character.inventory.any {
        it.equipped && it.category.equals("Cyberware", true) && it.name.equals("Implanted Linear Frame Beta", true)
    }

    private fun hasImplantedSigma(character: Character): Boolean = character.inventory.any {
        it.equipped && it.category.equals("Cyberware", true) && it.name.equals("Implanted Linear Frame Sigma", true)
    }

    /** BODY used by HP and Death Saves. External frames explicitly do not change either. */
    fun deathSaveBody(character: Character): Int {
        val graftCount = character.inventory.count { it.equipped && it.category.equals("Cyberware", true) &&
            it.name.contains("Grafted Muscle and Bone Lace", true) }
        val inferredBase = if (character.baseBody > 0) character.baseBody else (character.stats.body.coerceAtMost(10) - graftCount * 2).coerceAtLeast(1)
        val organic = (inferredBase + graftCount * 2).coerceAtMost(10)
        return when {
            hasImplantedBeta(character) -> 14
            hasImplantedSigma(character) -> 12
            else -> organic
        }
    }

    /** Armor penalty is paid once, using the worst armor worn. */
    fun armorPenalty(items: List<InventoryItem>): Int = items.asSequence()
        .filter { it.equipped && it.category.equals("Armor", true) }
        .mapNotNull { ArmorCatalog.penaltyFor(it.name) }
        .minOrNull() ?: 0

    fun maxHumanity(baseEmp: Int, items: List<InventoryItem>): Int {
        val reduction = items.asSequence()
            .filter { it.equipped && it.category.equals("Cyberware", true) }
            .sumOf { CyberwareCatalog.maxReductionFor(it.name) }
        return (baseEmp * 10 - reduction).coerceAtLeast(0)
    }

    private fun armorApplies(item: InventoryItem, head: Boolean): Boolean {
        if (!item.equipped || !item.category.equals("Armor", true) || ArmorCatalog.isShield(item.name)) return false
        val n = item.name.lowercase()
        if (n.contains("bodyweight")) return true
        val explicitHead = n.contains("head") || n.contains("helmet") || n.contains("کلاه") || n.contains("سر")
        val explicitBody = n.contains("body") || n.contains("بدن")
        return when {
            explicitHead -> head
            explicitBody -> !head
            else -> !head
        }
    }

    private fun isImplantedArmor(item: InventoryItem): Boolean =
        item.equipped && item.category.equals("Cyberware", true) &&
            (item.name.contains("Subdermal Armor", true) || item.name.contains("Skin Weave", true))

    private fun normalSp(item: InventoryItem): Int = item.sp ?: when {
        item.name.contains("Subdermal Armor", true) -> 11
        item.name.contains("Skin Weave", true) -> 7
        else -> ArmorCatalog.spFor(item.name) ?: 0
    }

    private fun locationSp(item: InventoryItem, head: Boolean): Int {
        val normal = normalSp(item)
        return if (head) item.currentHeadSp ?: item.currentSp ?: normal
        else item.currentBodySp ?: item.currentSp ?: normal
    }

    fun effectiveArmorSp(character: Character, head: Boolean): Int = character.inventory.asSequence()
        .filter { armorApplies(it, head) || isImplantedArmor(it) }
        .map { locationSp(it, head) }
        .maxOrNull() ?: 0

    /** When armor in a location is ablated, every source of SP in that same location ablates together. */
    fun ablateArmor(character: Character, head: Boolean, amount: Int = 1): Character {
        if (amount <= 0) return character
        val inv = character.inventory.map { item ->
            if (armorApplies(item, head) || isImplantedArmor(item)) {
                val normal = normalSp(item)
                val current = locationSp(item, head)
                if (head) item.copy(sp = item.sp ?: normal, currentHeadSp = (current - amount).coerceAtLeast(0), currentSp = null)
                else item.copy(sp = item.sp ?: normal, currentBodySp = (current - amount).coerceAtLeast(0), currentSp = null)
            } else item
        }
        // زره پوشیدنی که SP هر دو محلش صفر شده نابود است و از تن درمی‌آید.
        // Core دربارهٔ صفر صریح نیست؛ انتخاب GM این میز: زره صفر دیگر پوشیده نمی‌ماند.
        // سایبرویر کاشته‌شده (Skin Weave/Subdermal) استثناست — از تن درنمی‌آید و
        // طبق قواعد خودش روزی ۱ SP بازمی‌گردد.
        val cleaned = inv.filterNot { item ->
            item.equipped && item.category.equals("Armor", true) &&
                !ArmorCatalog.isShield(item.name) &&
                normalSp(item) > 0 &&
                locationSp(item, true) <= 0 && locationSp(item, false) <= 0
        }
        val next = character.copy(inventory = cleaned)
        return next.copy(headArmorSp = effectiveArmorSp(next, true), bodyArmorSp = effectiveArmorSp(next, false))
    }

    /** آیا این قطعه زره پوشیدنیِ نابودشده است؟ برای هشدار در UI. */
    fun isArmorDestroyed(item: InventoryItem): Boolean =
        item.category.equals("Armor", true) && !ArmorCatalog.isShield(item.name) &&
            normalSp(item) > 0 && locationSp(item, true) <= 0 && locationSp(item, false) <= 0

    /** SP فعلی یک قطعه در یک محل — برای نمایش در Inventory. */
    fun itemSp(item: InventoryItem, head: Boolean): Int = locationSp(item, head)

    /**
     * برچسب SP برای نمایش در کوله: «۹ / ۱۱».
     *
     * نکته‌ی مهم: نباید بیشترینِ دو محل را گرفت. یک زره‌ی بدن هیچ مقداری برای
     * محل سر ندارد، پس آن محل به عدد کامل برمی‌گردد و بیشترین همیشه «سالم»
     * نشان می‌داد — یعنی آسیب دیده نمی‌شد. فقط محلی را می‌خوانیم که این قطعه
     * واقعاً می‌پوشاند؛ زره‌ی دوقسمتی (Bodyweight) هر دو را نشان می‌دهد.
     */
    fun armorSpLabel(item: InventoryItem): String? {
        if (!item.category.equals("Armor", true)) return null
        val max = normalSp(item)
        if (max <= 0) return null
        val coversHead = armorCoversLocation(item, true)
        val coversBody = armorCoversLocation(item, false)
        return when {
            coversHead && coversBody -> {
                val h = locationSp(item, true); val b = locationSp(item, false)
                if (h == b) "$h / $max" else "سر $h • بدن $b / $max"
            }
            coversHead -> "${locationSp(item, true)} / $max"
            else -> "${locationSp(item, false)} / $max"
        }
    }

    /** آیا این قطعه (فارغ از پوشیده بودن) این محل را می‌پوشاند؟ */
    fun armorCoversLocation(item: InventoryItem, head: Boolean): Boolean {
        if (!item.category.equals("Armor", true) || ArmorCatalog.isShield(item.name)) return false
        val n = item.name.lowercase()
        if (n.contains("bodyweight")) return true
        val explicitHead = n.contains("head") || n.contains("helmet") || n.contains("کلاه") || n.contains("سر")
        val explicitBody = n.contains("body") || n.contains("بدن")
        return when {
            explicitHead -> head
            explicitBody -> !head
            else -> !head
        }
    }

    /** آیا SP این قطعه از حالت نو کمتر شده؟ */
    fun isArmorWorn(item: InventoryItem): Boolean {
        if (!item.category.equals("Armor", true)) return false
        val max = normalSp(item)
        if (max <= 0) return false
        val h = if (armorCoversLocation(item, true)) locationSp(item, true) else max
        val b = if (armorCoversLocation(item, false)) locationSp(item, false) else max
        return h < max || b < max
    }

    /** SP کاملِ اولیه‌ی یک قطعه. */
    fun itemMaxSp(item: InventoryItem): Int = normalSp(item)

    /**
     * Manual repair for worn armor. Ablation affects every source in a location at once, but repairing one
     * physical armor source does not also repair every other source. Implanted Skin Weave/Subdermal Armor
     * use their own recovery rules and are therefore not changed by this manual worn-armor adjustment.
     */
    fun repairArmor(character: Character, head: Boolean, amount: Int = 1): Character {
        if (amount <= 0) return character
        val candidates = character.inventory.withIndex().filter { (_, item) ->
            armorApplies(item, head) && locationSp(item, head) < normalSp(item)
        }
        val targetIndex = candidates.maxWithOrNull(
            compareBy<IndexedValue<InventoryItem>> { locationSp(it.value, head) }
                .thenBy { normalSp(it.value) }
        )?.index ?: return character

        val inv = character.inventory.mapIndexed { index, item ->
            if (index != targetIndex) item else {
                val normal = normalSp(item)
                val current = locationSp(item, head)
                if (head) item.copy(sp = item.sp ?: normal, currentHeadSp = (current + amount).coerceAtMost(normal), currentSp = null)
                else item.copy(sp = item.sp ?: normal, currentBodySp = (current + amount).coerceAtMost(normal), currentSp = null)
            }
        }
        val next = character.copy(inventory = inv)
        return next.copy(headArmorSp = effectiveArmorSp(next, true), bodyArmorSp = effectiveArmorSp(next, false))
    }

    /** Recalculate derived state after migration or a mechanical change. */
    fun syncDerived(character: Character): Character {
        // شناسه‌ی پایدار برگه: یک بار ساخته می‌شود و بعد هرگز عوض نمی‌شود.
        // چون syncDerived روی همه‌ی مسیرها (load، import، شبکه، persist) صدا
        // زده می‌شود، سیوهای قدیمی بدون هیچ migration جداگانه‌ای uid می‌گیرند.
        // گزارش تست ۳.۵: سیوهای قدیمی Fumble Recovery را با فلگ ۱ ذخیره می‌کردند در حالی که
        // هزینه‌اش همیشه ۴ امتیاز ثابت بوده. حالا مقدار ذخیره‌شده = امتیاز واقعی؛ پس ۱های
        // قدیمی (که ۴ امتیاز بابتشان پرداخت شده) به ۴ مهاجرت می‌کنند تا شرط >= 4 آن‌ها را
        // از کار نیندازد. (دو مهاجرت در یک اعلان ادغام شده‌اند: دو val هم‌نام در یک دامنه
        // در Kotlin خطای Conflicting declarations می‌دهد.)
        val withUid =
            if (character.sheetUid.isBlank()) character.copy(sheetUid = newSheetUid()) else character
        val character =
            if (withUid.role.equals("Solo", true) && (withUid.roleAbilityPoints["fumble"] ?: 0) in 1..3)
                withUid.copy(roleAbilityPoints = withUid.roleAbilityPoints + ("fumble" to 4))
            else withUid
        val inferredBaseEmp = when {
            character.baseEmp > 0 -> character.baseEmp
            character.maxHumanity > 0 -> maxOf(character.stats.emp, ceil(character.maxHumanity / 10.0).toInt())
            else -> character.stats.emp.coerceAtLeast(1)
        }
        val maxHum = maxHumanity(inferredBaseEmp, character.inventory)
        val hum = character.currentHumanity.coerceIn(0, maxHum)
        val currentEmp = minOf(inferredBaseEmp, currentEmpathy(hum))
        val graftCount = character.inventory.count { it.equipped && it.category.equals("Cyberware", true) &&
            it.name.contains("Grafted Muscle and Bone Lace", true) }
        val implantedBeta = hasImplantedBeta(character)
        val implantedSigma = hasImplantedSigma(character)
        val externalBeta = character.inventory.any { it.equipped && isExternalLinearFrame(it) && it.name.contains("Beta", true) }
        val externalSigma = character.inventory.any { it.equipped && isExternalLinearFrame(it) && it.name.contains("Sigma", true) }
        val migrationBody = when {
            implantedBeta || implantedSigma || externalBeta || externalSigma -> character.stats.body.coerceAtMost(10)
            else -> character.stats.body
        }
        val inferredBaseBody = if (character.baseBody > 0) character.baseBody else (migrationBody - graftCount * 2).coerceAtLeast(1)
        val organicBody = (inferredBaseBody + graftCount * 2).coerceAtMost(10)
        val internalBody = when { implantedBeta -> 14; implantedSigma -> 12; else -> organicBody }
        val effectiveBody = when { externalBeta -> 14; externalSigma -> 12; else -> internalBody }
        val stats = character.stats.copy(emp = currentEmp, body = effectiveBody)
        // External Linear Frames change BODY for checks/damage only; HP and Death Save stay based on internal BODY.
        val hpMax = maxHp(internalBody, stats.will)
        val withCore = character.copy(
            baseEmp = inferredBaseEmp,
            baseBody = inferredBaseBody,
            stats = stats,
            maxHumanity = maxHum,
            currentHumanity = hum,
            maxHp = hpMax,
            hp = character.hp.coerceIn(0, hpMax),
            maxLuck = stats.luck,
            currentLuck = character.currentLuck.coerceIn(0, stats.luck),
            inventory = character.inventory.map { item ->
                val implanted = isImplantedArmor(item)
                val armor = item.category.equals("Armor", true) && !ArmorCatalog.isShield(item.name)
                if (armor || implanted) {
                    val normal = normalSp(item)
                    val legacy = item.currentSp ?: normal
                    val appliesHead = implanted || armorApplies(item, true)
                    val appliesBody = implanted || armorApplies(item, false)
                    item.copy(
                        sp = item.sp ?: normal,
                        currentSp = null,
                        currentHeadSp = if (appliesHead) item.currentHeadSp ?: legacy else item.currentHeadSp,
                        currentBodySp = if (appliesBody) item.currentBodySp ?: legacy else item.currentBodySp
                    )
                } else item
            }
        )
        return withCore.copy(
            headArmorSp = effectiveArmorSp(withCore, true),
            bodyArmorSp = effectiveArmorSp(withCore, false)
        )
    }
}
