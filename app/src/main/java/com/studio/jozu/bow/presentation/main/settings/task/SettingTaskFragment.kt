package com.studio.jozu.bow.presentation.main.settings.task

import android.content.Context
import android.graphics.PointF
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
import com.studio.jozu.bow.databinding.SettingTaskFragmentBinding
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.DisposableListEx.dispose
import com.studio.jozu.bow.presentation.main.MainActivityFragmentListener
import com.studio.jozu.bow.usecase.TaskListCase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class SettingTaskFragment : Fragment() {
    companion object {
        /**
         * SettingTaskFragmentの生成を行う
         */
        fun newInstance(): SettingTaskFragment {
            return SettingTaskFragment()
        }
    }

    @Inject
    lateinit var taskListCase: TaskListCase

    private var listener: MainActivityFragmentListener? = null

    private var _binding: SettingTaskFragmentBinding? = null
    private val binding get() = _binding!!
    private val disposableList = mutableListOf<Disposable>()
    private var taskList = mutableListOf<Task>()

    private val addButtonCenter: PointF
        get() {
            val x = binding.settingTaskAdd.x + (binding.settingTaskAdd.width.toFloat() / 2)
            val y = binding.settingTaskAdd.y + (binding.settingTaskAdd.height.toFloat() / 2)
            return PointF(x, y)
        }

    private val taskListAdapter: TaskListAdapter
        get() = binding.settingTaskList.adapter as? TaskListAdapter ?: TaskListAdapter().apply {
            onEditing = ::onEditingTask
        }

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = SettingTaskFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        disposableList.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settingTaskEdit.isInvisible = true
        binding.settingTaskAdd.setOnClickListener { onClickAdd() }
        setUpList()
        requestTask()
    }

    private fun setUpList() {
        binding.settingTaskList.layoutManager = LinearLayoutManager(context).apply {
            orientation = RecyclerView.VERTICAL
        }

        binding.settingTaskList.adapter = taskListAdapter

        // 区切り線
        ContextCompat.getDrawable(requireContext(), R.drawable.gray_line_horizontal_1dp)?.let { dividerDrawable ->
            val dividerItemDecoration = DividerItemDecoration(context, RecyclerView.VERTICAL)
            dividerItemDecoration.setDrawable(dividerDrawable)
            binding.settingTaskList.addItemDecoration(dividerItemDecoration)
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                if (fromPos == toPos) {
                    return true
                }

                // データを更新
                val targetItem = taskList[fromPos]
                taskList.removeAt(fromPos)
                taskList.add(toPos, targetItem)
                val orderedTaskButtonList = taskList.mapIndexed { index, task ->
                    task.copy(order = index + 1)
                }
                taskList.clear()
                taskList.addAll(orderedTaskButtonList)

                // Viewを更新
                recyclerView.adapter?.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                Timber.d("SettingDogFragment#onMoved")

                val disposable = taskListCase.updateOrder(taskList)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                        },
                        {
                            Timber.e("SettingTaskFragment#onMoved: ${it.localizedMessage}")
                        }
                    )
                disposableList.add(disposable)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                Timber.d("onSwiped")
                val targetPos = viewHolder.adapterPosition
                val changeVisibilityTask = taskList[targetPos]
                val disposable = taskListCase.changeVisibility(changeVisibilityTask)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { resultTask ->
                            if (changeVisibilityTask.taskId != resultTask.taskId) {
                                Timber.e("SettingTaskFragment#onSwiped: different taskId")
                                return@subscribe
                            }

                            taskList[targetPos].enabled = resultTask.enabled
                            taskList[targetPos].updatedAt = resultTask.updatedAt.clone() as Calendar
                            taskListAdapter.notifyItemChanged(targetPos)
                        },
                        {
                            Timber.e("SettingDogFragment#onSwiped: ${it.localizedMessage}")
                        }
                    )
                disposableList.add(disposable)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.settingTaskList)
    }

    private fun requestTask() {
        listener?.showLoading()
        val disposable = taskListCase.getTaskListFromLocal()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { localTaskList ->
                    taskList.clear()
                    taskList.addAll(localTaskList)
                    taskListAdapter.replaceList(taskList)
                    listener?.hideLoading()
                },
                {
                    Timber.e("SettingTaskFragment#requestTask: ${it.localizedMessage}")
                }
            )
        disposableList.add(disposable)
    }

    private fun onClickAdd() {
        binding.settingTaskEdit.show(null, addButtonCenter) { canceled ->
            binding.settingTaskAdd.isVisible = true
            if (canceled) {
                return@show
            }

            listener?.showLoading()
            val task = binding.settingTaskEdit.task
            val disposable = taskListCase.add(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { addedTask ->
                        if (addedTask.taskId.isEmpty()) {
                            Timber.e("SettingTaskFragment#onClickAdd: taskId is empty")
                        } else {
                            taskList.add(addedTask)
                            taskListAdapter.saveTask(addedTask)
                        }
                        listener?.hideLoading()
                    },
                    {
                        Timber.e("SettingTaskFragment#onClickAdd: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }
        binding.settingTaskAdd.isVisible = false
    }

    private fun onEditingTask(task: Task, position: PointF) {
        val listPosition = IntArray(2)
        binding.settingTaskList.getLocationOnScreen(listPosition)
        val targetPosition = PointF(position.x, position.y - listPosition[1])

        binding.settingTaskEdit.show(task, targetPosition) { canceled ->
            if (canceled) {
                return@show
            }
            listener?.showLoading()
            val editedTask = binding.settingTaskEdit.task
            val disposable = taskListCase.edit(editedTask)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { resultTask ->
                        if (resultTask.taskId.isEmpty()) {
                            Timber.e("SettingTaskFragment#onEditingTask: taskId is empty")

                        }
                        requestTask()
                        listener?.hideLoading()
                    },
                    {
                        Timber.e("SettingTaskFragment#onEditingTask: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }
    }
}