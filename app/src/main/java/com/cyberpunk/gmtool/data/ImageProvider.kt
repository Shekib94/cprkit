package com.cyberpunk.gmtool.data // اگر تو پوشه دیگه‌ای ساختی، این خط رو اصلاح کن

import com.cyberpunk.gmtool.R

object ImageProvider {

    // این تابع اسم نقش و نوع عکس رو می‌گیره و آدرسِ فایلِ عکس رو برمی‌گردونه
    fun getRoleImage(roleName: String, isPortrait: Boolean): Int {
        val normalizedRole = roleName.lowercase().trim()

        return when (normalizedRole) {
            "solo", "سولو" -> if (isPortrait) R.drawable.cpr_solo2 else R.drawable.cpr_solo1
            "rockerboy", "راکربوی" -> if (isPortrait) R.drawable.cpr_rockerboy2 else R.drawable.cpr_rockerboy1
            "netrunner", "نترانر", "نت رانر" -> if (isPortrait) R.drawable.cpr_netrunner2 else R.drawable.cpr_netrunner1
            "tech", "تک", "تکنسین" -> if (isPortrait) R.drawable.cpr_tech2 else R.drawable.cpr_tech1
            "medtech", "مدتک", "مد تک", "پزشک" -> if (isPortrait) R.drawable.cpr_medtech2 else R.drawable.cpr_medtech1
            "media", "مدیا", "رسانه", "خبرنگار" -> if (isPortrait) R.drawable.cpr_media2 else R.drawable.cpr_media1
            "exec", "اگزک", "مدیر", "مدیر شرکتی" -> if (isPortrait) R.drawable.cpr_exec2 else R.drawable.cpr_exec1
            "lawman", "لاومن", "پلیس", "مامور قانون" -> if (isPortrait) R.drawable.cpr_lawman2 else R.drawable.cpr_lawman1
            "fixer", "فیکسر", "دلال", "واسطه" -> if (isPortrait) R.drawable.cpr_fixer2 else R.drawable.cpr_fixer1
            "nomad", "نومد", "کوچ‌نشین" -> if (isPortrait) R.drawable.cpr_nomad2 else R.drawable.cpr_nomad1

            // اگر کاراکتر نقشی نداشت یا اسمش اشتباه بود، عکس دیفالت رو نشون بده
            else -> R.drawable.cpr_norole2
        }
    }

    /**
     * پرتره‌ی NPC بر اساس دسته‌بندی (و در صورت نیاز نقش).
     * @param npcCategory مقدار Character.npcCategory (مثلاً NpcData.CAT_CYBERPSYCHO)
     * @param role نقش NPC (به‌عنوان fallback)
     */
    fun getNpcImage(npcCategory: String, role: String = "Solo", name: String = ""): Int {
        val n = name.lowercase()
        // پرتره‌های اختصاصی باس‌ها
        when {
            n.contains("royce") -> return R.drawable.npc_boss_royce
            n.contains("placide") -> return R.drawable.npc_boss_placide
            n.contains("sasquatch") -> return R.drawable.npc_boss_sasquatch
            n.contains("oda") -> return R.drawable.npc_boss_oda
            n.contains("adam smasher") || n.contains("smasher") -> return R.drawable.npc_boss
        }
        val cat = npcCategory.lowercase()
        return when {
            cat.contains("cyberpsycho") || cat.contains("سایبرسایکو") -> R.drawable.npc_cyberpsycho
            cat.contains("lawman") || cat.contains("لاومن") || cat.contains("پلیس") -> R.drawable.npc_lawman
            cat.contains("exec") || cat.contains("اگزک") -> R.drawable.npc_exec
            cat.contains("boss") || cat.contains("باس") || cat.contains("غول") -> R.drawable.npc_boss
            cat.contains("military") || cat.contains("نظامی") || cat.contains("مزدور") -> R.drawable.npc_military
            cat.contains("corporate") || cat.contains("شرکتی") -> R.drawable.npc_corpsec
            cat.contains("gang") || cat.contains("گنگ") -> R.drawable.npc_gangster
            role.equals("netrunner", true) -> R.drawable.npc_netrunner
            role.equals("nomad", true) -> R.drawable.npc_nomad
            role.equals("lawman", true) -> R.drawable.npc_lawman
            role.equals("exec", true) -> R.drawable.npc_exec
            role.equals("medtech", true) -> R.drawable.npc_medic
            else -> R.drawable.npc_gangster
        }
    }
}