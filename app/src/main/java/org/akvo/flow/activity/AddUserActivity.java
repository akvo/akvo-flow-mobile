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

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.User;

public class AddUserActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_user_activity);

        final EditText et = (EditText) findViewById(R.id.username);
        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = et.getText().toString();
                SurveyDbAdapter db = new SurveyDbAdapter(AddUserActivity.this).open();
                long id = db.createOrUpdateUser(null, username);
                db.close();

                // Select the newly created user, and exit the Activity
                FlowApp.getApp().setUser(new User(id, username));
                setResult(RESULT_OK);
                finish();
            }
        });
    }

}
