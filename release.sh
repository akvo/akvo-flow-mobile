#!/bin/bash
set -e

formattedDate=$(date +%Y-%m-%d_%H-%M-%S)
versionName=$(git describe --abbrev=0)
filename="$HOME/akvo-flow-mobile-deployment-${versionName}-${formattedDate}.log"
echo "logs will be saved to: ${filename}"

stream="K2 Engine"
zulip_release_started() {
  if [[ "${ZULIP_URL}" ]]; then
    curl -X POST https://akvo.zulipchat.com/api/v1/messages \
    -u ${ZULIP_URL} \
    -d "type=stream" \
    -d $"to=$stream" \
    -d "subject=Releases" \
    -d $"content=Releasing flow app $versionName"
  else
    echo "setup ZULIP_URL to send messages to zulip"
  fi
}

zulip_release_completed() {
  if [[ "${ZULIP_URL}" ]]; then
    curl -X POST https://akvo.zulipchat.com/api/v1/messages \
    -u ${ZULIP_URL} \
    -d "type=stream" \
    -d $"to=$stream"  \
    -d "subject=Releases" \
    -d "content=App release successful"
  else
    echo "setup ZULIP_URL to send messages to zulip"
  fi
}

zulip_release_started
util/upload-apk/script/flow-releases.sh |& tee ${filename}

a=$(grep -c 'generating apk version' ${filename})
b=$(grep -c 'New APK version successfully stored' ${filename})
c=$(cat tmp/instances.txt | grep -v ^$ | wc -l)

if [[ "${a}" == "${b}" ]] && [[ "${a}" == "${c}" ]]; then
  echo "Release successful"
  zulip_release_completed
else
  echo "Some instances failed"
  grep "Error updating APK version in GAE" ${filename}
fi
