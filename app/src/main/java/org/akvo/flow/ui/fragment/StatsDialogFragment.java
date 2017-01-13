/*
 *  Copyright (C) 2014-2017 Stichting Akvo (Akvo Foundation)
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.data.loader.models.Stats;
import org.akvo.flow.data.loader.StatsLoader;

import timber.log.Timber;

public class StatsDialogFragment extends DialogFragment implements LoaderCallbacks<Stats> {

    public static final String SURVEY_GROUP_ID_EXTRA = "surveyGroupId";

    private long mSurveyGroupId;

    private TextView mTotalView, mWeekView, mDayView;

    public static StatsDialogFragment newInstance(long surveyGroupId) {
        StatsDialogFragment f = new StatsDialogFragment();
        Bundle args = new Bundle();
        args.putLong(SURVEY_GROUP_ID_EXTRA, surveyGroupId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroupId = getArguments().getLong(SURVEY_GROUP_ID_EXTRA);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.stats_fragment, null);
        mTotalView = (TextView)v.findViewById(R.id.total);
        mWeekView = (TextView)v.findViewById(R.id.week);
        mDayView = (TextView)v.findViewById(R.id.day);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.stats);
        builder.setView(v);
        builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<Stats> onCreateLoader(int id, Bundle args) {
        return new StatsLoader(getActivity(), mSurveyGroupId);
    }

    @Override
    public void onLoadFinished(Loader<Stats> loader, Stats stats) {
        if (stats == null) {
            Timber.w("onFinished() - Loader returned no data");
            return;
        }
        mTotalView.setText(String.valueOf(stats.mTotal));
        mWeekView.setText(String.valueOf(stats.mThisWeek));
        mDayView.setText(String.valueOf(stats.mToday));
    }

    @Override
    public void onLoaderReset(Loader<Stats> loader) {
        // EMPTY
    }

}
