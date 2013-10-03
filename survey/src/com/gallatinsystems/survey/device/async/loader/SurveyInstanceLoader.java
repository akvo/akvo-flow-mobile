package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;

public class SurveyInstanceLoader extends DataLoader<Cursor> {
    private int mSurveyGroupId;

    public SurveyInstanceLoader(Context context, SurveyDbAdapter db, int surveyGroupId) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
    }

    @Override
    public Cursor loadData(SurveyDbAdapter database) {
        return database.getSurveyInstances(mSurveyGroupId);
    }

}
