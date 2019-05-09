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

import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class FormInstanceMetadataTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(TextUtils.class);
        when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return !(a != null && a.length() > 0);
            }
        });
    }

    @Test
    public void isValidShouldReturnFalseIfAllFieldsNull() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata(null, null, null,
                Collections.<String>emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }

    @Test
    public void isValidShouldReturnFalseIfAllFieldsEmpty() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata("", "", "",
                Collections.<String>emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }


    @Test
    public void isValidShouldReturnFalseZipFileNameNull() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata(null, "test", "test",
                Collections.<String>emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }

    @Test
    public void isValidShouldReturnFalseFormInstanceDataNull() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata("test", null, "test",
                Collections.<String>emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }

    @Test
    public void isValidShouldReturnFalseFormIdNull() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata("test", "test", null,
                Collections.<String>emptySet());

        assertFalse(formInstanceMetadata.isValid());
    }

    @Test
    public void isValidShouldReturnTrueIfAllValuesValid() {
        FormInstanceMetadata formInstanceMetadata = new FormInstanceMetadata("test", "test", "test",
                Collections.<String>emptySet());

        assertTrue(formInstanceMetadata.isValid());
    }
}
