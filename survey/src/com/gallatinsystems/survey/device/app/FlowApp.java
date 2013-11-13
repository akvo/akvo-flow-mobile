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

package com.gallatinsystems.survey.device.app;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.LangsPreferenceUtil;

public class FlowApp extends Application {
    private static final String TAG = FlowApp.class.getSimpleName();
    private static FlowApp app;// Singleton

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        app = this;
    }

    public static FlowApp getApp() {
        return app;
    }
    
    private void init() {
        mSurveyChecker.run();// Ensure surveys have put their languages
    }

    /**
     * Old versions of the app may not have translations support, thus they will
     * not store languages preference in the database. This task ensures that
     * any language stored in the device has properly set its languages in the
     * database, making them available to the user through the settings menu.
     */
    private Runnable mSurveyChecker = new Runnable() {

        @Override
        public void run() {
            SurveyDbAdapter database = new SurveyDbAdapter(FlowApp.this);
            database.open();

            // We check for the key not present in old devices: 'survey.languagespresent'
            // NOTE: 'survey.language' DID exist
            if (database.findPreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY) == null) {
                Log.d(TAG, "Recomputing available languages...");
                Toast.makeText(getApplicationContext(), R.string.configuring_languages, Toast.LENGTH_SHORT)
                        .show();

                // First, we add the default property, to avoid null cases within
                // the process
                database.savePreference(ConstantUtil.SURVEY_LANG_SETTING_KEY, "");
                database.savePreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY, "");

                // Recompute all the surveys, and store their languages
                for (Survey survey : database.listSurveys(null)) {
                    String[] langs = LangsPreferenceUtil.determineLanguages(FlowApp.this, survey);
                    Log.d(TAG, "Adding languages: " + langs.toString());
                    database.addLanguages(langs);
                }
            }

            database.close();
        }
    };

}
