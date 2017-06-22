/*
 *  Copyright (C) 2015,2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.response.value;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Signature implements Parcelable {

    private String name;
    private String image;

    public Signature() {
    }

    public Signature(Parcel in) {
        name = in.readString();
        image = in.readString();
    }

    public static final Creator<Signature> CREATOR = new Creator<Signature>() {
        @Override
        public Signature createFromParcel(Parcel in) {
            return new Signature(in);
        }

        @Override
        public Signature[] newArray(int size) {
            return new Signature[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isValid() {
        // Either both or none
        if (TextUtils.isEmpty(name)) {
            return TextUtils.isEmpty(image);
        }
        return !TextUtils.isEmpty(image);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(image);
    }
}
