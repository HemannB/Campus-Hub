package br.com.uri.campushub.student

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.databinding.ActivityEventMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale
import java.util.concurrent.Executors

class EventMapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_ADDRESS = "event_address"
        private const val MAP_FRAGMENT_TAG = "event_map_fragment"
    }

    private lateinit var binding: ActivityEventMapBinding
    private val geocoderExecutor = Executors.newSingleThreadExecutor()
    private var eventTitle = ""
    private var eventAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE).orEmpty()
        eventAddress = intent.getStringExtra(EXTRA_EVENT_ADDRESS).orEmpty()

        binding.textMapEventTitle.text = eventTitle.ifBlank { "Local do evento" }
        binding.textMapEventAddress.text = eventAddress.ifBlank { "Endereço não informado" }
        binding.buttonBackFromMap.setOnClickListener { finish() }

        setupMap()
    }

    override fun onMapReady(map: GoogleMap) {
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.uiSettings.isCompassEnabled = true

        if (eventAddress.isBlank()) {
            showMapError("Este evento ainda não possui endereço.")
        } else {
            locateEventAddress(map)
        }
    }

    override fun onDestroy() {
        geocoderExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun setupMap() {
        val existingFragment = supportFragmentManager
            .findFragmentByTag(MAP_FRAGMENT_TAG) as? SupportMapFragment

        val mapFragment = existingFragment ?: SupportMapFragment.newInstance().also { fragment ->
            supportFragmentManager.beginTransaction()
                .replace(R.id.eventMapContainer, fragment, MAP_FRAGMENT_TAG)
                .commit()
        }

        mapFragment.getMapAsync(this)
    }

    @Suppress("DEPRECATION")
    private fun locateEventAddress(map: GoogleMap) {
        if (!Geocoder.isPresent()) {
            showMapError("Este dispositivo não consegue localizar endereços.")
            return
        }

        setLoading(true)
        binding.textMapStatus.text = "Localizando endereço..."

        geocoderExecutor.execute {
            val result = runCatching {
                Geocoder(this, Locale.forLanguageTag("pt-BR"))
                    .getFromLocationName(eventAddress, 1)
                    .orEmpty()
                    .firstOrNull()
            }

            runOnUiThread {
                result.fold(
                    onSuccess = { address ->
                        if (address == null) {
                            showMapError("Endereço não encontrado. Confira os dados do evento.")
                        } else {
                            showEventOnMap(map, address)
                        }
                    },
                    onFailure = {
                        showMapError("Não foi possível consultar este endereço.")
                    }
                )
            }
        }
    }

    private fun showEventOnMap(map: GoogleMap, address: Address) {
        val position = LatLng(address.latitude, address.longitude)
        val resolvedAddress = address.getAddressLine(0) ?: eventAddress

        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(position)
                .title(eventTitle.ifBlank { "Local do evento" })
                .snippet(resolvedAddress)
        )?.showInfoWindow()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))

        setLoading(false)
        binding.textMapStatus.text = "Local encontrado"
        binding.textMapEventAddress.text = resolvedAddress
    }

    private fun showMapError(message: String) {
        setLoading(false)
        binding.textMapStatus.text = message
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressEventMap.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
