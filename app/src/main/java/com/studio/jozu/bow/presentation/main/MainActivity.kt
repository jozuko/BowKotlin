package com.studio.jozu.bow.presentation.main

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.MainActivityBinding
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.domain.MenuItem
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.DisposableListEx.dispose
import com.studio.jozu.bow.infrastructure.SharedHolder
import com.studio.jozu.bow.infrastructure.event.EventBusManager
import com.studio.jozu.bow.infrastructure.event.model.OnAddDogEvent
import com.studio.jozu.bow.infrastructure.event.model.OnDismissDialog
import com.studio.jozu.bow.presentation.dialog.BowAlertDialog
import com.studio.jozu.bow.presentation.main.calendar.CalendarFragment
import com.studio.jozu.bow.presentation.main.list.DogEventListFragment
import com.studio.jozu.bow.presentation.main.settings.dog.SettingDogFragment
import com.studio.jozu.bow.presentation.main.settings.task.SettingTaskFragment
import com.studio.jozu.bow.presentation.sign.SignActivity
import com.studio.jozu.bow.usecase.DogListCase
import com.studio.jozu.bow.usecase.EventCase
import com.studio.jozu.bow.usecase.SignCase
import com.studio.jozu.bow.usecase.TaskListCase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject

class MainActivity : AppCompatActivity(), MainActivityFragmentListener {
    companion object {
        private const val DIALOG_REQUEST_CODE_LOGOUT = "DIALOG_REQUEST_CODE_LOGOUT"

        fun startActivity(activity: Activity) {
            val intent = Intent(activity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(intent)
        }
    }

    @Inject
    lateinit var signCase: SignCase

    @Inject
    lateinit var dogListCase: DogListCase

    @Inject
    lateinit var eventCase: EventCase

    @Inject
    lateinit var taskListCase: TaskListCase

    @Inject
    lateinit var eventBusManager: EventBusManager

    @Inject
    lateinit var sharedHolder: SharedHolder

    private lateinit var binding: MainActivityBinding

    private val disposableList = mutableListOf<Disposable>()

    private val fragmentContainerId
        get() = binding.fragmentBase.id

    private val menuMinY get() = (binding.root.height - binding.menuTaskList.top).toFloat()
    private var menuTaskMaxY: Float = 0F
    private var menuSettingsMaxY: Float = 0F

    private var isShowTaskMenu: Boolean = false
    private var taskMenuShowAnimation: Animator? = null
    private var taskMenuHideAnimation: Animator? = null

    private var isShowSettingMenu: Boolean = false
    private var settingsMenuShowAnimation: Animator? = null
    private var settingsMenuHideAnimation: Animator? = null

    private val settingMenuList by lazy {
        listOf(
            MenuItem(imageRes = R.mipmap.ic_dog, title = getString(R.string.settings_dog)),
            MenuItem(imageRes = R.mipmap.ic_task, title = getString(R.string.settings_task)),
            MenuItem(imageRes = R.mipmap.ic_logout, title = getString(R.string.settings_logout)),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        BowComponent.instance.inject(this)

        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpMenu()
        setUpDogSelectionView()
        setUpViewButton()

        if (sharedHolder.showCalendar) {
            showCalendar()
        } else {
            showList()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableList.dispose()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportFragmentManager.findFragmentById(R.id.fragment_base)?.let { fragment ->
            changeMenuVisibility(fragment)
        }
    }

    private fun setUpMenu() {
        binding.root.doOnLayout {
            binding.menuTaskBase.y = menuMinY
            binding.menuSettingsBase.y = menuMinY
        }

        setUpTaskMenu()
        setUpSettingsMenu()

        binding.menuBackground.setOnClickListener {
            hideMenuSettings(withAnimation = true)
            hideMenuTask(withAnimation = true)
            binding.menuBackground.isVisible = false
        }
        binding.menuBackground.isVisible = false
    }

    private fun setUpTaskMenu() {
        val disposable = taskListCase.getTaskListFromLocal()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { taskList ->
                    val enabledTaskList = taskList.filter { it.enabled }
                    val taskAdapter = MenuListAdapter(
                        context = this,
                        menuList = enabledTaskList.map { MenuItem(imageRes = it.icon.iconRes, title = it.title) },
                        onLayoutHeight = { setMenuTaskMaxHeight(it, enabledTaskList) },
                        onClickItem = { onClickMenuTaskItem(it, enabledTaskList) },
                    )
                    binding.menuTaskList.adapter = taskAdapter
                },
                {
                    Timber.e("CalendarFragment#setUpTaskMenu: ${it.localizedMessage}")
                }
            )
        disposableList.add(disposable)

        binding.menuTaskImage.setOnClickListener {
            if (binding.menuTaskBase.y == menuMinY) {
                hideMenuSettings(withAnimation = true)
                showMenuTask()
                binding.menuBackground.isVisible = true
            } else {
                hideMenuTask(withAnimation = true)
                binding.menuBackground.isVisible = false
            }
        }
    }

    private fun setMenuTaskMaxHeight(viewHeight: Float, taskList: List<Task>) {
        val listMaxHeight = viewHeight * taskList.count() + (binding.root.height - menuMinY)
        menuTaskMaxY = if (listMaxHeight > binding.root.height) {
            0F
        } else {
            binding.root.height - listMaxHeight
        }
    }

    private fun onClickMenuTaskItem(position: Int, taskList: List<Task>) {
        if (!isShowTaskMenu) {
            return
        }

        val selectedTask = taskList[position]
        showDogTimeSelectionView(selectedTask)
    }

    private fun hideMenuTask(withAnimation: Boolean) {
        taskMenuShowAnimation?.cancel()
        if (withAnimation) {
            taskMenuHideAnimation = ValueAnimator.ofFloat(binding.menuTaskBase.y, menuMinY).apply {
                duration = 300
                addUpdateListener {
                    binding.menuTaskBase.y = it.animatedValue as Float
                }
                doOnStart {
                    isShowTaskMenu = false
                }
                start()
            }
        } else {
            binding.menuTaskBase.y = menuMinY
            isShowTaskMenu = false
        }
    }

    private fun showMenuTask() {
        taskMenuHideAnimation?.cancel()
        taskMenuShowAnimation = ValueAnimator.ofFloat(binding.menuTaskBase.y, menuTaskMaxY).apply {
            duration = 300
            addUpdateListener {
                binding.menuTaskBase.y = it.animatedValue as Float
            }
            doOnEnd {
                isShowTaskMenu = true
            }
            start()
        }
    }

    private fun setUpSettingsMenu() {
        val settingsAdapter = MenuListAdapter(
            context = this,
            menuList = settingMenuList,
            onLayoutHeight = { setMenuSettingsMaxHeight(it, settingMenuList) },
            onClickItem = { onClickMenuSettingsItem(it) },
        )
        binding.menuSettingsList.adapter = settingsAdapter

        binding.menuSettingsImage.setOnClickListener {
            if (binding.menuSettingsBase.y == menuMinY) {
                showMenuSettings()
                hideMenuTask(withAnimation = true)
                binding.menuBackground.isVisible = true
            } else {
                hideMenuSettings(withAnimation = true)
                binding.menuBackground.isVisible = false
            }
        }
    }

    private fun setMenuSettingsMaxHeight(viewHeight: Float, settingsList: List<MenuItem>) {
        val listMaxHeight = viewHeight * settingsList.count() + (binding.root.height - menuMinY)
        menuSettingsMaxY = if (listMaxHeight > binding.root.height) {
            0F
        } else {
            binding.root.height - listMaxHeight
        }

        Timber.d("CalendarFragment#setMenuSettingsMaxHeight: $menuSettingsMaxY")
    }

    private fun onClickMenuSettingsItem(position: Int) {
        if (!isShowSettingMenu) {
            return
        }

        Timber.d("CalendarFragment#onClickMenuSettingsItem: $position")
        when (position) {
            0 -> showDogSetting()
            1 -> showTaskSetting()
            2 -> confirmLogout()
            else -> {
                // do nothing.
            }
        }
    }

    private fun hideMenuSettings(withAnimation: Boolean) {
        settingsMenuShowAnimation?.cancel()
        if (withAnimation) {
            settingsMenuHideAnimation = ValueAnimator.ofFloat(binding.menuSettingsBase.y, menuMinY).apply {
                duration = 300
                addUpdateListener {
                    binding.menuSettingsBase.y = it.animatedValue as Float
                }
                doOnStart {
                    isShowSettingMenu = false
                }
                start()
            }
        } else {
            isShowSettingMenu = false
            binding.menuSettingsBase.y = menuMinY
        }
    }

    private fun showMenuSettings() {
        settingsMenuHideAnimation?.cancel()
        settingsMenuShowAnimation = ValueAnimator.ofFloat(binding.menuSettingsBase.y, menuSettingsMaxY).apply {
            duration = 300
            addUpdateListener {
                binding.menuSettingsBase.y = it.animatedValue as Float
            }
            doOnEnd {
                isShowSettingMenu = true
            }
            start()
        }
    }

    private fun confirmLogout() {
        BowAlertDialog.Builder().apply {
            requestCode = DIALOG_REQUEST_CODE_LOGOUT
            title = getString(R.string.common_confirm)
            message = getString(R.string.confirm_logout_message)
            isUseCancel = true
            okText = getString(R.string.common_yes)
            cancelText = getString(R.string.common_no)
        }.build().show(supportFragmentManager)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDismissDialog(event: OnDismissDialog) {
        if (event.requestCode == DIALOG_REQUEST_CODE_LOGOUT) {
            if (event.isCancel) {
                return
            }
            doLogout()
        }
    }

    private fun setUpDogSelectionView() {
        binding.dogSelectionView.isVisible = false

        binding.dogSelectionView.onCancel = {
            binding.dogSelectionView.isVisible = false
            binding.menuBackground.isVisible = false
        }

        binding.dogSelectionView.onSelectedDog = { task, timestamp, dogs ->
            val disposable = eventCase.add(task, dogs, timestamp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        eventBusManager.post(OnAddDogEvent(task, dogs, timestamp))
                        binding.dogSelectionView.isVisible = false
                        binding.menuBackground.isVisible = false
                    },
                    {
                        Timber.e("MainActivity#onClickMenuTaskItem: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }

        showLoading()
        val disposable = dogListCase.getDogListFromLocal()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { dogList ->
                    hideLoading()
                    binding.dogSelectionView.setDogList(dogList)
                },
                {
                    Timber.e("MainActivity#getDogListFromLocal: ${it.localizedMessage}")
                    Handler(Looper.getMainLooper()).post {
                        hideLoading()
                    }
                }
            )
        disposableList.add(disposable)
    }

    private fun showDogTimeSelectionView(selectedTask: Task) {
        hideMenuTask(withAnimation = true)
        binding.dogSelectionView.setTask(selectedTask)
        binding.dogSelectionView.setDefaultDog(null)
        binding.dogSelectionView.canDelete = false
        binding.dogSelectionView.isVisible = true
    }

    private fun setUpViewButton() {
        binding.showCalendar.setOnClickListener {
            showCalendar()
        }
        binding.showList.setOnClickListener {
            showList()
        }
        binding.showCalendar.isVisible = !sharedHolder.showCalendar
        binding.showList.isVisible = sharedHolder.showCalendar
    }

    override fun showLoading() {
        binding.loading.isVisible = true
    }

    override fun hideLoading() {
        binding.loading.isVisible = false
    }

    private fun doLogout() {
        Timber.d("MainActivity#doLogout:")
        showLoading()
        val disposable = signCase.signOut()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    SignActivity.startActivity(this)
                    finish()
                },
                {
                    Timber.e("MainActivity#doLogout: ${it.localizedMessage}")
                }
            )
        disposableList.add(disposable)
    }

    private fun changeMenuVisibility(currentFragment: Fragment?) {
        when (currentFragment) {
            is CalendarFragment -> {
                binding.menuSettingsBase.isVisible = true
                binding.menuTaskBase.isVisible = true
                binding.showCalendar.isVisible = !sharedHolder.showCalendar
                binding.showList.isVisible = sharedHolder.showCalendar
            }
            is DogEventListFragment -> {
                binding.menuSettingsBase.isVisible = true
                binding.menuTaskBase.isVisible = true
                binding.showCalendar.isVisible = !sharedHolder.showCalendar
                binding.showList.isVisible = sharedHolder.showCalendar
            }
            else -> {
                hideMenuSettings(withAnimation = false)
                hideMenuTask(withAnimation = false)
                binding.menuBackground.isVisible = false
                binding.menuSettingsBase.isVisible = false
                binding.menuTaskBase.isVisible = false
                binding.showCalendar.isVisible = false
                binding.showList.isVisible = false
            }
        }
    }

    private fun showDogSetting() {
        val fragment = SettingDogFragment.newInstance()
        supportFragmentManager.commit {
            addToBackStack(SettingDogFragment::class.simpleName)
            replace(fragmentContainerId, fragment)
        }
        changeMenuVisibility(fragment)
    }

    private fun showTaskSetting() {
        val fragment = SettingTaskFragment.newInstance()
        supportFragmentManager.commit {
            addToBackStack(SettingTaskFragment::class.simpleName)
            replace(fragmentContainerId, fragment)
        }
        changeMenuVisibility(fragment)
    }

    private fun showCalendar() {
        val fragment = CalendarFragment.newInstance()
        supportFragmentManager.commit {
            replace(fragmentContainerId, fragment)
        }
        sharedHolder.showCalendar = true
        changeMenuVisibility(fragment)
    }

    private fun showList() {
        val fragment = DogEventListFragment.newInstance()
        supportFragmentManager.commit {
            replace(fragmentContainerId, fragment)
        }
        sharedHolder.showCalendar = false
        changeMenuVisibility(fragment)
    }
}