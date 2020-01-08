/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import javax.inject.Inject;

public class ConnectivityStateManager {

    private final Context context;

    @Inject
    public ConnectivityStateManager(Context appContext) {
        this.context = appContext;
    }

    /**
     * checks whether or not we have a usable data connection
     *
     */
    public boolean isConnectionAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo[] infoArr = connMgr.getAllNetworkInfo();
            if (infoArr != null) {
                for (NetworkInfo anInfoArr : infoArr) {
                    if (NetworkInfo.State.CONNECTED == anInfoArr.getState()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
