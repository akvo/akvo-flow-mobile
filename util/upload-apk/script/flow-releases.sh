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

[[ -n "${FLOW_MOBILE}" ]] || { echo "FLOW_MOBILE env var needs to be set"; exit 1; }
[[ -n "${FLOW_DEPLOY_JAR}" ]] || { echo "FLOW_DEPLOY_JAR env var needs to be set"; exit 1; }
[[ -n "${FLOW_SERVER_CONFIG}" ]] || { echo "FLOW_SERVER_CONFIG env var needs to be set"; exit 1; }
[[ -n "${FLOW_S3_ACCESS_KEY}" ]] || { echo "FLOW_S3_ACCESS_KEY env var needs to be set"; exit 1; }
[[ -n "${FLOW_S3_SECRET_KEY}" ]] || { echo "FLOW_S3_SECRET_KEY env var needs to be set"; exit 1; }
[[ -n "${FLOW_GAE_USERNAME}" ]] || { echo "FLOW_GAE_USERNAME env var needs to be set"; exit 1; }
[[ -n "${FLOW_GAE_PASSWORD}" ]] || { echo "FLOW_GAE_PASSWORD env var needs to be set"; exit 1; }

# Move to the project directory
cd $FLOW_MOBILE

version=$(sed -n "/android:versionName=\"/{;s///;s/\".*$//;p;d;}" app/src/main/AndroidManifest.xml | tr -d " ")

rm -rf tmp
rm -rf builds
mkdir tmp
mkdir builds

# If an argument is provided with the instance name, deploy only that instance APK. Otherwise, deploy all of them
if [[ -n "$1" ]]; then
    echo $1 > tmp/instances.txt
else
    find $FLOW_SERVER_CONFIG/ -name "appengine-web.xml" -exec sed -n "s/\(.*\)<application>\(.*\)<\/application>\(.*\)/\2/p" {} \; | sort > tmp/instances.txt
fi

for i in $(cat tmp/instances.txt); do 
    rm -rf bin
    echo "=================================================="
    if [[ -f $FLOW_SERVER_CONFIG/$i/survey.properties ]]; then
        filename=builds/$i/$version/flow-$version.apk
        echo "generating apk version" $version "for instance" $i
        cp $FLOW_SERVER_CONFIG/$i/survey.properties app/src/main/res/raw/survey.properties
        ./gradlew assembleRelease
        mkdir -p builds/$i/$version
        mv app/bin/flow.apk $filename
        java -jar "$FLOW_DEPLOY_JAR" "$FLOW_S3_ACCESS_KEY" "$FLOW_S3_SECRET_KEY" "$i" "$filename" "$version" "$FLOW_GAE_USERNAME" "$FLOW_GAE_PASSWORD"
    else
        echo "Cannot find survey.properties file for instance" $i
    fi
done
