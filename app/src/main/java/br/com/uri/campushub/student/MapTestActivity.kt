package br.com.uri.campushub.student

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.databinding.ActivityMapTestBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale
import java.util.concurrent.Executors

class MapTestActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val MAP_FRAGMENT_TAG = "map_test_fragment"
    }

    private lateinit var binding: ActivityMapTestBinding
    private val geocoderExecutor = Executors.newSingleThreadExecutor()
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "Teste do Google Maps"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupMap()
        setupListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        binding.buttonLocateAddress.isEnabled = true
        binding.textMapStatus.text = "Mapa pronto. Localize o endereço."
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
                .replace(R.id.mapContainer, fragment, MAP_FRAGMENT_TAG)
                .commit()
        }

        mapFragment.getMapAsync(this)
    }

    private fun setupListeners() {
        binding.buttonLocateAddress.setOnClickListener {
            val address = binding.inputMapAddress.text?.toString()?.trim().orEmpty()

            if (address.isBlank()) {
                binding.textMapStatus.text = "Digite um endereço para continuar."
            } else {
                locateAddress(address)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun locateAddress(addressText: String) {
        if (!Geocoder.isPresent()) {
            showMapError("Este dispositivo não possui serviço de localização por endereço.")
            return
        }

        setLoading(true)
        binding.textMapStatus.text = "Procurando endereço..."

        geocoderExecutor.execute {
            val result = runCatching {
                Geocoder(this, Locale.forLanguageTag("pt-BR"))
                    .getFromLocationName(addressText, 1)
                    .orEmpty()
                    .firstOrNull()
            }

            runOnUiThread {
                result.fold(
                    onSuccess = { address ->
                        if (address == null) {
                            showMapError("Endereço não encontrado. Tente informar mais detalhes.")
                        } else {
                            showAddressOnMap(address, addressText)
                        }
                    },
                    onFailure = {
                        showMapError("Não foi possível consultar o endereço.")
                    }
                )
            }
        }
    }

    private fun showAddressOnMap(address: Address, searchedAddress: String) {
        val map = googleMap

        if (map == null) {
            showMapError("O mapa ainda não está pronto.")
            return
        }

        val position = LatLng(address.latitude, address.longitude)
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(position)
                .title("Local encontrado")
                .snippet(address.getAddressLine(0) ?: searchedAddress)
        )?.showInfoWindow()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))

        setLoading(false)
        binding.textMapStatus.text = address.getAddressLine(0) ?: searchedAddress
    }

    private fun showMapError(message: String) {
        setLoading(false)
        binding.textMapStatus.text = message
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressMap.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLocateAddress.isEnabled = !isLoading && googleMap != null
    }
}
