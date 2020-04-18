package app.coinverse.utils

import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS

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

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
</T> */
data class Resource<out T>(val status: Status, val data: T?, val message: String?) {
    companion object {
        fun <T> loading(data: T?) = Resource(LOADING, data, null)
        fun <T> success(data: T?) = Resource(SUCCESS, data, null)
        fun <T> error(msg: String, data: T?) = Resource(ERROR, data, msg)
    }
}