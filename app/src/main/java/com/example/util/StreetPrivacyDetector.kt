package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detector inteligente en dispositivo (sin permisos invasivos de cámara ni GPS)
 * que utiliza el acelerómetro para proteger la privacidad cuando el usuario:
 * 1. Camina por la calle (patrón rítmico de pasos continuos).
 * 2. Realiza un gesto rápido de inclinación o giro ("Tilt-to-Hide") cuando alguien se acerca.
 */
class StreetPrivacyDetector(
    context: Context,
    private val onTriggerPrivacy: (reason: String) -> Unit
) : SensorEventListener {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var lastTriggerTimestamp = 0L

    // Variables para detección de caminata rítmica (pasos en la calle)
    private val stepTimestamps = ArrayDeque<Long>()
    private var lastStepPeakTime = 0L

    // Variables para gesto rápido de inclinación / sacudida protectora
    private var lastZ = 0f
    private var hasBaseline = false

    val isSensorAvailable: Boolean
        get() = accelerometer != null

    fun start() {
        if (isListening || accelerometer == null) return
        hasBaseline = false
        stepTimestamps.clear()
        sensorManager?.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        isListening = true
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        hasBaseline = false
        stepTimestamps.clear()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val now = System.currentTimeMillis()

        // Evitar disparos repetidos en menos de 6 segundos
        if (now - lastTriggerTimestamp < 6_000L) return

        val magnitude = sqrt(x * x + y * y + z * z)
        val deltaFromGravity = kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH)

        if (!hasBaseline) {
            lastZ = z
            hasBaseline = true
            return
        }

        // 1. Gesto "Tilt-to-Hide" o teléfono puesto boca abajo / giro brusco protector
        val zDelta = kotlin.math.abs(z - lastZ)
        lastZ = z
        if (z < -6.5f || deltaFromGravity > 11.5f || zDelta > 9.5f) {
            lastTriggerTimestamp = now
            stepTimestamps.clear()
            onTriggerPrivacy("Activado por gesto rápido antimiradas")
            return
        }

        // 2. Detección de caminata en la calle (oscilación rítmica entre 2.2 y 7.5 m/s²)
        if (deltaFromGravity in 2.4f..8.5f && (now - lastStepPeakTime) > 330L) {
            lastStepPeakTime = now
            stepTimestamps.addLast(now)

            // Mantener ventana de los últimos 4.5 segundos
            while (stepTimestamps.isNotEmpty() && (now - stepTimestamps.first()) > 4_500L) {
                stepTimestamps.removeFirst()
            }

            // Si se detectan al menos 5 pasos rítmicos seguidos en la ventana, estamos en movimiento/calle
            if (stepTimestamps.size >= 5) {
                lastTriggerTimestamp = now
                stepTimestamps.clear()
                onTriggerPrivacy("Activado automáticamente al detectar movimiento en calle")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se requiere acción adicional
    }
}
