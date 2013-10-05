package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;

public class SurveyInstanceLoader extends DataLoader<Cursor> {
    private int mSurveyGroupId;
    private boolean mIsMonitored;
    private String mSurveyedLocaleId;

    public SurveyInstanceLoader(Context context, SurveyDbAdapter db, int surveyGroupId, 
            boolean isMonitored, String surveyedLocaleId) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mIsMonitored = isMonitored;
        mSurveyedLocaleId = surveyedLocaleId;
    }

    @Override
    public Cursor loadData(SurveyDbAdapter database) {
        if (mIsMonitored) {
            // Monitored
            return database.getSurveyInstances(mSurveyedLocaleId);
        }
        // Non monitored
        return database.getSurveyInstances(mSurveyGroupId);
    }

}
