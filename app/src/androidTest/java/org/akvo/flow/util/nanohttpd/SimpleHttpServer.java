/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation, either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.util.nanohttpd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by MelEnt on 2016-11-22.
 */

public class SimpleHttpServer extends NanoHTTPD
{
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
        if(serve != null) {
            try {
                Response response = serve.serve(session);
                if(response == null) {
                    return statusCode(Response.Status.NO_CONTENT);
                }
                return response;
            }
            catch (Exception e) {
                e.printStackTrace();
                return statusCode(Response.Status.INTERNAL_ERROR);
            }
        }
        else {
            // unsupported type
            return statusCode(Response.Status.METHOD_NOT_ALLOWED);
        }
    }
}
