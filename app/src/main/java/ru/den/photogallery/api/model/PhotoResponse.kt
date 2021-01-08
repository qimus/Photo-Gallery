package ru.den.photogallery.api.model

import com.google.gson.annotations.SerializedName
import ru.den.photogallery.model.GalleryItem

data class PhotoResponse(
    @SerializedName("photo")
    var galleryItems: List<GalleryItem>
)