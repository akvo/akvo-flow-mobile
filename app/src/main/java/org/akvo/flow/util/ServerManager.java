/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.util;

import android.content.Context;
import android.text.TextUtils;

import org.akvo.flow.data.preference.Prefs;

public class ServerManager {

    private final Context context;
    private Prefs prefs;

    public ServerManager(Context context) {
        this.context = context;
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
            serverBase = new PropertyUtil(context.getResources())
                    .getProperty(ConstantUtil.SERVER_BASE);
        }
        return serverBase;
    }


    public String getApiKey() {
        PropertyUtil props = new PropertyUtil(context.getResources());
        return props.getProperty(ConstantUtil.API_KEY);
    }
}
