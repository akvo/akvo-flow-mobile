/*
 * Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.util.logging;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;

import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;
import io.sentry.event.UserBuilder;
import timber.log.Timber;

public class ReleaseLoggingHelper implements LoggingHelper {

    private final Context context;
//    private final FlowAndroidRavenFactory ravenFactory;

    public ReleaseLoggingHelper(Context context/**, FlowAndroidRavenFactory flowAndroidRavenFactory**/) {
        this.context = context;
//        this.ravenFactory = flowAndroidRavenFactory;
    }

    @Override
    public void init() {
        String sentryDsn = getSentryDsn(context.getResources());
        if (!TextUtils.isEmpty(sentryDsn)) {
            Sentry.init(sentryDsn, new AndroidSentryClientFactory(context));
            Sentry.getContext().addTag(TagsFactory.GAE_INSTANCE_ID_TAG_KEY, BuildConfig.AWS_BUCKET);
            Timber.plant(new SentryTree());
        }
    }

    @Override
    public void initLoginData(String username, String deviceId) {
        io.sentry.context.Context sentryContext = Sentry.getContext();
        sentryContext.setUser(new UserBuilder().setUsername(username).build());
        sentryContext.addTag(TagsFactory.DEVICE_ID_TAG_KEY, deviceId);
    }

    @Override
    public void clearUser() {
        Sentry.getContext().clearUser();
    }

    @NonNull
    private String getSentryDsn(Resources resources) {
        return resources.getString(R.string.sentry_dsn);
    }
}
