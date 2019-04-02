/*
 * Copyright (C) 2015,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view;

import android.content.Context;

import junit.framework.TestCase;

import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.mockito.Mockito;

import static androidx.test.InstrumentationRegistry.getContext;

public class QuestionGroupTabTest extends TestCase {

    public void testIsLoaded() {
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        Context c = getContext();
        SurveyListener l = Mockito.mock(SurveyListener.class);
        QuestionInteractionListener qil = Mockito.mock(QuestionInteractionListener.class);
        QuestionGroup qg = Mockito.mock(QuestionGroup.class);

        QuestionGroupTab tab = new QuestionGroupTab(c, qg, l, qil);

        assertEquals(false, tab.isLoaded());
        tab.load();
        assertEquals(true, tab.isLoaded());
    }
}
