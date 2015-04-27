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

import android.animation.LayoutTransition;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.loader.InstanceLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.Instance;
import org.akvo.flow.util.PlatformUtil;

public class InstanceSetupActivity extends ActionBarActivity  implements LoaderCallbacks<Instance>,
        View.OnClickListener {
    private LinearLayout mInstances;
    private View mAddButton;
    private View mForm;
    private EditText mPasscodeInput;

    private SurveyDbAdapter mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instance_setup_activity);

        // Perform smooth transitions if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ((LinearLayout)(findViewById(R.id.main_ll))).setLayoutTransition(new LayoutTransition());
        }

        mAddButton = findViewById(R.id.add_dashboard);
        mAddButton.setOnClickListener(this);
        mForm = findViewById(R.id.form);
        mInstances = (LinearLayout)findViewById(R.id.instances);
        mPasscodeInput = (EditText)findViewById(R.id.passcode_et);
        findViewById(R.id.ok_button).setOnClickListener(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDatabase = new SurveyDbAdapter(getApplicationContext());
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
        mInstances.removeAllViews();
        if (cursor != null && cursor.moveToFirst()) {
            int regularColor = PlatformUtil.getResource(this, R.attr.textColorPrimary);
            int selectedColor = PlatformUtil.getResource(this, R.attr.textColorSecondary);
            LayoutInflater inflater = LayoutInflater.from(this);
            do {
                View view = inflater.inflate(R.layout.instance_list_item, null);

                final Instance instance = SurveyDbAdapter.getInstance(cursor);
                view.setTag(instance);

                TextView nameView = (TextView) view.findViewById(R.id.tv);
                nameView.setText(instance.getAppId());

                int colorRes = regularColor;
                if (instance.equals(FlowApp.getApp().getInstance())) {
                    colorRes = selectedColor;
                }
                nameView.setTextColor(getResources().getColorStateList(colorRes));

                // Alternate background
                int attr = cursor.getPosition() % 2 == 0 ? R.attr.listitem_bg1
                        : R.attr.listitem_bg2;
                final int res= PlatformUtil.getResource(this, attr);
                view.setBackgroundResource(res);
                view.setOnClickListener(this);
                mInstances.addView(view);
            } while (cursor.moveToNext());
            cursor.close();
        }

        displayInput(mInstances.getChildCount() == 0);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_dashboard:
                displayInput(true);
                break;
            case R.id.ok_button:
                final String code = mPasscodeInput.getText().toString();
                if (!validateCode(code)) {
                    Toast.makeText(InstanceSetupActivity.this, "Invalid code", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(InstanceSetupActivity.this, "Loading (" + code + ") ...", Toast.LENGTH_SHORT).show();

                Bundle args = new Bundle();
                args.putString("code", code);

                getSupportLoaderManager().restartLoader(0, args, InstanceSetupActivity.this);
                break;
            default:
                // Instance click
                Instance instance = (Instance)view.getTag();
                FlowApp.getApp().setInstance(instance);
                display();
                Intent i = new Intent(InstanceSetupActivity.this, SurveyGroupListActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return onOptionsItemSelected(item);
    }

    private boolean validateCode(String code) {
        // Perform a check-digit validation, using Luhn algorithm: http://en.wikipedia.org/wiki/Luhn_algorithm
        code = code != null ? code.trim() : null;
        if (TextUtils.isEmpty(code) || code.length() < 2) {
            return false;
        }
        int n = code.length();
        final int checkDigit = Character.getNumericValue(code.charAt(n-1));

        int luhnSum = 0;
        for (int i=0; i<n-1; i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            if (digit < 0 || digit > 9) {
                return false;
            }
            if (i % 2 == n % 2) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = 1 + (digit % 10);
                }
            }
            luhnSum += digit;
        }

        if (luhnSum % 10 == 0) {
            return checkDigit == 0;
        }
        return checkDigit == 10 - (luhnSum % 10);
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
        mPasscodeInput.getText().clear();
        display();
    }

    @Override
    public void onLoaderReset(Loader<Instance> loader) {
    }

    private void displayInput(boolean display) {
        if (display) {
            mAddButton.setVisibility(View.GONE);
            mForm.setVisibility(View.VISIBLE);
        } else {
            mForm.setVisibility(View.GONE);
            mAddButton.setVisibility(View.VISIBLE);
        }
    }

}

