package org.akvo.flow.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by MelEnt on 2016-11-22.
 */

public class SimpleHttpServer extends NanoHTTPD
{
    private String responseText;

    public SimpleHttpServer() throws IOException
    {
        super(9090);
    }

    public void setResponseText(String responseText)
    {
        this.responseText = responseText;
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        Map<String, String> map = session.getParms();
        StringBuilder output = new StringBuilder();
        for(Map.Entry<String, String> entry : map.entrySet())
        {
            output.append("Key: ");
            output.append(entry.getKey());
            output.append('\n');
            output.append("Value: ");
            output.append(entry.getValue());
            output.append('\n');
        }
        System.out.println(output.toString());
        return newFixedLengthResponse(output.toString());
    }
}
