/*
* Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import timber.log.Timber;

/**
 * Simple utility to make http calls and read the responses
 *
 */
public class HttpUtil {
    private static final String TAG = HttpUtil.class.getSimpleName();
    private static final int BUFFER_SIZE = 8192;

    public static String httpGet(String url) throws IOException {
        InternetDataConnection connection = new InternetDataConnection(url);
        InternetDataConnection.InputStreamProvider input = connection.connect().forInput();
        try {
            connection.verifyOk();
            String result = input.toStringValue();
            Timber.d(TAG + ": URL: %s - %s ms", url, String.valueOf(connection.getElapsedTime()));
            return result;
        } finally {
            connection.close();
        }
    }

    public static void httpGet(String url, File dst) throws IOException {
        InternetDataConnection connection = new InternetDataConnection(url);
        InternetDataConnection.InputStreamProvider input = connection.connect().forInput();
        try {
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
        InternetDataConnection connection = new InternetDataConnection(url);
        InternetDataConnection.BothStreams provider = connection.connect().forInput().andOutput();
        connection.getConnection().setRequestMethod("POST");
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(provider.output.get(), "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();

            connection.verifyOk();

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
