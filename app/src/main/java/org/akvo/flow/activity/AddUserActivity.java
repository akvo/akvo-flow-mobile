/*
 *  Copyright (C) 2015-2016 Stichting Akvo (Akvo Foundation)
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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.ConstantUtil;

public class AddUserActivity extends Activity implements TextWatcher{
    private View mNext;
    private EditText mName;
    private EditText mID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_user_activity);

        mName = (EditText) findViewById(R.id.username);
        mID = (EditText) findViewById(R.id.device_id);
        mNext = findViewById(R.id.login_btn);

        // Ensure the username is not left blank
        mNext.setEnabled(false);
        mName.addTextChangedListener(this);
        mID.addTextChangedListener(this);

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = mName.getText().toString().trim();
                String deviceId = mID.getText().toString().trim();
                SurveyDbAdapter db = new SurveyDbAdapter(AddUserActivity.this).open();
                long uid = db.createOrUpdateUser(null, username);
                db.savePreference(ConstantUtil.DEVICE_IDENT_KEY, deviceId);
                db.close();

                // Select the newly created user, and exit the Activity
                FlowApp.getApp().setUser(new User(uid, username));
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        boolean valid = !TextUtils.isEmpty(mName.getText()) && !TextUtils.isEmpty(mID.getText());
        mNext.setEnabled(valid);
    }

}
