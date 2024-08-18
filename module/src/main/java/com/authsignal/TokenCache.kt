package com.authsignal

import android.util.Log
import com.authsignal.models.AuthsignalResponse

private const val TAG = "authsignal"

class TokenCache private constructor() {
  var token: String? = null

  companion object {
    val shared: TokenCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { TokenCache() }
  }

  fun <T>handleTokenNotSetError(): AuthsignalResponse<T> {
    val error = "A token has not been set. Call 'setToken' first."

    Log.e(TAG, "Passkey request error: $error")

    return AuthsignalResponse(error = error)
  }
}