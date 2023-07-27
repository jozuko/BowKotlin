package com.studio.jozu.bow.presentation.main.settings.dog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.isseiaoki.simplecropview.CropImageView
import com.isseiaoki.simplecropview.callback.CropCallback
import com.isseiaoki.simplecropview.callback.LoadCallback
import com.isseiaoki.simplecropview.callback.SaveCallback
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.CropPhotoActivityBinding
import com.studio.jozu.bow.domain.extension.CalendarEx
import timber.log.Timber
import java.io.File

class RFGCropActivity : AppCompatActivity() {

    companion object {
        private const val KEY_FRAME_RECT = "FrameRect"
        private const val KEY_SOURCE_URI = "SourceUri"
    }

    private var mSourceUri: Uri? = null
    private var mFrameRect: RectF? = null
    private val mCompressFormat = Bitmap.CompressFormat.JPEG

    private var isLoading: Boolean = false

    private var mRatioX: Int = 1
    private val mRatioY: Int = 1
    private lateinit var binding: CropPhotoActivityBinding
    private var cameraUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraUri = Uri.fromFile(File(cacheDir, "dog-photo/${CalendarEx.now.timeInMillis}.jpg"))

        binding = CropPhotoActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (savedInstanceState != null) {
            // restore data
            mFrameRect = savedInstanceState.getParcelable(KEY_FRAME_RECT)
            mSourceUri = savedInstanceState.getParcelable(KEY_SOURCE_URI)
        }

        if (mSourceUri == null) {
            mSourceUri = Uri.parse(intent.getStringExtra("imageUri"))
        }

        if (intent.hasExtra("ratioX")) {
            mRatioX = intent.getIntExtra("ratioX", 1)
        }

        binding.cropImageView.load(mSourceUri)
            .initialFrameRect(mFrameRect)
            .useThumbnail(true)
            .execute(mLoadCallback)
        binding.cropImageView.setCropMode(CropImageView.CropMode.CUSTOM)
        // vc.customAspectRatio = CGSize(width: 2, height: 1)
        binding.cropImageView.setCustomRatio(mRatioX, mRatioY)

        supportActionBar?.title = getString(R.string.crop_photo)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.mipmap.ic_close)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // save data
        outState.putParcelable(KEY_FRAME_RECT, binding.cropImageView.actualCropRect)
        outState.putParcelable(KEY_SOURCE_URI, binding.cropImageView.sourceUri)
    }

    override fun onBackPressed() {
        if (isLoading) {
            return
        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.crop, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.finish_crop) {
            crop()
            return true
        } else if (id == android.R.id.home) {

            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    //region Private
    private fun startLoading() {
        isLoading = true
        closeKeyboard()
        binding.loading.isVisible = true
    }

    fun stopLoading() {
        isLoading = false
        binding.loading.isVisible = false
    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun crop() {
        isLoading = true
        startLoading()
        binding.cropImageView.crop(mSourceUri).execute(mCropCallback)
    }

    //endregion

    //region Callback
    private val mLoadCallback = object : LoadCallback {
        override fun onSuccess() {
            Timber.d("CropActivity load image success. sourceUri=$mSourceUri")
        }

        override fun onError(e: Throwable) {
            Timber.e("CropActivity load image onError ::${e.localizedMessage}")
            finish()
        }
    }

    private val mCropCallback = object : CropCallback {
        override fun onSuccess(cropped: Bitmap) {
            binding.cropImageView.save(cropped)
                .compressFormat(mCompressFormat)
                .execute(cameraUri, mSaveCallback)
        }

        override fun onError(e: Throwable) {
            Timber.e("CropActivity#mCropCallback onError ${e.localizedMessage}")
        }
    }

    private val mSaveCallback = object : SaveCallback {
        override fun onSuccess(uri: Uri?) {
            stopLoading()
            isLoading = false
            val intent = Intent()
            intent.putExtra("cropImage", uri)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        override fun onError(e: Throwable?) {
            Timber.e("CropActivity#mSaveCallback onError ${e?.localizedMessage ?: ""}")
        }
    }
    //endregion
}
