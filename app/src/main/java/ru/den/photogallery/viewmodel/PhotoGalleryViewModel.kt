package ru.den.photogallery.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import ru.den.photogallery.QueryPreferences
import ru.den.photogallery.api.State
import ru.den.photogallery.api.paging.FlickrDataSourceFactory
import ru.den.photogallery.model.GalleryItem

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {
    private var queryLiveData = MutableLiveData(QueryPreferences.getStoredQuery(app))
    val galleryItemLiveData: LiveData<PagedList<GalleryItem>>
    val searchTerm: String
        get() = queryLiveData.value ?: ""

    val stateLiveData: LiveData<State>

    private val flickrDataSourceFactory = FlickrDataSourceFactory(QueryPreferences.getStoredQuery(app))

    init {
        val config = PagedList.Config.Builder()
            .setPageSize(100)
            .setInitialLoadSizeHint(50)
            .setEnablePlaceholders(false)
            .build()

        stateLiveData = Transformations.switchMap(flickrDataSourceFactory.flickrDataSourceLiveData) { dataSource ->
            dataSource.stateLiveData
        }
        galleryItemLiveData = LivePagedListBuilder(flickrDataSourceFactory, config).build()
    }

    fun search(query: String) {
        flickrDataSourceFactory.search(query)
        QueryPreferences.setStoredQuery(app, query)
    }
}
