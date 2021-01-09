package ru.den.photogallery.ui.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.den.photogallery.R
import ru.den.photogallery.ThumbnailDownloader
import ru.den.photogallery.model.GalleryItem
import ru.den.photogallery.ui.PhotoPageActivity

class PhotoAdapter(
    private var photos: List<GalleryItem>,
    private val thumbnailDownloader: ThumbnailDownloader<PhotoViewHolder>,
    private val handleClick: (GalleryItem) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        val photoImageView = inflater.inflate(R.layout.list_item_gallery, parent, false) as ImageView

        return PhotoViewHolder(photoImageView)
    }

    fun setPhotos(photos: List<GalleryItem>) {
        this.photos = photos
        notifyDataSetChanged()
    }

    fun getItem(position: Int): GalleryItem {
        return photos[position]
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val galleryItem = photos[position]
        val placeholder = ContextCompat.getDrawable(context, R.drawable.ic_image_preview)
            ?: ColorDrawable()
        thumbnailDownloader.queueThumbnail(holder, galleryItem.url)
        holder.bindDrawable(placeholder)
        holder.bindGalleryItem(galleryItem)
    }

    override fun getItemCount(): Int {
        return photos.size
    }

    inner class PhotoViewHolder(private val imageView: ImageView) : RecyclerView.ViewHolder(imageView), View.OnClickListener {
        private lateinit var galleryItem: GalleryItem

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            handleClick(galleryItem)
        }

        fun bindDrawable(image: Drawable) {
            imageView.setImageDrawable(image)
        }

        fun bindGalleryItem(galleryItem: GalleryItem) {
            this.galleryItem = galleryItem
        }
    }
}
