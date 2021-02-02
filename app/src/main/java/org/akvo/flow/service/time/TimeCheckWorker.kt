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
package org.akvo.flow.service.time

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.akvo.flow.api.FlowApi
import org.akvo.flow.util.NotificationHelper
import org.akvo.flow.util.StringUtil
import timber.log.Timber
import java.io.IOException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class TimeCheckWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            val flowApi = FlowApi()
            val time = flowApi.serverTime
            if (StringUtil.isValid(time)) {
                val df: DateFormat = SimpleDateFormat(PATTERN, Locale.getDefault())
                df.timeZone = TimeZone.getTimeZone(TIMEZONE)
                df.parse(time)?.let {
                    val local = System.currentTimeMillis()
                    val onTime = abs(it.time - local) < OFFSET_THRESHOLD

                    if (!onTime) {
                        NotificationHelper.showTimeCheckNotification(applicationContext)
                    }
                }
            }
            Result.success()
        } catch (e: IOException) {
            Timber.e(e, "Error fetching time")
            Result.failure()
        } catch (e: ParseException) {
            Timber.e(e, "Error fetching time")
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "TimeCheckWorker"
        private const val OFFSET_THRESHOLD = (13 * 60 * 1000 ).toLong()// 13 minutes
        private const val PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'" // ISO 8601
        private const val TIMEZONE = "UTC"

        @JvmStatic
        fun scheduleWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val oneTimeWorkRequest = OneTimeWorkRequest.Builder(TimeCheckWorker::class.java)
                .setConstraints(constraints)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest)
        }
    }
}