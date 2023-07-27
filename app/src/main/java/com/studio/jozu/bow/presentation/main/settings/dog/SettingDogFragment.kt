package com.studio.jozu.bow.presentation.main.settings.dog

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.SettingDogFragmentBinding
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.DisposableListEx.dispose
import com.studio.jozu.bow.infrastructure.event.EventBusManager
import com.studio.jozu.bow.infrastructure.event.model.OnDismissDialog
import com.studio.jozu.bow.presentation.dialog.BowDatePickerDialog
import com.studio.jozu.bow.presentation.main.MainActivityFragmentListener
import com.studio.jozu.bow.presentation.result.CameraResult
import com.studio.jozu.bow.presentation.result.GalleryResult
import com.studio.jozu.bow.presentation.result.contract.CameraResultContract
import com.studio.jozu.bow.presentation.result.contract.GalleryResultContract
import com.studio.jozu.bow.usecase.DogListCase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class SettingDogFragment : Fragment() {
    companion object {
        private const val REQUEST_DIALOG_BIRTHDAY = "REQUEST_DIALOG_BIRTHDAY"

        /**
         * SettingDogFragmentの生成を行う
         */
        fun newInstance(): SettingDogFragment {
            return SettingDogFragment()
        }
    }

    @Inject
    lateinit var eventBusManager: EventBusManager

    @Inject
    lateinit var dogListCase: DogListCase

    private var listener: MainActivityFragmentListener? = null

    private val disposableList = mutableListOf<Disposable>()

    private var _binding: SettingDogFragmentBinding? = null
    private val binding get() = _binding!!

    private val dogList = mutableListOf<Dog>()

    private val galleryContract = registerForActivityResult(GalleryResultContract(), ::onResultGallery)
    private val cameraContract = registerForActivityResult(CameraResultContract(), ::onResultCamera)

    private val addButtonCenter: PointF
        get() {
            val x = binding.settingDogAdd.x + (binding.settingDogAdd.width.toFloat() / 2)
            val y = binding.settingDogAdd.y + (binding.settingDogAdd.height.toFloat() / 2)
            return PointF(x, y)
        }

    private val dogListAdapter: DogListAdapter?
        get() = binding.settingDogList.adapter as? DogListAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivityFragmentListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BowComponent.instance.inject(this)
    }

    override fun onResume() {
        super.onResume()
        eventBusManager.register(this)
    }

    override fun onPause() {
        super.onPause()
        eventBusManager.unregister(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = SettingDogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        disposableList.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpList()
        binding.settingDogEdit.isInvisible = true
        binding.settingDogEdit.onClickPhoto = { onClickPhoto() }
        binding.settingDogEdit.onClickBirthday = { onClickBirthday() }
        binding.settingDogAdd.setOnClickListener { onClickAddDog() }

        requestDog()
    }

    private fun setUpList() {
        binding.settingDogList.layoutManager = LinearLayoutManager(context).apply {
            orientation = RecyclerView.VERTICAL
        }
        binding.settingDogList.adapter = DogListAdapter().apply {
            onEditing = ::onEditing
        }

        // 区切り線
        ContextCompat.getDrawable(requireContext(), R.drawable.gray_line_horizontal_1dp)?.let { dividerDrawable ->
            val dividerItemDecoration = DividerItemDecoration(context, RecyclerView.VERTICAL)
            dividerItemDecoration.setDrawable(dividerDrawable)
            binding.settingDogList.addItemDecoration(dividerItemDecoration)
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                Timber.d("SettingDogFragment#onMove")
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                if (fromPos == toPos) {
                    return true
                }

                // データを更新
                val targetItem = dogList[fromPos]
                dogList.removeAt(fromPos)
                dogList.add(toPos, targetItem)
                val orderedDogButtonList = dogList.mapIndexed { index, dog ->
                    dog.copy(order = index + 1)
                }
                dogList.clear()
                dogList += orderedDogButtonList

                // Viewを更新
                recyclerView.adapter?.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                Timber.d("SettingDogFragment#onMoved")

                val disposable = dogListCase.updateOrder(dogList)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                        },
                        {
                            Timber.e("SettingDogFragment#onMoved: ${it.localizedMessage}")
                        }
                    )
                disposableList.add(disposable)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                Timber.d("onSwiped")
                val targetPos = viewHolder.adapterPosition
                val dog = dogList[targetPos]
                val disposable = dogListCase.changeVisibility(dog)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { resultDog ->
                            if (dog.dogId != resultDog.dogId) {
                                Timber.e("SettingDogFragment#onSwiped: different dog-id")
                                return@subscribe
                            }

                            dogList[targetPos].enabled = resultDog.enabled
                            dogList[targetPos].updatedAt = resultDog.updatedAt.clone() as Calendar
                            dogListAdapter?.notifyItemChanged(targetPos)
                        },
                        {
                            Timber.e("SettingDogFragment#onSwiped: ${it.localizedMessage}")
                        }
                    )
                disposableList.add(disposable)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.settingDogList)
    }

    private fun requestDog() {
        listener?.showLoading()
        val disposable = dogListCase.getDogListFromLocal()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { dogList ->
                    this.dogList.clear()
                    this.dogList += dogList
                    dogListAdapter?.replaceList(this.dogList)
                    listener?.hideLoading()
                },
                {
                    Timber.e("SettingDogFragment#requestDog: ${it.localizedMessage}")
                }
            )
        disposableList.add(disposable)
    }

    private fun onClickAddDog() {
        binding.settingDogEdit.show(null, addButtonCenter) { canceled, dogPhotoDegree ->
            binding.settingDogAdd.isVisible = true
            if (canceled) {
                return@show
            }

            listener?.showLoading()
            val dog = binding.settingDogEdit.dog
            val disposable = dogListCase.add(dog, dogPhotoDegree)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { resultDog ->
                        if (resultDog.dogId.isEmpty()) {
                            Timber.e("SettingDogFragment#onClickAddDog: dogId is empty")
                        } else {
                            dogList.add(resultDog)
                            dogListAdapter?.saveDog(resultDog)
                        }
                        listener?.hideLoading()
                    },
                    {
                        Timber.e("SettingDogFragment#onClickAddDog: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }
        binding.settingDogAdd.isVisible = false
    }

    private fun onEditing(dog: Dog, position: PointF) {
        val listPosition = IntArray(2)
        binding.settingDogList.getLocationOnScreen(listPosition)
        val targetPoint = PointF(position.x, position.y - listPosition[1])

        binding.settingDogEdit.show(dog, targetPoint) { canceled, dogPhotoDegree ->
            binding.settingDogAdd.isVisible = true
            if (canceled) {
                return@show
            }

            listener?.showLoading()
            val editedDog = binding.settingDogEdit.dog
            val disposable = dogListCase.edit(editedDog, dogPhotoDegree)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { resultDog ->
                        if (resultDog.dogId.isEmpty()) {
                            Timber.e("SettingDogFragment#onEditing: dogId is empty")
                        }
                        requestDog()
                        listener?.hideLoading()
                    },
                    {
                        Timber.e("SettingDogFragment#onEditing: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }
        binding.settingDogAdd.isVisible = false
    }


    private fun onClickPhoto() {
        showPictureDialog()
    }

    private var cameraUri: Uri? = null

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(context)
        val pictureDialogItems = arrayOf(
            getString(R.string.photo_from_gallery),
            getString(R.string.photo_from_camera),
        )

        pictureDialog.setItems(pictureDialogItems) { _, which ->
            when (which) {
                0 -> galleryContract.launch(null)
                1 -> cameraContract.launch(null)
            }
        }
        pictureDialog.show()
    }

    private fun onResultGallery(result: GalleryResult) {
        if (result.isCanceled) {
            return
        }
        result.uri ?: return

        dogListCase.savePhotoToCache(result.uri, binding.settingDogEdit.dog)
        binding.settingDogEdit.updatedPhoto()
    }

    private fun onResultCamera(result: CameraResult) {
        if (result.isCanceled) {
            return
        }
        result.bitmap ?: return

        dogListCase.savePhotoToCache(result.bitmap, binding.settingDogEdit.dog)
        binding.settingDogEdit.updatedPhoto()
    }

    private fun onClickBirthday() {
        val date = binding.settingDogEdit.dog.birthday ?: CalendarEx.today
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            binding.settingDogEdit.updateBirthday(
                Calendar.getInstance().apply {
                    this[Calendar.YEAR] = year
                    this[Calendar.MONTH] = month
                    this[Calendar.DAY_OF_MONTH] = dayOfMonth
                    this[Calendar.HOUR_OF_DAY] = 0
                    this[Calendar.MINUTE] = 0
                    this[Calendar.SECOND] = 0
                    this[Calendar.MILLISECOND] = 0
                }
            )
        }

        DatePickerDialog(
            requireContext(),
            listener,
            date[Calendar.YEAR],
            date[Calendar.MONTH],
            date[Calendar.DAY_OF_MONTH],
        ).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDismissDialog(event: OnDismissDialog) {
        if (event.requestCode == REQUEST_DIALOG_BIRTHDAY) {
            if (event.isCancel) {
                return
            }
            val millis = event.result?.getLong(BowDatePickerDialog.RESULT_KEY_DATE, 0L) ?: 0L
            if (millis == 0L) return
            binding.settingDogEdit.updateBirthday(Calendar.getInstance().apply { timeInMillis = millis })
        }
    }

}