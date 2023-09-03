package com.example.weathersnap.network

import com.example.weathersnap.data.WeatherModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterFace {
    @GET("forecast/?")
    fun getForecast(
        @Query("q") cityName: String, @Query("cnt") count: Int, @Query("appid") apiKey: String
    ): Call<WeatherModel>

    @GET("forecast/?")
    fun getCurrentLocationApi(
        @Query("lat") Latitude: Double,
        @Query("lon") longitude: Double,
        @Query("cnt") count: Int,
        @Query("appid") apiKey: String
    ): Call<WeatherModel>


}