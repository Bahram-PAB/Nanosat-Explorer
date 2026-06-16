package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Satellite
import com.example.data.SatelliteRepository
import com.example.data.missionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SatelliteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SatelliteRepository
    
    // UI input states
    val searchQuery = MutableStateFlow("")
    val selectedUnitSize = MutableStateFlow<String?>(null)
    val selectedWeightRange = MutableStateFlow<String?>(null) // "Micro", "Light", "Medium", "Heavy"
    val selectedStatus = MutableStateFlow<String?>(null)     // "Orbiting", "De-orbited", "Decayed", "Launch Failure"
    val selectedCountry = MutableStateFlow<String?>(null)
    val selectedMissionType = MutableStateFlow<String?>(null)
    val showOnlyFavorites = MutableStateFlow(false)

    // Current selected satellite detail
    private val _selectedSatellite = MutableStateFlow<Satellite?>(null)
    val selectedSatellite: StateFlow<Satellite?> = _selectedSatellite.asStateFlow()

    // Combined filtered list
    val uiState: StateFlow<List<Satellite>>

    // Total counts in SQLite catalog
    val totalSatelliteCount: StateFlow<Int>

    // Live list of all countries for filter dropdown
    val availableCountries: StateFlow<List<String>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SatelliteRepository(database.satelliteDao())

        // Ensure database template is populated from JSON asset
        viewModelScope.launch {
            repository.ensurePopulated(application)
        }

        // Group filters in type-safe bundles to support combine flow overloads cleanly
        val filterGroup1 = combine(
            searchQuery,
            selectedUnitSize,
            selectedWeightRange
        ) { query, unit, weight ->
            Triple(query, unit, weight)
        }

        val filterGroup2 = combine(
            selectedStatus,
            selectedCountry,
            showOnlyFavorites
        ) { status, country, favorites ->
            Triple(status, country, favorites)
        }

        // Live filtering combining all inputs
        uiState = combine(
            repository.allSatellites,
            filterGroup1,
            filterGroup2,
            selectedMissionType
        ) { list, g1, g2, missionType ->
            val (query, unit, weight) = g1
            val (status, country, favorites) = g2
            list.filter { sat ->
                // 1. Search Query (Name, Country, Agency)
                val matchesQuery = query.isEmpty() || sat.name.contains(query, ignoreCase = true) ||
                        sat.launchCountry.contains(query, ignoreCase = true) ||
                        sat.launchAgency.contains(query, ignoreCase = true)

                // 2. Unit Size Filter
                val matchesUnit = unit == null || sat.unitSize == unit

                // 3. Weight Range Filter
                val matchesWeight = when (weight) {
                    "Micro (< 1.5kg)" -> sat.weightKg < 1.5
                    "Light (1.5 - 5kg)" -> sat.weightKg in 1.5..5.0
                    "Medium (5 - 20kg)" -> sat.weightKg in 5.0..20.0
                    "Heavy (> 20kg)" -> sat.weightKg > 20.0
                    else -> true
                }

                // 4. Status Filter
                val matchesStatus = status == null || sat.status == status

                // 5. Country Filter
                val matchesCountry = country == null || sat.launchCountry == country

                // 6. Favorites Filter
                val matchesFavorites = !favorites || sat.isFavorite

                // 7. Mission Type Filter
                val matchesMissionType = missionType == null || sat.missionType == missionType

                matchesQuery && matchesUnit && matchesWeight && matchesStatus && matchesCountry && matchesFavorites && matchesMissionType
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Compile unique countries list dynamically
        availableCountries = repository.allSatellites.map { list ->
            list.map { it.launchCountry }.distinct().sorted()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        totalSatelliteCount = repository.allSatellites.map { list ->
            list.size
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    }

    fun selectSatellite(satellite: Satellite?) {
        _selectedSatellite.value = satellite
    }

    fun toggleFavorite(satellite: Satellite) {
        viewModelScope.launch {
            repository.update(satellite.copy(isFavorite = !satellite.isFavorite))
            // Update currently selected if it is the modified satellite
            if (_selectedSatellite.value?.id == satellite.id) {
                _selectedSatellite.value = _selectedSatellite.value?.copy(isFavorite = !satellite.isFavorite)
            }
        }
    }

    fun deleteSatellite(satellite: Satellite) {
        viewModelScope.launch {
            repository.delete(satellite)
            if (_selectedSatellite.value?.id == satellite.id) {
                _selectedSatellite.value = null
            }
        }
    }

    fun clearAllFilters() {
        selectedUnitSize.value = null
        selectedWeightRange.value = null
        selectedStatus.value = null
        selectedCountry.value = null
        selectedMissionType.value = null
        showOnlyFavorites.value = false
        searchQuery.value = ""
    }
}
