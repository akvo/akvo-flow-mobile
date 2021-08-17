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
package org.akvo.flow.data.datasource.files

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import org.akvo.flow.data.util.FileHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class MediaResolverHelperTest {
    @Mock
    var mockContext: Context? = null

    @Mock
    var mockContentResolver: ContentResolver? = null

    @Mock
    var mockExifHelper: ExifHelper? = null

    @Mock
    var mockFileHelper: FileHelper? = null

    @Mock
    var mockUri: Uri? = null

    @Mock
    var mockInputStream: InputStream? = null

    @Mock
    var mockCursor: Cursor? = null

    var helper: MediaResolverHelper? = null

    @Before
    fun setUp() {
        helper = spy(MediaResolverHelper(mockContext, mockExifHelper, mockFileHelper))
        `when`(mockContext!!.contentResolver).thenReturn(mockContentResolver)
    }

    @Test
    @Throws(FileNotFoundException::class)
    fun inputStreamFromUriShouldReturnNullWhenUriNotFound() {
        `when`(mockContentResolver!!.openInputStream(any(
            Uri::class.java)))
            .thenThrow(FileNotFoundException::class.java)

        val inputStream = helper!!.getInputStreamFromUri(mockUri!!)

        assertNull(inputStream)
    }

    @Test
    @Throws(FileNotFoundException::class)
    fun openFileDescriptorShouldReturnNullWhenUriNotFound() {
        `when`(mockContentResolver!!.openFileDescriptor(any(
            Uri::class.java), anyString()))
            .thenThrow(FileNotFoundException::class.java)

        val fileDescriptor = helper!!.openFileDescriptor(mockUri!!)

        assertNull(fileDescriptor)
    }

    @Test
    fun removeDuplicateImageShouldCallRemoveDuplicatedExtraFileIfNonEmptyPath() {
        `when`(mockContentResolver!!.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.DATE_TAKEN
            ),
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")).thenReturn(mockCursor)
        `when`(mockCursor!!.moveToFirst()).thenReturn(true)
        `when`(mockCursor!!.getString(anyInt())).thenReturn("abc")
        `when`(mockExifHelper!!.areDatesEqual(any(
            InputStream::class.java), any(InputStream::class.java)))
            .thenReturn(false)
        `when`(mockContentResolver!!.openInputStream(any(Uri::class.java))).thenReturn(
            mockInputStream)
        doNothing().`when`(mockFileHelper)!!.close(any(Closeable::class.java))
        `when`(mockContentResolver!!.delete(mockUri!!, null, null)).thenReturn(1)

        val result = helper!!.removeDuplicateImage(mockUri!!)

        assertTrue(result)
        verify(helper, times(1))!!.removeDuplicatedExtraFile(mockUri!!, "abc")
    }

    @Test
    fun removeDuplicateImageShouldNotCallRemoveDuplicatedExtraFileIfEmptyPath() {
        `when`(mockContentResolver!!.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.DATE_TAKEN
            ),
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")).thenReturn(mockCursor)
        `when`(mockCursor!!.moveToFirst()).thenReturn(false)
        `when`(mockContentResolver!!.delete(mockUri!!, null, null)).thenReturn(1)

        val result = helper!!.removeDuplicateImage(mockUri!!)

        assertTrue(result)
        verify(helper, times(0))!!.removeDuplicatedExtraFile(mockUri!!, "abc")
    }

    @Test
    fun deleteMediaShouldReturnFalseIfNothingDeleted() {
        `when`(mockContentResolver!!.delete(mockUri!!, null, null)).thenReturn(0)

        val deleted = helper!!.deleteMedia(mockUri!!)

        assertFalse(deleted)
    }

    @Test
    fun deleteMediaShouldReturnTrueIfDeleted() {
        `when`(mockContentResolver!!.delete(mockUri!!, null, null)).thenReturn(1)

        val deleted = helper!!.deleteMedia(mockUri!!)

        assertTrue(deleted)
    }
}
