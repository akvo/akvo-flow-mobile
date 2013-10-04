package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;

public class SurveyListLoader extends DataLoader<Cursor> {
    private int mSurveyGroupId;
    
    public SurveyListLoader(Context context, SurveyDbAdapter db, int surveyGroupId) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        return database.getSurveys(mSurveyGroupId);
    }

}
