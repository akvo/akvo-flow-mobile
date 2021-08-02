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

package org.akvo.flow.maps.presentation.list;

import org.akvo.flow.mapbox.offline.reactive.DeleteOfflineRegion;
import org.akvo.flow.mapbox.offline.reactive.RenameOfflineRegion;
import org.akvo.flow.maps.domain.entity.DomainOfflineArea;
import org.akvo.flow.maps.domain.interactor.GetSelectedOfflineRegionId;
import org.akvo.flow.maps.domain.interactor.LoadOfflineRegions;
import org.akvo.flow.maps.domain.interactor.SaveSelectedOfflineArea;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

public class OfflineAreasListPresenter {

    private final LoadOfflineRegions loadOfflineRegions;
    private final CompositeDisposable disposables;
    private final RenameOfflineRegion renameOfflineRegion;
    private final DeleteOfflineRegion deleteOfflineRegion;
    private final SaveSelectedOfflineArea saveSelectedOfflineArea;
    private final GetSelectedOfflineRegionId getSelectedOfflineRegion;

    private OfflineAreasListView view;

    @Inject
    public OfflineAreasListPresenter(RenameOfflineRegion renameOfflineRegion,
            DeleteOfflineRegion deleteOfflineRegion,
            SaveSelectedOfflineArea saveSelectedOfflineArea,
            GetSelectedOfflineRegionId getSelectedOfflineRegion,
            LoadOfflineRegions loadOfflineRegions) {
        this.loadOfflineRegions = loadOfflineRegions;
        this.renameOfflineRegion = renameOfflineRegion;
        this.deleteOfflineRegion = deleteOfflineRegion;
        this.saveSelectedOfflineArea = saveSelectedOfflineArea;
        this.getSelectedOfflineRegion = getSelectedOfflineRegion;
        disposables = new CompositeDisposable();
    }

    public void destroy() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
        saveSelectedOfflineArea.dispose();
        getSelectedOfflineRegion.dispose();
        loadOfflineRegions.dispose();
    }

    public void setView(OfflineAreasListView view) {
        this.view = view;
    }

    public void loadAreas() {
        view.showLoading();
        loadOfflineRegions.execute(new DisposableSingleObserver<List<DomainOfflineArea>>() {
            @Override
            public void onSuccess(List<DomainOfflineArea> domainOfflineAreas) {
                view.hideLoading();
                if (domainOfflineAreas.size() == 0) {
                    view.displayNoOfflineMaps();
                } else {
                    fetchSelectedArea(domainOfflineAreas);
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

    private void fetchSelectedArea(List<DomainOfflineArea> regions) {
        getSelectedOfflineRegion
                .execute(new DisposableMaybeObserver<Long>() {
                    @Override
                    public void onSuccess(Long selectedRegionId) {
                        view.showOfflineRegions(regions, selectedRegionId);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                        view.showOfflineRegions(regions, DomainOfflineArea.UNSELECTED_REGION);
                    }

                    @Override
                    public void onComplete() {
                        view.showOfflineRegions(regions, DomainOfflineArea.UNSELECTED_REGION);
                    }
                });
    }

    public void renameArea(long areaId, String newName) {
        view.showLoading();
        DisposableCompletableObserver subscribeWith = renameOfflineRegion.execute(areaId, newName)
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        loadAreas();
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.hideLoading();
                        view.showRenameError();
                    }
                });
        disposables.add(subscribeWith);
    }

    public void deleteArea(long areaId) {
        view.showLoading();
        DisposableCompletableObserver subscribeWith = deleteOfflineRegion.execute(areaId)
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        getSelectedOfflineRegion.execute(new DisposableMaybeObserver<Long>() {
                            @Override
                            public void onSuccess(Long selectedAreaId) {
                                if (areaId == selectedAreaId) {
                                    resetSelectedArea();
                                } else {
                                    loadAreas();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                loadAreas();
                            }

                            @Override
                            public void onComplete() {
                                loadAreas();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.hideLoading();
                        view.showDeleteError();
                    }
                });
        disposables.add(subscribeWith);
    }

    private void resetSelectedArea() {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveSelectedOfflineArea.AREA_ID_PARAM, -1L);
        saveSelectedOfflineArea.execute(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                loadAreas();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                loadAreas();
            }
        }, params);
    }

    public void selectRegion(long regionId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveSelectedOfflineArea.AREA_ID_PARAM, regionId);
        saveSelectedOfflineArea.execute(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                // EMPTY
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.showSelectError();
                loadAreas();
            }
        }, params);
    }
}
