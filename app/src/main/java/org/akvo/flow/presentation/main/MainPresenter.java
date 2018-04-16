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

import javax.inject.Inject;
import javax.inject.Named;

public class MainPresenter implements Presenter {

    private final UseCase isDeviceSetup;

    private MainView view;

    @Inject
    public MainPresenter(@Named("getIsDeviceSetUp") UseCase isDeviceSetup) {
        this.isDeviceSetup = isDeviceSetup;
    }

    public void setView(MainView view) {
        this.view = view;
    }

    @Override
    public void destroy() {
        isDeviceSetup.dispose();
    }

    public void checkDeviceSetup() {
        isDeviceSetup.execute(new DefaultObserver<Boolean>(){
            @Override
            public void onError(Throwable e) {
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
}
