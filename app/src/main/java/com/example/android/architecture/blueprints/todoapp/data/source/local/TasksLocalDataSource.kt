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
package com.example.android.architecture.blueprints.todoapp.data.source.local

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.support.annotation.VisibleForTesting
import android.text.TextUtils
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.google.common.base.Optional
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqlbrite2.SqlBrite
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.functions.Function


/**
 * Concrete implementation of a data source as a db.
 */
class TasksLocalDataSource private constructor(
        val context: Context,
        schedulerProvider: BaseSchedulerProvider
) : TasksDataSource {

    companion object {

        @SuppressLint("StaticFieldLeak")
        private lateinit var INSTANCE: TasksLocalDataSource
        private var isNeededToMakeInstance = true

        fun getInstance(context: Context, schedulerProvider: BaseSchedulerProvider): TasksLocalDataSource {

            if(isNeededToMakeInstance) {
                INSTANCE = TasksLocalDataSource(context, schedulerProvider)
                isNeededToMakeInstance = false
            }

            return INSTANCE
        }
    }

    private var mDatabaseHelper: BriteDatabase

    private var mTaskMapperFunction: Function<Cursor, Task>

    init {
        val dbHelper = TasksDbHelper(context)
        val sqlBrite = SqlBrite.Builder().build()
        mDatabaseHelper = sqlBrite.wrapDatabaseHelper(dbHelper, schedulerProvider.io())
        mTaskMapperFunction = Function { this.getTask(it) }
    }

    override fun getTasks(): Flowable<List<Task>> {
        val projection = arrayOf(
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID,
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_TITLE,
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_DESCRIPTION,
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED
        )

        val sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection),
                TasksPersistenceContract.TaskEntry.TABLE_NAME)
        return mDatabaseHelper.createQuery(TasksPersistenceContract.TaskEntry.TABLE_NAME, sql)
                .mapToList(mTaskMapperFunction)
                .toFlowable(BackpressureStrategy.BUFFER)
    }

    override fun getTask(taskId: String): Flowable<Optional<Task?>> {

        val projection = arrayOf(
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID,
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_TITLE,
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_DESCRIPTION,
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED)
        val sql = String.format("SELECT %s FROM %s WHERE %s LIKE ?",
                TextUtils.join(",", projection),
                TasksPersistenceContract.TaskEntry.TABLE_NAME,
                TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID)

        return mDatabaseHelper.createQuery(TasksPersistenceContract.TaskEntry.TABLE_NAME, sql, taskId)
                .mapToOneOrDefault({ cursor -> Optional.of(mTaskMapperFunction.apply(cursor)) }, Optional.absent())
                .toFlowable(BackpressureStrategy.BUFFER)
    }

    fun getTask(c: Cursor): Task {
        val itemId = c.getString(c.getColumnIndexOrThrow(TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID))
        val title = c.getString(c.getColumnIndexOrThrow(TasksPersistenceContract.TaskEntry.COLUMN_NAME_TITLE))
        val description = c.getString(c.getColumnIndexOrThrow(TasksPersistenceContract.TaskEntry.COLUMN_NAME_DESCRIPTION))
        val completed = c.getInt(c.getColumnIndexOrThrow(TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED)) == 1

        return Task(title, description, itemId, completed)
    }

    override fun saveTask(task: Task) {
        val values = ContentValues()

        values.put(TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID, task.id)
        values.put(TasksPersistenceContract.TaskEntry.COLUMN_NAME_TITLE, task.title)
        values.put(TasksPersistenceContract.TaskEntry.COLUMN_NAME_DESCRIPTION, task.description)
        values.put(TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED, task.completed)

        mDatabaseHelper.insert(TasksPersistenceContract.TaskEntry.TABLE_NAME,
                values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun completeTask(task: Task) { completeTask(task.id) }

    override fun completeTask(taskId: String) {
        val values = ContentValues()
        values.put(TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED, true)

        val selection = TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID + " Like ?"
        val selectionArgs = arrayOf(taskId)
        mDatabaseHelper.update(TasksPersistenceContract.TaskEntry.TABLE_NAME, values, selection, *selectionArgs)
    }

    override fun activateTask(task: Task) { activateTask(task.id) }

    override fun activateTask(taskId: String) {
        val values = ContentValues()
        values.put(TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED, false)

        val selection = TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID + " Like ?"
        val selectionArgs = arrayOf(taskId)
        mDatabaseHelper.update(TasksPersistenceContract.TaskEntry.TABLE_NAME,
                values, selection, *selectionArgs)
    }

    override fun clearCompletedTasks() {
        val selection = TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED + " Like ?"
        val selectionArgs = arrayOf("1")
        mDatabaseHelper.delete(TasksPersistenceContract.TaskEntry.TABLE_NAME, selection, *selectionArgs)
    }

    override fun refreshTasks() {

    }

    override fun deleteAllTasks() {
        mDatabaseHelper.delete(TasksPersistenceContract.TaskEntry.TABLE_NAME, null)
    }

    override fun deleteTask(taskId: String) {
        val selection = TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID + " Like ?"
        val selectionArgs = arrayOf(taskId)
        mDatabaseHelper.delete(TasksPersistenceContract.TaskEntry.TABLE_NAME, selection, *selectionArgs)
    }
}
