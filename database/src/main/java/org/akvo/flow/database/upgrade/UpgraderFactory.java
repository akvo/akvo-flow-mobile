/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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
import android.support.annotation.Nullable;

import org.akvo.flow.database.DatabaseHelper;

public class UpgraderFactory {

    @Nullable
    public DatabaseUpgrader createUpgrader(int upgradingFromVersion, DatabaseHelper helper,
            SQLiteDatabase db) {
        UpgraderVisitor databaseUpgrader = new UpgraderVisitor();
        if (upgradingFromVersion < DatabaseHelper.VER_LAUNCH) {
            databaseUpgrader.addUpgrader(new BeforeLaunchUpgrader(helper, db));
        } else {
            switch (upgradingFromVersion) {
                case DatabaseHelper.VER_LAUNCH:
                    databaseUpgrader.addUpgrader(new LaunchUpgrader(helper, db));
                case DatabaseHelper.VER_FORM_SUBMITTER:
                    databaseUpgrader.addUpgrader(new FormSubmitterUpgrader(helper, db));
                case DatabaseHelper.VER_FORM_DEL_CHECK:
                    databaseUpgrader.addUpgrader(new FormCheckUpgrader(helper, db));
                case DatabaseHelper.VER_FORM_VERSION:
                    databaseUpgrader.addUpgrader(new FormVersionUpgrader(helper, db));
                case DatabaseHelper.VER_CADDISFLY_QN:
                    databaseUpgrader.addUpgrader(new CaddisflyUpgrader(helper, db));
                case DatabaseHelper.VER_PREFERENCES_MIGRATE:
                    databaseUpgrader.addUpgrader(new PreferencesUpgrader(helper, db));
                case DatabaseHelper.VER_LANGUAGES_MIGRATE:
                    databaseUpgrader.addUpgrader(new LanguagesUpgrader(helper, db));
                default:
                    break;
            }
        }
        return databaseUpgrader;
    }
}
