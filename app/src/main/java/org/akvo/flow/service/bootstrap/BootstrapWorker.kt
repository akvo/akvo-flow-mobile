/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.service.bootstrap

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.util.ConstantUtil
import org.akvo.flow.util.NotificationHelper
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Service that will check a well-known location on the device's SD card for a
 * zip file that contains data that should be loaded on the device.
 *
 * The zip file can contain any number of directories which can each
 * contain ONE survey (the survey xml and any help media). The name of the
 * directory must be the surveyID and the name of the survey file will be used
 * for the survey name. The system will iterate through each directory and
 * install the survey and help media contained therein.
 *
 * If the survey is already present on the device, the survey in the ZIP file will overwrite the
 * data already on the device.
 *
 * If there are multiple zip files in the directory, this utility will process them in
 * lexicographical order by file name; Any fileswith a name starting with . will be skipped (to
 * prevent inadvertent processing of MAC OSX metadata files).
 *
 */
class BootstrapWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    @Inject
    lateinit var zipFileLister: ZipFileLister

    @Inject
    lateinit var bootstrapProcessor: BootstrapProcessor

    init {
        val application = context.applicationContext as FlowApp
        application.getApplicationComponent().inject(this)
    }

    override fun doWork(): Result {
        val zipFiles = zipFileLister.listSortedZipFiles()
        if (zipFiles.isEmpty()) {
            return Result.success()
        }
        displayBootstrapStartNotification()
        val processingResult = execute(zipFiles)
        when (processingResult) {
            is ProcessingResult.ProcessingErrorWrongDashboard -> {
                displayErrorNotification(applicationContext.getString(R.string.bootstrap_invalid_app_title),
                    applicationContext.getString(R.string.bootstrap_invalid_app_message))
            }
            is ProcessingResult.ProcessingError -> {
                displayErrorNotification(applicationContext.getString(R.string.bootstraperror),
                    "")
            }
            else -> {
                displayNotification(applicationContext.getString(R.string.bootstrapcomplete))
            }
        }
        return Result.success()
    }

    /**
     * Checks the bootstrap directory for unprocessed zip files. If they are
     * found, they're processed one at a time. If an error occurs, all
     * processing stops (subsequent zips won't be processed if there are
     * multiple zips in the directory) just in case data in a later zip depends
     * on the previous one being there.
     */
    private fun execute(zipFiles: List<File>):  ProcessingResult{
        var processingResult: ProcessingResult = ProcessingResult.ProcessingSuccess
        try {
            bootstrapProcessor.openDb()

            for (file in zipFiles) {
                processingResult = bootstrapProcessor.processZipFile(file)
                if (processingResult is ProcessingResult.ProcessingSuccess) {
                    break
                }
            }
        } catch (e: Exception) {
            val errorMessage = applicationContext.getString(R.string.bootstraperror)
            displayErrorNotification(errorMessage, "")
            Timber.e(e, "Bootstrap error")
            processingResult = ProcessingResult.ProcessingError
        } finally {
            bootstrapProcessor.closeDb()
        }

        return processingResult

    }

    private fun displayBootstrapStartNotification() {
        val startMessage = applicationContext.getString(R.string.bootstrapstart)
        displayNotification(startMessage)
    }

    private fun displayErrorNotification(errorTitle: String, errorMessage: String) {
        NotificationHelper.displayErrorNotification(errorTitle,
            errorMessage,
            applicationContext,
            ConstantUtil.NOTIFICATION_BOOTSTRAP)
    }

    private fun displayNotification(message: String) {
        NotificationHelper.displayNotification(message,
            "",
            applicationContext,
            ConstantUtil.NOTIFICATION_BOOTSTRAP)
    }


    companion object {
        private const val TAG = "BootstrapWorker"

        @JvmStatic
        fun scheduleWork(context: Context) {
            val surveyDownloadRequest = OneTimeWorkRequest.Builder(BootstrapWorker::class.java)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, surveyDownloadRequest)
        }
    }
}
