package com.cyberpunk.gmtool.data.net

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * سقف‌های سرور محلی.
 *
 * چرا: سرور روی گوشی GM کل سشن را نگه می‌دارد. قبلاً `ByteArray(contentLength)`
 * مستقیم از هدر ساخته می‌شد؛ یک `Content-Length` دستکاری‌شده (مثلاً ۲ میلیارد)
 * یعنی `OutOfMemoryError` و مرگ سرور، یعنی قطع شدن همه‌ی بازیکن‌ها در وسط جلسه.
 * این تست‌ها آن سقف را قفل می‌کنند.
 *
 * اجرا:  ./gradlew test
 */
class TinyHttpLimitsTest {

    @Test
    fun acceptsOrdinaryRequestBodies() {
        assertTrue(TinyHttpServer.isAcceptableBodyLength(0))
        assertTrue(TinyHttpServer.isAcceptableBodyLength(1_024))
        // یک برگه‌ی کامل شخصیت در عمل زیر ۱۰۰KB است.
        assertTrue(TinyHttpServer.isAcceptableBodyLength(100_000))
        assertTrue(TinyHttpServer.isAcceptableBodyLength(TinyHttpServer.MAX_BODY_BYTES))
    }

    @Test
    fun rejectsHostileOrBrokenLengths() {
        assertFalse("طول منفی یعنی هدر خراب", TinyHttpServer.isAcceptableBodyLength(-1))
        assertFalse(TinyHttpServer.isAcceptableBodyLength(TinyHttpServer.MAX_BODY_BYTES + 1))
        assertFalse(TinyHttpServer.isAcceptableBodyLength(Int.MAX_VALUE))
    }

    /**
     * سقف باید آن‌قدر کوچک بماند که حتی چند درخواست هم‌زمانِ بزرگ هم نتوانند
     * حافظه‌ی گوشی را تمام کنند. اگر روزی به body بزرگ‌تری نیاز شد، باید
     * جریان‌خوانی (streaming) اضافه شود، نه فقط بزرگ‌کردن این عدد.
     */
    @Test
    fun bodyLimitStaysWithinPhoneMemoryBudget() {
        val maxConcurrent = 24
        val worstCaseBytes = maxConcurrent.toLong() * TinyHttpServer.MAX_BODY_BYTES
        assertTrue(
            "بدترین حالت ${(worstCaseBytes / 1024 / 1024)}MB می‌شود؛ سقف را کوچک‌تر نگه دار",
            worstCaseBytes <= 96L * 1024 * 1024
        )
    }
}
