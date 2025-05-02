package com.gululu.aamediamate.models

enum class LanguageOption(val displayName: String, val language: String, val country:String) {
    SYSTEM("System", "", ""),
    ENGLISH("English", "en", "US"),
    SIMPLIFIED_CHINESE("简体中文", "zh", "CN"),
    TRADITIONAL_CHINESE("繁體中文", "zh", "HK")
}