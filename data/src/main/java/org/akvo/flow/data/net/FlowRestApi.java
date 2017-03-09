/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import org.akvo.flow.data.entity.ApiLocaleResult;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import timber.log.Timber;

@Singleton
public class FlowRestApi {

    private static final String HMAC_SHA_1_ALGORITHM = "HmacSHA1";
    private static final String CHARSET_UTF8 = "UTF-8";

    private final String androidId;
    private final String imei;
    private final String phoneNumber;
    private final RestServiceFactory serviceFactory;

    @Inject
    public FlowRestApi(DeviceHelper deviceHelper, RestServiceFactory serviceFactory) {
        this.androidId = deviceHelper.getAndroidID();
        this.imei = deviceHelper.getImei();
        this.phoneNumber = deviceHelper.getPhoneNumber();
        this.serviceFactory = serviceFactory;
    }

    public Observable<ApiLocaleResult> loadNewDataPoints(@NonNull String baseUrl,
            @NonNull String apiKey, long surveyGroup, @NonNull String timestamp) {
        return serviceFactory.createRetrofitService(baseUrl, FlowApiService.class)
                .loadNewDataPoints(buildSyncUrl(baseUrl, apiKey, surveyGroup, timestamp));
    }

    @NonNull
    private String buildSyncUrl(@NonNull String serverBaseUrl, String apiKey, long surveyGroup,
            @NonNull String timestamp) {
        // Note: To compute the HMAC auth token, query params must be alphabetically ordered
        StringBuilder queryStringBuilder = new StringBuilder();
        appendParam(queryStringBuilder, Param.ANDROID_ID, encodeParam(androidId));
        appendParam(queryStringBuilder, Param.IMEI, encodeParam(imei));
        appendParam(queryStringBuilder, Param.LAST_UPDATED, (!TextUtils.isEmpty(timestamp) ?
                timestamp : "0"));
        appendParam(queryStringBuilder, Param.PHONE_NUMBER, encodeParam(phoneNumber));
        appendParam(queryStringBuilder, Param.SURVEY_GROUP, surveyGroup + "");
        queryStringBuilder.append(Param.TIMESTAMP).append(Param.EQUALS).append(getTimestamp());
        final String query = queryStringBuilder.toString();
        return serverBaseUrl + "/" + Path.SURVEYED_LOCALE + "?" + query +
                Param.SEPARATOR + Param.HMAC + Param.EQUALS + getAuthorization(query, apiKey);
    }

    private void appendParam(@NonNull StringBuilder queryStringBuilder, @NonNull String paramName,
            @NonNull String paramValue) {
        queryStringBuilder.append(paramName).append(Param.EQUALS).append(paramValue).append(Param
                .SEPARATOR);
    }

    private String encodeParam(@Nullable String param) {
        if (TextUtils.isEmpty(param)) {
            return "";
        }
        try {
            return URLEncoder.encode(param, CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            Timber.e(e.getMessage());
            return "";
        }
    }

    private String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        try {
            return URLEncoder.encode(dateFormat.format(new Date()), CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            Timber.e(e.getMessage());
            return null;
        }
    }

    @Nullable
    private String getAuthorization(@NonNull String query, String apiKey) {
        String authorization = null;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(apiKey.getBytes(), HMAC_SHA_1_ALGORITHM);

            Mac mac = Mac.getInstance(HMAC_SHA_1_ALGORITHM);
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(query.getBytes());

            authorization = Base64.encodeToString(rawHmac, Base64.DEFAULT);
        } catch (@NonNull NoSuchAlgorithmException | InvalidKeyException e) {
            Timber.e(e.getMessage());
        }

        return authorization;
    }

    interface Path {

        String SURVEYED_LOCALE = "surveyedlocale";
    }

    interface Param {

        String SURVEY_GROUP = "surveyGroupId";
        String PHONE_NUMBER = "phoneNumber";
        String IMEI = "imei";
        String TIMESTAMP = "ts";
        String LAST_UPDATED = "lastUpdateTime";
        String HMAC = "h";
        String VERSION = "ver";
        String ANDROID_ID = "androidId";
        String SEPARATOR = "&";
        String EQUALS = "=";
    }
}
