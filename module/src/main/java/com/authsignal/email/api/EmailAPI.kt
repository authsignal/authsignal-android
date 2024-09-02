package com.authsignal.email.api

import android.util.Log
import com.authsignal.Encoder
import com.authsignal.email.api.models.*
import com.authsignal.models.AuthsignalResponse
import com.authsignal.models.ChallengeResponse
import com.authsignal.models.EnrollResponse
import com.authsignal.models.VerifyRequest
import com.authsignal.models.VerifyResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val TAG = "com.authsignal.email.api"

class EmailAPI(tenantID: String, private val baseURL: String) {
  private val client = HttpClient(Android) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
      })
    }
  }

  private val basicAuth = "Basic ${Encoder.toBase64String("$tenantID:".toByteArray())}"

  suspend fun enroll(token: String, email: String): AuthsignalResponse<EnrollResponse> {
    val url = "$baseURL/client/enroll/email-otp"
    val body = AddEmailAuthenticatorRequest(email)

    return postRequest(url, body, token)
  }

  suspend fun challenge(token: String): AuthsignalResponse<ChallengeResponse> {
    val url = "$baseURL/client/challenge/email-otp"

    return postRequest(url,token)
  }

  suspend fun verify(
    token: String,
    code: String,
  ): AuthsignalResponse<VerifyResponse> {
    val url = "$baseURL/client/verify/email-otp"
    val body = VerifyRequest(code)

    return postRequest(url, body, token)
  }

  private suspend inline fun <reified TRequest, reified TResponse>postRequest(
    url: String,
    body: TRequest,
    token: String? = null,
  ): AuthsignalResponse<TResponse> {
    val response = client.post(url) {

      contentType(ContentType.Application.Json)
      setBody(body)

      headers {
        append(
          HttpHeaders.Authorization,
          if (token != null) "Bearer $token" else basicAuth,
        )
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      try {
        val data = response.body<TResponse>()
        AuthsignalResponse(data = data)
      } catch (e : Exception) {
        AuthsignalResponse(error = e.message)
      }
    } else {
      val error = response.bodyAsText()
      Log.e(TAG, "Email request error: $error")
      AuthsignalResponse(error = error)
    }
  }

  private suspend inline fun <reified TResponse>postRequest(
    url: String,
    token: String,
  ): AuthsignalResponse<TResponse> {
    val response = client.post(url) {
      headers {
        append(HttpHeaders.Authorization, "Bearer $token")
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      try {
        val data = response.body<TResponse>()
        AuthsignalResponse(data = data)
      } catch (e : Exception) {
        AuthsignalResponse(error = e.message)
      }
    } else {
      val error = response.bodyAsText()
      Log.e(TAG, "Email request error: $error")
      AuthsignalResponse(error = error)
    }
  }
}
