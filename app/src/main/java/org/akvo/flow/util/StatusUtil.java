/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.Instance;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * utilities for checking system state
 * 
 * @author Christopher Fagiani
 */
public class StatusUtil {

    /**
     * checks whether or not we have a usable data connection
     * 
     * @param context
     * @return
     */
    public static boolean hasDataConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo[] infoArr = connMgr.getAllNetworkInfo();
            if (infoArr != null) {
                for (int i = 0; i < infoArr.length; i++) {
                    if (StatusUtil.syncOver3G(context)) {
                        // if we don't care what KIND of connection we have, just that there is one
                        if (NetworkInfo.State.CONNECTED == infoArr[i].getState()) {
                            return true;
                        }
                    } else {
                        // if we only want to use wifi, we need to check the type
                        if (infoArr[i].getType() == ConnectivityManager.TYPE_WIFI
                                && NetworkInfo.State.CONNECTED == infoArr[i].getState()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * gets the device's primary phone number
     * 
     * @return
     */
    public static String getPhoneNumber(Context context) {
        TelephonyManager teleMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String number = null;
        if (teleMgr != null) {
            // On a GSM device, this will only work if the provider put the
            // number on the SIM card
            number = teleMgr.getLine1Number();
        }
        if (number == null || number.trim().length() == 0
                || number.trim().equalsIgnoreCase("null")
                || number.trim().equalsIgnoreCase("unknown")) {
            // if we can't get the phone number, use the MAC instead
            WifiManager wifiMgr = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiMgr != null) {
                // presumably if we don't have a cell connection, then we must
                // be connected by WIFI so this should work
                WifiInfo info = wifiMgr.getConnectionInfo();
                if (info != null) {
                    number = info.getMacAddress();
                }
            }
        }
        // handle the case where we don't have a phone number OR a
        // WIFI connection (could be offline, using Bluetooth or cable
        // connection)
        if (number == null || number.trim().length() == 0) {
            number = teleMgr.getDeviceId(); // IMEI on a GSM device
        } else {
            number = number.trim(); // sometimes numbers are reported w leading
                                    // space
            if (number.startsWith("+"))
                number = number.substring(1); // sometimes the + prefix can
                                              // appear and disappear
        }
        return number;
    }

    /**
     * gets the device's IMEI (MEID or ESN for CDMA phone)
     * 
     * @return
     */
    public static String getImei(Context context) {
        TelephonyManager teleMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String number = null;
        if (teleMgr != null) {
            number = teleMgr.getDeviceId();
        }
        if (number == null) {
            number = "NO_IMEI";
        }
        return number;
    }
    
    public static boolean hasExternalStorage() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * syncOver3G checks the value of 'Sync over 3G' setting
     * @param context
     * @return true if the flag is enabled, false otherwise
     */
    private static boolean syncOver3G(Context context) {
        return Prefs.getBoolean(context, Prefs.KEY_DATA_ENABLED, Prefs.DEFAULT_DATA_ENABLED);
    }

    public static String getDeviceId(Context context) {
        return Prefs.getString(context, Prefs.KEY_DEVICE_ID, "");
    }

    /**
     * Get the specified server URL. If no custom server has been set (debug),
     * the default one will be returned.
     */
    public static String getServerBase(Context context) {
        String serverBase =  Prefs.getString(context, Prefs.KEY_APP_SERVER, null);
        if (TextUtils.isEmpty(serverBase) && FlowApp.getApp().getInstance() != null) {
            serverBase = FlowApp.getApp().getInstance().getServerBase();
        }
        return serverBase;
    }

    /**
     * Get the application (FLOW instance) id.
    public static String getApplicationId() {
        // Directly fetch the server from the properties file. A local serverBase found in the DB
        // will cause a permanent mismatch for all surveys, since XML files will contain original application

        if (FlowApp.getApp().getInstance() == null) {
            return null;
        }
        String serverBase = FlowApp.getApp().getInstance().getServerBase();
        try {
            // Match instance name from server base, for example:
            // https://akvoflow-X.appspot.com --> akvoflow-X
            String host = new URL(serverBase).getHost();
            if (!TextUtils.isEmpty(host) && host.contains(".")) {
                return host.substring(0, host.indexOf("."));
            }
        } catch (MalformedURLException e) {
            Log.e("getApplicationId() - ", e.getMessage());
        }
        return null;
    }
     */

}
