package ru.den.photogallery.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ru.den.photogallery.QueryPreferences
import ru.den.photogallery.api.FlickrRepository
import ru.den.photogallery.model.GalleryItem

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {
    private var queryLiveData = MutableLiveData<String>()
    private val flickRepository = FlickrRepository()
    val galleryItemLiveData: LiveData<List<GalleryItem>>
    val searchTerm: String
        get() = queryLiveData.value ?: ""

    init {
        queryLiveData.value = QueryPreferences.getStoredQuery(app)
        galleryItemLiveData = Transformations.switchMap(queryLiveData) { searchTerm ->
            if (searchTerm.isBlank()) {
                flickRepository.fetchPhotos()
            } else {
                flickRepository.searchPhotos(searchTerm)
            }

        }
    }

    fun search(query: String) {
        queryLiveData.value = query
        QueryPreferences.setStoredQuery(app, query)
    }
}
