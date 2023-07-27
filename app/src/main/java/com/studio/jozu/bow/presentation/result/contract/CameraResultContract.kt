package com.studio.jozu.bow.presentation.result.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import com.studio.jozu.bow.presentation.result.CameraResult

class CameraResultContract : ActivityResultContract<Nothing?, CameraResult>() {
    override fun createIntent(context: Context, input: Nothing?): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CameraResult {
        val bitmap = intent?.extras?.get("data") as? Bitmap
        val isCanceled = resultCode != Activity.RESULT_OK

        return CameraResult(isCanceled, bitmap)
    }
}