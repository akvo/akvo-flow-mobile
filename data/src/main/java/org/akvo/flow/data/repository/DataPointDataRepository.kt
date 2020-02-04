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
import io.reactivex.Observable
import io.reactivex.Single
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.data.entity.ApiDataPoint
import org.akvo.flow.data.entity.ApiLocaleResult
import org.akvo.flow.data.entity.images.DataPointImageMapper
import org.akvo.flow.data.net.RestApi
import org.akvo.flow.data.net.s3.S3RestApi
import org.akvo.flow.domain.exception.AssignmentRequiredException
import org.akvo.flow.domain.repository.DataPointRepository
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject

class DataPointDataRepository @Inject constructor(
    private val dataSourceFactory: DataSourceFactory,
    private val restApi: RestApi,
    private val s3RestApi: S3RestApi,
    private val mapper: DataPointImageMapper
) : DataPointRepository {

    override fun downloadDataPoints(surveyGroupId: Long): Single<Int> {
        return syncDataPoints(surveyGroupId)
            .onErrorResumeNext(fun(throwable: Throwable): Single<Int> {
                return if (isErrorForbidden(throwable)) {
                    Single.error(AssignmentRequiredException("Dashboard Assignment missing"))
                } else {
                    Single.error(throwable)
                }
            })
    }

    private fun isErrorForbidden(throwable: Throwable): Boolean {
        return (throwable is HttpException
            && throwable.code() == HttpURLConnection.HTTP_FORBIDDEN)
    }

    private fun syncDataPoints(surveyGroupId: Long): Single<Int> {
        return restApi.downloadDataPoints(surveyGroupId)
            .flatMap { apiLocaleResult -> syncDataPoints(apiLocaleResult, surveyGroupId) }
    }

    private fun syncDataPoints(apiLocaleResult: ApiLocaleResult, surveyGroupId: Long): Single<Int> {
        return dataSourceFactory.dataBaseDataSource
            .syncDataPoints(apiLocaleResult.dataPoints, surveyGroupId)
            .andThen(downLoadImages(apiLocaleResult.dataPoints))
            .andThen(Single.just(apiLocaleResult.dataPoints.size))
    }

    private fun downLoadImages(dataPoints: List<ApiDataPoint>): Completable {
        val images: List<String> = mapper.getImagesList(dataPoints)
        return Observable.fromIterable(images)
            .flatMapCompletable { image -> downLoadImage(image) }
    }

    private fun downLoadImage(filename: String): Completable {
        return s3RestApi.downloadImage(filename).flatMapCompletable { responseBody ->
            dataSourceFactory.fileDataSource.saveRemoteImageFile(filename, responseBody)
        }
    }
}
