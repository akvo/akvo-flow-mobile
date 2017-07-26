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

package org.akvo.flow.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.akvo.flow.database.migration.MigrationListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseHelperTest {

    public static final int OLD_VERSION = 77;

    @Mock
    private Context mockContext;

    @Mock
    private LanguageTable mockLanguageTable;

    @Mock
    private MigrationListener mockMigrationListener;

    @Mock
    private SQLiteDatabase mockDb;

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionBeforeLaunch() throws Exception {
        DatabaseHelper helper = spy(
                new DatabaseHelper(mockContext, mockLanguageTable, mockMigrationListener));
        doNothing().when(helper).onCreate(any(SQLiteDatabase.class));
        doNothing().when(helper).dropAllTables(any(SQLiteDatabase.class));

        helper.onUpgrade(mockDb, OLD_VERSION, DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(1)).dropAllTables(mockDb);
        verify(helper, times(1)).onCreate(mockDb);
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionLaunch() throws Exception {
        DatabaseHelper helper = spy(
                new DatabaseHelper(mockContext, mockLanguageTable, mockMigrationListener));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_LAUNCH, DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(1)).upgradeFromLaunch(mockDb);
        verify(helper, times(1)).upgradeFromFormSubmitter(mockDb);
        verify(helper, times(1)).upgradeFromFormCheck(mockDb);
        verify(helper, times(1)).upgradeFromFormVersion(mockDb);
        verify(helper, times(1)).upgradeFromCaddisfly(mockDb);
        verify(helper, times(1)).upgradeFromPreferences(mockDb);
        verify(helper, times(1)).upgradeFromLanguages(mockDb);
    }

    private void configureDatabaseHelper(DatabaseHelper helper) {
        doNothing().when(helper).upgradeFromLaunch(any(SQLiteDatabase.class));
        doNothing().when(helper).upgradeFromFormSubmitter(any(SQLiteDatabase.class));
        doNothing().when(helper).upgradeFromFormCheck(any(SQLiteDatabase.class));
        doNothing().when(helper).upgradeFromFormVersion(any(SQLiteDatabase.class));
        doNothing().when(helper).upgradeFromCaddisfly(any(SQLiteDatabase.class));
        doNothing().when(helper).upgradeFromPreferences(any(SQLiteDatabase.class));
        doNothing().when(helper).upgradeFromLanguages(any(SQLiteDatabase.class));
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionSubmitter() throws Exception {
        DatabaseHelper helper = spy(
                new DatabaseHelper(mockContext, mockLanguageTable, mockMigrationListener));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_FORM_SUBMITTER, DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(0)).upgradeFromLaunch(mockDb);
        verify(helper, times(1)).upgradeFromFormSubmitter(mockDb);
        verify(helper, times(1)).upgradeFromFormCheck(mockDb);
        verify(helper, times(1)).upgradeFromFormVersion(mockDb);
        verify(helper, times(1)).upgradeFromCaddisfly(mockDb);
        verify(helper, times(1)).upgradeFromPreferences(mockDb);
        verify(helper, times(1)).upgradeFromLanguages(mockDb);
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionCheck() throws Exception {
        DatabaseHelper helper = spy(
                new DatabaseHelper(mockContext, mockLanguageTable, mockMigrationListener));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_FORM_DEL_CHECK, DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(0)).upgradeFromLaunch(mockDb);
        verify(helper, times(0)).upgradeFromFormSubmitter(mockDb);
        verify(helper, times(1)).upgradeFromFormCheck(mockDb);
        verify(helper, times(1)).upgradeFromFormVersion(mockDb);
        verify(helper, times(1)).upgradeFromCaddisfly(mockDb);
        verify(helper, times(1)).upgradeFromPreferences(mockDb);
        verify(helper, times(1)).upgradeFromLanguages(mockDb);
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionFormVersion() throws Exception {
        DatabaseHelper helper = spy(
                new DatabaseHelper(mockContext, mockLanguageTable, mockMigrationListener));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_FORM_VERSION, DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(0)).upgradeFromLaunch(mockDb);
        verify(helper, times(0)).upgradeFromFormSubmitter(mockDb);
        verify(helper, times(0)).upgradeFromFormCheck(mockDb);
        verify(helper, times(1)).upgradeFromFormVersion(mockDb);
        verify(helper, times(1)).upgradeFromCaddisfly(mockDb);
        verify(helper, times(1)).upgradeFromPreferences(mockDb);
        verify(helper, times(1)).upgradeFromLanguages(mockDb);
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionCaddisfly() throws Exception {
        DatabaseHelper helper = spy(
                new DatabaseHelper(mockContext, mockLanguageTable, mockMigrationListener));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_CADDISFLY_QN, DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(0)).upgradeFromLaunch(mockDb);
        verify(helper, times(0)).upgradeFromFormSubmitter(mockDb);
        verify(helper, times(0)).upgradeFromFormCheck(mockDb);
        verify(helper, times(0)).upgradeFromFormVersion(mockDb);
        verify(helper, times(1)).upgradeFromCaddisfly(mockDb);
        verify(helper, times(1)).upgradeFromPreferences(mockDb);
        verify(helper, times(1)).upgradeFromLanguages(mockDb);
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionPreferences() throws Exception {
        DatabaseHelper helper = spy(
                new DatabaseHelper(mockContext, mockLanguageTable, mockMigrationListener));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_PREFERENCES_MIGRATE, DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(0)).upgradeFromLaunch(mockDb);
        verify(helper, times(0)).upgradeFromFormSubmitter(mockDb);
        verify(helper, times(0)).upgradeFromFormCheck(mockDb);
        verify(helper, times(0)).upgradeFromFormVersion(mockDb);
        verify(helper, times(0)).upgradeFromCaddisfly(mockDb);
        verify(helper, times(1)).upgradeFromPreferences(mockDb);
        verify(helper, times(1)).upgradeFromLanguages(mockDb);
    }

    @Test
    public void onUpgradeShouldUpgradeCorrectlyIfVersionLanguagesMigrate() throws Exception {
        DatabaseHelper helper = spy(
                new DatabaseHelper(mockContext, mockLanguageTable, mockMigrationListener));
        configureDatabaseHelper(helper);

        helper.onUpgrade(mockDb, DatabaseHelper.VER_LANGUAGES_MIGRATE, DatabaseHelper.DATABASE_VERSION);

        verify(helper, times(0)).upgradeFromLaunch(mockDb);
        verify(helper, times(0)).upgradeFromFormSubmitter(mockDb);
        verify(helper, times(0)).upgradeFromFormCheck(mockDb);
        verify(helper, times(0)).upgradeFromFormVersion(mockDb);
        verify(helper, times(0)).upgradeFromCaddisfly(mockDb);
        verify(helper, times(0)).upgradeFromPreferences(mockDb);
        verify(helper, times(1)).upgradeFromLanguages(mockDb);
    }
}
