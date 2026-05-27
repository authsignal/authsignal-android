package com.authsignal

data class AuthsignalWrapperSDKMetadata(
  val sdk: String,
  val version: String,
  val userAgentToken: String,
)

object AuthsignalRequestMetadata {
  private const val nativeSDK = "android"
  private const val nativeUserAgentToken = "AuthsignalAndroidSDK"

  @Volatile
  private var wrapperSDKMetadata: AuthsignalWrapperSDKMetadata? = null

  fun setWrapperSDK(sdk: String, version: String, userAgentToken: String) {
    wrapperSDKMetadata = AuthsignalWrapperSDKMetadata(sdk, version, userAgentToken)
  }

  fun clearWrapperSDK() {
    wrapperSDKMetadata = null
  }

  fun headers(tenantID: String?): Map<String, String> {
    val wrapper = wrapperSDKMetadata
    val headers = mutableMapOf(
      "X-Authsignal-SDK" to (wrapper?.sdk ?: nativeSDK),
      "X-Authsignal-Version" to (wrapper?.version ?: BuildConfig.VERSION_NAME),
    )

    tenantID?.takeIf { it.isNotBlank() }?.let {
      headers["X-Authsignal-Tenant-ID"] = it
    }

    if (wrapper != null) {
      headers["X-Authsignal-Native-SDK"] = nativeSDK
      headers["X-Authsignal-Native-Version"] = BuildConfig.VERSION_NAME
    }

    return headers
  }

  fun userAgentProductTokens(): String {
    val nativeProduct = "$nativeUserAgentToken/${BuildConfig.VERSION_NAME}"

    return wrapperSDKMetadata
      ?.let { "${it.userAgentToken}/${it.version} $nativeProduct" }
      ?: nativeProduct
  }
}
