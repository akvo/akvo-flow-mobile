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

import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.MakeDataPublic;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class PublishFilesPreferencePresenter implements Presenter {

    private final PublishedTimeHelper publishedTimeHelper;
    private final UseCase getPublishDataTime;
    private final MakeDataPublic makeDataPublic;

    private IPublishFilesPreferenceView view;

    @Inject
    public PublishFilesPreferencePresenter(PublishedTimeHelper publishedTimeHelper,
            @Named("getPublishDataTime") UseCase getPublishDataTime,
            MakeDataPublic makeDataPublic) {
        this.publishedTimeHelper = publishedTimeHelper;
        this.getPublishDataTime = getPublishDataTime;
        this.makeDataPublic = makeDataPublic;
    }

    public void setView(IPublishFilesPreferenceView view) {
        this.view = view;
    }

    public void onPublishClick() {
        //TODO: show "loading" until files are published
        makeDataPublic.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                //TODO: display error to user (other issue)
            }

            @Override
            public void onNext(Boolean published) {
                //TODO: make sure everything was published
                int progress = publishedTimeHelper
                        .getMaxPublishedTime(PublishedTimeHelper.MAX_PUBLISH_TIME_IN_MS) - 1;
                view.showPublished(progress);
                view.scheduleAlarm();
            }
        });
    }

    @Override
    public void destroy() {
        getPublishDataTime.dispose();
        makeDataPublic.dispose();
    }

    public void load() {
        getPublishDataTime.execute(new DefaultObserver<Long>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.showUnPublished();
            }

            @Override
            public void onNext(Long publishTime) {
                long timeSincePublished = publishedTimeHelper
                        .calculateTimeSincePublished(publishTime);
                if (timeSincePublished < PublishedTimeHelper.MAX_PUBLISH_TIME_IN_MS) {
                    view.showPublished(publishedTimeHelper.getMaxPublishedTime(timeSincePublished));
                } else {
                    view.showUnPublished();
                }
            }
        }, null);
    }
}
