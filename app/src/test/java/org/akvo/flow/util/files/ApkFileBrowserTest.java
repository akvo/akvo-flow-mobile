/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util.files;

import android.content.Context;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.domain.util.VersionHelper;
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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FileUtil.class)
public class ApkFileBrowserTest {

    private static final String MOCK_CHECKSUM = "123";
    private static final String MOCK_APK_PATH = "/path/apk/123/flow.apk";

    @Mock
    FileBrowser mockFileBrowser;

    @Mock
    Context mockContext;

    @Mock
    File mockFile;

    @Mock
    File mockFolder;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(FileUtil.class);
        PowerMockito.when(FileUtil.hexMd5(any(File.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return MOCK_CHECKSUM;
            }
        });

        doReturn(true).when(mockFile).delete();
        doReturn(MOCK_APK_PATH).when(mockFile).getAbsolutePath();
        doReturn(new File[] {
                mockFile
        }).when(mockFolder).listFiles();
    }

    @Test
    public void testEnsureVerifyLatestApkFileReturnsNullWhenCheckSumNull() {
        ApkFileBrowser apkFileBrowser = new ApkFileBrowser(mockFileBrowser, new VersionHelper());

        String path = apkFileBrowser.verifyLatestApkFile(mockContext, null);

        assertNull(path);
    }

    @Test
    public void testEnsureVerifyLatestApkFileReturnsNullWhenFileIsNull() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser, new VersionHelper()));

        doReturn(null).when(apkFileBrowser).getLatestApkFile(any(Context.class));

        String path = apkFileBrowser.verifyLatestApkFile(mockContext, MOCK_CHECKSUM);

        assertNull(path);
    }

    @Test
    public void testEnsureVerifyLatestApkFileReturnsNullWhenCheckSumsDoNotMatch() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser, new VersionHelper()));

        doReturn(mockFile).when(apkFileBrowser).getLatestApkFile(any(Context.class));

        String path = apkFileBrowser.verifyLatestApkFile(mockContext, "12");

        assertNull(path);
    }

    @Test
    public void testEnsureVerifyLatestApkFileReturnsNonNullWhenCheckSumsMatch() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser, new VersionHelper()));

        doReturn(mockFile).when(apkFileBrowser).getLatestApkFile(any(Context.class));

        String path = apkFileBrowser.verifyLatestApkFile(mockContext, MOCK_CHECKSUM);

        assertNotNull(path);
    }

    @Test
    public void testEnsureGetLatestApkFileReturnsNullIfNullApkFolder() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser, new VersionHelper()));

        doReturn(null).when(apkFileBrowser).getApksFoldersList(any(Context.class));

        File folder = apkFileBrowser.getLatestApkFile(mockContext);

        assertNull(folder);
    }

    @Test
    public void testEnsureGetLatestApkFileReturnsNullIfEmptyApkFolder() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser, new VersionHelper()));

        doReturn(new File[0]).when(apkFileBrowser).getApksFoldersList(any(Context.class));

        File file = apkFileBrowser.getLatestApkFile(mockContext);

        assertNull(file);
    }

    @Test
    public void testEnsureGetLatestApkFileReturnsNonNullIfNewestApkFolder() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser, new VersionHelper()));
        File[] folderList = new File[] {
                mockFolder
        };

        doReturn(getLaterVersionThanCurrent(1)).when(mockFolder).getName();
        doReturn(folderList).when(apkFileBrowser).getApksFoldersList(any(Context.class));

        File file = apkFileBrowser.getLatestApkFile(mockContext);

        assertNotNull(file);
        assertEquals(MOCK_APK_PATH, file.getAbsolutePath());
    }

    @Test
    public void testEnsureGetLatestApkFileReturnsMostRecentIfMultipleApksFolders() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser, new VersionHelper()));
        File secondFolder = mock(File.class);
        File secondFile = mock(File.class);
        File[] folderList = new File[] {
                mockFolder,
                secondFolder
        };

        doReturn(getLaterVersionThanCurrent(1)).when(mockFolder).getName();
        doReturn(getLaterVersionThanCurrent(2)).when(secondFolder).getName();
        doReturn(new File[] {
                secondFile
        }).when(secondFolder).listFiles();
        doReturn("path/12345").when(secondFile).getAbsolutePath();
        doReturn(folderList).when(apkFileBrowser).getApksFoldersList(any(Context.class));

        File file = apkFileBrowser.getLatestApkFile(mockContext);

        assertNotNull(file);
        assertEquals("path/12345", file.getAbsolutePath());
    }

    @Test
    public void testEnsureGetLatestApkFileReturnsNullIfOldApkFolder() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser,
                new VersionHelper()));
        File[] folderList = new File[] {
                mockFolder
        };

        doReturn(BuildConfig.VERSION_NAME).when(mockFolder).getName();
        doReturn(folderList).when(apkFileBrowser).getApksFoldersList(any(Context.class));

        File file = apkFileBrowser.getLatestApkFile(mockContext);

        assertNull(file);
    }

    @Test
    public void testEnsureGetLatestApkFileReturnsNullIfOldApksFolders() {
        ApkFileBrowser apkFileBrowser = spy(new ApkFileBrowser(mockFileBrowser, new VersionHelper()));
        File secondFolder = mock(File.class);
        File[] folderList = new File[] {
                mockFolder,
                secondFolder
        };

        doReturn(BuildConfig.VERSION_NAME).when(mockFolder).getName();
        doReturn("1.9.0").when(secondFolder).getName();
        doReturn(folderList).when(apkFileBrowser).getApksFoldersList(any(Context.class));

        File file = apkFileBrowser.getLatestApkFile(mockContext);

        assertNull(file);
    }

    private String getLaterVersionThanCurrent(int toAdd) {
        String[] versionParts = BuildConfig.VERSION_NAME.split("\\.");
        versionParts[versionParts.length - 1] =
                Integer.parseInt(versionParts[versionParts.length - 1]) + toAdd + "";
        return join(".", versionParts);
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     * Copied from {@link android.text.TextUtils} join method as static methods do not work on tests
     *
     * @param tokens an array objects to be joined. Strings will be formed from
     *               the objects by calling object.toString().
     */
    private static String join(CharSequence delimiter, Object[] tokens) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Object token : tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(token);
        }
        return sb.toString();
    }
}
