/*
 * Copyright (C) 2010-2016,2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain;

import android.content.Context;
import android.text.TextUtils;

import org.akvo.flow.R;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class SurveyedLocale implements Serializable {

    private static final long serialVersionUID = -3556354410813212814L;

    private String mId;
    private String mName;
    private long mSurveyGroupId;
    private Double mLatitude;
    private Double mLongitude;
    private int status;

    public SurveyedLocale(String id, String name, long surveyGroupId,
            Double latitude, Double longitude, int status) {
        mId = id;
        mName = name;
        mSurveyGroupId = surveyGroupId;
        mLatitude = latitude;
        mLongitude = longitude;
        this.status = status;
    }

    public long getSurveyGroupId() {
        return mSurveyGroupId;
    }

    public String getId() {
        return mId;
    }

    public Double getLatitude() {
        return mLatitude;
    }

    public Double getLongitude() {
        return mLongitude;
    }

    public String getName() {
        return mName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    /**
     * Get record name if exists, 'Unknown' otherwise
     */
    public String getDisplayName(Context context) {
        return TextUtils.isEmpty(mName) ? context.getString(R.string.unknown) : mName;
    }
}
