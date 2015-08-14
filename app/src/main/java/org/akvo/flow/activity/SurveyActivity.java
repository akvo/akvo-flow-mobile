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

package org.akvo.flow.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.loader.SurveyGroupLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.User;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.ExceptionReportingService;
import org.akvo.flow.service.LocationService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.service.TimeCheckService;
import org.akvo.flow.ui.fragment.DatapointsFragment;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

public class SurveyActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        RecordListListener {
    private static final String TAG = SurveyActivity.class.getSimpleName();
    
    // Argument to be passed to list/map fragments
    public static final String EXTRA_SURVEY_GROUP = "survey_group";
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";

    public static final String FRAGMENT_DATAPOINTS = "datapoints_fragment";

    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;

    private SurveyListAdapter mSurveyAdapter;
    private UsersAdapter mUsersAdapter;
    private UserToggleListener mUsersToggle;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private View mDrawer;
    private ListView mDrawerList;
    private TextView mUsernameView;
    private TextView mListHeader;
    private CharSequence mDrawerTitle, mTitle;

    private enum Mode { SURVEYS, USERS }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        mUsernameView = (TextView) findViewById(R.id.username);
        mListHeader = (TextView) findViewById(R.id.list_header);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer = findViewById(R.id.left_drawer);
        mDrawerList = (ListView) findViewById(R.id.survey_group_list);

        mTitle = mDrawerTitle = getTitle();

        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        mSurveyAdapter = new SurveyListAdapter(this);
        mUsersAdapter = new UsersAdapter(this);

        // Init navigation drawer
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);

        findViewById(R.id.users).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SurveyActivity.this, ListUserActivity.class));
            }
        });

        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SurveyActivity.this, SettingsActivity.class));
            }
        });

        mUsersToggle = new UserToggleListener();
        mUsersToggle.setMenuListMode(Mode.SURVEYS);
        mUsernameView.setOnClickListener(mUsersToggle);

        init();

        // Automatically select the survey and user
        SurveyGroup sg = mDatabase.getSurveyGroup(FlowApp.getApp().getSurveyGroupId());
        if (sg != null) {
            onSurveyGroupSelected(sg);
        }
        User u = FlowApp.getApp().getUser();
        if (u != null) {
            mUsernameView.setText(u.getName());
        }

        showDatapointsFragment();
    }

    // TODO: Add login, survey, datapoints mode selection
    private void showDatapointsFragment() {
        Fragment f = DatapointsFragment.instantiate(mSurveyGroup);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, f, FRAGMENT_DATAPOINTS).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDrawer();
        registerReceiver(mSurveysSyncReceiver,
                new IntentFilter(getString(R.string.action_surveys_sync)));
    }
    
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mSurveysSyncReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    private void init() {
        if (!StatusUtil.hasExternalStorage()) {
            ViewUtil.showConfirmDialog(R.string.checksd, R.string.sdmissing, this,
                    false,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SurveyActivity.this.finish();
                        }
                    },
                    null);
        } else {
            startService(new Intent(this, SurveyDownloadService.class));
            startService(new Intent(this, LocationService.class));
            startService(new Intent(this, BootstrapService.class));
            startService(new Intent(this, ExceptionReportingService.class));
            startService(new Intent(this, DataSyncService.class));
            startService(new Intent(this, ApkUpdateService.class));
            startService(new Intent(this, TimeCheckService.class));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String surveyedLocaleId = intent.getDataString();
            onRecordSelected(surveyedLocaleId);
        }
    }

    private void loadDrawer() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    private void onSurveyGroupSelected(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        setTitle(mSurveyGroup.getName());

        // Add group id - Used by the Content Provider. TODO: Find a less dirty solution...
        FlowApp.getApp().setSurveyGroupId(mSurveyGroup.getId());

        DatapointsFragment f = (DatapointsFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_DATAPOINTS);
        if (f != null) {
            f.refresh(mSurveyGroup);
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If the nav drawer is open, don't inflate the menu items.
        if (!mDrawerLayout.isDrawerOpen(mDrawer) && mSurveyGroup != null) {
            //getMenuInflater().inflate(R.menu.survey_activity, menu);
            return super.onCreateOptionsMenu(menu);
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRecordSelected(String surveyedLocaleId) {
        // Start SurveysActivity, sending SurveyGroup + Record
        SurveyedLocale record = mDatabase.getSurveyedLocale(surveyedLocaleId);
        Intent intent = new Intent(this, RecordActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable(RecordActivity.EXTRA_SURVEY_GROUP, mSurveyGroup);
        extras.putString(RecordActivity.EXTRA_RECORD_ID, record.getId());
        intent.putExtras(extras);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyGroupLoader(this, mDatabase);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mSurveyAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    class UserToggleListener implements View.OnClickListener {
        private Mode mListMode = Mode.SURVEYS;

        @Override
        public void onClick(View v) {
            switch (mListMode) {
                case SURVEYS:
                    setMenuListMode(Mode.USERS);
                    break;
                case USERS:
                    setMenuListMode(Mode.SURVEYS);
                    break;
            }
        }

        public void setMenuListMode(Mode mode) {
            mListMode = mode;
            switch (mListMode) {
                case SURVEYS:
                    mListHeader.setText("Surveys");
                    mUsernameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_expand, 0);
                    mDrawerList.setAdapter(mSurveyAdapter);
                    mDrawerList.setOnItemClickListener(mSurveyAdapter);
                    break;
                case USERS:
                    mListHeader.setText("Users");
                    mUsernameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_collapse, 0);
                    mDrawerList.setAdapter(mUsersAdapter);
                    mDrawerList.setOnItemClickListener(mUsersAdapter);
                    break;
            }
        }
    }

    class SurveyListAdapter extends CursorAdapter implements OnItemClickListener {
        final int mTextColor;

        public SurveyListAdapter(Context context) {
            super(context, null, 0);
            mTextColor = PlatformUtil.getResource(context, R.attr.textColorSecondary);
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

            TextView text1 = (TextView)view.findViewById(R.id.text1);
            text1.setText(surveyGroup.getName());
            text1.setTextColor(getResources().getColorStateList(mTextColor));

            // Alternate background
            /*
            int attr = cursor.getPosition() % 2 == 0 ? R.attr.listitem_bg1 : R.attr.listitem_bg2;
            final int res= PlatformUtil.getResource(context, attr);
            view.setBackgroundResource(res);
            */

            view.setTag(surveyGroup);
        }

        // TODO
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Ensure user is logged in
            if (FlowApp.getApp().getUser() == null) {
                Toast.makeText(SurveyActivity.this, R.string.mustselectuser,
                        Toast.LENGTH_LONG).show();
                return;
            }

            mDrawerList.setItemChecked(position, true);

            final SurveyGroup survey = (SurveyGroup) view.getTag();
            onSurveyGroupSelected(survey);
            mDrawerLayout.closeDrawers();
        }

    }

    class UsersAdapter extends ArrayAdapter<User> implements OnItemClickListener {
        final int regularColor, selectedColor;

        public UsersAdapter(Context context) {
            super(context, android.R.layout.simple_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            regularColor = PlatformUtil.getResource(context, R.attr.textColorPrimary);
            selectedColor = PlatformUtil.getResource(context, R.attr.textColorSecondary);

            Cursor c = mDatabase.getUsers();
            if (c != null && c.moveToFirst()) {
                do {
                    long id = c.getLong(c.getColumnIndexOrThrow(SurveyDbAdapter.UserColumns._ID));
                    String name = c.getString(c.getColumnIndexOrThrow(SurveyDbAdapter.UserColumns.NAME));
                    add(new User(id, name, null));// TODO: Do we need email?
                } while (c.moveToNext());
            }
        }

        @Override
        public int getCount() {
            return super.getCount() + 1;
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            // TODO: Use ViewHolder pattern
            View view = getLayoutInflater().inflate(R.layout.itemlistrow, null);
            TextView tv = (TextView) view.findViewById(R.id.itemheader);
            if (position == getCount() - 1) {
                // New user click
                tv.setText("Add user");
            } else {
                User u = getItem(position);
                view.setTag(u);
                tv.setText(u.getName());

                int colorRes = regularColor;
                final User loggedUser = FlowApp.getApp().getUser();
                if (loggedUser != null && loggedUser.getId() == u.getId()) {
                    colorRes = selectedColor;
                }
                tv.setTextColor(getResources().getColorStateList(colorRes));
            }

            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position == getCount() - 1) {
                onNewUserClick();
            } else {
                onUserSelected(getItem(position));
            }
            mUsersToggle.setMenuListMode(Mode.SURVEYS);
            mDrawerLayout.closeDrawers();
        }

        void onUserSelected(User user) {
            mUsernameView.setText(user.getName());
            FlowApp.getApp().setUser(user);
            mDatabase.savePreference(ConstantUtil.LAST_USER_SETTING_KEY,
                    String.valueOf(user.getId()));// Save the last id for future sessions
            notifyDataSetInvalidated();
        }

        void onNewUserClick() {
            final EditText et = new EditText(getContext());
            et.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            ViewUtil.ShowTextInputDialog(getContext(), R.string.adduser, R.string.userlabel,
                    et, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String username = et.getText().toString();
                            long id = mDatabase.createOrUpdateUser(null, username, null);
                            User u = new User(id, username, null);

                            add(u);
                            onUserSelected(u);
                        }
                    });
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
            loadDrawer();
        }
    };

}
