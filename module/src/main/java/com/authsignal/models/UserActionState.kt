package com.authsignal.models

object UserActionState {
    const val ALLOW = "ALLOW"
    const val BLOCK = "BLOCK"
    const val CHALLENGE_REQUIRED = "CHALLENGE_REQUIRED"
    const val CHALLENGE_FAILED = "CHALLENGE_FAILED"
    const val CHALLENGE_SUCCEEDED = "CHALLENGE_SUCCEEDED"
    const val REVIEW_REQUIRED = "REVIEW_REQUIRED"
    const val REVIEW_FAILED = "REVIEW_FAILED"
    const val REVIEW_SUCCEEDED = "REVIEW_SUCCEEDED"
}