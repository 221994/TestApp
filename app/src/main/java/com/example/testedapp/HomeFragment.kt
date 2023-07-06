package com.example.testedapp


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var locationSettingsResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: WeatherAdapter
    private val countOfDaysWeatherRequest = 5 * 8
    private val locationSettingsRequestCode = 1001
    private lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.linearMain.visibility = View.GONE
        initializeViewModel()
        initializeViews()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        requestPermissionLauncher = registerForPermissionResult()
        locationSettingsResultLauncher = registerForLocationSettingsResult()
        setupRecyclerView()
        setupListeners()
    }

    private fun initializeViewModel() {
        weatherViewModel = ViewModelProvider(requireActivity())[WeatherViewModel::class.java]
        weatherViewModel.weatherObserve.observe(viewLifecycleOwner) { forecasts ->
            adapter.list = forecasts
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
    }

    private fun setupListeners() {
        binding.searchMain.setOnClickListener {
            getTheDataFromViewModelOfSearchByCity()
            closeKeyboardLayoutAfterTheUserPressOnSearchIcon()
        }
        binding.ivGetCurrentLocation.setOnClickListener {
            getPermission()
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

    private fun registerForPermissionResult(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true && permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                getPermission()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun registerForLocationSettingsResult(): ActivityResultLauncher<IntentSenderRequest> {
        return registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // The user has enabled the location settings. Get the current location.
                getCurrentLocation()
            } else {
                // The user has not enabled the location settings.
                Toast.makeText(
                    requireContext(),
                    "Location settings are inadequate, please enable location manually",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        android.app.AlertDialog.Builder(context).setTitle("Rational dialog")
            .setMessage("You must allow the Location permission.").setPositiveButton(
                "Allow"
            ) { _, _ -> openAppSettings() }.setNegativeButton("Cancel", null).create().show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        })
    }

    private fun getPermission() {
        if (hasLocationPermissions()) {
            // Permissions are already granted, proceed to get current location
            checkLocationSettingsAndGetCurrentLocation()
            getCurrentLocation()
        } else {
            requestLocationPermissions()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val fineLocationPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationPermissionGranted && coarseLocationPermissionGranted
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Location permission not granted, handle the case accordingly
            return
        }
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.let { result ->
                    val location = result.lastLocation
                    // Handle the obtained location here
                    val latitude = location.latitude
                    val longitude = location.longitude
                    getTheDataFromViewModelOfCurrentArea(latitude, longitude)
                    // Remember to remove the location updates when no longer needed
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(
            createLocationRequest(), locationCallback, Looper.getMainLooper()
        )
    }

    private fun requestLocationPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun checkLocationSettingsAndGetCurrentLocation() {
        val locationRequest = createLocationRequest()
        val builder = createLocationSettingsRequestBuilder(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(requireContext())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnCompleteListener {
            try {
                task.getResult(ApiException::class.java)
                // All location settings are satisfied. Get the current location.
                getCurrentLocation()
            } catch (exception: ApiException) {
                handleLocationSettingsException(exception)
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2000
        }
    }

    private fun createLocationSettingsRequestBuilder(locationRequest: LocationRequest): LocationSettingsRequest.Builder {
        return LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    }

    private fun handleLocationSettingsException(exception: ApiException) {
        when (exception.statusCode) {
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                val resolvable = exception as? ResolvableApiException
                resolvable?.startResolutionForResult(requireActivity(), locationSettingsRequestCode)
            }

            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                showToast("Location settings is unavailable, please enable location manually")
            }

            else -> {
                // Handle unknown or unexpected status codes
                showToast("Unknown status code for location settings: ${exception.statusCode}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val API_KEY = "39a2a75e30e53ba6b6d54f6d8ccd9385"
    }
}