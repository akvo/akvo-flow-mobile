/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.joshdholtz.sentry.Sentry;

import org.json.JSONException;

import java.util.Map;

import timber.log.Timber;

public class SentryHelper extends LoggingHelper {

    public SentryHelper(Context context) {
        super(context);
    }

    @Override
    public void initSentry() {
        addTags();
        Sentry.setCaptureListener(new FlowSentryCaptureListener(tags));

        String sentryDsn = getSentryDsn(context.getResources());
        if (!TextUtils.isEmpty(sentryDsn)) {
            Sentry.init(context, sentryDsn, true, new FlowPostPermissionVerifier(context),
                    LoggingFactory.SENTRY_PROTOCOL_VERSION);
            Timber.plant(new SentryTree());
        }
    }

    private static class FlowSentryCaptureListener implements Sentry.SentryEventCaptureListener {

        private final Map<String, String> tags;

        private FlowSentryCaptureListener(@NonNull Map<String, String> tags) {
            this.tags = tags;
        }

        @Override
        public Sentry.SentryEventBuilder beforeCapture(Sentry.SentryEventBuilder builder) {
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
