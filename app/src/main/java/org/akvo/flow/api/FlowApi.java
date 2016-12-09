/*
 *  Copyright (C) 2013-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.api;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.response.SurveyedLocalesResponse;
import org.akvo.flow.exception.HttpException;
import org.akvo.flow.exception.HttpException.Status;
import org.akvo.flow.serialization.form.SurveyMetaParser;
import org.akvo.flow.serialization.response.SurveyedLocaleParser;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.PropertyUtil;
import org.akvo.flow.util.StatusUtil;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class FlowApi {

    private static final String TAG = FlowApi.class.getSimpleName();

    //These values never change
    private static final String API_KEY;
    private static final String PHONE_NUMBER;
    private static final String IMEI;
    private static final String ANDROID_ID;

    private static final int ERROR_UNKNOWN = -1;
    private static final String HMAC_SHA_1_ALGORITHM = "HmacSHA1";
    private static final String CHARSET_UTF8 = "UTF-8";

    private static final String HTTPS_PREFIX = "https";
    private static final String HTTP_PREFIX = "http";

    static {
        Context context = FlowApp.getApp();
        API_KEY = getApiKey(context);
        PHONE_NUMBER = StatusUtil.getPhoneNumber(context);
        IMEI = StatusUtil.getImei(context);
        ANDROID_ID = PlatformUtil.getAndroidID(context);
    }

    public String getServerTime(@NonNull String serverBase) throws IOException {
        if (serverBase.startsWith(HTTPS_PREFIX)) {
            serverBase = HTTP_PREFIX + serverBase.substring(HTTPS_PREFIX.length());
        }
        final String url = buildServerTimeUrl(serverBase);
        String response = HttpUtil.httpGet(url);
        String time = "";
        if (!TextUtils.isEmpty(response)) {
            JSONObject json;
            try {
                json = new JSONObject(response);
                time = json.getString("time");
            } catch (JSONException e1) {
                Log.e(TAG, "Error fetching time: ", e1);
            }
        }
        return time;
    }

    @NonNull
    private String buildServerTimeUrl(@NonNull String serverBase) {
        Uri.Builder builder = Uri.parse(serverBase).buildUpon();
        builder.appendPath(Path.TIME_CHECK);
        builder.appendQueryParameter(Param.TIMESTAMP, System.currentTimeMillis() + "");
        return builder.build().toString();
    }

    /**
     * Sends the location beacon to the server
     * The response is ignored
     */
    public void sendLocation(@NonNull String serverBase, @Nullable Double latitude,
            @Nullable Double longitude,
            @Nullable Float accuracy) {
        try {
            String url = buildLocationUrl(serverBase, latitude, longitude, accuracy);
            HttpUtil.httpGet(url);
        } catch (IOException e) {
            Log.e(TAG, "Could not send location beacon", e);
        }
    }

    @NonNull
    private String buildLocationUrl(@NonNull String serverBase, @Nullable Double latitude,
            @Nullable Double
                    longitude, @Nullable Float accuracy) {
        Uri.Builder builder = Uri.parse(serverBase).buildUpon();
        builder.appendPath(Path.BEACON_SERVICE);
        builder.appendQueryParameter(Param.PARAM_ACTION, Param.VALUE_BEACON);
        appendDeviceParams(builder);
        if (latitude != null && longitude != null && accuracy != null) {
            builder.appendQueryParameter(Param.LAT, latitude + "");
            builder.appendQueryParameter(Param.LON, longitude + "");
            builder.appendQueryParameter(Param.ACC, accuracy + "");
        }
        builder.appendQueryParameter(Param.OS_VERSION,
                "Android " + android.os.Build.VERSION.RELEASE);
        return builder.build().toString();
    }

    /**
     * Request the notifications GAE has ready for us, like the list of missing files.
     *
     * @return String body of the HTTP response
     * @throws Exception
     */
    @Nullable
    public JSONObject getDeviceNotification(@NonNull String serverBase, @NonNull String[] surveyIds)
            throws Exception {
        // Send the list of surveys we've got downloaded, getting notified of the deleted ones
        String url = buildDeviceNotificationUrl(serverBase, surveyIds);
        String response = HttpUtil.httpGet(url);
        if (!TextUtils.isEmpty(response)) {
            return new JSONObject(response);
        }
        return null;
    }

    @NonNull
    private String buildDeviceNotificationUrl(@NonNull String serverBase,
            @NonNull String[] surveyIds) {
        Uri.Builder builder = Uri.parse(serverBase).buildUpon();
        builder.appendPath(Path.DEVICE_NOTIFICATION);
        appendDeviceParams(builder);
        for (String id : surveyIds) {
            builder.appendQueryParameter(Param.FORM_ID, id);
        }
        return builder.build().toString();
    }

    @NonNull
    public List<Survey> getSurveyHeader(@NonNull String serverBaseUrl, @NonNull String surveyId)
            throws IOException {
        final String url = buildSurveyHeaderUrl(serverBaseUrl, surveyId);
        String response = HttpUtil.httpGet(url);
        if (response != null) {
            return new SurveyMetaParser().parseList(response, true);
        }
        return Collections.emptyList();
    }

    @NonNull
    private String buildSurveyHeaderUrl(@NonNull String serverBaseUrl, @NonNull String surveyId) {
        Uri.Builder builder = Uri.parse(serverBaseUrl).buildUpon();
        builder.appendPath(Path.SURVEY_HEADER_SERVICE);
        builder.appendQueryParameter(Param.PARAM_ACTION, Param.VALUE_HEADER);
        builder.appendQueryParameter(Param.SURVEY_ID, surveyId);
        appendDeviceParams(builder);
        return builder.build().toString();
    }

    public List<Survey> getSurveys(@NonNull String serverBase) throws IOException {
        List<Survey> surveys = new ArrayList<>();
        final String url = buildSurveysUrl(serverBase);
        String response = HttpUtil.httpGet(url);
        if (response != null) {
            surveys = new SurveyMetaParser().parseList(response);
        }
        return surveys;
    }

    @NonNull
    private String buildSurveysUrl(@NonNull String serverBaseUrl) {
        Uri.Builder builder = Uri.parse(serverBaseUrl).buildUpon();
        builder.appendPath(Path.SURVEY_LIST_SERVICE);
        builder.appendQueryParameter(Param.PARAM_ACTION, Param.VALUE_SURVEY);
        appendDeviceParams(builder);
        return builder.build().toString();
    }

    /**
     * Notify GAE back-end that data is available
     * Sends a message to the service with the file name that was just uploaded
     * so it can start processing the file
     */
    public int sendProcessingNotification(@NonNull String serverBaseUrl, @NonNull String formId,
            @NonNull String
                    action, @NonNull String fileName) {
        String url = buildProcessingNotificationUrl(serverBaseUrl, formId, action, fileName);
        try {
            HttpUtil.httpGet(url);
            return HttpStatus.SC_OK;
        } catch (HttpException e) {
            Log.e(TAG, e.getStatus() + " response for formId: " + formId);
            return e.getStatus();
        } catch (Exception e) {
            Log.e(TAG, "GAE sync notification failed for file: " + fileName);
            return ERROR_UNKNOWN;
        }
    }

    @NonNull
    private String buildProcessingNotificationUrl(@NonNull String serverBaseUrl,
            @NonNull String formId, @NonNull
            String action, @NonNull String fileName) {
        Uri.Builder builder = Uri.parse(serverBaseUrl).buildUpon();
        builder.appendPath(Path.NOTIFICATION);
        builder.appendQueryParameter(Param.PARAM_ACTION, action);
        builder.appendQueryParameter(Param.FORM_ID, formId);
        builder.appendQueryParameter(Param.FILENAME, fileName);
        appendDeviceParams(builder);
        return builder.build().toString();
    }

    @Nullable
    public List<SurveyedLocale> getSurveyedLocales(@NonNull String serverBaseUrl, long surveyGroup,
            @NonNull String timestamp)
            throws IOException {
        // Note: To compute the HMAC auth token, query params must be alphabetically ordered
        String url = buildSyncUrl(serverBaseUrl, surveyGroup, timestamp);
        String response = HttpUtil.httpGet(url);
        if (response != null) {
            SurveyedLocalesResponse slRes = new SurveyedLocaleParser().parseResponse(response);
            if (slRes.getError() != null) {
                throw new HttpException(slRes.getError(), Status.MALFORMED_RESPONSE);
            }
            return slRes.getSurveyedLocales();
        }

        return null;
    }

    @NonNull
    private String buildSyncUrl(@NonNull String serverBaseUrl, long surveyGroup,
            @NonNull String timestamp) {
        // Note: To compute the HMAC auth token, query params must be alphabetically ordered
        StringBuilder queryStringBuilder = new StringBuilder();
        appendParam(queryStringBuilder, Param.ANDROID_ID, encodeParam(ANDROID_ID));
        appendParam(queryStringBuilder, Param.IMEI, encodeParam(IMEI));
        appendParam(queryStringBuilder, Param.LAST_UPDATED, (!TextUtils.isEmpty(timestamp) ?
                timestamp : "0"));
        appendParam(queryStringBuilder, Param.PHONE_NUMBER, encodeParam(PHONE_NUMBER));
        appendParam(queryStringBuilder, Param.SURVEY_GROUP, surveyGroup + "");
        queryStringBuilder.append(Param.TIMESTAMP).append(Param.EQUALS).append(getTimestamp());
        final String query = queryStringBuilder.toString();
        return serverBaseUrl + "/" + Path.SURVEYED_LOCALE + "?" + query +
                Param.SEPARATOR + Param.HMAC + Param.EQUALS + getAuthorization(query);
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
            Log.e(TAG, e.getMessage());
            return "";
        }
    }

    private static String getApiKey(@NonNull Context context) {
        PropertyUtil props = new PropertyUtil(context.getResources());
        return props.getProperty(ConstantUtil.API_KEY);
    }

    @Nullable
    private String getAuthorization(@NonNull String query) {
        String authorization = null;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(API_KEY.getBytes(), HMAC_SHA_1_ALGORITHM);

            Mac mac = Mac.getInstance(HMAC_SHA_1_ALGORITHM);
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(query.getBytes());

            authorization = Base64.encodeToString(rawHmac, Base64.DEFAULT);
        } catch (@NonNull NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, e.getMessage());
        }

        return authorization;
    }

    private String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        try {
            return URLEncoder.encode(dateFormat.format(new Date()), CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    private void appendDeviceParams(@NonNull Uri.Builder builder) {
        Context context = FlowApp.getApp();
        builder.appendQueryParameter(Param.PHONE_NUMBER, PHONE_NUMBER);
        builder.appendQueryParameter(Param.ANDROID_ID, ANDROID_ID);
        builder.appendQueryParameter(Param.IMEI, IMEI);
        builder.appendQueryParameter(Param.VERSION, PlatformUtil.getVersionName(context));
        builder.appendQueryParameter(Param.DEVICE_ID, StatusUtil.getDeviceId(context));
    }

    interface Path {

        String SURVEYED_LOCALE = "surveyedlocale";
        String NOTIFICATION = "processor";
        String SURVEY_LIST_SERVICE = "surveymanager";
        String SURVEY_HEADER_SERVICE = "surveymanager";
        String DEVICE_NOTIFICATION = "devicenotification";
        String TIME_CHECK = "devicetimerest";
        String BEACON_SERVICE = "locationBeacon";
    }

    interface Param {

        String SURVEY_GROUP = "surveyGroupId";
        String PHONE_NUMBER = "phoneNumber";
        String IMEI = "imei";
        String TIMESTAMP = "ts";
        String LAST_UPDATED = "lastUpdateTime";
        String HMAC = "h";
        String VERSION = "ver";
        String DEVICE_ID = "devId";
        String ANDROID_ID = "androidId";
        String OS_VERSION = "osVersion";
        String LAT = "lat";
        String LON = "lon";
        String ACC = "acc";

        String PARAM_ACTION = "action";
        String FORM_ID = "formID";
        String SURVEY_ID = "surveyId";
        String FILENAME = "fileName";

        String VALUE_BEACON = "beacon";
        String VALUE_HEADER = "getSurveyHeader";
        String VALUE_SURVEY = "getAvailableSurveysDevice";

        String SEPARATOR = "&";
        String EQUALS = "=";
    }
}
