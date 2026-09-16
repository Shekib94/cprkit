package com.cyberpunk.gmtool.data



import kotlin.math.ceil

object StreetratData {
    // ترتیب: INT, REF, DEX, TECH, COOL, WILL, LUCK, MOVE, BODY, EMP
    // (اعداد رسمی از کتاب Core Cyberpunk RED — جداول Streetrats)

    private val rockerboyStats = listOf(
        Stats(7, 6, 6, 5, 6, 8, 7, 7, 3, 8),
        Stats(3, 7, 7, 7, 7, 6, 7, 7, 5, 8),
        Stats(4, 5, 7, 7, 6, 6, 7, 7, 5, 8),
        Stats(4, 5, 7, 7, 6, 8, 7, 6, 3, 8),
        Stats(3, 7, 7, 7, 6, 8, 6, 5, 4, 7),
        Stats(5, 6, 7, 5, 7, 8, 5, 7, 3, 7),
        Stats(5, 6, 6, 7, 7, 8, 7, 6, 3, 6),
        Stats(5, 7, 7, 5, 6, 6, 6, 6, 4, 8),
        Stats(3, 5, 5, 6, 7, 8, 7, 5, 5, 7),
        Stats(4, 5, 6, 5, 8, 8, 7, 6, 4, 7)
    )

    private val soloStats = listOf(
        Stats(6, 7, 7, 3, 8, 6, 5, 5, 6, 5),
        Stats(7, 8, 6, 3, 6, 6, 7, 5, 6, 6),
        Stats(5, 8, 7, 4, 7, 7, 6, 7, 8, 5),
        Stats(5, 8, 6, 4, 6, 7, 6, 5, 7, 6),
        Stats(6, 6, 7, 5, 7, 6, 7, 6, 8, 4),
        Stats(7, 7, 6, 5, 7, 6, 6, 7, 7, 5),
        Stats(7, 7, 6, 5, 6, 7, 7, 6, 6, 6),
        Stats(7, 8, 7, 5, 6, 6, 5, 6, 8, 4),
        Stats(7, 7, 6, 4, 6, 6, 6, 5, 6, 5),
        Stats(6, 6, 8, 5, 6, 6, 5, 6, 6, 5)
    )

    private val netrunnerStats = listOf(
        Stats(5, 8, 7, 7, 7, 4, 8, 7, 7, 4),
        Stats(5, 6, 7, 5, 8, 3, 8, 7, 5, 5),
        Stats(5, 6, 8, 6, 6, 4, 7, 6, 7, 4),
        Stats(5, 7, 7, 7, 7, 5, 8, 6, 5, 5),
        Stats(5, 8, 8, 5, 7, 3, 7, 5, 5, 6),
        Stats(6, 6, 6, 7, 8, 4, 7, 7, 6, 6),
        Stats(6, 6, 6, 7, 6, 5, 7, 7, 7, 6),
        Stats(5, 7, 8, 6, 8, 4, 8, 5, 7, 4),
        Stats(7, 6, 7, 7, 6, 3, 6, 5, 6, 5),
        Stats(7, 8, 6, 6, 6, 4, 7, 7, 5, 6)
    )

    private val execStats = listOf(
        Stats(8, 5, 5, 3, 8, 6, 6, 5, 5, 7),
        Stats(8, 6, 6, 4, 7, 6, 7, 7, 5, 7),
        Stats(8, 7, 6, 3, 8, 6, 7, 6, 4, 5),
        Stats(8, 5, 7, 5, 6, 5, 6, 5, 5, 7),
        Stats(7, 7, 6, 5, 8, 5, 7, 7, 5, 6),
        Stats(5, 7, 7, 3, 6, 7, 6, 5, 5, 7),
        Stats(6, 6, 7, 5, 8, 7, 6, 7, 4, 6),
        Stats(6, 7, 7, 3, 7, 5, 7, 5, 5, 7),
        Stats(7, 6, 7, 5, 7, 5, 7, 6, 5, 5),
        Stats(7, 7, 5, 5, 8, 6, 6, 7, 4, 7)
    )

    private val fixerStats = listOf(
        Stats(8, 5, 7, 4, 6, 5, 8, 5, 5, 8),
        Stats(8, 5, 5, 5, 6, 7, 8, 7, 5, 7),
        Stats(6, 6, 6, 4, 5, 6, 8, 6, 3, 8),
        Stats(7, 7, 5, 5, 7, 6, 7, 7, 5, 8),
        Stats(8, 6, 6, 3, 6, 5, 8, 7, 5, 6),
        Stats(8, 7, 5, 5, 6, 7, 7, 5, 3, 6),
        Stats(8, 6, 6, 5, 6, 5, 6, 7, 5, 8),
        Stats(6, 6, 7, 4, 7, 6, 7, 7, 4, 7),
        Stats(8, 7, 7, 5, 5, 5, 7, 6, 5, 7),
        Stats(6, 5, 6, 5, 5, 6, 8, 6, 4, 7)
    )

    private val lawmanStats = listOf(
        Stats(5, 6, 7, 5, 7, 8, 5, 6, 5, 6),
        Stats(6, 6, 6, 5, 6, 8, 5, 7, 5, 5),
        Stats(5, 7, 7, 7, 6, 7, 5, 5, 7, 6),
        Stats(6, 6, 7, 6, 6, 8, 5, 7, 7, 6),
        Stats(6, 6, 7, 6, 7, 7, 6, 5, 5, 6),
        Stats(7, 6, 5, 5, 7, 8, 5, 6, 7, 4),
        Stats(7, 8, 7, 5, 6, 8, 7, 6, 5, 4),
        Stats(5, 6, 6, 5, 6, 8, 5, 7, 6, 4),
        Stats(7, 7, 5, 5, 7, 7, 6, 5, 5, 6),
        Stats(6, 6, 5, 6, 8, 7, 5, 7, 6, 6)
    )

    private val mediaStats = listOf(
        Stats(6, 6, 5, 5, 8, 7, 5, 7, 5, 7),
        Stats(8, 7, 7, 3, 6, 6, 6, 5, 6, 8),
        Stats(6, 7, 7, 5, 6, 8, 5, 5, 5, 7),
        Stats(6, 5, 7, 5, 6, 7, 5, 5, 6, 6),
        Stats(6, 6, 7, 4, 8, 7, 6, 7, 5, 8),
        Stats(7, 5, 5, 4, 8, 7, 6, 7, 5, 8),
        Stats(8, 5, 6, 3, 7, 6, 6, 5, 6, 7),
        Stats(6, 5, 6, 5, 6, 8, 6, 6, 7, 8),
        Stats(7, 7, 5, 4, 6, 7, 6, 5, 6, 7),
        Stats(7, 6, 6, 3, 7, 6, 7, 6, 7, 6)
    )

    private val medtechStats = listOf(
        Stats(7, 5, 6, 7, 5, 3, 8, 5, 5, 7),
        Stats(6, 7, 7, 7, 4, 4, 6, 7, 7, 7),
        Stats(6, 5, 5, 8, 5, 3, 8, 5, 7, 8),
        Stats(8, 7, 6, 8, 3, 5, 6, 6, 5, 7),
        Stats(6, 7, 5, 7, 5, 5, 8, 7, 6, 8),
        Stats(8, 5, 5, 8, 5, 5, 6, 6, 5, 6),
        Stats(8, 6, 5, 8, 5, 4, 8, 5, 7, 7),
        Stats(6, 5, 7, 7, 3, 5, 8, 5, 5, 8),
        Stats(6, 6, 7, 7, 5, 4, 6, 6, 5, 6),
        Stats(8, 7, 6, 6, 3, 4, 8, 7, 6, 7)
    )

    private val nomadStats = listOf(
        Stats(6, 6, 8, 3, 6, 7, 6, 6, 6, 4),
        Stats(5, 7, 6, 5, 8, 8, 8, 7, 5, 4),
        Stats(5, 8, 6, 3, 8, 7, 6, 5, 6, 5),
        Stats(5, 8, 7, 4, 8, 6, 7, 7, 7, 5),
        Stats(6, 6, 6, 3, 6, 7, 6, 7, 7, 4),
        Stats(7, 6, 8, 4, 6, 7, 6, 5, 6, 5),
        Stats(6, 7, 8, 4, 6, 6, 7, 5, 7, 5),
        Stats(5, 7, 8, 3, 8, 6, 7, 5, 5, 5),
        Stats(6, 7, 6, 4, 8, 6, 6, 6, 6, 6),
        Stats(5, 6, 7, 4, 7, 8, 7, 7, 7, 4)
    )

    private val techStats = listOf(
        Stats(6, 7, 7, 8, 4, 4, 5, 5, 7, 6),
        Stats(7, 6, 6, 7, 5, 3, 7, 7, 5, 5),
        Stats(8, 6, 5, 7, 5, 4, 7, 7, 5, 7),
        Stats(7, 8, 7, 8, 4, 4, 6, 5, 6, 7),
        Stats(6, 6, 7, 6, 4, 3, 7, 7, 6, 6),
        Stats(8, 6, 7, 8, 4, 4, 7, 6, 7, 6),
        Stats(8, 8, 7, 8, 5, 4, 6, 5, 6, 6),
        Stats(8, 8, 7, 8, 5, 4, 6, 5, 6, 6),
        Stats(6, 6, 7, 8, 3, 3, 5, 7, 7, 7),
        Stats(8, 8, 5, 6, 4, 4, 6, 5, 6, 6)
    )

    fun getStatsForRole(role: String, roll: Int): Stats {
        val index = (roll - 1).coerceIn(0, 9)
        return statTableForRole(role)[index]
    }

    // دسترسی به جدول کامل نقش (برای متد Edgerunners که ستون‌به‌ستون رول می‌زند)
    fun statTableForRole(role: String): List<Stats> = when (role.uppercase()) {
        "ROCKERBOY" -> rockerboyStats
        "SOLO" -> soloStats
        "NETRUNNER" -> netrunnerStats
        "EXEC" -> execStats
        "FIXER" -> fixerStats
        "LAWMAN" -> lawmanStats
        "MEDIA" -> mediaStats
        "MEDTECH" -> medtechStats
        "NOMAD" -> nomadStats
        "TECH" -> techStats
        else -> soloStats
    }

    // متد Edgerunners: برای هر کدام از ۱۰ استت جداگانه 1d10 می‌ریزیم و از ستون همان رول
    // در جدول نقش، مقدار آن استت را برمی‌داریم (آماری به‌هم‌ریخته اما در محدوده‌ی نقش)
    fun rollEdgerunnerStats(role: String): Stats {
        val table = statTableForRole(role)
        fun col(roll: Int, pick: (Stats) -> Int): Int = pick(table[roll - 1])
        // هر استت یک d10 مستقل است. از DiceSource می‌آید تا در حالت «تاس دستی»
        // GM همه‌ی ده تاس را یک‌جا وارد کند (یک درخواست 10d10، نه ده دیالوگ).
        val rolls = DiceSource.roll(10, 10, "ساخت شخصیت — ۱۰ استت (10d10)")
        return Stats(
            int = col(rolls[0]) { it.int },
            ref = col(rolls[1]) { it.ref },
            dex = col(rolls[2]) { it.dex },
            tech = col(rolls[3]) { it.tech },
            cool = col(rolls[4]) { it.cool },
            will = col(rolls[5]) { it.will },
            luck = col(rolls[6]) { it.luck },
            move = col(rolls[7]) { it.move },
            body = col(rolls[8]) { it.body },
            emp = col(rolls[9]) { it.emp }
        )
    }

    // ۱۳ مهارت پایه‌ی زندگی (همه باید حداقل ۲ داشته باشند)
    val baseLifeSkills: List<String> = listOf(
        "Athletics", "Brawling", "Concentration", "Conversation",
        "Education", "Evasion", "First Aid", "Human Perception",
        "Language (Streetslang)", "Local Expert (Your Home)", "Perception",
        "Persuasion", "Stealth"
    )

    // ==========================================
    // ۱. زبان بومی (Cultural Origins)
    // ==========================================
    private val culturalLanguages = mapOf(
        1 to listOf("Chinese", "Cree", "Creole", "English", "French", "Navajo", "Spanish"),
        2 to listOf("Creole", "English", "German", "Guarani", "Mayan", "Portuguese", "Quechua", "Spanish"),
        3 to listOf("Dutch", "English", "French", "German", "Italian", "Norwegian", "Portuguese", "Spanish"),
        4 to listOf("English", "Finnish", "Polish", "Romanian", "Russian", "Ukrainian"),
        5 to listOf("Arabic", "Berber", "English", "Farsi", "French", "Hebrew", "Turkish"),
        6 to listOf("Arabic", "English", "French", "Hausa", "Lingala", "Oromo", "Portuguese", "Swahili", "Twi", "Yoruba"),
        7 to listOf("Bengali", "Dari", "English", "Hindi", "Nepali", "Sinhalese", "Tamil", "Urdu"),
        8 to listOf("Arabic", "Burmese", "English", "Filipino", "Hindi", "Indonesian", "Khmer", "Malayan", "Vietnamese"),
        9 to listOf("Cantonese Chinese", "English", "Japanese", "Korean", "Mandarin Chinese", "Mongolian"),
        10 to listOf("English", "French", "Hawaiian", "Maori", "Pama-Nyungan", "Tahitian")
    )

    fun rollNativeLanguage(roll: Int): Pair<String, Int> {
        val regionLanguages = culturalLanguages[roll] ?: culturalLanguages[1].orEmpty()
        val language = if (regionLanguages.isEmpty()) "" else regionLanguages.random()
        return Pair(gtr("Language (%1s)", language), 4)
    }

    // ==========================================
    // ۲. مهارت‌ها (Skills)
    // ==========================================
    private val baseSkills: Map<String, Int> = mapOf(
        "Athletics" to 2, "Brawling" to 2, "Concentration" to 2, "Conversation" to 2,
        "Education" to 2, "Evasion" to 6, "First Aid" to 2, "Human Perception" to 2,
        "Language (Streetslang)" to 2, "Local Expert (Your Home)" to 2, "Perception" to 2,
        "Persuasion" to 2, "Stealth" to 2
    )

    fun getSkillsForRole(role: String, languageRoll: Int): Map<String, Int> {
        val finalSkills = baseSkills.toMutableMap()

        val nativeLang = rollNativeLanguage(languageRoll)
        finalSkills[nativeLang.first] = nativeLang.second

        when (role.uppercase()) {
            "SOLO" -> {
                finalSkills["First Aid"] = 6
                finalSkills["Perception"] = 6
                finalSkills["Autofire"] = 6
                finalSkills["Handgun"] = 6
                finalSkills["Interrogation"] = 6
                finalSkills["Melee Weapon"] = 6
                finalSkills["Resist Torture/Drugs"] = 6
                finalSkills["Shoulder Arms"] = 6
                finalSkills["Tactics"] = 6
            }
            "ROCKERBOY" -> {
                finalSkills["Brawling"] = 6
                finalSkills["Persuasion"] = 6
                finalSkills["Composition"] = 6
                finalSkills["Handgun"] = 6
                finalSkills["Melee Weapon"] = 6
                finalSkills["Personal Grooming"] = 4
                finalSkills["Play Instrument (Guitar/Mic)"] = 6
                finalSkills["Streetwise"] = 6
                finalSkills["Wardrobe & Style"] = 4
            }
            "NETRUNNER" -> {
                finalSkills["Education"] = 6
                finalSkills["Stealth"] = 6
                finalSkills["Basic Tech"] = 6
                finalSkills["Conceal/Reveal Object"] = 6
                finalSkills["Cryptography"] = 6
                finalSkills["Cybertech"] = 6
                finalSkills["Electronics/Security Tech"] = 6
                finalSkills["Handgun"] = 6
                finalSkills["Library Search"] = 6
            }
            "TECH" -> {
                finalSkills["Education"] = 6
                finalSkills["First Aid"] = 6
                finalSkills["Basic Tech"] = 6
                finalSkills["Cybertech"] = 6
                finalSkills["Electronics/Security Tech"] = 6
                finalSkills["Land Vehicle Tech"] = 6
                finalSkills["Shoulder Arms"] = 6
                finalSkills["Science (Choose 1)"] = 6
                finalSkills["Weaponstech"] = 6
            }
            "MEDTECH" -> {
                finalSkills["Conversation"] = 6
                finalSkills["Education"] = 6
                finalSkills["Human Perception"] = 6
                finalSkills["Basic Tech"] = 6
                finalSkills["Cybertech"] = 4
                finalSkills["Deduction"] = 6
                finalSkills["Paramedic"] = 6
                finalSkills["Resist Torture/Drugs"] = 4
                finalSkills["Science (Choose 1)"] = 6
                finalSkills["Shoulder Arms"] = 6
            }
            "MEDIA" -> {
                finalSkills["Conversation"] = 6
                finalSkills["Human Perception"] = 6
                finalSkills["Perception"] = 6
                finalSkills["Persuasion"] = 6
                finalSkills["Bribery"] = 6
                finalSkills["Composition"] = 6
                finalSkills["Deduction"] = 6
                finalSkills["Handgun"] = 6
                finalSkills["Library Search"] = 4
                finalSkills["Lip Reading"] = 4
                finalSkills["Photography/Film"] = 4
            }
            "LAWMAN" -> {
                finalSkills["Brawling"] = 6
                finalSkills["Conversation"] = 6
                finalSkills["Autofire"] = 6
                finalSkills["Criminology"] = 6
                finalSkills["Deduction"] = 6
                finalSkills["Handgun"] = 6
                finalSkills["Interrogation"] = 6
                finalSkills["Shoulder Arms"] = 6
                finalSkills["Tracking"] = 6
            }
            "EXEC" -> {
                finalSkills["Conversation"] = 6
                finalSkills["Education"] = 6
                finalSkills["Human Perception"] = 6
                finalSkills["Persuasion"] = 6
                finalSkills["Accounting"] = 6
                finalSkills["Bureaucracy"] = 6
                finalSkills["Business"] = 6
                finalSkills["Deduction"] = 6
                finalSkills["Handgun"] = 6
                finalSkills["Lip Reading"] = 6
                finalSkills["Personal Grooming"] = 4
            }
            "FIXER" -> {
                finalSkills["Conversation"] = 6
                finalSkills["Human Perception"] = 6
                finalSkills["Language (Streetslang)"] = 4
                finalSkills["Local Expert (Your Home)"] = 6
                finalSkills["Persuasion"] = 4
                finalSkills["Bribery"] = 6
                finalSkills["Business"] = 6
                finalSkills["Forgery"] = 6
                finalSkills["Handgun"] = 6
                finalSkills["Pick Lock"] = 4
                finalSkills["Streetwise"] = 6
                finalSkills["Trading"] = 6
            }
            "NOMAD" -> {
                finalSkills["Brawling"] = 6
                finalSkills["First Aid"] = 6
                finalSkills["Perception"] = 4
                finalSkills["Stealth"] = 6
                finalSkills["Animal Handling"] = 6
                finalSkills["Drive Land Vehicle"] = 6
                finalSkills["Handgun"] = 6
                finalSkills["Melee Weapon"] = 6
                finalSkills["Tracking"] = 6
                finalSkills["Trading"] = 6
                finalSkills["Wilderness Survival"] = 6
            }
        }
        return finalSkills
    }

    // ==========================================
    // ۳. تجهیزات اولیه (Starter Equipment)
    // ==========================================
    fun getEquipmentForRole(role: String): StartingEquipment {
        fun se(
            weapons: List<String>,
            gear: List<String>,
            cyberware: List<String>,
            humanityLoss: Int,
            choices: List<ChoiceGroup> = emptyList()
        ) = StartingEquipment(weapons.toMutableList(), gear.toMutableList(), cyberware.toMutableList(), humanityLoss, choices)

        return when (role.uppercase()) {
            "ROCKERBOY" -> se(
                weapons = listOf(
                    "Very Heavy Pistol", "Basic VH Pistol Ammunition x50",
                    "Teargas Grenade x2",
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf(
                    "Agent", "Computer", "Glow Paint x5", "Pocket Amp", "Radio Scanner/Music Player", "Video Camera",
                    "Generic Chic: Jacket, Jewelry x3, Top x4",
                    "Leisurewear: Jewelry, Mirrorshades, Footwear", "Urbanflash: Bottoms, Top"
                ),
                cyberware = listOf("Audio Recorder", "Chemskin", "Cyberaudio Suite", "Tech Hair"),
                humanityLoss = 9,
                choices = listOf(
                    ChoiceGroup("Weapon Choice", listOf("Heavy Melee Weapon", "Flashbang Grenade")),
                    ChoiceGroup("Gear Choice", listOf("Electric Guitar", "Bug Detector"))
                )
            )
            "SOLO" -> se(
                weapons = listOf(
                    "Assault Rifle", "Very Heavy Pistol", "Basic VH Pistol Ammunition x30",
                    "Basic Rifle Ammunition x70",
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf("Agent", "Leisurewear: Footwear x2, Jacket x3, Mirrorshades, Bottoms x2, Top x2"),
                cyberware = listOf("Biomonitor", "Neural Link"),
                humanityLoss = 14,
                choices = listOf(
                    ChoiceGroup("Weapon/Defense Choice", listOf("Heavy Melee Weapon", "Bulletproof Shield")),
                    ChoiceGroup("Cyberware Choice", listOf("Sandevistan Speedware", "Wolvers"))
                )
            )
            "NETRUNNER" -> se(
                weapons = listOf(
                    "Very Heavy Pistol", "Basic VH Pistol Ammunition x30",
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf(
                    "Agent", "Cyberdeck (7 Slots)", "Virtuality Goggles", "Program: Armor", "Program: Sword",
                    "Generic Chic: Top x10", "Leisurewear: Footwear x2, Jewelry, Bottoms x2", "Urban Flash: Jacket"
                ),
                cyberware = listOf("Interface Plugs", "Neural Link", "Shift Tacts"),
                humanityLoss = 14,
                choices = listOf(
                    ChoiceGroup("Program Choice 1", listOf("Program: See Ya", "Program: Eraser")),
                    ChoiceGroup("Program Choice 2", listOf("Program: Sword", "Program: Vrizzbolt")),
                    ChoiceGroup("Program Choice 3", listOf("Program: Worm", "Program: Sword"))
                )
            )
            "TECH" -> se(
                weapons = listOf(
                    "Flashbang Grenade",
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf(
                    "Agent", "Anti-Smog Breathing Mask", "Disposable Cell Phone", "Duct Tape x5",
                    "Flashlight", "Road Flare x6", "Tech Bag",
                    "Generic Chic: Bottoms x8, Tops x10", "Leisurewear: Footwear x2"
                ),
                cyberware = listOf("Cybereye", "MicroOptics", "Skinwatch", "Tool Hand"),
                humanityLoss = 12,
                choices = listOf(
                    ChoiceGroup("Primary Weapon Package", listOf(
                        "Shotgun || Basic Shotgun Shell Ammunition x100",
                        "Assault Rifle || Basic Rifle Ammunition x100"
                    ))
                )
            )
            "MEDTECH" -> se(
                weapons = listOf(
                    "Smoke Grenade x2", "Bulletproof Shield",
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf(
                    "Agent", "Airhypo", "Handcuffs", "Flashlight", "Generic Chic Jacket x3",
                    "Glow Paint", "Medtech Bag", "Leisurewear: Footwear, Bottoms x3, Top x5"
                ),
                cyberware = listOf("Biomonitor", "Cybereye", "TeleOptics"),
                humanityLoss = 12,
                choices = listOf(
                    ChoiceGroup("Primary Weapon Package", listOf(
                        "Shotgun || Basic Shotgun Shell Ammunition x100 || Incendiary Shotgun Shell Ammunition x10",
                        "Assault Rifle || Basic Rifle Ammunition x100 || Incendiary Rifle Ammunition x10"
                    )),
                    ChoiceGroup("Cyberware Choice", listOf("Nasal Filters", "Toxin Binders"))
                )
            )
            "MEDIA" -> se(
                weapons = listOf("Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"),
                gear = listOf(
                    "Agent", "Audio Recorder", "Binoculars", "Flashlight", "Computer",
                    "Radio Scanner/Music Player", "Scrambler/Descrambler", "Video Camera",
                    "Generic Chic: Footwear, Bottoms, Top", "Leisurewear: Jacket", "Urbanflash: Mirrorshades"
                ),
                cyberware = listOf("Cyberaudio Suite", "Light Tattoo"),
                humanityLoss = 10,
                choices = listOf(
                    ChoiceGroup("Sidearm Package", listOf(
                        "Heavy Pistol || Basic H Pistol Ammunition x50",
                        "Very Heavy Pistol || Basic VH Pistol Ammunition x50"
                    )),
                    ChoiceGroup("Gear Choice", listOf("Disposable Cellphone x2", "Grapple Gun")),
                    ChoiceGroup("Cyberaudio Choice", listOf("Amplified Hearing", "Voice Stress Analyzer"))
                )
            )
            "LAWMAN" -> se(
                weapons = listOf(
                    "Heavy Pistol", "Basic H Pistol Ammunition x30",
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf(
                    "Agent", "Flashlight", "Handcuffs x2", "Radio Communicator", "Road Flare x10",
                    "Generic Chic: Jacket, Bottoms x2, Top x3",
                    "Leisurewear: Footwear x2, Jacket x2, Bottoms x2, Mirrorshades, Top x2"
                ),
                cyberware = listOf("Hidden Holster", "Subdermal Pocket"),
                humanityLoss = 10,
                choices = listOf(
                    ChoiceGroup("Primary Weapon Package", listOf(
                        "Assault Rifle || Basic Rifle Ammunition x100",
                        "Shotgun || Basic Shotgun Shell Ammunition x100",
                        "Shotgun || Basic Slug Ammunition x100"
                    )),
                    ChoiceGroup("Utility Choice", listOf("Bulletproof Shield", "Smoke Grenade x2"))
                )
            )
            "EXEC" -> se(
                weapons = listOf(
                    "Very Heavy Pistol", "Basic VH Pistol Ammunition x50",
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf(
                    "Radio Communicator x4", "Scrambler/Descrambler",
                    "Businesswear: Footwear, Jacket, Bottoms, Mirrorshades, Top, Jewelry x2"
                ),
                cyberware = listOf("Cyberaudio Suite", "Internal Agent"),
                humanityLoss = 12,
                choices = listOf(
                    ChoiceGroup("Fashionware Choice", listOf("Biomonitor", "Tech Hair")),
                    ChoiceGroup("Internal Choice", listOf("Toxin Binders", "Nasal Filters"))
                )
            )
            "FIXER" -> se(
                weapons = listOf(
                    "Light Melee Weapon",
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf(
                    "Agent", "Bug Detector", "Computer", "Disposable Phone x2",
                    "Generic Chic: Contacts, Jewelry", "Leisurewear: Mirrorshades",
                    "Urbanflash: Footwear, Jacket, Bottoms, Top"
                ),
                cyberware = listOf("Cyberaudio Suite", "Internal Agent", "Subdermal Pocket"),
                humanityLoss = 16,
                choices = listOf(
                    ChoiceGroup("Sidearm 1", listOf("Heavy Pistol", "Very Heavy Pistol")),
                    ChoiceGroup("Sidearm 2", listOf("Heavy Pistol", "Very Heavy Pistol")),
                    ChoiceGroup("Pistol Ammo", listOf("Basic H Pistol Ammunition x100", "Basic VH Pistol Ammunition x100")),
                    ChoiceGroup("Cyberaudio Choice", listOf("Voice Stress Analyzer", "Amplified Hearing"))
                )
            )
            "NOMAD" -> se(
                weapons = listOf(
                    "Light Armorjack Body Armor (SP11)", "Light Armorjack Head Armor (SP11)"
                ),
                gear = listOf(
                    "Agent", "Anti-Smog Breathing Mask", "Duct Tape", "Flashlight", "Grapple Gun",
                    "Inflatable Bed & Sleep-Bag", "Medtech Bag", "Radio Communicator x2", "Rope",
                    "Techtool", "Tent and Camping Equipment", "Bohemian: Jewelry",
                    "Nomad Leathers: Top x4, Bottom x2, Footwear x2, Jacket, Hat"
                ),
                cyberware = listOf("Neural Link"),
                humanityLoss = 14,
                choices = listOf(
                    ChoiceGroup("Sidearm Package", listOf(
                        "Heavy Pistol || Basic H Pistol Ammunition x100",
                        "Very Heavy Pistol || Basic VH Pistol Ammunition x100"
                    )),
                    ChoiceGroup("Second Weapon", listOf("Heavy Melee Weapon", "Heavy Pistol")),
                    ChoiceGroup("Cyberware Choice", listOf("Interface Plugs", "Wolvers"))
                )
            )
            else -> se(emptyList(), emptyList(), emptyList(), 0)
        }
    }

    // ==========================================
    // ۴. استت‌های فرعی (Derived Stats)
    // ==========================================
    fun calculateMaxHp(body: Int, will: Int): Int {
        val average = ceil((body + will) / 2.0).toInt()
        return 10 + (5 * average)
    }

    // جفت: (بیشینه انسانیت, انسانیت فعلی)
    fun calculateHumanity(emp: Int, humanityLoss: Int): Pair<Int, Int> {
        val maxHumanity = emp * 10
        val currentHumanity = (maxHumanity - humanityLoss).coerceAtLeast(0)
        return Pair(maxHumanity, currentHumanity)
    }

    fun getInitialLifestyleAndHousing(role: String): Pair<String, String> {
        return if (role.uppercase() == "EXEC") {
            Pair("Good Prepak", "Corporate Conapt")
        } else {
            Pair("Kibble", "Cargo Container")
        }
    }
}

data class ChoiceGroup(
    val category: String,
    val options: List<String>
)

data class StartingEquipment(
    val weapons: MutableList<String>,
    val gear: MutableList<String>,
    val cyberware: MutableList<String>,
    val humanityLoss: Int,
    val pendingChoices: List<ChoiceGroup>
)
