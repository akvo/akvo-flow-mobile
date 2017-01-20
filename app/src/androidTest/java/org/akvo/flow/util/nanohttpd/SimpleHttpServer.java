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

package org.akvo.flow.util.nanohttpd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import timber.log.Timber;

/**
 * Created by MelEnt on 2016-11-22.
 */

public class SimpleHttpServer extends NanoHTTPD {
    private Map<Method, HttpServe> methodServes = new HashMap<>();

    public SimpleHttpServer() throws IOException {
        super(9090);
    }

    public void setResponse(Method method, HttpServe serve) {
        methodServes.put(method, serve);
    }

    public void resetResponse() {
        methodServes.clear();
    }

    private Response statusCode(Response.Status status) {
        return newFixedLengthResponse(status, null, status.getDescription());
    }

    @Override
    public Response serve(IHTTPSession session) {
        HttpServe serve = methodServes.get(session.getMethod());
        if (serve != null) {
            try {
                Response response = serve.serve(session);
                if (response == null) {
                    return statusCode(Response.Status.NO_CONTENT);
                }
                return response;
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
                return statusCode(Response.Status.INTERNAL_ERROR);
            }
        } else {
            // unsupported type
            return statusCode(Response.Status.METHOD_NOT_ALLOWED);
        }
    }
}
