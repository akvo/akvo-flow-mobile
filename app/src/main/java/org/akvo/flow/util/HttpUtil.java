/*
 * Copyright (C) 2010-2016,2018-2019 Stichting Akvo (Akvo Foundation)
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

import androidx.annotation.NonNull;
import android.util.Log;

import org.akvo.flow.exception.HttpException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
