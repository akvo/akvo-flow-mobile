/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain;

import java.io.Serializable;
import java.util.List;

public class SurveyedLocale implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -3556354410813212814L;
    
    private String mId;
    private String mName;
    private long mLastModified;
    private long mSurveyGroupId;
    private Double mLatitude;
    private Double mLongitude;
    private List<SurveyInstance> mSurveyInstances = null;

    public SurveyedLocale(String id, String name, long lastModified, long surveyGroupId,
            Double latitude, Double longitude) {
        mId = id;
        mName = name;
        mLastModified = lastModified;
        mSurveyGroupId = surveyGroupId;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public long getSurveyGroupId() {
        return mSurveyGroupId;
    }

    public long getLastModified() {
        return mLastModified;
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
    
    public void setSurveyInstances(List<SurveyInstance> surveyInstances) {
        mSurveyInstances = surveyInstances;
    }
    
    public List<SurveyInstance> getSurveyInstances() {
        return mSurveyInstances;
    }
    
    public String getName() {
        return mName;
    }
    
}
