package com.gallatinsystems.survey.device.async.loader;


import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;

public class SurveyGroupLoader extends DataLoader<Cursor> {
    private int mSurveyGroupId;
    
    public SurveyGroupLoader(Context context, SurveyDbAdapter db) {
        this(context, db, SurveyGroup.ID_NONE);
    }
    
    public SurveyGroupLoader(Context context, SurveyDbAdapter db, int surveyGroupId) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        if (SurveyGroup.ID_NONE == mSurveyGroupId) {
            return database.getSurveyGroup(mSurveyGroupId);
        }
        // Load all
        return database.getSurveyGroups();
    }

}
