package com.gallatinsystems.survey.device.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.activity.SurveyViewActivity;
import com.gallatinsystems.survey.device.async.loader.SurveyListLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.service.BootstrapService;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class SurveyListFragment extends ListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener {
    private static final String TAG = SurveyListFragment.class.getSimpleName();
    
    // Cursor IDs
    private static final int ID_SURVEY_LIST = 0;
    //private static final int ID_SURVEY_INSTANCE_LIST = 1;
    
    private String mUserId;
    private SurveyGroup mSurveyGroup;
    private String mLocaleId;// If null, we need to create one
    
    private SurveyAdapter mAdapter;
    private SurveyDbAdapter mDatabase;
    
    public static SurveyListFragment instantiate() {
        SurveyListFragment fragment = new SurveyListFragment();
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDatabase = new SurveyDbAdapter(getActivity());
        mDatabase.open();

        if(mAdapter == null) {
            mAdapter = new SurveyAdapter(getActivity(), new ArrayList<Survey>());
            setListAdapter(mAdapter);
        }
        getListView().setOnItemClickListener(this);
    }
    
    public void refresh(SurveyGroup surveyGroup) {
        refresh(surveyGroup, null);
    }
    
    public void refresh(SurveyGroup surveyGroup, String localeId) {
        mSurveyGroup = surveyGroup;
        mLocaleId = localeId;
        getLoaderManager().restartLoader(ID_SURVEY_LIST, null, this);
    }
    
    public void setUserId(String userId) {
        mUserId = userId;
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mUserId != null) {
            if (!BootstrapService.isProcessing) {
                Survey survey = mAdapter.getItem(position);
                Intent i = new Intent(getActivity(), SurveyViewActivity.class);
                i.putExtra(ConstantUtil.USER_ID_KEY, mUserId);
                i.putExtra(ConstantUtil.SURVEY_ID_KEY, survey.getId());
                startActivity(i);
            } else {
                Toast.makeText(getActivity(), R.string.pleasewaitforbootstrap, 
                        Toast.LENGTH_LONG).show();
            }
        } else {
            // if the current user is null, we can't enter survey mode
            Toast.makeText(getActivity(), R.string.mustselectuser, Toast.LENGTH_LONG).show();
        }
    }

    class SurveyAdapter extends ArrayAdapter<Survey> {
        static final int LAYOUT_RES = android.R.layout.simple_list_item_1;

        public SurveyAdapter(Context context, List<Survey> surveys) {
            super(context, LAYOUT_RES, surveys);
        }
        
        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return isEnabled(getItem(position));
        }
        
        private boolean isEnabled(Survey survey) {
            // If the group is monitored, we need disable some surveys
            if (mSurveyGroup.isMonitored()) {
                final boolean isRegistered = !TextUtils.isEmpty(mLocaleId);
                final boolean isRegistrationSurvey = survey.getId().equals(mSurveyGroup.getRegisterSurveyId());
                if (!isRegistered) {
                    // Enable only registration survey
                    //listItem.setEnabled(isRegistrationSurvey);
                    return isRegistrationSurvey;
                } else {
                    //listItem.setEnabled(!isRegistrationSurvey);
                    return !isRegistrationSurvey;
                }
            } 
            
            return true;// Not monitored. All surveys are enabled
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View listItem = inflater.inflate(LAYOUT_RES, null);

            final Survey survey = getItem(position);
            
            TextView surveyNameView = (TextView)listItem.findViewById(android.R.id.text1);
            surveyNameView.setText(survey.getName());
            
            listItem.setEnabled(isEnabled(survey));
            
            return listItem;
        }
        
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_SURVEY_LIST:
                return new SurveyListLoader(getActivity(), mDatabase, mSurveyGroup.getId());
            /*
            case ID_SURVEY_INSTANCE_LIST:
                return new SurveyInstanceLoader(getActivity(), mDatabase, mSurveyGroup.getId(), mLocaleId);
            */
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }
        
        switch (loader.getId()) {
            case ID_SURVEY_LIST:
                mAdapter.clear();
                if (cursor.moveToFirst()) {
                    do {
                        mAdapter.add(SurveyDbAdapter.getSurvey(cursor));
                    } while (cursor.moveToNext());
                    
                } else {
                    Log.e(TAG, "onFinished() - Loader returned no data");
                }
                break;
            /*
            case ID_SURVEY_INSTANCE_LIST:
                // We just need the count. If no record exists, we should only enable register survey
                mRegistered = cursor.getCount() > 0;
                mAdapter.notifyDataSetChanged();
                break;
            */
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader.reset();
    }
    
}
