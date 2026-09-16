package com.cyberpunk.gmtool.data

import kotlin.random.Random

/**
 * منبع تاس برنامه.
 *
 * مسئله‌ای که این فایل حل می‌کند:
 * تنظیم «تاس خودکار» فقط در دو صفحه بررسی می‌شد. بقیه‌ی برنامه — به‌خصوص تب
 * Combat با ۳۵ نقطه‌ی تاس — مستقیم `Random` را صدا می‌زد، پس خاموش کردن آن
 * گزینه عملاً هیچ اثری نداشت.
 *
 * راه‌حل: همه‌ی تاس‌های قواعد از همین‌جا رد می‌شوند. وقتی GM حالت دستی را
 * انتخاب کند، UI یک [ManualDiceProvider] اینجا نصب می‌کند و هر درخواست تاس
 * به‌جای عدد تصادفی، منتظر عددی می‌ماند که GM از تاس فیزیکی وارد می‌کند.
 *
 * چرا سراسری و نه تزریق پارامتر: تزریق به ۳۵ محل یعنی ۳۵ فرصت برای فراموش
 * کردن یکی. این‌طور هیچ مسیری از قلم نمی‌افتد.
 *
 * ── دو قراردادِ اجرایی که رعایتشان با فراخوان است ─────────────────────────
 * ۱. **هیچ تاس قواعدی نباید بیرون از این فایل ساخته شود.** نه `Random.nextInt`،
 *    نه `(1..10).random()`. تستِ `DiceSourceEnforcementTest` این را روی کل
 *    سورس چک می‌کند تا قاعده با کامنت باقی نماند.
 * ۲. **درخواست تاس نباید روی نخ اصلی بماند.** میزبان دستی ([ManualDiceProvider])
 *    نمی‌تواند نخ اصلی را بلاک کند (فریز کامل + دیالوگی که هیچ‌وقت رسم نمی‌شود)،
 *    پس در آن حالت به تصادفی برمی‌گردد. هر عملیات قواعدی که تاس می‌ریزد باید از
 *    نخ پس‌زمینه اجرا شود؛ الگوی UI: `scope.launch(Dispatchers.Default) { ... }`.
 *    اگر کسی این را رعایت نکند، برنامه ساکت نمی‌ماند: [onManualFallback] صدا
 *    زده می‌شود و UI به GM هشدار می‌دهد که آن تاس تصادفی زده شد.
 */
object DiceSource {

    /** یک درخواست تاس که منتظر ورودی GM است. */
    data class Request(
        val count: Int,
        val sides: Int,
        /** توضیح کوتاه که چرا این تاس لازم است — روی دیالوگ نمایش داده می‌شود. */
        val label: String
    ) {
        val expression: String get() = "${count}d$sides"
    }

    /**
     * وقتی مقدار داشته باشد یعنی حالت دستی فعال است.
     * UI این را ست می‌کند؛ لایه‌ی قواعد فقط صدایش می‌زند.
     */
    @Volatile
    var manual: ((Request) -> List<Int>)? = null

    /**
     * وقتی در حالت دستی، درخواست به عدد واقعی نرسید و برنامه ناچار شد تصادفی
     * بریزد، این صدا زده می‌شود (نخ اصلی، انصراف GM، یا پایان مهلت).
     *
     * چرا لازم است: قاعده این است که «در حالت تاس دستی هیچ عددی از خود برنامه
     * نمی‌آید». اگر جایی این قاعده نقض شود، باید سر میز **دیده** شود، نه اینکه
     * بی‌صدا یک عدد ساختگی وارد قواعد شود.
     */
    @Volatile
    var onManualFallback: ((Request) -> Unit)? = null

    /** برچسبی که به تاس بعدی نسبت داده می‌شود (برای اینکه GM بداند چه می‌ریزد). */
    @Volatile
    private var pendingLabel: String = ""

    /** برچسب تاس بعدی را تعیین می‌کند. در حالت خودکار هیچ اثری ندارد. */
    fun label(text: String) {
        pendingLabel = text
    }

    private fun takeLabel(): String {
        val l = pendingLabel
        pendingLabel = ""
        return l
    }

    /** آیا الان حالت دستی است؟ */
    val isManual: Boolean get() = manual != null

    /**
     * ویبره هنگام تاس خودکار.
     * UI این را وصل می‌کند؛ لایه‌ی قواعد نمی‌داند ویبره چیست و نباید بداند.
     * اگر تنظیم ویبره خاموش باشد، UI اصلاً چیزی وصل نمی‌کند.
     */
    @Volatile
    var onAutoRoll: (() -> Unit)? = null

    /**
     * [count] تاس [sides] وجهی می‌ریزد.
     * در حالت خودکار تصادفی، در حالت دستی از GM می‌پرسد.
     */
    fun roll(count: Int, sides: Int, label: String = ""): List<Int> {
        if (count <= 0) return emptyList()
        // محافظ: d0/d1 معنا ندارد و Random.nextInt(1, sides + 1) با sides < 1
        // استثنا می‌داد (رشته‌ی آسیب ناقص یا جدول خالی می‌توانست صفحه را ببندد).
        // درخواست دستی دست‌نخورده رد می‌شود تا قاعده‌ی «هیچ تاسی بی‌صدا» نشکند.
        val faces = if (sides < 1) 1 else sides
        val handler = manual
        if (handler != null) {
            val request = Request(count, faces, label.ifBlank { takeLabel() })
            val values = handler(request)
            // اگر GM دیالوگ را ببندد، به تصادفی برنمی‌گردیم تا نتیجه‌ی ساختگی نسازیم.
            if (values.size == count) return values.map { it.coerceIn(1, faces) }
            // به عدد نرسید: به UI خبر می‌دهیم و بعد تصادفی می‌ریزیم تا بازی نخوابد.
            onManualFallback?.invoke(request)
        }
        takeLabel()
        // ویبره فقط برای تاس خودکار؛ در حالت دستی خودِ دیالوگ ویبره می‌دهد.
        onAutoRoll?.invoke()
        return List(count) { Random.nextInt(1, faces + 1) }
    }

    /** یک تاس. */
    fun rollOne(sides: Int, label: String = ""): Int = roll(1, sides, label).firstOrNull() ?: 1
}
