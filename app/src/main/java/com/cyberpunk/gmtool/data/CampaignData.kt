package com.cyberpunk.gmtool.data

import android.content.Context
import com.google.gson.Gson
import java.io.File

data class CampaignLocation(val id: Long = System.nanoTime(), val name: String = "", val threat: String = "Moderate", val notes: String = "")
data class CampaignFaction(val id: Long = System.nanoTime(), val name: String = "", val attitude: Int = 0, val agenda: String = "")
data class CampaignLoot(
    val name: String = "", val category: String = "Gear", val quantity: Int = 1, val claimedBy: Int? = null,
    val id: String = java.util.UUID.randomUUID().toString()
)
data class CampaignEncounter(
    val id: Long = System.nanoTime(), val title: String = "", val status: String = "PLANNED", val threat: String = "Typical",
    val locationId: Long? = null, val factionIds: List<Long> = emptyList(), val fighterIds: List<Int> = emptyList(),
    val rewardIp: Int = 0, val rewardEb: Int = 0, val rewardRep: Int = 0, val rewardsAwarded: Boolean = false,
    val loot: List<CampaignLoot> = emptyList(), val outcome: String = "", val consequence: String = "", val notes: String = ""
)
data class CampaignSessionHistory(val sessionId: Long, val name: String, val date: String, val playerIds: List<Int>, val totalIp: Int, val totalEb: Int, val totalRep: Int, val summary: String)

/**
 * هدف/پیچش داستانی شخصیِ یک بازیکن — فقط داخل همین کمپین ذخیره می‌شود
 * و به برگهٔ Agent شخصیت نمی‌رود (خواستهٔ صریح GM).
 * [secret] یعنی فقط GM ببیند (پیچش داستانی).
 */
data class CampaignCharacterGoal(
    val id: Long = System.nanoTime(),
    val characterId: Int = 0,
    val title: String = "",
    val detail: String = "",
    val secret: Boolean = false,
    val done: Boolean = false
)

data class Campaign(
    val id: Long = System.currentTimeMillis(), val name: String = "", val active: Boolean = false, val pinHash: String = "",
    val playerIds: List<Int> = emptyList(), val npcIds: List<Int> = emptyList(), val objectives: List<String> = emptyList(),
    val locations: List<CampaignLocation> = emptyList(), val factions: List<CampaignFaction> = emptyList(),
    val encounters: List<CampaignEncounter> = emptyList(), val sessionHistory: List<CampaignSessionHistory> = emptyList(), val notes: String = "",
    val characterGoals: List<CampaignCharacterGoal> = emptyList()
)

/** Crash-safe file-backed campaign store with migration from the old SharedPreferences save. */
class CampaignStore(private val context: Context) {
    private val gson = Gson()
    private val store = AtomicJsonFileStore(File(context.filesDir, "campaigns_v2.json"))
    @Volatile var lastLoadWarning: String? = null
        private set

    fun load(): List<Campaign> = PersistenceRuntime.locked {
        migrateLegacyIfNeeded()
        val result = store.read()
        lastLoadWarning = result.warning
        if (result.text.isNullOrBlank()) return@locked emptyList()
        val primary = runCatching { gson.fromJson(result.text, Array<Campaign>::class.java)?.toList().orEmpty() }
        if (primary.isSuccess) return@locked primary.getOrThrow().map(::normalize)
        val recovered = store.readBackupText()?.let { raw ->
            runCatching { gson.fromJson(raw, Array<Campaign>::class.java)?.toList().orEmpty() }.getOrNull()
        }
        if (recovered != null) {
            lastLoadWarning = "ذخیره کمپین خراب بود؛ نسخه پشتیبان سالم بازیابی و فایل اصلی ترمیم شد."
            store.repairMainFromKnownGoodBackup()
            return@locked recovered.map(::normalize)
        }
        lastLoadWarning = "ذخیره کمپین قابل خواندن نیست؛ فایل خراب برای بازیابی حذف نشده است."
        emptyList()
    }

    fun save(items: List<Campaign>, expectedEpoch: Long? = null): Boolean =
        store.write(gson.toJson(items.distinctBy { it.id }.map(::normalize)), expectedEpoch)

    fun activeId(): Long? = load().firstOrNull { it.active }?.id

    fun recordSession(campaignId: Long?, history: CampaignSessionHistory, expectedEpoch: Long? = null) {
        if (campaignId == null) return
        PersistenceRuntime.locked {
            val all = load()
            save(all.map {
                if (it.id == campaignId) it.copy(sessionHistory = listOf(history) + it.sessionHistory.filterNot { h -> h.sessionId == history.sessionId })
                else it
            }, expectedEpoch)
        }
    }

    fun removeSessionHistory(sessionId: Long, expectedEpoch: Long? = null) = PersistenceRuntime.locked {
        val all = load()
        save(all.map { it.copy(sessionHistory = it.sessionHistory.filterNot { h -> h.sessionId == sessionId }) }, expectedEpoch)
    }

    fun resolveEncounter(campaignId: Long, encounterId: Long, outcome: String, consequence: String = "", expectedEpoch: Long? = null): Boolean = PersistenceRuntime.locked {
        val all = load()
        val changed = all.map { c ->
            if (c.id != campaignId) c else c.copy(encounters = c.encounters.map { e ->
                if (e.id != encounterId) e else e.copy(
                    status = "RESOLVED",
                    outcome = outcome.ifBlank { e.outcome.ifBlank { "Combat resolved" } },
                    consequence = consequence.ifBlank { e.consequence }
                )
            })
        }
        save(changed, expectedEpoch)
    }

    private fun normalize(c: Campaign) = c.copy(
        playerIds = c.playerIds.distinct(),
        npcIds = c.npcIds.distinct(),
        objectives = c.objectives.filter { it.isNotBlank() },
        factions = c.factions.map { it.copy(attitude = it.attitude.coerceIn(-5, 5)) },
        encounters = c.encounters.map { e -> e.copy(
            fighterIds = e.fighterIds.distinct(),
            factionIds = e.factionIds.distinct(),
            loot = e.loot.map { l -> if (l.id.isNullOrBlank()) l.copy(id = java.util.UUID.randomUUID().toString()) else l }
        ) },
        sessionHistory = c.sessionHistory.distinctBy { it.sessionId }
    )

    private fun migrateLegacyIfNeeded() {
        if (store.mainFile().exists()) return
        val prefs = context.getSharedPreferences("gm_campaigns_v1", Context.MODE_PRIVATE)
        val raw = prefs.getString("campaigns", null) ?: return
        runCatching { store.write(raw) }
    }
}

fun shatteredMirrorsCampaign(): Campaign {
    val corp = CampaignFaction(name="Helix Meridian", attitude=-2, agenda="پاک‌کردن رد یک پروژه غیرقانونی و خریدن شاهدها")
    val ncpd = CampaignFaction(name="NCPD Internal Affairs", attitude=0, agenda="رسیدن به مدرک قبل از شرکت و جناح فاسد داخلی")
    val broker = CampaignFaction(name="Glasshouse Brokers", attitude=0, agenda="فروش اطلاعات به بالاترین پیشنهاد")
    val tower = CampaignLocation(name="Corporate Zone — Meridian Annex", threat="Severe", notes="ورودی چندلایه، امنیت فعال، مسیرهای فرار محدود")
    val court = CampaignLocation(name="Civic Center — Evidence Transit", threat="Severe", notes="شاهد زنده و زنجیره نگهداری مدرک هر دو اهمیت دارند")
    val auction = CampaignLocation(name="Upper Marina — Black Auction", threat="Extreme", notes="چند فکشن همزمان؛ تیراندازی ساده بدترین راه‌حل ممکن است")
    return Campaign(name="آینه‌های شکسته — SHATTERED MIRRORS", objectives=listOf("بفهمید چه کسی حافظه شاهد را دستکاری کرده", "مدرک اصلی را قبل از نابودی پیدا کنید", "تصمیم بگیرید حقیقت را بفروشید، منتشر کنید یا دفن کنید"),locations=listOf(tower,court,auction),factions=listOf(corp,ncpd,broker),encounters=listOf(
        CampaignEncounter(title="Session 1 — شاهد طبقه چهل‌ودوم",threat="Dangerous",locationId=tower.id,rewardIp=50,rewardEb=1200,notes="Extraction سخت: شاهد باید زنده بماند. Security Clock، مسیر فرار و هویت تیم مهم‌تر از تعداد کشته‌هاست."),
        CampaignEncounter(title="Session 2 — زنجیره مدرک",threat="Dangerous",locationId=court.id,rewardIp=60,rewardEb=1500,notes="تحقیق + فشار قانونی + کمین. شکست اجتماعی می‌تواند Encounter بعدی را سخت‌تر کند."),
        CampaignEncounter(title="Session 3 — حراج آینه‌ها",threat="Extreme",locationId=auction.id,rewardIp=80,rewardEb=2500,notes="حراج چندطرفه اطلاعات. هر فکشن هدف مستقل دارد؛ نتیجه این Session جهت ادامه کمپین را تعیین می‌کند.")
    ),notes="برای بازیکنان حرفه‌ای. سختی از Objectiveهای همزمان، اطلاعات ناقص، Consequence و دشمنان باهوش می‌آید؛ نه صرفاً HP بیشتر. از نتیجه واقعی Session 3 برای ساخت ادامه کمپین استفاده کن.")
}
