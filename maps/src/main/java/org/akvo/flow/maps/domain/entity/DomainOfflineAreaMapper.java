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

import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import org.akvo.flow.mapbox.offline.reactive.RegionNameMapper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import kotlin.Pair;

public class DomainOfflineAreaMapper {

    private static final long MEGABYTE = 1024L * 1024L;

    private final RegionNameMapper regionNameMapper;
    private final MapInfoMapper mapInfoMapper;

    @Inject
    public DomainOfflineAreaMapper(RegionNameMapper regionNameMapper, MapInfoMapper mapInfoMapper) {
        this.regionNameMapper = regionNameMapper;
        this.mapInfoMapper = mapInfoMapper;
    }

    public DomainOfflineArea transform(OfflineRegion region, OfflineRegionStatus status) {
        return new DomainOfflineArea(region.getID(),
                regionNameMapper.getRegionName(region),
                status.getCompletedResourceSize() / MEGABYTE + " MB",
                status.getDownloadState() == OfflineRegion.STATE_ACTIVE, status.isComplete(),
                mapInfoMapper.getMapInfo(region));
    }

    @NonNull
    public List<DomainOfflineArea> transform(List<Pair<OfflineRegion, OfflineRegionStatus>> pairs) {
        List<DomainOfflineArea> areas = new ArrayList<>();
        for (Pair<OfflineRegion, OfflineRegionStatus> p: pairs ) {
            areas.add(transform(p.getFirst(), p.getSecond()));
        }
        return areas;
    }
}
