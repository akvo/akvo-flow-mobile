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

package org.akvo.flow.presentation.datapoints.map.offline.selection;

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.map.offline.RegionNameMapper;

import javax.inject.Inject;

import timber.log.Timber;

public class OfflineMapDownloadPresenter implements Presenter {

    private final OfflineManager offlineManager;
    private final RegionNameMapper regionNameMapper;
    private OfflineMapDownloadView view;

    @Inject
    public OfflineMapDownloadPresenter(OfflineManager offlineManager,
            RegionNameMapper regionNameMapper) {
        this.offlineManager = offlineManager;
        this.regionNameMapper = regionNameMapper;
    }

    @Override
    public void destroy() {
        //EMPTY
    }

    public void setView(OfflineMapDownloadView view) {
        this.view = view;
    }

    public void downloadArea(OfflineTilePyramidRegionDefinition definition, String regionName) {
        view.showProgress();
        byte[] metadata = regionNameMapper.getRegionMetadata(regionName);

        // Create the offline region and launch the download
        offlineManager.createOfflineRegion(definition, metadata,
                new OfflineManager.CreateOfflineRegionCallback() {
                    @Override
                    public void onCreate(OfflineRegion offlineRegion) {
                        Timber.d("Offline region created: %s", regionName);
                        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
                            @Override
                            public void onStatusChanged(OfflineRegionStatus status) {
                                // Compute a percentage
                                double percentage = status.getRequiredResourceCount() >= 0
                                        ?
                                        (100.0 * status.getCompletedResourceCount() / status
                                                .getRequiredResourceCount()) : 0.0;

                                if (status.isComplete()) {
                                    // Download complete
                                    view.hideProgress();
                                } else if (status.isRequiredResourceCountPrecise()) {
                                    // Switch to determinate state
                                    view.updateProgress((int) Math.round(percentage));
                                    // Log what is being currently downloaded
                                    Timber.d("%s/%s resources; %s bytes downloaded.",
                                            String.valueOf(status.getCompletedResourceCount()),
                                            String.valueOf(status.getRequiredResourceCount()),
                                            String.valueOf(status.getCompletedResourceSize()));
                                }
                            }

                            @Override
                            public void onError(OfflineRegionError error) {
                                Timber.e("onError reason: %s", error.getReason());
                                Timber.e("onError message: %s", error.getMessage());
                            }

                            @Override
                            public void mapboxTileCountLimitExceeded(long limit) {
                                Timber.e("Mapbox tile count limit exceeded: %s", limit);
                                //TODO: notify user
                            }
                        });

                        // Change the region state
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
                    }

                    @Override
                    public void onError(String error) {
                        Timber.e("Error: %s", error);
                    }
                });

    }

}
