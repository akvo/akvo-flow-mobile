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

import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import org.akvo.flow.mapbox.offline.reactive.DeleteOfflineRegion;
import org.akvo.flow.mapbox.offline.reactive.GetOfflineRegions;
import org.akvo.flow.mapbox.offline.reactive.RenameOfflineRegion;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.datapoints.map.offline.list.entity.ListOfflineAreaMapper;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import kotlin.Pair;
import timber.log.Timber;

public class OfflineAreasListPresenter implements Presenter {

    private final ListOfflineAreaMapper mapper;
    private final CompositeDisposable disposables;
    private final GetOfflineRegions getOfflineRegions;
    private final RenameOfflineRegion renameOfflineRegion;
    private final DeleteOfflineRegion deleteOfflineRegion;

    private OfflineAreasListView view;

    @Inject
    public OfflineAreasListPresenter(ListOfflineAreaMapper mapper,
            GetOfflineRegions getOfflineRegions, RenameOfflineRegion renameOfflineRegion,
            DeleteOfflineRegion deleteOfflineRegion) {
        this.mapper = mapper;
        this.getOfflineRegions = getOfflineRegions;
        this.renameOfflineRegion = renameOfflineRegion;
        this.deleteOfflineRegion = deleteOfflineRegion;
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
        DisposableSingleObserver<List<Pair<OfflineRegion, OfflineRegionStatus>>> subscribeWith = getOfflineRegions
                .execute()
                .subscribeWith(
                        new DisposableSingleObserver<List<Pair<OfflineRegion, OfflineRegionStatus>>>() {
                            @Override
                            public void onSuccess(
                                    List<Pair<OfflineRegion, OfflineRegionStatus>> pairs) {
                                view.hideLoading();
                                if (pairs.size() == 0) {
                                    view.displayNoOfflineMaps();
                                } else {
                                    view.showOfflineRegions(mapper.transform(pairs));
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                Timber.e(e);
                                view.hideLoading();
                                view.displayNoOfflineMaps();
                            }
                        });
        disposables.add(subscribeWith);
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
                        loadAreas();
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.hideLoading();
                        view.showDeleteError();
                    }
                });
        disposables.add(subscribeWith);
    }
}
