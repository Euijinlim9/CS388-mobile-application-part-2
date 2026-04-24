package com.example.cs388_mobile_application_part_2

import retrofit2.http.GET
import retrofit2.http.Query
interface BggApiService {
    @GET("hot?type=boardgame")
    suspend fun getHotGames(): String

    @GET("search?type=boardgame")
    suspend fun searchGames(@Query("query") query: String): String

    @GET("thing")
    suspend fun getGameDetails(@Query("id") id: String, @Query("stats") stats: Int = 1): String
}
