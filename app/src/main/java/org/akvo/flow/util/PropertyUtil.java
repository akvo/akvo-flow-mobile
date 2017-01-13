/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.content.res.Resources;

import org.akvo.flow.R;

import java.io.InputStream;
import java.util.Properties;

import timber.log.Timber;

/**
 * Utility for populating a Properties object from the contents of a well-known
 * property file in the raw resource directory.
 * 
 * @author Christopher Fagiani
 */
public class PropertyUtil {

    private static Properties properties = null;

    /**
     * reads the property file from the apk and returns the contents in a
     * Properties object.
     * 
     * @param resources
     * @return
     */
    private static synchronized void loadProperties(Resources resources) {
        if (properties == null) {
            properties = new Properties();
            try {
                InputStream rawResource = resources
                        .openRawResource(R.raw.survey);
                properties.load(rawResource);
            } catch (Exception e) {
                Timber.e(e, "Could not load properties");
            }
        }
    }

    public PropertyUtil(Resources resources) {
        loadProperties(resources);
    }

    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    /**
     * Load a boolean property. Since all the properties are stored as Strings,
     * the value will be read first as a String, then converted into boolean.
     * 
     * @param propertyName The key for this property
     * @return true If the property value is "true", false otherwise
     */
    public boolean getBoolean(String propertyName) {
        return "true".equalsIgnoreCase(getProperty(propertyName));
    }

}
