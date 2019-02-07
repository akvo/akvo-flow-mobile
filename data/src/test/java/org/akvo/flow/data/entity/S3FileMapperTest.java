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
 *
 */

package org.akvo.flow.data.entity;

import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import org.akvo.flow.data.datasource.FlowFileBrowser;
import org.akvo.flow.data.util.FileHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class S3FileMapperTest {

    @Mock
    FlowFileBrowser mockFileBrowser;

    @Mock
    FileHelper mockFileHelper;

    @Mock
    File mockFile;

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
    public void shouldReturnNullForNullFilename() {
        S3FileMapper s3FileMapper = new S3FileMapper(mockFileBrowser, mockFileHelper);

        S3File file = s3FileMapper.transform(null);

        assertNull(file);
    }

    @Test
    public void shouldReturnNullForEmptyFilename() {
        S3FileMapper s3FileMapper = new S3FileMapper(mockFileBrowser, mockFileHelper);

        S3File file = s3FileMapper.transform("");

        assertNull(file);
    }

    @Test
    public void shouldReturnNullForNonExistingFilename() {
        S3FileMapper s3FileMapper = new S3FileMapper(mockFileBrowser, mockFileHelper);
        given(mockFileBrowser.getInternalFile(anyString(), anyString())).willReturn(mockFile);
        given(mockFile.exists()).willReturn(false);

        S3File file = s3FileMapper.transform("data.zip");

        assertNull(file);
    }

    @Test
    public void shouldReturnCorrectFileForArchive() {
        S3FileMapper s3FileMapper = new S3FileMapper(mockFileBrowser, mockFileHelper);
        given(mockFileBrowser.getInternalFile(anyString(), anyString())).willReturn(mockFile);
        given(mockFile.exists()).willReturn(true);
        given(mockFileHelper.getMd5Base64(any(File.class))).willReturn("123");
        given(mockFileHelper.hexMd5(any(File.class))).willReturn("1234");

        S3File file = s3FileMapper.transform("data.zip");

        assertNotNull(file);
        assertFalse(file.isPublic());
        assertEquals(S3File.S3_DATA_DIR, file.getDir());
        assertEquals(S3File.ACTION_SUBMIT, file.getAction());
    }

    @Test
    public void shouldReturnCorrectFileForJpgImage() {
        S3FileMapper s3FileMapper = new S3FileMapper(mockFileBrowser, mockFileHelper);
        given(mockFileBrowser.getInternalFile(anyString(), anyString())).willReturn(mockFile);
        given(mockFile.exists()).willReturn(true);
        given(mockFileHelper.getMd5Base64(any(File.class))).willReturn("123");
        given(mockFileHelper.hexMd5(any(File.class))).willReturn("1234");

        S3File file = s3FileMapper.transform("data.jpg");

        assertNotNull(file);
        assertTrue(file.isPublic());
        assertEquals(S3File.S3_IMAGE_DIR, file.getDir());
        assertEquals(S3File.ACTION_IMAGE, file.getAction());
    }

    @Test
    public void shouldReturnCorrectFileForVideo() {
        S3FileMapper s3FileMapper = new S3FileMapper(mockFileBrowser, mockFileHelper);
        given(mockFileBrowser.getInternalFile(anyString(), anyString())).willReturn(mockFile);
        given(mockFile.exists()).willReturn(true);
        given(mockFileHelper.getMd5Base64(any(File.class))).willReturn("123");
        given(mockFileHelper.hexMd5(any(File.class))).willReturn("1234");

        S3File file = s3FileMapper.transform("data.mp4");

        assertNotNull(file);
        assertTrue(file.isPublic());
        assertEquals(S3File.S3_IMAGE_DIR, file.getDir());
        assertEquals(S3File.ACTION_IMAGE, file.getAction());
    }

    @Test
    public void shouldReturnCorrectFileForUnexpectedFile() {
        S3FileMapper s3FileMapper = new S3FileMapper(mockFileBrowser, mockFileHelper);
        given(mockFileBrowser.getInternalFile(anyString(), anyString())).willReturn(mockFile);
        given(mockFile.exists()).willReturn(true);
        given(mockFileHelper.getMd5Base64(any(File.class))).willReturn("123");
        given(mockFileHelper.hexMd5(any(File.class))).willReturn("1234");

        S3File file = s3FileMapper.transform("data.txt");

        assertNotNull(file);
        assertFalse(file.isPublic());
        assertNull(file.getDir());
        assertNull(file.getAction());
    }
}
