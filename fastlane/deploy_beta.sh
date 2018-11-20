#!/usr/bin/env bash

if [[ "$TRAVIS_BRANCH" == release* ]] || [[ "$TRAVIS_BRANCH" == hotfix* ]] ; then fastlane beta
fi
