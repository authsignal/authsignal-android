package com.authsignal.totp

import com.authsignal.AuthsignalBase
import com.authsignal.totp.api.TOTPAPI
import com.authsignal.models.AuthsignalResponse
import com.authsignal.totp.api.models.EnrollTOTPResponse
import com.authsignal.models.VerifyResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class AuthsignalTOTP(
  tenantID: String,
  baseURL: String): AuthsignalBase() {
  private val api = TOTPAPI(tenantID, baseURL)

  suspend fun enroll(): AuthsignalResponse<EnrollTOTPResponse> {
    val token = this.token ?: return handleTokenNotSetError()

    return api.enroll(token)
  }

  suspend fun verify(code: String): AuthsignalResponse<VerifyResponse> {
    val token = this.token ?: return handleTokenNotSetError()

    return api.verify(token, code)
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun enrollAsync(): CompletableFuture<AuthsignalResponse<EnrollTOTPResponse>> =
    GlobalScope.future { enroll() }

  @OptIn(DelicateCoroutinesApi::class)
  fun verifyAsync(code: String): CompletableFuture<AuthsignalResponse<VerifyResponse>> =
    GlobalScope.future { verify(code) }
}