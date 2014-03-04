package org.akvo.flow.deploy;
/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.io.File;
import java.io.IOException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * Uploads a single fieldsurvey apk to s3. There are four arguments:
 * accessKey - S3 access key
 * secretKey - S3 secret key
 * instanceId - name of the instance, for example 'akvoflow-1'
 * apkPath - the local path to the apk file to be uploaded
 * 
 */
public class Deploy {

    public static void main(String[] args) throws IOException {  
        String bucketName = "akvoflow";
        
        System.out.println("===========================================");
        
        if (args.length != 4) {
        	System.out.println("Missing argument, please provide S3 access key, S3 secret key, instanceId and apkPath");
        	return;
        }
       
    	BasicAWSCredentials credentials = new BasicAWSCredentials(args[0],args[1]);
    	AmazonS3 s3 = new AmazonS3Client(credentials);
        
        File f = new File(args[3]);
        if (!f.exists()) {
        	System.out.println("Can't find apk at " + args[3]);
        	return;
        }
        // the path and name under which the apk will be stored on s3
        String s3Path = "apk/" + args[2] + "/" + f.getName();
 
        try {
        	PutObjectRequest putRequest = new PutObjectRequest(bucketName, s3Path, f);
        	ObjectMetadata metadata = new ObjectMetadata();
        	
        	// set content type as android package file
        	metadata.setContentType("application/vnd.android.package-archive");
        	
        	// set content length to length of file
        	metadata.setContentLength(f.length());
        	
        	// set access to public
        	putRequest.setMetadata(metadata);
        	putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        	
        	// try to put the apk in S3
        	PutObjectResult result = s3.putObject(putRequest);
        	System.out.println("Apk uploaded successfully, with result ETag " + result.getETag());
        	
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }
}
