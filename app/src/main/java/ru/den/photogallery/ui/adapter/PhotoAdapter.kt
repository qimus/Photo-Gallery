package ru.den.photogallery.ui.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.den.photogallery.R
import ru.den.photogallery.ThumbnailDownloader
import ru.den.photogallery.model.GalleryItem

class PhotoAdapter(
    private val thumbnailDownloader: ThumbnailDownloader<PhotoViewHolder>,
    private val handleClick: (GalleryItem) -> Unit
) : PagedListAdapter<GalleryItem, PhotoAdapter.PhotoViewHolder>(DIFF_CALLBACK) {
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        val photoImageView = inflater.inflate(R.layout.list_item_gallery, parent, false) as ImageView

        return PhotoViewHolder(photoImageView)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val galleryItem = getItem(position)!!
        val placeholder = ContextCompat.getDrawable(context, R.drawable.ic_image_preview)
            ?: ColorDrawable()
        thumbnailDownloader.queueThumbnail(holder, galleryItem.url)
        holder.bindDrawable(placeholder)
        holder.bindGalleryItem(galleryItem)
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

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GalleryItem>() {
            override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
