package com.example.patheaseapp.util

import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Converts technical exceptions into professional, user-friendly messages
 * that help the user understand what happened and how to fix it.
 */
fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is UnknownHostException -> {
            "Unable to connect. Please check your internet connection and try again."
        }
        is ConnectException, is SocketTimeoutException, is HttpRequestTimeoutException -> {
            "Connection timed out. Our servers might be busy, please try again in a moment."
        }
        is RestException -> {
            val errorBody = this.message ?: ""
            when {
                // Authentication Errors
                errorBody.contains("invalid_credentials", ignoreCase = true) ->
                    "The email or password you entered is incorrect. Please try again."

                errorBody.contains("user_not_found", ignoreCase = true) ->
                    "We couldn't find an account with that email. Please check your spelling or sign up."

                errorBody.contains("email_not_confirmed", ignoreCase = true) ->
                    "Please verify your email address. We sent a confirmation link to your inbox."

                errorBody.contains("weak_password", ignoreCase = true) ->
                    "Your password is too simple. For your security, please use a stronger one."

                errorBody.contains("over_email_send_rate_limit", ignoreCase = true) ->
                    "Slow down! You've requested too many emails. Please wait a few minutes."

                errorBody.contains("user_already_exists", ignoreCase = true) ->
                    "An account with this email already exists. Try signing in instead."

                errorBody.contains("invalid_grant", ignoreCase = true) ->
                    "This link is invalid or has expired. Please request a new one."

                errorBody.contains("bad_jwt", ignoreCase = true) || errorBody.contains("missing sub claim", ignoreCase = true) ->
                    "Your session has expired. Please sign in again."

                errorBody.contains("unexpected_failure", ignoreCase = true) ->
                    "We encountered an error while processing your request. Please try again later."

                // Database/API Errors
                errorBody.contains("duplicate key", ignoreCase = true) ->
                    "This item already exists in your collection."

                errorBody.contains("permission denied", ignoreCase = true) ->
                    "You don't have permission to perform this action."

                else -> "Oops! Something went wrong on our end. We're working on fixing it."
            }
        }
        else -> {
            val msg = this.message?.lowercase() ?: ""
            when {
                msg.contains("timeout") -> "The request took too long. Please check your connection."
                msg.contains("network") -> "A network error occurred. Please verify your connection."
                msg.contains("gps") || msg.contains("location") -> "We're having trouble accessing your location. Please check your GPS settings."
                else -> "An unexpected error occurred. Please try again."
            }
        }
    }
}
