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
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PropertyUtil;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class TagsFactory {

    private static final String GAE_INSTANCE_ID_TAG_KEY = "gae.instance";
    private static final String DEVICE_ID_TAG_KEY = "flow.device.id";
    private static final String DEVICE_MODEL_TAG_KEY = "device.model";
    private static final String OS_VERSION_TAG_KEY = "os.version";
    private static final String VERSION_NAME_TAG_KEY = "version.name";
    private static final String VERSION_CODE_TAG_KEY = "version.code";
    private static final String DEFAULT_TAG_VALUE = "NotSet";
    /**
     * The initial capacity is set to 9 for 6 tags (it should be increased if there are more tags)
     */
    public static final int INITIAL_CAPACITY = 9;

    private final Map<String, String> tags = new HashMap<>(INITIAL_CAPACITY);

    public TagsFactory(Context context) {
        Prefs prefs = new Prefs(context);
        initTags(context, prefs);
    }

    public Map<String, String> getTags() {
        return tags;
    }

    private void initTags(Context context, Prefs prefs) {
        tags.put(DEVICE_MODEL_TAG_KEY, Build.MODEL);
        tags.put(GAE_INSTANCE_ID_TAG_KEY, getAppId(context));
        tags.put(DEVICE_ID_TAG_KEY, getDeviceId(prefs));
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
    private String getDeviceId(Prefs prefs) {
        String deviceId = prefs.getString(Prefs.KEY_DEVICE_IDENTIFIER, DEFAULT_TAG_VALUE);
        if (TextUtils.isEmpty(deviceId)) {
            return DEFAULT_TAG_VALUE;
        }
        return deviceId;
    }

    @NonNull
    private String getAppId(Context context) {
        final PropertyUtil props = new PropertyUtil(context.getResources());
        String property = props.getProperty(ConstantUtil.S3_BUCKET);
        return TextUtils.isEmpty(property) ? DEFAULT_TAG_VALUE : property;
    }
}
