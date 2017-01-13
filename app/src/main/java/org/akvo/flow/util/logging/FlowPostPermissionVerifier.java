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

package org.akvo.flow.util.logging;

import android.content.Context;

import com.joshdholtz.sentry.PostPermissionVerifier;

import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.util.ConnectivityStateManager;

public class FlowPostPermissionVerifier extends PostPermissionVerifier {
    private final ConnectivityStateManager connectivityStateManager;
    private final Prefs prefs;

    public FlowPostPermissionVerifier(Context context) {
        this.connectivityStateManager = new ConnectivityStateManager(context);
        this.prefs = new Prefs(context);
    }

    /**
     * Decides if the stacktrace should be sent to the server
     *
     * @return
     * @param context
     */
    public boolean shouldAttemptPost(Context context) {
        boolean syncOver3GAllowed = prefs
                .getBoolean(Prefs.KEY_CELL_UPLOAD, Prefs.DEFAULT_VALUE_CELL_UPLOAD);
        //User did not allow using 3G and wifi is not connected
        return connectivityStateManager.isConnectionAvailable(syncOver3GAllowed) && super
                .shouldAttemptPost(context);
    }
}