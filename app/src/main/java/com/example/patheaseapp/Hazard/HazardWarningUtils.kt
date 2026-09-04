package com.example.patheaseapp.Hazard

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import com.example.patheaseapp.util.TtsManager
import java.util.Locale

// Helper functions for hazard warning feedback: device vibration and text-to-speech alerts.

@RequiresPermission(Manifest.permission.VIBRATE)
fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    val effect = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
    vibrator.vibrate(effect)
}

fun speakWarning(ttsManager: TtsManager, message: String) {
    ttsManager.speak(message)
}