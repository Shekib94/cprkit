package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CombatRules
import com.cyberpunk.gmtool.data.CriticalInjuries
import com.cyberpunk.gmtool.data.GameRules
import com.cyberpunk.gmtool.data.EquipmentUseRules
import com.cyberpunk.gmtool.data.Weapon
import com.cyberpunk.gmtool.data.VehicleCatalog
import com.cyberpunk.gmtool.data.VehicleDomain
import com.cyberpunk.gmtool.data.RuleReferenceData
import com.cyberpunk.gmtool.ui.components.FaText
import com.cyberpunk.gmtool.ui.components.RuleInfoButton
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.viewmodel.CombatUiRuntime
import kotlinx.coroutines.launch
import kotlin.math.min
// توابع توسعه‌ی دامنه‌های تفکیک‌شده‌ی ViewModel (vehicle/combat/…) در همین پکیج‌اند.
import com.cyberpunk.gmtool.viewmodel.*

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)
private val CardBg = Color(0xFF1A1A1A)

private data class AttackUiResult(
    val weaponName: String,
    val rollText: String,
    val attackTotal: Int,
    val defenseText: String,
    val hit: Boolean?,
    val damageText: String = "",
    val targetText: String = "",
    val jammed: Boolean = false,
    val rawDamage: Int? = null,
    val armorUsed: Int? = null,
    val hpDamage: Int? = null,
    val armorAblated: Int? = null,
    val criticalName: String? = null,
    val resolutionLabel: String = "",
    val resolutionNote: String = ""
)

@Composable
private fun DamageStepCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        shape = CutCornerShape(6.dp),
        modifier = modifier.border(1.dp, Red.copy(alpha = 0.28f), CutCornerShape(6.dp))
    ) {
        Column(Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(gtr(label), color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private data class VehicleTargetRef(val ownerId: Int, val vehicleId: String, val label: String)

@Composable
fun CombatTab(character: Character, viewModel: CharacterViewModel) {
    val allCharacters by viewModel.characters.collectAsState()
    val dataGeneration by viewModel.dataGeneration.collectAsState()
    val scope = rememberCoroutineScope()
    val savedRuntime = remember(character.id, dataGeneration) { viewModel.loadCombatUiRuntime(character.id) }
    val encounterCharacters = allCharacters.filter { it.id in viewModel.combatParticipantIds }
    var targetId by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.targetId) }
    val availableTargets = (if (encounterCharacters.isNotEmpty()) encounterCharacters else allCharacters).filter { it.id != character.id && !it.isDead }
    LaunchedEffect(availableTargets.map { it.id }) {
        if (targetId != null && availableTargets.none { it.id == targetId }) targetId = null
    }
    val target = availableTargets.firstOrNull { it.id == targetId }

    var attackResult by remember { mutableStateOf<AttackUiResult?>(null) }
    var distanceText by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.distanceText) }
    var targetDodges by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.targetDodges) }
    var aimTarget by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.aimTarget) } // None / Head / Held Item / Leg
    var heldItemWeaponId by remember { mutableStateOf<Int?>(null) }
    var grabItemWeaponId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(targetId) { heldItemWeaponId = null; grabItemWeaponId = null }
    var aimedMenu by remember { mutableStateOf(false) }
    var actionResult by remember { mutableStateOf<String?>(null) }
    var combatHelp by remember { mutableStateOf<Pair<String, String>?>(null) }
    var targetMenu by remember { mutableStateOf(false) }
    var criticalHealerId by remember(character.id) { mutableStateOf<Int?>(character.id) }
    var criticalHealerMenu by remember { mutableStateOf(false) }
    var showBlastDialog by remember { mutableStateOf(false) }
    var reloadWeaponId by remember { mutableStateOf<Int?>(null) }
    var blastTargetIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var blastCoveredIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var sandeActiveUntil by remember(character.id, dataGeneration) { mutableLongStateOf(savedRuntime.sandeActiveUntil) }
    var sandeCooldownUntil by remember(character.id, dataGeneration) { mutableLongStateOf(savedRuntime.sandeCooldownUntil) }
    var attackActionUsed by remember(character.id, dataGeneration) { mutableIntStateOf(savedRuntime.attackActionUsed) }
    var attackActionCap by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.attackActionCap) }
    // وقتی روشن باشد، محدودیت یک Action در نوبت نادیده گرفته می‌شود.
    // عمداً ذخیره نمی‌شود: هر بار GM باید آگاهانه روشنش کند.
    var gmOverrideAction by remember(character.id) { mutableStateOf(false) }
    var spotWeaknessUsed by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.spotWeaknessUsed) }
    var damageDeflectionUsed by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.damageDeflectionUsed) }
    var coverHp by remember(character.id, dataGeneration) { mutableIntStateOf(savedRuntime.coverHp) }
    var coverMaxHp by remember(character.id, dataGeneration) { mutableIntStateOf(savedRuntime.coverMaxHp) }
    var coverLabel by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.coverLabel) }
    var coverDamageExpr by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.coverDamageExpr) }
    var selectedVehicleId by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.selectedVehicleId ?: character.ownedVehicles.firstOrNull { it.active }?.id ?: character.ownedVehicles.firstOrNull()?.id) }
    var vehicleMenu by remember { mutableStateOf(false) }
    var maneuverDv by remember(character.id, dataGeneration) { mutableIntStateOf(savedRuntime.maneuverDv) }
    var ramTargetKey by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.ramTargetKey) }
    var ramTargetMenu by remember { mutableStateOf(false) }
    var ramNosBoosted by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.ramNosBoosted) }

    // Keep the combat screen readable: only the parts the GM needs right now need to stay open.
    var criticalExpanded by remember(character.id) { mutableStateOf(character.criticalInjuries.isNotEmpty()) }
    var effectsExpanded by remember(character.id) { mutableStateOf(false) }
    var targetExpanded by remember(character.id) { mutableStateOf(false) }
    // حالت ورودی حمله: با «سلاح» سلاح انتخاب می‌شود، با «بدن» اکشن‌های بدن/Grapple.
    var attackMode by remember(character.id) { mutableStateOf("Weapon") }
    var coverExpanded by remember(character.id) { mutableStateOf(false) }
    var martialExpanded by remember(character.id) { mutableStateOf(false) }
    var vehicleExpanded by remember(character.id) { mutableStateOf(false) }
    var defenseMode by remember(character.id) { mutableStateOf("Auto") } // Auto / Dodge / Shield / Human Shield / Range DV / Cover
    var selectedAttackWeaponId by remember(character.id, dataGeneration) { mutableStateOf<Int?>(savedRuntime.selectedAttackWeaponId ?: character.weapons.firstOrNull { it.isEquipped }?.id) }
    var selectedAttackMode by remember(character.id, dataGeneration) { mutableStateOf(savedRuntime.selectedAttackMode) } // Single / Autofire

    LaunchedEffect(
        character.id, dataGeneration, targetId, distanceText, targetDodges, aimTarget,
        sandeActiveUntil, sandeCooldownUntil, attackActionUsed, attackActionCap, spotWeaknessUsed,
        damageDeflectionUsed, coverHp, coverMaxHp, coverLabel, coverDamageExpr, selectedVehicleId,
        maneuverDv, ramTargetKey, ramNosBoosted, selectedAttackWeaponId, selectedAttackMode,
        viewModel.combatParticipantIds
    ) {
        if (viewModel.combatParticipantIds.isEmpty()) return@LaunchedEffect
        viewModel.saveCombatUiRuntime(character.id, CombatUiRuntime(
            targetId, distanceText, targetDodges, aimTarget, savedRuntime.heldActionSummary,
            sandeActiveUntil, sandeCooldownUntil, attackActionUsed, attackActionCap, spotWeaknessUsed,
            damageDeflectionUsed, coverHp, coverMaxHp, coverLabel, coverDamageExpr, selectedVehicleId,
            maneuverDv, ramTargetKey, ramNosBoosted, selectedAttackWeaponId, selectedAttackMode
        ))
    }

    var observedCombatRound by remember(character.id, dataGeneration) { mutableIntStateOf(viewModel.combatRound) }
    var observedTurnIndex by remember(character.id, dataGeneration) { mutableIntStateOf(viewModel.combatTurnIndex) }
    LaunchedEffect(viewModel.combatRound) {
        // Do not erase a restored mid-Round state on first composition; reset only on a real Round transition.
        if (viewModel.combatRound != observedCombatRound) {
            spotWeaknessUsed = false
            damageDeflectionUsed = emptySet()
            ramNosBoosted = false
            observedCombatRound = viewModel.combatRound
        }
    }
    LaunchedEffect(viewModel.combatTurnIndex) {
        // Likewise, preserve an already-spent Action when reopening/restoring the current Turn.
        if (viewModel.combatTurnIndex != observedTurnIndex) {
            observedTurnIndex = viewModel.combatTurnIndex
            val activeId = viewModel.combatInitiative.getOrNull(viewModel.combatTurnIndex)?.characterId
            if (activeId == character.id) {
                attackActionUsed = 0
                attackActionCap = null
            }
        }
    }

    fun performAttackInternal(weapon: Weapon, autofire: Boolean) {
        if (character.combatEffects.containsKey("no_action_this_turn")) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "Spinal Injury: این Turn نمی‌توانی Action بگیری.", false)
            return
        }
        if (character.combatEffects.keys.any { it.startsWith("iron_grip_by_") } && !weapon.modes.contains("Melee", true) && !weapon.type.equals("Melee Weapon", true) && !weapon.type.equals("Brawling", true) && !weapon.type.equals("Martial Arts", true)) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "Iron Grip: تا وقتی Grapple برقرار است Ranged Attack ممنوع است.", false)
            return
        }
        val inGrapple = character.combatEffects.keys.any { it.startsWith("grappled_by_") || it.startsWith("grappling_") }
        if (inGrapple && weapon.handsRequired >= 2) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "در Grapple هیچ‌کدام از طرفین نمی‌توانند از سلاح دو دستی استفاده کنند.", false)
            return
        }
        val seatInfo = viewModel.vehicleSeatInfo(character.id)
        val drivingWithoutPlugs = seatInfo?.third == "driver" && !character.inventory.any { it.equipped && it.name.contains("Interface Plugs", true) }
        if (drivingWithoutPlugs && weapon.handsRequired >= 2) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "Driving without Interface Plugs occupies one hand; a two-handed weapon cannot be used while driving.", false)
            return
        }
        val isMelee = weapon.type.equals("Melee Weapon", true) || weapon.modes.contains("Melee", true)
        val isThrown = weapon.type.equals("Athletics", true) && weapon.magazineSize == 0
        val loadedLower = weapon.loadedAmmoName.lowercase()
        val isShotgunFamily = weapon.category.contains("Shotgun", true) || weapon.name.equals("Constitution Arms Hurricane", true)
        val isShotgunShell = isShotgunFamily && loadedLower.contains("shotgun") && !loadedLower.contains("slug")
        val isExplosive = weapon.name.contains("Grenade", true) || weapon.name.contains("Rocket", true) ||
            weapon.type.contains("Grenade Launcher", true) || weapon.type.contains("Rocket Launcher", true) ||
            weapon.modes.contains("Explosive", true)
        if (CombatRules.isAutofireOnly(weapon) && !autofire) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "این سلاح فقط با Autofire شلیک می‌شود.", false)
            return
        }
        if (aimTarget != "None" && !CombatRules.canAimWeapon(weapon, autofire, isShotgunShell, isExplosive)) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "این حالت/سلاح طبق Core نمی‌تواند Aimed Shot انجام دهد.", false)
            return
        }
        val minBody = CombatRules.minimumBodyToFire(weapon)
        if (minBody > 0 && EquipmentUseRules.effectiveStat(character, "BODY", character.stats.body) < minBody) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "برای شلیک این سلاح BODY $minBody لازم است مگر اینکه mounted باشد.", false)
            return
        }
        if (weapon.loadedAmmoName.contains("Smart", true) && !EquipmentUseRules.cyberOptionActive(character, "Targeting Scope")) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "Smart Ammunition بدون Targeting Scope به‌دلیل safety feature اصلاً شلیک نمی‌شود.", false)
            return
        }
        val shotsNeeded = if (autofire) CombatRules.autofireAmmoCost(weapon) else if (weapon.magazineSize > 0) 1 else 0
        if (shotsNeeded > 0 && weapon.currentAmmo < shotsNeeded) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "مهمات کافی نیست.", false)
            return
        }
        if (autofire && weapon.autofireMultiplier <= 0) return

        val actionLimit = if (autofire || aimTarget != "None") 1 else weapon.rof.coerceAtLeast(1)
        val cap = attackActionCap
        // GM حرف آخر را می‌زند: اگر عمداً بخواهد شلیک اضافه بگیرد، برنامه جلویش را
        // نمی‌گیرد. فقط وقتی سقف رد شده باشد، در نتیجه یادآوری می‌شود.
        val overCap = cap != null && attackActionUsed >= cap
        if (!gmOverrideAction && overCap) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "Action این نوبت مصرف شده. برای شلیک اضافه، «GM: شلیک آزاد» را روشن کن یا NEXT TURN بزن.", false)
            return
        }
        if (!gmOverrideAction && cap == 2 && actionLimit < 2) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "حمله‌ی دوم همین Action باید با سلاح ROF 2 باشد.", false)
            return
        }

        val check = CombatRules.rollD10()
        // گزارش تست ۳.۵: آستانه‌ی Fumble Recovery ثابت و ۴ امتیاز است (نه «هر امتیاز بالای صفر»).
        val fumbleRecovery = character.role.equals("Solo", true) && (character.roleAbilityPoints["fumble"] ?: 0) >= 4
        val effectiveDie = if (check.criticalFailure && fumbleRecovery) 1 else check.totalDie
        val rollText = when {
            check.criticalSuccess -> "10 + ${check.extra}"
            check.criticalFailure && fumbleRecovery -> "1 (Fumble Recovery)"
            check.criticalFailure -> "1 - ${check.extra}"
            else -> check.first.toString()
        }
        val skillName = if (autofire) "Autofire" else weapon.type
        val stat = when (skillName) {
            "Melee Weapon", "Brawling", "Martial Arts", "Athletics" -> EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex)
            else -> EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref)
        }
        val skill = CombatRules.skillLevel(character, skillName)
        val precisionPoints = character.roleAbilityPoints["precisionAttack"] ?: 0
        val precisionBonus = when {
            precisionPoints >= 9 -> 3
            precisionPoints >= 6 -> 2
            precisionPoints >= 3 -> 1
            else -> 0
        }
        val injuryPenalty = if (isMelee) CombatRules.meleeInjuryPenalty(character) else CombatRules.rangedInjuryPenalty(character)
        val aimedPenalty = if (aimTarget != "None") -8 else 0
        val quality = CombatRules.qualityAttackModifier(weapon)
        val distance = distanceText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val installedCyber = character.inventory.filter { it.category.equals("Cyberware", true) && it.equipped }.map { it.name }
        val smartConnected = installedCyber.any { it.contains("Neural Link", true) } &&
            (installedCyber.any { it.contains("Interface Plugs", true) } || installedCyber.any { it.contains("Subdermal Grip", true) })
        val builtInSmartgun = weapon.name.equals("Malorian Arms 3516", true)
        if (builtInSmartgun && !smartConnected) {
            attackResult = AttackUiResult(weapon.name, "—", 0, "Malorian Arms 3516 باید از طریق Interface Plugs یا Subdermal Grip به Neural Link متصل باشد.", false)
            return
        }
        val smartgunBonus = if (!isMelee && (builtInSmartgun || weapon.attachments.any { it.equals("Smartgun Link", true) }) && smartConnected) 1 else 0
        val hasTeleOptics = com.cyberpunk.gmtool.data.EquipmentUseRules.cyberOptionActive(character, "TeleOptics")
        val snipingBonus = if (!autofire && distance >= 51 && weapon.attachments.any { it.equals("Sniping Scope", true) } && !hasTeleOptics) 1 else 0
        val total = effectiveDie + stat + skill + CombatRules.attackPenalty(character) + precisionBonus + injuryPenalty + aimedPenalty + quality + smartgunBonus + snipingBonus
        val poorJam = weapon.quality.equals("Poor", true) && check.criticalFailure && !fumbleRecovery
        fun hasEarProtection(c: Character): Boolean = c.inventory.any { it.equipped && it.name.equals("Auto Level Dampening Ear Protectors", true) } || EquipmentUseRules.cyberOptionActive(c, "Level Damper")
        var defenseText = "Attack Total only"
        var hit: Boolean? = null
        var defenseValue: Int? = null
        val currentTarget = target
        val selectedDefense = defenseMode
        if (currentTarget != null) {
            // Cover فقط Line of Sight را می‌بندد؛ ضربهٔ Melee/Brawling/Martial Arts با Cover متوقف نمی‌شود.
            if (selectedDefense == "Cover" && viewModel.isInCover(currentTarget.id) && !isMelee) {
                actionResult = "هدف پشت Cover است و Line of Sight قطع شده؛ اول باید از Cover خارج شود یا زاویهٔ حمله عوض شود."
                return
            }
            if (selectedDefense == "Shield" && viewModel.combatShieldHp(currentTarget.id) <= 0) {
                actionResult = "برای Shield defense، هدف باید Shield آماده با HP باقی‌مانده داشته باشد."
                return
            }
            if (selectedDefense == "Human Shield" && (isMelee || aimTarget == "Head" || viewModel.humanShieldId(currentTarget.id) == null)) {
                actionResult = "Human Shield فقط برای حملهٔ Ranged غیر Head و وقتی هدف واقعاً Human Shield در اختیار دارد قابل استفاده است."
                return
            }
            if (selectedDefense == "Dodge" && !isMelee && !CombatRules.canDodgeRanged(currentTarget)) {
                actionResult = "این هدف شرایط Dodge کردن حملهٔ Ranged را ندارد؛ Range DV را انتخاب کن."
                return
            }
        }

        if (!poorJam) {
            if (isShotgunShell) {
                defenseValue = 13
                defenseText = "Shotgun Shell DV13 • cone/front ≤6m/yd"
                hit = distance <= 6 && total > 13
            } else if (isExplosive) {
                // Core: explosive attacks target a 2m/yd square using the weapon's Range DV.
                // REF 8+ characters dodge the blast only after the attack check succeeds.
                val table = weapon.rangeSingle
                val dv = if (isThrown && distance > 25) null else CombatRules.rangeDv(table, distance)
                defenseValue = dv
                defenseText = if (dv == null) "خارج از برد جدول این سلاح" else gtr("Range DV %1s @ %2sm/yd • Blast 10×10m/yd", dv, distance)
                hit = dv?.let { total > it } ?: false
            } else if (currentTarget != null) {
                if (isMelee) {
                    val dr = CombatRules.rollD10()
                    defenseValue = CombatRules.defenderEvasionTotal(currentTarget, dr)
                    defenseText = gtr("Evasion %1s", defenseValue)
                    hit = total > defenseValue
                } else if ((selectedDefense == "Dodge" || (selectedDefense == "Auto" && targetDodges && viewModel.combatShieldHp(currentTarget.id) <= 0)) && CombatRules.canDodgeRanged(currentTarget)) {
                    val dr = CombatRules.rollD10()
                    defenseValue = CombatRules.defenderEvasionTotal(currentTarget, dr)
                    val refDv = CombatRules.rangeDv(if (autofire) weapon.rangeAuto else weapon.rangeSingle, distance)
                    defenseText = gtr("Dodge %1s • Range reference DV %2s @ %3sm/yd", defenseValue, refDv?.toString() ?: "OUT", distance)
                    hit = total > defenseValue
                } else {
                    val table = if (autofire) weapon.rangeAuto else weapon.rangeSingle
                    val dv = CombatRules.rangeDv(table, distance)
                    defenseValue = dv
                    defenseText = if (dv == null) "خارج از برد جدول این سلاح" else gtr("Range DV %1s @ %2sm/yd", dv, distance)
                    hit = dv?.let { total > it } ?: false
                }
            }
        }

        // Shrieker hurts an unprotected shooter whenever the weapon is actually fired, hit or miss.
        if (!poorJam && weapon.name.equals("Shrieker", true) && !hasEarProtection(character)) {
            viewModel.addCriticalInjury(character.id, "damaged_ear")
        }

        if (isMelee && currentTarget != null && hit != null) {
            viewModel.recordIncomingMelee(currentTarget.id, character.id, dodged = hit == false)
        }

        // Smart Ammunition: on a single-shot miss by 4 or less, a Targeting Scope user gets one immediate second chance.
        var smartText = ""
        if (hit == false && !autofire && !isMelee && loadedLower.contains("smart") && defenseValue != null && (defenseValue - total) in 0..4) {
            val hasScope = com.cyberpunk.gmtool.data.EquipmentUseRules.cyberOptionActive(character, "Targeting Scope")
            if (hasScope) {
                val secondRoll = com.cyberpunk.gmtool.data.DiceSource.rollOne(10, "تاس دوم دفاع")
                val secondTotal = 10 + secondRoll
                var secondDefense = defenseValue ?: 0
                if (currentTarget != null && (selectedDefense == "Dodge" || (selectedDefense == "Auto" && targetDodges)) && CombatRules.canDodgeRanged(currentTarget)) secondDefense = CombatRules.defenderEvasionTotal(currentTarget, CombatRules.rollD10())
                hit = secondTotal > secondDefense
                smartText = gtr("Smart Ammo second chance: 10 vs %1s → %2s", secondDefense, if(hit == true) "HIT" else "MISS")
            } else smartText = "Smart Ammo cannot fire its guidance feature without Targeting Scope cyberware."
        }

        var damageText = ""
        var resultRawDamage: Int? = null
        var resultArmorUsed: Int? = null
        var resultHpDamage: Int? = null
        var resultArmorAblated: Int? = null
        var resultCriticalName: String? = null
        var resultResolutionLabel = ""
        var resultResolutionNote = ""
        val ammoLabel = (weapon.loadedAmmoName + " " + weapon.name + " " + weapon.modes).lowercase()
        val specialNoDamage = listOf("flashbang", "teargas", "smoke", "emp", "biotoxin", "poison", "sleep").any { ammoLabel.contains(it) } ||
            weapon.name.equals("Microwaver", true) || weapon.name.equals("Shrieker", true) || weapon.name.equals("Air Pistol", true)
        fun specialAmmoEffect(victim: Character, penetrated: Boolean): String? {
            val name = victim.handle.ifBlank { victim.name }
            fun resist(skillName: String, statName: String, baseStat: Int, dv: Int): Pair<Int, Boolean> {
                val r = CombatRules.rollD10()
                val stat = EquipmentUseRules.effectiveStat(victim, statName, baseStat)
                val totalCheck = stat + CombatRules.skillLevel(victim, skillName) + r.totalDie + CombatRules.allActionsPenalty(victim)
                return totalCheck to (totalCheck > dv)
            }
            return when {
                ammoLabel.contains("flashbang") -> {
                    val (r, ok) = resist("Resist Torture/Drugs", "WILL", victim.stats.will, 15)
                    if (!ok) { viewModel.addCombatEffect(victim.id, "damaged_eye", 20); viewModel.addCombatEffect(victim.id, "damaged_ear", 20) }
                    gtr("%1s: Resist %2s vs DV15 → %3s", name, r, if (ok) "RESISTED" else "Damaged Eye + Damaged Ear (1 min)")
                }
                ammoLabel.contains("teargas") -> {
                    val meatEyes = victim.inventory.filter { it.category.equals("Cyberware", true) && it.equipped && it.name.equals("Cybereye", true) }.sumOf { it.quantity } < 2
                    if (!meatEyes) gtr("%1s: no meat eyes → Teargas has no eye effect", name)
                    else {
                        val (r, ok) = resist("Resist Torture/Drugs", "WILL", victim.stats.will, 13)
                        if (!ok) viewModel.addCombatEffect(victim.id, "damaged_eye", 20)
                        gtr("%1s: Resist %2s vs DV13 → %3s", name, r, if (ok) "RESISTED" else "Damaged Eye (1 min)")
                    }
                }
                ammoLabel.contains("emp") || weapon.name.equals("Microwaver", true) -> {
                    val (r, ok) = resist("Cybertech", "TECH", victim.stats.tech, 15)
                    if (!ok) viewModel.addCombatEffect(victim.id, "emp_disabled_2_items", 20)
                    gtr("%1s: Cybertech %2s vs DV15 → %3s", name, r, if (ok) "RESISTED" else "GM disables 2 cyberware/electronics (1 min)")
                }
                ammoLabel.contains("biotoxin") -> {
                    val (r, ok) = resist("Resist Torture/Drugs", "WILL", victim.stats.will, 15)
                    if (!ok) { val d=CombatRules.rollDamage("3d6"); viewModel.directCombatHpDamage(victim.id,d.total); gtr("%1s: Resist %2s vs DV15 FAIL → %3s direct HP", name, r, d.total) } else gtr("%1s: Resist %2s vs DV15 → RESISTED", name, r)
                }
                ammoLabel.contains("poison") -> {
                    val (r, ok) = resist("Resist Torture/Drugs", "WILL", victim.stats.will, 13)
                    if (!ok) { val d=CombatRules.rollDamage("2d6"); viewModel.directCombatHpDamage(victim.id,d.total); gtr("%1s: Resist %2s vs DV13 FAIL → %3s direct HP", name, r, d.total) } else gtr("%1s: Resist %2s vs DV13 → RESISTED", name, r)
                }
                ammoLabel.contains("sleep") -> {
                    val (r, ok) = resist("Resist Torture/Drugs", "WILL", victim.stats.will, 13)
                    if (!ok) { viewModel.addCombatEffect(victim.id,"unconscious",20); viewModel.addCombatEffect(victim.id,"prone",20) }
                    gtr("%1s: Resist %2s vs DV13 → %3s", name, r, if(ok) "RESISTED" else "PRONE + UNCONSCIOUS (1 min / until woken)")
                }
                weapon.name.equals("Shrieker", true) -> {
                    if (hasEarProtection(victim)) gtr("%1s: hearing protection blocks the sonic deafness effect", name)
                    else {
                        val (r, ok) = resist("Resist Torture/Drugs", "WILL", victim.stats.will, 15)
                        if (!ok) viewModel.addCriticalInjury(victim.id, "damaged_ear")
                        gtr("%1s: Resist %2s vs DV15 → %3s", name, r, if(ok) "RESISTED" else "Damaged Ear Critical Injury")
                    }
                }
                weapon.name.equals("Air Pistol", true) -> {
                    if (weapon.loadedAmmoName.contains("Acid", true)) {
                        viewModel.ablateArmor(victim.id, aimTarget == "Head", 1)
                        gtr("%1s: Acid Paintball → 0 HP damage, -1 SP at hit location", name)
                    } else gtr("%1s: Paintball hit → no HP damage", name)
                }
                (ammoLabel.contains("incendiary") || weapon.name.equals("Flamethrower", true)) && penetrated -> {
                    val fire = if (weapon.name.contains("Flamethrower", true)) 4 else 2
                    viewModel.clearCombatEffect(victim.id, if (fire == 4) "on_fire_2" else "on_fire_4")
                    viewModel.addCombatEffect(victim.id, "on_fire_$fire", -1)
                    gtr("%1s: IGNITED → %2s direct HP at end of each Turn until extinguished", name, fire)
                }
                else -> null
            }
        }
        if (hit == true) {
            if (currentTarget != null && isMelee) {
                val hitKind = when {
                    weapon.type.equals("Brawling", true) -> "brawling"
                    weapon.type.equals("Martial Arts", true) -> "martialarts"
                    weapon.type.equals("Melee Weapon", true) -> "meleeweapon"
                    else -> "melee"
                }
                viewModel.recordCombatHit(character.id, currentTarget.id, hitKind)
            }
            val damageRoll = if (autofire) {
                val two = CombatRules.rollDamage("2d6")
                val margin = (total - (defenseValue ?: total)).coerceAtLeast(1)
                val mult = min(margin, weapon.autofireMultiplier).coerceAtLeast(1)
                two.copy(total = two.total * mult)
            } else CombatRules.rollDamage(if (specialNoDamage) "0d6" else if (isShotgunShell) "3d6" else weapon.damage)

            var sharedRaw = damageRoll.total
            if (character.role.equals("Solo", true) && !spotWeaknessUsed) {
                if (!specialNoDamage) sharedRaw += character.roleAbilityPoints["spotWeakness"] ?: 0
                spotWeaknessUsed = true
            }

            if (isExplosive || isShotgunShell) {
                val ids = (blastTargetIds + listOfNotNull(currentTarget?.id)).toSet()
                if (ids.isEmpty()) {
                    damageText = "Damage roll ${damageRoll.dice.joinToString("+")}=${damageRoll.total}. هیچ فردی برای Blast Area انتخاب نشده است."
                } else {
                    val results = mutableListOf<String>()
                    ids.mapNotNull { id -> allCharacters.firstOrNull { it.id == id } }.forEach { victim ->
                        val name = victim.handle.ifBlank { victim.name }
                        if (isExplosive && victim.id in blastCoveredIds) {
                            results += gtr("%1s: protected by cover (GM marked cover as surviving the blast)", name)
                            return@forEach
                        }
                        if (targetDodges && CombatRules.canDodgeRanged(victim)) {
                            val dodge = CombatRules.defenderEvasionTotal(victim, CombatRules.rollD10())
                            if (dodge > total) {
                                results += gtr("%1s: DODGED blast (%2s > %3s)", name, dodge, total)
                                return@forEach
                            }
                        }
                        val defPoints = if (sharedRaw > 0 && !damageDeflectionUsed.contains(victim.id)) victim.roleAbilityPoints["damageDeflection"] ?: 0 else 0
                        val deflect = (defPoints / 2).coerceIn(0, 5)
                        val raw = (sharedRaw - deflect).coerceAtLeast(0)
                        if (deflect > 0) damageDeflectionUsed = damageDeflectionUsed + victim.id
                        val resolution = viewModel.applyCombatDamage(
                            targetId = victim.id,
                            rawDamage = raw,
                            head = false,
                            melee = false,
                            critical = damageRoll.critical && !CombatRules.cannotCauseCritical(weapon),
                            armorPiercing = weapon.modes.contains("Armor Piercing", true) || ammoLabel.contains("armor piercing") || ammoLabel.contains("armor-piercing"),
                            rubber = ammoLabel.contains("rubber"),
                            expansive = ammoLabel.contains("expansive"),
                            ignoreArmorBelowSp11 = CombatRules.ignoresArmorBelowSp11(weapon),
                            stunWeapon = CombatRules.isStunWeapon(weapon)
                        )
                        if (resolution != null) {
                            results += gtr("%1s: Armor %2s, HP -%3s, SP -%4s", name, resolution.armorUsed, resolution.hpDamage, resolution.armorAblated) +
                                (resolution.injuryKey?.let { gtr(" • Critical: %1s", CriticalInjuries.byKey(it)?.enName ?: it) } ?: "")
                            specialAmmoEffect(victim, resolution.hpDamage > 0)?.let { results += it }
                        }
                    }
                    damageText = gtr("ONE DAMAGE ROLL: %1s=%2s\n", damageRoll.dice.joinToString("+"), damageRoll.total) + results.joinToString("\n")
                }
            } else if (currentTarget != null) {
                val seatInfo = if (!isMelee) viewModel.vehicleSeatInfo(currentTarget.id) else null
                if (seatInfo != null) {
                    val (vehicleOwnerId, vehicleId, _) = seatInfo
                    val glassHp = viewModel.vehicleGlassHpForOccupant(vehicleOwnerId, vehicleId, currentTarget.id)
                    if (glassHp > 0) {
                        viewModel.damageVehicleGlassForOccupant(vehicleOwnerId, vehicleId, currentTarget.id, sharedRaw)
                        val left = viewModel.vehicleGlassHpForOccupant(vehicleOwnerId, vehicleId, currentTarget.id)
                        resultRawDamage = sharedRaw
                        resultResolutionLabel = "BULLETPROOF GLASS"
                        resultResolutionNote = "پنجره ضربه را کامل گرفت • HP $glassHp → $left" + if (left == 0) " • شیشه نابود شد؛ اضافهٔ Damage این ضربه عبور نمی‌کند." else ""
                        damageText = gtr("%1s=%2s • BULLETPROOF GLASS intercepted occupant attack • HP %3s → %4s", damageRoll.dice.joinToString("+"), damageRoll.total, glassHp, left) + if (left == 0) " • pane destroyed; excess damage does not pass through this attack" else ""
                        if (shotsNeeded > 0) viewModel.fireWeapon(character.id, weapon.id, shotsNeeded)
                        if (attackActionCap == null) attackActionCap = actionLimit
                        attackActionUsed += 1
                        attackResult = AttackUiResult(weapon.name, rollText, total, defenseText, true, damageText, currentTarget.handle.ifBlank { currentTarget.name }, false, resultRawDamage, null, null, null, null, resultResolutionLabel, resultResolutionNote)
                        return
                    }
                }
                val usePersonalShield = selectedDefense == "Shield" || (selectedDefense == "Auto" && viewModel.combatShieldHp(currentTarget.id) > 0)
                val personalShieldHp = if (!isExplosive && usePersonalShield) viewModel.combatShieldHp(currentTarget.id) else 0
                if (personalShieldHp > 0) {
                    viewModel.damageCombatShield(currentTarget.id, sharedRaw)
                    val left = viewModel.combatShieldHp(currentTarget.id)
                    resultRawDamage = sharedRaw
                    resultResolutionLabel = "SHIELD / COVER"
                    resultResolutionNote = "سپر ضربه را کامل گرفت • HP $personalShieldHp → $left" + if (left == 0) " • سپر نابود شد؛ اضافهٔ Damage این ضربه عبور نمی‌کند." else ""
                    damageText = gtr("%1s=%2s • SHIELD/COVER absorbed the hit • HP %3s → %4s", damageRoll.dice.joinToString("+"), damageRoll.total, personalShieldHp, left) + if (left == 0) " • shield destroyed; excess damage does not pass through" else ""
                    if (shotsNeeded > 0) viewModel.fireWeapon(character.id, weapon.id, shotsNeeded)
                    if (attackActionCap == null) attackActionCap = actionLimit
                    attackActionUsed += 1
                    attackResult = AttackUiResult(weapon.name, rollText, total, defenseText, true, damageText, currentTarget.handle.ifBlank { currentTarget.name }, false, resultRawDamage, null, null, null, null, resultResolutionLabel, resultResolutionNote)
                    return
                }
                val useHumanShield = selectedDefense == "Human Shield" || (selectedDefense == "Auto" && viewModel.combatShieldHp(currentTarget.id) <= 0)
                val shieldId = if (useHumanShield && !isMelee && aimTarget != "Head") viewModel.humanShieldId(currentTarget.id) else null
                val shieldVictim = shieldId?.let { id -> allCharacters.firstOrNull { it.id == id && !it.isDead } }
                val actualVictim = shieldVictim ?: currentTarget
                val defPoints = if (sharedRaw > 0 && !damageDeflectionUsed.contains(actualVictim.id)) actualVictim.roleAbilityPoints["damageDeflection"] ?: 0 else 0
                val deflect = (defPoints / 2).coerceIn(0, 5)
                val raw = (sharedRaw - deflect).coerceAtLeast(0)
                if (deflect > 0) damageDeflectionUsed = damageDeflectionUsed + actualVictim.id
                val isHeadShot = aimTarget == "Head" && !autofire
                val resolution = viewModel.applyCombatDamage(
                    targetId = actualVictim.id,
                    rawDamage = raw,
                    head = isHeadShot,
                    melee = isMelee,
                    critical = damageRoll.critical && !CombatRules.cannotCauseCritical(weapon),
                    halvesArmor = isMelee && !weapon.type.equals("Brawling", true),
                    armorPiercing = weapon.modes.contains("Armor Piercing", true) || ammoLabel.contains("armor piercing") || ammoLabel.contains("armor-piercing"),
                            rubber = ammoLabel.contains("rubber"),
                            expansive = ammoLabel.contains("expansive"),
                    ignoreArmorBelowSp11 = CombatRules.ignoresArmorBelowSp11(weapon),
                    stunWeapon = CombatRules.isStunWeapon(weapon)
                )
                if (resolution != null) {
                    resultRawDamage = raw
                    resultArmorUsed = resolution.armorUsed
                    resultHpDamage = resolution.hpDamage
                    resultArmorAblated = resolution.armorAblated
                    resultCriticalName = resolution.injuryKey?.let { CriticalInjuries.byKey(it)?.enName ?: it }
                    resultResolutionLabel = if (shieldVictim != null) "HUMAN SHIELD" else if (isHeadShot) "HEADSHOT" else "ARMOR → HP"
                    resultResolutionNote = buildString {
                        if (deflect > 0) append(gtr("Damage Deflection -%1s. ", deflect))
                        if (isMelee && !weapon.type.equals("Brawling", true)) append("Melee با نصف SP حل شد. ")
                        if (ammoLabel.contains("armor piercing") || ammoLabel.contains("armor-piercing") || weapon.modes.contains("Armor Piercing", true)) append("Armor Piercing فعال بود. ")
                        if (shieldVictim != null) append("Human Shield ضربه را به‌جای هدف دریافت کرد. ")
                    }.trim()
                }
                damageText = if (resolution != null) {
                    var baseText = gtr("%1s=%2s; Armor %3s; HP -%4s; SP -%5s", damageRoll.dice.joinToString("+"), damageRoll.total, resolution.armorUsed, resolution.hpDamage, resolution.armorAblated) +
                        (if (isHeadShot) " • Headshot" else "") +
                        (resolution.injuryKey?.let { " • Critical: ${CriticalInjuries.byKey(it)?.enName ?: it}" } ?: "")
                    if (shieldVictim != null) {
                        baseText += gtr(" • HUMAN SHIELD INTERPOSED: %1s took the shot", actualVictim.handle.ifBlank { actualVictim.name })
                        if (viewModel.getCharacter(actualVictim.id)?.isDead == true) {
                            viewModel.convertHumanShieldToCorpse(currentTarget.id, actualVictim.id)
                            baseText += " • now a Corpse Shield (HP = BODY)"
                        }
                    }
                    if (resolution.hpDamage > 0 && aimTarget == "Held Item" && shieldVictim == null) {
                        val chosen = heldItemWeaponId ?: actualVictim.weapons.firstOrNull { it.isEquipped }?.id
                        if (chosen != null && viewModel.dropHeldWeapon(actualVictim.id, chosen)) baseText += " • HELD ITEM DROPPED"
                        else baseText += " • HELD ITEM: GM resolves chosen non-weapon object"
                    }
                    if (resolution.hpDamage > 0 && aimTarget == "Leg" && shieldVictim == null && !actualVictim.criticalInjuries.contains("broken_leg")) {
                        viewModel.addCriticalInjury(actualVictim.id, "broken_leg")
                        baseText += " • Broken Leg"
                    }
                    baseText + (specialAmmoEffect(actualVictim, resolution.hpDamage > 0)?.let { "\n$it" } ?: "")
                } else gtr("Damage %1s", damageRoll.total)
            }
            if (ammoLabel.contains("smoke")) damageText += "\nSMOKE: 10×10m/yd area for 1 minute; typical obscured-task penalty -4."
            if (smartText.isNotBlank()) damageText = smartText + "\n" + damageText
        } else if (isExplosive && hit == false && defenseValue != null) {
            damageText = "طبق Core، در Miss محل فرود انفجار را GM داخل ناحیه تعیین می‌کند؛ بعد افراد واقعاً داخل Blast Area را انتخاب کن."
        }

        if (shotsNeeded > 0) viewModel.fireWeapon(character.id, weapon.id, shotsNeeded)
        if (isThrown) viewModel.removeWeapon(character.id, weapon.id)
        if (attackActionCap == null) attackActionCap = actionLimit
        attackActionUsed += 1

        attackResult = AttackUiResult(
            weaponName = weapon.name,
            rollText = rollText,
            attackTotal = total,
            defenseText = defenseText,
            hit = if (poorJam) false else hit,
            damageText = damageText,
            targetText = if (isExplosive || isShotgunShell) gtr("Area Targets (%1s)", blastTargetIds.size + if (currentTarget != null && currentTarget.id !in blastTargetIds) 1 else 0) else currentTarget?.let { it.handle.ifBlank { it.name } } ?: "",
            jammed = poorJam,
            rawDamage = resultRawDamage,
            armorUsed = resultArmorUsed,
            hpDamage = resultHpDamage,
            armorAblated = resultArmorAblated,
            criticalName = resultCriticalName,
            resolutionLabel = resultResolutionLabel,
            resolutionNote = resultResolutionNote
        )
    }

    /**
     * اجرای حمله. در حالت «تاس دستی» روی نخ پس‌زمینه اجرا می‌شود تا دیالوگ
     * ورود تاس فیزیکی بتواند رسم شود؛ اگر روی نخ اصلی بماند برنامه فریز می‌کند.
     */
    fun performAttack(weapon: Weapon, autofire: Boolean) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            scope.launch(kotlinx.coroutines.Dispatchers.Default) { performAttackInternal(weapon, autofire) }
        } else {
            performAttackInternal(weapon, autofire)
        }
    }

    /**
     * Suppressive Fire — از دکمه‌ی FIRE پایین صفحه اجرا می‌شود، نه از یک دکمه‌ی
     * جدا داخل فرم سلاح. منطق قواعدی همان منطق قبلی است (۱d10 + Autofire + REF،
     * بعد چک WILL + Concentration هر هدف، ۱۰ گلوله مصرف و اثر «suppressed»)،
     * فقط از فرم به مسیر شلیک اصلی منتقل شده و مثل بقیه‌ی شلیک‌ها در حالت تاس
     * دستی از نخ پس‌زمینه اجرا می‌شود.
     */
    fun performSuppressiveFireInternal(w: Weapon) {
        if (character.combatEffects.containsKey("no_action_this_turn")) {
            attackResult = AttackUiResult(w.name, "—", 0, "Spinal Injury: این Turn نمی‌توانی Action بگیری.", false)
            return
        }
        val cap = attackActionCap
        if (!gmOverrideAction && cap != null && attackActionUsed >= cap) {
            attackResult = AttackUiResult(w.name, "—", 0, "Action این نوبت مصرف شده. برای شلیک اضافه، «GM: free fire» را روشن کن.", false)
            return
        }
        if (w.currentAmmo < 10) {
            attackResult = AttackUiResult(w.name, "—", 0, "Suppressive Fire requires 10 bullets.", false)
            return
        }
        val check = CombatRules.rollD10()
        val total = EquipmentUseRules.effectiveStat(character, "REF", character.stats.ref) +
            CombatRules.skillLevel(character, "Autofire") + check.totalDie + CombatRules.attackPenalty(character)
        val affected = (if (encounterCharacters.isNotEmpty()) encounterCharacters else allCharacters).filter {
            it.id in blastTargetIds && it.id != character.id && !it.isDead &&
                viewModel.vehicleSeatInfo(it.id) == null && !viewModel.isInCover(it.id) &&
                viewModel.combatShieldHp(it.id) <= 0 && viewModel.humanShieldId(it.id) == null
        }
        if (affected.isEmpty()) {
            attackResult = AttackUiResult(
                w.name, "—", 0,
                "No eligible targets. Suppressive Fire only affects selected targets on foot and not in cover/shield; also verify ≤25m/yd and LOS.",
                false
            )
            return
        }
        val lines = affected.map { v ->
            val r = CombatRules.rollD10()
            val d = EquipmentUseRules.effectiveStat(v, "WILL", v.stats.will) +
                CombatRules.skillLevel(v, "Concentration") + r.totalDie + CombatRules.allActionsPenalty(v)
            if (d < total) {
                viewModel.addCombatEffect(v.id, "suppressed", 1)
                gtr("%1s: %2s < %3s → MUST MOVE/RUN TO COVER", v.handle.ifBlank { v.name }, d, total)
            } else gtr("%1s: %2s ≥ %3s → HOLDS", v.handle.ifBlank { v.name }, d, total)
        }
        viewModel.fireWeapon(character.id, w.id, 10)
        attackActionCap = 1
        attackActionUsed = 1
        attackResult = AttackUiResult(
            w.name, check.first.toString(), total,
            "Suppressive Fire • targets on foot ≤25m/yd, LOS, out of cover", true,
            lines.joinToString("\n"), "Encounter"
        )
    }

    fun performSuppressiveFire(weapon: Weapon) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            scope.launch(kotlinx.coroutines.Dispatchers.Default) { performSuppressiveFireInternal(weapon) }
        } else {
            performSuppressiveFireInternal(weapon)
        }
    }

    /**
     * دروازه‌ی عملیات قواعدیِ تاس‌دار.
     *
     * چرا لازم است: در حالت «تاس دستی»، [com.cyberpunk.gmtool.data.DiceSource] برای
     * گرفتن عدد از GM نخ فراخوان را بلاک می‌کند. روی نخ اصلی این کار ممکن نیست
     * (دیالوگ هیچ‌وقت رسم نمی‌شود و برنامه فریز می‌کند) و DiceSource ناچار می‌شود
     * تصادفی بریزد. پس در آن حالت کار را به نخ پس‌زمینه می‌بریم تا تاس واقعاً از
     * GM پرسیده شود. همین الگو در `performAttack` هم استفاده شده است.
     */
    fun rules(block: () -> Unit) {
        if (com.cyberpunk.gmtool.data.DiceSource.isManual) {
            scope.launch(kotlinx.coroutines.Dispatchers.Default) { block() }
        } else block()
    }

    Column(Modifier.fillMaxSize().background(Black)) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f).background(Black).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // بالای صفحه (HP/SP و درمان) بدون شماره است. ترتیب بخش‌ها: ۱) شرکت‌کننده‌های نبرد
        // ۲) نوبت/Initiative ۳) اکشن (حمله + دفاع) ۴) آسیب منطقه‌ای ۵) کاور/سپر ۶) نبرد خودرویی.
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.5.dp, Red, CutCornerShape(12.dp))
        ) {
            // گزارش تست ۳.۱: بدون fillMaxWidth عرض Column به اندازه‌ی محتوا می‌شد
            // و عدد HP وسط کارت نمی‌ایستاد.
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gtr("HIT POINTS (HP)"), color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundButton("−") { viewModel.adjustHp(character.id, -1) }
                    Text("${character.hp} / ${character.maxHp}", color = if (GameRules.isSeriouslyWounded(character.hp, character.maxHp)) Red else White,
                        fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                    RoundButton("+") { viewModel.adjustHp(character.id, 1) }
                }
                val woundText = when {
                    character.isDead -> "DEAD"
                    GameRules.isMortallyWounded(character.hp) -> gtr("MORTALLY WOUNDED • -4 Actions • MOVE -6 • Death Save: d10 + %1s < BODY %2s", character.deathSavePenalty, character.stats.body)
                    GameRules.isSeriouslyWounded(character.hp, character.maxHp) -> "SERIOUSLY WOUNDED • -2 to all Actions"
                    else -> "Not Seriously Wounded"
                }
                Text(gtr(woundText), color = if (GameRules.isSeriouslyWounded(character.hp, character.maxHp) || GameRules.isMortallyWounded(character.hp)) Red else Muted, fontSize = 12.sp)
                if (GameRules.isMortallyWounded(character.hp) && !character.isDead) {
                    Spacer(Modifier.height(8.dp))
                    var deathResult by remember { mutableStateOf<String?>(null) }
                    OutlinedButton(onClick = {
                        rules {
                            viewModel.rollDeathSave(character.id)?.let { (roll, success) -> deathResult = gtr("Death Save %1s → %2s", roll, if (success) "SUCCESS" else "FAIL / DEAD") }
                        }
                    }) { Text(deathResult ?: "ROLL DEATH SAVE", color = Red) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArmorCard("HEAD SP", GameRules.effectiveArmorSp(character, true), Modifier.weight(1f),
                onDec = { viewModel.ablateArmor(character.id, true, 1) }, onInc = { viewModel.repairArmor(character.id, true, 1) })
            ArmorCard("BODY SP", GameRules.effectiveArmorSp(character, false), Modifier.weight(1f),
                onDec = { viewModel.ablateArmor(character.id, false, 1) }, onInc = { viewModel.repairArmor(character.id, false, 1) })
        }

        // ── درمان و جراحات: زیر HP/SP و بدون شماره؛ با هر زخمی (و به‌ویژه Critical Injury) بالا می‌آید ──
        if (character.criticalInjuries.isNotEmpty() || character.hp < character.maxHp) {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Text(
                            "درمان و جراحات / TREATMENT", color = Red, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, modifier = Modifier.fillMaxWidth()
                        )
                        val woundLine = when {
                            character.isDead -> "DEAD"
                            GameRules.isMortallyWounded(character.hp) -> "MORTALLY WOUNDED • -4 Actions • MOVE -6 — درمان فوری لازم است"
                            GameRules.isSeriouslyWounded(character.hp, character.maxHp) -> "SERIOUSLY WOUNDED • -2 to all Actions"
                            character.hp < character.maxHp -> gtr("زخمی • %1s/%2s HP", character.hp, character.maxHp)
                            else -> "جراحت بحرانی ثبت نشده"
                        }
                        Text(
                            gtr(woundLine), color = if (character.hp < character.maxHp) Red else Muted,
                            fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                        )
                        CombatSectionHeader(
                            title = "CRITICAL INJURIES / جراحات بحرانی",
                            help = "Critical Injury علاوه بر Bonus Damage یک اثر ماندگار هم دارد. Quick Fix معمولاً اثر را موقتاً تا پایان روز خاموش می‌کند و حدود یک دقیقه زمان می‌برد؛ Treatment درمان دائمی است، چهار ساعت زمان می‌برد و روی خودت قابل انجام نیست. Skill و DV مناسب برای هر Injury جداست.",
                            expanded = criticalExpanded,
                            summary = gtr("%1s injury", character.criticalInjuries.size),
                            onToggle = { criticalExpanded = !criticalExpanded },
                            onHelp = { combatHelp = it }
                        )
                        if (criticalExpanded) {
                        if (character.criticalInjuries.isEmpty()) {
                            FaText(
                                "جراحت بحرانی ثبت نشده است. با ثبت جراحت، روش‌های درمان (Quick Fix / Treatment / Surgery) و انتخاب مداوگر همین‌جا فعال می‌شود.",
                                color = Muted, fontSize = 10.sp, lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), justify = true
                            )
                        }
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { criticalHealerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                val healer = allCharacters.firstOrNull { it.id == criticalHealerId }
                                Text(gtr("HEALER: %1s • GM confirms present", healer?.handle?.ifBlank { healer.name } ?: "SELECT"), color = Red, fontSize = 9.sp)
                            }
                            DropdownMenu(expanded = criticalHealerMenu, onDismissRequest = { criticalHealerMenu = false }) {
                                allCharacters.filter { !it.isDead }.forEach { h ->
                                    DropdownMenuItem(
                                        text = { Text("${h.handle.ifBlank { h.name }} • ${h.role}") },
                                        onClick = { criticalHealerId = h.id; criticalHealerMenu = false }
                                    )
                                }
                            }
                        }
                        character.criticalInjuries.forEach { key ->
                            val injury = CriticalInjuries.byKey(key)
                            val active = CriticalInjuries.effectActive(character, key)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RuleInfoButton("critical.$key")
                                Text("${injury?.enName ?: key} • ${if (active) injury?.effectFa ?: "" else "اثر مکانیکی فعلاً غیرفعال شده است"}", color = if (active) White else Muted, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick={ rules { actionResult=viewModel.attemptCriticalInjuryCare(character.id,key,false,false,criticalHealerId) } }, enabled=CriticalInjuries.quickFixOptions(key).isNotEmpty(), modifier=Modifier.weight(1f)) { Text(gtr("QUICK FIX"),color=Red,fontSize=8.sp) }
                                OutlinedButton(onClick={ rules { actionResult=viewModel.attemptCriticalInjuryCare(character.id,key,true,false,criticalHealerId) } }, enabled=CriticalInjuries.treatmentOptions(key).isNotEmpty(), modifier=Modifier.weight(1f)) { Text(gtr("TREAT"),color=Red,fontSize=8.sp) }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick={ rules { actionResult=viewModel.attemptCriticalInjuryCare(character.id,key,false,true,criticalHealerId) } }, enabled=CriticalInjuries.quickFixOptions(key).isNotEmpty(), modifier=Modifier.weight(1f)) { Text(gtr("CYBERTECH QF"),color=Muted,fontSize=8.sp) }
                                OutlinedButton(onClick={ rules { actionResult=viewModel.attemptCriticalInjuryCare(character.id,key,true,true,criticalHealerId) } }, enabled=CriticalInjuries.treatmentOptions(key).isNotEmpty(), modifier=Modifier.weight(1f)) { Text(gtr("CYBERTECH TREAT"),color=Muted,fontSize=8.sp) }
                            }
                        }
                        OutlinedButton(onClick={ viewModel.expireDailyQuickFixes(character.id); actionResult="Daily Quick Fix effects expired; permanent effect removals remain." }, modifier=Modifier.fillMaxWidth().padding(top=6.dp)) { Text(gtr("NEW DAY • EXPIRE TEMP QUICK FIXES"),color=Muted,fontSize=9.sp) }
                        }
                    }
                }
            }
        }

        // دکمه‌ی ESSENTIALS حذف شد: بخش‌های ضروری حالا در تب اصلیِ همیشه‌باز هستند.
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            OutlinedButton(onClick = {
                criticalExpanded = false
                effectsExpanded = false
                targetExpanded = false

                coverExpanded = false
                martialExpanded = false
                vehicleExpanded = false
            }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) { Text(gtr("COLLAPSE ALL"), color = Muted, fontSize = 9.sp) }
        }

        // ── بخش ۱ و ۲: شرکت‌کننده‌های نبرد و بعد نوبت/Initiative (کنترل‌های تب TURN سابق) ──
        CombatTurnSection(
            character = character,
            viewModel = viewModel,
            attackActionUsed = attackActionUsed,
            attackActionCap = attackActionCap,
            gmOverrideAction = gmOverrideAction,
            onActionSpent = { cap -> if (attackActionCap == null) attackActionCap = cap; attackActionUsed += 1 },
            onResetTurnTracking = {
                attackActionUsed = 0
                attackActionCap = null
                viewModel.clearTurnCombatTracking(character.id)
            },
            onResult = { msg -> actionResult = msg },
            onHelp = { pair -> combatHelp = pair }
        )

        Spacer(Modifier.height(12.dp))
        val equippedAttackWeapons = character.weapons.filter { it.isEquipped }
        LaunchedEffect(equippedAttackWeapons.map { it.id }) {
            if (selectedAttackWeaponId != null && equippedAttackWeapons.none { it.id == selectedAttackWeaponId }) {
                selectedAttackWeaponId = equippedAttackWeapons.firstOrNull()?.id
            }
            if (selectedAttackWeaponId == null) selectedAttackWeaponId = equippedAttackWeapons.firstOrNull()?.id
        }
        val workflowWeapon = equippedAttackWeapons.firstOrNull { it.id == selectedAttackWeaponId }
        LaunchedEffect(workflowWeapon?.id, workflowWeapon?.autofireMultiplier, workflowWeapon?.modes) {
            val w = workflowWeapon ?: return@LaunchedEffect
            val ok = when (selectedAttackMode) {
                "Autofire" -> w.autofireMultiplier > 0
                "Suppressive" -> w.modes.contains("Suppressive Fire", true)
                else -> true
            }
            if (!ok) selectedAttackMode = "Single"
        }


        // هدف و پارامترهای بدن/گرپل — قبل از کارت ATTACK می‌گیریم چون هر دو حالت داخل آن به این‌ها نیاز دارند.
        val currentTargetForActions = target
        val hasCyberarm = character.inventory.any { it.equipped && it.name.contains("Cyberarm", true) }
        val bodyDamage = CombatRules.bodyDamageExpression(character.stats.body, hasCyberarm)
        val grapplingTargetId = character.combatEffects.keys.firstOrNull { it.startsWith("grappling_") }?.removePrefix("grappling_")?.toIntOrNull()
        val grappledById = character.combatEffects.keys.firstOrNull { it.startsWith("grappled_by_") }?.removePrefix("grappled_by_")?.toIntOrNull()

        // ══════════════════════════════════════════════════
        // بخش ۳: اکشن — حمله و دفاع، همیشه باز و همه‌چیزِ یک حمله در یک جا
        // ══════════════════════════════════════════════════
        CombatSectionLabel(3, "ACTION — ATTACK & DEFENSE / اکشن: حمله و دفاع")
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = CutCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                .border(1.5.dp, Red, CutCornerShape(12.dp))
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(gtr("ATTACK"), color = Red, fontSize = 17.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        combatHelp = gtr("ATTACK") to "همه‌ی چیزی که برای یک حمله لازم است در همین بخش است: هدف، فاصله، سلاح، حالت شلیک، نشانه‌گیری، و اینکه هدف چطور دفاع می‌کند. دکمه‌ی شلیک پایین صفحه همیشه در دسترس است. بخش‌های پایین‌تر برای حالت‌های خاص‌اند."
                    }, modifier = Modifier.size(32.dp)) {
                        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Red.copy(alpha = .14f), border = androidx.compose.foundation.BorderStroke(1.dp, Red.copy(alpha = .65f))) {
                            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { Text("i", color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        }
                    }
                }
                HorizontalDivider(color = Red.copy(alpha = .35f), modifier = Modifier.padding(vertical = 8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                shape = CutCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).border(1.dp, Red.copy(alpha = 0.45f), CutCornerShape(10.dp))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(gtr("1 • TARGET"), color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Box {
                        OutlinedButton(onClick = { targetMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(target?.let { it.handle.ifBlank { it.name } } ?: "انتخاب هدف", color = if (target == null) Muted else White)
                        }
                        DropdownMenu(expanded = targetMenu, onDismissRequest = { targetMenu = false }) {
                            DropdownMenuItem(text = { Text("بدون هدف / فقط Roll") }, onClick = { targetId = null; targetMenu = false })
                            availableTargets.forEach { c ->
                                DropdownMenuItem(text = { Text(c.handle.ifBlank { c.name }) }, onClick = { targetId = c.id; targetMenu = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it.filter(Char::isDigit).take(4) },
                        label = { Text(gtr("2 • Distance m/yd")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = targetDodges, onCheckedChange = { targetDodges = it })
                        Text("اگر REF 8+ دارد Dodge کند", color = White, fontSize = 11.sp)
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("۳) با چه چیزی حمله می‌کنی؟", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        RuleInfoButton("combat.grapple")
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = attackMode == "Weapon", onClick = { attackMode = "Weapon" }, label = { Text("سلاح", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                        FilterChip(selected = attackMode == "Body", onClick = { attackMode = "Body" }, label = { Text("بدن", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                    }
                    if (attackMode == "Weapon") {
                    if (equippedAttackWeapons.isEmpty()) {
                        Text("سلاح مجهزی نداری؛ از GEAR یک Weapon را Equip کن.", color = Muted, fontSize = 11.sp)
                    } else {
                        equippedAttackWeapons.forEach { w ->
                            val selected = w.id == selectedAttackWeaponId
                            OutlinedButton(
                                onClick = { selectedAttackWeaponId = w.id; selectedAttackMode = "Single" },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) Red.copy(alpha = 0.16f) else Color.Transparent)
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(gtr(w.name), color = if (selected) Red else White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    val ammo = if (w.magazineSize > 0) " • ${w.currentAmmo}/${w.magazineSize}" else ""
                                    Text(gtr("%1s • ROF %2s%3s • %4s", w.damage, w.rof, ammo, w.type), color = Muted, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    workflowWeapon?.let { w ->
                        val supportsAutofire = w.autofireMultiplier > 0
                        val supportsSuppressive = w.modes.contains("Suppressive Fire", true)
                        if (supportsAutofire || supportsSuppressive) {
                            // Mode همان‌جایی است که نوع شلیک انتخاب می‌شود؛ دکمه‌ی شلیک
                            // فقط FIRE پایین صفحه است.
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = selectedAttackMode == "Single", onClick = { selectedAttackMode = "Single" }, label = { Text(gtr("Single")) }, modifier = Modifier.weight(1f))
                                if (supportsAutofire) {
                                    FilterChip(selected = selectedAttackMode == "Autofire", onClick = { selectedAttackMode = "Autofire" }, label = { Text(gtr("Autofire ×%1s", w.autofireMultiplier)) }, modifier = Modifier.weight(1f))
                                }
                                if (supportsSuppressive) {
                                    FilterChip(selected = selectedAttackMode == "Suppressive", onClick = { selectedAttackMode = "Suppressive" }, label = { Text(gtr("Suppressive")) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        if (w.magazineSize > 0) {
                            Text(gtr("Ammo: %1s/%2s • %3s", w.currentAmmo, w.magazineSize, w.loadedAmmoName), color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        val d = distanceText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        val dv = if (w.type.equals("Melee Weapon", true) || w.modes.contains("Melee", true)) null else CombatRules.rangeDv(w.rangeSingle, d)
                        Text(if (dv == null && !(w.type.equals("Melee Weapon", true) || w.modes.contains("Melee", true))) "Range: OUT OF TABLE" else if (dv != null) gtr("Range preview: DV %1s", dv) else "Melee: opposed by Evasion", color = if (dv == null && d > 0 && !w.modes.contains("Melee", true)) Red else Muted, fontSize = 10.sp)
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("۴) هدف‌گیری دقیق (Aimed Shot — اختیاری)", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        RuleInfoButton("combat.aimed_shot")
                    }
                    val aimAllowed = workflowWeapon?.let { w ->
                        val lower = w.loadedAmmoName.lowercase()
                        val shell = w.category.contains("Shotgun", true) && lower.contains("shotgun") && !lower.contains("slug")
                        val explosive = w.name.contains("Grenade", true) || w.name.contains("Rocket", true) || w.modes.contains("Explosive", true)
                        CombatRules.canAimWeapon(w, selectedAttackMode != "Single", shell, explosive)
                    } ?: false
                    Box {
                        OutlinedButton(onClick = { aimedMenu = true }, enabled = aimAllowed || aimTarget == "None", modifier = Modifier.fillMaxWidth()) {
                            Text(if (aimTarget == "None") "بدون Aimed Shot" else "$aimTarget • -8", color = if (aimTarget == "None") Muted else Red)
                        }
                        DropdownMenu(expanded = aimedMenu, onDismissRequest = { aimedMenu = false }) {
                            listOf("None", "Head", "Held Item", "Leg").forEach { choice ->
                                DropdownMenuItem(text = { Text(gtr(choice)) }, enabled = choice == "None" || aimAllowed, onClick = { aimTarget = choice; aimedMenu = false })
                            }
                        }
                    }
                    if (!aimAllowed && aimTarget != "None") {
                        Text("این Weapon/Mode نمی‌تواند Aimed Shot انجام دهد؛ Aim را روی None بگذار.", color = Red, fontSize = 9.sp)
                    }
                    if (aimTarget == "Held Item") {
                        val heldWeapons = target?.weapons?.filter { it.isEquipped }.orEmpty()
                        var heldMenu by remember(targetId, heldWeapons.map { it.id }) { mutableStateOf(false) }
                        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            OutlinedButton(onClick = { heldMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(heldWeapons.firstOrNull { it.id == heldItemWeaponId }?.name ?: if (heldWeapons.isEmpty()) "GM: non-weapon held item" else "انتخاب Held Weapon", color = Muted)
                            }
                            DropdownMenu(expanded = heldMenu, onDismissRequest = { heldMenu = false }) {
                                heldWeapons.forEach { hw -> DropdownMenuItem(text = { Text(gtr(hw.name)) }, onClick = { heldItemWeaponId = hw.id; heldMenu = false }) }
                            }
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text("Area / Explosive / Shotgun Shell را از پنل Advanced پایین انجام بده؛ Suppressive Fire را از Mode همین سلاح انتخاب کن و با FIRE بزن.", color = Muted, fontSize = 9.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Right) }
                    HorizontalDivider(color = Red.copy(alpha = .35f), modifier = Modifier.padding(vertical = 8.dp))
                    workflowWeapon?.let { w ->
                        Text("۵) اکشن روی سلاح انتخاب‌شده", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        if (w.magazineSize > 0) {
                            Text(gtr("Ammo: %1s/%2s • %3s", w.currentAmmo, w.magazineSize, w.loadedAmmoName), color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (w.magazineSize > 0 && w.currentAmmo < w.magazineSize) {
                            OutlinedButton(onClick = { reloadWeaponId = w.id }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                Text(gtr("RELOAD / CHOOSE AMMO"), color = White)
                            }
                        }
                        // پرت‌کردن (نارنجک/تیغ) هم از همان دکمه‌ی FIRE پایین صفحه
                        // انجام می‌شود؛ برچسب آن دکمه برای سلاح پرتابی «THROW» می‌شود.

                    if (w.modes.contains("Suppressive Fire", true)) {
                        // اینجا هیچ دکمه‌ی شلیکی نیست: شلیک فقط با FIRE پایین صفحه
                        // انجام می‌شود. Suppressive Fire یکی از Modeهای همین سلاح است.
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(gtr("Suppressive Fire را از Mode همین سلاح انتخاب کن (۱۰ گلوله) و با دکمهٔ FIRE پایین صفحه بزن؛ هدف‌ها همان‌هایی هستند که در AREA TARGETS تیک زده‌ای."), color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f))
                            RuleInfoButton("combat.suppressive_fire")
                        }
                    }
                    }
                    } else {
                        Text("حمله با بدن • BODY / GRAPPLE ACTIONS", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text("دفاع هدف: ضربهٔ Brawling و Martial Arts مانند Melee با Evasion (۱d10 + DEX + Evasion) دفع می‌شود، پس دکمهٔ Dodge/شرط REF ۸ اینجا مرتبط نیست. Grab و رهایی گرپل مقابلهٔ DEX + Brawling هستند (تساوی = برنده مدافع) و Choke/Throw بعد از Grab موفق خودکار موفق‌اند. حرکات ویژٔ Martial Arts جداگانه در بخش MARTIAL ARTS SPECIAL MOVES است.", color = Muted, fontSize = 9.sp, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Right)
                        }
                    if (grapplingTargetId != null || grappledById != null) {
                        val otherId = grapplingTargetId ?: grappledById
                        val other = allCharacters.firstOrNull { it.id == otherId }
                        Text(gtr("GRAPPLE: %1s • -2 to all Actions%2s", other?.handle?.ifBlank { other.name } ?: otherId, if (grappledById != null) " • no Move Action" else ""), color = Red, fontSize = 12.sp)
                    }
                    if (character.combatEffects.containsKey("prone")) {
                        OutlinedButton(onClick = { if (attackActionUsed == 0) { viewModel.markProne(character.id, false); attackActionCap = 1; attackActionUsed = 1; actionResult = "Get Up: Prone cleared; Action used." } }, enabled = attackActionUsed == 0, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) { Text(gtr("GET UP (ACTION)"), color = Red) }
                    }
                    // چرا بعضی دکمه‌های «بدن» خاموش‌اند؟ خودِ صفحه دلیلش را می‌گوید،
                    // چون قبلاً دکمه‌های خاکستری بدون توضیح می‌ماندند و GM نمی‌دانست چه کم است.
                    run {
                        val reasons = buildList {
                            if (currentTargetForActions == null) add("یک Target انتخاب کن")
                            if (attackActionUsed > 0 && !gmOverrideAction) add("Action این نوبت مصرف شده — برای اکشن بیشتر «GM: free fire» را روشن کن")
                            if (grapplingTargetId == null) add("CHOKE و THROW بعد از Grab موفق فعال می‌شوند")
                        }
                        if (reasons.isEmpty()) Text(gtr("همه‌ی اکشن‌های بدن باز است."), color = Color(0xFF66BB6A), fontSize = 9.sp)
                        else Text("خاموش یعنی: " + reasons.joinToString(" • "), color = Color(0xFFFFC107), fontSize = 9.sp, lineHeight = 14.sp)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            val t = currentTargetForActions ?: return@OutlinedButton
                            val fake = Weapon(id = -1001, name = "Brawling", category = "Brawling", type = "Brawling", damage = bodyDamage, rof = 2, modes = "Melee")
                            performAttack(fake, false)
                        }, enabled = currentTargetForActions != null, modifier = Modifier.weight(1f)) { Text(gtr("BRAWL %1s", bodyDamage), color = Red, fontSize = 11.sp) }
                        OutlinedButton(onClick = {
                            val t = currentTargetForActions ?: return@OutlinedButton
                            if (CombatRules.skillLevel(character, "Martial Arts") <= 0) { actionResult = "Martial Arts requires at least Rank 1."; return@OutlinedButton }
                            val fake = Weapon(id = -1002, name = "Martial Arts", category = "Martial Arts", type = "Martial Arts", damage = CombatRules.bodyDamageExpression(character.stats.body), rof = 2, modes = "Melee")
                            performAttack(fake, false)
                        }, enabled = currentTargetForActions != null && CombatRules.skillLevel(character, "Martial Arts") > 0, modifier = Modifier.weight(1f)) { Text(gtr("MARTIAL ARTS"), color = Red, fontSize = 11.sp) }
                        // گزارش تست ۳.۴: بدون مهارت Martial Arts (حداقل Rank 1) دکمه باید غیرفعال باشد،
                        // نه اینکه کلیک شود و پیام خطا بدهد.
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            val t = currentTargetForActions ?: return@OutlinedButton
                            if (attackActionUsed > 0 && !gmOverrideAction) { actionResult = "Action already used."; return@OutlinedButton }
                            val ar = CombatRules.rollD10(); val dr = CombatRules.rollD10()
                            val a = EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex) + CombatRules.skillLevel(character, "Brawling") + CombatRules.attackPenalty(character) + ar.totalDie
                            val d = EquipmentUseRules.effectiveStat(t, "DEX", t.stats.dex) + CombatRules.skillLevel(t, "Brawling") + CombatRules.attackPenalty(t) + dr.totalDie
                            if (a > d) { viewModel.startGrapple(character.id, t.id); actionResult = gtr("GRAB SUCCESS: %1s > %2s • both -2 Actions; defender loses Move Action.", a, d) }
                            else actionResult = gtr("GRAB FAILED: %1s vs %2s (defender wins ties).", a, d)
                            if (attackActionCap == null) attackActionCap = 1
                            attackActionUsed += 1
                        }, enabled = currentTargetForActions != null && grapplingTargetId == null && (attackActionUsed == 0 || gmOverrideAction), modifier = Modifier.weight(1f)) { Text(gtr("GRAB"), color = Red) }
                        OutlinedButton(onClick = {
                            val tid = grapplingTargetId ?: return@OutlinedButton
                            if (attackActionUsed > 0 && !gmOverrideAction) { actionResult = "Action already used."; return@OutlinedButton }
                            val count = viewModel.applyChoke(character.id, tid, character.stats.body, viewModel.combatRound)
                            actionResult = gtr("CHOKE: %1s direct HP • successive choke %2s/3%3s", character.stats.body, count, if (count >= 3) " • UNCONSCIOUS" else "")
                            if (attackActionCap == null) attackActionCap = 1
                            attackActionUsed += 1
                        }, enabled = grapplingTargetId != null && (attackActionUsed == 0 || gmOverrideAction), modifier = Modifier.weight(1f)) { Text(gtr("CHOKE"), color = Red) }
                        OutlinedButton(onClick = {
                            val tid = grapplingTargetId ?: return@OutlinedButton
                            if (attackActionUsed > 0 && !gmOverrideAction) { actionResult = "Action already used."; return@OutlinedButton }
                            viewModel.applyDirectCombatHpDamage(tid, character.stats.body)
                            viewModel.endGrapple(character.id, tid)
                            viewModel.markProne(tid, true)
                            actionResult = gtr("THROW: %1s direct HP • grapple ended • target Prone.", character.stats.body)
                            if (attackActionCap == null) attackActionCap = 1
                            attackActionUsed += 1
                        }, enabled = grapplingTargetId != null && (attackActionUsed == 0 || gmOverrideAction), modifier = Modifier.weight(1f)) { Text(gtr("THROW"), color = Red) }
                    }
                    currentTargetForActions?.let { targetForGrab ->
                        val held = targetForGrab.weapons.filter { it.isEquipped }
                        if (held.isNotEmpty() && grapplingTargetId == null) {
                            var grabItemMenu by remember(targetForGrab.id, held.map { it.id }) { mutableStateOf(false) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    OutlinedButton(onClick = { grabItemMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text(held.firstOrNull { it.id == grabItemWeaponId }?.name ?: "Held item", color = Muted, fontSize = 9.sp)
                                    }
                                    DropdownMenu(expanded = grabItemMenu, onDismissRequest = { grabItemMenu = false }) {
                                        held.forEach { hw -> DropdownMenuItem(text = { Text(gtr(hw.name)) }, onClick = { grabItemWeaponId = hw.id; grabItemMenu = false }) }
                                    }
                                }
                                OutlinedButton(onClick = {
                                    if (attackActionUsed > 0 && !gmOverrideAction) return@OutlinedButton
                                    val wid = grabItemWeaponId ?: held.first().id
                                    val ar = CombatRules.rollD10(); val dr = CombatRules.rollD10()
                                    val a = EquipmentUseRules.effectiveStat(character, "DEX", character.stats.dex) + CombatRules.skillLevel(character, "Brawling") + CombatRules.attackPenalty(character) + ar.totalDie
                                    val d = EquipmentUseRules.effectiveStat(targetForGrab, "DEX", targetForGrab.stats.dex) + CombatRules.skillLevel(targetForGrab, "Brawling") + CombatRules.attackPenalty(targetForGrab) + dr.totalDie
                                    actionResult = if (a > d && viewModel.transferHeldWeapon(character.id, targetForGrab.id, wid)) gtr("GRAB ITEM SUCCESS: %1s > %2s • item taken into your free hand.", a, d) else gtr("GRAB ITEM FAILED: %1s vs %2s (defender wins ties).", a, d)
                                    if (attackActionCap == null) attackActionCap = 1
                                    attackActionUsed += 1
                                }, enabled = attackActionUsed == 0 || gmOverrideAction, modifier = Modifier.weight(1f)) { Text(gtr("GRAB ITEM"), color = Red, fontSize = 9.sp) }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text("در Grab موفق می‌توانی به‌جای شروع Grapple یک وسیلهٔ در دست Defender را بگیری. GM تأیید می‌کند که دست آزاد داری.", color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
                                }
                                RuleInfoButton("combat.grab_item")
                            }
                        }
                    }
                    if (grappledById != null) {
                        OutlinedButton(onClick = {
                            if ((attackActionUsed > 0 && !gmOverrideAction) || character.combatEffects.containsKey("no_action_this_turn")) return@OutlinedButton
                            val grappler = allCharacters.firstOrNull { it.id == grappledById } ?: return@OutlinedButton
                            val ar = CombatRules.rollD10(); val dr = CombatRules.rollD10()
                            val ironGrip = character.combatEffects.containsKey("iron_grip_by_$grappledById")
                            val a = EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex) + CombatRules.skillLevel(character,"Brawling") + CombatRules.attackPenalty(character) + ar.totalDie + if(ironGrip) -2 else 0
                            val d = EquipmentUseRules.effectiveStat(grappler, "DEX", grappler.stats.dex) + CombatRules.skillLevel(grappler,"Brawling") + CombatRules.attackPenalty(grappler) + dr.totalDie
                            if(a>d){ viewModel.endGrapple(grappledById,character.id); actionResult=gtr("ESCAPE GRAPPLE SUCCESS: %1s > %2s%3s", a, d, if(ironGrip) " • Iron Grip -2 applied" else "") } else actionResult=gtr("ESCAPE GRAPPLE FAIL: %1s vs %2s%3s", a, d, if(ironGrip) " • Iron Grip -2 applied" else "")
                            attackActionCap=1; attackActionUsed=1
                        }, enabled=(attackActionUsed==0 || gmOverrideAction) && !character.combatEffects.containsKey("no_action_this_turn"), modifier=Modifier.fillMaxWidth()) { Text(gtr("ACTION: ESCAPE GRAPPLE"),color=Red) }
                    }
                    if (grapplingTargetId != null) {
                        TextButton(onClick = { viewModel.endGrapple(character.id, grapplingTargetId); actionResult = "Grapple released (no Action)." }) { Text(gtr("RELEASE GRAPPLE — FREE"), color = Muted) }
                    }
                    }
                }
            }

                HorizontalDivider(color = Red.copy(alpha = .35f), modifier = Modifier.padding(vertical = 10.dp))
                Text("۶) دفاع هدف (Defense) — HOW THE TARGET DEFENDS", color = Red, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                shape = CutCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).border(1.dp, Red.copy(alpha = 0.35f), CutCornerShape(10.dp))
            ) {
                Column(Modifier.padding(12.dp)) {
                    val defender = target
                    if (defender == null) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text("اول در کارت ATTACK یک Target انتخاب کن.", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) }
                    } else {
                        val shieldHp = viewModel.combatShieldHp(defender.id)
                        val humanId = viewModel.humanShieldId(defender.id)
                        val humanName = humanId?.let { id -> allCharacters.firstOrNull { it.id == id }?.let { it.handle.ifBlank { it.name } } }
                        val inCover = viewModel.isInCover(defender.id)
                        val canDodge = CombatRules.canDodgeRanged(defender)
                        Text(gtr("DEFENDER • %1s", defender.handle.ifBlank { defender.name }), color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(gtr("Dodge: %1s • Shield: %2s • Human Shield: %3s • Cover: %4s", if (canDodge) "مجاز" else "غیرمجاز", if (shieldHp > 0) "$shieldHp HP" else "ندارد", humanName ?: "ندارد", if (inCover) "بله" else "خیر"), color = Muted, fontSize = 9.sp)
                        Spacer(Modifier.height(8.dp))
                        val modes = listOf("Auto", "Dodge", "Shield", "Human Shield", "Range DV", "Cover")
                        modes.chunked(2).forEach { rowModes ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowModes.forEach { mode ->
                                    val enabledMode = when (mode) {
                                        "Dodge" -> canDodge
                                        "Shield" -> shieldHp > 0
                                        "Human Shield" -> humanId != null
                                        "Cover" -> inCover
                                        else -> true
                                    }
                                    FilterChip(
                                        selected = defenseMode == mode,
                                        onClick = { defenseMode = mode; targetDodges = mode == "Dodge" || mode == "Auto" },
                                        enabled = enabledMode,
                                        label = { Text(gtr(mode), fontSize = 9.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowModes.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        val defenseHint = when (defenseMode) {
                            "Dodge" -> "حملهٔ Ranged با Evasion هدف مقایسه می‌شود. برای Melee هم Evasion دفاع طبیعی است."
                            "Shield" -> "اگر حمله Hit شود، Damage کامل به Shield می‌خورد. در همان حمله Dodge انجام نمی‌شود."
                            "Human Shield" -> "در Hit معتبر، Damage به گروگان منتقل می‌شود؛ Melee و Aimed Shot به Head از این مسیر عبور می‌کنند."
                            "Range DV" -> "هدف Dodge نمی‌کند؛ Attack Total فقط با DV فاصلهٔ سلاح مقایسه می‌شود."
                            "Cover" -> "Cover واقعی Line of Sight را قطع می‌کند؛ این انتخاب حملهٔ مستقیم را متوقف می‌کند."
                            else -> "Auto: برنامه از Shield فعال، امکان Dodge و Human Shield موجود استفاده می‌کند. برای کنترل دقیق یکی از گزینه‌ها را دستی انتخاب کن."
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(gtr(defenseHint), color = if (defenseMode == "Cover") Red else Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Right)
                        }
                    }
                }
            }
            }
        }

        // فقط چیزهایی که واقعاً روی همین نبرد اثر دارند نشان داده می‌شوند؛
        // شمارنده‌های داخلی برنامه و اثرهای غیرنظامی (دارو/درمان/پشتیبان) اینجا
        // نمی‌آیند تا GM بفهمد کدام وضعیت در نبرد معنا دارد.
        val combatFx = com.cyberpunk.gmtool.data.CombatEffects.combatEntries(character.combatEffects)
        val hiddenFx = com.cyberpunk.gmtool.data.CombatEffects.hiddenNonCombatCount(character.combatEffects)
        if (combatFx.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(colors=CardDefaults.cardColors(containerColor=CardBg), modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    CombatSectionHeader(
                        title = gtr("ACTIVE COMBAT EFFECTS"),
                        help = "فقط وضعیت‌هایی که مکانیکِ همین نبرد را عوض می‌کنند: Prone، آتش، گِرَپل، سرکوب، بی‌هوشی و محدودیت Action/Move. هر اثر با اثر مکانیکی خودش نوشته شده. شمارنده‌های داخلی برنامه و اثرهای دارویی/درمانی اینجا نمی‌آیند، چون جایشان تب Medtech یا دستیار Lawman است.",
                        expanded = effectsExpanded,
                        summary = gtr("%1s اثر مؤثر در نبرد", combatFx.size),
                        onToggle = { effectsExpanded = !effectsExpanded },
                        onHelp = { combatHelp = it }
                    )
                    if (effectsExpanded) {
                    combatFx.forEach { (k, v) ->
                        val info = com.cyberpunk.gmtool.data.CombatEffects.entry(k)
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(gtr(info.labelFa), color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                if (v > 0) Text(gtr("%1s Turn", v), color = Muted, fontSize = 10.sp)
                                if (com.cyberpunk.gmtool.data.CombatEffects.canClearManually(k)) {
                                    TextButton(
                                        onClick = { viewModel.clearCombatEffect(character.id, k); actionResult = gtr("وضعیت «%1s» پاک شد.", info.labelFa) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) { Text(gtr("پاک"), color = Muted, fontSize = 10.sp) }
                                }
                            }
                            Text(gtr(info.detailFa), color = White, fontSize = 10.sp, lineHeight = 16.sp)
                        }
                    }
                    if (character.combatEffects.containsKey("on_fire_2") || character.combatEffects.containsKey("on_fire_4")) {
                        OutlinedButton(onClick={ viewModel.clearCombatEffect(character.id,"on_fire_2"); viewModel.clearCombatEffect(character.id,"on_fire_4"); attackActionCap=1; attackActionUsed=1 }, modifier=Modifier.fillMaxWidth().padding(top=6.dp)) { Text(gtr("ACTION: PUT OUT FIRE"), color=Red) }
                    }
                    if (hiddenFx > 0) {
                        Text(
                            gtr("%1s اثر غیرنظامی (دارو / درمان / پشتیبان در راه) در این کارت نمایش داده نمی‌شود — جای آن تب Medtech، BIO یا دستیار Lawman است.", hiddenFx),
                            color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    }
                }
            }
        }

        actionResult?.let { result ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(gtr("LAST ACTION"), color = Red, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(gtr(result), color = White, fontSize = 11.sp)
                    }
                    TextButton(onClick = { actionResult = null }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) { Text("×", color = Muted, fontSize = 18.sp) }
                }
            }
        }


        CombatSectionLabel(4, "AREA DAMAGE / آسیب منطقه‌ای و هدف پیشرفته")
        CombatSectionHeader(
            title = gtr("ADVANCED TARGET / AREA"),
            help = "این بخش برای تنظیمات پیشرفته هدف است: Area Targets برای انفجار، Shotgun Shell و Suppressive Fire، و همچنین تغییر دستی Dodge یا Aimed Shot. برای یک حمله معمولی از کارت ATTACK استفاده کن. اول هدف و فاصله را تعیین کن. برای شلیک، فاصله DV سلاح را تعیین می‌کند؛ اگر هدف شرایط Dodge گلوله را داشته باشد می‌تواند به‌جای DV با Evasion دفاع کند. Aimed Shot معمولاً -8 دارد و اثرش بسته به Head، Leg یا Held Item فرق می‌کند.",
            expanded = targetExpanded,
            summary = target?.let { "${it.handle.ifBlank { it.name }} • ${distanceText.ifBlank { "?" }}m" } ?: gtr("No target • %1sm", distanceText.ifBlank { "?" }),
            onToggle = { targetExpanded = !targetExpanded },
            onHelp = { combatHelp = it }
        )
        if (targetExpanded) {
        Box {
            OutlinedButton(onClick = { targetMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(target?.let { gtr("Target: %1s", it.handle.ifBlank { it.name }) } ?: "Target: none (roll only)", color = White)
            }
            DropdownMenu(expanded = targetMenu, onDismissRequest = { targetMenu = false }) {
                DropdownMenuItem(text = { Text(gtr("None")) }, onClick = { targetId = null; targetMenu = false })
                availableTargets.forEach { c ->
                    DropdownMenuItem(text = { Text(c.handle.ifBlank { c.name }) }, onClick = { targetId = c.id; targetMenu = false })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = distanceText, onValueChange = { distanceText = it.filter(Char::isDigit).take(4) },
            label = { Text(gtr("Distance m/yd")) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showBlastDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(gtr("AREA TARGETS (%1s) • انتخاب هدف‌های انفجار و Suppressive", blastTargetIds.size), color = Red)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = targetDodges, onCheckedChange = { targetDodges = it })
            Text(gtr("Target attempts dodge when REF 8+"), color = White, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Box {
                TextButton(onClick = { aimedMenu = true }) { Text(gtr("AIM: %1s%2s", aimTarget, if (aimTarget != "None") " (-8)" else ""), color = if (aimTarget == "None") Muted else Red, fontSize = 12.sp) }
                DropdownMenu(expanded = aimedMenu, onDismissRequest = { aimedMenu = false }) {
                    listOf("None", "Head", "Held Item", "Leg").forEach { choice ->
                        DropdownMenuItem(text = { Text(gtr(choice)) }, onClick = { aimTarget = choice; aimedMenu = false })
                    }
                }
            }
        }
        }
        CombatSectionLabel(5, "COVER / SHIELD / کاور و سپر")
        CombatSectionHeader(
            title = gtr("COVER / SHIELD"),
            help = "در RED چیزی به اسم نیمه‌کاور نداریم: Cover باید خط دید را واقعاً قطع کند و خودش HP دارد. Damage اول به Cover می‌خورد. Shield یک Cover قابل‌حمل است؛ وقتی آن را جلوی حمله می‌گیری دیگر همان حمله را Dodge نمی‌کنی. Human Shield فقط از Grapple ساخته می‌شود.",
            expanded = coverExpanded,
            summary = when { viewModel.combatShieldHp(character.id) > 0 -> gtr("Shield %1s HP", viewModel.combatShieldHp(character.id)); viewModel.isInCover(character.id) -> "In cover"; else -> "No cover" },
            onToggle = { coverExpanded = !coverExpanded },
            onHelp = { combatHelp = it }
        )
        if (coverExpanded) {
        Text(gtr("%1s • HP %2s/%3s", coverLabel, coverHp, coverMaxHp), color = White, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { viewModel.resolveSuppressiveCover(character.id); actionResult = "Character marked in cover." }, modifier = Modifier.weight(1f)) { Text(gtr("MARK IN COVER"), color = Red, fontSize = 9.sp) }
            OutlinedButton(onClick = { viewModel.leaveCover(character.id); actionResult = "Character left cover." }, modifier = Modifier.weight(1f)) { Text(gtr("LEAVE COVER"), color = Muted, fontSize = 9.sp) }
        }
        val coverPresets = listOf(
            "Thin Wood" to 5, "Thick Wood" to 20,
            "Thin Concrete" to 10, "Thick Concrete" to 25,
            "Thin Bulletproof Glass" to 15, "Thick Bulletproof Glass" to 30,
            "Thin Stone" to 20, "Thick Stone" to 40,
            "Thin Steel" to 25, "Thick Steel" to 50,
            "Plaster/Foam/Plastic" to 0, "Bulletproof Shield" to 10
        )
        var coverPresetMenu by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { coverPresetMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(gtr("SELECT COVER / SHIELD"), color = Red)
            }
            DropdownMenu(expanded = coverPresetMenu, onDismissRequest = { coverPresetMenu = false }) {
                coverPresets.forEach { (label, hp) ->
                    DropdownMenuItem(text = { Text(gtr("%1s • %2s HP", label, hp)) }, onClick = {
                        coverLabel = label; coverHp = hp; coverMaxHp = hp; coverPresetMenu = false
                    })
                }
            }
        }
        OutlinedTextField(
            value = coverDamageExpr,
            onValueChange = { coverDamageExpr = it.filter { ch -> ch.isDigit() || ch == 'd' || ch == 'D' }.lowercase() },
            label = { Text(gtr("Incoming damage (e.g. 5d6)")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = {
                if (coverHp > 0) {
                    val d = CombatRules.rollDamage(coverDamageExpr.ifBlank { "1d6" })
                    coverHp = (coverHp - d.total).coerceAtLeast(0)
                    actionResult = gtr("%1s takes %2s damage → %3s HP. Ordinary excess damage is lost when cover breaks; resolve explosive attacks separately.", coverLabel, d.total, coverHp)
                }
            }, enabled = coverHp > 0, modifier = Modifier.weight(1f)) { Text(gtr("DAMAGE COVER"), color = Red, fontSize = 9.sp) }
            OutlinedButton(onClick = { coverHp = 0; coverMaxHp = 0; coverLabel = "No Cover" }, modifier = Modifier.weight(1f)) { Text(gtr("CLEAR"), color = Muted, fontSize = 9.sp) }
        }
        val personalShieldHp = viewModel.combatShieldHp(character.id)
        val personalShieldKind = viewModel.combatShieldKind(character.id)
        val hasBulletShield = character.inventory.any { it.equipped && it.name.equals("Bulletproof Shield", true) }
        val hasPopupShield = character.inventory.any { it.equipped && it.name.equals("Popup Shield", true) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (hasBulletShield) OutlinedButton(onClick = {
                if (attackActionUsed > 0) actionResult = "Equipping a shield requires an unused Action."
                else if (viewModel.readyCombatShield(character.id, "bulletproof") == "OK") { attackActionCap=1; attackActionUsed=1; actionResult = "Bulletproof Shield equipped at 10 HP • Action used." }
                else actionResult = "Shield unavailable."
            }, enabled=attackActionUsed==0, modifier=Modifier.weight(1f)) { Text(gtr("EQUIP SHIELD"), color=Red, fontSize=9.sp) }
            if (hasPopupShield) OutlinedButton(onClick = { actionResult = if (viewModel.readyCombatShield(character.id, "popup") == "OK") "Popup Shield extended at 10 HP (no Action)." else "Popup Shield unavailable." }, modifier=Modifier.weight(1f)) { Text(gtr("POPUP SHIELD"), color=Red, fontSize=9.sp) }
            if (personalShieldHp > 0) OutlinedButton(onClick = {
                if (personalShieldKind == "popup") { viewModel.stowCombatShield(character.id); actionResult = "Popup Shield stowed (no Action)." }
                else if (attackActionUsed == 0) { viewModel.stowCombatShield(character.id); attackActionCap=1; attackActionUsed=1; actionResult = "Shield dropped • Action used." }
                else actionResult = "Dropping this shield requires an unused Action."
            }, enabled = personalShieldKind == "popup" || attackActionUsed == 0, modifier=Modifier.weight(1f)) { Text(gtr("STOW/DROP"), color=Muted, fontSize=9.sp) }
        }
        if (personalShieldHp > 0) Text(gtr("ACTIVE SHIELD: %1s • %2s HP", personalShieldKind ?: "shield", personalShieldHp), color=Red, fontWeight=FontWeight.Bold, fontSize=11.sp)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text("Cover خط دید را کاملاً قطع می‌کند و در RED حالت Partial Cover نداریم. Bulletproof/Popup Shield یک Cover با 10 HP است. اگر سپر با همان ضربه نابود شود، همان ضربه را کامل جذب می‌کند و Damage اضافه از آن عبور نمی‌کند.", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        }
        if (grapplingTargetId != null) {
            val hs = allCharacters.firstOrNull { it.id == grapplingTargetId }
            val holding = viewModel.humanShieldId(character.id) == grapplingTargetId
            OutlinedButton(onClick = {
                if (holding) {
                    viewModel.endGrapple(character.id, grapplingTargetId)
                    actionResult = "Live Human Shield released by ending the Grapple (no Action)."
                } else if (attackActionUsed == 0 && viewModel.equipHumanShield(character.id, grapplingTargetId)) {
                    attackActionCap = 1; attackActionUsed = 1
                    actionResult = gtr("%1s equipped as Human Shield • Action used. Ranged attacks you can see (except aimed head shots) may be interposed; Melee cannot be blocked.", hs?.handle?.ifBlank { hs.name } ?: "Target")
                } else actionResult = "Human Shield requires an active Grapple and an unused Action."
            }, modifier = Modifier.fillMaxWidth(), enabled = holding || attackActionUsed == 0) {
                Text(if (holding) "RELEASE HUMAN SHIELD / END GRAPPLE" else "ACTION: EQUIP GRAPPLED TARGET AS HUMAN SHIELD", color = Red, fontSize = 10.sp)
            }
        }

        }

        val maAikido = character.skills.firstOrNull { it.name.contains("Martial Arts",true) && it.name.contains("Aikido",true) }?.level ?: 0
        val maKarate = character.skills.firstOrNull { it.name.contains("Martial Arts",true) && it.name.contains("Karate",true) }?.level ?: 0
        val maJudo = character.skills.firstOrNull { it.name.contains("Martial Arts",true) && it.name.contains("Judo",true) }?.level ?: 0
        val maTkd = character.skills.firstOrNull { it.name.contains("Martial Arts",true) && (it.name.contains("Taekwondo",true) || it.name.contains("Tae Kwon",true)) }?.level ?: 0
        // گزارش تست ۳.۴: مهارت عام «Martial Arts» (بدون سبک مشخص، یا سبکی که حرکت‌هایش
        // پیاده‌سازی نشده) قبلاً هیچ بخشی را نشان نمی‌داد. حالا بخش باز می‌شود، توضیح
        // می‌دهد چرا حرکت سبک‌محور در دسترس نیست، و RECOVERY (که سبک نمی‌خواهد) کار می‌کند.
        val maKnownStyles = listOf("Aikido", "Karate", "Judo", "Taekwondo", "Tae Kwon")
        val maGeneric = character.skills
            .filter { it.name.startsWith("Martial Arts", true) && maKnownStyles.none { s -> it.name.contains(s, true) } }
            .maxOfOrNull { it.level } ?: 0
        val maStyleCount = listOf(maAikido, maKarate, maJudo, maTkd).count { it > 0 }
        if (maAikido + maKarate + maJudo + maTkd + maGeneric > 0) {
            CombatSectionHeader(
                title = gtr("MARTIAL ARTS SPECIAL MOVES"),
                help = "حرکت‌های ویژه Martial Arts شرط دارند؛ صرف داشتن Skill کافی نیست. بعضی‌ها WILL یا MOVE حداقل می‌خواهند و بعضی Combination هستند، یعنی باید در همان Turn Hitهای مشخصی را قبل از آن ثبت کرده باشی. READY یعنی شرط‌های فعلی برقرارند.",
                expanded = martialExpanded,
                summary = when {
                    maStyleCount > 0 && maGeneric > 0 -> gtr("%1s form + generic", maStyleCount)
                    maStyleCount > 0 -> gtr("%1s form", maStyleCount)
                    else -> "Generic Martial Arts"
                },
                onToggle = { martialExpanded = !martialExpanded },
                onHelp = { combatHelp = it }
            )
            if (martialExpanded) {
            if (maGeneric > 0) {
                Text(
                    "این شخصیت مهارت Martial Arts بدون سبک مشخص (یا با سبکی غیر از Aikido/Karate/Judo/Taekwondo) دارد. حرکت‌های ویژه‌ی سبک‌محور در این برنامه به نام سبک در مهارت گره خورده‌اند؛ مثلاً «Martial Arts (Karate)». تنها حرکتی که سبک نمی‌خواهد RECOVERY است که در صورت Prone بودن پایین ظاهر می‌شود. حمله‌ی معمولی Martial Arts از کارت ATTACK، حالت «بدن» در دسترس است.",
                    color = Muted, fontSize = 11.sp, lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), textAlign = TextAlign.Right
                )
            }
            if (character.combatEffects.containsKey("prone")) OutlinedButton(onClick={
                if (attackActionUsed > 0) return@OutlinedButton
                val rank=maxOf(maAikido,maKarate,maJudo,maTkd,maGeneric); val r=CombatRules.rollD10(); val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+rank+r.totalDie+CombatRules.attackPenalty(character)
                viewModel.markProne(character.id,false)
                if(total>13){ actionResult=gtr("RECOVERY SUCCESS %1s vs DV13 • Get Up completed and costs no Action.", total) }
                else { attackActionCap=1; attackActionUsed=1; actionResult=gtr("RECOVERY FAIL %1s vs DV13 • Get Up still succeeds, but it costs your Action.", total) }
            },enabled=attackActionUsed==0,modifier=Modifier.fillMaxWidth()){Text(gtr("RECOVERY • GET UP + DV13"),color=Red)}
            if(maAikido>0 && grapplingTargetId!=null) OutlinedButton(onClick={
                if(attackActionUsed>0 || character.combatEffects.containsKey("used_ma_iron_grip"))return@OutlinedButton
                viewModel.addCombatEffect(character.id,"used_ma_iron_grip",-1); val r=CombatRules.rollD10(); val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+maAikido+r.totalDie+CombatRules.attackPenalty(character)
                if(total>15){ viewModel.addCombatEffect(grapplingTargetId,"iron_grip_by_${character.id}",-1); actionResult="IRON GRIP SUCCESS • escape attempts -2; target cannot make Ranged Attacks." } else actionResult=gtr("IRON GRIP FAIL: %1s vs DV15", total)
                attackActionCap=1;attackActionUsed=1
            },enabled=attackActionUsed==0 && !character.combatEffects.containsKey("used_ma_iron_grip"),modifier=Modifier.fillMaxWidth()){Text(gtr("AIKIDO • IRON GRIP"),color=Red)}
            if(maKarate>0 && character.stats.will>=8 && currentTargetForActions!=null) OutlinedButton(onClick={
                if(attackActionUsed>0)return@OutlinedButton; val t=currentTargetForActions; val r=CombatRules.rollD10(); val d=CombatRules.rollD10(); val aimed=aimTarget=="Head"; val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+maKarate+r.totalDie+CombatRules.attackPenalty(character)+(if(aimed)-8 else 0); val def=CombatRules.defenderEvasionTotal(t,d)
                if(total>def){ val dmg=CombatRules.rollDamage(CombatRules.bodyDamageExpression(character.stats.body)); val injuryKey=if(aimed)"cracked_skull" else "broken_ribs"; val wasNew=!t.criticalInjuries.contains(injuryKey); viewModel.applyCombatDamage(t.id,dmg.total,head=aimed,melee=true,critical=dmg.critical,halvesArmor=true); viewModel.addCriticalInjury(t.id,injuryKey); actionResult=gtr("BONE BREAKING STRIKE HIT • %1s raw + %2s%3s", dmg.total, if(aimed)"Cracked Skull" else "Broken Ribs", if(wasNew)" + 5 Critical Bonus Damage" else " (already suffered; no duplicate Critical Injury/bonus)") } else actionResult=gtr("BONE BREAKING STRIKE MISS: %1s vs %2s", total, def)
                attackActionCap=1;attackActionUsed=1
            },modifier=Modifier.fillMaxWidth()){Text(gtr("KARATE • BONE BREAKING STRIKE"),color=Red)}
            if(maTkd>0 && character.stats.will>=8 && currentTargetForActions!=null) OutlinedButton(onClick={
                if(attackActionUsed>0)return@OutlinedButton; val t=currentTargetForActions; val r=CombatRules.rollD10(); val d=CombatRules.rollD10(); val aimed=aimTarget=="Head"; val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+maTkd+r.totalDie+CombatRules.attackPenalty(character)+(if(aimed)-8 else 0); val def=CombatRules.defenderEvasionTotal(t,d)
                if(total>def){ val dmg=CombatRules.rollDamage(CombatRules.bodyDamageExpression(character.stats.body)); val injuryKey=if(aimed)"brain_injury" else "spinal_injury"; val wasNew=!t.criticalInjuries.contains(injuryKey); viewModel.applyCombatDamage(t.id,dmg.total,head=aimed,melee=true,critical=dmg.critical,halvesArmor=true); viewModel.addCriticalInjury(t.id,injuryKey); actionResult=gtr("PRESSURE POINT STRIKE HIT • %1s%2s", if(aimed)"Brain Injury" else "Spinal Injury", if(wasNew)" + 5 Critical Bonus Damage" else " (already suffered; no duplicate Critical Injury/bonus)") } else actionResult=gtr("PRESSURE POINT STRIKE MISS: %1s vs %2s", total, def)
                attackActionCap=1;attackActionUsed=1
            },modifier=Modifier.fillMaxWidth()){Text(gtr("TAEKWONDO • PRESSURE POINT STRIKE"),color=Red)}
            if(maTkd>0 && CombatRules.effectiveMove(character)>=8 && currentTargetForActions!=null) OutlinedButton(onClick={
                if(attackActionUsed>0)return@OutlinedButton
                val t=currentTargetForActions
                val distance=distanceText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                if(distance>4){ actionResult="FLYING KICK requires target within 4m/yd."; return@OutlinedButton }
                val r=CombatRules.rollD10(); val d=CombatRules.rollD10(); val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+maTkd+r.totalDie+CombatRules.attackPenalty(character); val def=CombatRules.defenderEvasionTotal(t,d)
                viewModel.spendRemainingMovement(character.id)
                if(total>def){
                    val dmg=CombatRules.rollDamage(CombatRules.bodyDamageExpression(character.stats.body)); viewModel.applyCombatDamage(t.id,dmg.total,melee=true,critical=dmg.critical,halvesArmor=true); viewModel.markProne(t.id,true)
                    val seat=viewModel.vehicleSeatInfo(t.id)
                    if(seat!=null){
                        val owner=allCharacters.firstOrNull { it.id==seat.first }; val ov=owner?.ownedVehicles?.firstOrNull { it.id==seat.second }; val spec=ov?.let { VehicleCatalog.byName(it.specName) }
                        if(spec!=null && spec.tags.any { it in setOf("BIKE","JETSKI","GYRO") }) viewModel.leaveVehicle(t.id)
                    }
                    actionResult="FLYING KICK HIT • target Prone • all remaining movement spent." 
                } else actionResult=gtr("FLYING KICK MISS: %1s vs %2s • all remaining movement spent.", total, def)
                attackActionCap=1;attackActionUsed=1
            },enabled=viewModel.movementThisTurn(character.id)>=4 && attackActionUsed==0 && (distanceText.toIntOrNull() ?: 999)<=4,modifier=Modifier.fillMaxWidth()){Text(gtr("TAEKWONDO • FLYING KICK%1s", if(viewModel.movementThisTurn(character.id)>=4 && (distanceText.toIntOrNull() ?: 999)<=4) " • READY" else " • MOVE 4m / TARGET ≤4m"),color=Red,fontSize=10.sp)}
            if(maKarate>0 && currentTargetForActions!=null) {
                val t=currentTargetForActions
                val ready=character.combatEffects.containsKey("turn_hit_meleeweapon_${t.id}") && character.combatEffects.containsKey("turn_hit_martialarts_${t.id}") && !character.combatEffects.containsKey("used_ma_armor_breaking")
                OutlinedButton(onClick={
                    viewModel.addCombatEffect(character.id,"used_ma_armor_breaking",-1)
                    val r=CombatRules.rollD10(); val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+maKarate+r.totalDie+CombatRules.attackPenalty(character)
                    if(total>15){ viewModel.ablateArmor(t.id,true,2); viewModel.ablateArmor(t.id,false,2); actionResult=gtr("ARMOR BREAKING SUCCESS %1s vs DV15 • all worn armor locations ablated by 2.", total) } else actionResult=gtr("ARMOR BREAKING FAIL %1s vs DV15", total)
                },enabled=ready,modifier=Modifier.fillMaxWidth()){Text(gtr("KARATE • ARMOR BREAKING%1s", if(ready) " • READY" else ""),color=Red,fontSize=10.sp)}
            }
            if(maAikido>0 && currentTargetForActions!=null) {
                val t=currentTargetForActions
                val ready=character.combatEffects.containsKey("turn_hit_brawling_${t.id}") && character.combatEffects.containsKey("turn_hit_martialarts_${t.id}") && !character.combatEffects.containsKey("used_ma_disarming")
                OutlinedButton(onClick={
                    viewModel.addCombatEffect(character.id,"used_ma_disarming",-1)
                    val r=CombatRules.rollD10(); val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+maAikido+r.totalDie+CombatRules.attackPenalty(character)
                    actionResult=if(total>15) gtr("DISARMING COMBINATION SUCCESS %1s vs DV15 • choose one object in target's hands: take it or put it on the floor.", total) else gtr("DISARMING COMBINATION FAIL %1s vs DV15", total)
                },enabled=ready,modifier=Modifier.fillMaxWidth()){Text(gtr("AIKIDO • DISARMING COMBINATION%1s", if(ready) " • READY" else ""),color=Red,fontSize=10.sp)}
            }
            if(maJudo>0) {
                val dodgedKeys=character.combatEffects.keys.filter { it.startsWith("judo_dodged_from_") }
                val counterReady=dodgedKeys.isNotEmpty() && !character.combatEffects.containsKey("judo_melee_hit_since_turn") && !character.combatEffects.containsKey("used_ma_counter_throw")
                val counterTargetId=dodgedKeys.firstOrNull()?.removePrefix("judo_dodged_from_")?.toIntOrNull()
                OutlinedButton(onClick={
                    val tid=counterTargetId ?: return@OutlinedButton
                    viewModel.addCombatEffect(character.id,"used_ma_counter_throw",-1)
                    val r=CombatRules.rollD10(); val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+maJudo+r.totalDie+CombatRules.attackPenalty(character)
                    if(total>15){ viewModel.applyDirectCombatHpDamage(tid,character.stats.body); viewModel.markProne(tid,true); actionResult=gtr("COUNTER THROW SUCCESS %1s vs DV15 • unavoidable Throw: BODY %2s direct HP + Prone.", total, character.stats.body) } else actionResult=gtr("COUNTER THROW FAIL %1s vs DV15", total)
                    attackActionCap=1; attackActionUsed=1
                },enabled=counterReady && attackActionUsed==0,modifier=Modifier.fillMaxWidth()){Text(gtr("JUDO • COUNTER THROW%1s", if(counterReady) " • READY" else ""),color=Red,fontSize=10.sp)}
                if(grappledById!=null){
                    val meleeHits=character.combatEffects.keys.firstOrNull { it.startsWith("turn_melee_count_${grappledById}_") }?.substringAfterLast('_')?.toIntOrNull() ?: 0
                    val ready=meleeHits >= 2 && !character.combatEffects.containsKey("used_ma_grab_escape")
                    OutlinedButton(onClick={
                        viewModel.addCombatEffect(character.id,"used_ma_grab_escape",-1)
                        val r=CombatRules.rollD10(); val total=EquipmentUseRules.effectiveStat(character,"DEX",character.stats.dex)+maJudo+r.totalDie+CombatRules.attackPenalty(character)
                        if(total>15){ viewModel.endGrapple(grappledById,character.id); viewModel.addCriticalInjury(grappledById,"broken_arm"); actionResult=gtr("GRAB ESCAPE SUCCESS %1s vs DV15 • Grapple ended + Broken Arm + 5 Critical Bonus Damage.", total) } else actionResult=gtr("GRAB ESCAPE FAIL %1s vs DV15", total)
                    },enabled=ready,modifier=Modifier.fillMaxWidth()){Text(gtr("JUDO • GRAB ESCAPE%1s", if(ready) " • READY" else ""),color=Red,fontSize=10.sp)}
                }
            }
            }
        }

        Spacer(Modifier.height(20.dp))
        CombatSectionLabel(6, "VEHICLE COMBAT / نبرد خودرویی")
        CombatSectionHeader(
            title = gtr("VEHICLE COMBAT"),
            help = "خودرو SDP دارد و ممکن است SP داشته باشد. Driver، روشن/خاموش بودن، Basic Driving، Maneuver، NOS، Ram، شیشه ضدگلوله و سلاح‌های نصب‌شده اینجا مدیریت می‌شوند. Maneuver ناموفق باعث Lost Control می‌شود و GM حرکت آن Turn را تعیین می‌کند؛ برخورد مثل Ram حل می‌شود.",
            expanded = vehicleExpanded,
            summary = if (character.ownedVehicles.isEmpty()) "No vehicle" else gtr("%1s vehicle", character.ownedVehicles.size),
            onToggle = { vehicleExpanded = !vehicleExpanded },
            onHelp = { combatHelp = it }
        )
        if (vehicleExpanded) {
        val liveCharacter = allCharacters.firstOrNull { it.id == character.id } ?: character
        val selectedVehicle = liveCharacter.ownedVehicles.firstOrNull { it.id == selectedVehicleId }
        val selectedSpec = selectedVehicle?.let { VehicleCatalog.byName(it.specName) }
        if (liveCharacter.ownedVehicles.isEmpty()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text("Vehicle دائمی نداری. از Store یکی بخر یا یک Vehicle از Family Motorpool اضافه کن.", color=Muted, fontSize=11.sp, modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.Right) }
        } else {
            Box {
                OutlinedButton(onClick={vehicleMenu=true},modifier=Modifier.fillMaxWidth()){
                    Text(selectedVehicle?.let { if(it.customName.isBlank()) it.specName else it.customName } ?: "SELECT VEHICLE",color=Red)
                }
                DropdownMenu(expanded=vehicleMenu,onDismissRequest={vehicleMenu=false}) {
                    liveCharacter.ownedVehicles.forEach { v -> DropdownMenuItem(text={Text("${v.specName} • ${v.source}${if (v.source == "FAMILY") if (v.familyCheckedOut) " • OUT" else " • POOL" else ""}")},onClick={selectedVehicleId=v.id;viewModel.setActiveVehicle(character.id,v.id);vehicleMenu=false}) }
                }
            }
            if(selectedVehicle!=null && selectedSpec!=null){
                val heavy = selectedVehicle.upgrades.any { it.equals("Heavy Chassis",true) }
                val armored = selectedVehicle.upgrades.any { it.equals("Armored Chassis",true) }
                val combatPlow = selectedVehicle.upgrades.any { it.equals("Combat Plow",true) }
                val maxSdp = selectedSpec.sdp + if(heavy)20 else 0
                val seatLimit = selectedSpec.seats + selectedVehicle.upgrades.count { it.equals("Seating Upgrade", true) } * 2
                val occupants = viewModel.vehicleOccupants(character.id, selectedVehicle.id)
                val vehicleRunning = viewModel.isVehicleRunning(character.id, selectedVehicle.id)
                Text(gtr("%1s • SDP %2s/%3s • SP %4s • MOVE %5s • Seats %6s/%7s • %8s", selectedSpec.domain, selectedVehicle.currentSdp, maxSdp, if(armored)13 else 0, selectedSpec.combatMove, occupants.size, seatLimit, if(vehicleRunning) "RUNNING" else "STOPPED"),color=White,fontSize=12.sp)
                if(selectedVehicle.upgrades.isNotEmpty()) Text(gtr("Upgrades: %1s", selectedVehicle.upgrades.joinToString()),color=Muted,fontSize=10.sp)
                if (selectedVehicle.source == "FAMILY") {
                    val pendingThis = liveCharacter.pendingFamilyVehicleId == selectedVehicle.id
                    Text(
                        when {
                            selectedVehicle.destroyedUntil > 0L -> gtr("FAMILY REPAIR • %1s day(s) remaining", selectedVehicle.destroyedUntil)
                            selectedVehicle.familyCheckedOut || liveCharacter.roleRank >= 10 -> "FAMILY MOTORPOOL • checked out / available"
                            pendingThis -> "FAMILY MOTORPOOL • swap requested for next morning"
                            else -> "FAMILY MOTORPOOL • currently in pool"
                        }, color = if (selectedVehicle.familyCheckedOut || liveCharacter.roleRank >= 10) Muted else Red, fontSize = 10.sp
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (selectedVehicle.currentSdp <= 0 && selectedVehicle.destroyedUntil <= 0L) {
                            OutlinedButton(onClick={ actionResult=viewModel.sendDestroyedFamilyVehicleForRepair(character.id, selectedVehicle.id) }, modifier=Modifier.weight(1f)) {
                                Text(gtr("FAMILY REPAIR • 500eb / 1 WEEK"),color=Red,fontSize=8.sp)
                            }
                        } else if (selectedVehicle.destroyedUntil > 0L) {
                            OutlinedButton(onClick={ actionResult=viewModel.advanceFamilyRepairDay(character.id) }, modifier=Modifier.weight(1f)) {
                                Text(gtr("ADVANCE 1 REPAIR DAY"),color=Muted,fontSize=8.sp)
                            }
                        } else if (liveCharacter.roleRank < 10 && !selectedVehicle.familyCheckedOut) {
                            OutlinedButton(onClick={ actionResult=viewModel.requestFamilyVehicle(character.id, selectedVehicle.id) }, modifier=Modifier.weight(1f)) {
                                Text(if(pendingThis) "SWAP PENDING" else "REQUEST FAMILY VEHICLE",color=Red,fontSize=8.sp)
                            }
                        }
                        if (liveCharacter.pendingFamilyVehicleId != null && liveCharacter.roleRank < 10) {
                            OutlinedButton(onClick={ actionResult=viewModel.completeFamilyVehicleSwapNextMorning(character.id) }, modifier=Modifier.weight(1f)) {
                                Text(gtr("NEXT MORNING • COMPLETE SWAP"),color=Muted,fontSize=8.sp)
                            }
                        }
                    }
                }
                if (occupants.isNotEmpty()) {
                    Text(gtr("Occupants: ") + occupants.joinToString { o -> "${o.handle.ifBlank { o.name }} (${viewModel.vehicleSeatRole(o.id, character.id, selectedVehicle.id) ?: "passenger"})" }, color=Muted, fontSize=10.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick={
                        if (attackActionUsed > 0) actionResult = "Action already used."
                        else {
                            val result = viewModel.assignVehicleSeat(character.id, selectedVehicle.id, character.id, "driver", seatLimit)
                            actionResult = if (result == "OK") "Entered vehicle as Driver • Action used." else result
                            if (result == "OK") { attackActionCap = 1; attackActionUsed = 1 }
                        }
                    }, enabled=attackActionUsed==0 && viewModel.vehicleSeatInfo(character.id)==null, modifier=Modifier.weight(1f)){Text(gtr("ENTER → DRIVER"),color=Red,fontSize=8.sp)}
                    OutlinedButton(onClick={
                        val t=currentTargetForActions
                        actionResult=if(t==null) "Select a combat target first." else viewModel.assignVehicleSeat(character.id, selectedVehicle.id, t.id, "passenger", seatLimit)
                    }, modifier=Modifier.weight(1f)){Text(gtr("GM SEAT TARGET"),color=Red,fontSize=8.sp)}
                    OutlinedButton(onClick={
                        if (character.combatEffects.containsKey("no_move_this_turn")) actionResult="Cannot exit: Move Action is unavailable this Turn."
                        else if (viewModel.vehicleSeatInfo(character.id)==null) actionResult="Not currently in a vehicle."
                        else { viewModel.leaveVehicle(character.id); viewModel.recordCombatMovement(character.id, 1); actionResult="Exited vehicle • no Action, but movement used." }
                    }, modifier=Modifier.weight(1f)){Text(gtr("SELF EXIT"),color=Muted,fontSize=8.sp)}
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                OutlinedButton(onClick={
                    if (attackActionUsed > 0) actionResult="Action already used."
                    else {
                        val starting = !vehicleRunning
                        actionResult=viewModel.setVehicleRunning(character.id, selectedVehicle.id, starting) + " • Action used."
                        if (starting) { viewModel.moveCombatantToTop(character.id); actionResult += " • moved to top of Initiative Queue." }
                        attackActionCap=1; attackActionUsed=1
                    }
                }, enabled=attackActionUsed==0, modifier=Modifier.weight(1f)) { Text(if(vehicleRunning) "STOP VEHICLE • ACTION" else "START VEHICLE • ACTION", color=Red, fontSize=9.sp) }
                    RuleInfoButton("vehicle.start_combat")
                }
                if (selectedVehicle.upgrades.any { it.equals("Vehicle Heavy Weapon Mount", true) }) {
                    val gunner=viewModel.heavyWeaponGunner(character.id,selectedVehicle.id)
                    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF171717)),modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)) {
                        Column(Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(gtr("VEHICLE HEAVY WEAPON MOUNT"),color=Red,fontWeight=FontWeight.Bold,fontSize=10.sp)
                            Text(gunner?.let{gtr("Gunner: %1s", it.handle.ifBlank{it.name})} ?: "No gunner assigned • mount consumes one vehicle seat",color=Muted,fontSize=9.sp)
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick={
                                    val t=currentTargetForActions
                                    actionResult=if(t==null) "Select the passenger you want as gunner." else viewModel.assignVehicleSeat(character.id,selectedVehicle.id,t.id,"gunner",seatLimit)
                                },modifier=Modifier.weight(1f)){Text(gtr("ASSIGN TARGET AS GUNNER"),color=Red,fontSize=8.sp)}
                                OutlinedButton(onClick={
                                    actionResult=if(gunner==null) "Assign a gunner first." else gtr("HEAVY MOUNT READY • %1s uses their own Action to fire the installed two-handed ranged weapon; unlike driver-mounted weapons, the gunner may reload it while the vehicle is moving.", gunner.handle.ifBlank{gunner.name})
                                },modifier=Modifier.weight(1f)){Text(gtr("GUNNER ACTION"),color=Red,fontSize=8.sp)}
                            }
                        }
                    }
                }
                if (selectedVehicle.upgrades.any { it.equals("Seating Upgrade", true) } && occupants.isNotEmpty()) {
                    Text(gtr("EJECTOR SEATS"),color=Red,fontWeight=FontWeight.Bold,fontSize=10.sp)
                    occupants.forEach { o ->
                        OutlinedButton(onClick={actionResult=viewModel.ejectVehicleOccupant(character.id,selectedVehicle.id,o.id)},modifier=Modifier.fillMaxWidth().padding(top=3.dp)) {
                            Text(gtr("EJECT • %1s • 10m/yd UP", o.handle.ifBlank{o.name}),color=Muted,fontSize=8.sp)
                        }
                    }
                }
                if (selectedVehicle.upgrades.any { it.equals("Bulletproof Glass", true) }) {
                    val paneMax = selectedVehicle.upgrades.count { it.equals("Bulletproof Glass", true) }.coerceIn(0,2) * 15
                    Text(gtr("Bulletproof Glass • each window has independent %1s HP", paneMax), color=White,fontSize=10.sp)
                    occupants.forEach { o ->
                        val paneHp = viewModel.vehicleGlassHpForOccupant(character.id, selectedVehicle.id, o.id)
                        Text(gtr("↳ %1s window: %2s/%3s HP", o.handle.ifBlank { o.name }, paneHp, paneMax), color=Muted,fontSize=9.sp)
                    }
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("سلاح‌های نصب‌شده روی وسیله مسیر جداگانه دارند و فقط وقتی راننده‌ی آن وسیله باشی و Action آزاد داشته باشی روشن می‌شوند. دکمه‌های خاکستری یعنی Action این نوبت مصرف شده (یا «GM: free fire» خاموش است).", color = Muted, fontSize = 9.sp, lineHeight = 14.sp)
                }
                val onboardKinds=listOf("Onboard Flamethrower","Onboard Machine gun","Onboard Rocket Pod","Onboard Melee Weapon")
                onboardKinds.forEach { kind ->
                    val count=selectedVehicle.upgrades.count { it.equals(kind,true) }
                    repeat(count) { mountIndex ->
                        val ammo=viewModel.mountedWeaponAmmo(character.id,selectedVehicle.id,kind,mountIndex)
                        val ammoText=if(kind.equals("Onboard Melee Weapon",true)) "" else gtr(" • Ammo %1s", ammo)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
                            Text("$kind #${mountIndex+1}$ammoText",color=White,fontSize=9.sp,modifier=Modifier.weight(1.4f))
                            OutlinedButton(onClick={
                                if(attackActionUsed>0 && !gmOverrideAction) actionResult="Action already used."
                                else { actionResult=viewModel.fireMountedWeapon(character.id,selectedVehicle.id,kind,mountIndex);attackActionCap=1;attackActionUsed=1 }
                            },enabled=vehicleRunning && (attackActionUsed==0 || gmOverrideAction) && viewModel.vehicleSeatRole(character.id,character.id,selectedVehicle.id)=="driver",modifier=Modifier.weight(0.8f)){Text(gtr("FIRE"),color=Red,fontSize=8.sp)}
                            if(!kind.equals("Onboard Melee Weapon",true)) OutlinedButton(onClick={actionResult=viewModel.reloadMountedWeapon(character.id,selectedVehicle.id,kind,mountIndex,vehicleRunning)},enabled=!vehicleRunning,modifier=Modifier.weight(0.8f)){Text(gtr("RELOAD"),color=Muted,fontSize=8.sp)}
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    OutlinedButton(onClick={
                        if(attackActionUsed>0 && !gmOverrideAction) actionResult="Action already used."
                        else rules {
                            val msg=viewModel.resolveBasicDriving(character.id,character.id,selectedVehicle.id)
                            actionResult=msg
                            if(msg.contains("Action used")){ attackActionCap=1;attackActionUsed=1 }
                        }
                    },enabled=vehicleRunning && (attackActionUsed==0 || gmOverrideAction) && viewModel.vehicleSeatRole(character.id, character.id, selectedVehicle.id)=="driver",modifier=Modifier.weight(1f)){Text(gtr("BASIC DRIVE"),color=Muted,fontSize=8.sp)}
                    OutlinedButton(onClick={
                        if(attackActionUsed>0 && !gmOverrideAction) actionResult="Action already used."
                        else if(character.combatEffects.containsKey("no_move_this_turn")) actionResult="Move Action unavailable this Turn."
                        else rules {
                            actionResult=viewModel.resolveVehicleManeuver(character.id,character.id,selectedVehicle.id,maneuverDv)
                            attackActionCap=1;attackActionUsed=1
                            viewModel.addCombatEffect(character.id,"no_move_this_turn",-1)
                        }
                    },enabled=vehicleRunning && (attackActionUsed==0 || gmOverrideAction) && viewModel.vehicleSeatRole(character.id, character.id, selectedVehicle.id)=="driver",modifier=Modifier.weight(1f)){Text(gtr("MANEUVER DV%1s", maneuverDv),color=Red,fontSize=8.sp)}
                    OutlinedButton(onClick={maneuverDv=if(maneuverDv==13)17 else 13},modifier=Modifier.weight(0.65f)){Text("13/17",color=Muted,fontSize=8.sp)}
                }
                if(viewModel.vehicleLostControl(character.id,selectedVehicle.id)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text("LOST CONTROL • GM تمام حرکت این Turn را تعیین می‌کند. اگر برخوردی رخ داد آن را به‌صورت Ram حل کن.",color=Red,fontWeight=FontWeight.Bold,fontSize=10.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right) }
                    OutlinedButton(onClick={viewModel.clearVehicleLostControl(character.id,selectedVehicle.id);actionResult="Lost Control marker cleared after GM resolved movement."},modifier=Modifier.fillMaxWidth()){Text(gtr("GM: RESOLVE WITHOUT IMPACT / CLEAR"),color=Muted,fontSize=8.sp)}
                }
                val ramTargets = allCharacters.flatMap { owner ->
                    owner.ownedVehicles.filter { it.currentSdp > 0 && !(owner.id == character.id && it.id == selectedVehicle.id) }.map { v ->
                        VehicleTargetRef(owner.id, v.id, gtr("%1s • %2s • SDP %3s", owner.handle.ifBlank { owner.name }, v.customName.ifBlank { v.specName }, v.currentSdp))
                    }
                }
                val selectedRamTarget = ramTargets.firstOrNull { "${it.ownerId}|${it.vehicleId}" == ramTargetKey }
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick={ramTargetMenu=true}, modifier=Modifier.fillMaxWidth()) { Text(selectedRamTarget?.label ?: "SELECT RAM TARGET VEHICLE", color=Red, fontSize=9.sp) }
                    DropdownMenu(expanded=ramTargetMenu,onDismissRequest={ramTargetMenu=false}) {
                        ramTargets.forEach { ref -> DropdownMenuItem(text={Text(gtr(ref.label))},onClick={ramTargetKey="${ref.ownerId}|${ref.vehicleId}";ramTargetMenu=false}) }
                    }
                }
                val hasNos = selectedVehicle.upgrades.any { it.equals("NOS", true) }
                if (hasNos) {
                    val nosRemaining=viewModel.vehicleNosRemaining(character.id,selectedVehicle.id)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick={
                            if(attackActionUsed>0 && !gmOverrideAction) actionResult="Action already used."
                            else {
                                val msg=viewModel.useVehicleNos(character.id,selectedVehicle.id);actionResult=msg
                                if(msg.startsWith("NOS USED")){ramNosBoosted=true;attackActionCap=1;attackActionUsed=1}
                            }
                        },enabled=vehicleRunning && (attackActionUsed==0 || gmOverrideAction) && nosRemaining>0 && viewModel.vehicleSeatRole(character.id,character.id,selectedVehicle.id)=="driver",modifier=Modifier.weight(2f)){Text(gtr("USE NOS • ACTION • %1s LEFT", nosRemaining),color=Red,fontSize=8.sp)}
                        OutlinedButton(onClick={viewModel.resetVehicleNosDaily(character.id,selectedVehicle.id);ramNosBoosted=false;actionResult="NOS daily tanks reset."},modifier=Modifier.weight(1f)){Text(gtr("NEW DAY"),color=Muted,fontSize=8.sp)}
                    }
                    if(ramNosBoosted) Text(gtr("NOS BOOST ACTIVE THIS TURN • next front Ram is 8d6 via Combat Plow interaction."),color=Muted,fontSize=9.sp)
                }
                currentTargetForActions?.let { pedestrian ->
                    if (viewModel.vehicleSeatInfo(pedestrian.id) == null) {
                        Row(Modifier.fillMaxWidth().padding(bottom=6.dp), verticalAlignment=Alignment.CenterVertically) {
                        OutlinedButton(onClick={
                            val dodge = CombatRules.defenderEvasionTotal(pedestrian, CombatRules.rollD10())
                            if (dodge > 13) actionResult=gtr("PEDESTRIAN DODGED RAM: Evasion %1s > DV13. They may choose to end on top of the vehicle.", dodge)
                            else {
                                val expr=if(hasNos && ramNosBoosted) "8d6" else "6d6"
                                val d=CombatRules.rollDamage(expr)
                                actionResult=viewModel.resolveVehicleRamPedestrian(character.id, selectedVehicle.id, pedestrian.id, d.total) + gtr(" • dodge %1s vs DV13 • roll %2s=%3s", dodge, d.dice.joinToString("+"), d.total)
                                ramNosBoosted=false
                            }
                        }, enabled=selectedVehicle.currentSdp>0 && vehicleRunning && viewModel.vehicleSeatRole(character.id, character.id, selectedVehicle.id)=="driver", modifier=Modifier.weight(1f)) {
                            Text(gtr("RAM CURRENT TARGET ON FOOT • DV13 Evasion"), color=Red, fontSize=9.sp)
                        }
                        RuleInfoButton("vehicle.ram_pedestrian")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    OutlinedButton(onClick={
                        if (selectedRamTarget == null) actionResult="Select a target vehicle first."
                        else {
                            val d = CombatRules.rollDamage(coverDamageExpr.ifBlank { "5d6" })
                            val dealt = viewModel.damageVehicleAttack(selectedRamTarget.ownerId, selectedRamTarget.vehicleId, d.total, weakPoint = true)
                            actionResult = gtr("TARGET VEHICLE WEAK POINT: %1s raw • penetrating damage after target SP doubled → %2s SDP. Use only after a successful Aimed Shot at -8 (moving target weak point also requires DV13).", d.total, dealt)
                        }
                    }, enabled=selectedVehicle.currentSdp>0 && selectedRamTarget!=null, modifier=Modifier.weight(1f)){Text(gtr("WEAK POINT"),color=Red,fontSize=9.sp)}
                    OutlinedButton(onClick={
                        if (selectedRamTarget == null) actionResult="Select a target vehicle first."
                        else rules {
                            val expr=if(hasNos && ramNosBoosted) "8d6" else "6d6"
                            val d=CombatRules.rollDamage(expr)
                            actionResult=if(viewModel.vehicleLostControl(character.id,selectedVehicle.id))
                                viewModel.resolveLostControlCrash(character.id,selectedVehicle.id,selectedRamTarget.ownerId,selectedRamTarget.vehicleId)
                            else viewModel.resolveVehicleRam(character.id,selectedVehicle.id,selectedRamTarget.ownerId,selectedRamTarget.vehicleId,d.total) + gtr(" • roll %1s=%2s", d.dice.joinToString("+"), d.total)
                            ramNosBoosted=false
                        }
                    },enabled=selectedVehicle.currentSdp>0 && vehicleRunning && viewModel.vehicleSeatRole(character.id, character.id, selectedVehicle.id)=="driver",modifier=Modifier.weight(2f)){Text(if(viewModel.vehicleLostControl(character.id,selectedVehicle.id)) "LOST CONTROL CRASH" else gtr("RAM TARGET • %1s", if(hasNos&&ramNosBoosted)"8d6" else "6d6"),color=Red,fontSize=9.sp)}
                    OutlinedButton(onClick={
                        rules {
                            val dv=if(selectedVehicle.currentSdp<=0)17 else if(selectedVehicle.currentSdp <= maxSdp/2)13 else 9
                            actionResult=viewModel.repairVehicleToFull(character.id,selectedVehicle.id,dv)
                        }
                    },modifier=Modifier.weight(1f)){Text(gtr("REPAIR"),color=Muted,fontSize=9.sp)}
                }
                OutlinedButton(onClick={
                    if (selectedRamTarget == null) actionResult="Select a target vehicle first."
                    else {
                        val d=CombatRules.rollDamage(coverDamageExpr.ifBlank { "5d6" })
                        val dealt=viewModel.damageVehicleAttack(selectedRamTarget.ownerId,selectedRamTarget.vehicleId,d.total,false)
                        actionResult=gtr("TARGET VEHICLE HIT: %1s=%2s raw • target SP applied • %3s SDP damage. Use after resolving the ranged/melee hit normally.", d.dice.joinToString("+"), d.total, dealt)
                    }
                }, enabled=selectedRamTarget!=null, modifier=Modifier.fillMaxWidth()) { Text(gtr("APPLY HIT TO TARGET VEHICLE"),color=Muted,fontSize=9.sp) }
                if(selectedVehicle.currentSdp<=0) Text(gtr("DESTROYED • cannot move and no longer counts as cover until repaired."),color=Red,fontWeight=FontWeight.Bold,fontSize=11.sp)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text("Control Skill از نوع Vehicle می‌آید؛ Nomad Moto به Checkهای Drive/Pilot و Vehicle Tech اضافه می‌شود. Armored Chassis برابر SP13 می‌دهد و Heavy Chassis مقدار 20 SDP اضافه می‌کند. Repair: Minor با DV9 و 3 ساعت، Major با DV13 و 1 روز، Destroyed با DV17 و 1 هفته. دسته‌بندی ≤50% SDP به‌عنوان Major در این UI فقط workflow کمکی GM است. Aimed Shot به Weak Point جریمهٔ -8 دارد و Damage عبوری از SP را دوبرابر می‌کند.",color=Muted,fontSize=10.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right) }
            }
        }

        }

    }

        // ══════════════════════════════════════════════════
        // نوار چسبان پایین — خلاصهٔ زنده + شلیک + نوبت بعدی
        // همیشه در دسترس است، هر جای صفحه که باشی.
        // ══════════════════════════════════════════════════
        Surface(color = Color(0xFF101010), shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                HorizontalDivider(color = Red.copy(alpha = .5f))
                Spacer(Modifier.height(7.dp))
                // خلاصهٔ زنده: چه کسی، به چه کسی، با چه چیزی، چند متر
                // سلاح را اینجا دوباره از همان state بالا می‌گیریم؛
                // workflowWeapon داخل بلوک پایین‌تر تعریف شده و از اینجا دیده نمی‌شود.
                val barWeapon = character.weapons.firstOrNull { it.id == selectedAttackWeaponId && it.isEquipped }
                    ?: character.weapons.firstOrNull { it.isEquipped }
                run {
                    val tgtName = availableTargets.firstOrNull { it.id == targetId }?.name ?: gtr("no target")
                    val wpnName = barWeapon?.name ?: gtr("no weapon")
                    val modeTxt = when (selectedAttackMode) {
                        "Autofire" -> gtr("Autofire")
                        "Suppressive" -> gtr("Suppressive Fire")
                        else -> gtr("Single")
                    }
                    val distTxt = distanceText.ifBlank { "0" }
                    com.cyberpunk.gmtool.ui.components.FaText(
                        "${character.name} ← $tgtName • $wpnName • $modeTxt • ${distTxt}m",
                        color = Muted, fontSize = 10.sp, maxLines = 1, modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(5.dp))
                // وضعیت Action + کلید عبور GM.
                // دکمه‌ی FIRE دیگر خاکستری نمی‌شود؛ فقط رنگش هشدار می‌دهد که
                // Action این نوبت تمام شده. تصمیم با GM است، نه با برنامه.
                run {
                    val capNow = attackActionCap
                    val spent = capNow != null && attackActionUsed >= capNow
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        com.cyberpunk.gmtool.ui.components.FaText(
                            if (capNow == null) gtr("Action: free")
                            else gtr("Action %1s/%2s", attackActionUsed, capNow),
                            color = if (spent && !gmOverrideAction) Color(0xFFFFB300) else Muted,
                            fontSize = 10.sp, modifier = Modifier.weight(1f)
                        )
                        Text(gtr("GM: free fire"), color = if (gmOverrideAction) Red else Muted, fontSize = 10.sp)
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = gmOverrideAction,
                            onCheckedChange = { gmOverrideAction = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Red, checkedTrackColor = Red.copy(alpha = .35f)),
                            modifier = Modifier.scale(0.75f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val capNow = attackActionCap
                    val spent = capNow != null && attackActionUsed >= capNow
                    Button(
                        onClick = {
                            barWeapon?.let { w ->
                                if (selectedAttackMode == "Suppressive") performSuppressiveFire(w)
                                else performAttack(w, selectedAttackMode == "Autofire")
                            }
                        },
                        enabled = barWeapon != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (spent && !gmOverrideAction) Color(0xFF6D2320) else Red,
                            disabledContainerColor = Color(0xFF3A3A3A)
                        ),
                        shape = CutCornerShape(9.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            if (selectedAttackMode == "Suppressive") gtr("FIRE • SUPPRESSIVE")
                            else if (barWeapon?.type.equals("Athletics", true) || barWeapon?.modes?.contains("Explosive", true) == true) gtr("THROW")
                            else gtr("FIRE"),
                            color = if (barWeapon == null) Muted else Black,
                            fontWeight = FontWeight.Black, fontSize = 17.sp)
                    }
                }
            }
        }


    combatHelp?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { combatHelp = null },
            containerColor = CardBg,
            title = { Text(gtr(title), color = Red, fontWeight = FontWeight.Bold) },
            text = { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Text(gtr(body), color = White, fontSize = 14.sp, lineHeight = 23.sp, textAlign = TextAlign.Right) } },
            confirmButton = { TextButton(onClick = { combatHelp = null }) { Text("فهمیدم", color = Red, fontWeight = FontWeight.Bold) } }
        )
    }

    attackResult?.let { result ->
        Dialog(onDismissRequest = { attackResult = null }) {
            Card(colors = CardDefaults.cardColors(containerColor = Black), shape = CutCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(12.dp))) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.padding(22.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Text(gtr(result.weaponName), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(gtr("Roll: %1s", result.rollText), color = Muted)
                    Text(gtr("ATTACK TOTAL %1s", result.attackTotal), color = Red, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(gtr(result.defenseText), color = White)
                    if (result.targetText.isNotBlank()) Text(gtr("Target: %1s", result.targetText), color = Muted)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            result.jammed -> "POOR QUALITY MALFUNCTION — Action لازم است تا Unjam شود."
                            result.hit == true -> "HIT"
                            result.hit == false -> "MISS"
                            else -> "ROLL ONLY"
                        },
                        color = if (result.hit == true) White else Red, fontWeight = FontWeight.Bold
                    )
                    if (result.hit == true && (result.rawDamage != null || result.damageText.isNotBlank())) {
                        Spacer(Modifier.height(12.dp))
                        Text(gtr("DAMAGE RESOLUTION"), color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        if (result.rawDamage != null) {
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DamageStepCard("RAW", result.rawDamage.toString(), Modifier.weight(1f))
                                DamageStepCard("ARMOR", result.armorUsed?.toString() ?: "—", Modifier.weight(1f))
                                DamageStepCard("HP", result.hpDamage?.let { "-$it" } ?: "—", Modifier.weight(1f))
                                DamageStepCard("SP", result.armorAblated?.let { "-$it" } ?: "—", Modifier.weight(1f))
                            }
                        }
                        if (result.resolutionLabel.isNotBlank()) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)), modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
                                Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                    Text(gtr(result.resolutionLabel), color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (result.resolutionNote.isNotBlank()) Text(gtr(result.resolutionNote), color = White, fontSize = 11.sp, textAlign = TextAlign.Right)
                                }
                            }
                        }
                        result.criticalName?.let { injury ->
                            Card(colors = CardDefaults.cardColors(containerColor = Red.copy(alpha = 0.12f)), modifier = Modifier.fillMaxWidth().padding(top = 7.dp).border(1.dp, Red.copy(alpha = 0.45f), CutCornerShape(6.dp))) {
                                Text(gtr("CRITICAL INJURY • %1s", injury), color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(10.dp).fillMaxWidth(), textAlign = TextAlign.Right)
                            }
                        }
                    }
                    if (result.damageText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("جزئیات", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(gtr(result.damageText), color = White, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { attackResult = null }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) {
                        Text(gtr("CLOSE"), color = Black, fontWeight = FontWeight.Bold)
                    }
                }
                }
            }
        }
    }

    reloadWeaponId?.let { wid ->
        val w = character.weapons.firstOrNull { it.id == wid }
        if (w != null) {
            Dialog(onDismissRequest = { reloadWeaponId = null }) {
                Card(colors = CardDefaults.cardColors(containerColor = Black), modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(12.dp))) {
                    Column(Modifier.padding(18.dp).fillMaxWidth()) {
                        Text(gtr("RELOAD — %1s", w.name), color=Red, fontWeight=FontWeight.Bold)
                        Text(if (CombatRules.reloadActionCost(w) > 1) "Choose compatible ammunition. This Exotic reload requires 2 Actions across turns." else "Choose one compatible ammunition type. Reload consumes an Action in Core.", color=Muted, fontSize=11.sp)
                        val ammo = character.inventory.filter { it.category.equals("Ammo",true) && it.quantity>0 }.filter { item ->
                            val n=item.name.lowercase(); val t=w.ammoType.lowercase()
                            val compatible = when { t.contains("grenade")->n.contains("grenade"); t.contains("rocket")->n.contains("rocket"); t.contains("shotgun")->n.contains("shotgun")||n.contains("shell"); t.contains("rifle")->n.contains("rifle")||n.contains("ammo"); t.contains("pistol")->n.contains("pistol")||n.contains("ammo"); t.contains("arrow")->n.contains("arrow"); else->false }
                            compatible && !(w.name.equals("Dartgun", true) && n.contains("basic"))
                        }
                        val grenadeWeapons = if (w.ammoType.contains("Grenade", true)) character.weapons.filter { it.id != w.id && it.name.contains("Grenade", true) && it.magazineSize == 0 } else emptyList()
                        if (ammo.isEmpty() && grenadeWeapons.isEmpty()) Text(gtr("No compatible ammunition in inventory."), color=Muted, modifier=Modifier.padding(vertical=12.dp))
                        ammo.forEach { a -> OutlinedButton(onClick={
                            val cost=CombatRules.reloadActionCost(w); val stageKey="reload_stage_${w.id}"
                            if(cost>1 && !character.combatEffects.containsKey(stageKey)) { viewModel.addCombatEffect(character.id,stageKey,-1); actionResult=gtr("RELOAD 1/2: %1s • first Action spent; ammo choice will complete reload on a later Turn.", w.name) }
                            else { viewModel.reloadWeaponWithAmmo(character.id,w.id,a.name); if(cost>1)viewModel.clearCombatEffect(character.id,stageKey); actionResult=gtr("RELOAD%1s: %2s loaded with %3s.", if(cost>1) " 2/2" else "", w.name, a.name) }
                            attackActionCap=1; attackActionUsed=1; reloadWeaponId=null
                        }, modifier=Modifier.fillMaxWidth().padding(top=6.dp)) { Text("${a.name} ×${a.quantity}", color=White) } }
                        grenadeWeapons.forEach { g -> OutlinedButton(onClick={ viewModel.loadGrenadeWeaponIntoLauncher(character.id,w.id,g.id); attackActionCap=1; attackActionUsed=1; reloadWeaponId=null }, modifier=Modifier.fillMaxWidth().padding(top=6.dp)) { Text(gtr("Load %1s", g.name), color=White) } }
                    }
                }
            }
        }
    }



    if (showBlastDialog) {
        Dialog(onDismissRequest = { showBlastDialog = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Black), modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(12.dp))) {
                Column(Modifier.padding(18.dp).fillMaxWidth()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                            Text("محدوده انفجار / BLAST AREA — 10×10 m/yd", color = Red, fontWeight = FontWeight.Bold, fontSize = 19.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                            Text("افرادی را که واقعاً داخل محدوده‌ی انفجار هستند انتخاب کن. آسیب فقط یک‌بار رول می‌شود و برای هر هدف با زره و وضعیت خودش محاسبه می‌شود.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    val blastChoices = (if (encounterCharacters.isNotEmpty()) encounterCharacters else allCharacters).filter { !it.isDead }
                    blastChoices.forEach { c ->
                        Row(Modifier.fillMaxWidth().clickable { blastTargetIds = if (c.id in blastTargetIds) blastTargetIds - c.id else blastTargetIds + c.id }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = c.id in blastTargetIds, onCheckedChange = { blastTargetIds = if (c.id in blastTargetIds) blastTargetIds - c.id else blastTargetIds + c.id })
                            Text(c.handle.ifBlank { c.name }, color = White, modifier = Modifier.weight(1f))
                            Text(if (c.stats.ref >= 8) gtr("REF %1s • can dodge", c.stats.ref) else gtr("REF %1s", c.stats.ref), color = Muted, fontSize = 11.sp)
                        }
                        if (c.id in blastTargetIds) {
                            Row(Modifier.fillMaxWidth().padding(start=28.dp), verticalAlignment=Alignment.CenterVertically) {
                                Checkbox(checked=c.id in blastCoveredIds, onCheckedChange={ blastCoveredIds = if(c.id in blastCoveredIds) blastCoveredIds-c.id else blastCoveredIds+c.id })
                                Text("پشت Coverای است که از این انفجار سالم می‌ماند", color=Muted, fontSize=10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { showBlastDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) { Text(gtr("DONE"), color = Black) }
                }
            }
        }
    }

    }
}





@Composable
private fun ArmorCard(title: String, sp: Int, modifier: Modifier = Modifier, onDec: () -> Unit, onInc: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = CutCornerShape(12.dp),
        modifier = modifier.border(1.dp, Muted, CutCornerShape(12.dp))) {
        // گزارش تست ۳.۱: کارت‌های HEAD SP / BODY SP هم مثل HP باید تمام‌عرض باشند تا عدد وسط بایستد.
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(gtr(title), color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (sp > 0) onDec() }) { Text("➖", color = Red) }
                Text("$sp", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onInc) { Text("➕", color = Red) }
            }
        }
    }
}

@Composable
private fun RoundButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = if (label == "+") Red else Color(0xFF333333)),
        shape = CutCornerShape(8.dp), modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) {
        Text(gtr(label), color = if (label == "+") Black else White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

/** تیتر شماره‌دار بخش‌های تب COMBAT (۱ تا ۶) — ترتیب ثابت صفحه. بخش‌های بالای صفحه شماره ندارند. */
@Composable
private fun CombatSectionLabel(number: Int, title: String) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Text("$number • $title", color = Red, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CombatSectionHeader(
    title: String,
    help: String,
    expanded: Boolean,
    summary: String = "",
    onToggle: () -> Unit,
    onHelp: (Pair<String, String>) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = CutCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .border(1.dp, if (expanded) Red.copy(alpha = .8f) else Muted.copy(alpha = .25f), CutCornerShape(10.dp))
            .clickable { onToggle() }
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (expanded) "▾" else "▸", color = Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(gtr(title), color = if (expanded) Red else White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (summary.isNotBlank()) Text(gtr(summary), color = Muted, fontSize = 10.sp, maxLines = 1)
            }
            val centralKey = when {
                title.contains("CRITICAL INJURIES", true) -> "combat.critical_injury"
                title.contains("COVER / SHIELD", true) -> "combat.cover"
                else -> null
            }
            val centralHelp = centralKey?.let { RuleReferenceData.get(it) }
            IconButton(onClick = { onHelp((centralHelp?.title ?: title) to (centralHelp?.short ?: help)) }, modifier = Modifier.size(32.dp)) {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Red.copy(alpha = .14f), border = androidx.compose.foundation.BorderStroke(1.dp, Red.copy(alpha = .65f))) {
                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { Text("i", color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
            }
        }
    }
}
