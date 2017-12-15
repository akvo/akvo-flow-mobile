/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util.logging;

import android.content.Context;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import io.sentry.android.AndroidSentryClientFactory;
import io.sentry.connection.Connection;
import io.sentry.connection.EventSampler;
import io.sentry.connection.HttpConnection;
import io.sentry.connection.ProxyAuthenticator;
import io.sentry.connection.RandomEventSampler;
import io.sentry.dsn.Dsn;
import io.sentry.marshaller.Marshaller;

public class FlowAndroidSentryFactory extends AndroidSentryClientFactory {

    private final LoggingSendPermissionVerifier verifier;

    public FlowAndroidSentryFactory(Context context,
            LoggingSendPermissionVerifier loggingSendPermissionVerifier) {
        super(context);
        this.verifier = loggingSendPermissionVerifier;
    }

    @Override
    protected Connection createHttpConnection(Dsn dsn) {
        URL sentryApiUrl = HttpConnection.getSentryApiUrl(dsn.getUri(), dsn.getProjectId());

        String proxyHost = getProxyHost(dsn);
        String proxyUser = getProxyUser(dsn);
        String proxyPass = getProxyPass(dsn);
        int proxyPort = getProxyPort(dsn);

        Proxy proxy = null;
        if (proxyHost != null) {
            InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
            proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
            if (proxyUser != null && proxyPass != null) {
                Authenticator.setDefault(new ProxyAuthenticator(proxyUser, proxyPass));
            }
        }

        Double sampleRate = getSampleRate(dsn);
        EventSampler eventSampler = null;
        if (sampleRate != null) {
            eventSampler = new RandomEventSampler(sampleRate);
        }

        HttpConnection httpConnection = new FlowSentryHttpConnection(sentryApiUrl,
                dsn, proxy, eventSampler, verifier);

        Marshaller marshaller = createMarshaller(dsn);
        httpConnection.setMarshaller(marshaller);

        int timeout = getTimeout(dsn);
        httpConnection.setTimeout(timeout);

        boolean bypassSecurityEnabled = getBypassSecurityEnabled(dsn);
        httpConnection.setBypassSecurity(bypassSecurityEnabled);

        return httpConnection;
    }
}
