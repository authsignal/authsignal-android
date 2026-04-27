package com.authsignal

import com.authsignal.models.AuthsignalResponse
import com.authsignal.models.ChallengeResponse
import com.authsignal.models.api.ChallengeRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

abstract class BaseAPI(tenantID: String, protected val baseURL: String) {
  protected val client = HttpClientFactory.create()

  protected val basicAuth = "Basic ${Encoder.toBase64String("$tenantID:".toByteArray())}"

  suspend fun challenge(
    action: String? = null,
    token: String? = null,
  ): AuthsignalResponse<ChallengeResponse> {
    val url = "$baseURL/client/challenge"
    val body = ChallengeRequest(action = action)

    return try {
      val response = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(body)

        headers {
          append(
            HttpHeaders.Authorization,
            if (token == null) basicAuth else "Bearer $token",
          )
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
}
