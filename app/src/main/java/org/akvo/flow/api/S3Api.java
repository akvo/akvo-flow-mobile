package org.akvo.flow.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.PropertyUtil;
import org.apache.http.HttpStatus;

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
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class S3Api {
    private static final String TAG = S3Api.class.getSimpleName();

    private static final int BUFFER_SIZE = 8192;

    private String mBucket;
    private String mAccessKey;
    private String mSecret;

    public S3Api(Context c) {
        PropertyUtil properties = new PropertyUtil(c.getResources());
        mBucket = properties.getProperty(ConstantUtil.S3_BUCKET);
        mAccessKey = properties.getProperty(ConstantUtil.S3_ACCESSKEY);
        mSecret = properties.getProperty(ConstantUtil.S3_SECRET);
    }

    public boolean get(String objectKey, File dst) throws IOException {
        // Get date and signature
        final String date = getDate();
        final String payload = "GET\n\n\n" + date + "\n" + "/" + mBucket + "/" + objectKey;
        final String signature = getSignature(payload);
        final URL url = new URL(String.format(Path.URL, mBucket, objectKey));

        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Date", date);
            conn.setRequestProperty("Authorization", "AWS " + mAccessKey + ":" + signature);

            in = new BufferedInputStream(conn.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(dst));

            copyStream(in, out);

            int status = conn.getResponseCode();
            if (status != HttpStatus.SC_OK) {
                Log.e(TAG, "Status Code: " + status + ". Expected: 200 - OK");
                return false;
            }
            return true;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            FileUtil.close(in);
            FileUtil.close(out);
        }
    }

    public boolean put(String objectKey, File file, String type, boolean isPublic) throws IOException {
        // Get date and signature
        final String date = getDate();
        final String payload = "PUT\n\n" + type + "\n" + date + "\n" + "/" + mBucket + "/" + objectKey;
        final String signature = getSignature(payload);
        final URL url = new URL(String.format(Path.URL, mBucket, objectKey));

        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("ETag", FileUtil.getMD5Checksum(file));
            conn.setRequestProperty("Date", date);
            conn.setRequestProperty("Content-Type", type);
            conn.setRequestProperty("Authorization", "AWS " + mAccessKey + ":" + signature);
            if (isPublic) {
                // If we don't send this header, the object will be private by default
                conn.setRequestProperty("x-amz-acl", "public-read");
            }

            in = new BufferedInputStream(new FileInputStream(file));
            out = new BufferedOutputStream(conn.getOutputStream());

            copyStream(in, out);
            out.flush();

            int status = conn.getResponseCode();
            if (status >= 400) {
                Log.e(TAG, "Status Code: " + status + ". Expected: 2XX");
                return false;
            }
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
        final DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ");
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

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    interface Path {
        String URL = "https://%s.s3.amazonaws.com/%s";
    }
}
