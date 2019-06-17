#!/usr/bin/env bash
set -e

if [[ -z "$1" ]]
  then
    echo "At least one instance needs to be provided"
    exit 1
fi

[[ -n "${GITHUB_API_KEY}" ]] || { echo "GITHUB_API_KEY env var needs to be set"; exit 1; }
[[ -n "${GITHUB_USER}" ]] || { echo "GITHUB_USER env var needs to be set"; exit 1; }

git clone https://$GITHUB_USER:$GITHUB_API_KEY@github.com/akvo/akvo-flow-server-config.git
export FLOW_SERVER_CONFIG=akvo-flow-server-config

util/upload-apk/script/flow-releases.sh $1