/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.apkupdate;

import android.test.suitebuilder.annotation.SmallTest;

import org.akvo.flow.util.Prefs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

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
        returnNullApkData();
        ApkUpdateStore sut = spy(new ApkUpdateStore(new GsonMapper(), mockPrefs));
        ViewApkData newData = new ViewApkData("2.2.9", "", "");

        sut.updateApkData(newData);

        verify(mockPrefs, times(1)).setString(anyString(), anyString());
        verify(mockPrefs, times(1)).removePreference(anyString());
    }

    private void returnNullApkData() {
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

}