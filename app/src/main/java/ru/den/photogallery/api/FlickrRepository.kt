package ru.den.photogallery.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.den.photogallery.api.model.FlickrResponse
import ru.den.photogallery.model.GalleryItem

class FlickrRepository {
    private val flickrApi = ApiService.flickrApi

    fun fetchPhotosRequest(): Call<FlickrResponse> {
        return flickrApi.fetchPhotos()
    }

    fun fetchPhotos(): LiveData<List<GalleryItem>> =
        fetchPhotoMetadata(fetchPhotosRequest())

    fun searchPhotosRequest(query: String): Call<FlickrResponse> {
        return flickrApi.searchPhotos(query)
    }

    fun searchPhotos(query: String): LiveData<List<GalleryItem>> =
        fetchPhotoMetadata(searchPhotosRequest(query))

    @WorkerThread
    fun fetchPhoto(url: String): Bitmap? {
        val response = flickrApi.fetchUrlBytes(url).execute()
        val bitmap = response.body()?.byteStream()?.use(BitmapFactory::decodeStream)
        return bitmap
    }

    private fun fetchPhotoMetadata(flickrRequest: Call<FlickrResponse>): LiveData<List<GalleryItem>> {
        val responseLiveData = MutableLiveData<List<GalleryItem>>()
        flickrRequest.enqueue(object : Callback<FlickrResponse> {
            override fun onResponse(call: Call<FlickrResponse>, response: Response<FlickrResponse>) {
                Log.d(TAG, "Response received")
                val flickrResponse = response.body()
                var galleryItems = flickrResponse?.photos?.galleryItems ?: mutableListOf()
                galleryItems = galleryItems.filter { it.url.isNotBlank() }
                responseLiveData.postValue(galleryItems)
            }

            override fun onFailure(call: Call<FlickrResponse>, t: Throwable) {
                Log.e(TAG, "Failed to fetch photos", t)
            }
        })

        return responseLiveData
    }

    companion object {
        private const val TAG = "FlickrRepository"
    }
}
