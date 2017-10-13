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

package org.akvo.flow.data.migration.languages;

import android.content.Context;
import android.content.res.Resources;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;

import static android.test.MoreAsserts.assertEmpty;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class MigrationLanguageMapperTest {

    @Mock
    private Context mockContext;

    @Mock
    private Resources mockResources;

    private static final String[] MOCK_LANGUAGES_ARRAY = new String[] {
            "en",
            "fr",
            "es"
    };

    @Before
    public void setup() {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class)))
                .thenAnswer(new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        CharSequence a = (CharSequence) invocation.getArguments()[0];
                        return !(a != null && a.length() > 0);
                    }
                });

        given(mockContext.getResources()).willReturn(mockResources);
        given(mockResources.getStringArray(anyInt())).willReturn(MOCK_LANGUAGES_ARRAY);
    }

    @Test
    public void transformShouldReturnEmptyIfEmptyLanguageString() throws Exception {
        Set<String> result = new MigrationLanguageMapper(mockContext).transform("");

        assertEmpty(result);
    }

    @Test
    public void transformShouldReturnCorrectValue() throws Exception {
        Set<String> result = new MigrationLanguageMapper(mockContext).transform("0,2");

        assertEquals(2, result.size());
        assertTrue(result.contains("es"));
        assertTrue(result.contains("en"));
    }
}