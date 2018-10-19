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
package com.example.android.architecture.blueprints.todoapp.data.source

import android.support.annotation.VisibleForTesting
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.google.common.base.Optional
import io.reactivex.Flowable
import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
class TasksRepository(
        val tasksRemoteDataSource: TasksDataSource,
        val tasksLocalDataSource: TasksDataSource
) : TasksDataSource {

    companion object {

        private var isNeededToMakeInstance = true
        private lateinit var INSTANCE: TasksRepository

        fun getInstance(tasksRemoteDataSource: TasksDataSource,
                        tasksLocalDataSource: TasksDataSource) : TasksRepository {
            if(isNeededToMakeInstance) {
                INSTANCE = TasksRepository(tasksRemoteDataSource, tasksLocalDataSource)
                isNeededToMakeInstance = false
            }

            return INSTANCE
        }
    }

    private val mTasksRemoteDataSource = tasksRemoteDataSource

    private val mTasksLocalDataSource = tasksLocalDataSource

    @VisibleForTesting
    internal var mCachedTask: MutableMap<String, Task>? = null

    private var mCacheIsDirty = false

    override fun getTasks(): Flowable<List<Task>> {
        if(mCachedTask != null && !mCacheIsDirty) {
            return Flowable.fromIterable(mCachedTask!!.values).toList().toFlowable()
        } else if(mCachedTask == null) {
            mCachedTask = LinkedHashMap()
        }

        val remoteTasks = getAndSaveRemoteTasks()

        return if(mCacheIsDirty) {
            remoteTasks
        } else {
            val localTasks = getAndCacheLocalTasks()
            Flowable.concat(localTasks, remoteTasks)
                    .filter { !it.isEmpty() }
                    .firstOrError()
                    .toFlowable()
        }
    }

    private fun getAndCacheLocalTasks(): Flowable<List<Task>> =
            mTasksLocalDataSource.getTasks()
                    .flatMap { Flowable.fromIterable(it) }
                    .doOnNext { mCachedTask!![it.id] = it }
                    .toList()
                    .toFlowable()

    private fun getAndSaveRemoteTasks(): Flowable<List<Task>> =
            mTasksRemoteDataSource.getTasks()
                    .flatMap { Flowable.fromIterable(it) }
                    .doOnNext {
                        mTasksLocalDataSource.saveTask(it)
                        mCachedTask!![it.id] = it
                    }
                    .toList()
                    .toFlowable()

    override fun getTask(taskId: String): Flowable<Optional<Task?>> {
        val cachedTask = getTaskWithId(taskId)

        if(cachedTask != null) {
            return Flowable.just(Optional.of(cachedTask))
        }

        if(mCachedTask == null) {
            mCachedTask = LinkedHashMap()
        }

        val localTask = getTaskWithIdFromLocalRepository(taskId)
        val remoteTask = mTasksRemoteDataSource
                .getTask(taskId)
                .doOnNext {
                    if(it.isPresent) {
                        it.get().let {
                            if(it != null) {
                                mTasksLocalDataSource.saveTask(it)
                                mCachedTask!![it.id] = it
                            }
                        }
                    }
                }

        return Flowable.concat(localTask, remoteTask)
                .firstElement()
                .toFlowable()
    }

    private fun getTaskWithId(id: String?): Task? {
        return if(mCachedTask == null || mCachedTask!!.isEmpty()) {
            null
        } else {
            mCachedTask!![id]
        }
    }

    internal fun getTaskWithIdFromLocalRepository(taskId: String) =
            mTasksLocalDataSource
                    .getTask(taskId)
                    .doOnNext {
                        if(it.isPresent) {
                            it.get().let {
                                if(it != null) {
                                    mCachedTask!![taskId] = it
                                }
                            }
                        }
                    }
                    .firstElement()
                    .toFlowable()

    override fun saveTask(task: Task) {
        mTasksRemoteDataSource.saveTask(task)
        mTasksLocalDataSource.saveTask(task)

        if(mCachedTask == null) {
            mCachedTask = LinkedHashMap()
        }

        mCachedTask!![task.id] = task
    }

    override fun completeTask(task: Task) {
        mTasksRemoteDataSource.completeTask(task)
        mTasksLocalDataSource.completeTask(task)

        val completedTask = Task(task.title, task.description, task.id, true)

        if(mCachedTask == null) {
            mCachedTask = LinkedHashMap()
        }

        mCachedTask!![task.id] = completedTask
    }

    override fun completeTask(taskId: String) {
        getTaskWithId(taskId).let {
            if(it != null) {
                completeTask(it)
            }
        }
    }

    override fun activateTask(task: Task) {
        mTasksRemoteDataSource.activateTask(task)
        mTasksLocalDataSource.activateTask(task)

        val activateTask = Task(task.title, task.description, task.id)

        if(mCachedTask == null) {
            mCachedTask = LinkedHashMap()
        }

        mCachedTask!![task.id] = activateTask
    }

    override fun activateTask(taskId: String) {
        getTaskWithId(taskId).let {
            if(it != null) {
                activateTask(it)
            }
        }
    }

    override fun clearCompletedTasks() {
        mTasksRemoteDataSource.clearCompletedTasks()
        mTasksLocalDataSource.clearCompletedTasks()

        if(mCachedTask != null) {
            mCachedTask = LinkedHashMap()
        }

        mCachedTask!!.entries.iterator().let {
            while(it.hasNext()) {
                val entry = it.next()
                if(entry.value.completed) {
                    it.remove()
                }
            }
        }
    }

    override fun refreshTasks() {
        mCacheIsDirty = true
    }

    override fun deleteAllTasks() {
        mTasksRemoteDataSource.deleteAllTasks()
        mTasksLocalDataSource.deleteAllTasks()

        if(mCachedTask == null) {
            mCachedTask = LinkedHashMap()
        }

        mCachedTask!!.clear()
    }

    override fun deleteTask(taskId: String) {
        mTasksRemoteDataSource.deleteTask(taskId)
        mTasksLocalDataSource.deleteTask(taskId)

        mCachedTask!!.remove(taskId)
    }
}