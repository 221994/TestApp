package com.example.testedapp


import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.testedapp.databinding.FragmentHomeBinding
import com.example.testedapp.model.Forecast
import com.example.testedapp.ui.OnItemForeCastClickListener
import com.example.testedapp.ui.Utilities
import com.example.testedapp.ui.WeatherAdapter
import com.example.testedapp.ui.WeatherViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task


class HomeFragment : Fragment(), OnItemForeCastClickListener {
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var adapter: WeatherAdapter
    private val countOfDaysWeatherRequest = 5 * 8
    private lateinit var binding: FragmentHomeBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        setupListeners()
        setupRequestPermissionLauncher()
        setupLocationSettingsLauncher()
        setupLocationServices()
        setupRecyclerView()
        initializeViewModel()

    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.requireContext())
        createLocationRequest()
    }

    private fun setupLocationSettingsLauncher() {
        locationSettingsLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    startLocationUpdates()
                } else {
                    showLocationSettingsDialog()
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.locations.forEach { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    getTheDataFromViewModelOfCurrentArea(latitude, longitude)
                }
                // Stop location updates after receiving the first location
                stopLocationUpdates()
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun setupRequestPermissionLauncher() {
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    checkLocationSettings()
                } else {
                    showPermissionDeniedDialog()
                }
            }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Permission Denied")
            .setMessage("Location permission is required to access your current location.")
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.setCancelable(false).show()
    }

    private fun initializeViewModel() {
        weatherViewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        weatherViewModel.weatherList.observe(viewLifecycleOwner) { forecasts ->
            adapter.list = forecasts
            adapter.notifyDataSetChanged()
            if (forecasts.isNotEmpty()) {
                setValuesToFirstIndexWhichMeansTheCurrentDay(forecasts[0])
            }
        }
        weatherViewModel.cityName.observe(viewLifecycleOwner) { cityName ->
            binding.tvCityName.text = cityName
        }
    }

    private fun initializeViews() {
        binding.linearMain.visibility = View.GONE
        binding.rvMain.layoutManager = LinearLayoutManager(requireContext())
        adapter = WeatherAdapter(emptyList(), this)
        binding.rvMain.adapter = adapter
        binding.etMain.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getTheDataFromViewModelOfSearchByCity()
                closeKeyboardLayoutAfterTheUserPressOnSearchIcon()
            }
            true
        }
    }

    private fun setupRecyclerView() {
        binding.rvMain.layoutManager = LinearLayoutManager(requireContext())
        adapter = WeatherAdapter(emptyList(), this)
        binding.rvMain.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun setupListeners() {
        binding.searchMain.setOnClickListener {
            getTheDataFromViewModelOfSearchByCity()
            closeKeyboardLayoutAfterTheUserPressOnSearchIcon()
        }
        binding.ivGetCurrentLocation.setOnClickListener {
            requestLocationPermission()
        }
    }

    private fun getTheDataFromViewModelOfSearchByCity() {
        val cityName = binding.etMain.text.toString()
        weatherViewModel.getWeatherOfNextSevenDays(cityName, countOfDaysWeatherRequest, API_KEY)
    }

    private fun getTheDataFromViewModelOfCurrentArea(latitude: Double, longitude: Double) {
        weatherViewModel.getWeatherOfTheCurrentLocation(
            latitude, longitude, countOfDaysWeatherRequest, API_KEY
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setValuesToFirstIndexWhichMeansTheCurrentDay(forecast: Forecast) {
        binding.tvTemperature.text = "${Utilities.kelvinToCelsius(forecast.main.temp)}°"
        binding.tvFeelsLike.text = "${Utilities.kelvinToCelsius(forecast.main.feels_like)}°"
        binding.tvHighTemp.text = "${Utilities.kelvinToCelsius(forecast.main.temp_max)}°"
        binding.tvLowTemp.text = "${Utilities.kelvinToCelsius(forecast.main.temp_min)}°"
        binding.linearMain.visibility = View.VISIBLE

    }

    override fun onItemClick(forecast: Forecast, position: Int) {
        val action = HomeFragmentDirections.actionHomeFragmentToDetailFragment(forecast)
        findNavController().navigate(action)
    }

    private fun closeKeyboardLayoutAfterTheUserPressOnSearchIcon() {
        val inputManager =
            requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        inputManager?.hideSoftInputFromWindow(view?.windowToken, 0)
    }


    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }


    private fun requestLocationPermission() {
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        if (isLocationPermissionGranted()) {
            checkLocationSettings()
        } else {
            if (shouldExplainLocationPermissionRationale()) {
                showLocationPermissionRationaleDialog()
            } else {
                requestPermissionLauncher.launch(locationPermission)
            }
        }
    }

    private fun shouldExplainLocationPermissionRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showLocationPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Location Permission")
            .setMessage("You must allow location permission to access your current location.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.setCancelable(false).show()
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnCompleteListener {
            try {
                task.getResult(ApiException::class.java)
                startLocationUpdates()
            } catch (e: ApiException) {
                handleLocationSettingsException(e)
            }
        }
    }

    private fun handleLocationSettingsException(exception: ApiException) {
        if (exception.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
            handleResolutionRequired(exception)
        } else {
            Log.e(TAG, "Error checking location settings", exception)
        }
    }

    private fun handleResolutionRequired(exception: ApiException) {
        try {
            val resolvable = exception as ResolvableApiException
            locationSettingsLauncher.launch(
                IntentSenderRequest.Builder(resolvable.resolution).build()
            )
        } catch (sendEx: IntentSender.SendIntentException) {
            Log.e(TAG, "Error opening settings activity", sendEx)
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10 * 1000
            fastestInterval = 5 * 1000
        }
    }

    private fun showLocationSettingsDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Location Settings")
            .setMessage("Location settings are disabled. Enable them now?")
            .setPositiveButton("OK") { _, _ ->
                openLocationSettings()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.setCancelable(false).show()
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }


    companion object {
        const val API_KEY = "39a2a75e30e53ba6b6d54f6d8ccd9385"
    }
}