/*
 *  Copyright (C) 2013-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.data.loader.SurveyInfoLoader;
import org.akvo.flow.data.loader.models.SurveyInfo;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.ui.model.ViewSurveyInfo;
import org.akvo.flow.ui.model.ViewSurveyInfoMapper;
import org.akvo.flow.util.PlatformUtil;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

public class FormListFragment extends ListFragment
        implements LoaderCallbacks<Pair<List<SurveyInfo>, Boolean>>, OnItemClickListener {

    private static final String EXTRA_SURVEY_GROUP = "survey_group";
    private static final String EXTRA_RECORD = "record";

    private SurveyGroup mSurveyGroup;
    private SurveyedLocale mRecord;
    private SurveyAdapter mAdapter;
    private SurveyListListener mListener;
    private final ViewSurveyInfoMapper mapper = new ViewSurveyInfoMapper();

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
            mAdapter = new SurveyAdapter(getActivity(), mSurveyGroup);
            setListAdapter(mAdapter);
        }
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String surveyId = mAdapter.getItem(position).getId();
        mListener.onSurveyClick(surveyId);
    }

   static class SurveyAdapter extends ArrayAdapter<ViewSurveyInfo> {

        private static final int LAYOUT_RES = R.layout.survey_item;
        private final SurveyGroup mSurveyGroup;
        private final int[] backgrounds;
        private final int versionTextSize;
        private final int titleTextSize;

        public SurveyAdapter(Context context, SurveyGroup surveyGroup) {
            super(context, LAYOUT_RES, new ArrayList<ViewSurveyInfo>());
            this.mSurveyGroup = surveyGroup;
            this.backgrounds = new int[2];
            backgrounds[0] = PlatformUtil.getResource(getContext(), R.attr.listitem_bg1);
            backgrounds[1] = PlatformUtil.getResource(getContext(), R.attr.listitem_bg2);
            this.versionTextSize = context.getResources()
                    .getDimensionPixelSize(R.dimen.survey_version_text_size);
            this.titleTextSize = context.getResources()
                    .getDimensionPixelSize(R.dimen.survey_title_text_size);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            ViewSurveyInfo surveyInfo = getItem(position);
            return surveyInfo.isEnabled();
        }

        @NonNull @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            SurveyInfoViewHolder surveyInfoViewHolder;
            if (listItem == null) {
                listItem = LayoutInflater.from(getContext()).inflate(LAYOUT_RES, null);
                surveyInfoViewHolder = new SurveyInfoViewHolder(listItem);
                listItem.setTag(surveyInfoViewHolder);
            } else {
                surveyInfoViewHolder = (SurveyInfoViewHolder) listItem.getTag();
            }

            final ViewSurveyInfo surveyInfo = getItem(position);

            surveyInfoViewHolder.updateViews(surveyInfo, versionTextSize, titleTextSize);

            // Alternate background
            listItem.setBackgroundResource(backgrounds[position % 2 == 0 ? 0 : 1]);
            return listItem;
        }
    }

    @Override
    public Loader<Pair<List<SurveyInfo>, Boolean>> onCreateLoader(int id, Bundle args) {
        return new SurveyInfoLoader(getActivity(), mRecord.getId(), mSurveyGroup);
    }

    @Override
    public void onLoadFinished(Loader<Pair<List<SurveyInfo>, Boolean>> loader,
            Pair<List<SurveyInfo>, Boolean> data) {
        if (loader == null) {
            Timber.e("onLoadFinished() - Loader returned no data");
            return;
        }
        mAdapter.clear();
        boolean registered = data.second;
        List<ViewSurveyInfo> surveys = mapper
                .transform(data.first, mSurveyGroup, registered, getString(R.string.form_deleted));
        for (ViewSurveyInfo s : surveys) {
            mAdapter.add(s);
        }
    }

    @Override
    public void onLoaderReset(Loader<Pair<List<SurveyInfo>, Boolean>> loader) {
        //EMPTY
    }

    public interface SurveyListListener {

        void onSurveyClick(String surveyId);
    }

    public static class SurveyInfoViewHolder {

        private final View view;
        private final TextView surveyNameView;
        private final TextView lastSubmissionTitle;
        private final TextView lastSubmissionView;

        public SurveyInfoViewHolder(View view) {
            this.view = view;
            this.surveyNameView = (TextView) view.findViewById(R.id.survey_name_tv);
            this.lastSubmissionTitle = (TextView) view.findViewById(R.id.date_label);
            this.lastSubmissionView = (TextView) view.findViewById(R.id.date);
        }

        public void updateViews(ViewSurveyInfo surveyInfo, int versionTextSize, int titleTextSize) {
            SpannableString versionSpannable = getSpannableString(versionTextSize,
                    surveyInfo.getSurveyExtraInfo());
            SpannableString titleSpannable = getSpannableString(titleTextSize,
                    surveyInfo.getSurveyName());
            surveyNameView.setText(TextUtils.concat(titleSpannable, versionSpannable));
            view.setEnabled(surveyInfo.isEnabled());
            surveyNameView.setEnabled(surveyInfo.isEnabled());

            if (surveyInfo.getTime() != null) {
                lastSubmissionView.setText(surveyInfo.getTime());
                lastSubmissionTitle.setVisibility(View.VISIBLE);
                lastSubmissionView.setVisibility(View.VISIBLE);
            } else {
                lastSubmissionTitle.setVisibility(View.GONE);
                lastSubmissionView.setVisibility(View.GONE);
            }
        }

        @NonNull
        private SpannableString getSpannableString(int textSize, String string) {
            SpannableString spannable = new SpannableString(string);
            spannable.setSpan(new AbsoluteSizeSpan(textSize), 0, string.length(),
                    SPAN_INCLUSIVE_INCLUSIVE);
            return spannable;
        }
    }
}
