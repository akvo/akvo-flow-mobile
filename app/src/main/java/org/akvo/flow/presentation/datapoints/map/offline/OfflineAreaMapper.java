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

import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import org.akvo.flow.domain.entity.OfflineArea;
import org.akvo.flow.domain.entity.OfflineBounds;

import javax.inject.Inject;

import androidx.annotation.NonNull;

public class OfflineAreaMapper {

    private final OfflineBoundsMapper offlineBoundsMapper;

    @Inject
    public OfflineAreaMapper(OfflineBoundsMapper offlineBoundsMapper) {
        this.offlineBoundsMapper = offlineBoundsMapper;
    }

    @NonNull
    public ViewOfflineArea transform(OfflineArea area) {
        LatLngBounds latLngBounds = offlineBoundsMapper.transform(area.getBounds());
        return new ViewOfflineArea(area.getName(), latLngBounds, area.getZoom(), false, "0 MB");
    }

    @NonNull
    public OfflineArea transform(ViewOfflineArea area) {
        OfflineBounds latLngBounds = offlineBoundsMapper.transform(area.getBounds());
        return new OfflineArea(area.getName(), latLngBounds, area.getZoom());
    }
}
