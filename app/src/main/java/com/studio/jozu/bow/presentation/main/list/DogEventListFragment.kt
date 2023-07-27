package com.studio.jozu.bow.presentation.main.list

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.studio.jozu.bow.databinding.DogEventListFragmentBinding
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.Event
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.CalendarEx.isSameMonth
import com.studio.jozu.bow.domain.extension.DisposableListEx.dispose
import com.studio.jozu.bow.infrastructure.event.EventBusManager
import com.studio.jozu.bow.infrastructure.event.model.OnAddDogEvent
import com.studio.jozu.bow.presentation.main.MainActivityFragmentListener
import com.studio.jozu.bow.usecase.CalendarCase
import com.studio.jozu.bow.usecase.DogListCase
import com.studio.jozu.bow.usecase.EventCase
import com.studio.jozu.bow.usecase.TaskListCase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.lang.Integer.max
import java.util.*
import javax.inject.Inject

class DogEventListFragment : Fragment() {
    companion object {
        /**
         * DogEventListFragmentの生成を行う
         */
        fun newInstance(): DogEventListFragment {
            return DogEventListFragment().apply {
                arguments = Bundle().apply {
                }
            }
        }
    }

    @Inject
    lateinit var calendarCase: CalendarCase

    @Inject
    lateinit var dogListCase: DogListCase

    @Inject
    lateinit var eventCase: EventCase

    @Inject
    lateinit var taskListCase: TaskListCase

    @Inject
    lateinit var eventBusManager: EventBusManager

    private var listener: MainActivityFragmentListener? = null

    private val allMonthList by lazy { calendarCase.allMonthList.reversed() }
    private var loadedIndex = -1

    private val dogList = mutableListOf<Dog>()
    private val taskList = mutableListOf<Task>()
    private val eventList = mutableListOf<DogEventListModel>()

    private val disposableList = mutableListOf<Disposable>()

    private var binding: DogEventListFragmentBinding? = null

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
        binding = DogEventListFragmentBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        disposableList.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.dogEventList.adapter = DogEventListAdapter(::onSelectedEvent, ::onReadMore)
        binding.dogEventList.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        loadDog {
            loadTask {
                if (loadedIndex < 0) {
                    val fromIndex = 0
                    val toIndex = 1
                    loadEvent(fromIndex, toIndex)
                }
            }
        }
    }

    private fun loadDog(onCompleted: () -> Unit) {
        if (dogList.isNotEmpty()) {
            onCompleted.invoke()
            return
        }

        val disposable = dogListCase.getDogListFromLocal()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { dogList ->
                    addDogList(dogList)
                    onCompleted.invoke()
                },
                {
                    Timber.e("DogEventList#loadDog ${it.localizedMessage}")
                    Handler(Looper.getMainLooper()).post {
                        onCompleted.invoke()
                    }
                }
            )
        disposableList.add(disposable)
    }

    private fun loadTask(onCompleted: () -> Unit) {
        if (taskList.isNotEmpty()) {
            onCompleted.invoke()
            return
        }

        taskListCase.getTaskListFromLocal()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { taskList ->
                    addTaskList(taskList)
                    onCompleted.invoke()
                },
                {
                    Timber.e("DogEventList#loadTask ${it.localizedMessage}")
                    Handler(Looper.getMainLooper()).post {
                        onCompleted.invoke()
                    }
                }
            )
    }

    private fun loadEvent(fromIndex: Int, toIndex: Int) {
        val from = allMonthList[fromIndex].month
        val to = allMonthList[toIndex].month
        val fromMonth: Calendar
        val toMonth: Calendar
        if (from <= to) {
            fromMonth = from
            toMonth = to
        } else {
            fromMonth = to
            toMonth = from
        }

        loadedIndex = max(fromIndex, toIndex)
        loadEvent(fromMonth, toMonth)
    }

    private fun loadEvent(fromMonth: Calendar, toMonth: Calendar) {
        eventCase.find(fromMonth, toMonth)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { eventList ->
                    addEventList(eventList)
                },
                {
                    Timber.e("DogEventList#event.find ${it.localizedMessage}")
                }
            )
    }

    private fun addDogList(dogList: List<Dog>) {
        dogList.forEach { dog ->
            if (this.dogList.find { it.dogId == dog.dogId } == null) {
                this.dogList.add(dog)
            }
        }
        this.dogList.sortBy { it.order }
    }

    private fun addTaskList(taskList: List<Task>) {
        taskList.forEach { task ->
            if (this.taskList.find { it.taskId == task.taskId } == null) {
                this.taskList.add(task)
            }
        }
        this.taskList.sortBy { it.order }
    }

    private fun addEventList(eventList: List<Event>) {
        val binding = binding ?: return
        this.eventList.removeIf { it.viewType == DogEventListViewType.READ_MORE }

        eventList
            .sortedByDescending { it.timestamp }
            .forEach { event ->
                dogList.find { it.dogId == event.dogId }?.let { dog ->
                    taskList.find { it.taskId == event.taskId }?.let { task ->
                        val lastEvent = this.eventList.lastOrNull() as? DogEventListModelEvent
                        if (lastEvent == null || !lastEvent.event.timestamp.isSameMonth(event.timestamp)) {
                            val monthModel = DogEventListModelMonth(event.timestamp)
                            this.eventList.add(monthModel)
                        }

                        val index = this.eventList.indexOfFirst { listEvent ->
                            (listEvent as? DogEventListModelEvent)?.event?.eventId?.let { it == event.eventId } ?: false
                        }
                        val eventModel = DogEventListModelEvent(
                            event = event,
                            dog = dog,
                            task = task,
                        )
                        if (index < 0) {
                            this.eventList.add(eventModel)
                        } else {
                            this.eventList[index] = eventModel
                        }
                    }
                }
            }

        if (loadedIndex + 1 < allMonthList.count()) {
            this.eventList.add(DogEventListModelReadMore())
        }

        (binding.dogEventList.adapter as DogEventListAdapter).replaceEventList(this.eventList)
    }

    private fun onSelectedEvent(eventModel: DogEventListModelEvent) {
        editEvent(eventModel.event, eventModel.dog, eventModel.task)
    }

    private fun editEvent(event: Event, dog: Dog, task: Task) {
        val binding = binding ?: return
        binding.dogEventListDogSelectionView.setDogList(dogList)
        binding.dogEventListDogSelectionView.setDefaultDog(dog)
        binding.dogEventListDogSelectionView.setTask(task)
        binding.dogEventListDogSelectionView.setTimestamp(event.timestamp)
        binding.dogEventListDogSelectionView.canDelete = true
        binding.dogEventListDogSelectionView.isVisible = true

        binding.dogEventListDogSelectionView.onCancel = {
            binding.dogEventListDogSelectionView.isVisible = false
        }

        binding.dogEventListDogSelectionView.onSelectedDog = { _, timestamp, dogs ->
            val disposable = eventCase.edit(event, task, dogs, timestamp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        binding.dogEventListDogSelectionView.isVisible = false
                        this.eventList.clear()
                        loadEvent(0, loadedIndex)
                    },
                    {
                        Timber.e("CalendarPagerViewHolder#editEvent: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }

        binding.dogEventListDogSelectionView.onDeleteEvent = {
            val disposable = eventCase.delete(event)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        binding.dogEventListDogSelectionView.isVisible = false
                        this.eventList.clear()
                        loadEvent(0, loadedIndex)
                    },
                    {
                        Timber.e("CalendarPagerViewHolder#editEvent: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }
    }

    private fun onReadMore() {
        loadEvent(loadedIndex + 1, loadedIndex + 1)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAddDogEvent(event: OnAddDogEvent) {
        this.eventList.clear()
        loadEvent(0, loadedIndex)
    }
}