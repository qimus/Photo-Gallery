package ru.den.photogallery.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import ru.den.photogallery.api.model.FlickrResponse

interface FlickrApi {
    @GET("services/rest/?method=flickr.interestingness.getList")
    fun fetchPhotos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): Call<FlickrResponse>

    @GET
    fun fetchUrlBytes(@Url url: String): Call<ResponseBody>

    @GET("services/rest?method=flickr.photos.search")
    fun searchPhotos(
        @Query("text") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): Call<FlickrResponse>
}
