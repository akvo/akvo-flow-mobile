/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.net.s3;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import static org.akvo.flow.data.util.ApiUrls.S3_FILE_PATH;

public interface AwsS3 {

    @PUT(S3_FILE_PATH)
    Observable<Response<ResponseBody>> upload(@Path("key") String key,
                @Path("file") String file,
                @Header("Content-MD5") String md5Base64,
                @Header("Content-type") String contentType,
                @Header("Date") String date,
                @Header("Authorization") String authorization,
                @Body RequestBody body);

    @Headers({"x-amz-acl: public-read"})
    @PUT(S3_FILE_PATH)
    Observable<Response<ResponseBody>> uploadPublic(@Path("key") String key,
            @Path("file") String file,
            @Header("Content-MD5") String md5Base64,
            @Header("Content-type") String contentType,
            @Header("Date") String date,
            @Header("Authorization") String authorization,
            @Body RequestBody body);

    @GET(S3_FILE_PATH)
    Observable<ResponseBody> getSurvey(@Path("key") String key,
            @Path("file") String file,
            @Header("Date") String date,
            @Header("Authorization") String authorization);

    @GET(S3_FILE_PATH)
    Single<ResponseBody> downloadImage(@Path("key") String key,
            @Path("file") String file,
            @Header("Date") String date,
            @Header("Authorization") String authorization);
}
