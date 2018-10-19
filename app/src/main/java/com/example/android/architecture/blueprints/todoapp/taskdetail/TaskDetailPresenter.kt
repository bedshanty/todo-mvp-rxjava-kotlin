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
package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.google.common.base.Strings
import io.reactivex.disposables.CompositeDisposable

/**
 * Listens to user actions from the UI ([TaskDetailFragment]), retrieves the data and updates
 * the UI as required.
 */
class TaskDetailPresenter(
        private val taskId: String,
        private val tasksRepository: TasksRepository,
        private val taskDetailView: TaskDetailContract.View,
        private val schedulerProvider: BaseSchedulerProvider
) : TaskDetailContract.Presenter {

    private val compositeDisposable = CompositeDisposable()

    init {
        taskDetailView.presenter = this
    }

    override fun subscribe() {
        openTask()
    }

    override fun unsubscribe() {
        compositeDisposable.clear()
    }

    private fun openTask() {
        if (Strings.isNullOrEmpty(taskId)) {
            taskDetailView.showMissingTask()
            return
        }

        taskDetailView.setLoadingIndicator(true)
        compositeDisposable.add(tasksRepository
                .getTask(taskId)
                .filter { it.isPresent }
                .map { it.get() }
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        { if (it != null) showTask(it) },
                        { },
                        { taskDetailView.setLoadingIndicator(false) }))
    }

    override fun editTask() {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
            return
        }
        taskDetailView.showEditTask(taskId)
    }

    override fun deleteTask() {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
            return
        }
        tasksRepository.deleteTask(taskId)
        taskDetailView.showTaskDeleted()
    }

    override fun completeTask() {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
            return
        }
        tasksRepository.completeTask(taskId)
        taskDetailView.showTaskMarkedComplete()
    }

    override fun activateTask() {
        if (taskId.isEmpty()) {
            taskDetailView.showMissingTask()
            return
        }
        tasksRepository.activateTask(taskId)
        taskDetailView.showTaskMarkedActive()
    }

    private fun showTask(task: Task) {
        with(taskDetailView) {
            task.title.let {
                if (Strings.isNullOrEmpty(it)) {
                    hideTitle()
                } else {
                    showTitle(it)
                }
            }

            task.description.let {
                if (Strings.isNullOrEmpty(it)) {
                    hideDescription()
                } else {
                    showDescription(it)
                }
            }

            showCompletionStatus(task.completed)
        }
    }
}
