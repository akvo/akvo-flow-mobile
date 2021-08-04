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

package org.akvo.flow.database;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.spy;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.akvo.flow.database.tables.DataPointDownloadTable;
import org.akvo.flow.database.tables.FormUpdateNotifiedTable;
import org.akvo.flow.database.tables.LanguageTable;
import org.akvo.flow.database.tables.QuestionGroupTable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseHelperTest {

    @Mock
    private Context mockContext;

    @Mock
    private LanguageTable mockLanguageTable;

    @Mock
    private DataPointDownloadTable mockDataPointDownloadTable;

    @Mock
    private FormUpdateNotifiedTable mockFormUpdateNotifiedTable;

    @Mock
    private SQLiteDatabase mockDb;

    @Mock
    private QuestionGroupTable mockQuestionGroupTable;

    private void configureDatabaseHelper(DatabaseHelper helper) {
        doNothing().when(helper).upgradeFromResponses(any(SQLiteDatabase.class));
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionResponseMigrate() {
        DatabaseHelper helper = spy(new DatabaseHelper(mockContext, mockLanguageTable,
                mockDataPointDownloadTable, mockFormUpdateNotifiedTable, mockQuestionGroupTable));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_RESPONSE_ITERATION,
                DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(1)).upgradeFromResponses(mockDb);
        verify(helper, times(1)).upgradeFromTransmission(mockDb);
        verify(helper, times(1)).upgradeFromAssignment(mockDb);
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionTransmissionMigrate() {
        DatabaseHelper helper = spy(new DatabaseHelper(mockContext, mockLanguageTable,
                mockDataPointDownloadTable, mockFormUpdateNotifiedTable, mockQuestionGroupTable));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_TRANSMISSION_ITERATION,
                DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(1)).upgradeFromTransmission(mockDb);
        verify(helper, times(1)).upgradeFromAssignment(mockDb);
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionAssignmentsMigrate() {
        DatabaseHelper helper = spy(new DatabaseHelper(mockContext, mockLanguageTable,
                mockDataPointDownloadTable, mockFormUpdateNotifiedTable, mockQuestionGroupTable));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_DATA_POINT_ASSIGNMENTS_ITERATION,
                DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(1)).upgradeFromAssignment(mockDb);
    }
}
