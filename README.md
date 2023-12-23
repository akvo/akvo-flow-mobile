# Akvo Flow app 

[![Build Status](https://app.bitrise.io/app/fc66e09f794d97d7/status.svg?token=dz2zzeVH1ddhA27HNFOwFA)](https://app.bitrise.io/app/fc66e09f794d97d7) [![Build Status](https://travis-ci.org/akvo/akvo-flow-mobile.svg?branch=develop)](https://travis-ci.org/akvo/akvo-flow-mobile) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/eec7b9c7849f458188fd5ad624355517)](https://www.codacy.com/manual/Akvo/akvo-flow-mobile?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=akvo/akvo-flow-mobile&amp;utm_campaign=Badge_Grade) [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/eec7b9c7849f458188fd5ad624355517)](https://www.codacy.com/manual/Akvo/akvo-flow-mobile?utm_source=github.com&utm_medium=referral&utm_content=akvo/akvo-flow-mobile&utm_campaign=Badge_Coverage) [![API](https://img.shields.io/badge/API-15%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=15) [![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0)

The Akvo Flow app is the mobile application for the [Akvo Flow data collection tool](https://github.com/akvo/akvo-flow).

Akvo Flow was built specially for international development teams to monitor and evaluate initiatives while working in diverse locations that are often remote or lacking reliable basic infrastructure. It is designed for people who want to understand what is happening now, so they can make informed decisions that lead to secure, sustainable development aid investments.

## Getting Started

To set up your development environment you'll need:

* JDK 11
* Before installing Android Studio, please see [Installation Requirements](https://developer.android.com/studio/install.html) for your OS.
* Download [Android Studio](http://developer.android.com/sdk/index.html). The first time you launch Android Studio you will be required to download the Android SDK, ensure you have the latest version of the **Android SDK Tools** and the **Android SDK Build-Tools**. You will also need to check out the **Android Support Library**, **Android Support Repository** and **Google Play Services**.

## Importing the Project in Android Studio

* Click `File` > `New` >`Import Project...` and just select the root directory of the repository.

**Note:** For an overview on how Android Studio manages the Project structure, see the [official documentation](https://developer.android.com/studio/intro/index.html)

## Building the app

* Before building the app, ensure the corresponding `survey.properties` file is located in `app/`.
* In order to build for release, you will need to set up the `gradle.properties` file in the `app` module. Just copy the sample `gradle.properties.template` file without the *.template* extension, and edit the values according to your local environment.
* To build a **release** version of the app, for example for the **flow** flavour: `./gradlew assembleFlowRelease` (or simply `./gradlew aFR`). The generated APK will be located in the `app/build/outputs/apk/flow/release/` folder.

## Contributing

To ensure a consistent code style throughout the codebase, we stick to the [Android Code Style Guidelines]
(http://source.android.com/source/code-style.html) as much as possible.

Please import the [util/ide/android-studio/Akvo Java Source Formatting.xml](https://github.com/akvo/akvo-flow-mobile/blob/issue/607-update-readme/util/ide/android-studio/Akvo%20Java%20Source%20Formatting.xml) settings into your Android Studio. Go to `File` > `Settings` > `Editor` > `Code Style` > `Java`, select `Manage` and then `Import`.

## License

Licensed under the GNU General Public License (GPL) v3.
For more information see [LICENSE.txt] (https://github.com/akvo/akvo-flow-mobile/blob/develop/LICENSE.txt).
