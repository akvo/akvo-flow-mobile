/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.event;

import android.os.Bundle;

import org.akvo.flow.ui.view.QuestionView;

/**
 * event to be fired when the user interacts with a question in a significant
 * way.
 * 
 * @author Christopher Fagiani
 */
public class QuestionInteractionEvent {
    public static final String TAKE_PHOTO_EVENT = "PHOTO";
    public static final String TAKE_VIDEO_EVENT = "VIDEO";
    public static final String QUESTION_ANSWER_EVENT = "ANS";
    public static final String QUESTION_CLEAR_EVENT = "CLR";
    public static final String VIDEO_TIP_VIEW = "VIDTIP";
    public static final String PHOTO_TIP_VIEW = "PHOTOTIP";
    public static final String ACTIVITY_TIP_VIEW = "ACTIVITYTIP";
    public static final String SCAN_BARCODE_EVENT = "SCAN";
    public static final String EXTERNAL_SOURCE_EVENT = "EXTERNALSOURCE";
    public static final String CADDISFLY = "CADDISFLY";
    public static final String PLOTTING_EVENT = "PLOTTING";
    public static final String ADD_SIGNATURE_EVENT = "SIGNATURE";

    private String eventType;
    private QuestionView source;
    private Bundle data;// Arbitrary data associated with the event, if any

    public QuestionInteractionEvent(String type, QuestionView source, Bundle data) {
        this.eventType = type;
        this.source = source;
        this.data = data;
    }

    public String getEventType() {
        return eventType;
    }

    public Bundle getData() {
        return data;
    }

    public QuestionView getSource() {
        return source;
    }

    public void setSource(QuestionView source) {
        this.source = source;
    }
}
