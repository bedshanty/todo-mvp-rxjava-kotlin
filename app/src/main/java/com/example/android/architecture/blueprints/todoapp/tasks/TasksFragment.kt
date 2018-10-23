/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.tasks

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailActivity
import com.hannesdorfmann.adapterdelegates3.AdapterDelegatesManager
import java.util.*

/**
 * Display a grid of [Task]s. User can choose to view all, active or completed tasks.
 */
class TasksFragment : Fragment(), TasksContract.View {

    override lateinit var presenter: TasksContract.Presenter

    override var isActive: Boolean = false
        get() = isAdded

    private lateinit var noTasksView: View
    private lateinit var noTaskIcon: ImageView
    private lateinit var noTaskMainView: TextView
    private lateinit var noTaskAddView: TextView
    private lateinit var tasksView: LinearLayout
    private lateinit var filteringLabelView: TextView

    /**
     * Listener for clicks on tasks in the ListView.
     */
    private var itemListener: TaskItemListener = object : TaskItemListener {
        override fun onTaskClick(clickedTask: Task) {
            presenter.openTaskDetails(clickedTask)
        }

        override fun onCompleteTaskClick(completedTask: Task) {
            presenter.completeTask(completedTask)
        }

        override fun onActivateTaskClick(activatedTask: Task) {
            presenter.activateTask(activatedTask)
        }
    }

    private val listAdapter by lazy { TasksAdapter(ArrayList(0), itemListener) }

    override fun onResume() {
        super.onResume()
        presenter.subscribe()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        presenter.result(requestCode, resultCode)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.tasks_frag, container, false)

        // Set up tasks view
        with(root) {
            val recyclerView = findViewById<RecyclerView>(R.id.tasks_list).apply {
                adapter = listAdapter
                layoutManager = LinearLayoutManager(activity)
            }

            // Set up progress indicator
            findViewById<ScrollChildSwipeRefreshLayout>(R.id.refresh_layout).apply {
                setColorSchemeColors(
                        ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                        ContextCompat.getColor(requireContext(), R.color.colorAccent),
                        ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                )
                // Set the scrolling view in the custom SwipeRefreshLayout.
                scrollUpChild = recyclerView
                setOnRefreshListener { presenter.loadTasks(false) }
            }

            filteringLabelView = findViewById(R.id.filteringLabel)
            tasksView = findViewById(R.id.tasksLL)

            // Set up  no tasks view
            noTasksView = findViewById(R.id.noTasks)
            noTaskIcon = findViewById(R.id.noTasksIcon)
            noTaskMainView = findViewById(R.id.noTasksMain)
            noTaskAddView = (findViewById<TextView>(R.id.noTasksAdd)).also { it ->
                it.setOnClickListener { showAddTask() }
            }
        }

        // Set up floating action button
        val fab = activity!!.findViewById<FloatingActionButton>(R.id.fab_add_task)

        fab.apply {
            setImageResource(R.drawable.ic_add)
            setOnClickListener { presenter.addNewTask() }
        }

        setHasOptionsMenu(true)

        return root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> presenter.clearCompletedTasks()
            R.id.menu_filter -> showFilteringPopUpMenu()
            R.id.menu_refresh -> presenter.loadTasks(true)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.tasks_fragment_menu, menu)
    }

    override fun showFilteringPopUpMenu() {
        val activity = activity ?: return
        val context = context ?: return
        PopupMenu(context, activity.findViewById(R.id.menu_filter)).apply {
            menuInflater.inflate(R.menu.filter_tasks, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.active -> presenter.currentFiltering = TasksFilterType.ACTIVE_TASKS
                    R.id.completed -> presenter.currentFiltering = TasksFilterType.COMPLETED_TASKS
                    else -> presenter.currentFiltering = TasksFilterType.ALL_TASKS
                }
                presenter.loadTasks(false)
                true
            }
            show()
        }
    }

    override fun setLoadingIndicator(active: Boolean) {
        val root = view ?: return
        with(root.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)) {
            // Make sure setRefreshing() is called after the layout is done with everything else.
            post { isRefreshing = active }
        }
    }

    override fun showTasks(tasks: List<Task>) {

        listAdapter.tasks = tasks
        listAdapter.notifyDataSetChanged()
        tasksView.visibility = View.VISIBLE
        noTasksView.visibility = View.GONE
    }

    override fun showNoActiveTasks() {
        showNoTasksViews(resources.getString(R.string.no_tasks_active), R.drawable.ic_check_circle_24dp, false)
    }

    override fun showNoTasks() {
        showNoTasksViews(resources.getString(R.string.no_tasks_all), R.drawable.ic_assignment_turned_in_24dp, false)
    }

    override fun showNoCompletedTasks() {
        showNoTasksViews(resources.getString(R.string.no_tasks_completed), R.drawable.ic_verified_user_24dp, false)
    }

    override fun showSuccessfullySavedMessage() {
        showMessage(getString(R.string.successfully_saved_task_message))
    }

    private fun showNoTasksViews(mainText: String, iconRes: Int, showAddView: Boolean) {
        tasksView.visibility = View.GONE
        noTasksView.visibility = View.VISIBLE

        noTaskMainView.text = mainText
        noTaskIcon.setImageResource(iconRes)
        noTaskAddView.visibility = if (showAddView) View.VISIBLE else View.GONE
    }

    override fun showActiveFilterLabel() {
        filteringLabelView.text = resources.getString(R.string.label_active)
    }

    override fun showCompletedFilterLabel() {
        filteringLabelView.text = resources.getString(R.string.label_completed)
    }

    override fun showAllFilterLabel() {
        filteringLabelView.text = resources.getString(R.string.label_all)
    }

    override fun showAddTask() {
        val intent = Intent(context, AddEditTaskActivity::class.java)
        startActivityForResult(intent, AddEditTaskActivity.REQUEST_ADD_TASK)
    }

    override fun showTaskDetailsUi(taskId: String) {
        // in it's own Activity, since it makes more sense that way and it gives us the flexibility
        // to show some Intent stubbing.
        val intent = Intent(context, TaskDetailActivity::class.java).apply {
            putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId)
        }
        startActivity(intent)
    }

    override fun showTaskMarkedComplete() {
        showMessage(getString(R.string.task_marked_complete))
    }

    override fun showTaskMarkedActive() {
        showMessage(getString(R.string.task_marked_active))
    }

    override fun showCompletedTasksCleared() {
        showMessage(getString(R.string.completed_tasks_cleared))
    }

    override fun showLoadingTasksError() {
        showMessage(getString(R.string.loading_tasks_error))
    }

    private fun showMessage(message: String) {
        Snackbar.make(view!!, message, Snackbar.LENGTH_LONG).show()
    }

    private inner class TasksAdapter(tasks: List<Task>, itemListener: TaskItemListener)
        : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var tasks: List<Task> = tasks
            set(tasks) {
                field = tasks
                notifyDataSetChanged()
            }

        val delegates = AdapterDelegatesManager<List<Task>>()

        init {

            delegates.addDelegate(TasksAdapterDelegate(activity!!, ArrayList(0), itemListener))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = delegates.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = tasks.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = delegates.onBindViewHolder(tasks, position, holder)
    }

    interface TaskItemListener {

        fun onTaskClick(clickedTask: Task)

        fun onCompleteTaskClick(completedTask: Task)

        fun onActivateTaskClick(activatedTask: Task)
    }

    companion object {

        fun newInstance() = TasksFragment()
    }

}
