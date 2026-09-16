package com.cyberpunk.gmtool.viewmodel

// ─────────────────────────────────────────────────────────────
// دامنه‌ی مراقبت و وضعیت‌های پایدار بدن: Street Drugs، تراپی
// (Humanity/ترک اعتیاد)، اعتیادها، جراحت بحرانی و Quick Fix.
// توابع توسعه روی CharacterViewModel — API عمومی بدون تغییر.
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

// ---------- Street Drugs ----------
/** Consume one owned dose and start/extend its Primary Effect. */
fun CharacterViewModel.useStreetDrug(id: Int, inventoryIndex: Int): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (inventoryIndex !in c.inventory.indices) return "دوز پیدا نشد"
    val item = c.inventory[inventoryIndex]
    if (!item.category.equals("Drugs", true)) return "این آیتم Street Drug نیست"
    if (c.inventory.none { it.category.equals("Gear", true) && it.name.equals("Airhypo", true) }) return "برای administer کردن Street Drug با این اکشن، Airhypo لازم است."
    val rule = com.cyberpunk.gmtool.data.StreetDrugRules.rule(item.name) ?: return "Rule این Drug ثبت نشده است"

    var humanityLoss = 0
    if (item.name.equals("Black Lace", true)) {
        humanityLoss = com.cyberpunk.gmtool.data.CombatRules.rollDamage("2d6").total
    }
    val inv = c.inventory.toMutableList()
    if (item.quantity > 1) inv[inventoryIndex] = item.copy(quantity = item.quantity - 1) else inv.removeAt(inventoryIndex)
    val oldHours = c.activeDrugEffects.entries.firstOrNull { it.key.equals(item.name, true) }?.value ?: 0
    val canonicalName = com.cyberpunk.gmtool.data.StoreCatalog.findForInventory(item.name, "Drugs")?.name ?: item.name
    val next = c.copy(
        inventory = inv,
        currentHumanity = (c.currentHumanity - humanityLoss).coerceAtLeast(0),
        activeDrugEffects = c.activeDrugEffects.filterKeys { !it.equals(canonicalName, true) } + (canonicalName to (oldHours + rule.durationHours)),
        drugHumanityHeld = if (humanityLoss > 0) c.drugHumanityHeld.filterKeys { !it.equals(canonicalName, true) } +
            (canonicalName to ((c.drugHumanityHeld.entries.firstOrNull { it.key.equals(canonicalName, true) }?.value ?: 0) + humanityLoss))
        else c.drugHumanityHeld
    )
    val synced = com.cyberpunk.gmtool.data.GameRules.syncDerived(next)
    persist(_characters.value.map { if (it.id == id) synced else it })
    return "OK|$canonicalName|${oldHours + rule.durationHours}|$humanityLoss"
}

fun CharacterViewModel.applyTemporaryStatPenalty(id: Int, penalties: Map<String, Int>, hours: Int = 1) {
    updateCharacter(id) { c ->
        val merged = c.temporaryStatPenalties.toMutableMap()
        penalties.forEach { (stat, amount) -> merged[stat.uppercase()] = (merged[stat.uppercase()] ?: 0) + amount.coerceAtLeast(0) }
        c.copy(temporaryStatPenalties = merged, temporaryStatPenaltyHours = maxOf(c.temporaryStatPenaltyHours, hours.coerceAtLeast(1)))
    }
}

fun CharacterViewModel.clearTemporaryStatPenalties(id: Int) = updateCharacter(id) { it.copy(temporaryStatPenalties = emptyMap(), temporaryStatPenaltyHours = 0) }

/** Advance one in-game hour for active drug timers. Secondary Check is resolved explicitly when a timer reaches zero. */
fun CharacterViewModel.advanceStreetDrugHour(id: Int): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    if (c.activeDrugEffects.isEmpty()) return "هیچ Primary Effect فعالی وجود ندارد"
    val nextMap = c.activeDrugEffects.mapValues { (_, hours) -> (hours - 1).coerceAtLeast(0) }
    val ending = nextMap.filterValues { it == 0 }.keys
    updateCharacter(id) { ch ->
        val tempHours = (ch.temporaryStatPenaltyHours - 1).coerceAtLeast(0)
        ch.copy(activeDrugEffects = nextMap, temporaryStatPenaltyHours = tempHours,
            temporaryStatPenalties = if (tempHours == 0) emptyMap() else ch.temporaryStatPenalties)
    }
    return if (ending.isEmpty()) "OK" else "READY|${ending.joinToString(", ")}"
}

/** Resolve WILL + Resist Torture/Drugs + 1d10 against the drug's Secondary Effect DV. */
fun CharacterViewModel.resolveStreetDrugSecondary(id: Int, drugName: String): String {
    val c = getCharacter(id) ?: return "کاراکتر یافت نشد"
    val entry = c.activeDrugEffects.entries.firstOrNull { it.key.equals(drugName, true) } ?: return "این Drug فعال نیست"
    if (entry.value > 0) return "Primary Effect هنوز ${entry.value} ساعت باقی مانده است"
    val rule = com.cyberpunk.gmtool.data.StreetDrugRules.rule(drugName) ?: return "Rule این Drug ثبت نشده است"
    val autoFail = c.addictionRelapseRisk.any { it.equals(drugName, true) }
    val roll = com.cyberpunk.gmtool.data.CombatRules.rollD10()
    val resist = com.cyberpunk.gmtool.data.CombatRules.skillLevel(c, "Resist Torture/Drugs")
    val total = c.stats.will + resist + roll.totalDie
    val success = !autoFail && total > rule.secondaryDv
    val held = c.drugHumanityHeld.entries.firstOrNull { it.key.equals(drugName, true) }?.value ?: 0
    var next = c.copy(
        activeDrugEffects = c.activeDrugEffects.filterKeys { !it.equals(drugName, true) },
        drugHumanityHeld = c.drugHumanityHeld.filterKeys { !it.equals(drugName, true) }
    )
    if (success && drugName.equals("Black Lace", true) && held > 0) {
        next = next.copy(currentHumanity = (next.currentHumanity + held).coerceAtMost(next.maxHumanity))
    }
    if (!success) {
        next = next.copy(addictions = (next.addictions + drugName).distinct())
    }
    next = com.cyberpunk.gmtool.data.GameRules.syncDerived(next)
    persist(_characters.value.map { if (it.id == id) next else it })
    return "${if (success) "OK" else "FAIL"}|${roll.first}|$total|${rule.secondaryDv}|$held|${if (autoFail) "RELAPSE_AUTO_FAIL" else "ROLL"}"
}

// ---------- تراپی (بازیابی Humanity / ترک اعتیاد) ----------
/**
 * تراپی یک هفته طول می‌کشد. درمانگر باید صریحاً انتخاب شود و باید Medtech دیگری باشد.
 * در شکست، هفته و مواد مصرف می‌شوند و اثر اعمال نمی‌شود. Medtech نمی‌تواند روی خودش Therapy انجام دهد.
 */
fun CharacterViewModel.receiveTherapy(id: Int, kind: String, medtechId: Int, addictionName: String? = null): String {
    val patient = getCharacter(id) ?: return "کاراکتر یافت نشد"
    // When a PC Medtech performs therapy, the Core cost is the material cost, not the commercial service price.
    val (materialsCost, dv, dice) = when (kind) {
        "addiction" -> Triple(500, 15, 0)
        "standard" -> Triple(100, 15, 2)
        "extreme" -> Triple(500, 17, 4)
        else -> return "تراپی نامشخص"
    }
    if (kind == "addiction" && patient.addictions.isEmpty()) return "هیچ اعتیاد ثبت‌شده‌ای برای درمان وجود ندارد."
    if (patient.eurodollars < materialsCost) return "پول کافی نیست (مواد لازم: $materialsCost eb)"

    val medtech = getCharacter(medtechId)
        ?.takeIf { it.id != id && it.role.equals("Medtech", true) }
        ?: return "یک Medtech دیگر را به‌عنوان درمانگر انتخاب کن؛ Medtech نمی‌تواند روی خودش Therapy انجام دهد."

    val medicalTech = ((medtech.roleAbilityPoints["pharma"] ?: 0) + (medtech.roleAbilityPoints["cryo"] ?: 0)).coerceAtMost(10)
    val check = com.cyberpunk.gmtool.data.CombatRules.rollD10()
    val total = medtech.stats.tech + medicalTech + check.totalDie
    // A full week and the materials are spent whether the check succeeds or fails. The check must BEAT the DV.
    updateCharacter(id) { it.copy(eurodollars = it.eurodollars - materialsCost, therapyWeeksSpent = it.therapyWeeksSpent + 1) }
    if (total <= dv) return "FAIL|${check.first}|$total|$dv|Medtech:${medtech.handle.ifBlank { medtech.name }}"

    if (kind == "addiction") {
        val addiction = patient.addictions.firstOrNull { it.equals(addictionName, true) }
            ?: return "اعتیاد انتخاب‌شده دیگر روی بیمار ثبت نیست."
        updateCharacter(id) { ch ->
            ch.copy(
                addictions = ch.addictions.filterNot { it.equals(addiction, true) },
                addictionRelapseRisk = (ch.addictionRelapseRisk + addiction).distinct()
            )
        }
        return "OK|0|$addiction|Medtech:${medtech.handle.ifBlank { medtech.name }}|یک هفته • برای یک سالِ داخل بازی، Secondary Effect همان ماده خودکار Fail می‌شود"
    }

    val healed = com.cyberpunk.gmtool.data.DiceSource.roll(dice, 6, "درمان").sum()
    updateCharacter(id) { ch ->
        val hum = (ch.currentHumanity + healed).coerceAtMost(ch.maxHumanity)
        ch.copy(currentHumanity = hum, stats = ch.stats.copy(emp = minOf(ch.baseEmp, com.cyberpunk.gmtool.data.GameRules.currentEmpathy(hum))))
    }
    return "OK|$healed|Medtech:${medtech.handle.ifBlank { medtech.name }}|Total:$total|DV:$dv|یک هفته"
}


fun CharacterViewModel.addAddiction(id: Int, name: String) = updateCharacter(id) { c ->
    if (name.isBlank() || c.addictions.any { it.equals(name, true) }) c else c.copy(addictions = c.addictions + name.trim())
}

/** Adds a new Critical Injury and its Core 5 Bonus Damage. Duplicate injuries are ignored. */
fun CharacterViewModel.addCriticalInjury(id: Int, injuryKey: String) = updateCharacter(id) { c ->
    val injury = com.cyberpunk.gmtool.data.CriticalInjuries.byKey(injuryKey) ?: return@updateCharacter c
    if (c.criticalInjuries.contains(injury.key)) return@updateCharacter c
    val deathPenaltyInc = if (listOf(
            "dismembered_arm","dismembered_hand","collapsed_lung","spinal_injury","dismembered_leg",
            "lost_eye","brain_injury","whiplash","cracked_skull","crushed_windpipe","lost_ear"
        ).contains(injury.key)) 1 else 0
    c.copy(
        hp = (c.hp - 5).coerceAtLeast(0),
        criticalInjuries = c.criticalInjuries + injury.key,
        deathSavePenalty = c.deathSavePenalty + deathPenaltyInc,
        combatEffects = if (injury.key == "spinal_injury") c.combatEffects + ("no_action_next_turn" to -1) else c.combatEffects
    )
}

fun CharacterViewModel.clearCriticalInjury(id: Int, injuryKey: String) = updateCharacter(id) { c ->
    val oldBase = com.cyberpunk.gmtool.data.GameRules.baseDeathSavePenalty(c)
    val stripped = c.copy(
        criticalInjuries = c.criticalInjuries.filterNot { it == injuryKey },
        combatEffects = c.combatEffects - "quickfix_$injuryKey" - "critical_effect_removed_$injuryKey"
    )
    val newBase = com.cyberpunk.gmtool.data.GameRules.baseDeathSavePenalty(stripped)
    stripped.copy(deathSavePenalty = (c.deathSavePenalty - (oldBase - newBase)).coerceAtLeast(newBase))
}

private fun surgeryLevel(character: com.cyberpunk.gmtool.data.Character): Int =
    ((character.roleAbilityPoints["surgery"] ?: 0) * 2).coerceIn(0, 10)

private fun careSkillLevel(character: com.cyberpunk.gmtool.data.Character, skill: String): Int = when (skill) {
    "Surgery" -> surgeryLevel(character)
    else -> com.cyberpunk.gmtool.data.CombatRules.skillLevel(character, skill)
}

private fun likelyCyberReplacement(patient: com.cyberpunk.gmtool.data.Character, injuryKey: String): Boolean {
    val installed = patient.inventory.filter { it.equipped && it.category.equals("Cyberware", true) }.map { it.name }
    return when {
        injuryKey.contains("arm") || injuryKey.contains("hand") || injuryKey == "crushed_fingers" -> installed.any { it.contains("Cyberarm", true) }
        injuryKey.contains("leg") -> installed.any { it.contains("Cyberleg", true) }
        injuryKey.contains("eye") -> installed.any { it.contains("Cybereye", true) }
        injuryKey.contains("ear") -> installed.any { it.contains("Cyberaudio", true) }
        else -> false
    }
}

/** Core Critical Injury care. Quick Fix = 1 minute; Treatment = 4 hours and cannot target the healer.
 *  The healer is explicit so the app never silently uses a character who is not actually present in the scene. */
fun CharacterViewModel.attemptCriticalInjuryCare(patientId: Int, injuryKey: String, treatment: Boolean, preferCybertech: Boolean = false, healerId: Int? = null): String {
    val patient = getCharacter(patientId) ?: return "Patient not found"
    val injury = com.cyberpunk.gmtool.data.CriticalInjuries.byKey(injuryKey) ?: return "Critical Injury not found"
    if (injuryKey !in patient.criticalInjuries) return "Injury is no longer present"
    val baseOptions = if (treatment) com.cyberpunk.gmtool.data.CriticalInjuries.treatmentOptions(injuryKey) else com.cyberpunk.gmtool.data.CriticalInjuries.quickFixOptions(injuryKey)
    if (baseOptions.isEmpty()) {
        if (!treatment && com.cyberpunk.gmtool.data.CriticalInjuries.quickFixIsPermanent(injuryKey)) return "Use Quick Fix; a successful Quick Fix permanently removes this Injury Effect."
        return if (treatment) "No separate Treatment is required/available for this injury in the Core table." else "This injury has no Quick Fix; it requires Treatment."
    }

    data class Candidate(val healer: com.cyberpunk.gmtool.data.Character, val skill: String, val dv: Int, val level: Int)
    if (preferCybertech && !likelyCyberReplacement(patient, injuryKey)) return "Cybertech substitution requires the injured body section to be replaced by cyberware; use normal care or GM-adjudicate the affected side."
    val healer = healerId?.let { getCharacter(it) } ?: return "Select the healer who is actually present before rolling care."
    if (treatment && healer.id == patientId) return "Treatment cannot be performed on yourself."
    val candidates = mutableListOf<Candidate>()
    if (preferCybertech) {
        // Cybertech substitutes for the listed healing skill at the same DV for replaced cyber body parts.
        val cyber = careSkillLevel(healer, "Cybertech")
        if (cyber > 0) baseOptions.minByOrNull { it.dv }?.let { candidates += Candidate(healer, "Cybertech", it.dv, cyber) }
    } else {
        baseOptions.forEach { opt ->
            val level = careSkillLevel(healer, opt.skill)
            if (level > 0) candidates += Candidate(healer, opt.skill, opt.dv, level)
        }
    }
    val best = candidates.maxByOrNull { it.healer.stats.tech + it.level - it.dv } ?: return gtr("Selected healer has no eligible skill for %1s.", injury.enName)
    val roll = com.cyberpunk.gmtool.data.CombatRules.rollD10()
    val actionPenalty = com.cyberpunk.gmtool.data.CombatRules.allActionsPenalty(best.healer)
    val total = best.healer.stats.tech + best.level + roll.totalDie + actionPenalty
    val success = total > best.dv
    val who = best.healer.handle.ifBlank { best.healer.name }
    if (!success) return gtr("FAIL • %1s • TECH %2s + %3s %4s + roll %5s%6s = %7s vs DV%8s • %9s", who, best.healer.stats.tech, best.skill, best.level, roll.totalDie, if (actionPenalty != 0) " + penalty $actionPenalty" else "", total, best.dv, if (treatment) "4 hours" else "1 minute")

    if (treatment) {
        clearCriticalInjury(patientId, injuryKey)
        return gtr("TREATMENT SUCCESS • %1s • %2s vs DV%3s • 4 hours • %4s removed permanently.", who, total, best.dv, injury.enName)
    }

    updateCharacter(patientId) { ch ->
        val oldBase = com.cyberpunk.gmtool.data.GameRules.baseDeathSavePenalty(ch)
        val key = if (com.cyberpunk.gmtool.data.CriticalInjuries.quickFixIsPermanent(injuryKey)) "critical_effect_removed_$injuryKey" else "quickfix_$injuryKey"
        val next = ch.copy(combatEffects = ch.combatEffects + (key to -1))
        val newBase = com.cyberpunk.gmtool.data.GameRules.baseDeathSavePenalty(next)
        next.copy(deathSavePenalty = (ch.deathSavePenalty - (oldBase - newBase)).coerceAtLeast(newBase))
    }
    return gtr("QUICK FIX SUCCESS • %1s • %2s vs DV%3s • 1 minute • %4s.", who, total, best.dv, if (com.cyberpunk.gmtool.data.CriticalInjuries.quickFixIsPermanent(injuryKey)) "effect removed permanently" else "effect suppressed for the rest of the day")
}

/** Clear only day-limited Quick Fixes; permanent Critical Effect removals remain. */
fun CharacterViewModel.expireDailyQuickFixes(id: Int) = updateCharacter(id) { ch ->
    val beforeBase = com.cyberpunk.gmtool.data.GameRules.baseDeathSavePenalty(ch)
    val next = ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("quickfix_") })
    val afterBase = com.cyberpunk.gmtool.data.GameRules.baseDeathSavePenalty(next)
    next.copy(deathSavePenalty = (ch.deathSavePenalty + (afterBase - beforeBase)).coerceAtLeast(afterBase))
}

// ═══════════════════════════════════════════════════════════════════
// درمان واقعی روی کاراکترِ بازیکن — از کادر Medtech در Role Helper
//
// چرا اینجا: قبلاً صفحه‌ی Medtech در GM Tools فقط «مرجع و سناریو» بود و
// جراحت‌ها/HP بازیکن‌ها هیچ‌جا درمان نمی‌شد. این توابع همان کار را انجام
// می‌دهند: وضعیت بیمار از خود Character خوانده می‌شود (Seriously Wounded،
// Critical Injuries، HP) و بعد از موفقیت، همان جراحت/HP روی برگه تغییر
// می‌کند — نه در یک لیست جداگانه.
// ═══════════════════════════════════════════════════════════════════

/** وضعیت زخم بیمار برای نمایش و انتخاب DV — از خود کاراکتر خوانده می‌شود. */
fun CharacterViewModel.woundState(id: Int): com.cyberpunk.gmtool.data.MedtechCareRules.StabilizeTarget? {
    val c = getCharacter(id) ?: return null
    return com.cyberpunk.gmtool.data.MedtechCareRules.stabilizeTarget(c.hp, c.maxHp, c.isDead)
}

/**
 * پایدارسازی بیمار Mortally Wounded/Seriously Wounded با تاس از DiceSource.
 * موفقیت در DV15 → ۱ HP + بیهوشی؛ موفقیت در DV13 → تگ stabilized.
 * Critical Injuryها عمداً دست‌نخورده می‌مانند (Core: دو موضوع جدا هستند).
 */
fun CharacterViewModel.stabilizeCharacter(patientId: Int, healerId: Int): String {
    val patient = getCharacter(patientId) ?: return "بیمار پیدا نشد"
    val healer = getCharacter(healerId) ?: return "درمانگر پیدا نشد"
    val result = com.cyberpunk.gmtool.data.MedtechCareRules.rollStabilize(patient, healer)
    if (result.success) {
        if (result.target.dv == 15) {
            updateCharacter(patientId) { ch ->
                ch.copy(
                    hp = 1, isDead = false,
                    deathSavePenalty = com.cyberpunk.gmtool.data.GameRules.baseDeathSavePenalty(ch),
                    combatEffects = ch.combatEffects + ("unconscious" to 20) + ("stabilized" to -1)
                )
            }
        } else {
            addCombatEffect(patientId, "stabilized", -1)
        }
        markMedtechPatientOutcome(healerId, patientId, "Stabilized", "پایدارسازی موفق (DV${result.target.dv})")
    }
    return result.detailFa + if (result.needsTraining) " ⚠ این درمانگر Paramedic ندارد و با First Aid ریخت؛ برای DVهای سخت دوره‌ی Paramedic (۲ هفته + ۶۰ IP) لازم است." else ""
}

/**
 * Quick Fix یا Treatment یک Critical Injury مشخص، با انتخاب صریح درمانگر.
 * خروجی همان متن گزارش attemptCriticalInjuryCare است به‌علاوه‌ی ثبت نتیجه
 * در فهرست بیماران همان Medtech (اگر آن بیمار آنجا ثبت شده باشد).
 */
fun CharacterViewModel.careForInjury(
    medtechId: Int,
    patientId: Int,
    injuryKey: String,
    treatment: Boolean,
    preferCybertech: Boolean = false
): String {
    val injury = com.cyberpunk.gmtool.data.CriticalInjuries.byKey(injuryKey) ?: return "این جراحت در جدول Core نیست"
    val out = attemptCriticalInjuryCare(patientId, injuryKey, treatment, preferCybertech, medtechId)
    val ok = out.startsWith("TREATMENT SUCCESS") || out.startsWith("QUICK FIX SUCCESS")
    if (ok) {
        val state = if (treatment) "Recovering" else "Stabilized"
        val note = if (treatment) "درمان کامل ${injury.enName}" else "Quick Fix ${injury.enName}"
        markMedtechPatientOutcome(medtechId, patientId, state, note)
    }
    return out
}

/**
 * ساخت/تزریق Pharmaceutical طبق Core و اعمال اثر واقعی روی بیمار:
 * Speedheal → HP، Antibiotic/Stim/Surge → اثر موقت، Rapidetox → پاک‌کردن اثر.
 */
fun CharacterViewModel.administerPharmaceutical(
    patientId: Int,
    medtechId: Int,
    drugName: String,
    addictionRisk: Boolean = false,
    addictionName: String? = null
): String {
    val patient = getCharacter(patientId) ?: return "بیمار پیدا نشد"
    val medtech = getCharacter(medtechId) ?: return "Medtech پیدا نشد"
    val use = com.cyberpunk.gmtool.data.MedtechCareRules.rollPharmaceutical(patient, medtech, drugName)
    if (use.success) {
        if (use.hpDelta > 0) {
            updateCharacter(patientId) { ch ->
                ch.copy(hp = (ch.hp + use.hpDelta).coerceAtMost(ch.maxHp))
            }
        }
        use.addEffect?.let { (key, hours) -> addCombatEffect(patientId, key, hours) }
        use.removeEffectKey?.let { key ->
            updateCharacter(patientId) { ch -> ch.copy(activeDrugEffects = ch.activeDrugEffects.filterKeys { !it.equals(key, true) }) }
            clearCombatEffect(patientId, key)
        }
        if (addictionRisk) {
            val name = addictionName?.takeIf { it.isNotBlank() } ?: drugName
            if (com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "تست اعتیاد $name") >= 10) addAddiction(patientId, name)
        }
        markMedtechPatientOutcome(medtechId, patientId, "Recovering", "دارو: $drugName")
    }
    val extra = if (!use.learned) " ⚠ این دارو در Pharmaceuticalهای یادگرفته‌شده‌ی این Medtech نبود؛ اگر می‌خواهی رسماً یاد بگیرد، در تب BIO امتیاز Pharmaceuticals را بده." else ""
    return use.detailFa + extra
}

/** یک روز استراحت/نقاهت: روزی BODY HP (مدیریت‌شده با دست، نه خودکار). */
fun CharacterViewModel.applyRecoveryDay(patientId: Int, fasting: Boolean = false): String {
    val patient = getCharacter(patientId) ?: return "بیمار پیدا نشد"
    if (patient.isDead) return "این کاراکتر مرده است؛ Recovery معنا ندارد."
    if (com.cyberpunk.gmtool.data.GameRules.isMortallyWounded(patient.hp)) return "بیمار Mortally Wounded است؛ اول باید پایدارسازی موفق شود."
    val perDay = com.cyberpunk.gmtool.data.MedtechCareRules.naturalRecoveryPerDay(patient)
    val gained = (if (fasting) (perDay / 2).coerceAtLeast(0) else perDay).coerceAtMost(patient.maxHp - patient.hp)
    updateCharacter(patientId) { ch -> ch.copy(hp = (ch.hp + gained).coerceAtMost(ch.maxHp)) }
    val injuries = com.cyberpunk.gmtool.data.MedtechCareRules.activityDamageInjuries(patient)
    val warn = if (injuries.isNotEmpty()) " • ⚠ با حرکت بیش از ۴ متر/یارد در روز، اثر ${injuries.size} جراحت (دنده/شیء خارجی) دوباره آسیب وارد می‌کند." else ""
    return "روز استراحت ثبت شد: +$gained HP (اکنون ${getCharacter(patientId)?.hp ?: 0}/${patient.maxHp}). جراحت‌های درمان‌نشده سر جای خود هستند.$warn"
}

/**
 * آموزش Paramedic: ۲ هفته + ۶۰ IP. اگر مهارت روی برگه نیست ساخته می‌شود،
 * وگرنه ۶۰ IP از Improvement Points کم می‌شود (همان قاعده‌ی آموزش دوره).
 */
fun CharacterViewModel.teachParamedic(medtechId: Int): String {
    val c = getCharacter(medtechId) ?: return "کاراکتر پیدا نشد"
    val rules = com.cyberpunk.gmtool.data.MedtechCareRules
    if (rules.paramedicLevel(c) > 0) return "این کاراکتر از قبل Paramedic دارد (سطح ${rules.paramedicLevel(c)})."
    if (c.improvementPoints < rules.PARAMEDIC_IP_COST) {
        return "IP کافی نیست: دوره‌ی Paramedic برای این دوره‌ی آموزشی ${rules.PARAMEDIC_IP_COST} IP می‌خواهد و این کاراکتر ${c.improvementPoints} IP دارد."
    }
    updateCharacter(medtechId) { ch ->
        val hasSkill = ch.skills.any { it.name.equals("Paramedic", true) }
        val nextSkills = if (hasSkill) ch.skills else ch.skills + com.cyberpunk.gmtool.data.SkillData(
            name = "Paramedic", stat = "Technique", level = 0,
            description = "آموزش رسمی Medtech: ۲ هفته + ۶۰ IP"
        )
        val nextPoints = (com.cyberpunk.gmtool.data.CombatRules.skillLevel(ch.copy(skills = nextSkills), "Paramedic") + 2).coerceAtMost(10)
        ch.copy(
            improvementPoints = ch.improvementPoints - rules.PARAMEDIC_IP_COST,
            skills = nextSkills.map { if (it.name.equals("Paramedic", true)) it.copy(level = nextPoints) else it }
        )
    }
    val after = getCharacter(medtechId)
    return "دوره‌ی Paramedic ثبت شد: ${rules.PARAMEDIC_TRAINING_WEEKS} هفته آموزش و ${rules.PARAMEDIC_IP_COST} IP • اکنون Paramedic سطح ${after?.let { rules.paramedicLevel(it) } ?: 0} (آموزش باید داخل بازی زمان ببرد)."
}

/** یادداشت نتیجه‌ی درمان روی فهرست بیماران همین Medtech (اگر بیمار آنجا ثبت شده باشد). */
private fun CharacterViewModel.markMedtechPatientOutcome(medtechId: Int, patientId: Int, state: String, note: String) {
    updateCharacter(medtechId) { c ->
        val entry = c.medtechPatients.firstOrNull { it.linkedCharacterId == patientId } ?: return@updateCharacter c
        val patient = getCharacter(patientId)
        c.copy(
            medtechPatients = c.medtechPatients.map {
                if (it.id == entry.id) it.copy(
                    state = state,
                    injury = patient?.criticalInjuries?.joinToString(" • ") { key ->
                        com.cyberpunk.gmtool.data.CriticalInjuries.byKey(key)?.faName ?: key
                    } ?: it.injury,
                    surgerySuccess = if (note.startsWith("درمان کامل")) it.surgerySuccess + 1 else it.surgerySuccess,
                    note = listOf(it.note, note).filter { s -> s.isNotBlank() }.joinToString(" • ")
                ) else it
            }
        )
    }
}

/**
 * ساخت NPC از تمپلیت آماده (بک‌آپ لاومن، Team Member اگزک و...).
 *
 * چرا لازم بود: تابلوی پشتیبان‌ها فقط اسم واحدها را نشان می‌داد و کاربر
 * نمی‌دانست باید خودش همه‌ی NPCها را بسازد یا نه. حالا همان لحظه که GM
 * می‌گوید «نیروها رسیدند»، با یک دکمه همان تعداد کاراکتر از تمپلیت ساخته
 * می‌شود و اگر نبردی فعال باشد، مستقیم به فهرست شرکت‌کننده‌ها اضافه می‌شود.
 */
fun CharacterViewModel.spawnNpcTemplates(
    templateName: String,
    count: Int,
    addToEncounter: Boolean = false
): String {
    val template = com.cyberpunk.gmtool.data.NpcData.allTemplates.firstOrNull { it.name.equals(templateName, true) }
        ?: return "تمپلیت «$templateName» در فهرست NPCها پیدا نشد."
    val base = com.cyberpunk.gmtool.data.NpcData.toCharacter(template)
    val n = count.coerceIn(1, 12)
    // نام گروهی مثل «Local Beat Cop (×4)» برای یک‌نفره‌سازی شماره نمی‌خورد؛
    // وقتی چند نفر ساخته می‌شود، پسوند تعداد از اسم برداشته می‌شود.
    val baseName = template.name.replace(Regex("\\s*\\(×\\d+\\)\\s*$"), "").trim().ifBlank { template.name }
    val created = (1..n).map { i ->
        base.copy(
            name = if (n > 1) "$baseName #$i" else baseName,
            handle = if (n > 1) "$baseName #$i" else baseName,
            npcCategory = template.category
        )
    }
    val saved = addMany(created)
    if (addToEncounter && saved.isNotEmpty()) {
        setCombatParticipants(combatParticipantIds + saved.map { it.id }, rollInitiative = false)
    }
    val enc = if (addToEncounter && saved.isNotEmpty()) " و به فهرست شرکت‌کننده‌های نبرد اضافه شدند" else ""
    return "${saved.size} NPC از تمپلیت «${template.name}» ساخته شد$enc."
}

// ═══════════════════ Team Memberهای اگزک (توانایی نقش Teamwork) ═══════════════════

/** کلید ذخیره‌ی Loyalty روی خودِ Team Member (بدون تغییر ساختار داده). */
const val EXEC_LOYALTY_KEY = "loyalty"

/** Loyalty فعلی یک Team Member؛ اگر ثبت نشده باشد ۰. */
fun teamMemberLoyalty(c: Character): Int = c.roleAbilityPoints[EXEC_LOYALTY_KEY] ?: 0

/**
 * استخدام یک Team Member از کلاس‌های کتاب Core.
 *
 * چرا این تابع لازم شد: دکمه‌ی قبلی فقط یک دیالوگ بسته بود و هیچ NPC ای
 * نمی‌ساخت. حالا با یک کلیک همان کلاس (Company Bodyguard / Covert Operative /
 * Driver / Netrunner / Technician) از تمپلیت آماده‌ی برنامه ساخته می‌شود،
 * Loyalty شروعش (1d6 + 1 طبق کتاب) روی خودش ذخیره می‌شود و اگر GM بخواهد
 * مستقیم به فهرست شرکت‌کننده‌های نبرد هم اضافه می‌شود.
 */
fun CharacterViewModel.hireExecTeamMember(
    templateName: String,
    startingLoyalty: Int,
    addToEncounter: Boolean = false
): String {
    val template = com.cyberpunk.gmtool.data.NpcData.allTemplates.firstOrNull { it.name.equals(templateName, true) }
        ?: return "کلاس «$templateName» در فهرست Team Memberها پیدا نشد."
    val npc = com.cyberpunk.gmtool.data.NpcData.toCharacter(template).copy(
        npcCategory = com.cyberpunk.gmtool.data.NpcData.CAT_EXEC,
        notes = "Team Member اگزک — Loyalty اولیه: $startingLoyalty",
        roleAbilityPoints = mapOf(EXEC_LOYALTY_KEY to startingLoyalty.coerceIn(0, 10))
    )
    val saved = addMany(listOf(npc))
    if (saved.isEmpty()) return "استخدام انجام نشد."
    if (addToEncounter) setCombatParticipants(combatParticipantIds + saved.map { it.id }, rollInitiative = false)
    val enc = if (addToEncounter) " و به فهرست شرکت‌کننده‌های نبرد اضافه شد" else ""
    return "«${template.name}» استخدام شد (Loyalty اولیه $startingLoyalty)$enc. هر Team Memberی که گم شود، جایگزینش ۲۰۰eb هزینه‌ی استخدام دارد و Loyalty شروعش ۱ می‌شود."
}

/** تغییر Loyalty یک Team Member با همان سقف کتاب (۰ تا ۱۰). */
fun CharacterViewModel.adjustTeamLoyalty(id: Int, delta: Int): Int {
    val c = getCharacter(id) ?: return 0
    val updated = (teamMemberLoyalty(c) + delta).coerceIn(0, 10)
    updateCharacter(id) { ch -> ch.copy(roleAbilityPoints = ch.roleAbilityPoints + (EXEC_LOYALTY_KEY to updated)) }
    return updated
}

/** همه‌ی Team Memberهای اگزک که در برنامه ساخته شده‌اند (دسته‌ی تیم اگزک). */
fun CharacterViewModel.execTeamMembers(): List<Character> =
    characters.value.filter { !it.isAlly && it.npcCategory == com.cyberpunk.gmtool.data.NpcData.CAT_EXEC }

