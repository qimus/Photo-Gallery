package ru.den.photogallery.api.paging

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.den.photogallery.api.ApiService
import ru.den.photogallery.api.FlickrApi
import ru.den.photogallery.api.State
import ru.den.photogallery.api.model.FlickrResponse
import ru.den.photogallery.model.GalleryItem

class FlickrPagingSource(
    private val query: String = "",
    private val flickrApi: FlickrApi = ApiService.flickrApi
) : PageKeyedDataSource<Int, GalleryItem>() {
    private var _stateLiveData = MutableLiveData(State.LOADING)
    val stateLiveData: LiveData<State>
        get() = _stateLiveData

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, GalleryItem>
    ) {
        updateState(State.LOADING)
        handleCall(searchPhotos(1, params.requestedLoadSize)) { galleryItems ->
            if (galleryItems.isEmpty()) {
                updateState(State.EMPTY)
            }
            callback.onResult(galleryItems, null, 2)
        }
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, GalleryItem>
    ) {}

    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, GalleryItem>
    ) {
        handleCall(searchPhotos(params.key, params.requestedLoadSize)) { galleryItems ->
            val nextKey = if (galleryItems.isEmpty()) null else params.key + 1
            callback.onResult(galleryItems, nextKey)
        }
    }

    private fun handleCall(call: Call<FlickrResponse>, onSuccess: (List<GalleryItem>) -> Unit) {
        call.enqueue(object : Callback<FlickrResponse> {
            override fun onResponse(
                call: Call<FlickrResponse>,
                response: Response<FlickrResponse>
            ) {
                if (response.isSuccessful) {
                    val flickrResponse = response.body()
                    var galleryItems = flickrResponse?.photos?.galleryItems ?: mutableListOf()
                    galleryItems = galleryItems.filter { it.url.isNotBlank() }
                    updateState(State.DONE)
                    onSuccess(galleryItems)
                } else {
                    updateState(State.FAIL)
                }
            }

            override fun onFailure(call: Call<FlickrResponse>, t: Throwable) {
                updateState(State.FAIL)
            }
        })
    }

    private fun updateState(newState: State) {
        _stateLiveData.postValue(newState)
    }

    private fun searchPhotos(page: Int, perPage: Int): Call<FlickrResponse> {
        return if (query.isEmpty()) {
            flickrApi.fetchPhotos(page, perPage)
        } else {
            flickrApi.searchPhotos(query, page, perPage)
        }
    }
}
