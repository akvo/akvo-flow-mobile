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

package org.akvo.flow.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityStateManager {

    private final Context context;

    public ConnectivityStateManager(Context appContext) {
        this.context = appContext;
    }

    /**
     * checks whether or not we have a usable data connection
     *
     * @param syncOver3GAllowed
     * @return
     */
    public boolean isConnectionAvailable(boolean syncOver3GAllowed) {
        android.net.ConnectivityManager connMgr = (android.net.ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo[] infoArr = connMgr.getAllNetworkInfo();
            if (infoArr != null) {
                for (int i = 0; i < infoArr.length; i++) {
                    if (syncOver3GAllowed) {
                        // if we don't care what KIND of connection we have, just that there is one
                        if (NetworkInfo.State.CONNECTED == infoArr[i].getState()) {
                            return true;
                        }
                    } else {
                        // if we only want to use wifi, we need to check the type
                        if (infoArr[i].getType() == android.net.ConnectivityManager.TYPE_WIFI
                                && NetworkInfo.State.CONNECTED == infoArr[i].getState()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isWifiConnected() {
        android.net.ConnectivityManager connectionManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiCheck = connectionManager
                .getNetworkInfo(android.net.ConnectivityManager.TYPE_WIFI);
        return wifiCheck.isConnected();
    }
}
