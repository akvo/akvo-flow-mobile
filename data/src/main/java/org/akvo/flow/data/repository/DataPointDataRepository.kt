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
package org.akvo.flow.data.repository

import io.reactivex.Completable
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.data.entity.ApiLocaleResult
import org.akvo.flow.data.entity.ApiSurveyInstance
import org.akvo.flow.data.entity.images.DataPointImageMapper
import org.akvo.flow.data.net.RestApi
import org.akvo.flow.data.net.s3.S3RestApi
import org.akvo.flow.data.util.MediaHelper
import org.akvo.flow.domain.exception.AssignmentRequiredException
import org.akvo.flow.domain.repository.DataPointRepository
import retrofit2.HttpException
import timber.log.Timber
import java.net.HttpURLConnection
import javax.inject.Inject

class DataPointDataRepository @Inject constructor(
    private val dataSourceFactory: DataSourceFactory,
    private val restApi: RestApi,
    private val s3RestApi: S3RestApi,
    private val mapper: DataPointImageMapper,
    private val mediaHelper: MediaHelper
    ) : DataPointRepository {

    override suspend fun downloadDataPoints(surveyId: Long): Int {
        var syncedDataPoints = 0
        var backendDataPointsCursor = dataSourceFactory.dataBaseDataSource.getDataPointCursor(surveyId)
        var moreToLoad = true
        try {
            while (moreToLoad) {
                val apiLocaleResult: ApiLocaleResult = restApi.downloadDataPoints(surveyId, backendDataPointsCursor)
                syncedDataPoints += syncDataPoints(apiLocaleResult)
                if (apiLocaleResult.dataPoints.isNotEmpty()) {
                    backendDataPointsCursor = apiLocaleResult.cursor
                }
                moreToLoad = apiLocaleResult.dataPoints.isNotEmpty()
                        && backendDataPointsCursor != null // cursor is null with old datapoint api
            }
            dataSourceFactory.dataBaseDataSource.saveDataPointCursor(surveyId, backendDataPointsCursor)
            return syncedDataPoints
        } catch (e: HttpException) {
            if ((e.code() == HttpURLConnection.HTTP_NOT_FOUND)) {
                throw AssignmentRequiredException("Dashboard Assignment missing")
            } else {
                throw e
            }
        }
    }

    override fun cleanPathAndDownLoadMedia(filename: String): Completable {
        return downLoadMedia(mediaHelper.cleanMediaFileName(filename))
    }

    override fun markDataPointAsViewed(dataPointId: String): Completable {
        return dataSourceFactory.dataBaseDataSource.markDataPointAsViewed(dataPointId)
    }

    private suspend fun syncDataPoints(apiLocaleResult: ApiLocaleResult): Int {
        val dataPoints = apiLocaleResult.dataPoints
        //downLoadImages(apiLocaleResult.dataPoints) FIXME: do at the end
        return dataSourceFactory.dataBaseDataSource.syncDataPoints(dataPoints)
    }

    private suspend fun downLoadImages(formInstances: List<ApiSurveyInstance>) {
        mapper.getImagesList(formInstances)
            .filter { image -> !dataSourceFactory.fileDataSource.fileExists(image) }
            .map { image ->
                downloadImage(image)
            }
    }

    private suspend fun downloadImage(image: String) {
        try {
            val responseBody = s3RestApi.downloadImage(image)
            dataSourceFactory.fileDataSource.saveRemoteMediaFile(image, responseBody)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun downLoadMedia(filename: String): Completable {
        return s3RestApi.downloadMedia(filename).flatMapCompletable { responseBody ->
            dataSourceFactory.fileDataSource.saveRemoteMediaFile(filename, responseBody)
        }
    }
}
