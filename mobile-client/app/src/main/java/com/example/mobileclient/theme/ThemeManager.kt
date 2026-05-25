package com.example.mobileclient.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    var isDark: Boolean by mutableStateOf(true)
}
