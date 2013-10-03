package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class SurveyInstanceLoader extends DataLoader<Cursor> {
    private int mSurveyGroupId;// TODO: Use it

    public SurveyInstanceLoader(Context context, SurveyDbAdapter db, int surveyGroupId) {
        super(context, db);
    }

    @Override
    public Cursor loadData(SurveyDbAdapter database) {
        return database.listSurveyRespondent(ConstantUtil.SUBMITTED_STATUS, true);
    }

}
