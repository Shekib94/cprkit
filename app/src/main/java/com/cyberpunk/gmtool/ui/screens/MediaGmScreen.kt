package com.cyberpunk.gmtool.ui.screens

import com.cyberpunk.gmtool.data.gtr

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberpunk.gmtool.data.RoleHelperData
import com.cyberpunk.gmtool.data.mediaScenarios
import com.cyberpunk.gmtool.ui.components.DecisionBox
import com.cyberpunk.gmtool.ui.components.FaText
import com.cyberpunk.gmtool.ui.components.HelperCard
import com.cyberpunk.gmtool.ui.components.HelperToggle
import com.cyberpunk.gmtool.ui.components.PickerRows
import com.cyberpunk.gmtool.ui.components.RoleGloryCard
import com.cyberpunk.gmtool.ui.components.RoleHelperCard
import com.cyberpunk.gmtool.ui.components.RoleHelperCharacterCard
import com.cyberpunk.gmtool.ui.components.RoleHelperHeader
import com.cyberpunk.gmtool.ui.components.RoleHelperPage
import com.cyberpunk.gmtool.ui.components.RoleScenarioCard
import com.cyberpunk.gmtool.ui.components.RoleUi
import com.cyberpunk.gmtool.ui.components.RollBox
import com.cyberpunk.gmtool.viewmodel.CharacterViewModel

/**
 * ابزار کمکی GM برای Media.
 *
 * اینجا فقط چیزهایی است که خودمان برای مدیریت میز ساختیم: تصمیم‌گیری مرحله‌ی
 * تحقیق، ساخت سوژه/واکنش، صحنه‌ی آماده و پیشنهاد لحظه‌ی درخشش. خودِ Role
 * Ability (Rumor passive، Investigation فعال، تابلوی پرونده‌ها و انتشار) در تب
 * BIO همان کاراکتر است و از آنجا ریخته می‌شود — این صفحه جای قاعده‌ی اصلی
 * نیست و آن را تکرار نمی‌کند.
 */
@Composable
fun MediaGmAssistant(viewModel: CharacterViewModel) {
    val characters by viewModel.characters.collectAsState()
    val medias = characters.filter { it.role.equals("Media", true) && it.isAlly }
    var selectedId by remember(medias.map { it.id }) { mutableStateOf(medias.firstOrNull()?.id) }
    var scenarioIndex by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(RoleHelperData.mediaStages.first()) }
    var hasProof by remember { mutableStateOf(false) }
    var powerPlayer by remember { mutableStateOf(false) }

    RoleHelperPage {
        item {
            RoleHelperHeader(
                "ابزار کمکی GM برای Media",
                "این صفحه برای وقتی است که GM باید سریع تصمیم بگیرد: الان کجای تحقیق هستیم، قدم بعدی چیست و کدام صحنه الان روی میز بیاید. خودِ Credibility (Rumor passive، Investigation فعال، پرونده‌ها و انتشار) در تب BIO همان کاراکتر است."
            )
        }
        item {
            RoleHelperCharacterCard(
                title = "کاراکتر Media",
                characters = medias,
                selectedId = selectedId,
                onSelect = { selectedId = it },
                summary = { c -> gtr("Credibility %1s • Believability %2s/10 • پرونده‌ها %3s", c.roleRank, com.cyberpunk.gmtool.data.RoleAssistantData.mediaProfile(c.roleRank).believability, c.mediaCases.size) },
                chipLabel = { c -> gtr("%1s • Credibility %2s", c.handle.ifBlank { c.name }, c.roleRank) }
            )
        }
        item {
            HelperCard(
                "الان در کدام مرحله‌ی تحقیق هستیم؟",
                "مرحله را انتخاب کن تا بگوید قدم بعدی چیست و چه چیزی هنوز کم است."
            ) {
                PickerRows(RoleHelperData.mediaStages, stage) { stage = it }
                Spacer(Modifier.height(2.dp))
                HelperToggle("مدرک مستقل دارد", hasProof) { hasProof = it }
                HelperToggle("طرف مقابل قدرتمند است", powerPlayer) { powerPlayer = it }
                Spacer(Modifier.height(2.dp))
                val (verdict, tone) = RoleHelperData.mediaAdvice(stage, hasProof, powerPlayer)
                DecisionBox(verdict, tone)
            }
        }
        item {
            HelperCard("ساخت سریع خبر") {
                RollBox("یک سوژه بساز", RoleHelperData.mediaStories)
                Spacer(Modifier.height(6.dp))
                RollBox("واکنش بعد از انتشار", RoleHelperData.mediaReactions, "انتشار باید نتیجه داشته باشد، وگرنه کار Media بی‌معنا می‌شود.")
            }
        }
        item { RoleScenarioCard(mediaScenarios, scenarioIndex) { scenarioIndex = (scenarioIndex + 1) % mediaScenarios.size } }
        item {
            RoleHelperCard("یادآوری‌های میز") {
                FaText(
                    "• Rumor فقط جهت می‌دهد؛ ممکن است غلط باشد و به‌تنهایی منتشرشدنی نیست.\n" +
                        "• برای Investigation فعال، DV همان سطح Rumor است و Believability جدای از آن حساب می‌شود.\n" +
                        "• مدرک قابل راستی‌آزمایی، شانس باور شدن را بالا می‌برد؛ LUCK روی این Roll خرج نمی‌شود.\n" +
                        "• روی یک موضوع مشخص بعد از انتشار، دوباره Story تازه معنا ندارد مگر اطلاعات تازه بیاید.",
                    color = RoleUi.Muted, fontSize = 11.sp, lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth(), justify = true
                )
            }
        }
        item {
            RoleGloryCard("به Media صحنه‌ای بده که فقط او می‌تواند ببیند: یک منبع، یک مدرک، یک طرفدار — و یک سؤال که بدون انتشار جواب نمی‌گیرد. اینجا ارزش Credibility واقعاً معلوم می‌شود.")
        }
    }
}
