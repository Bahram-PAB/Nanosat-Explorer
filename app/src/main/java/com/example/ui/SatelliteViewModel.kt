package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Satellite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class SatelliteViewModel(application: Application) : AndroidViewModel(application) {

    private val _satellites = MutableStateFlow<List<Satellite>>(emptyList())
    val satellites: StateFlow<List<Satellite>> = _satellites

    init {
        // به محض اجرای برنامه، فایل شما لود می‌شود
        loadSatellitesFromJson()
    }

    private fun loadSatellitesFromJson() {
        viewModelScope.launch {
            val loadedList = withContext(Dispatchers.IO) {
                val list = mutableListOf<Satellite>()
                try {
                    // باز کردن فایل از پوشه assets
                    val inputStream = getApplication<Application>().assets.open("satellites.json")
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    
                    val jsonArray = JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        
                        // خواندن دقیق اطلاعات بر اساس ساختار فایل جی‌سان شما
                        list.add(
                            Satellite(
                                name = obj.optString("Mission name", "نامشخص"),
                                organisation = obj.optString("Organisation", "نامشخص"),
                                nation = obj.optString("Nation", "نامشخص"),
                                type = obj.optString("Type (U/mass)", "نامشخص"),
                                launchDate = obj.optString("Launch date", "نامشخص"),
                                status = obj.optString("Status", "نامشخص"),
                                description = obj.optString("Mission description", "توضیحات ندارد")
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                list
            }
            _satellites.value = loadedList
        }
    }
}
