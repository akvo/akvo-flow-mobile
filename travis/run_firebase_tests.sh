#!/usr/bin/env bash
set -e

$HOME/google-cloud-sdk/bin/gcloud firebase test android run --type instrumentation --app app/build/outputs/apk/flow/debug/app-flow-debug.apk --test app/build/outputs/apk/androidTest/flow/debug/app-flow-debug-androidTest.apk --device model=Nexus5,version=21,locale=en,orientation=portrait;