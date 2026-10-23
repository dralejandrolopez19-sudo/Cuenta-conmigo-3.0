package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.TealSecondary

@Composable
fun CuentaConmigoTopBar(
    onEditProfileClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    onSosClicked: () -> Unit = {},
    showSosButton: Boolean = true,
    isGuest: Boolean = false,
    isStreetModeActive: Boolean = false,
    onToggleStreetModeQuick: () -> Unit = {},
    onOpenStreetPrivacySettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Logo Badge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, TealSecondary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_logo),
                contentDescription = "Cuenta Conmigo Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Titles
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Cuenta Conmigo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    letterSpacing = (-0.3).sp
                )
                if (isGuest) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(TealSecondary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Invitado",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealSecondary
                        )
                    }
                }
            }
            Text(
                text = if (isStreetModeActive) "Modo Calle • Privacidad activa" else if (isGuest) "Modo Explorador" else "Cuaderno de Bienestar",
                fontSize = 12.sp,
                fontWeight = if (isStreetModeActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isStreetModeActive) PrivacyEmerald else if (isGuest) TealSecondary else MutedSlate
            )
        }

        if (showSosButton) {
            // Botón SOS Minimalista y de Acceso Directo (Solo visible en Inicio)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier
                    .clickable { onSosClicked() }
                    .testTag("topbar_sos_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "Botón SOS - Centros de Apoyo y Contención",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        // Menú Desplegable de 3 Puntos (Opciones limpias y Modo Calle centralizado)
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.testTag("topbar_overflow_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = CharcoalText
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                // Opción 1: Activar / Desactivar Modo Calle directamente con Switch
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = if (isStreetModeActive) "Modo Calle: Activo" else "Modo Calle: Desactivado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isStreetModeActive) PrivacyEmerald else CharcoalText
                            )
                            Text(
                                text = if (isStreetModeActive) "Tus datos están protegidos en público" else "Oculta datos sensibles ante miradas",
                                fontSize = 11.sp,
                                color = MutedSlate
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isStreetModeActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isStreetModeActive) PrivacyEmerald else CharcoalText
                        )
                    },
                    trailingIcon = {
                        Switch(
                            checked = isStreetModeActive,
                            onCheckedChange = {
                                onToggleStreetModeQuick()
                                menuExpanded = false
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrivacyEmerald
                            )
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleStreetModeQuick()
                    },
                    modifier = Modifier.testTag("menu_item_toggle_street_mode")
                )

                // Opción 2: Configuración avanzada del Escudo (Movimiento e Inteligencia Artificial)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "Opciones de Modo Calle e IA",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Detección por movimiento y camuflaje IA",
                                fontSize = 11.sp,
                                color = MutedSlate
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = TealSecondary
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenStreetPrivacySettings()
                    },
                    modifier = Modifier.testTag("menu_item_street_privacy_settings")
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                DropdownMenuItem(
                    text = { Text("Personalizar mi nombre") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onEditProfileClicked()
                    }
                )

                DropdownMenuItem(
                    text = { Text(if (isGuest) "Salir del modo invitado" else "Cerrar sesión") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onSignOutClicked()
                    }
                )
            }
        }
    }
}
