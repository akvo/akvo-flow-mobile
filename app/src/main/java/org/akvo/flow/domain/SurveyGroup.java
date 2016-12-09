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

public class SurveyGroup implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -5146372662599353969L;

    public static final long ID_NONE = -1;

    private long mId;
    private String mName;
    private boolean mMonitored;
    private String mRegisterSurveyId;

    public SurveyGroup(long id, String name, String registerSurveyId, boolean monitored) {
        mId = id;
        mName = name;
        mRegisterSurveyId = registerSurveyId;
        mMonitored = monitored;
    }

    public void setRegisterSurveyId(String surveyId) {
        mRegisterSurveyId = surveyId;
    }

    public String getRegisterSurveyId() {
        return mRegisterSurveyId;
    }

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public boolean isMonitored() {
        return mMonitored;
    }

    @Override
    public String toString() {
        return mName;
    }

}
