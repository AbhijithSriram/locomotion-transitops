package com.transitops.driver.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor to automatically add the JWT token to our requests.
 * We skip adding it to /auth/login and /auth/refresh as per the contract.
 */
class AuthInterceptor(private val getToken: () -> String?) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Do not add token for login or refresh
        if (path.contains("/auth/login") || path.contains("/auth/refresh")) {
            return chain.proceed(request)
        }

        val token = getToken()
        if (token != null) {
            val newRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            return chain.proceed(newRequest)
        }

        return chain.proceed(request)
    }
}
