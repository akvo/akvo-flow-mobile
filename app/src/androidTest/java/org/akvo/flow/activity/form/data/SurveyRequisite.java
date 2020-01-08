/*
 *  Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity.form.data;

import android.content.Context;

import org.akvo.flow.data.preference.Prefs;

public class SurveyRequisite {

    public static void setRequisites(Context context) {
        Prefs prefs = new Prefs(context);
        prefs.setLong(Prefs.KEY_USER_ID, 1L);
        //To bypass the need for a setup (create user)
        prefs.setBoolean(Prefs.KEY_SETUP, true);
        //To bypass "Low Storage" prompt
        prefs.setLong(Prefs.KEY_SPACE_AVAILABLE, 50L);
    }

    //Reset to stage before tests
    public static void resetRequisites(Context context) {
        Prefs prefs = new Prefs(context);
        prefs.setLong(Prefs.KEY_USER_ID, Prefs.DEFAULT_VALUE_USER_ID);
        prefs.setBoolean(Prefs.KEY_SETUP, false);
        prefs.setLong(Prefs.KEY_SPACE_AVAILABLE, Prefs.DEF_VALUE_SPACE_AVAILABLE);
    }
}
