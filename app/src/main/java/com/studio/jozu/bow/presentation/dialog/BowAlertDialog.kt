package com.studio.jozu.bow.presentation.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.infrastructure.event.EventBusManager
import com.studio.jozu.bow.infrastructure.event.model.OnDismissDialog
import java.io.Serializable
import javax.inject.Inject

class BowAlertDialog : DialogFragment() {
    private enum class Arguments {
        REQUEST_CODE,
        TITLE,
        MESSAGE,
        OK_TEXT,
        CANCEL_TEXT,
        USE_CANCEL,
        TAG_DATA,
    }

    class Builder(
        var requestCode: String = "",
        var title: String = "",
        var message: String = "",
        var okText: String = "",
        var cancelText: String = "",
        var isUseCancel: Boolean = false,
        var tagData: Serializable? = null,
    ) {
        fun build(): BowAlertDialog {
            return BowAlertDialog().apply {
                arguments = Bundle().apply {
                    putString(Arguments.REQUEST_CODE.name, requestCode)
                    putString(Arguments.TITLE.name, title)
                    putString(Arguments.MESSAGE.name, message)
                    putString(Arguments.OK_TEXT.name, okText)
                    putString(Arguments.CANCEL_TEXT.name, cancelText)
                    putBoolean(Arguments.USE_CANCEL.name, isUseCancel)
                    tagData?.let { putSerializable(Arguments.TAG_DATA.name, tagData) }
                }
            }
        }
    }

    @Inject
    lateinit var eventBusManager: EventBusManager

    init {
        BowComponent.instance.inject(this)
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, BowAlertDialog::class.simpleName)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val requestCode = arguments?.getString(Arguments.REQUEST_CODE.name, null) ?: ""
        val title = arguments?.getString(Arguments.TITLE.name, null) ?: ""
        val message = arguments?.getString(Arguments.MESSAGE.name, null) ?: ""
        val okText = arguments?.getString(Arguments.OK_TEXT.name, null) ?: ""
        val cancelText = arguments?.getString(Arguments.CANCEL_TEXT.name, null) ?: ""
        val isUseCancel = arguments?.getBoolean(Arguments.USE_CANCEL.name, false) ?: false
        val tagData = arguments?.getSerializable(Arguments.TAG_DATA.name)

        return AlertDialog.Builder(requireContext()).apply {
            if (title.isNotEmpty()) {
                setTitle(title)
            }
            if (message.isNotEmpty()) {
                setMessage(message)
            }
            val ok = if (okText.isNotEmpty()) okText else context.getString(android.R.string.ok)
            setPositiveButton(ok) { _, _ ->
                dismissDialog(isCancel = false, requestCode, tagData)
            }

            val cancel = if (cancelText.isNotEmpty()) cancelText else context.getString(android.R.string.cancel)
            if (isUseCancel) {
                setNegativeButton(cancel) { _, _ ->
                    dismissDialog(isCancel = true, requestCode, tagData)
                }
            }
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }

    private fun dismissDialog(isCancel: Boolean, requestCode: String, tagData: Serializable?) {
        eventBusManager.post(OnDismissDialog(isCancel, requestCode, tagData))
    }
}