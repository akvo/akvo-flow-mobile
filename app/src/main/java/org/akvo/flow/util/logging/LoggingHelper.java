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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PropertyUtil;
import org.akvo.flow.util.StatusUtil;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public abstract class LoggingHelper {

    private static final String INSTANCE_ID_KEY = "app.id";
    private static final String DEVICE_ID_KEY = "device.id";
    private static final String DEVICE_TAG_KEY = "device.model";
    private static final String OS_VERSION_TAG_KEY = "os.version";
    private static final String VERSION_NAME_TAG_KEY = "version.name";
    private static final String VERSION_CODE_TAG_KEY = "version.code";
    private static final String DEFAULT_TAG_VALUE = "NotSet";
    private static final int NUMBER_OF_TAGS = 6;

    final Context context;
    final Map<String, String> tags = new HashMap<>(NUMBER_OF_TAGS);

    LoggingHelper(Context context) {
        this.context = context;
    }

    void addTags() {
        tags.put(DEVICE_TAG_KEY, android.os.Build.MODEL);
        tags.put(INSTANCE_ID_KEY, getAppId());
        tags.put(DEVICE_ID_KEY, getDeviceId());
        tags.put(OS_VERSION_TAG_KEY, Build.VERSION.RELEASE);
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            tags.put(VERSION_NAME_TAG_KEY, versionName);
            tags.put(VERSION_CODE_TAG_KEY, versionCode + "");
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e("Error getting versionName and versionCode");
        }
    }

    @NonNull
    private String getDeviceId() {
        String deviceId = StatusUtil.getDeviceId(context);
        if (TextUtils.isEmpty(deviceId)) {
            return DEFAULT_TAG_VALUE;
        }
        return deviceId;
    }

    @NonNull
    private String getAppId() {
        final PropertyUtil props = new PropertyUtil(context.getResources());
        String property = props.getProperty(ConstantUtil.S3_BUCKET);
        return TextUtils.isEmpty(property) ? DEFAULT_TAG_VALUE : property;
    }

    public abstract void initSentry();

    public void initDebugTree() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Nullable
    String getSentryDsn(Resources resources) {
        final PropertyUtil props = new PropertyUtil(resources);
        String sentryDsn = props.getProperty(ConstantUtil.SENTRY_DSN);
        return sentryDsn;
    }
}
