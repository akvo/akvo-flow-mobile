/*
 * Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.database.cascade;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import org.akvo.flow.domain.Node;

import java.util.ArrayList;
import java.util.List;

public class CascadeDB {

    private static final String TABLE_NODE = "nodes";

    private final String mDBPath;
    private final Context mContext;
    private final NodeMapper nodeMapper = new NodeMapper();

    private SQLiteDatabase mDatabase;
    private CascadeDatabaseHelper mHelper;

    public CascadeDB(Context context, String dbPath) {
        mContext = context;
        mDBPath = dbPath;
    }

    public void open() throws SQLException {
        mHelper = new CascadeDatabaseHelper(mContext, mDBPath);
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
        List<Node> result = new ArrayList<>();
        if (!isOpen()) {
            return result;
        }
        Cursor c = mDatabase.query(TABLE_NODE, null,
                NodeColumns.PARENT + "=?",
                new String[]{String.valueOf(parent)},
                null, null, NodeColumns.NAME);

        if (c != null) {
            result = nodeMapper.mapNodes(c);
        }
        return result;
    }

    public SparseArray<List<Node>> getValues() {
        SparseArray<List<Node>> result = new SparseArray<>();
        if (!isOpen()) {
            return result;
        }
        Cursor c = mDatabase.query(TABLE_NODE, null, null, null, null, null, NodeColumns.NAME);

        if (c != null) {
            result = nodeMapper.nodesAsMap(c);
        }
        return result;
    }
}
