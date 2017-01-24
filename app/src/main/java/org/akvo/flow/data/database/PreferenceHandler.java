/*
 * Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

/**
 * Gets user preferences from database
 */
public class PreferenceHandler {

    public PreferenceHandler() {
    }

    /**
     * returns the value of a single setting identified by the key passed in
     */
    @Nullable
    public String findPreference(SQLiteDatabase db, String key) {
        String value = null;
        Cursor cursor = db.query(Tables.PREFERENCES,
                new String[] {
                        PreferencesColumns.KEY,
                        PreferencesColumns.VALUE
                }, PreferencesColumns.KEY + " = ?",
                new String[] {
                        key
                }, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                value = cursor.getString(cursor.getColumnIndexOrThrow(PreferencesColumns.VALUE));
            }
            cursor.close();
        }
        return value;
    }
}