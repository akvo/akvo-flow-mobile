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

package com.gallatinsystems.survey.device.domain;

import java.util.List;

public class SurveyedLocale {
    private String mId;
    private int mSurveyGroupId;
    private double mLatitude;
    private double mLongitude;
    private List<SurveyInstance> mSurveyInstances = null;

    public SurveyedLocale (String id, int surveyGroupId, double latitude, double longitude) {
        mId = id;
        mSurveyGroupId = surveyGroupId;
        mLatitude = latitude;
        mLongitude = longitude;
    }
    
    public int getSurveyGroupId() {
        return mSurveyGroupId;
    }
    
    public String getId() {
        return mId;
    }
    
    public double getLatitude() {
        return mLatitude;
    }
    
    public double getLongitude() {
        return mLongitude;
    }
    
    public void setSurveyInstances(List<SurveyInstance> surveyInstances) {
        mSurveyInstances = surveyInstances;
    }
    
    public List<SurveyInstance> getSurveyInstances() {
        return mSurveyInstances;
    }
    
}
