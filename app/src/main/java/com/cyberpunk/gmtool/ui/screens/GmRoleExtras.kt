package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.cyberpunk.gmtool.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel

private val XRed=Color(0xFFD32F2F); private val XCard=Color(0xFF1A1A1A); private val XWhite=Color(0xFFEAEAEA); private val XMuted=Color(0xFFAAAAAA)

@Composable fun RoleHelpersHub(
    vm: CharacterViewModel,
    onNomad: () -> Unit,
    onMedtech: () -> Unit,
    onMedia: () -> Unit,
    onExec: () -> Unit,
    onLawman: () -> Unit,
    onFixer: () -> Unit
) {
    // هر شش نقش یک صفحه‌ی کمکی GM دارند با قالب یکسان (RoleHelperScaffold).
    // خودِ Role Ability و توضیح قواعد همان نقش در تب BIO کاراکتر می‌ماند؛
    // این صفحات ابزار مدیریت میز هستند، نه جای قاعده‌ی اصلی.
    val roles = listOf(
        Triple("Nomad", "خانواده و ناوگان: پروفایل Family/Pack، Favor و Debt، تصمیم «الان Family وارد داستان شود؟» و Story Hook", onNomad),
        Triple("Medtech", "درمان واقعی کاراکترها: پایدارسازی، Quick Fix و Treatment جراحت‌ها، دارو و بازیابی", onMedtech),
        Triple("Media", "تصمیم‌گیری میز: مرحله‌ی تحقیق، ساخت سوژه و واکنش، و صحنه‌های آماده‌ی GM", onMedia),
        Triple("Exec", "تیم و منابع شرکت: ساخت Team Member به‌عنوان NPC، هزینه و اثر منبع، فشار شرکتی", onExec),
        Triple("Lawman", "Backup طبق Core: تماس، زمان رسیدن، ساخت نیروها به‌عنوان NPC و پرونده‌ها", onLawman),
        Triple("Fixer", "شبکه و معامله: Contacts روی برگه، Reach، Haggle و کارهای آماده", onFixer)
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxWidth()) {
                    Text(gtr("ROLE HELPERS"), color = XWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Text(
                        "ابزار کمک به GM برای اجرای Role Ability هر نقش: تصمیم‌گیری سریع، ساخت NPC لازم، جدول‌های آماده و صحنه‌های روی میز. توضیح خودِ Ability و Roll آن در تب BIO کاراکتر است.",
                        color = XMuted, fontSize = 12.sp, lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Right
                    )
                }
            }
        }
        items(roles) { (name, desc, open) ->
            Card(colors = CardDefaults.cardColors(containerColor = XCard), modifier = Modifier.fillMaxWidth().clickable { open() }) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(name.uppercase(), color = XWhite, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Text(desc, color = XMuted, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Right)
                    }
                }
            }
        }
    }
}

@Composable private fun HelperCard(title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF202020)),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
 com.cyberpunk.gmtool.ui.components.FaText(gtr(title),color=XRed,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth())
 com.cyberpunk.gmtool.ui.components.FaText(gtr(body),color=XWhite,fontSize=12.sp,lineHeight=20.sp,modifier=Modifier.fillMaxWidth())}}}

data class RoleGig(val role:String,val tier:String,val title:String,val pay:Int,val ip:Int,val brief:String,val difficulty:String,val gear:String)

private val tierData=listOf("0-2" to Triple(250,10,"آسان"),"3-4" to Triple(400,15,"معمولی"),"5-6" to Triple(750,25,"سخت"),"7-8" to Triple(1500,45,"خیلی سخت"),"9-10" to Triple(3000,80,"بحرانی"))

private val roleOrder = com.cyberpunk.gmtool.data.SoloGigData.roles

/**
 * ۲۰ کار برای هر Role = ۵ هسته × ۴ حالت.
 * توضیح هر کار از هستهٔ خودش می‌آید و حالت، یک قید اجرایی رویش می‌گذارد،
 * پس دیگر همهٔ کارهای یک Role یک متن تکراری نشان نمی‌دهند.
 */
private val jobs:List<RoleGig> = roleOrder.flatMap { role ->
    val seeds = com.cyberpunk.gmtool.data.SoloGigData.seeds[role].orEmpty()
    tierData.flatMapIndexed { i,(rep,data) ->
        com.cyberpunk.gmtool.data.SoloGigData.variants.mapIndexed { v,(variantName,variantNote) ->
            val (pay,ip,diff)=data
            val seed = seeds.getOrNull(i) ?: seeds.lastOrNull()
                ?: com.cyberpunk.gmtool.data.SoloGigData.Seed(role,"",  "")
            RoleGig(
                role, rep,
                "${seed.title} — $variantName",
                pay+v*50, ip,
                seed.brief + "\n\n" + variantNote,
                "$diff|$rep",
                com.cyberpunk.gmtool.data.SoloGigData.gearFor(role, seed)
            )
        }
    }
}

@Composable fun SoloJobsScreen(){
 var role by remember{mutableStateOf("Solo")}; var tier by remember{mutableStateOf("ALL")}; val roles=com.cyberpunk.gmtool.data.SoloGigData.roles
 Column(Modifier.fillMaxSize().padding(horizontal=12.dp)){
   com.cyberpunk.gmtool.ui.components.FaText(gtr("solo gigs"),color=XWhite,fontSize=18.sp,fontWeight=FontWeight.Black,modifier=Modifier.fillMaxWidth())
   com.cyberpunk.gmtool.ui.components.FaText(gtr("20 gigs per Role, filtered by REP"),color=XMuted,fontSize=11.sp,modifier=Modifier.fillMaxWidth())
   ScrollableTabRow(selectedTabIndex=roles.indexOf(role),containerColor=Color.Transparent,edgePadding=0.dp){roles.forEach{r->Tab(selected=role==r,onClick={role=r},text={Text(gtr(r),color=if(role==r)XRed else XMuted)})}}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)){listOf("ALL","0-2","3-4","5-6","7-8","9-10").forEach{t->FilterChip(selected=tier==t,onClick={tier=t},label={Text(gtr(t),fontSize=10.sp)})}}
   LazyColumn(contentPadding=PaddingValues(bottom=90.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
     item{
       val g=com.cyberpunk.gmtool.data.SoloGigData.roleGuide[role]
       if(g!=null) Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF1B1B1B)),modifier=Modifier.fillMaxWidth()){
         Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
           com.cyberpunk.gmtool.ui.components.FaText(gtr("How to run these gigs"),color=XRed,fontSize=12.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth())
           com.cyberpunk.gmtool.ui.components.FaText(gtr(g.first),color=XWhite,fontSize=11.sp,lineHeight=19.sp,modifier=Modifier.fillMaxWidth())
           com.cyberpunk.gmtool.ui.components.FaText(gtr(g.second),color=Color(0xFFFFB300),fontSize=11.sp,lineHeight=19.sp,modifier=Modifier.fillMaxWidth())
           com.cyberpunk.gmtool.ui.components.FaText(gtr(g.third),color=Color(0xFF4CAF50),fontSize=11.sp,lineHeight=19.sp,modifier=Modifier.fillMaxWidth())
         }
       }
     }
     items(jobs.filter{it.role==role&&(tier=="ALL"||it.tier==tier)}){j->
       Card(colors=CardDefaults.cardColors(containerColor=XCard),modifier=Modifier.fillMaxWidth()){
         Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
           com.cyberpunk.gmtool.ui.components.FaText(gtr(j.title),color=XWhite,fontWeight=FontWeight.Bold,fontSize=16.sp,modifier=Modifier.fillMaxWidth())
           // خط آمار کاملاً لاتین/عددی است؛ LTR بماند تا «250eb» و «REP 3-4» برعکس نشوند.
           CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr){
             Text(gtr("%1s • %2seb • %3s IP", gtr("%1s • REP %2s", j.difficulty.substringBefore('|'), j.difficulty.substringAfter('|')), j.pay, j.ip),color=XRed,fontSize=12.sp,modifier=Modifier.fillMaxWidth())
           }
           com.cyberpunk.gmtool.ui.components.FaText(gtr(j.brief),color=XWhite,fontSize=12.sp,lineHeight=20.sp,modifier=Modifier.fillMaxWidth())
           HorizontalDivider(color=XRed.copy(.25f))
           com.cyberpunk.gmtool.ui.components.FaText("تجهیزات پیشنهادی: ${j.gear}",color=XMuted,fontSize=11.sp,lineHeight=18.sp,modifier=Modifier.fillMaxWidth())
         }
       }
     }}
 }
}

data class Ally(val tier:String,val minRep:Int,val role:String,val name:String,val cost:Int,val specialty:String,val hp:Int,val sp:Int,val ability:String,val skills:String,val gear:String,
 val stats:com.cyberpunk.gmtool.data.Stats = com.cyberpunk.gmtool.data.Stats(),
 val skillTotals:List<Pair<String,Int>> = emptyList())

/**
 * STAT و عدد مهارت همراهان.
 * بدون این‌ها GM نمی‌توانست همراه را در Combat بگرداند — فقط HP/SP داشت.
 * الگو همان NpcData است: عدد مهارت یعنی STAT + Level (عدد نهایی، آماده‌ی افزودن 1d10).
 * Tier سطح را بالا می‌برد: Rookie → Elite.
 */
private fun allyStats(role:String, i:Int): com.cyberpunk.gmtool.data.Stats {
    // i = 0 Rookie، 1 Professional، 2 Veteran، 3 Elite
    val b = listOf(5,6,7,8)[i]      // STAT شاخص نقش
    val g = listOf(4,5,6,6)[i]      // STAT عمومی
    return when(role){
        "Solo"      -> com.cyberpunk.gmtool.data.Stats(g,b,b,g,g,b,g,b,b,g)
        "Netrunner" -> com.cyberpunk.gmtool.data.Stats(b,b,g,b,g,b,g,g,g,g)
        "Tech"      -> com.cyberpunk.gmtool.data.Stats(b,g,b,b,g,g,g,g,g,g)
        "Medtech"   -> com.cyberpunk.gmtool.data.Stats(b,g,b,b,g,b,g,g,g,b)
        "Nomad"     -> com.cyberpunk.gmtool.data.Stats(g,b,b,b,g,b,g,b,b,g)
        "Fixer"     -> com.cyberpunk.gmtool.data.Stats(b,g,g,g,b,b,g,g,g,b)
        "Lawman"    -> com.cyberpunk.gmtool.data.Stats(g,b,b,g,b,b,g,b,b,g)
        "Media"     -> com.cyberpunk.gmtool.data.Stats(b,g,g,g,b,b,g,g,g,b)
        "Rockerboy" -> com.cyberpunk.gmtool.data.Stats(g,g,b,g,b,b,g,g,g,b)
        else        -> com.cyberpunk.gmtool.data.Stats(b,g,g,b,b,b,g,g,g,b)
    }
}

/** عدد نهایی مهارت‌های کلیدی هر نقش (STAT + Level). */
private fun allySkillTotals(role:String, i:Int): List<Pair<String,Int>> {
    val hi = listOf(10,12,14,16)[i]   // مهارت اصلی
    val md = listOf(8,10,12,14)[i]    // مهارت پشتیبان
    val lo = listOf(6,8,10,12)[i]     // مهارت عمومی
    return when(role){
        "Solo"      -> listOf("Handgun" to hi,"Shoulder Arms" to hi,"Evasion" to md,"Athletics" to md,"Perception" to lo)
        "Netrunner" -> listOf("Interface" to hi,"Electronics/Security Tech" to md,"Stealth" to md,"Perception" to lo)
        "Tech"      -> listOf("Basic Tech" to hi,"Electronics/Security Tech" to hi,"Cybertech" to md,"Perception" to lo)
        "Medtech"   -> listOf("Medical Tech" to hi,"First Aid" to hi,"Human Perception" to md,"Perception" to lo)
        "Nomad"     -> listOf("Drive Land Vehicle" to hi,"Pilot Air Vehicle" to md,"Handgun" to md,"Basic Tech" to lo)
        "Fixer"     -> listOf("Persuasion" to hi,"Trading" to hi,"Streetwise" to md,"Human Perception" to md)
        "Lawman"    -> listOf("Handgun" to hi,"Shoulder Arms" to md,"Interrogation" to md,"Perception" to md)
        "Media"     -> listOf("Persuasion" to hi,"Photography/Film" to hi,"Human Perception" to md,"Stealth" to lo)
        "Rockerboy" -> listOf("Performance" to hi,"Persuasion" to hi,"Human Perception" to md,"Wardrobe & Style" to lo)
        else        -> listOf("Persuasion" to hi,"Bureaucracy" to hi,"Accounting" to md,"Human Perception" to md)
    }
}
private val allyNames=mapOf("Solo" to listOf("Jet Rabbit","Coyote","Ghostdog","Falcon"),"Netrunner" to listOf("Glitch","Trace","Neon Evil Lyn","Daemon"),"Tech" to listOf("Spanner","Bolt","Ironclad","Gearsmith"),"Medtech" to listOf("Doc June","Nightingale","Doc Reaper","Doc Miracle"),"Nomad" to listOf("Dusty","Rustwind","Wildtrack","Highway King"),"Fixer" to listOf("Patches","Smooth Talk","Old Money","Mr. Chrome"),"Lawman" to listOf("Officer Kane","Steel Badge","Blue Line","Iron Fist"),"Media" to listOf("Lens","Focus","Truthseeker","Revelation"),"Rockerboy" to listOf("Echo Beat","Amplify","Stage Dive","Silver Vox"),"Exec" to listOf("Ivy","White Room","Boardwalk","The Board"))
private val allies=allyNames.flatMap{(r,ns)->ns.mapIndexed{i,n->
 val tier=listOf("Rookie","Professional","Veteran","Elite")[i]; val rep=listOf(0,3,5,7)[i]; val baseCost=listOf(300,600,1500,4000)[i]; val hp=listOf(22,30,40,48)[i] + if(r in listOf("Solo","Lawman","Nomad"))4 else -2; val sp=listOf(7,11,13,15)[i]
 val spec=when(r){"Netrunner"->"نفوذ و پشتیبانی NET";"Tech"->"تعمیر و Field Fix";"Medtech"->"درمان و Stabilize";"Nomad"->"رانندگی و استخراج";"Fixer"->"Contact و معامله";"Lawman"->"امنیت و اجرای قانون";"Media"->"تحقیق و پوشش رسانه‌ای";"Rockerboy"->"جمعیت و نفوذ اجتماعی";"Exec"->"دسترسی شرکتی";else->"پشتیبانی رزمی"}
 val ability=when(r){"Solo"->listOf("Aggressive","Suppressive Fire","Born Combat","Legend in Action")[i];"Netrunner"->listOf("Support Hack","Data Breach","System Ghost","Black ICE Master")[i];"Tech"->listOf("Fix It","Field Fix","Field Improviser","Build Solutions")[i];"Medtech"->listOf("Field Patch","Stabilize","Lifesaver","Second Chance")[i];"Nomad"->listOf("Getaway Driver","Get You Out","Full Throttle","Road Master")[i];"Fixer"->listOf("Pull Some Strings","Make It Happen","Pull Strings","Make It Happen")[i];"Lawman"->listOf("Call It In","Authority","Enforce & Protect","Tactical Advantage")[i];"Media"->listOf("Publicity","Public Leverage","Story Breaker","Uncover the Truth")[i];"Rockerboy"->"Crowd Control";else->listOf("Office Access","Corporate Access","Open Doors","Strategic Edge")[i]}
 val skills=when(r){"Solo"->"Handgun/Rifle • Athletics • Awareness";"Netrunner"->"Interface • Electronics/Security Tech • Stealth";"Tech"->"Tech • Repair • Electronics/Security Tech";"Medtech"->"Medicine • First Aid • Human Perception";"Nomad"->"Drive Land Vehicle • Pilot • Handgun";"Fixer"->"Persuasion • Human Perception • Streetwise";"Lawman"->"Handgun/Rifle • Law • Human Perception";"Media"->"Persuasion • Photography/Film • Stealth";"Rockerboy"->"Performance • Persuasion • Human Perception";else->"Accounting • Persuasion • Bureaucracy"}
 val gear=when(r){"Solo","Lawman"->"Assault Rifle / Pistol • Armor";"Netrunner"->"Light Pistol • Netrunning Deck";"Tech"->"Light Pistol/SMG • Tech Tool Kit";"Medtech"->"Light Pistol • Medtool";"Nomad"->"SMG • Vehicle";"Fixer"->"Light Pistol • Agent/Contacts";"Media"->"Light Pistol • Camera Drone";"Rockerboy"->"Light Pistol • Guitar";else->"Light Pistol • Datapad"}
 Ally(tier,rep,r,n,baseCost + if(r in listOf("Tech","Lawman","Exec")) listOf(0,100,-100,500)[i] else 0,spec,hp,sp,ability,skills,gear,allyStats(r,i),allySkillTotals(r,i))
}}
private fun rolePortrait(role:String)=when(role){"Solo"->R.drawable.cpr_solo2;"Netrunner"->R.drawable.cpr_netrunner2;"Tech"->R.drawable.cpr_tech2;"Medtech"->R.drawable.cpr_medtech2;"Nomad"->R.drawable.cpr_nomad2;"Fixer"->R.drawable.cpr_fixer2;"Lawman"->R.drawable.cpr_lawman2;"Media"->R.drawable.cpr_media2;"Rockerboy"->R.drawable.cpr_rockerboy2;else->R.drawable.cpr_exec2}

/** برچسب کوچک «هوم‌برو» — یعنی این محتوا ساخته‌ی برنامه است نه کتاب Core. */
@Composable
private fun HomebrewBadge() {
    Text(
        gtr("Homebrew"),
        color = Color(0xFF9575CD), fontSize = 9.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color(0xFF9575CD).copy(alpha = .14f), CutCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun HireableAlliesScreen() {
    var tier by remember { mutableStateOf("Rookie") }
    var selected by remember { mutableStateOf<Ally?>(null) }
    val tiers = listOf("Rookie", "Professional", "Veteran", "Elite")

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(gtr("HIREABLE ALLIES"), color = XWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            HomebrewBadge()
        }
        com.cyberpunk.gmtool.ui.components.FaText(
            "Role را انتخاب کن؛ جزئیات همراه متناسب با Tier نمایش داده می‌شود. این همراه‌ها محتوای این برنامه‌اند، نه Core.",
            color = XMuted, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth())

        ScrollableTabRow(
            selectedTabIndex = tiers.indexOf(tier),
            containerColor = Color.Transparent,
            edgePadding = 0.dp
        ) {
            tiers.forEach { t ->
                Tab(
                    selected = tier == t,
                    onClick = { tier = t },
                    text = { Text(gtr(t), color = if (tier == t) XRed else XMuted) }
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allies.filter { it.tier == tier }) { ally ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = XCard),
                    modifier = Modifier.fillMaxWidth().clickable { selected = ally }
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(rolePortrait(ally.role)),
                            contentDescription = ally.role,
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(ally.role.uppercase(), color = XWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("برای دیدن مشخصات لمس کن", color = XRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    selected?.let { ally ->
        AlertDialog(
            onDismissRequest = { selected = null },
            containerColor = XCard,
            title = { Text("${ally.role.uppercase()} — ${ally.name}", color = XWhite, fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(Modifier.heightIn(max = 540.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  item {
                    Text(gtr("%1s • MIN REP %2s • %3seb / Mission", ally.tier, ally.minRep, ally.cost), color = XRed, fontWeight = FontWeight.Bold)
                    Text(gtr("HP %1s • SP %2s", ally.hp, ally.sp), color = XWhite)
                  }
                  // ── STATها: بدون این‌ها نمی‌شد همراه را در Combat گرداند ──
                  item {
                    Column(Modifier.fillMaxWidth().background(Color(0xFF141414), CutCornerShape(7.dp)).padding(9.dp)) {
                      com.cyberpunk.gmtool.ui.components.FaText("STATها", color = XRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                      Spacer(Modifier.height(5.dp))
                      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        val st = ally.stats
                        listOf(
                          listOf("INT" to st.int, "REF" to st.ref, "DEX" to st.dex, "TECH" to st.tech, "COOL" to st.cool),
                          listOf("WILL" to st.will, "LUCK" to st.luck, "MOVE" to st.move, "BODY" to st.body, "EMP" to st.emp)
                        ).forEach { row ->
                          Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            row.forEach { (k, v) ->
                              Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(k, color = XMuted, fontSize = 9.sp)
                                Text("$v", color = XWhite, fontSize = 15.sp, fontWeight = FontWeight.Black)
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                  // ── عدد نهایی مهارت‌ها: آماده‌ی افزودن 1d10 ──
                  item {
                    Column(Modifier.fillMaxWidth().background(Color(0xFF141414), CutCornerShape(7.dp)).padding(9.dp)) {
                      com.cyberpunk.gmtool.ui.components.FaText("عدد مهارت (STAT + Level) — فقط 1d10 اضافه کن", color = XRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                      Spacer(Modifier.height(5.dp))
                      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(Modifier.fillMaxWidth()) {
                          ally.skillTotals.forEach { (k, v) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                              Text(k, color = XWhite, fontSize = 12.sp)
                              Text("$v", color = Color(0xFFFFB300), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                          }
                        }
                      }
                    }
                  }
                  item {
                    com.cyberpunk.gmtool.ui.components.FaText("نقش: ${ally.specialty}", color = XWhite, modifier = Modifier.fillMaxWidth())
                    Text(gtr("Role Ability: %1s", ally.ability), color = XWhite)
                    Text(gtr("Gear: %1s", ally.gear), color = XMuted)
                  }
                  item {
                    HorizontalDivider(color = XRed.copy(alpha = 0.3f))
                    Spacer(Modifier.height(6.dp))
                    com.cyberpunk.gmtool.ui.components.FaText(
                        "این NPC همراه مأموریتی است و GM کنترلش می‌کند. Exec Team Member و Lawman Backup همچنان سیستم‌های جداگانه‌ی Role Ability هستند.",
                        color = XMuted, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth()
                    )
                  }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) {
                    Text("بستن", color = XRed)
                }
            }
        )
    }
}
