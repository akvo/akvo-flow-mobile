/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.content.Context;
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.data.preference.Prefs;

public class ServerManager {

    private Prefs prefs;

    public ServerManager(Context context) {
        this.prefs = new Prefs(context);
    }

    /**
     * Get the specified server URL. If no custom server has been set (debug),
     * the default one will be returned.
     *
     * @return server URL string
     */
    public String getServerBase() {
        String serverBase = prefs.getString(Prefs.KEY_BACKEND_SERVER, null);
        if (TextUtils.isEmpty(serverBase)) {
            serverBase = BuildConfig.SERVER_BASE;
        }
        return serverBase;
    }


    public String getApiKey() {
        return BuildConfig.API_KEY;
    }
}
