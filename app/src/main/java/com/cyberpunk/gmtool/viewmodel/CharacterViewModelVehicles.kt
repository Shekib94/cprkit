package com.cyberpunk.gmtool.viewmodel

// ─────────────────────────────────────────────────────────────
// دامنه‌ی وسیله‌های نقلیه (Nomad Motorpool، صندلی‌ها، شیشه، NOS،
// رانندگی، سلاح‌های نصب‌شده، Family Vehicle، تعمیر/فروش) به‌عنوان
// توابع توسعه روی CharacterViewModel.
//
// چرا این فایل: CharacterViewModel به ۲۷۰۰ خط رسیده بود و نگهداری
// یک فایل غول‌آسا سخت است. دامنه‌ها به فایل‌های هم‌بسته تقسیم شده‌اند؛
// API عمومی هیچ تغییری نکرده — UI همان `viewModel.foo(...)` را صدا
// می‌زند (فقط یک import ستاره‌ای از پکیج viewmodel لازم دارد).
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

fun CharacterViewModel.setNomadMotorpool(id: Int, vehicles: List<String>, upgrades: List<String> = emptyList()) =
    updateCharacter(id) { c ->
        val wanted = vehicles.distinct()
        val purchased = c.ownedVehicles.filterNot { it.source == "FAMILY" }
        val existingFamily = c.ownedVehicles.filter { it.source == "FAMILY" }.associateBy { it.specName }
        var family = wanted.mapNotNull { name ->
            val spec = com.cyberpunk.gmtool.data.VehicleCatalog.byName(name) ?: return@mapNotNull null
            val ups = upgrades.mapNotNull { raw -> raw.split("||", limit = 2).takeIf { it.size == 2 && it[0] == name }?.get(1) }
            (existingFamily[name] ?: com.cyberpunk.gmtool.data.OwnedVehicle(
                id = "family_${c.id}_${name.replace(" ", "_")}", specName = name, source = "FAMILY", currentSdp = spec.sdp
            )).copy(upgrades = ups)
        }
        // Moto 10 may have all Family Vehicles out. Below Rank 10, preserve at most one checked-out vehicle.
        family = if (c.role.equals("Nomad", true) && c.roleRank >= 10) {
            family.map { it.copy(familyCheckedOut = it.destroyedUntil <= 0L) }
        } else {
            val already = family.firstOrNull { it.familyCheckedOut && it.destroyedUntil <= 0L }?.id
                ?: family.firstOrNull { it.destroyedUntil <= 0L }?.id
            family.map { it.copy(familyCheckedOut = it.id == already) }
        }
        c.copy(
            nomadMotorpool = wanted,
            nomadVehicleUpgrades = upgrades,
            pendingFamilyVehicleId = c.pendingFamilyVehicleId?.takeIf { pid -> family.any { it.id == pid } },
            ownedVehicles = purchased + family
        )
    }

/** Active combat seating. Stored on each occupant so it survives screen changes without changing save schema. */
fun CharacterViewModel.vehicleOccupants(ownerId: Int, vehicleId: String): List<Character> {
    val prefix = "in_vehicle_${ownerId}_${vehicleId}_"
    return _characters.value.filter { c -> c.combatEffects.keys.any { it.startsWith(prefix) } }
}

fun CharacterViewModel.vehicleSeatRole(occupantId: Int, ownerId: Int, vehicleId: String): String? {
    val prefix = "in_vehicle_${ownerId}_${vehicleId}_"
    return getCharacter(occupantId)?.combatEffects?.keys?.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)
}

fun CharacterViewModel.assignVehicleSeat(ownerId: Int, vehicleId: String, occupantId: Int, role: String, seatLimit: Int): String {
    val owner = getCharacter(ownerId) ?: return "Vehicle owner not found"
    if (owner.ownedVehicles.none { it.id == vehicleId }) return "Vehicle not found"
    val occupants = vehicleOccupants(ownerId, vehicleId)
    if (occupants.none { it.id == occupantId } && occupants.size >= seatLimit.coerceAtLeast(1)) return "No free seats"
    if (role.equals("driver", true) && occupants.any { it.id != occupantId && vehicleSeatRole(it.id, ownerId, vehicleId) == "driver" }) return "Driver seat already occupied"
    updateCharacter(occupantId) { ch ->
        val clean = ch.combatEffects.filterKeys { !it.startsWith("in_vehicle_") }
        ch.copy(combatEffects = clean + ("in_vehicle_${ownerId}_${vehicleId}_${role.lowercase()}" to -1))
    }
    return "OK"
}

fun CharacterViewModel.leaveVehicle(occupantId: Int) = updateCharacter(occupantId) { ch ->
    ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith("in_vehicle_") })
}

/** Seating Upgrade ejector seat: removes the occupant from the vehicle and records the 10m/yd vertical launch. */
fun CharacterViewModel.ejectVehicleOccupant(ownerId: Int, vehicleId: String, occupantId: Int): String {
    val owner = getCharacter(ownerId) ?: return "Vehicle owner not found"
    val vehicle = owner.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return "Vehicle not found"
    if (vehicle.upgrades.none { it.equals("Seating Upgrade", true) }) return "Seating Upgrade required"
    if (vehicleOccupants(ownerId, vehicleId).none { it.id == occupantId }) return "Target is not in this vehicle"
    leaveVehicle(occupantId)
    addCombatEffect(occupantId, "ejected_10m", 1)
    return "EJECTED • occupant launched 10m/yd upward and is no longer seated. Resolve landing/fall from the actual scene."
}

fun CharacterViewModel.heavyWeaponGunner(ownerId: Int, vehicleId: String): Character? =
    vehicleOccupants(ownerId, vehicleId).firstOrNull { vehicleSeatRole(it.id, ownerId, vehicleId) == "gunner" }

/** ownerId + vehicle id for a combatant currently seated in a vehicle. */
fun CharacterViewModel.vehicleSeatInfo(occupantId: Int): Triple<Int, String, String>? {
    val key = getCharacter(occupantId)?.combatEffects?.keys?.firstOrNull { it.startsWith("in_vehicle_") } ?: return null
    val rest = key.removePrefix("in_vehicle_")
    val ownerText = rest.substringBefore('_')
    val ownerId = ownerText.toIntOrNull() ?: return null
    val afterOwner = rest.substringAfter('_')
    val role = afterOwner.substringAfterLast('_')
    val vehicleId = afterOwner.substringBeforeLast('_')
    return Triple(ownerId, vehicleId, role)
}

fun CharacterViewModel.resolveSuppressiveCover(id: Int) = updateCharacter(id) { ch ->
    ch.copy(combatEffects = (ch.combatEffects - "must_seek_cover_this_turn") + ("in_cover" to -1))
}

fun CharacterViewModel.leaveCover(id: Int) = clearCombatEffect(id, "in_cover")
fun CharacterViewModel.isInCover(id: Int): Boolean = getCharacter(id)?.combatEffects?.containsKey("in_cover") == true || combatShieldHp(id) > 0

/**
 * Core Bulletproof Glass: every window is separate cover (15 HP thin / 30 HP thick).
 * We key the pane to the occupant being shot so driver/passenger windows do not share damage.
 * The legacy no-window overload is retained for old UI/save compatibility.
 */
private fun glassPanePrefix(vehicleId: String, paneKey: String) = "vehicle_glass_damage_${vehicleId}_${paneKey}_"

fun CharacterViewModel.vehicleGlassHp(ownerId: Int, vehicleId: String, paneKey: String = "shared"): Int {
    val owner = getCharacter(ownerId) ?: return 0
    val vehicle = owner.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return 0
    val installs = vehicle.upgrades.count { it.equals("Bulletproof Glass", true) }.coerceIn(0, 2)
    if (installs == 0) return 0
    val maxHp = installs * 15
    val prefix = glassPanePrefix(vehicleId, paneKey)
    val damage = owner.combatEffects.keys.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull() ?: 0
    return (maxHp - damage).coerceAtLeast(0)
}

fun CharacterViewModel.vehicleGlassHpForOccupant(ownerId: Int, vehicleId: String, occupantId: Int): Int =
    vehicleGlassHp(ownerId, vehicleId, "occ$occupantId")

fun CharacterViewModel.damageVehicleGlass(ownerId: Int, vehicleId: String, damage: Int, paneKey: String = "shared"): Int {
    val owner = getCharacter(ownerId) ?: return 0
    val vehicle = owner.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return 0
    val maxHp = vehicle.upgrades.count { it.equals("Bulletproof Glass", true) }.coerceIn(0, 2) * 15
    if (maxHp <= 0) return 0
    val before = vehicleGlassHp(ownerId, vehicleId, paneKey)
    val absorbed = minOf(before, damage.coerceAtLeast(0))
    val newHp = (before - damage.coerceAtLeast(0)).coerceAtLeast(0)
    val newDamage = (maxHp - newHp).coerceIn(0, maxHp)
    updateCharacter(ownerId) { ch ->
        val prefix = glassPanePrefix(vehicleId, paneKey)
        ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith(prefix) } + ("${prefix}${newDamage}" to -1))
    }
    return absorbed
}

fun CharacterViewModel.damageVehicleGlassForOccupant(ownerId: Int, vehicleId: String, occupantId: Int, damage: Int): Int =
    damageVehicleGlass(ownerId, vehicleId, damage, "occ$occupantId")

fun CharacterViewModel.repairVehicleGlass(ownerId: Int, vehicleId: String) = updateCharacter(ownerId) { ch ->
    val prefix = "vehicle_glass_damage_${vehicleId}_"
    ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith(prefix) })
}

fun CharacterViewModel.applyRamWhiplashToOccupants(ownerId: Int, vehicleId: String, protectedByCombatPlow: Boolean): Int {
    if (protectedByCombatPlow) return 0
    val occupants = vehicleOccupants(ownerId, vehicleId)
    occupants.forEach { addCriticalInjury(it.id, "whiplash") }
    return occupants.size
}

/** Resolve a Core vehicle-vs-vehicle ram with persistent SDP and occupant Whiplash. */
fun CharacterViewModel.resolveVehicleRam(attackerOwnerId: Int, attackerVehicleId: String, targetOwnerId: Int, targetVehicleId: String, rawDamage: Int): String {
    if (attackerOwnerId == targetOwnerId && attackerVehicleId == targetVehicleId) return "Choose a different target vehicle."
    val attackerOwner = getCharacter(attackerOwnerId) ?: return "Attacker owner not found"
    val targetOwner = getCharacter(targetOwnerId) ?: return "Target owner not found"
    val attacker = attackerOwner.ownedVehicles.firstOrNull { it.id == attackerVehicleId } ?: return "Attacker vehicle not found"
    val target = targetOwner.ownedVehicles.firstOrNull { it.id == targetVehicleId } ?: return "Target vehicle not found"
    if (attacker.currentSdp <= 0) return "Attacker vehicle is destroyed."
    if (target.currentSdp <= 0) return "Target vehicle is already destroyed."
    val plow = attacker.upgrades.any { it.equals("Combat Plow", true) }
    val targetDamage = damageVehicleAttack(targetOwnerId, targetVehicleId, rawDamage)
    val selfDamage = if (plow) 0 else damageVehicleAttack(attackerOwnerId, attackerVehicleId, rawDamage)
    val targetWhiplash = applyRamWhiplashToOccupants(targetOwnerId, targetVehicleId, false)
    val attackerWhiplash = applyRamWhiplashToOccupants(attackerOwnerId, attackerVehicleId, plow)
    val aLeft = getCharacter(attackerOwnerId)?.ownedVehicles?.firstOrNull { it.id == attackerVehicleId }?.currentSdp ?: 0
    val tLeft = getCharacter(targetOwnerId)?.ownedVehicles?.firstOrNull { it.id == targetVehicleId }?.currentSdp ?: 0
    val attackerNote = if (plow) "Combat Plow protected attacker vehicle + occupants" else gtr("attacker Whiplash: %1s", attackerWhiplash)
    return gtr("RAM RESOLVED • raw %1s • target SDP -%2s → %3s • attacker SDP -%4s → %5s • %6s • target Whiplash: %7s", rawDamage, targetDamage, tLeft, selfDamage, aLeft, attackerNote, targetWhiplash)
}

/** Core vehicle-vs-pedestrian Ram after the pedestrian fails the DV13 Evasion check. */
fun CharacterViewModel.resolveVehicleRamPedestrian(attackerOwnerId: Int, attackerVehicleId: String, pedestrianId: Int, rawDamage: Int): String {
    val attackerOwner = getCharacter(attackerOwnerId) ?: return "Attacker owner not found"
    val pedestrian = getCharacter(pedestrianId) ?: return "Pedestrian not found"
    val attacker = attackerOwner.ownedVehicles.firstOrNull { it.id == attackerVehicleId } ?: return "Attacker vehicle not found"
    if (attacker.currentSdp <= 0) return "Attacker vehicle is destroyed."
    if (pedestrian.isDead) return "Pedestrian is already dead."
    val plow = attacker.upgrades.any { it.equals("Combat Plow", true) }
    val selfDamage = if (plow) 0 else damageVehicleAttack(attackerOwnerId, attackerVehicleId, rawDamage)
    val pedestrianResolution = applyCombatDamage(pedestrianId, rawDamage, head = false, melee = false, critical = false)
    addCriticalInjury(pedestrianId, "whiplash")
    val attackerWhiplash = applyRamWhiplashToOccupants(attackerOwnerId, attackerVehicleId, plow)
    val aLeft = getCharacter(attackerOwnerId)?.ownedVehicles?.firstOrNull { it.id == attackerVehicleId }?.currentSdp ?: 0
    val pNow = getCharacter(pedestrianId)
    val climbNote = if (pNow != null && !pNow.isDead && pNow.hp > 0) " • pedestrian may choose to end on top of the vehicle" else ""
    val attackerNote = if (plow) "Combat Plow protected attacker vehicle + occupants" else "attacker Whiplash: $attackerWhiplash"
    return gtr("PEDESTRIAN RAM • raw %1s • pedestrian HP -%2s + Whiplash • attacker SDP -%3s → %4s • %5s%6s", rawDamage, pedestrianResolution?.hpDamage ?: 0, selfDamage, aLeft, attackerNote, climbNote)
}

private fun vehicleControlSkillName(spec: com.cyberpunk.gmtool.data.VehicleSpec): String = when (spec.domain) {
    com.cyberpunk.gmtool.data.VehicleDomain.LAND -> "Drive Land Vehicle"
    com.cyberpunk.gmtool.data.VehicleDomain.AIR -> "Pilot Air Vehicle"
    com.cyberpunk.gmtool.data.VehicleDomain.SEA -> "Pilot Sea Vehicle"
}

private fun nosSpentPrefix(vehicleId: String) = "vehicle_nos_spent_${vehicleId}_"
fun CharacterViewModel.vehicleNosSpent(ownerId: Int, vehicleId: String): Int {
    val prefix = nosSpentPrefix(vehicleId)
    return getCharacter(ownerId)?.combatEffects?.keys?.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull() ?: 0
}
fun CharacterViewModel.vehicleNosCapacity(ownerId: Int, vehicleId: String): Int =
    getCharacter(ownerId)?.ownedVehicles?.firstOrNull { it.id == vehicleId }?.upgrades?.count { it.equals("NOS", true) } ?: 0
fun CharacterViewModel.vehicleNosRemaining(ownerId: Int, vehicleId: String): Int = (vehicleNosCapacity(ownerId, vehicleId) - vehicleNosSpent(ownerId, vehicleId)).coerceAtLeast(0)

/** Core NOS: one Action grants one additional Move Action; every installed tank is usable once per day. */
fun CharacterViewModel.useVehicleNos(ownerId: Int, vehicleId: String): String {
    val cap = vehicleNosCapacity(ownerId, vehicleId)
    if (cap <= 0) return "This vehicle has no NOS tank."
    val spent = vehicleNosSpent(ownerId, vehicleId)
    if (spent >= cap) return "All NOS tanks have already been used today."
    val prefix = nosSpentPrefix(vehicleId)
    updateCharacter(ownerId) { ch -> ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith(prefix) } + ("$prefix${spent + 1}" to -1)) }
    return gtr("NOS USED • tank %1s/%2s • Action grants one additional Move Action this Turn.", spent + 1, cap)
}

fun CharacterViewModel.resetVehicleNosDaily(ownerId: Int, vehicleId: String) = updateCharacter(ownerId) { ch ->
    val prefix = nosSpentPrefix(vehicleId)
    ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith(prefix) })
}

fun CharacterViewModel.vehicleLostControl(ownerId: Int, vehicleId: String): Boolean =
    getCharacter(ownerId)?.combatEffects?.containsKey("vehicle_lost_control_$vehicleId") == true

fun CharacterViewModel.clearVehicleLostControl(ownerId: Int, vehicleId: String) = updateCharacter(ownerId) { ch ->
    ch.copy(combatEffects = ch.combatEffects - "vehicle_lost_control_$vehicleId")
}

/** Core Basic Driving. Base >9 needs no check/action; otherwise DV10 and failure loses control. */
fun CharacterViewModel.resolveBasicDriving(driverId: Int, ownerId: Int, vehicleId: String): String {
    val driver = getCharacter(driverId) ?: return "Driver not found"
    val owner = getCharacter(ownerId) ?: return "Vehicle owner not found"
    val vehicle = owner.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return "Vehicle not found"
    val spec = com.cyberpunk.gmtool.data.VehicleCatalog.byName(vehicle.specName) ?: return "Vehicle spec not found"
    val skillName = vehicleControlSkillName(spec)
    val moto = if (driver.role.equals("Nomad", true)) driver.roleRank else 0
    val skill = com.cyberpunk.gmtool.data.CombatRules.skillLevel(driver, skillName) + moto
    val effectiveRef = com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(driver, "REF", driver.stats.ref) + com.cyberpunk.gmtool.data.GameRules.armorPenalty(driver.inventory)
    val base = effectiveRef + skill
    if (base > 9) return gtr("BASIC DRIVING AUTO • effective REF + %1s (+Moto) = %2s > 9 • no Action/check required.", skillName, base)
    val roll = com.cyberpunk.gmtool.data.CombatRules.rollD10()
    val total = base + roll.totalDie + com.cyberpunk.gmtool.data.CombatRules.allActionsPenalty(driver)
    val success = total > 10
    if (!success) updateCharacter(ownerId) { it.copy(combatEffects = it.combatEffects + ("vehicle_lost_control_$vehicleId" to -1)) }
    return gtr("BASIC DRIVING %1s • %2s + %3s = %4s vs DV10 • Action used.", if (success) "SUCCESS" else "FAIL → LOSE CONTROL", base, roll.totalDie, total)
}

/** Core Maneuver: Action + Move Action. Failure marks Lost Control for GM-directed movement/crash resolution. */
fun CharacterViewModel.resolveVehicleManeuver(driverId: Int, ownerId: Int, vehicleId: String, dv: Int): String {
    val driver = getCharacter(driverId) ?: return "Driver not found"
    val owner = getCharacter(ownerId) ?: return "Vehicle owner not found"
    val vehicle = owner.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return "Vehicle not found"
    val spec = com.cyberpunk.gmtool.data.VehicleCatalog.byName(vehicle.specName) ?: return "Vehicle spec not found"
    val skillName = vehicleControlSkillName(spec)
    val moto = if (driver.role.equals("Nomad", true)) driver.roleRank else 0
    val skill = com.cyberpunk.gmtool.data.CombatRules.skillLevel(driver, skillName) + moto
    val roll = com.cyberpunk.gmtool.data.CombatRules.rollD10()
    val effectiveRef = com.cyberpunk.gmtool.data.EquipmentUseRules.effectiveStat(driver, "REF", driver.stats.ref)
    val total = effectiveRef + skill + roll.totalDie + com.cyberpunk.gmtool.data.GameRules.armorPenalty(driver.inventory) + com.cyberpunk.gmtool.data.CombatRules.allActionsPenalty(driver)
    val success = total > dv
    if (!success) updateCharacter(ownerId) { it.copy(combatEffects = it.combatEffects + ("vehicle_lost_control_$vehicleId" to -1)) }
    return gtr("MANEUVER %1s • REF %2s + %3s %4s + %5s = %6s vs DV%7s • Action + Move Action used.", if (success) "CONTROLLED" else "FAIL → LOSE CONTROL", driver.stats.ref, skillName, skill, roll.totalDie, total, dv)
}

/** Lost Control itself does not roll damage; GM chooses movement. Use this to resolve the resulting impact as a normal ram. */
fun CharacterViewModel.resolveLostControlCrash(ownerId: Int, vehicleId: String, targetOwnerId: Int, targetVehicleId: String): String {
    if (!vehicleLostControl(ownerId, vehicleId)) return "Vehicle is not marked as having Lost Control."
    val d = com.cyberpunk.gmtool.data.CombatRules.rollDamage("6d6")
    val result = resolveVehicleRam(ownerId, vehicleId, targetOwnerId, targetVehicleId, d.total)
    clearVehicleLostControl(ownerId, vehicleId)
    return gtr("LOST CONTROL CRASH • %1s=%2s • %3s", d.dice.joinToString("+"), d.total, result)
}

private fun mountAmmoPrefix(vehicleId: String, kind: String, index: Int) = "vehicle_mount_ammo_${vehicleId}_${kind.replace(" ", "-")}_${index}_"
private fun mountedWeaponMaxAmmo(kind: String): Int = when {
    kind.equals("Onboard Machine gun", true) -> 30
    kind.equals("Onboard Rocket Pod", true) -> 3
    kind.equals("Onboard Flamethrower", true) -> 4
    else -> 0
}

fun CharacterViewModel.mountedWeaponAmmo(ownerId: Int, vehicleId: String, kind: String, index: Int): Int {
    val max = mountedWeaponMaxAmmo(kind)
    if (max <= 0) return 0
    val prefix = mountAmmoPrefix(vehicleId, kind, index)
    return getCharacter(ownerId)?.combatEffects?.keys?.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull()?.coerceIn(0, max) ?: max
}

fun CharacterViewModel.fireMountedWeapon(ownerId: Int, vehicleId: String, kind: String, index: Int): String {
    val owner = getCharacter(ownerId) ?: return "Vehicle owner not found"
    val vehicle = owner.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return "Vehicle not found"
    if (vehicle.upgrades.count { it.equals(kind, true) } <= index) return "Mounted weapon not found"
    val max = mountedWeaponMaxAmmo(kind)
    if (max <= 0) return if (kind.equals("Onboard Melee Weapon", true)) "ONBOARD MELEE • Very Heavy Melee Weapon attack; resolve hit normally." else "No ammo model for this mount."
    val before = mountedWeaponAmmo(ownerId, vehicleId, kind, index)
    val cost = if (kind.equals("Onboard Machine gun", true)) 10 else 1
    if (before < cost) return gtr("NOT ENOUGH AMMO • %1s/%2s remaining. Cannot reload while driving.", before, max)
    val after = before - cost
    val prefix = mountAmmoPrefix(vehicleId, kind, index)
    updateCharacter(ownerId) { ch -> ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith(prefix) } + ("$prefix$after" to -1)) }
    return when {
        kind.equals("Onboard Machine gun", true) -> gtr("ONBOARD MACHINE GUN • Autofire only • spent 10 rounds • %1s/%2s remain.", after, max)
        kind.equals("Onboard Rocket Pod", true) -> gtr("ONBOARD ROCKET POD • fired 1 rocket • %1s/%2s remain.", after, max)
        kind.equals("Onboard Flamethrower", true) -> gtr("ONBOARD FLAMETHROWER • Heavy Weapons • incendiary shotgun profile • %1s/%2s remain; fire deals 4 HP at end of Turns until extinguished.", after, max)
        else -> gtr("Mounted weapon fired • %1s/%2s remain.", after, max)
    }
}

/** Reloading mounted ranged weapons is only allowed while not driving. */
fun CharacterViewModel.reloadMountedWeapon(ownerId: Int, vehicleId: String, kind: String, index: Int, vehicleRunning: Boolean): String {
    if (vehicleRunning) return "Cannot reload this mounted weapon while driving."
    val max = mountedWeaponMaxAmmo(kind)
    if (max <= 0) return "This mount does not use reloadable ammo."
    val prefix = mountAmmoPrefix(vehicleId, kind, index)
    updateCharacter(ownerId) { ch -> ch.copy(combatEffects = ch.combatEffects.filterKeys { !it.startsWith(prefix) }) }
    return gtr("Mounted weapon reloaded to %1s.", max)
}

fun CharacterViewModel.setActiveVehicle(characterId: Int, vehicleId: String) = updateCharacter(characterId) { c ->
    c.copy(ownedVehicles = c.ownedVehicles.map { it.copy(active = it.id == vehicleId) })
}

/** Request a Family Motorpool vehicle. Below Moto 10, swapping a checked-out Family Vehicle completes next morning. */
fun CharacterViewModel.requestFamilyVehicle(characterId: Int, vehicleId: String): String {
    val c = getCharacter(characterId) ?: return "Character not found"
    val target = c.ownedVehicles.firstOrNull { it.id == vehicleId && it.source == "FAMILY" } ?: return "Family vehicle not found"
    if (target.destroyedUntil > 0L) return "این Family Vehicle هنوز ${target.destroyedUntil} روز تا پایان تعمیر فاصله دارد."
    if (c.role.equals("Nomad", true) && c.roleRank >= 10) {
        updateCharacter(characterId) { ch -> ch.copy(
            pendingFamilyVehicleId = null,
            ownedVehicles = ch.ownedVehicles.map { if (it.source == "FAMILY" && it.destroyedUntil <= 0L) it.copy(familyCheckedOut = true) else it }
        ) }
        return "Moto 10 • همه‌ی Family Vehicleهای سالم می‌توانند هم‌زمان بیرون باشند."
    }
    if (target.familyCheckedOut) return "این Family Vehicle همین حالا بیرون از Motorpool است."
    val current = c.ownedVehicles.firstOrNull { it.source == "FAMILY" && it.familyCheckedOut }
    if (current == null) {
        updateCharacter(characterId) { ch -> ch.copy(
            pendingFamilyVehicleId = null,
            ownedVehicles = ch.ownedVehicles.map { if (it.source == "FAMILY") it.copy(familyCheckedOut = it.id == vehicleId) else it }
        ) }
        return "Family Vehicle تحویل شد."
    }
    updateCharacter(characterId) { it.copy(pendingFamilyVehicleId = vehicleId) }
    return "SWAP REQUESTED • ${current.specName} با ${target.specName} صبح روز بعد تعویض می‌شود، به شرط نزدیک بودن Family."
}

/** Resolve the following-morning Family Motorpool swap. */
fun CharacterViewModel.completeFamilyVehicleSwapNextMorning(characterId: Int): String {
    val c = getCharacter(characterId) ?: return "Character not found"
    val pending = c.pendingFamilyVehicleId ?: return "هیچ تعویض Family Vehicle در انتظار نیست."
    val target = c.ownedVehicles.firstOrNull { it.id == pending && it.source == "FAMILY" } ?: return "Family vehicle not found"
    if (target.destroyedUntil > 0L) return "خودروی درخواستی هنوز در تعمیر است."
    updateCharacter(characterId) { ch -> ch.copy(
        pendingFamilyVehicleId = null,
        ownedVehicles = ch.ownedVehicles.map { v -> if (v.source == "FAMILY") v.copy(familyCheckedOut = v.id == pending) else v }
    ) }
    return "NEXT MORNING • ${target.specName} از Family Motorpool تحویل شد."
}

/** Send a destroyed Family Vehicle to the Family for the Core one-week/500eb full repair. */
fun CharacterViewModel.sendDestroyedFamilyVehicleForRepair(characterId: Int, vehicleId: String): String {
    val c = getCharacter(characterId) ?: return "Character not found"
    val v = c.ownedVehicles.firstOrNull { it.id == vehicleId && it.source == "FAMILY" } ?: return "Family vehicle not found"
    if (v.currentSdp > 0) return "این Family Vehicle نابود نشده است؛ تعمیرات روزمره مسئولیت Nomad است."
    if (v.destroyedUntil > 0L) return gtr("Family repair already in progress • %1s days remaining.", v.destroyedUntil)
    if (c.eurodollars < 500) return "برای Family repair به 500eb نیاز داری. GM می‌تواند در صورت بی‌پولی هزینه را waive کند و Reputation consequence را جدا اعمال کند."
    updateCharacter(characterId) { ch -> ch.copy(
        eurodollars = ch.eurodollars - 500,
        pendingFamilyVehicleId = ch.pendingFamilyVehicleId?.takeUnless { it == vehicleId },
        ownedVehicles = ch.ownedVehicles.map { if (it.id == vehicleId) it.copy(familyCheckedOut = false, active = false, destroyedUntil = 7L) else it }
    ) }
    return "FAMILY REPAIR STARTED • 500eb paid • 7 in-game days remaining."
}

/** Advance one in-game day for Family repair timers; completed repairs return at full SDP. */
fun CharacterViewModel.advanceFamilyRepairDay(characterId: Int): String {
    val c = getCharacter(characterId) ?: return "Character not found"
    if (c.ownedVehicles.none { it.source == "FAMILY" && it.destroyedUntil > 0L }) return "هیچ Family Vehicle در تعمیر نیست."
    val completed = mutableListOf<String>()
    updateCharacter(characterId) { ch -> ch.copy(ownedVehicles = ch.ownedVehicles.map { v ->
        if (v.source != "FAMILY" || v.destroyedUntil <= 0L) v else {
            val left = (v.destroyedUntil - 1L).coerceAtLeast(0L)
            if (left == 0L) {
                val spec = com.cyberpunk.gmtool.data.VehicleCatalog.byName(v.specName)
                val max = (spec?.sdp ?: v.currentSdp) + if (v.upgrades.any { it.equals("Heavy Chassis", true) }) 20 else 0
                completed += v.specName
                v.copy(destroyedUntil = 0L, currentSdp = max)
            } else v.copy(destroyedUntil = left)
        }
    }) }
    return if (completed.isEmpty()) "Family repair timers advanced by 1 day." else gtr("REPAIR COMPLETE • %1s returned at full SDP.", completed.joinToString())
}

fun CharacterViewModel.isVehicleRunning(ownerId: Int, vehicleId: String): Boolean =
    getCharacter(ownerId)?.combatEffects?.containsKey("vehicle_running_$vehicleId") == true

fun CharacterViewModel.setVehicleRunning(ownerId: Int, vehicleId: String, running: Boolean): String {
    val owner = getCharacter(ownerId) ?: return "Vehicle owner not found"
    val vehicle = owner.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return "Vehicle not found"
    if (running && vehicle.currentSdp <= 0) return "Destroyed vehicle cannot be started."
    if (running && vehicle.source == "FAMILY" && owner.roleRank < 10 && !vehicle.familyCheckedOut) return "این Family Vehicle داخل Motorpool است؛ ابتدا آن را درخواست کن و در صورت تعویض تا صبح روز بعد صبر کن."
    if (running && vehicle.source == "FAMILY" && vehicle.destroyedUntil > 0L) return "این Family Vehicle در تعمیر است."
    updateCharacter(ownerId) { ch ->
        ch.copy(combatEffects = if (running) ch.combatEffects + ("vehicle_running_$vehicleId" to -1) else ch.combatEffects - "vehicle_running_$vehicleId")
    }
    return if (running) "Vehicle started." else "Vehicle stopped."
}

fun CharacterViewModel.damageVehicle(characterId: Int, vehicleId: String, damage: Int): Int {
    var left = 0
    updateCharacter(characterId) { c -> c.copy(ownedVehicles = c.ownedVehicles.map { v ->
        if (v.id != vehicleId) v else v.copy(currentSdp = (v.currentSdp - damage.coerceAtLeast(0)).coerceAtLeast(0)).also { left = it.currentSdp }
    }) }
    return left
}

fun CharacterViewModel.repairVehicleToFull(characterId: Int, vehicleId: String, dv: Int): String {
    val c = getCharacter(characterId) ?: return "Character not found"
    val v = c.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return "Vehicle not found"
    val spec = com.cyberpunk.gmtool.data.VehicleCatalog.byName(v.specName) ?: return "Vehicle spec not found"
    if (v.source == "FAMILY" && v.currentSdp <= 0) return "Destroyed Family Vehicle: use Family repair (500eb, one week). Daily/non-destroyed repairs remain the Nomad's responsibility."
    val skillName = when (spec.domain) {
        com.cyberpunk.gmtool.data.VehicleDomain.LAND -> "Land Vehicle Tech"
        com.cyberpunk.gmtool.data.VehicleDomain.AIR -> "Air Vehicle Tech"
        com.cyberpunk.gmtool.data.VehicleDomain.SEA -> "Sea Vehicle Tech"
    }
    val moto = if (c.role.equals("Nomad", true)) c.roleRank else 0
    val skill = com.cyberpunk.gmtool.data.CombatRules.skillLevel(c, skillName) + moto
    val roll = com.cyberpunk.gmtool.data.CombatRules.rollD10()
    val total = c.stats.tech + skill + roll.totalDie
    val time = when (dv) { 9 -> "3 Hours"; 13 -> "1 Day"; else -> "1 Week" }
    if (total <= dv) return gtr("REPAIR FAIL • TECH %1s + %2s %3s + %4s = %5s vs DV%6s. Halfway through %7s you discover the repair must restart.", c.stats.tech, skillName, skill, roll.totalDie, total, dv, time)
    val max = spec.sdp + if (v.upgrades.any { it.equals("Heavy Chassis", true) }) 20 else 0
    updateCharacter(characterId) { ch ->
        val glassPrefix = "vehicle_glass_damage_${vehicleId}_"
        ch.copy(
            ownedVehicles = ch.ownedVehicles.map { if (it.id == vehicleId) it.copy(currentSdp = max) else it },
            combatEffects = ch.combatEffects.filterKeys { !it.startsWith(glassPrefix) }
        )
    }
    return gtr("REPAIR SUCCESS • %1s vs DV%2s • restored to %3s SDP after %4s; vehicle glass restored too.", total, dv, max, time)
}

fun CharacterViewModel.damageVehicleAttack(characterId: Int, vehicleId: String, rawDamage: Int, weakPoint: Boolean = false): Int {
    var dealt = 0
    updateCharacter(characterId) { c -> c.copy(ownedVehicles = c.ownedVehicles.map { v ->
        if (v.id != vehicleId) v else {
            val sp = if (v.upgrades.any { it.equals("Armored Chassis", true) }) 13 else 0
            val penetrating = (rawDamage - sp).coerceAtLeast(0)
            dealt = if (weakPoint) penetrating * 2 else penetrating
            v.copy(currentSdp = (v.currentSdp - dealt).coerceAtLeast(0))
        }
    }) }
    return dealt
}

fun CharacterViewModel.sellVehicle(characterId: Int, vehicleId: String, salePrice: Int): Boolean {
    val c = getCharacter(characterId) ?: return false
    val v = c.ownedVehicles.firstOrNull { it.id == vehicleId } ?: return false
    if (v.source == "FAMILY" || salePrice < 0) return false
    updateCharacter(characterId) { ch -> ch.copy(eurodollars = ch.eurodollars + salePrice, ownedVehicles = ch.ownedVehicles.filterNot { it.id == vehicleId }) }
    return true
}

