package com.authsignal.push

import com.authsignal.push.models.PushCredential
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

  suspend fun getCredential(publicKey: String): PushCredential? {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/user-authenticators/push?publicKey=$encodedKey"

    val response = client.get(url) {
      headers {
        append(HttpHeaders.Authorization, basicAuth)
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      val credentialResponse = response.body<CredentialResponse>()

      PushCredential(
        credentialResponse.userAuthenticatorId,
        credentialResponse.verifiedAt,
        credentialResponse.lastVerifiedAt,
      )
    } else {
      null
    }
  }

  suspend fun addCredential(
    token: String,
    publicKey: String,
    deviceName: String = ""): Boolean {
    val url = "$baseURL/user-authenticators/push"
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

    return response.status == HttpStatusCode.OK
  }

  suspend fun removeCredential(publicKey: String, signature: String): Boolean {
    val url = "$baseURL/user-authenticators/push/remove"
    val body = RemoveCredentialRequest(publicKey, signature)

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)

      headers {
        append(HttpHeaders.Authorization, basicAuth)
      }
    }

    return response.status == HttpStatusCode.OK
  }

  suspend fun getChallenge(publicKey: String): String? {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/user-authenticators/push/challenge?publicKey=$encodedKey"

    val response = client.get(url) {
      headers {
        append(HttpHeaders.Authorization, basicAuth)
      }
    }

    return if (response.status == HttpStatusCode.OK) {
      response.body<ChallengeResponse>().challengeId
    } else {
      null
    }
  }

  suspend fun updateChallenge(
    challengeId: String,
    publicKey: String,
    signature: String,
    approved: Boolean,
    verificationCode: String?
  ): Boolean {
    val url = "$baseURL/user-authenticators/push/challenge"
    val body = UpdateChallengeRequest(
      publicKey,
      challengeId,
      signature,
      approved,
      verificationCode)

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)

      headers {
        append(HttpHeaders.Authorization, basicAuth)
      }
    }

    return response.status == HttpStatusCode.OK
  }
}

@Serializable
data class AddCredentialRequest(
  val publicKey: String,
  val deviceName: String,
  val devicePlatform: String,
)

@Serializable
data class ChallengeResponse(val challengeId: String? = null)

@Serializable
data class CredentialResponse(
  val userAuthenticatorId: String,
  val verifiedAt: String,
  val lastVerifiedAt: String? = null)

@Serializable
data class RemoveCredentialRequest(
  val publicKey: String,
  val signature: String)

@Serializable
data class UpdateChallengeRequest(
  val publicKey: String,
  val challengeId: String,
  val signature: String,
  val approved: Boolean,
  val verificationCode: String?)