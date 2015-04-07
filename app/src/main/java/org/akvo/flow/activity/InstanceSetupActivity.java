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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.util.PlatformUtil;

/**
 * This activity will list all the users in the database and present them in
 * list form. From the list they can be either edited or selected for use as the
 * "current user". New users can also be added to the system using this activity
 * by activating the menu.
 *
 * @author Christopher Fagiani
 */
public class InstanceSetupActivity extends ActionBarActivity {

    private SurveyDbAdapter mDatabase;
    private InstanceListAdapter mAdapter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instance_setup_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDatabase = new SurveyDbAdapter(getApplicationContext());

        mListView = (ListView) findViewById(android.R.id.list);
        mAdapter = new InstanceListAdapter(this, null);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mAdapter);
        mListView.setEmptyView(findViewById(android.R.id.empty));
        //registerForContextMenu(mListView);
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
        Cursor cursor = mDatabase.getInstances();
        mAdapter.changeCursor(cursor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.instance_setup_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.add_instance:
                addInstance();
                return true;
        }

        return onOptionsItemSelected(item);
    }

    private void addInstance() {
        final EditText et = new EditText(this);
        et.setSingleLine();

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Add instance")
                .setMessage("Enter instance code")
                .setView(et)
                .setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO
                    }
                })
                .setNegativeButton(R.string.cancelbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.show();
    }

    class InstanceListAdapter extends CursorAdapter implements OnItemClickListener {
        final int regularColor, selectedColor;

        public InstanceListAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
            regularColor = PlatformUtil.getResource(context, R.attr.textColorPrimary);
            selectedColor = PlatformUtil.getResource(context, R.attr.textColorSecondary);
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
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyDbAdapter.InstanceColumns._ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyDbAdapter.InstanceColumns.NAME));

            //final Instance instance = ...
            //view.setTag(instance);

            TextView nameView = (TextView) view.findViewById(R.id.itemheader);
            nameView.setText(name);

            int colorRes = regularColor;
            /*
            final User loggedUser = FlowApp.getApp().getUser();
            if (loggedUser != null && loggedUser.getId() == id) {
                colorRes = selectedColor;
            }
            nameView.setTextColor(getResources().getColorStateList(colorRes));
            */

            // Alternate background
            int attr = cursor.getPosition() % 2 == 0 ? R.attr.listitem_bg1
                    : R.attr.listitem_bg2;
            final int res= PlatformUtil.getResource(context, attr);
            view.setBackgroundResource(res);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //TODO
        }

    }

}

