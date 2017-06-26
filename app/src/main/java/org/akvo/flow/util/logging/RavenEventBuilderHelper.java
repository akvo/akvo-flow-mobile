/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util.logging;

import com.getsentry.raven.environment.RavenEnvironment;
import com.getsentry.raven.event.EventBuilder;
import com.getsentry.raven.event.helper.EventBuilderHelper;

import java.util.Map;

import static org.akvo.flow.util.logging.TagsFactory.VERSION_NAME_TAG_KEY;

public class RavenEventBuilderHelper implements EventBuilderHelper {

    private final Map<String, String> tags;

    public RavenEventBuilderHelper(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public void helpBuildingEvent(EventBuilder eventBuilder) {
        eventBuilder.withSdkName(RavenEnvironment.SDK_NAME + ":android");
        for (String key : tags.keySet()) {
            if (VERSION_NAME_TAG_KEY.equals(key)) {
                eventBuilder.withRelease(key);
            }
            eventBuilder.withTag(key, tags.get(key));
        }
    }
}
