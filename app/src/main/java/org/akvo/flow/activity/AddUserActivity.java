/*
 *  Copyright (C) 2015-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.User;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import timber.log.Timber;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class AddUserActivity extends Activity {

    @BindView(R.id.login_btn)
    View nextBt;

    @BindView(R.id.username)
    EditText nameEt;

    @BindView(R.id.device_id)
    EditText deviceIdEt;

    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_user_activity);
        ButterKnife.bind(this);
        prefs = new Prefs(getApplicationContext());
        deviceIdEt.setText(prefs.getString(Prefs.KEY_DEVICE_IDENTIFIER, ""));
        Timber.e(new RuntimeException("failing tests testing sentry2"));
    }

    //TODO: database operations should be done on separate thread
    private void saveUserData() {
        String username = nameEt.getText().toString().trim();
        String deviceId = deviceIdEt.getText().toString().trim();
        Context context = getApplicationContext();
        SurveyDbDataSource db = new SurveyDbDataSource(context, null);
        db.open();
        long uid = db.createOrUpdateUser(null, username);
        db.close();

        prefs.setString(Prefs.KEY_DEVICE_IDENTIFIER, deviceId);

        // Select the newly created user, and exit the Activity
        FlowApp.getApp().setUser(new User(uid, username));
        setResult(RESULT_OK);
        finish();
    }

    @OnTextChanged(value = { R.id.username, R.id.device_id }, callback = AFTER_TEXT_CHANGED)
    void afterUserInputChanged() {
        boolean valid =
                !TextUtils.isEmpty(nameEt.getText()) && !TextUtils.isEmpty(deviceIdEt.getText());
        nextBt.setEnabled(valid);
    }

    @OnClick(R.id.login_btn)
    void onNextButtonTap() {
        saveUserData();
    }

    @OnEditorAction(R.id.device_id)
    boolean onDeviceIdAction(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            saveUserData();
            return true;
        }
        return false;
    }
}
