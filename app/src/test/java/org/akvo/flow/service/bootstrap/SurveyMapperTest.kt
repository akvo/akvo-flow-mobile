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
import org.akvo.flow.domain.Survey
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.domain.SurveyMetadata
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SurveyMapperTest {

    @MockK
    lateinit var surveyMetadata: SurveyMetadata

    @MockK
    lateinit var surveyGroup: SurveyGroup

    @MockK
    lateinit var survey: Survey

    private lateinit var surveyMapper: SurveyMapper

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        surveyMapper = spyk(SurveyMapper())
        every { (surveyMetadata.id) }.returns("1234")
        every { (surveyMetadata.version) }.returns(1.0)
        every { (surveyMetadata.name) }.returns("form1")
        every { (surveyMetadata.surveyGroup) }.returns(surveyGroup)
        every { (survey.setLocation(any())) }.returns(Unit)
        every { (survey.setFileName(any())) }.returns(Unit)
        every { (survey.name) }.returns("name")
        every { (survey.setName(any())) }.returns(Unit)
        every { (survey.setSurveyGroup(any())) }.returns(Unit)
        every { (survey.setVersion(any())) }.returns(Unit)
    }

    @Test
    fun getSurveyIdFromFilePathShouldReturnEmptyIfFolderMissing() {
        val surveyId = surveyMapper.getSurveyIdFromFilePath("file.xml")
        assertEquals("", surveyId)
    }

    //TODO: separate into several methods
    @Test
    fun getSurveyIdFromFilePathShouldReturnCorrectIdFromFolder() {
        var surveyId = surveyMapper.getSurveyIdFromFilePath("123/file.xml")
        assertEquals("123", surveyId)
        surveyId = surveyMapper.getSurveyIdFromFilePath("folder/123/file.xml")
        assertEquals("123", surveyId)
        surveyId = surveyMapper.getSurveyIdFromFilePath("123/folder/file.xml")
        assertEquals("123", surveyId)
        surveyId = surveyMapper.getSurveyIdFromFilePath("folder1/123/folder/file.xml")
        assertEquals("123", surveyId)
        surveyId = surveyMapper.getSurveyIdFromFilePath("/")
        assertEquals("", surveyId)
    }

    @Test
    fun getSurveyIdFromFilePathShouldReturnUseClosestFolderNameIfNoId() {
        val surveyId = surveyMapper.getSurveyIdFromFilePath("abc/folder/survey.xml")

        assertEquals("folder", surveyId)
    }

    @Test
    fun generateFileNameShouldReturnEmptyIfOnlySlash() {
        val fileName = surveyMapper.generateFileName("/")

        assertEquals("", fileName)
    }

    //TODO: separate into several methods
    @Test
    fun generateFileNameShouldReturnCorrectSurveyName() {
        var fileName = surveyMapper.generateFileName("file.xml")
        assertEquals("file.xml", fileName)
        fileName = surveyMapper.generateFileName("folder/file.xml")
        assertEquals("file.xml", fileName)
        fileName = surveyMapper.generateFileName("folder/folder/file.xml")
        assertEquals("file.xml", fileName)
    }

    @Test
    fun generateSurveyFolderNameShouldReturnEmptyForEmptyEntryName() {
        val folderName = surveyMapper.generateSurveyFolderName("")

        assertEquals("", folderName)
    }

    @Test
    fun generateSurveyFolderNameShouldReturnEmptyOnePartEntryName() {
        val folderName = surveyMapper.generateSurveyFolderName("name")

        assertEquals("", folderName)
    }

    @Test
    fun generateSurveyFolderNameShouldReturnCorrectFolderName() {
        val folderName = surveyMapper.generateSurveyFolderName("folder/file.xml")

        assertEquals("folder", folderName)
    }

    @Test
    fun createOrUpdateSurveyShouldCreateSurveyIfDbSurveyNull() {
        surveyMapper.createOrUpdateSurvey("file.xml", "123", null, "folder", surveyMetadata)

        verify { surveyMapper.createSurvey(any(), any(), any()) }
    }

    @Test
    fun createOrUpdateSurveyShouldNotCreateSurveyIfDbSurveyNotNull() {
        surveyMapper.createOrUpdateSurvey("file.xml", "123", survey, "folder", surveyMetadata)

        verify(atLeast = 0, atMost = 0) { surveyMapper.createSurvey(any(), any(), any()) }
    }
}