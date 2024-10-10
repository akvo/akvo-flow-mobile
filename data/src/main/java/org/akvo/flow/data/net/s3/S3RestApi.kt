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
package org.akvo.flow.data.net.s3

import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import org.akvo.flow.data.entity.S3File
import org.akvo.flow.data.entity.Transmission
import org.akvo.flow.data.net.RestServiceFactory
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import javax.inject.Singleton

@Singleton
open class S3RestApi(
    private val serviceFactory: RestServiceFactory,
    private val baseUrl: String,
    private val instanceId: String
) {

    fun uploadFile(transmission: Transmission): Observable<Response<ResponseBody>> {
        val s3File = transmission.s3File
        val formId = transmission.formId
        val file = MultipartBody.Part.createFormData("file", s3File.filename, s3File.file.asRequestBody(s3File.contentType.toMediaType()))
        return createRetrofitService().upload(instanceId, s3File.dir, formId, s3File.filename, file)
            .concatMap { response -> verifyResponse(response) }
    }

    fun downloadArchive(fileName: String): Observable<ResponseBody> {
        return createRetrofitService().getSurvey(instanceId, SURVEYS_FOLDER, fileName)
            .onErrorResumeNext(fun(throwable: Throwable): Observable<ResponseBody> {
                Timber.e(Exception(throwable), "Error downloading $fileName from s3")
                return Observable.error(throwable)
            })
    }

    fun downloadMedia(fileName: String): Single<ResponseBody> {
        return createRetrofitService().downloadMedia(instanceId, IMAGES_FOLDER, fileName)
            .onErrorResumeNext(fun(throwable: Throwable): Single<ResponseBody> {
                Timber.e(Exception(throwable), "Error downloading $fileName from s3")
                return Single.error(throwable)
            })
    }

    suspend fun downloadImage(fileName: String): ResponseBody {
        return createRetrofitService().downloadImage(instanceId, IMAGES_FOLDER, fileName)
    }

    private fun createRetrofitService(): S3Proxy {
        return serviceFactory.createRetrofitService(S3Proxy::class.java, baseUrl)
    }

    private fun verifyResponse(
        response: Response<ResponseBody>
    ): Observable<Response<ResponseBody>> {
        when {
            response.isSuccessful -> {
            }
            else -> {
                return Observable.error(HttpException(response))
            }
        }
        return Observable.just(response)
    }

    companion object {
        private const val SURVEYS_FOLDER = "surveys"
        private const val IMAGES_FOLDER = "images"
    }
}
