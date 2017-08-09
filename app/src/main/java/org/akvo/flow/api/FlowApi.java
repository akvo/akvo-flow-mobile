/*
 *  Copyright (C) 2013-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.api;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.exception.HttpException;
import org.akvo.flow.serialization.form.SurveyMetaParser;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.ServerManager;
import org.akvo.flow.util.StatusUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class FlowApi {

    private final String phoneNumber;
    private final String imei;
    private final String androidId;

    private final String deviceIdentifier;

    private static final int ERROR_UNKNOWN = -1;

    private static final String HTTPS_PREFIX = "https";
    private static final String HTTP_PREFIX = "http";
    private final String baseUrl;

    public FlowApi(Context context) {
        ServerManager serverManager = new ServerManager(context);
        this.baseUrl = serverManager.getServerBase();
        this.phoneNumber = StatusUtil.getPhoneNumber(context);
        this.imei = StatusUtil.getImei(context);
        this.androidId = PlatformUtil.getAndroidID(context);
        Prefs prefs = new Prefs(context);
        this.deviceIdentifier = prefs
                .getString(Prefs.KEY_DEVICE_IDENTIFIER, Prefs.DEFAULT_VALUE_DEVICE_IDENTIFIER);
    }

    public String getServerTime() throws IOException {
        String serverBase = baseUrl;
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
                Timber.e(e1, "Error fetching time");
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
     * Request the notifications GAE has ready for us, like the list of missing files.
     *
     * @return String body of the HTTP response
     * @throws Exception
     */
    @Nullable
    public JSONObject getDeviceNotification(@NonNull String[] surveyIds)
            throws Exception {
        // Send the list of surveys we've got downloaded, getting notified of the deleted ones
        String url = buildDeviceNotificationUrl(baseUrl, surveyIds);
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
    public List<Survey> getSurveyHeader(@NonNull String surveyId)
            throws IOException {
        final String url = buildSurveyHeaderUrl(baseUrl, surveyId);
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

    public List<Survey> getSurveys() throws IOException {
        List<Survey> surveys = new ArrayList<>();
        final String url = buildSurveysUrl(baseUrl);
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
    public int sendProcessingNotification(@NonNull String formId, @NonNull String action,
            @NonNull String fileName) {
        String url = buildProcessingNotificationUrl(baseUrl, formId, action, fileName);
        try {
            HttpUtil.httpGet(url);
            return HttpURLConnection.HTTP_OK;
        } catch (HttpException e) {
            Timber.e(e.getStatus() + " response for formId: " + formId);
            return e.getStatus();
        } catch (Exception e) {
            Timber.e("GAE sync notification failed for file: " + fileName);
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

    private void appendDeviceParams(@NonNull Uri.Builder builder) {
        builder.appendQueryParameter(Param.PHONE_NUMBER, phoneNumber);
        builder.appendQueryParameter(Param.ANDROID_ID, androidId);
        builder.appendQueryParameter(Param.IMEI, imei);
        builder.appendQueryParameter(Param.VERSION, BuildConfig.VERSION_NAME);
        builder.appendQueryParameter(Param.DEVICE_ID, deviceIdentifier);
    }

    interface Path {

        String NOTIFICATION = "processor";
        String SURVEY_LIST_SERVICE = "surveymanager";
        String SURVEY_HEADER_SERVICE = "surveymanager";
        String DEVICE_NOTIFICATION = "devicenotification";
        String TIME_CHECK = "devicetimerest";
    }

    interface Param {

        String PHONE_NUMBER = "phoneNumber";
        String IMEI = "imei";
        String TIMESTAMP = "ts";
        String VERSION = "ver";
        String DEVICE_ID = "devId";
        String ANDROID_ID = "androidId";

        String PARAM_ACTION = "action";
        String FORM_ID = "formID";
        String SURVEY_ID = "surveyId";
        String FILENAME = "fileName";

        String VALUE_HEADER = "getSurveyHeader";
        String VALUE_SURVEY = "getAvailableSurveysDevice";
    }
}
