package ru.den.photogallery.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.den.photogallery.QueryPreferences
import ru.den.photogallery.R
import ru.den.photogallery.ThumbnailDownloader
import ru.den.photogallery.api.State
import ru.den.photogallery.ui.adapter.PhotoAdapter
import ru.den.photogallery.framework.lifecycle.NotificationObserver
import ru.den.photogallery.viewmodel.PhotoGalleryViewModel
import ru.den.photogallery.framework.worker.PollWorker

class PhotoGalleryFragment : Fragment() {

    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoAdapter.PhotoViewHolder>
    private var progressDialog: DialogFragment? = null
    private lateinit var notificationObserver: NotificationObserver
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var noResultContainer: LinearLayout

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
            photoHolder.bindDrawable(drawable)
        }
        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)

        notificationObserver = NotificationObserver(requireContext())
        lifecycle.addObserver(notificationObserver)
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

        val toggleItem = menu.findItem(R.id.menu_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        val toggleItemTitle = if (isPolling) {
            getString(R.string.stop_polling)
        } else {
            getString(R.string.start_polling)
        }

        toggleItem.title = toggleItemTitle
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_item_clear -> {
                photoGalleryViewModel.search("")
                true
            }
            R.id.menu_toggle_polling -> {
                val isPolling = QueryPreferences.isPolling(requireContext())
                if (isPolling) {
                    PollWorker.cancelWork(requireContext())
                } else {
                    PollWorker.schedule(requireContext())
                }
                activity?.invalidateOptionsMenu()
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
        noResultContainer = view.findViewById(R.id.noResultContainer)
        photoRecyclerView = view.findViewById(R.id.photoRecyclerView)
        configureRecyclerView()
        configureObservable()
    }

    private fun configureRecyclerView() {
        photoRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        photoAdapter = PhotoAdapter(thumbnailDownloader) { galleryItem ->
            val intent = PhotoPageActivity.newIntent(requireContext(), galleryItem.photoPageUri)
            context?.startActivity(intent)
            (activity as? AppCompatActivity)?.overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }
        photoRecyclerView.adapter = photoAdapter
    }

    private fun configureObservable() {
        photoGalleryViewModel.galleryItemLiveData.observe(viewLifecycleOwner, { pagedList ->
            photoAdapter.submitList(pagedList)
        })

        photoGalleryViewModel.stateLiveData.observe(viewLifecycleOwner, { state ->
            when (state) {
                State.LOADING -> {
                    showProgress()
                    showNoResultContainer(false)
                }
                State.EMPTY -> {
                    showNoResultContainer(true)
                    hideProgress()
                }
                State.DONE -> hideProgress()
            }
        })
    }

    private fun showProgress() {
        if (progressDialog != null) {
            return
        }
        progressDialog = SpinnerModalDialog()
        progressDialog?.show(parentFragmentManager, "progress")
    }

    private fun hideProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showNoResultContainer(visible: Boolean) {
        noResultContainer.isVisible = visible
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        lifecycle.removeObserver(thumbnailDownloader.fragmentLifecycleObserver)
        lifecycle.removeObserver(notificationObserver)
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
