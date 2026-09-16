package com.cyberpunk.gmtool.data.net

import android.util.Log
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * سرور HTTP بسیار کوچک، فقط با سوکت خام.
 * عمداً هیچ کتابخانه‌ی خارجی اضافه نشده تا حجم و ریسک بالا نرود.
 *
 * سه محافظت که بعداً اضافه شدند (چون این سرور روی گوشی GM کل سشن را نگه می‌دارد
 * و مرگش یعنی قطع همه‌ی بازیکن‌ها):
 *  ۱. سقف اندازه‌ی body — `Content-Length` بزرگ یعنی `OutOfMemoryError` و مرگ سشن.
 *  ۲. سقف تعداد اتصال هم‌زمان — قبلاً استخر ثابت ۸ نخ بود و ۸ کلاینتِ کند
 *     می‌توانستند تا ۴۰ ثانیه کل سرور را قفل کنند.
 *  ۳. لاگ — قبلاً همه‌ی استثناها بلعیده می‌شدند و دیباگ روی گوشی ممکن نبود.
 */
class TinyHttpServer(
    private val port: Int,
    private val handler: (path: String, body: String) -> String
) {
    private var server: ServerSocket? = null
    private val running = AtomicBoolean(false)

    /** نخ‌ها daemon و نام‌دار، برای اینکه در trace های ANR قابل تشخیص باشند. */
    private var pool: ExecutorService = newPool()

    private fun newPool(): ExecutorService = Executors.newCachedThreadPool { r ->
        Thread(r, "tinyhttp-${threadCounter.incrementAndGet()}").apply { isDaemon = true }
    }

    /**
     * سقف اتصال هم‌زمان. بازیکن‌ها هر ~۹۰۰ms یک poll کوتاه می‌زنند، پس حتی
     * ۱۲ بازیکن هم به‌ندرت هم‌زمان در حال درخواست‌اند؛ اتصال اضافی با 503
     * رد می‌شود و کلاینت خودش با backoff برمی‌گردد.
     */
    private val connectionSlots = Semaphore(MAX_CONNECTIONS)

    private var acceptThread: Thread? = null

    val isRunning: Boolean get() = running.get()

    companion object {
        private const val TAG = "TinyHttp"
        /** سقف body درخواست — برگه‌ی کامل شخصیت در عمل زیر ۱۰۰KB است. */
        const val MAX_BODY_BYTES = 2 * 1024 * 1024
        /** سقف هدر — همان سقف قبلی، فقط نام‌گذاری شد. */
        private const val MAX_HEADER_BYTES = 16_384
        private const val MAX_CONNECTIONS = 24
        private const val SOCKET_TIMEOUT_MS = 20_000
        private val threadCounter = AtomicLong(0)

        /**
         * آیا این `Content-Length` قابل پذیرش است؟
         *
         * تابع خالص و در companion، تا بدون ساختن سرور (و بدون سوکت) قابل تست
         * باشد. اگر روزی سقف را عوض کردی، تست `TinyHttpLimitsTest` را هم
         * به‌روز کن — این عدد مرز بین «درخواست بزرگ» و «مرگ سشن با OOM» است.
         */
        fun isAcceptableBodyLength(contentLength: Int): Boolean =
            contentLength in 0..MAX_BODY_BYTES
    }

    fun start() {
        if (running.get()) return
        // اگر قبلاً stop() شده باشد، استخر خاموش است و execute با
        // RejectedExecutionException می‌ترکد؛ پس یک استخر تازه می‌سازیم.
        if (pool.isShutdown) pool = newPool()
        val s = ServerSocket(port)
        s.reuseAddress = true
        server = s
        running.set(true)
        acceptThread = Thread({
            while (running.get()) {
                val client = try {
                    s.accept()
                } catch (e: Exception) {
                    if (running.get()) Log.w(TAG, "accept failed: ${e.message}")
                    if (running.get()) continue else break
                }
                if (!connectionSlots.tryAcquire()) {
                    Log.w(TAG, "connection limit ($MAX_CONNECTIONS) reached; refusing ${client.inetAddress}")
                    runCatching {
                        writeJson(client.getOutputStream(), """{"ok":false,"error":"busy"}""", 503, "Service Unavailable")
                    }
                    runCatching { client.close() }
                    continue
                }
                pool.execute {
                    try {
                        serve(client)
                    } finally {
                        connectionSlots.release()
                    }
                }
            }
        }, "tinyhttp-accept").also { it.isDaemon = true; it.start() }
    }

    /**
     * @param graceMs فرصتی برای تمام‌شدن پاسخ‌های در حال پرواز. از نخ اصلی
     * باید `0` باشد (بلاک‌کردن رابط کاربری ارزشش را ندارد).
     */
    fun stop(graceMs: Long = 0) {
        running.set(false)
        runCatching { server?.close() }
        server = null
        pool.shutdown()
        if (graceMs > 0) {
            try {
                pool.awaitTermination(graceMs, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        runCatching { if (!pool.isTerminated) pool.shutdownNow() }
    }

    private fun serve(client: Socket) {
        try {
            client.soTimeout = SOCKET_TIMEOUT_MS
            val input = BufferedInputStream(client.getInputStream())
            val head = StringBuilder()
            var complete = false
            while (true) {
                val b = input.read()
                if (b < 0) break
                head.append(b.toChar())
                if (head.length >= 4 && head.endsWith("\r\n\r\n")) { complete = true; break }
                if (head.length > MAX_HEADER_BYTES) break
            }
            val out = client.getOutputStream()
            if (!complete) {
                Log.w(TAG, "malformed/truncated request header from ${client.inetAddress}")
                writeJson(out, """{"ok":false,"error":"bad request"}""", 400, "Bad Request")
                return
            }

            val lines = head.toString().split("\r\n")
            val requestLine = lines.firstOrNull().orEmpty()
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                writeJson(out, """{"ok":false,"error":"bad request"}""", 400, "Bad Request")
                return
            }
            val path = parts[1].substringBefore('?')

            var contentLength = 0
            var chunked = false
            for (l in lines.drop(1)) {
                val idx = l.indexOf(':')
                if (idx <= 0) continue
                val name = l.substring(0, idx).trim()
                val value = l.substring(idx + 1).trim()
                when {
                    name.equals("Content-Length", true) -> contentLength = value.toIntOrNull() ?: -1
                    name.equals("Transfer-Encoding", true) && value.contains("chunked", true) -> chunked = true
                }
            }

            // هدرِ بی‌نهایت یا دستکاری‌شده نباید به تخصیص حافظه برسد:
            // `ByteArray(contentLength)` با عدد بزرگ یعنی OOM و مرگ کل سشن.
            if (!isAcceptableBodyLength(contentLength)) {
                Log.w(TAG, "rejecting $path: Content-Length=$contentLength exceeds $MAX_BODY_BYTES")
                writeJson(out, """{"ok":false,"error":"payload too large"}""", 413, "Payload Too Large")
                return
            }
            if (chunked) {
                // پروتکل ما فقط JSON با طول معلوم می‌فرستد؛ chunked پشتیبانی نمی‌شود.
                Log.w(TAG, "rejecting $path: chunked Transfer-Encoding is not supported")
                writeJson(out, """{"ok":false,"error":"unsupported encoding"}""", 501, "Not Implemented")
                return
            }

            val body = if (contentLength > 0) {
                val buf = ByteArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val n = input.read(buf, read, contentLength - read)
                    if (n < 0) break
                    read += n
                }
                String(buf, 0, read, Charsets.UTF_8)
            } else ""

            val response = try {
                handler(path, body)
            } catch (e: Exception) {
                // استثنا دیگر بی‌صدا بلعیده نمی‌شود: بدون این لاگ، پیدا کردن
                // علت یک پاسخ «server» روی گوشی GM عملاً غیرممکن بود.
                Log.e(TAG, "handler failed for $path", e)
                """{"ok":false,"error":"server"}"""
            }
            writeJson(out, response)
        } catch (e: java.net.SocketException) {
            // کلاینت رفته — حالت عادی در شبکه‌ی محلی، فقط برای دیباگ.
            Log.d(TAG, "socket closed by peer: ${e.message}")
        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "timeout while serving ${client.inetAddress}")
        } catch (e: Exception) {
            Log.w(TAG, "unexpected error while serving: ${e.message}")
        } finally {
            runCatching { client.close() }
        }
    }

    private fun writeJson(
        out: OutputStream,
        json: String,
        status: Int = 200,
        reason: String = "OK"
    ) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-Length: ").append(bytes.size).append("\r\n")
            append("Connection: close\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("\r\n")
        }
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }
}

/** کلاینت HTTP کوچک با تایم‌اوت‌های کوتاه، مناسب شبکه‌ی محلی. */
object TinyHttpClient {
    private const val TAG = "TinyHttpClient"

    fun post(host: String, port: Int, path: String, body: String, timeoutMs: Int = 8000): String? {
        return try {
            val url = URL("http://$host:$port$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            conn.disconnect()
            if (code !in 200..299) Log.w(TAG, "POST $path -> HTTP $code")
            text
        } catch (e: Exception) {
            // null برمی‌گردد تا لایه‌ی بالاتر backoff بزند؛ علت فقط لاگ می‌شود.
            Log.d(TAG, "POST $path failed: ${e.message}")
            null
        }
    }

    fun get(host: String, port: Int, path: String, timeoutMs: Int = 6000): String? {
        return try {
            val url = URL("http://$host:$port$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            conn.disconnect()
            if (code !in 200..299) Log.w(TAG, "GET $path -> HTTP $code")
            text
        } catch (e: Exception) {
            Log.d(TAG, "GET $path failed: ${e.message}")
            null
        }
    }
}

object LanAddress {
    /**
     * IP محلی این دستگاه.
     * وقتی هات‌اسپات روشن است معمولاً روی رابط `ap0`/`swlan0` است و
     * آدرسی مثل 192.168.43.1 می‌گیرد.
     */
    fun localIp(): String {
        val candidates = mutableListOf<String>()
        runCatching {
            for (nif in NetworkInterface.getNetworkInterfaces()) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr !is Inet4Address || addr.isLoopbackAddress) continue
                    val ip = addr.hostAddress ?: continue
                    val name = nif.name.lowercase()
                    if (name.startsWith("ap") || name.contains("swlan") || name.contains("wlan1")) {
                        return ip // رابط هات‌اسپات — بالاترین اولویت
                    }
                    candidates.add(ip)
                }
            }
        }
        return candidates.firstOrNull { it.startsWith("192.168.") }
            ?: candidates.firstOrNull { it.startsWith("10.") }
            ?: candidates.firstOrNull()
            ?: "127.0.0.1"
    }
}
