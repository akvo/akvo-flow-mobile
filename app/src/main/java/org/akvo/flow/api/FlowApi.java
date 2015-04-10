/*
 *  Copyright (C) 2013-2015 Stichting Akvo (Akvo Foundation)
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.akvo.flow.api.parser.json.SurveyedLocaleParser;
import org.akvo.flow.api.response.SurveyedLocalesResponse;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.Instance;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.exception.HttpException;
import org.akvo.flow.exception.HttpException.Status;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.Prefs;
import org.akvo.flow.util.StatusUtil;

public class FlowApi {
    private static final String TAG = FlowApi.class.getSimpleName();
    
    private static final String PHONE_NUMBER;
    private static final String IMEI;

    private Instance mInstance;
    private String mBaseUrl;

    static {
        Context context = FlowApp.getApp();
        PHONE_NUMBER = StatusUtil.getPhoneNumber(context);
        IMEI = StatusUtil.getImei(context);
    }

    public FlowApi(Instance instance) {
        mInstance = instance;
        mBaseUrl = Prefs.getString(FlowApp.getApp(), Prefs.KEY_APP_SERVER, null);
        if (TextUtils.isEmpty(mBaseUrl)) {
            mBaseUrl = mInstance.getServerBase();
        }
    }
    
    public List<SurveyedLocale> getSurveyedLocales(long surveyGroup, String timestamp)
            throws IOException, HttpException {
        final String query = Param.IMEI + URLEncode(IMEI)
                + "&" + Param.LAST_UPDATED + (!TextUtils.isEmpty(timestamp)? timestamp : "0")
                + "&" + Param.PHONE_NUMBER + URLEncode(PHONE_NUMBER)
                + "&" + Param.SURVEY_GROUP + surveyGroup
                + "&" + Param.TIMESTAMP + getTimestamp();

        final String url = mBaseUrl + Path.SURVEYED_LOCALE
                + "?" + query
                //+ "&" + PARAM.HMAC + URLEncoder.encode(getAuthorization(query), "UTF-8");
                + "&" + Param.HMAC + getAuthorization(query);
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

    private static String URLEncode(String param) {
        if (TextUtils.isEmpty(param)) {
            return "";
        }
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
            return "";
        }
    }

    private String getAuthorization(String query) {
        String authorization = null;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(mInstance.getApiKey().getBytes(), "HmacSHA1");

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(query.getBytes());

            authorization = Base64.encodeToString(rawHmac, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, e.getMessage());
        }
        
        return authorization;
    }
    
    @SuppressLint("SimpleDateFormat")
    private String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        try {
            return URLEncoder.encode(dateFormat.format(new Date()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public static String getDeviceParams() {
        Context context = FlowApp.getApp();
        return Param.PHONE_NUMBER + URLEncode(PHONE_NUMBER)
                + "&" + Param.ANDROID_ID + URLEncode(PlatformUtil.getAndroidID(context))
                + "&" + Param.IMEI + URLEncode(IMEI)
                + "&" + Param.VERSION + URLEncode(PlatformUtil.getVersionName(context))
                + "&" + Param.DEVICE_ID + URLEncode(StatusUtil.getDeviceId(context));
    }
    
    interface Path {
        String SURVEYED_LOCALE = "/surveyedlocale";
    }
    
    interface Param {
        String SURVEY_GROUP = "surveyGroupId=";
        String PHONE_NUMBER = "phoneNumber=";
        String IMEI         = "imei=";
        String TIMESTAMP    = "ts=";
        String LAST_UPDATED = "lastUpdateTime=";
        String HMAC         = "h=";
        String VERSION      = "ver=";
        String DEVICE_ID    = "devId=";
        String ANDROID_ID   = "androidId=";
    }
}
