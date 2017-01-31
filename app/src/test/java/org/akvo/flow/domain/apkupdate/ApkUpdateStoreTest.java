/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.apkupdate;

import android.test.suitebuilder.annotation.SmallTest;

import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.util.ConstantUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

@SmallTest
@RunWith(PowerMockRunner.class)
public class ApkUpdateStoreTest {

    @Mock
    private Prefs mockPrefs;

    @Before
    public void setUp() throws Exception {
        doNothing().when(mockPrefs).setString(anyString(), anyString());
    }

    @Test
    public void updateApkData_ShouldUpdateIfSavedOutDated() throws Exception {
        apkDataWithVersion("2.2.8");
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));
        ViewApkData newData = new ViewApkData("2.2.9", "", "");

        sut.updateApkData(newData);

        verify(mockPrefs, times(1)).setString(anyString(), anyString());
        verify(mockPrefs, times(1)).removePreference(anyString());
    }

    private void apkDataWithVersion(final String version) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final ViewApkData savedData = new ViewApkData(version, "", "");
                return new GsonMapper().write(savedData, ViewApkData.class);
            }
        }).when(mockPrefs).getString(anyString(), anyString());
    }

    @Test
    public void updateApkData_ShouldUpdateIfSavedNull() throws Exception {
        nullStringApkData();
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));
        ViewApkData newData = new ViewApkData("2.2.9", "", "");

        sut.updateApkData(newData);

        verify(mockPrefs, times(1)).setString(anyString(), anyString());
        verify(mockPrefs, times(1)).removePreference(anyString());
    }

    private void nullStringApkData() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(mockPrefs).getString(anyString(), anyString());
    }

    @Test
    public void updateApkData_ShouldNotUpdateIfSavedUpdated() throws Exception {
        apkDataWithVersion("2.2.9");
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));
        ViewApkData newData = new ViewApkData("2.2.9", "", "");

        sut.updateApkData(newData);

        verify(mockPrefs, times(0)).setString(anyString(), anyString());
        verify(mockPrefs, times(0)).removePreference(anyString());
    }

    @Test
    public void updateApkData_ShouldNotUpdateIfSavedMoreRecent() throws Exception {
        apkDataWithVersion("2.2.10");
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));
        ViewApkData newData = new ViewApkData("2.2.9", "", "");

        sut.updateApkData(newData);

        verify(mockPrefs, times(0)).setString(anyString(), anyString());
        verify(mockPrefs, times(0)).removePreference(anyString());
    }

    @Test
    public void getApkData_ShouldReturnNullIfUnset() throws Exception {
        nullStringApkData();
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));

        ViewApkData newData = sut.getApkData();

        assertNull(newData);
    }

    @Test
    public void getApkData_ShouldReturnCorrectApkData() throws Exception {
        prefilledApkData();
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));

        ViewApkData newData = sut.getApkData();

        assertNotNull(newData);
        assertEquals("2.2.10", newData.getVersion());
        assertEquals("path", newData.getFileUrl());
        assertEquals("md5", newData.getMd5Checksum());
    }

    private void prefilledApkData() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final ViewApkData savedData = new ViewApkData("2.2.10", "path", "md5");
                return new GsonMapper().write(savedData, ViewApkData.class);
            }
        }).when(mockPrefs).getString(anyString(), anyString());
    }

    @Test
    public void shouldNotifyNewVersion_WhenPreferenceUnset() throws Exception {
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));
        given(mockPrefs
                .getLong(ApkUpdateStore.KEY_APP_UPDATE_LAST_NOTIFIED, ApkUpdateStore.NOT_NOTIFIED))
                .willReturn(ApkUpdateStore.NOT_NOTIFIED);

        boolean shouldNotify = sut.shouldNotifyNewVersion();

        assertTrue(shouldNotify);
    }

    @Test
    public void shouldNotifyNewVersion_WhenPreferenceOutDated() throws Exception {
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));
        given(mockPrefs
                .getLong(ApkUpdateStore.KEY_APP_UPDATE_LAST_NOTIFIED, ApkUpdateStore.NOT_NOTIFIED))
                .willReturn(
                        System.currentTimeMillis() - ConstantUtil.UPDATE_NOTIFICATION_DELAY_IN_MS);

        boolean shouldNotify = sut.shouldNotifyNewVersion();

        assertTrue(shouldNotify);
    }

    @Test
    public void shouldNotNotifyNewVersion_WhenPreferenceRecent() throws Exception {
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));
        given(mockPrefs
                .getLong(ApkUpdateStore.KEY_APP_UPDATE_LAST_NOTIFIED, ApkUpdateStore.NOT_NOTIFIED))
                .willReturn(System.currentTimeMillis());

        boolean shouldNotify = sut.shouldNotifyNewVersion();

        assertFalse(shouldNotify);
    }

}