/*
 * Copyright (C) 2016-2019 Stichting Akvo (Akvo Foundation)
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
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;

import androidx.annotation.NonNull;
import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;
import io.sentry.event.UserBuilder;
import timber.log.Timber;

public class ReleaseLoggingHelper implements LoggingHelper {

    private static final String GAE_INSTANCE_ID_TAG_KEY = "flow.gae.instance";
    private static final String DEVICE_ID_TAG_KEY = "flow.device.id";

    private final Context context;

    public ReleaseLoggingHelper(Context context) {
        this.context = context;
    }

    @Override
    public void init() {
        String sentryDsn = getSentryDsn(context.getResources());
        if (!TextUtils.isEmpty(sentryDsn)) {
            Sentry.init(sentryDsn, new AndroidSentryClientFactory(context));
            Sentry.getContext().addTag(GAE_INSTANCE_ID_TAG_KEY, BuildConfig.AWS_BUCKET);
            Timber.plant(new SentryTree());
        }
        Timber.plant(new CrashlyticsTree()); //TODO: add all the tags
    }

    @Override
    public void initLoginData(String username, String deviceId) {
        io.sentry.context.Context sentryContext = Sentry.getContext();
        if (!TextUtils.isEmpty(username)){
            sentryContext.setUser(new UserBuilder().setUsername(username).build());
        }
        if (!TextUtils.isEmpty(deviceId)) {
            sentryContext.addTag(DEVICE_ID_TAG_KEY, deviceId);
        }
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
