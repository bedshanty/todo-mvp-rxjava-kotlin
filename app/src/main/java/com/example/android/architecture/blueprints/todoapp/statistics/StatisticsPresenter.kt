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
package com.example.android.architecture.blueprints.todoapp.statistics


import android.util.Log
import android.util.Pair
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.google.common.primitives.Ints
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction

/**
 * Listens to user actions from the UI ([StatisticsFragment]), retrieves the data and updates
 * the UI as required.
 */
class StatisticsPresenter(
        private val tasksRepository: TasksRepository,
        private val statisticsView: StatisticsContract.View,
        private val schedulerProvider: BaseSchedulerProvider
) : StatisticsContract.Presenter {

    private val compositeDisposable = CompositeDisposable()

    init {
        statisticsView.presenter = this
    }

    override fun subscribe() {
        loadStatistics()
    }

    override fun unsubscribe() {
        compositeDisposable.clear()
    }

    private fun loadStatistics() {
        statisticsView.setProgressIndicator(true)

        EspressoIdlingResource.increment() // App is busy until further notice

        var i = 0
        val a: MutableList<String?> = MutableList(1000) {""}
        tasksRepository.getTasks().map {
            it.map {
                a.add(i, it.title)
                i++
            }
        }

        val tasks = tasksRepository
                .getTasks()
                .flatMap { Flowable.fromIterable(it) }
        val completedTasks = tasks.filter { it.completed }.count().toFlowable()
        val activeTasks = tasks.filter { it.active }.count().toFlowable()
        val disposable = Flowable
                .zip(completedTasks, activeTasks, BiFunction<Long, Long, Pair<Long, Long>> {
                    completed, active -> Pair.create(active, completed) })
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doFinally {
                    if(!EspressoIdlingResource.countingIdlingResource.isIdleNow) {
                        EspressoIdlingResource.decrement()
                    }
                }
                .subscribe (
                        { statisticsView.showStatistics(Ints.saturatedCast(it.first),
                                Ints.saturatedCast(it.second)) },
                        { statisticsView.showLoadingStatisticsError() },
                        { statisticsView.setProgressIndicator(false) })
        compositeDisposable.add(disposable)
    }
}
