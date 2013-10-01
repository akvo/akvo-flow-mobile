package com.gallatinsystems.survey.device.async.loader.base;

import android.content.Context;
import android.util.Log;

import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;

public abstract class DataLoader<D> extends AsyncLoader<AsyncResult<D>> {
    private D mResult;
    private String mErrorMsg;
    private SurveyDbAdapter mDatabase;
    
    public DataLoader(Context context) {
        super(context);
        mDatabase = new SurveyDbAdapter(context);
    }

    protected abstract D loadData(SurveyDbAdapter database);

    @Override
    public AsyncResult<D> loadInBackground() {
        mDatabase.open();
        mResult = null;
        mErrorMsg = null;
        mResult = loadData(mDatabase);
        if (mResult == null) {
            Log.e(getTag(), "Error loading data");
            mErrorMsg = "Error loading data";
        }
        mDatabase.close();

        return new AsyncResult<D>(mResult, mErrorMsg);
    }
    
    public String getTag() {
        return this.getClass().getSimpleName();
    }
    
}