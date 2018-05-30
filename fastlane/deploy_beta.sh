#!/usr/bin/env bash

if [[ "$TRAVIS_BRANCH" == release* ]]; then fastlane beta
fi
