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

package org.akvo.flow.maps.presentation;

import org.akvo.flow.maps.domain.entity.MapInfo;
import org.akvo.flow.maps.domain.interactor.GetSelectedOfflineMapInfo;

import javax.inject.Inject;

import io.reactivex.observers.DisposableMaybeObserver;
import timber.log.Timber;

public class MapBoxMapPresenter {

    private final GetSelectedOfflineMapInfo getSelectedOfflineMapInfo;

    private MapBoxMapItemListView view;

    @Inject
    public MapBoxMapPresenter(GetSelectedOfflineMapInfo getSelectedOfflineMapInfo) {
        this.getSelectedOfflineMapInfo = getSelectedOfflineMapInfo;
    }

    public void setView(MapBoxMapItemListView mapboxMapView) {
        this.view = mapboxMapView;
    }

    public void loadOfflineSettings() {
        getSelectedOfflineMapInfo.execute(new DisposableMaybeObserver<MapInfo>() {
            @Override
            public void onSuccess(MapInfo mapInfo) {
                view.displayOfflineMap(mapInfo);
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.displayUserLocation();
            }

            @Override
            public void onComplete() {
                view.displayUserLocation();
            }
        });
    }

    public void destroy() {
        getSelectedOfflineMapInfo.dispose();
    }
}
