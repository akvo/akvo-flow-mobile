#!/usr/bin/python
from hashlib import sha1
import hmac
import base64
import datetime
import urllib
import json

# Glimmer oh Hope
#BASE_URL = "http://flowaglimmerofhope.appspot.com/surveyedlocale"
#API_KEY = "4eh0Cy12010gvD88b4US5TL2ReKXwcA8"
#SURVEY_GROUP = "1083000"

# akvoflowsandbox
BASE_URL = "http://akvoflowsandbox.appspot.com/surveyedlocale"
API_KEY = "nk34aR11m9"
SURVEY_GROUP = "18693002"


PHONE_NUMBER = "07583453350"
IMEI = "353918051365464"


def get_ts():
    now = datetime.datetime.utcnow()
    return now.strftime("%Y/%m/%d %H:%M:%S")


def get_hash(query):
    digest = hmac.new(API_KEY, query, sha1).digest()
    return base64.b64encode(digest)


def sync(timestamp):
    query = "&".join([
        "imei=" + IMEI,
        "lastUpdateTime=" + timestamp,
        "phoneNumber=" + urllib.quote_plus(PHONE_NUMBER),
        "surveyGroupId=" + SURVEY_GROUP,
        "ts=" + urllib.quote_plus(get_ts()),]
    )

    url = BASE_URL + "?" + query + "&h=" + get_hash(query)

    print "URL: " + url
    res = urllib.urlopen(url)
    if res.getcode() != 200:
        print "Unexpected Response: %d - %s" % (res.getcode(), res.read())
        return

    data = res.read()
    print data

    jres = json.loads(data)
    locales = jres['surveyedLocaleData']
    if locales:
        timestamp = str(locales[-1]['lastUpdateDateTime'])
        sync(timestamp)


def main():
    sync('0')

if __name__ == "__main__":
    main()
