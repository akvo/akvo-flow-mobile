/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.domain.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("HardwareIds")
@Singleton
class DeviceHelper @Inject constructor(private val context: Context) {

    val phoneNumber: String? by lazy {
        computePhoneNumber()
    }

    private val macAddress: String? by lazy {
        computeMacAddress()
    }

    val imei: String by lazy {
        computeImei()
    }

    @get:SuppressLint("HardwareIds")
    val androidId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    /**
     * gets the device's primary phone number on the SIM card
     *
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    fun computePhoneNumber(): String? {
        if (androidId.isNotBlank()) {
            //if we have androidId, we don't need the phone number. Phone numbers can be duplicated like +52 or 1
            return ""
        }
        val teleMgr = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        var number: String? = null
        if (teleMgr != null && isAllowedToReadPhoneState) {
            // On a GSM device, this will only work if the provider put the
            // number on the SIM card
            number = teleMgr.line1Number
        }
        if (number == null
            || number.trim { it <= ' ' }.isEmpty()
            || number.trim { it <= ' ' }.equals("null", ignoreCase = true)
            || number.trim { it <= ' ' }.equals("unknown", ignoreCase = true)
        ) {
            // If we can't get the phone number, use the MAC instead (only Android < 6.0)
            number = macAddress
        }
        return number
    }

    /** Returns the devices Mac Address
     * (only Android < 6.0)
     */
    @SuppressLint("HardwareIds")
    private fun computeMacAddress(): String? {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            return ""
        }
        var macAddress: String? = null
        val wifiMgr =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        if (wifiMgr != null) {
            val info = wifiMgr.connectionInfo
            if (info != null) {
                macAddress = info.macAddress
            }
        }
        return macAddress
    }

    /**
     * gets the device's IMEI (MEID or ESN for CDMA phone)
     * (only Android < 10.0)
     */
    @SuppressLint("HardwareIds")
    fun computeImei(): String {
        var number: String? = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val teleMgr =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            if (teleMgr != null && isAllowedToReadPhoneState) {
                @Suppress("DEPRECATION")
                number = teleMgr.deviceId
            }
        }
        if (number == null) {
            number = "NO_IMEI"
        }
        return number
    }

    private val isAllowedToReadPhoneState: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PermissionChecker.PERMISSION_GRANTED
}
