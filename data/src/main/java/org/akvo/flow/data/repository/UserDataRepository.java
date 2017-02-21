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

package org.akvo.flow.data.repository;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.util.ConnectivityStateManager;
import org.akvo.flow.domain.repository.UserRepository;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class UserDataRepository implements UserRepository {

    private final ConnectivityStateManager connectivityStateManager;
    private final DataSourceFactory dataSourceFactory;

    @Inject
    public UserDataRepository(ConnectivityStateManager connectivityStateManager,
            DataSourceFactory dataSourceFactory) {
        this.connectivityStateManager = connectivityStateManager;
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public Observable<Boolean> allowedToSync() {
        return dataSourceFactory.getSharedPreferencesDataSource().mobileSyncEnabled().map(
                new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean enabled) {
                        return enabled || connectivityStateManager.isWifiConnected();
                    }
                });
    }
}
