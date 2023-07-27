package com.studio.jozu.bow.domain.extension

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.studio.jozu.bow.infrastructure.repository.model.BowErrorMessageRes
import com.studio.jozu.bow.infrastructure.repository.model.BowErrorRes
import okhttp3.ResponseBody
import timber.log.Timber

object ResponseBodyEx {
    fun ResponseBody?.dump(tag: String, code: Int) {
        val errorBody = this?.toString() ?: return

        try {
            val error = Gson().fromJson(errorBody, BowErrorMessageRes::class.java)
            Timber.e("$tag: response_code=$code error_detail=$error")
        } catch (e: JsonSyntaxException) {
            try {
                val error = Gson().fromJson(errorBody, BowErrorRes::class.java)
                Timber.e("$tag: response_code=$code error_detail=$error")
            } catch (e: JsonSyntaxException) {
                Timber.e("$tag: response_code=$code error_detail=$errorBody")
            }
        }

    }
}