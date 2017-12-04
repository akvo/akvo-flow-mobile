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

import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.util.ConnectivityStateManager;

public class LoggingSendPermissionVerifier {

    private final ConnectivityStateManager connectivityStateManager;
    private final Prefs prefs;

    public LoggingSendPermissionVerifier(ConnectivityStateManager connectivityStateManager,
            Prefs prefs) {
        this.connectivityStateManager = connectivityStateManager;
        this.prefs = prefs;
    }

    /**
     * Decides if the stacktrace should be sent to the server
     *
     */
    public boolean shouldAttemptPost() {
        boolean syncOver3GAllowed = prefs
                .getBoolean(Prefs.KEY_CELL_UPLOAD, Prefs.DEFAULT_VALUE_CELL_UPLOAD);
        return connectivityStateManager.isConnectionAvailable(syncOver3GAllowed);
    }
}
