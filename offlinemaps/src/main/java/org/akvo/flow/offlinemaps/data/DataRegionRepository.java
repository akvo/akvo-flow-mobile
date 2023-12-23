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

package org.akvo.flow.offlinemaps.data;

//import org.akvo.flow.mapbox.offline.reactive.GetOfflineRegion;
//import org.akvo.flow.mapbox.offline.reactive.GetOfflineRegions;
import org.akvo.flow.offlinemaps.domain.RegionRepository;
import org.akvo.flow.offlinemaps.domain.entity.DomainOfflineArea;
import org.akvo.flow.offlinemaps.domain.entity.DomainOfflineAreaMapper;
import org.akvo.flow.offlinemaps.domain.entity.MapInfo;
import org.akvo.flow.offlinemaps.domain.entity.MapInfoMapper;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;

public class DataRegionRepository implements RegionRepository {

//    private final GetOfflineRegions getOfflineRegions;
    private final DomainOfflineAreaMapper mapper;
//    private final GetOfflineRegion getOfflineRegion;
    private final MapInfoMapper mapInfoMapper;

    public DataRegionRepository(/*GetOfflineRegions getOfflineRegions,*/ DomainOfflineAreaMapper mapper,
            /*GetOfflineRegion getOfflineRegion,*/ MapInfoMapper mapInfoMapper) {
//        this.getOfflineRegions = getOfflineRegions;
        this.mapper = mapper;
//        this.getOfflineRegion = getOfflineRegion;
        this.mapInfoMapper = mapInfoMapper;
    }

    @Override
    public Single<List<DomainOfflineArea>> getOfflineRegions() {
//        return getOfflineRegions.execute().map(mapper::transform);
        return null;
    }

    @Override
    public Maybe<MapInfo> getOfflineRegion(Long regionId) {
//        return getOfflineRegion.execute(regionId).map(mapInfoMapper::getMapInfo);
        return null;
    }
}
