package com.example.testedapp.ui

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiServiceClient {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private val retrofit =
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create())
            .build()
    val apiService: ApiInterFace = retrofit.create(ApiInterFace::class.java)
}