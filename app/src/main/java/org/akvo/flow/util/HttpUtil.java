/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.util;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.akvo.flow.exception.HttpException;
import org.apache.http.HttpStatus;

/**
 * Simple utility to make http calls and read the responses
 *
 * @author Christopher Fagiani
 */
public class HttpUtil {
    private static final String TAG = HttpUtil.class.getSimpleName();
    private static final int BUFFER_SIZE = 8192;

    public static String httpGet(String url) throws IOException {
        // new
        InternetDataConnection connection = new InternetDataConnection(url);
        InternetDataConnection.InputStreamProvider input = connection.connect().forInput();
        try
        {
            connection.verifyOk();
            String result = input.toStringValue();
            Log.d(TAG, url + ": " + connection.getElapsedTime() + " ms");
            return result;
        } finally {
            connection.close();
        }
    }

    public static void httpGet(String url, File dst) throws IOException {
        // new
        InternetDataConnection connection = new InternetDataConnection(url);
        InternetDataConnection.InputStreamProvider input = connection.connect().forInput();
        try
        {
            connection.verifyOk();
            OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));
            input.toStream(out);
        } finally {
            connection.close();
        }
    }

    /**
     * does an HTTP Post to the url specified using the params passed in
     */
    public static String httpPost(String url, Map<String, String> params) throws IOException {
        // new
        InternetDataConnection connection = new InternetDataConnection(url);
        InternetDataConnection.BothStreams provider = connection.connect().forInput().andOutput();
        try
        {
            connection.verifyOk();
            Writer writer = new BufferedWriter(new OutputStreamWriter(provider.output.get(), "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();

            return provider.input.toStringValue();
        } finally {
            connection.close();
        }
    }

    public static String getQuery(Map<String, String> params) {
        if (params == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (Entry<String, String> param : params.entrySet()) {
            builder.append("&").append(param.getKey()).append("=").append(param.getValue());
        }
        // Skip the first "&", if found.
        return builder.length() > 0 ? builder.substring(1) : builder.toString();
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
        out.flush();
    }

}
