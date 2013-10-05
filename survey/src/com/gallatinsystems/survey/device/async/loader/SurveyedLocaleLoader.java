package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;

public class SurveyedLocaleLoader extends DataLoader<Cursor> {
    private int mSurveyGroupId;
    private double mLatitude;
    private double mLongitude;
    private double mRadius;

    public SurveyedLocaleLoader(Context context, SurveyDbAdapter db, int surveyGroupId,
            double latitude, double longitude, double radius) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mLatitude = latitude;
        mLongitude = longitude;
        mRadius = radius;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        //return database.getSurveyedLocales(mSurveyGroupId);
        return database.getFilteredSurveyedLocales(mSurveyGroupId, mLatitude, mLongitude, mRadius);
    }

}
