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

import com.getsentry.raven.Raven;
import com.getsentry.raven.android.AndroidRavenFactory;
import com.getsentry.raven.connection.Connection;
import com.getsentry.raven.connection.EventSampler;
import com.getsentry.raven.connection.HttpConnection;
import com.getsentry.raven.connection.RandomEventSampler;
import com.getsentry.raven.dsn.Dsn;
import com.getsentry.raven.marshaller.Marshaller;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class FlowAndroidRavenFactory extends AndroidRavenFactory {

    private final LoggingSendPermissionVerifier verifier;
    private RavenEventBuilderHelper ravenEventBuilderHelper;

    public FlowAndroidRavenFactory(Context context,
            LoggingSendPermissionVerifier loggingSendPermissionVerifier,
            RavenEventBuilderHelper ravenEventBuilderHelper) {
        super(context);
        this.verifier = loggingSendPermissionVerifier;
        this.ravenEventBuilderHelper = ravenEventBuilderHelper;
    }

    @Override
    public Raven createRavenInstance(Dsn dsn) {
        Raven raven = new Raven(createConnection(dsn), getContextManager(dsn));
        raven.addBuilderHelper(ravenEventBuilderHelper);
        return raven;
    }

    /**
     * Creates an HTTP connection to the Sentry server.
     *
     * @param dsn Data Source Name of the Sentry server.
     * @return an {@link FlowRavenHttpConnection}
     */
    protected Connection createHttpConnection(Dsn dsn) {
        URL sentryApiUrl = HttpConnection.getSentryApiUrl(dsn.getUri(), dsn.getProjectId());

        String proxyHost = getProxyHost(dsn);
        int proxyPort = getProxyPort(dsn);

        Proxy proxy = null;
        if (proxyHost != null) {
            InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
            proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
        }

        Double sampleRate = getSampleRate(dsn);
        EventSampler eventSampler = null;
        if (sampleRate != null) {
            eventSampler = new RandomEventSampler(sampleRate);
        }

        HttpConnection httpConnection = new FlowRavenHttpConnection(sentryApiUrl,
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
