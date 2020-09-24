/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.service.bootstrap

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import org.akvo.flow.data.database.SurveyDbDataSource
import org.akvo.flow.domain.Survey
import org.akvo.flow.domain.SurveyMetadata
import org.akvo.flow.util.ConstantUtil
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@RunWith(JUnit4::class)
class BootstrapProcessorTest {

    @MockK
    lateinit var databaseAdapter: SurveyDbDataSource

    @MockK
    lateinit var zipFile: ZipFile

    @MockK
    lateinit var zipEntry: ZipEntry

    @MockK
    lateinit var surveyMapper: SurveyMapper

    @MockK
    lateinit var fileProcessor: FileProcessor

    private lateinit var processor: BootstrapProcessor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        processor = spyk(BootstrapProcessor(databaseAdapter, surveyMapper, fileProcessor))
        every {
            (processor.processCascadeResource(any(),
                any()))
        }.returns(ProcessingResult.ProcessingSuccess)
        every { (databaseAdapter.getSurvey(any())) }.returns(Survey())
        every { (databaseAdapter.addSurveyGroup(any())) }.returns(Unit)
        every { (databaseAdapter.saveSurvey(any())) }.returns(Unit)
    }

    @Test
    fun processZipFileShouldReturnSuccessForEmptyZipFile() {
        every { (zipFile.entries()) }.returns(TestEntries(emptySequence()))
        every {
            processor.processSurveyFile(any(),
                any(),
                any())
        }.returns(ProcessingResult.ProcessingSuccess)

        val result = processor.processZipFile(zipFile)

        assertTrue(result is ProcessingResult.ProcessingSuccess)
    }

    @Test
    fun processZipFileShouldReturnSuccessForZipFileContainingFileWithNullName() {
        every { (zipFile.entries()) }.returns(TestEntries(sequenceOf(zipEntry)))
        every { (zipEntry.name) }.returns(null)
        every {
            processor.processSurveyFile(any(),
                any(),
                any())
        }.returns(ProcessingResult.ProcessingSuccess)

        val result = processor.processZipFile(zipFile)

        assertTrue(result is ProcessingResult.ProcessingSuccess)
    }

    @Test
    fun processZipFileShouldReturnSuccessForZipFileContainingOtherFiles() {
        every { (zipFile.entries()) }.returns(TestEntries(sequenceOf(zipEntry)))
        every { (zipEntry.name) }.returns("file.jpg")
        every {
            processor.processSurveyFile(any(),
                any(),
                any())
        }.returns(ProcessingResult.ProcessingSuccess)

        val result = processor.processZipFile(zipFile)

        assertTrue(result is ProcessingResult.ProcessingSuccess)
    }

    @Test
    fun processZipFileShouldProcessCascadeResCorrectly() {
        every { (zipFile.entries()) }.returns(TestEntries(sequenceOf(zipEntry)))
        every { (zipEntry.name) }.returns("file" + ConstantUtil.CASCADE_RES_SUFFIX)
        every {
            processor.processSurveyFile(any(),
                any(),
                any())
        }.returns(ProcessingResult.ProcessingSuccess)

        val result = processor.processZipFile(zipFile)

        verify { processor.processCascadeResource(any(), any())  }
        assertTrue(result is ProcessingResult.ProcessingSuccess)
    }

    @Test
    fun processZipFileShouldProcessFormsCorrectly() {
        every { (zipFile.entries()) }.returns(TestEntries(sequenceOf(zipEntry)))
        every { (zipEntry.name) }.returns("file" + ConstantUtil.XML_SUFFIX)
        every {
            processor.processSurveyFile(any(),
                any(),
                any())
        }.returns(ProcessingResult.ProcessingSuccess)

        val result = processor.processZipFile(zipFile)

        verify { processor.processSurveyFile(any(), any(), any()) }
        assertTrue(result is ProcessingResult.ProcessingSuccess)
    }

    @Test
    fun processSurveyFileShouldFailForEmptyAppName() {
        every { (zipFile.entries()) }.returns(TestEntries(sequenceOf(zipEntry)))
        every { (zipEntry.name) }.returns("file" + ConstantUtil.XML_SUFFIX)
        every { (surveyMapper.generateFileName(any())) }.returns("file.xml")
        every { (surveyMapper.getSurveyIdFromFilePath(any())) }.returns("id")
        every { (surveyMapper.generateSurveyFolderName(any())) }.returns("folder")
        every { (fileProcessor.createAndCopyNewSurveyFile(any(), any(), any(), any())) }.returns(spyk(File("file.xml")))
        val metadata = SurveyMetadata()
        metadata.app = ""
        every { (fileProcessor.readBasicSurveyData(any())) }.returns(metadata)

        val result = processor.processSurveyFile(zipFile, zipEntry, zipEntry.name)

        assertTrue(result is ProcessingResult.ProcessingErrorWrongDashboard)
    }

    @Test
    fun processSurveyFileShouldFailForWrongAppName() {
        every { (zipFile.entries()) }.returns(TestEntries(sequenceOf(zipEntry)))
        every { (zipEntry.name) }.returns("file" + ConstantUtil.XML_SUFFIX)
        every { (surveyMapper.generateFileName(any())) }.returns("file.xml")
        every { (surveyMapper.getSurveyIdFromFilePath(any())) }.returns("id")
        every { (surveyMapper.generateSurveyFolderName(any())) }.returns("folder")
        every { (fileProcessor.createAndCopyNewSurveyFile(any(), any(), any(), any())) }.returns(spyk(File("file.xml")))
        val metadata = SurveyMetadata()
        metadata.app = "dev"
        every { (fileProcessor.readBasicSurveyData(any())) }.returns(metadata)

        val result = processor.processSurveyFile(zipFile, zipEntry, zipEntry.name)

        assertTrue(result is ProcessingResult.ProcessingErrorWrongDashboard)
    }

    @Test
    fun processSurveyFileShouldSucceedForCorrectAppName() {
        every { (zipFile.entries()) }.returns(TestEntries(sequenceOf(zipEntry)))
        every { (zipEntry.name) }.returns("file" + ConstantUtil.XML_SUFFIX)
        every { (surveyMapper.generateFileName(any())) }.returns("file.xml")
        every { (surveyMapper.getSurveyIdFromFilePath(any())) }.returns("id")
        every { (surveyMapper.generateSurveyFolderName(any())) }.returns("folder")
        every { (fileProcessor.createAndCopyNewSurveyFile(any(), any(), any(), any())) }.returns(spyk(File("file.xml")))
        val metadata = SurveyMetadata()
        metadata.app = "akvoflow-uat1"
        every { (fileProcessor.readBasicSurveyData(any())) }.returns(metadata)
        every { (surveyMapper.createOrUpdateSurvey(any(), any(), any(), any(), any())) }.returns(Survey())

        val result = processor.processSurveyFile(zipFile, zipEntry, zipEntry.name)

        assertTrue(result is ProcessingResult.ProcessingSuccess)
    }

    class TestEntries(sequence: Sequence<ZipEntry>) : Enumeration<ZipEntry>,
        MutableIterator<ZipEntry?> {
        private val iter = sequence.iterator()

        override fun hasMoreElements(): Boolean {
            return iter.hasNext()
        }

        override fun nextElement(): ZipEntry? {
            return iter.next()
        }

        override fun hasNext(): Boolean {
            return iter.hasNext()
        }

        override fun next(): ZipEntry? {
            return iter.next()
        }

        override fun remove() {
            //ignored
        }
    }
}
