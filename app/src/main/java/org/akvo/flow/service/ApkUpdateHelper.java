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
import org.akvo.flow.domain.apkupdate.ApkData;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StringUtil;

public class ApkUpdateHelper {


    public ApkUpdateHelper() {
    }

    boolean shouldAppBeUpdated(@Nullable ApkData data, @NonNull Context context) {
        if (data == null) {
            return false;
        }
        //String version = data.getVersion();
        String version = "2.2.9"; //testing remove
        return StringUtil.isValid(version)
            && PlatformUtil.isNewerVersion(PlatformUtil.getVersionName(context), version)
            && StringUtil.isValid(data.getFileUrl());
    }
}
