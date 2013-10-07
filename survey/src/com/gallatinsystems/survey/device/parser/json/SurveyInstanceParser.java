package com.gallatinsystems.survey.device.parser.json;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.gallatinsystems.survey.device.domain.QuestionResponse;
import com.gallatinsystems.survey.device.domain.SurveyInstance;
import com.gallatinsystems.survey.device.parser.FlowParser;

public class SurveyInstanceParser implements FlowParser<SurveyInstance> {
    private static final String TAG = SurveyInstanceParser.class.getSimpleName();

    @Override
    public SurveyInstance parse(InputStream inputStream) {
        return null;
    }

    @Override
    public SurveyInstance parse(String response) {
        return null;
    }

    @Override
    public List<SurveyInstance> parseList(String response) {
        return null;
    }

    public SurveyInstance parse(JSONObject jSurveyInstance) {
        try {
            String uuid = jSurveyInstance.getString(Attrs.UUID);
            String surveyId = jSurveyInstance.getString(Attrs.SURVEY_ID);
            long date = jSurveyInstance.getLong(Attrs.DATE);
            
            JSONArray jQuestionResponses = jSurveyInstance.getJSONArray(Attrs.QUESTION_RESPONSE_LIST);
            List<QuestionResponse> responses = new QuestionResponseParser().parseList(jQuestionResponses);
            
            return new SurveyInstance(uuid, surveyId, date, responses);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public List<SurveyInstance> parseList(JSONArray jSurveyInstances) {
        List<SurveyInstance> surveyInstances = new ArrayList<SurveyInstance>();
        try {
            for (int i=0; i<jSurveyInstances.length(); i++) {
                JSONObject jSurveyInstance = jSurveyInstances.getJSONObject(i);
                SurveyInstance surveyInstance = parse(jSurveyInstance);
                if (surveyInstance != null) {
                    surveyInstances.add(surveyInstance);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        
        return surveyInstances;
    }
    
    interface Attrs {
        String UUID                   = "uuid";
        String DATE                   = "collectionDate";
        String SURVEY_ID              = "surveyId";
        String QUESTION_RESPONSE_LIST = "qasList";
    }

}
