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

import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionDefinition;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;

public class OfflineRegionMapper {

    private final RegionNameMapper regionNameMapper;

    private static final long  MEGABYTE = 1024L * 1024L;

    @Inject
    public OfflineRegionMapper(RegionNameMapper regionNameMapper) {
        this.regionNameMapper = regionNameMapper;
    }

    public ViewOfflineArea transform(@NonNull OfflineRegion region) {
        OfflineRegionDefinition definition = region.getDefinition();
        return new ViewOfflineArea(regionNameMapper.getRegionName(region),
                definition.getBounds(), definition.getMinZoom(), false, "0 MB");
    }

    public List<ViewOfflineArea> transform(@NonNull OfflineRegion[] regions) {
        List<ViewOfflineArea> offlineAreas = new ArrayList<>();
        for (OfflineRegion r : regions) {
            offlineAreas.add(transform(r));
        }
        return offlineAreas;
    }

    public ViewOfflineArea transform(OfflineRegion region, OfflineRegionStatus status) {
        OfflineRegionDefinition definition = region.getDefinition();
        return new ViewOfflineArea(regionNameMapper.getRegionName(region),
                definition.getBounds(),
                definition.getMinZoom(),
                status.getDownloadState() == OfflineRegion.STATE_ACTIVE,
                status.getCompletedResourceSize() / MEGABYTE + " MB");
    }
}
