/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package com.gallatinsystems.survey.device.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.async.loader.SurveyListLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;

public class SurveyListFragment extends ListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener {
    private static final String TAG = SurveyListFragment.class.getSimpleName();
    
    private static final String EXTRA_SURVEY_GROUP = "survey_group";
    private static final String EXTRA_RECORD       = "record";
    
    public interface SurveyListListener {
        public void startSurvey(Survey survey);
    }
    
    private SurveyGroup mSurveyGroup;
    private SurveyedLocale mRecord;
    private boolean mRegistered;
    
    private SurveyAdapter mAdapter;
    private SurveyDbAdapter mDatabase;
    private SurveyListListener mListener;
    
    public static SurveyListFragment instantiate(SurveyGroup surveyGroup, SurveyedLocale record) {
        SurveyListFragment fragment = new SurveyListFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_SURVEY_GROUP, surveyGroup);
        args.putSerializable(EXTRA_RECORD, record);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (SurveyListListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SurveyListListener");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroup = (SurveyGroup) getArguments().getSerializable(EXTRA_SURVEY_GROUP);
        mRecord = (SurveyedLocale) getArguments().getSerializable(EXTRA_RECORD);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        mDatabase = new SurveyDbAdapter(getActivity());
        mDatabase.open();

        if(mAdapter == null) {
            mAdapter = new SurveyAdapter(getActivity(), new ArrayList<Survey>());
            setListAdapter(mAdapter);
        }
        getListView().setOnItemClickListener(this);
        refresh();
    }
    
    public void refresh() {
        // Calculate if this locale is not registered yet
        if (mSurveyGroup.isMonitored() && mRecord != null) {
            mRegistered = mDatabase.getSurveyInstances(mRecord.getId()).getCount() > 0;
        }
        
        getLoaderManager().restartLoader(0, null, this);
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Survey survey = mAdapter.getItem(position);
        mListener.startSurvey(survey);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Add this fragment's options to the 'more' submenu
        menu.removeItem(R.id.more_submenu);
    }

    class SurveyAdapter extends ArrayAdapter<Survey> {
        static final int LAYOUT_RES = R.layout.survey_item;

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
            // If the group is monitored, we need to disable some surveys
            if (mSurveyGroup.isMonitored()) {
                boolean isRegistrationForm = survey.getId().equals(
                        mSurveyGroup.getRegisterSurveyId());
                if (mRegistered) {
                    // Disable only registration survey
                    return !isRegistrationForm;
                } else {
                    // Enable only registration survey
                    return isRegistrationForm;
                }
            }
            
            return true;// Not monitored. All surveys are enabled
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                listItem = inflater.inflate(LAYOUT_RES, null);
            }

            final Survey survey = getItem(position);
            
            TextView surveyNameView = (TextView)listItem.findViewById(R.id.text1);
            TextView surveyVersionView = (TextView)listItem.findViewById(R.id.text2);
            surveyNameView.setText(survey.getName());
            surveyVersionView.setText("v" + survey.getVersion());
            
            boolean enabled = isEnabled(survey);
            listItem.setEnabled(enabled);
            surveyNameView.setEnabled(enabled);
            surveyVersionView.setEnabled(enabled);
            
            return listItem;
        }
        
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyListLoader(getActivity(), mDatabase, mSurveyGroup.getId());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }
        
        mAdapter.clear();
        if (cursor.moveToFirst()) {
            do {
                mAdapter.add(SurveyDbAdapter.getSurvey(cursor));
            } while (cursor.moveToNext());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader.reset();
    }
    
}
