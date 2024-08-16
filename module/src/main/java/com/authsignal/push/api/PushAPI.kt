package com.authsignal.push.api

import android.util.Log
import com.authsignal.Encoder
import com.authsignal.push.api.models.*
import com.authsignal.push.models.AuthsignalResponse
import com.authsignal.push.models.PushCredential
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

class PushAPI(tenantID: String, private val baseURL: String) {
  private val client = HttpClient(Android) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
      })
    }
  }

  private val basicAuth = "Basic ${Encoder.toBase64String("$tenantID:".toByteArray())}"

  suspend fun getCredential(publicKey: String): AuthsignalResponse<PushCredential> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/push?publicKey=$encodedKey"

    val response = client.get(url) {
      headers {
        append(HttpHeaders.Authorization, basicAuth)
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      val credentialResponse = response.body<CredentialResponse>()

      val data = PushCredential(
        credentialResponse.userAuthenticatorId,
        credentialResponse.verifiedAt,
        credentialResponse.lastVerifiedAt,
      )

      AuthsignalResponse(data = data)
    } else {
      val error = response.bodyAsText()

      AuthsignalResponse(error = error)
    }
  }

  suspend fun addCredential(
    token: String,
    publicKey: String,
    deviceName: String = ""): AuthsignalResponse<Boolean> {
    val url = "$baseURL/client/user-authenticators/push"
    val body = AddCredentialRequest(
      publicKey,
      deviceName,
      devicePlatform = "android",
    )

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)

      headers {
        append(HttpHeaders.Authorization, "Bearer $token")
      }
    }

    val success = response.status == HttpStatusCode.OK

    return if (success) {
      AuthsignalResponse(data = true)
    } else {
      val error = response.bodyAsText()

      Log.e(TAG, "Add credential request error: $error")

      AuthsignalResponse(error = error)
    }
  }

  suspend fun removeCredential(
    publicKey: String,
    signature: String,
  ): AuthsignalResponse<Boolean> {
    val url = "$baseURL/client/user-authenticators/push/remove"
    val body = RemoveCredentialRequest(publicKey, signature)

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
      val error = response.bodyAsText()

      Log.e(TAG, "Remove credential request error: $error")

      AuthsignalResponse(error = error)
    }
  }

  suspend fun getChallenge(publicKey: String): AuthsignalResponse<String> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/push/challenge?publicKey=$encodedKey"

    val response = client.get(url) {
      headers {
        append(HttpHeaders.Authorization, basicAuth)
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      val data = response.body<PushChallengeResponse>().challengeId

      AuthsignalResponse(data = data)
    } else {
      val error = response.bodyAsText()

      AuthsignalResponse(error = error)
    }
  }

  suspend fun updateChallenge(
    challengeId: String,
    publicKey: String,
    signature: String,
    approved: Boolean,
    verificationCode: String?
  ): AuthsignalResponse<Boolean> {
    val url = "$baseURL/client/user-authenticators/push/challenge"
    val body = UpdateChallengeRequest(
      publicKey,
      challengeId,
      signature,
      approved,
      verificationCode,
    )

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
      val error = response.bodyAsText()

      Log.e(TAG, "Update credential request error: $error")

      AuthsignalResponse(error = error)
    }
  }
}
