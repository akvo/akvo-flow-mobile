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
import com.getsentry.raven.buffer.Buffer;
import com.getsentry.raven.buffer.DiskBuffer;
import com.getsentry.raven.connection.Connection;
import com.getsentry.raven.connection.EventSampler;
import com.getsentry.raven.connection.HttpConnection;
import com.getsentry.raven.connection.RandomEventSampler;
import com.getsentry.raven.dsn.Dsn;
import com.getsentry.raven.marshaller.Marshaller;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import timber.log.Timber;

public class FlowAndroidRavenFactory extends AndroidRavenFactory {

    /**
     * Default Buffer directory name.
     */
    private static final String DEFAULT_BUFFER_DIR = "raven-buffered-events";

    private final FlowPostPermissionVerifier verifier;
    private final Context context;

    public FlowAndroidRavenFactory(Context context) {
        super(context);
        this.context = context;
        this.verifier = new FlowPostPermissionVerifier(context);
    }

    @Override
    public com.getsentry.raven.Raven createRavenInstance(Dsn dsn) {
        Raven raven = new Raven(createConnection(dsn));
//        try {
//            // `ServletRequestListener` was added in the Servlet 2.4 API, and
//            // is used as part of the `HttpEventBuilderHelper`, see:
//            // https://tomcat.apache.org/tomcat-5.5-doc/servletapi/
//            Class.forName("javax.servlet.ServletRequestListener", false, this.getClass().getClassLoader());
//            raven.addBuilderHelper(new HttpEventBuilderHelper());
//        } catch (ClassNotFoundException e) {
//            logger.debug("The current environment doesn't provide access to servlets,"
//                    + "or provides an unsupported version.");
//        }
        raven.addBuilderHelper(new FlowAndroidEventBuilderHelper(context));
        return raven;
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

        Double sampleRate = getSampleRate(dsn);
        EventSampler eventSampler = null;
        if (sampleRate != null) {
            eventSampler = new RandomEventSampler(sampleRate);
        }

        HttpConnection httpConnection = new FlowHttpConnection(sentryApiUrl, dsn.getPublicKey(),
                dsn.getSecretKey(), proxy, eventSampler, verifier);

        Marshaller marshaller = createMarshaller(dsn);
        httpConnection.setMarshaller(marshaller);

        int timeout = getTimeout(dsn);
        httpConnection.setTimeout(timeout);

        boolean bypassSecurityEnabled = getBypassSecurityEnabled(dsn);
        httpConnection.setBypassSecurity(bypassSecurityEnabled);

        return httpConnection;
    }

    @Override
    protected Buffer getBuffer(Dsn dsn) {
        File bufferDir;
        if (dsn.getOptions().get(BUFFER_DIR_OPTION) != null) {
            bufferDir = new File(dsn.getOptions().get(BUFFER_DIR_OPTION));
        } else {
            bufferDir = new File(context.getCacheDir().getAbsolutePath(), DEFAULT_BUFFER_DIR);
        }

        Timber.d("Using buffer dir: " + bufferDir.getAbsolutePath());
        return new DiskBuffer(bufferDir, getBufferSize(dsn));
    }
}
