package com.authsignal.device

import com.authsignal.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DeviceAPI() {
  private val baseUrl = BuildConfig.BASE_URL

  private val client = HttpClient(Android) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
      })
    }
  }

  suspend fun addCredential(accessToken: String, publicKey: String): Boolean {
    val url = "$baseUrl/add-credential"
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
    val url = "$baseUrl/remove-credential"
    val body = RemoveCredentialRequest(publicKey, signature)

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)
    }

    return response.status == HttpStatusCode.OK
  }

  suspend fun getChallenge(publicKey: String): String? {
    val url = "$baseUrl/get-challenge"
    val body = GetChallengeRequest(publicKey)

    val response = client.post(url) {
      contentType(ContentType.Application.Json)
      setBody(body)
    }

    return if (response.status == HttpStatusCode.OK) {
      response.body<GetChallengeResponse>().sessionToken
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
    val url = "$baseUrl/update-challenge"
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
data class RemoveCredentialRequest(val publicKey: String, val signature: String)

@Serializable
data class GetChallengeRequest(val publicKey: String)

@Serializable
data class GetChallengeResponse(val sessionToken: String? = null)

@Serializable
data class UpdateChallengeRequest(
  val publicKey: String,
  val sessionToken: String,
  val signature: String,
  val approved: Boolean)