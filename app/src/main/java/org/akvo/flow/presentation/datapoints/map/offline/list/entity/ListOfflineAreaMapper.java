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

package org.akvo.flow.presentation.datapoints.map.offline.list.entity;

import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import org.akvo.flow.presentation.datapoints.map.offline.RegionNameMapper;

import javax.inject.Inject;

public class ListOfflineAreaMapper {

    private static final long MEGABYTE = 1024L * 1024L;

    private final RegionNameMapper regionNameMapper;

    @Inject
    public ListOfflineAreaMapper(RegionNameMapper regionNameMapper) {
        this.regionNameMapper = regionNameMapper;
    }

    public ListOfflineArea transform(OfflineRegion region, OfflineRegionStatus status) {
        return new ListOfflineArea(region.getID(),
                regionNameMapper.getRegionName(region),
                status.getCompletedResourceSize() / MEGABYTE + " MB",
                status.getDownloadState() == OfflineRegion.STATE_ACTIVE, status.isComplete());
    }
}
