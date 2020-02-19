#!/bin/bash
set -e

formattedDate=$(date +%Y-%m-%d_%H-%M-%S)
versionName=$(git describe --abbrev=0)
filename="$HOME/akvo-flow-mobile-deployment-${versionName}-${formattedDate}.log"
echo "logs will be saved to: ${filename}"
util/upload-apk/script/flow-releases.sh |& tee ${filename}