/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.android.architecture.blueprints.todoapp.Injection
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.util.ActivityUtils

/**
 * Displays task details screen.
 */
class TaskDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.taskdetail_act)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        // Get the requested task id
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)

        var taskDetailFragment: TaskDetailFragment? = supportFragmentManager
                .findFragmentById(R.id.contentFrame) as? TaskDetailFragment

        if (taskDetailFragment == null) {
            taskDetailFragment = TaskDetailFragment.newInstance(taskId)

            ActivityUtils.addFragmentToActivity(supportFragmentManager, taskDetailFragment, R.id.contentFrame)
        }
        // Create the presenter
        TaskDetailPresenter(taskId, Injection.provideTasksRepository(applicationContext),
                taskDetailFragment, Injection.provideSchedulerProvider())
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_TASK_ID = "TASK_ID"
    }
}
