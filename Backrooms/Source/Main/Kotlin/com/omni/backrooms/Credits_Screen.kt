package com.omni.backrooms

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Credits_Screen(onBack: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "cr")
    val scroll by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(28000, easing = LinearEasing)), label = "sc")

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        Crt_Overlay()
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp).graphicsLayer { alpha = 0.85f }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow)
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h      = maxHeight
            val offset = (-scroll * 1400f) + h.value
            Column(
                Modifier.fillMaxWidth().graphicsLayer { translationY = offset },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(h))
                Text(stringResource(R.string.credits_title), color = Yellow,
                    fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.credits_developed_by), color = TextSec,
                    fontSize = 11.sp, letterSpacing = 4.sp)
                Text(stringResource(R.string.credits_developer_name), color = YellowGlow,
                    fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(32.dp))
                Divider_Line(Modifier.width(200.dp))
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.credits_testers_label), color = TextSec,
                    fontSize = 11.sp, letterSpacing = 4.sp)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Alpha_Wanderer","NoclipRunner","LevelZeroSurvivor",
                    "YellowWallHunter","FluorescentDread","CarpetCrawler",
                    "BackroomsExplorer_7","VoidWatcher","HummingLight"
                ).forEach {
                    Text(it, color = TextDim, fontSize = 13.sp,
                        letterSpacing = 1.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(48.dp))
                Divider_Line(Modifier.width(120.dp))
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.app_name), color = TextDim.copy(0.5f),
                    fontSize = 10.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(h))
            }
        }
    }
}
