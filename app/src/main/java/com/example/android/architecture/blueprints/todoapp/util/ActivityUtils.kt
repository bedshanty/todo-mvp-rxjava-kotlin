package com.example.android.architecture.blueprints.todoapp.util

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager

class ActivityUtils {

    companion object {
        fun addFragmentToActivity(fragmentManager: FragmentManager,
                                         fragment: Fragment, frameId: Int) {
            val transaction = fragmentManager.beginTransaction()
            transaction.add(frameId, fragment)
            transaction.commit()
        }
    }
}