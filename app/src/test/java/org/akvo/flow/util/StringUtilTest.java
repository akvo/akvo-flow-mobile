/*
 * Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class StringUtilTest {

    @Before
    public void setup() {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return !(a != null && a.length() > 0);
            }
        });
    }

    @Test
    public void isValid_ShouldReturnFalseIfNull() throws Exception {
        boolean result = StringUtil.isValid(null);
        assertFalse(result);
    }

    @Test
    public void isValid_ShouldReturnFalseIfEmpty() throws Exception {
        boolean result = StringUtil.isValid("");
        assertFalse(result);
    }

    @Test
    public void isValid_ShouldReturnFalseIfNullString() throws Exception {
        boolean result = StringUtil.isValid("null");
        assertFalse(result);
    }

    @Test
    public void isValid_ShouldReturnTrueIfValid() throws Exception {
        boolean result = StringUtil.isValid("value");
        assertTrue(result);
    }

    @Test
    public void isNullOrEmpty_ShouldReturnTrueIfNull() throws Exception {
        boolean result = StringUtil.isNullOrEmpty(null);
        assertTrue(result);
    }

    @Test
    public void isNullOrEmpty_ShouldReturnTrueIfEmpty() throws Exception {
        boolean result = StringUtil.isNullOrEmpty("");
        assertTrue(result);
    }

    @Test
    public void isNullOrEmpty_ShouldReturnTrueIfOnlySpaces() throws Exception {
        boolean result = StringUtil.isNullOrEmpty("  ");
        assertTrue(result);
    }

    @Test
    public void isNullOrEmpty_ShouldReturnFalseIfNotEmpty() throws Exception {
        boolean result = StringUtil.isNullOrEmpty(" hello ");
        assertFalse(result);
    }

    @Test
    public void controlToSpace_ShouldReturnEmptyIfNullValue() throws Exception {
        String result = StringUtil.controlToSpace(null);
        assertEquals("", result);
    }

    @Test
    public void controlToSpace_ShouldReturnEmptyIfEmptyValue() throws Exception {
        String result = StringUtil.controlToSpace("");
        assertEquals("", result);
    }

    @Test
    public void controlToSpace_ShouldReturnStringWithoutControlChars() throws Exception {
        //testing with 2 line feeds
        String result = StringUtil.controlToSpace("" +'\u0010'+'\u0010');
        assertEquals("  ", result);
    }
}
