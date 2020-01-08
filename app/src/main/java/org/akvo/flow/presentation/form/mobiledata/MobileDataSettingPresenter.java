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

package org.akvo.flow.presentation.form.mobiledata;

import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.SaveEnableMobileData;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class MobileDataSettingPresenter implements Presenter {

    private final UseCase saveEnableMobileData;

    private MobileDataSettingView view;

    public void setView(MobileDataSettingView view) {
        this.view = view;
    }

    @Inject
    public MobileDataSettingPresenter(@Named("saveEnableMobileData") UseCase saveEnableMobileData) {
        this.saveEnableMobileData = saveEnableMobileData;
    }

    public void saveEnableMobileData(boolean enable) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveEnableMobileData.PARAM_ENABLE_MOBILE_DATA, enable);
        saveEnableMobileData.execute(new DefaultObserver<Boolean>() {

            @Override
            public void onNext(Boolean aBoolean) {
                view.dismissView();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.dismissView();
            }
        }, params);
    }

    @Override
    public void destroy() {
        saveEnableMobileData.dispose();
    }
}
