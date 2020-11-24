/*
 * Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.interactor.DefaultObserver
import org.akvo.flow.domain.interactor.forms.DownloadForms
import org.akvo.flow.util.ConstantUtil
import org.akvo.flow.util.NotificationHelper
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * This worker will check for new surveys on the device and install as needed
 *
 */
class SurveyDownloadWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    @Inject
    lateinit var downloadForms: DownloadForms

    init {
        val application = applicationContext as FlowApp
        application.getApplicationComponent().inject(this)
    }

    override fun doWork(): Result {
        NotificationHelper.displayFormsSyncingNotification(applicationContext)
        downloadForms.execute<Int>(object : DefaultObserver<Int?>() {
            override fun onError(e: Throwable) {
                Timber.e(e)
                NotificationHelper.displayErrorNotification(
                    applicationContext.getString(R.string.error_form_sync_title),
                    "",
                    applicationContext,
                    ConstantUtil.NOTIFICATION_FORM
                )
            }

            override fun onNext(downloaded: Int) {
                NotificationHelper.displayFormsSyncedNotification(applicationContext, downloaded)
            }
        })
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        downloadForms.dispose()
    }

    companion object {

        private const val TAG = "SurveyDownloadService"

        @JvmStatic
        fun scheduleWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val surveyDownloadRequest = OneTimeWorkRequest.Builder(SurveyDownloadWorker::class.java)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, surveyDownloadRequest)
        }
    }
}
