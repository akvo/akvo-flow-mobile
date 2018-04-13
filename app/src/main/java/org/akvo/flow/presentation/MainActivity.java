/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.presentation;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;

import javax.inject.Inject;

public class MainActivity extends BaseActivity {

    @Inject
    Navigator navigator;

    @Inject
    Prefs prefs;

    private boolean firstCreate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector();
        firstCreate = true;
        navigator.navigateToWalkThrough(this);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!firstCreate) {
            navigateToCorrectActivity();
        }
        firstCreate = false;
    }

    private void navigateToCorrectActivity() {
        if (prefs.getBoolean(Prefs.KEY_SETUP, false)) {
            navigator.navigateToSurveyActivity(this);
        } else {
            navigator.navigateToAddUser(this);
        }
    }
}
