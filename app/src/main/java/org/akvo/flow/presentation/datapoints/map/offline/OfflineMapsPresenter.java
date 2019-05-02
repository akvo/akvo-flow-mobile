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

package org.akvo.flow.presentation.datapoints.map.offline;

import android.content.Context;

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.akvo.flow.presentation.Presenter;

import javax.inject.Inject;

import timber.log.Timber;

public class OfflineMapsPresenter implements Presenter {

    private OfflineMapsView view;

    @Inject
    public OfflineMapsPresenter() {
    }

    @Override
    public void destroy() {

    }

    public void setView(OfflineMapsView view) {
        this.view = view;
    }

    public void load(Context context) {
        view.showLoading();
        //TODO: inject offlineManager
        OfflineManager.getInstance(context).listOfflineRegions(
                new OfflineManager.ListOfflineRegionsCallback() {
                    @Override
                    public void onList(OfflineRegion[] offlineRegions) {
                        if (offlineRegions != null && offlineRegions.length > 0) {
                            view.displayRegions(offlineRegions);
                            //TODO: map to string our something else
                        } else {
                            view.displayNoOfflineMaps();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Timber.e(error);
                        view.displayNoOfflineMaps();
                    }
                });
    }
}
