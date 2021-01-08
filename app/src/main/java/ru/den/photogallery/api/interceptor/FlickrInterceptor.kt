package ru.den.photogallery.api.interceptor

import okhttp3.Interceptor
import okhttp3.Response

private const val API_KEY = "3b814e6673b296d92ae6ce99c45f50f2"

class FlickrInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalHttpUrl = originalRequest.url

        val modifiedUrl = originalHttpUrl.newBuilder()
            .addQueryParameter("api_key", API_KEY)
            .addQueryParameter("format", "json")
            .addQueryParameter("nojsoncallback", "1")
            .addQueryParameter("extras", "url_s")
            .addQueryParameter("safesearch", "1")
            .build()

        val newRequest = originalRequest.newBuilder().url(modifiedUrl).build()

        return chain.proceed(newRequest)
    }
}
