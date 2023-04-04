package com.authsignal.device

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ChallengeAPI(private val baseURL: String) {
  private val client = HttpClient(Android) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
      })
    }
  }

  suspend fun addCredential(accessToken: String, publicKey: String): Boolean {
    val url = "$baseURL/device/add-credential"
    val body = AddCredentialRequest(publicKey)

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)

      headers {
        append(HttpHeaders.Authorization, "Bearer $accessToken")
      }
    }

    return response.status == HttpStatusCode.OK
  }

  suspend fun removeCredential(publicKey: String, signature: String): Boolean {
    val url = "$baseURL/device/remove-credential"
    val body = RemoveCredentialRequest(publicKey, signature)

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)
    }

    return response.status == HttpStatusCode.OK
  }

  suspend fun getChallenge(publicKey: String): String? {
    val url = "$baseURL/device/check-challenge"
    val body = ChallengeRequest(publicKey)

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)
    }

    return if (response.status == HttpStatusCode.OK) {
      response.body<ChallengeResponse>().sessionToken
    } else {
      null;
    }
  }

  suspend fun updateChallenge(
    challengeId: String,
    publicKey: String,
    signature: String,
    approved: Boolean
  ): Boolean {
    val url = "$baseURL/device/update-challenge"
    val body = UpdateChallengeRequest(
      publicKey,
      challengeId,
      signature,
      approved)

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)
    }

    return response.status == HttpStatusCode.OK
  }
}

@Serializable
data class AddCredentialRequest(val publicKey: String)

@Serializable
data class RemoveCredentialRequest(
  val publicKey: String,
  val signature: String)

@Serializable
data class ChallengeRequest(val publicKey: String)

@Serializable
data class ChallengeResponse(val sessionToken: String? = null)

@Serializable
data class UpdateChallengeRequest(
  val publicKey: String,
  val sessionToken: String,
  val signature: String,
  val approved: Boolean)

enum class AuthsignalRegion {
  US, AU, EU
}