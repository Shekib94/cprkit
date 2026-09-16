package com.cyberpunk.gmtool.data

import android.content.Context

// ذخیره‌ی تنظیمات برنامه (SharedPreferences)
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("cpr_settings", Context.MODE_PRIVATE)

    // تاس خودکار: true = برنامه خودش تاس می‌ریزد؛ false = تاس فیزیکی و ورود دستی عدد
    var autoDice: Boolean
        get() = prefs.safeBoolean(KEY_AUTO_DICE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_DICE, value).apply()

    // لرزش هنگام ریختن تاس
    var hapticsEnabled: Boolean
        get() = prefs.safeBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    /** زبان برنامه: ENGLISH / BILINGUAL (پیش‌فرض) / PERSIAN */
    var appLanguage: AppLanguage
        get() = AppLanguage.fromCode(prefs.safeString(KEY_APP_LANGUAGE, null))
        set(value) { prefs.edit().putString(KEY_APP_LANGUAGE, value.code).apply() }

    // قفل GM برای کتابِ کمپین آماده (هش SHA-256 رمز)
    // رمز پیش‌فرض داخل برنامه پخته شده تا نسخه‌ی پخش‌شده بین بازیکنان هم قفل باشد؛
    // GM می‌تواند بعد از ورود، رمز اختصاصی روی دستگاه خودش بگذارد.
    var gmPinHash: String?
        get() = prefs.safeString(KEY_GM_PIN, null)
        set(value) { prefs.edit().putString(KEY_GM_PIN, value).apply() }

    /** هش مؤثر: رمز اختصاصی دستگاه، وگرنه رمز پیش‌فرض برنامه. */
    fun effectivePinHash(): String = gmPinHash ?: DEFAULT_GM_PIN_HASH

    fun checkPin(pin: String): Boolean = sha256(pin) == effectivePinHash()

    /** آیا دستگاه هنوز روی رمز پیش‌فرض است؟ */
    val usesDefaultPin: Boolean get() = gmPinHash == null

    companion object {
        private const val KEY_AUTO_DICE = "auto_dice_rolls"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_GM_PIN = "gm_campaign_pin_hash"
        private const val KEY_APP_LANGUAGE = "app_language"

        /** رمز پیش‌فرض GM: 1224 — با setPin روی دستگاه خودت عوضش کن. */
        const val DEFAULT_GM_PIN = "1224"
        val DEFAULT_GM_PIN_HASH: String by lazy { sha256(DEFAULT_GM_PIN) }

        /** هش SHA-256 به‌صورت هگز؛ برای ذخیره‌ی رمز GM بدون نگه‌داشتن خود رمز. */
        fun sha256(input: String): String =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
