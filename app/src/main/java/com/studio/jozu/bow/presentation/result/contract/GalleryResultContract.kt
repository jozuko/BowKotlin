package com.studio.jozu.bow.presentation.result.contract

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.studio.jozu.bow.R
import com.studio.jozu.bow.presentation.result.GalleryResult

class GalleryResultContract : ActivityResultContract<Nothing, GalleryResult>() {
    override fun createIntent(context: Context, input: Nothing?): Intent {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        intent.type = "image/jpeg"

        val title = context.getString(R.string.photo_from_gallery)
        return Intent.createChooser(intent, title)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): GalleryResult {
        val data = intent?.data
        val isCanceled = data == null

        return GalleryResult(isCanceled, data)
    }
}