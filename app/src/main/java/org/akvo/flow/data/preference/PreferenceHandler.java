/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.preference;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import org.akvo.flow.data.database.PreferencesColumns;
import org.akvo.flow.data.database.Tables;

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