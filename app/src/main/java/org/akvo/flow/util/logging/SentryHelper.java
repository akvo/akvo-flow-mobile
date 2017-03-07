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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.joshdholtz.sentry.Sentry;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.json.JSONException;

import java.util.Map;

import timber.log.Timber;

public class SentryHelper {

    public static final String SENTRY_PROTOCOL_VERSION = "7";
    private final Context context;

    public SentryHelper(Context context) {
        this.context = context;
    }

    public void initSentry() {
        Sentry.setCaptureListener(new FlowSentryCaptureListener(context));
        String sentryDsn = getSentryDsn(context.getResources());
        if (!TextUtils.isEmpty(sentryDsn)) {
            Sentry.init(context, sentryDsn, true, new FlowPostPermissionVerifier(context),
                    SENTRY_PROTOCOL_VERSION);
            Timber.plant(new SentryTree());
        }
    }

    public void initDebugTree() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Nullable
    String getSentryDsn(Resources resources) {
        return resources.getString(R.string.sentry_dsn);
    }

    private static class FlowSentryCaptureListener implements Sentry.SentryEventCaptureListener {

        private final Context context;

        private FlowSentryCaptureListener(Context context) {
            this.context = context;
        }

        @Override
        public Sentry.SentryEventBuilder beforeCapture(Sentry.SentryEventBuilder builder) {
            TagsFactory tagsFactory = new TagsFactory(context);
            //tags value may change over time so we need to recreate them each time
            Map<String, String> tags = tagsFactory.getTags();
            try {
                for (String key : tags.keySet()) {
                    builder.getTags().put(key, tags.get(key));
                }
            } catch (JSONException e) {
                Timber.e("Error setting SentryEventCaptureListener");
            }
            return builder;
        }
    }
}
