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

package org.akvo.flow.util;

import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.util.nanohttpd.HttpServe;
import org.akvo.flow.util.nanohttpd.SimpleHttpServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by MelEnt on 2016-11-22.
 */

@RunWith(AndroidJUnit4.class)
public class OldHttpUtilTest
{
    private static SimpleHttpServer server;
    private static final String URL_STRING = "http://localhost:9090/";
    private static final String VALID_RESPONSE_STRING = "Valid_Response";

    private static NanoHTTPD.Response response(Object content)
    {
        if(content == null)
            return null;
        return NanoHTTPD.newFixedLengthResponse(String.valueOf(content));
    };

    //initiate response here so the test case can be easier to read
    private final HttpServe defaultMd5Post = new HttpServe()
    {
        @Override
        public NanoHTTPD.Response serve(@Nullable NanoHTTPD.IHTTPSession session) throws Exception
        {
            Map<String, String> files = new HashMap<>();
            //copy body to destination Map files
            session.parseBody(files);

            byte[] hash = MessageDigest.getInstance("MD5").digest(session.getQueryParameterString().getBytes());
            String output = Arrays.toString(hash);

            return response(output);
        }
    };

    private final HttpServe defaultGet = new HttpServe()
    {
        @Override
        public NanoHTTPD.Response serve(@Nullable NanoHTTPD.IHTTPSession session) throws Exception
        {
            return response(VALID_RESPONSE_STRING);
        }
    };

    @BeforeClass
    public static void startServer() throws IOException
    {
        server = new SimpleHttpServer();
        server.start();
    }
    @AfterClass
    public static void stopServer()
    {
        server.stop();
    }

    @Before
    public void resetServer()
    {
        server.resetResponse();
    }

    private File getTempFile() throws IOException
    {
        return File.createTempFile("temp_", ".txt");
    }

    @Test
    public void oldCanHttpGet() throws IOException, InterruptedException {
        server.setResponse(NanoHTTPD.Method.GET, defaultGet);

        String result = OldHttpUtil.httpGet(URL_STRING);
        assertEquals(result, VALID_RESPONSE_STRING+'\n');
    }

    @Test
    public void oldCanHttpGetToFile() throws IOException, InterruptedException
    {
        File file = getTempFile();

        server.setResponse(NanoHTTPD.Method.GET, defaultGet);
        String expected = OldHttpUtil.httpGet(URL_STRING);

        OldHttpUtil.httpGet(URL_STRING, file);

        assertEquals(expected, OldHttpUtil.readStream(new FileInputStream(file)));
        assertTrue(file.delete());
    }

    @Test
    public void oldCanHttpPost() throws IOException, InterruptedException, NoSuchAlgorithmException
    {
        server.setResponse(NanoHTTPD.Method.POST, defaultMd5Post);

        Map<String, String> params = new HashMap<>();
        params.put("firstK", "firstV");
        params.put("secondK", "secondV");

        String result = OldHttpUtil.httpPost(URL_STRING, params);

        byte[] hash = MessageDigest.getInstance("MD5").digest(OldHttpUtil.getQuery(params).getBytes());
        String expected = Arrays.toString(hash);

        assertEquals(result.substring(0, result.length()-1), expected);
    }
}

