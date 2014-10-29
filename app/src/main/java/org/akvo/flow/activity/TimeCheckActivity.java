/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.service.TimeCheckService;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeCheckActivity extends Activity {
    private static final String PATTERN = "HH:mm, yyyy-MM-dd (zzzz)";// ISO 8601

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.time_check_activity);


        TextView tv = (TextView)findViewById(R.id.local_time_tv);
        tv.setText(new SimpleDateFormat(PATTERN).format(new Date()));

        Button b = (Button)findViewById(R.id.adjust_btn);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS), 0);
                //finish();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Since date/time settings (might) have been updated, we fire the services sensitive
        // to time changes (the ones interacting with S3)
        startService(new Intent(this, SurveyDownloadService.class));
        startService(new Intent(this, DataSyncService.class));
        startService(new Intent(this, ApkUpdateService.class));
        startService(new Intent(this, TimeCheckService.class));// Re-check time setting status
        finish();
    }

}
