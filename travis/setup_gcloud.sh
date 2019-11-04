#!/usr/bin/env bash
set -e

gcloud version || true

if [[ ! -d ${HOME}/google-cloud-sdk/bin ]]; then
      rm -rf $HOME/google-cloud-sdk;
      curl https://sdk.cloud.google.com | bash;
fi

$HOME/google-cloud-sdk/bin/gcloud version
$HOME/google-cloud-sdk/bin/gcloud auth activate-service-account firebase-adminsdk-48ycx@akvoflowapp.iam.gserviceaccount.com --key-file=./akvoflowapp-firebase-adminsdk-48ycx-c664041e03.json
$HOME/google-cloud-sdk/bin/gcloud config set project akvoflowapp --quiet
