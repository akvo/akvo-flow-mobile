/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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
