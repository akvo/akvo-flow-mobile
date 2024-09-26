/*
 * Copyright (C) 2024 Stichting Akvo (Akvo Foundation)
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
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface S3Proxy {
    @Multipart
    @PUT("/{instance}/{folder}/{formId}/{filename}")
    fun upload(
        @Path("instance") instance: String?,
        @Path("folder") folder: String?,
        @Path("formId") formId: String?,
        @Path("filename") filename: String?,
        @Part file: MultipartBody.Part?
    ): Observable<Response<ResponseBody>>

    @GET("/{instance}/{folder}/{filename}")
    fun getSurvey(
        @Path("instance") instance: String?,
        @Path("folder") folder: String?,
        @Path("filename") filename: String?
    ): Observable<ResponseBody>

    @GET("/{instance}/{folder}/{filename}")
    fun downloadMedia(
        @Path("instance") instance: String?,
        @Path("folder") folder: String?,
        @Path("filename") filename: String?
    ): Single<ResponseBody>

    @GET("/{instance}/{folder}/{filename}")
    suspend fun downloadImage(
        @Path("instance") instance: String?,
        @Path("folder") folder: String?,
        @Path("filename") filename: String?
    ): ResponseBody
}