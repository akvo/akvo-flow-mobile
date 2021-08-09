#!/bin/bash
set -e

# Script to automatically build and upload the FLOW apks. It needs to be run
# from folder in the mobile repo holding the FLOW apk code.
# The script makes use of a jar file deploy-vxx.jar, which uploads the apk
# to S3 and notifies GAE.
# To deploy FLOW, the following env variables must be set:
#
# FLOW_S3_ACCESS_KEY=your_S3_access_key
# FLOW_S3_SECRET_KEY=your_S3_secret_key
# FLOW_SERVER_CONFIG=/path/to/akvo-flow-server-config
#

. release/upload-apk/version.properties

FLOW_DEPLOY_JAR="util/upload-apk/build/libs/deploy-${VERSION}.jar"

[[ -n "${FLOW_S3_ACCESS_KEY}" ]] || { echo "FLOW_S3_ACCESS_KEY env var needs to be set"; exit 1; }
[[ -n "${FLOW_S3_SECRET_KEY}" ]] || { echo "FLOW_S3_SECRET_KEY env var needs to be set"; exit 1; }
[[ -n "${FLOW_SERVER_CONFIG}" ]] || { echo "FLOW_SERVER_CONFIG env var needs to be set"; exit 1; }

. app/version.properties
version=${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}

rm -rf tmp
rm -rf builds
mkdir tmp
mkdir builds

build_name() {
    if [[ "$1" == "akvoflow-89" ]]; then
        echo "assembleBiogasRelease"
    elif [[ "$1" == "akvoflow-101" ]]; then
        echo "assembleCookstovesRelease"
    else
        echo "assembleFlowRelease"
    fi
}

flavor() {
    if [[ "$1" == "akvoflow-89" ]]; then
        echo "biogas"
    elif [[ "$1" == "akvoflow-101" ]]; then
        echo "cookstoves"
    else
        echo "flow"
    fi
}

# If an argument is provided with the instance name, deploy only that instance APK. Otherwise, deploy all of them
if [[ -n "$1" ]]; then
    echo $1 > tmp/instances.txt
else
    find $FLOW_SERVER_CONFIG/ -name "appengine-web.xml" -exec sed -n "s/\(.*\)<application>\(.*\)<\/application>\(.*\)/\2/p" {} \; | sort > tmp/instances.txt
fi

for i in $(cat tmp/instances.txt); do 
    rm -rf bin
    echo "=================================================="
    if [[ -f $FLOW_SERVER_CONFIG/$i/survey.properties && -f $FLOW_SERVER_CONFIG/$i/$i.p12 ]]; then
        accountId=$(sed -n "s/\(.*\)name=\"serviceAccountId\"[[:space:]]*value=\"\(.*\)\"\(.*\)/\2/p" $FLOW_SERVER_CONFIG/$i/appengine-web.xml)
        accountSecret=$FLOW_SERVER_CONFIG/$i/$i.p12
        filename=builds/$i/$version/flow-$version.apk
        build=$(build_name $i)
        flavor=$(flavor $i)

        echo "generating apk version" $version "for instance" $i "and" $build
        cp $FLOW_SERVER_CONFIG/$i/survey.properties app/survey.properties

        ./gradlew $build -Pnodexcount=true
        mkdir -p builds/$i/$version
        mv app/build/outputs/apk/$flavor/release/app-$flavor-release.apk $filename
        java -jar "$FLOW_DEPLOY_JAR" "$FLOW_S3_ACCESS_KEY" "$FLOW_S3_SECRET_KEY" "$i" "$filename" "$version" "$accountId" "$accountSecret"
    else
        echo "Cannot find survey.properties or p12 file for instance" $i
    fi
done
