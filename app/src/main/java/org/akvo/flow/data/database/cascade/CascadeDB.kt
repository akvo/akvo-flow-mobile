/*
 * Copyright (C) 2010-2018,2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.data.database.cascade

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.SparseArray
import org.akvo.flow.domain.Node

class CascadeDB(private val mContext: Context, private val mDBPath: String) {
    private val nodeMapper = NodeMapper()
    private var database: SQLiteDatabase? = null
    private lateinit var helper: CascadeDatabaseHelper

    @Throws(SQLException::class)
    fun open() {
        helper = CascadeDatabaseHelper(mContext, mDBPath)
        database = helper.getReadableDatabase()
    }

    fun close() {
        helper.close()
        database = null
    }

    fun loadAllValues(): SparseArray<List<Node>> {
        var result = SparseArray<List<Node>>()
        return database?.let { database ->
            val c = database.query(TABLE_NODE, null, null, null, null, null, NodeColumns.NAME)
            c?.let { cursor ->
                result = nodeMapper.nodesAsMap(cursor)
                if (!cursor.isClosed) {
                    cursor.close()
                }
                result
            }
        } ?: result
    }

    companion object {
        private const val TABLE_NODE = "nodes"
    }
}
