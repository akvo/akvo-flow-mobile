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

package org.akvo.flow.offlinemaps.presentation.download;

import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.akvo.flow.mapbox.offline.reactive.RegionNameMapper;

import javax.inject.Inject;

import timber.log.Timber;

public class OfflineMapDownloadPresenter {

    private final OfflineManager offlineManager;
    private final RegionNameMapper regionNameMapper;
    private OfflineMapDownloadView view;

    @Inject
    public OfflineMapDownloadPresenter(OfflineManager offlineManager,
            RegionNameMapper regionNameMapper) {
        this.offlineManager = offlineManager;
        this.regionNameMapper = regionNameMapper;
    }

    public void destroy() {
        //EMPTY
    }

    public void setView(OfflineMapDownloadView view) {
        this.view = view;
    }

    public void downloadArea(String styleUrl, LatLngBounds bounds, float pixelRatio, double zoom,
            String regionName) {
        view.showProgress();
        double minZoom = Math.max(zoom - 2, MapboxConstants.MINIMUM_ZOOM);
        double maxZoom = Math.min(zoom + 2, MapboxConstants.MAXIMUM_ZOOM);
        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                styleUrl, bounds, minZoom, maxZoom, pixelRatio);
        byte[] metadata = regionNameMapper.getRegionMetadata(regionName);
        offlineManager.createOfflineRegion(definition, metadata,
                new OfflineManager.CreateOfflineRegionCallback() {
                    @Override
                    public void onCreate(OfflineRegion offlineRegion) {
                        Timber.d("Offline region created: %s", regionName);
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
                        view.navigateToMapsList();
                    }

                    @Override
                    public void onError(String error) {
                        Timber.e("Error: %s", error);
                        view.showOfflineAreaError();
                    }
                });
    }
}
