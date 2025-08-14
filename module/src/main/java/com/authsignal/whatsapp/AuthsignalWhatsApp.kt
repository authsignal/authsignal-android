package com.authsignal.whatsapp

import com.authsignal.TokenCache
import com.authsignal.whatsapp.api.WhatsAppAPI
import com.authsignal.models.AuthsignalResponse
import com.authsignal.models.ChallengeResponse
import com.authsignal.models.VerifyResponse

class AuthsignalWhatsApp(
  tenantID: String,
  baseURL: String
) {
  private val api = WhatsAppAPI(tenantID, baseURL)
  private val cache = TokenCache.shared

  suspend fun challenge(): AuthsignalResponse<ChallengeResponse> {
    val token = cache.token ?: return cache.handleTokenNotSetError()

    return api.challenge(token)
  }

  suspend fun verify(code: String): AuthsignalResponse<VerifyResponse> {
    val token = cache.token ?: return cache.handleTokenNotSetError()

    val verifyResponse = api.verify(token, code)

    verifyResponse.data?.token.let {
      cache.token = it
    }

    return verifyResponse
  }
}
