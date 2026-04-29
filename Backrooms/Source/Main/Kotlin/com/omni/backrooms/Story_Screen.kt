package com.omni.backrooms

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Story_Screen(onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        Crt_Overlay()
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow)
        }
        AnimatedVisibility(
            visible, enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.94f),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp, vertical = 56.dp)
        ) {
            Book_Widget()
        }
    }
}

@Composable
private fun Book_Widget() {
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth(0.72f).fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 8.dp, bottomEnd = 8.dp))
            .background(Color(0xFF1A1408))
            .border(2.dp,
                Brush.verticalGradient(listOf(Color(0xFF5A3A10), Color(0xFF3A2208), Color(0xFF5A3A10))),
                RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 8.dp, bottomEnd = 8.dp))
            .drawWithContent {
                drawContent()
                drawRect(Brush.horizontalGradient(
                    listOf(Color.Black.copy(0.4f), Color.Transparent), startX = 0f, endX = 40f))
                drawLine(Color(0xFF3A2208), Offset(32f, 0f), Offset(32f, size.height), 3f)
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 52.dp, end = 24.dp, top = 32.dp, bottom = 32.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.story_title),
                color = Color(0xFF8B6914), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Divider_Line()
            Spacer(Modifier.height(4.dp))
            Story_Para(stringResource(R.string.story_para_1))
            Story_Para(stringResource(R.string.story_para_2))
            Story_Para(stringResource(R.string.story_para_3))
            Text(stringResource(R.string.story_quote),
                color = Color(0xFF8A6A40), fontSize = 12.sp, fontStyle = FontStyle.Italic,
                lineHeight = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Story_Para(text: String) {
    Text(text, color = Color(0xFFC8A870), fontSize = 13.sp,
        lineHeight = 22.sp, textAlign = TextAlign.Justify)
}
