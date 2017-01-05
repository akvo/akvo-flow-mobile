/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.akvo.flow.exception.HttpException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Simple utility to make http calls and read the responses
 *
 * @author Christopher Fagiani
 */
public class HttpUtil {

    private static final String TAG = HttpUtil.class.getSimpleName();
    private static final int BUFFER_SIZE = 8192;

    @NonNull
    public static String httpGet(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) (new URL(url).openConnection());
        final long t0 = System.currentTimeMillis();

        try {
            int status = getStatusCode(conn);
            if (status != HttpURLConnection.HTTP_OK) {
                throw new HttpException(conn.getResponseMessage(), status);
            }
            InputStream in = new BufferedInputStream(conn.getInputStream());
            String response = readStream(in);
            Log.d(TAG, url + ": " + (System.currentTimeMillis() - t0) + " ms");
            return response;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static void httpGet(String url, @NonNull File dst) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();

            in = new BufferedInputStream(conn.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(dst));

            copyStream(in, out);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                // TODO: Use custom exception?
                throw new IOException("Status Code: " + status + ". Expected: 200 - OK");
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            FileUtil.close(in);
            FileUtil.close(out);
        }
    }

    /**
     * does an HTTP Post to the url specified using the params passed in
     */
    public static String httpPost(String url, Map<String, String> params) throws IOException {
        OutputStream out = null;
        InputStream in = null;
        Writer writer;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);

            out = new BufferedOutputStream(conn.getOutputStream());
            writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));

            writer.write(getQuery(params));
            writer.flush();
            writer.close();

            in = new BufferedInputStream(conn.getInputStream());

            int status = getStatusCode(conn);
            if (status != HttpURLConnection.HTTP_OK) {
                throw new HttpException(conn.getResponseMessage(), status);
            }
            return readStream(in);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            FileUtil.close(out);
            FileUtil.close(in);
        }
    }

    private static int getStatusCode(@NonNull HttpURLConnection conn) throws IOException {
        try {
            return conn.getResponseCode();
        } catch (IOException e) {
            // HttpUrlConnection will throw an IOException if any 4XX
            // response is sent. If we request the status again, this
            // time the internal status will be properly set, and we'll be
            // able to retrieve it.
            return conn.getResponseCode();
        }
    }

    @NonNull
    private static String getQuery(@Nullable Map<String, String> params) {
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

    private static String readStream(@NonNull InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } finally {
            FileUtil.close(reader);
        }

        return builder.toString();
    }

    public static void copyStream(@NonNull InputStream in, @NonNull OutputStream out)
            throws IOException {
        byte[] b = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }
}
