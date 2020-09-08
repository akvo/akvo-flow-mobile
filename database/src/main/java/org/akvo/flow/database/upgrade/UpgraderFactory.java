/*
 * Copyright (C) 2017-2018,2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.database.upgrade;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import org.akvo.flow.database.DataPointDownloadTable;
import org.akvo.flow.database.DatabaseHelper;

public class UpgraderFactory {

    @Nullable
    public DatabaseUpgrader createUpgrader(int upgradingFromVersion, DatabaseHelper helper,
                                           SQLiteDatabase db) {
        UpgraderVisitor databaseUpgrader = new UpgraderVisitor();
        switch (upgradingFromVersion) {
            case DatabaseHelper.VER_RESPONSE_ITERATION:
                databaseUpgrader.addUpgrader(new ResponsesUpgrader(helper, db));
            case DatabaseHelper.VER_TRANSMISSION_ITERATION:
                databaseUpgrader.addUpgrader(new TransmissionsUpgrader(helper, db));
            case DatabaseHelper.VER_DATA_POINT_ASSIGNMENTS_ITERATION:
                databaseUpgrader.addUpgrader(new AssignmentsUpgrader(helper, db));
            case DatabaseHelper.VER_DATA_POINT_ASSIGNMENTS_ITERATION_2:
                databaseUpgrader.addUpgrader(new Assignments2Upgrader(db, new DataPointDownloadTable()));
            case DatabaseHelper.VER_CURSOR_ITERATION:
                databaseUpgrader.addUpgrader(new CursorUpgrader(helper, db));
            default:
                break;
        }
        return databaseUpgrader;
    }
}
