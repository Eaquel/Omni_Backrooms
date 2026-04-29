package com.omni.backrooms

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Room_Repository @Inject constructor(
    private val api: Api_Service
) {

    suspend fun fetchRooms(
        query      : String,
        filterLocked: Boolean?,
        language   : String?,
        page       : Int,
        pageSize   : Int
    ): RoomPage = api.getRooms(
        query    = query.ifBlank { null },
        locked   = filterLocked,
        language = language,
        page     = page,
        pageSize = pageSize
    )

    suspend fun createRoom(
        name      : String,
        maxPlayers: Int,
        difficulty: String,
        password  : String?
    ): String = api.createRoom(
        name       = name,
        maxPlayers = maxPlayers,
        difficulty = difficulty,
        password   = password
    )

    suspend fun joinRoom(roomId: String, password: String?): Boolean =
        api.joinRoom(roomId = roomId, password = password)
}

    suspend fun fetchLeaderboard(page: Int, difficulty: String?): List<LeaderboardEntry> =
        runCatching { api.getLeaderboard(page = page, diff = difficulty) }
            .getOrElse { emptyList() }

    suspend fun submitScore(level: Int, score: Long, survived: Int, difficulty: String): Boolean =
        runCatching { api.submitScore(level, score, survived, difficulty) }
            .getOrElse { false }

    suspend fun fetchActiveEvents(): List<Map<String, String>> =
        runCatching { api.getActiveEvents() }.getOrElse { emptyList() }
