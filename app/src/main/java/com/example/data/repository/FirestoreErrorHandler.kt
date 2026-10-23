package com.example.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("CREATE"),
    READ("READ"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    LIST("LIST"),
    GET("GET"),
    WRITE("WRITE"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    // Ignorar cancelaciones normales del ciclo de vida de Compose / Coroutines (ej. al cambiar de AuthScreen a HomeScreen)
    if (exception is CancellationException ||
        exception.message?.contains("coroutine scope left the composition", ignoreCase = true) == true ||
        exception.message?.contains("Job was cancelled", ignoreCase = true) == true
    ) {
        Log.d("FirestoreTeardown", "Operación cancelada por transición de composición: $path")
        return ""
    }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        Log.d("FirestoreTeardown", "Query omitida o cerrada debido a sesión finalizada: $path")
        return ""
    }

    val providerInfoList = currentUser.providerData.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    }

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser.uid)
        put("email", currentUser.email)
        put("emailVerified", currentUser.isEmailVerified)
        put("tenantId", currentUser.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", jsonString)
    return jsonString
}
