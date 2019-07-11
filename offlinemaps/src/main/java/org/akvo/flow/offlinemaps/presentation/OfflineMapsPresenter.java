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

package org.akvo.flow.offlinemaps.presentation;

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.akvo.flow.offlinemaps.domain.GetSelectedOfflineArea;
import org.akvo.flow.offlinemaps.domain.SaveSelectedOfflineArea;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableMaybeObserver;
import timber.log.Timber;

public class OfflineMapsPresenter {

    private OfflineMapsView view;

    private final OfflineRegionMapper mapper;
    private final OfflineManager offlineManager;
    private final GetSelectedOfflineArea getSelectedOfflineAre;
    private final SaveSelectedOfflineArea saveSelectedOfflineArea;

    public OfflineMapsPresenter(OfflineRegionMapper mapper, OfflineManager offlineManager,
            GetSelectedOfflineArea getSelectedOfflineAre,
            SaveSelectedOfflineArea saveSelectedOfflineArea) {
        this.mapper = mapper;
        this.offlineManager = offlineManager;
        this.getSelectedOfflineAre = getSelectedOfflineAre;
        this.saveSelectedOfflineArea = saveSelectedOfflineArea;
    }

    public void destroy() {
        getSelectedOfflineAre.dispose();
        saveSelectedOfflineArea.dispose();
    }

    public void setView(OfflineMapsView view) {
        this.view = view;
    }

    public void load() {
        view.showLoading();
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
        getSelectedOfflineAre.execute(new DisposableMaybeObserver<Long>() {
            @Override
            public void onSuccess(Long offlineAreaId) {
                view.displayRegions(mapper.transform(offlineRegions), offlineAreaId);
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.displayRegions(mapper.transform(offlineRegions),
                        ViewOfflineArea.UNSELECTED_REGION);
            }

            @Override
            public void onComplete() {
                // no regions found
                view.displayRegions(mapper.transform(offlineRegions),
                        ViewOfflineArea.UNSELECTED_REGION);
            }

        });
    }

    public void onOnlineMapSelected() {
        //setup selected maps to online
        saveSelectedArea(null);
    }

    public void onOfflineAreaSelected(ViewOfflineArea offlineArea) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveSelectedOfflineArea.AREA_ID_PARAM,
                offlineArea == null ? ViewOfflineArea.UNSELECTED_REGION : offlineArea.getId());
        saveSelectedArea(params);
    }

    private void saveSelectedArea(Map<String, Object> parameters) {
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
        }, parameters);
    }
}
