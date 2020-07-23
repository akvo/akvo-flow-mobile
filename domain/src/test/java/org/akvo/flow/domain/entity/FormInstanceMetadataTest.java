/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.domain.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class FormInstanceMetadataTest {

    @Test
    public void isValidShouldReturnFalseIfAllFieldsNull() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata(null, null, null,
                Collections.emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }

    @Test
    public void isValidShouldReturnFalseIfAllFieldsEmpty() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata("", "", "",
                Collections.emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }


    @Test
    public void isValidShouldReturnFalseZipFileNameNull() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata(null, "test", "test",
                Collections.emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }

    @Test
    public void isValidShouldReturnFalseFormInstanceDataNull() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata("test", null, "test",
                Collections.emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }

    @Test
    public void isValidShouldReturnFalseFormIdNull() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata("test", "test", null,
                Collections.emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }

    @Test
    public void isValidShouldReturnTrueIfAllValuesValid() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata("test", "test", "test",
                Collections.emptySet());

        assertTrue(formInstanceMetadata.isValid());
    }
}
