/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.ui.model;

import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import org.akvo.flow.data.loader.models.SurveyInfo;
import org.akvo.flow.domain.SurveyGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class ViewSurveyInfoMapperTest {

    @Mock
    private SurveyGroup mockSurveyGroup;

    @Mock
    private SurveyInfo mockSurveyItem;

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
    public void transform_ShouldReturnEmptyArrayIfNull() {
        ViewSurveyInfoMapper mapper = new ViewSurveyInfoMapper();
        List<SurveyInfo> original = null;

        List<ViewSurveyInfo> mapped = mapper.transform(original, mockSurveyGroup, true, "deleted");

        assertNotNull(mapped);
        assertTrue(mapped.isEmpty());
    }

    @Test
    public void transform_ShouldIgnoreNullItems() {
        ViewSurveyInfoMapper mapper = new ViewSurveyInfoMapper();
        List<SurveyInfo> original = new ArrayList<>();
        original.add(null);
        original.add(mockSurveyItem);

        List<ViewSurveyInfo> mapped = mapper.transform(original, mockSurveyGroup, true, "deleted");

        assertNotNull(mapped);
        assertEquals(1, mapped.size());
    }

    @Test
    public void transform_ShouldTransformCorrectlyDeletedSurvey() {
        ViewSurveyInfoMapper mapper = new ViewSurveyInfoMapper();

        given(mockSurveyItem.isDeleted()).willReturn(true);
        given(mockSurveyItem.getVersion()).willReturn("1.0");

        ViewSurveyInfo mapped = mapper.transform(mockSurveyItem, mockSurveyGroup, true, "deleted");

        assertFalse(mapped.isEnabled());
        assertEquals(" v1.0 - deleted", mapped.getSurveyExtraInfo());
    }

    @Test
    public void transform_ShouldTransformCorrectlyNonSubmittedSurvey() {
        ViewSurveyInfoMapper mapper = new ViewSurveyInfoMapper();

        given(mockSurveyItem.isDeleted()).willReturn(false);
        given(mockSurveyItem.getVersion()).willReturn("1.0");
        given(mockSurveyItem.getLastSubmission()).willReturn(null);

        ViewSurveyInfo mapped = mapper.transform(mockSurveyItem, mockSurveyGroup, true, "deleted");

        assertEquals(null, mapped.getTime());
    }

    @Test
    public void transform_ShouldTransformCorrectlyNonRegistrationMonitoredSurvey() {
        ViewSurveyInfoMapper mapper = new ViewSurveyInfoMapper();

        given(mockSurveyItem.isDeleted()).willReturn(false);
        given(mockSurveyItem.getVersion()).willReturn("1.0");
        given(mockSurveyItem.getLastSubmission()).willReturn(1L);
        given(mockSurveyItem.isRegistrationSurvey()).willReturn(false);
        given(mockSurveyGroup.isMonitored()).willReturn(true);

        ViewSurveyInfo mapped = mapper.transform(mockSurveyItem, mockSurveyGroup, false, "deleted");

        assertNotNull(mapped.getTime());
        assertFalse(mapped.isEnabled());
    }

    @Test
    public void transform_ShouldTransformCorrectlyAllSurveyFields() {
        ViewSurveyInfoMapper mapper = new ViewSurveyInfoMapper();

        given(mockSurveyItem.isDeleted()).willReturn(false);
        given(mockSurveyItem.getVersion()).willReturn("1.0");
        given(mockSurveyItem.getLastSubmission()).willReturn(System.currentTimeMillis());
        given(mockSurveyItem.isRegistrationSurvey()).willReturn(true);
        given(mockSurveyItem.getId()).willReturn("123");
        given(mockSurveyItem.getName()).willReturn("name");
        given(mockSurveyGroup.isMonitored()).willReturn(true);

        ViewSurveyInfo mapped = mapper.transform(mockSurveyItem, mockSurveyGroup, true, "deleted");

        assertEquals("123", mapped.getId());
        assertEquals("name", mapped.getSurveyName());
        assertEquals(" v1.0", mapped.getSurveyExtraInfo());
        assertNull(mapped.getTime());
        assertFalse(mapped.isEnabled());
    }


}