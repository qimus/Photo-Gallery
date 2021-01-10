package ru.den.photogallery.api.paging

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import ru.den.photogallery.model.GalleryItem

class FlickrDataSourceFactory(private var query: String = "") : DataSource.Factory<Int, GalleryItem>() {
    val flickrDataSourceLiveData = MutableLiveData<FlickrPagingSource>()

    override fun create(): DataSource<Int, GalleryItem> {
        val flickrDataSource = FlickrPagingSource(query)
        flickrDataSourceLiveData.postValue(flickrDataSource)
        return flickrDataSource
    }

    fun search(query: String) {
        this.query = query
        flickrDataSourceLiveData.value?.invalidate()
    }
}
