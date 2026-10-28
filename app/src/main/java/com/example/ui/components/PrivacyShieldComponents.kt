package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary

val PrivacySlateDark = Color(0xFF1E293B)
val PrivacyEmerald = Color(0xFF0D9488)
val PrivacyEmeraldLight = Color(0xFFCCFBF1)

@Composable
fun PrivacyShieldBanner(
    isStreetModeActive: Boolean,
    triggerReason: String,
    isAiDisguiseEnabled: Boolean,
    isGeneratingAi: Boolean,
    onOpenSettings: () -> Unit,
    onDisableStreetMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isStreetModeActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = PrivacySlateDark,
            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("street_privacy_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenSettings() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrivacyEmerald.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = if (isAiDisguiseEnabled) Icons.Default.AutoAwesome else Icons.Default.VisibilityOff,
                            contentDescription = "Escudo de privacidad activo",
                            tint = Color(0xFF5EEAD4),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAiDisguiseEnabled) "Modo Calle + Camuflaje IA" else "Modo Calle Activo",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isGeneratingAi) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    color = Color(0xFF5EEAD4),
                                    strokeWidth = 1.5.dp
                                )
                            }
                        }
                        Text(
                            text = triggerReason.ifBlank { "Toca cualquier dato oculto para verlo 5 s" },
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clickable { onOpenSettings() }
                            .testTag("banner_privacy_settings_btn")
                    ) {
                        Text(
                            text = "Opciones",
                            color = Color(0xFF5EEAD4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrivacyEmerald,
                        modifier = Modifier
                            .clickable { onDisableStreetMode() }
                            .testTag("disable_street_mode_banner_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Mostrar datos",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mostrar",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Componente que oculta, difumina o reemplaza con camuflaje IA un texto sensible cuando el Modo Calle está activo.
 * Al tocarlo, revela temporalmente el texto real durante 5 segundos.
 */
@Composable
fun PrivacyProtectedText(
    realText: String,
    itemId: String,
    isStreetModeActive: Boolean,
    isRevealed: Boolean,
    isAiDisguiseEnabled: Boolean,
    aiDisguiseText: String,
    onRevealClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = CharcoalText,
    maxLines: Int = Int.MAX_VALUE
) {
    if (!isStreetModeActive || isRevealed) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            Text(
                text = realText,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isStreetModeActive && isRevealed) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrivacyEmeraldLight
                ) {
                    Text(
                        text = "Visible 5s",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrivacyEmerald,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    } else if (isAiDisguiseEnabled) {
        // Modo Camuflaje IA: muestra un texto cotidiano neutro para despistar miradas
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onRevealClick(itemId) }
        ) {
            Text(
                text = aiDisguiseText,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFEDE9FE)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Camuflado con IA",
                        tint = Color(0xFF6D28D9),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "IA • Ver",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D28D9)
                    )
                }
            }
        }
    } else {
        // Modo Ocultamiento / Difuminado instantáneo en la calle
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = modifier
                .clickable { onRevealClick(itemId) }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Dato protegido",
                    tint = PrivacySlateDark,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(contentAlignment = Alignment.CenterStart) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Text(
                            text = realText.take(22).ifBlank { "Dato privado" },
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = MutedSlate.copy(alpha = 0.6f),
                            maxLines = 1,
                            modifier = Modifier.blur(10.dp)
                        )
                    }
                    Text(
                        text = "•••••• • Toca para ver",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrivacySlateDark
                    )
                }
            }
        }
    }
}

@Composable
fun StreetPrivacySettingsDialog(
    isStreetModeActive: Boolean,
    autoMotionEnabled: Boolean,
    isAiDisguiseEnabled: Boolean,
    isSensorAvailable: Boolean,
    isGeneratingAi: Boolean,
    aiStatusMessage: String?,
    onToggleStreetMode: (Boolean) -> Unit,
    onToggleAutoMotion: (Boolean) -> Unit,
    onToggleAiDisguise: (Boolean) -> Unit,
    onGenerateAiCamouflageNow: () -> Unit,
    onSimulateStreetMotion: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isStreetModeActive) PrivacyEmeraldLight else TealLight)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isStreetModeActive) PrivacyEmerald else TealSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Escudo Modo Calle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                    Text(
                        text = "Protección antimiradas en espacios públicos",
                        fontSize = 12.sp,
                        color = MutedSlate
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Interruptor Principal de Modo Calle
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStreetModeActive) PrivacyEmeraldLight.copy(alpha = 0.55f) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isStreetModeActive) PrivacyEmerald else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isStreetModeActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isStreetModeActive) PrivacyEmerald else CharcoalText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Activar Modo Calle ahora",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CharcoalText
                            )
                            Text(
                                text = "Oculta o difumina pastillas, diario, estados de ánimo y citas al instante. Toca un elemento para verlo por 5 segundos.",
                                fontSize = 12.sp,
                                color = MutedSlate,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isStreetModeActive,
                            onCheckedChange = onToggleStreetMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrivacyEmerald
                            ),
                            modifier = Modifier.testTag("dialog_toggle_street_mode")
                        )
                    }
                }

                // 2. Detección inteligente de movimiento y calle (Acelerómetro)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Detección Inteligente de Calle y Giro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CharcoalText
                                )
                                Text(
                                    text = if (isSensorAvailable) {
                                        "Activa el Modo Calle automáticamente al detectar que vas caminando o si inclinas bruscamente el teléfono cuando alguien se acerca."
                                    } else {
                                        "Protección automática por movimiento o simulación rápida."
                                    },
                                    fontSize = 12.sp,
                                    color = MutedSlate,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = autoMotionEnabled,
                                onCheckedChange = onToggleAutoMotion,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TealSecondary
                                ),
                                modifier = Modifier.testTag("toggle_motion_privacy_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onSimulateStreetMotion,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TealSecondary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("simulate_street_motion_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenLockPortrait,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Probar activación por movimiento / giro",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 3. Camuflaje Inteligente con IA (Gemini)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Camuflaje Inteligente con IA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CharcoalText
                                )
                                Text(
                                    text = "En lugar de puntos ocultos, usa IA (Gemini) para mostrar en pantalla textos neutros de tareas cotidianas mientras estás rodeado de gente.",
                                    fontSize = 12.sp,
                                    color = MutedSlate,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isAiDisguiseEnabled,
                                onCheckedChange = onToggleAiDisguise,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6D28D9)
                                ),
                                modifier = Modifier.testTag("toggle_ai_disguise_switch")
                            )
                        }

                        if (isGeneratingAi || !aiStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                if (isGeneratingAi) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color(0xFF6D28D9),
                                        strokeWidth = 1.8.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sincronizando alias discretos con IA...",
                                        fontSize = 11.sp,
                                        color = Color(0xFF5B21B6),
                                        fontWeight = FontWeight.Medium
                                    )
                                } else if (!aiStatusMessage.isNullOrBlank()) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF6D28D9),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = aiStatusMessage,
                                        fontSize = 11.sp,
                                        color = Color(0xFF5B21B6),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Información sobre protección de pantalla del sistema (FLAG_SECURE)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrivacySlateDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mientras el Modo Calle está activo, Android bloquea las capturas de pantalla y oculta la vista previa en Apps Recientes.",
                        fontSize = 11.sp,
                        color = MutedSlate,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
            ) {
                Text("Listo")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}
