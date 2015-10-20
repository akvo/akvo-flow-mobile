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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.List;

public class DrawerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final float ITEM_TEXT_SIZE = 14;

    // Context menu IDs
    private static final int ID_EDIT_USER = 0;
    private static final int ID_DELETE_USER = 1;

    // ExpandableList's group IDs
    private static final int GROUP_USERS = 0;
    private static final int GROUP_SURVEYS = 1;
    private static final int GROUP_SETTINGS = 2;

    // Loader IDs
    private static final int LOADER_SURVEYS = 0;
    private static final int LOADER_USERS = 1;

    public interface DrawerListener {
        void onSurveySelected(SurveyGroup surveyGroup);
        void onUserSelected(User user);
    }

    private ExpandableListView mListView;
    private DrawerAdapter mAdapter;

    private SurveyDbAdapter mDatabase;
    private DrawerListener mListener;

    private List<User> mUsers = new ArrayList<>();
    private List<SurveyGroup> mSurveys = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.navigation_drawer, container, false);
        mListView = (ExpandableListView)v.findViewById(R.id.list);

        return v;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mDatabase == null) {
            mDatabase = new SurveyDbAdapter(getActivity());
            mDatabase.open();
        }
        if (mAdapter == null) {
            mAdapter = new DrawerAdapter();
            mListView.setAdapter(mAdapter);
            mListView.expandGroup(GROUP_SURVEYS);
            mListView.setOnGroupClickListener(mAdapter);
            mListView.setOnChildClickListener(mAdapter);
            registerForContextMenu(mListView);
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
            mListener = (DrawerListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement surveys and users listeners");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
        if (mAdapter != null) {
            mAdapter.notifyDataSetInvalidated();
        }
    }

    public void load() {
        if (!isResumed()) {
            return;
        }
        getLoaderManager().restartLoader(LOADER_SURVEYS, null, this);
        getLoaderManager().restartLoader(LOADER_USERS, null, this);
    }

    public void onDrawerClosed() {
        mListView.collapseGroup(GROUP_USERS);
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
                if (cursor != null) {
                    mSurveys.clear();
                    if (cursor.moveToFirst()) {
                        do {
                            mSurveys.add(SurveyDbAdapter.getSurveyGroup(cursor));
                        } while (cursor.moveToNext());
                        cursor.close();
                    }
                    mAdapter.notifyDataSetInvalidated();
                }
                break;
            case LOADER_USERS:
                if (cursor != null) {
                    mUsers.clear();
                    if (cursor.moveToFirst()) {
                        do {
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyDbAdapter.UserColumns._ID));
                            String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyDbAdapter.UserColumns.NAME));
                            User user = new User(id, name);
                            // Skip selected user
                            if (!user.equals(FlowApp.getApp().getUser())) {
                                mUsers.add(new User(id, name));
                            }
                        } while (cursor.moveToNext());
                        cursor.close();
                    }
                    mAdapter.notifyDataSetInvalidated();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void addUser() {
        editUser(null);
    }

    private void editUser(final User user) {
        final boolean newUser = user == null;
        final EditText et = new EditText(getActivity());
        et.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        et.setSingleLine();
        if (!newUser) {
            et.append(user.getName());
        }

        int titleRes = user != null ? R.string.edit_user : R.string.add_user;

        ViewUtil.ShowTextInputDialog(getActivity(), titleRes, R.string.username, et,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Long uid = newUser ? null : user.getId();
                        String name = et.getText().toString();// TODO: Validate name
                        uid = mDatabase.createOrUpdateUser(uid, name);

                        User loggedUser = FlowApp.getApp().getUser();
                        if (newUser) {
                            // Automatically log in new users
                            mListener.onUserSelected(new User(uid, name));
                        } else if (user.equals(loggedUser)) {
                            loggedUser.setName(name);
                        }
                        load();
                    }
                });
    }

    private void deleteUser(final User user) {
        final long uid = user.getId();
        ViewUtil.showConfirmDialog(R.string.delete_user, R.string.delete_user_confirmation, getActivity(),
                true, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDatabase.deleteUser(uid);
                        if (user.equals(FlowApp.getApp().getUser())) {
                            FlowApp.getApp().setUser(null);
                        }
                        load();
                    }
                });
    }

    private User getUserForContextMenu(ExpandableListContextMenuInfo info) {
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int child = ExpandableListView.getPackedPositionChild(info.packedPosition);

        if (group != GROUP_USERS) {
            // Only user's group can trigger the context menu
            return null;
        }

        switch (type) {
            case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                if (child < mUsers.size()) {
                    return mUsers.get(child);
                }
                break;
            case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                return FlowApp.getApp().getUser();
        }

        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        User user = getUserForContextMenu((ExpandableListContextMenuInfo) menuInfo);

        if (user != null) {
            menu.setHeaderTitle(user.getName());
            menu.add(0, ID_EDIT_USER, ID_EDIT_USER, R.string.edit_user);
            menu.add(0, ID_DELETE_USER, ID_DELETE_USER, R.string.delete_user);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        User user = getUserForContextMenu((ExpandableListContextMenuInfo) item.getMenuInfo());

        if (user != null) {
            switch (item.getItemId()) {
                case ID_EDIT_USER:
                    editUser(user);
                    return true;
                case ID_DELETE_USER:
                    deleteUser(user);
                    return true;
            }
        }

        return super.onContextItemSelected(item);
    }

    class DrawerAdapter extends BaseExpandableListAdapter implements
            ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener {
        LayoutInflater mInflater;

        int mHighlightColor;

        public DrawerAdapter() {
            mInflater = LayoutInflater.from(getActivity());
            mUsers = new ArrayList<>();
            mSurveys = new ArrayList<>();
            mHighlightColor = PlatformUtil.getResource(getActivity(), R.attr.textColorSecondary);
        }

        @Override
        public int getGroupCount() {
            return 3;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            switch (groupPosition) {
                case GROUP_USERS:
                    return mUsers.size() + 1;
                case GROUP_SURVEYS:
                    return mSurveys.size();
                default:
                    return 0;
            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = mInflater.inflate(R.layout.drawer_item, null);
            }
            View divider = v.findViewById(R.id.divider);
            TextView tv = (TextView)v.findViewById(R.id.item_txt);
            ImageView img = (ImageView)v.findViewById(R.id.item_img);
            ImageView dropdown = (ImageView)v.findViewById(R.id.dropdown);


            switch (groupPosition) {
                case GROUP_USERS:
                    divider.setMinimumHeight(0);

                    User u = FlowApp.getApp().getUser();
                    String username = u != null ? u.getName() : getString(R.string.select_user);
                    tv.setTextSize(ITEM_TEXT_SIZE);
                    tv.setTextColor(Color.BLACK);
                    tv.setText(username);

                    img.setImageResource(R.drawable.ic_account_circle_black_48dp);
                    img.setVisibility(View.VISIBLE);
                    dropdown.setImageResource(isExpanded ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand);
                    dropdown.setVisibility(View.VISIBLE);
                    break;
                case GROUP_SURVEYS:
                    divider.setMinimumHeight((int)PlatformUtil.dp2Pixel(getActivity(), 3));
                    tv.setTextSize(ITEM_TEXT_SIZE);
                    tv.setTextColor(getResources().getColor(R.color.black_diabled));
                    tv.setText(R.string.surveys);
                    img.setVisibility(View.GONE);
                    dropdown.setVisibility(View.GONE);
                    break;
                case GROUP_SETTINGS:
                    divider.setMinimumHeight((int)PlatformUtil.dp2Pixel(getActivity(), 1));
                    tv.setTextSize(ITEM_TEXT_SIZE);
                    tv.setTextColor(Color.BLACK);
                    tv.setText(getString(R.string.settingslabel));
                    img.setVisibility(View.GONE);
                    dropdown.setVisibility(View.GONE);
                    break;
            }

            return v;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = mInflater.inflate(android.R.layout.simple_list_item_1, null);
            }
            TextView tv = (TextView)v.findViewById(android.R.id.text1);
            v.setPadding((int) PlatformUtil.dp2Pixel(getActivity(), 30), 0, 0, 0);

            tv.setTextSize(ITEM_TEXT_SIZE);
            tv.setTextColor(Color.BLACK);
            v.setBackgroundColor(Color.TRANSPARENT);

            switch (groupPosition) {
                case GROUP_USERS:
                    User user = isLastChild ? new User(-1, getString(R.string.new_user)) : mUsers.get(childPosition);
                    tv.setText(user.getName());
                    v.setTag(user);
                    break;
                case GROUP_SURVEYS:
                    SurveyGroup sg = mSurveys.get(childPosition);
                    tv.setText(sg.getName());
                    if (sg.getId() == FlowApp.getApp().getSurveyGroupId()) {
                        tv.setTextColor(getResources().getColorStateList(mHighlightColor));
                        v.setBackgroundColor(getResources().getColor(R.color.background_alternate));
                    }
                    v.setTag(sg);
                    break;
            }

            return v;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return groupPosition == GROUP_USERS || groupPosition == GROUP_SURVEYS;
        }

        @Override
        public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
            switch (groupPosition) {
                case GROUP_SURVEYS:
                    return true; // This way the expander cannot be collapsed
                case GROUP_SETTINGS:
                    startActivity(new Intent(getActivity(), SettingsActivity.class));
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            switch (groupPosition) {
                case GROUP_USERS:
                    User user = (User)v.getTag();
                    if (user.getId() == -1) {
                        addUser();
                    } else {
                        mListener.onUserSelected(user);
                    }
                    return true;
                case GROUP_SURVEYS:
                    SurveyGroup sg = (SurveyGroup)v.getTag();
                    mListener.onSurveySelected(sg);
                    return true;
            }
            return false;
        }
    }

}
