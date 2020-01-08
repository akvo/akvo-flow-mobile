/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.database.migration;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.akvo.flow.database.Tables;
import org.akvo.flow.database.TransmissionColumns;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TransmissionMigrationHelper {

    public void migrateTransmissions(SQLiteDatabase db) {
        Cursor cursor = db.query(Tables.TRANSMISSION, new String[] {
                TransmissionColumns._ID, TransmissionColumns.FILENAME
        }, null, null, null, null, null);
        if (cursor != null) {
            Map<Long, ContentValues> insertions = new HashMap<>(cursor.getCount());
            if (cursor.moveToFirst()) {
                int idColumnIndex = cursor.getColumnIndexOrThrow(TransmissionColumns._ID);
                int fileColumnIndex = cursor.getColumnIndexOrThrow(TransmissionColumns.FILENAME);
                do {
                    long id = cursor.getInt(idColumnIndex);
                    String filePath = cursor.getString(fileColumnIndex);
                    ContentValues contentValues = new ContentValues(1);
                    if (!TextUtils.isEmpty(filePath) && filePath.contains(File.separator)
                            && filePath.contains(".")) {
                        int separatorIdx = filePath.lastIndexOf(File.separator);
                        String filename = filePath.substring(separatorIdx + 1);
                        contentValues.put(TransmissionColumns.FILENAME, filename);
                        insertions.put(id, contentValues);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
            Set<Long> transmissionIds = insertions.keySet();
            for (long id : transmissionIds) {
                db.update(Tables.TRANSMISSION, insertions.get(id), TransmissionColumns._ID + " =? ",
                        new String[] {
                                id + ""
                        });
            }
        }

    }
}
