package com.authsignal.totp

import com.authsignal.TokenCache
import com.authsignal.totp.api.TOTPAPI
import com.authsignal.models.AuthsignalResponse
import com.authsignal.totp.api.models.EnrollTOTPResponse
import com.authsignal.models.VerifyResponse

class AuthsignalTOTP(
  tenantID: String,
  baseURL: String
) {
  private val api = TOTPAPI(tenantID, baseURL)
  private val cache = TokenCache.shared

  suspend fun enroll(): AuthsignalResponse<EnrollTOTPResponse> {
    val token = cache.token ?: return cache.handleTokenNotSetError()

    return api.enroll(token)
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