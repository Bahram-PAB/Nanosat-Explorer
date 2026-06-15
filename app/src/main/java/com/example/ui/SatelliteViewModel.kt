package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.Satellite
import com.example.data.SatelliteRepository
import com.example.data.missionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GeminiSearchState {
    object Idle : GeminiSearchState
    object Loading : GeminiSearchState
    data class Success(val satellite: Satellite) : GeminiSearchState
    data class Error(val message: String) : GeminiSearchState
}

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

    // Gemini search operational state
    private val _geminiSearchState = MutableStateFlow<GeminiSearchState>(GeminiSearchState.Idle)
    val geminiSearchState: StateFlow<GeminiSearchState> = _geminiSearchState.asStateFlow()

    // Combined filtered list
    val uiState: StateFlow<List<Satellite>>

    // Total count of satellites in local database
    val totalCount: StateFlow<Int>

    // Live list of all countries for filter dropdown
    val availableCountries: StateFlow<List<String>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SatelliteRepository(database.satelliteDao())

        // Ensure database template is populated with 24 real CubeSats
        viewModelScope.launch {
            repository.ensurePopulated()
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
                // 1. Search Query (Name, Country, Agency, Description, Mission Objective, Unit Size, Weight/Mass)
                val matchesQuery = query.isEmpty() || 
                        sat.name.contains(query, ignoreCase = true) ||
                        sat.launchCountry.contains(query, ignoreCase = true) ||
                        sat.launchAgency.contains(query, ignoreCase = true) ||
                        sat.description.contains(query, ignoreCase = true) ||
                        sat.missionObjective.contains(query, ignoreCase = true) ||
                        matchUnitAndWeightQuery(query, sat)

                // 2. Unit Size Filter (Robust, handles things like "12U.", "12 U", ignores spaces, case and trailing dots)
                val matchesUnit = if (unit == null) {
                    true
                } else {
                    val sUnit = sat.unitSize.trim().lowercase()
                    val sClean = sUnit.replace(".", "").replace(" ", "")
                    val fClean = unit.trim().lowercase().replace(".", "").replace(" ", "")
                    if (fClean == "other") {
                        val standardUnits = listOf("1u", "2u", "3u", "4u", "5u", "6u", "8u", "12u", "16u", "24u")
                        sClean !in standardUnits
                    } else {
                        isUnitMatch(sClean, fClean)
                    }
                }

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

        // Initialize totalCount flow based on allSatellites
        totalCount = repository.allSatellites.map { list ->
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

    fun resetGeminiState() {
        _geminiSearchState.value = GeminiSearchState.Idle
    }

    /**
     * Conducts a detailed lookup via Gemini for any nanosatellite from the nanosats.eu database.
     * On success, imports it into the local database and automatically opens its detail view!
     */
    fun searchOnlineSatellite(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            _geminiSearchState.value = GeminiSearchState.Loading
            try {
                // Call Gemini REST Service
                val result = withContext(Dispatchers.IO) {
                    GeminiClient.querySatelliteInfo(name)
                }

                // Map results to database entity
                val newSatellite = Satellite(
                    name = result.name,
                    unitSize = result.unitSize,
                    weightKg = result.weightKg,
                    launchCountry = result.launchCountry,
                    launchAgency = result.launchAgency,
                    launchDate = result.launchDate,
                    status = result.status,
                    description = result.description,
                    missionObjective = result.missionObjective,
                    imageUrl = result.imageUrl,
                    isCustom = true
                )

                // Save to local database
                val insertedId = withContext(Dispatchers.IO) {
                    repository.insert(newSatellite)
                }

                // Retrieve the inserted record
                val savedSatellite = repository.getSatelliteById(insertedId.toInt()) ?: newSatellite.copy(id = insertedId.toInt())

                _geminiSearchState.value = GeminiSearchState.Success(savedSatellite)
                _selectedSatellite.value = savedSatellite

            } catch (e: Exception) {
                _geminiSearchState.value = GeminiSearchState.Error(
                    e.message ?: "An unknown error occurred while retrieving satellite specs."
                )
            }
        }
    }

    private fun matchUnitAndWeightQuery(query: String, sat: Satellite): Boolean {
        if (query.isBlank()) return true
        
        val q = query.trim().lowercase()
        val qClean = q.replace(".", "").replace(" ", "")
        
        // 1. Check against the Satellite's designated standard or custom unitSize string
        val sUnit = sat.unitSize.trim().lowercase()
        val sUnitClean = sUnit.replace(".", "").replace(" ", "")
        
        // Check if the query matches the unit size using our robust helper
        if (isUnitMatch(sUnitClean, qClean)) {
            return true
        }
        
        // 2. Check against the weight of the satellite (representing the "mass" in Type (u/mass))
        // We construct multiple weight/mass strings that represent the mass value:
        // e.g., for weightKg = 5.3: "5.3", "5.3kg", "5.3 kg"
        // e.g., for weightKg = 10.0: "10", "10kg", "10 kg", "10.0", "10.0kg", "10.0 kg"
        val wDouble = sat.weightKg
        val wInt = wDouble.toInt()
        val wHasDecimal = (wDouble - wInt) > 0.0001
        
        val weightCandidates = mutableListOf<String>()
        
        if (wHasDecimal) {
            val wStr = wDouble.toString() // "5.3"
            weightCandidates.add(wStr)
            weightCandidates.add("${wStr}kg")
            weightCandidates.add("${wStr} kg")
        } else {
            weightCandidates.add(wInt.toString()) // "10"
            weightCandidates.add("${wInt}kg")
            weightCandidates.add("${wInt} kg")
            
            // Also support search with exact decimal, e.g. "10.0"
            val wStr = wDouble.toString() // "10.0"
            weightCandidates.add(wStr)
            weightCandidates.add("${wStr}kg")
            weightCandidates.add("${wStr} kg")
        }
        
        // Check if the query matches any of the mass representations
        for (candidate in weightCandidates) {
            val candClean = candidate.lowercase().replace(" ", "")
            if (qClean == candClean || qClean.contains(candClean) || candClean.contains(qClean)) {
                return true
            }
        }
        
        return false
    }

    private fun isUnitMatch(sClean: String, qClean: String): Boolean {
        if (sClean == qClean) return true
        
        val standardUnits = listOf("1u", "2u", "3u", "4u", "5u", "6u", "8u", "12u", "16u", "24u")
        if (qClean in standardUnits) {
            var index = sClean.indexOf(qClean)
            while (index != -1) {
                val charBefore = if (index > 0) sClean[index - 1] else null
                val charAfter = if (index + qClean.length < sClean.length) sClean[index + qClean.length] else null
                
                val isCharBeforeDigit = charBefore != null && charBefore.isDigit()
                val isCharAfterDigitOrU = charAfter != null && (charAfter.isDigit() || charAfter == 'u')
                
                if (!isCharBeforeDigit && !isCharAfterDigitOrU) {
                    return true
                }
                index = sClean.indexOf(qClean, index + 1)
            }
            return false
        }
        
        return sClean.contains(qClean) || qClean.contains(sClean)
    }
}
