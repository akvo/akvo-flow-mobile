/*
 * Copyright 2010-2014,2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.akvo.flow.deploy;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Uploads a single Akvo FLOW APK to s3. There are seven arguments: - accessKey
 * - S3 access key - secretKey - S3 secret key - instanceId - name of the
 * instance, - apkPath - the local path to the APK file to be - version - APK
 * version name - username - Google Account username - password - Google Account
 * password
 */
public class Deploy {
    private static final int S3_ACCESS_KEY = 0;
    private static final int S3_SECRET_KEY = 1;
    private static final int INSTANCE_ID = 2;
    private static final int APK_PATH = 3;
    private static final int VERSION = 4;
    private static final int ACCOUNT_ID = 5;
    private static final int ACCOUNT_SECRET = 6;

    private static final String BUCKET_NAME = "akvoflow";

    public static void main(String[] args) throws IOException {
        if (args.length != 7) {
            System.err.println("Missing argument, please provide S3 access key, S3 secret key, "
                    + "instanceId , apkPath, version, GAE username and GAE password");
            return;
        }

        File file = new File(args[APK_PATH]);
        if (!file.exists()) {
            System.err.println("Can't find apk at " + args[APK_PATH]);
            return;
        }

        final String accessKey = args[S3_ACCESS_KEY];
        final String secretKey = args[S3_SECRET_KEY];
        final String instance = args[INSTANCE_ID];
        final String accountId = args[ACCOUNT_ID];
        final String accountSecret = args[ACCOUNT_SECRET];
        final String version = args[VERSION];

        final String s3Path = "apk/" + instance + "/" + file.getName();
        final String s3Url = "https://app.akvoflow.org/" + instance + '/'
                + file.getName().replaceAll(".apk$", "");
        final String host = instance + ".appspot.com";

        try {
            uploadS3(accessKey, secretKey, s3Path, file);
            updateVersion(host, accountId, accountSecret, s3Url, version, getMD5Checksum(file),
                    instance);
        } catch (AmazonServiceException ase) {
            System.err
                    .println("Caught an AmazonServiceException, which means your request made it "
                            + "to Amazon S3, but was rejected with an error response for some reason.");
            System.err.println("Error Message:    " + ase.getMessage());
            System.err.println("HTTP Status Code: " + ase.getStatusCode());
            System.err.println("AWS Error Code:   " + ase.getErrorCode());
            System.err.println("Error Type:       " + ase.getErrorType());
            System.err.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.err
                    .println("Caught an AmazonClientException, which means the client encountered "
                            + "a serious internal problem while trying to communicate with S3, "
                            + "such as not being able to access the network.");
            System.err.println("Error Message: " + ace.getMessage());
        } catch (IOException e) {
            System.err.println("Error updating APK version in GAE for " + instance);
            e.printStackTrace();
        }

    }

    private static void uploadS3(String accessKey, String secretKey, String s3Path, File file)
            throws AmazonServiceException, AmazonClientException {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 s3 = new AmazonS3Client(credentials);

        PutObjectRequest putRequest = new PutObjectRequest(BUCKET_NAME, s3Path, file);
        ObjectMetadata metadata = new ObjectMetadata();

        // set content type as android package file
        metadata.setContentType("application/vnd.android.package-archive");

        // set content length to length of file
        metadata.setContentLength(file.length());

        // set access to public
        putRequest.setMetadata(metadata);
        putRequest.setCannedAcl(CannedAccessControlList.PublicRead);

        // try to put the apk in S3
        PutObjectResult result = s3.putObject(putRequest);
        System.out.println("Apk uploaded successfully, with result ETag " + result.getETag());
    }

    private static void updateVersion(String host, String accountId, String accountSecret,
            String url, String version, String md5, String instance) throws IOException {
        RemoteApiOptions options = new RemoteApiOptions().server(host, 443)
                .useServiceAccountCredential(accountId, accountSecret);
        RemoteApiInstaller installer = new RemoteApiInstaller();
        installer.install(options);
        try {
            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

            Entity e = new Entity("DeviceApplication");
            e.setProperty("appCode", "flowapp");
            e.setProperty("deviceType", "androidPhone");
            e.setProperty("version", version);
            e.setProperty("fileName", url);
            e.setProperty("md5Checksum", md5);

            final Date date = new Date(); // use the same timestampt
            e.setProperty("createdDateTime", date);
            e.setProperty("lastUpdateDateTime", date);
            ds.put(e);
        } finally {
            installer.uninstall();
        }
        System.out.println("New APK version successfully stored in GAE for instance: " + instance);
    }

    private static String getMD5Checksum(File file) {
        InputStream in = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            in = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            byte[] rawHash = md.digest();

            StringBuilder builder = new StringBuilder();
            for (byte b : rawHash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
                //Ignored
            }
        }
        return null;
    }
}
