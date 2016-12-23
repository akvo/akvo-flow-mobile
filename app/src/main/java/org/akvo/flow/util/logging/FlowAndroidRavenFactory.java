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

import android.content.Context;
import android.support.annotation.NonNull;

import com.getsentry.raven.android.AndroidRavenFactory;
import com.getsentry.raven.buffer.Buffer;
import com.getsentry.raven.connection.BufferedConnection;
import com.getsentry.raven.connection.Connection;
import com.getsentry.raven.connection.ConnectionException;
import com.getsentry.raven.connection.HttpConnection;
import com.getsentry.raven.connection.NoopConnection;
import com.getsentry.raven.dsn.Dsn;
import com.getsentry.raven.environment.RavenEnvironment;
import com.getsentry.raven.event.Event;
import com.getsentry.raven.marshaller.Marshaller;

import org.akvo.flow.util.StatusUtil;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import timber.log.Timber;

class FlowAndroidRavenFactory extends AndroidRavenFactory {

    private final Context applicationContext;
    private final Map<String, String> tags;

    public FlowAndroidRavenFactory(Context applicationContext, @NonNull Map<String, String> tags) {
        super(applicationContext);
        this.applicationContext = applicationContext;
        this.tags = tags;
    }

    @Override
    public com.getsentry.raven.Raven createRavenInstance(Dsn dsn) {
        com.getsentry.raven.Raven ravenInstance = super.createRavenInstance(dsn);
        ravenInstance.setConnection(createConnection(dsn));
        ravenInstance.addBuilderHelper(new FlowEventBuilderHelper(applicationContext, tags));
        return ravenInstance;
    }

    /**
     * Creates a connection to the given DSN by determining the protocol.
     *
     * @param dsn Data Source Name of the Sentry server to use.
     * @return a connection to the server.
     */
    protected Connection createConnection(Dsn dsn) {
        String protocol = dsn.getProtocol();
        Connection connection;

        if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
            Timber.i("Using an HTTP connection to Sentry.");
            connection = createHttpConnection(dsn);
        } else if (protocol.equalsIgnoreCase("out")) {
            Timber.i("Using StdOut to send events.");
            connection = createStdOutConnection(dsn);
        } else if (protocol.equalsIgnoreCase("noop")) {
            Timber.i("Using noop to send events.");
            connection = new NoopConnection();
        } else {
            throw new IllegalStateException(
                    "Couldn't create a connection for the protocol '" + protocol + "'");
        }

        Buffer eventBuffer = getBuffer(dsn);
        if (eventBuffer != null) {
            long flushtime = getBufferFlushtime(dsn);
            boolean gracefulShutdown = getBufferedConnectionGracefulShutdownEnabled(dsn);
            Long shutdownTimeout = getBufferedConnectionShutdownTimeout(dsn);
            connection = new BufferedConnection(connection, eventBuffer, flushtime,
                    gracefulShutdown,
                    shutdownTimeout);
        }

        // Enable async unless its value is 'false'.
        if (getAsyncEnabled(dsn)) {
            connection = createAsyncConnection(dsn, connection);
        }

        return connection;
    }

    /**
     * Creates an HTTP connection to the Sentry server.
     *
     * @param dsn Data Source Name of the Sentry server.
     * @return an {@link HttpConnection} to the server.
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

        FlowHttpConnection httpConnection = new FlowHttpConnection(sentryApiUrl, dsn.getPublicKey(),
                dsn.getSecretKey(), proxy, applicationContext);

        Marshaller marshaller = createMarshaller(dsn);
        httpConnection.setMarshaller(marshaller);

        int timeout = getTimeout(dsn);
        httpConnection.setTimeout(timeout);

        boolean bypassSecurityEnabled = getBypassSecurityEnabled(dsn);
        httpConnection.setBypassSecurity(bypassSecurityEnabled);

        return httpConnection;
    }

    /**
     * Allows use to intercept the sending of Events if the user does not allow it
     */
    private class FlowHttpConnection extends HttpConnection {

        private final Context applicationContext;
        private final String authHeader;

        public FlowHttpConnection(URL sentryApiUrl, String publicKey, String secretKey, Proxy proxy,
                Context applicationContext) {
            super(sentryApiUrl, publicKey, secretKey, proxy);
            this.applicationContext = applicationContext;
            authHeader = "Sentry sentry_version=" + LoggingFactory.SENTRY_PROTOCOL_VERSION + ","
                    + "sentry_client=" + RavenEnvironment.NAME + ","
                    + "sentry_key=" + publicKey + ","
                    + "sentry_secret=" + secretKey;
        }

        /**
         * TODO: overriding header to allow using with old server
         * remove once updated
         *
         * @return
         */
        @Override
        protected String getAuthHeader() {
            Timber.d("getAuthHeader()" + authHeader);
            return authHeader;
        }

        @Override
        protected void doSend(Event event) throws ConnectionException {
            Timber.d("Do send");
            if (!StatusUtil.isConnectionAllowed(applicationContext)) {
                throw new ConnectionException("User disallows sending events over 3G");
            }
            super.doSend(event);
        }
    }
}
