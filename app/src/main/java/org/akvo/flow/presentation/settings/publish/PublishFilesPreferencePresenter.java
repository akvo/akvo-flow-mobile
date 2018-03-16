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

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class PublishFilesPreferencePresenter implements Presenter {

    private static final long MAX_PUBLISH_TIME_IN_MS = 90 * 60 * 1000;
    private static final long INVALID_PUBLISH_TIME = -1L;

    private IPublishFilesPreferenceView view;

    private final UseCase getPublishDataTime;
    private final MakeDataPublic makeDataPublic ;

    @Inject
    public PublishFilesPreferencePresenter(
            @Named("getPublishDataTime") UseCase getPublishDataTime,
            MakeDataPublic makeDataPublic) {
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
                view.showPublished(getMaxPublishedTime(MAX_PUBLISH_TIME_IN_MS) - 1);
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
                long timeSincePublished = calculateTimeSincePublished(publishTime);
                if (timeSincePublished < MAX_PUBLISH_TIME_IN_MS) {
                    view.showPublished(getMaxPublishedTime(timeSincePublished));
                } else {
                    view.showUnPublished();
                }
            }
        }, null);
    }

    private long calculateTimeSincePublished(Long publishTime) {
        return publishTime == null || publishTime.equals(INVALID_PUBLISH_TIME) ?
                MAX_PUBLISH_TIME_IN_MS : System.currentTimeMillis() - publishTime;
    }

    private int getMaxPublishedTime(long timeSincePublished) {
        return (int) TimeUnit.MINUTES.convert(timeSincePublished, TimeUnit.MILLISECONDS);
    }
}
