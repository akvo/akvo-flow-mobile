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

package org.akvo.flow.data.net;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeviceHelper {

    private final Context context;

    @Inject
    public DeviceHelper(Context context) {
        this.context = context;
    }

    /**
     * gets the device's primary phone number
     *
     * @return
     */
    public String getPhoneNumber() {
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
            // If we can't get the phone number, use the MAC instead (only Android < 6.0)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                return "";
            }
            WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiMgr != null) {
                // presumably if we don't have a cell connection, then we must
                // be connected by WIFI so this should work
                WifiInfo info = wifiMgr.getConnectionInfo();
                if (info != null) {
                    number = info.getMacAddress();
                }
            }
        }
        return number;
    }

    /**
     * gets the device's IMEI (MEID or ESN for CDMA phone)
     *
     * @return
     */
    public String getImei() {
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

    public String getAndroidID() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
