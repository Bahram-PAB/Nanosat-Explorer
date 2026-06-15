package com.example.data

// این کلاس قالب ماهواره‌های شما را برای برنامه مشخص می‌کند
data class Satellite(
    val name: String,
    val organisation: String,
    val nation: String,
    val type: String,
    val launchDate: String,
    val status: String,
    val description: String,
    // این دو خط زیر به صورت زاپاس قرار گرفته‌اند تا اگر ظاهر برنامه به TLE نیاز داشت، خطا ندهد
    val tleLine1: String = "",
    val tleLine2: String = ""
)
