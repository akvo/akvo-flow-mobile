/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.support.net

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import org.akvo.flow.support.BuildConfig

class BasicAuthInterceptor : Interceptor {

    private val credentials: String by lazy { Credentials.basic(BuildConfig.USER, BuildConfig.PASSWORD) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().header("Authorization", credentials).build()
        return chain.proceed(request)
    }
}
