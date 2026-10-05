package com.fourdfit.app.data.api

import com.fourdfit.app.data.local.SecureTokenStore
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiFactory {
    fun create(
        baseUrl: String,
        tokenStore: SecureTokenStore,
        debug: Boolean,
        gson: Gson,
    ): FitApi {
        require(debug || baseUrl.startsWith("https://")) { "Release builds must use an HTTPS API base URL" }
        require(baseUrl.endsWith("/")) { "API base URL must end with '/'" }

        val logging =
            HttpLoggingInterceptor().apply {
                // BASIC never logs headers or bodies, so tokens and passwords stay out of logcat.
                level = if (debug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(AuthInterceptor(tokenStore))
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                // Optional hardening: add certificate pinning for your production host, e.g.
                // .certificatePinner(CertificatePinner.Builder().add("api.your-domain.com", "sha256/...").build())
                .build()

        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(FitApi::class.java)
    }
}

/** Adds the bearer token (read from encrypted storage) to every request. */
class AuthInterceptor(
    private val tokenStore: SecureTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.currentToken
        val request =
            if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain
                    .request()
                    .newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
        return chain.proceed(request)
    }
}
