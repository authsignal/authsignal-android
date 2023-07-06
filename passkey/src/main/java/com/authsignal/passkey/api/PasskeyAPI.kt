package com.authsignal.passkey.api

import android.util.Log
import com.authsignal.passkey.api.models.*
import com.authsignal.passkey.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val TAG = "authsignal"

class PasskeyAPI(tenantID: String, private val baseURL: String) {
  private val client = HttpClient(Android) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
      })
    }
  }

  suspend fun registrationOptions(
    token: String,
    userName: String,
  ): AuthsignalResponse<RegistrationOptsResponse> {
    val url = "$baseURL/user-authenticators/passkey/registration-options"
    val body = RegistrationOptsRequest(userName)

    return postRequest(url, body, token)
  }

  suspend fun addAuthenticator(
    token: String,
    challengeID: String,
    credential: PasskeyRegistrationCredential,
  ): AuthsignalResponse<AddAuthenticatorResponse> {
    val url = "$baseURL/user-authenticators/passkey"
    val body = AddAuthenticatorRequest(challengeID, credential)

    return postRequest(url, body, token)
  }

  suspend fun authenticationOptions(
    token: String,
  ): AuthsignalResponse<AuthenticationOptsResponse> {
    val url = "$baseURL/user-authenticators/passkey/authentication-options"
    val body = AuthenticationOptsRequest()

    return postRequest(url, body, token)
  }

  suspend fun verify(
    token: String,
    challengeID: String,
    credential: PasskeyAuthenticationCredential,
  ): AuthsignalResponse<VerifyResponse> {
    val url = "$baseURL/verify/passkey"
    val body = VerifyRequest(challengeID, credential)

    return postRequest(url, body, token)
  }

  private suspend inline fun <reified TRequest, reified TResponse>postRequest(
    url: String,
    body: TRequest,
    token: String,
  ): AuthsignalResponse<TResponse> {
    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)

      headers {
        append(HttpHeaders.Authorization, "Bearer $token")
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      val data = response.body<TResponse>()
      AuthsignalResponse(data = data)
    } else {
      val error = response.bodyAsText()
      Log.e(TAG, "Passkey request error: $error")
      AuthsignalResponse(error = error)
    }
  }
}
