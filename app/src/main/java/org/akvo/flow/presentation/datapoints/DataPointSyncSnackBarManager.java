/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

import android.view.View;

import androidx.annotation.StringRes;

import org.akvo.flow.R;
import org.akvo.flow.uicomponents.SnackBarManager;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class DataPointSyncSnackBarManager {

    private final SnackBarManager snackBarManager;

    @Inject
    public DataPointSyncSnackBarManager(SnackBarManager snackBarManager) {
        this.snackBarManager = snackBarManager;
    }

    public void showDownloadedResults(int numberOfSyncedItems, View rootView) {
        if (rootView != null) {
            String message = rootView.getResources()
                    .getQuantityString(R.plurals.data_points_sync_success_message,
                            numberOfSyncedItems, numberOfSyncedItems);
            snackBarManager.displaySnackBar(rootView, message, rootView.getContext());
        }
    }

    public void showErrorNoNetwork(View rootView, View.OnClickListener onClickListener) {
        displaySnackBarWithRetry(R.string.data_points_sync_error_message_network, rootView,
                onClickListener);
    }

    public void showErrorSync(View rootView, View.OnClickListener onClickListener) {
        displaySnackBarWithRetry(R.string.data_points_sync_error_message_default, rootView,
                onClickListener);
    }

    public void showErrorAssignmentMissing(View rootView) {
        if (rootView != null) {
            displaySnackBar(rootView.getContext()
                    .getString(R.string.data_points_sync_error_message_assignment), rootView);
        }
    }

    private void displaySnackBar(String message, View rootView) {
        if (rootView != null) {
            snackBarManager.displaySnackBar(rootView, message, rootView.getContext());
        }
    }

    private void displaySnackBarWithRetry(@StringRes int errorMessage, View rootView,
            View.OnClickListener onClickListener) {
        if (rootView != null) {
            snackBarManager.displaySnackBarWithAction(rootView, errorMessage, R.string.action_retry,
                    onClickListener, rootView.getContext());
        }
    }

    public void showNoDataPointsToDownload(View rootView) {
        if (rootView != null) {
            displaySnackBar(rootView.getContext()
                    .getString(R.string.data_points_sync_no_data_points), rootView);
        }
    }

    public void showErrorDataPointBeingDownloaded(@Nullable View rootView) {
        if (rootView != null) {
            displaySnackBar(rootView.getContext()
                    .getString(R.string.data_point_not_ready_to_be_opened), rootView);
        }
    }
}
