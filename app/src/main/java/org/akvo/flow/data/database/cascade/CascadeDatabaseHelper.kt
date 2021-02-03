/*
 * Copyright (C) 2017-2018,2021 Stichting Akvo (Akvo Foundation)
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
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class CascadeDatabaseHelper(private val context: Context, private val dbPath: String) :
    SQLiteOpenHelper(context, File(dbPath).name, null, VERSION) {

    private val preferences: SharedPreferences = context.getSharedPreferences(
        "${context.packageName}.database_versions",
        Context.MODE_PRIVATE
    )

    @Synchronized
    private fun installOrUpdateIfNecessary() {
        if (!installedDatabaseIsUpToDate()) {
            context.deleteDatabase(databaseName)
            installDatabaseFromFile()
            writeDatabaseWasUpdated()
        }
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        throw RuntimeException("The $databaseName database is not writable.")
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        installOrUpdateIfNecessary()
        return super.getReadableDatabase()
    }

    private fun installedDatabaseIsUpToDate(): Boolean {
        return preferences.getBoolean(dbPath, false)
    }

    private fun writeDatabaseNeedsToBeUpdated() {
        preferences.edit().apply {
            putBoolean(dbPath, false)
            apply()
        }
    }

    private fun writeDatabaseWasUpdated() {
        preferences.edit().apply {
            putBoolean(dbPath, true)
            apply()
        }
    }

    private fun installDatabaseFromFile() {
        val file = File(dbPath)
        val inputStream = FileInputStream(file)

        try {
            val outputFile = File(context.getDatabasePath(file.name).path)
            val outputStream = FileOutputStream(outputFile)

            inputStream.copyTo(outputStream)
            inputStream.close()

            outputStream.flush()
            outputStream.close()
        } catch (exception: Throwable) {
            throw RuntimeException("The $dbPath database couldn't be installed.", exception)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        //EMPTY
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        //EMPTY
    }

    companion object {
        private const val VERSION = 1
    }
}
