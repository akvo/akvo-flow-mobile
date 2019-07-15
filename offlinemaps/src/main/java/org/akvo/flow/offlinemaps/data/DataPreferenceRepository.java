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

import org.akvo.flow.offlinemaps.domain.PreferencesRepository;

import io.reactivex.Completable;
import io.reactivex.Maybe;

public class DataPreferenceRepository implements PreferencesRepository {

    private final OfflineSharedPreferenceDataSource dataSource;

    public DataPreferenceRepository(OfflineSharedPreferenceDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Completable saveSelectedOfflineArea(long offlineAreaId) {
        return dataSource.saveSelectedOfflineArea(offlineAreaId);
    }

    @Override
    public Maybe<Long> getSelectedOfflineArea() {
       return dataSource.getSelectedOfflineArea();
    }
}
