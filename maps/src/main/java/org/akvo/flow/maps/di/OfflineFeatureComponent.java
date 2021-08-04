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

package org.akvo.flow.maps.di;

import org.akvo.flow.maps.presentation.MapBoxMapItemListViewImpl;
import org.akvo.flow.maps.presentation.dialog.OfflineMapsDialog;
import org.akvo.flow.maps.presentation.download.OfflineMapDownloadActivity;
import org.akvo.flow.maps.presentation.geoshapes.GeoShapesMapViewImpl;
import org.akvo.flow.maps.presentation.list.OfflineAreasListActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@PerFeature
@Component(modules = OfflineFeatureModule.class)
public interface OfflineFeatureComponent {

    void inject(OfflineAreasListActivity activity);

    void inject(OfflineMapDownloadActivity activity);

    void inject(OfflineMapsDialog dialog);

    void inject(MapBoxMapItemListViewImpl flowMapView);

    void inject(GeoShapesMapViewImpl geoShapesMapView);
}
