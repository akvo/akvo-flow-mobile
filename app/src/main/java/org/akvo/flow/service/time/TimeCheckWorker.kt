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
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.interactor.time.FetchServerTime
import org.akvo.flow.util.NotificationHelper
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

class TimeCheckWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    lateinit var fetchServerTime: FetchServerTime

    init {
        val application = applicationContext as FlowApp
        application.getApplicationComponent().inject(this)
    }

    override suspend fun doWork(): Result {
        return try {
            val serverTime = fetchServerTime.execute()
            val local = System.currentTimeMillis()
            val onTime = serverTime == INVALID_TIME || abs(serverTime - local) < OFFSET_THRESHOLD

            if (!onTime) {
                NotificationHelper.showTimeCheckNotification(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error fetching time")
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "TimeCheckWorker"
        private const val OFFSET_THRESHOLD = (13 * 60 * 1000 ).toLong()// 13 minutes
        private const val INVALID_TIME = -1L

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
