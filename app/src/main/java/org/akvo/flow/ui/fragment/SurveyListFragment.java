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

package org.akvo.flow.ui.fragment;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.async.loader.SurveyInfoLoader;
import org.akvo.flow.async.loader.SurveyInfoLoader.SurveyQuery;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceColumns;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.util.ViewUtil;
import org.ocpsoft.prettytime.PrettyTime;

public class SurveyListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {
    private static final String TAG = SurveyListFragment.class.getSimpleName();
    
    private static final String EXTRA_SURVEY_GROUP = "survey_group";
    private static final String EXTRA_RECORD       = "record";
    
    public interface SurveyListListener {
        public void onSurveyClick(String surveyId);
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
            mAdapter = new SurveyAdapter(getActivity());
            setListAdapter(mAdapter);
        }
        getListView().setOnItemClickListener(this);
        refresh();
    }
    
    public void refresh() {
        // Calculate if this locale is not registered yet
        if (mSurveyGroup.isMonitored() && mRecord != null) {
            mRegistered = false;
            Cursor cursor = mDatabase.getSurveyInstances(mRecord.getId());
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    final int col = cursor.getColumnIndexOrThrow(SurveyInstanceColumns.SUBMITTED_DATE);
                    do {
                        if (!cursor.isNull(col)) {
                            mRegistered = true;
                        }
                    } while (cursor.moveToNext() && !mRegistered);
                }
                cursor.close();
            }
        }
        
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String surveyId = mAdapter.getItem(position).mId;
        if (mSurveyGroup.isMonitored() && mRegistered &&
                surveyId.equals(mSurveyGroup.getRegisterSurveyId())) {
            // Attempting to answer the registration form multiple times displays a warning
            ViewUtil.showConfirmDialog(R.string.reg_form_warning_title,
                    R.string.reg_form_warning_text,
                    getActivity(),
                    true,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onSurveyClick(surveyId);
                        }
                    });
        } else {
            mListener.onSurveyClick(surveyId);
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Add this fragment's options to the 'more' submenu
        menu.removeItem(R.id.more_submenu);
    }

    private boolean isRegistrationSurvey(String surveyId) {
        return surveyId.equals(mSurveyGroup.getRegisterSurveyId());
    }

    class SurveyAdapter extends ArrayAdapter<SurveyInfo> {
        static final int LAYOUT_RES = R.layout.survey_item;

        public SurveyAdapter(Context context) {
            super(context, LAYOUT_RES, new ArrayList<SurveyInfo>());
        }
        
        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return isEnabled(getItem(position).mId);
        }
        
        private boolean isEnabled(String surveyId) {
            if (mSurveyGroup.isMonitored()) {
                return mRegistered || isRegistrationSurvey(surveyId);
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

            final SurveyInfo surveyInfo = getItem(position);

            ImageView icon = (ImageView)listItem.findViewById(R.id.survey_icon);
            TextView surveyNameView = (TextView)listItem.findViewById(R.id.text1);
            TextView surveyVersionView = (TextView)listItem.findViewById(R.id.text2);
            TextView lastSubmissionTitle = (TextView)listItem.findViewById(R.id.date_label);
            TextView lastSubmissionView = (TextView)listItem.findViewById(R.id.date);
            surveyNameView.setText(surveyInfo.mName);
            surveyVersionView.setText("v" + surveyInfo.mVersion);

            final boolean enabled = isEnabled(surveyInfo.mId);
            listItem.setEnabled(enabled);
            surveyNameView.setEnabled(enabled);
            surveyVersionView.setEnabled(enabled);

            int iconRes = R.drawable.survey_icon;
            boolean showLastSubmission = false;
            if (mSurveyGroup.isMonitored()) {
                if (surveyInfo.mLastSubmission != null) {
                    showLastSubmission = true;
                    iconRes = isRegistrationSurvey(surveyInfo.mId) ?
                            R.drawable.register_survey_done_icon
                            : R.drawable.survey_done_icon;

                } else if (isRegistrationSurvey(surveyInfo.mId)) {
                    iconRes = R.drawable.register_survey_icon;
                }
            }
            icon.setImageResource(iconRes);

            if (showLastSubmission) {
                String time = new PrettyTime().format(new Date(surveyInfo.mLastSubmission));
                lastSubmissionView.setText(time);
                lastSubmissionTitle.setVisibility(View.VISIBLE);
                lastSubmissionView.setVisibility(View.VISIBLE);
            } else {
                lastSubmissionTitle.setVisibility(View.GONE);
                lastSubmissionView.setVisibility(View.GONE);
            }

            return listItem;
        }
        
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String recordId = mRecord != null ? mRecord.getId() : null;
        return new SurveyInfoLoader(getActivity(), mDatabase, mSurveyGroup.getId(), recordId);
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
                SurveyInfo s = new SurveyInfo();
                s.mId = cursor.getString(SurveyQuery.SURVEY_ID);
                s.mName = cursor.getString(SurveyQuery.NAME);
                s.mVersion = String.valueOf(cursor.getFloat(SurveyQuery.VERSION));
                if (!cursor.isNull(SurveyQuery.SUBMITTED)) {
                    s.mLastSubmission = cursor.getLong(SurveyQuery.SUBMITTED);
                }

                if (mSurveyGroup.isMonitored() && isRegistrationSurvey(s.mId)) {
                    mAdapter.insert(s, 0);// Make sure registration survey is at the top
                } else {
                    mAdapter.add(s);
                }
            } while (cursor.moveToNext());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader.reset();
    }

    /**
     * Wrapper for the data displayed in the list
     */
    class SurveyInfo {
        String mId;
        String mName;
        String mVersion;
        Long mLastSubmission;
    }

}
