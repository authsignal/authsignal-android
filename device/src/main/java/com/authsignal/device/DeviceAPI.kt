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

class DeviceAPI(private val tenantId: String) {
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
    val response = client.post("$baseUrl/add-credential") {
      contentType(ContentType.Application.Json)

      headers {
        append(HttpHeaders.Authorization, "Bearer $accessToken")
      }

      setBody(AddCredentialRequest(publicKey))
    }

    return response.status == HttpStatusCode.OK
  }

  suspend fun removeCredential(publicKey: String, signature: String): Boolean {
    val response = client.post("$baseUrl/remove-credential") {
      contentType(ContentType.Application.Json)
      setBody(RemoveCredentialRequest(tenantId, publicKey, signature))
    }

    return response.status == HttpStatusCode.OK
  }

  suspend fun getChallenge(publicKey: String): String? {
    val response = client.post("$baseUrl/get-challenge") {
      contentType(ContentType.Application.Json)
      setBody(GetChallengeRequest(tenantId, publicKey))
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
    val response = client.post("$baseUrl/update-challenge") {
      contentType(ContentType.Application.Json)
      setBody(UpdateChallengeRequest(tenantId, publicKey, challengeId, signature, approved))
    }

    return response.status == HttpStatusCode.OK
  }
}

@Serializable
data class AddCredentialRequest(val publicKey: String)

@Serializable
data class RemoveCredentialRequest(val tenantId: String, val publicKey: String, val signature: String)

@Serializable
data class GetChallengeRequest(val tenantId: String, val publicKey: String)

@Serializable
data class GetChallengeResponse(val sessionToken: String? = null)

@Serializable
data class UpdateChallengeRequest(
  val tenantId: String,
  val publicKey: String,
  val sessionToken: String,
  val signature: String,
  val approved: Boolean)