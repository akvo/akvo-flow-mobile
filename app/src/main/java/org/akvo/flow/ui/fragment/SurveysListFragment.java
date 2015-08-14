/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import org.akvo.flow.R;
import org.akvo.flow.async.loader.SurveyGroupLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.ui.adapter.SurveyListAdapter;

public class SurveysListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
    private static final String TAG = SurveysListFragment.class.getSimpleName();

    public interface SurveyListListener {
        void onSurveyClick(long id);
    }

    private SurveyListListener mListener;
    private SurveyListAdapter mAdapter;
    private SurveyDbAdapter mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    public void onResume() {
        super.onResume();
        refresh();
        getActivity().registerReceiver(surveySyncReceiver,
                new IntentFilter(getString(R.string.action_surveys_sync)));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(surveySyncReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    private void refresh() {
        getLoaderManager().restartLoader(0, null, SurveysListFragment.this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mDatabase == null) {
            mDatabase = new SurveyDbAdapter(getActivity());
            mDatabase.open();
        }

        if (mAdapter == null) {
            mAdapter = new SurveyListAdapter(getActivity());// Cursor Adapter
            setListAdapter(mAdapter);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        SurveyGroup sg = (SurveyGroup) view.getTag();
        if (sg != null) {
            mListener.onSurveyClick(sg.getId());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyGroupLoader(getActivity(), mDatabase);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private BroadcastReceiver surveySyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive()");
            refresh();
        }
    };

}
