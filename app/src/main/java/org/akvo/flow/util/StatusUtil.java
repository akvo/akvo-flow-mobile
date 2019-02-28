/*
 *  Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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

import android.os.Environment;
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;

import java.net.MalformedURLException;
import java.net.URL;

import timber.log.Timber;

/**
 * utilities for checking system state
 *
 * @author Christopher Fagiani
 */
public class StatusUtil {

    public static boolean hasExternalStorage() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Get the application (FLOW instance) id.
     */
    public static String getApplicationId() {
        // Directly fetch the server from the properties file. A local serverBase found in the DB
        // will cause a permanent mismatch for all surveys, since XML files will contain original application
        String serverBase = BuildConfig.SERVER_BASE;
        try {
            // Match instance name from server base, for example:
            // https://akvoflow-X.appspot.com --> akvoflow-X
            String host = new URL(serverBase).getHost();
            if (!TextUtils.isEmpty(host) && host.contains(".")) {
                return host.substring(0, host.indexOf("."));
            }
        } catch (MalformedURLException e) {
            Timber.e("getApplicationId() - "+ e.getMessage());
        }
        return null;
    }

}
