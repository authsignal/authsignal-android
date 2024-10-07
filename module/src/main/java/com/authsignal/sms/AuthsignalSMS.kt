package com.authsignal.sms

import com.authsignal.TokenCache
import com.authsignal.sms.api.SMSAPI
import com.authsignal.models.AuthsignalResponse
import com.authsignal.models.ChallengeResponse
import com.authsignal.models.EnrollResponse
import com.authsignal.models.VerifyResponse

class AuthsignalSMS(
  tenantID: String,
  baseURL: String
) {
  private val api = SMSAPI(tenantID, baseURL)
  private val cache = TokenCache.shared

  suspend fun enroll(phoneNumber: String): AuthsignalResponse<EnrollResponse> {
    val token = cache.token ?: return cache.handleTokenNotSetError()

    return api.enroll(token, phoneNumber)
  }

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