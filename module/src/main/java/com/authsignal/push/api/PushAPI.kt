package com.authsignal.push.api

import com.authsignal.APIError
import com.authsignal.BaseAPI
import com.authsignal.Encoder
import com.authsignal.models.*
import com.authsignal.models.api.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class PushAPI(tenantID: String, baseURL: String) : BaseAPI(tenantID, baseURL) {

  suspend fun getCredential(publicKey: String): AuthsignalResponse<AppCredential> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/push?publicKey=$encodedKey"

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
    deviceName: String = "",
    deviceIntegrityToken: String? = null,
    pushToken: String? = null): AuthsignalResponse<AppCredential> {
    val url = "$baseURL/client/user-authenticators/push"
    val body = AddAppCredentialRequest(
      publicKey,
      deviceName,
      devicePlatform = "android",
      performAttestation = deviceIntegrityToken?.let {
        AddAppCredentialDeviceIntegrity(provider = "PLAY_INTEGRITY", token = it)
      },
      pushToken = pushToken,
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
    val url = "$baseURL/client/user-authenticators/push/remove"
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

  suspend fun getChallengeSignNonce(publicKey: String): AuthsignalResponse<AppChallengeSignResponse> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/push/challenge/sign?publicKey=$encodedKey"

    return try {
      val response = client.post(url) {
        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val data = response.body<AppChallengeSignResponse>()

        AuthsignalResponse(data = data)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun getChallenge(publicKey: String, signature: String? = null): AuthsignalResponse<AppChallenge?> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/push/challenge?publicKey=$encodedKey"

    return try {
      val response = client.get(url) {
        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }

        if (signature != null) {
          header("X-Authsignal-Signature", signature)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val data = response.body<AppChallengeResponse>()

        val challengeId = data.challengeId ?: return AuthsignalResponse(data = null)
        val userId = data.userId ?: return AuthsignalResponse(data = null)

        val appChallenge =  AppChallenge(
          challengeId = challengeId,
          userId = userId,
          actionCode = data.actionCode,
          idempotencyKey = data.idempotencyKey,
          ipAddress = data.ipAddress,
          userAgent = data.userAgent,
          deviceId = data.deviceId,
          custom = data.custom,
          user = data.user,
        )

        return AuthsignalResponse(data = appChallenge)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun getSigningMessage(publicKey: String): AuthsignalResponse<AppUpdateSignResponse> {
    val encodedKey = Encoder.toBase64String(publicKey.toByteArray())
    val url = "$baseURL/client/user-authenticators/push/update/sign?publicKey=$encodedKey"

    return try {
      val response = client.post(url) {
        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        val data = response.body<AppUpdateSignResponse>()

        AuthsignalResponse(data = data)
      } else {
        APIError.mapToErrorResponse(response)
      }
    } catch (e: Exception) {
      APIError.handleNetworkException(e)
    }
  }

  suspend fun updateCredential(
    challengeId: String,
    publicKey: String,
    signature: String,
    pushToken: String,
  ): AuthsignalResponse<UpdateAppCredentialResponse> {
    val url = "$baseURL/client/user-authenticators/push"
    val body = UpdateAppCredentialRequest(challengeId, publicKey, signature, pushToken)

    return try {
      val response = client.patch(url) {
        contentType(ContentType.Application.Json)
        setBody(body)

        headers {
          append(HttpHeaders.Authorization, basicAuth)
        }
      }

      if (response.status == HttpStatusCode.OK) {
        AuthsignalResponse(data = response.body<UpdateAppCredentialResponse>())
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
    val url = "$baseURL/client/user-authenticators/push/challenge"
    val body = UpdateAppChallengeRequest(
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
}
