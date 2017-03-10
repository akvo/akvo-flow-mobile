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

import java.util.ArrayList;
import java.util.List;

public class UpgraderVisitor implements DatabaseUpgrader {

    private final List<DatabaseUpgrader> upgraders = new ArrayList<>();

    public List<DatabaseUpgrader> getUpgraders() {
        return upgraders;
    }

    @Override
    public void upgrade() {
        for (DatabaseUpgrader upgrader : upgraders) {
            upgrader.upgrade();
        }
    }

    public void addUpgrader(DatabaseUpgrader upgrader) {
        upgraders.add(upgrader);
    }
}
