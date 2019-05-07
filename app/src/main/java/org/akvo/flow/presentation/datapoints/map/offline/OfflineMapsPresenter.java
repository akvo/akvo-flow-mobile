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

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.akvo.flow.domain.entity.OfflineArea;
import org.akvo.flow.domain.entity.Optional;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class OfflineMapsPresenter implements Presenter {

    private OfflineMapsView view;

    private final OfflineRegionMapper mapper;
    private final OfflineManager offlineManager;
    private final UseCase getSelectedOfflineAre;
    private final OfflineAreaMapper offlineAreaMapper;

    @Inject
    public OfflineMapsPresenter(OfflineRegionMapper mapper, OfflineManager offlineManager,
            @Named("getSelectedOfflineArea") UseCase getSelectedOfflineAre,
            OfflineAreaMapper offlineAreaMapper) {
        this.mapper = mapper;
        this.offlineManager = offlineManager;
        this.getSelectedOfflineAre = getSelectedOfflineAre;
        this.offlineAreaMapper = offlineAreaMapper;
    }

    @Override
    public void destroy() {
        getSelectedOfflineAre.dispose();
    }

    public void setView(OfflineMapsView view) {
        this.view = view;
    }

    public void load() {
        view.showLoading();
        //TODO: make sure we "unsubscribe"
        offlineManager.listOfflineRegions(
                new OfflineManager.ListOfflineRegionsCallback() {
                    @Override
                    public void onList(OfflineRegion[] offlineRegions) {
                        view.hideLoading();
                        if (offlineRegions != null && offlineRegions.length > 0) {
                            checkSelectedRegion(offlineRegions);
                        } else {
                            view.displayNoOfflineMaps();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Timber.e(error);
                        view.hideLoading();
                        view.displayNoOfflineMaps();
                    }
                });
    }

    private void checkSelectedRegion(OfflineRegion[] offlineRegions) {
        getSelectedOfflineAre.execute(new DefaultObserver<Optional<OfflineArea>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.displayRegions(mapper.transform(offlineRegions), null);
            }

            @Override
            public void onNext(Optional<OfflineArea> offlineAreaOptional) {
                view.displayRegions(mapper.transform(offlineRegions),
                        offlineAreaMapper.transform(offlineAreaOptional));
            }
        }, null);

    }

    public void onOnlineMapSelected() {
        //setup selected maps to online
        view.dismiss();
    }
}
