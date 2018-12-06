/*
 *  Copyright (C) 2013-2018 Stichting Akvo (Akvo Foundation)
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
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.util.DeviceHelper;
import org.akvo.flow.util.HttpUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import timber.log.Timber;

public class FlowApi {

    private static final String HTTPS_PREFIX = "https";
    private static final String HTTP_PREFIX = "http";

    private final String phoneNumber;
    private final String imei;
    private final String androidId;
    private final String deviceIdentifier;
    private final String baseUrl;

    public FlowApi(Context context) {
        DeviceHelper deviceHelper = new DeviceHelper(context);
        this.baseUrl = BuildConfig.SERVER_BASE;
        this.phoneNumber = deviceHelper.getPhoneNumber();
        this.imei = deviceHelper.getImei();
        this.androidId = deviceHelper.getAndroidId();
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

    @NonNull
    private String buildSurveysUrl(@NonNull String serverBaseUrl) {
        Uri.Builder builder = Uri.parse(serverBaseUrl).buildUpon();
        builder.appendPath(Path.SURVEY_LIST_SERVICE);
        builder.appendQueryParameter(Param.PARAM_ACTION, Param.VALUE_SURVEY);
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
        String SURVEY_LIST_SERVICE = "surveymanager";
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

        String VALUE_SURVEY = "getAvailableSurveysDevice";
    }
}
