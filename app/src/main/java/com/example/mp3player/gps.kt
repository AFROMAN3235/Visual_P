package com.example.mp3player

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

class gps : AppCompatActivity() {

    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvTime: TextView
    private lateinit var btnRefresh: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        when {
            fineLocationGranted -> {
                getCurrentLocation()
            }
            coarseLocationGranted -> {
                getCurrentLocation()
            }
            else -> {
                Toast.makeText(this, "Разрешение на местоположение отклонено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps)

        try {
            initViews()
            initLocationClient()
            setupClickListeners()

            if (checkPermissions()) {
                if (isLocationEnabled()) {
                    getCurrentLocation()
                } else {
                    promptEnableLocation()
                }
            } else {
                requestPermissions()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("GpsActivity", "Crash: ${e.stackTraceToString()}")
        }
    }

    private fun initViews() {
        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvAlt = findViewById(R.id.tv_alt)
        tvTime = findViewById(R.id.tv_time)
        btnRefresh = findViewById(R.id.btnRefresh)
    }

    private fun initLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun checkPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun getCurrentLocation() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (!isLocationEnabled()) {
            promptEnableLocation()
            return
        }

        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            updateLocationUI(location)
                        } else {
                            showLocationUnavailable()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("GpsActivity", "Ошибка получения местоположения", e)
                        showLocationError()
                    }
            }
        } catch (e: SecurityException) {
            Log.e("GpsActivity", "Ошибка безопасности: ${e.message}")
        }
    }

    private fun updateLocationUI(location: Location) {
        runOnUiThread {
            val lat = String.format(Locale.US, "%.6f", location.latitude)
            val lon = String.format(Locale.US, "%.6f", location.longitude)
            val alt = String.format(Locale.US, "%.1f", location.altitude)
            val time = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                .format(Date(location.time))

            tvLat.text = "Широта: $lat"
            tvLon.text = "Долгота: $lon"
            tvAlt.text = "Высота: $alt м"
            tvTime.text = "Время: $time"
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun promptEnableLocation() {
        Toast.makeText(
            this,
            "Пожалуйста, включите службы определения местоположения",
            Toast.LENGTH_LONG
        ).show()
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun showLocationUnavailable() {
        Toast.makeText(
            this,
            "Не удалось получить местоположение. Попробуйте позже",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLocationError() {
        Toast.makeText(
            this,
            "Ошибка при получении местоположения",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()
        if (checkPermissions() && isLocationEnabled()) {
            getCurrentLocation()
        }
    }
}