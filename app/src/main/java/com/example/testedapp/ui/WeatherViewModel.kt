package com.example.testedapp.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testedapp.model.Forecast
import com.example.testedapp.model.WeatherModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val _weather = MutableLiveData<List<Forecast>>()
    val weatherList: LiveData<List<Forecast>> get() = _weather
    private val _cityName = MutableLiveData<String>()
    val cityName: LiveData<String> get() = _cityName

    fun getWeatherOfNextSevenDays(cityName: String, countOfDays: Int, apiKey: String) {
        ApiServiceClient.apiService.getForecast(cityName, countOfDays, apiKey)
            .enqueue(object : Callback<WeatherModel> {
                override fun onResponse(
                    call: Call<WeatherModel>, response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful && response.body()?.city?.name != null) {
                        val weatherResponse = response.body()
                        val theWholeListFromTheWeatherResponse =
                            weatherResponse?.list ?: emptyList()
                        val filterSevenDaysOnlyFromTheList =
                            filterForecastList(theWholeListFromTheWeatherResponse)
                        _weather.value = filterSevenDaysOnlyFromTheList
                        _cityName.value = response.body()?.city?.name
                        Log.d("TAG", "Response Success")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.d("TAG", "Response Failed: $errorBody")

                    }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    Log.d("TAG", t.message.toString())
                }
            })
    }

    fun getWeatherOfTheCurrentLocation(
        latitude: Double, longitude: Double, count: Int, apiKey: String
    ) {
        ApiServiceClient.apiService.getCurrentLocationApi(latitude, longitude, count, apiKey)
            .enqueue(object : Callback<WeatherModel> {
                override fun onResponse(
                    call: Call<WeatherModel>, response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {
                        val weatherResponse = response.body()
                        val theWholeListFromTheWeatherResponse =
                            weatherResponse?.list ?: emptyList()
                        val filterSevenDaysOnlyFromTheList =
                            filterForecastList(theWholeListFromTheWeatherResponse)
                        _weather.value = filterSevenDaysOnlyFromTheList
                        _cityName.value = response.body()?.city?.name
                        Log.d("TAG", "Response Success")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.d("TAG", "Response Failed: $errorBody")
                    }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    Log.d("TAG", t.message.toString())
                }

            })

    }

    private fun filterForecastList(forecastList: List<Forecast>): List<Forecast> {
        val filteredList = mutableListOf<Forecast>()
        val daysSet = mutableSetOf<String>()
        for (forecast in forecastList) {
            val day = forecast.dt_txt.substringBefore(" ").trim()
            if (daysSet.size < 7) {
                if (!daysSet.contains(day)) {
                    daysSet.add(day)
                    filteredList.add(forecast)
                }
            } else {
                break
            }
        }
        return filteredList
    }


}






