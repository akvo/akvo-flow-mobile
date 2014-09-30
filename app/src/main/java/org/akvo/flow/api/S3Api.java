package org.akvo.flow.api;

import android.content.Context;
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
    private static final String URL = "https://%s.s3.amazonaws.com/%s";
    private static final String PAYLOAD_GET = "GET\n\n\n%s\n/%s/%s";// date, bucket, obj
    private static final String PAYLOAD_PUT_PUBLIC = "PUT\n%s\n%s\n%s\nx-amz-acl:public-read\n/%s/%s";// md5, type, date, bucket, obj
    private static final String PAYLOAD_PUT_PRIVATE = "PUT\n%s\n%s\n%s\n/%s/%s";// md5, type, date, bucket, obj

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

            copyStream(in, out);

            int status = conn.getResponseCode();
            if (status != HttpStatus.SC_OK) {
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

            copyStream(in, out);
            out.flush();

            int status = conn.getResponseCode();
            if (status != 200 && status != 201) {
                Log.e(TAG, "Status Code: " + status + ". Expected: 200 or 201");
                return false;
            }
            String etag = conn.getHeaderField("ETag");
            etag = etag != null ? etag.replaceAll("\"", "") : null;// Remove quotes
            if (!md5Hex.equals(etag)) {
                Log.e(TAG, "ETag comparison failed. Response ETag: " + etag +
                        "Locally computed MD5: " + md5Hex);
                return false;
            }
            Log.d(TAG, "File successfully uploaded: " + file.getName());
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

}
