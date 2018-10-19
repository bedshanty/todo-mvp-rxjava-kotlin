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
package com.example.android.architecture.blueprints.todoapp.data

import com.google.common.base.Strings
import java.util.*

data class Task constructor(val title: String?, val description: String?,
                            val id: String = UUID.randomUUID().toString(),
                            val completed: Boolean = false ) {

    val active: Boolean
    get() {
        return !completed
    }

    constructor(title: String?, description: String?, completed: Boolean)
            : this(title, description, UUID.randomUUID().toString(), completed)

    fun getTitleForList(): String? {
        return if(!Strings.isNullOrEmpty(title)) {
            title
        } else {
            description
        }
    }

    fun isEmpty() = Strings.isNullOrEmpty(title) && Strings.isNullOrEmpty(description)

    override fun toString(): String = "Task with title $title"
}