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

package org.akvo.flow.maps.presentation.dialog;

import org.akvo.flow.maps.domain.entity.DomainOfflineArea;
import org.akvo.flow.maps.domain.interactor.GetSelectedOfflineRegionId;
import org.akvo.flow.maps.domain.interactor.LoadOfflineRegions;
import org.akvo.flow.maps.domain.interactor.SaveSelectedOfflineArea;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

public class OfflineMapsPresenter {

    private OfflineMapsView view;

    private final GetSelectedOfflineRegionId getSelectedOfflineAre;
    private final SaveSelectedOfflineArea saveSelectedOfflineArea;
    private final LoadOfflineRegions loadOfflineRegions;

    @Inject
    public OfflineMapsPresenter(GetSelectedOfflineRegionId getSelectedOfflineAre,
            SaveSelectedOfflineArea saveSelectedOfflineArea,
            LoadOfflineRegions loadOfflineRegions) {
        this.getSelectedOfflineAre = getSelectedOfflineAre;
        this.saveSelectedOfflineArea = saveSelectedOfflineArea;
        this.loadOfflineRegions = loadOfflineRegions;
    }

    public void destroy() {
        getSelectedOfflineAre.dispose();
        saveSelectedOfflineArea.dispose();
        loadOfflineRegions.dispose();
    }

    public void setView(OfflineMapsView view) {
        this.view = view;
    }

    public void load() {
        view.showLoading();
        loadOfflineRegions.execute(new DisposableSingleObserver<List<DomainOfflineArea>>() {
            @Override
            public void onSuccess(List<DomainOfflineArea> domainOfflineAreas) {
                view.hideLoading();
                if (domainOfflineAreas.size() > 0) {
                    checkSelectedRegion(domainOfflineAreas);
                } else {
                    view.displayNoOfflineMaps();
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.hideLoading();
                view.displayNoOfflineMaps();
            }
        });
    }

    private void checkSelectedRegion(List<DomainOfflineArea> offlineRegions) {
        getSelectedOfflineAre.execute(new DisposableMaybeObserver<Long>() {
            @Override
            public void onSuccess(Long offlineAreaId) {
                view.displayRegions(offlineRegions, offlineAreaId);
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.displayRegions(offlineRegions, DomainOfflineArea.UNSELECTED_REGION);
            }

            @Override
            public void onComplete() {
                // no regions found
                view.displayRegions(offlineRegions, DomainOfflineArea.UNSELECTED_REGION);
            }

        });
    }

    public void onOnlineMapSelected() {
        saveSelectedArea(null);
    }

    public void onOfflineAreaSelected(DomainOfflineArea offlineArea) {
        saveSelectedArea(offlineArea);
    }

    private void saveSelectedArea(DomainOfflineArea offlineArea) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveSelectedOfflineArea.AREA_ID_PARAM,
                offlineArea == null ? DomainOfflineArea.UNSELECTED_REGION : offlineArea.getId());
        saveSelectedOfflineArea.execute(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                view.notifyMapChange();
                view.dismiss();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                //TODO: notify user
            }
        }, params);
    }
}
