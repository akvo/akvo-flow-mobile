/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.util.logging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PropertyUtil;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class TagsFactory {

    static final String PLATFORM_TAG_VALUE = "Android";
    static final String PLATFORM_TAG_KEY = "Platform";
    static final String OS_VERSION_TAG_KEY = "OsVersion";
    static final String DEVICE_TAG_KEY = "Device";
    static final String VERSION_NAME_TAG_KEY = "VersionName";
    static final String VERSION_CODE_TAG_KEY = "VersionCode";
    static final String INSTANCE_ID_KEY = "appId";
    static final int NUMBER_OF_TAGS = 6;

    private final Map<String, String> tags = new HashMap<>(NUMBER_OF_TAGS);

    public TagsFactory(Context context) {
        addTags(context);
    }

    public Map<String, String> getTags() {
        return tags;
    }

    private void addTags(Context context) {
        tags.put(PLATFORM_TAG_KEY, PLATFORM_TAG_VALUE);
        tags.put(OS_VERSION_TAG_KEY, Build.VERSION.RELEASE);
        tags.put(DEVICE_TAG_KEY, Build.MODEL);
        final PropertyUtil props = new PropertyUtil(context.getResources());
        tags.put(INSTANCE_ID_KEY, props.getProperty(ConstantUtil.S3_BUCKET));
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
}