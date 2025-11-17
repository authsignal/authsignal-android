package com.authsignal.inapp.api

import com.authsignal.APIError
import com.authsignal.Encoder
import com.authsignal.inapp.api.models.InAppVerifyRequest
import com.authsignal.inapp.api.models.InAppVerifyResponse
import com.authsignal.models.AuthsignalResponse
import com.authsignal.models.api.*
import com.authsignal.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class InAppAPI(tenantID: String, private val baseURL: String) {
  private val client = HttpClient(Android) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
      })
    }
  }

  private val basicAuth = "Basic ${Encoder.toBase64String("$tenantID:".toByteArray())}"

  suspend fun getCredential(publicKey: String): AuthsignalResponse<AppCredential> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/in-app?publicKey=$encodedKey"

    return try {
      val response = client.get(url) {
        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val data = response.body<AppCredentialResponse>()

        val credential = AppCredential(
          userId = data.userId,
          credentialId = data.userAuthenticatorId,
          createdAt = data.verifiedAt,
          lastAuthenticatedAt = data.lastVerifiedAt,
        )

        AuthsignalResponse(data = credential)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun addCredential(
    token: String,
    publicKey: String,
    deviceName: String = ""): AuthsignalResponse<AppCredential> {
    val url = "$baseURL/client/user-authenticators/in-app"
    val body = AddAppCredentialRequest(
      publicKey,
      deviceName,
      devicePlatform = "android",
    )

    return try {
      val response = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)

        headers {
          append(HttpHeaders.Authorization, "Bearer $token")
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val data = response.body<AppCredentialResponse>()

        val credential = AppCredential(
          userId = data.userId,
          credentialId = data.userAuthenticatorId,
          createdAt = data.verifiedAt,
          lastAuthenticatedAt = data.lastVerifiedAt,
        )

        AuthsignalResponse(data = credential)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun removeCredential(
    publicKey: String,
    signature: String,
  ): AuthsignalResponse<Boolean> {
    val url = "$baseURL/client/user-authenticators/in-app/remove"
    val body = RemoveAppCredentialRequest(publicKey, signature)

    return try {
      val response = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)

        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      val success = response.status == HttpStatusCode.OK

      return if (success) {
        AuthsignalResponse(data = true)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun challenge(action: String?): AuthsignalResponse<ChallengeResponse> {
    val url = "$baseURL/client/challenge"
    val body = action?.let { ChallengeRequest(it) }

    return try {
      val response = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)

        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val data = response.body<ChallengeResponse>()

        AuthsignalResponse(data = data)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun verify(
    challengeId: String,
    publicKey: String,
    signature: String,
    token: String?
  ): AuthsignalResponse<InAppVerifyResponse> {
    val url = "$baseURL/client/verify/in-app"
    val body = InAppVerifyRequest(
      challengeId,
      publicKey,
      signature,
    )

    return try {
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

      val success = response.status == HttpStatusCode.OK

      if (success) {
        val data = response.body<InAppVerifyResponse>()
        AuthsignalResponse(data = data)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }
}
