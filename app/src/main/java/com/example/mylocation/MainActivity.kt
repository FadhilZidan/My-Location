package com.example.mylocation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var peta: GoogleMap? = null
    private var lokasi: Location? = null
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val mintaIzin: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                ambilLokasi()
            } else {
                tvStatusTitle.text = getString(R.string.permission_denied_title)
                tvStatusSubtitle.text = getString(R.string.permission_denied_subtitle)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvStatusTitle = findViewById(R.id.tvStatusTitle)
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Status Awal: Layar 1 (Menunggu izin lokasi)
        tvStatusTitle.text = getString(R.string.title_waiting_permission)
        tvStatusSubtitle.text = getString(R.string.subtitle_waiting_permission)

        val fragmentPeta = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        fragmentPeta.getMapAsync(this)

        if (cekIzin()) {
            ambilLokasi()
        } else {
            mintaIzin.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun cekIzin(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onMapReady(googleMap: GoogleMap) {
        peta = googleMap
        lokasi?.let { tampilkanPin(it) }
    }

    private fun ambilLokasi() {
        if (!cekIzin()) return

        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { result ->
                if (result != null) {
                    lokasi = result
                    tampilkanPin(result)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            lokasi = lastLoc
                            tampilkanPin(lastLoc)
                        } else {
                            tvStatusTitle.text = getString(R.string.title_location_my)
                            tvStatusSubtitle.text = "Lokasi belum tersedia..."
                        }
                    }
                }
            }.addOnFailureListener {
                tvStatusTitle.text = getString(R.string.title_location_my)
                tvStatusSubtitle.text = "Gagal mendapatkan lokasi"
            }
        } catch (_: SecurityException) {
            tvStatusTitle.text = getString(R.string.permission_denied_title)
            tvStatusSubtitle.text = getString(R.string.permission_denied_subtitle)
        }
    }

    private fun tampilkanPin(loc: Location) {
        val posisi = LatLng(loc.latitude, loc.longitude)
        peta?.apply {
            clear()
            addMarker(MarkerOptions().position(posisi).title("Lokasi Saya"))
            moveCamera(CameraUpdateFactory.newLatLngZoom(posisi, 16f))
        }

        // Status Setelah Lokasi Didapatkan: Layar 2 (Pin di lokasi saya)
        tvStatusTitle.text = getString(R.string.title_location_my)
        tvStatusSubtitle.text = "Lat ${loc.latitude} Lon ${loc.longitude}\nPin dipasang di posisi perangkat"
    }
}