/*
 * Copyright (C) 2017,2019-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.datapoints.list.entity;

import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.presentation.datapoints.DisplayNameMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ListDataPointMapper {

    private final DisplayNameMapper displayNameMapper;

    @Inject
    public ListDataPointMapper(DisplayNameMapper displayNameMapper) {
        this.displayNameMapper = displayNameMapper;
    }

    @Nullable
    private ListDataPoint transform(@Nullable DataPoint dataPoint) {
        if (dataPoint == null) {
            return null;
        }
        String displayName = displayNameMapper.createDisplayName(dataPoint.getName());
        double latitude = dataPoint.getLatitude() == null ? ListDataPoint.INVALID_COORDINATE : dataPoint.getLatitude();
        double longitude = dataPoint.getLongitude() == null? ListDataPoint.INVALID_COORDINATE: dataPoint.getLongitude();
        long lastModified = dataPoint.getLastModified();
        return new ListDataPoint(displayName, dataPoint.getStatus(), dataPoint.getId(),
                latitude, longitude, lastModified, dataPoint.wasViewed());
    }

    @NonNull
    public List<ListDataPoint> transform(List<DataPoint> dataPoints) {
        if (dataPoints == null) {
            return Collections.emptyList();
        }
        List<ListDataPoint> listDataPoints = new ArrayList<>(dataPoints.size());
        for (DataPoint dataPoint : dataPoints) {
            ListDataPoint listDataPoint = transform(dataPoint);
            if (listDataPoint != null) {
                listDataPoints.add(listDataPoint);
            }
        }
        return listDataPoints;
    }
}
