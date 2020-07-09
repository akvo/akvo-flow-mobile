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
import org.akvo.flow.data.entity.ApiDataPoint
import org.akvo.flow.data.entity.ApiLocaleResult
import org.akvo.flow.data.entity.images.DataPointImageMapper
import org.akvo.flow.data.net.RestApi
import org.akvo.flow.data.net.s3.S3RestApi
import org.akvo.flow.data.util.MediaHelper
import org.akvo.flow.domain.exception.AssignmentRequiredException
import org.akvo.flow.domain.repository.DataPointRepository
import retrofit2.HttpException
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
        var responseCode = 200
        var syncedDataPoints = 0
        var cursor = dataSourceFactory.dataBaseDataSource.getDataPointCursor(surveyId)
        try {
            while (responseCode == 200) {
                val apiLocaleResult = restApi.downloadDataPoints(surveyId, cursor)
                responseCode = apiLocaleResult.code
                syncedDataPoints += syncDataPoints(apiLocaleResult)
                cursor = apiLocaleResult.cursor
            }
            return syncedDataPoints
        } catch (e: HttpException) {
            if ((e.code() == HttpURLConnection.HTTP_FORBIDDEN)) {
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
        val syncDataPoints =
            dataSourceFactory.dataBaseDataSource.syncDataPoints(apiLocaleResult.dataPoints)
        downLoadImages(apiLocaleResult.dataPoints)
        return syncDataPoints
    }


    private suspend fun downLoadImages(dataPoints: List<ApiDataPoint>) {
        mapper.getImagesList(dataPoints)
            .filter { image -> !dataSourceFactory.fileDataSource.fileExists(image) }
            .map { image ->
                val responseBody = s3RestApi.downloadImage(image)
                dataSourceFactory.fileDataSource.saveRemoteMediaFile(image, responseBody)
                Unit
            }
    }

    private fun downLoadMedia(filename: String): Completable {
        return s3RestApi.downloadMedia(filename).flatMapCompletable { responseBody ->
            dataSourceFactory.fileDataSource.saveRemoteMediaFile(filename, responseBody)
        }
    }
}
