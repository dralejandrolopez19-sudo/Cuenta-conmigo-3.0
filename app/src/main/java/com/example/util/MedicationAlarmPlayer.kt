package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Receptor de alarmas programadas en el sistema Android (AlarmManager).
 * Garantiza que las notificaciones y alertas sonoras se disparen puntualmente.
 */
class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val medName = intent?.getStringExtra("MED_NAME") ?: "Pastilla programada"
        val medDosage = intent?.getStringExtra("MED_DOSAGE") ?: ""
        val schedTime = intent?.getStringExtra("SCHED_TIME") ?: ""
        val summary = if (medDosage.isNotBlank()) {
            "Hora de tomar: $medName ($medDosage) • $schedTime"
        } else {
            "Es hora de tomar tus pastillas programadas ($medName)"
        }
        MedicationAlarmPlayer(context.applicationContext).showSystemAlarmNotification(summary)
    }
}

/**
 * Gestiona el sonido de alarma médica:
 * 1. Síntesis acústica PCM de doble tono (880 Hz / 1174 Hz) mediante AudioTrack en USAGE_MEDIA y USAGE_ALARM
 *    (audible en el emulador WebRTC y en cualquier teléfono físico aunque no tenga tonos del sistema).
 * 2. Ringtone del sistema + ToneGenerator de respaldo.
 * 3. Vibración continua y notificación de alta prioridad hasta confirmar las pastillas.
 */
class MedicationAlarmPlayer(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "pastillas_alarm_channel_v2"
        private const val NOTIFICATION_ID = 4091
        private const val SAMPLE_RATE = 22050
    }

    private var ringtone: Ringtone? = null
    private var toneGeneratorAlarm: ToneGenerator? = null
    private var toneGeneratorMedia: ToneGenerator? = null
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

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
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
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 800, 400)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Programa alarmas exactas o de ventana en el AlarmManager del sistema para cada horario del día.
     */
    fun scheduleSystemAlarmsForToday(medId: String, medName: String, dosage: String, times: List<String>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = java.util.Calendar.getInstance()

        for (timeStr in times) {
            val parts = timeStr.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue

            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            if (target.timeInMillis <= now.timeInMillis) continue

            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                putExtra("MED_ID", medId)
                putExtra("MED_NAME", medName)
                putExtra("MED_DOSAGE", dosage)
                putExtra("SCHED_TIME", timeStr)
            }
            val requestCode = ("${medId}_$timeStr").hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                am.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    30_000L,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.w("MedicationAlarmPlayer", "No se pudo registrar AlarmManager para $timeStr", e)
            }
        }
    }

    private fun ensureAudibleVolume() {
        try {
            audioManager?.let { am ->
                val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val curMusic = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (curMusic < (maxMusic * 0.6f).toInt()) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxMusic * 0.8f).toInt(), 0)
                }

                val maxAlarm = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val curAlarm = am.getStreamVolume(AudioManager.STREAM_ALARM)
                if (curAlarm < (maxAlarm * 0.6f).toInt()) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, (maxAlarm * 0.85f).toInt(), 0)
                }
            }
        } catch (e: Exception) {
            Log.w("MedicationAlarmPlayer", "No se pudo ajustar el volumen automáticamente", e)
        }
    }

    /**
     * Genera un patrón de alarma médica armónica en memoria (A5 880Hz + D6 1175Hz + A5 880Hz)
     * para reproducirse nítidamente por AudioTrack sin depender de archivos externos.
     */
    private fun buildAlarmPcmBuffer(): ShortArray {
        val durationMs = 750
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        val segmentSamples = totalSamples / 3

        for (i in 0 until totalSamples) {
            val freq = when {
                i < segmentSamples -> 880.0
                i < segmentSamples * 2 -> 1174.66
                else -> 987.77
            }
            val localIdx = i % segmentSamples
            val env = when {
                localIdx < 350 -> localIdx / 350.0
                localIdx > segmentSamples - 600 -> ((segmentSamples - localIdx).coerceAtLeast(0)) / 600.0
                else -> 1.0
            }
            val angle = 2.0 * PI * i * freq / SAMPLE_RATE
            val fundamental = sin(angle)
            val harmonic = 0.25 * sin(angle * 2.0)
            val sampleValue = ((fundamental + harmonic) * 0.75 * env * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sampleValue.toShort()
        }
        return buffer
    }

    private fun playSynthesizedBeep(pcmData: ShortArray, usage: Int) {
        var track: AudioTrack? = null
        try {
            val byteSize = pcmData.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track.play()
        } catch (e: Exception) {
            Log.w("MedicationAlarmPlayer", "Aviso en AudioTrack ($usage)", e)
        } finally {
            alarmScope.launch {
                delay(850L)
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun startAlarm(summaryText: String = "Es hora de tomar tus pastillas programadas.") {
        showSystemAlarmNotification(summaryText)
        ensureAudibleVolume()

        // Si ya estaba sonando, reiniciamos el ciclo para garantizar feedback acústico inmediato
        if (isPlaying) {
            stopAlarmInternal(keepNotification = true)
        }
        isPlaying = true

        try {
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

        // Bucle acústico garantizado: AudioTrack PCM (USAGE_MEDIA + USAGE_ALARM) + ToneGenerator
        alarmLoopJob?.cancel()
        alarmLoopJob = alarmScope.launch {
            val pcmBuffer = buildAlarmPcmBuffer()
            val tgAlarm = try {
                ToneGenerator(AudioManager.STREAM_ALARM, 100)
            } catch (_: Exception) {
                null
            }
            val tgMedia = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            } catch (_: Exception) {
                null
            }
            toneGeneratorAlarm = tgAlarm
            toneGeneratorMedia = tgMedia

            while (isActive && isPlaying) {
                try {
                    ringtone?.let { rt ->
                        if (!rt.isPlaying) {
                            rt.play()
                        }
                    }
                    playSynthesizedBeep(pcmBuffer, AudioAttributes.USAGE_MEDIA)
                    playSynthesizedBeep(pcmBuffer, AudioAttributes.USAGE_ALARM)
                    tgAlarm?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 450)
                    tgMedia?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                } catch (e: Exception) {
                    Log.w("MedicationAlarmPlayer", "Aviso en ciclo sonoro de alarma", e)
                }
                delay(1400L)
            }
        }

        // Iniciar Vibración en bucle continuo
        try {
            val pattern = longArrayOf(0, 500, 300, 500, 300, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("MedicationAlarmPlayer", "Error al iniciar vibración de alarma", e)
        }
    }

    fun showSystemAlarmNotification(summaryText: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                9901,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("¡Alarma de Pastillas Activa!")
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()

            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w("MedicationAlarmPlayer", "No se pudo mostrar la notificación de alarma", e)
        }
    }

    private fun stopAlarmInternal(keepNotification: Boolean) {
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

            toneGeneratorAlarm?.stopTone()
            toneGeneratorAlarm?.release()
            toneGeneratorAlarm = null

            toneGeneratorMedia?.stopTone()
            toneGeneratorMedia?.release()
            toneGeneratorMedia = null

            vibrator?.cancel()
            if (!keepNotification) {
                notificationManager?.cancel(NOTIFICATION_ID)
            }
        } catch (e: Exception) {
            Log.e("MedicationAlarmPlayer", "Error al detener alarma de pastillas", e)
        }
    }

    fun stopAlarm() {
        stopAlarmInternal(keepNotification = false)
    }

    fun isAlarmActive(): Boolean = isPlaying
}
