package com.example.data

data class Satellite(
    val id: String = "",
    val name: String = "",
    val missionType: String = "", // برای هماهنگی با خط ۴۸ ارور
    val unitSize: String = "",
    val weightKg: Double = 0.0,
    val launchCountry: String = "",
    val launchAgency: String = "",
    val status: String = "",
    val launchDate: String = "",
    val description: String = "", // برای هماهنگی با خط ۸۰۸ ارور
    val missionObjective: String = "",
    val imageUrl: String? = null,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false
)
