package com.authsignal.passkey

import com.authsignal.models.AuthsignalResponse

object PasskeySdkErrors {
  suspend fun <T>contextError(): AuthsignalResponse<T> {
    return AuthsignalResponse(
      error = "An activity param must be provided when initializing the SDK to use passkeys.",
      errorCode = "sdk_error"
    )
  }
}
