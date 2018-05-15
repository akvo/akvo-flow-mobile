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

package org.akvo.flow.presentation.settings.publish;

import org.akvo.flow.domain.exception.FullStorageException;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class PublishFilesPreferencePresenter implements Presenter {

    private final PublishedTimeHelper publishedTimeHelper;
    private final UseCase getPublishDataTime;
    private final UseCase publishData;

    private IPublishFilesPreferenceView view;

    @Inject
    public PublishFilesPreferencePresenter(PublishedTimeHelper publishedTimeHelper,
            @Named("getPublishDataTime") UseCase getPublishDataTime,
            @Named("publishData") UseCase publishData) {
        this.publishedTimeHelper = publishedTimeHelper;
        this.getPublishDataTime = getPublishDataTime;
        this.publishData = publishData;
    }

    public void setView(IPublishFilesPreferenceView view) {
        this.view = view;
    }

    public void onPublishClick() {
        view.showLoading();
        publishData.dispose();
        publishData.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.showUnPublished();
                if (e instanceof FullStorageException) {
                    view.showNoSpaceLeftError();
                } else {
                    view.showGenericPublishError();
                }
            }

            @Override
            public void onNext(Boolean published) {
                if (published) {
                    view.scheduleAlarm();
                    load();
                } else {
                    view.showUnPublished();
                    view.showNoDataToPublish();
                }
            }
        }, null);
    }

    @Override
    public void destroy() {
        getPublishDataTime.dispose();
        publishData.dispose();
    }

    public void load() {
        getPublishDataTime.execute(new DefaultObserver<Long>() {
            @Override
            public void onError(Throwable e) {
                getPublishDataTime.dispose();
                view.showUnPublished();
                Timber.e(e);
            }

            @Override
            public void onNext(Long publishTime) {
                long timeSincePublished = publishedTimeHelper
                        .calculateTimeSincePublished(publishTime);
                if (timeSincePublished < PublishedTimeHelper.MAX_PUBLISH_TIME_IN_MS) {
                    int remainingTime = publishedTimeHelper.getRemainingPublishedTimeToDisplay(
                            timeSincePublished);
                    view.showPublished(remainingTime);
                } else {
                    getPublishDataTime.dispose();
                    view.showUnPublished();
                }
            }
        }, null);
    }
}
