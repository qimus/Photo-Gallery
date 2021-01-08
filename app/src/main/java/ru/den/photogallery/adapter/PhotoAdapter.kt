package ru.den.photogallery.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.den.photogallery.R
import ru.den.photogallery.ThumbnailDownloader
import ru.den.photogallery.model.GalleryItem

class PhotoAdapter(
    private var photos: List<GalleryItem>,
    private val thumbnailDownloader: ThumbnailDownloader<PhotoViewHolder>
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
        val placeholder = ContextCompat.getDrawable(context, R.drawable.bill_up_close)
            ?: ColorDrawable()
        thumbnailDownloader.queueThumbnail(holder, galleryItem.url)
        holder.bind(placeholder)
    }

    override fun getItemCount(): Int {
        return photos.size
    }

    class PhotoViewHolder(private val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        fun bind(image: Drawable) {
            imageView.setImageDrawable(image)
        }
    }
}
