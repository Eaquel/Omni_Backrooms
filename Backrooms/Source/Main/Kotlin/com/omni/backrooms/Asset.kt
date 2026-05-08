package com.omni.backrooms

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

enum class EntityType(
    val typeId     : Int,
    val baseSpeed  : Float,
    val hearRange  : Float,
    val sightRange : Float,
    val aggroRange : Float
) {
    SMILER      (0, 2.8f, 12f, 18f, 9f),
    HOWLER      (1, 3.5f, 20f, 22f, 12f),
    DULLLER     (2, 1.4f,  8f, 14f, 6f),
    PARTYGOER   (3, 4.2f, 16f, 20f, 10f),
    SKIN_STEALER(4, 3.0f, 14f, 16f, 8f),
    WRETCHED    (5, 5.5f, 10f, 12f, 7f),
    FACELING    (6, 2.2f, 18f, 24f, 14f),
    DEATHMOTHS  (7, 6.0f,  6f,  8f, 5f)
}

data class SpawnConfig(
    val count          : Int,
    val speedMult      : Float,
    val sightMult      : Float,
    val spawnIntervalMs: Long
)

data class PreloadEvent(
    val progress: Float,
    val stage   : String
)

@Serializable
data class StoryChapterRaw(
    val id            : Int,
    @SerialName("title_tr")      val titleTr      : String,
    @SerialName("title_en")      val titleEn      : String,
    val unlocked      : Boolean,
    @SerialName("paragraphs_tr") val paragraphsTr : List<String>,
    @SerialName("paragraphs_en") val paragraphsEn : List<String>
)

@Serializable
data class StoryJson(
    val version  : Int,
    val chapters : List<StoryChapterRaw>
)

@Singleton
class AssetManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private var storyCache: StoryJson? = null

    val defaultCharacters: List<CharacterDef> = listOf(
        CharacterDef("wanderer",    "Wanderer",     CharClass.WANDERER,  100f, 3.0f, 1.0f, 1.0f, listOf("Hayatta Kalma İçgüdüsü", "Çevre Adaptasyonu"), true,  true),
        CharacterDef("scout",       "Scout",        CharClass.SCOUT,      80f, 4.5f, 1.6f, 1.2f, listOf("Hızlı Koşu", "Sessiz Adım", "Erken Uyarı"),     false, false),
        CharacterDef("survivor",    "Survivor",     CharClass.SURVIVOR,  150f, 2.5f, 0.8f, 0.9f, listOf("Ağır Zırh", "Son Nefes", "HP Rejenerasyonu"),   false, false),
        CharacterDef("engineer",    "Engineer",     CharClass.ENGINEER,   90f, 3.2f, 1.0f, 1.1f, listOf("Tuzak Kurma", "Işık Tamiri", "Pil Uzatma"),      false, false),
        CharacterDef("ghost",       "Ghost",        CharClass.GHOST,      70f, 3.8f, 1.9f, 0.8f, listOf("Geçici Görünmezlik", "Yankısız Hareket"),        false, false)
    )

    fun getSpawnConfig(difficulty: String): SpawnConfig = when (difficulty.lowercase()) {
        "easy"   -> SpawnConfig(count=3,  speedMult=0.7f, sightMult=0.8f, spawnIntervalMs=40_000)
        "hard"   -> SpawnConfig(count=8,  speedMult=1.4f, sightMult=1.3f, spawnIntervalMs=12_000)
        else     -> SpawnConfig(count=5,  speedMult=1.0f, sightMult=1.0f, spawnIntervalMs=22_000)
    }

    fun loadStory(): StoryJson {
        storyCache?.let { return it }
        return try {
            val raw = ctx.assets.open("story.json").bufferedReader().readText()
            json.decodeFromString<StoryJson>(raw).also { storyCache = it }
        } catch (e: Exception) {
            Log.e("AssetManager", "story.json okunamadı: ${e.message}")
            StoryJson(version = 1, chapters = emptyList())
        }
    }

    fun storyChapterToDto(raw: StoryChapterRaw): StoryChapterDto = StoryChapterDto(
        id         = raw.id,
        titleTr    = raw.titleTr,
        titleEn    = raw.titleEn,
        contentTr  = raw.paragraphsTr.joinToString("\n\n"),
        contentEn  = raw.paragraphsEn.joinToString("\n\n"),
        isUnlocked = raw.unlocked
    )

    fun preload(): Flow<PreloadEvent> = flow {
        val stages = listOf(
            0.10f to "loading_stage_shaders",
            0.30f to "loading_stage_assets",
            0.55f to "loading_stage_entities",
            0.75f to "loading_stage_audio",
            0.90f to "loading_stage_network",
            1.00f to "loading_stage_done"
        )
        for ((progress, stageKey) in stages) {
            if (stageKey == "loading_stage_assets") {
                runCatching { loadStory() }
            }
            kotlinx.coroutines.delay(380)
            val resId = ctx.resources.getIdentifier(stageKey, "string", ctx.packageName)
            val label = if (resId != 0) ctx.getString(resId) else stageKey
            emit(PreloadEvent(progress = progress, stage = label))
        }
    }.flowOn(Dispatchers.IO)
}

data class CharacterDef(
    val id          : String,
    val name        : String,
    val clazz       : CharClass,
    val maxHp       : Float,
    val baseSpeed   : Float,
    val stealthMult : Float,
    val staminaMult : Float,
    val abilities   : List<String>,
    val isUnlocked  : Boolean,
    val isEquipped  : Boolean
)

enum class CharClass { WANDERER, SCOUT, SURVIVOR, ENGINEER, GHOST }
