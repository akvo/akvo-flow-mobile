package com.gallatinsystems.survey.device.async.loader;

import java.util.List;

import android.content.Context;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.Survey;

public class SurveyListLoader extends DataLoader<List<Survey>> {
    private int mSurveyGroupId;
    
    public SurveyListLoader(Context context, SurveyDbAdapter db, int surveyGroupId) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
    }

    @Override
    protected List<Survey> loadData(SurveyDbAdapter database) {
        return database.listSurveys(mSurveyGroupId);
    }

}
