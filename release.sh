#!/bin/bash
set -e

formattedDate=$(date +%Y-%m-%d_%H-%M-%S)
versionName=$(git describe --abbrev=0)
filename="$HOME/akvo-flow-mobile-deployment-${versionName}-${formattedDate}.log"
echo "logs will be saved to: ${filename}"

if [[ "${SLACK_WEBHOOK_URL}" ]]; then
  curl -X POST -H 'Content-type: application/json' --data '{"text":"Will start release '$versionName'!"}' https://hooks.slack.com/services/${SLACK_WEBHOOK_URL}
else
  echo "Setup SLACK_WEBHOOK_URL to send messages to slack"
fi

util/upload-apk/script/flow-releases.sh |& tee ${filename}

a=$(grep -c 'generating apk version' ${filename})
b=$(grep -c 'New APK version successfully stored' ${filename})
c=$(cat tmp/instances.txt | grep -v ^$ | wc -l)

if [[ "${a}" == "${b}" ]] && [[ "${a}" == "${c}" ]]; then
  echo "Release successful"
else
  echo "Some instances failed"
  grep "Error updating APK version in GAE" ${filename}
fi
