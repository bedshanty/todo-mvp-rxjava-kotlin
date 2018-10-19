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

package com.example.android.architecture.blueprints.todoapp.addedittask

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable

/**
 * Listens to user actions from the UI ([AddEditTaskFragment]), retrieves the data and updates
 * the UI as required.
 * @param taskId ID of the task to edit or null for a new task
 *
 * @param tasksRepository a repository of data for tasks
 *
 * @param addTaskView the add/edit view
 *
 * @param isDataMissing whether data needs to be loaded or not (for config changes)
 */
class AddEditTaskPresenter(
        private val taskId: String?,
        val tasksRepository: TasksDataSource,
        val addTaskView: AddEditTaskContract.View,
        override var isDataMissing: Boolean,
        val schedulerProvider: BaseSchedulerProvider
) : AddEditTaskContract.Presenter {

    private val isNewTask: Boolean
    get() {
        return taskId == null
    }

    private val compositeDisposable = CompositeDisposable()

    init {
        addTaskView.presenter = this
    }

    override fun subscribe() {
        if (taskId != null && isDataMissing) {
            populateTask()
        }
    }

    override fun unsubscribe() {
        compositeDisposable.clear()
    }

    override fun saveTask(title: String?, description: String?) {
        if (isNewTask) {
            createTask(title, description)
        } else {
            updateTask(title, description)
        }
    }

    override fun populateTask() {
        if(isNewTask) {
            throw java.lang.RuntimeException("populateTask() was called but task is new")
        }

        if(taskId != null) {

            compositeDisposable.add(tasksRepository
                    .getTask(taskId)
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(
                            {
                                if (it.isPresent) {
                                    it.get().let {
                                        if (addTaskView.isActive) {
                                            addTaskView.setTitle(it!!.title)
                                            addTaskView.setDescription(it.description)
                                        }
                                    }
                                } else {
                                    if (addTaskView.isActive) {
                                        addTaskView.showEmptyTaskError()
                                    }
                                }
                            },
                            {
                                if (addTaskView.isActive) {
                                    addTaskView.showEmptyTaskError()
                                }
                            }))
        }
    }

    private fun createTask(title: String?, description: String?) {
        val newTask = Task(title, description)
        if (newTask.isEmpty()) {
            addTaskView.showEmptyTaskError()
        } else {
            tasksRepository.saveTask(newTask)
            addTaskView.showTasksList()
        }
    }

    private fun updateTask(title: String?, description: String?) {
        if (taskId == null) {
            throw RuntimeException("updateTask() was called but task is new.")
        }
        tasksRepository.saveTask(Task(title, description, taskId))
        addTaskView.showTasksList() // After an edit, go back to the list.
    }
}
