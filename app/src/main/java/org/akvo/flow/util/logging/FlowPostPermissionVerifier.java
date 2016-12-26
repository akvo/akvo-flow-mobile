/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util.logging;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.joshdholtz.sentry.AbstractPermissionVerifier;

import org.akvo.flow.util.StatusUtil;

public class FlowPostPermissionVerifier extends AbstractPermissionVerifier {

    public FlowPostPermissionVerifier() {
    }

    /**
     * Decides if the stacktrace should be sent to the server
     *
     * @return
     * @param context
     */
    public boolean shouldAttemptPost(Context context) {
        if (!StatusUtil.isConnectionAllowed(context)) {
            //User did not allow using 3G and wifi is not connected
            return false;
        }
        //Is the permission set in manifest?
        PackageManager pm = context.getPackageManager();
        int hasPerm = pm.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE,
                context.getPackageName());
        if (hasPerm != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        //is there a connection?
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}