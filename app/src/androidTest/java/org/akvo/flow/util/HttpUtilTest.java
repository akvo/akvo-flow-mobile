package org.akvo.flow.util;


import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
    private static final String VALID_RESPONSE_STRING = "Valid_Response: "+Math.random();

    @Before
    public void startServer() throws IOException
    {
        server = new SimpleHttpServer();
        server.start();
    }
    @After
    public void stopServer()
    {
        server.stop();
    }

    private static NanoHTTPD.Response simpleResponse(String string)
    {
        return NanoHTTPD.newFixedLengthResponse(string);
    }

    private File getTempFile() throws IOException
    {
        return File.createTempFile("temp_", ".txt");
    }


    @Test
    public void oldCanHttpGetToFile() throws IOException
    {
        File file = getTempFile();

        server.setResponseText(VALID_RESPONSE_STRING);
        String expected = OldHttpUtil.httpGet(URL_STRING);

        OldHttpUtil.httpGet(URL_STRING, file);

        assertEquals(expected, OldHttpUtil.readStream(new FileInputStream(file)));
        assertTrue(file.delete());
    }

    @Test
    public void newCanHttpGetToFile() throws IOException
    {
        File file = getTempFile();

        server.setResponseText(VALID_RESPONSE_STRING);
        String expected = HttpUtil.httpGet(URL_STRING);

        HttpUtil.httpGet(URL_STRING, file);

        assertEquals(expected, OldHttpUtil.readStream(new FileInputStream(file)));
        assertTrue(file.delete());
    }

    @Test
    public void canHttpGetToFileSameFile() throws IOException
    {
        File oldFile = getTempFile(); //old Http
        File newFile = getTempFile(); //new Http

        server.setResponseText(VALID_RESPONSE_STRING);
        OldHttpUtil.httpGet(URL_STRING, oldFile);
        HttpUtil.httpGet(URL_STRING, newFile);

        String oldContent = OldHttpUtil.readStream(new FileInputStream(oldFile));
        String newContent = OldHttpUtil.readStream(new FileInputStream(newFile));

        assertEquals(oldContent, newContent);
    }

    @Test
    public void canHttpGetSameContent() throws IOException
    {
        server.setResponseText(VALID_RESPONSE_STRING);

        String oldContent = OldHttpUtil.httpGet(URL_STRING);
        String newContent = HttpUtil.httpGet(URL_STRING);

        assertEquals(oldContent, newContent);
    }

    @Test
    public void oldCanHttpPost() throws IOException
    {
        Map<String, String> params = new HashMap<>();
        params.put("firstK", "firstV");
        params.put("secondK", "secondV");
        String result = OldHttpUtil.httpPost(URL_STRING, params);

        System.out.println(result);
//        System.out.println(OldHttpUtil.httpGet(URL_STRING));
    }

}

