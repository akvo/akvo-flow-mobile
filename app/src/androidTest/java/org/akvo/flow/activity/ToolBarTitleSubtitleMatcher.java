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

package org.akvo.flow.activity;

import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class ToolBarTitleSubtitleMatcher {

    public static Matcher<View> withToolbarTitle(final Matcher<String> textMatcher) {

        return new BoundedMatcher<View, Toolbar>(Toolbar.class) {
            @Override
            public boolean matchesSafely(Toolbar toolbar) {
                return textMatcher.matches(toolbar.getTitle().toString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with toolbar title");
                textMatcher.describeTo(description);
            }
        };
    }

    public static Matcher<View> withToolbarSubtitle(final Matcher<String> textMatcher) {

        return new BoundedMatcher<View, Toolbar>(Toolbar.class) {
            @Override
            public boolean matchesSafely(Toolbar toolbar) {
                return textMatcher.matches(toolbar.getSubtitle().toString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with toolbar subtitle");
                textMatcher.describeTo(description);
            }
        };
    }
}
