/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.util.Base64;

import org.akvo.flow.exception.HttpException;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PropertyUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class S3Api {
    private static final String URL = "https://%s.s3.amazonaws.com/%s";
    private static final String PAYLOAD_GET = "GET\n\n\n%s\n/%s/%s";// date, bucket, obj
    private static final String PAYLOAD_PUT_PUBLIC = "PUT\n%s\n%s\n%s\nx-amz-acl:public-read\n/%s/%s";// md5, type, date, bucket, obj
    private static final String PAYLOAD_PUT_PRIVATE = "PUT\n%s\n%s\n%s\n/%s/%s";// md5, type, date, bucket, obj
    private static final String PAYLOAD_HEAD = "HEAD\n\n\n%s\n/%s/%s";// date, bucket, obj

    private String mBucket;
    private String mAccessKey;
    private String mSecret;

    public S3Api(Context c) {
        PropertyUtil properties = new PropertyUtil(c.getResources());
        mBucket = properties.getProperty(ConstantUtil.S3_BUCKET);
        mAccessKey = properties.getProperty(ConstantUtil.S3_ACCESSKEY);
        mSecret = properties.getProperty(ConstantUtil.S3_SECRET);
    }

    public String getEtag(String objectKey) throws IOException {
        // Get date and signature
        final String date = getDate();
        final String payload = String.format(PAYLOAD_HEAD, date, mBucket, objectKey);
        final String signature = getSignature(payload);
        final URL url = new URL(String.format(URL, mBucket, objectKey));

        HttpURLConnection conn = null;
        String etag = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Date", date);
            conn.setRequestProperty("Authorization", "AWS " + mAccessKey + ":" + signature);
            // Handle EOS bug in Android pre Jelly Bean: https://code.google.com/p/android/issues/detail?id=24672
            conn.setRequestProperty("Accept-Encoding", "");
            conn.setRequestMethod("HEAD");

            if (conn.getResponseCode() == 200) {
                etag = getEtag(conn);
            }
            return etag;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public void syncFile(String objectKey, File dst) throws IOException {
        final String etag = getEtag(objectKey);
        if (etag == null) {
            throw new HttpException("Could not read ETag from object: " + objectKey, 404);
        }
        if (dst.exists() && etag.equals(FileUtil.hexMd5(dst))) {
            // No need to re-fetch the file. The integrity of the local copy has been verified
            return;
        }
        get(objectKey, dst);
    }

    public void get(String objectKey, File dst) throws IOException {
        // Get date and signature
        final String date = getDate();
        final String payload = String.format(PAYLOAD_GET, date, mBucket, objectKey);
        final String signature = getSignature(payload);
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

    public boolean put(String objectKey, File file, String type, boolean isPublic) throws IOException {
        // Calculate data size, up to 2 GB
        final int size = file.length() < Integer.MAX_VALUE ? (int)file.length() : -1;

        // Get date and signature
        final byte[] rawMd5 = FileUtil.getMD5Checksum(file);
        final String md5Base64 = Base64.encodeToString(rawMd5, Base64.NO_WRAP);
        final String md5Hex = FileUtil.hexMd5(rawMd5);
        final String date = getDate();
        String payloadStr = isPublic ? PAYLOAD_PUT_PUBLIC : PAYLOAD_PUT_PRIVATE;
        final String payload = String.format(payloadStr, md5Base64, type, date, mBucket, objectKey);
        final String signature = getSignature(payload);
        final URL url = new URL(String.format(URL, mBucket, objectKey));

        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            if (size > 0) {
                conn.setFixedLengthStreamingMode(size);
            } else {
                conn.setChunkedStreamingMode(0);
            }
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-MD5", md5Base64);
            conn.setRequestProperty("Content-Type", type);
            conn.setRequestProperty("Date", date);
            if (isPublic) {
                // If we don't send this header, the object will be private by default
                conn.setRequestProperty("x-amz-acl", "public-read");
            }
            conn.setRequestProperty("Authorization", "AWS " + mAccessKey + ":" + signature);

            in = new BufferedInputStream(new FileInputStream(file));
            out = new BufferedOutputStream(conn.getOutputStream());

            HttpUtil.copyStream(in, out);
            out.flush();

            int status = conn.getResponseCode();
            if (status != 200 && status != 201) {
                Timber.e("Status Code: " + status + ". Expected: 200 or 201");
                return false;
            }
            String etag = getEtag(conn);
            if (!md5Hex.equals(etag)) {
                Timber.e("ETag comparison failed. Response ETag: " + etag +
                        "Locally computed MD5: " + md5Hex);
                return false;
            }
            Timber.d("File successfully uploaded: " + file.getName());
            return true;
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

    private String getSignature(String payload) {
        try {
            Key signingKey = new SecretKeySpec(mSecret.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(payload.getBytes());
            return Base64.encodeToString(rawHmac, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getEtag(HttpURLConnection conn) {
        String etag = conn.getHeaderField("ETag");
        return etag != null ? etag.replaceAll("\"", "") : null;// Remove quotes
    }

}
