package com.cyberpunk.gmtool.data.net

import com.cyberpunk.gmtool.data.gtr
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * پروتکل سشن محلی (LAN).
 *
 * گوشی GM یک سرور کوچک HTTP اجرا می‌کند؛ گوشی بازیکن‌ها به آن وصل می‌شوند.
 * هیچ اینترنتی لازم نیست — فقط هات‌اسپات یا وای‌فای مشترک.
 *
 * قاعده‌ی ثابت: **گوشی GM همیشه مرجع حقیقت است.**
 */
object LanProtocol {
    const val PORT = 8477
    const val PROTOCOL_VERSION = 1

    val gson: Gson = Gson()

    // مسیرها
    const val PATH_INFO = "/info"
    const val PATH_JOIN = "/join"
    const val PATH_POLL = "/poll"
    const val PATH_REQUEST = "/request"
    const val PATH_BYE = "/bye"
}

// ─────────────────────────── مدل‌های انتقال ───────────────────────────

/** آگهی سشن — بدون احراز هویت خوانده می‌شود تا کلاینت مطمئن شود سرور درست است. */
data class LanInfo(
    val protocol: Int = LanProtocol.PROTOCOL_VERSION,
    val sessionName: String = "",
    val gmName: String = "",
    val open: Boolean = true
)

/**
 * محتوای QR / کد دستی.
 * هر شخصیت کد مخصوص خودش را دارد؛ [slotId] تعیین می‌کند بازیکن به کدام نقش وصل می‌شود.
 * [slotId] برابر `NEW_PLAYER_SLOT` یعنی «بازیکن جدید».
 */
data class LanTicket(
    val host: String = "",
    val port: Int = LanProtocol.PORT,
    val sessionId: Long = 0L,
    val slotId: String = "",
    val code: String = "",
    val label: String = ""
) {
    companion object {
        /**
         * پیشوند slotId برای «بازیکن جدید».
         *
         * قبلاً فقط یک slot ثابت «NEW» وجود داشت، یعنی میز نمی‌توانست دو بازیکن
         * جدید هم‌زمان داشته باشد. حالا هر ورودِ تازه slot مخصوص خودش را
         * می‌گیرد: NEW1، NEW2، … بدون سقف. `startsWith` بودنِ بررسی یعنی
         * بلیت‌های قدیمیِ «NEW» هم هنوز معتبرند.
         */
        const val NEW_PLAYER_PREFIX = "NEW"
        const val NEW_PLAYER_SLOT = NEW_PLAYER_PREFIX
        fun isNewPlayerSlot(slotId: String): Boolean = slotId.startsWith(NEW_PLAYER_PREFIX)
        fun encode(t: LanTicket): String = LanProtocol.gson.toJson(t)
        fun decode(raw: String): LanTicket? = runCatching {
            LanProtocol.gson.fromJson(raw.trim(), LanTicket::class.java)
        }.getOrNull()
    }
}

/** درخواست پیوستن. اگر بازیکن برگه‌ی خودش را داشته باشد در [characterJson] می‌فرستد. */
data class JoinRequest(
    val code: String = "",
    val slotId: String = "",
    val deviceKey: String = "",
    val playerName: String = "",
    val characterJson: String? = null,
    val protocol: Int = LanProtocol.PROTOCOL_VERSION
)

data class JoinResponse(
    val ok: Boolean = false,
    val error: String = "",
    /** کلید ماندگار برای اتصال مجدد بدون اسکن دوباره. */
    val token: String = "",
    val characterId: Int = 0,
    val characterName: String = "",
    /** برگه‌ی کامل — وقتی بازیکن آن را ندارد یا نسخه‌ی GM برنده شده. */
    val characterJson: String = "",
    /** true یعنی GM هنوز باید تأیید کند (بازیکن جدید یا تعارض نسخه). */
    val pending: Boolean = false,
    val sessionName: String = ""
)

/** رویدادی که از GM به بازیکن می‌رود. */
data class LanEvent(
    val seq: Long = 0L,
    val type: String = "",
    val payload: String = "",
    val message: String = ""
) {
    companion object {
        /** برگه‌ی کامل شخصیت به‌روز شد. */
        const val CHARACTER = "character"
        /** درخواست بازیکن تأیید شد. */
        const val APPROVED = "approved"
        /** درخواست بازیکن رد شد. */
        const val REJECTED = "rejected"
        /** پیام خصوصی درون‌داستانی از GM. */
        const val WHISPER = "whisper"
        /** سشن بسته شد. */
        const val CLOSED = "closed"
        /** GM هنوز اتصال را تأیید نکرده. */
        const val PENDING = "pending"
    }
}

data class PollResponse(
    val ok: Boolean = false,
    val error: String = "",
    val events: List<LanEvent> = emptyList()
) {
    companion object {
        val listType = object : TypeToken<List<LanEvent>>() {}.type
    }
}

/**
 * درخواست تغییر از سمت بازیکن.
 * بازیکن هرگز مستقیم چیزی را تغییر نمی‌دهد؛ همیشه درخواست می‌دهد و GM تأیید می‌کند.
 */
data class ChangeRequest(
    val token: String = "",
    val kind: String = "",
    val summary: String = "",
    /** برگه‌ی کامل پیشنهادی بازیکن پس از تغییر. */
    val characterJson: String = ""
) {
    companion object {
        const val MONEY = "money"
        const val ITEM_ADD = "item_add"
        const val LOOT = "loot"
        const val IP_SPEND = "ip_spend"
        const val STAT = "stat"
        const val HUMANITY = "humanity"
        const val CONSUME = "consume"
        const val OTHER = "other"
        /**
         * برگه‌ی بازیکن و برگه‌ی GM برای همان شخصیت با هم فرق دارند و GM
         * باید انتخاب کند کدام‌یک منبع حقیقت است. تأیید = نسخه‌ی بازیکن می‌برد،
         * رد = نسخه‌ی GM می‌ماند.
         */
        const val CONFLICT = "conflict"

        /**
         * برچسب نوع درخواست.
         *
         * قبلاً اینجا متن فارسی hardcode بود، پس در حالت English/دوزبانه هم
         * پیام فارسی به بازیکن می‌رفت. کلیدها انگلیسی‌اند و ترجمه‌شان در
         * `assets/i18n/fa.19-lan.json` است.
         */
        fun kindLabel(kind: String): String = when (kind) {
            MONEY -> gtr("Money change")
            ITEM_ADD -> gtr("Add item")
            LOOT -> gtr("Take loot")
            IP_SPEND -> gtr("Spend IP / upgrade")
            STAT -> gtr("Stat or skill change")
            HUMANITY -> gtr("Humanity change")
            CONSUME -> gtr("Consume item")
            CONFLICT -> gtr("Sheet conflict")
            else -> gtr("Change")
        }
    }
}

data class SimpleResponse(val ok: Boolean = false, val error: String = "")

// ─────────────────────────── وضعیت سمت GM ───────────────────────────

/** یک نقش در سشن — چه وصل باشد چه نباشد. */
data class LanSlot(
    val slotId: String,
    val characterId: Int,
    val label: String,
    val code: String,
    /** تا وقتی false باشد این شخصیت اصلاً درگیر شبکه نیست و GM دستی می‌گرداندش. */
    val networked: Boolean = false,
    val connected: Boolean = false,
    val deviceKey: String = "",
    val lastSeen: Long = 0L
)

/** درخواست در انتظار تأیید GM. */
data class PendingRequest(
    val id: Long = System.nanoTime(),
    val slotId: String = "",
    val characterId: Int = 0,
    val characterName: String = "",
    val kind: String = "",
    /** خلاصه‌ای که خودِ کلاینت نوشته — فقط راهنماست، مبنای تأیید نیست. */
    val summary: String = "",
    val characterJson: String = "",
    /** true یعنی درخواست پیوستنِ بازیکن جدید، نه تغییر برگه. */
    val isJoin: Boolean = false,
    val at: Long = System.currentTimeMillis(),
    /**
     * تفاوت واقعی دو برگه که **سمت GM** محاسبه شده (`LanTrust.diffFields`).
     * چون کلاینت نمی‌تواند این فهرست را بنویسد، GM چیزی را تأیید می‌کند که
     * واقعاً اعمال می‌شود — نه آنچه بازیکن گفته می‌شود.
     */
    val diff: List<String> = emptyList(),
    /** فیلدهای فقط-GM که کلاینت سعی کرد عوض کند و نادیده گرفته شدند. */
    val blocked: List<String> = emptyList()
)

enum class LanRole { NONE, GM, PLAYER }

enum class LanConnState { OFFLINE, STARTING, ONLINE, RECONNECTING, ERROR }
