package com.projectinsight.smartdocumentscanner.util

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class TokenInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = PreferencesManager.getJwtToken(context)
        println("Current token: ${token?.take(10)}...") // Log first 10 chars

        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
                println("Added Authorization header")
            }
        }.build()

        println("Request to: ${request.url}")
        val response = chain.proceed(request)
        println("Response code: ${response.code}")

        if (response.code == 403) {
            println("403 Forbidden - Token details:")
            println("Token exists: ${token}")
            println("Token exists: ${!token.isNullOrBlank()}")
            println("Token length: ${token?.length}")

        }

        return response
    }
}
