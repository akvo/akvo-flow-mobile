package org.akvo.flow.ui.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.activity.SettingsActivity;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.loader.SurveyGroupLoader;
import org.akvo.flow.async.loader.UserLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.ViewUtil;

public class NavigationDrawer extends FrameLayout implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface OnSurveySelectedListener {
        void onSurveySelected(SurveyGroup surveyGroup);
    }

    public interface OnUserSelectedListener {
        void onUserSelected(User user);
    }

    private static final int LOADER_SURVEYS = 0;
    private static final int LOADER_USERS = 1;

    private ListView mUserList;
    private ListView mSurveyList;
    private TextView mUsernameView;
    private TextView mListHeader;

    private LoaderManager mLoaderManager;

    private SurveyListAdapter mSurveyAdapter;
    private UsersAdapter mUsersAdapter;
    private UserToggleListener mUsersToggle;

    private SurveyDbAdapter mDatabase;
    private OnSurveySelectedListener mSurveysListener;
    private OnUserSelectedListener mUsersListener;

    private enum Mode { SURVEYS, USERS }

    public NavigationDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.navigation_drawer, this);

        mUsernameView = (TextView) findViewById(R.id.username);
        mListHeader = (TextView) findViewById(R.id.list_header);
        mUserList = (ListView) findViewById(R.id.user_list);
        mSurveyList = (ListView) findViewById(R.id.survey_group_list);

        mUsersAdapter = new UsersAdapter(context);
        mUserList.setAdapter(mUsersAdapter);
        mUserList.setOnItemClickListener(mUsersAdapter);

        mSurveyAdapter = new SurveyListAdapter(context);
        mSurveyList.setAdapter(mSurveyAdapter);
        mSurveyList.setOnItemClickListener(mSurveyAdapter);

        // Add list footers
        LayoutInflater inflater = LayoutInflater.from(context);
        TextView addUserView = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);
        addUserView.setText("Add user");
        addUserView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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

                                mUsersAdapter.onUserSelected(u);
                                mLoaderManager.restartLoader(LOADER_USERS, null, NavigationDrawer.this);
                            }
                        });
            }
        });
        mUserList.addFooterView(addUserView);

        View settingsView = inflater.inflate(R.layout.navigation_drawer_footer, null);
        settingsView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(getContext(), SettingsActivity.class));
            }
        });
        mSurveyList.addFooterView(settingsView);

        mUsersToggle = new UserToggleListener();
        mUsersToggle.setMenuListMode(Mode.SURVEYS);
        mUsernameView.setOnClickListener(mUsersToggle);

        User u = FlowApp.getApp().getUser();
        if (u != null) {
            mUsernameView.setText(u.getName());
        }
    }

    public NavigationDrawer(Context context) {
        this(context, null);
    }

    public void init(LoaderManager loaderManager, SurveyDbAdapter db, OnSurveySelectedListener
            surveySelectedListener, OnUserSelectedListener userSelectedListener) {
        mLoaderManager = loaderManager;
        mDatabase = db;
        mSurveysListener = surveySelectedListener;
        mUsersListener = userSelectedListener;
    }

    public void load() {
        mLoaderManager.restartLoader(LOADER_SURVEYS, null, this);
        mLoaderManager.restartLoader(LOADER_USERS, null, this);
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
                    mUserList.setVisibility(GONE);
                    mSurveyList.setVisibility(VISIBLE);

                    mListHeader.setText("Surveys");
                    mUsernameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_expand, 0);
                    break;
                case USERS:
                    mUserList.setVisibility(VISIBLE);
                    mSurveyList.setVisibility(GONE);

                    mListHeader.setText("Users");
                    mUsernameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_collapse, 0);
                    break;
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_SURVEYS:
                return new SurveyGroupLoader(getContext(), mDatabase);
            case LOADER_USERS:
                return new UserLoader(getContext(), mDatabase);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_SURVEYS:
                mSurveyAdapter.swapCursor(cursor);
                break;
            case LOADER_USERS:
                mUsersAdapter.swapCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_SURVEYS:
                mSurveyAdapter.swapCursor(null);
                break;
            case LOADER_USERS:
                mUsersAdapter.swapCursor(null);
                break;
        }
    }

    // TODO: Use external adapter
    class SurveyListAdapter extends CursorAdapter implements AdapterView.OnItemClickListener {
        final int mTextColor;

        public SurveyListAdapter(Context context) {
            super(context, null, 0);
            mTextColor = PlatformUtil.getResource(context, R.attr.textColorSecondary);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.survey_group_list_item, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final SurveyGroup surveyGroup = SurveyDbAdapter.getSurveyGroup(cursor);

            TextView text1 = (TextView)view.findViewById(R.id.text1);
            text1.setText(surveyGroup.getName());
            text1.setTextColor(getResources().getColorStateList(mTextColor));

            view.setTag(surveyGroup);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final SurveyGroup survey = (SurveyGroup) view.getTag();
            mSurveysListener.onSurveySelected(survey);
        }

    }

    class UsersAdapter extends CursorAdapter implements AdapterView.OnItemClickListener {
        final int regularColor, selectedColor;

        public UsersAdapter(Context context) {
            super(context, null, 0);
            regularColor = PlatformUtil.getResource(context, R.attr.textColorPrimary);
            selectedColor = PlatformUtil.getResource(context, R.attr.textColorSecondary);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(android.R.layout.simple_list_item_1, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyDbAdapter.UserColumns._ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyDbAdapter.UserColumns.NAME));
            User user = new User(id, name, null);// TODO: Do we need email?

            TextView text1 = (TextView)view.findViewById(android.R.id.text1);
            text1.setText(name);

            final User loggedUser = FlowApp.getApp().getUser();
            boolean selected = loggedUser != null && loggedUser.getId() == id;
            //text1.setSelected(selected);

            view.setTag(user);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final User user = (User) view.getTag();
            mUserList.setItemChecked(position, true);
            mUserList.setSelection(position);
            onUserSelected(user);
        }

        void onUserSelected(User user) {
            mUsernameView.setText(user.getName());
            mUsersToggle.setMenuListMode(Mode.SURVEYS);
            if (mUsersListener != null) {
                mUsersListener.onUserSelected(user);
            }
        }

    }

}
