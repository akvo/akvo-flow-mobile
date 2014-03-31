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

package org.akvo.flow.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.loader.SurveyGroupLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.ExceptionReportingService;
import org.akvo.flow.service.LocationService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

public class SurveyGroupListActivity extends ActionBarActivity implements LoaderCallbacks<Cursor> {
    private static final String TAG = SurveyGroupListActivity.class.getSimpleName();
    
    // Loader IDs
    private static final int ID_SURVEY_GROUP_LIST = 0;
    
    private SurveyGroupListAdapter mAdapter;
    private SurveyDbAdapter mDatabase;
    
    private ListView mListView;
    private TextView mEmptyView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_group_activity);
        setTitle(R.string.survey_groups_activity);
        
        mDatabase = new SurveyDbAdapter(getApplicationContext());
        mDatabase.open();
        
        mListView = (ListView) findViewById(R.id.list_view);
        mEmptyView = (TextView) findViewById(android.R.id.empty);
        mAdapter = new SurveyGroupListAdapter(this, null);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mAdapter);
        mListView.setEmptyView(mEmptyView);
        
        mEmptyView.setText(R.string.loading);// Be friendly
        init();// No external storage will finish the application
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        getSupportLoaderManager().restartLoader(ID_SURVEY_GROUP_LIST, null, this);
        registerReceiver(mSurveysSyncReceiver,
                new IntentFilter(getString(R.string.action_surveys_sync)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDatabase.close();
        unregisterReceiver(mSurveysSyncReceiver);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    
    private void init() {
        if (!StatusUtil.hasExternalStorage()) {
            ViewUtil.showConfirmDialog(R.string.checksd, R.string.sdmissing, this,
                false, 
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SurveyGroupListActivity.this.finish();
                    }
                }, 
                null);
        } else {
            startService(new Intent(this, SurveyDownloadService.class));
            startService(new Intent(this, LocationService.class));
            startService(new Intent(this, BootstrapService.class));
            startService(new Intent(this, ExceptionReportingService.class));
            startService(new Intent(this, DataSyncService.class));
            checkApkUpdates();
        }
    }

    /**
     * Check if new FLOW versions are available to install.
     * First we check the local storage, to see if the version is already
     * downloaded. If so, we display a dialog to request the user to install it.
     * Otherwise, we trigger the ApkUpdateService to check for updates.
     */
    private void checkApkUpdates() {
        String[] latestVersion = ApkUpdateService.checkDownloadedVersions(this);
        if (latestVersion != null) {
            ApkUpdateService.displayInstallDialog(this, latestVersion[0], latestVersion[1]);
        } else {
            Intent apkUpdateIntent = new Intent(this, ApkUpdateService.class);
            apkUpdateIntent.putExtra(ApkUpdateService.EXTRA_MODE, ApkUpdateService.MODE_CHECK);
            startService(apkUpdateIntent);
        }
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_SURVEY_GROUP_LIST:
                return new SurveyGroupLoader(this, mDatabase);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ID_SURVEY_GROUP_LIST:
                mAdapter.changeCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    // ==================================== //
    // =========== Options Menu =========== //
    // ==================================== //
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.survey_group_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.users:
                Intent i = new Intent(this, ListUserActivity.class);
                startActivity(i);
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    class SurveyGroupListAdapter extends CursorAdapter implements OnItemClickListener {
        
        public SurveyGroupListAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }
        
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.survey_group_list_item, null);
            bindView(view, context, cursor);
            return view;
        }
        
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final SurveyGroup surveyGroup = SurveyDbAdapter.getSurveyGroup(cursor);
            
            TextView text1 = (TextView) view.findViewById(R.id.text1);
            TextView text2 = (TextView) view.findViewById(R.id.text2);
            text1.setText(surveyGroup.getName());
            if (surveyGroup.isMonitored()) {
                text2.setText(R.string.monitored_group);
            } else {
                text2.setText(R.string.regular_group);
            }
            view.setTag(surveyGroup);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // If the group is monitored, we need to trigger record selection Activity.
            // Otherwise, go directly to RecordActivity
            final SurveyGroup group = (SurveyGroup) view.getTag();
            Intent intent;
            if (group.isMonitored()) {
                // Trigger record selection Activity
                intent = new Intent(SurveyGroupListActivity.this, RecordListActivity.class);
            } else {
                // Go directly to Surveys/Responses (No record)
                intent = new Intent(SurveyGroupListActivity.this, RecordActivity.class);
            }
            Bundle extras = new Bundle();
            extras.putSerializable(RecordListActivity.EXTRA_SURVEY_GROUP, group);
            intent.putExtras(extras);
            startActivity(intent);
            // Add group id - Used by the Content Provider
            // TODO: Find a less dirty solution...
            FlowApp.getApp().setSurveyGroupId(group.getId());
        }
        
    }

    /**
     * BroadcastReceiver to notify of surveys synchronisation. This should be
     * fired from SurveyDownloadService.
     */
    private BroadcastReceiver mSurveysSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Surveys have been synchronised. Refreshing data...");
            mEmptyView.setText(R.string.no_surveys_text);
            getSupportLoaderManager().restartLoader(ID_SURVEY_GROUP_LIST, null, SurveyGroupListActivity.this);
        }
    };
    
}
