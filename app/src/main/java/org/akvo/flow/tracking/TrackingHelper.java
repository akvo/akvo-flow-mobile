/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.tracking;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class TrackingHelper {

    private final FirebaseAnalytics firebaseAnalytics;

    public TrackingHelper(Context context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void logStatsEvent(String tabName) {
        Bundle params = new Bundle();
        params.putString("from_tab", tabName);
        firebaseAnalytics.logEvent("menu_stats_pressed", params);
    }

    public void logSortEvent() {
        firebaseAnalytics.logEvent("list_menu_sort_pressed", null);
    }

    public void logDownloadEvent(String tabName) {
        Bundle params = new Bundle();
        params.putString("from_tab", tabName);
        firebaseAnalytics.logEvent("menu_download_pressed", params);
    }

    public void logUploadEvent(String tabName) {
        Bundle params = new Bundle();
        params.putString("from_tab", tabName);
        firebaseAnalytics.logEvent("menu_upload_pressed", params);
    }

    public void logSortEventChosen(String orderSuffix) {
        firebaseAnalytics.logEvent("sort_by_" + orderSuffix, null);
    }

    public void logSearchEvent() {
        firebaseAnalytics.logEvent("list_search_pressed", null);
    }
}
