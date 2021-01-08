package ru.den.photogallery.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import ru.den.photogallery.R

class PhotoGalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_gallery)
        Log.i("PhotoGalleryActivity", "activity created")

        val isFragmentContainerEmpty = savedInstanceState == null
        if (isFragmentContainerEmpty) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainer, PhotoGalleryFragment.newInstance())
                    .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("PhotoGalleryActivity", "activity started")
    }

    override fun onResume() {
        super.onResume()
        Log.i("PhotoGalleryActivity", "activity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.i("PhotoGalleryActivity", "activity paused")
    }

    override fun onStop() {
        super.onStop()
        Log.i("PhotoGalleryActivity", "activity stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("PhotoGalleryActivity", "activity destroyed")
    }
}
