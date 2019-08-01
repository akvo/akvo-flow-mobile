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

package org.akvo.flow.walkthrough.data;

import android.content.SharedPreferences;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;

public class WalkThroughSharedPreferenceDataSource {

    private static final String KEY_WALK_THROUGH_SEEN = "walkthrough_seen";

    private final SharedPreferences preferences;

    @Inject
    public WalkThroughSharedPreferenceDataSource(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public Completable saveWalkThroughSeen() {
        preferences.edit().putBoolean(KEY_WALK_THROUGH_SEEN, true).apply();
        return Completable.complete();
    }

    public Single<Boolean> walkThroughSeen() {
        return Single.just(preferences.getBoolean(KEY_WALK_THROUGH_SEEN, false));
    }
}
