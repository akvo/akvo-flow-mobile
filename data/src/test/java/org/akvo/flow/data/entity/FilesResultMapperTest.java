/*
 * Copyright (C) 2018-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Iterator;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FilesResultMapperTest {

    @Test
    public void transformShouldReturnEmptyDeletedFormsIfNullParam() {
        FilteredFilesResult result = new FilesResultMapper().transform(null);
        assertTrue(result.getDeletedForms().isEmpty());
    }

    @Test
    public void transformShouldReturnEmptyMissingFilesIfNullParam() {
        FilteredFilesResult result = new FilesResultMapper().transform(null);
        assertTrue(result.getMissingFiles().isEmpty());
    }

    @Test
    public void transformShouldReturnEmptyDeletedFormsIfApiDeletedFormsEmpty() {
        ApiFilesResult apiFilesResult = spy(new ApiFilesResult());
        when(apiFilesResult.getDeletedForms()).thenReturn(Collections.emptyList());

        FilteredFilesResult result = new FilesResultMapper().transform(apiFilesResult);

        assertTrue(result.getDeletedForms().isEmpty());
    }

    @Test
    public void transformShouldReturnEmptyDeletedFormsIfApiDeletedFormsNull() {
        ApiFilesResult apiFilesResult = spy(new ApiFilesResult());
        when(apiFilesResult.getDeletedForms()).thenReturn(null);

        FilteredFilesResult result = new FilesResultMapper().transform(apiFilesResult);

        assertTrue(result.getDeletedForms().isEmpty());
    }

    @Test
    public void transformShouldReturnEmptyMissingFilesIfApiMissingNull() {
        ApiFilesResult apiFilesResult = spy(new ApiFilesResult());
        when(apiFilesResult.getMissingFiles()).thenReturn(null);
        when(apiFilesResult.getMissingUnknown()).thenReturn(null);

        FilteredFilesResult result = new FilesResultMapper().transform(apiFilesResult);

        assertTrue(result.getMissingFiles().isEmpty());
    }

    @Test
    public void transformShouldReturnEmptyMissingFilesIfApiMissingEmpty() {
        ApiFilesResult apiFilesResult = spy(new ApiFilesResult());
        when(apiFilesResult.getMissingFiles()).thenReturn(Collections.<String>emptyList());
        when(apiFilesResult.getMissingUnknown()).thenReturn(null);

        FilteredFilesResult result = new FilesResultMapper().transform(apiFilesResult);

        assertTrue(result.getMissingFiles().isEmpty());
    }

    @Test
    public void transformShouldReturnEmptyMissingIfApiMissingUnKnownNull() {
        ApiFilesResult apiFilesResult = spy(new ApiFilesResult());
        when(apiFilesResult.getMissingUnknown()).thenReturn(null);
        when(apiFilesResult.getMissingFiles()).thenReturn(null);

        FilteredFilesResult result = new FilesResultMapper().transform(apiFilesResult);

        assertTrue(result.getMissingFiles().isEmpty());
    }

    @Test
    public void transformShouldReturnBothMissingIfApiMissingUnKnownValues() {
        ApiFilesResult apiFilesResult = spy(new ApiFilesResult());
        when(apiFilesResult.getMissingFiles()).thenReturn(Collections.singletonList("123"));
        when(apiFilesResult.getMissingUnknown()).thenReturn(Collections.singletonList("1234"));
        when(apiFilesResult.getDeletedForms()).thenReturn(Collections.emptyList());

        FilteredFilesResult result = new FilesResultMapper().transform(apiFilesResult);

        assertEquals(2, result.getMissingFiles().size());
        Iterator<String> iterator = result.getMissingFiles().iterator();
        assertEquals("123", iterator.next());
        assertEquals("1234", iterator.next());
    }

    @Test
    public void transformShouldReturnEmptyMissingFilesIfApiMissingUnKnownEmpty() {
        ApiFilesResult apiFilesResult = spy(new ApiFilesResult());
        when(apiFilesResult.getMissingUnknown()).thenReturn(Collections.emptyList());
        when(apiFilesResult.getMissingFiles()).thenReturn(null);

        FilteredFilesResult result = new FilesResultMapper().transform(apiFilesResult);

        assertTrue(result.getMissingFiles().isEmpty());
    }

    @Test
    public void getFilenameForPathShouldReturnNullIfNullPath() {
        String path = new FilesResultMapper().getFilenameFromPath(null);

        assertNull(path);
    }

    @Test
    public void getFilenameForPathShouldReturnEmptyIfEmptyPath() {
        String path = new FilesResultMapper().getFilenameFromPath("");

        assertTrue(path.isEmpty());
    }

    @Test
    public void getFilenameForPathShouldRemoveFoldersIfAny() {
        String path = new FilesResultMapper().getFilenameFromPath("/123/123/123.jpg");

        assertEquals("123.jpg", path);
    }

    @Test
    public void getFilenameForPathShouldReturnSelfIfNoFolders() {
        String path = new FilesResultMapper().getFilenameFromPath("123.jpg");

        assertEquals("123.jpg", path);
    }
}
