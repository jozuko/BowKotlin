package com.studio.jozu.bow.presentation.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.studio.jozu.bow.R
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.infrastructure.event.EventBusManager
import com.studio.jozu.bow.infrastructure.event.model.OnDismissDialog
import java.util.*
import javax.inject.Inject

/**
 * 日付選択ダイアログ
 *
 * Created by r.mori on 2018/11/02.
 * Copyright (c) 2018 rei-frontier. All rights reserved.
 */
class BowDatePickerDialog : BottomSheetDialogFragment() {
    companion object {
        private const val EXTRA_KEY_REQUEST_CODE = "extra_requestCode"
        private const val EXTRA_KEY_DATE = "extra_date"
        const val RESULT_KEY_DATE = "result_date"
    }

    @Inject
    lateinit var eventBusManager: EventBusManager

    private var requestCode: String = ""
    private lateinit var datePickerView: DatePickerView

    init {
        BowComponent.instance.inject(this)
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, BowDatePickerDialog::class.simpleName)
    }

    /**
     * ダイアログ生成
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // ダイアログを生成
        val dialog = super.onCreateDialog(savedInstanceState)

        // パラメータを取得
        requestCode = arguments?.getString(EXTRA_KEY_REQUEST_CODE, null) ?: ""
        val now = Calendar.getInstance()
        val millis = arguments?.getLong(EXTRA_KEY_DATE, now.timeInMillis) ?: now.timeInMillis
        val date = Calendar.getInstance().apply { timeInMillis = millis }

        // Viewを生成
        val view = View.inflate(context, R.layout.date_picker_dialog, null)
        dialog.setContentView(view)
        datePickerView = view.findViewById(R.id.datePickerView)
        datePickerView.setCurrentDate(date)

        view.findViewById<AppCompatButton>(R.id.date_picker_ok).setOnClickListener { onClickOk() }
        view.findViewById<AppCompatButton>(R.id.date_picker_cancel).setOnClickListener { onClickCancel() }
        return dialog
    }

    /**
     * OKボタンクリック時動作
     */
    private fun onClickOk() {
        val resultExtras = Bundle()
        resultExtras.putLong(RESULT_KEY_DATE, datePickerView.date.timeInMillis)
        eventBusManager.post(OnDismissDialog(isCancel = false, requestCode = requestCode, result = resultExtras))

        dialog?.dismiss()
    }

    /**
     * Cancelボタンクリック時動作
     */
    private fun onClickCancel() {
        dialog?.cancel()
    }

    /**
     * ダイアログキャンセル時動作
     */
    override fun onCancel(dialog: DialogInterface) {
        eventBusManager.post(OnDismissDialog(isCancel = true, requestCode = requestCode))
    }

    /**
     * ダイアログBuilder
     */
    class Builder(
        var requestCode: String = "",
        var date: Calendar = CalendarEx.today,
    ) {
        fun build(): BowDatePickerDialog {
            val extras = Bundle()
            extras.putString(EXTRA_KEY_REQUEST_CODE, requestCode)
            extras.putLong(EXTRA_KEY_DATE, date.timeInMillis)

            val fragment = BowDatePickerDialog()
            fragment.arguments = extras
            return fragment
        }
    }
}