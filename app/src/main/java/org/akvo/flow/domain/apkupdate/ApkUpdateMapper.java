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

package org.akvo.flow.domain.apkupdate;

import android.support.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

public class ApkUpdateMapper {

    @Inject
    public ApkUpdateMapper() {
    }

    @Nullable
    public ApkData transform(@Nullable JSONObject json) throws JSONException {
        if (json == null) {
            return null;
        }
        String latestVersion = json.getString("version");
        String apkUrl = json.getString("fileName");
        String md5Checksum = json.optString("md5Checksum", null);
        return new ApkData(latestVersion, apkUrl, md5Checksum);
    }
}
