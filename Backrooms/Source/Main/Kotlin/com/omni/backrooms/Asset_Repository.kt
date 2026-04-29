package com.omni.backrooms

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Asset_Repository @Inject constructor() {

    private val steps = listOf(
        "textures"  to 0.20f,
        "audio"     to 0.40f,
        "shaders"   to 0.60f,
        "entities"  to 0.78f,
        "network"   to 0.90f,
        "finalize"  to 1.00f
    )

    fun preload(): Flow<Float> = flow {
        var progress = 0f
        emit(progress)
        for ((_, target) in steps) {
            val delta = (target - progress) / 12
            repeat(12) {
                progress += delta
                emit(progress.coerceIn(0f, 1f))
                delay(55L)
            }
            progress = target
        }
        emit(1f)
    }
}

    data class PreloadStep(val tag: String, val weight: Float)

    private val detailedSteps = listOf(
        PreloadStep("textures_diffuse",  0.12f),
        PreloadStep("textures_normal",   0.08f),
        PreloadStep("audio_hum",         0.10f),
        PreloadStep("audio_ambience",    0.08f),
        PreloadStep("shaders_vhs",       0.10f),
        PreloadStep("shaders_lighting",  0.10f),
        PreloadStep("entities_mesh",     0.10f),
        PreloadStep("entities_anim",     0.08f),
        PreloadStep("network_init",      0.12f),
        PreloadStep("level_gen",         0.08f),
        PreloadStep("finalize",          0.04f)
    )

    fun preloadDetailed(): Flow<Pair<Float, String>> = flow {
        var accumulated = 0f
        for (step in detailedSteps) {
            val substeps = 8
            repeat(substeps) { i ->
                accumulated += step.weight / substeps
                emit(accumulated.coerceIn(0f, 1f) to step.tag)
                delay(45L)
            }
        }
        emit(1f to "done")
    }
