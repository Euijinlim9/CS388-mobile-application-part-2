package com.example.cs388_mobile_application_part_2

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
const val apiKey = BuildConfig.API_KEY
object RetrofitClient {
    private const val BASE_URL = "https://boardgamegeek.com/xmlapi2/"
    val service: BggApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).addInterceptor{chain->
            val request = chain.request().newBuilder().addHeader("Authorization", "Bearer $apiKey").build()
            chain.proceed(request)
        }.build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(BggApiService::class.java)
    }
}
