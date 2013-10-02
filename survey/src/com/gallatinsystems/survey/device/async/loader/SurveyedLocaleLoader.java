package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.AsyncLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;

public class SurveyedLocaleLoader extends AsyncLoader<Cursor> {
    private int mSurveyGroupId;// TODO: Use it

    public SurveyedLocaleLoader(Context context, int surveyGroupId) {
        super(context);
    }

    @Override
    public Cursor loadInBackground() {
        SurveyDbAdapter db = new SurveyDbAdapter(getContext())
                .open();
        Cursor cursor = db.getSurveyLocales(mSurveyGroupId);
        db.close();
        return cursor;
    }

}
