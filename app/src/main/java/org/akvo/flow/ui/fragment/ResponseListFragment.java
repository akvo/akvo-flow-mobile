/*
 *  Copyright (C) 2013-2014 Stichting Akvo (Akvo Foundation)
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.TransmissionHistoryActivity;
import org.akvo.flow.async.loader.SurveyInstanceLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.ui.adapter.ResponseListAdapter;
import org.akvo.flow.util.ConstantUtil;

public class ResponseListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
    private static final String TAG = ResponseListFragment.class.getSimpleName();

    private static final String EXTRA_SURVEY_GROUP = "survey_group";
    private static final String EXTRA_RECORD = "record";

    // TODO: Move all id constants to ConstantUtil
    private static int SURVEY_ID_KEY = R.integer.surveyidkey;
    private static int SURVEY_INSTANCE_ID_KEY = R.integer.respidkey;
    private static int FINISHED_KEY = R.integer.finishedkey;

    // Context menu items
    private static final int DELETE_ONE = 0;
    private static final int VIEW_HISTORY = 1;

    private SurveyGroup mSurveyGroup;
    private SurveyedLocale mRecord;
    private ResponseListAdapter mAdapter;

    private SurveyDbAdapter mDatabase;

    public static ResponseListFragment instantiate(SurveyGroup surveyGroup, SurveyedLocale record) {
        ResponseListFragment fragment = new ResponseListFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_SURVEY_GROUP, surveyGroup);
        args.putSerializable(EXTRA_RECORD, record);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroup = (SurveyGroup) getArguments().getSerializable(EXTRA_SURVEY_GROUP);
        mRecord = (SurveyedLocale) getArguments().getSerializable(EXTRA_RECORD);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(dataSyncReceiver,
                new IntentFilter(ConstantUtil.ACTION_DATA_SYNC));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(dataSyncReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
        mDatabase = null;
    }

    private void refresh() {
        getLoaderManager().restartLoader(0, null, ResponseListFragment.this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mDatabase == null) {
            mDatabase = new SurveyDbAdapter(getActivity());
            mDatabase.open();
        }

        if (mAdapter == null) {
            mAdapter = new ResponseListAdapter(getActivity());// Cursor Adapter
            setListAdapter(mAdapter);
        }
        registerForContextMenu(getListView());// Same implementation as before
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(0, VIEW_HISTORY, 0, R.string.transmissionhist);

        // Allow deletion only for 'saved' responses
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        View itemView = info.targetView;
        if (!(Boolean) itemView.getTag(FINISHED_KEY)) {
            menu.add(0, DELETE_ONE, 2, R.string.deleteresponse);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Long surveyInstanceId = mAdapter
                .getItemId(info.position);// This ID is the _id column in the SQLite db
        switch (item.getItemId()) {
            case DELETE_ONE:
                deleteSurveyInstance(surveyInstanceId);
                break;
            case VIEW_HISTORY:
                viewSurveyInstanceHistory(surveyInstanceId);
                break;
        }
        return true;
    }

    private void deleteSurveyInstance(final long surveyInstanceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.deleteonewarning)
                .setCancelable(true)
                .setPositiveButton(R.string.okbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                SurveyDbAdapter db = new SurveyDbAdapter(getActivity()).open();
                                db.deleteSurveyInstance(String.valueOf(surveyInstanceId));
                                db.close();
                                refresh();
                            }
                        })
                .setNegativeButton(R.string.cancelbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                dialog.cancel();
                            }
                        });
        builder.show();
    }

    private void viewSurveyInstanceHistory(long surveyInstanceId) {
        Intent i = new Intent(getActivity(), TransmissionHistoryActivity.class);
        i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, surveyInstanceId);
        startActivity(i);
    }

    /**
     * when a list item is clicked, get the user id and name of the selected
     * item and open one-survey activity, readonly.
     */
    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);

        Intent i = new Intent(view.getContext(), FormActivity.class);
        i.putExtra(ConstantUtil.SURVEY_ID_KEY, view.getTag(SURVEY_ID_KEY).toString());
        i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, (Long) view.getTag(SURVEY_INSTANCE_ID_KEY));

        i.putExtra(ConstantUtil.SURVEY_GROUP, mSurveyGroup);
        i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID, mRecord.getId());

        // Read-only vs editable
        if ((Boolean) view.getTag(FINISHED_KEY)) {
            i.putExtra(ConstantUtil.READONLY_KEY, true);
        } else {
            i.putExtra(ConstantUtil.SINGLE_SURVEY_KEY, true);
        }

        startActivity(i);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyInstanceLoader(getActivity(), mDatabase, mRecord.getId());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * BroadcastReceiver to notify of data synchronisation. This should be
     * fired from DataSyncService.
     */
    private BroadcastReceiver dataSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Survey Instance status has changed. Refreshing UI...");
            refresh();
        }
    };

}
