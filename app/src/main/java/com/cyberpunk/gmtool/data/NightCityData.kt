package com.cyberpunk.gmtool.data

import com.cyberpunk.gmtool.R

/**
 * داده‌ی نقشه‌ی آفلاین نایت‌سیتی — نقشه‌ها به‌صورت تصویر در res/drawable باندل شده‌اند
 * (map_city + ۸ منطقه). مختصات هات‌اسپات‌ها نرمالایزشده (۰..۱) نسبت به خود تصویر نقشه است
 * و تقریبی است؛ بعداً قابل کالیبره دقیق.
 */

enum class PlaceType(val fa: String) {
    CLINIC("درمانگاه/کلینیک"),
    BAR("بار/کلاب"),
    FOOD("رستوران/اغذیه"),
    GUN("فروشگاه اسلحه"),
    GARAGE("تعمیرگاه/کاراژ"),
    MARKET("بازار/فروشگاه"),
    HOTEL("هتل/خانه‌امن"),
    METRO("ایستگاه مترو"),
    CAR("بنگاه خودرو"),
    BANK("بانک/خودپرداز"),
    PARKING("پارکینگ"),
    RIPPER("ریپرداک"),
    CORP("سایت شرکتی"),
    GANG("مقر گنگ"),
    DANGER("نقطه خطرناک"),
    LANDMARK("محله/نماد"),
    STADIUM("استادیوم"),
    MALL("مرکز خرید"),
    PARK("پارک"),
    PORT("بندر/اسکله"),
    CAMP("اردوگاه/کمپ"),
    STATION("پایگاه/ایستگاه"),
    SITE("معدن/تأسیسات")
}

data class MapPlace(
    val num: Int,
    val nameEn: String,
    val nameFa: String,
    val type: PlaceType,
    val desc: String,
    val important: Boolean = false,
    val x: Float = -1f,   // مختصات نرمالایز هات‌اسپات روی نقشه (-۱ = بدون هات‌اسپات)
    val y: Float = -1f
)

data class District(
    val id: String,
    val num: String,
    val nameEn: String,
    val nameFa: String,
    val mapRes: Int,
    val slogan: String,
    val area: String,
    val pop: String,
    val living: String,
    val security: String,
    val summary: String,
    val subAreas: List<Pair<String, String>>,
    val gangs: List<String>,
    val corps: List<String>,
    val places: List<MapPlace>,
    val districtHotspots: List<Triple<Float, Float, Int>> = emptyList() // x,y,indexDistrict (فقط نقشه‌ی کل شهر)
)

object NightCityData {

    private val cityPlaces = listOf(
        MapPlace(1, "Arasaka Tower", "برج آراساکا", PlaceType.CORP, "مرکز آراساکا در سیتی سنتر.", true),
        MapPlace(2, "Militech Plaza", "پلازا میلی‌تک", PlaceType.CORP, "ستاد میلی‌تک.", true),
        MapPlace(3, "Night City Hall", "شهرداری نایت‌سیتی", PlaceType.LANDMARK, "ساختمان شهرداری."),
        MapPlace(4, "NCPD HQ", "ستاد NCPD", PlaceType.STATION, "مقر پلیس شهر."),
        MapPlace(5, "Trauma Team Center", "مرکز تروما تیم", PlaceType.CLINIC, "پایگاه آمبولانس زرهی."),
        MapPlace(6, "Afterlife", "افترلایف", PlaceType.BAR, "بار افسانه‌ای مزدوران؛ محل بستن گیگ‌های بزرگ.", true),
        MapPlace(7, "Lizzie's Bar", "بار لیزی", PlaceType.BAR, "بار The Mox در واتسون/کابوکی.", true),
        MapPlace(8, "Clouds", "کلاودز", PlaceType.BAR, "کلاب لوکس دلبران در وست‌بروک.", true),
        MapPlace(9, "The Heavy Hearts Club", "کلاب هوی هارتس", PlaceType.BAR, "کلاب معروف پاسیفیکا."),
        MapPlace(10, "No-Tell Motel", "متل نو-تل", PlaceType.HOTEL, "متل قرارهای مخفی در پاسیفیکا."),
        MapPlace(11, "The Mox", "گروه موکس", PlaceType.BAR, "گنگِ صاحب بار لیزی."),
        MapPlace(12, "Voodoo Boys NetCafe", "نت‌کافه وودو بویز", PlaceType.LANDMARK, "پاتوق نت‌رانرهای پاسیفیکا."),
        MapPlace(13, "Megabuilding H10", "مگابیلدینگ H10", PlaceType.LANDMARK, "آپارتمان غول‌پیکر وست‌بروک."),
        MapPlace(14, "Charter Hill Mansion", "منشن چارتر هیل", PlaceType.LANDMARK, "عمارت لوکس تپه‌ها."),
        MapPlace(15, "Japantown Market", "بازار ژاپن‌تاون", PlaceType.MARKET, "بازار ژاپنی وست‌بروک."),
        MapPlace(16, "Kabuki Market", "بازار کابوکی", PlaceType.MARKET, "بازار قدیمی واتسون."),
        MapPlace(17, "Rancho Coronado Clinic", "کلینیک رانچو کورونادو", PlaceType.CLINIC, "درمانگاه سانتو دومینگو."),
        MapPlace(18, "Dogtown Entrance", "ورودی داگ‌تاون", PlaceType.DANGER, "مرز شهر محصورِ بارگِست در پاسیفیکا.", true),
        MapPlace(19, "Delamain HQ", "دفتر دلامین", PlaceType.CORP, "شرکت تاکسی خودران."),
        MapPlace(20, "Biotechnica Tower", "برج بیوتکنیکا", PlaceType.CORP, "ستاد بیوتکنیکا."),
        MapPlace(21, "Petrochem Refinery", "پالایشگاه پتروکم", PlaceType.SITE, "تأسیسات سوخت سانتو/بدلندز."),
        MapPlace(22, "Columbus Park", "پارک کلمبوس", PlaceType.PARK, "پارک شهری."),
        MapPlace(23, "Pacifica Playgrounds", "زمین بازی پاسیفیکا", PlaceType.PARK, "پارک مخروبه ساحلی."),
        MapPlace(24, "West Wind Estate", "استیت وست ویند", PlaceType.LANDMARK, "محله مسکونی پاسیفیکا."),
        MapPlace(25, "Vista del Rey Marina", "مارینا ویستا دل ری", PlaceType.PORT, "بندر قایق‌های تفریحی."),
        MapPlace(26, "6th Street Market", "بازار خیابان ششم", PlaceType.MARKET, "بازار سانتو دومینگو."),
        MapPlace(27, "Arroyo Clinic", "کلینیک آرویو", PlaceType.CLINIC, "درمانگاه منطقه صنعتی."),
        MapPlace(28, "Longshore Stackyard", "محوطه بارگیری", PlaceType.PORT, "بندرگاه داکس جنوبی."),
        MapPlace(29, "NCX Spaceport", "فرودگاه فضایی NCX", PlaceType.PORT, "پایانه فضایی خارج شهر.", true),
        MapPlace(30, "Crystal Palace", "کریستال پالاس", PlaceType.LANDMARK, "ایستگاه/سازه فضایی لوکس.")
    )

    val districts = listOf(
        // 0 — کل شهر
        District(
            id = "city", num = "00", nameEn = "NIGHT CITY", nameFa = "نایت‌سیتی",
            mapRes = R.drawable.map_city,
            slogan = "The City of Dreams",
            area = "223.7 km²", pop = "6,890,000", living = "—", security = "—",
            summary = "کلان‌شهری آزاد در ساحل غربی آمریکای شمالی؛ هشت منطقه به‌علاوه‌ی بدلندز. " +
                "قدرت میان گنگ‌ها، مگاکورپ‌ها و مزدوران تقسیم شده. روی نقشه روی هر منطقه بزن تا نقشه‌ی اختصاصی‌اش باز شود.",
            subAreas = listOf(
                "Watson" to "واتسون", "Northside" to "نورث‌ساید", "Westbrook" to "وست‌بروک",
                "City Center" to "سیتی سنتر", "Heywood" to "هیوود", "Santo Domingo" to "سانتو دومینگو",
                "Pacifica" to "پاسیفیکا", "Badlands" to "بدلندز"
            ),
            gangs = listOf("Tyger Claws", "Maelstrom", "6th Street", "Valentinos", "Voodoo Boys", "Animals", "Barghest", "Aldecaldos"),
            corps = listOf("Arasaka", "Militech", "Biotechnica", "Zetatech", "Kang Tao", "Petrochem", "Trauma Team", "NCPD"),
            places = cityPlaces,
            // هات‌اسپات روی نقشه‌ی شهر: ضربه روی هر منطقه → باز شدن نقشه‌ی آن منطقه (ایندکس در لیست districts)
            districtHotspots = listOf(
                Triple(0.13f, 0.14f, 2), // Northside (بالا-چپ)
                Triple(0.30f, 0.09f, 1), // Watson (بالا)
                Triple(0.12f, 0.40f, 4), // Westbrook (چپ)
                Triple(0.29f, 0.36f, 3), // City Center (مرکز)
                Triple(0.37f, 0.56f, 5), // Heywood (پایین-مرکز)
                Triple(0.55f, 0.55f, 6), // Santo Domingo (راست-پایین)
                Triple(0.19f, 0.68f, 7), // Pacifica (پایین-چپ)
                Triple(0.66f, 0.14f, 8)  // Badlands (راست-بالا، خارج شهر)
            )
        ),
        District(
            id = "watson", num = "01", nameEn = "WATSON", nameFa = "واتسون",
            mapRes = R.drawable.map_watson,
            slogan = "Old Town, New Blood",
            area = "28.7 km²", pop = "1,150,000", living = "متوسط رو به بالا", security = "متوسط رو به بالا",
            summary = "قدیمی‌ترین و پرجمعیت‌ترین منطقه‌ی نایت‌سیتی؛ خانه‌ی مهاجران، بازارهای شلوغ، کلینیک‌های ریپرداک و " +
                "کارگاه‌های کوچک. ورودی بندر و پارک صنعتی آراساکا اینجاست. شب‌ها قلمرو تایگر کلاز است.",
            subAreas = listOf(
                "Kabuki" to "کابوکی", "Little China" to "چاینا کوچک", "North Oak Ave" to "بلوار نورث اوک",
                "Night City Harbor" to "بندر نایت‌سیتی", "Arasaka Industrial Park" to "پارک صنعتی آراساکا"
            ),
            gangs = listOf("Tyger Claws", "6th Street", "Maelstrom", "Voodoo Boys"),
            corps = listOf("Arasaka", "Militech", "Trauma Team"),
            places = listOf(
                MapPlace(1, "Watson Clinic", "کلینیک واتسون", PlaceType.CLINIC, "درمانگاه عمومی منطقه؛ خدمات اولیه و پایدارکردن ارزان.", true, 0.42f, 0.30f),
                MapPlace(2, "Kabuki Market", "بازار کابوکی", PlaceType.MARKET, "بازار تو‌در‌توی کابوکی؛ هر چیزی از قطعه تا غذا و کروم دست‌دوم پیدا می‌شود.", true, 0.30f, 0.44f),
                MapPlace(3, "24/7 Food Vendor", "رستوران فودلر ۲۴/۷", PlaceType.FOOD, "اغذیه‌فروشی شبانه‌روزی؛ پاتوق سریع برای وعده بین گیگ‌ها."),
                MapPlace(4, "Cazador Gun Shop", "فروشگاه اسلحه کاسادور", PlaceType.GUN, "اسلحه‌فروشی محلی؛ برای خرید تسلیحات سبک و مهمات."),
                MapPlace(5, "Dr. Chiro Ripperdoc", "ریپرداک دکتر چیروجی", PlaceType.RIPPER, "کلینیک نصب سایبرویر؛ کروم ارزان و نیمه‌قانونی نصب می‌کند."),
                MapPlace(6, "Nabuki Garage", "تعمیرگاه نایبوکی", PlaceType.GARAGE, "کاراژ تعمیر خودرو و موتور."),
                MapPlace(7, "Sleep Evern Hotel", "هتل اسلیپ اِورن", PlaceType.HOTEL, "مسافرخانه ارزان؛ اتاق ساعتی برای استراحت یا مخفی‌شدن."),
                MapPlace(8, "Nova Bar & Club", "بار و کلاب نووا", PlaceType.BAR, "بار محله؛ شنیدن شایعات و پیدا کردن فیکسر."),
                MapPlace(9, "Salt Bed Clinic", "کلینیک سالت بد", PlaceType.CLINIC, "کلینیک کوچک دیگر منطقه."),
                MapPlace(10, "Neftkudo Electronics", "فروشگاه الکترونیک نفت‌کودو", PlaceType.MARKET, "قطعه و گجت الکترونیکی."),
                MapPlace(11, "Multi-Storey Parking", "پارکینگ چند طبقه", PlaceType.PARKING, "پارکینگ عمومی؛ محل رایج کمین و معامله."),
                MapPlace(12, "Byte City Site", "سایت بایت‌سیتی", PlaceType.LANDMARK, "نقطه شهری معروف."),
                MapPlace(13, "Night Market Fast Food", "فست‌فود نایت‌مارکت", PlaceType.FOOD, "ردیف اغذیه در بازار شبانه."),
                MapPlace(14, "NCPD Watson Precinct", "مرکز پلیس NCPD واتسون", PlaceType.STATION, "کلانتری محلی؛ حضور پلیس در این منطقه محسوس‌تر است."),
                MapPlace(15, "Matsuo Garage", "گاراژ ماتسوو", PlaceType.GARAGE, "تعمیرگاه خودرو."),
                MapPlace(16, "Black Sky Bar", "بار بلک اسکای", PlaceType.BAR, "بار دیگر محله."),
                MapPlace(17, "Yukimura Inn", "مسافرخانه یوکیمورا", PlaceType.HOTEL, "اقامتگاه ارزان."),
                MapPlace(18, "Kabuki Clothing Store", "فروشگاه لباس کایوکی", PlaceType.MARKET, "لباس و استت خیابانی."),
                MapPlace(19, "China Market", "چاینا مارکت", PlaceType.MARKET, "بازار چاینا کوچک؛ اجناس وارداتی.", true, 0.44f, 0.56f),
                MapPlace(20, "Kabuki Kids Clinic", "کلینیک کودکان کایوکی", PlaceType.CLINIC, "درمانگاه محله."),
                MapPlace(21, "Kabuki Metro Station", "ایستگاه مترو کایوکی", PlaceType.METRO, "ایستگاه مترو برای جابه‌جایی سریع."),
                MapPlace(22, "Watson Motors", "بنگاه ماشین واتسون موتورز", PlaceType.CAR, "خرید/فروش خودرو."),
                MapPlace(23, "North Docks", "اسکله شمالی", PlaceType.PORT, "بندرگاه؛ قاچاق و ورود غیرقانونی کالا.", true, 0.60f, 0.74f),
                MapPlace(24, "Arasaka Warehouse", "انبار آراساکا", PlaceType.CORP, "انبار و تأسیسات آراساکا در پارک صنعتی.", true, 0.12f, 0.62f),
                MapPlace(25, "Lizzie's Bar", "بار لیزی", PlaceType.BAR, "بار نمادینِ گروه The Mox در کابوکی؛ پاتوق نت‌رانرها و فیکسرها.")
            ),
            districtHotspots = emptyList()
        ),
        District(
            id = "northside", num = "02", nameEn = "NORTHSIDE", nameFa = "نورث‌ساید",
            mapRes = R.drawable.map_northside,
            slogan = "Northside Never Sleeps",
            area = "15.6 km²", pop = "1,280,000", living = "پایین تا متوسط", security = "پایین",
            summary = "قلب طبقه کارگر؛ انبارها، کارگاه‌های قدیمی و منطقه‌ی صنعتی. قلمرو اصلی مِیل‌استروم؛ " +
                "محل کارخانه‌های سابق All Foods و خیابان‌هایی که پلیس کمتر واردشان می‌شود.",
            subAreas = listOf(
                "Old Northside" to "نورث‌ساید قدیم", "North Oak" to "نورث اوک",
                "Industrial Zone" to "منطقه صنعتی", "Junction" to "جانکشن",
                "Corporate Yards" to "یاردهای شرکتی", "The Stacks" to "د استکس",
                "Bordertown" to "بوردرتاون", "Arasaka Waterfront" to "اسکله آراساکا"
            ),
            gangs = listOf("Maelstrom", "Tyger Claws", "6th Street", "Voodoo Boys"),
            corps = listOf("Arasaka", "Militech"),
            places = listOf(
                MapPlace(1, "Northside Hotel", "هتل نورث‌ساید", PlaceType.HOTEL, "اقامتگاه ارزان کارگری."),
                MapPlace(2, "Cloud Nine Bar", "بار کلود ناین", PlaceType.BAR, "بار منطقه."),
                MapPlace(3, "Street Chow", "رستوران استریت چاو", PlaceType.FOOD, "اغذیه خیابانی."),
                MapPlace(4, "Northside Market", "بازار نورث‌ساید", PlaceType.MARKET, "بازار محلی."),
                MapPlace(5, "Doc Rios Clinic", "کلینیک داک ریوس", PlaceType.CLINIC, "ریپرداک/درمانگاه محله."),
                MapPlace(6, "Gun-O-Matic", "فروشگاه اسلحه گان-اوماتیک", PlaceType.GUN, "اسلحه‌فروشی."),
                MapPlace(7, "AutoFix Garage", "تعمیرگاه آتوفیکس", PlaceType.GARAGE, "کاراژ خودرو.", true, 0.30f, 0.30f),
                MapPlace(8, "Ironworks Garage", "کاراژ آیرون‌ورکس", PlaceType.GARAGE, "تعمیرگاه صنعتی."),
                MapPlace(9, "Night Market", "نایت‌مارکت", PlaceType.MARKET, "بازار شبانه."),
                MapPlace(10, "The Rusty Spoke", "بار رستی اسپوک", PlaceType.BAR, "بار موتورسوارها."),
                MapPlace(11, "Noodle Stand", "فست‌فود نودل استند", PlaceType.FOOD, "نودل‌فروشی."),
                MapPlace(12, "Northside Metro Station", "ایستگاه مترو نورث‌ساید", PlaceType.METRO, "مترو."),
                MapPlace(13, "Herbline Clinic", "کلینیک هربلاین", PlaceType.CLINIC, "درمانگاه."),
                MapPlace(14, "Fuel Depot", "انبار سوخت", PlaceType.SITE, "پمپ بنزین/انبار سوخت."),
                MapPlace(15, "Afterlife North", "افترلایف (شعبه نورث)", PlaceType.BAR, "بار مزدوران؛ قرار گیگ‌های بزرگ اینجا بسته می‌شود.", true, 0.55f, 0.22f),
                MapPlace(16, "24/7 Store", "فروشگاه ۲۴/۷", PlaceType.MARKET, "سوپرمارکت شبانه‌روزی."),
                MapPlace(17, "Carniceria Cortes", "رستوران کانیسریا کورتس", PlaceType.FOOD, "رستوران محلی."),
                MapPlace(18, "Junction Metro", "مترو جانکشن", PlaceType.METRO, "ایستگاه تقاطع."),
                MapPlace(19, "The Glitch Bar", "بار گلیچ", PlaceType.BAR, "بار نت‌رانرها."),
                MapPlace(20, "Northside Bank", "بانک نورث‌ساید", PlaceType.BANK, "بانک/خودپرداز."),
                MapPlace(21, "Buck-A-Bite Diner", "رستوران باک-ا-بایت", PlaceType.FOOD, "داینر ارزان."),
                MapPlace(22, "Chrome Deals", "فروشنده وسایل نقلیه کروم دیلز", PlaceType.CAR, "بنگاه خودرو."),
                MapPlace(23, "Border Inn", "مسافرخانه بوردر این", PlaceType.HOTEL, "اقامتگاه مرزی."),
                MapPlace(24, "Rifle & Thing", "فروشگاه اسلحه رایل اند تینگ", PlaceType.GUN, "اسلحه‌فروشی."),
                MapPlace(25, "Maelstrom Hideout", "مخفیگاه مِیل‌استروم", PlaceType.GANG, "مقر گنگ مِیل‌استروم در منطقه صنعتی؛ به‌شدت خطرناک.", true, 0.62f, 0.55f),
                MapPlace(26, "NCPD North Precinct", "کلانتری NCPD نورث", PlaceType.STATION, "پلیس منطقه."),
                MapPlace(27, "Scrapheap Market", "بازار اسکرهیپ", PlaceType.MARKET, "بازار اوراق و ضایعات."),
                MapPlace(28, "Stacks Motel", "متل استکس", PlaceType.HOTEL, "متل ارزان.")
            ),
            districtHotspots = emptyList()
        ),
        District(
            id = "citycenter", num = "03", nameEn = "CITY CENTER", nameFa = "سیتی سنتر",
            mapRes = R.drawable.map_citycenter,
            slogan = "Your Wealth, Their Power",
            area = "10.2 km²", pop = "920,000", living = "خیلی بالا", security = "بسیار بالا",
            summary = "قلب اقتصادی و اداری شهر؛ کورپو پلازا، منطقه مالی و داون‌تاون. امن‌ترین و تمیزترین منطقه، " +
                "اما زیر سیطره‌ی کامل مگاکورپ‌ها؛ دوربین‌ها و پلیس خصوصی همه‌جا هست.",
            subAreas = listOf(
                "Corpo Plaza" to "کورپو پلازا", "Financial District" to "منطقه مالی",
                "Downtown" to "داون‌تاون", "Memorial Park" to "پارک مموریال",
                "Grand Imperial Mall" to "گرند ایمپریال مال", "Avenue of the Stars" to "خیابان ستارگان"
            ),
            gangs = listOf("Viper Eyes", "6th Street", "Voodoo Boys (محدود)", "Barghest (محدود)"),
            corps = listOf("Arasaka", "Militech", "Biotechnica", "Trauma Team", "EuroBank"),
            places = listOf(
                MapPlace(1, "City Center Clinic", "کلینیک مدسنتر", PlaceType.CLINIC, "درمانگاه لوکس منطقه."),
                MapPlace(2, "Riot Royale Club", "کلاب رایوت رویال", PlaceType.BAR, "کلاب گران‌قیمت."),
                MapPlace(3, "H10 Executive Hotel", "هتل H10 اگزکیوتیو", PlaceType.HOTEL, "هتل شرکتی مجلل."),
                MapPlace(4, "Gunrunner Boutique", "بوتیک اسلحه گان‌رانر", PlaceType.GUN, "فروشگاه تسلیحات رده‌بالا."),
                MapPlace(5, "Arasaka Tower", "برج آراساکا", PlaceType.CORP, "مرکز فرماندهی آراساکا؛ نماد سلطه شرکتی و یکی از امن‌ترین نقاط شهر.", true, 0.50f, 0.30f),
                MapPlace(6, "Emporium Luxury Store", "فروشگاه لوکس امپوریوم", PlaceType.MARKET, "بوتیک اجناس گران."),
                MapPlace(7, "EuroBank City Center", "بانک یوروبانک سنتر", PlaceType.BANK, "بانک اصلی منطقه مالی."),
                MapPlace(8, "The Jade Dragon", "رستوران جید دراگون", PlaceType.FOOD, "رستوران شرقی لوکس."),
                MapPlace(9, "Villefort Motors", "بنگاه ماشین ویل‌فورت", PlaceType.CAR, "خودروی لوکس."),
                MapPlace(10, "Memorial Clinic", "کلینیک مموریال", PlaceType.CLINIC, "درمانگاه نزدیک پارک."),
                MapPlace(11, "Cloud 9 Lounge", "بار کلود ۹ لاژ", PlaceType.BAR, "لانج طبقات بالا."),
                MapPlace(12, "City Auto Works", "تعمیرگاه سیتی آتو ورکس", PlaceType.GARAGE, "کاراژ رسمی."),
                MapPlace(13, "The Glen Hotel", "هتل گلن", PlaceType.HOTEL, "هتل تجاری."),
                MapPlace(14, "PetroChem Bank", "بانک پتروکم", PlaceType.BANK, "شعبه بانکی."),
                MapPlace(15, "Grand Imperial Mall", "گرند ایمپریال مال", PlaceType.MALL, "بزرگ‌ترین مرکز خرید شهر؛ فروشگاه‌های زنجیره‌ای و سرگرمی.", true, 0.62f, 0.40f),
                MapPlace(16, "Afterlife City Branch", "شعبه افترلایف سنتر", PlaceType.BAR, "شعبه بار مزدوران."),
                MapPlace(17, "Dain's Gun Emporium", "فروشگاه اسلحه دِین", PlaceType.GUN, "اسلحه‌فروشی."),
                MapPlace(18, "Militech Office", "دفتر میلی‌تک", PlaceType.CORP, "دفتر مرکزی تسلیحاتی."),
                MapPlace(19, "Boulevard Bistro", "رستوران بولِوارد بیسترو", PlaceType.FOOD, "رستوران فرانسوی‌مآب."),
                MapPlace(20, "City Center Parking Hub", "پارکینگ مرکزی سنتر", PlaceType.PARKING, "پارکینگ طبقاتی بزرگ."),
                MapPlace(21, "Memorial Park", "پارک مموریال", PlaceType.PARK, "پارک بزرگ یادبود در قلب منطقه.", true, 0.30f, 0.55f)
            ),
            districtHotspots = emptyList()
        ),
        District(
            id = "westbrook", num = "04", nameEn = "WESTBROOK", nameFa = "وست‌بروک",
            mapRes = R.drawable.map_westbrook,
            slogan = "Arasaka Industries — Advancing Tomorrow",
            area = "15.9 km²", pop = "1,050,000", living = "بالا", security = "متوسط به بالا",
            summary = "منطقه‌ی تفریح، لاکچری و فناوری؛ ژاپن‌تاون، کازینوها و کلاب‌های گران، تپه‌های لوکس نورث اوک و " +
                "مگابیلدینگ H6. قلمرو تفریحی ثروتمندان و قلمرو تایگر کلاز در ژاپن‌تاون.",
            subAreas = listOf(
                "North Oak" to "نورث اوک", "Charter Hill" to "چارتر هیل",
                "Japantown" to "ژاپن‌تاون", "Hilltop" to "هیل‌تاپ",
                "Megabuilding H6" to "مگابیلدینگ H6", "Vista del Rey" to "ویستا دل ری"
            ),
            gangs = listOf("Tyger Claws", "6th Street", "Maelstrom", "Tyger Eyes", "Insiders"),
            corps = listOf("Arasaka", "Zetatech", "Biotechnica", "Trauma Team", "Kang Tao"),
            places = listOf(
                MapPlace(1, "Westbrook Clinic", "کلینیک وست‌بروک", PlaceType.CLINIC, "درمانگاه خصوصی."),
                MapPlace(2, "Westbrook Bar", "بار وست‌بروک", PlaceType.BAR, "بار لوکس."),
                MapPlace(3, "Corner Slice Pizza", "پیتزا کرنر اسلایس", PlaceType.FOOD, "پیتزافروشی."),
                MapPlace(4, "Heavy Metal Arms", "فروشگاه اسلحه هوی متال", PlaceType.GUN, "اسلحه‌فروشی."),
                MapPlace(5, "Turbo Tech Garage", "کاراژ توربو تک", PlaceType.GARAGE, "تعمیرگاه تیونینگ."),
                MapPlace(6, "CrediMax Bank", "بانک کردی‌مکس", PlaceType.BANK, "بانک منطقه."),
                MapPlace(7, "Vista Hotel", "هتل ویستا", PlaceType.HOTEL, "هتل مجلل."),
                MapPlace(8, "Westbrook Metro Station", "ایستگاه مترو وست‌بروک", PlaceType.METRO, "مترو."),
                MapPlace(9, "DataLine Tech Store", "فروشگاه دیتالاین", PlaceType.MARKET, "گجت و فناوری."),
                MapPlace(10, "Westbrook Market", "بازار وست‌بروک", PlaceType.MARKET, "بازار ژاپن‌تاون.", true, 0.50f, 0.55f),
                MapPlace(11, "Zetatech Tower", "برج زیتاتک", PlaceType.CORP, "برج شرکت هوافضا و رایانش.", true, 0.30f, 0.18f),
                MapPlace(12, "Luxury Clinic", "کلینیک لاکچری", PlaceType.CLINIC, "جراحی زیبایی/پزشکی لوکس."),
                MapPlace(13, "Kogane Noodle Bar", "نودل بار کوگانه", PlaceType.FOOD, "رستوران نودل ژاپنی."),
                MapPlace(14, "CyberDoc RX", "سایبرداک RX", PlaceType.RIPPER, "ریپرداک رده‌بالا."),
                MapPlace(15, "Buck-A-Bite Diner", "داینر باک-ا-بایت", PlaceType.FOOD, "فست‌فود."),
                MapPlace(16, "AutoFix Westbrook", "تعمیرگاه آتوفیکس", PlaceType.GARAGE, "کاراژ."),
                MapPlace(17, "H6 Main Entrance", "ورودی اصلی مگابیلدینگ H6", PlaceType.LANDMARK, "مگابیلدینگ غول‌پیکر مسکونی.", true, 0.25f, 0.72f),
                MapPlace(18, "Riverside Lounge", "لانج ریورساید", PlaceType.BAR, "لانج کنار آب."),
                MapPlace(19, "Farrier & Sons Bank", "بانک فاریِر و پسران", PlaceType.BANK, "بانک."),
                MapPlace(20, "Westbrook Transit Hub", "هاب ترابری وست‌بروک", PlaceType.METRO, "پایانه حمل‌ونقل."),
                MapPlace(21, "Clouds", "کلاب کلاودز", PlaceType.BAR, "کلاب لوکس دلبران (Dolls)؛ تجارت پرسود شبانه.", true, 0.55f, 0.45f)
            ),
            districtHotspots = emptyList()
        ),
        District(
            id = "heywood", num = "05", nameEn = "HEYWOOD", nameFa = "هیوود",
            mapRes = R.drawable.map_heywood,
            slogan = "Heywood — Home Is Here",
            area = "18.7 km²", pop = "1,230,000", living = "متوسط", security = "متوسط تا پایین",
            summary = "منطقه‌ی مسکونی و تجاریِ متوسط؛ محله‌های طبقه متوسط، پارک و رستوران. قلمرو اصلی والنتینوها؛ " +
                "بخش‌های شرقی در کنترل تایگر کلاز. جنوبش خطرناک‌تر است.",
            subAreas = listOf(
                "Vista Glen" to "ویستا گلن", "The Glen" to "گلن", "Downtown Heywood" to "مرکز هیوود",
                "South Heywood" to "هیوود جنوبی", "Coronado Ranch" to "رنچ کورونادو"
            ),
            gangs = listOf("Valentinos", "Maelstrom", "Scavengers", "Animals", "Tyger Claws", "6th Street"),
            corps = listOf("Trauma Team", "Biotechnica", "NCPD Heywood Precinct", "City Services"),
            places = listOf(
                MapPlace(1, "Heywood Clinic", "کلینیک هیوود", PlaceType.CLINIC, "درمانگاه منطقه.", true, 0.45f, 0.16f),
                MapPlace(2, "El Cesar Bar", "بار لِسزار", PlaceType.BAR, "بار محلی."),
                MapPlace(3, "El Pinche Restaurant", "رستوران ال پینچه", PlaceType.FOOD, "رستوران مکزیکی."),
                MapPlace(4, "Vista Glen Market", "مارکت ویستا گلن", PlaceType.MARKET, "بازار محله."),
                MapPlace(5, "Armorer's Bench", "فروشگاه اسلحه آرمورر", PlaceType.GUN, "اسلحه‌فروشی."),
                MapPlace(6, "Glen Lane Club", "کلاب گلن لین", PlaceType.BAR, "کلاب شبانه."),
                MapPlace(7, "Heywood Auto Garage", "گاراژ هیوود آتو", PlaceType.GARAGE, "تعمیرگاه."),
                MapPlace(8, "People's Bank", "بانک اتحاد مردمی", PlaceType.BANK, "بانک محلی."),
                MapPlace(9, "Wellspring Hospital", "بیمارستان ول‌اسپرینگ", PlaceType.CLINIC, "بیمارستان اصلی منطقه.", true, 0.40f, 0.45f),
                MapPlace(10, "Night City Bank", "بانک نایت سیتی", PlaceType.BANK, "شعبه بانک."),
                MapPlace(11, "Heywood Diner", "داینر ماستر هیوود", PlaceType.FOOD, "داینر."),
                MapPlace(12, "Coronado Motel", "متل کورونادو", PlaceType.HOTEL, "متل."),
                MapPlace(13, "Central Bus Station", "ایستگاه اتوبوس مرکزی", PlaceType.METRO, "پایانه اتوبوس."),
                MapPlace(14, "Route 66 Diner", "بار روتسی بارشکا", PlaceType.FOOD, "داینر جاده."),
                MapPlace(15, "Heywood Metro", "مترو هیوود", PlaceType.METRO, "ایستگاه مترو."),
                MapPlace(16, "Valentinos Patch", "پاتوق گنگ والنتینوس", PlaceType.GANG, "قلمرو والنتینوها در شرق.", true, 0.60f, 0.40f),
                MapPlace(17, "Ripperdoc McCoy", "ریپرداک مککووی", PlaceType.RIPPER, "ریپرداک محله."),
                MapPlace(18, "Coronado Market", "بازار محلی کورونادو", PlaceType.MARKET, "بازار."),
                MapPlace(19, "Taco Loco Restaurant", "رستوران تاکو لوکو", PlaceType.FOOD, "اغذیه مکزیکی."),
                MapPlace(20, "Coronado Ranch Repair", "تعمیرگاه رنچ کورونادو", PlaceType.GARAGE, "کاراژ محله.")
            ),
            districtHotspots = emptyList()
        ),
        District(
            id = "santodomingo", num = "06", nameEn = "SANTO DOMINGO", nameFa = "سانتو دومینگو",
            mapRes = R.drawable.map_santodomingo,
            slogan = "El Pueblo Unido Jamás Será Vencido",
            area = "22.1 km²", pop = "1,520,000", living = "متوسط", security = "متوسط رو به پایین",
            summary = "قلب صنعتی و نیروگاهی شهر؛ کارخانه‌ها، پالایشگاه و نیروگاه. محله‌های کارگری رنگارنگ مثل " +
                "رانچو کورونادو و آرویو. منطقه‌ی سکست‌استریت و والنتینوها.",
            subAreas = listOf(
                "Rancho Coronado" to "رانچو کورونادو", "Arroyo" to "آرویو",
                "Barrio Corona" to "باریو کورونا", "Mercado" to "مرکادو",
                "Coastview" to "کوست‌ویو", "LS Financial District" to "منطقه مالی LS",
                "South Docks" to "داکس جنوبی", "Bayview" to "بی‌ویو"
            ),
            gangs = listOf("6th Street", "Maelstrom", "Valentinos", "Voodoo Boys", "Animals"),
            corps = listOf("Arasaka", "Militech", "Trauma Team", "Biotechnica", "Kang Tao", "City Services"),
            places = listOf(
                MapPlace(1, "Santo Domingo Clinic", "کلینیک سانتو دومینگو", PlaceType.CLINIC, "درمانگاه منطقه.", true, 0.40f, 0.12f),
                MapPlace(2, "El Coyote Cojo Bar", "بار ال کایوتی کوهو", PlaceType.BAR, "بار مشهور محله."),
                MapPlace(3, "Carniceria La Chingona", "رستوران لا چینگونا", PlaceType.FOOD, "رستوران/قصابی."),
                MapPlace(4, "AutoShop El Padre", "تعمیرگاه ال پادره", PlaceType.GARAGE, "کاراژ."),
                MapPlace(5, "Santo Domingo Intl. Bank", "بانک بین‌المللی سانتو", PlaceType.BANK, "بانک."),
                MapPlace(6, "La Chispa Club", "کلاب لا چیسپا", PlaceType.BAR, "کلاب."),
                MapPlace(7, "Santo Domingo Metro", "ایستگاه مترو سانتو", PlaceType.METRO, "مترو."),
                MapPlace(8, "Mercado de Pulgas", "بازار مرکادو د پولگاس", PlaceType.MARKET, "بازار دست‌فروش‌ها.", true, 0.42f, 0.40f),
                MapPlace(9, "Raijin Clinic", "کلینیک رایجین مردمی", PlaceType.CLINIC, "درمانگاه."),
                MapPlace(10, "Los Hermanos", "رستوران لوس هرمانوس", PlaceType.FOOD, "داینر خانوادگی."),
                MapPlace(11, "Santo Motel", "متل سانتو", PlaceType.HOTEL, "متل."),
                MapPlace(12, "Valentinos Nightclub", "کلاب شبانه والنتینوس", PlaceType.BAR, "پاتوق گنگ."),
                MapPlace(13, "Santo Domingo Motors", "بنگاه سانتو دومینگو موتورز", PlaceType.CAR, "خودرو."),
                MapPlace(14, "Jefe Taco Bar", "تاکوبار جفه", PlaceType.FOOD, "اغذیه."),
                MapPlace(15, "Taqueria El Jefe", "تاکریا ال جفه", PlaceType.FOOD, "تاکو."),
                MapPlace(16, "Rancho Coronado Hotel", "هتل رانچو کورونادو", PlaceType.HOTEL, "هتل."),
                MapPlace(17, "Despues de Todo Bar", "بار دسپوس د تودو", PlaceType.BAR, "بار محلی."),
                MapPlace(18, "Street Market", "بازار شبانه سنتی", PlaceType.MARKET, "بازار خیابانی."),
                MapPlace(19, "Arroyo Metro Station", "ایستگاه مونوریل آرویو", PlaceType.METRO, "مترو."),
                MapPlace(20, "Industrial Danger Zone", "منطقه خطرناک ناحیه صنعتی", PlaceType.DANGER, "کارخانه‌ها و داکس؛ حادثه و درگیری.", true, 0.55f, 0.66f)
            ),
            districtHotspots = emptyList()
        ),
        District(
            id = "pacifica", num = "07", nameEn = "PACIFICA", nameFa = "پاسیفیکا",
            mapRes = R.drawable.map_pacifica,
            slogan = "No Rules, Just Survival",
            area = "27.4 km²", pop = "470,000", living = "پایین", security = "بسیار پایین",
            summary = "پروژه‌ی تفریحی-توریستی رها و نیمه‌سازه؛ حالا در کنترل وودو بویز و شهرِ محصورِ داگ‌تاون " +
                "(بارگِست). NCPD تقریباً حضور ندارد. خطرناک‌ترین منطقه برای خارجی‌ها.",
            subAreas = listOf(
                "West Wind Estate" to "وست ویند استیت", "Coastview" to "کوست‌ویو",
                "Dogtown" to "داگ‌تاون", "Combat Zone" to "کامبت زون",
                "Grand Imperial Mall" to "گرند ایمپریال مال", "Wellsprings" to "ول‌اسپرینگز",
                "Pacifica Stadium" to "استادیوم پاسیفیکا", "The Boneyard" to "بونیارد"
            ),
            gangs = listOf("Voodoo Boys", "Animals", "Scavengers", "Tiger Claws", "6th Street", "The Mox", "Barghest (Dogtown)"),
            corps = listOf("Trauma Team", "Biotechnica", "Militech", "Kang Tao", "Petrochem", "NCPD Pacifica"),
            places = listOf(
                MapPlace(1, "Pacifica Clinic", "کلینیک پاسیفیکا", PlaceType.CLINIC, "درمانگاه محدود."),
                MapPlace(2, "Heavy Hearts Club", "مکان Heavy Hearts", PlaceType.BAR, "کلاب معروف.", true, 0.42f, 0.20f),
                MapPlace(3, "Ocean View", "اقامتگاه اوشن ویو", PlaceType.HOTEL, "منطقه ساحلی."),
                MapPlace(4, "The Moth", "بار/کلاب The Moth", PlaceType.BAR, "کلاب وودو بویز؟"),
                MapPlace(5, "Coast Arms Gun Shop", "فروشگاه اسلحه کوست آرمز", PlaceType.GUN, "اسلحه‌فروشی بازار سیاه."),
                MapPlace(6, "Pacific Fixer", "فیکسر پاسیفیک", PlaceType.LANDMARK, "محل تماس فیکسر."),
                MapPlace(7, "Grand Imperial Mall", "گرند ایمپریال مال پاسیفیکا", PlaceType.MALL, "مال نیمه‌رهاشده؛ حالا دژ و بازار گنگ.", true, 0.38f, 0.45f),
                MapPlace(8, "Pacifica Playgrounds", "زمین بازی پاسیفیکا", PlaceType.PARK, "پارک/زمین مخروبه."),
                MapPlace(9, "Pacific Trust Bank", "بانک پاسیفیک تراست", PlaceType.BANK, "بانک رهاشده."),
                MapPlace(10, "Pacific Bites", "اغذیه پاسیفیک بایتس", PlaceType.FOOD, "اغذیه خیابانی."),
                MapPlace(11, "Combat Zone", "منطقه کامبت زون", PlaceType.DANGER, "منطقه‌ی بی‌قانون و جنگ گنگ‌ها؛ وارد نشو.", true, 0.55f, 0.42f),
                MapPlace(12, "Doc Ortega Clinic", "کلینیک داک اورتگا", PlaceType.CLINIC, "ریپرداک/درمانگاه."),
                MapPlace(13, "Clouds Club", "کلاب کلاودز (شعبه)", PlaceType.BAR, "بار/کلاب."),
                MapPlace(14, "No-Tell Motel", "متل نو-تل", PlaceType.HOTEL, "متل مخفی برای قرارها.", true, 0.62f, 0.30f),
                MapPlace(15, "Pacifica Stadium", "استادیوم پاسیفیکا", PlaceType.STADIUM, "استادیوم بزرگ متروکه.", true, 0.45f, 0.62f),
                MapPlace(16, "Boneyard Arms", "فروشگاه اسلحه بونیارد آرمز", PlaceType.GUN, "بازار اسلحه."),
                MapPlace(17, "Boneyard Market", "بازار متروکه بونیارد", PlaceType.MARKET, "بازار قاچاق.", true, 0.60f, 0.60f),
                MapPlace(18, "Nomad Camp", "کمپ نومد", PlaceType.CAMP, "اردوگاه کوچ‌نشینان لبه منطقه."),
                MapPlace(19, "Sunset Fuel Pump", "پمپ بنزین سانست", PlaceType.SITE, "پمپ بنزین."),
                MapPlace(20, "Pacifica Tower", "برج ارتباطی پاسیفیکا", PlaceType.STATION, "دکل/برج مخابراتی."),
                MapPlace(21, "Dogtown Entrance", "ورودی داگ‌تاون", PlaceType.DANGER, "مرز شهر محصورِ بارگِست؛ عبور بدون اجازه = مرگ.", true, 0.58f, 0.22f)
            ),
            districtHotspots = emptyList()
        ),
        District(
            id = "badlands", num = "08", nameEn = "BADLANDS", nameFa = "بدلندز",
            mapRes = R.drawable.map_badlands,
            slogan = "Nomad Life — Choose Your Road",
            area = "45.2 km²", pop = "125,000", living = "خیلی پایین", security = "خیلی پایین",
            summary = "بیابان‌های وسیع خارج از شهر؛ جاده‌های خاکی، مزارع خورشیدی، کمپ کوچ‌نشینان (آلدکالدو) و " +
                "تأسیسات رهاشده‌ی شرکتی. قانون جاده و کاروان‌ها حکم می‌کند. مواد معدنی و مکان‌های مخفی ارزشمندند.",
            subAreas = listOf(
                "Northern Wastes" to "دشت‌های شمالی", "Western Ridges" to "تپه‌های غربی",
                "Central Basin" to "حوضه مرکزی", "Southern Pass" to "گذرگاه جنوبی"
            ),
            gangs = listOf("Aldecaldos", "Wraiths", "Scorpions", "Rattlers", "Barghest", "6th Street"),
            corps = listOf("Militech", "Biotechnica", "Kang Tao", "Petrochem", "NCPD (ایستگاه‌های مرزی)"),
            places = listOf(
                MapPlace(1, "Nomad Camp: Ironwind", "کمپ نومد: آیرون‌ویند", PlaceType.CAMP, "اردوگاه کوچ‌نشینان.", true, 0.22f, 0.12f),
                MapPlace(2, "Abandoned Airfield", "فرودگاه متروکه", PlaceType.SITE, "باند هوایی رها؛ مناسب قاچاق.", true, 0.38f, 0.10f),
                MapPlace(3, "Miltech Patrol Base", "پایگاه گشتی میلی‌تک", PlaceType.STATION, "پایگاه نظامی شرکتی.", true, 0.52f, 0.10f),
                MapPlace(4, "Solar Farm Alpha", "مزرعه خورشیدی آلفا", PlaceType.SITE, "نیروگاه خورشیدی.", true, 0.66f, 0.10f),
                MapPlace(5, "Raffens' Hideout", "مخفیگاه رافن‌ها", PlaceType.GANG, "کمین راهزنان جاده.", true, 0.12f, 0.30f),
                MapPlace(6, "Fuel Station 21", "پمپ بنزین ۲۱", PlaceType.SITE, "ایستگاه سوخت."),
                MapPlace(7, "Rocky Ridge Camp", "کمپ راکی ریج", PlaceType.CAMP, "اردوگاه."),
                MapPlace(8, "Corporate Outpost", "پایگاه شرکتی", PlaceType.STATION, "پاسگاه شرکت."),
                MapPlace(9, "Oil Pump Station", "ایستگاه پمپ نفت", PlaceType.SITE, "تلمبه‌خانه.", true, 0.28f, 0.40f),
                MapPlace(10, "Sunset Motel", "متل سانست", PlaceType.HOTEL, "متل کنار جاده.", true, 0.55f, 0.32f),
                MapPlace(11, "Highway Outpost", "پاسگاه بزرگراه", PlaceType.STATION, "ایست بین‌راهی."),
                MapPlace(12, "Wreckage Field", "محوطه لاشه‌ها", PlaceType.SITE, "گورستان خودرو/قطعات."),
                MapPlace(13, "Biotechnica Facility", "تأسیسات بیوتکنیکا", PlaceType.CORP, "سایت زیستی شرکتی.", true, 0.18f, 0.55f),
                MapPlace(14, "Main Fuel Depot", "انبار سوخت اصلی", PlaceType.SITE, "دپوی سوخت.", true, 0.42f, 0.52f),
                MapPlace(15, "Jake's Garage", "گاراژ جیک", PlaceType.GARAGE, "تعمیرگاه/پناه نومدها.", true, 0.58f, 0.50f),
                MapPlace(16, "Old Mining Site", "معدن قدیمی", PlaceType.SITE, "معدن رهاشده.", true, 0.82f, 0.50f),
                MapPlace(17, "Scorpion's Den", "لانه اسکورپیون", PlaceType.GANG, "مقر گنگ اسکورپیون.", true, 0.28f, 0.62f),
                MapPlace(18, "Border Checkpoint", "ایست بازرسی مرزی", PlaceType.STATION, "مرز؛ بازرسی و باج.", true, 0.42f, 0.68f),
                MapPlace(19, "NCPD Roadblock", "سد راهی NCPD", PlaceType.STATION, "بست راه."),
                MapPlace(20, "Coast View Point", "نقطه دید ساحلی", PlaceType.LANDMARK, "منظره ساحل."),
                MapPlace(21, "Nomad Camp: Whisper", "کمپ نومد: ویسپر", PlaceType.CAMP, "اردوگاه."),
                MapPlace(22, "Crystal Rock Mine", "معدن کریستال راک", PlaceType.SITE, "معدن."),
                MapPlace(23, "Power Substation", "پست برق", PlaceType.SITE, "پست فشارقوی."),
                MapPlace(24, "Abandoned Pipeline", "خط لوله متروک", PlaceType.SITE, "لوله‌گذاری رها.", true, 0.78f, 0.68f),
                MapPlace(25, "Smuggler's Cove", "خلیج قاچاقچیان", PlaceType.PORT, "پناهگاه ساحلی قاچاق."),
                MapPlace(26, "Nomad Camp: Red Sands", "کمپ نومد: رد سندز", PlaceType.CAMP, "اردوگاه."),
                MapPlace(27, "Radio Tower", "برج رادیویی", PlaceType.STATION, "دکل مخابراتی.", true, 0.62f, 0.78f),
                MapPlace(28, "Arroyo Relay Station", "ایستگاه رله آرویو", PlaceType.STATION, "ایستگاه رله."),
                MapPlace(29, "Miltech Listening Post", "پست شنودی میلی‌تک", PlaceType.STATION, "پایگاه شنود.", true, 0.72f, 0.85f),
                MapPlace(30, "Aldecaldo Camp", "کمپ آلدکالدو", PlaceType.CAMP, "کمپ اصلی قبیله آلدکالدو.", true, 0.88f, 0.85f)
            ),
            districtHotspots = emptyList()
        )
    )

}
