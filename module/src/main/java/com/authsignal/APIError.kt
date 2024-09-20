package com.authsignal

import android.util.Log
import com.authsignal.models.AuthsignalResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

private const val TAG = "com.authsignal"

object APIError {
  suspend fun <T>mapToErrorResponse(response: HttpResponse): AuthsignalResponse<T> {
    return try {
      val errorResponse = response.body<APIErrorResponse>()

      val errorType = errorResponse.errorCode ?: errorResponse.error

      val error = if (!errorResponse.errorDescription.isNullOrEmpty())
        errorResponse.errorDescription
      else
        errorResponse.error

      Log.e(TAG, "API error: $error")

      AuthsignalResponse(data = null, error = error, errorType = errorType)
    } catch (e : Exception) {
      AuthsignalResponse(data = null, error = e.message)
    }
  }
}

@Serializable
data class APIErrorResponse(
  val error: String,
  val errorCode: String? = null,
  val errorDescription: String? = null,
)