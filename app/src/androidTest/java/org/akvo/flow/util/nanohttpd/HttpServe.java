package org.akvo.flow.util.nanohttpd;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by MelEnt on 2017-01-02.
 */

public interface HttpServe
{
    NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) throws Exception;
}
