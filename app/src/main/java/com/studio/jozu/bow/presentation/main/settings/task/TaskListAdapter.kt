package com.studio.jozu.bow.presentation.main.settings.task

import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.studio.jozu.bow.R
import com.studio.jozu.bow.domain.Task

class TaskListAdapter : RecyclerView.Adapter<TaskListAdapter.TaskViewHolder>() {
    class TaskViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val viewIcon: ImageView = view.findViewById(R.id.task_list_row_icon)
        val viewName: TextView = view.findViewById(R.id.task_list_row_name)
        val viewEdit: ImageView = view.findViewById(R.id.task_list_row_edit)
        val viewVisibilityOff: ImageView = view.findViewById(R.id.task_list_row_visibility_off)
    }

    private var taskList = listOf<Task>()
    var onEditing: ((task: Task, targetPosition: PointF) -> Unit)? = null

    fun saveTask(editedTask: Task) {
        var changePosition = 0
        val newTaskList = mutableListOf<Task>()

        //既存のタスクが編集されている場合は置き換える
        taskList.forEachIndexed { index, listTask ->
            if (listTask.taskId == editedTask.taskId) {
                newTaskList.add(editedTask)
                changePosition = index
            } else {
                newTaskList.add(listTask)
            }
        }

        // 新規タスクの場合は追加する
        if (taskList.find { it.taskId == editedTask.taskId } == null) {
            newTaskList.add(editedTask)
            changePosition = newTaskList.count() - 1
        }

        this.taskList = newTaskList
        notifyItemChanged(changePosition)
    }

    fun replaceList(newTaskList: List<Task>) {
        this.taskList = newTaskList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.setting_task_edit_list_row, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val rowTask = taskList[position]
        holder.viewName.text = rowTask.title
        holder.viewIcon.setImageResource(rowTask.icon.iconRes)

        holder.viewEdit.setOnClickListener { view ->
            val posArray = IntArray(2)
            view.getLocationOnScreen(posArray)
            val centerX = posArray[0] + (view.width.toFloat() / 2)
            val centerY = posArray[1] + (view.height.toFloat() / 2)
            onEditing?.invoke(rowTask, PointF(centerX, centerY))
        }

        holder.viewVisibilityOff.isInvisible = rowTask.enabled
    }

    override fun getItemCount(): Int {
        return taskList.count()
    }

}