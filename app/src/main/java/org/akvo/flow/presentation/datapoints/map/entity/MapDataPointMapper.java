/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.datapoints.map.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.presentation.datapoints.DisplayNameMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class MapDataPointMapper {

    private final DisplayNameMapper displayNameMapper;

    @Inject
    public MapDataPointMapper(DisplayNameMapper displayNameMapper) {
        this.displayNameMapper = displayNameMapper;
    }

    @Nullable
    private MapDataPoint transform(@Nullable DataPoint dataPoint) {
        if (dataPoint == null || dataPoint.getLatitude() == null
                || dataPoint.getLongitude() == null) {
            //a map datapoint needs to have location data or it will not be displayed
            // so no need to add it
            return null;
        }
        String displayName = displayNameMapper.createDisplayName(dataPoint.getName());
        return new MapDataPoint(dataPoint.getId(), displayName, dataPoint.getLatitude(),
                dataPoint.getLongitude());
    }

    @NonNull
    public List<MapDataPoint> transform(@Nullable List<DataPoint> dataPoints) {
        if (dataPoints == null) {
            return Collections.emptyList();
        }
        List<MapDataPoint> mapDataPoints = new ArrayList<>(dataPoints.size());
        for (DataPoint dataPoint : dataPoints) {
            MapDataPoint mapDataPoint = transform(dataPoint);
            if (mapDataPoint != null) {
                mapDataPoints.add(mapDataPoint);
            }
        }
        return mapDataPoints;
    }
}
