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

package org.akvo.flow.presentation.datapoints;

import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

import org.akvo.flow.R;

public class DataPointSyncSnackBarManager {

    private final DataPointSyncView dataPointSyncView;

    public DataPointSyncSnackBarManager(DataPointSyncView dataPointSyncView) {
        this.dataPointSyncView = dataPointSyncView;
    }

    public void showSyncedResults(int numberOfSyncedItems) {
        View rootView = dataPointSyncView.getRootView();
        if (rootView != null) {
            displaySnackBar(
                    rootView.getContext().getString(R.string.data_points_sync_success_message,
                            numberOfSyncedItems));
        }
    }

    public void showErrorSyncNotAllowed() {
        View rootView = dataPointSyncView.getRootView();
        if (rootView != null) {
            Snackbar.make(rootView, R.string.data_points_sync_error_mobile_data_sync,
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_settings, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dataPointSyncView.onSettingsPressed();
                        }
                    })
                    .show();
        }
    }

    public void showErrorNoNetwork() {
        displaySnackBarWithRetry(R.string.data_points_sync_error_message_network);
    }

    public void showErrorSync() {
        displaySnackBarWithRetry(R.string.data_points_sync_error_message_default);
    }

    public void showErrorAssignmentMissing() {
        View rootView = dataPointSyncView.getRootView();
        if (rootView != null) {
            displaySnackBar(rootView.getContext()
                    .getString(R.string.data_points_sync_error_message_assignment));
        }
    }

    private void displaySnackBar(String message) {
        View rootView = dataPointSyncView.getRootView();
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void displaySnackBarWithRetry(@StringRes int errorMessage) {
        View rootView = dataPointSyncView.getRootView();
        if (rootView != null) {
            Snackbar.make(rootView, errorMessage, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dataPointSyncView.onRetryRequested();
                        }
                    })
                    .show();
        }
    }
}