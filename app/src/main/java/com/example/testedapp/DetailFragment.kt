package com.example.testedapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.testedapp.databinding.FragmentDetailBinding
import com.example.testedapp.ui.Utilities
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class DetailFragment : Fragment() {
    lateinit var binding: FragmentDetailBinding
    private val args: DetailFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvDetailTemp.text =
            "${Utilities.kelvinToCelsius(args.ForeCastObjectDetail.main.temp)}°"
        binding.tvDetailHumidity.text = args.ForeCastObjectDetail.main.humidity.toString() + "%"
        binding.tvDetailHighTemp.text =
            "${Utilities.kelvinToCelsius(args.ForeCastObjectDetail.main.temp_max)}°"
        binding.tvDetailLowTemp.text =
            "${Utilities.kelvinToCelsius(args.ForeCastObjectDetail.main.temp_min)}°"
        binding.tvDetailPressure.text = args.ForeCastObjectDetail.main.pressure.toString() + " MB"
        binding.tvDetailWindSpeed.text =
            convertWindSpeedToKilometersPerHour(args.ForeCastObjectDetail.wind.speed).toString() + " KM/H"
        binding.tvDetailVisibility.text =
            convertVisibilityToKilometers(args.ForeCastObjectDetail.visibility).toString() + " KM"
        val windDegree = args.ForeCastObjectDetail.wind.deg
        val windDirection = convertWindDegreeToDirection(windDegree)
        binding.tvDetailWindDegree.text = windDirection
        val dateFromArgs = args.ForeCastObjectDetail.dt_txt
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val date = LocalDateTime.parse(dateFromArgs, formatter).toLocalDate()
        val formattedDate = getFormattedDate(date)
        binding.tvDetailsByNumbers.text = formattedDate
        binding.ivBackFromDetail.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun convertVisibilityToKilometers(visibility: Int): Double {
        val metersInKilometer = 1000
        return visibility.toDouble() / metersInKilometer
    }

    private fun convertWindSpeedToKilometersPerHour(windSpeed: Double): Int {
        val metersPerSecondToKilometersPerHour = 3.6
        return (windSpeed * metersPerSecondToKilometersPerHour).toInt()
    }

    private fun convertWindDegreeToDirection(windDegree: Int): String {
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        val index = (windDegree % 360 / 45)
        return directions[index]
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