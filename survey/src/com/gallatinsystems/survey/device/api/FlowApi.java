package com.gallatinsystems.survey.device.api;

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

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.app.FlowApp;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.parser.json.SurveyedLocaleParser;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.HttpUtil;
import com.gallatinsystems.survey.device.util.PropertyUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;

public class FlowApi {
    private static final String TAG = FlowApi.class.getSimpleName();
    
    private static final String BASE_URL;
    private static final String PHONE_NUMBER;
    private static final String IMEI;
    private static final String API_KEY;

    static {
        Context context = FlowApp.getApp();
        BASE_URL = getBaseUrl(context);
        PHONE_NUMBER = getPhoneNumber(context);
        IMEI = getImei(context);
        API_KEY = getApiKey(context);
    }
    
    public List<SurveyedLocale> getSurveyedLocales(int surveyGroup) throws IOException {
        List<SurveyedLocale> surveyedLocales = null;
        final String query =  PARAM.IMEI + IMEI
                + "&" + PARAM.LAST_UPDATED + 0// TODO
                + "&" + PARAM.PHONE_NUMBER + PHONE_NUMBER
                + "&" + PARAM.SURVEY_GROUP + surveyGroup
                + "&" + PARAM.TIMESTAMP + getTimestamp();
            
        final String url = BASE_URL + Path.SURVEYED_LOCALE 
                + "?" + query
                + "&" + PARAM.HMAC + URLEncoder.encode(getAuthorization(query), "UTF-8");
        String response = HttpUtil.httpGet(url);
        if (response != null) {
            surveyedLocales = new SurveyedLocaleParser().parseList(response);
        }
        
        return surveyedLocales;
    }
    
    private static String getBaseUrl(Context context) {
        SurveyDbAdapter db = new SurveyDbAdapter(context);
        db.open();
        String serverBase = db.findPreference(ConstantUtil.SERVER_SETTING_KEY);
        db.close();
        if (serverBase != null && serverBase.trim().length() > 0) {
            serverBase = context.getResources().getStringArray(R.array.servers)[Integer
                    .parseInt(serverBase)];
        } else {
            serverBase = new PropertyUtil(context.getResources()).
                    getProperty(ConstantUtil.SERVER_BASE);
        }
            
        return serverBase;
    }
    
    private static String getPhoneNumber(Context context) {
        try {
            return URLEncoder.encode(StatusUtil.getPhoneNumber(context), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    private static String getImei(Context context) {
        try {
            return URLEncoder.encode(StatusUtil.getImei(context), "UTF-8");
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
