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

import android.content.Context;
import android.content.res.Resources;

import org.akvo.flow.data.R;

import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import timber.log.Timber;

@Singleton
public class PropertiesDataSource {

    private static final String SERVER_BASE = "serverBase";
    private static final String API_KEY = "apiKey";

    private final Properties properties;
    private final Resources resources;
    private boolean loaded;

    @Inject
    public PropertiesDataSource(Context context) {
        properties = new Properties();
        resources = context.getResources();
    }

    private String getProperty(String propertyName) {
        if (!loaded) {
            loadProperties();
        }
        return properties.getProperty(propertyName);
    }

    /**
     * reads the property file from the apk and returns the contents in a
     * Properties object.
     *
     */
    private void loadProperties() {
        try {
            InputStream rawResource = resources
                    .openRawResource(R.raw.survey);
            properties.load(rawResource);
            loaded = true;
        } catch (Exception e) {
            Timber.e(e, "Could not load properties");
        }
    }

    public Observable<String> getBaseUrl() {
        return Observable.just(getProperty(SERVER_BASE));
    }

    public Observable<String> getApiKey() {
        return Observable.just(getProperty(API_KEY));
    }
}
