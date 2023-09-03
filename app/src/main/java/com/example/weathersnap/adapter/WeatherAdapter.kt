package com.example.weathersnap.adapter

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.weathersnap.databinding.CustomRvBinding
import com.example.weathersnap.data.Forecast
import com.example.weathersnap.util.Utilities
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class WeatherAdapter(
    var list: List<Forecast>, private val listener: OnItemForeCastClickListener
) : RecyclerView.Adapter<WeatherAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = CustomRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentPosition = list[position]
        holder.bindViews(currentPosition, listener)

    }

    inner class MyViewHolder(private val binding: CustomRvBinding) : ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bindViews(forecast: Forecast, action: OnItemForeCastClickListener) {
            val date =
                LocalDate.parse(forecast.dt_txt.substringBefore(" "), DateTimeFormatter.ISO_DATE)
            val formattedDate = getFormattedDate(date)
            binding.tvDayOfTheWeather.text = formattedDate
            binding.tvHumidity.text = "${forecast.main.humidity}%"
            binding.tvTempOfTheDay.text = "${Utilities.kelvinToCelsius(forecast.main.temp)}°"
            binding.tvWeatherDescription.text = forecast.weather[0].description
            if (adapterPosition == 0) {
                setViewOfFirstIndexTextStyleNormal()
            } else {
                binding.tvDayOfTheWeather.setTypeface(null, Typeface.BOLD)
            }
            binding.imageViewArrow.setOnClickListener {
                action.onItemClick(forecast, adapterPosition)
            }
        }

        private fun setViewOfFirstIndexTextStyleNormal() {
            binding.tvDayOfTheWeather.setTypeface(null, Typeface.NORMAL)
            binding.tvHumidity.setTypeface(null, Typeface.NORMAL)
            binding.tvTempOfTheDay.setTypeface(null, Typeface.NORMAL)
            binding.tvWeatherDescription.setTypeface(null, Typeface.NORMAL)
        }


        private fun getFormattedDate(date: LocalDate): String {
            val currentLocalDate = LocalDate.now()
            return when (date) {
                currentLocalDate -> {
                    "Today"
                }

                currentLocalDate.plusDays(1) -> "Tomorrow"
                else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            }
        }
    }
}

interface OnItemForeCastClickListener {
    fun onItemClick(forecast: Forecast, position: Int)
}