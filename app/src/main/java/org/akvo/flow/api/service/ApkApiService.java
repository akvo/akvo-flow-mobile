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
package org.akvo.flow.api.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import java.io.IOException;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.StatusUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class ApkApiService {

    private static final String APK_VERSION_SERVICE_PATH =
        "/deviceapprest?action=getLatestVersion&deviceType=androidPhone&appCode=flowapp";

    @Nullable
    public JSONObject getApkDataObject(@NonNull Context context) throws IOException, JSONException {
        final String url = StatusUtil.getServerBase(context) + APK_VERSION_SERVICE_PATH;
        String response = HttpUtil.httpGet(url);
        if (!TextUtils.isEmpty(response)) {
            return new JSONObject(response);
        }
        return null;
    }
}
