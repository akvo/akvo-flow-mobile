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
package org.akvo.flow.domain.apkupdate;

import android.os.Parcel;
import android.os.Parcelable;

public class ViewApkData implements Parcelable {

    private final String version;
    private final String fileUrl;
    private final String md5Checksum;

    public ViewApkData(String version, String fileUrl, String md5Checksum) {
        this.version = version;
        this.fileUrl = fileUrl;
        this.md5Checksum = md5Checksum;
    }

    protected ViewApkData(Parcel in) {
        version = (String) in.readValue(String.class.getClassLoader());
        fileUrl = (String) in.readValue(String.class.getClassLoader());
        md5Checksum = (String) in.readValue(String.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(version);
        dest.writeValue(fileUrl);
        dest.writeValue(md5Checksum);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ViewApkData> CREATOR = new Creator<ViewApkData>() {
        @Override
        public ViewApkData createFromParcel(Parcel in) {
            return new ViewApkData(in);
        }

        @Override
        public ViewApkData[] newArray(int size) {
            return new ViewApkData[size];
        }
    };

    public String getVersion() {
        return version;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }
}
