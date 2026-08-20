package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.LocationCoordinate
import com.example.model.SosLogEntry
import com.example.model.SosState
import com.example.service.LocationProvider
import com.example.service.SmsDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SosUiState(
    val emergencyContact: String = DEFAULT_EMERGENCY_CONTACT,
    val contactName: String = "Emergency Contact",
    val sosState: SosState = SosState.Idle,
    val lastLocation: LocationCoordinate? = null,
    val generatedMessage: String = "",
    val countdownActive: Boolean = false,
    val countdownRemaining: Int = 3,
    val historyLogs: List<SosLogEntry> = emptyList(),
    val autoSendCountdownEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_EMERGENCY_CONTACT = "+1234567890"
    }
}

class SosViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
    private val locationProvider = LocationProvider(application)
    private val smsDispatcher = SmsDispatcher(application)

    private val _uiState = MutableStateFlow(
        SosUiState(
            emergencyContact = prefs.getString("emergency_contact", SosUiState.DEFAULT_EMERGENCY_CONTACT)
                ?: SosUiState.DEFAULT_EMERGENCY_CONTACT,
            contactName = prefs.getString("contact_name", "Emergency Contact") ?: "Emergency Contact",
            autoSendCountdownEnabled = prefs.getBoolean("countdown_enabled", true)
        )
    )
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun setEmergencyContact(number: String, name: String = "Emergency Contact") {
        val cleanNumber = number.trim()
        val cleanName = name.trim().ifBlank { "Emergency Contact" }
        prefs.edit()
            .putString("emergency_contact", cleanNumber)
            .putString("contact_name", cleanName)
            .apply()

        _uiState.update {
            it.copy(
                emergencyContact = cleanNumber,
                contactName = cleanName
            )
        }
    }

    fun toggleCountdownMode(enabled: Boolean) {
        prefs.edit().putBoolean("countdown_enabled", enabled).apply()
        _uiState.update { it.copy(autoSendCountdownEnabled = enabled) }
    }

    fun initiateEmergencySos(immediate: Boolean = false) {
        if (!immediate && _uiState.value.autoSendCountdownEnabled) {
            startCountdown()
        } else {
            executeSosDispatch()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                sosState = SosState.Countdown(3),
                countdownActive = true,
                countdownRemaining = 3
            )
        }

        countdownJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                _uiState.update {
                    it.copy(
                        sosState = SosState.Countdown(sec),
                        countdownRemaining = sec
                    )
                }
                delay(1000L)
            }
            _uiState.update { it.copy(countdownActive = false) }
            executeSosDispatch()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.update {
            it.copy(
                sosState = SosState.Idle,
                countdownActive = false,
                countdownRemaining = 3
            )
        }
    }

    fun executeSosDispatch() {
        viewModelScope.launch {
            _uiState.update { it.copy(sosState = SosState.AcquiringLocation) }

            val location = locationProvider.fetchCurrentLocation()
            val contact = _uiState.value.emergencyContact

            if (location == null) {
                // Could not fetch GPS location in time
                val fallbackMsg = "EMERGENCY SOS! I need immediate help! (Unable to acquire precise GPS signal at this moment)."
                val smsResult = smsDispatcher.sendEmergencySms(contact, fallbackMsg)

                if (smsResult.isSuccess) {
                    val log = SosLogEntry(
                        recipient = contact,
                        latitude = 0.0,
                        longitude = 0.0,
                        mapsUrl = "",
                        status = "Sent (No GPS Fix)"
                    )
                    _uiState.update {
                        it.copy(
                            sosState = SosState.Success(
                                latitude = 0.0,
                                longitude = 0.0,
                                mapsUrl = "",
                                recipient = contact,
                                timestamp = System.currentTimeMillis()
                            ),
                            generatedMessage = fallbackMsg,
                            historyLogs = listOf(log) + it.historyLogs
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            sosState = SosState.Error(
                                message = "Unable to get GPS location & SMS delivery failed: ${smsResult.exceptionOrNull()?.localizedMessage ?: "Unknown error"}",
                                preparedMapsUrl = null
                            ),
                            generatedMessage = fallbackMsg
                        )
                    }
                }
                return@launch
            }

            // Location obtained successfully!
            val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(location.timestamp))
            val mapsUrl = location.googleMapsUrl
            val sosMessage = "EMERGENCY SOS! I need immediate help. My current GPS location: $mapsUrl (Accuracy: ~${location.accuracyMeters.toInt()}m at $formattedTime)"

            _uiState.update {
                it.copy(
                    lastLocation = location,
                    generatedMessage = sosMessage,
                    sosState = SosState.SendingSms(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        mapsUrl = mapsUrl
                    )
                )
            }

            // Small visual progression delay so the user clearly sees state transition
            delay(500L)

            val smsResult = smsDispatcher.sendEmergencySms(contact, sosMessage)

            if (smsResult.isSuccess) {
                val log = SosLogEntry(
                    recipient = contact,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    mapsUrl = mapsUrl,
                    status = "SMS Dispatched Successfully"
                )
                _uiState.update {
                    it.copy(
                        sosState = SosState.Success(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            mapsUrl = mapsUrl,
                            recipient = contact,
                            timestamp = System.currentTimeMillis()
                        ),
                        historyLogs = listOf(log) + it.historyLogs
                    )
                }
            } else {
                val errorDesc = smsResult.exceptionOrNull()?.localizedMessage
                    ?: "SMS service error. Please use manual SMS fallback."
                _uiState.update {
                    it.copy(
                        sosState = SosState.Error(
                            message = "SMS send failed: $errorDesc",
                            canFallbackToIntent = true,
                            preparedMapsUrl = mapsUrl
                        )
                    )
                }
            }
        }
    }

    fun resetSosState() {
        _uiState.update {
            it.copy(
                sosState = SosState.Idle,
                countdownActive = false
            )
        }
    }
}
