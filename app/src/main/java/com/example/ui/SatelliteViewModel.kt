package com.example.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Satellite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

// وضعیت‌های مورد نیاز برای هوش مصنوعی Gemini در برنامه
sealed interface GeminiSearchState {
    object Idle : GeminiSearchState
    object Loading : GeminiSearchState
    data class Success(val response: String) : GeminiSearchState
    data class Error(val message: String) : GeminiSearchState
}

class SatelliteViewModel(application: Application) : AndroidViewModel(application) {

    // لیست اصلی نگهداری ماهواره‌ها در حافظه
    private val allSatellitesList = mutableStateListOf<Satellite>()

    // تمام متغیرهایی که MainActivity برای فیلتر و نمایش به آن‌ها نیاز دارد
    val searchQuery = mutableStateOf("")
    val selectedUnitSize = mutableStateOf<String?>(null)
    val selectedWeightRange = mutableStateOf<String?>(null)
    val selectedStatus = mutableStateOf<String?>(null)
    val selectedCountry = mutableStateOf<String?>(null)
    val selectedMissionType = mutableStateOf<String?>(null)
    val showOnlyFavorites = mutableStateOf(false)
    val selectedSatellite = mutableStateOf<Satellite?>(null)
    val geminiSearchState = mutableStateOf<GeminiSearchState>(GeminiSearchState.Idle)

    // فیلتر شدن خودکار لیست ماهواره‌ها به محض تغییر فیلترها توسط کاربر
    val uiState = derivedStateOf {
        var filtered = allSatellitesList.toList()
        val query = searchQuery.value
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        val country = selectedCountry.value
        if (country != null) {
            filtered = filtered.filter { it.launchCountry == country }
        }
        val status = selectedStatus.value
        if (status != null) {
            filtered = filtered.filter { it.status == status }
        }
        if (showOnlyFavorites.value) {
            filtered = filtered.filter { it.isFavorite }
        }
        filtered
    }

    // متغیرهای آماری برنامه
    val totalCount = derivedStateOf { uiState.value.size }
    val availableCountries = derivedStateOf { allSatellitesList.map { it.launchCountry }.distinct().sorted() }

    init {
        loadSatellitesFromJson()
    }

    // خواندن فایل JSON شما و تبدیل فیلدها به ساختار استاندارد برنامه
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
                        
                        // استخراج وزن عددی از متن (مثلا "8.5 kg" تبدیل به 8.5 می‌شود)
                        val numericWeight = try {
                            weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                        } catch(e: Exception) { 0.0 }

                        temp.add(
                            Satellite(
                                id = UUID.randomUUID().toString(),
                                name = obj.optString("Mission name", "نامشخص"),
                                missionType = obj.optString("Mission description", "نامشخص"),
                                unitSize = obj.optString("Type (U/mass)", "نامشخص"),
                                weightKg = numericWeight,
                                launchCountry = obj.optString("Nation", "نامشخص"),
                                launchAgency = obj.optString("Organisation", "نامشخص"),
                                status = obj.optString("Status", "نامشخص"),
                                launchDate = obj.optString("Launch date", "نامشخص"),
                                missionObjective = obj.optString("Mission description", "بدون توضیحات")
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                temp
            }
            allSatellitesList.clear()
            allSatellitesList.addAll(list)
        }
    }

    // متدها و اکشن‌های کلیک که لایه UI به آن‌ها نیاز دارد
    fun selectSatellite(satellite: Satellite?) {
        selectedSatellite.value = satellite
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
        val index = allSatellitesList.indexOfFirst { it.id == satellite.id }
        if (index != -1) {
            val updated = allSatellitesList[index].copy(isFavorite = !allSatellitesList[index].isFavorite)
            allSatellitesList[index] = updated
            if (selectedSatellite.value?.id == satellite.id) {
                selectedSatellite.value = updated
            }
        }
    }

    fun deleteSatellite(satellite: Satellite) {
        allSatellitesList.removeAll { it.id == satellite.id }
        if (selectedSatellite.value?.id == satellite.id) {
            selectedSatellite.value = null
        }
    }

    fun resetGeminiState() {
        geminiSearchState.value = GeminiSearchState.Idle
    }

    fun searchOnlineSatellite(query: String) {
        geminiSearchState.value = GeminiSearchState.Loading
        viewModelScope.launch {
            // شبیه‌سازی پاسخ آنلاین برای عدم کرش برنامه
            geminiSearchState.value = GeminiSearchState.Success("اطلاعات تکمیلی یافت نشد.")
        }
    }
}
