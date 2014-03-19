/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.UserColumns;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.ConstantUtil;

/**
 * This activity will list all the users in the database and present them in
 * list form. From the list they can be either edited or selected for use as the
 * "current user". New users can also be added to the system using this activity
 * by activating the menu.
 * 
 * @author Christopher Fagiani
 */
public class ListUserActivity extends ActionBarActivity {
    private static final int EDIT_ID = 0;
    private static final int DELETE_ID = 1;
    
    private SurveyDbAdapter mDatabase;
    private UserListAdapter mAdapter;
    private ListView mListView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userlist_activity);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        mDatabase = new SurveyDbAdapter(getApplicationContext());
        
        mListView = (ListView) findViewById(android.R.id.list);
        mAdapter = new UserListAdapter(this, null);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mAdapter);
        mListView.setEmptyView(findViewById(android.R.id.empty));
        registerForContextMenu(mListView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        display();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDatabase.close();
    }
    
    private void display() {
        Cursor cursor = mDatabase.getUsers();
        mAdapter.changeCursor(cursor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.userlist_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.add_user:
                handleCreate(null);
                return true;
        }
        
        return onOptionsItemSelected(item);
    }

    /**
     * presents an edit and "delete" option when the user long-clicks a list
     * item
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // the parent method adds the "edit" button
        menu.add(0, EDIT_ID, EDIT_ID, R.string.editmenu);
        menu.add(0, DELETE_ID, DELETE_ID, R.string.deleteusermenu);
    }

    /**
     * spawns an activity (configured in initializeFields) in Edit mode
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        switch (item.getItemId()) {
            case EDIT_ID:
                handleCreate(info.id);
                return true;
            case DELETE_ID:
                handleDelete(info.id);
                return true;
        }
        return super.onContextItemSelected(item);
    }
    
    private void handleCreate(Long id) {
        Intent i = new Intent(this, UserEditActivity.class);
        if (id != null) {
            i.putExtra(ConstantUtil.ID_KEY, id);
        }
        startActivityForResult(i, 0);
    }

    private void handleDelete(long id) {
        String savedId = mDatabase.getPreference(ConstantUtil.LAST_USER_SETTING_KEY);
        mDatabase.deleteUser(id);
        if (savedId != null && savedId.equals(id)) {
            mDatabase.savePreference(ConstantUtil.LAST_USER_SETTING_KEY, "");
        }

        display();
    }
    
    class UserListAdapter extends CursorAdapter implements OnItemClickListener {
        
        public UserListAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }
        
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.itemlistrow, null);
            bindView(view, context, cursor);
            return view;
        }
        
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(UserColumns._ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(UserColumns.NAME));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(UserColumns.EMAIL));
            
            final User user = new User(id, name, email);
            view.setTag(user);
            
            TextView nameView = (TextView) view.findViewById(R.id.itemheader);
            nameView.setText(name);
            
            int leftDrawable = 0;
            if (FlowApp.getApp().getUser() != null) {
                if (FlowApp.getApp().getUser().getId() == id) {
                    leftDrawable = R.drawable.ic_menu_allfriends;
                }
            }
            nameView.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, 0, 0, 0);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Set the user in the App, and finish the Activity
            User user = (User) view.getTag();
            FlowApp.getApp().setUser(user);
            display();
            Toast.makeText(ListUserActivity.this, "Logged in as " + user.getName(), 
                    Toast.LENGTH_LONG).show();
            finish();
        }
        
    }
    
}
