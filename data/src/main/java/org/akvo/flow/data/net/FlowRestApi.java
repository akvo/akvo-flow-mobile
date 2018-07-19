/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.net;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

import org.akvo.flow.data.entity.ApiFilesResult;
import org.akvo.flow.data.entity.ApiLocaleResult;
import org.akvo.flow.data.entity.S3File;
import org.akvo.flow.data.entity.Transmission;
import org.akvo.flow.data.net.gae.DataPointDownloadService;
import org.akvo.flow.data.net.gae.DeviceFilesService;
import org.akvo.flow.data.net.gae.ProcessingNotificationService;
import org.akvo.flow.data.net.s3.AwsS3;
import org.akvo.flow.data.util.ApiUrls;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Singleton;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

@Singleton
public class FlowRestApi {
    private static final String PAYLOAD_PUT_PUBLIC = "PUT\n%s\n%s\n%s\nx-amz-acl:public-read\n/%s/%s";// md5, type, date, bucket, obj
    private static final String PAYLOAD_PUT_PRIVATE = "PUT\n%s\n%s\n%s\n/%s/%s";// md5, type, date, bucket, obj

    private final String androidId;
    private final String imei;
    private final String phoneNumber;
    private final RestServiceFactory serviceFactory;
    private final Encoder encoder;
    private final String version;
    private final ApiUrls apiUrls;
    private final SignatureHelper signatureHelper;
    private final S3User s3User;
    private final DateFormat dateFormat;

    public FlowRestApi(DeviceHelper deviceHelper, RestServiceFactory serviceFactory,
            Encoder encoder, String version, ApiUrls apiUrls, SignatureHelper signatureHelper,
            S3User s3User, DateFormat dateFormat) {
        this.androidId = deviceHelper.getAndroidId();
        this.imei = deviceHelper.getImei();
        this.phoneNumber = deviceHelper.getPhoneNumber();
        this.serviceFactory = serviceFactory;
        this.encoder = encoder;
        this.version = version;
        this.apiUrls = apiUrls;
        this.signatureHelper = signatureHelper;
        this.s3User = s3User;
        this.dateFormat = dateFormat;
    }

    public Flowable<ApiLocaleResult> downloadDataPoints(long surveyGroup,
            @NonNull String timestamp) {
        String lastUpdated = !TextUtils.isEmpty(timestamp) ? timestamp : "0";
        String phoneNumber = encoder.encodeParam(this.phoneNumber);
        return serviceFactory.createRetrofitServiceWithInterceptor(DataPointDownloadService.class,
                apiUrls.getGaeUrl())
                .loadNewDataPoints(androidId, imei, lastUpdated, phoneNumber, surveyGroup + "");
    }

    public Observable<ApiFilesResult> getPendingFiles(List<String> formIds, String deviceId) {
        return serviceFactory.createRetrofitService(DeviceFilesService.class, apiUrls.getGaeUrl())
                .getFilesLists(phoneNumber, androidId, imei, version, deviceId, formIds);
    }

    public Observable<?> notifyFileAvailable(String action, String formId,
            String filename, String deviceId) {
        return serviceFactory
                .createRetrofitService(ProcessingNotificationService.class, apiUrls.getGaeUrl())
                .notifyFileAvailable(action, formId, filename, phoneNumber, androidId, imei,
                        version, deviceId);
    }

    public Observable<ResponseBody> uploadFile(Transmission transmission) {
        S3File s3File = transmission.getS3File();
        final String date = getDate();

        if (s3File.isPublic()) {
            return uploadPublicFile(date, s3File);
        } else {
            return uploadPrivateFile(date, s3File);
        }
    }

    private Observable<ResponseBody> uploadPublicFile(String date, S3File s3File) {
        String authorization = getAmazonAuth(date, PAYLOAD_PUT_PUBLIC, s3File);
        return createRetrofitService()
                .uploadPublic(s3File.getDir(), s3File.getFilename(), s3File.getMd5Base64(),
                        s3File.getContentType(), date, authorization, createBody(s3File));
    }

    private AwsS3 createRetrofitService() {
        return serviceFactory.createRetrofitService(AwsS3.class, apiUrls.getS3Url());
    }

    private Observable<ResponseBody> uploadPrivateFile(String date, S3File s3File) {
        String authorization = getAmazonAuth(date, PAYLOAD_PUT_PRIVATE, s3File);
        return createRetrofitService()
                .upload(s3File.getDir(), s3File.getFilename(), s3File.getMd5Base64(),
                        s3File.getContentType(), date, authorization, createBody(s3File));
    }

    @NonNull
    private RequestBody createBody(S3File s3File) {
        return RequestBody.create(MediaType.parse(s3File.getContentType()), s3File.getFile());
    }

    @NonNull
    private String getAmazonAuth(String date, String payloadStr, S3File s3File) {
        final String payload = String
                .format(payloadStr, s3File.getMd5Base64(), s3File.getContentType(), date,
                        s3User.getBucket(), s3File.getObjectKey());
        final String signature = signatureHelper
                .getAuthorization(payload, s3User.getSecret(), Base64.NO_WRAP);
        return "AWS " + s3User.getAccessKey() + ":" + signature;
    }

    private String getDate() {
        return dateFormat.format(new Date()) + "GMT";
    }
}
