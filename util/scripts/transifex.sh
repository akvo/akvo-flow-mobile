#!/bin/bash

# pulls the latests strings from transifex website
# tested on Ubuntu 16.10
# Prerequisites:
# you need to have your transifex-client installed see https://docs.transifex.com/client/installing-the-client
# you also better create an API token see here https://docs.transifex.com/api/introduction

TX_FOLDER='.tx'
LANGUAGES='fr,pt,vi,ne,km,hi,id,es'
APP_TRANSIFEX_URL='https://www.transifex.com/akvo-foundation/akvo-flow-mobile/stringsxml/'
TRANSLATIONS_FOLDER='translations'
STRINGS_FOLDER='akvo-flow-mobile.stringsxml'
RES_FOLDER='res'
VALUES_FOLDER='values'

if [ -e "$TX_FOLDER" ]; then
   echo "tx already setup"
else
   echo "running initial tx setup"
   tx init
   tx set --auto-remote ${APP_TRANSIFEX_URL}
fi

rm -rf ${TRANSLATIONS_FOLDER}

# pull all the translation files for the following languages
tx pull -l ${LANGUAGES}

cd ${TRANSLATIONS_FOLDER}/${STRINGS_FOLDER}
rm -rf ${RES_FOLDER}
mkdir ${RES_FOLDER}

# for each xml language file -> create values-CODE directory and place the correcponding strings.xml file there
# original xml translations come named like fr.xml or pt.xml
for entry in *.xml
do
  mkdir ${RES_FOLDER}/${VALUES_FOLDER}-${entry%%.*}
  mv ${entry} ${RES_FOLDER}/${VALUES_FOLDER}-${entry%%.*}/strings.xml
done

# rename the indonesian as app uses 'in'
mv ${RES_FOLDER}/${VALUES_FOLDER}-id ${RES_FOLDER}/${VALUES_FOLDER}-in

# update the changes with app's res folder
rsync -avhu --progress ${RES_FOLDER} ../../../../app/src/main/




