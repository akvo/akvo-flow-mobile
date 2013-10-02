package com.gallatinsystems.survey.device.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
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
import com.gallatinsystems.survey.device.async.loader.base.AsyncResult;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.service.BootstrapService;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class SurveyListFragment extends ListFragment implements LoaderCallbacks<AsyncResult<List<Survey>>>, OnItemClickListener {
    private static final String TAG = SurveyListFragment.class.getSimpleName();
    private static final String ARG_SURVEY_GROUP = "survey_group";
    private static final int ID_SURVEY_LIST = 0;
    
    private String mUserId;
    private int mSurveyGroupId;
    private SurveyAdapter mAdapter;
    
    public static SurveyListFragment instantiate(int surveyGroupId) {
        SurveyListFragment fragment = new SurveyListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SURVEY_GROUP, surveyGroupId);
        fragment.setArguments(bundle);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroupId = getArguments().getInt(ARG_SURVEY_GROUP);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //setRetainInstance(true);

        if(mAdapter == null) {
            mAdapter = new SurveyAdapter(getActivity(), new ArrayList<Survey>());
            setListAdapter(mAdapter);
        }
        getListView().setOnItemClickListener(this);
        //getLoaderManager().restartLoader(ID_SURVEY_LIST, null, this);
    }
    
    public void setSurveyGroup(int surveyGroupId) {
        mSurveyGroupId = surveyGroupId;
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
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View listItem = inflater.inflate(LAYOUT_RES, null);

            final Survey survey = getItem(position);
            
            TextView surveyNameView = (TextView)listItem.findViewById(android.R.id.text1);
            surveyNameView.setText(survey.getName());
            
            return listItem;
        }
        
    }

    @Override
    public Loader<AsyncResult<List<Survey>>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_SURVEY_LIST:
                return new SurveyListLoader(getActivity(), mSurveyGroupId);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<AsyncResult<List<Survey>>> loader,
            AsyncResult<List<Survey>> result) {
        Exception e = result.getException();
        if (e != null) {
            Log.e(TAG, e.getMessage());
            return;
        }
        
        List<Survey> surveys = result.getData();
        if (surveys != null) {
            mAdapter.clear();
            for (Survey survey : surveys) {
                mAdapter.add(survey);
            }
        } else {
            Log.e(TAG, "onFinished() - Loader returned no data");
        }
    }

    @Override
    public void onLoaderReset(Loader<AsyncResult<List<Survey>>> loader) {
        loader.reset();
    }
    
}
