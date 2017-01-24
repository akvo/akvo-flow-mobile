# Akvo Flow app

The Akvo Flow app is the mobile application for data collection.

Akvo Flow is a tool built specially for international development teams to monitor and evaluate initiatives while working in diverse locations that are often remote or lacking reliable basic infrastructure. It is designed for people who want to understand what is happening now, so they can make informed decisions that lead to secure, sustainable development aid investments.

## Getting Started

To set up your development environment you'll need:

* [Oracle Java 8 SDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) is recommended, but open JDK 8 may also work.
* Before installing Android Studio, please see [Installation Requirements](https://developer.android.com/studio/install.html) for your OS.
* Download [Android Studio](http://developer.android.com/sdk/index.html). The first time you launch Android Studio you will be required to download the Android SDK, ensure you have at least the version 25.0.3 of the **Android SDK Tools** and the version 25.0.1 of the **Android SDK Build-Tools**. You will also need to check out the **Android Support Library**, **Android Support Repository** and **Google Play Services**.
* [Optional] [Gradle](http://www.gradle.org/): You can either use a local Gradle installation, or just use the bundled binaries (**recommended**).

## Importing the Project in Android Studio

* Click `File` > `New` >`Import Project...` and just select the root directory of the repository.

**Note:** For an overview on how Android Studio manages the Project structure, see the [official documentation](https://developer.android.com/studio/intro/index.html)

## Building the app

* Before building the app, ensure the corresponding `survey.properties` file is located in `app/src/main/res/raw/`.
* You will need to set up the `gradle.properties` file in the `app` module. Just copy the sample `gradle.properties.template` file without the *.template* extension, and edit the values according to your local environment.
* To build a **release** version of the app, for example for the **flow** flavour: `./gradlew assembleFlowRelease` (or simply `./gradlew aFR`). The generated APK will be located in the `app/bin` folder.

## Contributing

To ensure a consistent code style throughout the codebase, we stick to the [Android Code Style Guidelines]
(http://source.android.com/source/code-style.html) as much as possible.

Please import the 'util/ide/android-studio/Akvo Java Source Formatting.xml' settings into your Android Studio. Go to `File` > `Settings` > `Editor` > `Code Style` > `Java`, select `Manage` and then `Import`.

## License

Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)

Akvo Flow is free software: you can redistribute it and modify it under the terms of the GNU General Public License (GPL) as published by the Free Software Foundation, either version 3 of the License or any later version.

Akvo Flow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License in the file LICENSE.txt.

The license can also be seen at http://www.gnu.org/licenses/gpl.html.
