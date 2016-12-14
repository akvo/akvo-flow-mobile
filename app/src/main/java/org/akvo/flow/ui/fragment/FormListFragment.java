/*
 *  Copyright (C) 2013-2016 Stichting Akvo (Akvo Foundation)
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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.async.loader.SurveyInfoLoader;
import org.akvo.flow.async.loader.SurveyInfoLoader.SurveyQuery;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.util.PlatformUtil;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

public class FormListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {
    private static final String TAG = FormListFragment.class.getSimpleName();

    private static final String EXTRA_SURVEY_GROUP = "survey_group";
    private static final String EXTRA_RECORD = "record";

    public interface SurveyListListener {
        void onSurveyClick(String surveyId);
    }

    private SurveyGroup mSurveyGroup;
    private SurveyedLocale mRecord;
    private boolean mRegistered;

    private SurveyAdapter mAdapter;
    private SurveyDbAdapter mDatabase;
    private SurveyListListener mListener;

    public FormListFragment() {
    }

    public static FormListFragment newInstance(SurveyGroup surveyGroup, SurveyedLocale record) {
        FormListFragment fragment = new FormListFragment();
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
            mListener = (SurveyListListener) activity;
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        if (mAdapter == null) {
            mAdapter = new SurveyAdapter(getActivity());
            setListAdapter(mAdapter);
        }
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabase = new SurveyDbAdapter(getActivity());
        mDatabase.open();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDatabase.close();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String surveyId = mAdapter.getItem(position).mId;
        mListener.onSurveyClick(surveyId);
    }

    private boolean isRegistrationSurvey(String surveyId) {
        return surveyId.equals(mSurveyGroup.getRegisterSurveyId());
    }

    //TODO: make static to avoid memory leaks
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
            SurveyInfo surveyInfo = getItem(position);
            return !surveyInfo.mDeleted && isSurveyEnabled(surveyInfo.mId);
        }

        private boolean isSurveyEnabled(String surveyId) {
            if (mSurveyGroup.isMonitored()) {
                return isRegistrationSurvey(surveyId) != mRegistered;
            }

            return !mRegistered;// Not monitored. Only one response allowed
        }

        @NonNull @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                listItem = inflater.inflate(LAYOUT_RES, null);
            }

            final SurveyInfo surveyInfo = getItem(position);

            TextView surveyNameView = (TextView) listItem.findViewById(R.id.survey_name_tv);
            TextView lastSubmissionTitle = (TextView) listItem.findViewById(R.id.date_label);
            TextView lastSubmissionView = (TextView) listItem.findViewById(R.id.date);

            StringBuilder surveyExtraInfo = new StringBuilder(20);
            String version = surveyInfo == null ? "" : surveyInfo.mVersion;
            surveyExtraInfo.append(" v").append(version);

            boolean enabled = isSurveyEnabled(surveyInfo.mId);
            if (surveyInfo.mDeleted) {
                enabled = false;
                surveyExtraInfo.append(" - ").append(getString(R.string.form_deleted));
            }
            SpannableString versionSpannable = getSpannableString(
                    getResources().getDimensionPixelSize(R.dimen.survey_version_text_size),
                    surveyExtraInfo.toString());
            SpannableString titleSpannable = getSpannableString(
                    getResources().getDimensionPixelSize(R.dimen.survey_title_text_size),
                    surveyInfo.mName);
            surveyNameView.setText(TextUtils.concat(titleSpannable, versionSpannable));
            listItem.setEnabled(enabled);
            surveyNameView.setEnabled(enabled);

            if (surveyInfo.mLastSubmission != null && !isRegistrationSurvey(surveyInfo.mId)) {
                String time = new PrettyTime().format(new Date(surveyInfo.mLastSubmission));
                lastSubmissionView.setText(time);
                lastSubmissionTitle.setVisibility(View.VISIBLE);
                lastSubmissionView.setVisibility(View.VISIBLE);
            } else {
                lastSubmissionTitle.setVisibility(View.GONE);
                lastSubmissionView.setVisibility(View.GONE);
            }

            // Alternate background
            int attr = position % 2 == 0 ? R.attr.listitem_bg1 : R.attr.listitem_bg2;
            final int res = PlatformUtil.getResource(getContext(), attr);
            listItem.setBackgroundResource(res);

            return listItem;
        }

        @NonNull
        private SpannableString getSpannableString(int textSize, String string) {
            SpannableString spannable = new SpannableString(string);
            spannable.setSpan(new AbsoluteSizeSpan(textSize), 0, string.length(),
                    SPAN_INCLUSIVE_INCLUSIVE);
            return spannable;
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyInfoLoader(getActivity(), mDatabase, mSurveyGroup.getId(),
                mRecord.getId());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }

        mAdapter.clear();
        List<SurveyInfo> surveys = new ArrayList<>();// Buffer items before adapter addition
        mRegistered = false; // Calculate if this record is registered yet
        if (cursor.moveToFirst()) {
            do {
                SurveyInfo s = new SurveyInfo();
                s.mId = cursor.getString(SurveyQuery.SURVEY_ID);
                s.mName = cursor.getString(SurveyQuery.NAME);
                s.mVersion = String.valueOf(cursor.getFloat(SurveyQuery.VERSION));
                s.mDeleted = cursor.getInt(SurveyQuery.DELETED) == 1;
                if (!cursor.isNull(SurveyQuery.SUBMITTED)) {
                    s.mLastSubmission = cursor.getLong(SurveyQuery.SUBMITTED);
                    mRegistered = true;
                }

                if (mSurveyGroup.isMonitored() && isRegistrationSurvey(s.mId)) {
                    surveys.add(0, s);// Make sure registration survey is at the top
                } else {
                    surveys.add(s);
                }
            } while (cursor.moveToNext());
        }

        // Dump the temporary list into the adapter. This way mRegistered it's been
        // properly initialized, having looped through all the items first.
        for (SurveyInfo s : surveys) {
            mAdapter.add(s);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader.reset();
    }

    /**
     * Wrapper for the data displayed in the list
     */
    private static class SurveyInfo {
        String mId;
        String mName;
        String mVersion;
        Long mLastSubmission;
        boolean mDeleted;
    }

}
