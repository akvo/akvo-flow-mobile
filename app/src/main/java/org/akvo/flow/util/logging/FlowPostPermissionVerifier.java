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

import android.content.Context;

import com.joshdholtz.sentry.PostPermissionVerifier;

import org.akvo.flow.util.StatusUtil;

public class FlowPostPermissionVerifier extends PostPermissionVerifier {

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
        return super.shouldAttemptPost(context);
    }
}