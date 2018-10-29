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

package org.akvo.flow.ui.view;

import android.content.Context;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.presentation.SnackBarManager;

import javax.inject.Inject;

public class LocationSnackBarManager {

    private final SnackBarManager snackBarManager;

    @Inject
    public LocationSnackBarManager(SnackBarManager snackBarManager) {
        this.snackBarManager = snackBarManager;
    }

    public void displayPermissionMissingSnackBar(View layout, View.OnClickListener listener,
            Context context) {
        snackBarManager.displaySnackBarWithAction(layout, R.string.location_permission_refused,
                R.string.action_retry, listener, context);
    }

    public void displayLocationTimeoutSnackBar(View coordinatorLayout,
            View.OnClickListener listener, Context context) {
        snackBarManager.displaySnackBarWithAction(coordinatorLayout, R.string.location_timeout,
                R.string.action_retry, listener, context);
    }

    public void displayGeoLocationDiabled(View coordinatorLayout,
            View.OnClickListener listener, Context context) {
        snackBarManager.displaySnackBarWithAction(coordinatorLayout, R.string.geodialog,
                R.string.action_enable, listener, context);
    }
}
