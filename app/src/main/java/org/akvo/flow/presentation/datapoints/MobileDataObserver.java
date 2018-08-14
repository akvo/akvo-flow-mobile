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

package org.akvo.flow.presentation.datapoints;

import org.akvo.flow.domain.interactor.DefaultObserver;

import timber.log.Timber;

public abstract class MobileDataObserver extends DefaultObserver<Boolean> {

    @Override
    public void onError(Throwable e) {
        Timber.e(e); //should not happen
    }

    @Override
    public void onNext(Boolean allowed) {
        if (allowed == null || !allowed) {
            onMobileDataNotAllowed();
        } else {
            onMobileDataAllowed();
        }
    }

    protected abstract void onMobileDataNotAllowed();

    protected abstract void onMobileDataAllowed();
}
