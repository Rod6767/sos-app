package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

class SmsDispatcher(private val context: Context) {

    fun sendEmergencySms(phoneNumber: String, message: String): Result<Unit> {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val cleanNumber = phoneNumber.trim()
            if (cleanNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("Emergency phone number is empty"))
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(cleanNumber, null, message, null, null)
            }
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e("SmsDispatcher", "SMS permission denied: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("SmsDispatcher", "Failed to send SMS: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun createSmsIntent(phoneNumber: String, message: String): Intent {
        val cleanNumber = phoneNumber.trim()
        val uri = Uri.parse("smsto:$cleanNumber")
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createDialerIntent(phoneNumber: String): Intent {
        val cleanNumber = phoneNumber.trim()
        val uri = Uri.parse("tel:$cleanNumber")
        return Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createMapsIntent(latitude: Double, longitude: Double): Intent {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Emergency+Location)")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
