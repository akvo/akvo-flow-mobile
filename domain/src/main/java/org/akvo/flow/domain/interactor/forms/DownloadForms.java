/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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
 *
 */

package org.akvo.flow.domain.interactor.forms;

import org.akvo.flow.domain.repository.FormRepository;
import org.akvo.flow.domain.repository.UserRepository;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;

public class DownloadForms {

    private final FormRepository formRepository;
    private final UserRepository userRepository;
    private final CompositeDisposable disposables;

    @Inject
    protected DownloadForms(FormRepository formRepository,
            UserRepository userRepository) {
        this.formRepository = formRepository;
        this.userRepository = userRepository;
        this.disposables = new CompositeDisposable();
    }

    @SuppressWarnings("unchecked")
    public <T> void execute(DisposableObserver<T> observer) {
        final Observable<T> observable = buildUseCaseObservable();
        addDisposable(observable.subscribeWith(observer));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

    private Observable buildUseCaseObservable() {
        return userRepository.getDeviceId()
                .concatMap(new Function<String, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(String deviceId) {
                        return formRepository.downloadForms(deviceId);
                    }
                });
    }
}
