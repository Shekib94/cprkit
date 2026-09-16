package com.cyberpunk.gmtool.data

import android.content.Context
import com.google.gson.Gson
import java.io.File

data class SessionLogEntry(val time: String = "", val text: String = "")
data class PlayerSessionReward(
    val characterId: Int = 0,
    /** Pending reward values for the next milestone/mission payout. */
    val ip: Int = 0,
    val eb: Int = 0,
    val rep: Int = 0,
    /** Legacy one-shot flag retained only for save migration. New rewards never lock the Session. */
    val awarded: Boolean = false,
    /** Number of reward batches already paid during this Session. */
    val awardCount: Int = 0,
    /** Cumulative paid totals; used by Session history and GM review. */
    val awardedIpTotal: Int = 0,
    val awardedEbTotal: Int = 0,
    val awardedRepTotal: Int = 0
)
data class GameSession(
    val id: Long = System.currentTimeMillis(),
    val campaignId: Long? = null,
    val name: String = "",
    val date: String = "",
    val ended: Boolean = false,
    val players: List<PlayerSessionReward> = emptyList(),
    val log: List<SessionLogEntry> = emptyList()
)
data class PersistedSessionState(
    val count: Int = 1,
    val sessions: List<GameSession> = emptyList(),
    val activeSessionId: Long? = null
)

/** File-backed session history; suitable for very long campaigns. */
class SessionStore(private val context: Context) {
    private val gson = Gson()
    private val store = AtomicJsonFileStore(File(context.filesDir, "sessions_v3.json"))
    @Volatile var lastLoadWarning: String? = null
        private set

    fun load(): PersistedSessionState = PersistenceRuntime.locked {
        migrateLegacyIfNeeded(context)
        val result = store.read()
        lastLoadWarning = result.warning
        if (result.text.isNullOrBlank()) return@locked PersistedSessionState()
        val primary = runCatching { gson.fromJson(result.text, PersistedSessionState::class.java) }
        primary.getOrNull()?.let { return@locked normalize(it) }
        val recovered = store.readBackupText()?.let { raw -> runCatching { gson.fromJson(raw, PersistedSessionState::class.java) }.getOrNull() }
        if (recovered != null) {
            lastLoadWarning = "ذخیره سشن‌ها خراب بود؛ نسخه پشتیبان سالم بازیابی و فایل اصلی ترمیم شد."
            store.repairMainFromKnownGoodBackup()
            return@locked normalize(recovered)
        }
        lastLoadWarning = "ذخیره سشن‌ها قابل خواندن نیست؛ فایل خراب برای بازیابی حذف نشده است."
        PersistedSessionState()
    }

    fun save(state: PersistedSessionState, expectedEpoch: Long? = null): Boolean = store.write(gson.toJson(normalize(state)), expectedEpoch)

    fun detachCampaign(campaignId: Long, expectedEpoch: Long? = null) = PersistenceRuntime.locked {
        val s = load()
        save(s.copy(sessions = s.sessions.map { if (it.campaignId == campaignId) it.copy(campaignId = null) else it }), expectedEpoch)
    }

    fun appendLog(sessionId: Long?, entry: SessionLogEntry, expectedEpoch: Long? = null) {
        if (sessionId == null) return
        PersistenceRuntime.locked {
            val s = load()
            save(s.copy(sessions = s.sessions.map { if (it.id == sessionId) it.copy(log = listOf(entry) + it.log) else it }), expectedEpoch)
        }
    }

    private fun normalize(state: PersistedSessionState): PersistedSessionState {
        val sessions = state.sessions.distinctBy { it.id }.map { s -> s.copy(players = s.players.distinctBy { it.characterId }) }
        val active = state.activeSessionId?.takeIf { id -> sessions.any { it.id == id && !it.ended } }
        return state.copy(count = state.count.coerceAtLeast(1), sessions = sessions, activeSessionId = active)
    }

    companion object {
        fun migrateLegacyIfNeeded(context: Context) {
            val target = File(context.filesDir, "sessions_v3.json")
            if (target.exists()) return
            val prefs = context.getSharedPreferences("gm_sessions_v2", Context.MODE_PRIVATE)
            val raw = prefs.getString("state", null) ?: return
            runCatching {
                val parsed = Gson().fromJson(raw, PersistedSessionState::class.java) ?: PersistedSessionState()
                AtomicJsonFileStore(target).write(Gson().toJson(parsed))
            }
        }
    }
}
