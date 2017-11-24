///*
// * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
// *
// * This file is part of Akvo Flow.
// *
// * Akvo Flow is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * Akvo Flow is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
// *
// */
//
//package org.akvo.flow.util.logging;
//
//import com.getsentry.raven.connection.ConnectionException;
//import com.getsentry.raven.connection.EventSampler;
//import com.getsentry.raven.connection.HttpConnection;
//import com.getsentry.raven.dsn.Dsn;
//import com.getsentry.raven.event.Event;
//
//import java.net.Proxy;
//import java.net.URL;
//
///**
// * An {@link HttpConnection} that verifies if the user allowed using mobile networks before
// * sending an exception to sentry
// */
//public class FlowRavenHttpConnection extends HttpConnection {
//
//    private final LoggingSendPermissionVerifier permissionVerifier;
//
//    public FlowRavenHttpConnection(URL sentryUrl, Dsn dsn,
//            Proxy proxy, EventSampler eventSampler, LoggingSendPermissionVerifier verifier) {
//        super(sentryUrl, dsn.getPublicKey(), dsn.getSecretKey(), proxy, eventSampler);
//        this.permissionVerifier = verifier;
//    }
//
//    @Override
//    protected void doSend(Event event) throws ConnectionException {
//        if (permissionVerifier.shouldAttemptPost()) {
//            super.doSend(event);
//        } else {
//            throw new ConnectionException("Connection forbidden by user");
//        }
//    }
//}
