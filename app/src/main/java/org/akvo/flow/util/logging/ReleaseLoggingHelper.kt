/*
 * Copyright (C) 2016-2019 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.akvo.flow.util.logging

import android.text.TextUtils
import com.crashlytics.android.Crashlytics
import org.akvo.flow.BuildConfig
import timber.log.Timber

class ReleaseLoggingHelper : LoggingHelper {

    override fun init() {
        Crashlytics.setString(GAE_INSTANCE_ID_TAG_KEY, BuildConfig.AWS_BUCKET)
        Timber.plant(CrashlyticsTree())
    }

    override fun initLoginData(username: String?, deviceId: String?) {
        if (!TextUtils.isEmpty(username)) {
            Crashlytics.setString("user", username)
        }
        if (!TextUtils.isEmpty(deviceId)) {
            Crashlytics.setString(DEVICE_ID_TAG_KEY, deviceId)
        }
    }

    override fun clearUser() {
        Crashlytics.setString("user", "")
    }

    companion object {
        private const val GAE_INSTANCE_ID_TAG_KEY = "flow.gae.instance"
        private const val DEVICE_ID_TAG_KEY = "flow.device.id"
    }

}