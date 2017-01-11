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
public class HttpUtilTest
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

    private final HttpServe defaultMd5Post = new HttpServe()
    {
        @Override
        public NanoHTTPD.Response serve(@Nullable NanoHTTPD.IHTTPSession session) throws Exception
        {
            Map<String, String> files = new HashMap<>();
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
    public void newCanHttpGetToFile() throws IOException, InterruptedException
    {
        File file = getTempFile();
        //sets a response to be returned by the server, based on the request
        server.setResponse(NanoHTTPD.Method.GET, defaultGet);

        //read contents from URL
        String expected = HttpUtil.httpGet(URL_STRING);

        //copy contents from URL to destination 'file'
        HttpUtil.httpGet(URL_STRING, file);

        assertEquals(expected+'\n', OldHttpUtil.readStream(new FileInputStream(file)));
        assertTrue(file.delete());
    }

    @Test
    public void newCanHttpGet() throws IOException, InterruptedException
    {
        server.setResponse(NanoHTTPD.Method.GET, defaultGet);

        String result = HttpUtil.httpGet(URL_STRING);
        assertEquals(result, VALID_RESPONSE_STRING);
    }

    @Test
    public void newCanHttpPost() throws IOException, InterruptedException, NoSuchAlgorithmException
    {
        server.setResponse(NanoHTTPD.Method.POST, defaultMd5Post);

        Map<String, String> params = new HashMap<>();
        params.put("firstK", "firstV");
        params.put("secondK", "secondV");

        //post the contents of the Map and retrieve the result as String
        String result = HttpUtil.httpPost(URL_STRING, params);


        byte[] hash = MessageDigest.getInstance("MD5").digest(HttpUtil.getQuery(params).getBytes());
        String expected = Arrays.toString(hash);

        //assert that the responded md5 hash is correct with the given md5 hash
        assertEquals(result, expected);
    }



}

