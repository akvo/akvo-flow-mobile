/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */
package org.akvo.flow.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.io.IOException;
import org.akvo.flow.api.service.ApkApiService;
import org.akvo.flow.domain.apkupdate.ApkData;
import org.akvo.flow.domain.apkupdate.ApkUpdateMapper;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

public class ApkUpdateHelper {

    private final ApkApiService apkApiService;
    private final ApkUpdateMapper apkUpdateMapper;

    @Inject
    public ApkUpdateHelper(ApkApiService apkApiService, ApkUpdateMapper apkUpdateMapper) {
        this.apkApiService = apkApiService;
        this.apkUpdateMapper = apkUpdateMapper;
    }

    Pair<Boolean, ApkData> shouldUpdate(@NonNull Context context, String serverBase) throws IOException, JSONException {
        JSONObject json = apkApiService.getApkDataObject(serverBase);
        ApkData data = apkUpdateMapper.transform(json);
        return new Pair<>(shouldAppBeUpdated(data, context), data);
    }

    private boolean shouldAppBeUpdated(@Nullable ApkData data, @NonNull Context context) {
        if (data == null) {
            return false;
        }
        String version = data.getVersion();
        return StringUtil.isValid(version)
            && PlatformUtil.isNewerVersion(PlatformUtil.getVersionName(context), version)
            && StringUtil.isValid(data.getFileUrl());
    }
}
