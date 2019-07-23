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

package org.akvo.flow.offlinemaps.data;

import android.content.SharedPreferences;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Maybe;

public class OfflineSharedPreferenceDataSource {

    private static final long LONG_VALUE_UNSET = -1;
    private static final String KEY_OFFLINE_AREA_ID = "offline_area_id";

    private final SharedPreferences preferences;

    @Inject
    public OfflineSharedPreferenceDataSource(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public Maybe<Long> getSelectedOfflineArea() {
        long areaId = preferences.getLong(KEY_OFFLINE_AREA_ID, LONG_VALUE_UNSET);
        if (areaId == LONG_VALUE_UNSET) {
            return Maybe.empty();
        } else {
            return Maybe.just(areaId);
        }
    }

    public Completable saveSelectedOfflineArea(long areaId) {
        preferences.edit().putLong(KEY_OFFLINE_AREA_ID, areaId).apply();
        return Completable.complete();
    }
}
