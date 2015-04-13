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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
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
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.loader.InstanceLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.Instance;
import org.akvo.flow.util.PlatformUtil;

public class InstanceSetupActivity extends ActionBarActivity  implements LoaderCallbacks<Instance> {

    private SurveyDbAdapter mDatabase;
    private InstanceListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instance_setup_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDatabase = new SurveyDbAdapter(getApplicationContext());

        ListView lv = (ListView) findViewById(android.R.id.list);
        mAdapter = new InstanceListAdapter(this, null);
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(mAdapter);
        lv.setEmptyView(findViewById(android.R.id.empty));
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
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setSingleLine();

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Add instance")
                .setMessage("Enter instance code")
                .setView(et)
                .setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String code = et.getText().toString();
                        Toast.makeText(InstanceSetupActivity.this, "Loading (" + code + ") ...", Toast.LENGTH_LONG).show();

                        Bundle args = new Bundle();
                        args.putString("code", code);

                        getSupportLoaderManager().restartLoader(0, args, InstanceSetupActivity.this);
                    }
                })
                .setNegativeButton(R.string.cancelbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.show();
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<Instance> onCreateLoader(int id, Bundle args) {
        return new InstanceLoader(this, args.getString("code"));
    }

    @Override
    public void onLoadFinished(Loader<Instance> loader, Instance instance) {
        if (instance == null) {
            Toast.makeText(this, "Error loading instance", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, instance.toString(), Toast.LENGTH_LONG).show();
        mDatabase.addInstance(instance);
        display();
    }

    @Override
    public void onLoaderReset(Loader<Instance> loader) {
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
            final Instance instance = SurveyDbAdapter.getInstance(cursor);
            view.setTag(instance);

            TextView nameView = (TextView) view.findViewById(R.id.itemheader);
            nameView.setText(instance.getAppId());

            int colorRes = regularColor;
            if (instance.equals(FlowApp.getApp().getInstance())) {
                colorRes = selectedColor;
            }
            nameView.setTextColor(getResources().getColorStateList(colorRes));

            // Alternate background
            int attr = cursor.getPosition() % 2 == 0 ? R.attr.listitem_bg1
                    : R.attr.listitem_bg2;
            final int res= PlatformUtil.getResource(context, attr);
            view.setBackgroundResource(res);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Instance instance = (Instance)view.getTag();
            FlowApp.getApp().setInstance(instance);
            display();
            Intent i = new Intent(InstanceSetupActivity.this, SurveyGroupListActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        }

    }

}

