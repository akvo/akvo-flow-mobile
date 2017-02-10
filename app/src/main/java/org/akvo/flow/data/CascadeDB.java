/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.akvo.flow.domain.Node;

import java.util.ArrayList;
import java.util.List;

public class CascadeDB {

    public static final String TABLE_NODE = "nodes";

    private String mDBPath;

    public interface NodeColumns {
        String ID = "id";
        String NAME = "name";
        String CODE = "code";
        String PARENT = "parent";
    }

    private DatabaseHelper mHelper;
    public SQLiteDatabase mDatabase;

    private static final int VERSION = 1;

    private final Context mContext;

    public CascadeDB(Context context, String dbPath) {
        mContext = context;
        mDBPath = dbPath;
    }

    public void open() throws SQLException {
        mHelper = new DatabaseHelper(mContext, mDBPath);
        mDatabase = mHelper.getReadableDatabase();
    }

    public void close() {
        mHelper.close();
        mDatabase = null;
    }

    public boolean isOpen() {
        return mDatabase != null;
    }

    public List<Node> getValues(long parent) {
        // For backwards compatibility, we'll query all columns, and check if the code exist.
        Cursor c = mDatabase.query(TABLE_NODE, null,
                NodeColumns.PARENT + "=?",
                new String[]{String.valueOf(parent)},
                null, null, NodeColumns.NAME);

        final List<Node> result = new ArrayList<>();
        if (c != null) {
            if (c.moveToFirst()) {
                final int codeCol = c.getColumnIndex(NodeColumns.CODE);
                do {
                    Long id = c.getLong(c.getColumnIndex(NodeColumns.ID));
                    String name = c.getString(c.getColumnIndex(NodeColumns.NAME));
                    String code = codeCol > -1 ? c.getString(codeCol) : null;
                    result.add(new Node(id, name, code));
                } while (c.moveToNext());
            }
            c.close();
        }
        return result;
    }

    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context, String dbPath) {
            super(context, dbPath, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //EMPTY
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //EMPTY
        }

    }

}
