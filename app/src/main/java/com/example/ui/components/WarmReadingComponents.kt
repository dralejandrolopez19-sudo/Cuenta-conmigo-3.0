package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Paleta de colores cálidos (sepia / pergamino suave / ámbar) diseñada para
 * reducir la fatiga visual en el Diario y la Agenda.
 */
object WarmReadingPalette {
    val PaperBackground = Color(0xFFF3E3C3)
    val CardBackground = Color(0xFFFBF0D9)
    val ElevatedSurface = Color(0xFFF7E8C8)
    val SubtleSurface = Color(0xFFEAD4AA)
    val BorderColor = Color(0xFFD9B98B)
    val InkPrimary = Color(0xFF3B2314)
    val InkSecondary = Color(0xFF69462D)
    val InkMuted = Color(0xFF8C6244)
    val AmberAccent = Color(0xFFB45309)
    val AmberSoft = Color(0xFFFDE68A)
    val AmberFilterColor = Color(0xFFD97706)
    val NightDimColor = Color(0xFF1C1108)
}

@Composable
fun WarmReadingHeaderToggle(
    isReadingModeActive: Boolean,
    isAutoByTime: Boolean,
    onToggleReadingMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "reading_mode"
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isReadingModeActive) {
            WarmReadingPalette.AmberAccent.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isReadingModeActive) {
                WarmReadingPalette.AmberAccent.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onToggleReadingMode() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("${tagPrefix}_toggle_button")
            ) {
                Icon(
                    imageVector = if (isReadingModeActive) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                    contentDescription = "Activar o desactivar modo lectura cálido",
                    tint = if (isReadingModeActive) WarmReadingPalette.AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isReadingModeActive) "Lectura Cálida" else "Modo Lectura",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isAutoByTime) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isReadingModeActive) WarmReadingPalette.AmberAccent.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "AUTO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isReadingModeActive) WarmReadingPalette.AmberAccent else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("${tagPrefix}_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configurar horario y brillo del modo lectura",
                    tint = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun WarmReadingStatusBar(
    isReadingModeActive: Boolean,
    isAutoByTime: Boolean,
    startHour: Int,
    endHour: Int,
    dimLevel: Float,
    warmthLevel: Float,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "reading_mode"
) {
    AnimatedVisibility(
        visible = isReadingModeActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val scheduleLabel = String.format(Locale.getDefault(), "%02d:00–%02d:00", startHour, endHour)
        val dimPercent = (dimLevel * 100).roundToInt()
        val warmthPercent = (warmthLevel * 100).roundToInt()

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = WarmReadingPalette.ElevatedSurface,
            border = BorderStroke(1.dp, WarmReadingPalette.BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSettings() }
                .testTag("${tagPrefix}_status_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(WarmReadingPalette.AmberAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = WarmReadingPalette.AmberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isAutoByTime) {
                                "Modo Lectura Cálido activo · Auto ($scheduleLabel)"
                            } else {
                                "Modo Lectura Cálido activo · Manual"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarmReadingPalette.InkPrimary
                        )
                        Text(
                            text = "Brillo -${dimPercent}% · Calidez ${warmthPercent}% · Toca para ajustar",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmReadingPalette.InkSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.BrightnessLow,
                    contentDescription = "Ajustar brillo cálido",
                    tint = WarmReadingPalette.AmberAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Capa visual no intrusiva que aplica un filtro cálido ámbar y reducción de brillo
 * sobre el Diario y la Agenda sin bloquear toques.
 */
@Composable
fun WarmReadingScreenOverlay(
    isReadingModeActive: Boolean,
    dimLevel: Float,
    warmthLevel: Float,
    modifier: Modifier = Modifier
) {
    if (!isReadingModeActive) return

    val clampedDim = dimLevel.coerceIn(0.05f, 0.70f)
    val clampedWarmth = warmthLevel.coerceIn(0.10f, 0.85f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmReadingPalette.AmberFilterColor.copy(alpha = clampedWarmth * 0.16f))
            .background(WarmReadingPalette.NightDimColor.copy(alpha = clampedDim * 0.42f))
            .testTag("warm_reading_screen_overlay")
    )
}

@Composable
fun WarmReadingSettingsDialog(
    isReadingModeActive: Boolean,
    isAutoByTime: Boolean,
    startHour: Int,
    endHour: Int,
    dimLevel: Float,
    warmthLevel: Float,
    currentHour: Int,
    onToggleReadingMode: (Boolean) -> Unit,
    onToggleAutoByTime: (Boolean) -> Unit,
    onUpdateSchedule: (Int, Int) -> Unit,
    onUpdateDimLevel: (Float) -> Unit,
    onUpdateWarmthLevel: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val scheduleText = String.format(Locale.getDefault(), "%02d:00 a %02d:00", startHour, endHour)
    val currentHourText = String.format(Locale.getDefault(), "%02d:00", currentHour)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isReadingModeActive) WarmReadingPalette.CardBackground else MaterialTheme.colorScheme.surface,
            border = if (isReadingModeActive) BorderStroke(1.dp, WarmReadingPalette.BorderColor) else null,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("warm_reading_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(WarmReadingPalette.AmberAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = WarmReadingPalette.AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Modo de Lectura Cálido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Diario y Agenda · Descanso visual",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar ajustes de lectura",
                            tint = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Switch principal de Modo Lectura
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isReadingModeActive) WarmReadingPalette.ElevatedSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Activar ahora",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tonos sepia suaves y menor luminancia",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isReadingModeActive,
                            onCheckedChange = onToggleReadingMode,
                            modifier = Modifier.testTag("reading_mode_manual_switch")
                        )
                    }
                }

                // Activación automática según la hora
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isReadingModeActive) WarmReadingPalette.ElevatedSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Automático según la hora",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Horario activo: $scheduleText (Hora actual: $currentHourText)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAutoByTime,
                                onCheckedChange = onToggleAutoByTime,
                                modifier = Modifier.testTag("reading_mode_auto_switch")
                            )
                        }

                        if (isAutoByTime) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hora de inicio
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isReadingModeActive) WarmReadingPalette.CardBackground else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isReadingModeActive) WarmReadingPalette.BorderColor else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onUpdateSchedule((startHour + 23) % 24, endHour) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("reading_start_hour_minus")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Restar hora inicio", modifier = Modifier.size(16.dp))
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Desde",
                                                fontSize = 10.sp,
                                                color = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d:00", startHour),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { onUpdateSchedule((startHour + 1) % 24, endHour) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("reading_start_hour_plus")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Sumar hora inicio", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Hora de fin
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isReadingModeActive) WarmReadingPalette.CardBackground else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isReadingModeActive) WarmReadingPalette.BorderColor else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onUpdateSchedule(startHour, (endHour + 23) % 24) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("reading_end_hour_minus")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Restar hora fin", modifier = Modifier.size(16.dp))
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Hasta",
                                                fontSize = 10.sp,
                                                color = if (isReadingModeActive) WarmReadingPalette.InkSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d:00", endHour),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { onUpdateSchedule(startHour, (endHour + 1) % 24) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("reading_end_hour_plus")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Sumar hora fin", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Control de reducción de brillo
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessLow,
                                contentDescription = null,
                                tint = WarmReadingPalette.AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Reducción de brillo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "-${(dimLevel * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarmReadingPalette.AmberAccent
                        )
                    }
                    Slider(
                        value = dimLevel,
                        onValueChange = onUpdateDimLevel,
                        valueRange = 0.10f..0.65f,
                        colors = SliderDefaults.colors(
                            thumbColor = WarmReadingPalette.AmberAccent,
                            activeTrackColor = WarmReadingPalette.AmberAccent
                        ),
                        modifier = Modifier.testTag("reading_dim_slider")
                    )
                }

                // Control de calidez cromática
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbTwilight,
                                contentDescription = null,
                                tint = WarmReadingPalette.AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Temperatura cálida (Ámbar)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isReadingModeActive) WarmReadingPalette.InkPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${(warmthLevel * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarmReadingPalette.AmberAccent
                        )
                    }
                    Slider(
                        value = warmthLevel,
                        onValueChange = onUpdateWarmthLevel,
                        valueRange = 0.15f..0.85f,
                        colors = SliderDefaults.colors(
                            thumbColor = WarmReadingPalette.AmberAccent,
                            activeTrackColor = WarmReadingPalette.AmberAccent
                        ),
                        modifier = Modifier.testTag("reading_warmth_slider")
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReadingModeActive) WarmReadingPalette.AmberAccent else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_reading_settings_button")
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
