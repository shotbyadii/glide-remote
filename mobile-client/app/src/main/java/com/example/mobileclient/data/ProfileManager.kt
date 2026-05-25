package com.example.mobileclient.data

import android.content.Context
import android.content.SharedPreferences

object ProfileManager {
    private const val PREFS_NAME = "gtv_physics_profiles"
    private const val KEY_LAST_SENSITIVITY = "last_sensitivity"
    private const val KEY_LAST_ACCELERATION = "last_acceleration"
    private const val KEY_LAST_FRICTION = "last_friction"
    private const val KEY_LAST_TRACKPAD_SENSITIVITY = "last_trackpad_sensitivity"
    private const val KEY_PROFILE_LIST = "profile_list"
    private const val KEY_LAST_IP = "last_ip"
    private const val KEY_HAPTIC_TAP_ENABLED = "haptic_tap_enabled"
    private const val KEY_HAPTIC_HOVER_ENABLED = "haptic_hover_enabled"
    private const val KEY_HAPTIC_TAP_INTENSITY = "haptic_tap_intensity"
    private const val KEY_HAPTIC_HOVER_INTENSITY = "haptic_hover_intensity"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLastValues(context: Context, sensitivity: Float, acceleration: Float, friction: Float, trackpadSensitivity: Float) {
        prefs(context).edit()
            .putFloat(KEY_LAST_SENSITIVITY, sensitivity)
            .putFloat(KEY_LAST_ACCELERATION, acceleration)
            .putFloat(KEY_LAST_FRICTION, friction)
            .putFloat(KEY_LAST_TRACKPAD_SENSITIVITY, trackpadSensitivity)
            .apply()
    }

    fun loadLastValues(context: Context): PhysicsProfile {
        val p = prefs(context)
        return PhysicsProfile(
            name = "Last Used",
            sensitivity = p.getFloat(KEY_LAST_SENSITIVITY, 40.0f),
            acceleration = p.getFloat(KEY_LAST_ACCELERATION, 0.08f),
            friction = p.getFloat(KEY_LAST_FRICTION, 0.9f),
            trackpadSensitivity = p.getFloat(KEY_LAST_TRACKPAD_SENSITIVITY, 1.0f)
        )
    }

    fun saveProfile(context: Context, profile: PhysicsProfile) {
        val p = prefs(context)
        val list = listProfiles(context).toMutableList()
        if (!list.contains(profile.name)) {
            list.add(profile.name)
            p.edit().putString(KEY_PROFILE_LIST, list.joinToString("|")).apply()
        }
        p.edit()
            .putFloat(profile.name + "_sensitivity", profile.sensitivity)
            .putFloat(profile.name + "_acceleration", profile.acceleration)
            .putFloat(profile.name + "_friction", profile.friction)
            .putFloat(profile.name + "_trackpad", profile.trackpadSensitivity)
            .apply()
    }

    fun loadProfile(context: Context, name: String): PhysicsProfile? {
        val p = prefs(context)
        if (listProfiles(context).contains(name)) {
            return PhysicsProfile(
                name = name,
                sensitivity = p.getFloat(name + "_sensitivity", 40.0f),
                acceleration = p.getFloat(name + "_acceleration", 0.08f),
                friction = p.getFloat(name + "_friction", 0.9f),
                trackpadSensitivity = p.getFloat(name + "_trackpad", 1.0f)
            )
        }
        return null
    }

    fun deleteProfile(context: Context, name: String) {
        val p = prefs(context)
        val list = listProfiles(context).toMutableList()
        list.remove(name)
        p.edit()
            .putString(KEY_PROFILE_LIST, list.joinToString("|"))
            .remove(name + "_sensitivity")
            .remove(name + "_acceleration")
            .remove(name + "_friction")
            .remove(name + "_trackpad")
            .apply()
    }

    fun listProfiles(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_PROFILE_LIST, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun saveLastIp(context: Context, ip: String) {
        prefs(context).edit().putString(KEY_LAST_IP, ip).apply()
    }

    fun loadLastIp(context: Context): String {
        return prefs(context).getString(KEY_LAST_IP, "") ?: ""
    }

    fun saveHapticSettings(context: Context, tapEnabled: Boolean, hoverEnabled: Boolean, tapIntensity: Float, hoverIntensity: Float) {
        prefs(context).edit()
            .putBoolean(KEY_HAPTIC_TAP_ENABLED, tapEnabled)
            .putBoolean(KEY_HAPTIC_HOVER_ENABLED, hoverEnabled)
            .putFloat(KEY_HAPTIC_TAP_INTENSITY, tapIntensity)
            .putFloat(KEY_HAPTIC_HOVER_INTENSITY, hoverIntensity)
            .apply()
    }

    fun isHapticTapEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_HAPTIC_TAP_ENABLED, true)
    fun isHapticHoverEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_HAPTIC_HOVER_ENABLED, true)
    fun getHapticTapIntensity(context: Context): Float = prefs(context).getFloat(KEY_HAPTIC_TAP_INTENSITY, 0.8f)
    fun getHapticHoverIntensity(context: Context): Float = prefs(context).getFloat(KEY_HAPTIC_HOVER_INTENSITY, 0.4f)
}
