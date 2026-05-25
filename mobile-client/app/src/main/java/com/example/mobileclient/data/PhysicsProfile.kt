package com.example.mobileclient.data

data class PhysicsProfile(
    val name: String,
    val sensitivity: Float,
    val acceleration: Float,
    val friction: Float,
    val trackpadSensitivity: Float = 1.0f
)
