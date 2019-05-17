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

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.map.offline.list.entity.ListOfflineArea;
import org.akvo.flow.presentation.datapoints.map.offline.list.entity.ListOfflineAreaMapper;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

public class OfflineAreasListPresenter implements Presenter {

    private final OfflineManager offlineManager;
    private final ListOfflineAreaMapper mapper;
    private final CompositeDisposable disposables;

    private OfflineAreasListView view;

    @Inject
    public OfflineAreasListPresenter(OfflineManager offlineManager,
            ListOfflineAreaMapper mapper) {
        this.offlineManager = offlineManager;
        this.mapper = mapper;
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
                        view.hideLoading();
                        if (offlineRegions != null && offlineRegions.length > 0) {
                            DisposableObserver observer = checkOfflineRegionsStatus(offlineRegions)
                                    .subscribeWith(
                                            new DefaultObserver<List<ListOfflineArea>>() {
                                                @Override
                                                public void onNext(
                                                        List<ListOfflineArea> viewOfflineAreas) {
                                                    view.showOfflineRegions(viewOfflineAreas);
                                                }

                                                @Override
                                                public void onError(Throwable e) {
                                                    Timber.e(e);
                                                    //TODO: show error
                                                }
                                            });
                            disposables.add(observer);
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

    private Single<OfflineRegion[]> getOfflineRegions() {
        return Single.create(emitter -> offlineManager
                .listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                    @Override
                    public void onList(OfflineRegion[] offlineRegions) {
                        emitter.onSuccess(offlineRegions);
                    }

                    @Override
                    public void onError(String error) {
                        emitter.onError(new Exception(error));
                    }
                }));
    }

    private Observable<List<ListOfflineArea>> checkOfflineRegionsStatus(
            OfflineRegion[] offlineRegions) {
        return Observable.fromArray(offlineRegions)
                .flatMap(new Function<OfflineRegion, Observable<ListOfflineArea>>() {
                    @Override
                    public Observable<ListOfflineArea> apply(OfflineRegion region) {
                        return getOfflineRegion(region).toObservable();
                    }
                })
                .toList()
                .toObservable();
    }

    private Single<ListOfflineArea> getOfflineRegion(OfflineRegion region) {
        return Single.create(emitter -> region.getStatus(
                new OfflineRegion.OfflineRegionStatusCallback() {
                    @Override
                    public void onStatus(OfflineRegionStatus status) {
                        emitter.onSuccess(mapper.transform(region, status));
                    }

                    @Override
                    public void onError(String error) {
                        emitter.onError(new Exception(error));
                    }
                }));
    }
}
