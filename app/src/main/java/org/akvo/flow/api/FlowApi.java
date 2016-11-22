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

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
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
import org.json.JSONObject;

public class FlowApi {

    private static final String TAG = FlowApi.class.getSimpleName();

    private static final String API_KEY;
    private static final String PHONE_NUMBER;
    private static final String IMEI;
    private static final String SURVEY_LIST_SERVICE_PATH = "/surveymanager?action=getAvailableSurveysDevice";
    private static final String SURVEY_HEADER_SERVICE_PATH = "/surveymanager?action=getSurveyHeader&surveyId=";
    private static final String NOTIFICATION_PATH = "/processor?action=";
    public static final String DEVICE_NOTIFICATION_PATH = "/devicenotification";

    // Sync constants
    private static final String FILENAME_PARAM = "&fileName=";
    private static final String FORMID_PARAM = "&formID=";
    private static final int ERROR_UNKNOWN = -1;

    private static final String HMAC_SHA_1_ALGORITHM = "HmacSHA1";

    static {
        Context context = FlowApp.getApp();
        API_KEY = getApiKey(context);
        PHONE_NUMBER = StatusUtil.getPhoneNumber(context);
        IMEI = StatusUtil.getImei(context);
    }

    /**
     * Request the notifications GAE has ready for us, like the list of missing files.
     * @param serverBase
     * @return String body of the HTTP response
     * @throws Exception
     */
    @Nullable
    public JSONObject getDeviceNotification(String serverBase, String surveyIds) throws Exception {
        // Send the list of surveys we've got downloaded, getting notified of the deleted ones
        String url = serverBase + DEVICE_NOTIFICATION_PATH + "?" + getDeviceParams() + surveyIds;
        String response = HttpUtil.httpGet(url);
        if (!TextUtils.isEmpty(response)) {
            return new JSONObject(response);
        }
        return null;
    }

    public void getSurveyHeader(String serverBaseUrl, List<Survey> surveys, String id) throws IOException {
        final String url = serverBaseUrl + SURVEY_HEADER_SERVICE_PATH + id + "&" + getDeviceParams();
        String response = HttpUtil.httpGet(url);
        if (response != null) {
            surveys.addAll(new SurveyMetaParser().parseList(response, true));
        }
    }

    public List<Survey> getSurveys(String serverBase, List<Survey> surveys) throws IOException {
        final String url = serverBase + SURVEY_LIST_SERVICE_PATH + "&" + getDeviceParams();
        String response = HttpUtil.httpGet(url);
        if (response != null) {
            surveys = new SurveyMetaParser().parseList(response);
        }
        return surveys;
    }

    /**
     * Notify GAE back-end that data is available
     * Sends a message to the service with the file name that was just uploaded
     * so it can start processing the file
     */
    public int sendProcessingNotification(String serverBaseUrl, String formId, String action, String fileName) {
        String url = serverBaseUrl + NOTIFICATION_PATH + action
                + FORMID_PARAM + formId
                + FILENAME_PARAM + fileName + "&" + getDeviceParams();
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

    @Nullable
    public List<SurveyedLocale> getSurveyedLocales(String serverBaseUrl, long surveyGroup, String timestamp)
            throws IOException {
        Context context = FlowApp.getApp();
        // Note: To compute the HMAC auth token, query params must be alphabetically ordered
        final String query = Param.ANDROID_ID + URLEncode(PlatformUtil.getAndroidID(context))
                + "&" + Param.IMEI + URLEncode(IMEI)
                + "&" + Param.LAST_UPDATED + (!TextUtils.isEmpty(timestamp)? timestamp : "0")
                + "&" + Param.PHONE_NUMBER + URLEncode(PHONE_NUMBER)
                + "&" + Param.SURVEY_GROUP + surveyGroup
                + "&" + Param.TIMESTAMP + getTimestamp();

        final String url = serverBaseUrl + Path.SURVEYED_LOCALE
                + "?" + query
                + "&" + Param.HMAC + getAuthorization(query);
        String response = HttpUtil.httpGet(url);
        if (response != null) {
            Log.d(TAG, response); //TODO: remove
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

    private static String getApiKey(Context context) {
        PropertyUtil props = new PropertyUtil(context.getResources());
        return props.getProperty(ConstantUtil.API_KEY);
    }
    
    private String getAuthorization(String query) {
        String authorization = null;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(API_KEY.getBytes(), HMAC_SHA_1_ALGORITHM);

            Mac mac = Mac.getInstance(HMAC_SHA_1_ALGORITHM);
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
