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

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class PublishedTimeHelper {

    public static final long MAX_PUBLISH_TIME_IN_MS = 90 * 60 * 1000;
    private static final long INVALID_PUBLISH_TIME = -1L;
    private static final int ONE_MINUTE = 1;

    @Inject
    public PublishedTimeHelper() {
    }

    public long calculateTimeSincePublished(Long publishTime) {
        return publishTime == null || publishTime.equals(INVALID_PUBLISH_TIME) ?
                MAX_PUBLISH_TIME_IN_MS :
                System.currentTimeMillis() - publishTime;
    }

    public int getRemainingPublishedTime(long timeSincePublished) {
        long timeRemainingInMs = MAX_PUBLISH_TIME_IN_MS - timeSincePublished;
        return (int) TimeUnit.MINUTES.convert(timeRemainingInMs, TimeUnit.MILLISECONDS);
    }

    public int getRemainingPublishedTimeToDisplay(long timeSincePublished) {
        return getRemainingPublishedTime(timeSincePublished) + ONE_MINUTE;
    }
}
