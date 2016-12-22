/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.Nullable;
import android.util.Log;

import com.joshdholtz.sentry.Sentry;

import timber.log.Timber;

class SentryTree extends Timber.Tree {

    @Override
    protected void log(int priority, @Nullable String tag, @Nullable String message,
            @Nullable Throwable t) {
        //We will only send stacktraces
        if (!shouldSendLog(priority)) {
            return;
        }

        if (t != null) {
            Sentry.captureException(t);
        }
    }

    private boolean shouldSendLog(int priority) {
        return priority == Log.ERROR || priority == Log.ASSERT;
    }
}