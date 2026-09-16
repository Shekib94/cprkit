package com.cyberpunk.gmtool.viewmodel

// ─────────────────────────────────────────────────────────────
// دامنه‌ی مبارزه: وضعیت نشستِ فعال (initiative/turnها)، runtime
// رابط کاربری مبارزه، سپرها (Combat/Human Shield)، حرکت و
// تعقیب، گلاویزی، خفگی، آسیب مستقیم، Death Save و پایدارسازی.
// توابع توسعه روی CharacterViewModel — API عمومی بدون تغییر.
// helperهای خصوصیِ قبلی اکنون internal هستند چون هم کلاس اصلی و
// هم این فایل به آن‌ها نیاز دارند.
// ─────────────────────────────────────────────────────────────

import com.cyberpunk.gmtool.data.gtr


import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CharacterRepository
import com.cyberpunk.gmtool.data.GameSaveManager
import com.cyberpunk.gmtool.data.GameSaveSlot
import com.cyberpunk.gmtool.data.InventoryItem
import com.cyberpunk.gmtool.data.LifepathData
import com.cyberpunk.gmtool.data.SettingsRepository
import com.cyberpunk.gmtool.data.SkillData
import com.cyberpunk.gmtool.data.StreetratData
import com.cyberpunk.gmtool.data.Stats
import com.cyberpunk.gmtool.data.safeInt
import com.cyberpunk.gmtool.data.safeLong
import com.cyberpunk.gmtool.data.safeString
import com.cyberpunk.gmtool.data.safeStringSet
import com.cyberpunk.gmtool.data.Weapon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

internal fun CharacterViewModel.reloadCombatStateFromPrefs() {
    combatParticipantIds = combatPrefs.safeStringSet("participants", emptySet(), getApplication<Application>().applicationContext).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    combatInitiative = runCatching {
        val arr = org.json.JSONArray(combatPrefs.safeString("initiative", "[]", getApplication<Application>().applicationContext) ?: "[]")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            CombatInitiativeEntry(o.getInt("id"), o.getInt("total"), o.getString("roll"), o.optInt("tie", 0))
        }
    }.getOrDefault(emptyList())
    combatRound = combatPrefs.safeInt("round", 1, getApplication<Application>().applicationContext).coerceAtLeast(1)
    combatTurnIndex = combatPrefs.safeInt("turn", 0, getApplication<Application>().applicationContext).coerceAtLeast(0)
    combatLinkedCampaignId = combatPrefs.safeLong("linked_campaign", -1L, getApplication<Application>().applicationContext).takeIf { it >= 0L }
    combatLinkedEncounterId = combatPrefs.safeLong("linked_encounter", -1L, getApplication<Application>().applicationContext).takeIf { it >= 0L }
    combatLinkedSessionId = combatPrefs.safeLong("linked_session", -1L, getApplication<Application>().applicationContext).takeIf { it >= 0L }
}

internal fun CharacterViewModel.persistCombatState() {
    val arr = org.json.JSONArray()
    combatInitiative.forEach { e ->
        arr.put(org.json.JSONObject().put("id", e.characterId).put("total", e.total).put("roll", e.rollText).put("tie", e.tieBreak))
    }
    val e = combatPrefs.edit()
        .putStringSet("participants", combatParticipantIds.map { it.toString() }.toSet())
        .putString("initiative", arr.toString())
        .putInt("round", combatRound)
        .putInt("turn", combatTurnIndex)
    if (combatLinkedCampaignId != null) e.putLong("linked_campaign", combatLinkedCampaignId!!) else e.remove("linked_campaign")
    if (combatLinkedEncounterId != null) e.putLong("linked_encounter", combatLinkedEncounterId!!) else e.remove("linked_encounter")
    if (combatLinkedSessionId != null) e.putLong("linked_session", combatLinkedSessionId!!) else e.remove("linked_session")
    e.apply()
}

fun CharacterViewModel.loadCombatUiRuntime(characterId: Int): CombatUiRuntime {
    // اینجا نقطه‌ی ورودِ تب مبارزه است: هر کلیک روی تب، همین خواندن را انجام می‌دهد.
    // اگر کلید با نوع دیگری ذخیره شده باشد، getString مستقیم ClassCastException
    // می‌داد و برنامه فوراً بسته می‌شد؛ حالا کلید خراب پاک و مقدار پیش‌فرض برمی‌گردد.
    val raw = combatPrefs.safeString("runtime_$characterId", null, getApplication<Application>().applicationContext) ?: return CombatUiRuntime()
    return runCatching {
        val o = org.json.JSONObject(raw)
        CombatUiRuntime(
            targetId = o.optInt("targetId", -1).takeIf { it >= 0 },
            distanceText = o.optString("distanceText", "10"),
            targetDodges = o.optBoolean("targetDodges", true),
            aimTarget = o.optString("aimTarget", "None"),
            heldActionSummary = o.optString("heldActionSummary", "").takeIf { it.isNotBlank() },
            sandeActiveUntil = o.optLong("sandeActiveUntil", 0L),
            sandeCooldownUntil = o.optLong("sandeCooldownUntil", 0L),
            attackActionUsed = o.optInt("attackActionUsed", 0),
            attackActionCap = o.optInt("attackActionCap", -1).takeIf { it >= 0 },
            spotWeaknessUsed = o.optBoolean("spotWeaknessUsed", false),
            damageDeflectionUsed = o.optJSONArray("damageDeflectionUsed")?.let { a -> (0 until a.length()).map { a.optInt(it) }.toSet() } ?: emptySet(),
            coverHp = o.optInt("coverHp", 0), coverMaxHp = o.optInt("coverMaxHp", 0),
            coverLabel = o.optString("coverLabel", "No Cover"), coverDamageExpr = o.optString("coverDamageExpr", "5d6"),
            selectedVehicleId = o.optString("selectedVehicleId", "").takeIf { it.isNotBlank() },
            maneuverDv = o.optInt("maneuverDv", 13),
            ramTargetKey = o.optString("ramTargetKey", "").takeIf { it.isNotBlank() },
            ramNosBoosted = o.optBoolean("ramNosBoosted", false),
            selectedAttackWeaponId = o.optInt("selectedAttackWeaponId", -1).takeIf { it >= 0 },
            selectedAttackMode = o.optString("selectedAttackMode", "Single")
        )
    }.getOrDefault(CombatUiRuntime())
}

fun CharacterViewModel.saveCombatUiRuntime(characterId: Int, r: CombatUiRuntime) {
    val o = org.json.JSONObject()
        .put("targetId", r.targetId ?: -1).put("distanceText", r.distanceText).put("targetDodges", r.targetDodges)
        .put("aimTarget", r.aimTarget).put("heldActionSummary", r.heldActionSummary ?: "")
        .put("sandeActiveUntil", r.sandeActiveUntil).put("sandeCooldownUntil", r.sandeCooldownUntil)
        .put("attackActionUsed", r.attackActionUsed).put("attackActionCap", r.attackActionCap ?: -1)
        .put("spotWeaknessUsed", r.spotWeaknessUsed).put("coverHp", r.coverHp).put("coverMaxHp", r.coverMaxHp)
        .put("coverLabel", r.coverLabel).put("coverDamageExpr", r.coverDamageExpr)
        .put("selectedVehicleId", r.selectedVehicleId ?: "").put("maneuverDv", r.maneuverDv)
        .put("ramTargetKey", r.ramTargetKey ?: "").put("ramNosBoosted", r.ramNosBoosted)
        .put("selectedAttackWeaponId", r.selectedAttackWeaponId ?: -1).put("selectedAttackMode", r.selectedAttackMode)
    val def = org.json.JSONArray(); r.damageDeflectionUsed.forEach { def.put(it) }; o.put("damageDeflectionUsed", def)
    combatPrefs.edit().putString("runtime_$characterId", o.toString()).apply()
}

internal fun CharacterViewModel.clearCombatRuntimePrefs() {
    val e = combatPrefs.edit()
    combatPrefs.all.keys.filter { it.startsWith("runtime_") }.forEach { e.remove(it) }
    e.apply()
}

fun CharacterViewModel.toggleCombatParticipant(id: Int) {
    combatParticipantIds = if (id in combatParticipantIds) combatParticipantIds - id else combatParticipantIds + id
    combatInitiative = combatInitiative.filter { it.characterId in combatParticipantIds }
    combatTurnIndex = combatTurnIndex.coerceIn(0, (combatInitiative.size - 1).coerceAtLeast(0))
    persistCombatState()
}

fun CharacterViewModel.setCombatParticipants(ids: Set<Int>, rollInitiative: Boolean = true) {
    combatLinkedCampaignId = null; combatLinkedEncounterId = null; combatLinkedSessionId = null
    startCombatParticipantsInternal(ids, rollInitiative)
}

fun CharacterViewModel.startCampaignEncounter(campaignId: Long, encounterId: Long, ids: Set<Int>, sessionId: Long? = null) {
    combatLinkedCampaignId = campaignId
    combatLinkedEncounterId = encounterId
    combatLinkedSessionId = sessionId
    startCombatParticipantsInternal(ids, true)
}

fun CharacterViewModel.restartCombatParticipantsPreservingLink(ids: Set<Int>, rollInitiative: Boolean = true) {
    startCombatParticipantsInternal(ids, rollInitiative)
}

internal fun CharacterViewModel.startCombatParticipantsInternal(ids: Set<Int>, rollInitiative: Boolean) {
    val valid = _characters.value.map { it.id }.toSet()
    combatParticipantIds = ids.filter { it in valid }.toSet()
    combatInitiative = emptyList(); combatRound = 1; combatTurnIndex = 0
    clearCombatRuntimePrefs()
    persistCombatState()
    if (rollInitiative && combatParticipantIds.isNotEmpty()) rollCombatInitiative()
}

fun CharacterViewModel.clearCombatEncounter() {
    combatParticipantIds = emptySet(); combatInitiative = emptyList(); combatRound = 1; combatTurnIndex = 0
    combatLinkedCampaignId = null; combatLinkedEncounterId = null; combatLinkedSessionId = null
    clearCombatRuntimePrefs()
    persistCombatState()
}

fun CharacterViewModel.rollCombatInitiative() {
    val participants = _characters.value.filter { it.id in combatParticipantIds && !it.isDead }
    val preliminary = participants.map { c ->
        // همان فرمول دکمه‌ی «ROLL SELF» — با Sandevistan فعال (زمان اثر در وضعیت
        // زمان‌اجرای همان کاراکتر ذخیره شده است).
        val r = com.cyberpunk.gmtool.data.CombatRules.rollInitiative(
            c, loadCombatUiRuntime(c.id).sandeActiveUntil
        )
        CombatInitiativeEntry(
            c.id, r.total,
            gtr("%1s + REF %2s + Speedware %3s + Solo %4s", r.die, r.bonus.ref, r.bonus.speedware, r.bonus.solo)
        )
    }
    // Core: Initiative Queue نزولی است؛ تساوی‌ها با roll مجدد شکسته می‌شوند.
    val tieGroups = preliminary.groupBy { it.total }
    val tieValues = mutableMapOf<Int, Int>()
    tieGroups.values.filter { it.size > 1 }.forEach { group ->
        // Core: ties are rerolled until an order is established. Keep the original Initiative total on screen
        // and use a unique reroll only as the tiebreak key.
        val used = mutableSetOf<Int>()
        group.forEach { e ->
            var t: Int
            // Core: تساوی‌ها تا روشن‌شدن ترتیب دوباره ریخته می‌شوند؛ پس هر تلاش یک
            // تاس واقعی است و باید از DiceSource بیاید (در حالت دستی، GM می‌ریزد).
            do { t = com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "تاس تساوی Initiative") }
            while (t in used && used.size < 10)
            used += t
            tieValues[e.characterId] = t
        }
    }
    val withTies = preliminary.map { e -> e.copy(tieBreak = tieValues[e.characterId] ?: 0) }
    combatInitiative = withTies.sortedWith(compareByDescending<CombatInitiativeEntry> { it.total }.thenByDescending { it.tieBreak })
    combatRound = 1
    combatTurnIndex = 0
    combatInitiative.firstOrNull()?.characterId?.let { firstId ->
        val first = getCharacter(firstId)
        if (first != null && !first.isDead && com.cyberpunk.gmtool.data.GameRules.isMortallyWounded(first.hp)) rollDeathSave(firstId)
    }
    persistCombatState()
    if (combatInitiative.firstOrNull()?.characterId?.let { getCharacter(it)?.isDead } == true) nextCombatTurn()
}

fun CharacterViewModel.nextCombatTurn() {
    if (combatInitiative.isEmpty()) return
    // Resolve end-of-turn Core effects before advancing the queue.
    combatInitiative.getOrNull(combatTurnIndex)?.characterId?.let { endingId ->
        getCharacter(endingId)?.let { c ->
            val fire = when {
                c.combatEffects.containsKey("on_fire_4") -> 4
                c.combatEffects.containsKey("on_fire_2") -> 2
                else -> 0
            }
            updateCharacter(endingId) { cur ->
                var after = if (fire > 0) cur.copy(hp = (cur.hp - fire).coerceAtLeast(0)) else cur
                val moved = after.combatEffects.keys.firstOrNull { it.startsWith("moved_this_turn_") }
                    ?.removePrefix("moved_this_turn_")?.toIntOrNull() ?: 0
                if (moved > 4) {
                    var direct = 0
                    if (com.cyberpunk.gmtool.data.CriticalInjuries.effectActive(after, "broken_ribs")) direct += 5
                    if (com.cyberpunk.gmtool.data.CriticalInjuries.effectActive(after, "foreign_object_body") || com.cyberpunk.gmtool.data.CriticalInjuries.effectActive(after, "foreign_object_head")) direct += 5
                    if (direct > 0) after = after.copy(hp = (after.hp - direct).coerceAtLeast(0))
                    if (com.cyberpunk.gmtool.data.CriticalInjuries.effectActive(after, "lost_ear") || com.cyberpunk.gmtool.data.CriticalInjuries.effectActive(after, "damaged_ear")) after = after.copy(combatEffects = after.combatEffects + ("no_move_next_turn" to -1))
                }
                after.copy(combatEffects = after.combatEffects.mapNotNull { (k, v) ->
                    when {
                        k.startsWith("turn_hit_") || k.startsWith("turn_melee_count_") || k.startsWith("moved_this_turn_") || k.startsWith("judo_dodged_from_") || k.startsWith("used_ma_") || k == "judo_melee_hit_since_turn" || k == "run_used_this_turn" || k == "death_save_done" || k == "must_seek_cover_this_turn" -> null
                        v < 0 -> k to v
                        v > 1 -> k to (v - 1)
                        else -> null
                    }
                }.toMap())
            }
        }
    }
    var attempts = 0
    do {
        if (combatTurnIndex >= combatInitiative.lastIndex) {
            combatTurnIndex = 0
            combatRound += 1
        } else combatTurnIndex += 1
        attempts++
        val id = combatInitiative.getOrNull(combatTurnIndex)?.characterId
        val alive = id?.let { getCharacter(it)?.isDead == false } ?: false
        if (alive) break
    } while (attempts <= combatInitiative.size)
    combatInitiative.getOrNull(combatTurnIndex)?.characterId?.let { startingId ->
        getCharacter(startingId)?.let { cur ->
            var fx = cur.combatEffects
            if (fx.containsKey("no_move_next_turn")) fx = fx - "no_move_next_turn" + ("no_move_this_turn" to 1)
            if (fx.containsKey("no_action_next_turn")) fx = fx - "no_action_next_turn" + ("no_action_this_turn" to 1)
            if (fx.containsKey("suppressed")) fx = fx - "suppressed" + ("must_seek_cover_this_turn" to -1)
            if (fx !== cur.combatEffects) updateCharacter(startingId) { it.copy(combatEffects = fx) }
        }
        val live = getCharacter(startingId)
        if (live != null && !live.isDead && com.cyberpunk.gmtool.data.GameRules.isMortallyWounded(live.hp)) {
            rollDeathSave(startingId)
        }
    }
    persistCombatState()
}

/** Core Start Vehicle: after starting, the driver jumps to the top of the Initiative Queue. */
fun CharacterViewModel.moveCombatantToTop(characterId: Int) {
    val idx = combatInitiative.indexOfFirst { it.characterId == characterId }
    if (idx < 0) return
    val entry = combatInitiative[idx]
    combatInitiative = listOf(entry) + combatInitiative.filterIndexed { i, _ -> i != idx }
    combatTurnIndex = 0
    persistCombatState()
}

fun CharacterViewModel.previousCombatTurn() {
    if (combatInitiative.isEmpty()) return
    var attempts = 0
    do {
        if (combatTurnIndex <= 0) {
            combatTurnIndex = combatInitiative.lastIndex
            combatRound = (combatRound - 1).coerceAtLeast(1)
        } else combatTurnIndex -= 1
        attempts++
        val id = combatInitiative.getOrNull(combatTurnIndex)?.characterId
        val alive = id?.let { getCharacter(it)?.isDead == false } ?: false
        if (alive) break
    } while (attempts <= combatInitiative.size)
    persistCombatState()
}

fun CharacterViewModel.addCombatEffect(id: Int, key: String, turns: Int = 20) = updateCharacter(id) { c ->
    val current = c.combatEffects[key] ?: 0
    val value = if (turns < 0) turns else maxOf(current, turns)
    c.copy(combatEffects = c.combatEffects + (key to value))
}

fun CharacterViewModel.clearCombatEffect(id: Int, key: String) = updateCharacter(id) { c -> c.copy(combatEffects = c.combatEffects - key) }

/** Track movement during the current personal Turn for Core injury and Martial Arts requirements. */
fun CharacterViewModel.recordCombatMovement(id: Int, meters: Int): Int {
    val add = meters.coerceAtLeast(0)
    val c = getCharacter(id) ?: return 0
    if (c.combatEffects.containsKey("no_move_this_turn") || c.combatEffects.containsKey("prone") || c.combatEffects.keys.any { it.startsWith("grappled_by_") }) return 0
    val old = c.combatEffects.entries.firstOrNull { it.key.startsWith("moved_this_turn_") }?.key
        ?.removePrefix("moved_this_turn_")?.toIntOrNull() ?: 0
    val total = (old + add).coerceAtMost(com.cyberpunk.gmtool.data.CombatRules.effectiveMove(c) * 2)
    updateCharacter(id) { ch ->
        ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("moved_this_turn_") } + ("moved_this_turn_$total" to -1))
    }
    return total
}

fun CharacterViewModel.movementThisTurn(id: Int): Int = getCharacter(id)?.combatEffects?.keys
    ?.firstOrNull { it.startsWith("moved_this_turn_") }?.removePrefix("moved_this_turn_")?.toIntOrNull() ?: 0

/** Core Run: only after taking a Move Action; spends the Action to gain one additional Move Action. */
fun CharacterViewModel.runCombatMovement(id: Int): Int {
    val c = getCharacter(id) ?: return 0
    val moved = movementThisTurn(id)
    val oneMove = com.cyberpunk.gmtool.data.CombatRules.effectiveMove(c) * 2
    if (moved <= 0 || c.combatEffects.containsKey("no_move_this_turn") || c.combatEffects.containsKey("prone") || c.combatEffects.keys.any { it.startsWith("grappled_by_") }) return 0
    val skateBonus = if (c.inventory.count { it.equipped && it.name.equals("Skate Foot", true) } >= 2) 6 else 0
    val total = (moved + oneMove + skateBonus).coerceAtMost(oneMove * 2 + skateBonus)
    updateCharacter(id) { ch -> ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("moved_this_turn_") } + ("moved_this_turn_$total" to -1) + ("run_used_this_turn" to -1)) }
    return total
}

/** Core Human Shield: only the attacker in an existing Grapple may equip that defender, using an Action. */
fun CharacterViewModel.equipHumanShield(attackerId: Int, defenderId: Int): Boolean {
    val a = getCharacter(attackerId) ?: return false
    val d = getCharacter(defenderId) ?: return false
    if (!a.combatEffects.containsKey("grappling_$defenderId") || !d.combatEffects.containsKey("grappled_by_$attackerId")) return false
    updateCharacter(attackerId) { ch ->
        ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("human_shield_") } + ("human_shield_$defenderId" to -1))
    }
    return true
}

fun CharacterViewModel.unequipHumanShield(attackerId: Int) = updateCharacter(attackerId) { ch ->
    ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("human_shield_") })
}

fun CharacterViewModel.humanShieldId(attackerId: Int): Int? = getCharacter(attackerId)?.combatEffects?.keys
    ?.firstOrNull { it.startsWith("human_shield_") }?.removePrefix("human_shield_")?.toIntOrNull()

/** Persistent personal shield state. Bulletproof/Popup Shields are 10 HP cover; corpse shields use the corpse's BODY. */
fun CharacterViewModel.combatShieldHp(id: Int): Int = getCharacter(id)?.combatEffects?.keys
    ?.firstOrNull { it.startsWith("combat_shield_hp_") }?.removePrefix("combat_shield_hp_")?.toIntOrNull() ?: 0

fun CharacterViewModel.combatShieldKind(id: Int): String? = getCharacter(id)?.combatEffects?.keys
    ?.firstOrNull { it.startsWith("combat_shield_kind_") }?.removePrefix("combat_shield_kind_")

fun CharacterViewModel.readyCombatShield(id: Int, kind: String): String {
    val c = getCharacter(id) ?: return "Character not found"
    val allowed = when (kind) {
        "bulletproof" -> c.inventory.any { it.equipped && it.name.equals("Bulletproof Shield", true) }
        "popup" -> c.inventory.any { it.equipped && it.name.equals("Popup Shield", true) }
        else -> false
    }
    if (!allowed) return "Shield is not equipped/installed."
    val damageKeyPrefix = "combat_shield_damage_${kind}_"
    val damageTaken = c.combatEffects.keys.firstOrNull { it.startsWith(damageKeyPrefix) }?.removePrefix(damageKeyPrefix)?.toIntOrNull() ?: 0
    val hp = (10 - damageTaken).coerceAtLeast(0)
    if (hp <= 0) return "Shield is destroyed and must be repaired/replaced."
    updateCharacter(id) { ch ->
        val clean = ch.combatEffects.filterKeys { !it.startsWith("combat_shield_hp_") && !it.startsWith("combat_shield_kind_") }
        ch.copy(combatEffects = clean + ("combat_shield_hp_$hp" to -1) + ("combat_shield_kind_$kind" to -1))
    }
    return "OK"
}

fun CharacterViewModel.stowCombatShield(id: Int) = updateCharacter(id) { ch ->
    ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("combat_shield_hp_") && !it.startsWith("combat_shield_kind_") })
}

fun CharacterViewModel.restoreCombatShield(id: Int, kind: String) = updateCharacter(id) { ch ->
    ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("combat_shield_damage_${kind}_") && !it.startsWith("combat_shield_hp_") && !it.startsWith("combat_shield_kind_") })
}

/** Returns damage absorbed. Excess damage is lost when cover is destroyed in RED. */
fun CharacterViewModel.damageCombatShield(id: Int, damage: Int): Int {
    val hp = combatShieldHp(id)
    if (hp <= 0) return 0
    val absorbed = minOf(hp, damage.coerceAtLeast(0))
    val next = (hp - damage.coerceAtLeast(0)).coerceAtLeast(0)
    val kind = combatShieldKind(id) ?: "shield"
    updateCharacter(id) { ch ->
        var fx = ch.combatEffects.filterKeys { !it.startsWith("combat_shield_hp_") && !it.startsWith("combat_shield_damage_${kind}_") }
        val damageTaken = if (kind == "bulletproof" || kind == "popup") (10 - next).coerceIn(0, 10) else 0
        if (kind == "bulletproof" || kind == "popup") fx = fx + ("combat_shield_damage_${kind}_$damageTaken" to -1)
        fx = if (next > 0) fx + ("combat_shield_hp_$next" to -1) else fx.filterKeys { !it.startsWith("combat_shield_kind_") }
        ch.copy(combatEffects = fx)
    }
    return absorbed
}

fun CharacterViewModel.convertHumanShieldToCorpse(holderId: Int, corpseId: Int) {
    val corpse = getCharacter(corpseId) ?: return
    updateCharacter(holderId) { ch ->
        val clean = ch.combatEffects.filterKeys { !it.startsWith("human_shield_") && !it.startsWith("combat_shield_hp_") && !it.startsWith("combat_shield_kind_") }
        ch.copy(combatEffects = clean + ("combat_shield_hp_${corpse.stats.body.coerceAtLeast(1)}" to -1) + ("combat_shield_kind_corpse" to -1))
    }
}

/** Record Core Martial Arts combination prerequisites for this personal Turn. */
fun CharacterViewModel.recordCombatHit(attackerId: Int, targetId: Int, kind: String) {
    updateCharacter(attackerId) { ch ->
        val prefix = "turn_melee_count_${targetId}_"
        val oldKey = ch.combatEffects.keys.firstOrNull { it.startsWith(prefix) }
        val oldCount = oldKey?.removePrefix(prefix)?.toIntOrNull() ?: 0
        var fx = ch.combatEffects
        if (oldKey != null) fx = fx - oldKey
        fx = fx + ("turn_hit_${kind.lowercase()}_$targetId" to -1) + ("${prefix}${oldCount + 1}" to -1)
        ch.copy(combatEffects = fx)
    }
}

fun CharacterViewModel.clearTurnCombatTracking(id: Int) = updateCharacter(id) { ch ->
    ch.copy(combatEffects = ch.combatEffects.filterKeys { key ->
        !key.startsWith("turn_hit_") && !key.startsWith("turn_melee_count_") && !key.startsWith("moved_this_turn_") &&
            !key.startsWith("judo_dodged_from_") && !key.startsWith("used_ma_") && key != "judo_melee_hit_since_turn" && key != "run_used_this_turn" && key != "must_seek_cover_this_turn"
    })
}

/** Track whether a defender has dodged every Melee Attack since their last Turn for Judo Counter Throw. */
fun CharacterViewModel.recordIncomingMelee(defenderId: Int, attackerId: Int, dodged: Boolean) {
    if (dodged) addCombatEffect(defenderId, "judo_dodged_from_$attackerId", -1)
    else addCombatEffect(defenderId, "judo_melee_hit_since_turn", -1)
}

fun CharacterViewModel.directCombatHpDamage(id: Int, amount: Int) = updateCharacter(id) { c ->
    c.copy(hp = (c.hp - amount.coerceAtLeast(0)).coerceAtLeast(0), combatEffects = c.combatEffects - "unconscious")
}

/** Reload with a specific compatible ammunition stack and remember its special type. */
fun CharacterViewModel.reloadWeaponWithAmmo(id: Int, weaponId: Int, ammoName: String) = updateCharacter(id) { c ->
    val weapon = c.weapons.firstOrNull { it.id == weaponId } ?: return@updateCharacter c
    if (weapon.magazineSize <= 0) return@updateCharacter c
    val idx = c.inventory.indexOfFirst { it.category.equals("Ammo", true) && it.quantity > 0 && it.name.equals(ammoName, true) && isCompatibleAmmo(weapon, it.name) }
    if (idx < 0) return@updateCharacter c
    val need = weapon.magazineSize - weapon.currentAmmo
    if (need <= 0) return@updateCharacter c
    val stack = c.inventory[idx]
    val loaded = minOf(need, stack.quantity)
    val inv = c.inventory.mapIndexedNotNull { i, it -> if (i != idx) it else (it.quantity - loaded).let { q -> if (q > 0) it.copy(quantity=q) else null } }
    c.copy(weapons = c.weapons.map { if (it.id == weaponId) it.copy(currentAmmo=it.currentAmmo+loaded, loadedAmmoName=stack.name) else it }, inventory=inv)
}

fun CharacterViewModel.applyCombatDamage(
    targetId: Int,
    rawDamage: Int,
    head: Boolean = false,
    melee: Boolean = false,
    critical: Boolean = false,
    halvesArmor: Boolean = melee,
    armorPiercing: Boolean = false,
    rubber: Boolean = false,
    expansive: Boolean = false,
    ignoreArmorBelowSp11: Boolean = false,
    stunWeapon: Boolean = false
): com.cyberpunk.gmtool.data.DamageResolution? {
    val target = getCharacter(targetId) ?: return null
    val (updated, resolution) = com.cyberpunk.gmtool.data.CombatRules.resolveDamage(
        target = target,
        rawDamage = rawDamage,
        head = head,
        melee = melee,
        critical = critical,
        halvesArmor = halvesArmor,
        armorPiercing = armorPiercing,
        rubber = rubber,
        expansive = expansive,
        ignoreArmorBelowSp11 = ignoreArmorBelowSp11,
        stunWeapon = stunWeapon
    )
    updateCharacter(targetId) { updated }
    return resolution
}

fun CharacterViewModel.applyDirectCombatHpDamage(targetId: Int, amount: Int, floorAtOne: Boolean = false) {
    updateCharacter(targetId) { c ->
        if (c.isDead) c else {
            val nextHp = if (floorAtOne && c.hp > 1) (c.hp - amount.coerceAtLeast(0)).coerceAtLeast(1) else (c.hp - amount.coerceAtLeast(0)).coerceAtLeast(0)
            c.copy(hp = nextHp)
        }
    }
}

fun CharacterViewModel.startGrapple(attackerId: Int, defenderId: Int) {
    if (attackerId == defenderId) return
    updateCharacter(attackerId) { c ->
        c.copy(combatEffects = c.combatEffects.filterKeys { !it.startsWith("grappling_") } + ("grappling_$defenderId" to -1))
    }
    updateCharacter(defenderId) { c ->
        c.copy(combatEffects = c.combatEffects.filterKeys { !it.startsWith("grappled_by_") } + ("grappled_by_$attackerId" to -1))
    }
}

fun CharacterViewModel.endGrapple(attackerId: Int, defenderId: Int) {
    updateCharacter(attackerId) { c ->
        c.copy(combatEffects = c.combatEffects - "grappling_$defenderId" - "human_shield_$defenderId")
    }
    updateCharacter(defenderId) { c ->
        c.copy(combatEffects = c.combatEffects.filterKeys { it != "grappled_by_$attackerId" && it != "iron_grip_by_$attackerId" && !it.startsWith("choke_chain_${attackerId}_") })
    }
}

fun CharacterViewModel.markProne(id: Int, prone: Boolean) = updateCharacter(id) { c ->
    c.copy(combatEffects = if (prone) c.combatEffects + ("prone" to -1) else c.combatEffects - "prone")
}

/** Core Choke: BODY direct HP; three successive Rounds of Choking render the target Unconscious. */
fun CharacterViewModel.applyChoke(attackerId: Int, defenderId: Int, body: Int, round: Int): Int {
    val defender = getCharacter(defenderId) ?: return 0
    val prefix = "choke_chain_${attackerId}_"
    val oldKey = defender.combatEffects.keys.firstOrNull { it.startsWith(prefix) }
    val parts = oldKey?.removePrefix(prefix)?.split('_')
    val lastRound = parts?.getOrNull(0)?.toIntOrNull()
    val oldCount = parts?.getOrNull(1)?.toIntOrNull() ?: 0
    val count = when {
        lastRound == round -> oldCount.coerceAtLeast(1) // never count twice in the same Round
        lastRound == round - 1 -> (oldCount + 1).coerceAtMost(3)
        else -> 1
    }
    val beforeHp = defender.hp
    // Core wording only floors at 1 if a target that started above 1 HP would be reduced below 0.
    val rawAfter = beforeHp - body.coerceAtLeast(0)
    if (beforeHp > 1 && rawAfter < 0) {
        updateCharacter(defenderId) { it.copy(hp = 1, combatEffects = it.combatEffects + ("unconscious" to -1)) }
    } else applyDirectCombatHpDamage(defenderId, body, floorAtOne = false)
    updateCharacter(defenderId) { c ->
        var fx = c.combatEffects.filterKeys { !it.startsWith(prefix) }
        fx = fx + ("${prefix}${round}_${count}" to -1)
        if (count >= 3) fx = fx + ("unconscious" to -1)
        c.copy(combatEffects = fx)
    }
    return count
}

fun CharacterViewModel.rollDeathSave(id: Int): Pair<Int, Boolean>? {
    val c = getCharacter(id) ?: return null
    if (c.isDead || !com.cyberpunk.gmtool.data.GameRules.isMortallyWounded(c.hp) || c.combatEffects.containsKey("death_save_done")) return null
    val roll = com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "تاس d10")
    val success = roll != 10 && (roll + c.deathSavePenalty) < com.cyberpunk.gmtool.data.GameRules.deathSaveBody(c)
    updateCharacter(id) { it.copy(
        deathSavePenalty = it.deathSavePenalty + 1,
        isDead = !success,
        combatEffects = it.combatEffects + ("death_save_done" to -1)
    ) }
    return roll to success
}

/** Successful stabilization of a Mortally Wounded character returns them to 1 HP, unconscious. */
fun CharacterViewModel.stabilizeMortallyWounded(id: Int): Boolean {
    val c = getCharacter(id) ?: return false
    if (c.isDead || !com.cyberpunk.gmtool.data.GameRules.isMortallyWounded(c.hp)) return false
    updateCharacter(id) { it.copy(hp = 1, isDead = false, deathSavePenalty = com.cyberpunk.gmtool.data.GameRules.baseDeathSavePenalty(it), combatEffects = it.combatEffects + ("unconscious" to 20)) }
    return true
}
