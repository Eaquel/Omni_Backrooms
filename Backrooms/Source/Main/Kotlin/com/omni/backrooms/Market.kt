package com.omni.backrooms

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MarketTab(val labelRes: Int, val icon: ImageVector) {
    Boosts    (R.string.market_tab_boosts,     Icons.Default.Bolt),
    Characters(R.string.market_tab_characters, Icons.Default.Person),
    Soulium   (R.string.market_tab_soulium,    Icons.Default.AutoAwesome),
    Vip       (R.string.market_tab_vip,        Icons.Default.Star),
    Daily     (R.string.market_tab_daily,      Icons.Default.LocalOffer)
}

private val ANON_NAME_CHARS = listOf(
    '%','#','₺','&','@','!','?','*','§','¿','¡','†','‡','~','^','|','≈','∆','√','∞'
)
private const val ANON_NAME_FRAME_MS = 120L

@Composable
fun rememberAnonDisplayName(): String {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(ANON_NAME_FRAME_MS); frame++ } }
    return (0..5).joinToString("") { slot -> ANON_NAME_CHARS[(frame + slot * 3) % ANON_NAME_CHARS.size].toString() }
}

fun buildAnonId(ctx: Context): String {
    val id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)?.take(8)?.lowercase() ?: "00000000"
    return "Player$id"
}

data class MarketUiState(
    val items          : List<MarketItemDto> = emptyList(),
    val dailyDeals     : List<MarketItemDto> = emptyList(),
    val isLoading      : Boolean             = false,
    val error          : String?             = null,
    val purchasing     : String?             = null,
    val successMsg     : String?             = null,
    val tab            : MarketTab           = MarketTab.Boosts,
    val omniumBal      : Long                = 0L,
    val souliumBal     : Long                = 0L,
    val isVip          : Boolean             = false,
    val confirmItem    : MarketItemDto?      = null,
    val characters     : List<CharacterDto>  = emptyList(),
    val selectedChar   : CharacterDto?       = null,
    val charsLoading   : Boolean             = false,
    val equipping      : String?             = null
)

@HiltViewModel
class MarketVM @Inject constructor(
    private val api         : ApiService,
    private val assetManager: AssetManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appCtx: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init { loadTab(MarketTab.Boosts); loadDaily(); loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            runCatching { api.getProfile() }.onSuccess { p ->
                _state.update { it.copy(omniumBal=p.omniumAmount, souliumBal=p.souliumAmount, isVip=p.isVip) }
            }
        }
    }

    fun setTab(tab: MarketTab) {
        _state.update { it.copy(tab=tab) }
        when (tab) { MarketTab.Characters -> loadCharacters(); MarketTab.Daily -> return; else -> loadTab(tab) }
    }

    private fun loadTab(tab: MarketTab) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading=true, error=null) }
            runCatching { api.getMarketItems(tab.name.lowercase()) }
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

    fun loadCharacters() {
        viewModelScope.launch {
            _state.update { it.copy(charsLoading=true) }
            runCatching { api.getCharacters() }
                .onSuccess { chars ->
                    val selected = chars.firstOrNull { it.isEquipped } ?: chars.firstOrNull()
                    _state.update { it.copy(charsLoading=false, characters=chars, selectedChar=selected) }
                }
                .onFailure { _state.update { it.copy(charsLoading=false, characters=emptyList()) } }
        }
    }

    fun selectChar(char: CharacterDto) { _state.update { it.copy(selectedChar=char) } }

    fun equipChar(charId: String) {
        viewModelScope.launch {
            _state.update { it.copy(equipping=charId) }
            runCatching { api.equipCharacter(charId) }
                .onSuccess {
                    val updated = _state.value.characters.map { c -> c.copy(isEquipped=c.id==charId) }
                    _state.update { it.copy(equipping=null, characters=updated) }
                    runCatching {
                        val analytics = FirebaseAnalytics.getInstance(appCtx)
                        analytics.logEvent("character_equip") { param("char_id", charId) }
                        FirebaseFirestore.getInstance().collection("user_chars").document(charId).set(mapOf("equipped" to true))
                    }
                }
                .onFailure { e -> _state.update { it.copy(equipping=null, error=e.message) } }
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
                    val nb       = resp.newBalance
                    val newItems = _state.value.items.map { i -> if (i.id==item.id) i.copy(isOwned=true) else i }
                    val msg      = "${item.nameTr} satın alındı!"
                    _state.update { s ->
                        when (item.currency.lowercase()) {
                            "omnium"  -> s.copy(purchasing=null, items=newItems, omniumBal=nb,  successMsg=msg)
                            "soulium" -> s.copy(purchasing=null, items=newItems, souliumBal=nb, successMsg=msg)
                            else      -> s.copy(purchasing=null, items=newItems,                successMsg=msg)
                        }
                    }
                    runCatching {
                        val analytics = FirebaseAnalytics.getInstance(appCtx)
                        analytics.logEvent(FirebaseAnalytics.Event.PURCHASE) {
                            param(FirebaseAnalytics.Param.ITEM_ID,   item.id)
                            param(FirebaseAnalytics.Param.ITEM_NAME, item.nameTr)
                            param(FirebaseAnalytics.Param.CURRENCY,  item.currency)
                            param(FirebaseAnalytics.Param.VALUE,     item.price.toDouble())
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(purchasing=null, error=e.message) }
                    runCatching { FirebaseCrashlytics.getInstance().log("purchase_fail item=${item.id} err=${e.message}") }
                }
        }
    }

    fun clearMessage() { _state.update { it.copy(successMsg=null, error=null) } }

    fun displayedItems(): List<MarketItemDto> {
        val s = _state.value
        return if (s.tab==MarketTab.Daily) s.dailyDeals else s.items
    }

    fun getClassColor(clazz: String): Color = when (clazz.lowercase()) {
        "wanderer" -> Yellow; "scout" -> SuccessGreen; "survivor" -> DangerRed
        "engineer" -> CrtAmber; "ghost" -> SouliumCol; else -> TextSec
    }

    fun getClassIcon(clazz: String): ImageVector = when (clazz.lowercase()) {
        "scout"    -> Icons.Default.DirectionsRun; "survivor" -> Icons.Default.Shield
        "engineer" -> Icons.Default.Build;         "ghost"    -> Icons.Default.BrightnessLow
        else       -> Icons.Default.Person
    }

    private fun fallbackItems(tab: MarketTab): List<MarketItemDto> = when (tab) {
        MarketTab.Boosts -> listOf(
            fakeItem("speed_boost",  "Hız Boost",       "Hareket hızını %30 artırır.", "Speed Boost",    "Increases movement speed by 30%.", 500,  "omnium"),
            fakeItem("night_vision", "Gece Görüşü",     "Karanlıkta görüşü artırır.",  "Night Vision",   "Improves vision in the dark.",     300,  "omnium"),
            fakeItem("silent_walk",  "Sessiz Adım",     "Entity duyma mesafesini azaltır.", "Silent Step","Reduces entity hearing range.",   750,  "omnium"),
            fakeItem("heal_pack",    "İlk Yardım Kiti", "50 HP yeniler.",              "First Aid Kit",  "Restores 50 HP.",                  200,  "omnium"),
            fakeItem("stamina",      "Stamina Artışı",  "Koşu süresini uzatır.",       "Stamina Boost",  "Extends sprint duration.",         400,  "omnium"),
            fakeItem("radar",        "Mini Radar",      "Entity konumunu gösterir.",   "Mini Radar",     "Shows entity positions.",          1200, "omnium")
        )
        MarketTab.Soulium -> listOf(
            fakeItem("soul_100",  "100 Soulium",  "Temel paket.",   "100 Soulium",  "Starter pack.",  0, "tl", realPrice="0.99"),
            fakeItem("soul_500",  "500 Soulium",  "Değer paketi.",  "500 Soulium",  "Value pack.",    0, "tl", realPrice="3.99"),
            fakeItem("soul_2000", "2000 Soulium", "Premium paket.", "2000 Soulium", "Premium pack.",  0, "tl", realPrice="12.99"),
            fakeItem("soul_5000", "5000 Soulium", "Mega paket.",    "5000 Soulium", "Mega pack.",     0, "tl", realPrice="24.99")
        )
        MarketTab.Vip -> listOf(
            fakeItem("vip_monthly", "Aylık VIP",  "30 gün VIP avantajları.", "Monthly VIP", "30 days of VIP perks.", 0, "tl", realPrice="24.99"),
            fakeItem("vip_yearly",  "Yıllık VIP", "365 gün + bonus.",        "Yearly VIP",  "365 days + bonus.",     0, "tl", realPrice="199.99")
        )
        else -> emptyList()
    }

    private fun fallbackDaily(): List<MarketItemDto> = listOf(
        fakeItem("daily_1", "Günlük Fırsat 1", "Sınırlı süre!", "Daily Deal 1", "Limited time!", 250, "omnium"),
        fakeItem("daily_2", "Günlük Fırsat 2", "Bugüne özel!",  "Daily Deal 2", "Today only!",   800, "soulium")
    )

    private fun fakeItem(id: String, nameTr: String, descTr: String, nameEn: String, descEn: String, price: Long, currency: String, realPrice: String?=null) =
        MarketItemDto(id=id, nameTr=nameTr, nameEn=nameEn, descTr=descTr, descEn=descEn, category="", price=realPrice?.replace(",",".")?.toDoubleOrNull()?.times(100)?.toLong()?:price, currency=currency, imageUrl=null, isOwned=false, isEquipped=false, isLimited=false, expiresMs=null)
}

@Composable
internal fun Market(onBack: () -> Unit, vm: MarketVM = hiltViewModel()) {
    val s     by vm.state.collectAsState()
    val items  = vm.displayedItems()
    val ctx    = LocalContext.current

    LaunchedEffect(s.successMsg, s.error) {
        if (s.successMsg != null || s.error != null) { kotlinx.coroutines.delay(3000); vm.clearMessage() }
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        CrtOverlay()
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Color.Black.copy(0.6f)).padding(horizontal=8.dp, vertical=4.dp), verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint=Yellow) }
                Text(stringResource(R.string.market_title), color=Yellow, fontSize=16.sp, fontWeight=FontWeight.Bold, letterSpacing=3.sp)
                Spacer(Modifier.weight(1f))
                CurrencyBadge(s.omniumBal,  OmniumCol,  Icons.Default.Diamond)
                Spacer(Modifier.width(8.dp))
                CurrencyBadge(s.souliumBal, SouliumCol, Icons.Default.AutoAwesome)
                if (s.isVip) { Spacer(Modifier.width(8.dp)); VipBadge() }
            }
            ScrollableTabRow(
                selectedTabIndex = s.tab.ordinal,
                containerColor   = MetalBg.copy(0.5f),
                contentColor     = Yellow,
                indicator        = { pos -> Box(Modifier.tabIndicatorOffset(pos[s.tab.ordinal]).height(2.dp).background(Yellow)) },
                edgePadding      = 4.dp
            ) {
                MarketTab.entries.forEach { t ->
                    Tab(
                        selected = s.tab==t, onClick = { vm.setTab(t) },
                        icon     = { Icon(t.icon, null, Modifier.size(15.dp)) },
                        text     = { Text(stringResource(t.labelRes), fontSize=10.sp, letterSpacing=1.sp) },
                        selectedContentColor   = Yellow,
                        unselectedContentColor = TextDim
                    )
                }
            }
            DividerLine()
            if (s.tab == MarketTab.Characters) {
                CharactersTab(apiChars=s.characters, selected=s.selectedChar, equipping=s.equipping, isLoading=s.charsLoading, onSelect=vm::selectChar, onEquip=vm::equipChar, getColor=vm::getClassColor, getIcon=vm::getClassIcon, ctx=ctx)
            } else if (s.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color=Yellow, strokeWidth=2.dp) }
            } else {
                LazyVerticalGrid(
                    columns               = GridCells.Adaptive(150.dp),
                    contentPadding        = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        MarketCard(item=item, isPurchasing=s.purchasing==item.id, onBuy={ vm.requestPurchase(item) })
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
private fun CharactersTab(
    apiChars : List<CharacterDto>,
    selected : CharacterDto?,
    equipping: String?,
    isLoading: Boolean,
    onSelect : (CharacterDto) -> Unit,
    onEquip  : (String) -> Unit,
    getColor : (String) -> Color,
    getIcon  : (String) -> ImageVector,
    ctx      : Context
) {
    val anonId   = remember { buildAnonId(ctx) }
    val anonName = rememberAnonDisplayName()
    val glowRect = CharacterDto(id="glow_rect", nameTr="OMNI-001", nameEn="OMNI-001", clazz="wanderer", maxHp=100f, baseSpeed=3f, stealthMult=1f, staminaMult=1f, abilities=listOf("Temel Koşu","Standart Fener"), isUnlocked=true, isEquipped=false, imageUrl=null, price=0L, currency="omnium")
    val anonChar = CharacterDto(id="anonymous", nameTr=anonName, nameEn=anonName, clazz="ghost", maxHp=150f, baseSpeed=4.5f, stealthMult=2f, staminaMult=1.5f, abilities=listOf("Karanlık Kaynaşma","Ses Maskesi","Hayalet Geçişi"), isUnlocked=false, isEquipped=false, imageUrl=null, price=2500L, currency="soulium")
    val allChars = remember(apiChars) { listOf(glowRect, anonChar) + apiChars.filter { it.id != "glow_rect" && it.id != "anonymous" } }
    val resolvedSelected = selected ?: glowRect

    Column(Modifier.fillMaxSize()) {
        if (isLoading) Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) { CircularProgressIndicator(color=Yellow, strokeWidth=2.dp, modifier=Modifier.size(24.dp)) }
        LazyRow(Modifier.fillMaxWidth().padding(vertical=12.dp), contentPadding=PaddingValues(horizontal=16.dp), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            items(allChars, key={ it.id }) { char ->
                CharacterThumb(
                    name       = if (char.id=="anonymous") anonName else char.nameTr,
                    isSelected = resolvedSelected.id==char.id,
                    isUnlocked = char.isUnlocked,
                    isEquipped = char.isEquipped,
                    isGlowRect = char.id=="glow_rect",
                    isAnon     = char.id=="anonymous",
                    classColor = getColor(char.clazz),
                    onClick    = { onSelect(char) }
                )
            }
        }
        DividerLine()
        val displayChar = allChars.firstOrNull { it.id==resolvedSelected.id } ?: glowRect
        CharacterDetailPanel(char=displayChar, liveName=if (displayChar.id=="anonymous") anonName else displayChar.nameTr, classColor=getColor(displayChar.clazz), classIcon=getIcon(displayChar.clazz), isGlowRect=displayChar.id=="glow_rect", isAnon=displayChar.id=="anonymous", isEquipping=equipping==displayChar.id, onEquip={ onEquip(displayChar.id) })
    }
}

@Composable
private fun CharacterThumb(name: String, isSelected: Boolean, isUnlocked: Boolean, isEquipped: Boolean, isGlowRect: Boolean, isAnon: Boolean, classColor: Color, onClick: () -> Unit) {
    val inf  = rememberInfiniteTransition(label="cth")
    val glow by inf.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(1800, easing=EaseInOut), RepeatMode.Reverse), "g")
    Column(
        horizontalAlignment=Alignment.CenterHorizontally,
        modifier=Modifier.width(80.dp).clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) classColor.copy(0.15f) else MetalBg)
            .border(2.dp, if (isSelected) classColor.copy(glow) else BorderCol, RoundedCornerShape(4.dp))
            .clickable(onClick=onClick).padding(8.dp)
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)).background(DarkBg).drawWithContent { drawContent(); if (!isUnlocked) drawRect(Color.Black.copy(0.65f)) }, Alignment.Center) {
            when {
                isGlowRect -> {
                    val ga by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1200, easing=EaseInOut), RepeatMode.Reverse), "gr")
                    Box(Modifier.size(28.dp, 38.dp).clip(RoundedCornerShape(3.dp)).background(Brush.verticalGradient(listOf(Color(0xFF00BFFF).copy(ga), Color(0xFF1E90FF).copy(ga*0.6f)))).border(1.dp, Color(0xFF87CEFA).copy(ga), RoundedCornerShape(3.dp)))
                }
                isAnon && !isUnlocked -> Icon(Icons.Default.Lock, null, tint=TextDim.copy(0.7f), modifier=Modifier.size(22.dp))
                isUnlocked -> Text(name.take(2).uppercase(), color=classColor, fontSize=16.sp, fontWeight=FontWeight.Black)
                else -> Icon(Icons.Default.Lock, null, tint=TextDim, modifier=Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(name, color=if (isSelected) classColor else TextSec, fontSize=if (isAnon) 11.sp else 10.sp, fontWeight=if (isSelected) FontWeight.Bold else FontWeight.Normal, textAlign=TextAlign.Center, maxLines=1, overflow=TextOverflow.Clip)
        if (isEquipped) {
            Spacer(Modifier.height(2.dp))
            Box(Modifier.clip(RoundedCornerShape(1.dp)).background(SuccessGreen.copy(0.2f)).padding(horizontal=4.dp, vertical=1.dp)) {
                Text(stringResource(R.string.char_active_label), color=SuccessGreen, fontSize=7.sp, fontWeight=FontWeight.Black, letterSpacing=1.sp)
            }
        }
    }
}

@Composable
private fun CharacterDetailPanel(char: CharacterDto, liveName: String, classColor: Color, classIcon: ImageVector, isGlowRect: Boolean, isAnon: Boolean, isEquipping: Boolean, onEquip: () -> Unit) {
    val inf  = rememberInfiniteTransition(label="dp")
    val glow by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1400, easing=EaseInOut), RepeatMode.Reverse), "dg")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp, vertical=16.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(16.dp), verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(6.dp)).background(classColor.copy(0.15f)).border(2.dp, classColor.copy(0.5f), RoundedCornerShape(6.dp)), Alignment.Center) {
                if (isGlowRect) Box(Modifier.size(32.dp, 48.dp).clip(RoundedCornerShape(3.dp)).background(Brush.verticalGradient(listOf(Color(0xFF00BFFF).copy(glow), Color(0xFF1E90FF).copy(glow*0.6f)))).border(1.dp, Color(0xFF87CEFA).copy(glow), RoundedCornerShape(3.dp)))
                else Icon(classIcon, null, tint=classColor, modifier=Modifier.size(36.dp))
            }
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(liveName, color=if (isAnon) classColor.copy(glow) else Yellow, fontSize=if (isAnon) 18.sp else 22.sp, fontWeight=FontWeight.Black, letterSpacing=if (isAnon) 2.sp else 0.sp)
                    Box(Modifier.clip(RoundedCornerShape(2.dp)).background(classColor.copy(0.2f)).padding(horizontal=8.dp, vertical=3.dp)) {
                        Text(char.clazz.uppercase(), color=classColor, fontSize=9.sp, fontWeight=FontWeight.Black, letterSpacing=2.sp)
                    }
                }
                Text(
                    if (char.isUnlocked) stringResource(R.string.characters_unlocked) else "${stringResource(R.string.char_locked_label)} — ${char.price} ${char.currency.uppercase()}",
                    color=if (char.isUnlocked) SuccessGreen else TextDim, fontSize=12.sp
                )
            }
        }
        DividerLine()
        Text(stringResource(R.string.char_stats_hp), color=TextSec, fontSize=11.sp, letterSpacing=2.sp, fontWeight=FontWeight.Bold)
        CharStatBar(stringResource(R.string.char_stats_hp),      char.maxHp/200f,         "${char.maxHp.toInt()} HP",            DangerRed)
        CharStatBar(stringResource(R.string.char_stats_speed),   char.baseSpeed/6f,       "${char.baseSpeed} m/s",               SuccessGreen)
        CharStatBar(stringResource(R.string.char_stats_stealth), char.stealthMult/2f,     "${(char.stealthMult*100).toInt()}%",  SouliumCol)
        CharStatBar(stringResource(R.string.char_stats_stamina), char.staminaMult/2f,     "${(char.staminaMult*100).toInt()}%",  CrtAmber)
        DividerLine()
        if (char.abilities.isNotEmpty()) {
            Text("Yetenekler", color=TextSec, fontSize=11.sp, letterSpacing=2.sp, fontWeight=FontWeight.Bold)
            char.abilities.forEach { ability ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(MetalBg).border(1.dp, classColor.copy(0.3f), RoundedCornerShape(2.dp)).padding(horizontal=12.dp, vertical=8.dp), horizontalArrangement=Arrangement.spacedBy(10.dp), verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint=classColor, modifier=Modifier.size(14.dp))
                    Text(ability, color=TextSec, fontSize=13.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (char.isUnlocked) {
            if (char.isEquipped) {
                Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint=SuccessGreen, modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.market_equipped), color=SuccessGreen, fontSize=13.sp)
                }
            } else {
                OmniButton(if (isEquipping) "Takılıyor…" else stringResource(R.string.char_select_label), onEquip, enabled=!isEquipping, width=260.dp, height=50.dp, accent=classColor)
            }
        } else {
            OmniButton("${stringResource(R.string.char_unlock_prefix)}${char.price} ${char.currency.uppercase()}", {}, width=260.dp, height=50.dp, accent=CrtAmber)
        }
    }
}

@Composable
private fun CharStatBar(label: String, value: Float, display: String, color: Color) {
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color=TextSec, fontSize=11.sp)
            Text(display, color=color, fontSize=11.sp, fontWeight=FontWeight.Bold)
        }
        LinearProgressIndicator(progress={ value.coerceIn(0f,1f) }, modifier=Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color=color, trackColor=MetalBg)
    }
}

@Composable
private fun MarketCard(item: MarketItemDto, isPurchasing: Boolean, onBuy: () -> Unit) {
    val currencyColor = when (item.currency.lowercase()) { "omnium"->OmniumCol; "soulium"->SouliumCol; "tl"->SuccessGreen; else->CrtAmber }
    val inf  = rememberInfiniteTransition(label="card")
    val glow by inf.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(2000, easing=EaseInOut), RepeatMode.Reverse), "g")
    Column(
        horizontalAlignment=Alignment.CenterHorizontally,
        modifier=Modifier.clip(RoundedCornerShape(3.dp)).background(MetalBg).border(1.dp, if (item.isLimited) CrtAmber.copy(glow) else BorderCol, RoundedCornerShape(3.dp)).padding(12.dp)
    ) {
        if (item.isLimited) {
            Box(Modifier.fillMaxWidth().padding(bottom=4.dp), Alignment.TopEnd) {
                Box(Modifier.clip(RoundedCornerShape(2.dp)).background(CrtAmber.copy(0.2f)).padding(horizontal=6.dp, vertical=2.dp)) {
                    Text(stringResource(R.string.market_limited), color=CrtAmber, fontSize=8.sp, fontWeight=FontWeight.Black, letterSpacing=1.sp)
                }
            }
        }
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(currencyColor.copy(0.12f)), Alignment.Center) {
            Icon(Icons.Default.Category, null, tint=currencyColor.copy(0.9f), modifier=Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(item.nameTr, color=Yellow, fontSize=12.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp, textAlign=TextAlign.Center, maxLines=2, overflow=TextOverflow.Ellipsis)
        Text(item.descTr, color=TextDim, fontSize=9.sp, textAlign=TextAlign.Center, maxLines=2, overflow=TextOverflow.Ellipsis, lineHeight=13.sp, modifier=Modifier.padding(top=3.dp))
        Spacer(Modifier.height(10.dp))
        if (item.isOwned) {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint=SuccessGreen, modifier=Modifier.size(14.dp))
                Text(stringResource(R.string.market_equipped), color=SuccessGreen, fontSize=10.sp, fontWeight=FontWeight.Bold)
            }
        } else if (isPurchasing) {
            CircularProgressIndicator(color=Yellow, strokeWidth=2.dp, modifier=Modifier.size(24.dp))
        } else {
            Box(contentAlignment=Alignment.Center, modifier=Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(2.dp)).background(currencyColor.copy(0.15f)).border(1.dp, currencyColor.copy(0.5f), RoundedCornerShape(2.dp)).clickable(onClick=onBuy)) {
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
            Modifier.align(Alignment.Center).fillMaxWidth(0.8f).clip(RoundedCornerShape(4.dp)).background(MetalBg).border(1.dp, Yellow.copy(0.4f), RoundedCornerShape(4.dp)).clickable {}.padding(20.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp), horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.market_confirm_title), color=Yellow, fontSize=18.sp, fontWeight=FontWeight.Black)
            DividerLine()
            Text(item.nameTr, color=Yellow,   fontSize=15.sp, fontWeight=FontWeight.Bold)
            Text(item.descTr, color=TextSec,  fontSize=12.sp, textAlign=TextAlign.Center)
            DividerLine()
            Text("${item.price} ${item.currency.uppercase()}", color=OmniumCol, fontSize=20.sp, fontWeight=FontWeight.Black)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OmniButton(stringResource(R.string.common_cancel), onCancel,  width=120.dp, height=44.dp, accent=TextDim)
                OmniButton(stringResource(R.string.market_buy),    onConfirm, width=120.dp, height=44.dp)
            }
        }
    }
}

@Composable
private fun CurrencyBadge(amount: Long, color: Color, icon: ImageVector) {
    Row(Modifier.clip(RoundedCornerShape(2.dp)).background(MetalBg).padding(horizontal=8.dp, vertical=4.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint=color, modifier=Modifier.size(12.dp))
        Text(amount.toString(), color=color, fontSize=12.sp, fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun VipBadge() {
    Box(Modifier.clip(RoundedCornerShape(2.dp)).background(Color(0xFFFFD700).copy(0.2f)).border(1.dp, Color(0xFFFFD700).copy(0.6f), RoundedCornerShape(2.dp)).padding(horizontal=8.dp, vertical=4.dp)) {
        Text("VIP", color=Color(0xFFFFD700), fontSize=11.sp, fontWeight=FontWeight.Black, letterSpacing=2.sp)
    }
}
