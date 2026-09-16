package com.cyberpunk.gmtool.data




/**
 * Beginner-facing, paraphrased Cyberpunk RED reference.
 * Keep text short enough for table-side use; RuleInfoButton reads the same source.
 * This is a rules aid, not a replacement for the Core Rulebook.
 */
data class RuleReferenceEntry(
    val key: String,
    val title: String,
    val category: String,
    val short: String,
    val full: String,
    val tags: List<String> = emptyList()
)

object RuleReferenceData {
    val entries: List<RuleReferenceEntry> = listOf(
        RuleReferenceEntry(
            "combat.aimed_shot", "Aimed Shot — هدف‌گیری دقیق", "Combat",
            "برای زدن سر، پا یا وسیله‌ای که هدف در دست دارد استفاده می‌شود. حمله حداکثر ROF 1 دارد و Check با جریمهٔ -8 انجام می‌شود.",
            "Aimed Shot وقتی به کار می‌رود که فقط آسیب‌زدن کافی نیست و می‌خواهی یک نتیجهٔ مشخص بگیری. کل Action را صرف همان حمله می‌کنی و -8 به Check می‌گیری. سر آسیب عبوری را بیشتر می‌کند، شلیک به پا می‌تواند Broken Leg ایجاد کند و زدن وسیلهٔ در دست می‌تواند باعث افتادن آن شود. همیشه محدودیت‌های خود سلاح را هم چک کن؛ بعضی سلاح‌ها اصلاً Aimed Shot ندارند.",
            listOf("aim", "head", "leg", "held item", "هدف‌گیری")
        ),
        RuleReferenceEntry(
            "combat.critical_injury", "Critical Injury — جراحت بحرانی", "Combat",
            "وقتی Critical Injury ایجاد می‌شود، علاوه بر اثر مخصوص آن جراحت، 5 آسیب اضافه مستقیماً به HP وارد می‌شود.",
            "Critical Injury فقط یک نام یا وضعیت نمایشی نیست. هر جراحت اثر مکانیکی خودش را دارد و هنگام ایجاد شدن 5 Bonus Damage به HP وارد می‌کند. بعضی جراحت‌ها حرکت، Perception، استفاده از دست یا Death Save را بدتر می‌کنند. Quick Fix اثر بعضی جراحت‌ها را موقتاً مهار می‌کند؛ Treatment درمان واقعی و دائمی است.",
            listOf("critical", "injury", "جراحت", "bonus damage")
        ),
        RuleReferenceEntry(
            "combat.wound_state", "Wound State — وضعیت زخم", "Combat",
            "کم‌شدن HP فقط عدد نیست: Seriously Wounded و Mortally Wounded روی Actionها جریمه می‌دهند و باید در Checkهای مرتبط حساب شوند.",
            "با پایین‌آمدن HP، وضعیت زخم شخصیت تغییر می‌کند. Seriously Wounded یعنی شخصیت هنوز می‌تواند عمل کند اما کارها سخت‌تر می‌شوند. Mortally Wounded خطرناک‌تر است، جریمهٔ بیشتری به Actionها می‌دهد و Death Save وارد بازی می‌شود. این جریمه‌ها فقط برای حمله نیستند؛ Checkهای درمان، مهارت و کارهای دیگر هم باید وضعیت زخم را در نظر بگیرند.",
            listOf("hp", "seriously wounded", "mortally wounded", "زخم")
        ),
        RuleReferenceEntry(
            "combat.death_save", "Death Save — نجات از مرگ", "Combat",
            "در Mortally Wounded در زمان‌های لازم d10 می‌اندازی؛ باید زیر BODY اصلاح‌شده نتیجه بگیری. 10 طبیعی شکست خودکار است و هر Death Save بعدی سخت‌تر می‌شود.",
            "Death Save برای وقتی است که شخصیت در آستانهٔ مرگ قرار دارد. نتیجه باید از BODY اصلاح‌شده کمتر باشد. 10 طبیعی همیشه شکست است. بعد از هر Death Save، Penalty مربوط به Death Save بیشتر می‌شود؛ بنابراین ماندن طولانی در این وضعیت به‌سرعت خطرناک می‌شود. بعضی Critical Injuryها هم Base Death Save Penalty را افزایش می‌دهند.",
            listOf("death", "save", "body", "مرگ")
        ),
        RuleReferenceEntry(
            "combat.armor_ablation", "Armor & Ablation — زره", "Combat",
            "Damage اول با SP مقایسه می‌شود. اگر آسیب از زره عبور کند، HP کم می‌شود و معمولاً SP همان محل 1 واحد Ablate می‌شود.",
            "زره در RED آسیب را با SP متوقف می‌کند. اگر Damage نتواند از SP عبور کند، HP آسیب نمی‌بیند. اگر عبور کند، مقدار باقی‌مانده به HP می‌رسد و زره هم ضعیف‌تر می‌شود. Head Armor و Body Armor جدا حساب می‌شوند؛ پس هنگام هدف‌گیری محل برخورد را درست انتخاب کن.",
            listOf("armor", "sp", "ablate", "زره")
        ),
        RuleReferenceEntry(
            "combat.cover", "Cover — کاور", "Combat",
            "Cover مثل SP روی بدن نیست؛ خودش HP دارد و تا وقتی بین مهاجم و هدف است، اول باید Cover از سر راه برداشته شود.",
            "در RED، Cover یک جسم واقعی با HP است. اگر خط شلیک را کامل قطع کند، حمله به Cover برخورد می‌کند نه مستقیم به هدف. Damage می‌تواند Cover را تخریب کند و وقتی HP آن به صفر برسد دیگر محافظت نمی‌کند. برای GM مهم است که جنس و HP تقریبی Cover را درست انتخاب کند.",
            listOf("cover", "hp", "کاور")
        ),
        RuleReferenceEntry(
            "combat.autofire", "Autofire — رگبار", "Combat",
            "Autofire با Skill و DV مخصوص خودش انجام می‌شود و Damage بر اساس میزان عبور Check از DV تا سقف تعیین‌شدهٔ سلاح ضرب می‌شود.",
            "Autofire با شلیک معمولی فرق دارد. از Autofire Skill استفاده کن، مهمات لازم را مصرف کن و نتیجهٔ Check را با DV مناسب Range مقایسه کن. مقدار موفقیت بالاتر از DV، Multiplier آسیب را می‌سازد، اما هر سلاح سقف Autofire خودش را دارد. اگر سلاح یا خشاب مهمات کافی ندارد، Autofire قابل اجرا نیست.",
            listOf("autofire", "rifle", "smg", "رگبار")
        ),
        RuleReferenceEntry(
            "combat.suppressive_fire", "Suppressive Fire — آتش سرکوب", "Combat",
            "برای مجبورکردن دشمن‌ها به رفتن پشت Cover است، نه برای Damage مستقیم. نتیجه با مقاومت هدف‌ها مقایسه می‌شود.",
            "Suppressive Fire کنترل میدان است. مهاجم با Autofire Check فشار ایجاد می‌کند و هدف‌های در محدوده باید در برابر آن مقاومت کنند. هدفی که شکست بخورد باید از منبع آتش فاصله بگیرد و به Cover مناسب برسد. این گزینه زمانی خوب است که می‌خواهی حرکت دشمن را کنترل کنی، نه اینکه صرفاً بیشترین Damage را بدهی.",
            listOf("suppressive", "autofire", "cover", "سرکوب")
        ),
        RuleReferenceEntry(
            "medical.quick_fix_treatment", "Quick Fix vs Treatment — درمان", "Medical",
            "Quick Fix راه‌حل موقت برای خاموش‌کردن اثر جراحت است؛ Treatment درمان دائمی است و زمان و Skill مناسب خودش را می‌خواهد.",
            "برای Critical Injury دو مفهوم جدا داریم. Quick Fix معمولاً سریع‌تر است و اثر جراحت را موقتاً کنترل می‌کند، اما خود جراحت را پاک نمی‌کند. Treatment درمان اصلی است و بسته به Injury به Paramedic یا Surgery و DV مشخص نیاز دارد. هر Injury جدول درمان خودش را دارد؛ برای مثال Cracked Skull می‌تواند با Paramedic یا Surgery درمان شود.",
            listOf("paramedic", "surgery", "quick fix", "treatment", "درمان")
        ),
        RuleReferenceEntry(
            "net.interface_check", "Interface Check — نت‌رانینگ", "Netrunning",
            "Checkهای معمول Interface هم از قانون d10 RED استفاده می‌کنند: 10 طبیعی یک d10 اضافه می‌کند و 1 طبیعی یک d10 اضافه را از نتیجه کم می‌کند.",
            "در NET Architecture، بسیاری از Actionهای Netrunner با Interface + d10 حل می‌شوند. این d10 یک Check عادی RED است و Critical Success/Failure طبیعی روی آن کار می‌کند؛ پس نباید فقط یک عدد 1 تا 10 ساده بدون ادامهٔ رول باشد. DV یا Defense هدف بسته به Action تعیین می‌شود.",
            listOf("interface", "netrunning", "d10", "نت")
        ),
        RuleReferenceEntry(
            "net.black_ice", "Black ICE — بلک‌آیس", "Netrunning",
            "Black ICE برنامهٔ تهاجمی NET است و در Cyberdeck دو Slot می‌گیرد. فعال‌بودن، REZ و اثر هر ICE را جدا دنبال کن.",
            "Black ICE با Program معمولی یکی نیست. برای نصب در Cyberdeck دو Slot اشغال می‌کند و هنگام برخورد با هدف، قواعد Attack/Defense و Effect مخصوص خودش را دارد. GM بهتر است REZ، وضعیت فعال/غیرفعال و اثرهای ماندگار را جدا ثبت کند تا Encounter شبکه‌ای گم نشود.",
            listOf("black ice", "cyberdeck", "slots", "rez")
        ),
        RuleReferenceEntry(
            "fixer.haggle", "Haggle — چانه‌زنی فیکسر", "Roles & Economy",
            "Haggle یک Opposed Check در Operator است و جایزهٔ موفقیت با Rank فیکسر تغییر می‌کند؛ همیشه فقط «10٪ تخفیف» نیست.",
            "Operator فیکسر در Rankهای مختلف مزایای متفاوتی برای معامله می‌دهد. Rankهای پایین می‌توانند قیمت خرید یا فروش را بهتر کنند، Rankهای میانی روی خرید عمده یا دستمزد Job اثر می‌گذارند و Rankهای بالاتر مزایای قوی‌تری دارند. پس بعد از Haggle موفق، نتیجه باید واقعاً در Transaction اعمال شود، نه اینکه فقط یک پیام نمایش داده شود.",
            listOf("fixer", "operator", "haggle", "economy", "فیکسر")
        ),
        RuleReferenceEntry(
            "exec.teamwork", "Teamwork — تیم Exec", "Roles & Economy",
            "Exec از Rank 3 اولین Team Member را می‌گیرد، در Rank 5 عضو دوم و در Rank 9 عضو سوم؛ تعداد اعضا برابر خود Rank نیست.",
            "Teamwork مزیت اصلی Exec برای داشتن نیروهای سازمانی است. اولین Team Member در Rank 3 وارد می‌شود، Rank 5 ظرفیت را به دو نفر و Rank 9 به سه نفر می‌رساند. این افراد باید مثل منابع واقعی داستانی مدیریت شوند و صرفاً عددی برابر Role Rank نیستند.",
            listOf("exec", "teamwork", "team member", "اکزک")
        ),
        RuleReferenceEntry(
            "nomad.motorpool", "Family Motorpool — موتورپول Nomad", "Roles & Economy",
            "Nomad با Moto به خودروهای Family دسترسی پیدا می‌کند. قبل از Rank 10 معمولاً فقط یک Family Vehicle هم‌زمان بیرون است.",
            "Motorpool یعنی دسترسی به ناوگان خانواده، نه مالکیت شخصی نامحدود همهٔ خودروها. انتخاب خودرو با Rank و Upgradeهای Moto جلو می‌رود. تا قبل از Rank 10، محدودیت خودروهای هم‌زمان و زمان/شرایط تعویض باید رعایت شود؛ Rank 10 آزادی بیشتری برای بیرون‌داشتن ناوگان می‌دهد.",
            listOf("nomad", "moto", "motorpool", "vehicle", "نومد")
        ),
        RuleReferenceEntry(
            "vehicle.repair", "Vehicle Repair — تعمیر خودرو", "Vehicles",
            "Core برای Minor، Major و Destroyed Repair، DV و زمان جدا می‌دهد؛ مرز عددی مثل «زیر 50٪ SDP = Major» یک کمک‌ابزاری GM است نه قانون صریح Core.",
            "هنگام تعمیر وسیلهٔ نقلیه ابتدا نوع خرابی را مشخص کن، سپس DV و زمان همان سطح را استفاده کن. اگر برنامه برای سرعت کار بر اساس درصد SDP یک Severity پیشنهاد می‌دهد، آن را پیشنهاد GM در نظر بگیر و اجازه بده GM سطح واقعی را با توجه به صحنه تغییر دهد.",
            listOf("vehicle", "repair", "sdp", "تعمیر")
        ),
        RuleReferenceEntry(
            "cyberware.humanity", "Cyberware & Humanity — سایبرویر", "Gear & Cyberware",
            "نصب Cyberware می‌تواند Humanity را کم کند و بعضی قطعات سقف Humanity قابل‌بازیابی را هم پایین نگه می‌دارند.",
            "Cyberware فقط یک آیتم Inventory نیست. هنگام Install باید پیش‌نیاز، Option Slot، Pair بودن بعضی قطعات و Humanity Loss بررسی شود. Therapy می‌تواند Humanity را برگرداند، اما وجود Cyberware نصب‌شده می‌تواند Maximum قابل‌بازیابی را محدود کند. خریدن قطعه به‌تنهایی نباید اثر نصب را اعمال کند.",
            listOf("cyberware", "humanity", "install", "سایبرویر")
        ),
        RuleReferenceEntry(
            "gear.quality", "Weapon Quality — کیفیت سلاح", "Gear & Cyberware",
            "Poor، Standard و Excellent Quality روی قابلیت اطمینان/Check سلاح اثر دارند؛ Quality را جدا از Damage و Weapon Type در نظر بگیر.",
            "Quality ویژگی مستقل سلاح است. Excellent Quality برای Attack Check مزیت دارد و Poor Quality ریسک Jam/Fumble بیشتری ایجاد می‌کند. وقتی GM سلاح سفارشی یا NPC اختصاصی می‌سازد، Quality را آگاهانه تعیین کند و آن را با Exotic بودن یا Damage اشتباه نگیرد.",
            listOf("weapon", "quality", "excellent", "poor", "سلاح")
        ),
        RuleReferenceEntry(
            "core.skill_check", "Skill Check & Critical d10 — چک مهارت", "Core Checks",
            "در Check عادی: STAT + Skill + d10 + Modifier. روی 10 طبیعی یک d10 دیگر اضافه و روی 1 طبیعی یک d10 دیگر از نتیجه کم می‌شود.",
            "این موتور پایهٔ بیشتر Checkهای RED است. ابتدا Base را از STAT و Skill مناسب بساز، Modifierهای موقعیت و Wound را اعمال کن و d10 بینداز. اگر d10 اول 10 باشد، یک d10 دیگر به نتیجه اضافه می‌شود؛ اگر 1 باشد، یک d10 دیگر از نتیجه کم می‌شود. این قانون باید در همهٔ مسیرهای استاندارد Check یکسان باشد.",
            listOf("skill", "check", "critical success", "critical failure", "d10")
        ),
        RuleReferenceEntry(
            "core.dv", "DV & Opposed Check — سختی", "Core Checks",
            "بعضی Checkها باید به DV برسند یا از آن عبور کنند؛ بعضی دیگر Opposed هستند و نتیجهٔ دو طرف مستقیماً با هم مقایسه می‌شود.",
            "قبل از Roll مشخص کن Check از چه نوعی است. در Check ثابت، DV از جدول یا شرایط می‌آید. در Opposed Check، هر دو طرف Roll می‌کنند و نتیجه‌ها مقایسه می‌شوند. Tie و جزئیات خاص هر Action را از قانون همان Action بگیر؛ یک قانون کلی را به همهٔ موقعیت‌ها تحمیل نکن.",
            listOf("dv", "opposed", "check", "difficulty")
        ),
        RuleReferenceEntry(
            "character.creation_methods", "روش‌های ساخت شخصیت", "Character Creation",
            "Streetrat سریع و آماده است، Edgerunner انتخاب بیشتری می‌دهد و Complete Package بیشترین کنترل را روی ساخت شخصیت می‌دهد.",
            "سه روش ساخت برای سرعت‌ها و سطح کنترل متفاوت طراحی شده‌اند. Streetrat برای شروع سریع مناسب است، Edgerunner بخشی از انتخاب‌ها را به بازیکن می‌دهد و Complete Package اجازه می‌دهد امتیازهای STAT و Skill را مستقیم مدیریت کنی. روش ساخت را با تجربهٔ گروه و زمانی که برای Session Zero دارید انتخاب کن.",
            listOf("streetrat", "edgerunner", "complete package", "ساخت شخصیت")
        ),
        RuleReferenceEntry(
            "character.stats", "STATها — ویژگی‌های پایه", "Character Creation",
            "STATها توانایی‌های خام شخصیت‌اند؛ Skill Check معمولاً یک STAT مناسب را با Level مهارت و d10 ترکیب می‌کند.",
            "INT، REF، DEX، TECH، COOL، WILL، LUCK، MOVE، BODY و EMP پایهٔ بیشتر محاسبات شخصیت‌اند. بالا بودن یک STAT به‌تنهایی جای Skill را نمی‌گیرد. BODY روی دوام و آسیب تن‌به‌تن اثر دارد، MOVE روی حرکت، EMP با Humanity ارتباط دارد و LUCK یک منبع محدود برای بهترکردن بعضی Checkهاست.",
            listOf("stats", "ref", "dex", "body", "emp", "luck")
        ),
        RuleReferenceEntry(
            "character.skills", "Skills — مهارت‌ها", "Character Creation",
            "Level مهارت نشان می‌دهد شخصیت چقدر آموزش دیده است. در ساخت شخصیت بعضی مهارت‌های پایه حداقل Level مشخص دارند و Skillهای x2 هزینهٔ بیشتری دارند.",
            "هر Skill به یک STAT متصل است و Base آن معمولاً STAT + Skill است. Skillهای Difficult با علامت x2 گران‌تر پیشرفت می‌کنند. هنگام ساخت کاراکتر، مهارت‌های پایهٔ زندگی را فراموش نکن؛ برنامه باید اجازه ندهد با پایین‌آوردن آن‌ها امتیاز آزاد غیرقانونی ایجاد شود.",
            listOf("skills", "x2", "base skill", "مهارت")
        ),
        RuleReferenceEntry(
            "character.luck", "LUCK — شانس", "Character Creation",
            "LUCK یک STAT مصرف‌شدنی در طول Session است؛ می‌توانی قبل از Roll از امتیازهای باقی‌مانده برای بهترکردن Check استفاده کنی.",
            "LUCK با بقیهٔ STATها فرق دارد چون علاوه بر مقدار پایه، یک Pool مصرف‌شدنی دارد. بازیکن باید قبل از Roll اعلام کند چند امتیاز LUCK خرج می‌کند. Pool در زمان مناسب طبق قواعد بازی بازیابی می‌شود؛ پس آن را مثل Modifier دائمی حساب نکن.",
            listOf("luck", "pool", "شانس")
        ),
        RuleReferenceEntry(
            "character.hp", "HP — نقاط جان", "Character Creation",
            "HP از BODY و WILL به دست می‌آید و با پایین‌آمدنش Wound State تغییر می‌کند.",
            "HP فقط مقدار آسیب قابل‌تحمل نیست؛ مرزهای Seriously Wounded و Mortally Wounded را هم تعیین می‌کند. هنگام تغییر BODY یا WILL، Maximum HP را دوباره محاسبه کن و Current HP را طوری مدیریت کن که از Max بالاتر نرود.",
            listOf("hp", "body", "will", "health")
        ),
        RuleReferenceEntry(
            "character.humanity", "Humanity & EMP — انسانیت", "Character Creation",
            "Humanity سلامت روانی/انسانی شخصیت را دنبال می‌کند و با EMP ارتباط مستقیم دارد؛ Cyberware و بعضی اثرها می‌توانند آن را پایین بیاورند.",
            "Humanity را با HP اشتباه نکن. نصب Cyberware معمولاً Humanity Loss دارد و کاهش Humanity می‌تواند EMP را پایین بیاورد. Therapy می‌تواند بخشی از Humanity را برگرداند، اما Cyberware نصب‌شده سقف قابل‌بازیابی را محدود می‌کند. خرید Cyberware به‌تنهایی Humanity را کم نمی‌کند؛ نصب آن مهم است.",
            listOf("humanity", "emp", "therapy", "cyberpsychosis", "انسانیت")
        ),
        RuleReferenceEntry(
            "character.reputation", "Reputation — شهرت", "Character Creation",
            "Reputation نشان می‌دهد اسم شخصیت در خیابان‌ها چقدر شناخته شده است و در Faceoffها می‌تواند وارد بازی شود.",
            "Reputation یک Bonus دائمی برای همهٔ Social Checkها نیست. وقتی شخصیت‌ها برای ترساندن یا تحت‌تأثیر قراردادن هم با شهرت روبه‌رو می‌شوند، Faceoff و Reputation مرتبط می‌شود. مقدار Reputation را فقط وقتی تغییر بده که اتفاق‌های داستانی واقعاً شهرت شخصیت را عوض کرده باشند.",
            listOf("reputation", "faceoff", "شهرت")
        ),
        RuleReferenceEntry(
            "economy.eurodollars", "Eurodollars & Transactions — پول", "Roles & Economy",
            "€$ پول نقد/قابل‌خرج شخصیت است. خرید، فروش، Loot و هزینه‌های ماهانه باید به‌صورت Transaction روشن ثبت شوند.",
            "تغییر پول خودش Skill Check نیست. قیمت پایه، Quality، Haggle و Modifierهای GM را اول مشخص کن و بعد Transaction را ثبت کن. Loot رایگان می‌تواند به Inventory اضافه شود ولی نباید وانمود کند که خرید انجام شده است.",
            listOf("eurodollars", "money", "transaction", "eb", "پول")
        ),
        RuleReferenceEntry(
            "combat.initiative", "Initiative — ترتیب نوبت", "Combat",
            "در شروع درگیری ترتیب Actionها با Initiative مشخص می‌شود و معمولاً تا پایان همان Combat حفظ می‌شود.",
            "Initiative برای فهمیدن این است که چه کسی زودتر عمل می‌کند. Modifierهای Role Ability یا وضعیت‌ها را هنگام ساخت نتیجه اعمال کن. اگر چند نفر نتیجهٔ برابر دارند، Tie را با قاعدهٔ مربوط به Initiative حل کن، نه با تغییر دلخواه ترتیب در هر Round.",
            listOf("initiative", "turn", "round", "ابتکار")
        ),
        RuleReferenceEntry(
            "combat.action_move", "Action & Move — اکشن و حرکت", "Combat",
            "در Turn معمولاً یک Action و حرکت تا مقدار MOVE در اختیار داری؛ بعضی کارها Action را مصرف می‌کنند و بعضی نه.",
            "حرکت و Action را جدا دنبال کن. Draw کردن وسیلهٔ در دسترس معمولاً با Stow کردن یکسان نیست و بعضی تجهیزات استثنا دارند. Critical Injury، Grapple یا محیط می‌تواند MOVE را محدود کند؛ پس مقدار حرکت قابل‌استفاده همیشه فقط STAT خام MOVE نیست.",
            listOf("action", "move", "turn", "حرکت")
        ),
        RuleReferenceEntry(
            "combat.ranged_attack", "Ranged Attack — حملهٔ دوربرد", "Combat",
            "برای حملهٔ دوربرد Skill مناسب سلاح + REF + d10 را با DV برد یا دفاع هدف مقایسه می‌کنی.",
            "نوع سلاح تعیین می‌کند از Handgun، Shoulder Arms، Heavy Weapons، Archery یا Skill دیگری استفاده شود. Range Band روی DV اثر دارد. اگر هدف REF کافی داشته باشد و شرایط لازم برقرار باشد، ممکن است به‌جای DV ثابت بتواند Dodge کند.",
            listOf("ranged", "handgun", "shoulder arms", "dv", "برد")
        ),
        RuleReferenceEntry(
            "combat.melee_attack", "Melee & Brawling — تن‌به‌تن", "Combat",
            "حمله‌های Melee و Brawling معمولاً Opposed هستند و با دفاع Evade هدف مقایسه می‌شوند.",
            "Melee Weapon و Brawling کاربرد یکسان ندارند. Brawling برای مشت، لگد، Grapple و برخی Actionهای بدنی است؛ Melee Weapon برای سلاح نزدیک. Damage، ROF و تعامل با Armor را از نوع حمله بگیر و Modifierهای Injury یا Grapple را فراموش نکن.",
            listOf("melee", "brawling", "evasion", "تن به تن")
        ),
        RuleReferenceEntry(
            "combat.grapple", "Grab, Grapple & Choke — گرفتن", "Combat",
            "Grab با Brawling حل می‌شود و موفقیت می‌تواند هدف را Grappled کند؛ بعد از آن گزینه‌هایی مثل Choke یا Throw وارد بازی می‌شوند.",
            "Grapple یک وضعیت واقعی است و روی حرکت و بعضی Actionها اثر دارد. برای شروع Grab، Checkهای طرفین را درست مقایسه کن. رهاشدن، Throw و Choke هرکدام نتیجه و Damage مخصوص خودشان دارند؛ آن‌ها را یک Attack ساده تلقی نکن.",
            listOf("grab", "grapple", "choke", "throw")
        ),
        RuleReferenceEntry(
            "combat.hold_action", "Hold Action — نگه‌داشتن Action", "Combat",
            "Action را الآن خرج می‌کنی، اما از قبل Trigger یا جای Initiative، خود Action و Target را مشخص می‌کنی تا بعداً در همان Queue اجرا شود.",
            "Hold Action برای واکنش تعریف‌شده است، نه ذخیرهٔ آزاد Action. هنگام Hold باید دقیقاً بگویی چه اتفاق یا چه نقطه‌ای از Initiative آن را Trigger می‌کند، چه Actionی انجام می‌دهی و Target چه کسی/چیزی است. اگر Trigger مناسب رخ ندهد، Action خرج‌شده برنمی‌گردد.",
            listOf("hold", "initiative", "trigger", "action")
        ),
        RuleReferenceEntry(
            "combat.grab_item", "Grab Item — گرفتن وسیله از دست هدف", "Combat",
            "در Grab موفق می‌توانی به‌جای Grapple، یک وسیله‌ای را که Defender در دست دارد به دست آزاد خودت بگیری.",
            "برای Grab Item همان Opposed DEX + Brawling Check را انجام بده. اگر Attacker برنده شود و دست آزاد داشته باشد، می‌تواند یک شیء نگه‌داشته‌شدهٔ Defender را بگیرد. این انتخاب به‌جای شروع Grapple است؛ پس -2 Grapple و محدودیت Move ایجاد نمی‌شود.",
            listOf("grab", "held item", "brawling", "disarm")
        ),
        RuleReferenceEntry(
            "combat.choke_throw", "Choke & Throw — خفه‌کردن و پرتاب", "Combat",
            "هر دو نیاز به Grapple فعال دارند. Choke به‌اندازه BODY مستقیم HP می‌زند؛ Throw همان Damage مستقیم را می‌دهد، Grapple را تمام می‌کند و هدف را Prone می‌کند.",
            "Choke و Throw Armor را نادیده می‌گیرند و SP را ablate نمی‌کنند. Choke سه Round پیاپی روی همان هدف او را بیهوش می‌کند. Throw پس از Damage Grapple را تمام می‌کند و Target را Prone می‌گذارد. این‌ها Action هستند و فقط Attacker فعلی Grapple می‌تواند از آن‌ها استفاده کند.",
            listOf("choke", "throw", "grapple", "prone")
        ),
        RuleReferenceEntry(
            "combat.martial_recovery", "Recovery — برخاستن رزمی", "Combat",
            "هنگام Get Up می‌توانی Martial Arts Special Move Resolution را مقابل DV13 امتحان کنی؛ موفقیت باعث می‌شود Get Up Action مصرف نکند.",
            "Recovery برای همهٔ Martial Arts Formها در دسترس است. خود Get Up انجام می‌شود؛ سپس Resolution را می‌سنجی. اگر موفق شوی Action آزاد می‌ماند، اما اگر Fail شوی همان Get Up هزینهٔ Action عادی خودش را دارد و نمی‌توانی همان Turn بی‌نهایت دوباره امتحان کنی.",
            listOf("martial arts", "recovery", "get up", "prone")
        ),
        RuleReferenceEntry(
            "combat.flying_kick", "Flying Kick — ضربهٔ پرشی", "Combat",
            "MOVE 8+ و حداقل 4m حرکت قبلی می‌خواهد؛ باقی‌ماندهٔ Movement را خرج می‌کنی و Target باید حداکثر 4m جلوتر باشد.",
            "Flying Kick جای دو Martial Arts Attack عادی را می‌گیرد. پس از شرط MOVE و حرکت قبلی، تمام Movement باقی‌ماندهٔ Turn خرج می‌شود. روی Hit، Damage عادی Martial Arts وارد می‌شود و Target Prone می‌شود؛ اگر روی وسیلهٔ بدون کابین بسته باشد نیز می‌تواند از آن بیرون بیفتد.",
            listOf("taekwondo", "flying kick", "movement", "prone")
        ),
        RuleReferenceEntry(
            "vehicle.start_combat", "Start Vehicle in Combat — روشن‌کردن وسیله", "Vehicles",
            "Start Vehicle یک Action است؛ با روشن‌شدن وسیله MOVE آن را می‌گیری و راننده به بالای Initiative Queue می‌رود.",
            "در Combat، Start Vehicle فقط تغییر یک Boolean نیست. راننده Action را خرج می‌کند، وسیله فعال می‌شود و جای Initiative طبق قانون تغییر می‌کند. سیستم باید این جابه‌جایی Queue را هم ثبت کند تا ترتیب Turnها اشتباه نشود.",
            listOf("vehicle", "start", "initiative", "driver")
        ),
        RuleReferenceEntry(
            "vehicle.ram_pedestrian", "Ramming a Pedestrian — زیرگرفتن", "Vehicles",
            "فرد پیاده می‌تواند با DEX + Evasion مقابل DV13 جاخالی بدهد؛ در برخورد، 6d6 به دو طرف وارد می‌شود و افراد درگیر Whiplash می‌گیرند.",
            "Ramming فقط Vehicle-vs-Vehicle نیست. برای هدف پیاده ابتدا Dodge DV13 حل می‌شود. در برخورد، Damage تصادف و Whiplash اعمال می‌شوند و اگر عابر زنده بماند می‌تواند انتخاب کند روی وسیله باقی بماند. Upgradeهای وسیله مثل Combat Plow ممکن است سمت مهاجم را تغییر دهند.",
            listOf("ram", "pedestrian", "vehicle", "whiplash", "evasion")
        ),
        RuleReferenceEntry(
            "combat.reload", "Reload & Ammunition — خشاب و مهمات", "Combat",
            "مهمات باید واقعاً مصرف شود و Reload معمولاً Action است؛ بعضی سلاح‌های Exotic استثنا یا زمان بیشتری دارند.",
            "Magazine Capacity را جدا از Ammo ذخیره‌شده دنبال کن. Autofire و Suppressive Fire مصرف مهمات بیشتری دارند. اگر سلاح برای Reload دو Action یا شرط ویژه دارد، برنامه باید همان استثنا را حفظ کند و Reload استاندارد را روی آن تحمیل نکند.",
            listOf("reload", "ammo", "magazine", "خشاب")
        ),
        RuleReferenceEntry(
            "combat.martial_arts", "Martial Arts — هنرهای رزمی", "Combat",
            "Martial Arts از Form و Special Moveهای خودش استفاده می‌کند و هر Move شرط‌های جداگانه دارد.",
            "Special Move را فقط وقتی اجرا کن که شخصیت Form و Requirement لازم را دارد. بعضی Moveها Critical Injury مشخص ایجاد می‌کنند؛ اگر Critical Injury ایجاد شد، Bonus Damage و Effect آن Injury را نیز اعمال کن. بیشتر Special Moveها Aimed Shot نیستند مگر متن همان Move خلافش را بگوید.",
            listOf("martial arts", "special move", "karate", "judo")
        ),
        RuleReferenceEntry(
            "medical.stabilize", "Stabilization — پایدارسازی", "Medical",
            "Stabilize برای متوقف‌کردن روند مرگ و آماده‌کردن بیمار برای بهبود است؛ درمان Critical Injury موضوع جداگانه‌ای است.",
            "موفقیت در Stabilization به این معنی نیست که همهٔ Critical Injuryها درمان شده‌اند. بعد از پایدارشدن، Recovery و Treatmentهای لازم را جدا پیگیری کن. شرایط بیمار و Skill مناسب پزشک باید در Check لحاظ شوند.",
            listOf("stabilize", "first aid", "paramedic", "پایدارسازی")
        ),
        RuleReferenceEntry(
            "lawman.backup", "Backup — پشتیبان لاومن", "Role",
            "در خطر (یا پیش از Initiative، یا با یک Action وسط نبرد) یک d10 بریز: نتیجه مساوی یا کمتر از Rank یعنی کسی پاسخ می‌دهد. اگر جواب نداد، نوبت بعد می‌توانی دوباره تلاش کنی.",
            "بعد از پاسخ، یک 1d6 بریز: همان تعداد Rounds تا رسیدن نیروها. اگر آن تاس ۶ بیاید، به‌جای رده‌ی معمول، نیروی یک رده بالاتر می‌رسد (در Rank ۱۰ دو گروه). رده‌ها: ۱-۲ امنیت شرکتی، ۳-۴ پلیس محله، ۵-۷ کلانتر شهرستان، ۸ مارشال، ۹ C-SWAT، ۱۰ پلیس ملی. نیروهای Backup یک Combat Number واحد دارند که هم برای حمله و هم دفاع استفاده می‌شود و نمی‌توانند از گلوله Dodge کنند. Backup جایگزین اقدام‌های خودِ لاومن نمی‌شود و استفاده‌ی بی‌دلیل از آن می‌تواند پاسخ سازمان را سرد کند.",
            listOf("backup", "lawman", "backup call", "پشتیبان", "لاومن")
        ),
        RuleReferenceEntry(
            "media.credibility", "Credibility — اعتبار مدیا", "Role",
            "Media دو نوع استفاده دارد: Rumorهای passive (GM مخفیانه Rank + 1d10 می‌ریزد) و Investigationهای active (مهارت تحقیق + STAT در برابر DV همان سطح Rumor).",
            "نتیجه‌ی passive نشان می‌دهد Media بدون تلاش آگاهانه چه چیزی به گوشش می‌رسد؛ برای active باید Skill مناسب و Rumor Level را انتخاب کنی و DV همان سطح (Vague 13، Typical 15، Substantial 17، Detailed 21) را بزنی. Believability = Base Believability همان Rank + Evidence Bonus (حداقل یک مدرک قابل راستی‌آزمایی +1، بیش از چهار مدرک +2) و سقفش ۱۰ است. Rumor همیشه حقیقت نیست؛ فقط lead می‌دهد.",
            listOf("credibility", "media", "rumor", "believability", "مدیا", "اعتبار")
        ),
        RuleReferenceEntry(
            "medical.recovery", "Recovery — بازیابی HP", "Medical",
            "بعد از Stabilize شدن، شخصیت با گذشت روزها HP بازیابی می‌کند؛ Speedheal و درمان‌های دیگر می‌توانند روند را تغییر دهند.",
            "Recovery را از Treatment Critical Injury جدا نگه دار. ممکن است HP شخصیت بالا برود ولی Injury درمان‌نشده هنوز Effect خود را داشته باشد. اپ باید هم Current HP و هم وضعیت Treatment هر Injury را مستقل ثبت کند.",
            listOf("recovery", "hp", "speedheal", "ریکاوری")
        ),
        RuleReferenceEntry(
            "net.net_actions", "NET Actions — اکشن‌های شبکه", "Netrunning",
            "Netrunner در Turn خود تعداد مشخصی NET Action دارد و این Actionها با Meat Action یکی نیستند.",
            "Interface Rank تعیین می‌کند در Turn چند NET Action در دسترس است. حرکت در Floorها، Programها، Backdoor، Control، Slide و Virus را با هزینهٔ NET Action خودشان پیگیری کن. محدودیت فاصله از Access Point و وضعیت Jacked In نیز همیشه مهم است.",
            listOf("net actions", "interface", "turn", "netrunner")
        ),
        RuleReferenceEntry(
            "net.jack_in_out", "Jack In / Jack Out — ورود و خروج", "Netrunning",
            "برای Netrun باید در برد Access Point بمانی. Jack Out امن با قطع‌شدن ناگهانی ارتباط یکسان نیست.",
            "Netrunner باید موقعیت فیزیکی‌اش را همزمان با وضعیت NET دنبال کند. خارج‌شدن از برد، قطع ناگهانی یا ترک غیرایمن Architecture می‌تواند پیامد داشته باشد. برنامه بهتر است Jacked In بودن و Access Point فعال را واضح نشان دهد.",
            listOf("jack in", "jack out", "access point", "نت رانر")
        ),
        RuleReferenceEntry(
            "net.backdoor", "Backdoor — عبور از Password", "Netrunning",
            "Backdoor برای شکستن Passwordها و موانع دسترسی در NET Architecture استفاده می‌شود.",
            "وقتی به Password می‌رسی، Backdoor Check را با DV همان مانع حل کن. موفقیت اجازهٔ عبور می‌دهد؛ شکست به معنی حذف مانع از Architecture نیست. وضعیت هر Password را برای هر Netrunner یا Session به‌درستی ثبت کن.",
            listOf("backdoor", "password", "architecture")
        ),
        RuleReferenceEntry(
            "net.slide", "Slide — فرار از Black ICE", "Netrunning",
            "Slide برای جاگذاشتن Black ICE دنبال‌کننده است و یک Check رقابتی/اختصاصی دارد؛ فرار خودکار نیست.",
            "وقتی Black ICE به Netrunner می‌چسبد، صرف حرکت به Floor دیگر همیشه مشکل را حل نمی‌کند. Slide مکانیک مشخص فرار است. نتیجهٔ Check و این‌که کدام ICE دنبال‌کننده است باید جدا پیگیری شود.",
            listOf("slide", "black ice", "escape")
        ),
        RuleReferenceEntry(
            "net.virus", "Virus — تغییر پایدار معماری", "Netrunning",
            "Virus در پایین‌ترین بخش Architecture برای ایجاد یک تغییر تعریف‌شده در سیستم استفاده می‌شود و GM دامنهٔ اثر و DV را تعیین می‌کند.",
            "Virus یک دکمهٔ «هک کامل» نیست. بازیکن باید دقیق بگوید چه تغییری می‌خواهد، GM مناسب‌بودن آن را بررسی می‌کند و سپس DV/زمان لازم تعیین می‌شود. نتیجهٔ موفق را به‌صورت یادداشت پایدار برای Architecture ذخیره کن.",
            listOf("virus", "architecture", "netrunner")
        ),
        RuleReferenceEntry(
            "gear.weapon_types", "Weapon Types — نوع سلاح", "Gear & Cyberware",
            "نوع سلاح Damage، ROF، Skill، Range Table، Magazine و قابلیت‌هایی مثل Autofire را مشخص می‌کند.",
            "اسم ظاهری سلاح کافی نیست؛ مکانیک آن از Weapon Type می‌آید. Exotic Weapon می‌تواند قواعد متفاوتی داشته باشد. هنگام ساخت سلاح سفارشی NPC، مشخص کن کدام بخش Core است و کدام Effect اختصاصی GM.",
            listOf("weapons", "rof", "damage", "range")
        ),
        RuleReferenceEntry(
            "gear.armor", "Armor — زره و Penalty", "Gear & Cyberware",
            "Armor علاوه بر SP ممکن است Penalty داشته باشد. Head و Body Armor را جدا ثبت کن و Ablation هرکدام مستقل است.",
            "زره‌های سنگین‌تر می‌توانند REF، DEX یا MOVE را محدود کنند. این Penalty باید روی همهٔ مکانیک‌های مرتبط اعمال شود، نه فقط Attack. اگر Head Armor و Body Armor متفاوت‌اند، SP و Ablation هر بخش را جدا نگه دار.",
            listOf("armor", "sp", "penalty", "head armor")
        ),
        RuleReferenceEntry(
            "gear.ammo", "Special Ammunition — مهمات ویژه", "Gear & Cyberware",
            "Ammoهای ویژه Effect مخصوص دارند و معمولاً فقط با سلاح/نوع مهمات سازگار خودشان کار می‌کنند.",
            "Basic، Armor-Piercing، Incendiary، Rubber و انواع دیگر را یکسان حساب نکن. Effect هر Ammo هنگام Hit یا Damage اعمال می‌شود و ممکن است روی Ablation، Fire یا Critical Injury اثر متفاوتی داشته باشد. نوع مهمات Loaded را کنار Magazine ثبت کن.",
            listOf("ammo", "armor piercing", "incendiary", "مهمات")
        ),
        RuleReferenceEntry(
            "gear.cyberware_install", "Install Cyberware — نصب سایبرویر", "Gear & Cyberware",
            "خرید Cyberware با نصب آن یکی نیست؛ نصب باید Clinic/Facility، Surgery یا شرط‌های خود قطعه را رعایت کند.",
            "Inventory می‌تواند قطعهٔ خریداری‌شده را نگه دارد بدون اینکه Effect آن فعال باشد. هنگام Install پیش‌نیازهای Foundational Cyberware، Option Slot، Pair requirement و Humanity Loss را اعمال کن. Removal نیز باید وضعیت Slot و Humanity را درست به‌روزرسانی کند.",
            listOf("install", "cyberware", "surgery", "clinic")
        ),
        RuleReferenceEntry(
            "gear.cyberdeck", "Cyberdeck Slots — اسلات‌های دک", "Gear & Cyberware",
            "Program و Hardware داخل Cyberdeck Slot می‌گیرند؛ Black ICE دو Slot مصرف می‌کند.",
            "قبل از فعال‌کردن Programها ظرفیت Deck را بررسی کن. Slot مصرفی Hardware و Programها را جدا جمع بزن و اجازه نده Loadout از ظرفیت Deck بیشتر شود. Black ICE به‌دلیل ساختار خودش دو Slot می‌گیرد.",
            listOf("cyberdeck", "slot", "program", "hardware")
        ),
        RuleReferenceEntry(
            "gear.street_drugs", "Street Drugs — مواد خیابانی", "Gear & Cyberware",
            "هر Street Drug یک Primary Effect زمان‌دار و یک Secondary Effect با DV دارد؛ تمام‌شدن Primary یعنی زمان بررسی Secondary رسیده است.",
            "دوز را که مصرف می‌کنی Primary Effect فوراً فعال می‌شود و مدت خودش را دارد. بعد از پایان آن، Check مقاومت Secondary Effect با WILL + Resist Torture/Drugs انجام می‌شود. شکست می‌تواند Addiction یا Penalty ماندگار ایجاد کند. مصرف دوباره معمولاً درمان اعتیاد نیست؛ Therapy مسیر درمان است.",
            listOf("street drugs", "addiction", "secondary effect", "resist torture/drugs", "مواد")
        ),
        RuleReferenceEntry(
            "gear.weapon_attachments", "Weapon Attachments — اتچمنت سلاح", "Gear & Cyberware",
            "Attachmentها Slot محدود دارند و بعضی فقط روی نوع مشخصی از Ranged Weapon نصب می‌شوند؛ Exotic Weaponها معمولاً Attachment استاندارد نمی‌پذیرند.",
            "قبل از Install، سازگاری سلاح، تعداد Slotهای مصرف‌شده و محدودیت تکرار را چک کن. Extended و Drum Magazine هم‌زمان روی یک سلاح نصب نمی‌شوند و بعضی Underbarrelها دو Slot می‌گیرند. نصب Attachment نباید Damage یا نوع سلاح را بی‌دلیل عوض کند مگر متن خود Attachment بگوید.",
            listOf("attachment", "weapon mod", "smartgun", "magazine", "اتچمنت")
        ),
        RuleReferenceEntry(
            "gear.external_option_hosts", "Smart Glasses & Battleglove — میزبان Option", "Gear & Cyberware",
            "Smart Glasses و Battleglove می‌توانند بعضی Cyberware Optionها را بیرون بدن میزبانی کنند؛ Option فقط وقتی میزبان پوشیده/Equipped باشد قابل‌استفاده است.",
            "Battleglove سه Slot برای Cyberarm/Cyberlimb Option دارد و Smart Glasses دو Slot برای Cybereye Option. Option نصب‌شده در این وسایل داخل بدن کاشته نشده، بنابراین Humanity Loss نصب بدنی را ایجاد نمی‌کند. خود وسیله باید پوشیده یا Equipped باشد تا Optionهای داخلش در دسترس باشند.",
            listOf("smart glasses", "battleglove", "cyberware option", "slots")
        ),
        RuleReferenceEntry(
            "economy.price_categories", "Price Categories — ردهٔ قیمت", "Roles & Economy",
            "قیمت‌ها در RED علاوه بر عدد eb یک Category دارند؛ این Category روی دسترسی، ساخت، ارتقا و بازار اثر دارد.",
            "Cheap، Everyday، Costly، Premium، Expensive، Very Expensive، Luxury و Super Luxury فقط برچسب ظاهری نیستند. Fixer Reach، Tech Fabrication/Upgrade و Night Market ممکن است به Category وابسته باشند. وقتی قیمت تغییر می‌کند، Category اصلی آیتم را گم نکن.",
            listOf("price", "eb", "luxury", "economy")
        ),
        RuleReferenceEntry(
            "economy.lifestyle_housing", "Lifestyle & Housing — هزینهٔ ماهانه", "Roles & Economy",
            "Lifestyle و Housing دو هزینهٔ جدا هستند و در پایان ماه باید هر دو مدیریت شوند مگر Role/داستان استثنا بدهد.",
            "Housing محل زندگی است و Lifestyle سطح خرج روزمره، غذا و سرگرمی را پوشش می‌دهد. Corporate Housing یا منابع Role می‌توانند بخشی از هزینه را حذف کنند، اما به‌طور خودکار Lifestyle را رایگان نمی‌کنند. پرداخت ماهانه را به‌صورت Transaction واضح ثبت کن.",
            listOf("lifestyle", "housing", "rent", "ماهانه")
        ),
        RuleReferenceEntry(
            "economy.night_market", "Night Market — بازار شبانه", "Roles & Economy",
            "Night Market راهی برای دسترسی به کالاهایی است که همیشه در فروشگاه عادی پیدا نمی‌شوند و موجودی آن به Roll/جدول وابسته است.",
            "بازار شبانه را به فروشگاه نامحدود تبدیل نکن. دسته و تعداد کالاها بر اساس قواعد Market تعیین می‌شود و Fixer می‌تواند روی دسترسی اثر بگذارد. موجودی تولیدشده را برای همان Market نگه دار تا با هر بازشدن صفحه دوباره تغییر نکند.",
            listOf("night market", "market", "fixer", "بازار")
        ),
        RuleReferenceEntry(
            "role.solo", "Solo — Combat Awareness", "Roles & Economy",
            "Solo امتیازهای Combat Awareness را در شروع Combat بین قابلیت‌های رزمی خودش تخصیص می‌دهد.",
            "Solo برای انعطاف در نبرد طراحی شده است. Combat Awareness Rank یک Pool از Pointها می‌دهد که بین گزینه‌هایی مثل Initiative Reaction، Precision Attack، Spot Weakness و Threat Detection توزیع می‌شوند. برنامه باید Allocation فعال را برای همان Combat واضح نشان دهد.",
            listOf("solo", "combat awareness")
        ),
        RuleReferenceEntry(
            "role.rockerboy", "Rockerboy — Charismatic Impact", "Roles & Economy",
            "Rockerboy با Charismatic Impact می‌تواند طرفداران را تحت تأثیر قرار دهد؛ اندازهٔ گروه و قدرت اثر با Rank بالا می‌رود.",
            "Charismatic Impact جای همهٔ Social Skillها را نمی‌گیرد. اول مشخص کن مخاطب Fan است یا می‌تواند Fan شود، سپس Check و نتیجهٔ Rank را اعمال کن. درخواست‌های بزرگ‌تر یا خطرناک‌تر به Rank بالاتر نیاز دارند.",
            listOf("rockerboy", "charismatic impact", "fan")
        ),
        RuleReferenceEntry(
            "role.tech", "Tech — Maker", "Roles & Economy",
            "Maker به Tech اجازه می‌دهد Field Expertise، Upgrade، Fabricate و Invent را توسعه دهد؛ امتیازهای تخصص و زمان/هزینهٔ پروژه را جدا دنبال کن.",
            "Tech با Maker فقط یک Bonus ثابت نمی‌گیرد. تخصص‌ها روی Checkهای مشخص اثر دارند و ساخت/ارتقا به Price Category، مواد، DV و زمان وابسته است. Invent برای طراحی چیزی است که از قبل دستور ساخت آماده ندارد و نیازمند تأیید GM است.",
            listOf("tech", "maker", "fabricate", "upgrade", "invent")
        ),
        RuleReferenceEntry(
            "role.medtech", "Medtech — Medicine", "Roles & Economy",
            "Medicine Rank بین Surgery، Pharmaceuticals و Cryosystem Operation سرمایه‌گذاری می‌شود و Medical Tech از مجموع تخصص‌های مربوط بالا می‌رود.",
            "Medtech تنها Role دارای دسترسی واقعی به Surgery است. Pharmaceuticals امکان ساخت داروهای تخصصی و Cryosystem Operation دسترسی به تجهیزات سردخانه‌ای می‌دهد. Pointهای تخصص را با Rank Role اشتباه نکن و محدودیت هر تخصص را رعایت کن.",
            listOf("medtech", "medicine", "surgery", "pharmaceuticals")
        ),
        RuleReferenceEntry(
            "role.media", "Media — Credibility", "Roles & Economy",
            "Credibility تعیین می‌کند Media به چه منابعی دسترسی دارد و گزارشش تا چه حد باور و اثر ایجاد می‌کند.",
            "Media با جمع‌آوری شایعه و منبع، Story می‌سازد. Rank بالاتر دامنهٔ منابع، مخاطب و Believability را افزایش می‌دهد. خود انتشار گزارش به معنی تغییر فوری جهان نیست؛ GM اثر اجتماعی مناسب نتیجه و Rank را اعمال می‌کند.",
            listOf("media", "credibility", "rumor", "story")
        ),
        RuleReferenceEntry(
            "role.lawman", "Lawman — Backup", "Roles & Economy",
            "Lawman با Backup می‌تواند در خطر نیروی کمکی درخواست کند؛ پاسخ و زمان رسیدن به Roll و Rank وابسته است.",
            "Backup یک منبع نامحدود برای هر صحنه نیست. درخواست Action می‌خواهد، ممکن است کسی پاسخ ندهد و رسیدن نیرو زمان می‌برد. سوءاستفاده می‌تواند پیامد سازمانی داشته باشد. نوع گروه پشتیبان با Rank تعیین می‌شود.",
            listOf("lawman", "backup", "police")
        ),

        RuleReferenceEntry(
            "net.pathfinder", "Pathfinder — نقشه‌خوانی NET", "Netrunning",
            "با یک NET Action و Interface Check برای فهمیدن ساختار و چیزهایی که جلوتر در Architecture هستند استفاده می‌شود.",
            "Pathfinder برای جلو رفتن نیست؛ برای شناختن مسیر و محتویات NET Architecture است. نتیجهٔ Check مشخص می‌کند Netrunner تا چه حد از Floorها و موانع پیش رو باخبر می‌شود. Boosterهایی که Pathfinder را بهتر می‌کنند قبل از Roll حساب شوند.",
            listOf("pathfinder", "net action", "architecture")
        ),
        RuleReferenceEntry(
            "net.cloak", "Cloak — پنهان‌کردن ردپا", "Netrunning",
            "با Cloak رد اعمال و Virus خودت را در Architecture سخت‌تر برای پیدا شدن می‌کنی؛ دشمن باید با Pathfinder از امتیاز Cloak عبور کند.",
            "Cloak برای مخفی‌کردن کارهایی است که در NET انجام داده‌ای، مخصوصاً Virus. یک NET Action مصرف می‌کند و نتیجهٔ Interface Check امتیاز Cloak را می‌سازد. Eraser می‌تواند این Check را تقویت کند.",
            listOf("cloak", "eraser", "virus")
        ),
        RuleReferenceEntry(
            "net.control", "Control — کنترل Node", "Netrunning",
            "روی Control Node از Interface Check استفاده کن تا کنترل دستگاه یا سیستم وصل‌شده را بگیری؛ خودِ اثر دستگاه هنوز به شرایط Meatspace بستگی دارد.",
            "Control فقط به Netrunner اجازهٔ استفاده از Control Node را می‌دهد. گرفتن کنترل به معنی انجام خودکار هر کار ممکن نیست؛ دوربین، در، Turret یا دستگاه متصل قابلیت و محدودیت خودش را دارد. اگر Node قفل است اول مانع مربوط را حل کن.",
            listOf("control node", "control", "interface")
        ),
        RuleReferenceEntry(
            "net.eye_dee", "Eye-Dee — شناسایی File", "Netrunning",
            "برای فهمیدن File یا دادهٔ ناشناس از Eye-Dee استفاده می‌شود؛ بعد از شناسایی، کپی‌کردن File معمولاً NET Action جدا نمی‌خواهد.",
            "Eye-Dee کمک می‌کند Netrunner بفهمد File چیست و چه ارزشی/خطری دارد. شناسایی با Interface Check انجام می‌شود. برنامه می‌تواند وضعیت IDENTIFIED و COPIED را جدا نگه دارد تا GM بداند داده فقط دیده شده یا واقعاً برداشته شده است.",
            listOf("eye-dee", "file", "copy")
        ),
        RuleReferenceEntry(
            "net.zap", "Zap — حملهٔ پایه NET", "Netrunning",
            "Zap یک NET Action تهاجمی پایه است: Interface + d10 در برابر DEF + d10 هدف؛ در صورت موفقیت به REZ آسیب می‌زند.",
            "Zap برای آسیب‌زدن به Program یا Black ICE در فضای NET استفاده می‌شود. مثل Checkهای دیگر RED، d10 طبیعی 10/1 ادامهٔ Critical Success/Failure دارد. REZ را مثل HP برنامه دنبال کن؛ وقتی به صفر برسد هدف Derezzed می‌شود.",
            listOf("zap", "rez", "def")
        ),
        RuleReferenceEntry(
            "net.programs", "Programs — فعال‌سازی برنامه‌ها", "Netrunning",
            "Booster/Defenderها برای اثرگذاشتن باید Rezzed باشند و فعال/غیرفعال‌کردنشان NET Action می‌خواهد؛ Slot و محدودیت استفادهٔ هر Program را هم چک کن.",
            "Cyberdeck فقط داشتن Program را ثبت نمی‌کند؛ باید Slot، وضعیت Rezzed، REZ و محدودیت‌های خود Program مدیریت شوند. Black ICE دو Slot می‌گیرد و رفتار آن با Booster/Defender/Attacker متفاوت است.",
            listOf("program", "rezzed", "cyberdeck", "slots")
        ),
        RuleReferenceEntry(
            "net.unsafe_jack_out", "Unsafe Jack Out — خروج اجباری", "Netrunning",
            "اگر بدون Jack Out امن اتصال را قطع کنی، Black ICEهای درگیر می‌توانند اثر فرار/حملهٔ خود را اعمال کنند و Architecture برای ورود بعدی Reset می‌شود.",
            "Unsafe Jack Out گزینهٔ اضطراری است. قبل از زدن آن ببین چه Black ICEهایی هنوز با Netrunner درگیرند، چون خروج ناامن می‌تواند فوراً اثرهای خطرناک ایجاد کند. بعد از خروج، وضعیت محلی Session باید مطابق Reset معماری پاک شود.",
            listOf("unsafe jack out", "black ice", "reset")
        ),
        RuleReferenceEntry(
            "role.fixer", "Fixer — Operator", "Roles & Economy",
            "Operator فقط Haggle نیست؛ Contacts، Reach، Grease و دسترسی به کالا/خدمات کمیاب را با Rank بهتر می‌کند.",
            "Fixer واسطهٔ بازار و آدم‌هاست. Rank Operator مشخص می‌کند به چه سطحی از کالا و ارتباطات دسترسی داری و چه مزیت‌هایی از Haggle می‌گیری. مزایا را به شکل امتیاز قابل‌تقسیم بین چند زیرمهارت حساب نکن؛ هر Rank بستهٔ مزایای خودش را باز می‌کند.",
            listOf("fixer", "operator", "contacts", "reach", "grease")
        ),
        RuleReferenceEntry(
            "role.exec", "Exec — Teamwork", "Roles & Economy",
            "Teamwork علاوه بر Team Memberها مزایای شرکتی مثل Housing و Insurance را در Rankهای مشخص باز می‌کند.",
            "Exec منابع سازمانی دارد، نه یک Pool آزاد برابر Rank. Team Memberها در Rankهای مشخص اضافه می‌شوند و مزایای مسکن/بیمه هم در Rankهای مشخص ارتقا پیدا می‌کنند. GM باید حضور و دسترسی واقعی این منابع در داستان را هم در نظر بگیرد.",
            listOf("exec", "teamwork", "housing", "insurance")
        ),
        RuleReferenceEntry(
            "role.nomad", "Nomad — Moto", "Roles & Economy",
            "Moto دسترسی به Family Motorpool و Upgradeها را باز می‌کند؛ قبل از Rank 10 معمولاً فقط یک Family Vehicle هم‌زمان در اختیار است.",
            "Nomad با بالا رفتن Moto گزینه‌های Motorpool و Upgrade بیشتری می‌گیرد. خودروهای Family را با مالکیت شخصی اشتباه نکن. تعویض خودرو و تعداد خودروهای بیرون از Motorpool محدودیت داستانی/زمانی دارد و باید در ابزار GM قابل مشاهده باشد.",
            listOf("nomad", "moto", "family motorpool")
        ),
        RuleReferenceEntry(
            "vehicle.driving", "Driving & Control — رانندگی", "Vehicles",
            "در شرایط عادی رانندگی ساده ممکن است Check نخواهد، اما مانور سخت، سرعت، آسیب یا تعقیب می‌تواند Control Skill Check لازم کند.",
            "نوع وسیله مشخص می‌کند از Drive Land Vehicle، Pilot Air Vehicle، Pilot Sea Vehicle یا Skill دیگر استفاده شود. Critical Injury راننده، شرایط جاده و Damage وسیله می‌توانند روی Check اثر بگذارند. GM فقط در موقعیت معنادار Roll بخواهد.",
            listOf("driving", "vehicle", "control", "رانندگی")
        ),
        RuleReferenceEntry(
            "vehicle.ramming", "Ramming — تصادف عمدی", "Vehicles",
            "Ramming به وسیله، هدف و سرنشینان آسیب می‌زند و می‌تواند Whiplash ایجاد کند؛ Upgradeهایی مثل Combat Plow ممکن است این نتیجه را تغییر دهند.",
            "هنگام Ram، Damage وسیله و هدف را جدا اعمال کن و وضعیت سرنشینان را هم بررسی کن. اگر Vehicle Upgrade استثنا می‌دهد، همان استثنا را قبل از اعمال نتیجهٔ استاندارد حساب کن. توقف یا ادامهٔ حرکت به نتیجهٔ برخورد وابسته است.",
            listOf("ramming", "whiplash", "combat plow", "تصادف")
        )
    )

    private val criticalEntries: List<RuleReferenceEntry> = CriticalInjuries.all.map { injury ->
        RuleReferenceEntry(
            key = "critical.${injury.key}",
            title = "${injury.enName} — ${injury.faName}",
            category = "Critical Injuries",
            short = injury.effectFa,
            full = buildString {
                append(injury.effectFa)
                append("\n\nQuick Fix: ")
                append(injury.quickFixFa)
                append("\nTreatment: ")
                append(injury.treatmentFa)
                append("\n\nیادآوری: هنگام ایجاد Critical Injury، 5 Bonus Damage مستقیم به HP وارد می‌شود.")
            },
            tags = listOf(injury.enName, injury.faName, if (injury.body) "body" else "head", "critical injury")
        )
    }

    private fun StoreItem.referenceKey(): String = "item.${category.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}.${name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}"

    private val storeEntries: List<RuleReferenceEntry> = StoreCatalog.items.map { item ->
        val homebrew = item.subtitle.contains("HOME BREW", ignoreCase = true) || item.description.contains("Homebrew", ignoreCase = true)
        val facts = buildList {
            if (item.basePrice > 0) add("قیمت پایه: ${item.basePrice}eb")
            if (item.damage.isNotBlank()) add(gtr("Damage: %1s", item.damage))
            if (item.weaponSkill.isNotBlank()) add(gtr("Skill: %1s", item.weaponSkill))
            if (item.rof.isNotBlank()) add(gtr("ROF: %1s", item.rof))
            if (item.ammoType.isNotBlank()) add(gtr("Ammo: %1s", item.ammoType))
            if (item.magStd != null) add(gtr("Magazine: %1s", item.magStd))
            if (item.autofire.isNotBlank()) add(gtr("Autofire: %1s", item.autofire))
            if (item.slot.isNotBlank()) add(gtr("Install: %1s", item.slot))
            if (item.humanityLoss != null) add(gtr("Humanity Loss: %1s", item.humanityLoss))
            if (item.programClass.isNotBlank()) add(gtr("Class: %1s", item.programClass))
            if (item.rez != null) add(gtr("REZ: %1s", item.rez))
        }
        RuleReferenceEntry(
            key = item.referenceKey(),
            title = item.name + if (homebrew) gtr(" — Homebrew") else "",
            category = if (homebrew) gtr("Homebrew / Custom") else gtr("Items — %1s", item.category),
            short = ItemDetailsCatalog.fullDescription(item).lineSequence().firstOrNull().orEmpty().ifBlank { item.subtitle.ifBlank { "اطلاعات مکانیکی این آیتم در Catalog ثبت شده است." } },
            full = buildString {
                append(ItemDetailsCatalog.fullDescription(item).ifBlank { "برای این آیتم توضیح تفصیلی ثبت نشده است." })
                if (facts.isNotEmpty()) {
                    append("\n\n")
                    append(facts.joinToString(" • "))
                }
                if (item.modes.isNotBlank()) { append("\n\nکاربرد/Mode: "); append(item.modes) }
                if (homebrew) append("\n\nاین آیتم در پروژه به‌صراحت Homebrew/Adaptation علامت خورده و نباید با قانون Core اشتباه گرفته شود.")
            },
            tags = listOf(item.name, item.category, item.subtitle, if (homebrew) "homebrew" else "core item").filter { it.isNotBlank() }
        )
    }

    val allEntries: List<RuleReferenceEntry> = entries + criticalEntries + storeEntries
    private val byKey = allEntries.associateBy { it.key }
    private val storeByNameCategory = StoreCatalog.items.zip(storeEntries).associate { (item, entry) -> (item.name.lowercase() to item.category.lowercase()) to entry }
    fun get(key: String): RuleReferenceEntry? = byKey[key]
    fun getForStoreItem(name: String, category: String): RuleReferenceEntry? = storeByNameCategory[name.lowercase() to category.lowercase()]
    val categories: List<String> = allEntries.map { it.category }.distinct()
}
