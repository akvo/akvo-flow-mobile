/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.util.logging;

import android.content.Context;
import android.support.annotation.NonNull;

import com.getsentry.raven.android.event.helper.AndroidEventBuilderHelper;
import com.getsentry.raven.event.EventBuilder;

import java.util.Map;

/**
 * Add custom tags to Raven crash reporting with information about device, model etc...
 */
class FlowEventBuilderHelper extends AndroidEventBuilderHelper {

    private final Map<String, String> tags;

    public FlowEventBuilderHelper(Context applicationContext, @NonNull Map<String, String> tags) {
        super(applicationContext);
        this.tags = tags;
    }

    @Override
    public void helpBuildingEvent(EventBuilder eventBuilder) {
        for (String key : tags.keySet()) {
            eventBuilder.withTag(key, tags.get(key));
        }
        super.helpBuildingEvent(eventBuilder);
    }
}
