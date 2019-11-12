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
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.uicomponents.LocaleAwareActivity;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.logging.LoggingHelper;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class AddUserActivity extends LocaleAwareActivity {

    @BindView(R.id.login_btn)
    View nextBt;

    @BindView(R.id.username)
    EditText nameEt;

    @BindView(R.id.device_id)
    EditText deviceIdEt;

    @Inject
    LoggingHelper helper;

    @Inject
    Prefs prefs;

    @Inject
    SurveyDbDataSource surveyDbDataSource;

    @Inject
    Navigator navigator;

    private boolean isJustCreated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_user_activity);
        initializeInjector();
        ButterKnife.bind(this);
        deviceIdEt.setText(prefs.getString(Prefs.KEY_DEVICE_IDENTIFIER, ""));
        navigateToSurveyIfNoSetupNeeded();
        isJustCreated = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isJustCreated) {
            isJustCreated = false;
        } else {
            navigateToSurveyIfNoSetupNeeded();
        }
    }

    private void navigateToSurveyIfNoSetupNeeded() {
        boolean deviceSetCorrectly = prefs.getBoolean(Prefs.KEY_SETUP, false);
        if (deviceSetCorrectly) {
            navigateToSurvey();
        }
    }

    private void navigateToSurvey() {
        navigator.navigateToSurveyActivity(this);
        finish();
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    //TODO: database operations should be done on separate thread
    private void saveUserData() {
        String username = nameEt.getText().toString().trim();
        String deviceId = deviceIdEt.getText().toString().trim();
        boolean emptyUserName = TextUtils.isEmpty(username);
        boolean emptyDeviceId = TextUtils.isEmpty(deviceId);
        if (emptyUserName) {
            nameEt.setError(getString(R.string.error_username_required));
        }
        if (emptyDeviceId) {
            deviceIdEt.setError(getString(R.string.error_device_id_required));
        }
        if (!emptyUserName && !emptyDeviceId && nextBt.isEnabled()) {
            nextBt.setEnabled(false);
            surveyDbDataSource.open();
            long userId = surveyDbDataSource.createOrUpdateUser(null, username);
            surveyDbDataSource.close();

            prefs.setString(Prefs.KEY_DEVICE_IDENTIFIER, deviceId);
            prefs.setBoolean(Prefs.KEY_SETUP, true);

            // Select the newly created user, and exit the Activity
            prefs.setLong(Prefs.KEY_USER_ID, userId);
            helper.initLoginData(username, deviceId);
            navigateToSurvey();
        }
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
