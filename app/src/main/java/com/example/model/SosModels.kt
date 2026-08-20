package com.example.model

sealed interface SosState {
    data object Idle : SosState
    data class Countdown(val secondsRemaining: Int) : SosState
    data object AcquiringLocation : SosState
    data class SendingSms(val latitude: Double, val longitude: Double, val mapsUrl: String) : SosState
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val mapsUrl: String,
        val recipient: String,
        val timestamp: Long
    ) : SosState
    data class Error(
        val message: String,
        val canFallbackToIntent: Boolean = true,
        val preparedMapsUrl: String? = null
    ) : SosState
}

data class LocationCoordinate(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "GPS"
) {
    val googleMapsUrl: String
        get() = "https://maps.google.com/?q=$latitude,$longitude"
}

data class SosLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val recipient: String,
    val latitude: Double,
    val longitude: Double,
    val mapsUrl: String,
    val status: String
)
