package com.studio.jozu.bow.infrastructure.event.model

import android.os.Bundle
import java.io.Serializable

data class OnDismissDialog(val isCancel: Boolean, val requestCode: String, val tagData: Serializable? = null, val result: Bundle? = null)
