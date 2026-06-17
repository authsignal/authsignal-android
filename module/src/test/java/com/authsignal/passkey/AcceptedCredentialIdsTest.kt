package com.authsignal.passkey

import com.authsignal.passkey.api.models.Authenticator
import com.authsignal.passkey.api.models.WebauthnCredential
import org.junit.Assert.assertEquals
import org.junit.Test

class AcceptedCredentialIdsTest {
  private fun passkey(credentialId: String) = Authenticator(
    userAuthenticatorId = "ua_$credentialId",
    verificationMethod = "PASSKEY",
    webauthnCredential = WebauthnCredential(credentialId = credentialId),
  )

  @Test
  fun includesAllServerPasskeysAndCurrentCredential() {
    val authenticators = listOf(passkey("cred-a"), passkey("cred-b"))

    val result = buildAcceptedCredentialIds(authenticators, "cred-c")

    assertEquals(listOf("cred-a", "cred-b", "cred-c"), result)
  }

  @Test
  fun doesNotDuplicateCurrentCredentialWhenAlreadyPresent() {
    val authenticators = listOf(passkey("cred-a"), passkey("cred-b"))

    val result = buildAcceptedCredentialIds(authenticators, "cred-a")

    assertEquals(listOf("cred-a", "cred-b"), result)
  }

  @Test
  fun ignoresNonPasskeyAuthenticators() {
    val authenticators = listOf(
      passkey("cred-a"),
      Authenticator(verificationMethod = "SMS", webauthnCredential = null),
      Authenticator(verificationMethod = "AUTHENTICATOR_APP", webauthnCredential = null),
    )

    val result = buildAcceptedCredentialIds(authenticators, "cred-a")

    assertEquals(listOf("cred-a"), result)
  }

  @Test
  fun ignoresPasskeysMissingAWebauthnCredential() {
    val authenticators = listOf(
      passkey("cred-a"),
      Authenticator(verificationMethod = "PASSKEY", webauthnCredential = null),
    )

    val result = buildAcceptedCredentialIds(authenticators, "cred-b")

    assertEquals(listOf("cred-a", "cred-b"), result)
  }

  @Test
  fun returnsOnlyCurrentCredentialWhenServerListIsEmpty() {
    val result = buildAcceptedCredentialIds(emptyList(), "cred-a")

    assertEquals(listOf("cred-a"), result)
  }
}
