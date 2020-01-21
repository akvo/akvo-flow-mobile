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

import android.text.TextUtils
import io.reactivex.Observable
import okhttp3.ResponseBody
import org.akvo.flow.data.entity.S3File
import org.akvo.flow.data.entity.Transmission
import org.akvo.flow.data.net.RestServiceFactory
import org.akvo.flow.data.util.ApiUrls
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.text.DateFormat
import java.util.Date
import javax.inject.Singleton

@Singleton
open class S3RestApi(
    private val serviceFactory: RestServiceFactory, private val apiUrls: ApiUrls,
    private val amazonAuthHelper: AmazonAuthHelper, private val dateFormat: DateFormat,
    private val bodyCreator: BodyCreator
) {

    fun uploadFile(transmission: Transmission): Observable<Response<ResponseBody>> {
        val s3File = transmission.s3File
        val date = formattedDate()
        return when {
            s3File.isPublic -> {
                uploadPublicFile(date, s3File)
            }
            else -> {
                uploadPrivateFile(date, s3File)
            }
        }
    }

    fun downloadArchive(fileName: String): Observable<ResponseBody> {
        val date = formattedDate()
        val authorization = amazonAuthHelper
            .getAmazonAuthForGet(date, PAYLOAD_GET, "$SURVEYS_FOLDER/$fileName")
        return createRetrofitService().getSurvey(SURVEYS_FOLDER, fileName, date, authorization)
            .onErrorResumeNext(fun(throwable: Throwable): Observable<ResponseBody> {
                Timber.e(Exception(throwable), "Error downloading $fileName from s3")
                return Observable.error(throwable)
            })
    }

    fun downloadImage(fileName: String): Observable<ResponseBody> {
        val date = formattedDate()
        val authorization = amazonAuthHelper
            .getAmazonAuthForGet(date, PAYLOAD_GET, "$IMAGES_FOLDER/$fileName")
        return createRetrofitService().downloadImage(IMAGES_FOLDER, fileName, date, authorization)
            .onErrorResumeNext(fun(throwable: Throwable): Observable<ResponseBody> {
                Timber.e(Exception(throwable), "Error downloading $fileName from s3")
                return Observable.error(throwable)
            })
    }

    private fun uploadPublicFile(date: String, s3File: S3File): Observable<Response<ResponseBody>> {
        val authorization = amazonAuthHelper.getAmazonAuthForPut(date, PAYLOAD_PUT_PUBLIC, s3File)
        return createRetrofitService()
            .uploadPublic(
                s3File.dir,
                s3File.filename,
                s3File.md5Base64,
                s3File.contentType,
                date,
                authorization,
                bodyCreator.createBody(s3File)
            )
            .concatMap { response -> verifyResponse(response, s3File) }
    }

    private fun createRetrofitService(): AwsS3 {
        return serviceFactory.createRetrofitService(AwsS3::class.java, apiUrls.s3Url)
    }

    private fun uploadPrivateFile(
        date: String,
        s3File: S3File
    ): Observable<Response<ResponseBody>> {
        val authorization = amazonAuthHelper
            .getAmazonAuthForPut(date, PAYLOAD_PUT_PRIVATE, s3File)
        val body = bodyCreator.createBody(s3File)
        return createRetrofitService()
            .upload(
                s3File.dir,
                s3File.filename,
                s3File.md5Base64,
                s3File.contentType,
                date,
                authorization,
                body
            )
            .concatMap { response -> verifyResponse(response, s3File) }
    }

    private fun verifyResponse(
        response: Response<ResponseBody>,
        s3File: S3File
    ): Observable<Response<ResponseBody>> {
        when {
            response.isSuccessful -> {
                val etag = getEtag(response)
                if (TextUtils.isEmpty(etag) || etag != s3File.md5Hex) {
                    return Observable.error(Exception("File upload to S3 Failed" + s3File.filename))
                }
            }
            else -> {
                return Observable.error(HttpException(response))
            }
        }
        return Observable.just(response)
    }

    private fun getEtag(response: Response<ResponseBody>): String? {
        var eTag = response.headers()["ETag"]
        if (!TextUtils.isEmpty(eTag)) {
            eTag = eTag!!.replace("\"".toRegex(), "")
        }
        return eTag
    }

    open fun formattedDate(): String {
        return dateFormat.format(Date()) + "GMT"
    }

    companion object {
        private const val PAYLOAD_PUT_PUBLIC =
            "PUT\n%s\n%s\n%s\nx-amz-acl:public-read\n/%s/%s" // md5, type, date, bucket, obj
        private const val PAYLOAD_PUT_PRIVATE =
            "PUT\n%s\n%s\n%s\n/%s/%s" // md5, type, date, bucket, obj
        private const val PAYLOAD_GET = "GET\n\n\n%s\n/%s/%s" // date, bucket, obj
        private const val SURVEYS_FOLDER = "surveys"
        private const val IMAGES_FOLDER = "images"
    }
}
