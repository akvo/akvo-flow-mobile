package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.AsyncLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class SurveyInstanceLoader extends AsyncLoader<Cursor> {
    private int mSurveyGroupId;// TODO: Use it

    public SurveyInstanceLoader(Context context, int surveyGroupId) {
        super(context);
    }

    @Override
    public Cursor loadInBackground() {
        SurveyDbAdapter db = new SurveyDbAdapter(getContext())
                .open();
        Cursor cursor = db.listSurveyRespondent(ConstantUtil.SUBMITTED_STATUS, true);
        db.close();
        return cursor;
    }

}
