package com.authsignal.device.api

import com.authsignal.APIError
import com.authsignal.Encoder
import com.authsignal.models.AuthsignalResponse
import com.authsignal.device.api.models.*
import com.authsignal.device.models.DeviceCredential
import com.authsignal.models.ChallengeResponse
import com.authsignal.passkey.api.models.ChallengeRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class DeviceAPI(tenantID: String, private val baseURL: String) {
  private val client = HttpClient(Android) {
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
      })
    }
  }

  private val basicAuth = "Basic ${Encoder.toBase64String("$tenantID:".toByteArray())}"

  suspend fun challenge(): AuthsignalResponse<ChallengeResponse> {
    val url = "$baseURL/client/challenge"

    return try {
      val response = client.post(url) {
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

  suspend fun getCredential(publicKey: String): AuthsignalResponse<DeviceCredential> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/device?publicKey=$encodedKey"

    return try {
      val response = client.get(url) {
        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val credentialResponse = response.body<CredentialResponse>()

        val data = DeviceCredential(
          credentialResponse.userAuthenticatorId,
          credentialResponse.verifiedAt,
          credentialResponse.lastVerifiedAt,
        )

        AuthsignalResponse(data = data)
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
    deviceName: String = ""): AuthsignalResponse<Boolean> {
    val url = "$baseURL/client/user-authenticators/device"
    val body = AddCredentialRequest(
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

      val success = response.status == HttpStatusCode.OK

      if (success) {
        AuthsignalResponse(data = true)
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
    val url = "$baseURL/client/user-authenticators/device/remove"
    val body = RemoveCredentialRequest(publicKey, signature)

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

  suspend fun getChallenge(publicKey: String): AuthsignalResponse<DeviceChallengeResponse> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/device/challenge?publicKey=$encodedKey"

    return try {
      val response = client.get(url) {
        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val data = response.body<DeviceChallengeResponse>()

        AuthsignalResponse(data = data)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun claimChallenge(
    challengeId: String,
    publicKey: String,
    signature: String,
  ): AuthsignalResponse<ClaimChallengeResponse> {
    val url = "$baseURL/client/user-authenticators/device/challenge/claim"
    val body = ClaimChallengeRequest(
      publicKey,
      challengeId,
      signature,
    )

    return try {
      val response = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)

        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val data = response.body<ClaimChallengeResponse>()
        AuthsignalResponse(data = data)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun updateChallenge(
    challengeId: String,
    publicKey: String,
    signature: String,
    approved: Boolean,
    verificationCode: String?
  ): AuthsignalResponse<Boolean> {
    val url = "$baseURL/client/user-authenticators/device/challenge"
    val body = UpdateChallengeRequest(
      publicKey,
      challengeId,
      signature,
      approved,
      verificationCode,
    )

    return try {
      val response = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)

        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      val success = response.status == HttpStatusCode.OK

      if (success) {
        AuthsignalResponse(data = true)
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
  ): AuthsignalResponse<VerifyDeviceResponse> {
    val url = "$baseURL/client/verify/device"
    val body = VerifyDeviceRequest(
      challengeId,
      publicKey,
      signature,
    )

    return try {
      val response = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)

        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      val success = response.status == HttpStatusCode.OK

      if (success) {
        val data = response.body<VerifyDeviceResponse>()
        AuthsignalResponse(data = data)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }
}
