/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.main;

import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.walkthrough.domain.interactor.GetWalkThroughSeen;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

public class MainPresenter implements Presenter {

    private final UseCase isDeviceSetup;
    private final GetWalkThroughSeen wasWalkThroughSeen;

    private MainView view;

    @Inject
    public MainPresenter(@Named("getIsDeviceSetUp") UseCase isDeviceSetup,
            GetWalkThroughSeen getWalkThroughSeen) {
        this.isDeviceSetup = isDeviceSetup;
        this.wasWalkThroughSeen = getWalkThroughSeen;
    }

    public void setView(MainView view) {
        this.view = view;
    }

    public void checkDeviceSetup() {
        isDeviceSetup.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.navigateToDeviceSetUp();
            }

            @Override
            public void onNext(Boolean setUp) {
                if (setUp) {
                    view.navigateToSurvey();
                } else {
                    view.navigateToDeviceSetUp();
                }
            }
        }, null);
    }

    @Override
    public void destroy() {
        isDeviceSetup.dispose();
        wasWalkThroughSeen.dispose();
    }

    public void checkWalkThroughDisplay() {
        wasWalkThroughSeen.execute(new DisposableSingleObserver<Boolean>() {
            @Override
            public void onSuccess(Boolean seen) {
                if (seen) {
                    checkDeviceSetup();
                } else {
                    view.navigateToWalkThrough();
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.navigateToWalkThrough();
            }
        });
    }
}
