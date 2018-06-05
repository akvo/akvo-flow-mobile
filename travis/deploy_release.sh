#!/usr/bin/env bash
set -e

[[ -n "${GITHUB_API_KEY}" ]] || { echo "GITHUB_API_KEY env var needs to be set"; exit 1; }

git clone https://valllllll2000:$GITHUB_API_KEY@github.com/akvo/akvo-flow-server-config.git
export FLOW_SERVER_CONFIG=akvo-flow-server-config

util/upload-apk/script/flow-releases.sh $1