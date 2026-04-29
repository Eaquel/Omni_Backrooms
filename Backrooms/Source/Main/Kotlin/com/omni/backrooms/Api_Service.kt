package com.omni.backrooms

import retrofit2.http.*

interface Api_Service {

    @GET("rooms")
    suspend fun getRooms(
        @Query("q")        query    : String?,
        @Query("locked")   locked   : Boolean?,
        @Query("lang")     language : String?,
        @Query("page")     page     : Int,
        @Query("pageSize") pageSize : Int
    ): RoomPage

    @POST("rooms")
    suspend fun createRoom(
        @Query("name")       name       : String,
        @Query("maxPlayers") maxPlayers : Int,
        @Query("difficulty") difficulty : String,
        @Query("password")   password   : String?
    ): String

    @POST("rooms/{id}/join")
    suspend fun joinRoom(
        @Path("id")        roomId   : String,
        @Query("password") password : String?
    ): Boolean

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") roomId: String): Boolean

    @GET("player/profile")
    suspend fun getProfile(): PlayerProfile

    @PUT("player/profile")
    suspend fun updateProfile(@Body profile: PlayerProfile): PlayerProfile
}

import retrofit2.http.*

interface Api_Service {

    @GET("rooms")
    suspend fun getRooms(
        @Query("q")        query    : String?,
        @Query("locked")   locked   : Boolean?,
        @Query("lang")     language : String?,
        @Query("page")     page     : Int,
        @Query("pageSize") pageSize : Int
    ): RoomPage

    @POST("rooms")
    suspend fun createRoom(
        @Query("name")       name       : String,
        @Query("maxPlayers") maxPlayers : Int,
        @Query("difficulty") difficulty : String,
        @Query("password")   password   : String?
    ): String

    @POST("rooms/{id}/join")
    suspend fun joinRoom(@Path("id") roomId: String, @Query("password") password: String?): Boolean

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") roomId: String): Boolean

    @GET("player/profile")
    suspend fun getProfile(): PlayerProfile

    @PUT("player/profile")
    suspend fun updateProfile(@Body profile: PlayerProfile): PlayerProfile

    @GET("leaderboard")
    suspend fun getLeaderboard(
        @Query("page")     page    : Int = 0,
        @Query("pageSize") size    : Int = 50,
        @Query("difficulty") diff  : String? = null
    ): List<LeaderboardEntry>

    @POST("player/score")
    suspend fun submitScore(
        @Query("level")     level     : Int,
        @Query("score")     score     : Long,
        @Query("survived")  survived  : Int,
        @Query("difficulty") difficulty: String
    ): Boolean

    @GET("events/active")
    suspend fun getActiveEvents(): List<Map<String, String>>

    @POST("auth/google")
    suspend fun loginWithGoogle(@Query("idToken") idToken: String): Map<String, String>

    @POST("auth/refresh")
    suspend fun refreshToken(@Query("refreshToken") token: String): Map<String, String>
}
