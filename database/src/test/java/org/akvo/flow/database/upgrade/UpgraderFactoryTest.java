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

import org.akvo.flow.database.DatabaseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UpgraderFactoryTest {

    @Test
    public void createUpgrader_ShouldCreateCorrectUpgraderWhenBeforeLaunch() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory.createUpgrader(77, null, null);

        assertEquals(1, upgrader.getUpgraders().size());
        assertTrue(upgrader.getUpgraders().get(0) instanceof BeforeLaunchUpgrader);
    }

    @Test
    public void createUpgrader_ShouldCreateCorrectUpgraderWhenLaunch() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_LAUNCH, null, null);

        assertEquals(6, upgrader.getUpgraders().size());
        assertTrue(containsLaunchUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormSubmitterUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormCheckUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormVersionUpgrader(upgrader.getUpgraders()));
        assertTrue(containsCaddisflyUpgrader(upgrader.getUpgraders()));
        assertTrue(containsPreferencesUpgrader(upgrader.getUpgraders()));
    }

    @Test
    public void createUpgrader_ShouldCreateCorrectUpgraderWhenFormSubmitter() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_FORM_SUBMITTER, null, null);

        assertEquals(5, upgrader.getUpgraders().size());
        assertFalse(containsLaunchUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormSubmitterUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormCheckUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormVersionUpgrader(upgrader.getUpgraders()));
        assertTrue(containsCaddisflyUpgrader(upgrader.getUpgraders()));
        assertTrue(containsPreferencesUpgrader(upgrader.getUpgraders()));
    }

    @Test
    public void createUpgrader_ShouldCreateCorrectUpgraderWhenFormCheck() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_FORM_DEL_CHECK, null, null);

        assertEquals(4, upgrader.getUpgraders().size());
        assertFalse(containsLaunchUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormSubmitterUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormCheckUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormVersionUpgrader(upgrader.getUpgraders()));
        assertTrue(containsCaddisflyUpgrader(upgrader.getUpgraders()));
        assertTrue(containsPreferencesUpgrader(upgrader.getUpgraders()));
    }

    @Test
    public void createUpgrader_ShouldCreateCorrectUpgraderWhenFormVersion() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_FORM_VERSION, null, null);

        assertEquals(3, upgrader.getUpgraders().size());
        assertFalse(containsLaunchUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormSubmitterUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormCheckUpgrader(upgrader.getUpgraders()));
        assertTrue(containsFormVersionUpgrader(upgrader.getUpgraders()));
        assertTrue(containsCaddisflyUpgrader(upgrader.getUpgraders()));
        assertTrue(containsPreferencesUpgrader(upgrader.getUpgraders()));
    }

    @Test
    public void createUpgrader_ShouldCreateCorrectUpgraderWhenCaddisfly() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_CADDISFLY_QN, null, null);

        assertEquals(2, upgrader.getUpgraders().size());
        assertFalse(containsLaunchUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormSubmitterUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormCheckUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormVersionUpgrader(upgrader.getUpgraders()));
        assertTrue(containsCaddisflyUpgrader(upgrader.getUpgraders()));
        assertTrue(containsPreferencesUpgrader(upgrader.getUpgraders()));
    }

    @Test
    public void createUpgrader_ShouldCreateCorrectUpgraderWhenPreferences() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_PREFERENCES_MIGRATE, null, null);

        assertEquals(1, upgrader.getUpgraders().size());
        assertFalse(containsLaunchUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormSubmitterUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormCheckUpgrader(upgrader.getUpgraders()));
        assertFalse(containsFormVersionUpgrader(upgrader.getUpgraders()));
        assertFalse(containsCaddisflyUpgrader(upgrader.getUpgraders()));
        assertTrue(containsPreferencesUpgrader(upgrader.getUpgraders()));
    }

    @Test
    public void createUpgrader_ShouldCreateNoUpgraderWhenLanguages() {
        UpgraderFactory upgraderFactory = new UpgraderFactory();
        UpgraderVisitor upgrader = (UpgraderVisitor) upgraderFactory
                .createUpgrader(DatabaseHelper.VER_LANGUAGES_MIGRATE, null, null);

        assertEquals(0, upgrader.getUpgraders().size());
    }

    private boolean containsLaunchUpgrader(List<DatabaseUpgrader> upgraders) {
        for (DatabaseUpgrader upgrader : upgraders) {
            if (upgrader instanceof LaunchUpgrader) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFormSubmitterUpgrader(List<DatabaseUpgrader> upgraders) {
        for (DatabaseUpgrader upgrader : upgraders) {
            if (upgrader instanceof FormSubmitterUpgrader) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFormCheckUpgrader(List<DatabaseUpgrader> upgraders) {
        for (DatabaseUpgrader upgrader : upgraders) {
            if (upgrader instanceof FormCheckUpgrader) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFormVersionUpgrader(List<DatabaseUpgrader> upgraders) {
        for (DatabaseUpgrader upgrader : upgraders) {
            if (upgrader instanceof FormVersionUpgrader) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCaddisflyUpgrader(List<DatabaseUpgrader> upgraders) {
        for (DatabaseUpgrader upgrader : upgraders) {
            if (upgrader instanceof CaddisflyUpgrader) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPreferencesUpgrader(List<DatabaseUpgrader> upgraders) {
        for (DatabaseUpgrader upgrader : upgraders) {
            if (upgrader instanceof PreferencesUpgrader) {
                return true;
            }
        }
        return false;
    }
}
