# GM Kit — an unofficial companion for use with Cyberpunk RED

> **Unofficial fan content** provided under the [Homebrew Content Policy of R. Talsorian Games](https://rtalsoriangames.com/homebrew-content-policy/).
> Not affiliated with, endorsed by, or sponsored by R. Talsorian Games or CD Projekt RED.
> *Cyberpunk* and *Cyberpunk RED* are trademarks of R. Talsorian Games.
> This project is free, contains no ads and no in-app purchases, and is meant for use at your own table.
> See [Legal](#legal--حقوقی) before publishing anything.

ابزارِ Game Master برای **Cyberpunk RED** روی اندروید — برگه‌ی شخصیت، موتور قواعد، مبارزه،
نقشه‌ی نبرد، ابزارهای GM و سشن محلی (LAN) که گوشی GM را به مرجع حقیقت و گوشی بازیکن‌ها را
به برگه‌ی زنده تبدیل می‌کند. کاملاً آفلاین؛ هیچ اینترنتی لازم نیست.

---

## آنچه هست / What this is

یک اپلیکیشن اندرویدی (Kotlin + Jetpack Compose) که سه کار را با هم انجام می‌دهد:

1. **برگه‌ی زنده‌ی شخصیت** — Statها، مهارت‌ها، سایبرویر، انسانیت، جراحت‌های بحرانی،
   موجودی، سلاح، پول و IP؛ با محاسبه‌ی خودکار همه‌ی مقادیر مشتق.
2. **موتور قواعد** — چک‌های d10 با Critical Success/Failure، آسیب، SP و ablation زره،
   ضربه‌ی سر، جداول Critical Injury (2d6)، Death Save، Humanity Loss و Cyberpsychosis.
3. **ابزار سر میز** — نقشه‌ی نبرد با توکن و کالیبراسیون، NPC و برخورد تصادفی،
   فروشگاه/اقتصاد، Lifepath، گیگ‌ها، کمپین و فیلم‌نامه‌ی صحنه‌ها، مرجع قواعد، و سشن LAN.

هدف طراحی: **سر میز هیچ‌کس منتظر اپ نماند.** اگر شبکه قطع شود، اگر تاس دستی انتخاب شود،
اگر سشنی ساخته نشود — بازی مثل قبل ادامه دارد.

---

## قابلیت‌ها

### شخصیت و قواعد
- ساخت شخصیت با هر دو روش کتاب: **Streetrat Template** و **Complete Package**، به‌علاوه‌ی Lifepath کامل.
- ۶۳ مهارت از Master Skill List با STAT درست، مهارت‌های Difficult (×۲) و هزینه‌ی IP/ساخت.
- نقش‌ها و Role Abilityها (Solo، Netrunner، Medtech، Media، Nomad، Fixer، Exec، Lawman، Rockerboy، Tech)
  به‌همراه دستیارهای مخصوص هر نقش.
- Humanity / EMP، سایبرویر با Humanity Loss، ترک اعتیاد، داروهای خیابانی و Cyberpsychosis.
- جراحت‌های بحرانی: هر ۲۲ جراحت کتاب با اثر مکانیکی، Quick Fix و Treatment و DVهایشان.

### مبارزه
- صف Initiative، Action/Move Action، Aim، Autofire، shotgun shell، گرپل، کاور و وسایل نقلیه.
- محاسبه‌ی کامل آسیب: SP، ablation، Armor Piercing، نصف‌شدن زره در melee، ضریب ×۲ سر
  (و ×۳ با Cracked Skull)، Bonus Damage پنج‌تاییِ جراحت بحرانی، و جراحت اجباری برای هدف Mortally Wounded.
- **تاس دستی**: با خاموش‌کردن «تاس خودکار»، *همه‌ی* تاس‌های قواعد از GM پرسیده می‌شوند
  و عدد تاس فیزیکی وارد می‌شود — با لاگ کامل تاس‌ها.

### ابزار GM
- **نقشه‌ی نبرد**: ۱۰۳ نقشه‌ی داخلی در ۴ سطح تهدید، با شبکه، کالیبراسیون متر/خانه، توکن،
  ارتفاع/طبقه و ذخیره‌ی وضعیت صحنه.
- **NPC**: ۶۹ قالب آماده + ساخت NPC دسته‌ای (Bulk).
- **فروشگاه/اقتصاد**: ۲۶۵ آیتم، Night Market، Availability، خرید/فروش، بودجه‌ی Fashion و تراکنش‌های idempotent.
- **کمپین و فیلم‌نامه**: ۵۲ صحنه‌ی نوشته‌شده با پیوند مستقیم به نقشه‌ها، ۹ گیگ گروهی (Crew) و تولیدکننده‌ی گیگ انفرادی.
- **مرجع قواعد**: ۸۰ مدخل قاعده با توضیح کوتاه و بلند، قابل جست‌وجو سر میز.
- ساعت/تقویم کمپین، آب‌وهوا، برخورد تصادفی، و سیاهه‌ی رویدادهای مبارزه (Combat Log).

### سشن محلی (LAN)
- گوشی GM سرور سبک HTTP اجرا می‌کند (بدون کتابخانه‌ی خارجی)؛ بازیکن‌ها با **اسکن QR** یا
  کد شش‌رقمی وصل می‌شوند. تولید QR داخل پروژه انجام می‌شود؛ فقط خواندن آن با zxing است.
- برگه‌ی بازیکن به‌صورت زنده از GM به‌روزرسانی می‌شود؛ هر تغییر بازیکن **درخواست** است و
  GM باید تأیید کند.
- هر برگه یک `sheetUid` پایدار دارد؛ اسکن مجدد یا push بعدی **همان ردیف** را به‌روز
  می‌کند و شخصیت تکراری نمی‌سازد. اگر دو نسخه‌ی یک برگه (GM و بازیکن) موقع join فرق
  داشته باشند، اختلاف با diff به GM نشان داده می‌شود و GM منبع حقیقت را انتخاب می‌کند:
  تأیید = نسخه‌ی بازیکن، رد = نسخه‌ی GM.
- GM اول حاضرانِ میز را انتخاب می‌کند (شخصیت‌های موجود + «بازیکن جدید» بدون سقف) و بعد
  از تأیید، QR هر نقش صادر می‌شود.
- اطلاعات مخفی GM (یادداشت NPC، کتاب کمپین، اهداف پنهان) هرگز ارسال نمی‌شود.
  جزئیات مرز اعتماد: [`LanTrust.kt`](app/src/main/java/data/net/LanTrust.kt).

### ذخیره‌سازی
- نوشتن اتمیک (`.tmp` + `fsync` + `.bak` + rename اتمیک) برای همه‌ی فایل‌های JSON.
- اسلات‌های Save با نسخه‌ی فرمت، checksum و **rollback خودکار** اگر اندروید وسط restore
  پروسه را بکشد؛ Autosave چرخشی.
- بازیابی از نسخه‌ی پشتیبان وقتی فایل اصلی خراب است + هشدار صریح به کاربر (نه حذف بی‌صدا).

---

## ساخت و اجرا / Build

| مورد | مقدار |
| --- | --- |
| Gradle Wrapper | 9.6.0 |
| Android Gradle Plugin | 9.4.0 (Kotlin داخلی AGP؛ پلاگین جداگانه‌ی `kotlin-android` لازم نیست) |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| compileSdk / targetSdk | 37 |
| minSdk | 24 (اندروید ۷.۰) |
| JDK | ۱۷ یا جدیدتر (۲۱ پیشنهاد می‌شود؛ همان که Android Studio همراه دارد) |

```bash
./gradlew assembleDebug      # ساخت APK دیباگ
./gradlew test               # تست‌های واحد (JVM) — قواعد، شبکه، ذخیره‌سازی
./gradlew lint               # lint اندروید
```

اگر اولین بار است می‌سازی: `local.properties` با مسیر `sdk.dir` لازم است
(این فایل عمداً در `.gitignore` است).

> ⚠️ این پیکربندی روی لبه‌ی تکنولوژی است (AGP 9.4 / SDK 37). اگر Android Studio قدیمی‌تری
> داری، یا AGP را پایین بیاور یا Studio را به‌روز کن؛ نسخه‌ها همه در
> [`gradle/libs.versions.toml`](gradle/libs.versions.toml) و
> [`app/build.gradle.kts`](app/build.gradle.kts) هستند.

---

## معماری

```
app/src/main/java/com/cyberpunk/gmtool/
├── (ریشه)                    MainActivity, CyberpunkNavGraph
├── data/                     ← قلب پروژه: قواعد و داده، بدون وابستگی به UI
│   ├── GameRules.kt          HP، SP، ablation، Death Save، Humanity (محاسبات مشترک)
│   ├── CombatRules.kt        رول d10، آسیب، جداول جراحت، جریمه‌ها
│   ├── CriticalInjuries.kt   ۲۲ جراحت کتاب + گزینه‌های درمان
│   ├── EquipmentUseRules.kt  تأثیر تجهیزات/سایبرویر روی STAT و مهارت
│   ├── DiceSource.kt         تنها منبع تاس (خودکار یا دستی) — هیچ مسیر دیگری تاس نمی‌سازد
│   ├── AtomicJsonFileStore.kt, PersistenceRuntime.kt, GameSaveManager.kt   ذخیره‌سازی مقاوم به crash
│   ├── Localization.kt       i18n (کلید = رشته‌ی انگلیسیِ سورس)
│   └── net/                  سشن محلی: LanHost, LanClient, LanManager, LanProtocol,
│                             LanTrust (مرز اعتماد), TinyHttp (سرور/کلاینت), QrCode (رمزگذار QR)
├── ui/
│   ├── screens/              ۳۱ فایل صفحه/تب (Compose)
│   └── components/           قطعه‌های مشترک (QR، متن فارسی، تاس دستی، مجوز شبکه‌ی محلی)
└── viewmodel/                        وضعیت مشترک + دروازه‌ی ذخیره/پخش
    ├── CharacterViewModel.kt         هسته: state، persist، LAN، CRUD، سیو/بازیابی، پاداش‌ها
    ├── CharacterViewModelCombat.kt   دامنه‌ی مبارزه (initiative، سپرها، آسیب، Death Save)
    ├── CharacterViewModelGear.kt     دامنه‌ی تجهیزات (موجودی، فروشگاه، نصب Cyberware/Attachment)
    ├── CharacterViewModelCare.kt     دامنه‌ی مراقبت (داروها، تراپی، اعتیاد، جراحت بحرانی)
    └── CharacterViewModelVehicles.kt دامنه‌ی وسایل نقلیه (Motorpool، رانندگی، سلاح نصبی)
```

> ساختار پوشه‌ها با اعلان `package` هم‌راستا است (`com/cyberpunk/gmtool/**`).
> قبلاً ۹۴ فایل از ۹۹ فایل روی دیسک با پکیج خودش نمی‌خواند، که هر refactor/import
> خودکار در IDE را به درگیری تبدیل می‌کرد.

**قاعده‌ی تاس (قابل تست):** همه‌ی تاس‌های قواعدی از `DiceSource` می‌آیند — نه `Random`، نه
`(1..10).random()`. تست `DiceSourceEnforcementTest` کل سورس را می‌گردد و هر تاس دست‌ساز را
رد می‌کند، پس این وعده با کامنت باقی نمی‌ماند. قرارداد دومش این است که در حالت «تاس دستی»،
عملیات قواعدی **نباید روی نخ اصلی** بماند (میزبان دستی برای پرسیدن عدد از GM نخ را بلاک
می‌کند و روی نخ اصلی این کار یعنی فریز): الگوی UI `rules { ... }` است که در حالت دستی کار را
به `Dispatchers.Default` می‌برد. اگر جایی این قرارداد نقض شود، برنامه بی‌صدا عدد نمی‌سازد:
`DiceSource.onManualFallback` صدا زده می‌شود و به GM هشدار می‌دهد.

**قاعده‌ی اصلی:** منطق قواعد در `data/` است و UI هیچ فرمولی را خودش حساب نمی‌کند.
هر عددی که سر میز دیده می‌شود از `GameRules`/`CombatRules` می‌آید، پس یک اصلاح در یک جا
روی همه‌ی صفحه‌ها اثر می‌گذارد — و با تست واحد قابل محافظت است.

اندازه‌ی فعلی: **۱۰۵ فایل Kotlin، ~۴۰٬۳۰۰ خط** (۲۹۲ تابع `@Composable`) + ۴۳ فایل داده.

### نقاط شناخته‌شده‌ی ضعف (صادقانه)
- **فایل‌های غول:** `ui/screens/GmToolsScreen.kt` ۲٬۷۴۸ خط، `BioTab.kt` ۲٬۰۰۷،
  `CombatTab.kt` ۱٬۹۷۳ (پس از جدا شدن تب نوبت)، `BattleMapScreen.kt` ۱٬۹۳۶،
  `CharacterViewModel.kt` ۱٬۰۷۵ و تب تازه‌ی `TurnActionTab.kt` ۶۶۰ خط.
  تقسیم‌شان بر اساس دامنه اولین کار بازسازی است؛ ولی تقسیم `CombatTab` بی‌خطر نیست:
  تقریباً همه‌ی بخش‌هایش ~۲۰۰ متغیر `remember` مشترک دارند و بدون تست UI هر
  جابه‌جایی state می‌تواند بی‌صدا رفتار را عوض کند (اول تست اسکرین، بعد بریدن).
- بخشی از منطق به **نام انگلیسی آیتم‌ها** وابسته است (`"Linear Frame Sigma"`, `"Armor"`).
  تا وقتی این‌طور است، «قانون طلاییِ عوض‌نکردن رشته‌های انگلیسی» در `Localization.kt`
  الزامی می‌ماند. راه‌حل پایدار: شناسه‌ی پایدار/enum برای آیتم، دسته و اثر.
- **وزن دارایی‌های تصویری:** `app/src/main/res` ۲۹MB است که ۲۸MB آن ۲۱۷ فایل WebP
  (نقشه‌های نبرد + پرتره‌ها) است. هر ۲۱۱ فایل تصویری صفحه‌ها واقعاً استفاده می‌شوند
  (اسکن منابع بی‌استفاده: صفر) و با کیفیت نزدیک به بهینه ذخیره شده‌اند. پس تنها راه
  کم‌کردن وزن، افت کیفیت هدفمند است — که عمداً انجام نشد. اگر خواستی (با این اعداد):
  `cwebp -q 85` روی نقشه‌ها ≈۳۲٪ کم می‌کند (PSNR ≈۳۷dB)، `-q 92` ≈۳٪ (PSNR ≈۴۵dB).
  گزینه‌ی بهترِ بدون افت: Play Asset Delivery / دریافت نقشه‌ها در اولین اجرا.
- ~~CI وجود ندارد~~ **حل شد:** `.github/workflows/android-ci.yml` حالا `test`،
  `assembleDebug`، `assembleRelease` (تا R8 و `keepRules` هم واقعاً اجرا شوند) و
  `lintDebug` را روی هر push/PR اجرا می‌کند.
- حالت انگلیسی کامل نیست: متن اثر جراحت‌ها (`effectFa`) و بخشی از پیام‌های داده‌ای
  هنوز فقط فارسی‌اند.

---

## چه چیزهایی در این بازبینی اصلاح شد

- **مرز «Seriously Wounded»**: کد `hp in 1 until threshold` بود، یعنی کاراکتری که دقیقاً
  روی نصف HP می‌ایستاد پنالتی `-2` و DV پایدارسازی `13` نمی‌گرفت؛ حالا `1..threshold`
  است («equal to or less than ½» طبق کتاب) و مرزش در `RulesRegressionTest` تست دارد.
- **تنها منبع تاس، واقعاً تنهاست**: تاس‌های Combat، درمان، داروی خیابانی، Lifepath،
  ابزارهای GM، تاس تساوی Initiative و انتخاب جراحت بحرانی همه از `DiceSource` رد
  می‌شوند. دو تست نگهبان اضافه شد: `DiceSourceEnforcementTest` (ساختاری — کل سورس را
  می‌گردد و هر `Random`/`(a..b).random()` بیرون از `DiceSource` را رد می‌کند) و
  `DiceSourceTest` (رفتاری). افتادن به تصادفی دیگر بی‌صدا نیست: `onManualFallback`
  به GM هشدار می‌دهد.
- **Initiative یک فرمول دارد**: قبلاً دکمه‌ی «ROLL SELF» و «ROLL ALL» دو عدد مختلف
  می‌دادند (یکی Sandevistan فعال را حساب می‌کرد و دیگری نه). حالا هر دو
  `CombatRules.rollInitiative` را صدا می‌زنند و `InitiativeRulesTest` قفلش می‌کند.
- **ساختار پوشه‌ها = اعلان پکیج**: ۹۴ فایل به `app/src/main/java/com/cyberpunk/gmtool/**`
  منتقل شدند؛ دیگر هیچ فایلی روی دیسک با پکیج خودش نمی‌خواند-نمی‌خواند.
- **`.idea/` از مخزن بیرون آمد** و `.gitignore` کل پوشه را نادیده می‌گیرد (قبلاً
  `misc.xml` با مسیر SDK محلی کامیت شده بود).
- **مجوز شبکه‌ی محلی اندروید ۱۷**: `ACCESS_LOCAL_NETWORK` به منیفست اضافه شد و
  درخواست زمان‌اجرا با راهنمای «اگر رد شد» در `ui/components/LocalNetworkPermission.kt`
  و صفحه‌ی سشن وصل شد. بدون این، روی اندروید ۱۷ ساخت/پیوستن سشن بی‌صدا شکست می‌خورد.
- **CI**: `test` + `assembleDebug` + `assembleRelease` (تا R8 و `keepRules` هم واقعاً
  اجرا شوند) + `lintDebug`، و گزارش شکست به‌صورت issue.
- **`keepRules/rules.keep`**: هر قاعده با دلیلش مستند شد (Gson/نام فیلدها = کلید سیو،
  reflection در `LanTrust`).
- **تب مبارزه بازچینش شد (این پاس):** پنل‌های تکراری «TURN / ACTION WORKFLOW» و
  «QUICK TURN ACTIONS» و کارت «TURN STATUS» حذف شدند و لیست دومِ سلاح‌ها در کارت
  ATTACK (که تکرارِ بخش انتخاب سلاح بود) برداشته شد. اکشن‌های بدن (Brawling،
  Martial Arts، Grab، Choke، Throw، Grab Item، Get Up، Escape/Release Grapple)
  حالا **داخل همان کارت ATTACK** و زیر یک انتخاب دوتایی «سلاح / بدن» هستند:
  حالت «سلاح» لیست سلاح‌های Equip‌شده + Ammo/Range/Aim + اکشن سلاح انتخاب‌شده
  (Reload، Autofire، Suppressive Fire، Throw) را نشان می‌دهد؛ حالت «بدن» اکشن‌های
  بدنی. آن‌چه جای دیگری نداشت (ثبت حرکت و Run، Hold Action، Stabilize هدف) در یک
  کارت جمع‌وجور `OTHER ACTIONS` ماند؛ هشدار «Suppressive Fire — اجبار به Cover»
  همیشه‌باز است.
- **مسیر دفاع در حمله‌های بدنی در UI صریح شد:** ضربه‌های Brawling و Martial Arts
  مثل Melee با `DEX + Evasion` دفاع می‌شوند (شرط REF ۸ و دکمه‌ی Dodge فقط برای
  Ranged است)، Grab و رهایی Grapple مقابله‌ی `DEX + Brawling` هستند و Choke/Throw
  بعد از Grab موفق خودکار موفق‌اند. کد از قبل همین را انجام می‌داد (`isMelee` از
  `modes = "Melee"` و `defenderEvasionTotal`)، ولی حالا در متن راهنما هم می‌آید.
- **دکمه‌ی پاک‌سازی نسخه‌های تکراری حذف شد** (به‌همراه دیالوگ تأیید و توابع
  `removeDuplicateCharacters`/`countDuplicateSheets`/`findDuplicateSheetIds`) و
  برچسب دکمه‌های New/Import در نوار پایین صفحه‌ی بازیکنان دیگر دو خط نمی‌شکند. 

### پاس چهارم: تب «نوبت» جدا شد و مسیرهای کرش بسته شد

- **تب ششم `TURN`:** هر چیزی که به نوبت/انکوانتر مربوط بود از تب COMBAT بیرون
  آمد و در یک تب مستقل (`ui/screens/TurnActionTab.kt`) نشست — پنل روستر و
  انکوانتر، INITIATIVE/ROUND، کنترلر نوبت، Sandevistan، `OTHER ACTIONS`
  (Move/Run/Hold/Stabilize)، دیالوگ‌های روستر و صف نوبت و نوار چسبان
  `END TURN → NEXT` + `QUEUE`. تب COMBAT دیگر هیچ state یا دکمه‌ی نوبت ندارد.
- **ثبت خطای کشنده:** آخرین استثنا در `filesDir/last-crash.txt` ذخیره می‌شود
  (`data/CrashLog.kt` + `MainActivity`) و کارت «آخرین خطای برنامه» در صفحه‌ی
  بازیکنان متن آن را نشان می‌دهد. کرش دیگر بی‌سرنخ نیست.
- **ترمیم سیوهای قدیمی:** همه‌ی فیلدهای مرجعِ `Character` بعد از خواندن JSON
  ترمیم می‌شوند (Gson نمونه را بدون سازنده می‌سازد و کلید غایب/`null` را روی
  نوع غیر-null می‌نشاند) — این یکی از مسیرهای کلاسیک «صفحه باز می‌شود و
  برنامه بسته می‌شود» بود.
- **محافظ خواندن تنظیمات (`data/SafePrefs.kt`):** `SharedPreferences.getX` روی
  ناسازگاری نوع `ClassCastException` می‌دهد، نه `null`. تب مبارزه با هر بار
  باز شدن کلید `runtime_<id>` را می‌خواند؛ حالا این خواندن‌ها از توابع safe رد
  می‌شوند، کلید خراب یک‌بار پاک می‌شود و نامش در داشبورد دیده می‌شود
  (تست: `SafePrefsTest`).
- **`DiceSource`:** `d0/d1` دیگر استثنا نمی‌دهد (وجه‌ها محافظت‌شده) و درخواست
  دستی GM دست‌نخورده می‌ماند.

### پاس پنجم: درمان Medtech از BIO به Role Helper منتقل شد (و واقعی شد)

- **صفحه‌ی Medtech در GM Tools بازنویسی شد** (`ui/screens/MedtechGmScreen.kt`).
  قبلاً فقط مرجع و سناریو بود؛ حالا بیمار از لیست خودِ کاراکترهای برنامه
  انتخاب می‌شود، وضعیتش **از همان برگه** خوانده می‌شود (Seriously/Mortally
  Wounded، HP، فهرست Critical Injury) و درمان روی همان برگه می‌نشیند:
  - **Stabilize:** DV از وضعیت بیمار (۱۰/۱۳/۱۵)، موفقیت در DV15 → ۱ HP و بیهوشی.
  - **Quick Fix (۱ دقیقه) / Treatment (۴ ساعت)** برای هر Critical Injury، با
    نمایش آیتم‌به‌آیتم «چه مشکلی دارد» و DV/مهارت مورد نیاز؛ Treatment موفق
    آن جراحت را از برگه حذف می‌کند (`clearCriticalInjury`).
  - **Pharmaceutical:** ساخت/تزریق با TECH + Medical Tech در برابر DV13؛
    Speedheal واقعاً HP می‌دهد (BODY + WILL)، Antibiotic/Stim/Surge اثر موقت
    می‌گذارند و Rapidetox اثر را پاک می‌کند. داروی یادنگرفته هشدار می‌گیرد.
  - **Recovery:** ثبت روز استراحت (روزی BODY HP) با هشدار جراحت‌های حرکتی.
  - **دوره‌ی Paramedic:** چون Paramedic یک مهارت آموزش‌دیده است (۲ هفته + ۶۰ IP)
    و Medtech بدون آن فقط First Aid دارد.
- **تابلوی بیماران از تب BIO حذف شد** و همان‌جا زیر انتخاب بیمار نشست؛ هر ردیف
  می‌تواند به یک کاراکتر واقعی وصل شود (`MedtechPatient.linkedCharacterId`).
- **قواعد در `data/MedtechCareRules.kt`** جمع شد (DVها، فرمول‌ها، اثر داروها،
  بازیابی) تا UI و تب COMBAT یک عدد واحد بدهند + تست `MedtechCareTest`
  (سطح Paramedic، DV پایدارسازی، Speedheal/BODY+WILL، تعداد dose، بازیابی).

### پاس ششم: پنج مشکل گزارش‌شده + تکمیل تب COMBAT

- **نام برنامه:** `app_name` → «CPR KIT» (نامی که روی صفحه‌ی نصب دیده می‌شود)
  و تیتر صفحه‌ی ورود → «CYBER KIT».
- **قابلیت Media از BIO به Role Helper منتقل شد** (`ui/screens/MediaGmScreen.kt`):
  Rumorهای passive، Investigation فعال روی DV هر سطح و تابلوی پرونده‌ها با
  Believability و انتشار. شاخه‌ی Media در BIO فقط یک اشاره به
  GM Tools › ROLE HELPERS › MEDIA است.
- **دکمه‌های رول Lawman:** علت به‌هم‌ریختگی متن، `Arrangement.SpaceBetween` در
  `RollButton` بود که متن لاتین مثل `1d10 ≤ 4` را حرف‌به‌حرف می‌شکست؛ حالا
  چیدمان ترتیبی است و مقدار تاس داخل کادر با `softWrap=false` و جهت LTR می‌نشیند.
- **سیستم Backup واقعی شد** (`data/BackupRules.kt`): جدول Core (1d10 مساوی یا
  کمتر از Rank، 1d6 برای رسیدن، تاس ۶ = یک رده بالاتر، در Rank ۱۰ دو گروه)،
  دکمه‌ی «ساخت NPC نیروها» که همان تمپلیت واقعی `NpcData` را می‌سازد و به فهرست
  شرکت‌کننده‌های نبرد اضافه می‌کند، و ثبت وضعیت «نیرو در راه» روی برگه.
  `BackupRulesTest` هر رده را روی تمپلیت واقعی چک می‌کند.
- **یکسان‌سازی طراحی Role Helper** (`ui/components/RoleHelperScaffold.kt`):
  Exec / Lawman / Fixer دیگر دیالوگ نیستند و مثل Nomad / Medtech / Media
  صفحه‌ی کامل با کارت‌های یکسان دارند؛ صفحه‌ی Nomad هم به همان قالب آمد.
- **تب COMBAT (این پاس):**
  - کارت `ACTIVE COMBAT EFFECTS` فقط اثرهای مؤثر در نبرد را نشان می‌دهد، هر
    کدام با برچسب فارسی و اثر مکانیکی خودش؛ شمارنده‌های داخلی (مثل «moved this
    turn») و اثرهای دارویی/درمانی/پشتیبان از این کارت بیرون‌اند و فقط شمرده
    می‌شوند (`data/CombatEffects.kt` + `CombatEffectsTest`).
  - اکشن‌های بدن (GRAB / CHOKE / THROW / GRAB ITEM / ESCAPE GRAPPLE) با کلید
    «GM: free fire» هم باز می‌شوند و بالای همان بخش، دلیلِ خاموش‌بودنِ هر دکمه
    نوشته می‌شود (قبلاً دکمه‌های خاکستری بدون توضیح می‌ماندند).
  - تنها راه شلیک، دکمه‌ی FIRE پایین صفحه است: دکمه‌های `SUPPRESSIVE FIRE` و
    `THROW` از فرم سلاح حذف شدند و Suppressive Fire به‌جای آن یک **Mode** است
    (Single / Autofire / Suppressive) که همان FIRE آن را اجرا می‌کند.
  - سلاح‌های نصب‌شده روی وسیله مسیر جدا دارند ولی حالا همان قاعده‌ی Action و
    همان کلید GM را رعایت می‌کنند.

### کارهایی که هنوز در این نسخه نیست (نقشه‌ی کار بعدی)

این‌ها در بازبینی شناسایی و طرح‌ریزی شده‌اند ولی **در همین اسنپ‌شات اعمال نشده‌اند**:

- جمع‌کردن قاعده‌های وابسته به «نام آیتم» در یک جدول واحد (مثل
  `data/ItemRules.kt`) + تستی که هر برچسب را روی کاتالوگ واقعی امتحان کند.
- خواندن ظرفیت‌ها (Slot سایبردک، هزینه‌ی هاردور) از رکورد فروشگاه به‌جای حدس از نام.
- `DiceSource.pick/pickOrNull/pickDistinct`: انتخاب از فهرست (`list.random()`,
  `shuffled()`) هم یک تاس است و باید از `DiceSource` رد شود؛ تست ساختاری فعلی فقط
  `Random`/`(a..b).random()` را می‌گیرد.
- شکستن فایل‌های غول (بالا) + تست‌دارکردن جدول‌های GM (`generateNetArchitecture`,
  `generateMarket`) که الان داخل `GmToolsScreen` هستند و تست ندارند.

---

## تست‌ها

```bash
./gradlew test
```

همین‌ها به‌علاوه‌ی `assembleDebug`، `assembleRelease` و `lintDebug` در CI هم اجرا می‌شوند:
[`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml) روی هر push/PR
(JDK 21، چون Gradle 9.x و AGP 9.x به JDK 17+ نیاز دارند).

| فایل | چه چیزی را قفل می‌کند |
| --- | --- |
| `RulesRegressionTest` | اعداد و فهرست‌های قواعدی: مهارت‌های Difficult، هزینه‌ی IP، HP، ablation زره، Humanity/EMP، NPCها، فروشگاه |
| `CriticalInjuryDiceTest` | انتخاب جراحت بحرانی **همیشه** از `DiceSource` می‌آید — حتی انتخاب آخر؛ هیچ تاسی بی‌صدا ساخته نمی‌شود |
| `LanTrustTest` | مرز اعتماد سشن: فیلدهای GM-Only نه ارسال می‌شوند نه پذیرفته؛ diff هر فیلدِ مدل `Character` را می‌بیند |
| `TinyHttpLimitsTest` | سقف اندازه‌ی body سرور محلی (جلوگیری از OOM و مرگ سشن) |
| `RoleAssistantDataTest` | داده‌های دستیار نقش‌ها (داروها، DVها، جدول Credibility) |
| `DiceSourceTest` | «تنها منبع تاس»: در حالت دستی هر تاس از GM پرسیده می‌شود؛ افتادن به تصادفی همیشه گزارش می‌شود |
| `DiceSourceEnforcementTest` | تست ساختاری روی کل سورس: هیچ `Random`/`(a..b).random()` بیرون از `DiceSource` نباشد |
| `MedtechCareTest` | DV پایدارسازی، روند Paramedic، Speedheal (BODY+WILL)، تعداد dose و بازیابی روزانه |
| `BackupRulesTest` | شرایط تماس Backup (1d10 ≤ Rank)، جدول ۱ تا ۱۰، تاس ۶ = رده بالاتر، و اینکه هر رده به تمپلیت واقعی NPC وصل است |
| `CombatEffectsTest` | طبقه‌بندی اثرها: شمارنده‌های داخلی هیچ‌وقت نمایش داده نشوند و هر اثر رزمی برچسب و توضیح مکانیکی داشته باشد |

هر قاعده‌ای که «اگر بی‌صدا خراب شود سر میز فاجعه می‌سازد» باید اینجا یک تست داشته باشد.

---

## House Ruleها

قواعدِ کتاب به‌صورت پیش‌فرض **بدون تغییر** پیاده شده‌اند. مواردی که کتاب صریح نیست
یا این میز تصمیم دیگری گرفته، در [`docs/HOUSE_RULES.md`](docs/HOUSE_RULES.md) فهرست شده‌اند
(و در همان نقطه از کد هم کامنت خورده‌اند). اگر برای میز خودت چیزی را عوض کردی، همان‌جا اضافه کن.

---

## زبان و ترجمه (i18n)

سه حالت: **English**، **دوزبانه** (سرخط‌ها انگلیسی، توضیحات فارسی — حالت پیش‌فرض) و **فارسی**.

طراحی: کلید ترجمه، **خودِ رشته‌ی انگلیسیِ داخل سورس** است و ترجمه فقط هنگام نمایش اعمال
می‌شود (`data/Localization.kt`). اگر ترجمه‌ای نباشد، همان متن انگلیسی نشان داده می‌شود —
پس می‌توان ترجمه را تدریجی جلو برد بدون اینکه برنامه بشکند.

- ۱۹ بسته‌ی ترجمه در `app/src/main/assets/i18n/` (مجموعاً ۳٬۸۹۶ کلید).
- افزودن بسته: فایل `fa.NN-name.json` را در همان پوشه بگذار و نامش را به
  `LocalizationLoader.PHASE_FILES` اضافه کن.

> **قانون طلایی:** رشته‌های انگلیسی داخل سورس را عوض نکن؛ آن‌ها شناسه‌اند — هم برای ترجمه
> و هم در فایل‌های سیو. (این قانون نشانه‌ی یک بدهی معماری است که در بالا گفته شد:
> وابستگی منطق به نام‌های نمایشی.)

**راست‌به‌چپ:** چیدمان کل اپ عمداً LTR نگه داشته شده (`supportsRtl="false"`) تا ترتیب تب‌ها
با زبان دستگاه وارونه نشود؛ متن فارسی به‌صورت موضعی RTL می‌شود. تکه‌های لاتین داخل جمله‌ی
فارسی با `U+2066/U+2069` ایزوله می‌شوند تا نقطه و ویرگول جابه‌جا نشوند
(`ui/components/PersianText.kt` — دلیلش در همان فایل توضیح داده شده).

---

## سشن محلی: مدل امنیتی

- سرور فقط روی شبکه‌ی محلی و با HTTP ساده است (`network_security_config.xml`)؛ اپ هیچ
  ترافیک اینترنتی ندارد.
- هر نقش یک کد شش‌رقمی (از `SecureRandom`) و هر اتصال یک توکن دارد؛ اتصال دوم به یک نقش
  رد می‌شود مگر همان دستگاه برگردد یا نقش stale شده باشد.
- **GM مرجع حقیقت است.** بازیکن هرگز مستقیم چیزی را تغییر نمی‌دهد؛ درخواست می‌فرستد و
  GM تأیید می‌کند.
- آنچه GM تأیید می‌کند **diff محاسبه‌شده در سمت GM** است (`LanTrust.diffFields`)، نه خلاصه‌ای
  که کلاینت نوشته؛ و فیلدهای فقط-GM (`isDead`, `criticalInjuries`, `deathSavePenalty`,
  `notes`, `npcCategory`, `npcTier`, `isAlly`, `appliedTransactionIds`, `baseEmp`, `baseBody`,
  `reputation`, `sheetUid`, `id`) هرگز از پیشنهاد بازیکن پذیرفته نمی‌شوند.
- سرور سقف اندازه‌ی body و سقف تعداد اتصال هم‌زمان دارد تا یک کلاینت خراب نتواند کل
  سشن را بیندازد.

---

## Legal / حقوقی

این پروژه محتوای طرفداری (fan content) است و تحت
[Homebrew Content Policy](https://rtalsoriangames.com/homebrew-content-policy/) شرکت
R. Talsorian Games ارائه می‌شود. خلاصه‌ی تعهداتی که این مخزن رعایت می‌کند:

1. **کاملاً رایگان** است و رایگان می‌ماند: بدون فروش، بدون paywall، بدون اشتراک،
   بدون تبلیغات و بدون خرید درون‌برنامه‌ای.
2. **عنوان** محصول با نام بازی شروع نمی‌شود؛ نام بازی فقط به‌شکل توصیفی
   («… for use with Cyberpunk RED») به‌کار می‌رود. به همین دلیل برچسب لانچر `GM Kit` است
   و نام کامل در `app/src/main/res/values/strings.xml` (`app_full_name`) و در صفحه‌ی
   تنظیمات اپ نشان داده می‌شود. اگر روزی خواستی عمومی منتشر کنی، صفحه‌ی خوش‌آمد
   (`ui/screens/WelcomeScreen.kt`) هم که «CYBERPUNK / GM TOOLKIT» را بزرگ نشان می‌دهد
   باید به همان شکل توصیفی تغییر کند؛ برای استفاده‌ی شخصی لازم نیست.
3. **توزیع** از بازارهای retail (DriveThruRPG، itch.io و مانند آن) انجام نمی‌شود.
4. **متن کتاب** برای اپ‌ها فقط به‌شکل خلاصه مجاز است. این بند جایی است که این مخزن
   هنوز کامل رعایت نمی‌کند: توضیحات آیتم‌ها در `data/StoreCatalog.kt`،
   `data/RuleReferenceData.kt` و اثر جراحت‌ها در `data/CriticalInjuries.kt` طولانی‌تر از
   «خلاصه + ارجاع به صفحه» هستند. **تا وقتی عمومی منتشر نکنی مشکلی نیست**؛ پیش از
   انتشار باید به شکل خلاصه و ارجاع صفحه (مثلاً «+2 Personal Grooming w/ Chemskin — CPR ص. ۳۵۹»)
   کوتاه شوند.
5. **هنر و تصاویر**: ۱۰۳ نقشه‌ی نبرد و آیکون‌های NPC باید ساخته‌ی خودت یا دارای مجوز باشند؛
   اگر هرکدام از کتاب یا بازی ویدیویی برداشته شده‌اند، پیش از انتشار حذفشان کن.
6. سلب‌مسئولیت بالا در سه جا هست: همین README، `strings.xml` (`app_legal_notice`) و
   صفحه‌ی تنظیمات اپ.

---

## وضعیت و نقشه‌ی راه

- [x] موتور قواعد کامل (چک، آسیب، زره، جراحت بحرانی، Death Save، Humanity)
- [x] ساخت شخصیت (Streetrat / Complete Package) و Lifepath
- [x] نقشه‌ی نبرد، NPC، فروشگاه، کمپین و گیگ‌ها
- [x] سشن محلی LAN با QR و دروازه‌ی تأیید GM
- [x] ذخیره‌سازی مقاوم به crash با rollback
- [x] تست‌های رگرسیون قواعد + مرز اعتماد شبکه
- [ ] CI (GitHub Actions) برای `test` و `lint`
- [ ] شکستن `CharacterViewModel` به چند ViewModel/UseCase + تزریق وابستگی
- [ ] جای‌گذاری نام‌های نمایشی با شناسه‌های پایدار در منطق قواعد
- [ ] کوتاه‌کردن متن آیتم‌ها به خلاصه + ارجاع صفحه (پیش از انتشار عمومی)
- [ ] بسته‌بندی نقشه‌ها با Play Asset Delivery / دانلود درون‌برنامه‌ای
- [ ] تکمیل حالت انگلیسی (`effectEn` برای جراحت‌ها و بقیه‌ی متن‌های داده‌ای)
- [ ] اسکرین‌شات در README (`docs/screenshots/`)

---

*ساخته‌شده برای سر میز خودم؛ اگر به درد میز تو هم خورد، خوشحال می‌شوم.*
