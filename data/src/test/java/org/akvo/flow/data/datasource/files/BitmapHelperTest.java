/*
 * Copyright (C) 2019-2020 Stichting Akvo (Akvo Foundation)
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

import android.graphics.Bitmap;

import org.akvo.flow.data.util.FileHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.Closeable;
import java.io.OutputStream;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BitmapHelperTest {

    @Mock
    FileHelper mockFileHelper;

    @Mock
    Bitmap mockBitmap;

    private BitmapHelper bitmapHelper;

    @Before
    public void setUp() {
        doNothing().when(mockFileHelper).close(any(Closeable.class));
        when(mockBitmap.compress(any(Bitmap.CompressFormat.class), anyInt(), any(OutputStream.class))).thenReturn(true);
        bitmapHelper = new BitmapHelper(mockFileHelper);
    }

    @Test
    public void compressBitmapShouldReturnFalseForNullBitmap() {
        boolean compressed = bitmapHelper.compressBitmap(null, "abc");

        assertFalse(compressed);
    }

    @Test
    public void compressBitmapShouldReturnFalseForFileNotFound() {
        boolean compressed = bitmapHelper.compressBitmap(mockBitmap, "");

        assertFalse(compressed);
    }

    @Test
    public void compressBitmapShouldReturnTrue() {
        boolean compressed = bitmapHelper.compressBitmap(mockBitmap, "abc");

        assertTrue(compressed);
    }
}
