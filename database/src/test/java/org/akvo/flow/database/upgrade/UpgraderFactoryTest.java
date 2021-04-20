/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.database.DatabaseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UpgraderFactoryTest {

    @Mock
    DatabaseHelper mockDbHelper;

    @Mock
    SQLiteDatabase mockDb;

    @Test
    public void createUpgraderShouldCreateCorrectUpgraderWhenResponse() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_RESPONSE_ITERATION, mockDbHelper, mockDb);

        assertEquals(7, upgrader.getUpgraders().size());
        assertTrue(containsResponsesUpgrader(upgrader.getUpgraders()));
    }

    @Test
    public void createUpgraderShouldCreateCorrectUpgraderWhenTransmissionsIteration() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_TRANSMISSION_ITERATION, mockDbHelper, mockDb);

        assertEquals(6, upgrader.getUpgraders().size());
    }

    @Test
    public void createUpgraderShouldCreateCorrectUpgraderWhenAssignmentIteration() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_DATA_POINT_ASSIGNMENTS_ITERATION, mockDbHelper, mockDb);

        assertEquals(5, upgrader.getUpgraders().size());
    }

    @Test
    public void createUpgraderShouldCreateCorrectUpgraderWhenAssignmentIteration2() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_DATA_POINT_ASSIGNMENTS_ITERATION_2, mockDbHelper,
                        mockDb);

        assertEquals(4, upgrader.getUpgraders().size());
    }

    @Test
    public void createUpgraderShouldCreateCorrectUpgraderWhenCursorIteration() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_CURSOR_ITERATION, mockDbHelper,
                        mockDb);

        assertEquals(3, upgrader.getUpgraders().size());
    }

    @Test
    public void createUpgraderShouldCreateCorrectUpgraderWhenSurveyViewedIteration() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_SURVEY_VIEWED, mockDbHelper,
                        mockDb);

        assertEquals(2, upgrader.getUpgraders().size());
    }

    @Test
    public void createUpgraderShouldCreateNoUpgraderWhenDataPointStatusIteration() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_DATAPOINT_STATUS, mockDbHelper,
                        mockDb);

        assertEquals(1, upgrader.getUpgraders().size());
    }

    @Test
    public void createUpgraderShouldCreateNoUpgraderWhenFormVersionUpdateIteration() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_FORM_VERSION_UPDATE, mockDbHelper,
                        mockDb);

        assertEquals(0, upgrader.getUpgraders().size());
    }

    private boolean containsResponsesUpgrader(List<DatabaseUpgrader> upgraders) {
        for (DatabaseUpgrader upgrader : upgraders) {
            if (upgrader instanceof ResponsesUpgrader) {
                return true;
            }
        }
        return false;
    }
}
