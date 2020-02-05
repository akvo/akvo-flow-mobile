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

package org.akvo.flow.data.datasource.files;

import org.akvo.flow.data.entity.images.DataImageLocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@RunWith(PowerMockRunner.class)
public class ExifHelperTest {

    @Mock
    InputStream mockInputStrem;

    @Test
    public void updateExifDataShouldReturnFalseForNullInputStream() {
        ExifHelper helper = new ExifHelper();

        DataImageLocation location = helper.updateExifData(null, "");

        assertNull(location.getLatitude());
        assertNull(location.getLongitude());
    }

    @Test
    public void updateExifDataShouldReturnFalseForInvalidFile() throws IOException {
        ExifHelper helper = new ExifHelper();
        File file = File.createTempFile("abc", ".txt");

        DataImageLocation location = helper.updateExifData(mockInputStrem, file.getAbsolutePath());

        assertNull(location.getLatitude());
        assertNull(location.getLongitude());
    }

    @Test
    public void areDatesEqualShouldReturnFalseForNullInputStream() {
        ExifHelper helper = new ExifHelper();

        boolean equal = helper.areDatesEqual(null, mockInputStrem);

        assertFalse(equal);
    }
}
