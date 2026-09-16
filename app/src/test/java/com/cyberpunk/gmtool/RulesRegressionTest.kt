package com.cyberpunk.gmtool

import com.cyberpunk.gmtool.data.GameRules
import com.cyberpunk.gmtool.data.InventoryItem
import com.cyberpunk.gmtool.data.NpcData
import com.cyberpunk.gmtool.data.SkillCatalog
import com.cyberpunk.gmtool.data.StoreCatalog
import com.cyberpunk.gmtool.data.StreetratData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های محافظ برای اعداد و قواعدی که اگر بی‌صدا خراب شوند، سر میز فاجعه می‌سازند.
 *
 * چرا این فایل وجود دارد:
 * بارها پیش آمده که یک اصلاح در یک گوشه، جای دیگری را شکسته — مثلاً حذف یک مهارت
 * که هنوز در سناریوها استفاده می‌شد، یا یک NPC تکراری که کسی ندید. این تست‌ها
 * دقیقاً همان دسته خطا را در چند ثانیه می‌گیرند.
 *
 * اجرا:  ./gradlew test
 */
class RulesRegressionTest {

    // ─────────────────── مهارت‌ها ───────────────────

    /** Core ص۸۲: دقیقاً هفت مهارت Difficult (x2) وجود دارد. */
    @Test
    fun difficultSkills_areExactlyTheSevenFromCore() {
        val expected = listOf(
            "Autofire", "Heavy Weapons", "Martial Arts", "Demolitions",
            "Electronics/Security Tech", "Paramedic", "Pilot Air Vehicle"
        )
        expected.forEach {
            assertTrue("«$it» باید Difficult باشد", SkillCatalog.isDifficult(it))
        }
        // نمونه‌ای از مهارت‌های عادی نباید Difficult باشند
        listOf("Handgun", "Perception", "Persuasion", "Stealth", "Brawling").forEach {
            assertFalse("«$it» نباید Difficult باشد", SkillCatalog.isDifficult(it))
        }
    }

    /** هزینه‌ی IP مهارت Difficult دقیقاً دوبرابر عادی است. */
    @Test
    fun improvementCost_doublesForDifficultSkills() {
        assertEquals(20, SkillCatalog.improvementCost("Handgun", 1))
        assertEquals(40, SkillCatalog.improvementCost("Autofire", 1))
        assertEquals(40, SkillCatalog.improvementCost("Pilot Air Vehicle", 1))
        assertEquals(200, SkillCatalog.improvementCost("Handgun", 10))
        assertEquals(400, SkillCatalog.improvementCost("Autofire", 10))
    }

    /** هزینه‌ی امتیاز ساخت شخصیت هم همین قاعده را دارد. */
    @Test
    fun creationPointCost_doublesForDifficultSkills() {
        assertEquals(4, SkillCatalog.creationPointCost("Handgun", 4))
        assertEquals(8, SkillCatalog.creationPointCost("Martial Arts", 4))
    }

    /** «Leadership» در Master Skill List کتاب نیست و نباید قابل انتخاب باشد. */
    @Test
    fun leadership_isNotASelectableSkill() {
        assertFalse(
            "Leadership مهارت کتاب نیست",
            SkillCatalog.allSkillNames().any { it.equals("Leadership", true) }
        )
    }

    // ─────────────────── انسانیت و EMP ───────────────────

    /** Core ص۸۰: EMP برابر رقم دهگانِ Humanity است. */
    @Test
    fun empathy_isTheTensDigitOfHumanity() {
        assertEquals(6, GameRules.currentEmpathy(60))
        assertEquals(4, GameRules.currentEmpathy(44))
        assertEquals(3, GameRules.currentEmpathy(39))
        assertEquals(1, GameRules.currentEmpathy(10))
        assertEquals(0, GameRules.currentEmpathy(9))
        assertEquals(0, GameRules.currentEmpathy(0))
    }

    /** انسانیت منفی نباید EMP منفی بدهد. */
    @Test
    fun empathy_neverGoesNegative() {
        assertEquals(0, GameRules.currentEmpathy(-15))
    }

    // ─────────────────── HP ───────────────────

    /** فرمول HP کتاب: میانگین BODY و WILL، گرد به بالا، طبق جدول. */
    @Test
    fun maxHp_matchesTheCoreTable() {
        // فرمول کتاب: 10 + 5 × میانگینِ گردشده‌ی BODY و WILL
        assertEquals(40, StreetratData.calculateMaxHp(6, 6))   // avg 6
        assertEquals(60, StreetratData.calculateMaxHp(10, 10)) // avg 10
        assertEquals(20, StreetratData.calculateMaxHp(2, 2))   // avg 2
        assertEquals(45, StreetratData.calculateMaxHp(6, 7))   // avg 6.5 -> 7
        // پایین‌ترین حالت
        assertTrue(StreetratData.calculateMaxHp(2, 2) < StreetratData.calculateMaxHp(6, 6))
        // بالاترین حالت
        assertTrue(StreetratData.calculateMaxHp(10, 10) > StreetratData.calculateMaxHp(6, 6))
    }

    /** HP باید با بالا رفتن BODY/WILL یکنواخت زیاد شود، نه پرش عجیب. */
    @Test
    fun maxHp_isMonotonic() {
        var previous = 0
        for (v in 1..10) {
            val hp = StreetratData.calculateMaxHp(v, v)
            assertTrue("HP نباید با افزایش STAT کم شود (v=$v)", hp >= previous)
            previous = hp
        }
    }

    // ─────────────────── زره ───────────────────

    /** هر اصابت نافذ، SP همان محل را ۱ کم می‌کند. */
    @Test
    fun armorAblation_reducesSpByOnePerHit() {
        val armor = InventoryItem(
            name = "Light Armorjack Body Armor (SP11)",
            category = "Armor", sp = 11, currentBodySp = 11, equipped = true
        )
        var c = com.cyberpunk.gmtool.data.Character(inventory = listOf(armor))
        assertEquals(11, GameRules.effectiveArmorSp(c, false))
        c = GameRules.ablateArmor(c, head = false, amount = 1)
        assertEquals(10, GameRules.effectiveArmorSp(c, false))
        c = GameRules.ablateArmor(c, head = false, amount = 1)
        assertEquals(9, GameRules.effectiveArmorSp(c, false))
    }

    /**
     * برچسب SP باید محلی را بخواند که قطعه واقعاً می‌پوشاند.
     * قبلاً بیشترینِ دو محل گرفته می‌شد و چون زره‌ی بدن مقدار «سر» ندارد،
     * همیشه عدد کامل نشان می‌داد و آسیب پنهان می‌ماند.
     */
    @Test
    fun armorSpLabel_showsDamageOnBodyOnlyArmor() {
        val damaged = InventoryItem(
            name = "Light Armorjack Body Armor (SP11)",
            category = "Armor", sp = 11, currentBodySp = 9, equipped = true
        )
        val label = GameRules.armorSpLabel(damaged)
        assertTrue("برچسب باید آسیب را نشان دهد، نه ۱۱ کامل: $label", label?.startsWith("9") == true)
        assertTrue("باید فرسوده تشخیص داده شود", GameRules.isArmorWorn(damaged))
    }

    /** زره سالم نباید فرسوده علامت بخورد. */
    @Test
    fun armorSpLabel_freshArmorIsNotWorn() {
        val fresh = InventoryItem(
            name = "Light Armorjack Body Armor (SP11)",
            category = "Armor", sp = 11, currentBodySp = 11, equipped = true
        )
        assertFalse(GameRules.isArmorWorn(fresh))
    }

    // ─────────────────── فروشگاه ───────────────────

    /** Core ص۹۲/۹۴: سلاح سرد سنگین و کمان قابل مخفی‌کردن نیستند. */
    @Test
    fun heavyWeaponsAndBows_areNotConcealable() {
        listOf("Heavy Melee Weapon", "Very Heavy Melee Weapon", "Bows / Crossbow").forEach { name ->
            val item = StoreCatalog.items.firstOrNull { it.name == name }
            assertTrue("$name در کاتالوگ پیدا نشد", item != null)
            assertEquals("$name نباید قابل مخفی‌شدن باشد", "No", item!!.concealed)
        }
    }

    /** هیچ آیتمی نباید قیمت منفی داشته باشد. */
    @Test
    fun storeItems_haveNonNegativePrices() {
        val bad = StoreCatalog.items.filter { it.basePrice < 0 }
        assertTrue("آیتم با قیمت منفی: ${bad.map { it.name }}", bad.isEmpty())
    }

    /** نام آیتم‌های فروشگاه نباید تکراری باشد (در یک دسته). */
    @Test
    fun storeItems_haveNoDuplicateNamesWithinCategory() {
        val dups = StoreCatalog.items
            .groupBy { it.category.lowercase() + "|" + it.name.lowercase() }
            .filter { it.value.size > 1 }
            .keys
        assertTrue("آیتم تکراری: $dups", dups.isEmpty())
    }

    // ─────────────────── NPCها ───────────────────

    /** NPC تکراری یعنی در فهرست دو بار دیده می‌شود — این یک بار واقعاً اتفاق افتاد. */
    @Test
    fun npcTemplates_haveUniqueNames() {
        val dups = NpcData.allTemplates.groupBy { it.name.lowercase() }.filter { it.value.size > 1 }.keys
        assertTrue("NPC تکراری: $dups", dups.isEmpty())
    }

    /** STATها باید در بازه‌ی منطقی باشند (BODY تا ۱۲ برای بورگ‌ها). */
    @Test
    fun npcStats_areWithinValidRange() {
        NpcData.allTemplates.forEach { t ->
            val s = t.stats
            listOf(
                "INT" to s.int, "REF" to s.ref, "DEX" to s.dex, "TECH" to s.tech,
                "COOL" to s.cool, "WILL" to s.will, "LUCK" to s.luck,
                "MOVE" to s.move, "BODY" to s.body, "EMP" to s.emp
            ).forEach { (label, v) ->
                assertTrue("${t.name}: $label=$v خارج از بازه", v in 0..12)
            }
        }
    }

    /** هر NPC باید دست‌کم یک مهارت داشته باشد وگرنه در نبرد بی‌استفاده است. */
    @Test
    fun npcTemplates_haveSkills() {
        val empty = NpcData.allTemplates.filter { it.skillTotals.isEmpty() }
        assertTrue("NPC بدون مهارت: ${empty.map { it.name }}", empty.isEmpty())
    }

    /** هر NPC ساخته‌شده باید HP مثبت داشته باشد. */
    @Test
    fun npcToCharacter_producesUsableSheets() {
        NpcData.allTemplates.forEach { t ->
            val c = NpcData.toCharacter(t)
            assertTrue("${t.name}: HP باید مثبت باشد", c.maxHp > 0)
            assertEquals("${t.name}: HP فعلی باید کامل باشد", c.maxHp, c.hp)
            assertFalse("${t.name}: NPC نباید متحد باشد", c.isAlly)
        }
    }

    /**
     * NPCهای سلاح‌دار باید مهمات یدکی داشته باشند.
     * پیش از این فقط خشابِ داخل سلاح پر می‌شد و مثلاً آدام اسمشر
     * بعد از یک راکت خلع‌سلاح می‌شد.
     */
    @Test
    fun npcsWithMagazineWeapons_carrySpareAmmo() {
        val offenders = mutableListOf<String>()
        NpcData.allTemplates.forEach { t ->
            val c = NpcData.toCharacter(t)
            val needsAmmo = c.weapons.any { it.magazineSize > 0 && it.ammoType.isNotBlank() }
            if (needsAmmo) {
                val hasAmmo = c.inventory.any { it.category.equals("Ammo", true) }
                if (!hasAmmo) offenders += t.name
            }
        }
        assertTrue("NPC بدون مهمات یدکی: $offenders", offenders.isEmpty())
    }

    // ─────────────────── سایبرویر ───────────────────

    /** Humanity Loss هیچ‌وقت منفی نیست. */
    @Test
    fun cyberwareHumanityLoss_isNeverNegative() {
        StoreCatalog.items.filter { it.category == "Cyberware" }.forEach {
            val hl = it.humanityLoss ?: 0
            assertTrue("${it.name}: HL منفی", hl >= 0)
        }
    }

    /** سقف انسانیت با نصب سایبرویر پایین می‌آید ولی منفی نمی‌شود. */
    @Test
    fun maxHumanity_dropsWithCyberwareButStaysNonNegative() {
        val none = GameRules.maxHumanity(6, emptyList())
        assertEquals(60, none)
        val withChrome = GameRules.maxHumanity(
            6,
            listOf(InventoryItem("Cyberaudio Suite", "Cyberware", equipped = true))
        )
        assertTrue("سقف باید کم شود", withChrome <= none)
        assertTrue("سقف نباید منفی شود", GameRules.maxHumanity(1, List(20) {
            InventoryItem("Cyberleg", "Cyberware", equipped = true)
        }) >= 0)
    }

    // ─────────────────── مهاجرت سیوهای قدیمی ───────────────────

    /**
     * گزارش تست ۳.۵: سیوهای قدیمی Fumble Recovery را با فلگ ۱ ذخیره می‌کردند
     * (هزینه‌اش همیشه ۴ امتیاز ثابت بوده). بعد از تغییر شرط رزمی به «امتیاز
     * واقعی >= 4»، syncDerived باید ۱ قدیمی را هنگام load به ۴ مهاجرت کند؛
     * وگرنه سولوهای قدیمی بی‌صدا قابلیتشان را از دست می‌دادند.
     */
    @Test
    fun syncDerived_migratesLegacyFumbleFlagToActualPoints() {
        val legacy = com.cyberpunk.gmtool.data.Character(
            role = "Solo",
            roleAbilityPoints = mapOf("fumble" to 1)
        )
        assertEquals(4, GameRules.syncDerived(legacy).roleAbilityPoints["fumble"])

        // مقدار جدید (۴) دست‌نخورده می‌ماند
        val modern = legacy.copy(roleAbilityPoints = mapOf("fumble" to 4))
        assertEquals(4, GameRules.syncDerived(modern).roleAbilityPoints["fumble"])

        // نداشتن قابلیت یعنی ۰/نبود — مهاجرت چیزی اضافه نمی‌کند
        val without = legacy.copy(roleAbilityPoints = emptyMap())
        assertEquals(0, GameRules.syncDerived(without).roleAbilityPoints["fumble"] ?: 0)

        // نقش غیر از Solo مهاجرت نمی‌گیرد
        val otherRole = legacy.copy(role = "Netrunner")
        assertEquals(1, GameRules.syncDerived(otherRole).roleAbilityPoints["fumble"])
    }

    // ─────────────────── زخم‌ها: مرز Seriously Wounded ───────────────────

    /**
     * Core: «You are Seriously Wounded if your Current Hit Points are equal to or less
     * than 1/2 your Full Hit Point total» — یعنی خودِ نصف هم شامل است.
     *
     * این تست دقیقاً همان جایی را قفل می‌کند که قبلاً `hp in 1 until threshold` یک
     * واحد کم می‌آورد: کاراکتری که دقیقاً روی نصف HP می‌ایستاد، پنالتی -2 (و DV
     * پایدارسازی 13، و رنگ هشدار در Combat) را نمی‌گرفت.
     */
    @Test
    fun seriouslyWounded_includesExactlyHalfHp() {
        val maxHp = 30
        assertEquals(15, GameRules.seriouslyWoundedThreshold(maxHp))
        assertTrue("دقیقاً روی نصف HP باید Seriously Wounded باشد", GameRules.isSeriouslyWounded(15, maxHp))
        assertTrue("زیر نصف هم باید باشد", GameRules.isSeriouslyWounded(14, maxHp))
        assertFalse("یک بالای نصف نباید باشد", GameRules.isSeriouslyWounded(16, maxHp))
        assertFalse("HP صفر «Mortally Wounded» است نه «Seriously Wounded»", GameRules.isSeriouslyWounded(0, maxHp))
        assertFalse("HP کامل شامل نمی‌شود", GameRules.isSeriouslyWounded(maxHp, maxHp))
    }

    /** HP فرد (۲۵) → آستانه گرد به بالا می‌شود ۱۳؛ پنالتی باید از همان ۱۳ شروع شود. */
    @Test
    fun seriouslyWoundedThreshold_roundsUpForOddMaxHp() {
        assertEquals(13, GameRules.seriouslyWoundedThreshold(25))
        assertTrue(GameRules.isSeriouslyWounded(13, 25))
        assertFalse(GameRules.isSeriouslyWounded(14, 25))
    }
}
