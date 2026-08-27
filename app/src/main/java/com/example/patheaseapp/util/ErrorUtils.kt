package com.example.patheaseapp.util

import io.github.jan.supabase.exceptions.RestException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Converts technical exceptions into natural, user-friendly messages
 * similar to high-quality professional applications.
 */
fun Exception.toUserFriendlyMessage(): String {
    return when (this) {
        is UnknownHostException -> {
            "No internet connection. Please check your settings and try again."
        }
        is ConnectException, is SocketTimeoutException -> {
            "We're having trouble reaching our servers. Please try again in a moment."
        }
        is RestException -> {
            val errorBody = this.message ?: ""
            when {
                // Auth Errors
                errorBody.contains("invalid_credentials", ignoreCase = true) ->
                    "Incorrect email or password. Please try again."

                errorBody.contains("user_not_found", ignoreCase = true) ->
                    "We couldn't find an account with that email address."

                errorBody.contains("email_not_confirmed", ignoreCase = true) ->
                    "Please check your inbox and verify your email address to continue."

                errorBody.contains("weak_password", ignoreCase = true) ->
                    "That password is too easy to guess. Please use a stronger one."

                errorBody.contains("over_email_send_rate_limit", ignoreCase = true) ->
                    "You've requested too many links. Please wait a few minutes before trying again."

                errorBody.contains("user_already_exists", ignoreCase = true) ->
                    "An account with this email already exists. Try signing in instead."

                errorBody.contains("invalid_grant", ignoreCase = true) ->
                    "This link is invalid or has expired. Please request a new one."

                errorBody.contains("bad_jwt", ignoreCase = true) || errorBody.contains("missing sub claim", ignoreCase = true) ->
                    "Your session has expired. Please open the link from your email again."

                errorBody.contains("unexpected_failure", ignoreCase = true) || errorBody.contains("email_provider_disabled", ignoreCase = true) ->
                    "Registration is currently unavailable. Please try again later."

                // General API Errors
                else -> "Something went wrong on our end. Please try again later."
            }
        }
        else -> {
            val msg = this.message?.lowercase() ?: ""
            when {
                msg.contains("timeout") -> "The request timed out. Please check your connection."
                msg.contains("network") -> "A network error occurred. Please try again."
                else -> "An unexpected error occurred. Please try again."
            }
        }
    }
}
