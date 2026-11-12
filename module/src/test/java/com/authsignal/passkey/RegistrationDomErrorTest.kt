package com.authsignal.passkey

import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import com.authsignal.SdkErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrationDomErrorTest {
  private fun invalidState(message: String?) =
    registrationDomErrorResponse(CreatePublicKeyCredentialDomException(InvalidStateError(), message))

  @Test
  fun googlePasswordManagerMessageIsMatchedExcludedCredential() {
    val response = invalidState("One of the excluded credentials exists on the local device")

    assertEquals(SdkErrorCodes.MatchedExcludedCredential, response.errorCode)
    assertEquals("One of the excluded credentials exists on the local device", response.error)
  }

  @Test
  fun localisedProviderMessageIsMatchedExcludedCredential() {
    val response = invalidState("Vous avez déjà une clé d'accès pour Moneygram.")

    assertEquals(SdkErrorCodes.MatchedExcludedCredential, response.errorCode)
    assertEquals("Vous avez déjà une clé d'accès pour Moneygram.", response.error)
  }

  @Test
  fun otherEnglishMessageIsMatchedExcludedCredential() {
    val response = invalidState(
      "The user attempted to register an authenticator that contains one of the credentials already registered with the relying party."
    )

    assertEquals(SdkErrorCodes.MatchedExcludedCredential, response.errorCode)
  }

  @Test
  fun genericMessageIsMatchedExcludedCredential() {
    val response = invalidState("An unexpected error has occurred. Try again later.")

    assertEquals(SdkErrorCodes.MatchedExcludedCredential, response.errorCode)
    assertEquals("An unexpected error has occurred. Try again later.", response.error)
  }

  @Test
  fun missingMessageFallsBackToDefault() {
    val response = invalidState(null)

    assertEquals(SdkErrorCodes.MatchedExcludedCredential, response.errorCode)
    assertEquals("An existing credential is already available for this device.", response.error)
  }

  @Test
  fun otherDomErrorsAreNotMatchedExcludedCredential() {
    val response = registrationDomErrorResponse(
      CreatePublicKeyCredentialDomException(NotAllowedError(), "Not allowed")
    )

    assertEquals(SdkErrorCodes.DomError, response.errorCode)
  }
}
