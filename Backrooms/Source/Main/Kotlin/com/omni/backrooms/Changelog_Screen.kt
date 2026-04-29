package com.omni.backrooms

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class CL(val version: String, val date: String, val items: List<String>)

@Composable
fun Changelog_Screen(onBack: () -> Unit) {
    val entries = remember {
        listOf(
            CL("v0.9.3-beta","2026-04-20", listOf(
                "Arayüz: Ana menü CRT efekti güçlendirildi",
                "Arayüz: Halı progress bar eklendi",
                "Arayüz: Flicker animasyonu optimize edildi")),
            CL("v0.9.2-beta","2026-03-30", listOf(
                "Loading: Prosedürel koridor arka planı",
                "Video/Audio: ExoPlayer 1.10.0 entegrasyonu",
                "Video/Audio: VHS filtresi native'e taşındı")),
            CL("v0.9.1-beta","2026-03-10", listOf(
                "Online Altyapı: UDP soket katmanı",
                "Online Altyapı: Oda oluştur/bul akışı",
                "Online Altyapı: Şifreli oda desteği")),
            CL("v0.9.0-beta","2026-02-20", listOf(
                "Voice Chat: Agora RTC 4.6.3 entegrasyonu",
                "Voice Chat: PTT modu eklendi",
                "Billboard Systems: Dinamik uyarı tabelaları")),
            CL("v0.8.5-beta","2026-01-28", listOf(
                "Entity AI: C++23 A* pathfinding",
                "Entity AI: Aggro yarıçap sistemi",
                "Genel: NDK 29, CMake 4.3.1, Hilt 2.59.2"))
        )
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        Crt_Overlay()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow)
                }
                Text(stringResource(R.string.changelog_title), color = Yellow,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(stringResource(R.string.changelog_branch_beta), color = CrtAmber, fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }
            Divider_Line()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                entries.forEach { e ->
                    Omni_Panel(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(e.version, color = Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(e.date, color = TextDim, fontSize = 10.sp)
                            }
                            Divider_Line()
                            e.items.forEach { item ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("›", color = YellowDim, fontSize = 13.sp)
                                    Text(item, color = TextSec, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
