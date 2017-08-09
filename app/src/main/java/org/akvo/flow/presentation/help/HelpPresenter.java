/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.help;

import org.akvo.flow.domain.interactor.DefaultSubscriber;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class HelpPresenter implements Presenter {

    private final UseCase allowedToConnect;
    private HelpView view;

    @Inject
    public HelpPresenter(@Named("allowedToConnect") UseCase allowedToConnect) {
        this.allowedToConnect = allowedToConnect;
    }

    @Override
    public void destroy() {
        allowedToConnect.unSubscribe();
    }

    public void setView(HelpView view) {
        this.view = view;
    }

    public void load() {
        view.showProgress();
        allowedToConnect.execute(new DefaultSubscriber<Boolean>() {

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.hideProgress();
                view.displayError();
            }

            @Override
            public void onNext(Boolean mobileSyncEnabled) {
                if (mobileSyncEnabled != null && mobileSyncEnabled) {
                    view.loadWebView();
                } else {
                    view.hideProgress();
                    view.displayErrorDataSyncDisabled();
                }
            }
        }, null);
    }
}
