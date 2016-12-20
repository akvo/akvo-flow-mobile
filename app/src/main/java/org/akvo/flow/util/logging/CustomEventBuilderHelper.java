/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.util.logging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.getsentry.raven.android.event.helper.AndroidEventBuilderHelper;
import com.getsentry.raven.event.EventBuilder;

import timber.log.Timber;

/**
 * Add custom tags to Raven crash reporting with information about device, model etc...
 */
public class CustomEventBuilderHelper extends AndroidEventBuilderHelper {

    private static final String PLATFORM = "Android";
    private static final String OS_VERSION_TAG_KEY = "OsVersion";
    private static final String DEVICE_TAG_KEY = "Device";
    private static final String VERSION_NAME_TAG_KEY = "VersionName";
    private static final String VERSION_CODE_TAG_KEY = "VersionCode";
    private final Context applicationContext;

    public CustomEventBuilderHelper(Context applicationContext) {
        super(applicationContext);
        this.applicationContext = applicationContext;
    }

    @Override
    public void helpBuildingEvent(EventBuilder eventBuilder) {
        eventBuilder.withPlatform(PLATFORM);
        eventBuilder.withTag(OS_VERSION_TAG_KEY, Build.VERSION.RELEASE);
        eventBuilder.withTag(DEVICE_TAG_KEY, android.os.Build.MODEL);
        try {
            PackageInfo packageInfo = applicationContext.getPackageManager()
                    .getPackageInfo(applicationContext.getPackageName(), 0);
            String versionName = packageInfo.versionName;
            eventBuilder.withTag(VERSION_NAME_TAG_KEY, versionName);
            int versionCode = packageInfo.versionCode;
            eventBuilder.withTag(VERSION_CODE_TAG_KEY, versionCode+"");
            Timber.d("Raven : helpBuildingEvent"+eventBuilder.toString());
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e("Error getting versionName and versionCode");
        }
        super.helpBuildingEvent(eventBuilder);
    }
}
