package com.omni.backrooms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Events_Screen(onBack: () -> Unit) {
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
                Text(stringResource(R.string.menu_events), color = Yellow,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
            Divider_Line()
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Event, null, tint = TextDim, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.events_empty), color = TextDim, fontSize = 14.sp, letterSpacing = 2.sp)
                    Text(stringResource(R.string.events_coming_soon), color = TextDim.copy(0.5f), fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}
