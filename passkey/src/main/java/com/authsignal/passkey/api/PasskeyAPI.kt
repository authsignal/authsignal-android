package com.authsignal.passkey.api

import com.authsignal.passkey.api.models.*
import com.authsignal.passkey.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class PasskeyAPI(tenantID: String, private val baseURL: String) {
  private val client = HttpClient(Android) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
      })
    }
  }

  suspend fun registrationOptions(token: String, userName: String): RegistrationOptsResponse? {
    val url = "$baseURL/user-authenticators/passkey/registration-options"
    val body = RegistrationOptsRequest(userName)

    return postRequest(url, body, token)
  }

  suspend fun addAuthenticator(
    token: String,
    challengeID: String,
    credential: PasskeyRegistrationCredential,
  ): AddAuthenticatorResponse? {
    val url = "$baseURL/user-authenticators/passkey"
    val body = AddAuthenticatorRequest(challengeID, credential)

    return postRequest(url, body, token)
  }

  suspend fun authenticationOptions(token: String): AuthenticationOptsResponse? {
    val url = "$baseURL/user-authenticators/passkey/authentication-options"
    val body = AuthenticationOptsRequest()

    return postRequest(url, body, token)
  }

  suspend fun verify(
    token: String,
    challengeID: String,
    credential: PasskeyAuthenticationCredential,
  ): VerifyResponse? {
    val url = "$baseURL/verify/passkey"
    val body = VerifyRequest(challengeID, credential)

    return postRequest(url, body, token)
  }

  private suspend inline fun <reified TRequest, reified TResponse>postRequest(
    url: String,
    body: TRequest,
    token: String,
  ): TResponse? {
    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)

      headers {
        append(HttpHeaders.Authorization, "Bearer $token")
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      response.body<TResponse>()
    } else {
      return null
    }
  }
}
