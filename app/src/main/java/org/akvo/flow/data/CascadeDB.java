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

    private static final String TABLE_NODE = "nodes";

    private final String mDBPath;
    private final Context mContext;

    private SQLiteDatabase mDatabase;
    private DatabaseHelper mHelper;

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
        final List<Node> result = new ArrayList<>();
        if (!isOpen()) {
            return result;
        }
        Cursor c = mDatabase.query(TABLE_NODE, null,
                NodeColumns.PARENT + "=?",
                new String[]{String.valueOf(parent)},
                null, null, NodeColumns.NAME);

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

        private static final int VERSION = 1;

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

    class NodeColumns {
        static final String ID = "id";
        static final String NAME = "name";
        static final String CODE = "code";
        static final String PARENT = "parent";
    }
}
