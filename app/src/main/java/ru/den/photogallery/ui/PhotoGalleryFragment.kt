package ru.den.photogallery.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.den.photogallery.R
import ru.den.photogallery.ThumbnailDownloader
import ru.den.photogallery.adapter.PhotoAdapter
import ru.den.photogallery.viewmodel.PhotoGalleryViewModel

class PhotoGalleryFragment : Fragment() {

    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoAdapter.PhotoViewHolder>
    private var progressDialog: DialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        retainInstance = true
        setHasOptionsMenu(true)

        photoGalleryViewModel = ViewModelProvider(requireActivity())
            .get(PhotoGalleryViewModel::class.java)

        val responseHandler = Handler(Looper.getMainLooper())

        thumbnailDownloader = ThumbnailDownloader(responseHandler) { photoHolder, bitmap ->
            val drawable = BitmapDrawable(resources, bitmap)
            photoHolder.bind(drawable)
        }
        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)

        val searchItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                Log.i(TAG, "onQueryTextSubmit: $query")
                photoGalleryViewModel.search(query)
                showProgress()
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.i(TAG, "onQueryTextChange: $newText")
                return false
            }
        })


        searchView.setOnSearchClickListener {
            searchView.setQuery(photoGalleryViewModel.searchTerm, false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_item_clear -> {
                photoGalleryViewModel.search("")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "onCreateView")
        viewLifecycleOwner.lifecycle.addObserver(thumbnailDownloader.viewLifecycleObserver)
        return inflater.inflate(R.layout.fragment_photo_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated")
        photoRecyclerView = view.findViewById(R.id.photoRecyclerView)
        configureRecyclerView()
        photoRecyclerView.adapter = PhotoAdapter(listOf(), thumbnailDownloader)

        photoGalleryViewModel.galleryItemLiveData.observe(viewLifecycleOwner, { galleryItems ->
            (photoRecyclerView.adapter as PhotoAdapter).setPhotos(galleryItems)
            hideProgress()
            Log.d(TAG, "Response received: $galleryItems")
        })
    }

    private fun showProgress() {
        progressDialog = SpinnerModalDialog()
        progressDialog?.show(parentFragmentManager, "progress")
    }

    private fun hideProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun configureRecyclerView() {
        photoRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        photoRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastUpdateTime: Long = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
                val firstPosition = layoutManager.findFirstVisibleItemPosition()
                val lastPosition = layoutManager.findLastVisibleItemPosition()
                val adapter = recyclerView.adapter as? PhotoAdapter ?: return

                if (lastUpdateTime > 0 && System.currentTimeMillis() - lastUpdateTime < 100) {
                    return
                }

                lastUpdateTime = System.currentTimeMillis()

                (firstPosition downTo firstPosition - 10)
                    .takeWhile { it >= 0 }
                    .map { adapter.getItem(it) }
                    .map { item -> thumbnailDownloader.preupload(item.url) }

                (lastPosition until lastPosition + 10)
                    .takeWhile { it < adapter.itemCount }
                    .map { adapter.getItem(it) }
                    .map { item -> thumbnailDownloader.preupload(item.url) }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        lifecycle.removeObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView")
        viewLifecycleOwner.lifecycle.removeObserver(thumbnailDownloader.viewLifecycleObserver)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.i(TAG, "onAttach")
    }

    override fun onDetach() {
        super.onDetach()
        Log.i(TAG, "onDetach")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.i(TAG, "onActivityCreated")
    }

    companion object {
        private const val TAG = "PhotoGalleryFragment"
        fun newInstance(): PhotoGalleryFragment {
            return PhotoGalleryFragment()
        }
    }
}
