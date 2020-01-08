/*
 *  Copyright (C) 2013,2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.api;

import android.util.Base64;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.data.net.SignatureHelper;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.HttpUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class S3Api {
    private static final String URL = "https://%s.s3.amazonaws.com/%s";
    private static final String PAYLOAD_GET = "GET\n\n\n%s\n/%s/%s";// date, bucket, obj

    private String mBucket;
    private String mAccessKey;
    private String mSecret;

    private final SignatureHelper signatureHelper = new SignatureHelper();

    public S3Api() {
        mBucket = BuildConfig.AWS_BUCKET;
        mAccessKey = BuildConfig.AWS_ACCESS_KEY_ID;
        mSecret = BuildConfig.AWS_SECRET_KEY;
    }

    public void get(String objectKey, File dst) throws IOException {
        // Get date and signature
        final String date = getDate();
        final String payload = String.format(PAYLOAD_GET, date, mBucket, objectKey);
        final String signature = signatureHelper.getAuthorization(payload, mSecret, Base64.NO_WRAP);
        final URL url = new URL(String.format(URL, mBucket, objectKey));

        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Date", date);
            conn.setRequestProperty("Authorization", "AWS " + mAccessKey + ":" + signature);

            in = new BufferedInputStream(conn.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(dst));

            HttpUtil.copyStream(in, out);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
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

    private String getDate() {
        final DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date()) + "GMT";
    }
}
