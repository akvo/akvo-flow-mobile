/*
 * Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.util.logging;

import android.content.Context;
import android.text.TextUtils;

import com.joshdholtz.sentry.Sentry;

import org.json.JSONException;

import java.util.Map;

import timber.log.Timber;

public class SentryHelper extends LoggingHelper {

    private final Context context;

    public SentryHelper(Context context) {
        this.context = context;
    }

    @Override
    public void initSentry() {
        Sentry.setCaptureListener(new FlowSentryCaptureListener(context));
        String sentryDsn = getSentryDsn(context.getResources());
        if (!TextUtils.isEmpty(sentryDsn)) {
            Sentry.init(context, sentryDsn, true, new FlowPostPermissionVerifier(),
                    LoggingFactory.SENTRY_PROTOCOL_VERSION);
            Timber.plant(new SentryTree());
        }
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
