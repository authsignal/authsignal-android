package com.authsignal.inapp

import android.content.Context
import com.authsignal.models.AuthsignalResponse
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.tasks.await

class PlayIntegrityManager(private val context: Context?) {
  suspend fun requestToken(nonce: String): AuthsignalResponse<String> {
    val ctx = context
      ?: return AuthsignalResponse(
        error = "Context is required for device integrity.",
        errorCode = "sdk_error",
      )

    val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx)

    if (availability != ConnectionResult.SUCCESS) {
      return AuthsignalResponse(
        error = "Google Play Services is not available on this device (status: $availability).",
        errorCode = "sdk_error",
      )
    }

    return try {
      val integrityManager = IntegrityManagerFactory.create(ctx)

      val request = IntegrityTokenRequest.builder()
        .setNonce(nonce)
        .build()

      val response = integrityManager.requestIntegrityToken(request).await()

      AuthsignalResponse(data = response.token())
    } catch (e: Exception) {
      AuthsignalResponse(
        error = "Play Integrity request failed: ${e.message}",
        errorCode = "sdk_error",
      )
    }
  }
}
