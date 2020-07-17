/*
 * Copyright (C) 2018-2020 Stichting Akvo (Akvo Foundation)
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
 *
 */
package org.akvo.flow.data.net.s3

import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.akvo.flow.data.util.ApiUrls
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Path

interface AwsS3 {
    @PUT(ApiUrls.S3_FILE_PATH)
    fun upload(
        @Path("key") key: String?,
        @Path("file") file: String?,
        @Header("Content-MD5") md5Base64: String?,
        @Header("Content-type") contentType: String?,
        @Header("Date") date: String?,
        @Header("Authorization") authorization: String?,
        @Body body: RequestBody?
    ): Observable<Response<ResponseBody>>

    @Headers("x-amz-acl: public-read")
    @PUT(ApiUrls.S3_FILE_PATH)
    fun uploadPublic(
        @Path("key") key: String?,
        @Path("file") file: String?,
        @Header("Content-MD5") md5Base64: String?,
        @Header("Content-type") contentType: String?,
        @Header("Date") date: String?,
        @Header("Authorization") authorization: String?,
        @Body body: RequestBody?
    ): Observable<Response<ResponseBody>>

    @GET(ApiUrls.S3_FILE_PATH)
    fun getSurvey(
        @Path("key") key: String?,
        @Path("file") file: String?,
        @Header("Date") date: String?,
        @Header("Authorization") authorization: String?
    ): Observable<ResponseBody>

    @GET(ApiUrls.S3_FILE_PATH)
    fun downloadImage(
        @Path("key") key: String?,
        @Path("file") file: String?,
        @Header("Date") date: String?,
        @Header("Authorization") authorization: String?
    ): Single<ResponseBody>

    @GET(ApiUrls.S3_FILE_PATH)
    suspend fun downloadImageNew(
        @Path("key") key: String?,
        @Path("file") file: String?,
        @Header("Date") date: String?,
        @Header("Authorization") authorization: String?
    ): ResponseBody
}
