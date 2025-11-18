package com.authsignal

import android.util.Log
import com.authsignal.models.AuthsignalResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

private const val TAG = "com.authsignal"

object APIError {
  suspend fun <T>mapToErrorResponse(response: HttpResponse): AuthsignalResponse<T> {
    return try {

      val errorResponse = response.body<APIErrorResponse>()

      val errorCode = errorResponse.errorCode ?: errorResponse.error

      val error = if (!errorResponse.errorDescription.isNullOrEmpty())
        errorResponse.errorDescription
      else
        errorResponse.error

      Log.e(TAG, "API error: $error")

      if (response.status == HttpStatusCode.NotFound && error.isNullOrEmpty()) {
        return AuthsignalResponse(data = null, error = "API endpoint not found. Ensure your Authsignal base URL is valid.")
      }

      AuthsignalResponse(data = null, error = error, errorCode = errorCode)
    } catch (e : Exception) {
      AuthsignalResponse(data = null, error = e.message)
    }
  }

  fun <T>handleNetworkException(e: Exception): AuthsignalResponse<T> {
    return AuthsignalResponse(
      error = e.message,
      errorCode = "network_error"
    )
  }
}

@Serializable
data class APIErrorResponse(
  val error: String,
  val errorCode: String? = null,
  val errorDescription: String? = null,
)

object SdkErrorCodes {
  const val SdkError = "sdk_error"
  const val UserCanceled = "user_canceled"
  const val NoCredential = "no_credential"
  const val MatchedExcludedCredential = "matched_excluded_credential"
  const val InvalidStateError = "invalid_state_error"
  const val DomError = "dom_error"
  const val InvalidCredential = "invalid_credential"
  const val InvalidPinFormat = "invalid_pin_format"
}