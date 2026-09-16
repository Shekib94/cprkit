package com.cyberpunk.gmtool.data.net

import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.InventoryItem
import com.cyberpunk.gmtool.data.SkillData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های مرز اعتماد سشن محلی.
 *
 * چرا این فایل وجود دارد:
 * «GM مرجع حقیقت است» تا وقتی درست است که **کد** هم همان را اجرا کند. قبلاً
 * `LanHost.sanitize()` برابر `= c` بود و `approve()` کل برگه‌ی پیشنهادی بازیکن
 * را بی‌کم‌وکاست روی نسخه‌ی GM می‌نشاند؛ یعنی آن قاعده فقط یک جمله در کامنت
 * بود. این تست‌ها همان جمله را به یک invariant قابل اندازه‌گیری تبدیل می‌کنند.
 *
 * اجرا:  ./gradlew test
 */
class LanTrustTest {

    /** برگه‌ای که روی گوشی GM است؛ شامل اطلاعات و وضعیت‌های فقط-GM. */
    private fun gmSheet() = Character(
        id = 7,
        name = "Rookie",
        handle = "Static",
        eurodollars = 320,
        hp = 30,
        maxHp = 40,
        notes = "GM only: he owes the Scavs",
        npcCategory = "Gang",
        npcTier = "Hard",
        isAlly = true,
        isDead = false,
        deathSavePenalty = 1,
        criticalInjuries = listOf("broken_leg"),
        appliedTransactionIds = listOf("gig-12-reward"),
        reputation = 3,
        baseEmp = 6,
        baseBody = 6,
        inventory = listOf(InventoryItem("Medtech Kit"), InventoryItem("Stim")),
        skills = listOf(SkillData("Handgun", "Reflexes", 4))
    )

    // ─────────────────── لایه‌ی خروجی ───────────────────

    /** اطلاعات GM هیچ‌وقت به گوشی بازیکن نمی‌رود — نه اینکه برود و پنهان شود. */
    @Test
    fun sanitize_stripsGmOnlyFieldsBeforeSendingToPlayer() {
        val out = LanTrust.sanitizeForPlayer(gmSheet())

        assertEquals("یادداشت GM نباید ارسال شود", "", out.notes)
        assertEquals("طبقه‌بندی NPC اطلاعات GM است", "", out.npcCategory)
        assertEquals("درجه‌ی سختی NPC اطلاعات GM است", "", out.npcTier)

        // بقیه‌ی برگه متعلق به بازیکن است و باید دست‌نخورده برسد.
        assertEquals(320, out.eurodollars)
        assertEquals(30, out.hp)
        assertEquals(listOf("broken_leg"), out.criticalInjuries)
        assertEquals(2, out.inventory.size)
    }

    /** برگه‌ی «بازیکن جدید» نمی‌تواند با وضعیت قواعدیِ ساخته‌شده وارد شود. */
    @Test
    fun forNewPlayer_clearsRulesStateButKeepsCreationSheet() {
        val incoming = Character(
            id = 99, isDead = true, deathSavePenalty = 4,
            criticalInjuries = listOf("lost_eye", "spinal_injury"),
            notes = "x", npcCategory = "Boss", npcTier = "Very Hard",
            isAlly = false, appliedTransactionIds = listOf("fake-reward"),
            reputation = 8, eurodollars = 500
        )

        val cleaned = LanTrust.forNewPlayer(incoming)

        assertEquals(0, cleaned.id)
        assertFalse(cleaned.isDead)
        assertEquals(0, cleaned.deathSavePenalty)
        assertTrue(cleaned.criticalInjuries.isEmpty())
        assertTrue(cleaned.isAlly)
        assertEquals(0, cleaned.reputation)
        assertTrue(cleaned.appliedTransactionIds.isEmpty())
        assertEquals("", cleaned.notes)
        // پول و ساخت شخصیت حذف نمی‌شود؛ فقط وضعیت‌های GM-Only صفر می‌شوند.
        assertEquals(500, cleaned.eurodollars)
    }

    // ─────────────────── لایه‌ی ورودی ───────────────────

    /** تغییرات متعلق به بازیکن باید عبور کنند، وگرنه بازی عملاً قفل می‌شود. */
    @Test
    fun guardedMerge_acceptsPlayerOwnedChanges() {
        val current = gmSheet()
        val proposed = current.copy(
            eurodollars = 120,
            inventory = current.inventory + InventoryItem("Antibiotic"),
            hp = 22
        )

        val merged = LanTrust.guardedMerge(current, proposed)

        assertEquals(120, merged.character.eurodollars)
        assertEquals(3, merged.character.inventory.size)
        assertEquals(22, merged.character.hp)
        assertEquals(7, merged.character.id)
        assertTrue("هیچ فیلدی نباید بی‌دلیل مسدود شود", merged.blocked.isEmpty())
        assertTrue(merged.accepted.isNotEmpty())
    }

    /** یک کلاینت دستکاری‌شده نمی‌تواند وضعیت‌های قواعدی/GM را عوض کند. */
    @Test
    fun guardedMerge_blocksGmOnlyFieldsFromClient() {
        val current = gmSheet()
        val hostile = current.copy(
            id = 1,
            eurodollars = 999_999,
            isDead = false,
            deathSavePenalty = 0,
            criticalInjuries = emptyList(),
            notes = "hacked",
            isAlly = false,
            reputation = 10,
            appliedTransactionIds = emptyList(),
            baseEmp = 10,
            baseBody = 10
        )

        val merged = LanTrust.guardedMerge(current, hostile)

        // پول پذیرفته می‌شود: قانونش «تأیید GM» است، نه «غیرقابل تغییر».
        assertEquals(999_999, merged.character.eurodollars)

        // ولی این‌ها هرگز از کلاینت نمی‌آیند.
        assertEquals(7, merged.character.id)
        assertEquals(1, merged.character.deathSavePenalty)
        assertEquals(listOf("broken_leg"), merged.character.criticalInjuries)
        assertEquals("GM only: he owes the Scavs", merged.character.notes)
        assertTrue(merged.character.isAlly)
        assertEquals(3, merged.character.reputation)
        assertEquals(listOf("gig-12-reward"), merged.character.appliedTransactionIds)
        assertEquals(6, merged.character.baseEmp)
        assertEquals(6, merged.character.baseBody)

        // و GM باید ببیند که کلاینت چه چیزی را خواسته بود.
        assertTrue(merged.blocked.containsAll(
            listOf(
                "appliedTransactionIds", "baseBody", "baseEmp",
                "criticalInjuries", "deathSavePenalty", "isAlly", "notes", "reputation"
            )
        ))
        assertFalse("`id` همیشه بازنویسی می‌شود؛ گزارشش نویز است", merged.blocked.contains("id"))
    }

    /** اگر برگه‌ی فعلی GM در دسترس نباشد، پیشنهاد مثل «بازیکن جدید» پاک‌سازی می‌شود. */
    @Test
    fun guardedMerge_withoutCurrentSheet_stillStripsGmState() {
        val merged = LanTrust.guardedMerge(null, gmSheet())

        assertEquals(0, merged.character.id)
        assertEquals("", merged.character.notes)
        assertTrue(merged.character.criticalInjuries.isEmpty())
        assertEquals(0, merged.character.deathSavePenalty)
    }

    // ─────────────────── لایه‌ی diff ───────────────────

    /** diff باید نام آیتم‌ها را نشان دهد، نه فقط «۲ ← ۳ مورد». */
    @Test
    fun diff_namesAddedAndRemovedItems() {
        val current = gmSheet()
        val after = current.copy(
            inventory = listOf(InventoryItem("Medtech Kit"), InventoryItem("Antibiotic"))
        )

        val line = LanTrust.diffFields(current, after).first { it.startsWith("inventory") }

        assertTrue("آیتم تازه باید نام‌گذاری شود: $line", line.contains("Antibiotic"))
        assertTrue("آیتم حذف‌شده باید نام‌گذاری شود: $line", line.contains("Stim"))
    }

    /** تغییر سطح مهارت باید دیده شود — همان چیزی که GM برای خرج IP لازم دارد. */
    @Test
    fun diff_showsWhichSkillChanged() {
        val current = gmSheet()
        val after = current.copy(skills = listOf(SkillData("Handgun", "Reflexes", 6)))

        val line = LanTrust.diffFields(current, after).first { it.startsWith("skills") }

        assertTrue("نام مهارت باید در diff بیاید: $line", line.contains("Handgun"))
    }

    /** هر فیلد تغییریافته دقیقاً یک خط diff می‌دهد؛ برگه‌ی یکسان diff ندارد. */
    @Test
    fun diff_hasOneLinePerChangedField() {
        val before = gmSheet()

        assertEquals(emptyList<String>(), LanTrust.diffFields(before, before))

        val after = before.copy(eurodollars = 500, hp = 12, currentLuck = 3)
        assertEquals(3, LanTrust.diffFields(before, after).size)
    }

    // ─────────────────── هویت برگه (sheetUid) ───────────────────

    /**
     * uid هویت جهانی برگه است؛ اگر کلاینت بتواند عوضش کند، هر اسکن می‌تواند
     * یک شخصیت موازی بسازد. پس merge همیشه نسخه‌ی GM را نگه می‌دارد و
     * تلاش برای تغییرش را گزارش می‌کند.
     */
    @Test
    fun guardedMerge_keepsGmSheetUid_andReportsClientChange() {
        val current = gmSheet().copy(sheetUid = "uid-gm")
        val hostile = current.copy(sheetUid = "uid-fake", eurodollars = 100)

        val merged = LanTrust.guardedMerge(current, hostile)

        assertEquals("uid-gm", merged.character.sheetUid)
        assertTrue("تغییر uid باید به GM گزارش شود", merged.blocked.contains("sheetUid"))
    }

    /** برگه‌ی بازیکن جدید با uid خودش وارد می‌شود تا اسکن‌های بعدی همان را بشناسند. */
    @Test
    fun forNewPlayer_keepsSheetUid() {
        val incoming = gmSheet().copy(id = 99, sheetUid = "uid-player")

        val cleaned = LanTrust.forNewPlayer(incoming)

        assertEquals("uid-player", cleaned.sheetUid)
    }

    /** uid به گوشی بازیکن هم می‌رسد؛ sanitize فقط اسرار GM را برمی‌دارد. */
    @Test
    fun sanitize_keepsSheetUid() {
        val out = LanTrust.sanitizeForPlayer(gmSheet().copy(sheetUid = "uid-gm"))

        assertEquals("uid-gm", out.sheetUid)
    }

    /**
     * مهم‌ترین تست این فایل:
     * diff با reflection روی همه‌ی فیلدهای [Character] ساخته می‌شود، پس فیلد
     * تازه‌ای که فردا اضافه شود **خودبه‌خود** در برگه‌ی تأیید GM دیده می‌شود.
     * اگر روزی کسی فهرست حذف (exclusion) اضافه کرد، این تست می‌شکند.
     */
    @Test
    fun diff_tracksEveryDeclaredFieldOfCharacter() {
        val declared = Character::class.java.declaredFields
            .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertEquals(declared, LanTrust.trackedFieldNames().toSet())
        assertTrue("مدل شخصیت باید بیش از ۴۰ فیلد داشته باشد", declared.size > 40)
    }
}
