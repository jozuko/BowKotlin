package com.studio.jozu.bow.infrastructure.repository

import com.studio.jozu.bow.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class BowApiHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request()
                .newBuilder()
                .header("x-api-key", BuildConfig.API_KEY)
                .build()
        )
    }
}