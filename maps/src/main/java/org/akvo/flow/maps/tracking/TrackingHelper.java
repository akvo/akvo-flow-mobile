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

package org.akvo.flow.maps.tracking;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class TrackingHelper {

    private final FirebaseAnalytics firebaseAnalytics;

    public TrackingHelper(Context context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void logOfflineAreasListDialogOpened() {
        firebaseAnalytics.logEvent("offline_areas_list_dialog_opened", null);
    }

    public void logOfflineAreaDownloadPressed() {
        firebaseAnalytics.logEvent("offline_area_download_pressed", null);
    }

    public void logUseOnlineMapSelected() {
        firebaseAnalytics.logEvent("use_online_map_selected", null);
    }

    public void logUseOfflineAreaSelected(String name) {
        Bundle params = new Bundle();
        params.putString("area_name", name);
        firebaseAnalytics.logEvent("use_offline_area_selected", params);
    }
}
