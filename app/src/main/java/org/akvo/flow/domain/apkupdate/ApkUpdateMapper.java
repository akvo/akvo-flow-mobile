/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.domain.apkupdate;

import android.support.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class ApkUpdateMapper {

    @Nullable
    public ViewApkData transform(@Nullable JSONObject json) throws JSONException {
        if (json == null) {
            return null;
        }
        String latestVersion = json.getString("version");
        String apkUrl = json.getString("fileName");
        String md5Checksum = json.optString("md5Checksum", null);
        return new ViewApkData(latestVersion, apkUrl, md5Checksum);
    }
}
