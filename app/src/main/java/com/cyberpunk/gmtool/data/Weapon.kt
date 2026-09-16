package com.cyberpunk.gmtool.data

import java.io.Serializable

// مدل سلاح (مقادیر val تا با کپی مدیریت شوند و با Gson سازگار بمانند)
data class Weapon(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",        // مثلاً "Assault Rifle" یا "Light Melee"
    val type: String = "",            // مهارت مرتبط: "Shoulder Arms" / "Handgun" / "Melee" / ...
    val damage: String = "",          // مثلاً "5d6" یا "3d6"
    val rof: Int = 1,                 // Rate of Fire
    val handsRequired: Int = 1,
    val concealable: Boolean = false,
    val cost: Int = 0,
    val magazineSize: Int = 0,        // خشاب (برای سلاح سرد 0)
    val currentAmmo: Int = 0,         // فشنگ فعلی داخل سلاح
    val isEquipped: Boolean = false,
    val ammoType: String = "",
    val loadedAmmoName: String = "Basic", // exact inventory ammo loaded; enables Core special-ammo resolution
    val modes: String = "",
    val autofireMultiplier: Int = 0,
    val rangeSingle: List<Pair<String, Int>> = emptyList(),
    val rangeAuto: List<Pair<String, Int>> = emptyList(),
    val quality: String = "Standard",
    // Installed Core weapon attachments. Non-Exotic ranged weapons have 3 attachment slots.
    val attachments: List<String> = emptyList(),
    // Battleglove only: externally installed Cyberarm/Cyberlimb options (no Humanity Loss because not implanted).
    val cyberOptions: List<String> = emptyList()
) : Serializable
