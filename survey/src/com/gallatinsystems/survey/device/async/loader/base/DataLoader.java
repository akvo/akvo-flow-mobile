package com.gallatinsystems.survey.device.async.loader.base;

import android.content.Context;

import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;

public abstract class DataLoader<D> extends AsyncLoader<D> {
    private SurveyDbAdapter mDatabase;
    
    public DataLoader(Context context, SurveyDbAdapter db) {
        super(context);
        mDatabase = db;
    }

    protected abstract D loadData(SurveyDbAdapter database);

    @Override
    public D loadInBackground() {
        return loadData(mDatabase);
    }
    
}