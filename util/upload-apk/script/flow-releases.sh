#!/bin/bash

# Script to automatically build and upload the FLOW apk. It needs to be run 
# from the /survey folder in the mobile repo holding the FLOW apk code.
# The script makes use of a jar file uploadS3.jar, which uploads the apk
# to S3. The S3_ACCESS_KEY and S3_SECRET_KEY need to be replaced by the proper
# credential values for S3. 

# The scripts reads the version number of the apk directly from the versionName 
# property in AndroidManifest.xml

UPLOAD_S3_PATH=/path/to/uploadS3jar
APK_BUILD_DIR=/path/to/apk/build/directory
SERVER_CONFIG_PATH=/path/to/akvo-flow-server-config/repo

# Configuration
S3_ACCESS_KEY=s3_access_key
S3_SECRET_KEY=s3_secret_key
GAE_USERNAME=username
GAE_PASSWORD=password

VERSION=$(sed -n '/android:versionName="/{;s///;s/".*$//;p;d;}' AndroidManifest.xml | tr -d ' ')

rm -r tmp
rm -r builds
mkdir tmp
mkdir builds


find $SERVER_CONFIG_PATH/ -name 'appengine-web.xml' -exec sed -n 's/\(.*\)<application>\(.*\)<\/application>\(.*\)/\2/p' {} \; | sort > tmp/instances.txt
for i in $(cat tmp/instances.txt); do 
    rm -r bin
    rm -r gen
    echo '=================================================='
    if [ -f $SERVER_CONFIG_PATH/$i/survey.properties ]; then
        echo 'generating apk version' $VERSION 'for instance' $i
        ant flow-release -Dsurvey.properties=$SERVER_CONFIG_PATH/$i/survey.properties >> tmp/antout.txt
        mkdir -p builds/$i/$VERSION
        mv bin/fieldsurvey-*.apk builds/$i/$VERSION/
        echo 'Deploying APK...'
        java -jar $UPLOAD_S3_PATH/deploy.jar $S3_ACCESS_KEY $S3_SECRET_KEY $i $APK_BUILD_DIR/$i/$VERSION/fieldsurvey-$VERSION.apk $VERSION $GAE_USERNAME $GAE_PASSWORD
    else
        'Cannot find survey.properties file for instance' $i
    fi
done
