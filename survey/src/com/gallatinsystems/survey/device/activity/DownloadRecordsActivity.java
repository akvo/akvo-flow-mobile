package com.gallatinsystems.survey.device.activity;


import java.net.URLEncoder;
import java.util.List;

import org.apache.http.HttpException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.exception.PersistentUncaughtExceptionHandler;
import com.gallatinsystems.survey.device.parser.json.SurveyedLocaleParser;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.HttpUtil;
import com.gallatinsystems.survey.device.util.PropertyUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;

public class DownloadRecordsActivity extends ActionBarActivity {
    private static final String TAG = DownloadRecordsActivity.class.getSimpleName();
    
    private static final String SURVEYED_LOCALE_SERVICE_PATH = "/surveyedlocale?surveyGroupId=";
    private static final String PARAM_PHONE_NUMBER = "&phoneNumber=";
    private static final String PARAM_IMEI = "&imei=";
    private static final String PARAM_ONLY_COUNT = "&checkAvailable=";
    
    
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";
    public static final String EXTRA_SURVEY_GROUP_NAME = "survey_group_name";
    
    private int mSurveyGroupId;
    private String mSurveyGroupName;
    
    private SurveyDbAdapter mDatabase;
    private String mServerBase;
    
    private TextView mServerRecordsView;
    private TextView mDeviceRecordsView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_records_activity);
        
        mServerRecordsView = (TextView) findViewById(R.id.totalonserver);
        mDeviceRecordsView = (TextView) findViewById(R.id.cachedonphone);
        ((Button) findViewById(R.id.downloadRecords)).setOnClickListener(mOnDownloadRecordsListener);
        
        mSurveyGroupId = getIntent().getIntExtra(EXTRA_SURVEY_GROUP_ID, SurveyGroup.ID_NONE);
        mSurveyGroupName = getIntent().getStringExtra(EXTRA_SURVEY_GROUP_NAME);
        
        TextView surveyGroupNameView = (TextView) findViewById(R.id.survey_group_name);
        surveyGroupNameView.setText(mSurveyGroupName);
        
        // Tmp hack
        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();
        mServerBase = getServerBase();
        
        setupDeviceRecords();
        
        mServerRecordsView.setText("Loading...");
        new DownloadRecordsTask().execute(DownloadRecordsTask.MODE_COUNT);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDatabase.close();
    }
    
    private void setupDeviceRecords() {
        final int count = mDatabase.getSurveyedLocalesCount(mSurveyGroupId);
        mDeviceRecordsView.setText(String.valueOf(count));
    }
    
    class DownloadRecordsTask extends AsyncTask<Integer, Void, Integer> {
        public static final int MODE_COUNT    = 0;
        public static final int MODE_DOWNLOAD = 1;
        
        private int mMode;

        @Override
        protected Integer doInBackground(Integer... params) {
            mMode = params[0];
            switch (mMode) {
                case MODE_COUNT:
                    return getServerRecordCount();
                case MODE_DOWNLOAD:
                    return downloadRecords();
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            if (result != null) {
                switch (mMode) {
                    case MODE_COUNT:
                        mServerRecordsView.setText(result.toString());
                        break;
                    case MODE_DOWNLOAD:
                        Toast.makeText(DownloadRecordsActivity.this, "Synced " + result.toString()  + " records", 
                                Toast.LENGTH_LONG).show();
                        setupDeviceRecords();
                        break;
                }
            }
        }
        
        private Integer getServerRecordCount() {
            String response = null;
            int count = 0;
            try {
                final String url = mServerBase
                        + SURVEYED_LOCALE_SERVICE_PATH + mSurveyGroupId
                        + PARAM_PHONE_NUMBER + URLEncoder.encode(StatusUtil.getPhoneNumber(DownloadRecordsActivity.this), "UTF-8")
                        + PARAM_IMEI + URLEncoder.encode(StatusUtil.getImei(DownloadRecordsActivity.this), "UTF-8")
                        + PARAM_ONLY_COUNT + true;
                response = HttpUtil.httpGet(url);
                if (response != null) {
                    count = new SurveyedLocaleParser().getSurveyedLocaleCount(response);
                }
            } catch (HttpException e) {
                Log.e(TAG, "Server returned an unexpected response", e);
                PersistentUncaughtExceptionHandler.recordException(e);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                PersistentUncaughtExceptionHandler.recordException(e);
            }
            
            return count;
        }
        
        private Integer downloadRecords() {
            String response = null;
            int syncedRecords = 0;
            try {
                final String url = mServerBase
                        + SURVEYED_LOCALE_SERVICE_PATH + mSurveyGroupId
                        + PARAM_PHONE_NUMBER + URLEncoder.encode(StatusUtil.getPhoneNumber(DownloadRecordsActivity.this), "UTF-8")
                        + PARAM_IMEI + URLEncoder.encode(StatusUtil.getImei(DownloadRecordsActivity.this), "UTF-8")
                        + PARAM_ONLY_COUNT + false;
                response = HttpUtil.httpGet(url);
                if (response != null) {
                    List<SurveyedLocale> surveyedLocales = new SurveyedLocaleParser().parseList(response);
                    if (surveyedLocales != null) {
                        SurveyDbAdapter database = new SurveyDbAdapter(DownloadRecordsActivity.this).open();
                        database.syncSurveyedLocales(surveyedLocales);
                        database.close();
                        syncedRecords = surveyedLocales.size();
                    }
                }
            } catch (HttpException e) {
                Log.e(TAG, "Server returned an unexpected response", e);
                PersistentUncaughtExceptionHandler.recordException(e);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                PersistentUncaughtExceptionHandler.recordException(e);
            }
            
            return syncedRecords;
        }
        
    }
            
    private String getServerBase() {
        String serverBase = mDatabase.findPreference(ConstantUtil.SERVER_SETTING_KEY);
        if (serverBase != null && serverBase.trim().length() > 0) {
            serverBase = getResources().getStringArray(R.array.servers)[Integer
                    .parseInt(serverBase)];
        } else {
            serverBase = new PropertyUtil(getResources()).
                    getProperty(ConstantUtil.SERVER_BASE);
        }
            
        return serverBase;
    }
    
    private OnClickListener mOnDownloadRecordsListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            Toast.makeText(DownloadRecordsActivity.this, "Downloading...", Toast.LENGTH_LONG).show();
            new DownloadRecordsTask().execute(DownloadRecordsTask.MODE_DOWNLOAD);
        }
    };

}
