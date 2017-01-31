/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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
