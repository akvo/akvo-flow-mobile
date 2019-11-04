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

package org.akvo.flow.offlinemaps.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.mapbox.mapboxsdk.offline.OfflineManager;

import org.akvo.flow.mapbox.offline.reactive.DeleteOfflineRegion;
import org.akvo.flow.mapbox.offline.reactive.GetOfflineRegion;
import org.akvo.flow.mapbox.offline.reactive.GetOfflineRegions;
import org.akvo.flow.mapbox.offline.reactive.RegionNameMapper;
import org.akvo.flow.mapbox.offline.reactive.RenameOfflineRegion;
import org.akvo.flow.offlinemaps.data.DataPreferencesRepository;
import org.akvo.flow.offlinemaps.data.DataRegionRepository;
import org.akvo.flow.offlinemaps.data.OfflineSharedPreferenceDataSource;
import org.akvo.flow.offlinemaps.domain.PreferencesRepository;
import org.akvo.flow.offlinemaps.domain.RegionRepository;
import org.akvo.flow.offlinemaps.domain.entity.DomainOfflineAreaMapper;
import org.akvo.flow.offlinemaps.domain.entity.MapInfoMapper;
import org.akvo.flow.offlinemaps.domain.interactor.GetSelectedOfflineMapInfo;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class OfflineFeatureModule {

    private final Application application;

    public OfflineFeatureModule(Application application) {
        this.application = application;
    }

    @Singleton
    @Provides
    Context provideContext() {
        return application;
    }

    @Singleton
    @Provides
    RegionNameMapper regionNameMapper() {
        return new RegionNameMapper();
    }

    @Singleton
    @Provides
    SharedPreferences providesSharedPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences("offline_prefs", Context.MODE_PRIVATE);
    }

    @Singleton
    @Provides
    PreferencesRepository providePreferencesRepository(
            OfflineSharedPreferenceDataSource dataSource) {
        return new DataPreferencesRepository(dataSource);
    }

    @Singleton
    @Provides
    GetOfflineRegion provideGetOfflineRegion(Context context) {
        return new GetOfflineRegion(context);
    }

    @Singleton
    @Provides
    GetOfflineRegions provideGetOfflineRegions(Context context) {
        return new GetOfflineRegions(context);
    }

    @Singleton
    @Provides
    RegionRepository providesRegionRepository(MapInfoMapper mapInfoMapper,
            DomainOfflineAreaMapper domainOfflineAreaMapper, GetOfflineRegion getOfflineRegion,
            GetOfflineRegions getOfflineRegions) {
        return new DataRegionRepository(getOfflineRegions, domainOfflineAreaMapper,
                getOfflineRegion, mapInfoMapper);
    }

    @Singleton
    @Provides
    RenameOfflineRegion providesRenameOfflineRegion(Context context,
            RegionNameMapper regionNameMapper) {
        return new RenameOfflineRegion(context, regionNameMapper);
    }

    @Singleton
    @Provides
    DeleteOfflineRegion providesDeleteOfflineRegion(Context context) {
        return new DeleteOfflineRegion(context);
    }

    @Singleton
    @Provides
    OfflineManager providesOfflineManager(Context context) {
        return OfflineManager.getInstance(context);
    }

    @Provides
    GetSelectedOfflineMapInfo provideGetSelectedOfflineMapInfo(
            PreferencesRepository preferenceRepository, RegionRepository regionRepository) {
        return new GetSelectedOfflineMapInfo(preferenceRepository, regionRepository);
    }
}
