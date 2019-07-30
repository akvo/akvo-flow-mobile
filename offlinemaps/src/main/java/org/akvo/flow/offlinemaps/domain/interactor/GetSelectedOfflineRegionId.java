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

package org.akvo.flow.offlinemaps.domain.interactor;

import org.akvo.flow.offlinemaps.domain.PreferencesRepository;

import javax.inject.Inject;

import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.schedulers.Schedulers;

public class GetSelectedOfflineRegionId {

    private final CompositeDisposable disposables;
    private final PreferencesRepository userRepository;

    @Inject
    public GetSelectedOfflineRegionId(PreferencesRepository preferencesRepository) {
        this.disposables = new CompositeDisposable();
        this.userRepository = preferencesRepository;
    }

    public void execute(DisposableMaybeObserver<Long> observer) {
        addDisposable(buildUseCaseObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(observer));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    private Maybe<Long> buildUseCaseObservable() {
        return userRepository.getSelectedOfflineArea();
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }
}
