package org.openedx.app.data.networking

import org.openedx.core.ApiConstants
import org.openedx.core.data.storage.PreferencesManager
import okhttp3.Interceptor
import okhttp3.Response

class HeadersInterceptor(private val preferencesManager: PreferencesManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder().apply {
                    val token = preferencesManager.accessToken

                    if (token.isNotEmpty()) {
                        addHeader("Authorization", "${ApiConstants.TOKEN_TYPE_BEARER} $token")
                    }

                    addHeader("Accept", "application/json")
                }.build()
        )
    }
}