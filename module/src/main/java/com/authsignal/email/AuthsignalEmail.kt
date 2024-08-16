package com.authsignal.email

import com.authsignal.AuthsignalBase
import com.authsignal.email.api.EmailAPI
import com.authsignal.models.AuthsignalResponse
import com.authsignal.models.ChallengeResponse
import com.authsignal.models.EnrollResponse
import com.authsignal.models.VerifyResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class AuthsignalEmail(
  tenantID: String,
  baseURL: String): AuthsignalBase() {
  private val api = EmailAPI(tenantID, baseURL)

  suspend fun enroll(email: String): AuthsignalResponse<EnrollResponse> {
    val token = this.token ?: return handleTokenNotSetError()

    return api.enroll(token, email)
  }

  suspend fun challenge(): AuthsignalResponse<ChallengeResponse> {
    val token = this.token ?: return handleTokenNotSetError()

    return api.challenge(token)
  }

  suspend fun verify(code: String): AuthsignalResponse<VerifyResponse> {
    val token = this.token ?: return handleTokenNotSetError()

    return api.verify(token, code)
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun enrollAsync(email: String): CompletableFuture<AuthsignalResponse<EnrollResponse>> =
    GlobalScope.future { enroll(email) }

  @OptIn(DelicateCoroutinesApi::class)
  fun challengeAsync(): CompletableFuture<AuthsignalResponse<ChallengeResponse>> =
    GlobalScope.future { challenge() }

  @OptIn(DelicateCoroutinesApi::class)
  fun verifyAsync(code: String): CompletableFuture<AuthsignalResponse<VerifyResponse>> =
    GlobalScope.future { verify(code) }
}