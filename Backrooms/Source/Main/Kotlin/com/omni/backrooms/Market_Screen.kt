package com.omni.backrooms

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class MTab(val res: Int, val icon: ImageVector) {
    Boosts    (R.string.market_tab_boosts,     Icons.Default.Bolt),
    Characters(R.string.market_tab_characters, Icons.Default.Person),
    Soulium   (R.string.market_tab_soulium,    Icons.Default.AutoAwesome),
    Vip       (R.string.market_tab_vip,        Icons.Default.Star),
    Daily     (R.string.market_tab_daily,      Icons.Default.LocalOffer)
}

private data class MItem(val id: String, val label: String, val price: String, val currency: Color, val owned: Boolean = false)

@Composable
fun Market_Screen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(MTab.Boosts) }

    val items = remember {
        mapOf(
            MTab.Boosts     to listOf(MItem("speed","Hız Boost","500", OmniumCol), MItem("nv","Gece Görüşü","300", OmniumCol), MItem("sw","Sessiz Yürüyüş","750", OmniumCol)),
            MTab.Characters to listOf(MItem("wanderer","Wanderer","1200", OmniumCol, true), MItem("ghost","Hayalet","2500", SouliumCol), MItem("scav","Avcı","3000", SouliumCol)),
            MTab.Soulium    to listOf(MItem("s100","100 Soulium","0.99 ₺", Color(0xFF4CAF50)), MItem("s500","500 Soulium","3.99 ₺", Color(0xFF4CAF50)), MItem("s2k","2000 Soulium","12.99 ₺", Color(0xFF4CAF50))),
            MTab.Vip        to listOf(MItem("vip_m","Aylık VIP","24.99 ₺", Color(0xFFFFD700)), MItem("vip_y","Yıllık VIP","199.99 ₺", Color(0xFFFFD700))),
            MTab.Daily      to listOf(MItem("d1","Günlük 1","250", OmniumCol), MItem("d2","Günlük 2","800", SouliumCol))
        )
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050503))) {
        Crt_Overlay()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Yellow) }
                Text(stringResource(R.string.market_title), color = Yellow,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
            Divider_Line()
            ScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                containerColor   = MetalBg.copy(0.5f),
                contentColor     = Yellow,
                indicator = { positions ->
                    Box(Modifier.tabIndicatorOffset(positions[tab.ordinal]).height(2.dp).background(Yellow))
                },
                edgePadding = 8.dp
            ) {
                MTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t },
                        icon = { Icon(t.icon, null, Modifier.size(16.dp)) },
                        text = { Text(stringResource(t.res), fontSize = 10.sp, letterSpacing = 1.sp) },
                        selectedContentColor = Yellow, unselectedContentColor = TextDim)
                }
            }
            Divider_Line()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items[tab] ?: emptyList(), key = { it.id }) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(MetalBg)
                            .border(1.dp, BorderCol, RoundedCornerShape(3.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Category, null, tint = item.currency.copy(0.8f), modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(item.label, color = Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        if (item.owned) {
                            Text(stringResource(R.string.market_equipped), color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Box(
                                Alignment.Center,
                                Modifier.fillMaxWidth().height(30.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(item.currency.copy(0.15f))
                                    .border(1.dp, item.currency.copy(0.5f), RoundedCornerShape(2.dp))
                            ) { Text(item.price, color = item.currency, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
