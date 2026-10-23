package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.onGoogleSignInClicked
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealSecondary

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var wasCancelledOnce by remember { mutableStateOf(false) }
    var isRetryingClean by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftCreamBackground)
            .statusBarsPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Logo de la Aplicación
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, TealSecondary.copy(alpha = 0.35f), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo),
                    contentDescription = "Cuenta Conmigo Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cuenta Conmigo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "Cuaderno de Bienestar y Salud Mental",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TealSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Tarjeta de Conexión con Google + Opción de Privacidad Modo Calle
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_minimal_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bienvenido a tu espacio seguro",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Conéctate con tu cuenta de Google para sincronizar y proteger tu cuaderno de bienestar.",
                        fontSize = 13.sp,
                        color = MutedSlate,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón Principal de Google
                    GoogleSignInButton(
                        onAuthSuccess = {
                            errorMessage = null
                            wasCancelledOnce = false
                            onAuthSuccess()
                        },
                        onAuthError = { err ->
                            errorMessage = err
                        },
                        onAuthCancelled = {
                            wasCancelledOnce = true
                        }
                    )

                    // Botón de recarga limpia si el selector se cerró o se desea cambiar de cuenta
                    if (wasCancelledOnce || errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                isRetryingClean = true
                                errorMessage = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isRetryingClean = false
                                        wasCancelledOnce = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { err ->
                                        isRetryingClean = false
                                        errorMessage = err
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isRetryingClean = false
                                    },
                                    clearStateBeforeRequest = true
                                )
                            },
                            enabled = !isRetryingClean,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, TealSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("retry_clean_google_sign_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Limpiar y abrir selector de cuentas Google",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealSecondary
                            )
                        }
                    }

                    // Mensaje de Error amigable si ocurre algún problema
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CoralPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(1.dp, CoralPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = CoralPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "“Un día y un paso a la vez. Tu bienestar y privacidad importan.”",
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MutedSlate,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
        }
    }
}
