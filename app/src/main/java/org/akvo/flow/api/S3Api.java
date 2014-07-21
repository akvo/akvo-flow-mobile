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

    private static final String GET = "GET";
    private static final String PREFIX_SURVEY = "surveys/";

    private PropertyUtil mProperties;
    private String mBucket;
    private String mAccessKey;
    private String mSecret;

    public S3Api(Context c) {
        mProperties = new PropertyUtil(c.getResources());
        mBucket = mProperties.getProperty(ConstantUtil.S3_BUCKET);
        mAccessKey = mProperties.getProperty(ConstantUtil.S3_ACCESSKEY);
        mSecret = mProperties.getProperty(ConstantUtil.S3_SECRET);
    }

    private boolean get(String objectKey, File dst) throws IOException {
        // Get date and signature
        final DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        final String d = df.format(new Date()) + "GMT";
        final String signature = getSignature(GET, objectKey, d);
        final URL url = new URL(String.format(Path.URL, mBucket, objectKey));

        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Date", d);
            conn.setRequestProperty("Authorization", "AWS " + mAccessKey + ":" + signature);

            in = new BufferedInputStream(conn.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(dst));

            byte[] b = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(b)) != -1) {
                out.write(b, 0, read);
            }

            int status = conn.getResponseCode();// TODO: Check for IOException
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

    public boolean downloadSurvey(String filename, File dst) throws IOException {
        return get(PREFIX_SURVEY + filename, dst);
    }

    private String getSignature(String method, String objectKey, String date) {
        final String payload = method + "\n\n\n" + date + "\n" + "/" + mBucket + "/" + objectKey;

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

    interface Path {
        String URL = "https://%s.s3.amazonaws.com/%s";
    }
}
