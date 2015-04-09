/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.service.LocationService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.util.ArrayPreferenceUtil;
import org.akvo.flow.util.Prefs;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.StringUtil;
import org.akvo.flow.util.LangsPreferenceData;
import org.akvo.flow.util.LangsPreferenceUtil;
import org.akvo.flow.util.ViewUtil;

/**
 * Displays user editable preferences and takes care of persisting them to the
 * database. Some options require the user to enter an administrator passcode
 * via a dialog box before the operation can be performed.
 *
 * @author Christopher Fagiani
 */
public class PreferencesActivity extends Activity implements OnClickListener,
        OnCheckedChangeListener {
    private CheckBox beaconCheckbox;
    private CheckBox screenOnCheckbox;
    private CheckBox mobileDataCheckbox;
    private TextView languageTextView;
    private TextView serverTextView;
    private TextView identTextView;
    private TextView maxImgSizeTextView;
    private TextView localeTextView;

    private LangsPreferenceData langsPrefData;
    private String[] langsSelectedNameArray;
    private boolean[] langsSelectedBooleanArray;
    private int[] langsSelectedMasterIndexArray;

    private String[] maxImgSizes;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.preferences);

        beaconCheckbox = (CheckBox) findViewById(R.id.beaconcheckbox);
        screenOnCheckbox = (CheckBox) findViewById(R.id.screenoptcheckbox);
        mobileDataCheckbox = (CheckBox) findViewById(R.id.uploadoptioncheckbox);
        languageTextView = (TextView) findViewById(R.id.surveylangvalue);
        serverTextView = (TextView) findViewById(R.id.servervalue);
        identTextView = (TextView) findViewById(R.id.identvalue);
        maxImgSizeTextView = (TextView) findViewById(R.id.max_img_size_txt);
        localeTextView = (TextView) findViewById(R.id.locale_name);

        maxImgSizes = getResources().getStringArray(R.array.max_image_size_pref);

        // Setup event listeners
        beaconCheckbox.setOnCheckedChangeListener(this);
        screenOnCheckbox.setOnCheckedChangeListener(this);
        mobileDataCheckbox.setOnCheckedChangeListener(this);
        findViewById(R.id.pref_locale).setOnClickListener(this);
        findViewById(R.id.pref_surveylang).setOnClickListener(this);
        findViewById(R.id.pref_server).setOnClickListener(this);
        findViewById(R.id.pref_deviceid).setOnClickListener(this);
        findViewById(R.id.pref_resize).setOnClickListener(this);
    }

    /**
     * loads the preferences from the DB and sets their current value in the UI
     */
    private void populateFields() {
        boolean screenOn = Prefs.getBoolean(this, Prefs.KEY_SCREEN_ON, Prefs.DEFAULT_SCREEN_ON);
        screenOnCheckbox.setChecked(screenOn);

        boolean sendBeacon = Prefs.getBoolean(this, Prefs.KEY_SEND_BEACONS, Prefs.DEFAULT_SEND_BEACONS);
        beaconCheckbox.setChecked(sendBeacon);

        boolean cellData = Prefs.getBoolean(this, Prefs.KEY_DATA_ENABLED, Prefs.DEFAULT_DATA_ENABLED);
        mobileDataCheckbox.setChecked(cellData);

        langsPrefData = LangsPreferenceUtil.createLangPrefData(this,
                Prefs.getString(this, Prefs.KEY_LANGUAGE, ""),
                Prefs.getString(this, Prefs.KEY_LANGUAGES_PRESENT, ""));
        languageTextView.setText(ArrayPreferenceUtil.formSelectedItemString(
                langsPrefData.getLangsSelectedNameArray(),
                langsPrefData.getLangsSelectedBooleanArray()));

        String server = StatusUtil.getServerBase(this);
        serverTextView.setText(server);

        int imgSize = Prefs.getInt(this, Prefs.KEY_MAX_IMG_SIZE, Prefs.DEFAULT_MAX_IMG_SIZE);
        maxImgSizeTextView.setText(maxImgSizes[imgSize]);

        identTextView.setText(Prefs.getString(this, Prefs.KEY_DEVICE_ID, ""));
        localeTextView.setText(FlowApp.getApp().getAppDisplayLanguage());
    }

    /**
     * opens db connection and sets up listeners (after we hydrate values so we
     * don't trigger the onCheckChanged listener when we set initial values)
     */
    public void onResume() {
        super.onResume();
        populateFields();
    }

    /**
     * displays a pop-up dialog containing the upload or language options
     * depending on what was clicked
     */
    @Override
    public void onClick(View v) {
        final Context context = this;
        switch (v.getId()) {
            case R.id.pref_surveylang:
                langsSelectedNameArray = langsPrefData.getLangsSelectedNameArray();
                langsSelectedBooleanArray = langsPrefData.getLangsSelectedBooleanArray();
                langsSelectedMasterIndexArray = langsPrefData.getLangsSelectedMasterIndexArray();

                ViewUtil.displayLanguageSelector(this, langsSelectedNameArray,
                        langsSelectedBooleanArray,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int clicked) {
                                String lang = LangsPreferenceUtil.formLangPreferenceString(
                                        langsSelectedBooleanArray, langsSelectedMasterIndexArray);
                                Prefs.setString(context, Prefs.KEY_LANGUAGE, lang);

                                languageTextView.setText(ArrayPreferenceUtil
                                        .formSelectedItemString(langsSelectedNameArray,
                                                langsSelectedBooleanArray));
                                if (dialog != null) {
                                    dialog.dismiss();
                                }
                            }
                        }
                );
                break;
            case R.id.pref_locale:
                showLanguageDialog();
                break;
            case R.id.pref_server:
                ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {
                        @Override
                        public void onAuthenticated() {
                            final EditText inputView = new EditText(PreferencesActivity.this);
                            // one line only
                            inputView.setSingleLine();
                            inputView.setText(StatusUtil.getServerBase(PreferencesActivity.this));
                            ViewUtil.ShowTextInputDialog(
                                    PreferencesActivity.this, R.string.serverlabel,
                                    R.string.serverlabel, inputView,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // drop any control chars, especially tabs
                                            String s = StringUtil.ControlToSPace(inputView.getText().toString());
                                            Prefs.setString(context, Prefs.KEY_APP_SERVER, s);
                                            serverTextView.setText(StatusUtil.getServerBase(PreferencesActivity.this));
                                        }
                                    }
                            );
                        }
                    }
                );
                break;
            case R.id.pref_deviceid:
                ViewUtil.showAdminAuthDialog(this,
                        new ViewUtil.AdminAuthDialogListener() {
                            @Override
                            public void onAuthenticated() {
                                final EditText inputView = new EditText(PreferencesActivity.this);
                                // one line only
                                inputView.setSingleLine();
                                ViewUtil.ShowTextInputDialog(
                                        PreferencesActivity.this,
                                        R.string.identlabel,
                                        R.string.setidentlabel, inputView,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                String s = StringUtil.ControlToSPace(inputView.getText().toString());
                                                // drop any control chars, especially tabs
                                                identTextView.setText(s);
                                                Prefs.setString(context, Prefs.KEY_DEVICE_ID, s);
                                                // Trigger the SurveySync Service, in order to force
                                                // a backend connection with the new Device ID
                                                startService(new Intent(PreferencesActivity.this,
                                                        SurveyDownloadService.class));
                                            }
                                        }
                                );
                            }
                        }
                );
                break;
            case R.id.pref_resize:
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.resize_large_images).setItems(maxImgSizes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Prefs.setInt(context, Prefs.KEY_MAX_IMG_SIZE, which);
                                maxImgSizeTextView.setText(maxImgSizes[which]);
                                if (dialog != null) {
                                    dialog.dismiss();
                                }
                            }
                        }
                );
                builder.show();
                break;
        }
    }

    /**
     * Saves the value of the checkbox to the preferences
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.beaconcheckbox:
                Prefs.setBoolean(this, Prefs.KEY_SEND_BEACONS, isChecked);
                if (isChecked) {
                    // if the option changed, kick the service so it reflects the change
                    startService(new Intent(this, LocationService.class));
                } else {
                    stopService(new Intent(this, LocationService.class));
                }
                break;
            case R.id.screenoptcheckbox:
                Prefs.setBoolean(this, Prefs.KEY_SCREEN_ON, isChecked);
                break;
            case R.id.uploadoptioncheckbox:
                Prefs.setBoolean(this, Prefs.KEY_DATA_ENABLED, isChecked);
                break;
        }
    }

    private void showLanguageDialog() {
        final String[] languageCodes = getResources().getStringArray(R.array.app_language_codes);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_language).setItems(R.array.app_languages,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FlowApp.getApp().setAppLanguage(languageCodes[which], true);
                    }
                }
        );
        builder.show();
    }

}
