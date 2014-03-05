#!/bin/bash
set -e

# Script to automatically build and upload the FLOW apk. It needs to be run 
# from the /survey folder in the mobile repo holding the FLOW apk code.
# The script makes use of a jar file deploy.jar, which uploads the apk
# to S3 nd notifies GAE.
# To deploy FLOW, the following env variables must be set:
#
# FLOW_DEPLOY_JAR=/path/to/deploy.jar
# FLOW_SERVER_CONFIG=/path/to/akvo-flow-server-config
# FLOW_S3_ACCESS_KEY=your_S3_access_key
# FLOW_S3_SECRET_KEY=your_S3_secret_key
# FLOW_GAE_USERNAME=google_username
# FLOW_GAE_PASSWORD=google_password
#
# The scripts reads the version number of the apk directly from the versionName 
# property in AndroidManifest.xml

[[ -n "${FLOW_DEPLOY_JAR}" ]] || { echo "FLOW_DEPLOY_JAR env var needs to be set"; exit 1; }
[[ -n "${FLOW_SERVER_CONFIG}" ]] || { echo "FLOW_SERVER_CONFIG env var needs to be set"; exit 1; }
[[ -n "${FLOW_S3_ACCESS_KEY}" ]] || { echo "FLOW_S3_ACCESS_KEY env var needs to be set"; exit 1; }
[[ -n "${FLOW_S3_SECRET_KEY}" ]] || { echo "FLOW_S3_SECRET_KEY env var needs to be set"; exit 1; }
[[ -n "${FLOW_GAE_USERNAME}" ]] || { echo "FLOW_GAE_USERNAME env var needs to be set"; exit 1; }
[[ -n "${FLOW_GAE_PASSWORD}" ]] || { echo "FLOW_GAE_PASSWORD env var needs to be set"; exit 1; }

VERSION=$(sed -n '/android:versionName="/{;s///;s/".*$//;p;d;}' AndroidManifest.xml | tr -d ' ')

rm -rf tmp
rm -rf builds
mkdir tmp
mkdir builds

find $FLOW_SERVER_CONFIG/ -name 'appengine-web.xml' -exec sed -n 's/\(.*\)<application>\(.*\)<\/application>\(.*\)/\2/p' {} \; | sort > tmp/instances.txt
for i in $(cat tmp/instances.txt); do 
    rm -rf bin
    rm -rf gen
    echo '=================================================='
    if [[ -f $FLOW_SERVER_CONFIG/$i/survey.properties ]]; then
        echo 'generating apk version' $VERSION 'for instance' $i
        ant flow-release -Dsurvey.properties=$FLOW_SERVER_CONFIG/$i/survey.properties >> tmp/antout.txt
        mkdir -p builds/$i/$VERSION
        mv bin/fieldsurvey-*.apk builds/$i/$VERSION/
        java -jar $FLOW_DEPLOY_JAR $FLOW_S3_ACCESS_KEY $FLOW_S3_SECRET_KEY $i builds/$i/$VERSION/fieldsurvey-$VERSION.apk $VERSION $FLOW_GAE_USERNAME $FLOW_GAE_PASSWORD
    else
        echo 'Cannot find survey.properties file for instance' $i
    fi
done
