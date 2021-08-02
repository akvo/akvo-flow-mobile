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

package org.akvo.flow.maps.domain.entity;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionDefinition;

import javax.inject.Inject;

import androidx.annotation.Nullable;

public class MapInfoMapper {

    @Inject
    public MapInfoMapper() {
    }

    @Nullable
    public MapInfo getMapInfo(@Nullable OfflineRegion region) {
        if (region == null) {
            return null;
        }
        OfflineRegionDefinition definition = region.getDefinition();
        LatLng center = definition.getBounds().getCenter();
        return new MapInfo(center.getLatitude(), center.getLongitude(), getZoom(definition));
    }

    private double getZoom(OfflineRegionDefinition definition) {
        return definition.getMinZoom() + MapInfo.ZOOM_MAX;
    }
}