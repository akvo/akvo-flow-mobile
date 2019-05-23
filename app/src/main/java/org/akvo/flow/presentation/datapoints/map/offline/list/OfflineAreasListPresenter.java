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

package org.akvo.flow.presentation.datapoints.map.offline.list;

import android.util.Pair;

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.map.offline.RegionNameMapper;
import org.akvo.flow.presentation.datapoints.map.offline.list.entity.ListOfflineAreaMapper;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

public class OfflineAreasListPresenter implements Presenter {

    private final OfflineManager offlineManager;
    private final ListOfflineAreaMapper mapper;
    private final RegionNameMapper regionNameMapper;
    private final CompositeDisposable disposables;

    private OfflineAreasListView view;
    private OfflineRegion[] offlineRegions;

    @Inject
    public OfflineAreasListPresenter(OfflineManager offlineManager,
            ListOfflineAreaMapper mapper, RegionNameMapper regionNameMapper) {
        this.offlineManager = offlineManager;
        this.mapper = mapper;
        this.regionNameMapper = regionNameMapper;
        disposables = new CompositeDisposable();
    }

    @Override
    public void destroy() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    public void setView(OfflineAreasListView view) {
        this.view = view;
    }

    public void loadAreas() {
        view.showLoading();
        DisposableSingleObserver<OfflineRegion[]> singleObserver = getOfflineRegions()
                .subscribeWith(new DisposableSingleObserver<OfflineRegion[]>() {
                    @Override
                    public void onSuccess(OfflineRegion[] offlineRegions) {
                        if (offlineRegions != null && offlineRegions.length > 0) {
                            subscribeToOfflineRegionsStatus(offlineRegions);
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
        disposables.add(singleObserver);
    }

    private void subscribeToOfflineRegionsStatus(OfflineRegion[] offlineRegions) {
        DisposableSingleObserver observer = checkOfflineRegionsStatus(offlineRegions)
                .subscribeWith(
                        new DisposableSingleObserver<List<Pair<OfflineRegion, OfflineRegionStatus>>>() {

                            @Override
                            public void onSuccess(
                                    List<Pair<OfflineRegion, OfflineRegionStatus>> pairs) {
                                view.hideLoading();
                                view.showOfflineRegions(mapper.transform(pairs));
                            }

                            @Override
                            public void onError(Throwable e) {
                                view.hideLoading();
                                Timber.e(e);
                                //TODO: show error
                            }
                        });
        disposables.add(observer);
    }

    private Single<OfflineRegion[]> getOfflineRegions() {
        return Single.create(emitter -> offlineManager
                .listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                    @Override
                    public void onList(OfflineRegion[] offlineRegions) {
                        OfflineAreasListPresenter.this.offlineRegions = offlineRegions;
                        emitter.onSuccess(offlineRegions);
                    }

                    @Override
                    public void onError(String error) {
                        emitter.onError(new Exception(error));
                    }
                }));
    }

    private Single<List<Pair<OfflineRegion, OfflineRegionStatus>>> checkOfflineRegionsStatus(
            OfflineRegion[] offlineRegions) {
        return Observable.fromArray(offlineRegions)
                .flatMap(new Function<OfflineRegion, Observable<Pair<OfflineRegion, OfflineRegionStatus>>>() {
                    @Override
                    public Observable<Pair<OfflineRegion, OfflineRegionStatus>> apply(OfflineRegion region) {
                        return getOfflineRegion(region).toObservable();
                    }
                })
                .toList();
    }

    private Single<Pair<OfflineRegion, OfflineRegionStatus>> getOfflineRegion(OfflineRegion region) {
        return Single.create(emitter -> region.getStatus(
                new OfflineRegion.OfflineRegionStatusCallback() {
                    @Override
                    public void onStatus(OfflineRegionStatus status) {
                        emitter.onSuccess(new Pair<>(region, status));
                    }

                    @Override
                    public void onError(String error) {
                        emitter.onError(new Exception(error));
                    }
                }));
    }

    public void renameArea(long areaId, String newName) {
        OfflineRegion region = findOfflineRegion(areaId);
        if (region != null) {
            view.showLoading();
            region.updateMetadata(regionNameMapper.getRegionMetadata(newName),
                    new OfflineRegion.OfflineRegionUpdateMetadataCallback() {
                        @Override
                        public void onUpdate(byte[] metadata) {
                            view.hideLoading();
                            view.displayUpdatedName(areaId, newName);
                        }

                        @Override
                        public void onError(String error) {
                            view.hideLoading();
                            view.showRenameError();
                        }
                    });
        }
    }

    @Nullable
    private OfflineRegion findOfflineRegion(long areaId) {
        if (offlineRegions != null) {
            for (OfflineRegion r: offlineRegions) {
                if (r.getID() == areaId) {
                    return r;
                }
            }
        }
        return null;
    }
}
