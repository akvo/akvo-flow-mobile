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

package org.akvo.flow.walkthrough.presentation;

import org.akvo.flow.walkthrough.domain.interactor.SetWalkThroughSeen;

import javax.inject.Inject;

import io.reactivex.observers.DisposableCompletableObserver;
import timber.log.Timber;

public class OfflineMapsWalkThroughPresenter {

    private final SetWalkThroughSeen walkThroughSeen;

    @Inject
    public OfflineMapsWalkThroughPresenter(SetWalkThroughSeen walkThroughSeen) {
        this.walkThroughSeen = walkThroughSeen;
    }

    public void setWalkThroughSeen() {
        walkThroughSeen.execute(new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                // EMPTY
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }
        });
    }

    public void destroy() {
        walkThroughSeen.dispose();
    }
}
