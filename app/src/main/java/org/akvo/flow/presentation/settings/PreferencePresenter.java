/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.settings;

import org.akvo.flow.domain.entity.UserSettings;
import org.akvo.flow.domain.interactor.DefaultSubscriber;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class PreferencePresenter implements Presenter {

    private final UseCase getUserSettings;
    private final ViewUserSettingsMapper mapper;

    private PreferenceView view;

    @Inject
    public PreferencePresenter(@Named("getUserSettings") UseCase getUserSettings,
            ViewUserSettingsMapper mapper) {
        this.getUserSettings = getUserSettings;
        this.mapper = mapper;
    }

    public void setView(PreferenceView view) {
        this.view = view;
    }

    public void loadPreferences(final List<String> languages) {
        view.showLoading();
        getUserSettings.execute(new DefaultSubscriber<UserSettings>() {
            @Override
            public void onNext(UserSettings userSettings) {
                ViewUserSettings viewUserSettings = mapper.transform(userSettings, languages);
                view.hideLoading();
                view.displaySettings(viewUserSettings);
            }
        }, null);
    }

    @Override
    public void destroy() {
        getUserSettings.unSubscribe();
    }
}
