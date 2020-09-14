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

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.akvo.flow.data.database.SurveyDbDataSource
import org.akvo.flow.util.SurveyFileNameGenerator
import org.akvo.flow.util.SurveyIdGenerator
import org.akvo.flow.util.files.FormFileBrowser
import org.akvo.flow.util.files.FormResourcesFileBrowser
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@RunWith(JUnit4::class)
class BootstrapProcessorTest {

    @MockK
    lateinit var applicationContext: Context

    @MockK
    lateinit var resourcesFileUtil: FormResourcesFileBrowser

    @MockK
    lateinit var surveyFileNameGenerator: SurveyFileNameGenerator

    @MockK
    lateinit var surveyIdGenerator: SurveyIdGenerator

    @MockK
    lateinit var databaseAdapter: SurveyDbDataSource

    @MockK
    lateinit var formFileBrowser: FormFileBrowser

    @MockK
    lateinit var zipFile: ZipFile

    lateinit var processor: BootstrapProcessor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        processor = BootstrapProcessor(resourcesFileUtil,
            applicationContext,
            surveyFileNameGenerator,
            surveyIdGenerator,
            databaseAdapter,
            formFileBrowser)
        //`when`(applicationContext.getString(anyInt())).thenReturn("")
    }

    @Test
    fun processZipFileShouldReturnSuccessForEmptyZipFile() {
        every { (zipFile.entries()) }.returns(object : Enumeration<ZipEntry>,
            MutableIterator<ZipEntry?> {
            override fun hasMoreElements(): Boolean {
                return false
            }

            override fun nextElement(): ZipEntry? {
                return null
            }

            override fun hasNext(): Boolean {
                return false
            }

            override fun next(): ZipEntry? {
                return null
            }

            override fun remove() {
                //ignored
            }
        })

        val result = processor.processZipFile(zipFile)
        assertTrue(result is ProcessingResult.ProcessingSuccess)
    }
}
