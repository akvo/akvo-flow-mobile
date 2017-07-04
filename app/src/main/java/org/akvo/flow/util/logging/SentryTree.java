/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.getsentry.raven.android.Raven;

import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

class SentryTree extends Timber.Tree {

    private static final List<Class> IGNORED_EXCEPTIONS = Arrays
            .asList(new Class[] { java.net.ConnectException.class,
                    javax.net.ssl.SSLHandshakeException.class,
                    java.security.cert.CertificateNotYetValidException.class,
                    javax.net.ssl.SSLProtocolException.class,
                    java.net.SocketTimeoutException.class,
                    java.net.UnknownHostException.class,
                    java.net.ConnectException.class,
                    javax.net.ssl.SSLException.class
            });

    @Override
    protected void log(int priority, @Nullable String tag, @Nullable String message,
            @Nullable Throwable t) {

        if (t == null || priorityTooLow(priority) || isThrowableExcluded(t)) {
            return;
        }

        captureException(t, message);
    }

    @VisibleForTesting
    void captureException(@NonNull Throwable t, @Nullable String message) {
        if (TextUtils.isEmpty(message)) {
            Raven.capture(t);
        } else {
            Raven.capture(new Throwable(message, t));
        }
    }

    /**
     * Some exceptions are not useful to be sent to sentry, this method will filter them out
     */
    private boolean isThrowableExcluded(Throwable t) {
        return IGNORED_EXCEPTIONS.contains(t.getClass()) || containsFilteredMessage(t);
    }

    private boolean containsFilteredMessage(Throwable t) {
        return !TextUtils.isEmpty(t.getMessage()) && t.getMessage()
                .contains("Connection timed out");
    }

    /**
     * Configure which level should be sent
     */
    private boolean priorityTooLow(int priority) {
        return priority < Log.ERROR;
    }
}