/*
 * Copyright (C) 2010-2017,2019 Stichting Akvo (Akvo Foundation)
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

import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import io.sentry.Sentry;
import timber.log.Timber;

class SentryTree extends Timber.Tree {

    private static final List<Class> IGNORED_EXCEPTIONS = Arrays
            .asList(new Class[] {
                    java.io.EOFException.class,
                    java.io.InterruptedIOException.class,
                    java.net.ConnectException.class,
                    java.net.NoRouteToHostException.class,
                    java.net.SocketTimeoutException.class,
                    java.net.SocketException.class,
                    java.net.UnknownHostException.class,
                    java.security.cert.CertificateNotYetValidException.class,
                    javax.net.ssl.SSLProtocolException.class,
                    javax.net.ssl.SSLHandshakeException.class,
                    javax.net.ssl.SSLException.class,
                    okhttp3.internal.http2.StreamResetException.class,
                    okhttp3.internal.http2.ConnectionShutdownException.class
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
            Sentry.capture(t);
        } else {
            Sentry.capture(new Throwable(message, t));
        }
    }

    /**
     * Some exceptions are not useful to be sent to sentry, this method will filter them out
     */
    private boolean isThrowableExcluded(Throwable t) {
        return IGNORED_EXCEPTIONS.contains(t.getClass()) || containsExcludedCause(t) ||
                containsFilteredMessage(t);
    }

    private boolean containsExcludedCause(Throwable t) {
        return t.getCause() != null && IGNORED_EXCEPTIONS.contains(t.getCause().getClass());
    }

    private boolean containsFilteredMessage(Throwable t) {
        String message = t.getMessage();
        return !TextUtils.isEmpty(message) &&
                (message.contains("HTTP 5") ||
                        message.contains("Connection timed out") ||
                        message.contains("unexpected end of stream"));
    }

    /**
     * Configure which level should be sent
     */
    private boolean priorityTooLow(int priority) {
        return priority < Log.ERROR;
    }
}