package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.theme.CharcoalText
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Almacena temporalmente el nombre obtenido del token de Google para sincronizarlo
 * de forma segura dentro de AppViewModel sin cancelar la corrutina al desmontar AuthScreen.
 */
object PendingGoogleProfileSync {
    @Volatile
    var pendingDisplayName: String? = null

    @Volatile
    var autoSignInJob: Job? = null

    fun consumeDisplayName(): String? {
        val name = pendingDisplayName
        pendingDisplayName = null
        return name
    }
}

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { errorMsg ->
                    isLoading = false
                    onAuthError(errorMsg)
                },
                scope = coroutineScope,
                onAuthCancelled = {
                    isLoading = false
                    onAuthCancelled()
                }
            )
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = CharcoalText
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = CharcoalText,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Continuar con Google",
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continuar con Google",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = CharcoalText,
                    maxLines = 1
                )
            }
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
    clearStateBeforeRequest: Boolean = false
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id).trim()
    } catch (e: Exception) {
        onAuthError("Configuración de Google no encontrada en el proyecto.")
        return
    }

    if (clientId.isEmpty()) {
        onAuthError("Identificador de cliente web de Google vacío.")
        return
    }

    val activityContext = context.findActivity() ?: context
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            // Cancelar cualquier intento automático en segundo plano para evitar colisión del selector
            val activeAutoJob = PendingGoogleProfileSync.autoSignInJob
            if (activeAutoJob != null && activeAutoJob.isActive) {
                activeAutoJob.cancel()
                PendingGoogleProfileSync.autoSignInJob = null
                kotlinx.coroutines.delay(150L)
            }

            if (clearStateBeforeRequest) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    kotlinx.coroutines.delay(120L)
                } catch (e: Exception) {
                    Log.w("Auth", "Aviso limpiando estado previo de credenciales", e)
                }
            }

            val result = try {
                if (clearStateBeforeRequest) {
                    val broadGoogleOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(broadGoogleOption)
                        .build()
                    credentialManager.getCredential(activityContext, fallbackRequest)
                } else {
                    credentialManager.getCredential(activityContext, primaryRequest)
                }
            } catch (primaryErr: Exception) {
                if (primaryErr is CancellationException && primaryErr !is GetCredentialCancellationException) {
                    throw primaryErr
                }
                if (!clearStateBeforeRequest && primaryErr !is GetCredentialCancellationException) {
                    Log.w("Auth", "Reintentando selector con GetGoogleIdOption tras fallo primario", primaryErr)
                    try {
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: Exception) {}
                    val broadGoogleOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(broadGoogleOption)
                        .build()
                    credentialManager.getCredential(activityContext, fallbackRequest)
                } else {
                    throw primaryErr
                }
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                val googleName = googleIdTokenCredential.displayName?.trim()?.ifBlank { null }
                    ?: listOfNotNull(googleIdTokenCredential.givenName, googleIdTokenCredential.familyName)
                        .joinToString(" ").trim().ifBlank { null }

                PendingGoogleProfileSync.pendingDisplayName = googleName

                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Credencial de Google no compatible.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No se encontraron credenciales de Google disponibles", e)
            onAuthError("No hay cuentas de Google activas en este dispositivo. Agrega una cuenta de Google en Ajustes o usa el botón de recarga.")
        } catch (e: CancellationException) {
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            val msg = e.localizedMessage ?: "Error al autenticar"
            val friendly = when {
                msg.contains("16:") || msg.contains("10:") || msg.contains("DEVELOPER_ERROR") || msg.contains("28444") ->
                    "No se pudo verificar la sesión con Google Play Services. Pulsa 'Limpiar y abrir selector de cuentas Google'."
                msg.contains("network", ignoreCase = true) ->
                    "Error de conexión de red al conectar con Google. Verifica tu internet."
                else ->
                    "No se pudo completar el acceso con Google ($msg). Pulsa 'Limpiar y abrir selector de cuentas Google'."
            }
            onAuthError(friendly)
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id).trim()
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    if (clientId.isEmpty()) {
        onUnauthenticated()
        return
    }

    val activityContext = context.findActivity() ?: context
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    PendingGoogleProfileSync.autoSignInJob = scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                val googleName = googleIdTokenCredential.displayName?.trim()?.ifBlank { null }
                    ?: listOfNotNull(googleIdTokenCredential.givenName, googleIdTokenCredential.familyName)
                        .joinToString(" ").trim().ifBlank { null }

                PendingGoogleProfileSync.pendingDisplayName = googleName

                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        } finally {
            PendingGoogleProfileSync.autoSignInJob = null
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    PendingGoogleProfileSync.autoSignInJob?.cancel()
    PendingGoogleProfileSync.autoSignInJob = null
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("Auth", "Aviso al limpiar credenciales", e)
        } finally {
            onSignOutComplete()
        }
    }
}
