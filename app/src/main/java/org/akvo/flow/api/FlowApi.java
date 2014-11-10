/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.exception.HttpException;
import org.akvo.flow.exception.HttpException.Status;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PropertyUtil;
import org.akvo.flow.util.StatusUtil;

public class FlowApi {
    private static final String TAG = FlowApi.class.getSimpleName();
    
    private static final String BASE_URL;
    private static final String PHONE_NUMBER;
    private static final String IMEI;
    private static final String API_KEY;

    static {
        Context context = FlowApp.getApp();
        BASE_URL = StatusUtil.getServerBase(context);
        PHONE_NUMBER = getPhoneNumber(context);
        IMEI = getImei(context);
        API_KEY = getApiKey(context);
    }
    
    public List<SurveyedLocale> getSurveyedLocales(long surveyGroup, String timestamp)
            throws IOException, HttpException {
        final String query =  PARAM.IMEI + IMEI
                + "&" + PARAM.LAST_UPDATED + (!TextUtils.isEmpty(timestamp)? timestamp : "0")
                + "&" + PARAM.PHONE_NUMBER + PHONE_NUMBER
                + "&" + PARAM.SURVEY_GROUP + surveyGroup
                + "&" + PARAM.TIMESTAMP + getTimestamp();
            
        final String url = BASE_URL + Path.SURVEYED_LOCALE 
                + "?" + query
                //+ "&" + PARAM.HMAC + URLEncoder.encode(getAuthorization(query), "UTF-8");
                + "&" + PARAM.HMAC + getAuthorization(query);
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

    private static String getPhoneNumber(Context context) {
        try {
            String phoneNumber = StatusUtil.getPhoneNumber(context);
            if (phoneNumber != null) {
                return URLEncoder.encode(phoneNumber, "UTF-8");
            }
            return "";
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    private static String getImei(Context context) {
        try {
            String imei = StatusUtil.getImei(context);
            if (imei != null) {
                return URLEncoder.encode(StatusUtil.getImei(context), "UTF-8");
            }
            return "";
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
    
    private static String getApiKey(Context context) {
        PropertyUtil props = new PropertyUtil(context.getResources());
        return props.getProperty(ConstantUtil.API_KEY);
    }
    
    private String getAuthorization(String query) {
        String authorization = null;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(API_KEY.getBytes(), "HmacSHA1");

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(query.getBytes());

            authorization = Base64.encodeToString(rawHmac, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage());
        } catch (InvalidKeyException e) {
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
    
    interface Path {
        String SURVEYED_LOCALE = "/surveyedlocale";
    }
    
    interface PARAM {
        String SURVEY_GROUP = "surveyGroupId=";
        String PHONE_NUMBER = "phoneNumber=";
        String IMEI         = "imei=";
        String TIMESTAMP    = "ts=";
        String LAST_UPDATED = "lastUpdateTime=";
        String HMAC         = "h=";
    }
}
