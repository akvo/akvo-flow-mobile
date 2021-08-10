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

import io.reactivex.observers.TestObserver
import org.akvo.flow.data.util.FlowFileBrowser
import org.akvo.flow.utils.FileHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class FileDataSourceTest {

    @Mock
    internal var mockFileHelper: FileHelper? = null

    @Mock
    internal var mockFlowFileBrowser: FlowFileBrowser? = null

    @Mock
    internal var mockFolder: File? = null

    @Mock
    internal var mockFile: File? = null

    @Test
    fun moveZipFilesShouldCompleteSuccessfully() {
        val observer = TestObserver<Void>()

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.moveZipFiles().subscribe(observer)

        verify(fileDataSource, times(1))!!.moveFilesInFolder("akvoflow/data/files")
        observer.assertNoErrors()
    }

    @Test
    fun moveMediaFilesShouldCompleteSuccessfully() {
        val observer = TestObserver<Void>()

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.moveMediaFiles().subscribe(observer)

        verify(fileDataSource, times(1))!!.moveFilesInFolder("akvoflow/data/media/result-images")
        verify(fileDataSource, times(1))!!.moveFilesInFolder("akvoflow/data/media")
        observer.assertNoErrors()
    }

    @Test
    fun moveFilesInFolderShouldCompleteSuccessfully() {
        `when`(mockFlowFileBrowser!!.getPublicFolder(anyString())).thenReturn(mockFolder)
        `when`(mockFolder!!.exists()).thenReturn(true)
        val listOfFiles = emptyArray<File>()
        `when`(mockFolder!!.listFiles()).thenReturn(listOfFiles)

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.moveFilesInFolder("abc")

        verify(fileDataSource, times(1))!!.moveAndDeleteFolder("abc", mockFolder, listOfFiles)
    }

    @Test
    fun moveFilesInFolderShouldNotMoveAndDeleteIfNullFolder() {
        `when`(mockFlowFileBrowser!!.getPublicFolder(anyString())).thenReturn(null)
        val listOfFiles = emptyArray<File>()

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.moveFilesInFolder("abc")

        verify(fileDataSource, times(0))!!.moveAndDeleteFolder("abc", mockFolder, listOfFiles)
    }

    @Test
    fun moveFilesInFolderShouldNotMoveAndDeleteIfFolderDoesNotExist() {
        `when`(mockFlowFileBrowser!!.getPublicFolder(anyString())).thenReturn(mockFolder)
        `when`(mockFolder!!.exists()).thenReturn(false)
        val listOfFiles = emptyArray<File>()

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.moveFilesInFolder("abc")

        verify(fileDataSource, times(0))!!.moveAndDeleteFolder("abc", mockFolder, listOfFiles)
    }

    @Test(expected = SecurityException::class)
    fun moveFilesInFolderShouldFailIfSecurityException() {
        `when`(mockFlowFileBrowser!!.getPublicFolder(anyString())).thenReturn(mockFolder)
        `when`(mockFolder!!.exists()).thenThrow(SecurityException())
        val listOfFiles = emptyArray<File>()

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.moveFilesInFolder("abc")

        verify(fileDataSource, times(0))!!.moveAndDeleteFolder("abc", mockFolder, listOfFiles)
    }

    @Test
    fun moveAndDeleteFolderShouldDoNothingIfNullFiles() {
        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.moveAndDeleteFolder("abc", mockFolder, null)

        verify(fileDataSource, times(0))!!.moveFiles(any(), anyString())
        verify(mockFolder, times(0))!!.delete()
    }

    @Test
    fun moveAndDeleteFolderShouldCompleteSuccessfullyForEmptyFolder() {
        val listOfFiles = emptyArray<File>()

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.moveAndDeleteFolder("abc", mockFolder, listOfFiles)

        verify(fileDataSource, times(1))!!.moveFiles(any(), anyString())
        verify(mockFolder, times(1))!!.delete()
    }

    @Test
    fun moveAndDeleteFolderShouldNoDeleteIfNotAllFilesWereMoved() {
        val listOfFiles = arrayOf(mockFile, mockFile)

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))
        `when`(fileDataSource!!.moveFiles(any(), anyString())).thenReturn(1)

        fileDataSource.moveAndDeleteFolder("abc", mockFolder, listOfFiles)

        verify(fileDataSource, times(1))!!.moveFiles(any(), anyString())
        verify(mockFolder, times(0))!!.delete()
    }

    @Test
    fun moveAndDeleteFolderShouldCompleteSuccessfullyForNonEmptyFolder() {
        val listOfFiles = arrayOf(mockFile, mockFile)

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))
        `when`(fileDataSource!!.moveFiles(any(), anyString())).thenReturn(2)

        fileDataSource.moveAndDeleteFolder("abc", mockFolder, listOfFiles)

        verify(fileDataSource, times(1))!!.moveFiles(any(), anyString())
        verify(mockFolder, times(1))!!.delete()
    }

    @Test
    fun moveFilesShouldReturn0IfNullFiles() {
        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        val moved = fileDataSource.moveFiles(null, "abc")

        assertEquals(0, moved)
        verify(mockFlowFileBrowser, times(0))!!.getExistingInternalFolder(anyString())
    }

    @Test
    fun moveFilesShouldReturn0IfNoFiles() {
        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        val moved = fileDataSource.moveFiles(emptyArray<File>(), "abc")

        assertEquals(0, moved)
        verify(mockFlowFileBrowser, times(0))!!.getExistingInternalFolder(anyString())
    }

    @Test
    fun moveFilesShouldIgnoreNonDeletedFiles() {
        `when`(mockFile!!.isDirectory).thenReturn(false)
        `when`(mockFileHelper!!.copyFileToFolder(any(), any())).thenReturn(null)
        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        val moved = fileDataSource.moveFiles(arrayOf(mockFile), "abc")

        assertEquals(0, moved)
        verify(mockFlowFileBrowser, times(1))!!.getExistingInternalFolder(anyString())
    }

    @Test
    fun moveFilesShouldReturnCorrectNumberOfMovedFiles() {
        `when`(mockFile!!.isDirectory).thenReturn(false)
        `when`(mockFileHelper!!.copyFileToFolder(any(), any())).thenReturn("abc")
        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        val moved = fileDataSource.moveFiles(arrayOf(mockFile, mockFile), "abc")

        assertEquals(2, moved)
        verify(mockFlowFileBrowser, times(1))!!.getExistingInternalFolder(anyString())
    }

    @Test
    fun copyFileOrDirShouldCallDeleteDirectoryIfNeeded() {
        `when`(mockFolder!!.isDirectory).thenReturn(true)

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.copyFileOrDir("akvoflow/data/files", mockFolder, mockFolder)

        verify(fileDataSource, times(1)).deleteDirectory(any())
        verify(fileDataSource, times(0)).moveAndDeleteFile(any(), any())
    }

    @Test
    fun copyFileOrDirShouldCallMoveAndDeleteFileIfNotFilesFolder() {
        `when`(mockFolder!!.isDirectory).thenReturn(true)

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.copyFileOrDir("akvoflow/data", mockFolder, mockFolder)

        verify(fileDataSource, times(0)).deleteDirectory(any())
        verify(fileDataSource, times(1)).moveAndDeleteFile(any(), any())
    }

    @Test
    fun copyFileOrDirShouldCallMoveAndDeleteFileIfFile() {
        `when`(mockFolder!!.isDirectory).thenReturn(false)

        val fileDataSource = spy(FileDataSource(mockFileHelper, mockFlowFileBrowser, null))

        fileDataSource.copyFileOrDir("akvoflow/data/files", mockFolder, mockFolder)

        verify(fileDataSource, times(0)).deleteDirectory(any())
        verify(fileDataSource, times(1)).moveAndDeleteFile(any(), any())
    }
}
