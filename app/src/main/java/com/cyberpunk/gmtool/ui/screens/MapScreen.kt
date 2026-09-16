package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cyberpunk.gmtool.data.District
import com.cyberpunk.gmtool.data.MapPlace
import com.cyberpunk.gmtool.data.NightCityData
import com.cyberpunk.gmtool.data.PlaceType

private val Red = Color(0xFFD32F2F)
private val Black = Color(0xFF0F0F0F)
private val White = Color(0xFFE0E0E0)
private val Muted = Color(0xFFAAAAAA)
private val CardBg = Color(0xFF1A1A1A)

/** آیکون مناسب برای هر نوع مکان */
private fun placeIcon(t: PlaceType) = when (t) {
    PlaceType.CLINIC, PlaceType.RIPPER -> Icons.Default.LocalHospital
    PlaceType.BAR -> Icons.Default.LocalBar
    PlaceType.FOOD -> Icons.Default.Restaurant
    PlaceType.GUN -> Icons.Default.GpsFixed
    PlaceType.GARAGE -> Icons.Default.Build
    PlaceType.MARKET, PlaceType.MALL -> Icons.Default.ShoppingCart
    PlaceType.HOTEL -> Icons.Default.Bed
    PlaceType.METRO, PlaceType.STATION -> Icons.Default.DirectionsTransit
    PlaceType.CAR, PlaceType.PARKING -> Icons.Default.DirectionsCar
    PlaceType.BANK -> Icons.Default.AccountBalance
    PlaceType.CORP -> Icons.Default.Business
    PlaceType.GANG, PlaceType.DANGER -> Icons.Default.Dangerous
    PlaceType.STADIUM -> Icons.Default.Stadium
    PlaceType.PARK, PlaceType.LANDMARK, PlaceType.CAMP -> Icons.Default.Place
    PlaceType.PORT -> Icons.Default.Sailing
    PlaceType.SITE -> Icons.Default.Factory
}

// ================= صفحه‌ی نقشه (داخل گوشی) =================
@Composable
fun MapApp() {
    var districtIndex by remember { mutableStateOf(0) }
    var fullscreen by remember { mutableStateOf(false) }
    var showPlaces by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<MapPlace?>(null) }

    val district = NightCityData.districts[districtIndex]

    Column(Modifier.fillMaxSize().background(Black)) {
        // نوار انتخاب منطقه
        LazyRow(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(NightCityData.districts.size) { i ->
                val d = NightCityData.districts[i]
                val selected = i == districtIndex
                Surface(
                    color = if (selected) Red else CardBg,
                    shape = CutCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, if (selected) Red else Red.copy(0.3f), CutCornerShape(8.dp))
                        .clickable { districtIndex = i; showPlaces = false }
                ) {
                    Text(
                        "${d.num} ${d.nameFa}",
                        color = if (selected) Color.Black else White,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // دکمه‌های حالت
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { showPlaces = false },
                shape = CutCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (!showPlaces) Red else CardBg),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) { Icon(Icons.Default.Map, null, tint = if (!showPlaces) Color.Black else White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("نقشه", color = if (!showPlaces) Color.Black else White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Button(
                onClick = { showPlaces = true },
                shape = CutCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (showPlaces) Red else CardBg),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) { Icon(Icons.Default.List, null, tint = if (showPlaces) Color.Black else White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("مکان‌ها (${district.places.size})", color = if (showPlaces) Color.Black else White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            if (!showPlaces) {
                IconButton(onClick = { fullscreen = true }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Fullscreen, gtr("Fullscreen"), tint = Red)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (showPlaces) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                PlacesList(district, onSelect = { selectedPlace = it }, onPickMap = {
                    showPlaces = false
                })
            }
        } else {
            // عنوان منطقه + راهنما
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    Text("${district.nameEn} • ${district.nameFa}", color = Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("با دو انگشت زوم کن، با یک انگشت جابه‌جا شو. روی مارکر قرمز نقاط مهم بزن.", color = Muted, fontSize = 11.sp)
                }
            }
            ZoomableMap(
                district = district,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                onHotspot = { p -> selectedPlace = p },
                onDistrictHotspot = { idx -> districtIndex = idx }
            )
        }
    }

    // دیالوگ توضیح مکان
    selectedPlace?.let { p ->
        PlaceDetailDialog(p, district = district, onDismiss = { selectedPlace = null })
    }

    // تمام‌صفحه
    if (fullscreen) {
        FullscreenMap(
            district = district,
            onClose = { fullscreen = false },
            onHotspot = { p -> selectedPlace = p },
            onDistrictHotspot = { idx -> districtIndex = idx }
        )
    }
}

// ================= لیست مکان‌ها =================
@Composable
private fun PlacesList(district: District, onSelect: (MapPlace) -> Unit, onPickMap: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Box(Modifier.fillMaxWidth().background(Red.copy(0.12f), CutCornerShape(8.dp)).border(1.dp, Red.copy(0.4f), CutCornerShape(8.dp)).padding(12.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(gtr(district.summary), color = White, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Right)
                }
            }
        }
        items(district.places, key = { it.num }) { p ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(p) }
                    .background(CardBg, CutCornerShape(8.dp)).border(1.dp, Red.copy(0.3f), CutCornerShape(8.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).background(if (p.important) Red else Red.copy(0.25f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("${p.num}", color = if (p.important) Color.Black else White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(10.dp))
                Icon(placeIcon(p.type), null, tint = Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.weight(1f)) {
                        Text(p.nameFa, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                        Text("${p.type.fa} • ${p.nameEn}", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    }
                }
                Icon(Icons.Default.ChevronLeft, null, tint = Red)
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ================= دیالوگ توضیح مکان =================
@Composable
private fun PlaceDetailDialog(p: MapPlace, district: District, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Black),
            shape = CutCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().border(2.dp, Red, CutCornerShape(14.dp))
        ) {
            Column(Modifier.padding(18.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(gtr(p.nameEn), color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("${p.nameFa} • ${district.nameFa}", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(placeIcon(p.type), null, tint = Red, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(p.type.fa, color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (p.important) {
                            Spacer(Modifier.width(10.dp))
                            Surface(color = Red, shape = CutCornerShape(4.dp)) { Text("نقطه مهم", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(gtr(p.desc), color = White, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Red), shape = CutCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("بستن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ================= تمام‌صفحه =================
@Composable
private fun FullscreenMap(
    district: District,
    onClose: () -> Unit,
    onHotspot: (MapPlace) -> Unit,
    onDistrictHotspot: (Int) -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Black)) {
            ZoomableMap(
                district = district,
                modifier = Modifier.fillMaxSize(),
                onHotspot = onHotspot,
                onDistrictHotspot = onDistrictHotspot
            )
            // دکمه بستن
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .background(Color.Black.copy(0.6f), CircleShape)
            ) { Icon(Icons.Default.Close, gtr("Close"), tint = Red) }
        }
    }
}

// ================= نقشه‌ی زوم/پان با هات‌اسپات =================
// ابعاد تصاویر کراپ‌شده (برای محاسبه‌ی Fit)
private const val CITY_AR = 1038f / 758f
private const val DIST_AR = 848f / 699f

/** ناحیه‌ی واقعی تصویر پس از ContentScale.Fit داخل Box (با letterbox) */
private fun fittedRect(boxW: Float, boxH: Float, imgAr: Float): Rect {
    val boxAr = boxW / boxH
    return if (imgAr > boxAr) {
        // تصویر عریض‌تر: عرض پر، ارتفاع وسط‌چین
        val h = boxW / imgAr
        Rect(0f, (boxH - h) / 2f, boxW, (boxH - h) / 2f + h)
    } else {
        val w = boxH * imgAr
        Rect((boxW - w) / 2f, 0f, (boxW - w) / 2f + w, boxH)
    }
}

@Composable
private fun ZoomableMap(
    district: District,
    modifier: Modifier = Modifier,
    onHotspot: (MapPlace) -> Unit,
    onDistrictHotspot: (Int) -> Unit
) {
    var scale by remember(district.id) { mutableStateOf(1f) }
    var offset by remember(district.id) { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val imgAr = if (district.id == "city") CITY_AR else DIST_AR

    Box(
        modifier
            .background(Color(0xFF05070d), CutCornerShape(8.dp))
            .clip(CutCornerShape(8.dp))
            .onSizeChanged { boxSize = it }
            .pointerInput(district.id) {
                detectTapGestures { tap ->
                    if (boxSize.width == 0) return@detectTapGestures
                    val r = fittedRect(boxSize.width.toFloat(), boxSize.height.toFloat(), imgAr)
                    fun hit(nx: Float, ny: Float, radius: Float): Boolean {
                        // نقطه‌ی تصویر → فضای اعمال‌نشده → تبدیل با scale/offset
                        val imgPt = Offset(r.left + nx * r.width, r.top + ny * r.height)
                        val cx = boxSize.width / 2f
                        val cy = boxSize.height / 2f
                        val scr = Offset(
                            (imgPt.x - cx) * scale + cx + offset.x,
                            (imgPt.y - cy) * scale + cy + offset.y
                        )
                        val dx = scr.x - tap.x
                        val dy = scr.y - tap.y
                        return (dx * dx + dy * dy) < radius * radius
                    }
                    for (p in district.places) {
                        if (p.important && p.x in 0f..1f && p.y in 0f..1f && hit(p.x, p.y, 44f)) {
                            onHotspot(p); return@detectTapGestures
                        }
                    }
                    if (district.id == "city") {
                        for ((hx, hy, targetIdx) in district.districtHotspots) {
                            if (hit(hx, hy, 64f)) { onDistrictHotspot(targetIdx); return@detectTapGestures }
                        }
                    }
                }
            }
            .pointerInput(district.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = newScale
                    val maxX = (boxSize.width * (newScale - 1)) / 2f + 80f
                    val maxY = (boxSize.height * (newScale - 1)) / 2f + 80f
                    offset = Offset(
                        (offset.x + pan.x).coerceIn(-maxX, maxX),
                        (offset.y + pan.y).coerceIn(-maxY, maxY)
                    )
                }
            }
    ) {
        Image(
            painter = painterResource(id = district.mapRes),
            contentDescription = district.nameEn,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale, scaleY = scale,
                    translationX = offset.x, translationY = offset.y
                )
        )

        // لایه‌ی مارکر روی تصویر (با همان ترنسفورم)
        androidx.compose.foundation.Canvas(
            Modifier.fillMaxSize().graphicsLayer(
                scaleX = scale, scaleY = scale,
                translationX = offset.x, translationY = offset.y
            )
        ) {
            val r = fittedRect(size.width, size.height, imgAr)
            district.places.forEach { p ->
                if (p.important && p.x in 0f..1f && p.y in 0f..1f) {
                    val c = Offset(r.left + p.x * r.width, r.top + p.y * r.height)
                    drawCircle(color = Color(0xCC000000), radius = 15f, center = c)
                    drawCircle(color = Red, radius = 12f, center = c)
                    drawCircle(color = Color.White, radius = 4f, center = c)
                }
            }
            if (district.id == "city") {
                district.districtHotspots.forEach { (hx, hy, _) ->
                    val c = Offset(r.left + hx * r.width, r.top + hy * r.height)
                    drawCircle(color = Color(0xCC000000), radius = 13f, center = c)
                    drawCircle(color = Color(0xFFFFC107), radius = 9f, center = c)
                }
            }
        }

        if (scale > 1.05f) {
            IconButton(
                onClick = { scale = 1f; offset = Offset.Zero },
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                    .background(Color.Black.copy(0.65f), CircleShape)
            ) { Icon(Icons.Default.CenterFocusStrong, gtr("Reset zoom"), tint = Red) }
        }
    }
}
