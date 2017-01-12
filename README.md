# Akvo Flow app

The Akvo Flow app is the mobile application for data collection.

> Akvo Flow is a tool built specially for international development teams to monitor and evaluate initiatives while working in diverse locations that are often remote or lacking reliable basic infrastructure. It is designed for people who want to understand what is happening now, so they can make informed decisions that lead to secure, sustainable development aid investments.

Getting Started
---------------

To set up your development environment you'll need:

* [Android SDK](http://developer.android.com/sdk/index.html): With the **SDK Manager**, ensure you have at least the version 23.0.2 of the **Android SDK Tools** and the version 20 of the **Android SDK Build-Tools**. You will also need to check out the **Android Support Library**, **Android Support Repository** and **Google Play Services**.
* [Optional] [Android Studio](https://developer.android.com/sdk/installing/studio.html) or [Eclipse](http://eclipse.org/) (using the [Gradle Plugin](http://www.gradle.org/docs/current/userguide/eclipse_plugin.html)) (Given the built-in Gradle support, **Android Studio is recommended**).
* [Optional] [Gradle](http://www.gradle.org/): You can either use a local Gradle installation, or just use the bundled binaries (**recommended**).

Importing the Project in Android Studio
---------------------------------------

* Click `File` > `Import Project...` and select the `build.gradle` file from the root directory of the repository. This will recursively import the *modules* of the app.

**Note:** For an overview on how Android Studio manages the Project structure, see the [official documentation](https://developer.android.com/sdk/installing/studio.html)

Building the app
----------------

* Before building the app, ensure the corresponding `survey.properties` file is located in `app/src/main/res/raw/`.
* In order to automatically sign the generated APKs, we will need to set up the `gradle.properties` file in the `app` module. Just copy the sample `gradle.properties.template` file without the *.template* extension, and edit the values according to your local environment.
* To build a **release** version of the app: `./gradlew assemblyRelease` (or simply `./gradlew aR`). The generated APK will be located in `app/build/outputs/apk/app-release.apk`

License
-------
Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)

Akvo Flow is free software: you can redistribute it and modify it under the terms of the GNU General Public License (GPL) as published by the Free Software Foundation, either version 3 of the License or any later version.

Akvo Flow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License in the file LICENSE.txt.

The license can also be seen at http://www.gnu.org/licenses/gpl.html.
