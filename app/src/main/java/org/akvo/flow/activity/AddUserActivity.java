/*
 *  Copyright (C) 2015-2018 Stichting Akvo (Akvo Foundation)
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
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.BaseActivity;
import org.akvo.flow.util.logging.LoggingHelper;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class AddUserActivity extends BaseActivity {

    @BindView(R.id.login_btn)
    View nextBt;

    @BindView(R.id.username)
    EditText nameEt;

    @BindView(R.id.device_id)
    EditText deviceIdEt;

    @Inject
    Prefs prefs;

    @Inject
    SurveyDbDataSource surveyDbDataSource;

    @Inject
    LoggingHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_user_activity);
        initializeInjector();
        ButterKnife.bind(this);
        deviceIdEt.setText(prefs.getString(Prefs.KEY_DEVICE_IDENTIFIER, ""));
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    //TODO: database operations should be done on separate thread
    private void saveUserData() {
        String username = nameEt.getText().toString().trim();
        String deviceId = deviceIdEt.getText().toString().trim();
        surveyDbDataSource.open();
        long uid = surveyDbDataSource.createOrUpdateUser(null, username);
        surveyDbDataSource.close();

        prefs.setString(Prefs.KEY_DEVICE_IDENTIFIER, deviceId);

        // Select the newly created user, and exit the Activity
        FlowApp.getApp().setUser(new User(uid, username));
        helper.initLoginData(username, deviceId);
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
