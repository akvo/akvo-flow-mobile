/*
 *  Copyright (C) 2013-2017,2019 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import org.akvo.flow.data.loader.FormInfoLoader;
import org.akvo.flow.data.loader.models.FormInfo;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.ui.model.ViewForm;
import org.akvo.flow.ui.model.ViewFormMapper;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;
import static org.akvo.flow.util.ConstantUtil.DATA_POINT_ID_EXTRA;

public class FormListFragment extends ListFragment
        implements LoaderCallbacks<List<FormInfo>>, OnItemClickListener {

    private SurveyGroup mSurveyGroup;
    private SurveyAdapter mAdapter;
    private FormListListener mListener;
    private final ViewFormMapper mapper = new ViewFormMapper();
    private String recordId;

    public FormListFragment() {
    }

    public static FormListFragment newInstance() {
        return new FormListFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (FormListListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement FormListListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mSurveyGroup = (SurveyGroup) intent.getSerializableExtra(ConstantUtil.SURVEY_EXTRA);
        recordId = intent.getStringExtra(DATA_POINT_ID_EXTRA);
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
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String surveyId = mAdapter.getItem(position).getId();
        mListener.onFormClick(surveyId);
    }

    static class SurveyAdapter extends ArrayAdapter<ViewForm> {

        private static final int LAYOUT_RES = R.layout.survey_item;

        private final int[] backgrounds;
        private final int versionTextSize;
        private final int titleTextSize;

        SurveyAdapter(Context context) {
            super(context, LAYOUT_RES, new ArrayList<>());
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
            ViewForm viewForm = getItem(position);
            return viewForm.isEnabled();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            FormViewHolder formViewHolder;
            if (listItem == null) {
                listItem = LayoutInflater.from(getContext()).inflate(LAYOUT_RES, null);
                formViewHolder = new FormViewHolder(listItem);
                listItem.setTag(formViewHolder);
            } else {
                formViewHolder = (FormViewHolder) listItem.getTag();
            }

            final ViewForm viewForm = getItem(position);

            formViewHolder.updateViews(viewForm, versionTextSize, titleTextSize);

            // Alternate background
            listItem.setBackgroundResource(backgrounds[position % 2 == 0 ? 0 : 1]);
            return listItem;
        }

        public void addAll(List<ViewForm> forms) {
            for (ViewForm s : forms) {
                add(s);
            }
        }
    }

    @NonNull
    @Override
    public Loader<List<FormInfo>> onCreateLoader(int id, Bundle args) {
        return new FormInfoLoader(getActivity(), recordId, mSurveyGroup);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<FormInfo>> loader,
            List<FormInfo> data) {
        mAdapter.clear();
        List<ViewForm> forms = mapper
                .transform(data, mSurveyGroup, getString(R.string.form_deleted));
        mAdapter.addAll(forms);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<FormInfo>> loader) {
        //EMPTY
    }


    public interface FormListListener {

        void onFormClick(String surveyId);
    }

    static class FormViewHolder {

        private final View view;
        private final TextView formNameView;
        private final TextView lastSubmissionTitle;
        private final TextView lastSubmissionView;

        FormViewHolder(View view) {
            this.view = view;
            this.formNameView = view.findViewById(R.id.survey_name_tv);
            this.lastSubmissionTitle = view.findViewById(R.id.date_label);
            this.lastSubmissionView = view.findViewById(R.id.date);
        }

        void updateViews(ViewForm surveyInfo, int versionTextSize, int titleTextSize) {
            SpannableString versionSpannable = getSpannableString(versionTextSize,
                    surveyInfo.getSurveyExtraInfo());
            SpannableString titleSpannable = getSpannableString(titleTextSize,
                    surveyInfo.getSurveyName());
            formNameView.setText(TextUtils.concat(titleSpannable, versionSpannable));
            view.setEnabled(surveyInfo.isEnabled());
            formNameView.setEnabled(surveyInfo.isEnabled());

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
