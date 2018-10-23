package com.example.android.architecture.blueprints.todoapp.tasks

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.example.android.architecture.blueprints.todoapp.R

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.hannesdorfmann.adapterdelegates3.AdapterDelegate

class TasksAdapterDelegate(activity: Activity, var tasks: List<Task>, private val itemListener: TasksFragment.TaskItemListener) : AdapterDelegate<List<Task>>() {

    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<Task>, position: Int) = true

    override fun onCreateViewHolder(parent: ViewGroup)
            = TasksViewHolder(inflater.inflate(R.layout.task_item, parent, false))

    override fun onBindViewHolder(items: List<Task>, position: Int, holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        val vh = holder as TasksViewHolder
        val task = items[position]
        val rowViewBackground =
                if (task.completed) R.drawable.list_completed_touch_feedback
                else R.drawable.touch_feedback

        holder.itemView.setBackgroundResource(rowViewBackground)
        holder.itemView.setOnClickListener { itemListener.onTaskClick(task) }

        vh.title.text = task.title

        with(vh.complete) {
            isChecked = task.completed
            setOnClickListener {
                if (!task.completed) {
                    itemListener.onCompleteTaskClick(task)
                } else {
                    itemListener.onActivateTaskClick(task)
                }
            }
        }
    }

    class TasksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val title: TextView = itemView.findViewById(R.id.title)
        val complete: CheckBox = itemView.findViewById(R.id.complete)
    }
}
