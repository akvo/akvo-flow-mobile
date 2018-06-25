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
import org.akvo.flow.data.util.Constants;

import java.io.File;
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
public class RestApi {
    private static final String PAYLOAD_PUT_PUBLIC = "PUT\n%s\n%s\n%s\nx-amz-acl:public-read\n/%s/%s";// md5, type, date, bucket, obj
    private static final String PAYLOAD_PUT_PRIVATE = "PUT\n%s\n%s\n%s\n/%s/%s";// md5, type, date, bucket, obj

    private final String androidId;
    private final String imei;
    private final String phoneNumber;
    private final FlowServiceFactory serviceFactory;
    private final Encoder encoder;
    private final String version;
    private final ApiUrls apiUrls;
    private final SignatureHelper signatureHelper;
    private final S3User s3User;
    private final DateFormat dateFormat;

    public RestApi(DeviceHelper deviceHelper, FlowServiceFactory serviceFactory,
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

    public Observable<Boolean> notifyFileAvailable(String action, String formId, String filename,
            String deviceId) {
        serviceFactory
                .createRetrofitService(ProcessingNotificationService.class, apiUrls.getGaeUrl())
                .notifyFileAvailable(action, formId, filename, phoneNumber, androidId, imei,
                        version, deviceId);
        return Observable.just(true); //TODO: verify result
    }

    //TODO: cleanup this method a bit
    public Observable<ResponseBody> uploadFile(Transmission transmission) {

        S3File s3File = transmission.getS3File();
        File file = s3File.getFile();
        final String md5Base64 = getMd5Base64(s3File);

        final String date = getDate();
        String filename = file.getName();
        String contentType = contentType(filename);
        boolean isPublic = s3File.isPublic();
        final String payloadStr = isPublic ? PAYLOAD_PUT_PUBLIC : PAYLOAD_PUT_PRIVATE;
        String objectKey = s3File.getDir() + filename;
        final String payload = String
                .format(payloadStr, md5Base64, contentType, date, s3User.getBucket(), objectKey);
        final String signature = signatureHelper
                .getAuthorization(payload, s3User.getSecret(), Base64.NO_WRAP);
        String authorization = "AWS " + s3User.getAccessKey() + ":" + signature;

        RequestBody body = RequestBody.create(MediaType.parse(contentType), file);
        AwsS3 retrofitService = serviceFactory
                .createRetrofitService(AwsS3.class, apiUrls.getS3Url());
        //TODO: remove hardcoded values
        if (isPublic) {
            return retrofitService
                    .uploadPublic("images", filename, md5Base64, contentType, date, authorization, body);
        } else {
            return retrofitService.upload("devicezip", filename, md5Base64, contentType, date, authorization, body);
        }
       // return Observable.just(transmission); //TODO: check result ++ retry
    }

    private String getMd5Base64(S3File file) {
        final byte[] rawMd5 = file.getRawMd5();
        return Base64.encodeToString(rawMd5, Base64.NO_WRAP);
    }

    private String contentType(String filename) {
        String ext = filename.substring(filename.lastIndexOf("."));
        switch (ext) {
            case Constants.PNG_SUFFIX:
                return Constants.PNG_CONTENT_TYPE;
            case Constants.JPG_SUFFIX:
                return Constants.JPEG_CONTENT_TYPE;
            case Constants.VIDEO_SUFFIX:
                return Constants.VIDEO_CONTENT_TYPE;
            case Constants.ARCHIVE_SUFFIX:
                return Constants.DATA_CONTENT_TYPE;
            default:
                return null;
        }
    }

    private String getDate() {
        return dateFormat.format(new Date()) + "GMT";
    }
}
