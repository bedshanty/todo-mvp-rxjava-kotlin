package com.example.android.architecture.blueprints.todoapp.data.source.local

import android.provider.BaseColumns

class TasksPersistenceContract private constructor() {

    abstract class TaskEntry : BaseColumns {

        companion object {

            const val TABLE_NAME : String = "tasks"
            const val COLUMN_NAME_ENTRY_ID : String = "entryid"
            const val COLUMN_NAME_TITLE : String = "title"
            const val COLUMN_NAME_DESCRIPTION : String = "description"
            const val COLUMN_NAME_COMPLETED : String = "completed"
        }
    }
}