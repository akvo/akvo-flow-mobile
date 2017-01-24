/*
* Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.TempTestFileFactory;
import org.akvo.flow.util.nanohttpd.HttpServe;
import org.akvo.flow.util.nanohttpd.SimpleHttpServer;
import org.junit.After;
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

/**
 * Created by MelEnt on 2016-11-22.
 */

@RunWith(AndroidJUnit4.class)
public class OldHttpUtilTest {
    private static SimpleHttpServer server;
    private static final String URL_STRING = "http://localhost:9090/";
    private static final String VALID_RESPONSE_STRING = "Valid_Response";
    private static final TempTestFileFactory TestFileFactory = new TempTestFileFactory();

    private static NanoHTTPD.Response response(Object content) {
        if (content == null) {
            return null;
        }
        return NanoHTTPD.newFixedLengthResponse(String.valueOf(content));
    };

    //initiate response here so the test case can be easier to read
    private final HttpServe defaultMd5Post = new HttpServe() {
        @Override
        public NanoHTTPD.Response serve(@Nullable NanoHTTPD.IHTTPSession session) throws Exception {
            Map<String, String> files = new HashMap<>();
            //copy body to destination Map files
            session.parseBody(files);

            byte[] hash = MessageDigest.getInstance("MD5")
                    .digest(session.getQueryParameterString().getBytes());
            String output = Arrays.toString(hash);

            return response(output);
        }
    };

    private final HttpServe defaultGet = new HttpServe() {
        @Override
        public NanoHTTPD.Response serve(@Nullable NanoHTTPD.IHTTPSession session) throws Exception {
            return response(VALID_RESPONSE_STRING);
        }
    };

    @BeforeClass
    public static void startServer() throws IOException {
        server = new SimpleHttpServer();
        server.start();
    }

    @AfterClass
    public static void stopServer() {
        server.stop();
    }

    @Before
    public void resetServer() {
        server.resetResponse();
    }

    @After
    public void afterTest() {
        TestFileFactory.deleteTempFiles();
    }

    @Test
    public void oldCanHttpGet() throws IOException, InterruptedException {
        server.setResponse(NanoHTTPD.Method.GET, defaultGet);

        String result = OldHttpUtil.httpGet(URL_STRING);
        assertEquals(VALID_RESPONSE_STRING + '\n', result);
    }

    @Test
    public void oldCanHttpGetToFile() throws IOException, InterruptedException {
        File file = TestFileFactory.generateTempFile();

        server.setResponse(NanoHTTPD.Method.GET, defaultGet);
        String expected = OldHttpUtil.httpGet(URL_STRING);

        OldHttpUtil.httpGet(URL_STRING, file);

        assertEquals(expected, OldHttpUtil.readStream(new FileInputStream(file)));
    }

    @Test
    public void oldCanHttpPost()
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        server.setResponse(NanoHTTPD.Method.POST, defaultMd5Post);

        Map<String, String> params = new HashMap<>();
        params.put("firstK", "firstV");
        params.put("secondK", "secondV");

        String result = OldHttpUtil.httpPost(URL_STRING, params);

        byte[] hash = MessageDigest.getInstance("MD5")
                .digest(OldHttpUtil.getQuery(params).getBytes());
        String expected = Arrays.toString(hash);

        assertEquals(expected, result.substring(0, result.length() - 1));
    }
}
