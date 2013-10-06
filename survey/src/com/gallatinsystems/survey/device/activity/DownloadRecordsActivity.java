package com.gallatinsystems.survey.device.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;

public class DownloadRecordsActivity extends ActionBarActivity {
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";
    public static final String EXTRA_SURVEY_GROUP_NAME = "survey_group_name";
    
    private int mSurveyGroupId;
    private String mSurveyGroupName;
    
    private TextView mServerRecordsView;
    private TextView mDeviceRecordsView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_records_activity);
        
        mServerRecordsView = (TextView) findViewById(R.id.totalonserver);
        mDeviceRecordsView = (TextView) findViewById(R.id.cachedonphone);
        
        mSurveyGroupId = getIntent().getIntExtra(EXTRA_SURVEY_GROUP_ID, SurveyGroup.ID_NONE);
        mSurveyGroupName = getIntent().getStringExtra(EXTRA_SURVEY_GROUP_NAME);
        
        TextView surveyGroupNameView = (TextView) findViewById(R.id.survey_group_name);
        surveyGroupNameView.setText(mSurveyGroupName);
        
        setupDeviceRecords();
    }
    
    private void setupDeviceRecords() {
        // Dirty way of getting the records...
        SurveyDbAdapter database = new SurveyDbAdapter(this).open();
        final int count = database.getSurveyedLocalesCount(mSurveyGroupId);
        database.close();
        mDeviceRecordsView.setText(String.valueOf(count));
    }

}
