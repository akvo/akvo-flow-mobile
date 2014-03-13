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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.activity.SurveyViewActivity;
import com.gallatinsystems.survey.device.activity.TransmissionHistoryActivity;
import com.gallatinsystems.survey.device.async.loader.SurveyInstanceLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter.SurveyInstanceColumns;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter.SurveyColumns;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.util.ConstantUtil;

import java.util.Date;

public class ResponseListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
    //private static final String TAG = ResponseListFragment.class.getSimpleName();
    
    private static final String EXTRA_SURVEY_GROUP = "survey_group";
    private static final String EXTRA_RECORD       = "record";

    private static int SURVEY_ID_KEY          = R.integer.surveyidkey;
    private static int SURVEY_INSTANCE_ID_KEY = R.integer.respidkey;
    private static int USER_ID_KEY            = R.integer.useridkey;
    private static int FINISHED_KEY           = R.integer.finishedkey;
    
    // Loader id
    private static final int ID_SURVEY_INSTANCE_LIST = 0;
    
    // Context menu items
    private static final int DELETE_ONE = 0;// TODO: Should we allow this? - Record might be synced
    private static final int VIEW_HISTORY = 1;

    private static final int UPDATE_INTERVAL_MS = 10000; // every ten seconds
    
    private SurveyGroup mSurveyGroup;
    private SurveyedLocale mRecord;
    private ResponseListCursorAdapter mAdapter;
    
    private SurveyDbAdapter mDatabase;
    
    private Handler mHandler = new Handler();
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            refresh();
            mHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };
    
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
        mHandler.post(mUpdateTimeTask);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateTimeTask);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }
    
    private void refresh() {
        getLoaderManager().restartLoader(ID_SURVEY_INSTANCE_LIST, null, ResponseListFragment.this);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDatabase = new SurveyDbAdapter(getActivity());
        mDatabase.open();

        if(mAdapter == null) {
            mAdapter = new ResponseListCursorAdapter(getActivity());// Cursor Adapter
            setListAdapter(mAdapter);
        }
        registerForContextMenu(getListView());// Same implementation as before
        setHasOptionsMenu(true);
        
        refresh();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(0, VIEW_HISTORY, 0, R.string.transmissionhist);

        // Allow deletion only for 'saved' responses
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        View itemView = info.targetView;
        if (!(Boolean)itemView.getTag(FINISHED_KEY)) {
            menu.add(0, DELETE_ONE, 2, R.string.deleteresponse);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Long surveyInstanceId = mAdapter.getItemId(info.position);// This ID is the _id column in the SQLite db
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

        Intent i = new Intent(view.getContext(), SurveyViewActivity.class);
        i.putExtra(ConstantUtil.USER_ID_KEY, (Long) view.getTag(USER_ID_KEY));
        i.putExtra(ConstantUtil.SURVEY_ID_KEY, ((Long) view.getTag(SURVEY_ID_KEY)).toString());
        i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, (Long) view.getTag(SURVEY_INSTANCE_ID_KEY));
        
        i.putExtra(ConstantUtil.SURVEY_GROUP, mSurveyGroup);
        if (mSurveyGroup.isMonitored()) {
            i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID, mRecord.getId());
        }
        
        // Read-only vs editable
        if ((Boolean)view.getTag(FINISHED_KEY)) {
            i.putExtra(ConstantUtil.READONLY_KEY, true);
        } else {
            i.putExtra(ConstantUtil.SINGLE_SURVEY_KEY, true);
        }
        

        startActivity(i);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_SURVEY_INSTANCE_LIST:
                return new SurveyInstanceLoader(getActivity(), mDatabase, mSurveyGroup.getId(), 
                        mSurveyGroup.isMonitored(), 
                        mRecord != null ? mRecord.getId() : null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ID_SURVEY_INSTANCE_LIST:
                mAdapter.changeCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    class ResponseListCursorAdapter extends CursorAdapter {
        final int SURVEY_ID_KEY = R.integer.surveyidkey;
        final int RESP_ID_KEY = R.integer.respidkey;
        final int USER_ID_KEY = R.integer.useridkey;
        final int FINISHED_KEY = R.integer.finishedkey;

        public ResponseListCursorAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final int syncedDateCol = cursor.getColumnIndexOrThrow(SurveyInstanceColumns.SYNC_DATE);
            final int exportedDateCol = cursor.getColumnIndexOrThrow(SurveyInstanceColumns.EXPORTED_DATE);
            final int submittedDateCol = cursor.getColumnIndexOrThrow(SurveyInstanceColumns.SUBMITTED_DATE);
            final int savedDateCol = cursor.getColumnIndexOrThrow(SurveyInstanceColumns.SAVED_DATE);

            String status = "";
            int icon = R.drawable.redcircle;
            boolean finished = false;
            long displayDate = 0L;// Should never display this value;

            // TODO: The STATUS can be determined with these values. No need for STATUS column?
            if (!cursor.isNull(syncedDateCol)) {
                status = "Synced: ";
                displayDate = cursor.getLong(syncedDateCol);
                icon = R.drawable.checkmark2;
                finished = true;
            } else if (!cursor.isNull(exportedDateCol)) {
                status = "Exported: ";
                displayDate = cursor.getLong(exportedDateCol);
                icon = R.drawable.yellowcircle;
                finished = true;
            } else if (!cursor.isNull(submittedDateCol)) {
                status = "Submitted: ";
                displayDate = cursor.getLong(submittedDateCol);
                icon = R.drawable.yellowcircle;
                finished = true;
            } else if (!cursor.isNull(savedDateCol)) {
                status = "Saved: ";
                icon = R.drawable.disk;
                displayDate = cursor.getLong(savedDateCol);
            }

            // Format the date string
            Date date = new Date(displayDate);
            TextView dateView = (TextView) view.findViewById(R.id.text2);
            dateView.setText(status
                    + DateFormat.getLongDateFormat(context).format(date) + " "
                    + DateFormat.getTimeFormat(context).format(date));
            TextView headingView = (TextView) view.findViewById(R.id.text1);
            headingView.setText(cursor.getString(cursor.getColumnIndex(SurveyColumns.NAME)));
            view.setTag(SURVEY_ID_KEY, cursor.getLong(cursor
                    .getColumnIndex(SurveyInstanceColumns.SURVEY_ID)));
            view.setTag(RESP_ID_KEY, cursor.getLong(cursor
                    .getColumnIndex(SurveyInstanceColumns._ID)));
            view.setTag(USER_ID_KEY, cursor.getLong(cursor
                    .getColumnIndex(SurveyInstanceColumns.USER_ID)));
            view.setTag(FINISHED_KEY, finished);
            ImageView stsIcon = (ImageView) view.findViewById(R.id.xmitstsicon);
            stsIcon.setImageResource(icon);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.submittedrow, null);
            bindView(view, context, cursor);
            return view;
        }

    }
}
