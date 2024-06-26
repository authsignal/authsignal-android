package com.authsignal.passkey.api

import android.util.Log
import com.authsignal.passkey.Encoder
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

  private val basicAuth = "Basic ${Encoder.toBase64String("$tenantID:".toByteArray())}"

  suspend fun challenge(
    action: String,
  ): AuthsignalResponse<ChallengeResponse> {
    val url = "$baseURL/client/challenge"
    val body = ChallengeRequest(action)

    return postRequest(url, body)
  }

  suspend fun registrationOptions(
    token: String,
    userName: String? = null,
    displayName: String? = null,
  ): AuthsignalResponse<RegistrationOptsResponse> {
    val url = "$baseURL/client/user-authenticators/passkey/registration-options"
    val body = RegistrationOptsRequest(userName, displayName)

    return postRequest(url, body, token)
  }

  suspend fun addAuthenticator(
    token: String,
    challengeID: String,
    credential: PasskeyRegistrationCredential,
  ): AuthsignalResponse<AddAuthenticatorResponse> {
    val url = "$baseURL/client/user-authenticators/passkey"
    val body = AddAuthenticatorRequest(challengeID, credential)

    return postRequest(url, body, token)
  }

  suspend fun authenticationOptions(
    token: String?,
    challengeID: String?
  ): AuthsignalResponse<AuthenticationOptsResponse> {
    val url = "$baseURL/client/user-authenticators/passkey/authentication-options"
    val body = AuthenticationOptsRequest(challengeID)

    return postRequest(url, body, token)
  }

  suspend fun verify(
    challengeID: String,
    credential: PasskeyAuthenticationCredential,
    token: String?,
    deviceID: String?,
  ): AuthsignalResponse<VerifyResponse> {
    val url = "$baseURL/client/verify/passkey"
    val body = VerifyRequest(challengeID, credential, deviceID)

    return postRequest(url, body, token)
  }

  suspend fun getPasskeyAuthenticator(credentialId: String): AuthsignalResponse<PasskeyAuthenticatorResponse> {
    val url = "$baseURL/client/user-authenticators/passkey?credentialId=$credentialId"

    val response = client.get(url) {
      headers {
        append(HttpHeaders.Authorization, basicAuth)
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      val passkeyAuthenticatorResponse = response.body<PasskeyAuthenticatorResponse>()

      AuthsignalResponse(data = passkeyAuthenticatorResponse)
    } else {
      val error = response.bodyAsText()

      AuthsignalResponse(error = error)
    }
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
      Log.e(TAG, "Passkey request error: $error")
      AuthsignalResponse(error = error)
    }
  }
}
