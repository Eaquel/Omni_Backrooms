package com.omni.backrooms

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
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

internal enum class MarketTab(val labelRes: Int, val icon: ImageVector) {
    Boosts    (R.string.market_tab_boosts,     Icons.Default.Bolt),
    Characters(R.string.market_tab_characters, Icons.Default.Person),
    Soulium   (R.string.market_tab_soulium,    Icons.Default.AutoAwesome),
    Vip       (R.string.market_tab_vip,        Icons.Default.Star),
    Daily     (R.string.market_tab_daily,      Icons.Default.LocalOffer)
}

data class MarketUiState(
    val items       : List<MarketItemDto> = emptyList(),
    val dailyDeals  : List<MarketItemDto> = emptyList(),
    val isLoading   : Boolean             = false,
    val error       : String?             = null,
    val purchasing  : String?             = null,
    val successMsg  : String?             = null,
    val tab         : MarketTab           = MarketTab.Boosts,
    val omniumBal   : Long                = 0L,
    val souliumBal  : Long                = 0L,
    val isVip       : Boolean             = false,
    val confirmItem : MarketItemDto?      = null
)

@HiltViewModel
class MarketVM @Inject constructor(
    private val api         : ApiService,
    private val profileVM   : PlayerProfileVM
) : ViewModel() {

    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init {
        loadTab(MarketTab.Boosts)
        loadDaily()
        viewModelScope.launch {
            profileVM.profile.collect { p ->
                _state.update { it.copy(omniumBal=p.omniumAmount, souliumBal=p.souliumAmount, isVip=p.isVip) }
            }
        }
    }

    fun setTab(tab: MarketTab) {
        _state.update { it.copy(tab=tab) }
        if (tab == MarketTab.Daily) return
        loadTab(tab)
    }

    private fun loadTab(tab: MarketTab) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading=true, error=null) }
            val category = tab.name.lowercase()
            runCatching { api.getMarketItems(category) }
                .onSuccess { page -> _state.update { it.copy(isLoading=false, items=page.items) } }
                .onFailure { e   -> _state.update { it.copy(isLoading=false, error=e.message, items=fallbackItems(tab)) } }
        }
    }

    private fun loadDaily() {
        viewModelScope.launch {
            runCatching { api.getDailyDeals() }
                .onSuccess { deals -> _state.update { it.copy(dailyDeals=deals) } }
                .onFailure { _state.update { it.copy(dailyDeals=fallbackDaily()) } }
        }
    }

    fun requestPurchase(item: MarketItemDto) { _state.update { it.copy(confirmItem=item) } }
    fun cancelPurchase()                      { _state.update { it.copy(confirmItem=null) } }

    fun confirmPurchase() {
        val item = _state.value.confirmItem ?: return
        _state.update { it.copy(confirmItem=null, purchasing=item.id) }
        viewModelScope.launch {
            runCatching { api.buyItem(BuyRequest(item.id, item.currency)) }
                .onSuccess { resp ->
                    val newBal = resp.newBalance
                    _state.update { s ->
                        val newItems = s.items.map { i -> if (i.id==item.id) i.copy(isOwned=true) else i }
                        when (item.currency.lowercase()) {
                            "omnium"  -> s.copy(purchasing=null, items=newItems, omniumBal=newBal,  successMsg="${item.nameTr} satın alındı!")
                            "soulium" -> s.copy(purchasing=null, items=newItems, souliumBal=newBal, successMsg="${item.nameTr} satın alındı!")
                            else      -> s.copy(purchasing=null, items=newItems, successMsg="${item.nameTr} satın alındı!")
                        }
                    }
                }
                .onFailure { e -> _state.update { it.copy(purchasing=null, error=e.message) } }
        }
    }

    fun clearMessage() { _state.update { it.copy(successMsg=null, error=null) } }

    fun displayedItems(): List<MarketItemDto> {
        val s = _state.value
        return if (s.tab == MarketTab.Daily) s.dailyDeals else s.items
    }

    private fun fallbackItems(tab: MarketTab): List<MarketItemDto> = when (tab) {
        MarketTab.Boosts     -> listOf(
            fakeItem("speed_boost",  "Hız Boost",       "Hareket hızını %30 artırır.", "500",  "omnium"),
            fakeItem("night_vision", "Gece Görüşü",     "Karanlıkta görüşü artırır.",  "300",  "omnium"),
            fakeItem("silent_walk",  "Sessiz Adım",     "Entity duyma mesafesini azaltır.","750","omnium"),
            fakeItem("heal_pack",    "İlk Yardım Kiti", "50 HP yeniler.",              "200",  "omnium"),
            fakeItem("stamina",      "Stamina Artışı",  "Koşu süresini uzatır.",       "400",  "omnium"),
            fakeItem("radar",        "Mini Radar",      "Entity konumunu gösterir.",   "1200", "omnium")
        )
        MarketTab.Characters -> listOf(
            fakeItem("char_scout",    "Scout",    "Hızlı ve sessiz.", "2500", "omnium"),
            fakeItem("char_survivor", "Survivor", "Yüksek HP.",       "800",  "soulium"),
            fakeItem("char_engineer", "Engineer", "Tuzak kurabilir.", "1200", "soulium"),
            fakeItem("char_ghost",    "Ghost",    "Görünmezlik.",     "2000", "soulium")
        )
        MarketTab.Soulium    -> listOf(
            fakeItem("soul_100",  "100 Soulium",  "Temel paket.",   "0.99",  "tl"),
            fakeItem("soul_500",  "500 Soulium",  "Değer paketi.",  "3.99",  "tl"),
            fakeItem("soul_2000", "2000 Soulium", "Premium paket.", "12.99", "tl"),
            fakeItem("soul_5000", "5000 Soulium", "Mega paket.",    "24.99", "tl")
        )
        MarketTab.Vip        -> listOf(
            fakeItem("vip_monthly", "Aylık VIP",  "30 gün VIP avantajları.", "24.99",  "tl"),
            fakeItem("vip_yearly",  "Yıllık VIP", "365 gün + bonus.",        "199.99", "tl")
        )
        else -> emptyList()
    }

    private fun fallbackDaily(): List<MarketItemDto> = listOf(
        fakeItem("daily_1", "Günlük Fırsat 1", "Sınırlı süre!", "250", "omnium"),
        fakeItem("daily_2", "Günlük Fırsat 2", "Bugüne özel!",  "800", "soulium")
    )

    private fun fakeItem(id: String, name: String, desc: String, price: String, currency: String) = MarketItemDto(
        id=id, nameTr=name, nameEn=name, descTr=desc, descEn=desc,
        category="", price=price.toLongOrNull() ?: 0L, currency=currency,
        imageUrl=null, isOwned=false, isEquipped=false, isLimited=false, expiresMs=null
    )
}

@Composable
private internal fun Market(onBack: () -> Unit, vm: MarketVM = hiltViewModel()) {
    val s   by vm.state.collectAsState()
    val items = vm.displayedItems()

    LaunchedEffect(s.successMsg, s.error) {
        if (s.successMsg != null || s.error != null) {
            kotlinx.coroutines.delay(3000)
            vm.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal=8.dp, vertical=4.dp),
                verticalAlignment=Alignment.CenterVertically
            ) {
                IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint=Yellow) }
                Text(stringResource(R.string.market_title), color=Yellow, fontSize=16.sp, fontWeight=FontWeight.Bold, letterSpacing=3.sp)
                Spacer(Modifier.weight(1f))
                CurrencyBadge(s.omniumBal,  OmniumCol,  Icons.Default.Diamond)
                Spacer(Modifier.width(8.dp))
                CurrencyBadge(s.souliumBal, SouliumCol, Icons.Default.AutoAwesome)
                if (s.isVip) {
                    Spacer(Modifier.width(8.dp))
                    VipBadge()
                }
            }

            ScrollableTabRow(
                selectedTabIndex = s.tab.ordinal,
                containerColor   = MetalBg.copy(0.5f),
                contentColor     = Yellow,
                indicator = { pos -> Box(Modifier.tabIndicatorOffset(pos[s.tab.ordinal]).height(2.dp).background(Yellow)) },
                edgePadding = 4.dp
            ) {
                MarketTab.entries.forEach { t ->
                    Tab(
                        selected = s.tab==t, onClick = { vm.setTab(t) },
                        icon = { Icon(t.icon, null, Modifier.size(15.dp)) },
                        text = { Text(stringResource(t.labelRes), fontSize=10.sp, letterSpacing=1.sp) },
                        selectedContentColor = Yellow, unselectedContentColor = TextDim
                    )
                }
            }

            DividerLine()

            if (s.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color=Yellow, strokeWidth=2.dp) }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key={ it.id }) { item ->
                        MarketCard(
                            item        = item,
                            isPurchasing= s.purchasing==item.id,
                            onBuy       = { vm.requestPurchase(item) }
                        )
                    }
                }
            }
        }

        s.successMsg?.let { msg ->
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp), containerColor=SuccessGreen) {
                Text(msg, color=Color.Black, fontWeight=FontWeight.Bold)
            }
        }

        s.error?.let { err ->
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp), containerColor=DangerRed) {
                Text(err, color=Color.White)
            }
        }

        s.confirmItem?.let { item ->
            PurchaseConfirmDialog(item=item, onConfirm=vm::confirmPurchase, onCancel=vm::cancelPurchase)
        }
    }
}

@Composable
private fun MarketCard(item: MarketItemDto, isPurchasing: Boolean, onBuy: () -> Unit) {
    val currencyColor = when (item.currency.lowercase()) {
        "omnium"  -> OmniumCol
        "soulium" -> SouliumCol
        "tl"      -> SuccessGreen
        else      -> CrtAmber
    }

    val inf  = rememberInfiniteTransition(label = "card")
    val glow by inf.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(2000, easing=EaseInOut), RepeatMode.Reverse), "g")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MetalBg)
            .border(1.dp, if(item.isLimited) CrtAmber.copy(glow) else BorderCol, RoundedCornerShape(3.dp))
            .padding(12.dp)
    ) {
        if (item.isLimited) {
            Box(Modifier.fillMaxWidth().padding(bottom=4.dp), Alignment.TopEnd) {
                Box(Modifier.clip(RoundedCornerShape(2.dp)).background(CrtAmber.copy(0.2f)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Text("SINIRLI", color=CrtAmber, fontSize=8.sp, fontWeight=FontWeight.Black, letterSpacing=1.sp)
                }
            }
        }

        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(currencyColor.copy(0.12f)),
            Alignment.Center
        ) {
            Icon(Icons.Default.Category, null, tint=currencyColor.copy(0.9f), modifier=Modifier.size(28.dp))
        }

        Spacer(Modifier.height(8.dp))

        Text(item.nameTr, color=Yellow, fontSize=12.sp, fontWeight=FontWeight.Bold,
            letterSpacing=1.sp, textAlign=TextAlign.Center, maxLines=2, overflow=TextOverflow.Ellipsis)

        Text(item.descTr, color=TextDim, fontSize=9.sp, textAlign=TextAlign.Center,
            maxLines=2, overflow=TextOverflow.Ellipsis, lineHeight=13.sp, modifier=Modifier.padding(top=3.dp))

        Spacer(Modifier.height(10.dp))

        if (item.isOwned) {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint=SuccessGreen, modifier=Modifier.size(14.dp))
                Text(stringResource(R.string.market_equipped), color=SuccessGreen, fontSize=10.sp, fontWeight=FontWeight.Bold)
            }
        } else if (isPurchasing) {
            CircularProgressIndicator(color=Yellow, strokeWidth=2.dp, modifier=Modifier.size(24.dp))
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(currencyColor.copy(0.15f))
                    .border(1.dp, currencyColor.copy(0.5f), RoundedCornerShape(2.dp))
                    .clickable(onClick=onBuy)
            ) {
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(item.price.toString(), color=currencyColor, fontSize=12.sp, fontWeight=FontWeight.Bold)
                    Text(item.currency.uppercase(), color=currencyColor.copy(0.7f), fontSize=9.sp)
                }
            }
        }
    }
}

@Composable
private fun PurchaseConfirmDialog(item: MarketItemDto, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)).clickable(onClick=onCancel)) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(4.dp))
                .background(MetalBg)
                .border(1.dp, Yellow.copy(0.4f), RoundedCornerShape(4.dp))
                .clickable {}
                .padding(20.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Text("Satın Al", color=Yellow, fontSize=18.sp, fontWeight=FontWeight.Black)
            DividerLine()
            Text(item.nameTr, color=Yellow, fontSize=15.sp, fontWeight=FontWeight.Bold)
            Text(item.descTr, color=TextSec, fontSize=12.sp, textAlign=TextAlign.Center)
            DividerLine()
            Text("${item.price} ${item.currency.uppercase()}", color=OmniumCol, fontSize=20.sp, fontWeight=FontWeight.Black)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OmniButton(stringResource(R.string.common_cancel), onCancel, width=120.dp, height=44.dp, accent=TextDim)
                OmniButton(stringResource(R.string.market_buy), onConfirm, width=120.dp, height=44.dp)
            }
        }
    }
}

@Composable
private fun CurrencyBadge(amount: Long, color: Color, icon: ImageVector) {
    Row(
        Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg).padding(horizontal=8.dp, vertical=4.dp),
        verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint=color, modifier=Modifier.size(12.dp))
        Text(amount.toString(), color=color, fontSize=12.sp, fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun VipBadge() {
    Box(
        Modifier.clip(RoundedCornerShape(2.dp)).background(Color(0xFFFFD700).copy(0.2f))
            .border(1.dp, Color(0xFFFFD700).copy(0.6f), RoundedCornerShape(2.dp))
            .padding(horizontal=8.dp, vertical=4.dp)
    ) {
        Text("VIP", color=Color(0xFFFFD700), fontSize=11.sp, fontWeight=FontWeight.Black, letterSpacing=2.sp)
    }
}

data class CharactersUiState(
    val characters  : List<CharacterDto> = emptyList(),
    val selected    : CharacterDto?      = null,
    val isLoading   : Boolean            = false,
    val error       : String?            = null,
    val equipping   : String?            = null,
    val successMsg  : String?            = null
)

@HiltViewModel
class CharactersVM @Inject constructor(
    private val api         : ApiService,
    private val assetManager: AssetManager
) : ViewModel() {

    private val _state = MutableStateFlow(CharactersUiState())
    val state: StateFlow<CharactersUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { api.getCharacters() }
                .onSuccess { chars ->
                    val selected = chars.firstOrNull { it.isEquipped } ?: chars.firstOrNull()
                    _state.update { it.copy(isLoading = false, characters = chars, selected = selected) }
                }
                .onFailure { _state.update { it.copy(isLoading = false, characters = fallback()) } }
        }
    }

    fun select(char: CharacterDto) { _state.update { it.copy(selected = char) } }

    fun equip(charId: String) {
        viewModelScope.launch {
            _state.update { it.copy(equipping = charId) }
            runCatching { api.equipCharacter(charId) }
                .onSuccess {
                    _state.update { s ->
                        val updated = s.characters.map { c -> c.copy(isEquipped = c.id == charId) }
                        s.copy(equipping = null, characters = updated, successMsg = "Karakter takıldı!")
                    }
                }
                .onFailure { e -> _state.update { it.copy(equipping = null, error = e.message) } }
        }
    }

    fun clearMsg() { _state.update { it.copy(successMsg = null, error = null) } }

    fun getClassColor(clazz: String): Color = when (clazz.lowercase()) {
        "wanderer"  -> Yellow
        "scout"     -> SuccessGreen
        "survivor"  -> DangerRed
        "engineer"  -> CrtAmber
        "ghost"     -> SouliumCol
        else        -> TextSec
    }

    fun getClassIcon(clazz: String) = when (clazz.lowercase()) {
        "scout"    -> Icons.Default.DirectionsRun
        "survivor" -> Icons.Default.Shield
        "engineer" -> Icons.Default.Build
        "ghost"    -> Icons.Default.BrightnessLow
        else       -> Icons.Default.Person
    }

    private fun fallback(): List<CharacterDto> = assetManager.defaultCharacters.map { c ->
        CharacterDto(
            id          = c.id,
            nameTr      = c.name,
            nameEn      = c.name,
            clazz       = c.clazz.name.lowercase(),
            maxHp       = c.maxHp,
            baseSpeed   = c.baseSpeed,
            stealthMult = c.stealthMult,
            staminaMult = c.staminaMult,
            abilities   = c.abilities,
            isUnlocked  = c.isUnlocked,
            isEquipped  = c.isEquipped,
            imageUrl    = null,
            price       = 0L,
            currency    = "omnium"
        )
    }
}

@Composable
fun Characters(onBack: () -> Unit, vm: CharactersVM = hiltViewModel()) {
    val s by vm.state.collectAsState()

    LaunchedEffect(s.successMsg, s.error) {
        if (s.successMsg != null || s.error != null) {
            kotlinx.coroutines.delay(2500)
            vm.clearMsg()
        }
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack("Karakterler", onBack)
            DividerLine()

            if (s.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    LazyRow(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(s.characters, key = { it.id }) { char ->
                            CharacterThumb(
                                char        = char,
                                isSelected  = s.selected?.id == char.id,
                                classColor  = vm.getClassColor(char.clazz),
                                onClick     = { vm.select(char) }
                            )
                        }
                    }

                    DividerLine()

                    s.selected?.let { char ->
                        CharacterDetailPanel(
                            char       = char,
                            classColor = vm.getClassColor(char.clazz),
                            classIcon  = vm.getClassIcon(char.clazz),
                            isEquipping= s.equipping == char.id,
                            onEquip    = { vm.equip(char.id) }
                        )
                    }
                }
            }
        }

        s.successMsg?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = SuccessGreen
            ) { Text(msg, color = Color.Black, fontWeight = FontWeight.Bold) }
        }

        s.error?.let { err ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = DangerRed
            ) { Text(err, color = Color.White) }
        }
    }
}

@Composable
private fun CharacterThumb(
    char       : CharacterDto,
    isSelected : Boolean,
    classColor : Color,
    onClick    : () -> Unit
) {
    val inf  = rememberInfiniteTransition(label = "cth")
    val glow by inf.animateFloat(
        0.3f, 0.9f,
        infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), "g"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) classColor.copy(0.15f) else MetalBg)
            .border(2.dp, if (isSelected) classColor.copy(glow) else BorderCol, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(classColor.copy(0.3f), DarkBg), radius = 80f))
                .drawWithContent {
                    drawContent()
                    if (!char.isUnlocked) drawRect(Color.Black.copy(0.6f))
                },
            Alignment.Center
        ) {
            if (char.isUnlocked) {
                Text(char.nameTr.take(2).uppercase(), color = classColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
            } else {
                Icon(Icons.Default.Lock, null, tint = TextDim, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            char.nameTr,
            color    = if (isSelected) classColor else TextSec,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines  = 1
        )

        if (char.isEquipped) {
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(1.dp))
                    .background(SuccessGreen.copy(0.2f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("AKTİF", color = SuccessGreen, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun CharacterDetailPanel(
    char        : CharacterDto,
    classColor  : Color,
    classIcon   : ImageVector,
    isEquipping : Boolean,
    onEquip     : () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(classColor.copy(0.15f))
                    .border(2.dp, classColor.copy(0.5f), RoundedCornerShape(6.dp)),
                Alignment.Center
            ) {
                Icon(classIcon, null, tint = classColor, modifier = Modifier.size(36.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(char.nameTr, color = Yellow, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Box(
                        Modifier.clip(RoundedCornerShape(2.dp))
                            .background(classColor.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(char.clazz.uppercase(), color = classColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                }
                Text(
                    if (char.isUnlocked) "Kilidi Açık" else "Kilitli — ${char.price} ${char.currency.uppercase()}",
                    color    = if (char.isUnlocked) SuccessGreen else TextDim,
                    fontSize = 12.sp
                )
            }
        }

        DividerLine()

        Text("İstatistikler", color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)

        StatRow("Can Puanı",    char.maxHp / 200f,     "${char.maxHp.toInt()} HP",               DangerRed)
        StatRow("Hareket Hızı", char.baseSpeed / 6f,   "${char.baseSpeed} m/s",                   SuccessGreen)
        StatRow("Gizlilik",     char.stealthMult / 2f, "${(char.stealthMult * 100).toInt()}%",    SouliumCol)
        StatRow("Dayanıklılık", char.staminaMult / 2f, "${(char.staminaMult * 100).toInt()}%",    CrtAmber)

        DividerLine()

        Text("Yetenekler", color = TextSec, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)

        char.abilities.forEach { ability ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MetalBg)
                    .border(1.dp, classColor.copy(0.3f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, null, tint = classColor, modifier = Modifier.size(14.dp))
                Text(ability, color = TextSec, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(4.dp))

        if (char.isUnlocked) {
            if (char.isEquipped) {
                Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bu karakter aktif olarak kullanılıyor", color = SuccessGreen, fontSize = 13.sp)
                }
            } else {
                OmniButton(
                    text    = if (isEquipping) "Takılıyor…" else "Karakteri Tak",
                    onClick = onEquip,
                    enabled = !isEquipping,
                    width   = Double.MAX_VALUE.dp,
                    height  = 50.dp,
                    accent  = classColor
                )
            }
        } else {
            OmniButton(
                text   = "Kilidi Aç — ${char.price} ${char.currency.uppercase()}",
                onClick = {},
                width  = Double.MAX_VALUE.dp,
                height = 50.dp,
                accent = CrtAmber
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: Float, display: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = TextSec, fontSize = 11.sp)
            Text(display, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress       = { value.coerceIn(0f, 1f) },
            modifier       = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color          = color,
            trackColor     = MetalBg
        )
    }
}

data class LeaderboardUiState(
    val entries     : List<LeaderboardEntry> = emptyList(),
    val myRank      : Int?                   = null,
    val isLoading   : Boolean                = false,
    val error       : String?                = null,
    val difficulty  : String?                = null,
    val region      : String?                = null,
    val page        : Int                    = 0,
    val totalPages  : Int                    = 1
)

@HiltViewModel
class LeaderboardVM @Inject constructor(
    private val api      : ApiService,
    private val profileVM: PlayerProfileVM
) : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                api.getLeaderboard(page = s.page, difficulty = s.difficulty, region = s.region)
            }.onSuccess { page ->
                _state.update { it.copy(
                    isLoading   = false,
                    entries     = page.entries,
                    myRank      = page.myRank,
                    totalPages  = maxOf(1, (50 + 49) / 50)
                ) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message, entries = fallback()) }
            }
        }
    }

    fun setDifficulty(d: String?) { _state.update { it.copy(difficulty = d, page = 0) }; load() }
    fun setRegion(r: String?)     { _state.update { it.copy(region = r, page = 0) }; load() }

    fun nextPage() {
        val s = _state.value
        if (s.page < s.totalPages - 1) { _state.update { it.copy(page = it.page + 1) }; load() }
    }

    fun prevPage() {
        if (_state.value.page > 0) { _state.update { it.copy(page = it.page - 1) }; load() }
    }

    fun rankColor(rank: Int): Color = when (rank) {
        1    -> Color(0xFFFFD700)
        2    -> Color(0xFFC0C0C0)
        3    -> Color(0xFFCD7F32)
        else -> TextSec
    }

    fun rankIcon(rank: Int) = when (rank) {
        1, 2, 3 -> Icons.Default.EmojiEvents
        else    -> null
    }

    private fun fallback(): List<LeaderboardEntry> = (1..20).map { rank ->
        LeaderboardEntry(
            rank       = rank,
            playerId   = rank,
            playerName = "Player_${rank * 7 + 13}",
            avatarUrl  = null,
            level      = maxOf(1, 20 - rank + 1),
            score      = maxOf(100L, (50000L - rank * 2400)),
            survived   = maxOf(0, 15 - rank),
            difficulty = listOf("hard","hard","normal","normal","easy").random(),
            region     = listOf("TR","EN","DE").random()
        )
    }
}

@Composable
fun Leaderboard(onBack: () -> Unit, vm: LeaderboardVM = hiltViewModel()) {
    val s by vm.state.collectAsState()

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            TopBarBack("Liderlik Tablosu", onBack)
            DividerLine()

            Row(
                Modifier.fillMaxWidth().background(MetalBg.copy(0.5f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Zorluk:", color = TextDim, fontSize = 10.sp)
                listOf(null to "Tümü", "easy" to "Kolay", "normal" to "Normal", "hard" to "Zor")
                    .forEach { (key, label) ->
                        FilterPill(label, s.difficulty == key) { vm.setDifficulty(key) }
                    }
                Spacer(Modifier.width(8.dp))
                Text("Bölge:", color = TextDim, fontSize = 10.sp)
                listOf(null to "Tümü", "TR" to "TR", "EN" to "EN", "DE" to "DE")
                    .forEach { (key, label) ->
                        FilterPill(label, s.region == key) { vm.setRegion(key) }
                    }
            }

            DividerLine()

            if (s.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Yellow, strokeWidth = 2.dp)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    s.myRank?.let { myRank ->
                        Box(
                            Modifier.fillMaxWidth()
                                .background(Yellow.copy(0.08f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = Yellow, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Senin Sıralaman: #$myRank", color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        DividerLine()
                    }

                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(s.entries, key = { _, e -> e.playerId }) { _, entry ->
                            LeaderboardRow(entry = entry, rankColor = vm.rankColor(entry.rank), rankIcon = vm.rankIcon(entry.rank))
                        }
                        if (s.entries.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                                    Text("Henüz kayıt yok", color = TextDim, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().background(Color.Black.copy(0.5f)).padding(horizontal = 16.dp, vertical = 8.dp),
                        Arrangement.Center,
                        Alignment.CenterVertically
                    ) {
                        OmniButton("Önceki", vm::prevPage, enabled = s.page > 0, width = 100.dp, height = 36.dp)
                        Spacer(Modifier.width(16.dp))
                        Text("${s.page + 1} / ${s.totalPages}", color = TextSec, fontSize = 12.sp)
                        Spacer(Modifier.width(16.dp))
                        OmniButton("Sonraki", vm::nextPage, enabled = s.page < s.totalPages - 1, width = 100.dp, height = 36.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, rankColor: Color, rankIcon: ImageVector?) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(if (entry.rank <= 3) rankColor.copy(0.08f) else MetalBg.copy(0.6f))
            .border(1.dp, if (entry.rank <= 3) rankColor.copy(0.4f) else BorderCol, RoundedCornerShape(2.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(28.dp), Alignment.Center) {
            if (rankIcon != null) {
                Icon(rankIcon, null, tint = rankColor, modifier = Modifier.size(22.dp))
            } else {
                Text("#${entry.rank}", color = rankColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            Modifier.size(32.dp).clip(CircleShape).background(rankColor.copy(0.2f)),
            Alignment.Center
        ) {
            Text(entry.playerName.take(2).uppercase(), color = rankColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }

        Column(Modifier.weight(1f)) {
            Text(entry.playerName, color = Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Lv.${entry.level}", color = TextDim, fontSize = 10.sp)
                Text(entry.region, color = TextDim, fontSize = 10.sp)
                Text(entry.difficulty.uppercase(), color = when (entry.difficulty) {
                    "hard"   -> DangerRed
                    "normal" -> CrtAmber
                    else     -> SuccessGreen
                }, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(formatScore(entry.score), color = rankColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("${entry.survived} hayatta", color = TextDim, fontSize = 9.sp)
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clip(RoundedCornerShape(2.dp))
            .background(if (selected) Yellow.copy(0.15f) else MetalBg.copy(0.5f))
            .border(1.dp, if (selected) Yellow.copy(0.6f) else BorderCol, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = if (selected) Yellow else TextDim, fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun formatScore(score: Long): String = when {
    score >= 1_000_000 -> "${score / 1_000_000}M"
    score >= 1_000     -> "${score / 1_000}K"
    else               -> score.toString()
}
