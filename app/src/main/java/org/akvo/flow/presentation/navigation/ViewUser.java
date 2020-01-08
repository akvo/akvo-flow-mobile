/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class ViewUser implements Parcelable {

    public static final long ADD_USER_ID = -2;

    private final long id;
    private final String name;

    public ViewUser(long id, String name) {
        this.id = id;
        this.name = name;
    }

    protected ViewUser(Parcel in) {
        id = in.readLong();
        name = in.readString();
    }

    public static final Creator<ViewUser> CREATOR = new Creator<ViewUser>() {
        @Override
        public ViewUser createFromParcel(Parcel in) {
            return new ViewUser(in);
        }

        @Override
        public ViewUser[] newArray(int size) {
            return new ViewUser[size];
        }
    };

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
    }
}
