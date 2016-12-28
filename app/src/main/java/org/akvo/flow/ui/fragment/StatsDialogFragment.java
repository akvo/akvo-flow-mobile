/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.async.loader.StatsLoader;
import org.akvo.flow.data.database.SurveyDbAdapter;

public class StatsDialogFragment extends DialogFragment implements LoaderCallbacks<StatsLoader.Stats> {
    private static final String TAG = StatsDialogFragment.class.getSimpleName();

    private long mSurveyGroupId;
    private SurveyDbAdapter mDatabase;

    private TextView mTotalView, mWeekView, mDayView;

    public static StatsDialogFragment newInstance(long surveyGroupId) {
        StatsDialogFragment f = new StatsDialogFragment();
        Bundle args = new Bundle();
        args.putLong("surveyGroupId", surveyGroupId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroupId = getArguments().getLong("surveyGroupId");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDatabase = new SurveyDbAdapter(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDatabase.close();
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
    public Loader<StatsLoader.Stats> onCreateLoader(int id, Bundle args) {
        return new StatsLoader(getActivity(), mDatabase, mSurveyGroupId);
    }

    @Override
    public void onLoadFinished(Loader<StatsLoader.Stats> loader, StatsLoader.Stats stats) {
        if (stats == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }
        mTotalView.setText(String.valueOf(stats.mTotal));
        mWeekView.setText(String.valueOf(stats.mThisWeek));
        mDayView.setText(String.valueOf(stats.mToday));
    }

    @Override
    public void onLoaderReset(Loader<StatsLoader.Stats> loader) {
    }

}
