package ru.den.photogallery.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.den.photogallery.api.interceptor.FlickrInterceptor

private const val BASE_URL = "https://api.flickr.com"

object ApiService {
    private val retrofit: Retrofit

    init {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(FlickrInterceptor())
            .addInterceptor(loggingInterceptor)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val flickrApi: FlickrApi by lazy { createService() }

    private inline fun <reified T> createService(): T {
        return retrofit.create(T::class.java)
    }
}
