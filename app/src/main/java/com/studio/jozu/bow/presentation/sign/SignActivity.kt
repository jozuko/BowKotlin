package com.studio.jozu.bow.presentation.sign

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.SignActivityBinding
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.domain.ResultType
import com.studio.jozu.bow.domain.extension.DisposableListEx.dispose
import com.studio.jozu.bow.infrastructure.event.EventBusManager
import com.studio.jozu.bow.infrastructure.event.model.OnDismissDialog
import com.studio.jozu.bow.presentation.dialog.BowAlertDialog
import com.studio.jozu.bow.presentation.main.MainActivity
import com.studio.jozu.bow.usecase.SignCase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject

class SignActivity : AppCompatActivity() {
    companion object {
        private const val DIALOG_REQUEST_NOT_FOUND = "DIALOG_REQUEST_NOT_FOUND"
        private const val DIALOG_REQUEST_TERMINATE = "DIALOG_REQUEST_TERMINATE"

        fun startActivity(activity: Activity) {
            val intent = Intent(activity, SignActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(intent)
        }
    }

    @Inject
    lateinit var signCase: SignCase

    @Inject
    lateinit var eventBusManager: EventBusManager

    private lateinit var binding: SignActivityBinding

    private val disposableList = mutableListOf<Disposable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BowComponent.instance.inject(this)

        if (signCase.isSignIn) {
            gotoMain()
            return
        }

        binding = SignActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isLoading = false

        binding.signIn.setOnClickListener { onClickSignIn() }
    }

    override fun onResume() {
        super.onResume()
        eventBusManager.register(this)
    }

    override fun onPause() {
        super.onPause()
        eventBusManager.unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableList.dispose()
    }

    private var isLoading: Boolean
        get() = binding.loading.isVisible
        set(value) {
            binding.loading.isVisible = value
        }

    private fun onClickSignIn() {
        isLoading = true
        binding.errorMessage.isVisible = false

        val disposable = signCase.signIn(binding.mailAddress.text.toString())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Timber.i("SignActivity#signIn: $result")
                    isLoading = false
                    analyzeSignResult(result)
                },
                { error ->
                    Timber.e("SignActivity#signIn: ${error.localizedMessage}")
                    Handler(Looper.getMainLooper()).post {
                        isLoading = false
                        internalError()
                    }
                }
            )
        disposableList.add(disposable)
    }

    private fun signUp() {
        isLoading = true
        binding.errorMessage.isVisible = false

        val disposable = signCase.signUp(binding.mailAddress.text.toString())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Timber.i("SignActivity#signUp: $result")
                    isLoading = false
                    analyzeSignResult(result)
                },
                { error ->
                    Timber.e("SignActivity#signUp: ${error.localizedMessage}")
                    Handler(Looper.getMainLooper()).post {
                        isLoading = false
                        internalError()
                    }
                }
            )
        disposableList.add(disposable)
    }

    private fun analyzeSignResult(result: ResultType) {
        isLoading = false
        when (result) {
            ResultType.SUCCESS -> gotoMain()
            ResultType.NOT_FOUND -> userNotFound()
            ResultType.VALIDATION_ERROR -> validationError()
            ResultType.INTERNAL_ERROR -> internalError()
        }
    }

    private fun gotoMain() {
        MainActivity.startActivity(this)
        finish()
    }

    private fun userNotFound() {
        BowAlertDialog.Builder().apply {
            requestCode = DIALOG_REQUEST_NOT_FOUND
            title = getString(R.string.common_confirm)
            message = getString(R.string.error_user_not_found, binding.mailAddress.text.toString())
            isUseCancel = true
            okText = getString(R.string.common_yes)
            cancelText = getString(R.string.common_no)
        }.build().show(supportFragmentManager)
    }

    private fun validationError() {
        binding.errorMessage.text = getString(R.string.error_validation_mail_address)
        binding.errorMessage.isVisible = true
    }

    private fun internalError() {
        BowAlertDialog.Builder().apply {
            requestCode = DIALOG_REQUEST_TERMINATE
            title = getString(R.string.common_error)
            message = getString(R.string.error_message_terminate)
        }.build().show(supportFragmentManager)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDismissDialog(event: OnDismissDialog) {
        if (event.requestCode == DIALOG_REQUEST_TERMINATE) {
            finish()
        } else if (event.requestCode == DIALOG_REQUEST_NOT_FOUND) {
            if (event.isCancel) {
                return
            }
            signUp()
        }
    }
}