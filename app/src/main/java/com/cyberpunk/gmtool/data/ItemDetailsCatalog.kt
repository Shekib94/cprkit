package com.cyberpunk.gmtool.data

/**
 * One canonical presentation layer for Store + owned Inventory.
 * StoreItem.description remains the Core-derived rules summary; this object adds only play-facing
 * clarification that prevents common table mistakes. No UI owns a second copy of item rules text.
 */
object ItemDetailsCatalog {
    fun item(name: String, category: String): StoreItem? = StoreCatalog.findForInventory(name, category)


    /** Single canonical player-facing description. All UIs should render this instead of composing their own copies. */
    fun fullDescription(item: StoreItem): String {
        val visual = visualDescription(item)
        if (!item.category.equals("Cyberware", true)) {
            val extra = clarification(item)
            return listOf(visual, item.description.trim(), extra.trim()).filter { it.isNotBlank() }.joinToString("\n\n")
        }
        val r = CyberwareCatalog.ruleFor(item.name)
        val install = buildString {
            if (r != null) {
                append("نصب: ${r.install}.")
                r.prerequisite?.let { append(" پیش‌نیاز: $it.") }
                if (r.foundationCapacity != null) append(" ${r.foundationCapacity} Option Slot فراهم می‌کند.")
                else if (!r.noSlot && !r.chipware) append(" ${r.slotsUsed} Option Slot مصرف می‌کند.")
                if (r.paired) append(" برای اثر کامل باید به‌صورت Pair نصب شود و Humanity Loss هر نسخه جداگانه محاسبه می‌شود.")
                append(" Humanity Loss در Character Generation برابر ${r.hl} است")
                if (r.hlDice.isNotBlank()) append(" و پس از Character Generation با ${r.hlDice} تعیین می‌شود")
                append(". Uninstall، Humanity از‌دست‌رفته را خودکار برنمی‌گرداند؛ بازیابی آن به Therapy وابسته است.")
            }
        }
        return listOf(visual, item.description.trim(), install.trim()).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    /** A short visual cue so the item can be imagined at the table, kept in the same canonical source. */
    fun visualDescription(item: StoreItem): String {
        val n = item.name.lowercase()
        return when {
            "wolver" in n -> "ظاهر: سه تیغه‌ی بلند و براق از روی بندِ انگشت‌ها بیرون می‌جهند؛ وقتی جمع شوند زیر پوست و قاب Cyberarm پنهان می‌شوند و دست تقریباً عادی به نظر می‌رسد."
            item.category.equals("Weapons", true) -> "ظاهر: یک سلاح ${item.name} با بدنه‌ی صنعتی، خطوط زاویه‌دار و قطعات کاربردیِ Night City؛ ساخته شده برای استفاده‌ی خشن و سریع، نه نمایش ویترینی."
            item.category.equals("Cyberware", true) -> "ظاهر: ${item.name} یک قطعه‌ی سایبری با اتصالات فلزی، کابل‌های ظریف و نشانگرهای کم‌نور است؛ بسته به محل نصب، بخشی از آن زیر پوست پنهان و بخشی مثل یک ارتقای مکانیکی دیده می‌شود."
            item.category.equals("Armor", true) -> "ظاهر: ${item.name} زرهی شهری و لایه‌لایه است؛ صفحات محافظ زیر پارچه یا روی بدنه قرار گرفته‌اند تا بدون ظاهر نظامیِ کامل، ضربه را بگیرند."
            item.category.equals("Clothing", true) -> "ظاهر: ${item.name} پوشاک خیابانی Night City با برش تیز، متریال مصنوعی و جزئیات مد روز است؛ بیشتر هویت و استایل می‌سازد تا حفاظت رزمی."
            item.category.equals("Vehicles", true) -> "ظاهر: ${item.name} وسیله‌ای با بدنه‌ی زاویه‌دار، پنل‌های مقاوم و نورهای شهری است؛ طراحی‌اش حس سرعت، دوام و تعمیرپذیری در خیابان را منتقل می‌کند."
            item.category.equals("Programs", true) || item.category.equals("Hardware", true) -> "ظاهر: در فضای NET، ${item.name} به شکل یک ماژول/آیکون دیجیتال تیز و نورانی دیده می‌شود؛ در دنیای واقعی، رد آن روی deck به شکل چیپ یا ماژول کوچک است."
            item.category.equals("Drugs", true) -> "ظاهر: ${item.name} معمولاً در یک دوز کوچک خیابانی، آمپول، کپسول یا بسته‌ی مهروموم‌شده با برچسب‌های هشدار و رنگ‌بندی بازار سیاه عرضه می‌شود."
            item.category.equals("Ammo", true) -> "ظاهر: ${item.name} مهماتی با پوکه/بدنه‌ی مشخص و نشانه‌گذاری رنگی یا حک‌شده است تا نوع آن در یک نگاه از مهمات عادی تشخیص داده شود."
            else -> "ظاهر: ${item.name} وسیله‌ای جمع‌وجور و کاربردی با طراحی صنعتیِ Night City است؛ خط‌وخش، پلاستیک سخت و فلز مات آن نشان می‌دهد برای استفاده‌ی روزمره ساخته شده است."
        }
    }

    fun clarification(item: StoreItem): String = when (item.category) {
        "Weapons" -> weaponNote(item)
        "Weapon Mods" -> "Weapon Attachment است؛ فقط روی سلاح واجد شرایط نصب می‌شود. Exotic Weaponها به‌طور پیش‌فرض Attachment و مهمات Non-Basic نمی‌پذیرند مگر توضیح خود سلاح خلافش را بگوید. ظرفیت Attachment سلاح را قبل از نصب بررسی کن."
        "Ammo" -> ammoNote(item)
        "Armor" -> armorNote(item)
        "Cyberware" -> ""
        "Clothing" -> "Fashion است، نه Armor. پوشیدن این قطعه به‌تنهایی SP نمی‌دهد. قیمت و Style همان رکورد فروشگاه است و Inventory نیز دقیقاً از همین رکورد استفاده می‌کند."
        "Gear" -> gearNote(item)
        "Drugs" -> "Street Drug: Primary Effect بلافاصله اعمال می‌شود. در پایان مدت اثر، WILL + Resist Torture/Drugs + 1d10 در برابر DVِ Secondary Effect رول می‌شود. شکست می‌تواند Addiction/اثر ثانویه‌ی ماندگار ایجاد کند تا با Therapy درمان شود. دوزهای بیشتر مدت Primary Effect را تمدید می‌کنند."
        "Hardware" -> "Cyberdeck Hardware است. Hardware و Program از Slotهای محدود Cyberdeck استفاده می‌کنند؛ Hardware معمولاً 1 Slot می‌گیرد مگر خود آیتم خلافش را بگوید. Install/Uninstall آن یک ساعت زمان می‌برد."
        "Programs" -> if (item.programClass.contains("Black ICE", true))
            "Black ICE دو Cyberdeck Slot می‌گیرد. Activate/Deactivate یک NET Action است. PER برای Slide، SPD برای واکنش/Initiative، ATK/DEF برای نبرد و REZ به‌منزله HP برنامه است."
        else "Program معمولی یک Cyberdeck Slot می‌گیرد. Activate/Deactivate یک NET Action است و هر copy فقط یک بار در هر Meatspace Round می‌تواند Activated شود. اثر دقیق همین رکورد را اجرا کن."
        "Vehicles" -> "Vehicle instance پس از خرید SDP فعلی خودش را دارد. در Combat از MOVE و SDP همین مدل استفاده کن؛ تعمیر و Upgrade وضعیت همان وسیله را تغییر می‌دهند، نه تعریف پایه‌ی Catalog را."
        else -> ""
    }

    private fun weaponNote(i: StoreItem): String = buildString {
        append("برای Attack از ${i.weaponSkill.ifBlank { "Skill درج‌شده در سلاح" }} استفاده کن")
        if (i.rof.isNotBlank()) append("؛ ROF ${i.rof}")
        if (i.magStd != null) append("؛ خشاب Standard ${i.magStd}")
        if (i.autofire.isNotBlank()) append(". Autofire از Skillِ Autofire و جدول DV مخصوص خودش استفاده می‌کند")
        append(". Melee Weaponها هنگام Damage فقط نصف SP زره را لحاظ می‌کنند؛ این قاعده به Brawling تعمیم داده نمی‌شود.")
    }

    private fun ammoNote(i: StoreItem): String {
        val n=i.name.lowercase()
        return when {
            "armor piercing" in n -> "وقتی این مهمات باعث Ablation شود، به‌جای 1، مقدار 2 SP کم می‌شود. برای Shotgun Shell در دسترس نیست."
            "biotoxin" in n -> "Damage معمول سلاح را نمی‌دهد؛ هدف گوشتی DV15 Resist Torture/Drugs می‌دهد و در شکست 3d6 مستقیم به HP می‌گیرد؛ Armor ablate نمی‌شود. فقط Arrow/Grenade."
            "emp" in n -> "Damage معمول نمی‌دهد؛ هدف DV15 Cybertech می‌دهد. در شکست GM دو Cyberware یا وسیله الکترونیکی حمل‌شده را برای 1 دقیقه از کار می‌اندازد. فقط Grenade."
            "expansive" in n -> "اگر Foreign Object Critical Injury ایجاد شود، یک Critical Injury دیگر غیر از Foreign Object نیز رول می‌شود؛ injury دوم Bonus Damage ندارد. Arrow/Bullet/Slug."
            "flashbang" in n -> "Damage معمول نمی‌دهد؛ DV15 Resist Torture/Drugs. شکست: Damaged Eye و Damaged Ear برای 1 دقیقه، بدون Bonus Damage. فقط Grenade."
            "incendiary" in n -> "اگر Damage از Armor عبور کند هدف آتش می‌گیرد؛ تا وقتی با Action خاموش نکند، پایان هر Turn مقدار 2 HP مستقیم می‌گیرد. این اثر stack نمی‌شود."
            "poison" in n -> "Damage معمول نمی‌دهد؛ DV13 Resist Torture/Drugs، شکست = 2d6 مستقیم به HP و بدون Ablation. فقط Arrow/Grenade."
            "rubber" in n -> "این Damage Critical Injury ایجاد نمی‌کند و Armor را ablate نمی‌کند."
            "sleep" in n -> "اثر Sleep را طبق DV و محدودیت نوع مهمات همین آیتم اجرا کن؛ Damage معمول جایگزین اثر ویژه می‌شود."
            "smart" in n -> "Smart Ammunition برای اصلاح شلیک از مکانیزم ویژه‌ی خودش استفاده می‌کند؛ آن را با Smartgun Link یکی ندان."
            "smoke" in n -> "Damage ندارد؛ ناحیه‌ی دود ایجاد می‌کند. جریمه‌ی معمول برای کاری که دید در دود مختلش کرده -4 است."
            "teargas" in n -> "Damage ندارد؛ هدف دارای meat eyes باید DV13 Resist Torture/Drugs بدهد؛ شکست = Damaged Eye برای 1 دقیقه، بدون Bonus Damage."
            else -> "Basic Ammunition ویژگی ویژه ندارد. Grenade/Rocket تکی خریداری می‌شوند؛ سایر انواع مهمات در بسته‌های 10تایی قیمت‌گذاری می‌شوند."
        }
    }

    private fun armorNote(i: StoreItem): String = if (i.name.contains("Shield", true))
        "Shield برابر 10 HP Cover متحرک است و یک دست را اشغال می‌کند. Damage ابتدا HP سپر را کم می‌کند؛ در 0 HP دیگر محافظت نمی‌کند."
    else if (i.name.contains("Bodyweight", true))
        "Bodyweight Suit همیشه Head و Body را با هم می‌پوشاند؛ هر محل SP11 مستقل دارد، هر دو با هم Repair می‌شوند و فقط یک Suit قابل پوشیدن است. هنگام پوشیدن یک Hardware-only Slot به Cyberdeck متصل می‌دهد."
    else "Armor فقط در محل Head/Body خودش SP می‌دهد. اگر Damage از SP عبور کند Armor معمولاً 1 SP ablate می‌شود. Armor Penalty مجموعه‌ی پوشیده‌شده طبق قواعد layering محاسبه می‌شود."

    private fun cyberwareNote(i: StoreItem): String {
        val r=CyberwareCatalog.ruleFor(i.name) ?: return "خرید آزاد است؛ نصب باید prerequisite، Slot و Humanity را بررسی کند."
        return buildString {
            append("خرید این قطعه آزاد است؛ اثر و Humanity فقط هنگام Install اعمال می‌شود. ")
            if (r.prerequisite != null) append("پیش‌نیاز: ${r.prerequisite}. ")
            if (r.foundationCapacity != null) append("این قطعه ${r.foundationCapacity} Option Slot فراهم می‌کند. ")
            else if (!r.noSlot && !r.chipware) append("${r.slotsUsed} Option Slot مصرف می‌کند. ")
            if (r.paired) append("Paired است: دو نسخه روی دو foundation جدا لازم است و HL هر نسخه جدا پرداخت می‌شود. ")
            append("پس از Character Generation، HL از ${r.hlDice.ifBlank { r.hl.toString() }} تعیین می‌شود. Uninstall Humanity فعلی را برنمی‌گرداند؛ فقط Maximum Humanity را در صورت امکان بالا می‌برد و بازیابی Humanity به Therapy نیاز دارد.")
        }
    }

    private fun gearNote(i: StoreItem): String = when {
        i.name.contains("Cyberdeck", true) -> "Cyberdeck برای Netrunning به Neural Link + Interface Plugs نیاز دارد. Program و Hardware از Slotهای همین deck استفاده می‌کنند؛ Poor/Standard/Excellent به‌ترتیب ظرفیت تعریف‌شده در رکورد خود را دارند."
        i.name.contains("Airhypo", true) -> "برای administer کردن یک dose به هدف willing با Action استفاده می‌شود؛ علیه هدف unwilling معمولاً باید Melee Attack موفق انجام شود."
        i.name.contains("Cryo", true) -> "Cryo gear برای stabilization/حمل بیمار طبق قواعد Medtech و Trauma Team استفاده می‌شود؛ آن را به‌عنوان healing فوری معمولی حساب نکن."
        else -> "کاربرد مکانیکی و محدودیت این Gear در توضیح اصلی بالا آمده است. اگر آیتم Check، Action، range یا prerequisite مشخصی دارد همان مقدار بر قواعد عمومی اولویت دارد."
    }
}
