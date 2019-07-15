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

package org.akvo.flow.injector.module;

import android.content.Context;
import android.content.SharedPreferences;

import org.akvo.flow.mapbox.offline.reactive.GetOfflineRegion;
import org.akvo.flow.mapbox.offline.reactive.GetOfflineRegions;
import org.akvo.flow.mapbox.offline.reactive.RegionNameMapper;
import org.akvo.flow.offlinemaps.data.DataPreferencesRepository;
import org.akvo.flow.offlinemaps.data.DataRegionRepository;
import org.akvo.flow.offlinemaps.data.OfflineSharedPreferenceDataSource;
import org.akvo.flow.offlinemaps.domain.PreferencesRepository;
import org.akvo.flow.offlinemaps.domain.entity.DomainOfflineAreaMapper;
import org.akvo.flow.offlinemaps.domain.entity.MapInfoMapper;
import org.akvo.flow.offlinemaps.domain.interactor.GetSelectedOfflineMapInfo;
import org.akvo.flow.offlinemaps.domain.interactor.GetSelectedOfflineRegionId;

import dagger.Module;
import dagger.Provides;

@Module
public class OfflineModule {

    @Provides
    GetSelectedOfflineRegionId provideGetSelectedOfflineAreaId(
            PreferencesRepository preferenceRepository) {
        return new GetSelectedOfflineRegionId(preferenceRepository);
    }

    @Provides
    GetSelectedOfflineMapInfo provideGetSelectedOfflineMapInfo(Context context,
            PreferencesRepository preferenceRepository) {
        MapInfoMapper mapInfoMapper = new MapInfoMapper();
        RegionNameMapper regionNameMapper = new RegionNameMapper();
        DataRegionRepository regionRepository = new DataRegionRepository(
                new GetOfflineRegions(context),
                new DomainOfflineAreaMapper(regionNameMapper, mapInfoMapper),
                new GetOfflineRegion(context), mapInfoMapper);
        return new GetSelectedOfflineMapInfo(preferenceRepository, regionRepository);
    }

    @Provides
    PreferencesRepository providePreferencesRepository(Context context) {
        SharedPreferences offlinePrefs = context.getApplicationContext()
                .getSharedPreferences("offline_prefs", Context.MODE_PRIVATE);
        return new DataPreferencesRepository(new OfflineSharedPreferenceDataSource(offlinePrefs));
    }
}
