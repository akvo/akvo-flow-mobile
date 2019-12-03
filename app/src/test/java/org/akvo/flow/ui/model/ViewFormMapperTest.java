/*
 * Copyright (C) 2010-2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.model;

import org.akvo.flow.data.loader.models.FormInfo;
import org.akvo.flow.domain.SurveyGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class ViewFormMapperTest {

    @Mock
    private SurveyGroup mockSurveyGroup;

    @Mock
    private FormInfo mockSurveyItem;

    @Test
    public void transformShouldReturnEmptyArrayIfNull() {
        ViewFormMapper mapper = new ViewFormMapper();
        List<FormInfo> original = null;

        List<ViewForm> mapped = mapper.transform(original, mockSurveyGroup, "deleted");

        assertNotNull(mapped);
        assertTrue(mapped.isEmpty());
    }

    @Test
    public void transformShouldIgnoreNullItems() {
        ViewFormMapper mapper = new ViewFormMapper();
        List<FormInfo> original = new ArrayList<>();
        original.add(null);
        original.add(mockSurveyItem);

        List<ViewForm> mapped = mapper.transform(original, mockSurveyGroup, "deleted");

        assertNotNull(mapped);
        assertEquals(1, mapped.size());
    }

    @Test
    public void transformShouldTransformCorrectlyDeletedSurvey() {
        ViewFormMapper mapper = new ViewFormMapper();

        given(mockSurveyItem.isDeleted()).willReturn(true);
        given(mockSurveyItem.getVersion()).willReturn("1.0");

        ViewForm mapped = mapper.transform(mockSurveyItem, mockSurveyGroup, "deleted");

        assertFalse(mapped.isEnabled());
        assertEquals(" v1.0 - deleted", mapped.getSurveyExtraInfo());
    }

    @Test
    public void transformShouldTransformCorrectlyNonSubmittedSurvey() {
        ViewFormMapper mapper = new ViewFormMapper();

        given(mockSurveyItem.isDeleted()).willReturn(false);
        given(mockSurveyItem.getVersion()).willReturn("1.0");
        given(mockSurveyItem.getLastSubmission()).willReturn(null);

        ViewForm mapped = mapper.transform(mockSurveyItem, mockSurveyGroup, "deleted");

        assertNull(mapped.getTime());
    }

    @Test
    public void transformShouldTransformCorrectlyNonRegistrationMonitoredSurvey() {
        ViewFormMapper mapper = new ViewFormMapper();

        given(mockSurveyItem.isDeleted()).willReturn(false);
        given(mockSurveyItem.getVersion()).willReturn("1.0");
        given(mockSurveyItem.getLastSubmission()).willReturn(1L);
        given(mockSurveyItem.isRegistrationForm()).willReturn(false);
        given(mockSurveyGroup.isMonitored()).willReturn(true);

        ViewForm mapped = mapper.transform(mockSurveyItem, mockSurveyGroup, "deleted");

        assertNotNull(mapped.getTime());
        assertFalse(mapped.isEnabled());
    }

    @Test
    public void transformShouldTransformCorrectlyAllSurveyFields() {
        ViewFormMapper mapper = new ViewFormMapper();

        given(mockSurveyItem.isDeleted()).willReturn(false);
        given(mockSurveyItem.getVersion()).willReturn("1.0");
        given(mockSurveyItem.getLastSubmission()).willReturn(System.currentTimeMillis());
        given(mockSurveyItem.isRegistrationForm()).willReturn(true);
        given(mockSurveyItem.getId()).willReturn("123");
        given(mockSurveyItem.getName()).willReturn("name");
        given(mockSurveyItem.isSubmittedDataPoint()).willReturn(true);
        given(mockSurveyGroup.isMonitored()).willReturn(true);

        ViewForm mapped = mapper.transform(mockSurveyItem, mockSurveyGroup, "deleted");

        assertEquals("123", mapped.getId());
        assertEquals("name", mapped.getSurveyName());
        assertEquals(" v1.0", mapped.getSurveyExtraInfo());
        assertNull(mapped.getTime());
        assertTrue(mapped.isEnabled());
    }
}