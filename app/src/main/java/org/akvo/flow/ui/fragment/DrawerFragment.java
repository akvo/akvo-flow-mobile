package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.activity.SettingsActivity;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.loader.SurveyGroupLoader;
import org.akvo.flow.async.loader.UserLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.PlatformUtil;

public class DrawerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface SurveyListener {
        void onSurveySelected(SurveyGroup surveyGroup);
    }

    public interface UserListener {
        void onNewUser();
        void onUserSelected(User user);
    }

    private static final int LOADER_SURVEYS = 0;
    private static final int LOADER_USERS = 1;

    private ListView mUserList;
    private ListView mSurveyList;
    private TextView mUsernameView;
    private TextView mListHeader;

    private SurveyListAdapter mSurveyAdapter;
    private UsersAdapter mUsersAdapter;
    private UserToggleListener mUsersToggle;

    private SurveyDbAdapter mDatabase;
    private SurveyListener mSurveysListener;
    private UserListener mUsersListener;

    private enum Mode { SURVEYS, USERS }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.navigation_drawer, container, false);

        mUsernameView = (TextView) v.findViewById(R.id.username);
        mListHeader = (TextView) v.findViewById(R.id.list_header);
        mUserList = (ListView) v.findViewById(R.id.user_list);
        mSurveyList = (ListView) v.findViewById(R.id.survey_group_list);

        // Add list footers
        TextView addUserView = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);
        addUserView.setText("Add user");
        addUserView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUsersListener.onNewUser();
            }
        });
        mUserList.addFooterView(addUserView);

        View settingsView = inflater.inflate(R.layout.navigation_drawer_footer, null);
        settingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
            }
        });
        mSurveyList.addFooterView(settingsView);

        mUsersToggle = new UserToggleListener();
        mUsersToggle.setMenuListMode(Mode.SURVEYS);
        mUsernameView.setOnClickListener(mUsersToggle);

        return v;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mDatabase == null) {
            mDatabase = new SurveyDbAdapter(getActivity());
            mDatabase.open();
        }
        if (mUsersAdapter == null) {
            mUsersAdapter = new UsersAdapter(getActivity());
            mUserList.setAdapter(mUsersAdapter);
            mUserList.setOnItemClickListener(mUsersAdapter);
        }
        if (mSurveyAdapter == null) {
            mSurveyAdapter = new SurveyListAdapter(getActivity());
            mSurveyList.setAdapter(mSurveyAdapter);
            mSurveyList.setOnItemClickListener(mSurveyAdapter);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSurveysListener = (SurveyActivity)activity;
            mUsersListener = (SurveyActivity)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement surveys and users listeners");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
        updateUser();
    }

    public void load() {
        getLoaderManager().restartLoader(LOADER_SURVEYS, null, this);
        getLoaderManager().restartLoader(LOADER_USERS, null, this);
    }

    public void updateUser() {
        User user = FlowApp.getApp().getUser();
        mUsernameView.setText(user != null ? user.getName() : null);
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
                    mUserList.setVisibility(View.GONE);
                    mSurveyList.setVisibility(View.VISIBLE);

                    mListHeader.setText("Surveys");
                    mUsernameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_expand, 0);
                    break;
                case USERS:
                    mUserList.setVisibility(View.VISIBLE);
                    mSurveyList.setVisibility(View.GONE);

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
                return new SurveyGroupLoader(getActivity(), mDatabase);
            case LOADER_USERS:
                return new UserLoader(getActivity(), mDatabase);
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
