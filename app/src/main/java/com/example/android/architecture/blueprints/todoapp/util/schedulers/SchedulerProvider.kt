package com.example.android.architecture.blueprints.todoapp.util.schedulers

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SchedulerProvider : BaseSchedulerProvider {

    companion object {

        private var isNeededToMakeInstance = true
        private lateinit var INSTANCE: SchedulerProvider

        fun getInstance(): SchedulerProvider {

            synchronized(this) {

                if (isNeededToMakeInstance) {
                    INSTANCE = SchedulerProvider()
                    isNeededToMakeInstance = false
                }

                return INSTANCE
            }
        }
    }

    override fun computation(): Scheduler {
        return Schedulers.computation()
    }

    override fun io(): Scheduler {
        return Schedulers.io()
    }

    override fun ui(): Scheduler {
        return AndroidSchedulers.mainThread()
    }
}