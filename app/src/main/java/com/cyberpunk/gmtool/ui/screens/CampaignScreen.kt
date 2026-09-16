package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.*
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel
import com.cyberpunk.gmtool.ui.components.CoreItemDetailDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
// توابع توسعه‌ی دامنه‌های تفکیک‌شده‌ی ViewModel در همین پکیج‌اند.
import com.cyberpunk.gmtool.viewmodel.*

private fun campaignPinHash(v:String)=MessageDigest.getInstance("SHA-256").digest(v.toByteArray()).joinToString(""){"%02x".format(it)}

private val CRed=Color(0xFFD32F2F); private val CWhite=Color(0xFFE0E0E0); private val CMuted=Color(0xFFAAAAAA); private val CCard=Color(0xFF1A1A1A)

@Composable
fun CampaignScreen(viewModel: CharacterViewModel, onExit: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { CampaignStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val generation by viewModel.dataGeneration.collectAsState()
    val epoch = remember(generation) { PersistenceRuntime.currentEpoch() }
    val chars by viewModel.characters.collectAsState()
    var campaigns by remember(generation) { mutableStateOf<List<Campaign>>(emptyList()) }
    var opened by remember(generation) { mutableStateOf<Long?>(null) }
    var hydrated by remember(generation) { mutableStateOf(false) }
    var addCampaign by remember { mutableStateOf(false) }
    var deleteCampaignId by remember { mutableStateOf<Long?>(null) }
    var showScript by remember { mutableStateOf(false) }
    var pinTarget by remember { mutableStateOf<Campaign?>(null) }
    var pinManage by remember { mutableStateOf<Campaign?>(null) }

    fun persist(next: List<Campaign>) { campaigns = next }

    LaunchedEffect(generation) {
        val loaded = withContext(Dispatchers.IO) { store.load() }
        campaigns = loaded
        // همیشه با فهرست کمپین‌ها شروع کن تا بتوان بین کمپین‌ها جابه‌جا شد.
        // (قبلاً مستقیم وارد کمپین فعال می‌شد و فهرست هرگز دیده نمی‌شد.)
        opened = null
        hydrated = true
    }

    // Debounced background persistence: typing GM notes never serializes a long campaign on the UI thread.
    LaunchedEffect(campaigns, generation, hydrated) {
        if (!hydrated) return@LaunchedEffect
        delay(250)
        val snapshot = campaigns
        withContext(Dispatchers.IO) { store.save(snapshot, epoch) }
    }

    val current = campaigns.firstOrNull { it.id == opened }
    LaunchedEffect(chars.map { it.id to it.appliedTransactionIds.size }, generation, hydrated) {
        if (!hydrated) return@LaunchedEffect
        val valid = chars.map { it.id }.toSet()
        val byId = chars.associateBy { it.id }
        val cleaned = campaigns.map { cp -> cp.copy(
            playerIds = cp.playerIds.filter { it in valid },
            npcIds = cp.npcIds.filter { it in valid },
            encounters = cp.encounters.map { e ->
                val fighters = e.fighterIds.filter { it in valid }
                val eligible = fighters.filter { byId[it]?.isAlly == true }
                val rewardRecovered = e.rewardsAwarded || (eligible.isNotEmpty() && eligible.all { id ->
                    "encounter_reward_${cp.id}_${e.id}_$id" in byId[id].orEmptyTransactions()
                })
                val lootRecovered = e.loot.map { loot ->
                    if (loot.claimedBy != null) loot else {
                        val prefix = "encounter_loot_${cp.id}_${e.id}_${loot.id}_"
                        val claimant = chars.firstOrNull { ch -> ch.appliedTransactionIds.any { it.startsWith(prefix) } }?.id
                        if (claimant != null) loot.copy(claimedBy = claimant) else loot
                    }
                }
                e.copy(fighterIds = fighters, rewardsAwarded = rewardRecovered, loot = lootRecovered)
            }
        ) }
        if (cleaned != campaigns) persist(cleaned)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (!hydrated) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CRed) }
        } else if (showScript) {
            CampaignScriptScreen(
                onBack = { showScript = false },
                // قفل GM فعال است: بدون رمز، کتاب کمپین «صفرها» باز نمی‌شود
                // تا محتوای جلسات بین بازیکن‌ها اسپویل نشود.
                requirePin = true,
                onInstantiate = { built ->
                    // اگر همین کمپین قبلاً ساخته شده، دوباره نساز — به‌روزرسانی‌اش کن.
                    // وگرنه نسخه‌ی دوم ساخته می‌شود و GM خلاصه‌ی جلسه‌ها را
                    // در نسخه‌ی قدیمی جا می‌گذارد و فکر می‌کند گم شده‌اند.
                    val existing = campaigns.firstOrNull { it.name == built.name }
                    if (existing == null) {
                        val protectedBuilt = built.copy(pinHash = campaignPinHash("1224"))
                        val withActive = if (campaigns.none { it.active }) protectedBuilt.copy(active = true) else protectedBuilt
                        persist(listOf(withActive) + campaigns)
                        opened = withActive.id
                    } else {
                        // محتوای کتاب تازه می‌شود، ولی چیزهایی که GM سر میز
                        // ساخته دست‌نخورده می‌مانند: بازیکن‌ها، پیشرفت، اهداف شخصی.
                        val merged = existing.copy(
                            objectives = built.objectives,
                            locations = built.locations,
                            factions = built.factions.map { nf ->
                                existing.factions.firstOrNull { it.name == nf.name }?.let { old ->
                                    nf.copy(id = old.id, attitude = old.attitude)
                                } ?: nf
                            },
                            encounters = built.encounters.map { ne ->
                                existing.encounters.firstOrNull { it.title == ne.title } ?: ne
                            } + existing.encounters.filterNot { oe ->
                                built.encounters.any { it.title == oe.title }
                            },
                            sessionHistory = (existing.sessionHistory + built.sessionHistory)
                                .distinctBy { it.sessionId },
                            notes = built.notes
                        )
                        persist(campaigns.map { if (it.id == existing.id) merged else it })
                        opened = existing.id
                    }
                    showScript = false
                }
            )
        } else if (current == null) LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item { Text(gtr("CAMPAIGNS"),color=CWhite,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right) }
            item { Button(onClick={addCampaign=true},colors=ButtonDefaults.buttonColors(containerColor=CRed),modifier=Modifier.fillMaxWidth()){ Text("کمپین جدید",color=Color.Black,fontWeight=FontWeight.Bold) } }
            item { Button(onClick={ val c=shatteredMirrorsCampaign(); persist(listOf(c)+campaigns); opened=c.id },colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF5E35B1)),modifier=Modifier.fillMaxWidth()){ Text("افزودن کمپین آماده: آینه‌های شکسته (۳ جلسه سخت)",color=Color.White,fontWeight=FontWeight.Bold) } }
            item {
                OutlinedButton(onClick={showScript=true},shape=CutCornerShape(10.dp),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=CRed)) {
                    Icon(Icons.Default.MenuBook,null,tint=CRed); Spacer(Modifier.width(6.dp)); Text("کتاب کمپین آماده: «صفرها» 🔒",color=CWhite,fontWeight=FontWeight.Bold)
                }
            }
            items(campaigns,key={it.id}) { c -> Row(Modifier.fillMaxWidth().background(CCard,CutCornerShape(8.dp)).clickable{if(c.pinHash.isBlank()) opened=c.id else pinTarget=c}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick={deleteCampaignId=c.id}){Icon(Icons.Default.Delete,null,tint=CMuted)}
                IconButton(onClick={pinManage=c}){Icon(if(c.pinHash.isBlank()) Icons.Default.LockOpen else Icons.Default.Lock,null,tint=if(c.pinHash.isBlank()) CMuted else CRed)}
                IconButton(onClick={ persist(campaigns.map{ x-> if(x.id==c.id) x.copy(active=!c.active) else if(!c.active) x.copy(active=false) else x }) }){
                    Icon(if(c.active) Icons.Default.PauseCircle else Icons.Default.PlayCircle,null,tint=if(c.active) CRed else CMuted)
                }
                Column(Modifier.weight(1f)){Text(gtr(c.name),color=CWhite,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right);Text(gtr("%1s encounter • %2s session%3s", c.encounters.count{it.status=="PLANNED"}, c.sessionHistory.size, if(c.active) " • ACTIVE" else ""),color=if(c.active) CRed else CMuted,fontSize=11.sp)}
            }}
        } else CampaignDetail(current,chars,viewModel,onBack={opened=null},onUpdate={u->persist(campaigns.map{if(it.id==u.id)u else it})},onActivate={id->persist(campaigns.map{it.copy(active=it.id==id)})},
            // پایان موقت: هیچ داده‌ای پاک نمی‌شود، فقط قفلِ «فعال» باز می‌شود
            // تا بشود کمپین دیگری را فعال کرد و بعداً برگشت.
            onPause={id->persist(campaigns.map{if(it.id==id) it.copy(active=false) else it})},
            onOpenBook={showScript=true})
    }
    if(addCampaign) TextEntryDialog("کمپین جدید","نام کمپین",onDismiss={addCampaign=false}) { n -> val c=Campaign(name=n,active=campaigns.none{it.active});persist(listOf(c)+campaigns);opened=c.id;addCampaign=false }

    deleteCampaignId?.let { id ->
        val doomed = campaigns.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { deleteCampaignId = null }, containerColor = CCard,
            title = { Text("حذف کمپین", color = CWhite, fontWeight = FontWeight.Bold) },
            text = { Text("«${doomed?.name ?: "کمپین"}» حذف شود؟ Sessionهای قدیمی پاک نمی‌شوند؛ از کمپین جدا می‌شوند تا orphan باقی نمانند.", color = CMuted) },
            confirmButton = { TextButton(onClick = {
                val remaining0 = campaigns.filterNot { it.id == id }
                val remaining = if (doomed?.active == true && remaining0.isNotEmpty() && remaining0.none { it.active })
                    remaining0.mapIndexed { i, c -> if (i == 0) c.copy(active = true) else c } else remaining0
                deleteCampaignId = null
                scope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        PersistenceRuntime.locked {
                            val ok = store.save(remaining, epoch)
                            if (ok) SessionStore(context.applicationContext).detachCampaign(id, epoch)
                            ok
                        }
                    }
                    if (saved) {
                        campaigns = remaining
                        if (opened == id) opened = null
                        PersistenceRuntime.publishExternalDataChange()
                    }
                }
            }) { Text("حذف", color = CRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick={deleteCampaignId=null}) { Text("انصراف", color=CWhite) } }
        )
    }
    pinTarget?.let { target -> CampaignPinDialog(target, false, onDismiss={pinTarget=null}) { entered -> if(campaignPinHash(entered)==target.pinHash){opened=target.id;pinTarget=null;true}else false } }
    pinManage?.let { target -> CampaignPinDialog(target, true, onDismiss={pinManage=null}) { entered -> val updated=target.copy(pinHash=if(entered.isBlank()) "" else campaignPinHash(entered)); persist(campaigns.map{if(it.id==target.id)updated else it}); pinManage=null; true } }
}

@Composable private fun CampaignPinDialog(c:Campaign, manage:Boolean,onDismiss:()->Unit,onSubmit:(String)->Boolean){
 var value by remember{mutableStateOf("")}; var error by remember{mutableStateOf(false)}
 AlertDialog(onDismissRequest=onDismiss,containerColor=CCard,title={Text(if(manage) "رمز کمپین: ${c.name}" else "ورود به ${c.name}",color=CWhite)},text={Column{OutlinedTextField(value=value,onValueChange={value=it;error=false},label={Text(if(manage)"رمز جدید — خالی = بدون رمز" else "رمز")},singleLine=true);if(error)Text("رمز نادرست است",color=CRed)}},confirmButton={TextButton(onClick={if(!onSubmit(value))error=true}){Text(if(manage)"ذخیره" else "ورود",color=CRed)}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف",color=CMuted)}})
}

@Composable private fun CampaignDetail(c: Campaign, chars: List<Character>, vm: CharacterViewModel, onBack:()->Unit,onUpdate:(Campaign)->Unit,onActivate:(Long)->Unit,onPause:(Long)->Unit,onOpenBook:()->Unit){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val epoch = PersistenceRuntime.currentEpoch()
    var itemDetail by remember { mutableStateOf<String?>(null) }
    var addMode by remember { mutableStateOf<String?>(null) }
    var editEncounter by remember { mutableStateOf<CampaignEncounter?>(null) }
    var deleteEncounterId by remember { mutableStateOf<Long?>(null) }
    var objectiveCharacterId by remember { mutableStateOf<Int?>(null) }
    val members = chars.filter { it.id in c.playerIds || it.id in c.npcIds }
    val planned = c.encounters.count { it.status != "RESOLVED" }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=14.dp), verticalArrangement=Arrangement.spacedBy(12.dp), contentPadding=PaddingValues(top=14.dp,bottom=100.dp)) {
            item {
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF151515)),shape=CutCornerShape(12.dp),modifier=Modifier.fillMaxWidth().border(1.dp,CRed.copy(.55f),CutCornerShape(12.dp))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.End) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f),horizontalAlignment=Alignment.End) {
                                Text(gtr(c.name),color=CWhite,fontSize=24.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Right,modifier=Modifier.fillMaxWidth())
                                Text(if(c.active) "● کمپین فعال" else "کمپین غیرفعال",color=if(c.active)CRed else CMuted,fontSize=12.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                            }
                            if(c.active) TextButton(onClick={onPause(c.id)}){Text("پایان موقت",color=CMuted,fontWeight=FontWeight.Bold)}
                            else TextButton(onClick={onActivate(c.id)}){Text("فعال کن",color=CRed,fontWeight=FontWeight.Bold)}
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            CampaignStat("اعضا",members.size.toString(),Modifier.weight(1f))
                            CampaignStat("Encounter",planned.toString(),Modifier.weight(1f))
                            CampaignStat("Session",c.sessionHistory.size.toString(),Modifier.weight(1f))
                        }
                    }
                }
            }
            item { CampaignSection("اعضای کمپین", "Player و NPCهای مرتبط با این کمپین") {
                if(chars.isEmpty()) Text("هنوز کاراکتری ساخته نشده.",color=CMuted,fontSize=12.sp)
                else FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { chars.forEach { ch ->
                    val selected=ch.id in (if(ch.isAlly)c.playerIds else c.npcIds)
                    FilterChip(selected=selected,onClick={onUpdate(if(ch.isAlly)c.copy(playerIds=toggle(c.playerIds,ch.id)) else c.copy(npcIds=toggle(c.npcIds,ch.id)))},label={Text("${if(ch.isAlly) "P" else "NPC"} • ${ch.handle.ifBlank{ch.name}.take(16)}")})
                }}
            } }
            item { CampaignSection("اهداف", "کارهایی که گروه باید دنبال کند", onAdd={addMode="objective"}) {
                if(c.objectives.isEmpty()) EmptyCampaignHint("هنوز هدفی ثبت نشده")
                c.objectives.forEachIndexed{i,o->MiniRow("${i+1}. $o"){onUpdate(c.copy(objectives=c.objectives.filterIndexed{x,_->x!=i}))}}
            } }
            item { CampaignSection("اهداف و پیچش‌های هر کاراکتر", "فقط داخل همین کمپین ذخیره می‌شود و به Agent شخصیت نمی‌رود") {
                val players = members.filter { it.isAlly }
                if(players.isEmpty()) EmptyCampaignHint("Playerای به کمپین اضافه نشده")
                players.forEach { ch ->
                    val goals = c.characterGoals.filter { it.characterId == ch.id }
                    Column(Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                            IconButton(onClick={objectiveCharacterId=ch.id}) { Icon(Icons.Default.Add,null,tint=CRed) }
                            Text(ch.handle.ifBlank{ch.name},color=CWhite,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),textAlign=TextAlign.Right)
                        }
                        if(goals.isEmpty()) Text("هدفی ثبت نشده",color=CMuted,fontSize=10.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                        goals.forEach { g ->
                            Row(Modifier.fillMaxWidth().background(Color(0xFF202020),CutCornerShape(7.dp)).padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
                                IconButton(onClick={onUpdate(c.copy(characterGoals=c.characterGoals.filterNot{it.id==g.id}))},modifier=Modifier.size(30.dp)){Icon(Icons.Default.Delete,null,tint=CMuted)}
                                TextButton(onClick={onUpdate(c.copy(characterGoals=c.characterGoals.map{if(it.id==g.id)it.copy(secret=!it.secret) else it}))}){
                                    Text(if(g.secret) "پنهان از بازیکن" else "قابل گفتن",color=if(g.secret) CRed else CMuted,fontSize=9.sp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(g.title,color=if(g.done) CMuted else CWhite,fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                                    if(g.detail.isNotBlank()) Text(g.detail,color=CMuted,fontSize=10.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                                }
                                Checkbox(checked=g.done,onCheckedChange={onUpdate(c.copy(characterGoals=c.characterGoals.map{if(it.id==g.id)it.copy(done=!it.done) else it}))},colors=CheckboxDefaults.colors(checkedColor=CRed))
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            } }
            item { CampaignSection("مکان‌ها", "لوکیشن‌های مهم و سطح خطر", onAdd={addMode="location"}) {
                if(c.locations.isEmpty()) EmptyCampaignHint("هنوز مکانی ثبت نشده")
                c.locations.forEach{l->Row(Modifier.fillMaxWidth().background(Color(0xFF202020),CutCornerShape(7.dp)).padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(gtr(l.name),color=CWhite,modifier=Modifier.weight(1f),textAlign=TextAlign.Right,fontWeight=FontWeight.SemiBold)
                    TextButton(onClick={val levels=listOf("Corporate","Moderate","Combat Zone","Outskirts");val n=levels[(levels.indexOf(l.threat).coerceAtLeast(0)+1)%levels.size];onUpdate(c.copy(locations=c.locations.map{if(it.id==l.id)it.copy(threat=n) else it}))}){Text(gtr(l.threat),color=CRed,fontSize=10.sp)}
                    IconButton(onClick={onUpdate(c.copy(locations=c.locations.filterNot{x->x.id==l.id}))},modifier=Modifier.size(32.dp)){Icon(Icons.Default.Delete,null,tint=CMuted)}
                };Spacer(Modifier.height(5.dp))}
            } }
            item { CampaignSection("گروه‌ها و جناح‌ها", "رابطه‌ی کمپین با Factionها؛ امتیاز از -5 تا +5", onAdd={addMode="faction"}) {
                if(c.factions.isEmpty()) EmptyCampaignHint("هنوز Faction ثبت نشده")
                c.factions.forEach{f->Row(Modifier.fillMaxWidth().background(Color(0xFF202020),CutCornerShape(7.dp)).padding(6.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(gtr(f.name),color=CWhite,modifier=Modifier.weight(1f),textAlign=TextAlign.Right,fontWeight=FontWeight.SemiBold)
                    IconButton(onClick={onUpdate(c.copy(factions=c.factions.map{if(it.id==f.id)it.copy(attitude=(it.attitude+1).coerceAtMost(5)) else it}))},modifier=Modifier.size(32.dp)){Text("+",color=CRed,fontWeight=FontWeight.Bold)}
                    Text("${f.attitude}",color=if(f.attitude<0) CRed else CWhite,fontWeight=FontWeight.Bold,modifier=Modifier.width(28.dp),textAlign=TextAlign.Center)
                    IconButton(onClick={onUpdate(c.copy(factions=c.factions.map{if(it.id==f.id)it.copy(attitude=(it.attitude-1).coerceAtLeast(-5)) else it}))},modifier=Modifier.size(32.dp)){Text("−",color=CRed,fontWeight=FontWeight.Bold)}
                    IconButton(onClick={onUpdate(c.copy(factions=c.factions.filterNot{x->x.id==f.id}))},modifier=Modifier.size(32.dp)){Icon(Icons.Default.Delete,null,tint=CMuted)}
                };Spacer(Modifier.height(5.dp))}
            } }
            item { val newEncounterTitle = gtr("Encounter %1s", c.encounters.size+1)
                CampaignSection("Encounterها", "درگیری‌ها را آماده کن و مستقیم به Combat بفرست", onAdd={editEncounter=CampaignEncounter(title=newEncounterTitle)}) { if(c.encounters.isEmpty()) EmptyCampaignHint("Encounter برنامه‌ریزی نشده") } }
            items(c.encounters,key={it.id}) { e ->
                Card(colors=CardDefaults.cardColors(containerColor=CCard),shape=CutCornerShape(10.dp),modifier=Modifier.fillMaxWidth().border(1.dp,if(e.status=="RESOLVED")CMuted.copy(.4f) else CRed.copy(.45f),CutCornerShape(10.dp))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp),horizontalAlignment=Alignment.End){
                        Text(gtr(e.title),color=CWhite,fontWeight=FontWeight.Bold,fontSize=16.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                        Text("${e.status} • ${e.threat} • ${e.fighterIds.size} نفر",color=CMuted,fontSize=11.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick={
                            scope.launch {
                                val activeSession = withContext(Dispatchers.IO) { SessionStore(context.applicationContext).load().activeSessionId }
                                vm.startCampaignEncounter(c.id,e.id,e.fighterIds.toSet(),activeSession)
                            }
                        }){Text("ارسال به Combat",color=CRed,fontSize=10.sp)};TextButton(onClick={editEncounter=e}){Text("ویرایش",color=CWhite,fontSize=10.sp)};TextButton(onClick={deleteEncounterId=e.id}){Text("حذف",color=CMuted,fontSize=10.sp)}}
                        val eligible=e.fighterIds.mapNotNull{id->chars.firstOrNull{it.id==id && it.isAlly}}
                        if(!e.rewardsAwarded && eligible.isNotEmpty() && (e.rewardIp+e.rewardEb+e.rewardRep)>0) TextButton(onClick={scope.launch { vm.awardEncounterRewards(c.id,e).onSuccess { awarded -> onUpdate(c.copy(encounters=c.encounters.map{if(it.id==e.id)awarded else it})) } }}){Text("پاداش همه • IP ${e.rewardIp} • ${e.rewardEb}eb • REP ${e.rewardRep}",color=CRed,fontSize=10.sp)}
                        e.loot.forEach{loot->if(loot.claimedBy==null){Column(Modifier.fillMaxWidth()){Text(gtr("Loot: %1s ×%2s", loot.name, loot.quantity),color=CWhite,fontSize=11.sp,modifier=Modifier.fillMaxWidth().clickable{itemDetail=loot.name},textAlign=TextAlign.Right);Row{eligible.forEach{ch->TextButton(onClick={scope.launch { vm.claimEncounterLoot(c.id,e,loot.id,ch.id).onSuccess { claimed -> onUpdate(c.copy(encounters=c.encounters.map{if(it.id==e.id)claimed else it})) } }}){Text("→ ${ch.name.take(10)}",color=CRed,fontSize=9.sp)}}}}} else Text(gtr("Loot: %1s → %2s", loot.name, chars.firstOrNull{it.id==loot.claimedBy}?.name?:"?"),color=CMuted,fontSize=10.sp,modifier=Modifier.clickable{itemDetail=loot.name})}
                    }
                }
            }
            item{CampaignSection("تاریخچه Session", "خلاصه‌ی Sessionهای ثبت‌شده") { if(c.sessionHistory.isEmpty()) EmptyCampaignHint("هنوز خلاصه‌ای ثبت نشده") } }
            items(c.sessionHistory,key={it.sessionId}){h->
                var expanded by remember(h.sessionId) { mutableStateOf(false) }
                Card(colors=CardDefaults.cardColors(containerColor=CCard),shape=CutCornerShape(10.dp),modifier=Modifier.fillMaxWidth().clickable{expanded=!expanded}){
                    Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.End){
                        Text("${h.name} • ${h.date}",color=CWhite,fontWeight=FontWeight.Bold,fontSize=14.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                        Text(gtr("IP %1s • %2seb • REP %3s", h.totalIp, h.totalEb, h.totalRep),color=CRed,fontSize=11.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                        if(h.summary.isNotBlank()){
                            Spacer(Modifier.height(4.dp))
                            // خلاصه‌ها طولانی‌اند؛ تا کرده نمایش بده و با یک ضربه باز شود.
                            Text(gtr(h.summary),color=CMuted,fontSize=12.sp,lineHeight=20.sp,
                                maxLines=if(expanded) Int.MAX_VALUE else 3,
                                overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
                            Text(if(expanded) "بستن" else "متن کامل",color=CRed,fontSize=10.sp,modifier=Modifier.padding(top=4.dp))
                        }
                    }
                }
            }
            // دسترسی مستقیم به کتاب کمپین از داخل خود کمپین
            if(c.name == com.cyberpunk.gmtool.data.CampaignScriptData.CAMPAIGN_NAME) item {
                OutlinedButton(onClick=onOpenBook,shape=CutCornerShape(10.dp),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=CRed)){
                    Icon(Icons.Default.MenuBook,null,tint=CRed); Spacer(Modifier.width(6.dp))
                    Text("کتاب کمپین: جزئیات کامل ۱۵ جلسه",color=CWhite,fontWeight=FontWeight.Bold)
                }
            }
            item { CampaignSection("یادداشت GM", "اطلاعات خصوصی، سرنخ‌ها و برنامه‌ی Session بعدی") { OutlinedTextField(value=c.notes,onValueChange={onUpdate(c.copy(notes=it))},placeholder={Text("سرنخ‌ها، NPCهای مهم، اتفاق بعدی...",color=CMuted)},modifier=Modifier.fillMaxWidth().heightIn(min=140.dp),colors=darkCampaignFields()) } }
        }
    }
    deleteEncounterId?.let { id ->
        AlertDialog(onDismissRequest={deleteEncounterId=null},containerColor=CCard,title={Text("حذف Encounter",color=CWhite,fontWeight=FontWeight.Bold)},text={Text("این Encounter حذف شود؟ این کار Fighterها، NPCها یا Characterها را حذف نمی‌کند.",color=CMuted)},confirmButton={TextButton(onClick={onUpdate(c.copy(encounters=c.encounters.filterNot{it.id==id}));deleteEncounterId=null}){Text("حذف",color=CRed,fontWeight=FontWeight.Bold)}},dismissButton={TextButton(onClick={deleteEncounterId=null}){Text("انصراف",color=CWhite)}})
    }
    objectiveCharacterId?.let { cid -> TextEntryDialog("هدف / پیچش داستانی","عنوان",onDismiss={objectiveCharacterId=null}) { v ->
        // فقط در خود کمپین ذخیره می‌شود؛ به Agent شخصیت اضافه نمی‌شود.
        onUpdate(c.copy(characterGoals = c.characterGoals + CampaignCharacterGoal(characterId=cid, title=v)))
        objectiveCharacterId=null } }
    addMode?.let { mode -> TextEntryDialog("افزودن",when(mode){"objective"->"Objective";"location"->"Location";else->"Faction"},onDismiss={addMode=null}){v->onUpdate(when(mode){"objective"->c.copy(objectives=c.objectives+v);"location"->c.copy(locations=c.locations+CampaignLocation(name=v));else->c.copy(factions=c.factions+CampaignFaction(name=v))});addMode=null} }
    editEncounter?.let { e -> EncounterDialog(e,c,chars,onDismiss={editEncounter=null}) { updated -> val exists=c.encounters.any{it.id==updated.id};onUpdate(c.copy(encounters=if(exists)c.encounters.map{if(it.id==updated.id)updated else it}else listOf(updated)+c.encounters));editEncounter=null } }
    itemDetail?.let { label -> CoreItemDetailDialog(label = label, onDismiss = { itemDetail = null }) }
}

@Composable private fun CampaignStat(label:String,value:String,modifier:Modifier=Modifier){Column(modifier.background(Color(0xFF202020),CutCornerShape(7.dp)).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(value,color=CRed,fontSize=20.sp,fontWeight=FontWeight.Black);Text(gtr(label),color=CMuted,fontSize=10.sp)}}
@Composable private fun CampaignSection(title:String,subtitle:String,onAdd:(()->Unit)?=null,content:@Composable ColumnScope.()->Unit){Card(colors=CardDefaults.cardColors(containerColor=CCard),shape=CutCornerShape(10.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(12.dp),horizontalAlignment=Alignment.End){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),horizontalAlignment=Alignment.End){Text(gtr(title),color=CRed,fontSize=16.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right);Text(gtr(subtitle),color=CMuted,fontSize=10.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)};if(onAdd!=null) IconButton(onClick=onAdd){Icon(Icons.Default.Add,null,tint=CRed)}};Spacer(Modifier.height(8.dp));content()}}}
@Composable private fun EmptyCampaignHint(text:String){Text(gtr(text),color=CMuted,fontSize=11.sp,modifier=Modifier.fillMaxWidth().padding(vertical=8.dp),textAlign=TextAlign.Center)}

@Composable private fun EncounterDialog(e:CampaignEncounter,c:Campaign,chars:List<Character>,onDismiss:()->Unit,onSave:(CampaignEncounter)->Unit){var x by remember(e){mutableStateOf(e)};AlertDialog(onDismissRequest=onDismiss,containerColor=CCard,title={Text(gtr("Encounter"),color=CWhite)},text={LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){item{OutlinedTextField(x.title,{x=x.copy(title=it)},label={Text(gtr("Title"))},colors=darkCampaignFields())};item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Easy","Typical","Dangerous").forEach{t->FilterChip(x.threat==t,{x=x.copy(threat=t)},{Text(gtr(t))})}}};item{Text(gtr("Fighters"),color=CRed);FlowRow{chars.forEach{ch->FilterChip(ch.id in x.fighterIds,{x=x.copy(fighterIds=toggle(x.fighterIds,ch.id))},{Text(ch.name.take(12))})}}};item{Text(gtr("Location / Factions"),color=CRed);c.locations.forEach{l->FilterChip(x.locationId==l.id,{x=x.copy(locationId=l.id)},{Text(gtr(l.name))})};c.factions.forEach{f->FilterChip(f.id in x.factionIds,{x=x.copy(factionIds=toggle(x.factionIds,f.id))},{Text(gtr(f.name))})}};item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){IntField("IP",x.rewardIp){x=x.copy(rewardIp=it)};IntField("eb",x.rewardEb){x=x.copy(rewardEb=it)};IntField("REP",x.rewardRep){x=x.copy(rewardRep=it)}}};item{var lootText by remember(x.id){mutableStateOf(x.loot.joinToString(", "){it.name})};OutlinedTextField(lootText,{ text ->
                    lootText=text
                    val names=text.split(",").map{it.trim()}.filter{it.isNotBlank()}
                    x=x.copy(loot=names.mapIndexed { i, name ->
                        x.loot.getOrNull(i)?.takeIf { it.name == name } ?: CampaignLoot(name=name)
                    })
                },label={Text(gtr("Loot (comma separated)"))},colors=darkCampaignFields())};item{OutlinedTextField(x.outcome,{x=x.copy(outcome=it)},label={Text(gtr("Outcome"))},colors=darkCampaignFields());OutlinedTextField(x.consequence,{x=x.copy(consequence=it)},label={Text(gtr("Consequence"))},colors=darkCampaignFields());Row{FilterChip(x.status=="RESOLVED",{x=x.copy(status=if(x.status=="RESOLVED")"PLANNED" else "RESOLVED")},{Text(gtr("RESOLVED"))})}}}},confirmButton={TextButton(onClick={onSave(x)}){Text(gtr("SAVE"),color=CRed)}},dismissButton={TextButton(onClick=onDismiss){Text(gtr("CANCEL"),color=CMuted)}})}
@Composable private fun IntField(label:String,v:Int,set:(Int)->Unit){OutlinedTextField(v.toString(),{set(it.toIntOrNull()?.coerceAtLeast(0)?:0)},label={Text(gtr(label))},modifier=Modifier.width(90.dp),colors=darkCampaignFields())}
@Composable private fun SectionTitle(t:String)=Text(gtr(t),color=CRed,fontWeight=FontWeight.Bold,fontSize=15.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Right)
@Composable private fun AddButton(onClick:()->Unit)=TextButton(onClick=onClick){Icon(Icons.Default.Add,null,tint=CRed);Text(gtr("ADD"),color=CRed)}
@Composable private fun MiniRow(t:String,onDelete:()->Unit){Row(Modifier.fillMaxWidth().padding(vertical=2.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onDelete,modifier=Modifier.size(30.dp)){Icon(Icons.Default.Close,null,tint=CMuted)};Text(gtr(t),color=CWhite,modifier=Modifier.weight(1f),textAlign=TextAlign.Right)}}
private fun <T> toggle(xs:List<T>,x:T)=if(x in xs)xs-x else xs+x
@Composable private fun TextEntryDialog(title:String,label:String,onDismiss:()->Unit,onSave:(String)->Unit){var v by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,containerColor=CCard,title={Text(gtr(title),color=CWhite)},text={OutlinedTextField(v,{v=it},label={Text(gtr(label))},colors=darkCampaignFields())},confirmButton={TextButton(enabled=v.isNotBlank(),onClick={onSave(v.trim())}){Text(gtr("SAVE"),color=CRed)}},dismissButton={TextButton(onClick=onDismiss){Text(gtr("CANCEL"),color=CMuted)}})}
@Composable private fun darkCampaignFields()=OutlinedTextFieldDefaults.colors(focusedTextColor=CWhite,unfocusedTextColor=CWhite,focusedBorderColor=CRed,unfocusedBorderColor=CMuted,focusedLabelColor=CRed,unfocusedLabelColor=CMuted)

private fun Character?.orEmptyTransactions(): List<String> = this?.appliedTransactionIds ?: emptyList()
