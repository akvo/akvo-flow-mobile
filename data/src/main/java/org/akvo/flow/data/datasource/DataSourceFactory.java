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

package org.akvo.flow.data.datasource;

import org.akvo.flow.data.datasource.preferences.SharedPreferencesDataSource;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataSourceFactory {

    private final SharedPreferencesDataSource sharedPreferencesDataSource;
    private final ImageDataSource imageDataSource;
    private final DatabaseDataSource dataBaseDataSource;
    private final PropertiesDataSource propertiesDataSource;

    @Inject
    public DataSourceFactory(SharedPreferencesDataSource sharedPreferencesDataSource,
            ImageDataSource imageDataSource, DatabaseDataSource dataBaseDataSource, PropertiesDataSource propertiesDataSource) {
        this.sharedPreferencesDataSource = sharedPreferencesDataSource;
        this.imageDataSource = imageDataSource;
        this.dataBaseDataSource = dataBaseDataSource;
        this.propertiesDataSource = propertiesDataSource;
    }

    public SharedPreferencesDataSource getSharedPreferencesDataSource() {
        return sharedPreferencesDataSource;
    }

    public ImageDataSource getImageDataSource() {
        return imageDataSource;
    }

    public DatabaseDataSource getDataBaseDataSource() {
        return dataBaseDataSource;
    }

    public PropertiesDataSource getPropertiesDataSource() {
        return propertiesDataSource;
    }

}
