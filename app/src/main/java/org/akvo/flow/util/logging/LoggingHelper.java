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
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PropertyUtil;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public abstract class LoggingHelper {

    private static final String PLATFORM_TAG_VALUE = "Android";
    private static final String PLATFORM_TAG_KEY = "Platform";
    private static final String OS_VERSION_TAG_KEY = "OsVersion";
    private static final String DEVICE_TAG_KEY = "Device";
    private static final String VERSION_NAME_TAG_KEY = "VersionName";
    private static final String VERSION_CODE_TAG_KEY = "VersionCode";
    private static final String INSTANCE_ID_KEY = "appId";

    final Context context;
    final Map<String, String> tags = new HashMap<>(6);

    LoggingHelper(Context context) {
        this.context = context;
    }

    void addTags() {
        tags.put(PLATFORM_TAG_KEY, PLATFORM_TAG_VALUE);
        tags.put(OS_VERSION_TAG_KEY, Build.VERSION.RELEASE);
        tags.put(DEVICE_TAG_KEY, android.os.Build.MODEL);
        final PropertyUtil props = new PropertyUtil(context.getResources());
        tags.put(INSTANCE_ID_KEY, props.getProperty(ConstantUtil.S3_BUCKET));
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            String versionName = packageInfo.versionName;
            tags.put(VERSION_NAME_TAG_KEY, versionName);
            int versionCode = packageInfo.versionCode;
            tags.put(VERSION_CODE_TAG_KEY, versionCode + "");
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e("Error getting versionName and versionCode");
        }
    }

    public abstract void initSentry();

    public void initDebugTree() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @NonNull
    String getSentryDsn(Resources resources) {
        final PropertyUtil props = new PropertyUtil(resources);
        String sentryDsn = props.getProperty(ConstantUtil.SENTRY_DSN);
        if (TextUtils.isEmpty(sentryDsn)) {
            throw new IllegalArgumentException("Missing sentry dsn");
        }
        return sentryDsn;
    }

    public abstract void plantTimberTree();
}
