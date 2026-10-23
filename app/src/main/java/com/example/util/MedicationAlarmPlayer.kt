package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gestiona el sonido de alarma (Ringtone del sistema + ToneGenerator de respaldo en canal de alarma),
 * la vibración continua y la notificación de alta prioridad hasta que el usuario confirme sus pastillas.
 */
class MedicationAlarmPlayer(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "pastillas_alarm_channel"
        private const val NOTIFICATION_ID = 4091
    }

    private var ringtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var alarmLoopJob: Job? = null
    private val alarmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var isPlaying = false

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val notificationManager: NotificationManager? by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    init {
        createAlarmNotificationChannel()
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarmas de Pastillas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas sonoras y persistentes para la toma puntual de pastillas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 400, 500, 400, 800, 600)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun startAlarm(summaryText: String = "Es hora de tomar tus pastillas programadas.") {
        showSystemAlarmNotification(summaryText)

        if (isPlaying) return
        isPlaying = true

        try {
            // 1. Iniciar Ringtone de Alarma del sistema si está disponible
            var alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            if (alertUri != null) {
                ringtone = RingtoneManager.getRingtone(context, alertUri)?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = true
                    }
                    play()
                }
            }
        } catch (e: Exception) {
            Log.e("MedicationAlarmPlayer", "Aviso al reproducir Ringtone del sistema", e)
        }

        // 2. Bucle acústico garantizado con ToneGenerator en STREAM_ALARM y re-disparo de Ringtone
        alarmLoopJob?.cancel()
        alarmLoopJob = alarmScope.launch {
            val tg = try {
                ToneGenerator(AudioManager.STREAM_ALARM, 100)
            } catch (e: Exception) {
                Log.w("MedicationAlarmPlayer", "No se pudo inicializar ToneGenerator", e)
                null
            }
            toneGenerator = tg

            while (isActive && isPlaying) {
                try {
                    // Asegurar que si el Ringtone no está en loop en API < 28 o no sonó, vuelva a sonar
                    ringtone?.let { rt ->
                        if (!rt.isPlaying) {
                            rt.play()
                        }
                    }
                    // Emitir pulso sonoro de alarma de respaldo (audible en emuladores y dispositivos sin tonos)
                    tg?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
                } catch (e: Exception) {
                    Log.w("MedicationAlarmPlayer", "Aviso en ciclo sonoro de alarma", e)
                }
                delay(1500L)
            }
        }

        // 3. Iniciar Vibración en bucle continuo
        try {
            val pattern = longArrayOf(0, 500, 400, 500, 400, 800, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 1)
            }
        } catch (e: Exception) {
            Log.e("MedicationAlarmPlayer", "Error al iniciar vibración de alarma", e)
        }
    }

    private fun showSystemAlarmNotification(summaryText: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("¡Alarma de Pastillas!")
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w("MedicationAlarmPlayer", "No se pudo mostrar la notificación de alarma", e)
        }
    }

    fun stopAlarm() {
        isPlaying = false
        try {
            alarmLoopJob?.cancel()
            alarmLoopJob = null

            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            ringtone = null

            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null

            vibrator?.cancel()
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e("MedicationAlarmPlayer", "Error al detener alarma de pastillas", e)
        }
    }

    fun isAlarmActive(): Boolean = isPlaying
}
