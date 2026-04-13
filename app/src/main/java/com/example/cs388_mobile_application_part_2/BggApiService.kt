package com.example.cs388_mobile_application_part_2

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BggApiService {
    @GET("hot?type=boardgame")
    fun getHotGames(): Call<String>

    @GET("search?type=boardgame")
    fun searchGames(@Query("query") query: String): Call<String>

    @GET("thing")
    fun getGameDetails(@Query("id") id: String, @Query("stats") stats: Int = 1): Call<String>
}
