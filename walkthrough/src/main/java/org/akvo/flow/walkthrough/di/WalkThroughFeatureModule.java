/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.walkthrough.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import org.akvo.flow.walkthrough.data.DataPreferencesRepository;
import org.akvo.flow.walkthrough.data.WalkThroughSharedPreferenceDataSource;
import org.akvo.flow.walkthrough.domain.PreferencesRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class WalkThroughFeatureModule {

    private final Application application;

    public WalkThroughFeatureModule(Application application) {
        this.application = application;
    }

    @Singleton
    @Provides
    Context provideContext() {
        return application;
    }

    @Singleton
    @Provides
    SharedPreferences providesSharedPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences("walkthrough_prefs", Context.MODE_PRIVATE);
    }

    @Singleton
    @Provides
    PreferencesRepository providePreferencesRepository(
            WalkThroughSharedPreferenceDataSource dataSource) {
        return new DataPreferencesRepository(dataSource);
    }
}
