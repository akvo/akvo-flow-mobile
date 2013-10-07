package com.gallatinsystems.survey.device.parser.json;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.gallatinsystems.survey.device.domain.QuestionResponse;
import com.gallatinsystems.survey.device.parser.FlowParser;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class QuestionResponseParser implements FlowParser<QuestionResponse> {
    private static final String TAG = QuestionResponseParser.class.getSimpleName();

    @Override
    public QuestionResponse parse(InputStream inputStream) {
        return null;
    }

    @Override
    public QuestionResponse parse(String response) {
        return null;
    }

    public QuestionResponse parse(JSONObject jSurveyedLocale) {
        try {
            String val = jSurveyedLocale.getString(Attrs.ANSWER);
            String questionId = jSurveyedLocale.getString(Attrs.QUESTION_ID);
            
            return new QuestionResponse(val, ConstantUtil.VALUE_RESPONSE_TYPE, questionId);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public List<QuestionResponse> parseList(String questionResponses) {
        return null;
    }

    public List<QuestionResponse> parseList(JSONArray jResponses) {
        List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
        try {
            for (int i=0; i<jResponses.length(); i++) {
                JSONObject jQResponse = jResponses.getJSONObject(i);
                QuestionResponse response = parse(jQResponse);
                if (response != null) {
                    responses.add(response);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        
        return responses;
    }
    
    interface Attrs {
        String QUESTION_ID = "q";
        String ANSWER      = "a";
    }

}
