package com.cyberpunk.gmtool.data.net

import com.cyberpunk.gmtool.data.Character
import java.lang.reflect.Field

/**
 * مرز اعتماد سشن محلی (LAN).
 * ============================================================
 * مسئله‌ای که این فایل حل می‌کند:
 * گوشی بازیکن همیشه **کل برگه‌ی پیشنهادی** خودش را می‌فرستد (`ChangeRequest.characterJson`)
 * و سمت GM قبلاً همان برگه را بی‌کم‌وکاست روی نسخه‌ی خودش می‌نشاند. یعنی یک کلاینت
 * دستکاری‌شده (یا یک باگ ساده در گوشی بازیکن) می‌توانست هر فیلدی را عوض کند —
 * حتی فیلدهایی که در اختیار GM و قواعد هستند: `isDead`، `criticalInjuries`،
 * `deathSavePenalty`، یادداشت GM، یا طبقه‌بندی NPC.
 *
 * قاعده‌ی جدید، در سه لایه:
 *  ۱. [sanitizeForPlayer] — فیلدهای فقط-GM هرگز به گوشی بازیکن **فرستاده نمی‌شوند**.
 *  ۲. [guardedMerge] — فیلدهای فقط-GM هرگز از گوشی بازیکن **پذیرفته نمی‌شوند**؛
 *     بقیه‌ی تغییرات با یک diff خوانا و قابل اتکا به GM نشان داده می‌شود تا
 *     تأیید او واقعاً آگاهانه باشد (نه بر اساس خلاصه‌ای که خودِ کلاینت نوشته).
 *  ۳. [diffFields] — diff سمت سرور ساخته می‌شود، پس کلاینت نمی‌تواند خلاصه را
 *     خوش‌خط‌تر از واقعیت نشان دهد.
 *
 * چرا reflection: مدل [Character] حدود ۶۰ فیلد دارد و مدام فیلد تازه می‌گیرد.
 * اگر diff را دستی می‌نوشتیم، هر فیلد جدید یک حفره‌ی تازه در برگه‌ی تأیید GM
 * بود (دقیقاً همان دسته باگی که `persist()` قبلاً خورده بود). با reflection
 * هر فیلد جدید خودبه‌خود در diff دیده می‌شود.
 *
 * نکته‌ی R8: فیلدهای `com.cyberpunk.gmtool.data.**` در `keepRules/rules.keep`
 * نگه داشته شده‌اند، پس reflection در نسخه‌ی release هم سالم می‌ماند.
 */
object LanTrust {

    /**
     * فیلدهایی که مالکیتشان با GM/قواعد است، نه بازیکن.
     * این فهرست هم برای «نفرستادن» استفاده می‌شود و هم برای «نپذیرفتن».
     */
    private val GM_ONLY_FIELDS: Set<String> = setOf(
        "id",                     // هویت برگه — همیشه از سمت GM تعیین می‌شود
        "isDead",                 // مرگ یک وضعیت قواعدی است، نه یک ویرایش
        "deathSavePenalty",       // انباشته‌ی قواعدی سمت GM
        "criticalInjuries",       // فقط در resolveDamage/درمان سمت GM تغییر می‌کند
        "notes",                  // یادداشت GM روی برگه (به‌ویژه NPCها)
        "npcCategory",            // طبقه‌بندی NPC — اطلاعات GM
        "npcTier",                // درجه‌ی سختی NPC — اطلاعات GM
        "isAlly",                 // مرز NPC/بازیکن؛ نباید از کلاینت برگردد
        "appliedTransactionIds",  // دفترچه‌ی idempotency پاداش‌ها؛ بازنویسی‌اش یعنی پاداش تکراری
        "baseEmp",                // پایه‌های زمان ساخت شخصیت
        "baseBody",
        "reputation",             // REP را GM اهدا می‌کند
        "sheetUid"                // هویت برگه؛ عوض‌شدنش یعنی ساختن یک شخصیت موازی
    )

    /** همان فهرست، بدون `id` — چون `id` همیشه بازنویسی می‌شود و گزارشش نویز است. */
    private val REPORTABLE_GM_ONLY: Set<String> = GM_ONLY_FIELDS - "id"

    // ───────────────────────── لایه‌ی ۱: خروجی ─────────────────────────

    /**
     * برگه‌ای که به گوشی بازیکن می‌رود.
     * اطلاعات مخفی GM حذف می‌شوند — نه اینکه فرستاده شوند و «مخفی بمانند».
     */
    fun sanitizeForPlayer(c: Character): Character = c.copy(
        notes = "",
        npcCategory = "",
        npcTier = ""
    )

    /**
     * برگه‌ی «بازیکن جدید» که برای اولین بار از شبکه می‌آید.
     * وضعیت‌های قواعدیِ سمت GM صفر می‌شوند تا یک کلاینت نتواند با برگه‌ی
     * از پیش ساخته‌شده، مثلاً با `criticalInjuries` خالی یا REP بالا، وارد شود.
     */
    // توجه: sheetUid عمداً اینجا صفر نمی‌شود — برگه‌ی بازیکن جدید باید همان uid
    // خودش را نگه دارد تا گوشی بازیکن و گوشی GM از همان لحظه‌ی اول بر سر هویت
    // برگه توافق داشته باشند و اسکن‌های بعدی شخصیت تکراری نسازند.
    fun forNewPlayer(c: Character): Character = c.copy(
        id = 0,
        isDead = false,
        deathSavePenalty = 0,
        criticalInjuries = emptyList(),
        notes = "",
        npcCategory = "",
        npcTier = "",
        isAlly = true,
        appliedTransactionIds = emptyList(),
        reputation = 0
    )

    // ───────────────────────── لایه‌ی ۲: ورودی ─────────────────────────

    data class MergeResult(
        /** برگه‌ای که واقعاً روی گوشی GM اعمال می‌شود. */
        val character: Character,
        /** تغییراتی که پذیرفته شدند — همان چیزی که GM تأیید می‌کند. */
        val accepted: List<String>,
        /** تغییراتی که کلاینت خواست ولی رد شدند (فیلدهای فقط-GM). */
        val blocked: List<String>
    )

    /**
     * پیشنهاد بازیکن را روی برگه‌ی فعلی GM می‌نشاند، ولی فیلدهای فقط-GM را
     * از نسخه‌ی GM برمی‌گرداند. اگر [current] در دسترس نباشد (حالت غیرعادی)،
     * فقط فیلدهای GM-Only پاک‌سازی می‌شوند.
     */
    fun guardedMerge(current: Character?, proposed: Character): MergeResult {
        if (current == null) {
            val cleaned = forNewPlayer(proposed)
            return MergeResult(cleaned, diffFields(proposed, cleaned), emptyList())
        }
        val merged = proposed.copy(
            id = current.id,
            isDead = current.isDead,
            deathSavePenalty = current.deathSavePenalty,
            criticalInjuries = current.criticalInjuries,
            notes = current.notes,
            npcCategory = current.npcCategory,
            npcTier = current.npcTier,
            isAlly = current.isAlly,
            appliedTransactionIds = current.appliedTransactionIds,
            baseEmp = current.baseEmp,
            baseBody = current.baseBody,
            reputation = current.reputation,
            sheetUid = current.sheetUid
        )
        return MergeResult(
            character = merged,
            accepted = diffFields(current, merged),
            blocked = blockedFields(current, proposed)
        )
    }

    /** کدام فیلدهای فقط-GM را کلاینت سعی کرده بود عوض کند؟ */
    fun blockedFields(current: Character, proposed: Character): List<String> =
        REPORTABLE_GM_ONLY.filter { name ->
            val f = field(name) ?: return@filter false
            read(current, f) != read(proposed, f)
        }.sorted()

    // ───────────────────────── لایه‌ی ۳: diff ─────────────────────────

    /** برچسب‌های کوتاه برای فیلدهای پرکاربرد؛ بقیه با همان نام فیلد می‌آیند. */
    private val LABELS: Map<String, String> = mapOf(
        "eurodollars" to "eb",
        "improvementPoints" to "IP",
        "currentHumanity" to "Humanity",
        "maxHumanity" to "Max Humanity",
        "currentLuck" to "Luck",
        "fashionBudget" to "Fashion eb",
        "roleRank" to "Role Rank",
        "headArmorSp" to "Head SP",
        "bodyArmorSp" to "Body SP",
        "monthlyRent" to "Rent"
    )

    private fun label(fieldName: String): String = LABELS[fieldName] ?: fieldName

    /**
     * نام همه‌ی فیلدهایی که در diff دیده می‌شوند.
     *
     * عمدتاً برای تست و دیباگ: `LanTrustTest` با این فهرست مطمئن می‌شود که
     * **هر** فیلد [Character] زیر نظر است، پس اگر فردا فیلد تازه‌ای اضافه شود
     * نمی‌تواند بی‌صدا از برگه‌ی تأیید GM رد شود.
     */
    fun trackedFieldNames(): List<String> = characterFields().map { it.name }

    /**
     * تفاوت دو برگه به شکل خط‌های خوانا.
     *
     * برای مجموعه‌ها (inventory، weapons، skills، …) به‌جای «۳ ← ۵ مورد» نام
     * آیتم‌های اضافه/حذف/تغییرکرده نشان داده می‌شود، چون GM دقیقاً همان را
     * لازم دارد ببیند: کدام آیتم وارد برگه شد.
     */
    fun diffFields(before: Character, after: Character): List<String> {
        val out = mutableListOf<String>()
        for (f in characterFields()) {
            val b = read(before, f)
            val a = read(after, f)
            if (b == a) continue
            out += "${label(f.name)}: ${describe(b, a)}"
        }
        return out
    }

    private fun describe(before: Any?, after: Any?): String = when {
        before is List<*> && after is List<*> -> describeList(before, after)
        before is Map<*, *> && after is Map<*, *> -> describeMap(before, after)
        before is Set<*> && after is Set<*> -> describeList(before.toList(), after.toList())
        else -> "${short(before)} ← ${short(after)}"
    }

    private fun describeList(before: List<*>, after: List<*>): String {
        fun nameOf(item: Any?): String? = property(item, "name")?.toString()

        // اگر همه‌ی عضوها نام دارند (InventoryItem، Weapon، SkillData، …) diff را
        // بر اساس نام می‌سازیم تا GM ببیند **کدام** آیتم وارد برگه شد؛ وگرنه
        // (رشته‌های ساده، عضوهای بی‌نام یا نام تکراری) فقط اندازه‌ها.
        val kb: Map<String, Any?> = before.mapNotNull { item -> nameOf(item)?.let { it to item } }.toMap()
        val ka: Map<String, Any?> = after.mapNotNull { item -> nameOf(item)?.let { it to item } }.toMap()
        if (kb.size != before.size || ka.size != after.size) {
            return "${before.size} ← ${after.size} مورد"
        }

        val added = ka.keys - kb.keys
        val removed = kb.keys - ka.keys
        val changed = ka.keys.intersect(kb.keys).filter { kb[it] != ka[it] }

        val parts = mutableListOf<String>()
        if (added.isNotEmpty()) parts += "+${added.size} ${names(added)}"
        if (removed.isNotEmpty()) parts += "-${removed.size} ${names(removed)}"
        if (changed.isNotEmpty()) parts += "~${changed.size} ${names(changed)}"
        return if (parts.isEmpty()) "${before.size} ← ${after.size} مورد" else parts.joinToString(" • ")
    }

    private fun describeMap(before: Map<*, *>, after: Map<*, *>): String {
        val keys = (before.keys + after.keys).distinct()
        val changed = keys.filter { before[it] != after[it] }
        return if (changed.isEmpty()) "${before.size} ← ${after.size}"
        else "${changed.take(3).joinToString("، ")}${if (changed.size > 3) " +${changed.size - 3}" else ""}"
    }

    private fun names(keys: Collection<String>): String =
        keys.take(3).joinToString("، ").let {
            if (keys.size > 3) "$it +${keys.size - 3}" else it
        }

    private fun short(v: Any?): String = when (v) {
        null -> "—"
        is String -> if (v.length > 40) "«${v.take(37)}…»" else "«$v»"
        is List<*> -> "${v.size} مورد"
        is Map<*, *> -> "${v.size} مورد"
        is Set<*> -> "${v.size} مورد"
        else -> v.toString()
    }

    // ───────────────────────── reflection ─────────────────────────

    private val fieldsCache: List<Field> by lazy {
        Character::class.java.declaredFields
            .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .onEach { it.isAccessible = true }
    }

    private fun characterFields(): List<Field> = fieldsCache

    private fun field(name: String): Field? = fieldsCache.firstOrNull { it.name == name }

    private fun read(target: Any?, f: Field): Any? =
        if (target == null) null else runCatching { f.get(target) }.getOrNull()

    /** خواندن یک ویژگی از عضو یک مجموعه (مثلاً `name` از InventoryItem). */
    private fun property(item: Any?, name: String): Any? {
        if (item == null) return null
        return runCatching {
            val f = item.javaClass.getDeclaredField(name)
            f.isAccessible = true
            f.get(item)
        }.getOrNull()
    }
}
