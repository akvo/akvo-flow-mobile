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

package org.akvo.flow.data.entity.form

import androidx.test.platform.app.InstrumentationRegistry
import org.akvo.flow.data.tests.R
import org.akvo.flow.data.util.FileHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.InputStream

@RunWith(MockitoJUnitRunner::class)
class XmlFormParserTest {

    @Test
    fun shouldParseCorrectlyFormWith1Group() {
        val parser = XmlFormParser(FileHelper())

        val input: InputStream =
            InstrumentationRegistry.getInstrumentation().targetContext.resources
            .openRawResource(R.raw.date_form)
        val result: DataForm = parser.parseXmlForm(input)

        assertEquals("1.0", result.version)
        assertEquals("DateForm", result.name)
        assertEquals(1, result.groups.size)
        assertEquals("DateFormGroup", result.groups[0].heading)
        assertEquals(true, result.groups[0].repeatable)
    }

    @Test
    fun shouldParseCorrectlyMissingValues() {
        val parser = XmlFormParser(FileHelper())

        val input: InputStream =
            InstrumentationRegistry.getInstrumentation().targetContext.resources
                .openRawResource(R.raw.empty_form)
        val result: DataForm = parser.parseXmlForm(input)

        assertEquals("0.0", result.version)
        assertEquals("", result.name)
        assertEquals(1, result.groups.size)
        assertEquals("", result.groups[0].heading)
        assertEquals(false, result.groups[0].repeatable)
    }

}