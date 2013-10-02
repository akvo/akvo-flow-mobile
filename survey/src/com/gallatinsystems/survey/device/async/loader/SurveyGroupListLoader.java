package com.gallatinsystems.survey.device.async.loader;

import java.util.List;

import android.content.Context;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;

public class SurveyGroupListLoader extends DataLoader<List<SurveyGroup>> {
    
    public SurveyGroupListLoader(Context context) {
        super(context);
    }

    @Override
    protected List<SurveyGroup> loadData(SurveyDbAdapter database) {
        return database.getSurveyGroups();
    }

}
