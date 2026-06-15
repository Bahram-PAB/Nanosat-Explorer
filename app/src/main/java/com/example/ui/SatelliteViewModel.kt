package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Satellite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

sealed interface GeminiSearchState {
    object Idle : GeminiSearchState
    object Loading : GeminiSearchState
    data class Success(val response: String) : GeminiSearchState
    data class Error(val message: String) : GeminiSearchState
}

class SatelliteViewModel(application: Application) : AndroidViewModel(application) {

    private val _allSatellites = MutableStateFlow<List<Satellite>>(emptyList())
    val allSatellites: StateFlow<List<Satellite>> = _allSatellites

    // تعریف متغیرها به صورت StateFlow جهت هماهنگی کامل با MainActivity
    val searchQuery = MutableStateFlow("")
    val selectedUnitSize = MutableStateFlow<String?>(null)
    val selectedWeightRange = MutableStateFlow<String?>(null)
    val selectedStatus = MutableStateFlow<String?>(null)
    val selectedCountry = MutableStateFlow<String?>(null)
    val selectedMissionType = MutableStateFlow<String?>(null)
    val showOnlyFavorites = MutableStateFlow(false)
    
    private val _selectedSatellite = MutableStateFlow<Satellite?>(null)
    val selectedSatellite: StateFlow<Satellite?> = _selectedSatellite

    private val _geminiSearchState = MutableStateFlow<GeminiSearchState>(GeminiSearchState.Idle)
    val geminiSearchState: StateFlow<GeminiSearchState> = _geminiSearchState

    // ترکیب فیلترها و تولید خودکار لیست نهایی برای نمایش در UI
    val uiState: StateFlow<List<Satellite>> = combine(
        _allSatellites, searchQuery, selectedCountry, selectedStatus, showOnlyFavorites
    ) { list, query, country, status, favOnly ->
        var filtered = list
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        if (country != null) {
            filtered = filtered.filter { it.launchCountry == country }
        }
        if (status != null) {
            filtered = filtered.filter { it.status == status }
        }
        if (favOnly) {
            filtered = filtered.filter { it.isFavorite }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = combine(uiState) { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableCountries: StateFlow<List<String>> = combine(_allSatellites) { list ->
        list.map { it.launchCountry }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSatellitesFromJson()
    }

    private fun loadSatellitesFromJson() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val temp = mutableListOf<Satellite>()
                try {
                    val inputStream = getApplication<Application>().assets.open("satellites.json")
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val weightStr = obj.optString("Type (U/mass)", "0")
                        val numericWeight = try {
                            weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                        } catch(e: Exception) { 0.0 }

                        val missionDesc = obj.optString("Mission description", "بدون توضیحات")

                        temp.add(
                            Satellite(
                                id = UUID.randomUUID().toString(),
                                name = obj.optString("Mission name", "نامشخص"),
                                missionType = missionDesc,
                                unitSize = obj.optString("Type (U/mass)", "نامشخص"),
                                weightKg = numericWeight,
                                launchCountry = obj.optString("Nation", "نامشخص"),
                                launchAgency = obj.optString("Organisation", "نامشخص"),
                                status = obj.optString("Status", "نامشخص"),
                                launchDate = obj.optString("Launch date", "نامشخص"),
                                description = missionDesc,
                                missionObjective = missionDesc
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                temp
            }
            _allSatellites.value = list
        }
    }

    // متدهای تعاملی مورد نیاز کامپوننت‌های MainActivity
    fun selectSatellite(satellite: Satellite?) {
        _selectedSatellite.value = satellite
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

    fun toggleFavorite(satellite: Satellite) {
        val currentList = _allSatellites.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == satellite.id }
        if (index != -1) {
            val updated = currentList[index].copy(isFavorite = !currentList[index].isFavorite)
            currentList[index] = updated
            _allSatellites.value = currentList
            if (_selectedSatellite.value?.id == satellite.id) {
                _selectedSatellite.value = updated
            }
        }
    }

    fun deleteSatellite(satellite: Satellite) {
        val currentList = _allSatellites.value.toMutableList()
        currentList.removeAll { it.id == satellite.id }
        _allSatellites.value = currentList
        if (_selectedSatellite.value?.id == satellite.id) {
            _selectedSatellite.value = null
        }
    }

    fun resetGeminiState() {
        _geminiSearchState.value = GeminiSearchState.Idle
    }

    fun searchOnlineSatellite(query: String) {
        _geminiSearchState.value = GeminiSearchState.Loading
        viewModelScope.launch {
            _geminiSearchState.value = GeminiSearchState.Success("اطلاعات آنلاین یافت نشد.")
        }
    }
}
