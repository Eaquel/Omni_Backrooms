package com.omni.backrooms

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val maps         : List<MapDto>    = emptyList(),
    val selected     : MapDto?         = null,
    val isLoading    : Boolean         = false,
    val error        : String?         = null,
    val filter       : MapFilter       = MapFilter.ALL
)

enum class MapFilter(val label: String) {
    ALL("Tümü"), UNLOCKED("Açık"), LOCKED("Kilitli"), FAVORITES("Favoriler")
}

@HiltViewModel
class MapsVM @Inject constructor(
    private val api         : ApiService,
    private val assetManager: AssetManager
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { api.getMaps() }
                .onSuccess  { maps -> _state.update { it.copy(isLoading = false, maps = maps) } }
                .onFailure  { e   -> _state.update { it.copy(isLoading = false, error = e.message) }
                    _state.update { it.copy(maps = buildFallbackMaps()) }
                }
        }
    }

    fun select(map: MapDto) {
        _state.update { it.copy(selected = map) }
    }

    fun clearSelection() {
        _state.update { it.copy(selected = null) }
    }

    fun setFilter(f: MapFilter) {
        _state.update { it.copy(filter = f) }
    }

    fun filteredMaps(): List<MapDto> {
        val s = _state.value
        return when (s.filter) {
            MapFilter.ALL       -> s.maps
            MapFilter.UNLOCKED  -> s.maps.filter { it.isUnlocked }
            MapFilter.LOCKED    -> s.maps.filter { !it.isUnlocked }
            MapFilter.FAVORITES -> s.maps
        }
    }

    fun getLevelTheme(level: Int) = assetManager.getLevelTheme(level)

    private fun buildFallbackMaps(): List<MapDto> = listOf(
        MapDto("level_0","Level 0","The Lobby",0,"Sarı duvarlar, nemli halı...","Yellow walls, damp carpet...",null,true,2, listOf(0,2)),
        MapDto("level_1","Level 1","Habitable Zone",1,"Beton koridorlar...","Concrete corridors...",null,false,4,listOf(0,1,2,6)),
        MapDto("level_2","Level 2","Pipe Dreams",2,"Dev borular...","Giant pipes...",null,false,5,listOf(1,3,6,7)),
        MapDto("level_3","Level 3","Electrical Station",3,"Elektrik vızıltısı...","Electrical hum...",null,false,6,listOf(0,1,2,3,5)),
        MapDto("level_4","Level 4","Abandoned Office",4,"Terk edilmiş ofisler...","Abandoned offices...",null,false,5,listOf(2,4,7)),
        MapDto("level_5","Level 5","The Hotel",5,"Lüks otel koridorları...","Luxury hotel corridors...",null,false,7,listOf(0,1,2,3,4,5,6,7)),
        MapDto("level_6","Level 6","Lights Out",6,"Tam karanlık...","Pitch black...",null,false,9,listOf(0,2,3,5,6,7)),
        MapDto("level_7","Level 7","Thalassophobia",7,"Su altı hissi...","Underwater feeling...",null,false,8,listOf(1,3,4,5))
    )
}

@Composable
fun Maps(onBack: () -> Unit, vm: MapsVM = hiltViewModel()) {
    val state    by vm.state.collectAsState()
    val filtered  = vm.filteredMaps()

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack(stringResource(R.string.maps_title), onBack)
            DividerLine()

            Row(
                Modifier.fillMaxWidth().background(MetalBg.copy(0.5f)).padding(horizontal=12.dp, vertical=6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MapFilter.entries.forEach { f ->
                    val sel = state.filter == f
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clip(RoundedCornerShape(2.dp))
                            .background(if (sel) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
                            .border(1.dp, if (sel) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
                            .clickable { vm.setFilter(f) }
                            .padding(horizontal=10.dp, vertical=5.dp)
                    ) {
                        Text(f.label, color=if(sel) Yellow else TextDim, fontSize=11.sp,
                            fontWeight=if(sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            DividerLine()

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color=Yellow, strokeWidth=2.dp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { map ->
                        MapCard(map = map, theme = vm.getLevelTheme(map.level), onClick = { vm.select(map) })
                    }
                }
            }
        }

        state.selected?.let { map ->
            MapDetailOverlay(
                map    = map,
                theme  = vm.getLevelTheme(map.level),
                onDismiss = { vm.clearSelection() }
            )
        }
    }
}

@Composable
private fun MapCard(map: MapDto, theme: LevelTheme, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MetalBg)
            .border(1.dp, if (map.isUnlocked) theme.primaryColor.copy(0.4f) else BorderCol, RoundedCornerShape(3.dp))
            .clickable(enabled = true, onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth().height(80.dp)
                .background(Brush.verticalGradient(listOf(theme.primaryColor.copy(0.3f), DarkBg)))
                .drawWithContent {
                    drawContent()
                    if (!map.isUnlocked) drawRect(Color.Black.copy(0.6f))
                }
        ) {
            Text(
                "Level ${map.level}",
                color = theme.primaryColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center)
            )
            if (!map.isUnlocked) {
                Icon(Icons.Default.Lock, null, tint=TextDim, modifier=Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp))
            }
            ThreatBadge(map.threatLevel, Modifier.align(Alignment.BottomStart).padding(6.dp))
        }

        Column(Modifier.padding(10.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(map.nameTr, color=Yellow, fontSize=12.sp, fontWeight=FontWeight.Bold, maxLines=1, overflow=TextOverflow.Ellipsis)
            Text(map.descTr, color=TextSec, fontSize=10.sp, maxLines=2, overflow=TextOverflow.Ellipsis, lineHeight=14.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp), modifier=Modifier.padding(top=2.dp)) {
                map.entityTypes.take(4).forEach { typeId ->
                    val entity = EntityType.entries.getOrNull(typeId)
                    if (entity != null) {
                        Box(
                            Modifier.clip(RoundedCornerShape(1.dp))
                                .background(DangerRed.copy(0.15f))
                                .padding(horizontal=4.dp, vertical=2.dp)
                        ) {
                            Text(entity.displayName.take(4), color=DangerRed, fontSize=8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapDetailOverlay(map: MapDto, theme: LevelTheme, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.85f)).clickable(onClick=onDismiss)) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(4.dp))
                .background(MetalBg)
                .border(1.dp, theme.primaryColor.copy(0.5f), RoundedCornerShape(4.dp))
                .clickable {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Level ${map.level} — ${map.nameTr}", color=theme.primaryColor, fontSize=18.sp, fontWeight=FontWeight.Black)
                IconButton(onClick=onDismiss) { Icon(Icons.Default.Close, null, tint=TextSec) }
            }

            DividerLine()

            Text(map.descTr, color=TextSec, fontSize=13.sp, lineHeight=20.sp)

            Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                InfoChip("Zorluk", map.threatLevel.toString(), DangerRed)
                InfoChip("Varlıklar", map.entityTypes.size.toString(), CrtAmber)
                InfoChip("Durum", if (map.isUnlocked) "Açık" else "Kilitli",
                    if (map.isUnlocked) SuccessGreen else TextDim)
            }

            DividerLine()

            Text("Varlıklar", color=TextSec, fontSize=11.sp, letterSpacing=2.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                map.entityTypes.forEach { typeId ->
                    val entity = EntityType.entries.getOrNull(typeId) ?: return@forEach
                    Box(
                        Modifier.clip(RoundedCornerShape(2.dp))
                            .background(DangerRed.copy(0.12f))
                            .border(1.dp, DangerRed.copy(0.4f), RoundedCornerShape(2.dp))
                            .padding(horizontal=8.dp, vertical=4.dp)
                    ) {
                        Text(entity.displayName, color=DangerRed, fontSize=10.sp)
                    }
                }
            }

            if (!map.isUnlocked) {
                OmniButton(
                    text = "Kilidi Aç",
                    onClick = {},
                    width = Double.MAX_VALUE.dp,
                    height = 44.dp,
                    accent = CrtAmber
                )
            }
        }
    }
}

@Composable
private fun ThreatBadge(level: Int, modifier: Modifier = Modifier) {
    val color = when {
        level >= 8 -> DangerRed
        level >= 5 -> CrtAmber
        level >= 3 -> Yellow
        else       -> SuccessGreen
    }
    val label = when {
        level >= 8 -> "KRİTİK"
        level >= 5 -> "YÜKSEK"
        level >= 3 -> "ORTA"
        else       -> "DÜŞÜK"
    }
    Box(
        modifier.clip(RoundedCornerShape(2.dp))
            .background(color.copy(0.2f))
            .border(1.dp, color.copy(0.6f), RoundedCornerShape(2.dp))
            .padding(horizontal=5.dp, vertical=2.dp)
    ) {
        Text(label, color=color, fontSize=8.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp)
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color=TextDim, fontSize=9.sp, letterSpacing=1.sp)
        Text(value, color=color, fontSize=14.sp, fontWeight=FontWeight.Bold)
    }
}
